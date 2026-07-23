package com.citecircle.app.feature.feed

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citecircle.app.core.data.PostRepository
import com.citecircle.app.core.data.UserRepository
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.Post
import com.citecircle.app.core.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import kotlinx.coroutines.flow.collect

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update

// ──────────────────────────────────────────────────────────────────────────────
// FeedUiState & FeedViewModel
// ──────────────────────────────────────────────────────────────────────────────

sealed interface FeedUiState {
    object Loading : FeedUiState
    data class Success(val posts: List<Post>) : FeedUiState
    object Empty : FeedUiState
    data class Error(val message: String) : FeedUiState
}

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository,
    userRepository: UserRepository
) : ViewModel() {

    val currentUser = userRepository.getCurrentUser()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _selectedFilter = MutableStateFlow("For You")
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _selectedTopic = MutableStateFlow("All Topics")
    val selectedTopic = _selectedTopic.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _postsFlow = postRepository.getFeedPosts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    private val _endorseOverrides = MutableStateFlow<Map<String, Pair<Boolean, Int>>>(emptyMap())
    private val _saveOverrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    private var lastPostsSize = -1

    init {
        viewModelScope.launch {
            _postsFlow.collect { posts ->
                if (lastPostsSize != -1 && posts.size > lastPostsSize) {
                    refreshFeed()
                }
                lastPostsSize = posts.size
            }
        }
    }

    private data class FeedParams(
        val filter: String,
        val topic: String,
        val refreshing: Boolean
    )

    private val _feedParams = combine(_selectedFilter, _selectedTopic, _isRefreshing) { filter, topic, refreshing ->
        FeedParams(filter, topic, refreshing)
    }

    val uiState: StateFlow<FeedUiState> = combine(
        _postsFlow,
        _endorseOverrides,
        _saveOverrides,
        _feedParams
    ) { posts, endorseOverrides, saveOverrides, params ->
        val (filter, topic, refreshing) = params
        if (refreshing) {
            FeedUiState.Loading
        } else {
            val mappedPosts = posts.map { post ->
                val overrideE = endorseOverrides[post.id]
                val overrideS = saveOverrides[post.id]
                post.copy(
                    isEndorsed = overrideE?.first ?: post.isEndorsed,
                    endorseCount = overrideE?.second ?: post.endorseCount,
                    isSaved = overrideS ?: post.isSaved
                )
            }
            val baseFiltered = when (filter) {
                "Following" -> mappedPosts.filter { it.author.isFollowing }
                "My Field" -> mappedPosts.filter { it.author.fieldOfStudy == "Human-Computer Interaction" }
                "Trending" -> mappedPosts.sortedByDescending { it.endorseCount }
                else -> mappedPosts
            }
            val finalFiltered = if (topic == "All Topics") {
                baseFiltered
            } else {
                baseFiltered.filter { post ->
                    post.content.contains(topic, ignoreCase = true) ||
                    post.attachedPaper?.fieldTags?.any { it.equals(topic, ignoreCase = true) } == true
                }
            }
            if (finalFiltered.isEmpty()) FeedUiState.Empty else FeedUiState.Success(finalFiltered)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedUiState.Loading)

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
        _selectedTopic.value = "All Topics"
    }

    fun setTopic(topic: String) {
        _selectedTopic.value = topic
    }

    fun refreshFeed() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(1000)
            _isRefreshing.value = false
        }
    }

    fun endorsePost(postId: String) {
        viewModelScope.launch {
            val currentPosts = _postsFlow.value
            val currentPost = currentPosts.find { it.id == postId }
            val (currentEndorsed, currentCount) = _endorseOverrides.value[postId]
                ?: Pair(currentPost?.isEndorsed ?: false, currentPost?.endorseCount ?: 0)
            val newEndorsed = !currentEndorsed
            val newCount = if (newEndorsed) currentCount + 1 else maxOf(0, currentCount - 1)
            _endorseOverrides.update { it + (postId to Pair(newEndorsed, newCount)) }
            postRepository.endorsePost(postId)
        }
    }

    fun savePost(postId: String) {
        viewModelScope.launch {
            val currentPosts = _postsFlow.value
            val currentPost = currentPosts.find { it.id == postId }
            val currentSaved = _saveOverrides.value[postId] ?: (currentPost?.isSaved ?: false)
            val newSaved = !currentSaved
            _saveOverrides.update { it + (postId to newSaved) }
            postRepository.savePost(postId)
        }
    }

}

// ──────────────────────────────────────────────────────────────────────────────
// FeedScreen Composable
// ──────────────────────────────────────────────────────────────────────────────

val feedFilters = listOf("For You", "Following", "My Field", "Trending")

val trendingTopics = listOf(
    "All Topics",
    "Machine Learning",
    "Human-Computer Interaction",
    "Genomics",
    "Quantum Physics",
    "Cognitive Science",
    "Health Equity",
    "Topology"
)

