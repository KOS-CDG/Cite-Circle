package com.citecircle.app.core.data

import com.citecircle.app.core.model.AiReviewReport
import com.citecircle.app.core.model.PaperDraft
import com.citecircle.app.core.network.ChatMessage
import com.citecircle.app.core.network.FireworksApi
import com.citecircle.app.core.network.ChatCompletionRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FireworksAiReviewRepository @Inject constructor(
    private val api: FireworksApi,
    private val json: Json
) : AiReviewRepository {

    private val _progress = MutableStateFlow<AiReviewStage>(AiReviewStage.Idle)
    override fun getReviewProgress(): Flow<AiReviewStage> = _progress.asStateFlow()

    override suspend fun reviewPaper(draft: PaperDraft): AiReviewReport {
        try {
            _progress.value = AiReviewStage.InProgress("Reading manuscript draft...", 1, 6)
            delay(500)

            _progress.value = AiReviewStage.InProgress("Analyzing document abstract...", 2, 6)
            delay(500)

            _progress.value = AiReviewStage.InProgress("Contacting Fireworks.ai service...", 3, 6)

            val modelId = "accounts/fireworks/models/gpt-oss-120b"
            val systemPrompt = """
                You are an expert peer reviewer for academic manuscripts. Your job is to critique the provided title and abstract of a research draft.
                Analyze the draft on five key criteria:
                1. Structure (logic flow, completeness)
                2. Citations (relevancy, background depth)
                3. Clarity (readability, scientific rigor)
                4. Originality (novelty, significance)
                
                You must output your complete analysis as a valid JSON object matching the following schema:
                {
                  "score": integer (overall readiness score between 0 and 100),
                  "structure": integer (score between 0 and 100),
                  "citations": integer (score between 0 and 100),
                  "clarity": integer (score between 0 and 100),
                  "originality": integer (score between 0 and 100),
                  "suggestions": [
                    {
                      "id": "s1",
                      "section": "Abstract",
                      "text": "Detailed, specific, actionable feedback text.",
                      "severity": "MODERATE",
                      "isAddressed": false
                    }
                  ]
                }
                Provide between 3 to 6 suggestions. Section must be one of: Abstract, Related Work, Methodology, Results, Discussion, Conclusion.
                Severity must be one of: MINOR, MODERATE, NEEDS_ATTENTION.
                Do not include any explanation, markdown packaging, or text outside the JSON object itself.
            """.trimIndent()

            val userContent = """
                Paper Title: ${draft.title}
                Abstract Summary: ${draft.abstract}
            """.trimIndent()

            val request = ChatCompletionRequest(
                model = modelId,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userContent)
                ),
                temperature = 0.2,
                useJsonMode = true
            )

            val response = api.chatCompletions(request)

            _progress.value = AiReviewStage.InProgress("Receiving critique feedback...", 4, 6)
            delay(500)

            val responseText = response.choices.firstOrNull()?.message?.content
                ?: throw IllegalStateException("Received an empty response from Fireworks.ai")

            _progress.value = AiReviewStage.InProgress("Validating JSON output...", 5, 6)
            delay(500)

            _progress.value = AiReviewStage.InProgress("Parsing suggestions & scores...", 6, 6)
            
            // Extract JSON object safely from response string
            val jsonStart = responseText.indexOf('{')
            val jsonEnd = responseText.lastIndexOf('}')
            val cleanJson = if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                responseText.substring(jsonStart, jsonEnd + 1)
            } else {
                responseText.trim().removeSurrounding("```json", "```").removeSurrounding("```", "```").trim()
            }
            val report = json.decodeFromString<AiReviewReport>(cleanJson)

            _progress.value = AiReviewStage.Complete(report)
            return report
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "An unexpected error occurred during AI review"
            _progress.value = AiReviewStage.Error(errorMessage)
            throw e
        }
    }
}
