package com.citecircle.app.feature.circles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citecircle.app.core.data.CircleRepository
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import javax.inject.Inject


// ──────────────────────────────────────────────────────────────────────────────
// ViewModel
// ──────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class DraftDetailViewModel @Inject constructor(
    private val circleRepository: CircleRepository
) : ViewModel() {

    private val _draftId = MutableStateFlow("")
    private val _draft = MutableStateFlow<CircleDraft?>(null)
    private val _comments = MutableStateFlow<List<DraftComment>>(emptyList())
    private val _reviewRequests = MutableStateFlow<List<DraftReviewRequest>>(emptyList())
    private val _members = MutableStateFlow<List<CircleMember>>(emptyList())

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    val state: StateFlow<DraftDetailState> = combine(
        _draft, _comments, _reviewRequests, _members
    ) { draft: CircleDraft?, comments: List<DraftComment>, reviewRequests: List<DraftReviewRequest>, members: List<CircleMember> ->
        if (draft == null) {
            DraftDetailState.Loading
        } else {
            DraftDetailState.Success(
                draft = draft,
                comments = comments,
                reviewRequests = reviewRequests,
                members = members
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DraftDetailState.Loading)

    fun loadDraftData(draftId: String) {
        _draftId.value = draftId
        viewModelScope.launch {
            circleRepository.getDraftDetails(draftId).collect { d ->
                _draft.value = d
                if (d != null) {
                    circleRepository.getCircleMembers(d.circleId).collect { _members.value = it }
                }
            }
        }
        viewModelScope.launch {
            circleRepository.getDraftComments(draftId).collect { _comments.value = it }
        }
        viewModelScope.launch {
            circleRepository.getDraftReviewRequests(draftId).collect { _reviewRequests.value = it }
        }
    }

    fun addComment(sectionIndex: Int, paragraphOffset: Int, content: String) {
        val did = _draftId.value
        viewModelScope.launch {
            circleRepository.addDraftComment(did, sectionIndex, paragraphOffset, content)
            loadDraftData(did)
            _toastMessage.value = "Inline comment posted!"
        }
    }

    fun resolveComment(commentId: String) {
        val did = _draftId.value
        viewModelScope.launch {
            circleRepository.resolveDraftComment(commentId)
            loadDraftData(did)
            _toastMessage.value = "Comment thread resolved."
        }
    }

    fun requestReview(reviewerId: String, sectionTarget: String, notes: String) {
        val did = _draftId.value
        viewModelScope.launch {
            circleRepository.requestDraftReview(did, reviewerId, sectionTarget, notes)
            loadDraftData(did)
            _toastMessage.value = "Co-author review request sent!"
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}

sealed interface DraftDetailState {
    object Loading : DraftDetailState
    data class Success(
        val draft: CircleDraft,
        val comments: List<DraftComment>,
        val reviewRequests: List<DraftReviewRequest>,
        val members: List<CircleMember>
    ) : DraftDetailState
}

// ──────────────────────────────────────────────────────────────────────────────
// DraftDetailScreen Composable
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftDetailScreen(
    draftId: String,
    onBack: () -> Unit,
    viewModel: DraftDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(draftId) {
        viewModel.loadDraftData(draftId)
    }

    val state by viewModel.state.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    var selectedSectionIndex by remember { mutableStateOf(0) }
    var activeCommentDrawerSectionIndex by remember { mutableStateOf<Int?>(null) }
    var showRequestReviewDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Manuscript Review", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.ccColors.paperCream)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.ccColors.paperCream)
                .padding(padding)
        ) {
            when (val detailState = state) {
                is DraftDetailState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is DraftDetailState.Success -> {
                    val draft = detailState.draft
                    val sections = if (draft.sections.isNotEmpty()) draft.sections else listOf(
                        "Abstract",
                        "1. Introduction & Background",
                        "2. System Architecture & Methodology",
                        "3. Empirical Evaluation & Results",
                        "4. Discussion & Ethical Considerations"
                    )

                    Column(modifier = Modifier.fillMaxSize()) {
                        // ── Header Card ──
                        CcCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                when (draft.status) {
                                                    DraftStatus.UNDER_REVIEW -> CcColors.HighlighterYellow
                                                    DraftStatus.REVISION -> Color(0xFFFF9800)
                                                    DraftStatus.READY_TO_SUBMIT -> Color(0xFF4CAF50)
                                                    else -> MaterialTheme.ccColors.marginGray.copy(alpha = 0.2f)
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = draft.status.displayName(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = CcColors.InkNavy
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.Gray.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(text = draft.fileFormat.name, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Text(text = draft.version, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ccColors.marginGray)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = draft.title,
                                    fontFamily = FrauncesFamily,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.Gray.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = draft.leadAuthorName.take(1).ifBlank { "L" }, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Lead Author: ${draft.leadAuthorName}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.ccColors.marginGray
                                    )
                                }
                            }
                        }

                        // ── Co-Author Peer Review Bar ──
                        CcCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Group, contentDescription = null, tint = CcColors.CircleBlue, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Co-Author Reviews (${detailState.reviewRequests.size})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (detailState.reviewRequests.isNotEmpty()) "${detailState.reviewRequests.first().reviewerName} (${detailState.reviewRequests.first().status.name})" else "No active review requests",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.ccColors.marginGray
                                        )
                                    }
                                }

                                Button(
                                    onClick = { showRequestReviewDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = CcColors.CircleBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Invite Reviewer", fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // ── Section Selector Chips ──
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(sections) { idx, sectionName ->
                                val countForSec = detailState.comments.count { it.sectionIndex == idx }
                                FilterChip(
                                    selected = (selectedSectionIndex == idx),
                                    onClick = { selectedSectionIndex = idx },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = sectionName)
                                            if (countForSec > 0) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(CircleShape)
                                                        .background(CcColors.HighlighterYellow)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(text = "$countForSec", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CcColors.InkNavy)
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // ── Section Content Viewer with Inline Comment Anchor ──
                        val activeSectionName = sections.getOrElse(selectedSectionIndex) { "Section" }
                        val commentsForSection = detailState.comments.filter { it.sectionIndex == selectedSectionIndex }

                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            item {
                                CcCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Text(
                                            text = activeSectionName,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontFamily = FrauncesFamily,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Paragraph 1 Text
                                        Text(
                                            text = getSampleSectionParagraph1(activeSectionName, draft),
                                            style = MaterialTheme.typography.bodyLarge,
                                            lineHeight = 26.sp
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Paragraph 2 Text
                                        Text(
                                            text = getSampleSectionParagraph2(activeSectionName),
                                            style = MaterialTheme.typography.bodyLarge,
                                            lineHeight = 26.sp
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Inline Section Comments Trigger Bar
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(CcColors.HighlighterYellow.copy(alpha = 0.2f))
                                                .border(1.dp, CcColors.HighlighterYellow, RoundedCornerShape(8.dp))
                                                .clickable { activeCommentDrawerSectionIndex = selectedSectionIndex }
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Comment, contentDescription = null, tint = CcColors.InkNavy, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "${commentsForSection.size} Section Comments (${commentsForSection.count { it.isResolved }} resolved)",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CcColors.InkNavy
                                                )
                                            }

                                            Text(text = "View / Comment →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CcColors.InkNavy)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Inline Comment Drawer Bottom Sheet Overlay ──
                    if (activeCommentDrawerSectionIndex != null) {
                        val secIdx = activeCommentDrawerSectionIndex!!
                        val secName = sections.getOrElse(secIdx) { "Section" }
                        val commentsForSec = detailState.comments.filter { it.sectionIndex == secIdx }

                        InlineCommentDrawerSheet(
                            sectionName = secName,
                            comments = commentsForSec,
                            onDismiss = { activeCommentDrawerSectionIndex = null },
                            onPostComment = { content ->
                                viewModel.addComment(secIdx, 0, content)
                            },
                            onResolveComment = { cid ->
                                viewModel.resolveComment(cid)
                            }
                        )
                    }

                    // ── Request Review Dialog ──
                    if (showRequestReviewDialog) {
                        RequestReviewDialog(
                            members = detailState.members,
                            sections = sections,
                            onDismiss = { showRequestReviewDialog = false },
                            onRequestReview = { reviewerId, target, notes ->
                                viewModel.requestReview(reviewerId, target, notes)
                                showRequestReviewDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Inline Comment Drawer Sheet
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InlineCommentDrawerSheet(
    sectionName: String,
    comments: List<DraftComment>,
    onDismiss: () -> Unit,
    onPostComment: (String) -> Unit,
    onResolveComment: (String) -> Unit
) {
    var newCommentText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Section Comments: $sectionName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No comments anchored to this section yet. Post the first feedback below!")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                ) {
                    items(comments, key = { it.id }) { comment ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (comment.isResolved) Color.Gray.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)
                                .border(1.dp, if (comment.isResolved) Color.Transparent else CcColors.HighlighterYellow, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = comment.authorName.take(1).ifBlank { "U" }, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = comment.authorName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    if (comment.isResolved) {
                                        Text(text = "✓ Resolved", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                    } else {
                                        TextButton(onClick = { onResolveComment(comment.id) }, contentPadding = PaddingValues(0.dp)) {
                                            Text(text = "Resolve", fontSize = 11.sp)
                                        }
                                    }
                                }

                                Text(text = comment.content, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Post comment input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    placeholder = { Text("Write inline comment...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (newCommentText.isNotBlank()) {
                            onPostComment(newCommentText)
                            newCommentText = ""
                        }
                    },
                    enabled = newCommentText.isNotBlank()
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Post Comment", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Request Review Dialog
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun RequestReviewDialog(
    members: List<CircleMember>,
    sections: List<String>,
    onDismiss: () -> Unit,
    onRequestReview: (reviewerId: String, sectionTarget: String, notes: String) -> Unit
) {
    var selectedReviewerId by remember { mutableStateOf(members.firstOrNull()?.userId ?: "") }
    var selectedTargetSection by remember { mutableStateOf(sections.firstOrNull() ?: "Full Manuscript") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Request Co-Author Peer Review", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Select Reviewer from Circle:", style = MaterialTheme.typography.labelSmall)
                members.forEach { m ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReviewerId = m.userId }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (selectedReviewerId == m.userId), onClick = { selectedReviewerId = m.userId })
                        Text(text = "${m.name} (${m.role.displayName()})", style = MaterialTheme.typography.bodySmall)
                    }
                }

                OutlinedTextField(
                    value = selectedTargetSection,
                    onValueChange = { selectedTargetSection = it },
                    label = { Text("Target Section for Review") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reviewer Notes & Instructions") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedReviewerId.isNotBlank()) {
                        onRequestReview(selectedReviewerId, selectedTargetSection, notes)
                    }
                },
                enabled = selectedReviewerId.isNotBlank()
            ) {
                Text("Send Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper methods for generating realistic draft manuscript text
private fun getSampleSectionParagraph1(sectionName: String, draft: CircleDraft): String {
    return when {
        sectionName.contains("Abstract", ignoreCase = true) -> draft.abstract.ifBlank {
            "As generative AI and collaborative writing platforms become deeply integrated into scientific research workflows, questions arise concerning cognitive load, trust calibration, and epistemic distribution between human authors and intelligent assistants."
        }
        sectionName.contains("Introduction", ignoreCase = true) ->
            "Recent advancements in distributed research environments have transformed how academic lab groups iterate on preprints and working drafts. While traditional manuscript workflows rely on asynchronous PDF email attachments, real-time section commenting and co-author peer review lower friction and improve publication quality."
        sectionName.contains("Methodology", ignoreCase = true) ->
            "We conducted a mixed-methods empirical evaluation across 12 university research labs over a 6-month period. Participants were assigned to write, review, and annotate working manuscripts using our collaborative research workspace."
        sectionName.contains("Evaluation", ignoreCase = true) ->
            "Empirical results demonstrated a 34% reduction in manuscript revision cycle times and a statistically significant increase in peer reviewer satisfaction scores (p < 0.001)."
        else ->
            "This section analyzes the key implications of collaborative draft authoring for scientific communities, highlighting best practices for open peer review and continuous feedback loops."
    }
}

private fun getSampleSectionParagraph2(sectionName: String): String {
    return "Furthermore, inline section comments anchored directly to paragraph offsets enable lead authors to address specific methodological critiques without losing contextual clarity during pre-submission review iterations."
}
