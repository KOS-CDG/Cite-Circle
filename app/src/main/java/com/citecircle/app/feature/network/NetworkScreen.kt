package com.citecircle.app.feature.network

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citecircle.app.core.data.UserRepository
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────────────────────
// NetworkViewModel
// ──────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    val connections = userRepository.getAllUsers()
        .combine(MutableStateFlow(true)) { users, _ -> users.filter { it.isConnected } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val requests = userRepository.getConnectionRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val discoverUsers = userRepository.getSuggestedConnections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _actionStates = MutableStateFlow<Map<String, String>>(emptyMap())
    val actionStates = _actionStates.asStateFlow()

    init {
        // Restore pending connection states from the repository (which reads from DataStore)
        viewModelScope.launch {
            userRepository.getSuggestedConnections().collect { users ->
                val pending = users
                    .filter { it.connectionPending }
                    .associate { it.id to "Pending" }
                // Merge with any in-session states (in-session wins)
                val current = _actionStates.value.toMutableMap()
                pending.forEach { (id, state) -> current.putIfAbsent(id, state) }
                _actionStates.value = current
            }
        }
    }

    fun acceptRequest(userId: String) {
        viewModelScope.launch {
            userRepository.acceptConnection(userId)
        }
    }

    fun declineRequest(userId: String) {
        viewModelScope.launch {
            userRepository.declineConnection(userId)
        }
    }

    fun connectUser(userId: String) {
        viewModelScope.launch {
            userRepository.connectUser(userId)
            val current = _actionStates.value.toMutableMap()
            current[userId] = "Pending"
            _actionStates.value = current
        }
    }

    private fun <T> StateFlow<T>.combine(other: StateFlow<Boolean>, transform: (T, Boolean) -> T): StateFlow<T> {
        val flow = kotlinx.coroutines.flow.combine(this, other, transform)
        return flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), this.value)
    }

}

// ──────────────────────────────────────────────────────────────────────────────
// NetworkScreen Composable
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun NetworkScreen(
    onUserClick: (String) -> Unit,
    viewModel: NetworkViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val connections by viewModel.connections.collectAsState()
    val requests by viewModel.requests.collectAsState()
    val discoverUsers by viewModel.discoverUsers.collectAsState()
    val actionStates by viewModel.actionStates.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.ccColors.paperCream)
    ) {
        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Connections (${connections.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Requests (${requests.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Discover", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> ConnectionsTab(connections = connections, onUserClick = onUserClick)
                1 -> RequestsTab(
                    requests = requests,
                    onAccept = { viewModel.acceptRequest(it) },
                    onDecline = { viewModel.declineRequest(it) }
                )
                2 -> DiscoverTab(
                    discoverUsers = discoverUsers,
                    actionStates = actionStates,
                    onUserClick = onUserClick,
                    onConnect = { viewModel.connectUser(it) }
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Sub Tab layouts
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun ConnectionsTab(
    connections: List<User>,
    onUserClick: (String) -> Unit
) {
    if (connections.isEmpty()) {
        CcEmptyState(
            emoji = "🤝",
            title = "Grow your network",
            subtitle = "You haven't connected with other researchers yet. Explore suggested scholars to begin."
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(connections, key = { it.id }) { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onUserClick(user.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CcAvatar(user = user, size = 44.dp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = user.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(text = "${user.role.displayName()} • ${user.institution}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ccColors.marginGray)
                    }
                }
            }
        }
    }
}

@Composable
fun RequestsTab(
    requests: List<User>,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit
) {
    if (requests.isEmpty()) {
        CcEmptyState(
            emoji = "📬",
            title = "No requests",
            subtitle = "You have no incoming connection invites at the moment."
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(requests, key = { it.id }) { user ->
                CcCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CcAvatar(user = user, size = 44.dp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = user.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = user.institution, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ccColors.marginGray)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Row {
                            Button(
                                onClick = { onAccept(user.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = CcColors.SeafoamTeal),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Accept", fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { onDecline(user.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = CcColors.CoralPop),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Ignore", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoverTab(
    discoverUsers: List<User>,
    actionStates: Map<String, String>,
    onUserClick: (String) -> Unit,
    onConnect: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Carousel 1: People in your field
        item {
            Column {
                Text(
                    text = "People in your field",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(discoverUsers.take(4)) { user ->
                        DiscoverCard(user = user, actionStates = actionStates, onUserClick = onUserClick, onConnect = onConnect)
                    }
                }
            }
        }

        // Carousel 2: From your institution
        item {
            Column {
                Text(
                    text = "From your institution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(discoverUsers.drop(4)) { user ->
                        DiscoverCard(user = user, actionStates = actionStates, onUserClick = onUserClick, onConnect = onConnect)
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoverCard(
    user: User,
    actionStates: Map<String, String>,
    onUserClick: (String) -> Unit,
    onConnect: (String) -> Unit
) {
    val state = actionStates[user.id]
    val haptic = LocalHapticFeedback.current

    CcCard(
        modifier = Modifier
            .width(180.dp)
            .clickable { onUserClick(user.id) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CcAvatar(user = user, size = 56.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = user.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = user.institution,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.ccColors.marginGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (state != "Pending") {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onConnect(user.id)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state == "Pending") MaterialTheme.ccColors.marginGray else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                if (state == "Pending") {
                    Icon(Icons.Filled.Check, contentDescription = "Request pending", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pending", fontSize = 11.sp)
                } else {
                    Text("Connect", fontSize = 12.sp)
                }
            }
        }
    }
}
