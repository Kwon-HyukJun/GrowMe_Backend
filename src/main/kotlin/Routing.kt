package com.example

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.*

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
}

fun Application.configureRouting() {

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Post)
    }

    routing {
        // "/" 경로에 대한 GET 요청을 처리
        get("/") {
            call.respondText("Hello World 123")
        }
        bookRouter()
        topicRouter()
        reviewRouter()
        presentationRouter()
        thoughtRouter()
    }
}
