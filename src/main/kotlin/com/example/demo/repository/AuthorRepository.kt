package com.example.demo.repository

import com.example.demo.domain.Author
import java.time.LocalDate

interface AuthorRepository {
    fun create(name: String, birthDate: LocalDate?): Author
    fun findById(authorId: Long): Author?
    fun findAll(limit: Int = 50, offset: Int = 0): List<Author>
    fun update(authorId: Long, name: String, birthDate: LocalDate?): Author
}
