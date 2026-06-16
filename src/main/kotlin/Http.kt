package com.example

import io.ktor.http.HttpHeaders
import io.ktor.server.application.*
import io.ktor.serialization.jackson.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*

fun Application.configureHttp() {
    install(DefaultHeaders) {
        header("X-Frame-Options", "DENY")
        header("X-Content-Type-Options", "nosniff")
        header(HttpHeaders.StrictTransportSecurity, "max-age=31536000; includeSubDomains")
        header("Referrer-Policy", "no-referrer")
        header("Permissions-Policy", "camera=(), microphone=(), geolocation=()")
        header("Cross-Origin-Opener-Policy", "same-origin")
        header("Cross-Origin-Resource-Policy", "same-origin")
        header("X-Permitted-Cross-Domain-Policies", "none")
        header(HttpHeaders.CacheControl, "no-store, no-cache, must-revalidate, max-age=0")
        header(HttpHeaders.Pragma, "no-cache")
        header(HttpHeaders.Expires, "0")
        header(
            "Content-Security-Policy",
            "default-src 'self'; script-src 'self' https://esm.sh; style-src 'self'; img-src 'self' data:; " +
                "connect-src 'self'; font-src 'self' data:; object-src 'none'; base-uri 'self'; " +
                "form-action 'self'; frame-ancestors 'none'"
        )
    }

    install(ContentNegotiation) {
        jackson()
    }
}
