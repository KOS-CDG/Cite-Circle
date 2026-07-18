package com.citecircle.app.feature.papers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citecircle.app.core.data.PaperRepository
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.Paper
import com.citecircle.app.feature.publish.ReadinessGaugeCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────────────────────
// PaperDetailViewModel
// ──────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class PaperDetailViewModel @Inject constructor(
    private val paperRepository: PaperRepository
) : ViewModel() {

    private val _paper = MutableStateFlow<Paper?>(null)
    val paper: StateFlow<Paper?> = _paper.asStateFlow()

    fun loadPaperData(paperId: String) {
        viewModelScope.launch {
            paperRepository.getPaperById(paperId).collect {
                _paper.value = it
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
    viewModel: PaperDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(paperId) {
        viewModel.loadPaperData(paperId)
    }

    val paper by viewModel.paper.collectAsState()
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
                        Spacer(modifier = Modifier.height(20.dp))
                    }

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
                    Row(modifier = Modifier.fillMaxWidth()) {
                        CcPrimaryButton(
                            text = "Cite Paper",
                            onClick = {
                                val textCitation = "${p.authors.firstOrNull()?.name ?: "Unknown"} et al. (${p.year}). ${p.title}. ${p.journal}. DOI: ${p.doi}"
                                clipboardManager.setText(AnnotatedString(textCitation))
                            },
                            icon = Icons.Outlined.ContentCopy,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )

                        CcSecondaryButton(
                            text = "Download PDF",
                            onClick = {},
                            icon = Icons.Outlined.Description,
                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
