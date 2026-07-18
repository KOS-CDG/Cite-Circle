package com.citecircle.app.feature.publish

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.sin
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citecircle.app.core.data.AiReviewRepository
import com.citecircle.app.core.data.AiReviewStage
import com.citecircle.app.core.data.FakeDataSource
import com.citecircle.app.core.data.PaperRepository
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random
import androidx.hilt.navigation.compose.hiltViewModel

// ──────────────────────────────────────────────────────────────────────────────
// PublishViewModel
// ──────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class PublishViewModel @Inject constructor(
    private val aiReviewRepository: AiReviewRepository,
    private val paperRepository: PaperRepository
) : ViewModel() {

    private val _currentStep = MutableStateFlow(1)
    val currentStep = _currentStep.asStateFlow()

    private val _draft = MutableStateFlow(PaperDraft())
    val draft = _draft.asStateFlow()

    private val _reviewReport = MutableStateFlow<AiReviewReport?>(null)
    val reviewReport = _reviewReport.asStateFlow()

    private val _reviewProgress = MutableStateFlow<AiReviewStage>(AiReviewStage.Idle)
    val reviewProgress = _reviewProgress.asStateFlow()

    fun setStep(step: Int) {
        _currentStep.value = step
    }

    fun updateDraft(updater: (PaperDraft) -> PaperDraft) {
        _draft.value = updater(_draft.value)
    }

    fun runAiReview(onComplete: () -> Unit) {
        viewModelScope.launch {
            _reviewProgress.value = AiReviewStage.InProgress("Reading manuscript...", 1, 6)
            // Call simulated API
            launch {
                aiReviewRepository.getReviewProgress().collect {
                    _reviewProgress.value = it
                }
            }
            val report = aiReviewRepository.reviewPaper(_draft.value)
            _reviewReport.value = report
            _reviewProgress.value = AiReviewStage.Complete(report)
            onComplete()
        }
    }

    fun publishPaper(onSuccess: () -> Unit) {
        viewModelScope.launch {
            paperRepository.publishPaper(_draft.value)
            onSuccess()
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// PublishFlowScreen Composable
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishFlowScreen(
    onDismiss: () -> Unit,
    onQuickPost: () -> Unit = {},
    viewModel: PublishViewModel = hiltViewModel()
) {
    val step by viewModel.currentStep.collectAsState()
    val draft by viewModel.draft.collectAsState()
    val report by viewModel.reviewReport.collectAsState()
    val progress by viewModel.reviewProgress.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Publish manuscript", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close publish flow")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.ccColors.paperCream)
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = step,
                label = "publish_step_transition",
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }
                }
            ) { currentStep ->
                when (currentStep) {
                    1 -> Step1ChooseType(
                        onQuickPostClick = onQuickPost,
                        onPublishPaperClick = { viewModel.setStep(2) }
                    )
                    2 -> Step2Details(
                        draft = draft,
                        onDraftChange = { viewModel.updateDraft { it } },
                        onBack = { viewModel.setStep(1) },
                        onContinue = {
                            viewModel.setStep(3)
                            viewModel.runAiReview {}
                        }
                    )
                    3 -> Step3AiReview(
                        progress = progress,
                        report = report,
                        onBackToEdit = { viewModel.setStep(2) },
                        onPublish = {
                            viewModel.publishPaper {
                                viewModel.setStep(4)
                            }
                        }
                    )
                    4 -> Step4Success(
                        draft = draft,
                        onDone = onDismiss
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Step 1: Choose Type
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun Step1ChooseType(
    onQuickPostClick: () -> Unit,
    onPublishPaperClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "What would you like to share?",
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FrauncesFamily,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Card 1: Quick Post
        CcCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.ccColors.divider, RoundedCornerShape(16.dp))
                .clickable(onClick = onQuickPostClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📝", fontSize = 42.sp)
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        text = "Quick Post",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CcColors.InkNavy
                    )
                    Text(
                        text = "Ask a question, share an update, or start a discussion.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.ccColors.marginGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 2: Publish Paper
        CcCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                .clickable(onClick = onPublishPaperClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📚", fontSize = 42.sp)
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        text = "Publish a Paper",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Upload a manuscript, run AI Pre-Review checks, and share with academia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.ccColors.marginGray
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Step 2: Paper Details Input
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun Step2Details(
    draft: PaperDraft,
    onDraftChange: () -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    var title by remember { mutableStateOf(draft.title) }
    var abstract by remember { mutableStateOf(draft.abstract) }
    var pdfName by remember { mutableStateOf(draft.pdfFileName) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        Text(
            text = "Enter Paper Details",
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FrauncesFamily,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        CcTextField(
            value = title,
            onValueChange = { title = it },
            label = "Paper Title",
            placeholder = "e.g. Situated Cognition in AI-Augmented workspaces",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        CcTextField(
            value = abstract,
            onValueChange = { abstract = it },
            label = "Abstract Summary",
            singleLine = false,
            maxLines = 6,
            placeholder = "Provide a summary of the methodology and findings...",
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // PDF Attachment Block
        if (pdfName != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Description, contentDescription = "PDF Uploaded", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = pdfName!!, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text(text = "2.4 MB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ccColors.marginGray)
                }
                IconButton(onClick = { pdfName = null }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove PDF", tint = CcColors.CoralPop)
                }
            }
        } else {
            CcSecondaryButton(
                text = "Upload manuscript PDF",
                onClick = { pdfName = "Situated_Cognition_Paper_Draft.pdf" },
                icon = Icons.Filled.Add,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CcSecondaryButton(text = "Back", onClick = onBack, modifier = Modifier.weight(1f).padding(end = 12.dp))
            CcPrimaryButton(
                text = "Continue",
                onClick = {
                    draft.copy(title = title, abstract = abstract, pdfFileName = pdfName)
                    onContinue()
                },
                enabled = title.isNotBlank() && abstract.isNotBlank() && pdfName != null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Step 3: AI Review Analysis
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun Step3AiReview(
    progress: AiReviewStage,
    report: AiReviewReport?,
    onBackToEdit: () -> Unit,
    onPublish: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (progress) {
            is AiReviewStage.InProgress -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AiPencilProgress()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = progress.message,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Running pre-review scans (${progress.step}/${progress.totalSteps})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.ccColors.marginGray
                    )
                }
            }
            is AiReviewStage.Complete -> {
                if (report != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "AI Pre-Review Report",
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = FrauncesFamily,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Circular Readiness Gauge Card
                        ReadinessGaugeCard(score = report.score)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Section breakdown bars
                        SectionBreakdownCard(report = report)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Suggestions Title
                        Text(
                            text = "Actionable Suggestions (${report.suggestions.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Suggestion items list
                        report.suggestions.forEach { suggestion ->
                            SuggestionRowItem(suggestion = suggestion)
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Disclaimer chip
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.ccColors.highlighterYellowAlpha),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Warning, contentDescription = "Disclaimer warning", tint = CcColors.InkNavy)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "AI pre-review is advisory — final publishing is your call.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CcColors.InkNavy
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CcSecondaryButton(text = "Revise Draft", onClick = onBackToEdit, modifier = Modifier.weight(1f).padding(end = 12.dp))
                            CcPrimaryButton(text = "Publish anyway", onClick = onPublish, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            else -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun ReadinessGaugeCard(score: Int) {
    var animationProgress by remember { mutableStateOf(0f) }
    LaunchedEffect(score) {
        animate(
            initialValue = 0f,
            targetValue = score.toFloat(),
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        ) { value, _ ->
            animationProgress = value
        }
    }

    CcCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 8.dp.toPx()
                    val radius = size.width / 2 - strokeWidth
                    val center = Offset(size.width / 2, size.height / 2)

                    // Draw track
                    drawCircle(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )

                    // Draw score arc
                    drawArc(
                        color = if (score > 80) CcColors.SeafoamTeal else CcColors.CircleBlue,
                        startAngle = -90f,
                        sweepAngle = 3.6f * animationProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth),
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                    )
                }
                Text(
                    text = "${animationProgress.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Text(text = "Readiness Score", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = if (score > 80) "Excellent paper structure! Minor tweaks recommended." else "Good start. Add citation context to optimize peer approval.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.ccColors.marginGray
                )
            }
        }
    }
}

@Composable
fun SectionBreakdownCard(report: AiReviewReport) {
    CcCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Section Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            ScoreProgressRow(label = "Structure", score = report.structure)
            Spacer(modifier = Modifier.height(12.dp))
            ScoreProgressRow(label = "Citations", score = report.citations)
            Spacer(modifier = Modifier.height(12.dp))
            ScoreProgressRow(label = "Clarity", score = report.clarity)
            Spacer(modifier = Modifier.height(12.dp))
            ScoreProgressRow(label = "Originality", score = report.originality)
        }
    }
}

@Composable
fun ScoreProgressRow(label: String, score: Int) {
    val barColor = when {
        score >= 80 -> CcColors.SeafoamTeal
        score >= 60 -> CcColors.HighlighterYellow
        else -> CcColors.CoralPop
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text(text = "$score/100", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { score.toFloat() / 100f },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = barColor,
            trackColor = MaterialTheme.ccColors.divider
        )
    }
}

@Composable
fun SuggestionRowItem(suggestion: AiSuggestion) {
    var isAddressed by remember { mutableStateOf(suggestion.isAddressed) }
    var expanded by remember { mutableStateOf(false) }

    val dotColor = when (suggestion.severity) {
        Severity.MINOR -> CcColors.SeafoamTeal
        Severity.MODERATE -> CcColors.HighlighterYellow
        Severity.NEEDS_ATTENTION -> CcColors.CoralPop
    }

    CcCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Severity dot indicator
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "[${suggestion.section}]",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                Checkbox(
                    checked = isAddressed,
                    onCheckedChange = { isAddressed = it }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = suggestion.text,
                style = MaterialTheme.typography.bodySmall,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = if (expanded) "Show less" else "Show more",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { expanded = !expanded }
                    .padding(top = 4.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Step 4: Success Screen
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun Step4Success(
    draft: PaperDraft,
    onDone: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Confetti burst Canvas raining dots
        SuccessConfettiRain()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(CcColors.SeafoamTeal),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Success check", tint = Color.White, modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Published!",
                fontFamily = FrauncesFamily,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your manuscript has been shared to CiteCircle and indexed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.ccColors.marginGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Mini Paper card representation
            CcCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = draft.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "By ${FakeDataSource.currentUser.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ccColors.marginGray)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            CcPrimaryButton(text = "Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun SuccessConfettiRain() {
    val particles = remember { List(30) { ConfettiParticle() } }
    val infiniteTransition = rememberInfiniteTransition(label = "success_confetti")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_rain"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val y = (p.startY + progress * size.height * p.speedMultiplier) % size.height
            val x = p.startX + sin(progress * 5f + p.swaySeed) * 20.dp.toPx()
            drawCircle(
                color = p.color,
                radius = p.radius.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

private class ConfettiParticle {
    val startX = Random.nextFloat() * 400.dp.value
    val startY = Random.nextFloat() * -300.dp.value
    val radius = Random.nextInt(4, 8)
    val speedMultiplier = Random.nextFloat() * 0.5f + 0.6f
    val swaySeed = Random.nextFloat() * 2f
    val color = when (Random.nextInt(4)) {
        0 -> CcColors.CircleBlue
        else -> CcColors.CoralPop
    }
}
