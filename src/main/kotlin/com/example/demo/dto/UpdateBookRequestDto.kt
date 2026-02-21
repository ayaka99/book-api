package com.example.demo.dto

data class UpdateBookRequestDto(
    val title: String,
    val price: Int,
    val publicationStatus: Int,
    val authorIds: List<Long>?,
    val authors: List<CreateAuthorRequestDto>?
)