@Composable
fun FeedHeaderContent(
    selectedFilter: String,
    selectedTopic: String,
    onFilterSelected: (String) -> Unit,
    onTopicSelected: (String) -> Unit,
    currentUser: User?,
    onComposePost: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.ccColors.paperCream)
    ) {
        // Horizontal filter row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(top = 8.dp, bottom = 4.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            feedFilters.forEach { filter ->
                CcChip(
                    label = filter,
                    selected = selectedFilter == filter,
                    onClick = { onFilterSelected(filter) }
                )
            }
        }

        // Horizontally scrollable topics chip row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(bottom = 8.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(trendingTopics) { topic ->
                val isSelected = selectedTopic == topic
                val backgroundTransition by animateColorAsState(
                    targetValue = if (isSelected) CcColors.CircleBlue.copy(alpha = 0.15f) else Color.Transparent,
                    label = "topic_bg"
                )
                val borderTransition by animateColorAsState(
                    targetValue = if (isSelected) CcColors.CircleBlue else MaterialTheme.ccColors.divider,
                    label = "topic_border"
                )
                val textTransition by animateColorAsState(
                    targetValue = if (isSelected) CcColors.CircleBlue else MaterialTheme.colorScheme.onSurface,
                    label = "topic_text"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(backgroundTransition)
                        .border(1.dp, borderTransition, RoundedCornerShape(20.dp))
                        .clickable { onTopicSelected(topic) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp).padding(end = 4.dp),
                                tint = CcColors.CircleBlue
                            )
                        }
                        Text(
                            text = topic,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textTransition
                        )
                    }
                }
            }
        }

        // Composer bar — tapping opens QuickPostScreen
        FeedComposerBar(
            currentUser = currentUser,
            onClick = onComposePost
        )

        HorizontalDivider(color = MaterialTheme.ccColors.divider)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onPostClick: (String) -> Unit,
    onPaperClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onCircleClick: (String) -> Unit,
    onComposePost: () -> Unit = {},
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedTopic by viewModel.selectedTopic.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshFeed() },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.ccColors.paperCream)
    ) {
        when (val state = uiState) {
            is FeedUiState.Loading -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    item {
                        FeedHeaderContent(
                            selectedFilter = selectedFilter,
                            selectedTopic = selectedTopic,
                            onFilterSelected = { viewModel.setFilter(it) },
                            onTopicSelected = { viewModel.setTopic(it) },
                            currentUser = currentUser,
                            onComposePost = onComposePost
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    items(3) {
                        CcPostShimmer()
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            is FeedUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        FeedHeaderContent(
                            selectedFilter = selectedFilter,
                            selectedTopic = selectedTopic,
                            onFilterSelected = { viewModel.setFilter(it) },
                            onTopicSelected = { viewModel.setTopic(it) },
                            currentUser = currentUser,
                            onComposePost = onComposePost
                        )
                    }
                    items(state.posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            onPostClick = onPostClick,
                            onUserClick = onUserClick,
                            onCircleClick = onCircleClick,
                            onEndorse = { viewModel.endorsePost(it) },
                            onSave = { viewModel.savePost(it) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
            is FeedUiState.Empty -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    item {
                        FeedHeaderContent(
                            selectedFilter = selectedFilter,
                            selectedTopic = selectedTopic,
                            onFilterSelected = { viewModel.setFilter(it) },
                            onTopicSelected = { viewModel.setTopic(it) },
                            currentUser = currentUser,
                            onComposePost = onComposePost
                        )
                    }
                    item {
                        CcEmptyState(
                            emoji = "📭",
                            title = "No posts found",
                            subtitle = "Try altering your filters or follow more researchers to populate your feed.",
                            actionLabel = "Refresh Feed",
                            onAction = { viewModel.refreshFeed() }
                        )
                    }
                }
            }
            is FeedUiState.Error -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    item {
                        FeedHeaderContent(
                            selectedFilter = selectedFilter,
                            selectedTopic = selectedTopic,
                            onFilterSelected = { viewModel.setFilter(it) },
                            onTopicSelected = { viewModel.setTopic(it) },
                            currentUser = currentUser,
                            onComposePost = onComposePost
                        )
                    }
                    item {
                        CcEmptyState(
                            emoji = "⚠️",
                            title = "Something went wrong",
                            subtitle = state.message,
                            actionLabel = "Retry",
                            onAction = { viewModel.refreshFeed() }
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// FeedComposerBar — "What's on your research mind?" entry point
// ──────────────────────────────────────────────────────────────────────────────

private val composerFlairShortcuts = listOf(
    "❓ Question",
    "💬 Discussion",
    "📚 Resource"
)

@Composable
fun FeedComposerBar(
    currentUser: User?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessHigh
        ),
        label = "composer_scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics { contentDescription = "Compose a new post" }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Avatar + ghost input row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Avatar (or placeholder ring if user not yet loaded)
            if (currentUser != null) {
                CcAvatar(user = currentUser, size = 40.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Tappable ghost text field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.ccColors.divider,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "What's on your research mind?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.ccColors.marginGray
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Flair shortcut chips + photo shortcut
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo shortcut
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.ccColors.marginGray,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Photo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.ccColors.marginGray,
                    fontWeight = FontWeight.Medium
                )
            }

            // Cite a paper shortcut
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Outlined.FormatQuote,
                    contentDescription = null,
                    tint = MaterialTheme.ccColors.marginGray,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Cite paper",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.ccColors.marginGray,
                    fontWeight = FontWeight.Medium
                )
            }

            // Flair shortcuts
            composerFlairShortcuts.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.ccColors.marginGray,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
