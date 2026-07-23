package com.citecircle.app.feature.papers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citecircle.app.core.data.PaperRepository
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.Paper
import com.citecircle.app.core.model.Shelf
import com.citecircle.app.feature.publish.ReadinessGaugeCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check

import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import kotlinx.coroutines.delay

// ──────────────────────────────────────────────────────────────────────────────
// SummaryUiState
// ──────────────────────────────────────────────────────────────────────────────

sealed interface SummaryUiState {
    object Idle : SummaryUiState
    object Loading : SummaryUiState
    data class Success(val summary: String) : SummaryUiState
    data class Error(val message: String) : SummaryUiState
}

// ──────────────────────────────────────────────────────────────────────────────
// PaperDetailViewModel
// ──────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class PaperDetailViewModel @Inject constructor(
    private val paperRepository: PaperRepository
) : ViewModel() {

    private val _paper = MutableStateFlow<Paper?>(null)
    val paper: StateFlow<Paper?> = _paper.asStateFlow()

    private val _summaryState = MutableStateFlow<SummaryUiState>(SummaryUiState.Idle)
    val summaryState: StateFlow<SummaryUiState> = _summaryState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    fun loadPaperData(paperId: String) {
        viewModelScope.launch {
            paperRepository.getPaperById(paperId).collect {
                _paper.value = it
            }
        }
    }

    fun refreshPaperData(paperId: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadPaperData(paperId)
            _paper.value?.let { p ->
                loadSummary(p.id, p.title, p.abstract, force = true)
            }
            delay(800)
            _isRefreshing.value = false
        }
    }

    val shelves: StateFlow<List<Shelf>> = paperRepository.getShelves()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun isPaperSaved(paperId: String): Flow<Boolean> = paperRepository.isPaperSaved(paperId)

    fun toggleSave(paperId: String) {
        viewModelScope.launch {
            paperRepository.toggleSavePaper(paperId)
        }
    }

    fun addPaperToShelf(paperId: String, shelfId: String) {
        viewModelScope.launch {
            paperRepository.addPaperToShelf(paperId, shelfId)
        }
    }

    fun citePaper(paperId: String) {
        viewModelScope.launch {
            val updated = paperRepository.citePaper(paperId)
            if (updated.title.isNotEmpty()) {
                _paper.value = _paper.value?.copy(citationCount = updated.citationCount) ?: updated
            } else {
                _paper.value?.let { current ->
                    _paper.value = current.copy(citationCount = current.citationCount + 1)
                }
            }
        }
    }

    fun loadSummary(paperId: String, title: String, abstract: String, force: Boolean = false) {
        if (_summaryState.value is SummaryUiState.Success && !force) return
        viewModelScope.launch {
            _summaryState.value = SummaryUiState.Loading
            try {
                val summary = paperRepository.getPaperSummary(paperId, title, abstract)
                _summaryState.value = SummaryUiState.Success(summary)
            } catch (e: Exception) {
                _summaryState.value = SummaryUiState.Error(
                    e.localizedMessage ?: "Failed to generate summary"
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// PaperDetailScreen Composable
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperDetailScreen(
    paperId: String,
    onBack: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onReadPaperClick: (String) -> Unit = {},
    viewModel: PaperDetailViewModel = hiltViewModel()
) {

    LaunchedEffect(paperId) {
        viewModel.loadPaperData(paperId)
    }

    val paper by viewModel.paper.collectAsState()
    val summaryState by viewModel.summaryState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paper Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                actions = {
                    val isSaved by remember(paperId) { viewModel.isPaperSaved(paperId) }
                        .collectAsState(initial = false)
                    val shelves by viewModel.shelves.collectAsState()
                    var showShelfMenu by remember { mutableStateOf(false) }

                    IconButton(onClick = { viewModel.toggleSave(paperId) }) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save paper",
                            tint = if (isSaved) CcColors.CircleBlue else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (isSaved && shelves.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { showShelfMenu = true }) {
                                Icon(Icons.Outlined.FolderOpen, contentDescription = "Add to Shelf")
                            }
                            DropdownMenu(
                                expanded = showShelfMenu,
                                onDismissRequest = { showShelfMenu = false }
                            ) {
                                Text(
                                    text = "Add to Shelf",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    color = MaterialTheme.ccColors.marginGray
                                )
                                shelves.forEach { shelf ->
                                    val containsPaper = shelf.paperIds.contains(paperId)
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (containsPaper) Icons.Filled.Check else Icons.Outlined.Folder,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = if (containsPaper) CcColors.CircleBlue else MaterialTheme.ccColors.marginGray
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(shelf.name)
                                            }
                                        },
                                        onClick = {
                                            if (!containsPaper) {
                                                viewModel.addPaperToShelf(paperId, shelf.id)
                                            }
                                            showShelfMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshPaperData(paperId) },
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.ccColors.paperCream)
                .padding(paddingValues)
        ) {
            val p = paper
            if (p == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(24.dp)
                ) {
                    // Title in Fraunces
                    Text(
                        text = p.title,
                        fontFamily = FrauncesFamily,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Authors horizontal list row
                    Text(
                        text = "Authors",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.ccColors.marginGray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(p.authors) { author ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { onAuthorClick(author.id) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CcAvatar(user = author, size = 28.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = author.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // DOI and citation info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DOI: ${p.doi}",
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMonoFamily,
                                color = MaterialTheme.ccColors.marginGray
                            )
                            Text(
                                text = "${p.journal} • ${p.year}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Citation highlighted stats
                        HighlighterSweep {
                            Box(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${p.citationCount} Citations",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CcColors.InkNavy
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // AI Score (if available)
                    if (p.aiScore != null) {
                        ReadinessGaugeCard(score = p.aiScore!!)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // AI Key Takeaways Card
                    AiPaperSummaryCard(
                        summaryState = summaryState,
                        onExpand = { viewModel.loadSummary(p.id, p.title, p.abstract) },
                        onRetry = { viewModel.loadSummary(p.id, p.title, p.abstract, force = true) }
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    // Abstract body details
                    Text(
                        text = "Abstract",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = p.abstract,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Tags
                    if (p.fieldTags.isNotEmpty()) {
                        Text(
                            text = "Field Tags",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.ccColors.marginGray,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(p.fieldTags) { tag ->
                                CcChip(label = tag, selected = false, onClick = {})
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Interactive Action Button links
                    Column(modifier = Modifier.fillMaxWidth()) {
                        CcPrimaryButton(
                            text = "📖 Read Preprint / Interactive PDF Studio",
                            onClick = { onReadPaperClick(p.id) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            CcSecondaryButton(
                                text = "Cite Paper",
                                onClick = {
                                    val textCitation = "${p.authors.firstOrNull()?.name ?: "Unknown"} et al. (${p.year}). ${p.title}. ${p.journal}. DOI: ${p.doi}"
                                    clipboardManager.setText(AnnotatedString(textCitation))
                                    viewModel.citePaper(p.id)
                                },
                                icon = Icons.Outlined.ContentCopy,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )

                            CcSecondaryButton(
                                text = "Download PDF",
                                onClick = { onReadPaperClick(p.id) },
                                icon = Icons.Outlined.Description,
                                modifier = Modifier.weight(1f).padding(start = 8.dp)
                            )
                        }
                    }


                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// AiPaperSummaryCard — collapsible AI key takeaways card
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun AiPaperSummaryCard(
    summaryState: SummaryUiState,
    onExpand: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "summary_arrow"
    )

    CcCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header Row ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isExpanded = !isExpanded
                        if (isExpanded && summaryState is SummaryUiState.Idle) onExpand()
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Gradient icon badge
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(CcColors.CircleBlue, CcColors.SeafoamTeal)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✨", fontSize = 17.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "AI Key Takeaways",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "3-bullet TL;DR · Powered by Fireworks AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.ccColors.marginGray
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse summary" else "Expand summary",
                    tint = MaterialTheme.ccColors.marginGray,
                    modifier = Modifier.graphicsLayer { rotationZ = arrowRotation }
                )
            }

            // ── Expandable Content ──
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {

                    HorizontalDivider(
                        color = MaterialTheme.ccColors.divider,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    when (summaryState) {
                        is SummaryUiState.Idle, is SummaryUiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AiPencilProgress()
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Generating summary…",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.ccColors.marginGray
                                    )
                                }
                            }
                        }

                        is SummaryUiState.Success -> {
                            // Parse bullet lines from the AI response
                            val rawLines = summaryState.summary.lines()
                            val bullets = rawLines
                                .map { it.trim() }
                                .filter { it.startsWith("•") || it.startsWith("-") || it.startsWith("*") }
                                .map { it.trimStart('•', '-', '*').trim() }
                                .filter { it.isNotBlank() }
                                .ifEmpty {
                                    rawLines.map { it.trim() }.filter { it.isNotBlank() }
                                }

                            bullets.forEachIndexed { idx, bullet ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 5.dp)
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(CcColors.CircleBlue)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = bullet,
                                        style = MaterialTheme.typography.bodySmall,
                                        lineHeight = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (idx < bullets.lastIndex) Spacer(Modifier.height(6.dp))
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Generated by Fireworks AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.ccColors.marginGray.copy(alpha = 0.55f),
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        is SummaryUiState.Error -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Warning,
                                    contentDescription = null,
                                    tint = CcColors.CoralPop,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Couldn't generate summary",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CcColors.CoralPop,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Retry",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { onRetry() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
