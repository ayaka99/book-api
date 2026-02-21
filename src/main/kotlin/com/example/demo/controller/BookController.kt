package com.example.demo.controller

import com.example.demo.dto.CreateBookRequestDto
import com.example.demo.dto.UpdateBookRequestDto
import com.example.demo.service.BookService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/books")
class BookController(
    private val bookService: BookService
) {
    @PostMapping
    fun createBook(@RequestBody createBookRequestDto: CreateBookRequestDto): ResponseEntity<Long> {
        val createdBookId: Long = bookService.createBook(createBookRequestDto)
        return ResponseEntity.ok(createdBookId)
    }

    @PutMapping("/{bookId}")
    fun updateBook(
        @PathVariable bookId: Long,
        @RequestBody updateBookRequestDto: UpdateBookRequestDto
    ): Long {

        val updated = bookService.updateBookWithAuthors(
            bookId = bookId,
            updateBookRequestDto = updateBookRequestDto
        )

        return updated.bookId
    }
}
