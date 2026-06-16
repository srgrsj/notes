package com.example

import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(noteRepository: NoteRepository) {
    routing {
        route("/api/notes") {
            get {
                call.respond(noteRepository.getAll())
            }

            post {
                val request = call.receive<NoteUpsertRequest>()
                if (request.title.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Заголовок не должен быть пустым"))
                    return@post
                }

                val note = noteRepository.create(request)
                call.respond(HttpStatusCode.Created, note)
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Не указан идентификатор заметки"))
                    return@get
                }

                val note = noteRepository.getById(id) ?: run {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Заметка не найдена"))
                    return@get
                }

                call.respond(note)
            }

            put("/{id}") {
                val id = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Не указан идентификатор заметки"))
                    return@put
                }
                val request = call.receive<NoteUpsertRequest>()
                if (request.title.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Заголовок не должен быть пустым"))
                    return@put
                }

                val note = noteRepository.update(id, request) ?: run {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Заметка не найдена"))
                    return@put
                }

                call.respond(note)
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Не указан идентификатор заметки"))
                    return@delete
                }

                if (!noteRepository.delete(id)) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Заметка не найдена"))
                    return@delete
                }

                call.respond(HttpStatusCode.OK, mapOf("status" to "deleted"))
            }
        }

        get("/") {
            call.respondText(loadStatic("index.html"), ContentType.Text.Html)
        }

        get("/app.js") {
            call.respondText(loadStatic("app.js"), ContentType.Application.JavaScript)
        }

        get("/styles.css") {
            call.respondText(loadStatic("styles.css"), ContentType.Text.CSS)
        }
    }
}

private fun loadStatic(name: String): String {
    val stream = object {}.javaClass.classLoader.getResourceAsStream("static/$name")
        ?: error("Static resource static/$name not found")

    return stream.bufferedReader().use { it.readText() }
}
