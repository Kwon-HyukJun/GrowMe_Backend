package com.example

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json


suspend fun callGpt(prompt: String): String
{
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