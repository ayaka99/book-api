package com.example.demo.service

import com.example.demo.domain.Author
import com.example.demo.error.NotFoundException
import com.example.demo.repository.AuthorRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 著者の登録・更新に関する業務ロジックを提供するサービス。
 *
 * 業務ルール:
 * - birthDate は未来日を許可しない（不正入力は 400 相当）
 * - 存在しない著者IDの更新は 404 相当
 */
@Service
class AuthorService(
    private val authorRepository: AuthorRepository
) {
    /**
     * 著者を新規作成する。
     *
     * @throws IllegalArgumentException birthDateが未来日の場合
     */
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

    /**
     * 指定IDの著者を更新する。
     *
     * トランザクション内で更新を行い、途中失敗時はロールバックされる。
     *
     * @throws NotFoundException 指定IDの著者が存在しない場合
     * @throws IllegalArgumentException birthDate が未来日の場合
     */
    @Transactional
    fun updateAuthor(authorId: Long, name: String, birthDate: LocalDate?): Author {
        if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
            throw IllegalArgumentException("birth_date must be today or earlier")
        }

        authorRepository.findById(authorId)
            ?: throw NotFoundException("author not found: $authorId")

        return authorRepository.update(authorId, name, birthDate)
    }

}
