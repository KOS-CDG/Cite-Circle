package com.citecircle.app.feature.publish

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.citecircle.app.core.data.UserRepository
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
    private val paperRepository: PaperRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _currentStep = MutableStateFlow(1)
    val currentStep = _currentStep.asStateFlow()

    private val _draft = MutableStateFlow(PaperDraft())
    val draft = _draft.asStateFlow()

    private val _reviewReport = MutableStateFlow<AiReviewReport?>(null)
    val reviewReport = _reviewReport.asStateFlow()

    private val _reviewProgress = MutableStateFlow<AiReviewStage>(AiReviewStage.Idle)
    val reviewProgress = _reviewProgress.asStateFlow()

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing = _isPublishing.asStateFlow()

    private val _publishError = MutableStateFlow<String?>(null)
    val publishError = _publishError.asStateFlow()

    val currentUser = userRepository.getCurrentUser()

    fun setStep(step: Int) {
        _currentStep.value = step
    }

    fun updateDraft(updater: (PaperDraft) -> PaperDraft) {
        _draft.value = updater(_draft.value)
    }

    fun runAiReview(onComplete: () -> Unit) {
        viewModelScope.launch {
            _reviewProgress.value = AiReviewStage.InProgress("Reading manuscript...", 1, 5)
            val progressJob = launch {
                aiReviewRepository.getReviewProgress().collect {
                    _reviewProgress.value = it
                }
            }
            try {
                val report = aiReviewRepository.reviewPaper(_draft.value)
                _reviewReport.value = report
                _reviewProgress.value = AiReviewStage.Complete(report)
                onComplete()
            } catch (e: Exception) {
                _reviewProgress.value = AiReviewStage.Error(
                    e.message ?: "AI review failed. Please check your connection and try again."
                )
            } finally {
                progressJob.cancel()
            }
        }
    }

    fun publishPaper(onSuccess: () -> Unit) {
        if (_isPublishing.value) return
        viewModelScope.launch {
            _isPublishing.value = true
            _publishError.value = null
            try {
                paperRepository.publishPaper(_draft.value)
                onSuccess()
            } catch (e: Exception) {
                _publishError.value = e.message ?: "Couldn't publish your manuscript. Please try again."
            } finally {
                _isPublishing.value = false
            }
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
    val isPublishing by viewModel.isPublishing.collectAsState()
    val publishError by viewModel.publishError.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState(initial = null)

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
                        onDraftChange = { updated -> viewModel.updateDraft { updated } },
                        onBack = { viewModel.setStep(1) },
                        onContinue = { updated ->
                            viewModel.updateDraft { updated }
                            viewModel.setStep(3)
                            viewModel.runAiReview {}
                        }
                    )


                    3 -> Step3AiReview(
                        progress = progress,
                        report = report,
                        isPublishing = isPublishing,
                        publishError = publishError,
                        onBackToEdit = { viewModel.setStep(2) },
                        onRetry = { viewModel.runAiReview {} },
                        onPublish = {
                            viewModel.publishPaper {
                                viewModel.setStep(4)
                            }
                        }
                    )
                    4 -> Step4Success(
                        draft = draft,
                        authorName = currentUser?.name ?: FakeDataSource.currentUser.name,
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
    onDraftChange: (PaperDraft) -> Unit,
    onBack: () -> Unit,
    onContinue: (PaperDraft) -> Unit
) {


    var title by remember { mutableStateOf(draft.title) }
    var abstract by remember { mutableStateOf(draft.abstract) }
    var pdfName by remember { mutableStateOf(draft.pdfFileName) }
    var pdfSizeLabel by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val projection = arrayOf(
                android.provider.OpenableColumns.DISPLAY_NAME,
                android.provider.OpenableColumns.SIZE
            )
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            var displayName = uri.lastPathSegment ?: "document.pdf"
            var sizeKb: Long? = null
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (nameIndex >= 0) displayName = c.getString(nameIndex)
                    if (sizeIndex >= 0) sizeKb = c.getLong(sizeIndex) / 1024L
                }
            }
            val sizeLabel = when {
                sizeKb == null  -> ""
                sizeKb!! < 1024 -> "${sizeKb} KB"
                else            -> "${ "%.1f".format(sizeKb!! / 1024.0) } MB"
            }
            pdfName = displayName
            pdfSizeLabel = sizeLabel
            onDraftChange(draft.copy(title = title, abstract = abstract, pdfFileName = displayName, pdfFileSizeKb = sizeKb, pdfUri = uri))
        }
    }

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
            onValueChange = {
                title = it
                onDraftChange(draft.copy(title = title, abstract = abstract, pdfFileName = pdfName))
            },
            label = "Paper Title",
            placeholder = "e.g. Situated Cognition in AI-Augmented workspaces",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        CcTextField(
            value = abstract,
            onValueChange = {
                abstract = it
                onDraftChange(draft.copy(title = title, abstract = abstract, pdfFileName = pdfName))
            },
            label = "Abstract Summary",
            singleLine = false,
            maxLines = 6,
            placeholder = "Provide a summary of the methodology and findings...",
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Document Attachment Block
        if (pdfName != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Description, contentDescription = "File Uploaded", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = pdfName!!, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    if (!pdfSizeLabel.isNullOrBlank()) {
                        Text(text = pdfSizeLabel!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ccColors.marginGray)
                    }
                }
                IconButton(onClick = {
                    pdfName = null
                    pdfSizeLabel = null
                    onDraftChange(draft.copy(title = title, abstract = abstract, pdfFileName = null, pdfFileSizeKb = null, pdfUri = null))
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove manuscript", tint = CcColors.CoralPop)
                }
            }
        } else {
            CcSecondaryButton(
                text = "Upload manuscript (PDF, Word, or Text)",
                onClick = {
                    filePickerLauncher.launch(
                        arrayOf(
                            "application/pdf",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/msword",
                            "application/vnd.oasis.opendocument.text",
                            "text/plain",
                            "application/rtf",
                            "text/rtf"
                        )
                    )
                },
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
                    val updated = draft.copy(title = title, abstract = abstract, pdfFileName = pdfName)
                    onContinue(updated)
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
    isPublishing: Boolean = false,
    publishError: String? = null,
    onBackToEdit: () -> Unit,
    onPublish: () -> Unit,
    onRetry: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AI Pre-Review Report",
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = FrauncesFamily,
                                fontWeight = FontWeight.Bold
                            )

                            if (report.verdict.isNotBlank()) {
                                VerdictBadge(verdict = report.verdict, deskRejected = report.deskRejected)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (report.deskRejected) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Cancel, contentDescription = "Desk Rejection", tint = MaterialTheme.colorScheme.error)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Early Desk Rejection",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = report.summary.ifEmpty { "Manuscript was desk rejected due to non-IMRaD genre or hard gate ethical failure." },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        } else {
                            // Circular Readiness Gauge Card
                            ReadinessGaugeCard(score = report.score, verdict = report.verdict)

                            Spacer(modifier = Modifier.height(16.dp))

                            // Section breakdown bars
                            SectionBreakdownCard(report = report)

                            Spacer(modifier = Modifier.height(16.dp))
                        }

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

                        if (publishError != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Warning, contentDescription = "Publish error", tint = MaterialTheme.colorScheme.onErrorContainer)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Publishing failed: $publishError",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CcSecondaryButton(
                                text = "Revise Draft",
                                onClick = onBackToEdit,
                                enabled = !isPublishing,
                                modifier = Modifier.weight(1f).padding(end = 12.dp)
                            )
                            CcPrimaryButton(
                                text = "Publish anyway",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPublish()
                                },
                                enabled = !isPublishing && !report.deskRejected,
                                isLoading = isPublishing,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            is AiReviewStage.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = "Review error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "AI review couldn't finish",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = progress.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.ccColors.marginGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        CcSecondaryButton(
                            text = "Back to Edit",
                            onClick = onBackToEdit,
                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                        )
                        CcPrimaryButton(
                            text = "Retry Review",
                            onClick = onRetry,
                            modifier = Modifier.weight(1f)
                        )
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
fun VerdictBadge(verdict: String, deskRejected: Boolean = false) {
    val (badgeBg, badgeFg, text) = when {
        deskRejected || verdict.uppercase() == "REJECT" -> Triple(CcColors.CoralPop.copy(alpha = 0.15f), CcColors.CoralPop, "REJECT")
        verdict.uppercase() == "MAJOR_REVISIONS" -> Triple(CcColors.HighlighterYellow, CcColors.InkNavy, "MAJOR REVISIONS")
        verdict.uppercase() == "MINOR_REVISIONS" -> Triple(CcColors.CircleBlue.copy(alpha = 0.15f), CcColors.CircleBlue, "MINOR REVISIONS")
        else -> Triple(CcColors.SeafoamTeal.copy(alpha = 0.2f), CcColors.SeafoamTeal, "ACCEPT")
    }

    Surface(
        color = badgeBg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            text = text,
            color = badgeFg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ReadinessGaugeCard(score: Int, verdict: String = "") {
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
                        color = if (score >= 80) CcColors.SeafoamTeal else if (score >= 60) CcColors.CircleBlue else CcColors.CoralPop,
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
                Text(
                    text = if (verdict.isNotBlank()) "Readiness: ${verdict.replace("_", " ")}" else "Readiness Score",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        score >= 85 -> "Exemplary paper structure! Minimal revisions needed."
                        score >= 70 -> "Proficient submission. Minor presentational tweaks recommended."
                        score >= 50 -> "Developing manuscript. Address methodological revisions before publishing."
                        else -> "Requires major structural or ethical revisions before review."
                    },
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

            ScoreProgressRow(label = "Structure (30%)", score = report.structure)
            Spacer(modifier = Modifier.height(12.dp))
            ScoreProgressRow(label = "Citations (25%)", score = report.citations)
            Spacer(modifier = Modifier.height(12.dp))
            ScoreProgressRow(label = "Clarity (20%)", score = report.clarity)
            Spacer(modifier = Modifier.height(12.dp))
            ScoreProgressRow(label = "Originality (25%)", score = report.originality)
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

                Surface(
                    color = dotColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = suggestion.severity.name.replace("_", " "),
                        color = if (suggestion.severity == Severity.MODERATE) CcColors.InkNavy else dotColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

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

            if (!suggestion.passageQuote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "“${suggestion.passageQuote}”",
                        style = MaterialTheme.typography.labelMedium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.ccColors.marginGray,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Text(
                text = if (expanded) "Show less" else "Show more",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { expanded = !expanded }
                    .padding(top = 6.dp)
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
    authorName: String,
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
                    Text(text = "By $authorName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ccColors.marginGray)
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
