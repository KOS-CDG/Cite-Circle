package com.citecircle.app.feature.publish

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citecircle.app.core.data.CircleRepository
import com.citecircle.app.core.data.PaperRepository
import com.citecircle.app.core.data.PostRepository
import com.citecircle.app.core.data.UserRepository
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────────────────────
// QuickPostViewModel
// ──────────────────────────────────────────────────────────────────────────────

const val QUICK_POST_MAX_CHARS = 500
const val QUICK_POST_WARN_CHARS = 480

private val QUICK_POST_DRAFT_KEY = stringPreferencesKey("quick_post_draft")

enum class PostAudience(val label: String) {
    PUBLIC("Public"),
    FOLLOWERS("Followers Only"),
    CIRCLE("Circle")
}

data class QuickPostUiState(
    val text: String = "",
    val flair: PostFlair = PostFlair.NONE,
    val audience: PostAudience = PostAudience.PUBLIC,
    val selectedCircle: Circle? = null,
    val citedPaper: Paper? = null,
    val isPosting: Boolean = false,
    val createdPost: Post? = null
) {
    val canSubmit: Boolean get() = text.isNotBlank() && !isPosting && createdPost == null
}

@HiltViewModel
class QuickPostViewModel @Inject constructor(
    private val postRepository: PostRepository,
    userRepository: UserRepository,
    circleRepository: CircleRepository,
    paperRepository: PaperRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickPostUiState())
    val uiState = _uiState.asStateFlow()

    val currentUser = userRepository.getCurrentUser()
        .stateIn<User?>(viewModelScope, SharingStarted.Eagerly, null)

    val joinedCircles = circleRepository.getJoinedCircles()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val papers = paperRepository.getAllPapers()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var draftSaveJob: Job? = null

    init {
        // Restore any auto-saved draft
        viewModelScope.launch {
            val savedDraft = dataStore.data.first()[QUICK_POST_DRAFT_KEY].orEmpty()
            if (savedDraft.isNotEmpty()) {
                _uiState.update { state ->
                    if (state.text.isEmpty()) state.copy(text = savedDraft) else state
                }
            }
        }
    }

    fun onTextChange(value: String) {
        val clamped = value.take(QUICK_POST_MAX_CHARS)
        _uiState.update { it.copy(text = clamped) }
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            delay(400)
            dataStore.edit { prefs -> prefs[QUICK_POST_DRAFT_KEY] = clamped }
        }
    }

    fun onFlairSelected(flair: PostFlair) {
        _uiState.update {
            it.copy(flair = if (it.flair == flair) PostFlair.NONE else flair)
        }
    }

    fun onAudienceSelected(audience: PostAudience) {
        _uiState.update {
            if (audience == PostAudience.CIRCLE) it.copy(audience = audience)
            else it.copy(audience = audience, selectedCircle = null)
        }
    }

    fun onCircleSelected(circle: Circle) {
        _uiState.update { it.copy(audience = PostAudience.CIRCLE, selectedCircle = circle) }
    }

    fun onPaperCited(paper: Paper) {
        _uiState.update { it.copy(citedPaper = paper) }
    }

    fun onCitedPaperRemoved() {
        _uiState.update { it.copy(citedPaper = null) }
    }

    fun submitPost() {
        val author = currentUser.value ?: return
        val state = _uiState.value
        if (!state.canSubmit) return

        _uiState.update { it.copy(isPosting = true) }
        viewModelScope.launch {
            val post = Post(
                id = "post_qp_${System.currentTimeMillis()}",
                author = author,
                content = state.text.trim(),
                type = if (state.citedPaper != null) PostType.PAPER_SHARE else PostType.DISCUSSION,
                flair = state.flair,
                circleId = state.selectedCircle?.id,
                circleName = state.selectedCircle?.name,
                attachedPaper = state.citedPaper
            )
            val success = postRepository.createPost(post)
            if (success) {
                draftSaveJob?.cancel()
                dataStore.edit { prefs -> prefs.remove(QUICK_POST_DRAFT_KEY) }
                _uiState.update { it.copy(isPosting = false, createdPost = post) }
            } else {
                _uiState.update { it.copy(isPosting = false) }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// QuickPostScreen
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPostScreen(
    onDismiss: () -> Unit,
    onPostCreated: (Post) -> Unit,
    onViewPost: (String) -> Unit = {},
    viewModel: QuickPostViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val joinedCircles by viewModel.joinedCircles.collectAsState()
    val papers by viewModel.papers.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }

    var showPaperPicker by remember { mutableStateOf(false) }
    var showCirclePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Show confirmation snackbar once the post lands, then leave the composer
    LaunchedEffect(uiState.createdPost) {
        val post = uiState.createdPost ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Post shared!",
            actionLabel = "View Post",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            onViewPost(post.id)
        } else {
            onPostCreated(post)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Create post",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.semantics { contentDescription = "Close post composer" }
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.submitPost() },
                        enabled = uiState.canSubmit,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .semantics { contentDescription = "Share post" }
                    ) {
                        if (uiState.isPosting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Post", fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // ── Author row + audience selector ──
            currentUser?.let { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CcAvatar(user = user, size = 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        AudienceSelector(
                            audience = uiState.audience,
                            selectedCircle = uiState.selectedCircle,
                            onAudienceSelected = viewModel::onAudienceSelected,
                            onPickCircle = { showCirclePicker = true }
                        )
                    }
                }
            }

            // ── Text input ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                BasicTextField(
                    value = uiState.text,
                    onValueChange = viewModel::onTextChange,
                    enabled = uiState.createdPost == null,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 26.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 160.dp)
                        .focusRequester(focusRequester)
                        .semantics { contentDescription = "Post text input" },
                    decorationBox = { innerTextField ->
                        Box {
                            if (uiState.text.isEmpty()) {
                                Text(
                                    text = "What's on your research mind?",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.ccColors.marginGray
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            // ── Cited paper preview ──
            uiState.citedPaper?.let { paper ->
                CitedPaperCard(
                    paper = paper,
                    onRemove = viewModel::onCitedPaperRemoved,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // ── Flair chips ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                flairOptions.forEach { (flair, label) ->
                    CcChip(
                        label = label,
                        selected = uiState.flair == flair,
                        onClick = { viewModel.onFlairSelected(flair) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.ccColors.divider)

            // ── Bottom toolbar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        Toast.makeText(context, "PDF attachments coming soon", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.semantics { contentDescription = "Attach PDF" }
                ) {
                    Icon(
                        Icons.Outlined.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.ccColors.marginGray
                    )
                }
                IconButton(
                    onClick = {
                        Toast.makeText(context, "Image picker coming soon", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.semantics { contentDescription = "Add image" }
                ) {
                    Icon(
                        Icons.Outlined.Image,
                        contentDescription = null,
                        tint = MaterialTheme.ccColors.marginGray
                    )
                }
                IconButton(
                    onClick = { showPaperPicker = true },
                    modifier = Modifier.semantics { contentDescription = "Cite a paper" }
                ) {
                    Icon(
                        Icons.Outlined.FormatQuote,
                        contentDescription = null,
                        tint = MaterialTheme.ccColors.marginGray
                    )
                }

                Spacer(Modifier.weight(1f))

                // Live character counter — turns coral near the limit
                Text(
                    text = "${uiState.text.length}/$QUICK_POST_MAX_CHARS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (uiState.text.length >= QUICK_POST_WARN_CHARS) CcColors.CoralPop
                    else MaterialTheme.ccColors.marginGray,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        }
    }

    // ── Paper picker sheet ──
    if (showPaperPicker) {
        ModalBottomSheet(
            onDismissRequest = { showPaperPicker = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            PaperPickerSheetContent(
                papers = papers,
                onPaperSelected = { paper ->
                    viewModel.onPaperCited(paper)
                    showPaperPicker = false
                }
            )
        }
    }

    // ── Circle picker sheet ──
    if (showCirclePicker) {
        ModalBottomSheet(
            onDismissRequest = { showCirclePicker = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            CirclePickerSheetContent(
                circles = joinedCircles,
                onCircleSelected = { circle ->
                    viewModel.onCircleSelected(circle)
                    showCirclePicker = false
                }
            )
        }
    }
}

private val flairOptions = listOf(
    PostFlair.QUESTION to "❓ Question",
    PostFlair.DISCUSSION to "💬 Discussion",
    PostFlair.PAPER_FEEDBACK to "📄 Paper Feedback",
    PostFlair.RESOURCE to "📚 Resource"
)

// ──────────────────────────────────────────────────────────────────────────────
// Audience selector pill
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun AudienceSelector(
    audience: PostAudience,
    selectedCircle: Circle?,
    onAudienceSelected: (PostAudience) -> Unit,
    onPickCircle: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val icon = when (audience) {
        PostAudience.PUBLIC -> Icons.Outlined.Public
        PostAudience.FOLLOWERS -> Icons.Outlined.People
        PostAudience.CIRCLE -> Icons.Outlined.Groups
    }
    val label = when {
        audience == PostAudience.CIRCLE && selectedCircle != null ->
            "${selectedCircle.iconEmoji} ${selectedCircle.name}"
        else -> audience.label
    }

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.ccColors.divider, RoundedCornerShape(16.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .semantics { contentDescription = "Select post audience" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Public") },
                leadingIcon = { Icon(Icons.Outlined.Public, contentDescription = null) },
                onClick = {
                    onAudienceSelected(PostAudience.PUBLIC)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Followers Only") },
                leadingIcon = { Icon(Icons.Outlined.People, contentDescription = null) },
                onClick = {
                    onAudienceSelected(PostAudience.FOLLOWERS)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Post to a Circle…") },
                leadingIcon = { Icon(Icons.Outlined.Groups, contentDescription = null) },
                onClick = {
                    expanded = false
                    onPickCircle()
                }
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Cited paper preview card
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun CitedPaperCard(
    paper: Paper,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    CcCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.ccColors.highlighterYellowAlpha),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    tint = MaterialTheme.ccColors.inkNavy,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = paper.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${paper.journal} • ${paper.year}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.ccColors.marginGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier.semantics { contentDescription = "Remove cited paper" }
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = CcColors.CoralPop)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Picker sheets
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun PaperPickerSheetContent(
    papers: List<Paper>,
    onPaperSelected: (Paper) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = "Cite a paper",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FrauncesFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        if (papers.isEmpty()) {
            CcEmptyState(
                emoji = "📄",
                title = "No papers yet",
                subtitle = "Papers shared on CiteCircle will show up here."
            )
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(papers, key = { it.id }) { paper ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPaperSelected(paper) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = paper.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${paper.journal} • ${paper.year} • ${paper.citationCount} citations",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.ccColors.marginGray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CirclePickerSheetContent(
    circles: List<Circle>,
    onCircleSelected: (Circle) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = "Post to a Circle",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FrauncesFamily,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        if (circles.isEmpty()) {
            CcEmptyState(
                emoji = "🫧",
                title = "No Circles joined",
                subtitle = "Join a Circle first to share posts with its members."
            )
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(circles, key = { it.id }) { circle ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCircleSelected(circle) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(circle.bannerColor).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(circle.iconEmoji, fontSize = 20.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = circle.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${circle.memberCount} members",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.ccColors.marginGray
                            )
                        }
                    }
                }
            }
        }
    }
}
