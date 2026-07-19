package com.citecircle.app.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.StarOutline
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
import com.citecircle.app.core.data.NotificationRepository
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.NotifType
import com.citecircle.app.core.model.Notification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────────────────────
// NotificationsViewModel
// ──────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val notifications: StateFlow<List<Notification>> = notificationRepository.getNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markAllRead() {
        viewModelScope.launch {
            notifications.value.forEach {
                notificationRepository.markAsRead(it.id)
            }
        }
    }

    fun dismissNotification(id: String) {
        viewModelScope.launch {
            notificationRepository.dismissNotification(id)
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(id)
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// NotificationsScreen Composable
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNotificationClick: (String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.markAllRead() }) {
                        Icon(Icons.Outlined.DoneAll, contentDescription = "Mark all read")
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
            if (notifications.isEmpty()) {
                CcEmptyState(
                    emoji = "🔔",
                    title = "All caught up!",
                    subtitle = "When researchers endorse your posts, cite your papers, or invite you to circles, you'll see them here."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    // Today Group
                    val todayNotifs = notifications.filter { it.timestamp > System.currentTimeMillis() - 86_400_000L }
                    val olderNotifs = notifications.filter { it.timestamp <= System.currentTimeMillis() - 86_400_000L }

                    if (todayNotifs.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Today")
                        }
                        items(todayNotifs, key = { it.id }) { notif ->
                            NotificationItemRow(
                                notification = notif,
                                onClick = {
                                    viewModel.markRead(notif.id)
                                    onNotificationClick(notif.targetId)
                                }
                            )
                        }
                    }

                    if (olderNotifs.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Earlier")
                        }
                        items(olderNotifs, key = { it.id }) { notif ->
                            NotificationItemRow(
                                notification = notif,
                                onClick = {
                                    viewModel.markRead(notif.id)
                                    onNotificationClick(notif.targetId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.ccColors.marginGray,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun NotificationItemRow(
    notification: Notification,
    onClick: () -> Unit
) {
    val icon = when (notification.type) {
        NotifType.ENDORSEMENT -> Icons.Outlined.StarOutline
        NotifType.COMMENT -> Icons.Outlined.ChatBubbleOutline
        NotifType.CONNECTION -> Icons.Filled.PersonAdd
        NotifType.CIRCLE_INVITE -> Icons.Filled.Groups
        NotifType.AI_APPROVED -> Icons.Filled.AutoAwesome
        NotifType.CITATION -> Icons.Filled.FormatQuote
        NotifType.NEW_FOLLOWER -> Icons.Filled.Person
    }

    val iconColor = when (notification.type) {
        NotifType.ENDORSEMENT -> CcColors.HighlighterYellow
        NotifType.COMMENT -> CcColors.CircleBlue
        NotifType.CONNECTION -> CcColors.SeafoamTeal
        NotifType.CIRCLE_INVITE -> CcColors.InkNavy
        NotifType.AI_APPROVED -> CcColors.SeafoamTeal
        NotifType.CITATION -> CcColors.HighlighterYellow
        NotifType.NEW_FOLLOWER -> CcColors.CircleBlue
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (notification.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Notification Badge status icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))
            CcTimestamp(timestamp = notification.timestamp)
        }

        if (!notification.isRead) {
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(CcColors.CoralPop)
            )
        }
    }
}
