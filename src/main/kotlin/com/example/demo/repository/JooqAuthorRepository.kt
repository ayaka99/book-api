package com.example.demo.repository

import com.example.demo.domain.Author
import com.example.demo.error.NotFoundException
import com.example.jooq.Tables.AUTHORS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class JooqAuthorRepository(
    private val dsl: DSLContext
) : AuthorRepository {

    override fun create(name: String, birthDate: LocalDate?): Author {
        val record = dsl.insertInto(AUTHORS)
            .set(AUTHORS.NAME, name)
            .set(AUTHORS.BIRTH_DATE, birthDate)
            .returning(AUTHORS.AUTHOR_ID, AUTHORS.NAME, AUTHORS.BIRTH_DATE)
            .fetchOne() ?: error("Insert failed: authors")

        return Author(
            authorId = record.get(AUTHORS.AUTHOR_ID)!!,
            name = record.get(AUTHORS.NAME)!!,
            birthDate = record.get(AUTHORS.BIRTH_DATE)
        )
    }

    override fun findById(authorId: Long): Author? {
        val record = dsl.select(AUTHORS.AUTHOR_ID, AUTHORS.NAME, AUTHORS.BIRTH_DATE)
            .from(AUTHORS)
            .where(AUTHORS.AUTHOR_ID.eq(authorId))
            .fetchOne() ?: return null

        return Author(
            authorId = record.get(AUTHORS.AUTHOR_ID)!!,
            name = record.get(AUTHORS.NAME)!!,
            birthDate = record.get(AUTHORS.BIRTH_DATE)
        )
    }

    override fun findAll(limit: Int, offset: Int): List<Author> {
        return dsl.select(AUTHORS.AUTHOR_ID, AUTHORS.NAME, AUTHORS.BIRTH_DATE)
            .from(AUTHORS)
            .orderBy(AUTHORS.AUTHOR_ID.asc())
            .limit(limit)
            .offset(offset)
            .fetch { record ->
                Author(
                    authorId = record.get(AUTHORS.AUTHOR_ID)!!,
                    name = record.get(AUTHORS.NAME)!!,
                    birthDate = record.get(AUTHORS.BIRTH_DATE)
                )
            }
    }

    override fun update(authorId: Long, name: String, birthDate: LocalDate?): Author {
        val record = dsl.update(AUTHORS)
            .set(AUTHORS.NAME, name)
            .set(AUTHORS.BIRTH_DATE, birthDate)
            .where(AUTHORS.AUTHOR_ID.eq(authorId))
            .returning(AUTHORS.AUTHOR_ID, AUTHORS.NAME, AUTHORS.BIRTH_DATE)
            .fetchOne()
            ?: throw NotFoundException("author not found")

        return Author(
            authorId = record.get(AUTHORS.AUTHOR_ID)!!,
            name = record.get(AUTHORS.NAME)!!,
            birthDate = record.get(AUTHORS.BIRTH_DATE)
        )
    }

}
