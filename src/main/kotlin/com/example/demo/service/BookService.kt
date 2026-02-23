package com.example.demo.service

import com.example.demo.domain.Book
import com.example.demo.dto.CreateBookRequestDto
import com.example.demo.dto.UpdateBookRequestDto
import com.example.demo.error.NotFoundException
import com.example.demo.repository.BookRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 書籍の登録・更新および著者との紐付け管理を行うサービス。
 *
 * 業務ルール:
 * - 価格は0以上
 * - 書籍には著者が最低1人必要
 * - publicationStatusは 0/1 のみ許可
 * - 出版済（1）→未出版（0）への状態遷移は禁止
 *
 * 書籍更新時は既存の著者紐付けを一度削除し、指定内容で再構築する。
 */
@Service
class BookService(
    private val bookRepository: BookRepository,
    private val authorService: AuthorService
) {
    /**
     * 書籍を新規作成し、指定された著者と紐付ける。
     *
     * 著者指定は以下を組み合わせ可能:
     * - authorIds: 既存著者のIDリスト
     * - authors: 新規著者の入力情報
     *
     * @throws IllegalArgumentException 入力が業務ルールに違反する場合
     */
    @Transactional
    fun createBook(createBookRequestDto: CreateBookRequestDto): Long {

        if (createBookRequestDto.publicationStatus !in setOf(0, 1)) {
            throw IllegalArgumentException("publicationStatus must be 0 or 1")
        }

        val totalAuthors =
            (createBookRequestDto.authorIds?.size ?: 0) +
                    (createBookRequestDto.authors?.size ?: 0)

        if (totalAuthors == 0) {
            throw IllegalArgumentException("at least one author is required")
        }

        if (createBookRequestDto.price < 0) {
            throw IllegalArgumentException("price must be 0 or greater")
        }

        val book = bookRepository.insertBook(
            title = createBookRequestDto.title,
            price = createBookRequestDto.price,
            publicationStatus = createBookRequestDto.publicationStatus
        )
        val bookId = book.bookId

        // 既存著者ID（authorIds）
        val existingAuthorIds: List<Long> = createBookRequestDto.authorIds ?: emptyList()

        // 新規著者（authors）→ 作成してID化
        val newAuthorIds: List<Long> = (createBookRequestDto.authors ?: emptyList())
            .map { dto ->
                authorService.createAuthor(
                    name = dto.name,
                    birthDate = dto.birthDate
                ).authorId
            }

        val allAuthorIds: List<Long> = existingAuthorIds + newAuthorIds

        for (authorId in allAuthorIds) {
            bookRepository.insertBookAuthorRelation(bookId, authorId)
        }

        return bookId
    }

    /**
     * 指定著者に紐づく書籍一覧を取得する。
     *
     * 読み取り専用トランザクションで実行する。
     */
    @Transactional(readOnly = true)
    fun getBooksByAuthorId(authorId: Long): List<Book> {
        return bookRepository.findBooksByAuthorId(authorId)
    }

    /**
     * 書籍を更新し、著者紐付けを指定内容で置換する。
     *
     * トランザクション内で以下を一括実行する:
     * - 書籍本体の更新
     * - 新規著者の作成（指定がある場合）
     * - 中間テーブルの置換（既存削除→再登録）
     *
     * @throws NotFoundException 指定IDの書籍が存在しない場合
     * @throws IllegalArgumentException 入力が業務ルールに違反する場合
     */
    @Transactional
    fun updateBookWithAuthors(bookId: Long, updateBookRequestDto: UpdateBookRequestDto): Book {

        if (updateBookRequestDto.publicationStatus !in setOf(0, 1)) {
            throw IllegalArgumentException("publicationStatus must be 0 or 1")
        }

        val totalAuthors =
            (updateBookRequestDto.authorIds?.size ?: 0) +
                    (updateBookRequestDto.authors?.size ?: 0)

        if (totalAuthors == 0) {
            throw IllegalArgumentException("at least one author is required")
        }

        val current = bookRepository.findBookByBookId(bookId)
            ?: throw NotFoundException("book not found: $bookId")

        val currentStatus = current.publicationStatus
        val newStatus = updateBookRequestDto.publicationStatus

        val UNPUBLISHED = 0
        val PUBLISHED = 1

        if (currentStatus == PUBLISHED && newStatus == UNPUBLISHED) {
            throw IllegalArgumentException("published book cannot be changed to unpublished")
        }

        if (updateBookRequestDto.price < 0) {
            throw IllegalArgumentException("price must be 0 or greater")
        }

        val updatedBook = bookRepository.updateBook(
            bookId = bookId,
            title = updateBookRequestDto.title,
            price = updateBookRequestDto.price,
            publicationStatus = updateBookRequestDto.publicationStatus
        )

        // 既存の紐付けを全削除
        bookRepository.deleteBookAuthorRelations(bookId)

        // 既存著者ID
        val existingAuthorIds: List<Long> = updateBookRequestDto.authorIds ?: emptyList()

        // 新規著者を作成してID化
        val newAuthorIds: List<Long> = (updateBookRequestDto.authors ?: emptyList())
            .map { createAuthorRequestDto ->
                authorService.createAuthor(
                    name = createAuthorRequestDto.name,
                    birthDate = createAuthorRequestDto.birthDate
                ).authorId
            }

        val allAuthorIds: List<Long> = existingAuthorIds + newAuthorIds

        for (authorId in allAuthorIds) {
            bookRepository.insertBookAuthorRelation(bookId, authorId)
        }

        return updatedBook
    }
}
