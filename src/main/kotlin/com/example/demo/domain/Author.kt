package com.example.demo.domain

import java.time.LocalDate

data class Author(
    val authorId: Long,
    val name: String,
    val birthDate: LocalDate?
)
