package com.citecircle.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import com.citecircle.app.core.designsystem.*
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
// OtherProfileViewModel
// ──────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class OtherProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val paperRepository: PaperRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    private val _papers = MutableStateFlow<List<Paper>>(emptyList())

    val state: StateFlow<OtherProfileState> = combine(_user, _papers) { user, papers ->
        if (user == null) OtherProfileState.Loading
        else OtherProfileState.Success(user, papers)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OtherProfileState.Loading)

    fun loadUserData(userId: String) {
        viewModelScope.launch {
            userRepository.getUserById(userId).collect {
                _user.value = it
            }
        }
        viewModelScope.launch {
            paperRepository.getPapersForUser(userId).collect {
                _papers.value = it
            }
        }
    }

    fun toggleConnect() {
        val user = _user.value ?: return
        viewModelScope.launch {
            userRepository.connectUser(user.id)
            loadUserData(user.id)
        }
    }
}

sealed interface OtherProfileState {
    object Loading : OtherProfileState
    data class Success(val user: User, val papers: List<Paper>) : OtherProfileState
}

// ──────────────────────────────────────────────────────────────────────────────
// OtherProfileScreen Composable
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onPaperClick: (String) -> Unit,
    onMessageClick: (String) -> Unit,
    viewModel: OtherProfileViewModel = hiltViewModel()
) {
    LaunchedEffect(userId) {
        viewModel.loadUserData(userId)
    }

    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.ccColors.paperCream)
                .padding(paddingValues)
        ) {
            when (val profileState = state) {
                is OtherProfileState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is OtherProfileState.Success -> {
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
                                        .height(120.dp)
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(CcColors.InkNavy, CcColors.CircleBlue)
                                            )
                                        )
                                )

                                // Info section details
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
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Interactive Connect/Message Action Button Row
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        val isPending = user.connectionPending
                                        val isConnected = user.isConnected

                                        Button(
                                            onClick = { viewModel.toggleConnect() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isConnected || isPending) MaterialTheme.ccColors.marginGray else MaterialTheme.colorScheme.primary
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).height(40.dp)
                                        ) {
                                            if (isPending) {
                                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Pending", fontSize = 12.sp)
                                            } else if (isConnected) {
                                                Text("Connected", fontSize = 12.sp)
                                            } else {
                                                Text("Connect", fontSize = 12.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        CcSecondaryButton(
                                            text = "Message",
                                            onClick = { onMessageClick("conv1") }, // Mock conversation navigation
                                            modifier = Modifier.weight(1f).height(40.dp)
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

                                    // Interest chips
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
                                tabs = listOf("Papers", "About"),
                                selectedIndex = selectedTab,
                                onTabSelected = { selectedTab = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // 3. Tab content
                        when (selectedTab) {
                            0 -> {
                                if (profileState.papers.isEmpty()) {
                                    item {
                                        CcEmptyState(
                                            emoji = "📄",
                                            title = "No publications",
                                            subtitle = "This researcher hasn't uploaded any publications yet."
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
}
