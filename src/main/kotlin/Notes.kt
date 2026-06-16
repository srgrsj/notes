package com.example

import java.sql.Connection
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: String,
    val updatedAt: String,
)

data class NoteUpsertRequest(
    val title: String = "",
    val content: String = "",
)

data class ErrorResponse(
    val message: String,
)

interface NoteRepository {
    fun getAll(): List<Note>
    fun getById(id: String): Note?
    fun create(request: NoteUpsertRequest): Note
    fun update(id: String, request: NoteUpsertRequest): Note?
    fun delete(id: String): Boolean
}

class DatabaseNoteRepository(
    private val dataSource: DataSource,
) : NoteRepository {
    init {
        initializeSchema()
        seedIfEmpty()
    }

    override fun getAll(): List<Note> = withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT id, title, content, created_at, updated_at
            FROM notes
            ORDER BY updated_at DESC
            """.trimIndent()
        ).use { statement ->
            statement.executeQuery().use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(resultSet.toNote())
                    }
                }
            }
        }
    }

    override fun getById(id: String): Note? = withConnection { connection ->
        connection.prepareStatement(
            """
            SELECT id, title, content, created_at, updated_at
            FROM notes
            WHERE id = ?
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, id)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) resultSet.toNote() else null
            }
        }
    }

    override fun create(request: NoteUpsertRequest): Note {
        val timestamp = OffsetDateTime.now(ZoneOffset.UTC)
        val note = Note(
            id = UUID.randomUUID().toString(),
            title = request.title.trim(),
            content = request.content.trim(),
            createdAt = timestamp.toString(),
            updatedAt = timestamp.toString(),
        )

        withConnection { connection ->
            connection.prepareStatement(
                """
                INSERT INTO notes (id, title, content, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, note.id)
                statement.setString(2, note.title)
                statement.setString(3, note.content)
                statement.setObject(4, timestamp)
                statement.setObject(5, timestamp)
                statement.executeUpdate()
            }
        }

        return note
    }

    override fun update(id: String, request: NoteUpsertRequest): Note? {
        val updatedAt = OffsetDateTime.now(ZoneOffset.UTC)
        val changed = withConnection { connection ->
            connection.prepareStatement(
                """
                UPDATE notes
                SET title = ?, content = ?, updated_at = ?
                WHERE id = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, request.title.trim())
                statement.setString(2, request.content.trim())
                statement.setObject(3, updatedAt)
                statement.setString(4, id)
                statement.executeUpdate()
            }
        }

        return if (changed > 0) getById(id) else null
    }

    override fun delete(id: String): Boolean = withConnection { connection ->
        connection.prepareStatement(
            """
            DELETE FROM notes
            WHERE id = ?
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, id)
            statement.executeUpdate() > 0
        }
    }

    private fun initializeSchema() {
        withConnection { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS notes (
                        id VARCHAR(36) PRIMARY KEY,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                        updated_at TIMESTAMP WITH TIME ZONE NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }

    private fun seedIfEmpty() {
        val notesCount = withConnection { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT COUNT(*) FROM notes").use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        }

        if (notesCount > 0) {
            return
        }

        create(
            NoteUpsertRequest(
                title = "План на день",
                content = "Собрать простую апишку, подключить React и проверить CRUD."
            )
        )
        create(
            NoteUpsertRequest(
                title = "Идея заметки",
                content = "Добавить поиск и сортировку по дате обновления прямо на клиенте."
            )
        )
    }

    private fun <T> withConnection(block: (Connection) -> T): T = dataSource.connection.use(block)
}

private fun java.sql.ResultSet.toNote(): Note {
    val createdAt = getObject("created_at", OffsetDateTime::class.java)
    val updatedAt = getObject("updated_at", OffsetDateTime::class.java)

    return Note(
        id = getString("id"),
        title = getString("title"),
        content = getString("content"),
        createdAt = createdAt.toInstant().toString(),
        updatedAt = updatedAt.toInstant().toString(),
    )
}
