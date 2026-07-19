package com.citecircle.app.feature.circles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citecircle.app.core.data.CircleRepository
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.Circle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.delay
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import kotlinx.coroutines.flow.onEach

// ──────────────────────────────────────────────────────────────────────────────
// CirclesViewModel
// ──────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class CirclesViewModel @Inject constructor(
    private val circleRepository: CircleRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _circles = circleRepository.getAllCircles()

    val discoverCircles: StateFlow<List<Circle>> = combine(_circles, _searchQuery) { circles, query ->
        if (query.isBlank()) circles
        else circles.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val joinedCircles: StateFlow<List<Circle>> = _circles.combine(MutableStateFlow(true)) { circles, _ ->
        circles.filter { it.isJoined }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleJoinCircle(circleId: String, currentlyJoined: Boolean) {
        viewModelScope.launch {
            if (currentlyJoined) {
                circleRepository.leaveCircle(circleId)
            } else {
                circleRepository.joinCircle(circleId)
            }
        }
    }

    private fun <T> StateFlow<T>.combine(other: StateFlow<Boolean>, transform: (T, Boolean) -> T): StateFlow<T> {
        val flow = kotlinx.coroutines.flow.combine(this, other, transform)
        return flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), this.value)
    }

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        // Mark loading done once data arrives
        viewModelScope.launch {
            _circles.onEach {
                _isLoading.value = false
            }.collect {}
        }
        // Safety net: never show shimmer longer than 2s
        viewModelScope.launch {
            delay(2000)
            _isLoading.value = false
        }
    }

    fun refreshCircles() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(800)
            _isRefreshing.value = false
        }
    }

}

// ──────────────────────────────────────────────────────────────────────────────
// CirclesScreen Composable
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CirclesScreen(
    onCircleClick: (String) -> Unit,
    viewModel: CirclesViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val discoverCircles by viewModel.discoverCircles.collectAsState()
    val joinedCircles by viewModel.joinedCircles.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.ccColors.paperCream)
    ) {
        // Tab Row selector
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Discover Circles", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("My Circles (${joinedCircles.size})", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            // Search bar for discovering
            CcSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = "Search circles (e.g. Machine Learning)..."
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Show shimmer while initial data loads
            if (isLoading) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(4) {
                        CcCircleShimmer()
                    }
                }
            } else if (discoverCircles.isEmpty()) {
                CcEmptyState(
                    emoji = "🔍",
                    title = "No circles found",
                    subtitle = "Try entering a different field name or search term.",
                    actionLabel = "Clear search",
                    onAction = { viewModel.updateSearchQuery("") }
                )
            } else {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refreshCircles() },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(discoverCircles, key = { it.id }) { circle ->
                            DiscoverCircleCard(
                                circle = circle,
                                onClick = { onCircleClick(circle.id) },
                                onJoinToggle = { viewModel.toggleJoinCircle(circle.id, circle.isJoined) }
                            )
                        }
                    }
                }
            }
        } else {
            if (joinedCircles.isEmpty()) {
                CcEmptyState(
                    emoji = "🏰",
                    title = "No joined circles",
                    subtitle = "Join circles to see discussion forums and collaborate on topics.",
                    actionLabel = "Explore Circles",
                    onAction = { selectedTab = 0 }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(joinedCircles, key = { it.id }) { circle ->
                        JoinedCircleItem(
                            circle = circle,
                            onClick = { onCircleClick(circle.id) }
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Child UI cards
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun DiscoverCircleCard(
    circle: Circle,
    onClick: () -> Unit,
    onJoinToggle: () -> Unit
) {
    CcCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Header with Emoji badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(circle.bannerColor).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = circle.iconEmoji, fontSize = 20.sp)
                    }

                    // Category tag
                    Text(
                        text = circle.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.ccColors.marginGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = circle.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = circle.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.ccColors.marginGray,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column {
                // Member count + Activity Sparkline
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${circle.memberCount} members",
                        fontSize = 11.sp,
                        color = MaterialTheme.ccColors.marginGray
                    )

                    // Sparkline activity representation
                    if (circle.weeklyActivity.isNotEmpty()) {
                        SparklineCanvas(activity = circle.weeklyActivity)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onJoinToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (circle.isJoined) MaterialTheme.ccColors.marginGray else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (circle.isJoined) "Joined" else "Join Circle",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun JoinedCircleItem(
    circle: Circle,
    onClick: () -> Unit
) {
    CcCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(circle.bannerColor).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = circle.iconEmoji, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = circle.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${circle.memberCount} members • ${circle.weeklyPostCount} posts this week",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.ccColors.marginGray
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Unread activity dot in CoralPop
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(CcColors.CoralPop)
            )
        }
    }
}

@Composable
fun SparklineCanvas(activity: List<Int>) {
    Canvas(modifier = Modifier.size(50.dp, 16.dp)) {
        val spacing = size.width / (activity.size - 1)
        val maxVal = activity.maxOrNull()?.toFloat() ?: 1f
        val strokeWidth = 1.5.dp.toPx()

        val points = activity.mapIndexed { idx: Int, value: Int ->
            val x = idx * spacing
            val y = size.height - (value.toFloat() / maxVal * size.height)
            Offset(x, y)
        }

        for (i in 0 until points.size - 1) {
            drawLine(
                color = CcColors.SeafoamTeal,
                start = points[i],
                end = points[i + 1],
                strokeWidth = strokeWidth
            )
        }
    }
}
