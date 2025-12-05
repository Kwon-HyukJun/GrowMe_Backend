package com.example


import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import kotlinx.serialization.json.Json
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.feedbackRouter() {
    post("/feedback") {
        val id = call.receiveText()
        println("🔥 받은 sessionId = $id")

        val row = transaction {
            feedbackInfo
                .select { feedbackInfo.id eq id.toInt() }
                .singleOrNull()
        }

        if (row == null) {
            call.respond("해당 ID 없음")
            return@post
        }

        val title = row[feedbackInfo.title]
        val topic = row[feedbackInfo.topic]
        val text = row[feedbackInfo.text]
        val qnaQuery = row[feedbackInfo.qnaQuery]
        var prompt : String

        if (title != null) {
            prompt = """
                너는 발표와 질의응답을 평가해서 피드백을 만들어 주는 AI 코치야.

                아래는 한 학생의 독서 이후(발표 / 독후감) 정보야.

                [책 제목]
                ${title}
                
                [독서 후기 (발표 / 독후감) 내용(원고 또는 요약)]
                ${text}

                [질의응답 내역]
                - 형식: [{"question": "...", "answer": "..."}, ...]
                ${qnaQuery}

                위 내용을 모두 종합해서, 이 학생의 text에 대해 
                다음 두 가지를 JSON 형식으로만 출력해.

                1. score  
                - 0~100점 사이의 정수  
                - 내용의 구성(도입-전개-결론), 논리성, 내용 이해도, 질의응답 대응 등을 종합적으로 평가한 점수

                2. summaries  
                - 문자열 5개로 구성된 배열  
                - 각 원소는 한 문장 내외의 한국어 문장  
                - 말투는 공손한 ~요체  
                - 예시는 “목소리 톤이 안정적이고 전달력이 좋아요.”와 같은 형식  
                - ① 잘한 점 2개, ② 개선하면 좋을 점 3개 정도로 구성

                반드시 아래와 같은 JSON만 출력해.  
                앞뒤에 설명, 주석, 코드 블록, 다른 텍스트는 절대 붙이지 마.

                {
                  "score": 87,
                  "summaries": [
                    "목소리 톤이 안정적이고 전달력이 좋아요.",
                    "발표 구조가 비교적 잘 잡혀 있어요.",
                    "중간에 예시나 사례를 조금 더 넣으면 좋아요."
                    "..."
                    "..."
                  ]
                }

                단, 실제 출력에서는 score 값과 summaries 내용은 위 예시 대신
                네가 평가한 결과로 채워서 내보내.

            """.trimIndent()
        } else {

            prompt = """
                너는 발표와 질의응답을 평가해서 피드백을 만들어 주는 AI 코치야.

                아래는 한 학생의 발표 정보야.

                [발표 주제]
                ${topic}
                
                [발표 내용(원고 또는 요약)]
                ${text}

                [질의응답 내역]
                - 형식: [{"question": "...", "answer": "..."}, ...]
                ${qnaQuery}

                위 내용을 모두 종합해서, 이 학생의 발표에 대해 
                다음 두 가지를 JSON 형식으로만 출력해.

                1. score  
                - 0~100점 사이의 정수  
                - 발표 내용의 구성(도입-전개-결론), 논리성, 전달력(목소리, 속도, 강조), 내용 이해도, 질의응답 대응 등을 종합적으로 평가한 점수

                2. summaries  
                - 문자열 5개로 구성된 배열  
                - 각 원소는 한 문장 내외의 한국어 문장  
                - 말투는 공손한 ~요체  
                - 예시는 “목소리 톤이 안정적이고 전달력이 좋아요.”와 같은 형식  
                - ① 잘한 점 2개, ② 개선하면 좋을 점 3개 정도로 구성

                반드시 아래와 같은 JSON만 출력해.  
                앞뒤에 설명, 주석, 코드 블록, 다른 텍스트는 절대 붙이지 마.

                {
                  "score": 87,
                  "summaries": [
                    "목소리 톤이 안정적이고 전달력이 좋아요.",
                    "발표 구조가 비교적 잘 잡혀 있어요.",
                    "중간에 예시나 사례를 조금 더 넣으면 좋아요."
                    "..."
                    "..."
                  ]
                }

                단, 실제 출력에서는 score 값과 summaries 내용은 위 예시 대신
                네가 평가한 결과로 채워서 내보내.

            """.trimIndent()
        }

        val result = callGpt(prompt)

        call.respondText(result, ContentType.Application.Json)
    }
}
