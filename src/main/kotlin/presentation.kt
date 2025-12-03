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
import io.ktor.server.request.receiveMultipart
import io.ktor.http.content.forEachPart
import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import io.ktor.http.HttpStatusCode
import java.io.File

private const val WHISPER_PATH =
    "C:\\Users\\ablem\\whisper.cpp\\build\\bin\\Release\\whisper-cli.exe"

private const val MODEL_PATH =
    "C:\\Users\\ablem\\whisper.cpp\\models\\ggml-small.bin"


fun runWhisper(audioFile: File): String {

    val outputTxt = File(audioFile.absolutePath + ".txt")

    val process = ProcessBuilder(
        WHISPER_PATH,
        "-m", MODEL_PATH,
        "-f", audioFile.absolutePath,
        "-l", "ko",          // 언어 강제: 한국어
        "-otxt"
    )
        .redirectErrorStream(true)
        .directory(File("C:\\Users\\ablem\\whisper.cpp\\build\\bin\\Release"))
        .start()

    val whisperOutput = process.inputStream.bufferedReader().readText()
    val exitCode = process.waitFor()

    if (exitCode != 0) {
        throw RuntimeException("Whisper 실행 오류: $whisperOutput")
    }

    // txt 파일 읽기
    val resultText = outputTxt.readText()

    // 불필요 파일 삭제
    outputTxt.delete()

    return resultText
}

private suspend fun callGpt(topic: String, text: String): String
{
    val prompt = """
                "${topic}"에 대해 발표 대본을 써봤는데 숫자로만 평가해줘. 평가 근거를 들어서.
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

fun Route.presentationRouter()
{
    route("/presentation")
    {
        post()
        {
            val multipart = call.receiveMultipart()
            var audioFile: File? = null
            var topic: String? = null

            multipart.forEachPart { part ->
                when (part) {

                    is PartData.FormItem -> {
                        if (part.name == "topic") {
                            topic = part.value
                        }
                    }

                    is PartData.FileItem -> {
                        val ext = File(part.originalFileName!!).extension
                        val tempFile = File.createTempFile("upload_", ".$ext")

                        tempFile.outputStream().use { output ->
                            part.streamProvider().use { input ->
                                input.copyTo(output)
                            }
                        }
                        audioFile = tempFile
                    }

                    else -> {}
                }
                part.dispose()
            }

            if (audioFile == null) {
                call.respond(HttpStatusCode.BadRequest, "녹음 파일이 없습니다.")
                return@post
            }
            if (topic == null) {
                call.respond(HttpStatusCode.BadRequest, "주제 없습니다.")
                return@post
            }

            try {
                val result = runWhisper(audioFile!!)
                audioFile!!.delete()

                val reComent = callGpt(topic!!, result)

                call.respond(reComent)

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "STT 오류: ${e.message}"
                )
            }
        }
    }
}