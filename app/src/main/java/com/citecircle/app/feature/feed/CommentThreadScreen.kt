package com.citecircle.app.feature.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citecircle.app.core.data.CommentRepository
import com.citecircle.app.core.data.PostRepository
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.Comment
import com.citecircle.app.core.model.Post
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────────────────────
// CommentViewModel
// ──────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class CommentViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository
) : ViewModel() {

    private val _postId = MutableStateFlow("")
    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    private val _post = MutableStateFlow<Post?>(null)

    // Optimistic overrides so the endorse/save buttons update immediately,
    // mirroring the pattern used in FeedViewModel.
    private val _endorseOverride = MutableStateFlow<Pair<Boolean, Int>?>(null)
    private val _saveOverride = MutableStateFlow<Boolean?>(null)

    private var postJob: Job? = null
    private var commentsJob: Job? = null

    val state: StateFlow<CommentScreenState> = combine(
        _post, _comments, _endorseOverride, _saveOverride
    ) { post, comments, endorseOverride, saveOverride ->
        if (post == null) {
            CommentScreenState.Loading
        } else {
            val displayPost = post.copy(
                isEndorsed = endorseOverride?.first ?: post.isEndorsed,
                endorseCount = endorseOverride?.second ?: post.endorseCount,
                isSaved = saveOverride ?: post.isSaved
            )
            CommentScreenState.Success(displayPost, comments)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CommentScreenState.Loading)

    fun loadPostData(postId: String) {
        // Only clear optimistic overrides when actually navigating to a different
        // post — addComment() re-triggers this with the same postId to refresh
        // comments, and that shouldn't discard a pending endorse/save toggle.
        if (_postId.value != postId) {
            _endorseOverride.value = null
            _saveOverride.value = null
        }
        _postId.value = postId

        postJob?.cancel()
        postJob = viewModelScope.launch {
            postRepository.getPostById(postId).collect { _post.value = it }
        }

        commentsJob?.cancel()
        commentsJob = viewModelScope.launch {
            commentRepository.getCommentsForPost(postId).collect { _comments.value = it }
        }
    }

    fun addComment(content: String, parentId: String? = null) {
        viewModelScope.launch {
            commentRepository.addComment(_postId.value, content, parentId)
            // Reload trigger
            loadPostData(_postId.value)
        }
    }

    fun endorsePost() {
        val post = _post.value ?: return
        viewModelScope.launch {
            val (currentEndorsed, currentCount) = _endorseOverride.value
                ?: Pair(post.isEndorsed, post.endorseCount)
            val newEndorsed = !currentEndorsed
            val newCount = if (newEndorsed) currentCount + 1 else maxOf(0, currentCount - 1)
            _endorseOverride.value = Pair(newEndorsed, newCount)
            postRepository.endorsePost(post.id)
        }
    }

    fun savePost() {
        val post = _post.value ?: return
        viewModelScope.launch {
            val currentSaved = _saveOverride.value ?: post.isSaved
            _saveOverride.value = !currentSaved
            postRepository.savePost(post.id)
        }
    }
}

sealed interface CommentScreenState {
    object Loading : CommentScreenState
    data class Success(val post: Post, val comments: List<Comment>) : CommentScreenState
}

// ──────────────────────────────────────────────────────────────────────────────
// CommentThreadScreen Composable
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CommentThreadScreen(
    postId: String,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: CommentViewModel = hiltViewModel()
) {
    LaunchedEffect(postId) {
        viewModel.loadPostData(postId)
    }

    val state by viewModel.state.collectAsState()
    var commentText by remember { mutableStateOf("") }
    var selectedParentCommentId by remember { mutableStateOf<String?>(null) }
    var replyPlaceholderText by remember { mutableStateOf("Write a comment...") }

    // Bottom sheet states for reporting
    val sheetState = rememberModalBottomSheetState()
    var showReportSheet by remember { mutableStateOf(false) }
    var selectedReportComment by remember { mutableStateOf<Comment?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discussion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            // Reply Composer
            Surface(
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text(replyPlaceholderText, color = MaterialTheme.ccColors.marginGray) },
                        maxLines = 3,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.ccColors.divider
                        ),
                        trailingIcon = {
                            if (selectedParentCommentId != null) {
                                Text(
                                    text = "Cancel",
                                    color = CcColors.CoralPop,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable {
                                            selectedParentCommentId = null
                                            replyPlaceholderText = "Write a comment..."
                                        }
                                        .padding(8.dp)
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                viewModel.addComment(commentText, selectedParentCommentId)
                                commentText = ""
                                selectedParentCommentId = null
                                replyPlaceholderText = "Write a comment..."
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send comment", tint = Color.White)
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.ccColors.paperCream)
                .padding(paddingValues)
        ) {
            when (val screenState = state) {
                is CommentScreenState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is CommentScreenState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        // Original Post
                        item {
                            PostCard(
                                post = screenState.post,
                                onPostClick = {},
                                onUserClick = onUserClick,
                                onCircleClick = {},
                                onEndorse = { viewModel.endorsePost() },
                                onSave = { viewModel.savePost() },
                                modifier = Modifier.padding(16.dp)
                            )
                            Divider(color = MaterialTheme.ccColors.divider, thickness = 1.dp)
                            Text(
                                text = "Comments",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                            )
                        }

                        // Empty State check
                        if (screenState.comments.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No comments yet. Start the discussion!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.ccColors.marginGray
                                    )
                                }
                            }
                        }

                        // Comments List
                        items(screenState.comments) { comment ->
                            CommentRowItem(
                                comment = comment,
                                onReplyClick = {
                                    selectedParentCommentId = comment.id
                                    replyPlaceholderText = "Reply to ${comment.author.name}..."
                                },
                                onLongPress = {
                                    selectedReportComment = comment
                                    showReportSheet = true
                                },
                                onUserClick = onUserClick
                            )
                        }
                    }
                }
            }
        }

        // Long Press bottom sheet options
        if (showReportSheet) {
            ModalBottomSheet(
                onDismissRequest = { showReportSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Comment Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showReportSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.FavoriteBorder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Copy Comment Text")
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showReportSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.FavoriteBorder, contentDescription = null, tint = CcColors.CoralPop)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Report Comment", color = CcColors.CoralPop)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommentRowItem(
    comment: Comment,
    onReplyClick: () -> Unit,
    onLongPress: () -> Unit,
    onUserClick: (String) -> Unit,
    isReply: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            )
            .padding(
                start = if (isReply) 52.dp else 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 12.dp
            )
    ) {
        CcAvatar(user = comment.author, size = if (isReply) 32.dp else 40.dp)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.author.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onUserClick(comment.author.id) }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = comment.author.role.emoji(),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                CcTimestamp(timestamp = comment.timestamp)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Reply",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onReplyClick)
                )
            }

            // Draw nested replies recursively
            if (comment.replies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                comment.replies.forEach { reply ->
                    CommentRowItem(
                        comment = reply,
                        onReplyClick = onReplyClick,
                        onLongPress = onLongPress,
                        onUserClick = onUserClick,
                        isReply = true
                    )
                }
            }
        }
    }
}
