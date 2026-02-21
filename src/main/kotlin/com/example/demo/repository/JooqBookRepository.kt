package com.example.demo.repository

import com.example.demo.domain.Book
import com.example.demo.error.NotFoundException
import com.example.jooq.Tables.BOOKS
import com.example.jooq.Tables.BOOK_AUTHOR_RELATIONS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository


@Repository
class JooqBookRepository(
    private val dsl: DSLContext
) : BookRepository {

    override fun insertBook(title: String, price: Int, publicationStatus: Int): Book {
        val record = dsl.insertInto(BOOKS)
            .set(BOOKS.TITLE, title)
            .set(BOOKS.PRICE, price)
            .set(BOOKS.PUBLICATION_STATUS, publicationStatus)
            .returning(BOOKS.BOOK_ID, BOOKS.TITLE, BOOKS.PRICE, BOOKS.PUBLICATION_STATUS)
            .fetchOne() ?: error("Insert failed: books")

        return Book(
            bookId = record.get(BOOKS.BOOK_ID)!!,
            title = record.get(BOOKS.TITLE)!!,
            price = record.get(BOOKS.PRICE)!!,
            publicationStatus = record.get(BOOKS.PUBLICATION_STATUS)!!
        )
    }

    override fun insertBookAuthorRelation(bookId: Long, authorId: Long) {
        dsl.insertInto(BOOK_AUTHOR_RELATIONS)
            .set(BOOK_AUTHOR_RELATIONS.BOOK_ID, bookId)
            .set(BOOK_AUTHOR_RELATIONS.AUTHOR_ID, authorId)
            .execute()
    }

    override fun findBookByBookId(bookId: Long): Book? {
        return dsl.select(
            BOOKS.BOOK_ID,
            BOOKS.TITLE,
            BOOKS.PRICE,
            BOOKS.PUBLICATION_STATUS
        )
            .from(BOOKS)
            .where(BOOKS.BOOK_ID.eq(bookId))
            .fetchOne { r ->
                Book(
                    bookId = r.get(BOOKS.BOOK_ID)!!,
                    title = r.get(BOOKS.TITLE)!!,
                    price = r.get(BOOKS.PRICE)!!,
                    publicationStatus = r.get(BOOKS.PUBLICATION_STATUS)!!
                )
            }
    }

    override fun findBooksByAuthorId(authorId: Long): List<Book> {
        val bookRecords = dsl
            .select(
                BOOKS.BOOK_ID,
                BOOKS.TITLE,
                BOOKS.PRICE,
                BOOKS.PUBLICATION_STATUS
            )
            .from(BOOKS)
            .join(BOOK_AUTHOR_RELATIONS)
            .on(BOOKS.BOOK_ID.eq(BOOK_AUTHOR_RELATIONS.BOOK_ID))
            .where(BOOK_AUTHOR_RELATIONS.AUTHOR_ID.eq(authorId))
            .orderBy(BOOKS.BOOK_ID.asc())
            .fetch()

        return bookRecords.map { record ->
            Book(
                bookId = record.get(BOOKS.BOOK_ID)!!,
                title = record.get(BOOKS.TITLE)!!,
                price = record.get(BOOKS.PRICE)!!,
                publicationStatus = record.get(BOOKS.PUBLICATION_STATUS)!!
            )
        }
    }

    override fun updateBook(bookId: Long, title: String, price: Int, publicationStatus: Int): Book {
        val record = dsl.update(BOOKS)
            .set(BOOKS.TITLE, title)
            .set(BOOKS.PRICE, price)
            .set(BOOKS.PUBLICATION_STATUS, publicationStatus)
            .where(BOOKS.BOOK_ID.eq(bookId))
            .returning(BOOKS.BOOK_ID, BOOKS.TITLE, BOOKS.PRICE, BOOKS.PUBLICATION_STATUS)
            .fetchOne()
            ?: throw NotFoundException("book not found")

        return Book(
            bookId = record.get(BOOKS.BOOK_ID)!!,
            title = record.get(BOOKS.TITLE)!!,
            price = record.get(BOOKS.PRICE)!!,
            publicationStatus = record.get(BOOKS.PUBLICATION_STATUS)!!
        )
    }

    override fun deleteBookAuthorRelations(bookId: Long) {
        dsl.deleteFrom(BOOK_AUTHOR_RELATIONS)
            .where(BOOK_AUTHOR_RELATIONS.BOOK_ID.eq(bookId))
            .execute()
    }
}
