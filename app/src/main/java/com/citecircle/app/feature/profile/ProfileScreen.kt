package com.citecircle.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citecircle.app.core.data.PaperRepository
import com.citecircle.app.core.data.UserRepository
import com.citecircle.app.core.data.CircleRepository
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.Circle
import com.citecircle.app.core.model.Paper
import com.citecircle.app.core.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────────────────────
// ProfileViewModel
// ──────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val paperRepository: PaperRepository,
    private val circleRepository: CircleRepository
) : ViewModel() {

    private val _user = userRepository.getCurrentUser()

    val papers = _user.combine(paperRepository.getAllPapers()) { user, allPapers ->
        allPapers.filter { paper -> paper.authors.any { it.id == user.id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val joinedCircles = circleRepository.getJoinedCircles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val state: StateFlow<ProfileState> = combine(_user, papers, joinedCircles) { user, papers, circles ->
        ProfileState.Success(user, papers, circles)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileState.Loading)
}

sealed interface ProfileState {
    object Loading : ProfileState
    data class Success(val user: User, val papers: List<Paper>, val circles: List<Circle>) : ProfileState
}

// ──────────────────────────────────────────────────────────────────────────────
// ProfileScreen Composable (Current User)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onPaperClick: (String) -> Unit,
    onCircleClick: (String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.ccColors.paperCream)
    ) {
        when (val profileState = state) {
            is ProfileState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is ProfileState.Success -> {
                val user = profileState.user

                Column(modifier = Modifier.fillMaxSize()) {
                    // Header cover gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(CcColors.InkNavy, CcColors.CircleBlue)
                                )
                            )
                    )

                    // Avatar overlay details
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                val overlap = 40.dp.roundToPx()
                                layout(placeable.width, (placeable.height - overlap).coerceAtLeast(0)) {
                                    placeable.placeRelative(0, -overlap)
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CcAvatar(user = user, size = 80.dp)

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.name,
                                    fontFamily = FrauncesFamily,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Text(
                                    text = "${user.role.displayName()} • ${user.institution}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.ccColors.marginGray
                                )
                            }

                            CcSecondaryButton(
                                text = "Edit",
                                onClick = onEditProfile,
                                modifier = Modifier.height(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ORCID details stubs
                        if (user.orcidId.isNotEmpty()) {
                            Text(
                                text = "ORCID: ${user.orcidId}",
                                fontSize = 12.sp,
                                fontFamily = JetBrainsMonoFamily,
                                color = CcColors.SeafoamTeal,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats count columns row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            StatItem(count = user.followerCount, label = "Followers")
                            StatItem(count = user.followingCount, label = "Following")
                            // Citations highlight count
                            HighlighterSweep {
                                Box(
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    StatItem(count = user.citationCount, label = "Citations")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bio
                        Text(text = user.bio, style = MaterialTheme.typography.bodyMedium)

                        Spacer(modifier = Modifier.height(12.dp))

                        // Field Interest chips stubs row
                        if (user.interests.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(user.interests) { interest ->
                                    CcChip(label = interest, selected = false, onClick = {})
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Tab selector
                    CcTabRow(
                        tabs = listOf("Papers", "Circles", "About"),
                        selectedIndex = selectedTab,
                        onTabSelected = { selectedTab = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Tab items lists switcher
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            0 -> PapersProfileTab(papers = profileState.papers, onPaperClick = onPaperClick)
                            1 -> CirclesProfileTab(circles = profileState.circles, onCircleClick = onCircleClick)
                            2 -> AboutProfileTab(user = user)
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Helpers / Tabs contents
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun StatItem(count: Int, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ccColors.marginGray)
    }
}

@Composable
fun PapersProfileTab(
    papers: List<Paper>,
    onPaperClick: (String) -> Unit
) {
    if (papers.isEmpty()) {
        CcEmptyState(
            emoji = "📄",
            title = "No publications",
            subtitle = "Publish a paper draft using the central FAB to populate your portfolio."
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(papers) { paper ->
                CcCard(modifier = Modifier.fillMaxWidth().clickable { onPaperClick(paper.id) }) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = paper.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "${paper.journal} • ${paper.year}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ccColors.marginGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "${paper.citationCount} citations", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CcColors.CircleBlue)
                    }
                }
            }
        }
    }
}

@Composable
fun CirclesProfileTab(
    circles: List<Circle>,
    onCircleClick: (String) -> Unit
) {
    if (circles.isEmpty()) {
        CcEmptyState(
            emoji = "🏰",
            title = "No circles",
            subtitle = "You haven't joined any discussion circles yet."
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(circles) { circle ->
                CcCard(modifier = Modifier.fillMaxWidth().clickable { onCircleClick(circle.id) }) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = circle.iconEmoji, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = circle.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = "${circle.memberCount} members", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ccColors.marginGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutProfileTab(user: User) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(text = "Research Interests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = user.interests.joinToString { it }, style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Affiliation Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "${user.institution}\nGlobal Academic Community Network", style = MaterialTheme.typography.bodyMedium)
    }
}
