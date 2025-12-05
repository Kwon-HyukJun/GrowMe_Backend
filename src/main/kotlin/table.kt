package com.example

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.json.jsonb
import kotlinx.serialization.json.Json

object feedbackInfo : Table("feedback_info") {
    val id = integer("id").autoIncrement()
    override val primaryKey = PrimaryKey(id)

    val title = text("title").nullable()
    val topic = text("topic").nullable()
    val text = text("text")
    val qnaQuery = jsonb<List<Map<String, String>>>("qna_query", Json)
}