package com.example.demo.dto

import java.time.LocalDate

data class CreateAuthorRequestDto(
    val name: String,
    val birthDate: LocalDate?
)
