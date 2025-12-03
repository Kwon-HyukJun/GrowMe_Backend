package com.example

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
data class LLMRequest(val model: String, val messages: List<ChatMessage>)

@Serializable
data class BookInfo(val title: String, val author: String)

@Serializable
data class TopicInfo(val topic: String)

@Serializable
data class ChatCompletionResponse(
    val choices: List<ChatChoice>
)

@Serializable
data class ChatChoice(
    val message: ChatMessageContent
)

@Serializable
data class ChatMessageContent(
    val role: String,
    val content: String
)