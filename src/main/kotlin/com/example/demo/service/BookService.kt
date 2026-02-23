package com.example.demo.service

import com.example.demo.domain.Book
import com.example.demo.dto.CreateAuthorRequestDto
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

    companion object {
        private const val UNPUBLISHED = 0
        private const val PUBLISHED = 1
        private val ALLOWED_PUBLICATION_STATUS = setOf(UNPUBLISHED, PUBLISHED)
    }

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

        validateBookInput(
            price = createBookRequestDto.price,
            publicationStatus = createBookRequestDto.publicationStatus,
            authorIds = createBookRequestDto.authorIds,
            authorsCount = createBookRequestDto.authors?.size ?: 0
        )

        val book = bookRepository.insertBook(
            title = createBookRequestDto.title,
            price = createBookRequestDto.price,
            publicationStatus = createBookRequestDto.publicationStatus
        )
        val bookId = book.bookId

        val allAuthorIds = resolveAuthorIds(
            existingAuthorIds = createBookRequestDto.authorIds,
            newAuthors = createBookRequestDto.authors
        )

        allAuthorIds.forEach { authorId ->
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

        validateBookInput(
            price = updateBookRequestDto.price,
            publicationStatus = updateBookRequestDto.publicationStatus,
            authorIds = updateBookRequestDto.authorIds,
            authorsCount = updateBookRequestDto.authors?.size ?: 0
        )

        val current = bookRepository.findBookByBookId(bookId)
            ?: throw NotFoundException("book not found: $bookId")

        // 出版済（1）→未出版（0）への状態遷移は禁止
        if (current.publicationStatus == PUBLISHED && updateBookRequestDto.publicationStatus == UNPUBLISHED) {
            throw IllegalArgumentException("published book cannot be changed to unpublished")
        }

        val updatedBook = bookRepository.updateBook(
            bookId = bookId,
            title = updateBookRequestDto.title,
            price = updateBookRequestDto.price,
            publicationStatus = updateBookRequestDto.publicationStatus
        )

        // 既存の紐付けを全削除し、指定内容で再構築
        bookRepository.deleteBookAuthorRelations(bookId)

        val allAuthorIds = resolveAuthorIds(
            existingAuthorIds = updateBookRequestDto.authorIds,
            newAuthors = updateBookRequestDto.authors
        )

        allAuthorIds.forEach { authorId ->
            bookRepository.insertBookAuthorRelation(bookId, authorId)
        }

        return updatedBook
    }

    private fun validateBookInput(
        price: Int,
        publicationStatus: Int,
        authorIds: List<Long>?,
        authorsCount: Int
    ) {
        if (publicationStatus !in ALLOWED_PUBLICATION_STATUS) {
            throw IllegalArgumentException("publicationStatus must be 0 or 1")
        }
        if (price < 0) {
            throw IllegalArgumentException("price must be 0 or greater")
        }
        val totalAuthors = (authorIds?.size ?: 0) + authorsCount
        if (totalAuthors == 0) {
            throw IllegalArgumentException("at least one author is required")
        }
    }

    private fun resolveAuthorIds(
        existingAuthorIds: List<Long>?,
        newAuthors: List<CreateAuthorRequestDto>?
    ): List<Long> {
        val ids = mutableListOf<Long>()
        ids.addAll(existingAuthorIds ?: emptyList())

        val newAuthorIds = (newAuthors ?: emptyList()).map { createAuthorRequestDto ->
            authorService.createAuthor(
                name = createAuthorRequestDto.name,
                birthDate = createAuthorRequestDto.birthDate
            ).authorId
        }
        ids.addAll(newAuthorIds)

        return ids
    }
}
