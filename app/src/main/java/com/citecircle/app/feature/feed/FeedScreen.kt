package com.citecircle.app.feature.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citecircle.app.core.data.PostRepository
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.Post
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
    private val postRepository: PostRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow("For You")
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _postsFlow = postRepository.getFeedPosts()

    val uiState: StateFlow<FeedUiState> = combine(_postsFlow, _selectedFilter, _isRefreshing) { posts, filter, refreshing ->
        if (refreshing) {
            FeedUiState.Loading
        } else {
            val filtered = when (filter) {
                "Following" -> posts.filter { it.author.isFollowing }
                "My Field" -> posts.filter { it.author.fieldOfStudy == "Human-Computer Interaction" } // Match current user field
                "Trending" -> posts.sortedByDescending { it.endorseCount }
                else -> posts // For You
            }
            if (filtered.isEmpty()) FeedUiState.Empty else FeedUiState.Success(filtered)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedUiState.Loading)

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun refreshFeed() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(1000) // Simulated network call delay
            _isRefreshing.value = false
        }
    }

    fun endorsePost(postId: String) {
        viewModelScope.launch {
            postRepository.endorsePost(postId)
        }
    }

    fun savePost(postId: String) {
        viewModelScope.launch {
            postRepository.savePost(postId)
        }
    }


}

// ──────────────────────────────────────────────────────────────────────────────
// FeedScreen Composable
// ──────────────────────────────────────────────────────────────────────────────

val feedFilters = listOf("For You", "Following", "My Field", "Trending")

@Composable
fun FeedScreen(
    onPostClick: (String) -> Unit,
    onPaperClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onCircleClick: (String) -> Unit,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.ccColors.paperCream)
    ) {
        // Horizontal filter row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            feedFilters.forEach { filter ->
                CcChip(
                    label = filter,
                    selected = selectedFilter == filter,
                    onClick = { viewModel.setFilter(filter) }
                )
            }
        }

        // Pull to refresh simulation / Feed content
        Box(modifier = Modifier.weight(1f)) {
            when (val state = uiState) {
                is FeedUiState.Loading -> {
                    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
                        items(3) {
                            CcPostShimmer()
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                is FeedUiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                    CcEmptyState(
                        emoji = "📭",
                        title = "No posts found",
                        subtitle = "Try altering your filters or follow more researchers to populate your feed.",
                        actionLabel = "Refresh Feed",
                        onAction = { viewModel.refreshFeed() }
                    )
                }
                is FeedUiState.Error -> {
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
