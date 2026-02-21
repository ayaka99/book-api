package com.example.demo.error

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ErrorResponse(
    val message: String
)

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        val msg = when {
            e.message?.contains("birth_date") == true -> "生年月日は今日以前の日付を指定してください"
            else -> "リクエストが不正です"
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(msg))
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(e: NotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse("対象が見つかりません"))
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(e: DataIntegrityViolationException): ResponseEntity<ErrorResponse> {
        // 外部キー違反など = 入力に起因する整合性NGとして400扱い
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse("リクエストが不正です"))
    }
}
