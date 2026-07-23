package com.citecircle.app.feature.circles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citecircle.app.core.data.CircleRepository
import com.citecircle.app.core.data.PaperRepository
import com.citecircle.app.core.data.PostRepository
import com.citecircle.app.core.data.UserRepository
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.*
import com.citecircle.app.feature.feed.PostCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow

// ──────────────────────────────────────────────────────────────────────────────
// CircleDetailViewModel
// ──────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class CircleDetailViewModel @Inject constructor(
    private val circleRepository: CircleRepository,
    private val postRepository: PostRepository,
    private val paperRepository: PaperRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _circleId = MutableStateFlow("")
    private val _circle = MutableStateFlow<Circle?>(null)
    private val _workspace = MutableStateFlow<CircleWorkspace?>(null)
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    private val _papers = MutableStateFlow<List<Paper>>(emptyList())
    private val _readingLists = MutableStateFlow<List<CircleReadingList>>(emptyList())
    private val _drafts = MutableStateFlow<List<CircleDraft>>(emptyList())
    private val _members = MutableStateFlow<List<CircleMember>>(emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    val state: StateFlow<CircleDetailState> = combine(
        listOf(_circle, _workspace, _posts, _papers, _readingLists, _drafts, _members)
    ) { args: Array<Any?> ->
        val circle = args[0] as? Circle
        val workspace = args[1] as? CircleWorkspace
        @Suppress("UNCHECKED_CAST")
        val posts = (args[2] as? List<Post>) ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val papers = (args[3] as? List<Paper>) ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val readingLists = (args[4] as? List<CircleReadingList>) ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val drafts = (args[5] as? List<CircleDraft>) ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val members = (args[6] as? List<CircleMember>) ?: emptyList()

        if (circle == null) CircleDetailState.Loading
        else CircleDetailState.Success(
            circle = circle,
            workspace = workspace ?: CircleWorkspace(id = "ws_${circle.id}", circleId = circle.id),
            posts = posts,
            papers = papers,
            readingLists = readingLists,
            drafts = drafts,
            members = members
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CircleDetailState.Loading)


    fun loadCircleData(circleId: String) {
        _circleId.value = circleId
        viewModelScope.launch {
            circleRepository.getCircleById(circleId).collect { _circle.value = it }
        }
        viewModelScope.launch {
            circleRepository.getCircleWorkspace(circleId).collect { _workspace.value = it }
        }
        viewModelScope.launch {
            postRepository.getPostsForCircle(circleId).collect { _posts.value = it }
        }
        viewModelScope.launch {
            paperRepository.getPapersForCircle(circleId).collect { _papers.value = it }
        }
        viewModelScope.launch {
            circleRepository.getCircleReadingLists(circleId).collect { _readingLists.value = it }
        }
        viewModelScope.launch {
            circleRepository.getCircleDrafts(circleId).collect { _drafts.value = it }
        }
        viewModelScope.launch {
            circleRepository.getCircleMembers(circleId).collect { _members.value = it }
        }
    }

    fun refreshCircleData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadCircleData(_circleId.value)
            delay(600)
            _isRefreshing.value = false
        }
    }

    fun toggleJoinCircle() {
        val circle = _circle.value ?: return
        viewModelScope.launch {
            if (circle.isJoined) {
                circleRepository.leaveCircle(circle.id)
            } else {
                circleRepository.joinCircle(circle.id)
            }
            loadCircleData(circle.id)
        }
    }

    fun updatePinBoard(text: String) {
        val cid = _circleId.value
        viewModelScope.launch {
            circleRepository.updatePinboard(cid, text)
            loadCircleData(cid)
            _toastMessage.value = "Pin board updated!"
        }
    }

    fun updateMemberRole(userId: String, role: CircleRole) {
        val cid = _circleId.value
        viewModelScope.launch {
            circleRepository.updateMemberRole(cid, userId, role)
            loadCircleData(cid)
            _toastMessage.value = "Member role updated to ${role.displayName()}"
        }
    }

    fun generateInviteCode(onCodeGenerated: (String) -> Unit) {
        val cid = _circleId.value
        viewModelScope.launch {
            val code = circleRepository.generateInviteCode(cid)
            onCodeGenerated(code)
            loadCircleData(cid)
        }
    }

    fun createDraft(title: String, abstract: String, format: DraftFormat) {
        val cid = _circleId.value
        viewModelScope.launch {
            circleRepository.createCircleDraft(cid, title, abstract, format)
            loadCircleData(cid)
            _toastMessage.value = "New draft manuscript created!"
        }
    }

    fun createReadingList(title: String, description: String, paperIds: List<String>) {
        val cid = _circleId.value
        viewModelScope.launch {
            circleRepository.createCircleReadingList(cid, title, description, paperIds)
            loadCircleData(cid)
            _toastMessage.value = "Shared reading list created!"
        }
    }

    fun saveReadingListToMyLibrary(readingListId: String) {
        viewModelScope.launch {
            circleRepository.saveReadingListToMyLibrary(readingListId)
            _toastMessage.value = "Saved all papers in reading list to your library!"
        }
    }

    fun endorsePost(postId: String) {
        viewModelScope.launch {
            postRepository.endorsePost(postId)
            loadCircleData(_circleId.value)
        }
    }

    fun savePost(postId: String) {
        viewModelScope.launch {
            postRepository.savePost(postId)
            loadCircleData(_circleId.value)
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}

sealed interface CircleDetailState {
    object Loading : CircleDetailState
    data class Success(
        val circle: Circle,
        val workspace: CircleWorkspace,
        val posts: List<Post>,
        val papers: List<Paper>,
        val readingLists: List<CircleReadingList>,
        val drafts: List<CircleDraft>,
        val members: List<CircleMember>
    ) : CircleDetailState
}

// ──────────────────────────────────────────────────────────────────────────────
// CircleDetailScreen Composable
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleDetailScreen(
    circleId: String,
    onBack: () -> Unit,
    onPostClick: (String) -> Unit,
    onPaperClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onDraftClick: (String) -> Unit = {},
    viewModel: CircleDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(circleId) {
        viewModel.loadCircleData(circleId)
    }

    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    var showPostComposer by remember { mutableStateOf(false) }
    var showCreateDraftSheet by remember { mutableStateOf(false) }
    var showCreateReadingListSheet by remember { mutableStateOf(false) }
    var showEditPinboardDialog by remember { mutableStateOf(false) }
    var generatedInviteCode by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if ((state as? CircleDetailState.Success)?.circle?.isJoined == true && selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = { showPostComposer = true },
                    containerColor = CcColors.HighlighterYellow
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Post to circle", tint = CcColors.InkNavy)
                }
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshCircleData() },
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.ccColors.paperCream)
                .padding(paddingValues)
        ) {
            when (val detailState = state) {
                is CircleDetailState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is CircleDetailState.Success -> {
                    val circle = detailState.circle

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Custom Header Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .background(Color(circle.bannerColor))
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.3f))
                            ) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }

                            // Large overlay emoji icon
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .border(2.dp, Color(circle.bannerColor), RoundedCornerShape(16.dp))
                                    .align(Alignment.BottomStart)
                                    .offset(x = 20.dp, y = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = circle.iconEmoji, fontSize = 38.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(26.dp))

                        // Info Section
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = circle.name,
                                    fontFamily = FrauncesFamily,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "${circle.memberCount} members • ${circle.category}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.ccColors.marginGray
                                )
                            }

                            Button(
                                onClick = { viewModel.toggleJoinCircle() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (circle.isJoined) MaterialTheme.ccColors.marginGray else MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (circle.isJoined) Icons.Filled.Check else Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = if (circle.isJoined) "Joined" else "Join")
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 4 Primary Workspace Tabs: Feed / Papers / Workspace / Members
                        CcTabRow(
                            tabs = listOf("Feed", "Papers", "Workspace", "Members"),
                            selectedIndex = selectedTabIndex,
                            onTabSelected = { selectedTabIndex = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Tab Content Switcher
                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedTabIndex) {
                                0 -> FeedTabContent(
                                    posts = detailState.posts,
                                    onPostClick = onPostClick,
                                    onUserClick = onUserClick,
                                    onEndorse = { viewModel.endorsePost(it) },
                                    onSave = { viewModel.savePost(it) }
                                )
                                1 -> PapersTabContent(
                                    papers = detailState.papers,
                                    readingLists = detailState.readingLists,
                                    onPaperClick = onPaperClick,
                                    onCreateReadingList = { showCreateReadingListSheet = true },
                                    onSaveReadingList = { viewModel.saveReadingListToMyLibrary(it) }
                                )
                                2 -> WorkspaceTabContent(
                                    circle = circle,
                                    workspace = detailState.workspace,
                                    drafts = detailState.drafts,
                                    onDraftClick = onDraftClick,
                                    onCreateDraft = { showCreateDraftSheet = true },
                                    onEditPinboard = { showEditPinboardDialog = true }
                                )
                                3 -> MembersTabContent(
                                    members = detailState.members,
                                    workspace = detailState.workspace,
                                    onUserClick = onUserClick,
                                    onUpdateRole = { uid, role -> viewModel.updateMemberRole(uid, role) },
                                    onGenerateInvite = {
                                        viewModel.generateInviteCode { code ->
                                            generatedInviteCode = code
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Post Composer Sheet overlay
                    if (showPostComposer) {
                        PostComposerSheet(
                            circle = circle,
                            onDismiss = { showPostComposer = false },
                            onPostCreated = {
                                viewModel.loadCircleData(circle.id)
                                showPostComposer = false
                            }
                        )
                    }

                    // Create Draft Sheet overlay
                    if (showCreateDraftSheet) {
                        CreateDraftSheet(
                            onDismiss = { showCreateDraftSheet = false },
                            onCreateDraft = { title, abstract, format ->
                                viewModel.createDraft(title, abstract, format)
                                showCreateDraftSheet = false
                            }
                        )
                    }

                    // Create Reading List Sheet overlay
                    if (showCreateReadingListSheet) {
                        CreateReadingListSheet(
                            papers = detailState.papers,
                            onDismiss = { showCreateReadingListSheet = false },
                            onCreateReadingList = { title, desc, pids ->
                                viewModel.createReadingList(title, desc, pids)
                                showCreateReadingListSheet = false
                            }
                        )
                    }

                    // Edit Pinboard Dialog overlay
                    if (showEditPinboardDialog) {
                        EditPinboardDialog(
                            currentText = detailState.workspace.pinBoardText,
                            onDismiss = { showEditPinboardDialog = false },
                            onSave = { newText ->
                                viewModel.updatePinBoard(newText)
                                showEditPinboardDialog = false

                            }
                        )
                    }

                    // Generated Invite Code Alert
                    if (generatedInviteCode != null) {
                        AlertDialog(
                            onDismissRequest = { generatedInviteCode = null },
                            title = { Text(text = "Circle Invite Code", fontWeight = FontWeight.Bold) },
                            text = {
                                Column {
                                    Text("Share this invite code with your lab members or connection network to grant instant access:")
                                    Spacer(Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CcColors.HighlighterYellow.copy(alpha = 0.2f))
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = generatedInviteCode ?: "",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = CcColors.InkNavy
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = { generatedInviteCode = null }) {
                                    Text("Done")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Tab View contents
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun FeedTabContent(
    posts: List<Post>,
    onPostClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onEndorse: (String) -> Unit,
    onSave: (String) -> Unit
) {
    if (posts.isEmpty()) {
        CcEmptyState(
            emoji = "📝",
            title = "No feed posts yet",
            subtitle = "Be the first to start a discussion in this circle!"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Weekly discussion card highlighted at top
            item {
                CcCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .background(CcColors.HighlighterYellow.copy(alpha = 0.15f))
                            .border(1.5.dp, CcColors.HighlighterYellow, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "📌 Pinned: Weekly Discussion Topic",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CcColors.InkNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Welcome to our circle feed! Share working hypotheses, request peer review on preprints, or propose new lab collaborations.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            items(posts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    onPostClick = onPostClick,
                    onUserClick = onUserClick,
                    onCircleClick = {},
                    onEndorse = onEndorse,
                    onSave = onSave
                )
            }
        }
    }
}

@Composable
fun PapersTabContent(
    papers: List<Paper>,
    readingLists: List<CircleReadingList>,
    onPaperClick: (String) -> Unit,
    onCreateReadingList: () -> Unit,
    onSaveReadingList: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // ── Shared Circle Reading Lists ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Shared Reading Lists",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Curated paper collections for seminars & lit reviews",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.ccColors.marginGray
                    )
                }

                Button(
                    onClick = onCreateReadingList,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "New List", fontSize = 12.sp)
                }
            }
        }

        if (readingLists.isEmpty()) {
            item {
                CcEmptyState(
                    emoji = "📚",
                    title = "No reading lists created",
                    subtitle = "Create a reading list for lab seminars or literature reviews."
                )
            }
        } else {
            items(readingLists, key = { it.id }) { list ->
                CcCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🔖", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = list.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Curated by ${list.createdByName} • ${list.paperCount} papers",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.ccColors.marginGray
                                    )
                                }
                            }

                            Button(
                                onClick = { onSaveReadingList(list.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = CcColors.HighlighterYellow),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.BookmarkBorder,
                                    contentDescription = null,
                                    tint = CcColors.InkNavy,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Save to My Library", fontSize = 11.sp, color = CcColors.InkNavy, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (list.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = list.description, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.ccColors.divider)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Circle Publications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (papers.isEmpty()) {
            item {
                CcEmptyState(
                    emoji = "📄",
                    title = "No publications cataloged",
                    subtitle = "Publish a paper to this circle or attach one to share with the group."
                )
            }
        } else {
            items(papers, key = { it.id }) { paper ->
                CcCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPaperClick(paper.id) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = paper.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Published by ${paper.authors.joinToString { it.name }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.ccColors.marginGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = paper.abstract,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkspaceTabContent(
    circle: Circle,
    workspace: CircleWorkspace,
    drafts: List<CircleDraft>,
    onDraftClick: (String) -> Unit,
    onCreateDraft: () -> Unit,
    onEditPinboard: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // ── Collaborative Pin Board ──
        item {
            CcCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .background(CcColors.CircleBlue.copy(alpha = 0.08f))
                        .border(1.5.dp, CcColors.CircleBlue.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📌", fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Lab Project Pin Board",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CcColors.CircleBlue
                            )
                        }

                        IconButton(onClick = onEditPinboard, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Pinboard", tint = CcColors.CircleBlue, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = workspace.pinBoardText.ifBlank { "No lab notices posted yet. Click edit to post team announcements or deadlines!" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // ── Active Drafts Section ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Active Manuscripts & Drafts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Collaborative authoring, peer review & section comments",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.ccColors.marginGray
                    )
                }

                Button(
                    onClick = onCreateDraft,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "New Draft", fontSize = 12.sp)
                }
            }
        }

        if (drafts.isEmpty()) {
            item {
                CcEmptyState(
                    emoji = "📝",
                    title = "No working drafts uploaded",
                    subtitle = "Upload or create a manuscript draft to initiate co-author reviews."
                )
            }
        } else {
            items(drafts, key = { it.id }) { draft ->
                DraftCardItem(draft = draft, onClick = { onDraftClick(draft.id) })
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.ccColors.divider)
            Spacer(modifier = Modifier.height(8.dp))

            CircleActivityHeatmap(
                weeklyActivity = circle.weeklyActivity,
                accentColor = Color(circle.bannerColor)
            )
        }
    }
}

@Composable
fun DraftCardItem(
    draft: CircleDraft,
    onClick: () -> Unit
) {
    CcCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status badge
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
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (draft.abstract.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = draft.abstract,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Lead Author: ${draft.leadAuthorName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.ccColors.marginGray
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "💬 ${draft.commentCount} comments", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ccColors.marginGray)
                    Text(text = "👥 ${draft.reviewCount} reviews", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ccColors.marginGray)
                }
            }
        }
    }
}

@Composable
fun MembersTabContent(
    members: List<CircleMember>,
    workspace: CircleWorkspace,
    onUserClick: (String) -> Unit,
    onUpdateRole: (String, CircleRole) -> Unit,
    onGenerateInvite: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // ── Access Control & Invite Code Header ──
        item {
            CcCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (workspace.accessType == CircleAccessType.PUBLIC) Icons.Filled.Public else Icons.Filled.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = CcColors.CircleBlue
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Access: ${workspace.accessType.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (workspace.inviteCode.isNotBlank()) "Active Invite Code: ${workspace.inviteCode}" else "Generate invite codes for lab members",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.ccColors.marginGray
                        )
                    }

                    Button(
                        onClick = onGenerateInvite,
                        colors = ButtonDefaults.buttonColors(containerColor = CcColors.HighlighterYellow),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.VpnKey, contentDescription = null, tint = CcColors.InkNavy, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Invite Code", fontSize = 12.sp, color = CcColors.InkNavy, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "Lab Group & Circle Members (${members.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(members, key = { it.userId }) { member ->
            var showRoleMenu by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onUserClick(member.userId) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = member.name.take(1).ifBlank { "U" }, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = member.institution.ifBlank { "Researcher" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.ccColors.marginGray
                    )
                }

                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (member.role) {
                                    CircleRole.ADMIN -> Color(0xFFE91E63)
                                    CircleRole.LEAD_RESEARCHER -> CcColors.CircleBlue
                                    CircleRole.CONTRIBUTOR -> Color(0xFF4CAF50)
                                    CircleRole.GUEST_OBSERVER -> Color.Gray
                                }.copy(alpha = 0.15f)
                            )
                            .clickable { showRoleMenu = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = member.role.displayName(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (member.role) {
                                CircleRole.ADMIN -> Color(0xFFE91E63)
                                CircleRole.LEAD_RESEARCHER -> CcColors.CircleBlue
                                CircleRole.CONTRIBUTOR -> Color(0xFF2E7D32)
                                CircleRole.GUEST_OBSERVER -> Color.DarkGray
                            }
                        )
                    }

                    DropdownMenu(
                        expanded = showRoleMenu,
                        onDismissRequest = { showRoleMenu = false }
                    ) {
                        CircleRole.values().forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.displayName()) },
                                onClick = {
                                    onUpdateRole(member.userId, role)
                                    showRoleMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Sheet & Dialog Overlays
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun CreateDraftSheet(
    onDismiss: () -> Unit,
    onCreateDraft: (title: String, abstract: String, format: DraftFormat) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var abstractText by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf(DraftFormat.PDF) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Upload / Create Manuscript Draft", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Manuscript Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = abstractText,
                    onValueChange = { abstractText = it },
                    label = { Text("Abstract / Summary") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Text(text = "Draft Format:", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DraftFormat.values().forEach { fmt ->
                        FilterChip(
                            selected = (selectedFormat == fmt),
                            onClick = { selectedFormat = fmt },
                            label = { Text(fmt.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreateDraft(title, abstractText, selectedFormat)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Create Draft")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateReadingListSheet(
    papers: List<Paper>,
    onDismiss: () -> Unit,
    onCreateReadingList: (title: String, description: String, paperIds: List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val selectedPaperIds = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Create Shared Reading List", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("List Title (e.g. Weekly Lab Seminar)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Syllabus notes") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "Select Papers to Include:", style = MaterialTheme.typography.labelSmall)
                papers.take(4).forEach { paper ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectedPaperIds.contains(paper.id)) selectedPaperIds.remove(paper.id)
                                else selectedPaperIds.add(paper.id)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedPaperIds.contains(paper.id),
                            onCheckedChange = { checked ->
                                if (checked) selectedPaperIds.add(paper.id) else selectedPaperIds.remove(paper.id)
                            }
                        )
                        Text(text = paper.title, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreateReadingList(title, description, selectedPaperIds.toList())
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Create List")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditPinboardDialog(
    currentText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit Lab Pin Board", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Pin Board Announcements & Checklist") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5
            )
        },
        confirmButton = {
            Button(onClick = { onSave(text) }) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// CircleActivityHeatmap — GitHub-style 12-week contribution grid
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun CircleActivityHeatmap(
    weeklyActivity: List<Int>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val weeks = 12
    val days  = 7
    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    val grid: List<List<Int>> = if (weeklyActivity.isEmpty()) {
        List(weeks) { List(days) { 0 } }
    } else {
        List(weeks) { week ->
            List(days) { day ->
                val base = weeklyActivity[day % weeklyActivity.size]
                val variation = ((week * 13 + day * 7) % 20) - 10
                maxOf(0, base + variation)
            }
        }
    }

    val maxVal       = grid.flatten().maxOrNull()?.coerceAtLeast(1) ?: 1
    val totalPosts   = grid.flatten().sum()
    val peakDayIdx   = weeklyActivity.indices.maxByOrNull { weeklyActivity[it] } ?: 0
    val peakDayLabel = dayLabels.getOrElse(peakDayIdx) { "–" }

    val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val peakColor  = CcColors.CircleBlue

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Community Activity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "12-week contribution heatmap",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.ccColors.marginGray
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(end = 6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                dayLabels.forEach { label ->
                    Box(
                        modifier = Modifier.size(width = 28.dp, height = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.ccColors.marginGray
                        )
                    }
                }
            }

            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height((14 * days + 3 * (days - 1)).dp)
            ) {
                val cellW  = (size.width - (weeks - 1) * 3.dp.toPx()) / weeks
                val cellH  = (size.height - (days - 1) * 3.dp.toPx()) / days
                val radius = CornerRadius(3.dp.toPx())

                for (week in 0 until weeks) {
                    for (day in 0 until days) {
                        val ratio     = grid[week][day].toFloat() / maxVal
                        val cellColor = lerp(emptyColor, peakColor, ratio)
                        val left = week * (cellW + 3.dp.toPx())
                        val top  = day  * (cellH + 3.dp.toPx())

                        drawRoundRect(
                            color        = cellColor,
                            topLeft      = Offset(left, top),
                            size         = Size(cellW, cellH),
                            cornerRadius = radius
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Less", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.ccColors.marginGray)
            Spacer(Modifier.width(4.dp))
            listOf(0.08f, 0.28f, 0.52f, 0.76f, 1f).forEach { ratio ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 1.5.dp)
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(lerp(emptyColor, peakColor, ratio))
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(text = "More", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.ccColors.marginGray)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HeatmapStatPill(label = "12-week posts", value = totalPosts.toString(), modifier = Modifier.weight(1f))
            HeatmapStatPill(label = "Peak day",      value = peakDayLabel,          modifier = Modifier.weight(1f))
            HeatmapStatPill(label = "Peak count",    value = maxVal.toString(),     modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeatmapStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CcColors.CircleBlue
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.ccColors.marginGray,
            fontSize = 10.sp
        )
    }
}
