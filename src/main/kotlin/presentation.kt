package com.example

import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.request.receiveMultipart
import io.ktor.http.content.forEachPart
import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import io.ktor.http.HttpStatusCode
import java.io.File
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.insert

private const val FFMPEG_PATH =
    "C:\\ffmpeg-7.1.1-essentials_build\\bin\\ffmpeg.exe"

private const val WHISPER_PATH =
    "C:\\Users\\ablem\\whisper.cpp\\build\\bin\\Release\\whisper-cli.exe"

private const val MODEL_PATH =
    "C:\\Users\\ablem\\whisper.cpp\\models\\ggml-small.bin"


private fun addDB(topic : String, content: String)
{
    transaction {
        feedbackInfo.insert {
            it[feedbackInfo.topic] = topic
            it[text] = content
            it[qnaQuery] = mapOf()
        }
    }
}

fun convertWebmToWav(input: File): File {
    val output = File(input.absolutePath + ".wav")

    val process = ProcessBuilder(
        FFMPEG_PATH,
        "-y",
        "-i", input.absolutePath,
        "-ac", "1",
        "-ar", "16000",
        output.absolutePath
    ).start()

    process.waitFor()

    return output
}

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

fun Route.presentationRouter()
{
    route("/presentation")
    {
        post()
        {
            val multipart = call.receiveMultipart()
            var audioFile: File? = null
            var topic: String? = null
            var flag: Boolean = false

            multipart.forEachPart { part ->
                when (part) {

                    is PartData.FormItem -> {
                        when (part.name) {
                            "topic" -> topic = part.value
                            "isFromBookPage" -> flag = part.value.toBoolean()
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
                val wavFile = convertWebmToWav(audioFile!!)
                val result = runWhisper(wavFile)
                audioFile!!.delete()
                wavFile.delete()

                addDB(topic, result)

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