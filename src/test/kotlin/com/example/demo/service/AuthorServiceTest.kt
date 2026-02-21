package com.example.demo.service

import com.example.demo.error.NotFoundException
import com.example.demo.repository.AuthorRepository
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import java.time.LocalDate

class AuthorServiceTest {

    private val authorRepository = mock<AuthorRepository>()
    private val sut = AuthorService(authorRepository)

    /**
     *  createAuthor_01
     *  著者の生年月日が未来日の場合、IllegalArgumentException
     *  Repositoryは呼ばれないことを確認
     */
    @Test
    fun createAuthor_01() {
        // 準備
        val future = LocalDate.now().plusDays(1)

        // 実行
        assertThrows(IllegalArgumentException::class.java) {
            sut.createAuthor(name = "Alice", birthDate = future)
        }

        // 検証
        verifyNoInteractions(authorRepository)
    }

    /**
     *  updateAuthor_01
     *  著者の生年月日が未来日の場合、IllegalArgumentException
     *  Repositoryは呼ばれないことを確認
     */
    @Test
    fun updateAuthor_01() {
        // 準備
        val future = LocalDate.now().plusDays(1)

        // 実行
        assertThrows(IllegalArgumentException::class.java) {
            sut.updateAuthor(authorId = 1L, name = "Alice", birthDate = future)
        }

        // 検証
        verifyNoInteractions(authorRepository)
    }

    /**
     *  updateAuthor_02
     *  存在しない著者IDを更新した場合、NotFoundException
     *  findByIdが呼ばれ、updateは呼ばれないことを確認
     */
    @Test
    fun updateAuthor_02() {
        // 準備
        whenever(authorRepository.findById(1L)).thenReturn(null)

        // 実行
        assertThrows(NotFoundException::class.java) {
            sut.updateAuthor(authorId = 1L, name = "Alice", birthDate = null)
        }

        // 検証
        verify(authorRepository).findById(1L)
        verify(authorRepository, never()).update(any(), any(), any())
    }
}
