package com.example.demo

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class ApiExceptionHandlerIT @Autowired constructor(
    val mockMvc: MockMvc
) {

    /**
     * postAuthors_400
     * POST/authors でbirthDateが未来日の場合に400を返す（IllegalArgumentException → 400変換の確認）
     */
    @Test
    fun postAuthors_400() {
        val json = """
            {
              "name": "Alice",
              "birthDate": "${LocalDate.now().plusDays(1)}"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/authors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
            .andExpect(status().isBadRequest)
    }

    /**
     * putAuthors_404
     * PUT/authors/{id} で存在しない著者IDを指定した場合に404を返す（NotFoundException → 404 変換の確認）
     */
    @Test
    fun putAuthors_404() {
        val json = """
            {
              "name": "Alice",
              "birthDate": null
            }
        """.trimIndent()

        mockMvc.perform(
            put("/authors/999999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
            .andExpect(status().isNotFound)
    }

    /**
     * postBooks_400
     * POST/books で存在しないauthorIdを指定した場合に400を返す（DataIntegrityViolationException → 400 変換の確認）
     */
    @Test
    fun postBooks_400() {
        val json = """
            {
              "title": "Book",
              "price": 1000,
              "publicationStatus": 1,
              "authorIds": [999999],
              "authors": []
            }
        """.trimIndent()

        mockMvc.perform(
            post("/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
            .andExpect(status().isBadRequest)
    }
}