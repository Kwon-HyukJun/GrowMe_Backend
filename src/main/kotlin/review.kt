package com.example

import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.http.content.streamProvider
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.ByteArrayInputStream
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.insert

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

private fun addDB(title: String, content: String) : Int {
    return transaction {
        feedbackInfo.insert {
            it[feedbackInfo.title] = title
            it[text] = content
            it[qnaQuery] = mapOf()
        } get feedbackInfo.id
    }
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
                val id = addDB(book, extractedText);
                //reText = callGpt(book!!, extractedText)
                call.respond(id)
                return@post
            }
            call.respond("fail")
        }
    }
}