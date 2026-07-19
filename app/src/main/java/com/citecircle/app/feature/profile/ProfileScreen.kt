package com.citecircle.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
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
    onSettingsClick: () -> Unit = {},
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

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // 1. Cover Gradient and Avatar details block
                    item {
                        Column {
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
                                // 1. Top row: Avatar on left, Action buttons (Edit Profile & Settings) on right
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    CcAvatar(user = user, size = 80.dp)

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        CcSecondaryButton(
                                            text = "Edit Profile",
                                            onClick = onEditProfile,
                                            modifier = Modifier.height(36.dp)
                                        )
                                        IconButton(
                                            onClick = onSettingsClick,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        ) {
                                            Icon(
                                                Icons.Outlined.Settings,
                                                contentDescription = "Settings",
                                                tint = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // 2. Name & Role information block
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = user.name,
                                        fontFamily = FrauncesFamily,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = "${user.role.displayName()} • ${user.institution}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.ccColors.marginGray
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

                                Spacer(modifier = Modifier.height(16.dp))

                                // Stats count columns row (Stacked Metrics with dividers and background)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatItem(count = user.followerCount, label = "Followers", modifier = Modifier.weight(1f))
                                    VerticalDivider(modifier = Modifier.height(24.dp), color = MaterialTheme.ccColors.divider)
                                    StatItem(count = user.followingCount, label = "Following", modifier = Modifier.weight(1f))
                                    VerticalDivider(modifier = Modifier.height(24.dp), color = MaterialTheme.ccColors.divider)
                                    HighlighterSweep(modifier = Modifier.weight(1f)) {
                                        StatItem(count = user.citationCount, label = "Citations", modifier = Modifier.fillMaxWidth())
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Bio
                                Text(text = user.bio, style = MaterialTheme.typography.bodyMedium)

                                Spacer(modifier = Modifier.height(12.dp))

                                // Field Interest chips stubs row
                                if (user.interests.isNotEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(user.interests) { interest ->
                                            CcChip(label = interest, selected = false, onClick = {})
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Tab selector
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        CcTabRow(
                            tabs = listOf("Papers", "Circles", "About"),
                            selectedIndex = selectedTab,
                            onTabSelected = { selectedTab = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 3. Tab items switcher
                    when (selectedTab) {
                        0 -> {
                            if (profileState.papers.isEmpty()) {
                                item {
                                    CcEmptyState(
                                        emoji = "📄",
                                        title = "No publications",
                                        subtitle = "Publish a paper draft using the central FAB to populate your portfolio."
                                    )
                                }
                            } else {
                                items(profileState.papers) { paper ->
                                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
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
                        1 -> {
                            if (profileState.circles.isEmpty()) {
                                item {
                                    CcEmptyState(
                                        emoji = "🏰",
                                        title = "No circles",
                                        subtitle = "You haven't joined any discussion circles yet."
                                    )
                                }
                            } else {
                                items(profileState.circles) { circle ->
                                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
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
                        2 -> {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
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
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Helpers
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun StatItem(count: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.ccColors.marginGray
        )
    }
}
