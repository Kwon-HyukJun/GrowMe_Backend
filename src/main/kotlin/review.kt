package com.example

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.http.content.streamProvider
import kotlinx.serialization.json.Json
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.ByteArrayInputStream

private fun readHwp(bytes: ByteArray): String
{
    return "mock"
}

private fun readPdf(bytes: ByteArray): String
{
    val document = PDDocument.load(ByteArrayInputStream(bytes))
    val stripper = PDFTextStripper()
    stripper.sortByPosition = true
    val text = stripper.getText(document)
    document.close()
    return text
}

private suspend fun callGpt(book: String, text: String): String
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

fun Route.reviewRouter()
{
    route("/review")
    {
        post()
        {
            val multipart = call.receiveMultipart()
            var extractedText = "파일을 찾을 수 없습니다."
            var book : String? = null
            var flag = true
            var fileBytes: ByteArray? = null
            var fileName: String? = null
            var reText = "지원하지 않는 형식"

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        if (part.name == "book") {
                            book = part.value
                        }
                    }

                    is PartData.FileItem -> {
                        fileName = part.originalFileName
                        fileBytes = part.streamProvider().readBytes()
                    }

                    else -> {}
                }
                part.dispose()
            }

            if(fileBytes == null || fileName == null || book == null) {
                call.respond("파일 받기 실패")
                return@post
            }

            if(fileName!!.endsWith(".hwp", ignoreCase = true)) {
                extractedText = readHwp(fileBytes!!)
            } else if(fileName!!.endsWith(".pdf", ignoreCase = true)) {
                extractedText = readPdf(fileBytes!!)
            } else {
                extractedText = "지원 안함"
                flag = false
            }

            if(flag)
            {
                reText = callGpt(book!!, extractedText)
            }

            call.respond(reText)
        }
    }
}