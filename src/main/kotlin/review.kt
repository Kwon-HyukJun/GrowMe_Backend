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
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import kotlinx.serialization.json.Json
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.http.content.streamProvider
import kotlinx.serialization.internal.throwMissingFieldException
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import kr.dogfoot.hwplib.reader.HWPReader
import java.io.ByteArrayInputStream

fun readHwp(bytes: ByteArray): String
{
    return "mock"
}

fun readPdf(bytes: ByteArray): String
{
    val document = PDDocument.load(ByteArrayInputStream(bytes))
    val stripper = PDFTextStripper()
    stripper.sortByPosition = true
    val text = stripper.getText(document)
    document.close()
    return text
}

fun askGpt(text: String): String
{
    return "good"
}

fun Route.reviewRouter()
{
    route("/review")
    {
        post()
        {
            val multipart = call.receiveMultipart()
            var extractedText = "파일을 찾을 수 없습니다."
            var flag : Boolean = true

            multipart.forEachPart { part ->
                if(part is PartData.FileItem) {
                    val fileName = part.originalFileName ?: ""
                    val bytes = part.streamProvider().readBytes()


                    if(fileName.endsWith(".hwp", ignoreCase = true)) {
                        extractedText = readHwp(bytes)
                    } else if(fileName.endsWith(".pdf", ignoreCase = true)) {
                        extractedText = readPdf(bytes)
                    } else {
                        extractedText = "지원 안함"
                        flag = false
                    }

                    if(flag)
                    {
                        askGpt(extractedText)
                    }
                }
            }

            call.respond(extractedText)
        }
    }
}