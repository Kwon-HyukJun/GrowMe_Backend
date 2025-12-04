package com.example

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json


suspend fun callGpt(book: String, text: String): String
{
    val prompt = """
                "${book}"에 대해 독후감을 써봤는데 숫자로만 평가해줘. 평가 근거를 들어서.
                독후감: 
                 "${text}"
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

    return content
}