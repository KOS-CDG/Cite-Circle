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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.citecircle.app.core.data.PaperRepository
import com.citecircle.app.core.data.UserRepository
import com.citecircle.app.core.data.CircleRepository
import com.citecircle.app.core.data.PublicationRepository
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.Circle
import com.citecircle.app.core.model.OrcidSyncState
import com.citecircle.app.core.model.Paper
import com.citecircle.app.core.model.Publication
import com.citecircle.app.core.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────────────────────
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow

// ──────────────────────────────────────────────────────────────────────────────
// ProfileViewModel
// ──────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val paperRepository: PaperRepository,
    private val circleRepository: CircleRepository,
    private val publicationRepository: PublicationRepository
) : ViewModel() {

    private val _user = userRepository.getCurrentUser()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _publications = MutableStateFlow<List<Publication>>(emptyList())
    val publications = _publications.asStateFlow()

    val syncState = publicationRepository.getSyncState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OrcidSyncState())

    /** One-shot sync outcome for the snackbar; cleared once shown. */
    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage = _syncMessage.asStateFlow()

    init {
        loadPublications()
    }

    private fun loadPublications() {
        viewModelScope.launch {
            publicationRepository.getPublications().collect { _publications.value = it }
        }
    }

    fun syncPublications() {
        viewModelScope.launch {
            val result = publicationRepository.syncPublications()
            _syncMessage.value = result.message.ifBlank {
                if (result.success) "Publications up to date" else "Sync failed"
            }
            if (result.success) loadPublications()
        }
    }

    fun consumeSyncMessage() {
        _syncMessage.value = null
    }

    val papers = _user.combine(paperRepository.getAllPapers()) { user, allPapers ->
        allPapers.filter { paper -> paper.authors.any { it.id == user.id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val joinedCircles = circleRepository.getJoinedCircles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val state: StateFlow<ProfileState> = combine(_user, papers, joinedCircles) { user, papers, circles ->
        ProfileState.Success(user, papers, circles)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileState.Loading)

    fun refreshProfile() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(800)
            _isRefreshing.value = false
        }
    }

    fun updateCoverPhoto(coverUrl: String) {
        viewModelScope.launch {
            userRepository.getCurrentUser().firstOrNull()?.let { current ->
                userRepository.updateCurrentUser(current.copy(coverUrl = coverUrl))
            }
        }
    }
}

sealed interface ProfileState {
    object Loading : ProfileState
    data class Success(val user: User, val papers: List<Paper>, val circles: List<Circle>) : ProfileState
}

// ──────────────────────────────────────────────────────────────────────────────
// ProfileScreen Composable (Current User)
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onPaperClick: (String) -> Unit,
    onCircleClick: (String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val publications by viewModel.publications.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Sync outcomes — including the actionable 400s (no ORCID, cooldown active) —
    // surface here rather than being swallowed.
    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeSyncMessage()
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshProfile() },
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

                val pickCoverMediaLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) {
                        viewModel.updateCoverPhoto(uri.toString())
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // 1. Cover Photo & Profile Info Block
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Header cover photo container
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            ) {
                                if (user.coverUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = user.coverUrl,
                                        contentDescription = "Cover photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(CcColors.InkNavy, CcColors.CircleBlue)
                                                )
                                            )
                                    )
                                }

                                // Cover Photo Upload Button
                                Surface(
                                    color = Color.Black.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable {
                                            pickCoverMediaLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CameraAlt,
                                            contentDescription = "Change Cover",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Edit Cover",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Details block below cover
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                            ) {
                                // Row 1: Overlapping Avatar on left, Action buttons on right
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Box(modifier = Modifier.offset(y = (-36).dp)) {
                                        CcAvatar(user = user, size = 80.dp, showRing = true)
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Button(
                                            onClick = onEditProfile,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                            modifier = Modifier.height(38.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Edit,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Edit Profile",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        IconButton(
                                            onClick = onSettingsClick,
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Settings,
                                                contentDescription = "Settings",
                                                tint = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height((-18).dp))

                                // Row 2: User Name & Role Details
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
                            tabs = listOf("Papers", "Publications", "Circles", "About"),
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
                            item {
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                    OrcidSyncHeader(
                                        orcidId = user.orcidId,
                                        syncState = syncState,
                                        publicationCount = publications.size,
                                        totalCitations = publications.sumOf { it.citationCount },
                                        onSync = { viewModel.syncPublications() },
                                    )
                                }
                            }

                            if (publications.isEmpty()) {
                                item {
                                    CcEmptyState(
                                        emoji = "🔬",
                                        title = if (user.orcidId.isBlank()) "No ORCID linked" else "No publications yet",
                                        subtitle = if (user.orcidId.isBlank()) {
                                            "Add your ORCID iD in Edit Profile, then sync to import your indexed work."
                                        } else {
                                            "Tap Sync to pull your publications and citation counts from OpenAlex."
                                        }
                                    )
                                }
                            } else {
                                items(publications, key = { it.id }) { publication ->
                                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                        PublicationCard(
                                            publication = publication,
                                            onOpen = { url -> uriHandler.openUri(url) },
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
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
                        3 -> {
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
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
