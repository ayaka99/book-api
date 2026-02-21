package com.example.demo.repository

import com.example.demo.domain.Book

interface BookRepository {

    /**
     * 書籍を登録し、生成されたBookを返す
     */
    fun insertBook(
        title: String,
        price: Int,
        publicationStatus: Int
    ): Book

    /**
     * 書籍と著者を紐付ける
     */
    fun insertBookAuthorRelation(
        bookId: Long,
        authorId: Long
    )

    fun findBookByBookId(bookId: Long): Book?

    fun findBooksByAuthorId(authorId: Long): List<Book>

    fun updateBook(bookId: Long, title: String, price: Int, publicationStatus: Int): Book

    fun deleteBookAuthorRelations(bookId: Long)
}
