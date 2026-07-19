package com.citecircle.app.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
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

/**
 * Chat completion request for Fireworks.ai.
 *
 * @param useJsonMode When true, sends response_format=json_object so the model returns
 *                    structured JSON (used by the AI paper reviewer).
 *                    When false (default for chat), the field is omitted so the model
 *                    returns plain, readable prose.
 */
@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("response_format") val responseFormat: ResponseFormat? = null,
    val temperature: Double = 0.2,
    @SerialName("max_tokens") val maxTokens: Int = 2048,
    @Transient val useJsonMode: Boolean = false
) {
    // Provide a factory-style companion so callers can set useJsonMode cleanly
    companion object {
        operator fun invoke(
            model: String,
            messages: List<ChatMessage>,
            temperature: Double = 0.2,
            maxTokens: Int = 2048,
            useJsonMode: Boolean = false
        ) = ChatCompletionRequest(
            model = model,
            messages = messages,
            responseFormat = if (useJsonMode) ResponseFormat("json_object") else null,
            temperature = temperature,
            maxTokens = maxTokens,
            useJsonMode = useJsonMode
        )
    }
}

@Serializable
data class ChatCompletionChoice(
    val message: ChatMessage
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<ChatCompletionChoice> = emptyList()
)

interface FireworksApi {
    @POST("chat/completions")
    suspend fun chatCompletions(@Body request: ChatCompletionRequest): ChatCompletionResponse
}
