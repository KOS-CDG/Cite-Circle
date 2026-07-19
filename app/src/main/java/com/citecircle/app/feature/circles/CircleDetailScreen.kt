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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import com.citecircle.app.core.model.Circle
import com.citecircle.app.core.model.Paper
import com.citecircle.app.core.model.Post
import com.citecircle.app.core.model.User
import com.citecircle.app.feature.feed.PostCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    private val _papers = MutableStateFlow<List<Paper>>(emptyList())
    private val _members = MutableStateFlow<List<User>>(emptyList())

    val state: StateFlow<CircleDetailState> = combine(_circle, _posts, _papers, _members) { circle, posts, papers, members ->
        if (circle == null) CircleDetailState.Loading
        else CircleDetailState.Success(circle, posts, papers, members)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CircleDetailState.Loading)

    fun loadCircleData(circleId: String) {
        _circleId.value = circleId
        viewModelScope.launch {
            circleRepository.getCircleById(circleId).collect { _circle.value = it }
        }
        viewModelScope.launch {
            postRepository.getPostsForCircle(circleId).collect { _posts.value = it }
        }
        viewModelScope.launch {
            paperRepository.getPapersForCircle(circleId).collect { _papers.value = it }
        }
        viewModelScope.launch {
            userRepository.getAllUsers().collect { _members.value = it.take(8) } // Fake member list
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
}

sealed interface CircleDetailState {
    object Loading : CircleDetailState
    data class Success(
        val circle: Circle,
        val posts: List<Post>,
        val papers: List<Paper>,
        val members: List<User>
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
    viewModel: CircleDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(circleId) {
        viewModel.loadCircleData(circleId)
    }

    val state by viewModel.state.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showPostComposer by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if ((state as? CircleDetailState.Success)?.circle?.isJoined == true) {
                FloatingActionButton(
                    onClick = { showPostComposer = true },
                    containerColor = CcColors.HighlighterYellow
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Post to circle", tint = CcColors.InkNavy)
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
                                .height(140.dp)
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
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .border(2.dp, Color(circle.bannerColor), RoundedCornerShape(16.dp))
                                    .align(Alignment.BottomStart)
                                    .offset(x = 24.dp, y = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = circle.iconEmoji, fontSize = 42.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Info Section
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = circle.name,
                                    fontFamily = FrauncesFamily,
                                    fontSize = 24.sp,
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Tabs Row: Posts / Papers / About / Members
                        CcTabRow(
                            tabs = listOf("Posts", "Papers", "About", "Members"),
                            selectedIndex = selectedTabIndex,
                            onTabSelected = { selectedTabIndex = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Tab content switcher
                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedTabIndex) {
                                0 -> PostsTabContent(
                                    posts = detailState.posts,
                                    onPostClick = onPostClick,
                                    onUserClick = onUserClick,
                                    onEndorse = { viewModel.endorsePost(it) },
                                    onSave = { viewModel.savePost(it) }
                                )
                                1 -> PapersTabContent(
                                    papers = detailState.papers,
                                    onPaperClick = onPaperClick
                                )
                                2 -> AboutTabContent(circle = circle)
                                3 -> MembersTabContent(
                                    members = detailState.members,
                                    onUserClick = onUserClick
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
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Tab View contents
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun PostsTabContent(
    posts: List<Post>,
    onPostClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onEndorse: (String) -> Unit,
    onSave: (String) -> Unit
) {
    if (posts.isEmpty()) {
        CcEmptyState(
            emoji = "📝",
            title = "No posts yet",
            subtitle = "Be the first to post a discussion topic in this circle!"
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
                            text = "Welcome to our circle! What research goals are you focusing on this week? Post below to coordinate papers or gather peer feedback.",
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
    onPaperClick: (String) -> Unit
) {
    if (papers.isEmpty()) {
        CcEmptyState(
            emoji = "📄",
            title = "No papers cataloged",
            subtitle = "Publish a paper to this circle or attach one to share with the community."
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
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
fun AboutTabContent(circle: Circle) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // ── Activity Heatmap ──
        CircleActivityHeatmap(
            weeklyActivity = circle.weeklyActivity,
            accentColor = Color(circle.bannerColor)
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.ccColors.divider)
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "About Circle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = circle.description, style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Circle Rules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "1. Respect academic integrity and citation attribution standards.\n" +
                    "2. Stay constructive: peer feedback should help clarify research.\n" +
                    "3. Open sharing of code and dataset repositories is strongly encouraged.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun MembersTabContent(
    members: List<User>,
    onUserClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(members, key = { it.id }) { user ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onUserClick(user.id) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CcAvatar(user = user, size = 40.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${user.role.displayName()} • ${user.institution}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.ccColors.marginGray
                    )
                }
            }
        }
    }
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

    // Tile the real weeklyActivity data across 12 weeks with realistic variation
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

        // ── Header ──
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

        // ── Day labels + Canvas grid ──
        Row(modifier = Modifier.fillMaxWidth()) {

            // Day-of-week labels on the left
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

            // Canvas-drawn grid
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

        // ── Legend ──
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

        Spacer(modifier = Modifier.height(16.dp))

        // ── Stats pills ──
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
