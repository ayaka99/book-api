package com.example.demo.service

import com.example.demo.domain.Author
import com.example.demo.domain.Book
import com.example.demo.dto.CreateAuthorRequestDto
import com.example.demo.dto.CreateBookRequestDto
import com.example.demo.dto.UpdateBookRequestDto
import com.example.demo.repository.BookRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.time.LocalDate

class BookServiceTest {

    private val bookRepository = mock<BookRepository>()
    private val authorService = mock<AuthorService>()
    private val sut = BookService(bookRepository, authorService)

    @BeforeEach
    fun setup() {
        whenever(bookRepository.findBookByBookId(1L)).thenReturn(
            Book(
                bookId = 1L,
                title = "Existing",
                price = 1000,
                publicationStatus = 0
            )
        )
    }

    /**
     *  createBook_01
     *  書籍登録時、著者が0人の場合は IllegalArgumentException
     *  著者は最低1人必須であることを確認
     *  AuthorServiceが呼ばれないことを確認
     */
    @Test
    fun createBook_01() {
        // 準備
        val req = CreateBookRequestDto(
            title = "New",
            price = 1000,
            publicationStatus = 0,
            authorIds = emptyList(),
            authors = emptyList()
        )

        // 実行、検証
        assertThrows(IllegalArgumentException::class.java) {
            sut.createBook(req)
        }

        verifyNoMoreInteractions(authorService)
    }

    /**
     * createBook_02
     * 既存著者IDのみを指定して書籍を作成できること（insertBook → relation登録の確認）
     */
    @Test
    fun createBook_02() {
        // 準備
        val req = CreateBookRequestDto(
            title = "Book",
            price = 1000,
            publicationStatus = 1,
            authorIds = listOf(10L, 20L),
            authors = emptyList()
        )
        whenever(
            bookRepository.insertBook(
                title = req.title,
                price = req.price,
                publicationStatus = req.publicationStatus
            )
        ).thenReturn(
            Book(
                bookId = 1L,
                title = req.title,
                price = req.price,
                publicationStatus = req.publicationStatus
            )
        )

        // 実行
        val result = sut.createBook(req)

        // 検証
        assertEquals(1L, result)

        val order = inOrder(bookRepository)
        order.verify(bookRepository).insertBook(req.title, req.price, req.publicationStatus)
        order.verify(bookRepository).insertBookAuthorRelation(1L, 10L)
        order.verify(bookRepository).insertBookAuthorRelation(1L, 20L)
        order.verifyNoMoreInteractions()
        verifyNoMoreInteractions(authorService)
    }

    /**
     * createBook_03
     * authorIds に重複があっても1回ずつしか関連登録されないこと
     */
    @Test
    fun createBook_03() {
        // 準備
        val req = CreateBookRequestDto(
            title = "Book",
            price = 1000,
            publicationStatus = 1,
            authorIds = listOf(10L, 10L),
            authors = emptyList()
        )
        whenever(
            bookRepository.insertBook(req.title, req.price, req.publicationStatus)
        ).thenReturn(
            Book(
                bookId = 1L,
                title = req.title,
                price = req.price,
                publicationStatus = req.publicationStatus
            )
        )

        // 実行
        sut.createBook(req)

        // 検証
        verify(bookRepository).insertBook(req.title, req.price, req.publicationStatus)
        verify(bookRepository).insertBookAuthorRelation(1L, 10L)
        verify(bookRepository, times(1)).insertBookAuthorRelation(1L, 10L)
        verifyNoMoreInteractions(authorService)
    }

    /**
     *  updateBookWithAuthors_01
     *  既存著者のみ指定の場合
     *  update → relations全削除 → relations再insert
     *  リレーション置換順序の確認
     */
    @Test
    fun updateBookWithAuthors_01() {
        // 準備
        val bookId = 1L
        val req = UpdateBookRequestDto(
            title = "New Title",
            price = 1200,
            publicationStatus = 1,
            authorIds = listOf(10L, 20L),
            authors = null
        )
        val updatedBook = Book(
            bookId = bookId,
            title = req.title,
            price = req.price,
            publicationStatus = req.publicationStatus
        )
        whenever(bookRepository.updateBook(bookId, req.title, req.price, req.publicationStatus))
            .thenReturn(updatedBook)

        // 実行
        sut.updateBookWithAuthors(bookId, req)

        // 検証
        val order = inOrder(bookRepository)
        order.verify(bookRepository).findBookByBookId(bookId)
        order.verify(bookRepository).updateBook(bookId, req.title, req.price, req.publicationStatus)
        order.verify(bookRepository).deleteBookAuthorRelations(bookId)
        order.verify(bookRepository).insertBookAuthorRelation(bookId, 10L)
        order.verify(bookRepository).insertBookAuthorRelation(bookId, 20L)
        order.verifyNoMoreInteractions()
    }

