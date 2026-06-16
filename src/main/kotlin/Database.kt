package com.example

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import java.io.Closeable
import javax.sql.DataSource

data class DatabaseSettings(
    val jdbcUrl: String,
    val username: String,
    val password: String,
)

fun Application.createNoteRepository(): NoteRepository {
    val settings = DatabaseSettings(
        jdbcUrl = requireEnvironmentVariable("JDBC_DATABASE_URL"),
        username = requireEnvironmentVariable("JDBC_DATABASE_USERNAME"),
        password = requireEnvironmentVariable("JDBC_DATABASE_PASSWORD"),
    )

    val dataSource = createDataSource(settings)
    monitor.subscribe(ApplicationStopped) {
        (dataSource as? Closeable)?.close()
    }

    return DatabaseNoteRepository(dataSource)
}

fun createDataSource(settings: DatabaseSettings): HikariDataSource {
    val config = HikariConfig().apply {
        jdbcUrl = settings.jdbcUrl
        username = settings.username
        password = settings.password
        maximumPoolSize = 10
        minimumIdle = 1
        isAutoCommit = true
    }

    return HikariDataSource(config)
}

fun createDataSource(
    jdbcUrl: String,
    username: String,
    password: String,
): HikariDataSource = createDataSource(
    DatabaseSettings(
        jdbcUrl = jdbcUrl,
        username = username,
        password = password,
    )
)

private fun requireEnvironmentVariable(name: String): String =
    System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: error("Environment variable $name is required")
