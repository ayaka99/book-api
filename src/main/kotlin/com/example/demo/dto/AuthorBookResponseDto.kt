package com.example.demo.dto

data class AuthorBookResponseDto(
    val bookId: Long,
    val title: String,
    val price: Int,
    val publicationStatus: Int
)
