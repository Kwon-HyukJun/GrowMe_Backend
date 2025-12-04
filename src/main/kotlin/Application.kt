package com.example

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.statuspages.StatusPages
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase() {
    Database.connect(
        url = "jdbc:postgresql://localhost:5432/growme",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "1234"
    )

    transaction {
        SchemaUtils.create(feedbackInfo) // 테이블 자동 생성
    }
}


fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) {
        json() // JSON 직렬화 설정
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                cause.localizedMessage ?: "Internal Server Error"
            )
        }
    }

    configureRouting()
    configureDatabase()
}
