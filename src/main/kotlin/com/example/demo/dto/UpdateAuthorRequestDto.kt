package com.example.demo.dto

import java.time.LocalDate

data class UpdateAuthorRequestDto(
    val name: String,
    val birthDate: LocalDate?
)
