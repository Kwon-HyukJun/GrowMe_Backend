package com.example

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import kotlinx.serialization.json.Json
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

fun Route.bookRouter()
{
    route("/book")
    {
        post {
            val book = call.receive<String>()

            val prompt = """
                "${book}"과 관련된 도서 5권을 추천해줘.
                반드시 아래 형식의 JSON 배열로만 답해.

                [
                  {"title": "도서명1", "author": "저자1"},
                  {"title": "도서명2", "author": "저자2"},
                  ...
                ]
                설명은 절대 쓰지 마.
            """.trimIndent()

            val requestBody = LLMRequest(
                model = "gpt-4o-mini",
                messages = listOf(
                    ChatMessage("user", prompt)
                )
            )

            val llmResponse: HttpResponse = client.post("https://api.openai.com/v1/chat/completions") {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer ${System.getenv("OPENAI_API_KEY")}")

                setBody(requestBody)
            }

            val resultText = llmResponse.bodyAsText()

            val json = Json {
                ignoreUnknownKeys = true
            }

            val parsed = json.decodeFromString<ChatCompletionResponse>(resultText)

            val content = parsed.choices.first().message.content

            val books = json.decodeFromString<List<BookInfo>>(content)

            call.respond(books)
        }
    }
}