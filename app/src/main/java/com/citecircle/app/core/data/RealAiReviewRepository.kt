package com.citecircle.app.core.data

import com.citecircle.app.core.model.AiReviewReport
import com.citecircle.app.core.model.AiSuggestion
import com.citecircle.app.core.model.PaperDraft
import com.citecircle.app.core.model.Severity
import com.citecircle.app.core.network.AiReviewReportDto
import com.citecircle.app.core.network.AiSuggestionDto
import com.citecircle.app.core.network.CiteCircleApi
import com.citecircle.app.core.network.ReviewRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes peer review through the Cite Circle backend rather than calling Fireworks
 * from the device. The server holds the API key and archives every report to the
 * MongoDB `ai_reviews` collection, neither of which an on-device call can do.
 */
@Singleton
class RealAiReviewRepository @Inject constructor(
    private val api: CiteCircleApi,
) : AiReviewRepository {

    private val _progress = MutableStateFlow<AiReviewStage>(AiReviewStage.Idle)
    override fun getReviewProgress(): Flow<AiReviewStage> = _progress.asStateFlow()

    override suspend fun reviewPaper(draft: PaperDraft): AiReviewReport {
        _progress.value = AiReviewStage.InProgress("Parsing manuscript sections & references...", 1, 5)
        return try {
            _progress.value = AiReviewStage.InProgress("Evaluating ethical hard gates...", 2, 5)
            _progress.value = AiReviewStage.InProgress("Analyzing structure & citations via Fireworks.ai...", 3, 5)
            
            val dto = api.reviewPaper(
                ReviewRequestDto(
                    title = draft.title,
                    abstract = draft.abstract,
                    fullText = draft.fullText,
                    sections = draft.sections.ifEmpty { null }
                )
            )

            _progress.value = AiReviewStage.InProgress("Formatting recommendations & verdict...", 4, 5)

            val report = dto.toDomain()
            _progress.value = AiReviewStage.Complete(report)
            report
        } catch (e: Exception) {
            _progress.value = AiReviewStage.Error(e.message ?: "AI review failed")
            throw e
        }
    }
}

private fun AiSuggestionDto.toDomain() = AiSuggestion(
    id = id,
    section = section,
    text = text,
    severity = runCatching { Severity.valueOf(severity) }.getOrDefault(Severity.MODERATE),
    passageQuote = passageQuote,
    isAddressed = isAddressed,
)

private fun AiReviewReportDto.toDomain() = AiReviewReport(
    score = overallScore,
    structure = structure,
    citations = citations,
    clarity = clarity,
    originality = originality,
    verdict = verdict,
    summary = summary,
    strengths = strengths,
    weaknesses = weaknesses,
    suggestions = suggestions.map { it.toDomain() },
    deskRejected = deskRejected
)
