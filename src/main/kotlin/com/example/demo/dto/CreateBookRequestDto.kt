package com.example.demo.dto

/**
 * 書籍作成リクエスト
 *
 * authorIds: 既存著者を使用
 * authors: 新規著者を同時作成
 * どちらか片方は必須（Serviceでチェック）
 */
data class CreateBookRequestDto(
    val title: String,
    val price: Int,
    val publicationStatus: Int,
    val authorIds: List<Long>? = null,
    val authors: List<CreateAuthorRequestDto>? = null
)
