package com.example

import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.Route
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.select

@kotlinx.serialization.Serializable
data class ThoughtMessage(
    val id: String,
    val question: String,
    val answer: String
)

fun Route.thoughtRouter() {
    post("/thought/id") {
        try {
            val request = call.receiveText()   // 예: "12"
            val id = request.toInt()           // Int 변환

            val row = transaction {
                feedbackInfo
                    .select { feedbackInfo.id eq id }
                    .singleOrNull()
            }

            if (row == null) {
                call.respond("해당 ID 없음")
                return@post
            }

            val title = row[feedbackInfo.title]
            val topic = row[feedbackInfo.topic]
            val text = row[feedbackInfo.text]

            var question : String

            if(title != null)
            {
                val prompt = """
                    ${title}을 읽고 난 후 다음과 같은 주장문을 썼는데 심층적인 질문 한 개만 사족없이 출력해줘.
                    주장문 : ${text}
                    """
                question = callGpt(prompt)
            }
            else {
                val prompt = """
                    ${topic} 주제로 다음과 같은 주장문을 썼는데 심층적인 질문 한 개만 사족없이 출력해줘.
                    주장문 : ${text}
                    """
                question = callGpt(prompt)
            }

            call.respond(question)

        } catch (e: Exception) {
            println("❌ Error: ${e.message}")
            call.respond("실패")
        }
    }


    post("/thought/message") {
        val body = call.receive<ThoughtMessage>()

        try {
            transaction {

                // 기존 데이터 있는지 조회
                val existing = feedbackInfo
                    .select { feedbackInfo.id eq body.id.toInt() }
                    .singleOrNull()

                // 기존 json 리스트 읽기 (없으면 빈 리스트)
                val oldList: List<Map<String, String>> =
                    existing?.get(feedbackInfo.qnaQuery) ?: emptyList()

                // 새 항목 생성
                val newItem = mapOf(body.question to body.answer)

                // append
                val updatedList = oldList + newItem   // 👈 리스트에 추가

                // DB 업데이트
                feedbackInfo.update({ feedbackInfo.id eq body.id.toInt() }) {
                    it[qnaQuery] = updatedList
                }
            }

            call.respond("DB 저장 완료!")

        } catch (e: Exception) {
            println("❌ Error: ${e.message}")
            call.respond("DB 저장 실패")
        }
    }
}
