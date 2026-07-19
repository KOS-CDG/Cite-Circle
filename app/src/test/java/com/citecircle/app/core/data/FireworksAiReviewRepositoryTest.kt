package com.citecircle.app.core.data

import com.citecircle.app.core.model.AiReviewReport
import com.citecircle.app.core.model.PaperDraft
import com.citecircle.app.core.network.ChatCompletionChoice
import com.citecircle.app.core.network.ChatCompletionResponse
import com.citecircle.app.core.network.ChatMessage
import com.citecircle.app.core.network.FireworksApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FireworksAiReviewRepositoryTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Test
    fun testSerializationOfAiReviewReport() {
        val sampleJson = """
            {
              "score": 85,
              "structure": 90,
              "citations": 75,
              "clarity": 88,
              "originality": 82,
              "suggestions": [
                {
                  "id": "s1",
                  "section": "Abstract",
                  "text": "The abstract should be expanded.",
                  "severity": "MODERATE",
                  "isAddressed": false
                }
              ]
            }
        """.trimIndent()

        val report = json.decodeFromString<AiReviewReport>(sampleJson)
        assertEquals(85, report.score)
        assertEquals(90, report.structure)
        assertEquals(75, report.citations)
        assertEquals(88, report.clarity)
        assertEquals(82, report.originality)
        assertEquals(1, report.suggestions.size)
        assertEquals("Abstract", report.suggestions[0].section)
    }

    @Test
    fun testRepositoryWithFakeApi() = runBlocking {
        val fakeApi = object : FireworksApi {
            override suspend fun chatCompletions(request: com.citecircle.app.core.network.ChatCompletionRequest): ChatCompletionResponse {
                val jsonPayload = """
                    ```json
                    {
                      "score": 92,
                      "structure": 95,
                      "citations": 88,
                      "clarity": 90,
                      "originality": 94,
                      "suggestions": [
                        {
                          "id": "s1",
                          "section": "Methodology",
                          "text": "Clarify statistical sample size.",
                          "severity": "MINOR",
                          "isAddressed": false
                        }
                      ]
                    }
                    ```
                """.trimIndent()
                return ChatCompletionResponse(
                    choices = listOf(
                        ChatCompletionChoice(
                            message = ChatMessage(role = "assistant", content = jsonPayload)
                        )
                    )
                )
            }
        }

        val repository = FireworksAiReviewRepository(fakeApi, json)
        val draft = PaperDraft(
            title = "Situated Cognition in AI-Augmented Workspaces",
            abstract = "This paper explores situated cognition in modern software engineering environments."
        )

        val report = repository.reviewPaper(draft)
        assertNotNull(report)
        assertEquals(92, report.score)
        assertEquals(95, report.structure)
    }
}
