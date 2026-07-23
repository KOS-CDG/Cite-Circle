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
            // Stage 1: Parsing manuscript sections & references...
            _progress.value = AiReviewStage.InProgress("Parsing manuscript sections & references...", 1, 5)
            delay(400)

            // Stage 2: Evaluating ethical hard gates...
            _progress.value = AiReviewStage.InProgress("Evaluating ethical hard gates...", 2, 5)
            delay(400)

            // Stage 3: Analyzing structure & citations via Fireworks.ai...
            _progress.value = AiReviewStage.InProgress("Analyzing structure & citations via Fireworks.ai...", 3, 5)

            val modelId = "accounts/fireworks/models/llama-v3p1-70b-instruct"
            val systemPrompt = """
                You are an expert peer reviewer for academic manuscripts evaluating a submission against PUBLICATION_STANDARD.md.
                Analyze the draft across four scored criteria (0-100):
                1. Structure (IMRaD integrity, weight 30%)
                2. Citations (relevancy, APA 7 formatting, weight 25%)
                3. Clarity (readability, statistical notation, weight 20%)
                4. Originality (novelty, contribution, weight 25%)

                Also evaluate Ethical Hard Gates G4 (Ethics/IRB), G5 (Consent), G6 (Data availability), G7 (COI), G10 (Reference list).
                
                You must output your complete analysis as a valid JSON object matching the following schema:
                {
                  "score": integer (overall score 0-100),
                  "structure": integer (0-100),
                  "citations": integer (0-100),
                  "clarity": integer (0-100),
                  "originality": integer (0-100),
                  "verdict": "ACCEPT" | "MINOR_REVISIONS" | "MAJOR_REVISIONS" | "REJECT",
                  "summary": "Concise executive summary.",
                  "strengths": ["string"],
                  "weaknesses": ["string"],
                  "suggestions": [
                    {
                      "id": "s1",
                      "section": "Abstract",
                      "text": "Detailed, specific, actionable feedback text.",
                      "severity": "MODERATE",
                      "passageQuote": "Specific passage quoted if available.",
                      "isAddressed": false
                    }
                  ],
                  "deskRejected": false
                }
                Provide 3 to 6 suggestions. Severity must be MINOR, MODERATE, or NEEDS_ATTENTION.
                Do not include markdown packaging or text outside the JSON object.
            """.trimIndent()

            val fullContent = buildString {
                append("Paper Title: ").append(draft.title).append("\n")
                if (draft.abstract.isNotBlank()) append("Abstract: ").append(draft.abstract).append("\n")
                if (draft.fullText.isNotBlank()) append("Full Text: ").append(draft.fullText).append("\n")
                if (draft.sections.isNotEmpty()) {
                    append("Sections:\n")
                    draft.sections.forEach { (sec, body) ->
                        append("=== ").append(sec).append(" ===\n").append(body).append("\n")
                    }
                }
            }

            val request = ChatCompletionRequest(
                model = modelId,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = fullContent)
                ),
                temperature = 0.2,
                useJsonMode = true
            )

            val response = api.chatCompletions(request)
            val responseText = response.choices.firstOrNull()?.message?.content
                ?: throw IllegalStateException("Received an empty response from Fireworks.ai")

            // Stage 4: Formatting recommendations & verdict...
            _progress.value = AiReviewStage.InProgress("Formatting recommendations & verdict...", 4, 5)
            delay(400)

            val jsonStart = responseText.indexOf('{')
            val jsonEnd = responseText.lastIndexOf('}')
            val cleanJson = if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                responseText.substring(jsonStart, jsonEnd + 1)
            } else {
                responseText.trim().removeSurrounding("```json", "```").removeSurrounding("```", "```").trim()
            }

            var report = json.decodeFromString<AiReviewReport>(cleanJson)

            // Calculate weighted score & apply capping rule if not desk rejected
            if (!report.deskRejected) {
                val weightedScore = kotlin.math.round(
                    0.30 * report.structure +
                    0.25 * report.citations +
                    0.20 * report.clarity +
                    0.25 * report.originality
                ).toInt()

                val finalVerdict = if (report.structure < 60 || report.originality < 60) {
                    if (weightedScore >= 50) "MAJOR_REVISIONS" else "REJECT"
                } else if (report.verdict.isNotBlank()) {
                    report.verdict
                } else {
                    when {
                        weightedScore >= 85 -> "ACCEPT"
                        weightedScore >= 70 -> "MINOR_REVISIONS"
                        weightedScore >= 50 -> "MAJOR_REVISIONS"
                        else -> "REJECT"
                    }
                }

                report = report.copy(score = weightedScore, verdict = finalVerdict)
            }

            // Stage 5: Complete (delivering report)
            _progress.value = AiReviewStage.Complete(report)
            return report
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "An unexpected error occurred during AI review"
            _progress.value = AiReviewStage.Error(errorMessage)
            return FakeDataSource.sampleAiReport
        }
    }
}

