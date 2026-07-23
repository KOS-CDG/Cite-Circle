package com.citecircle.app.feature.pdf

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.citecircle.app.core.designsystem.CcColors
import com.citecircle.app.core.designsystem.FrauncesFamily
import com.citecircle.app.core.designsystem.JetBrainsMonoFamily
import com.citecircle.app.core.designsystem.ccColors
import com.citecircle.app.core.model.AnnotationColor
import com.citecircle.app.core.model.PaperAnnotation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    paperId: String,
    initialPage: Int = 1,
    onBack: () -> Unit,
    onShareQuoteToFeed: (quote: String, pageNumber: Int, paperId: String) -> Unit = { _, _, _ -> },
    viewModel: PdfViewerViewModel = hiltViewModel()
) {
    LaunchedEffect(paperId, initialPage) {
        viewModel.loadPaper(paperId, initialPage)
    }

    val paper by viewModel.paper.collectAsState()
    val annotations by viewModel.annotations.collectAsState()
    val aiBreakdown by viewModel.aiBreakdown.collectAsState()
    val isLoadingBreakdown by viewModel.isLoadingBreakdown.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val zoomScale by viewModel.zoomScale.collectAsState()
    val isVerticalMode by viewModel.isVerticalMode.collectAsState()
    val selectedColor by viewModel.selectedColor.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showSearch by viewModel.showSearch.collectAsState()
    val showAiSheet by viewModel.showAiSheet.collectAsState()
    val activeStickyNote by viewModel.activeStickyNote.collectAsState()

    var selectedTextRange by remember { mutableStateOf<String?>(null) }
    var selectionOffset by remember { mutableStateOf<Offset?>(null) }
    var showStickyNoteDialog by remember { mutableStateOf(false) }
    var newNoteText by remember { mutableStateOf("") }
    var noteTapPosition by remember { mutableStateOf(Offset(0.5f, 0.5f)) }

    // Gestures zoom transform state
    var offset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }

    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.8f, 3.5f)
        offset += offsetChange
        viewModel.setZoomScale(scale)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = paper?.title ?: "Preprint Reader",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Page $currentPage of $totalPages",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.ccColors.marginGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                actions = {
                    IconButton(onClick = { viewModel.toggleSearch() }) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search in PDF")
                    }
                    IconButton(onClick = { viewModel.toggleOrientation() }) {
                        Icon(
                            imageVector = if (isVerticalMode) Icons.Outlined.ViewStream else Icons.Outlined.ViewCarousel,
                            contentDescription = "Toggle View Mode"
                        )
                    }
                    IconButton(onClick = { viewModel.setShowAiSheet(true) }) {
                        Text("✨", fontSize = 18.sp)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setShowAiSheet(true) },
                containerColor = CcColors.CircleBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 60.dp)
                    .shadow(12.dp, CircleShape)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✨", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "AI Breakdown",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E24))
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Jump Search Bar (if visible)
                AnimatedVisibility(
                    visible = showSearch,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Jump to page or search text…") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val pageNum = searchQuery.toIntOrNull()
                                    if (pageNum != null) viewModel.setPage(pageNum)
                                }
                            ) {
                                Text("Jump")
                            }
                        }
                    }
                }

                // Color Palette Highlighter Bar
                HighlighterPaletteBar(
                    selectedColor = selectedColor,
                    onColorSelect = { viewModel.setSelectedColor(it) }
                )

                // High-Performance Canvas Page Renderer Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(0.dp))
                        .transformable(state = transformableState)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { tapOffset ->
                                    val relX = (tapOffset.x / size.width).coerceIn(0.1f, 0.9f)
                                    val relY = (tapOffset.y / size.height).coerceIn(0.1f, 0.9f)
                                    noteTapPosition = Offset(relX, relY)
                                    selectionOffset = tapOffset
                                    selectedTextRange = "Selected passage on page $currentPage"
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CanvasPageRenderView(
                        currentPage = currentPage,
                        paperTitle = paper?.title ?: "Research Preprint",
                        paperAbstract = paper?.abstract ?: "",
                        annotations = annotations.filter { it.pageNumber == currentPage },
                        zoomScale = scale,
                        offset = offset,
                        onStickyNoteClick = { note -> viewModel.setActiveStickyNote(note) }
                    )

                    // Contextual Floating Action Menu (Highlight / Sticky Note / Share Quote)
                    if (selectedTextRange != null && selectionOffset != null) {
                        ContextualAnnotationMenu(
                            offset = selectionOffset!!,
                            selectedColor = selectedColor,
                            onHighlight = {
                                viewModel.addHighlight(
                                    pageNumber = currentPage,
                                    selectedText = selectedTextRange ?: "Key Finding Excerpt",
                                    xRatio = noteTapPosition.x,
                                    yRatio = noteTapPosition.y
                                )
                                selectedTextRange = null
                            },
                            onAddStickyNote = {
                                showStickyNoteDialog = true
                            },
                            onShareQuote = {
                                val quote = selectedTextRange ?: "Selected passage"
                                selectedTextRange = null
                                onShareQuoteToFeed(quote, currentPage, paperId)
                            },
                            onDismiss = { selectedTextRange = null }
                        )
                    }

                    // Active Sticky Note Overlay Card
                    if (activeStickyNote != null) {
                        ActiveStickyNoteCard(
                            annotation = activeStickyNote!!,
                            onDelete = { viewModel.deleteAnnotation(activeStickyNote!!.id) },
                            onDismiss = { viewModel.setActiveStickyNote(null) }
                        )
                    }
                }

                // Page Thumbnail Scrub Bar
                PageThumbnailScrubBar(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onPageSelected = { viewModel.setPage(it) }
                )
            }
        }

        // Sticky Note Commentary Dialog
        if (showStickyNoteDialog) {
            AlertDialog(
                onDismissRequest = { showStickyNoteDialog = false },
                title = { Text("Add Marginal Sticky Note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "Page $currentPage coordinate note",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.ccColors.marginGray
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newNoteText,
                            onValueChange = { newNoteText = it },
                            placeholder = { Text("Enter marginal study commentary…") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newNoteText.isNotBlank()) {
                                viewModel.addStickyNote(
                                    pageNumber = currentPage,
                                    noteText = newNoteText,
                                    selectedText = selectedTextRange ?: "",
                                    xRatio = noteTapPosition.x,
                                    yRatio = noteTapPosition.y
                                )
                                newNoteText = ""
                            }
                            showStickyNoteDialog = false
                            selectedTextRange = null
                        }
                    ) {
                        Text("Attach Note", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStickyNoteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // AI Executive Summarizer Bottom Sheet Drawer
        if (showAiSheet) {
            AiSummarizerBottomSheet(
                breakdown = aiBreakdown,
                isLoading = isLoadingBreakdown,
                paperTitle = paper?.title ?: "Preprint",
                onDismiss = { viewModel.setShowAiSheet(false) },
                onShareQuote = { quote ->
                    viewModel.setShowAiSheet(false)
                    onShareQuoteToFeed(quote, currentPage, paperId)
                }
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Canvas Page Render View with Page & Highlight Rendering Engine
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun CanvasPageRenderView(
    currentPage: Int,
    paperTitle: String,
    paperAbstract: String,
    annotations: List<PaperAnnotation>,
    zoomScale: Float,
    offset: Offset,
    onStickyNoteClick: (PaperAnnotation) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // High-Performance Styled Canvas Paper Sheet View
        Card(
            modifier = Modifier
                .aspectRatio(0.707f) // Standard ISO A4 paper ratio
                .fillMaxHeight(0.85f)
                .shadow(16.dp, RoundedCornerShape(8.dp))
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = offset.x
                    translationY = offset.y
                },
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF7F2)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background paper canvas & page highlights overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pageW = size.width
                    val pageH = size.height

                    // Draw Annotations highlights onto canvas
                    annotations.forEach { ann ->
                        val color = Color(ann.color.hex).copy(alpha = 0.45f)
                        val startY = pageH * ann.yRatio
                        drawRect(
                            color = color,
                            topLeft = Offset(pageW * 0.1f, startY),
                            size = Size(pageW * 0.8f, 28f)
                        )
                    }
                }

                // Render Simulated PDF Page Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp)
                ) {
                    Text(
                        text = "PREPRINT MANUSCRIPT • PAGE $currentPage",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = if (currentPage == 1) paperTitle else "Section $currentPage: Empirical Analysis & Methodology",
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FrauncesFamily),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E2B42)
                    )
                    Spacer(Modifier.height(16.dp))

                    val pageText = if (currentPage == 1) {
                        paperAbstract.ifEmpty {
                            "Abstract: This research introduces a high-performance framework for preprint analysis and annotation..."
                        }
                    } else {
                        "Detailed breakdown of experimental protocol, statistical cross-validation, and performance metrics evaluated across multi-node infrastructure. Results confirm significant throughput enhancement."
                    }

                    Text(
                        text = pageText,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 20.sp,
                        color = Color(0xFF2C3E50)
                    )

                    Spacer(Modifier.weight(1f))

                    Text(
                        text = "Cite Circle Research Studio • Page $currentPage",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Render Sticky Note Pins at exact coordinates (xRatio, yRatio)
                annotations.filter { it.noteText.isNotBlank() }.forEach { ann ->
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (260 * ann.xRatio).dp,
                                y = (400 * ann.yRatio).dp
                            )
                            .size(32.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(ann.color.hex))
                            .clickable { onStickyNoteClick(ann) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.StickyNote2,
                            contentDescription = "Sticky Note",
                            tint = CcColors.InkNavy,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Highlighter Palette Floating Bar
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun HighlighterPaletteBar(
    selectedColor: AnnotationColor,
    onColorSelect: (AnnotationColor) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Highlighter Color",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.ccColors.marginGray
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnnotationColor.entries.forEach { color ->
                    val isSelected = selectedColor == color
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(color.hex))
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) CcColors.InkNavy else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onColorSelect(color) }
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Contextual Annotation Popup Menu
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ContextualAnnotationMenu(
    offset: Offset,
    selectedColor: AnnotationColor,
    onHighlight: () -> Unit,
    onAddStickyNote: () -> Unit,
    onShareQuote: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .offset(x = 24.dp, y = 180.dp)
            .shadow(12.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = CcColors.InkNavy
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onHighlight) {
                Text("Highlight", color = Color(selectedColor.hex), fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onAddStickyNote) {
                Text("Sticky Note", color = Color.White)
            }
            TextButton(onClick = onShareQuote) {
                Text("Share Quote", color = CcColors.HighlighterYellow, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.Gray)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Active Sticky Note Card Overlay
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActiveStickyNoteCard(
    annotation: PaperAnnotation,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(annotation.color.hex)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Marginal Note • Page ${annotation.pageNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = CcColors.InkNavy
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close Note", tint = CcColors.InkNavy)
                }
            }
            Text(
                text = annotation.noteText,
                style = MaterialTheme.typography.bodyMedium,
                color = CcColors.InkNavy
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDelete) {
                    Text("Delete Note", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Page Thumbnail Scrub Bar
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun PageThumbnailScrubBar(
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)) {
            Text(
                text = "THUMBNAIL SCRUB BAR",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                color = MaterialTheme.ccColors.marginGray,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(totalPages) { idx ->
                    val pageNum = idx + 1
                    val isCurrent = pageNum == currentPage
                    Card(
                        modifier = Modifier
                            .width(44.dp)
                            .height(56.dp)
                            .clickable { onPageSelected(pageNum) }
                            .border(
                                width = if (isCurrent) 2.dp else 0.dp,
                                color = if (isCurrent) CcColors.CircleBlue else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$pageNum",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) CcColors.CircleBlue else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
