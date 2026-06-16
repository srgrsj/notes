package com.example

import io.ktor.server.application.Application

fun Application.configure(noteRepository: NoteRepository = createNoteRepository()) {
    configureHttp()
    configureRouting(noteRepository)
}
