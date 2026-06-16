package com.example

import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.*

class ServerTest {
    private fun createTestRepository(): Pair<NoteRepository, HikariDataSource> {
        val dataSource = createDataSource(
            jdbcUrl = "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            username = "sa",
            password = "",
        )

        return DatabaseNoteRepository(dataSource) to dataSource
    }

    @Test
    fun `test root endpoint`() = testApplication {
        val (repository, dataSource) = createTestRepository()
        application {
            configure(repository)
        }

        assertEquals(HttpStatusCode.OK, client.get("/").status)
        dataSource.close()
    }

    @Test
    fun `can list seeded notes`() = testApplication {
        val (repository, dataSource) = createTestRepository()
        application {
            configure(repository)
        }

        val response = client.get("/api/notes")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("План на день"))
        dataSource.close()
    }

    @Test
    fun `can create note`() = testApplication {
        val (repository, dataSource) = createTestRepository()
        application {
            configure(repository)
        }

        val response = client.post("/api/notes") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Тест","content":"Проверка создания"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains(""""title":"Тест""""))
        dataSource.close()
    }

    @Test
    fun `cannot create note without title`() = testApplication {
        val (repository, dataSource) = createTestRepository()
        application {
            configure(repository)
        }

        val response = client.post("/api/notes") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"","content":"Без заголовка"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        dataSource.close()
    }

    @Test
    fun `can update and delete note`() = testApplication {
        val (repository, dataSource) = createTestRepository()
        application {
            configure(repository)
        }

        val createResponse = client.post("/api/notes") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Черновик","content":"Первая версия"}""")
        }
        val createdBody = createResponse.bodyAsText()
        val id = """"id":"([^"]+)"""".toRegex().find(createdBody)?.groupValues?.get(1)
            ?: fail("Expected note id in response: $createdBody")

        val updateResponse = client.put("/api/notes/$id") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Черновик обновлен","content":"Вторая версия"}""")
        }

        assertEquals(HttpStatusCode.OK, updateResponse.status)
        assertTrue(updateResponse.bodyAsText().contains("Черновик обновлен"))

        val deleteResponse = client.delete("/api/notes/$id")

        assertEquals(HttpStatusCode.OK, deleteResponse.status)
        assertTrue(deleteResponse.bodyAsText().contains("deleted"))
        assertEquals(HttpStatusCode.NotFound, client.get("/api/notes/$id").status)
        dataSource.close()
    }
}
