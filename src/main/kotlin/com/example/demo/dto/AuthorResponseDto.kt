package com.example.demo.dto

import java.time.LocalDate

data class AuthorResponseDto(
    val authorId: Long,
    val name: String,
    val birthDate: LocalDate?
)
