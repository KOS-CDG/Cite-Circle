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
import com.citecircle.app.core.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow

// ──────────────────────────────────────────────────────────────────────────────
// NotificationsViewModel
// ──────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val notifications: StateFlow<List<Notification>> = notificationRepository.getNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    fun refreshNotifications() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(800)
            _isRefreshing.value = false
        }
    }

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
// NotificationGroup
// ──────────────────────────────────────────────────────────────────────────────

data class NotificationGroup(
    val type: NotifType,
    val targetId: String,
    val notifications: List<Notification>
) {
    val id: String = "${type.name}_$targetId"
    val isRead: Boolean get() = notifications.all { it.isRead }
    val latestTimestamp: Long get() = notifications.maxOf { it.timestamp }
    val latestActor: User get() = notifications.maxByOrNull { it.timestamp }?.actor ?: notifications.first().actor

    fun getSummaryText(): String {
        if (notifications.size == 1) return notifications.first().content
        val name = latestActor.name
        val count = notifications.size - 1
        val others = if (count == 1) "1 other" else "$count others"
        return when (type) {
            NotifType.ENDORSEMENT -> "$name and $others endorsed your post"
            NotifType.COMMENT -> "$name and $others commented on your post"
            NotifType.CITATION -> "$name and $others cited your paper"
            NotifType.CIRCLE_INVITE -> "$name and $others invited you to circles"
            NotifType.NEW_FOLLOWER -> "$name and $others started following you"
            NotifType.CONNECTION -> "$name and $others connected with you"
            else -> notifications.first().content
        }
    }
}

private fun groupNotifications(notifications: List<Notification>): List<NotificationGroup> {
    val groupsMap = mutableMapOf<String, MutableList<Notification>>()
    notifications.forEach { notif ->
        val key = if (notif.targetId.isBlank()) {
            notif.id // Unique key for ungrouped items
        } else {
            "${notif.type.name}_${notif.targetId}"
        }
        groupsMap.getOrPut(key) { mutableListOf() }.add(notif)
    }
    return groupsMap.values.map {
        val sorted = it.sortedByDescending { n -> n.timestamp }
        NotificationGroup(
            type = sorted.first().type,
            targetId = sorted.first().targetId,
            notifications = sorted
        )
    }.sortedByDescending { it.latestTimestamp }
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
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var expandedGroups by remember { mutableStateOf(setOf<String>()) }

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
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshNotifications() },
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
                val groupedNotifs = remember(notifications) { groupNotifications(notifications) }
                val todayGroups = groupedNotifs.filter { it.latestTimestamp > System.currentTimeMillis() - 86_400_000L }
                val olderGroups = groupedNotifs.filter { it.latestTimestamp <= System.currentTimeMillis() - 86_400_000L }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    if (todayGroups.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Today")
                        }
                        todayGroups.forEach { group ->
                            renderGroup(
                                group = group,
                                expandedGroups = expandedGroups,
                                onToggleExpand = { id ->
                                    expandedGroups = if (expandedGroups.contains(id)) {
                                        expandedGroups - id
                                    } else {
                                        expandedGroups + id
                                    }
                                },
                                onNotificationClick = { notif ->
                                    viewModel.markRead(notif.id)
                                    onNotificationClick(notif.targetId)
                                }
                            )
                        }
                    }

                    if (olderGroups.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Earlier")
                        }
                        olderGroups.forEach { group ->
                            renderGroup(
                                group = group,
                                expandedGroups = expandedGroups,
                                onToggleExpand = { id ->
                                    expandedGroups = if (expandedGroups.contains(id)) {
                                        expandedGroups - id
                                    } else {
                                        expandedGroups + id
                                    }
                                },
                                onNotificationClick = { notif ->
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

private fun LazyListScope.renderGroup(
    group: NotificationGroup,
    expandedGroups: Set<String>,
    onToggleExpand: (String) -> Unit,
    onNotificationClick: (Notification) -> Unit
) {
    if (group.notifications.size == 1) {
        item(key = group.notifications.first().id) {
            NotificationItemRow(
                notification = group.notifications.first(),
                onClick = { onNotificationClick(group.notifications.first()) }
            )
        }
    } else {
        val isExpanded = expandedGroups.contains(group.id)
        item(key = group.id) {
            GroupHeaderRow(
                group = group,
                isExpanded = isExpanded,
                onClick = { onToggleExpand(group.id) }
            )
        }
        if (isExpanded) {
            items(group.notifications, key = { it.id }) { notif ->
                NotificationItemRow(
                    notification = notif,
                    onClick = { onNotificationClick(notif) },
                    modifier = Modifier.padding(start = 24.dp) // Slight indent for grouped items
                )
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
fun GroupHeaderRow(
    group: NotificationGroup,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "group_arrow"
    )

    val icon = getNotificationIcon(group.type)
    val iconColor = getNotificationIconColor(group.type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (group.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f))
            .drawBehind {
                if (!group.isRead) {
                    drawRect(
                        color = CcColors.CircleBlue,
                        topLeft = Offset.Zero,
                        size = Size(4.dp.toPx(), size.height)
                    )
                }
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Notification Badge icon
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
                text = group.getSummaryText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (!group.isRead) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${group.notifications.size} updates",
                    style = MaterialTheme.typography.labelSmall,
                    color = CcColors.CircleBlue,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " • ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.ccColors.marginGray
                )
                CcTimestamp(timestamp = group.latestTimestamp)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.ccColors.marginGray,
            modifier = Modifier.graphicsLayer { rotationZ = arrowRotation }
        )
    }
}

@Composable
fun NotificationItemRow(
    notification: Notification,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = getNotificationIcon(notification.type)
    val iconColor = getNotificationIconColor(notification.type)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (notification.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f))
            .drawBehind {
                if (!notification.isRead) {
                    drawRect(
                        color = CcColors.CircleBlue,
                        topLeft = Offset.Zero,
                        size = Size(4.dp.toPx(), size.height)
                    )
                }
            }
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

private fun getNotificationIcon(type: NotifType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        NotifType.ENDORSEMENT -> Icons.Outlined.StarOutline
        NotifType.COMMENT -> Icons.Outlined.ChatBubbleOutline
        NotifType.CONNECTION -> Icons.Filled.PersonAdd
        NotifType.CIRCLE_INVITE -> Icons.Filled.Groups
        NotifType.AI_APPROVED -> Icons.Filled.AutoAwesome
        NotifType.CITATION -> Icons.Filled.FormatQuote
        NotifType.NEW_FOLLOWER -> Icons.Filled.Person
    }
}

private fun getNotificationIconColor(type: NotifType): Color {
    return when (type) {
        NotifType.ENDORSEMENT -> CcColors.HighlighterYellow
        NotifType.COMMENT -> CcColors.CircleBlue
        NotifType.CONNECTION -> CcColors.SeafoamTeal
        NotifType.CIRCLE_INVITE -> CcColors.InkNavy
        NotifType.AI_APPROVED -> CcColors.SeafoamTeal
        NotifType.CITATION -> CcColors.HighlighterYellow
        NotifType.NEW_FOLLOWER -> CcColors.CircleBlue
    }
}
