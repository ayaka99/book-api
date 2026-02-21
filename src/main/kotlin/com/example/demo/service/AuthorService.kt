package com.example.demo.service

import com.example.demo.domain.Author
import com.example.demo.error.NotFoundException
import com.example.demo.repository.AuthorRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 著者の登録・取得を扱うService。
 */
@Service
class AuthorService(
    private val authorRepository: AuthorRepository
) {
    @Transactional
    fun createAuthor(name: String, birthDate: LocalDate?): Author {
        if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
            throw IllegalArgumentException("birth_date must be today or earlier")
        }
        return authorRepository.create(name, birthDate)
    }

    @Transactional(readOnly = true)
    fun getAuthor(authorId: Long): Author? {
        return authorRepository.findById(authorId)
    }

    @Transactional(readOnly = true)
    fun listAuthors(limit: Int = 50, offset: Int = 0): List<Author> {
        return authorRepository.findAll(limit, offset)
    }

    fun updateAuthor(authorId: Long, name: String, birthDate: LocalDate?): Author {
        if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
            throw IllegalArgumentException("birth_date must be today or earlier")
        }

        authorRepository.findById(authorId)
            ?: throw NotFoundException("author not found: $authorId")

        return authorRepository.update(authorId, name, birthDate)
    }

}