    /**
     *  updateBookWithAuthors_02
     *  新規著者のみ指定の場合
     *  AuthorService.createAuthor呼び出し
     *  生成されたIDでrelations insert
     *  処理順序の確認
     */
    @Test
    fun updateBookWithAuthors_02() {
        // 準備
        val bookId = 1L
        val req = UpdateBookRequestDto(
            title = "New Title",
            price = 1200,
            publicationStatus = 1,
            authorIds = emptyList(),
            authors = listOf(
                CreateAuthorRequestDto(
                    name = "New Author",
                    birthDate = LocalDate.of(1990, 1, 1)
                )
            )
        )
        val updatedBook = Book(
            bookId = bookId,
            title = req.title,
            price = req.price,
            publicationStatus = req.publicationStatus
        )
        whenever(bookRepository.updateBook(bookId, req.title, req.price, req.publicationStatus))
            .thenReturn(updatedBook)

        val birth = LocalDate.of(1990, 1, 1)
        val createdAuthor = Author(
            authorId = 99L,
            name = "New Author",
            birthDate = birth
        )
        whenever(authorService.createAuthor("New Author", birth))
            .thenReturn(createdAuthor)

        // 実行
        sut.updateBookWithAuthors(bookId, req)

        // 検証
        val order = inOrder(bookRepository, authorService)
        order.verify(bookRepository).findBookByBookId(bookId)
        order.verify(bookRepository).updateBook(bookId, req.title, req.price, req.publicationStatus)
        order.verify(bookRepository).deleteBookAuthorRelations(bookId)
        order.verify(authorService).createAuthor("New Author", birth)
        order.verify(bookRepository).insertBookAuthorRelation(bookId, 99L)
        order.verifyNoMoreInteractions()
    }

    /**
     *  updateBookWithAuthors_03
     *  既存著者＋新規著者指定の場合
     *  既存著者ID insert
     *  新規著者作成 → insert
     *  両対応処理の確認
     */
    @Test
    fun updateBookWithAuthors_03() {
        // 準備
        val bookId = 1L
        val birth = LocalDate.of(1990, 1, 1)
        val req = UpdateBookRequestDto(
            title = "New Title",
            price = 1200,
            publicationStatus = 1,
            authorIds = listOf(10L),
            authors = listOf(
                CreateAuthorRequestDto(
                    name = "New Author",
                    birthDate = birth
                )
            )
        )
        val updatedBook = Book(
            bookId = bookId,
            title = req.title,
            price = req.price,
            publicationStatus = req.publicationStatus
        )
        whenever(bookRepository.updateBook(bookId, req.title, req.price, req.publicationStatus))
            .thenReturn(updatedBook)

        val createdAuthor = Author(
            authorId = 99L,
            name = "New Author",
            birthDate = birth
        )
        whenever(authorService.createAuthor("New Author", birth))
            .thenReturn(createdAuthor)

        // 実行
        sut.updateBookWithAuthors(bookId, req)

        // 検証（順序には依存しない。重要な呼び出しのみ検証する）
        verify(bookRepository).findBookByBookId(bookId)
        verify(bookRepository).updateBook(bookId, req.title, req.price, req.publicationStatus)
        verify(bookRepository).deleteBookAuthorRelations(bookId)
        // 既存著者との紐付け
        verify(bookRepository).insertBookAuthorRelation(bookId, 10L)
        // 新規著者作成と紐付け
        verify(authorService).createAuthor("New Author", birth)
        verify(bookRepository).insertBookAuthorRelation(bookId, 99L)
        verifyNoMoreInteractions(bookRepository, authorService)
    }

    /**
     *  updateBookWithAuthors_04
     *  書籍更新時、著者が0人の場合は IllegalArgumentException
     *  著者は最低1人必須であることを確認
     */
    @Test
    fun updateBookWithAuthors_04() {
        // 準備
        val bookId = 1L
        whenever(bookRepository.findBookByBookId(bookId)).thenReturn(
            Book(bookId, "Existing", 1000, 0)
        )
        val req = UpdateBookRequestDto(
            title = "New",
            price = 1000,
            publicationStatus = 0,
            authorIds = emptyList(),
            authors = emptyList()
        )

        // 実行、検証
        assertThrows(IllegalArgumentException::class.java) {
            sut.updateBookWithAuthors(bookId, req)
        }
    }

    /**
     *  updateBookWithAuthors_05
     *  出版済 → 未出版への変更時 IllegalArgumentException
     */
    @Test
    fun updateBookWithAuthors_05() {
        // 準備
        val bookId = 1L
        val UNPUBLISHED = 0
        val PUBLISHED = 1
        whenever(bookRepository.findBookByBookId(bookId)).thenReturn(
            Book(
                bookId = bookId,
                title = "Old",
                price = 1000,
                publicationStatus = PUBLISHED // 出版済み
            )
        )
        val req = UpdateBookRequestDto(
            title = "New",
            price = 1200,
            publicationStatus = UNPUBLISHED, // 未出版に変更
            authorIds = listOf(10L),
            authors = null
        )

        // 実行、検証
        assertThrows(IllegalArgumentException::class.java) {
            sut.updateBookWithAuthors(bookId, req)
        }

        verifyNoMoreInteractions(authorService)
    }

    /**
     *  updateBookWithAuthors_06
     *  価格が0未満の時 IllegalArgumentException
     */
    @Test
    fun updateBookWithAuthors_06() {
        // 準備
        val bookId = 1L
        whenever(bookRepository.findBookByBookId(bookId)).thenReturn(
            Book(bookId = bookId, title = "Existing", price = 1000, publicationStatus = 0)
        )
        val req = UpdateBookRequestDto(
            title = "New",
            price = -1,
            publicationStatus = 0,
            authorIds = listOf(10L),
            authors = null
        )

        // 実行、検証
        assertThrows(IllegalArgumentException::class.java) {
            sut.updateBookWithAuthors(bookId, req)
        }

        verifyNoMoreInteractions(authorService)
    }
}