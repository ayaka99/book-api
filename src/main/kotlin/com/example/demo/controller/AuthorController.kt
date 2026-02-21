package com.example.demo.controller

import com.example.demo.dto.AuthorResponseDto
import com.example.demo.dto.AuthorBookResponseDto
import com.example.demo.dto.CreateAuthorRequestDto
import com.example.demo.dto.UpdateAuthorRequestDto
import com.example.demo.error.NotFoundException
import com.example.demo.service.AuthorService
import com.example.demo.service.BookService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/authors")
class AuthorController(
    private val authorService: AuthorService,
    private val bookService: BookService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody requestDto: CreateAuthorRequestDto): AuthorResponseDto {
        val author = authorService.createAuthor(requestDto.name, requestDto.birthDate)
        return AuthorResponseDto(
            authorId = author.authorId,
            name = author.name,
            birthDate = author.birthDate
        )
    }

    @GetMapping("/{authorId}")
    fun get(@PathVariable authorId: Long): AuthorResponseDto {
        val author = authorService.getAuthor(authorId)
            ?: throw NotFoundException("author not found: id=$authorId")

        return AuthorResponseDto(
            authorId = author.authorId,
            name = author.name,
            birthDate = author.birthDate
        )
    }

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "50") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): List<AuthorResponseDto> {
        return authorService.listAuthors(limit, offset)
            .map { AuthorResponseDto(it.authorId, it.name, it.birthDate) }
    }

    @GetMapping("/{authorId}/books")
    fun getBooksByAuthorId(@PathVariable authorId: Long): List<AuthorBookResponseDto> {
        val books = bookService.getBooksByAuthorId(authorId)
        return books.map { book ->
            AuthorBookResponseDto(
                bookId = book.bookId,
                title = book.title,
                price = book.price,
                publicationStatus = book.publicationStatus
            )
        }
    }

    @PutMapping("/{authorId}")
    fun updateAuthor(
        @PathVariable authorId: Long,
        @RequestBody request: UpdateAuthorRequestDto
    ): AuthorResponseDto {

        val updated = authorService.updateAuthor(
            authorId = authorId,
            name = request.name,
            birthDate = request.birthDate
        )

        return AuthorResponseDto(
            authorId = updated.authorId,
            name = updated.name,
            birthDate = updated.birthDate
        )
    }
}
