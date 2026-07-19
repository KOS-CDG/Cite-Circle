package com.citecircle.app.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

// ──────────────────────────────────────────────────────────────────────────────
// Fireworks.ai Chat Completions API
// ──────────────────────────────────────────────────────────────────────────────

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ResponseFormat(
    val type: String
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("response_format") val responseFormat: ResponseFormat = ResponseFormat("json_object"),
    val temperature: Double = 0.2,
    @SerialName("max_tokens") val maxTokens: Int = 2048
)

@Serializable
data class ChatCompletionChoice(
    val message: ChatMessage
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<ChatCompletionChoice> = emptyList()
)

interface FireworksApi {
    @POST("inference/v1/chat/completions")
    suspend fun chatCompletions(@Body request: ChatCompletionRequest): ChatCompletionResponse
}
