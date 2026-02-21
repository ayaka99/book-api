package com.example.demo.service

import com.example.demo.domain.Book
import com.example.demo.dto.CreateBookRequestDto
import com.example.demo.dto.UpdateBookRequestDto
import com.example.demo.error.NotFoundException
import com.example.demo.repository.BookRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 書籍と著者の紐付けを管理するサービス。
 *
 * 更新時は中間テーブルを全削除して再構築する仕様。
 */
@Service
class BookService(
    private val bookRepository: BookRepository,
    private val authorService: AuthorService
) {
    @Transactional
    fun createBook(createBookRequestDto: CreateBookRequestDto): Long {

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

    fun getBooksByAuthorId(authorId: Long): List<Book> {
        return bookRepository.findBooksByAuthorId(authorId)
    }

    @Transactional
    fun updateBookWithAuthors(bookId: Long, updateBookRequestDto: UpdateBookRequestDto): Book {

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
