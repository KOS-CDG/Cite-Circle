package com.citecircle.app.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.OrcidSyncState
import com.citecircle.app.core.model.Publication
import com.citecircle.app.core.model.SyncStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Header for the Publications tab: ORCID status plus the manual sync control.
 *
 * The button stays enabled while a cooldown is active — the server owns that
 * rule and returns a message explaining it, which reads better than a dead
 * button with no explanation.
 */
@Composable
fun OrcidSyncHeader(
    orcidId: String,
    syncState: OrcidSyncState,
    publicationCount: Int,
    totalCitations: Int,
    onSync: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSyncing = syncState.status == SyncStatus.RUNNING

    CcCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ORCID",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.ccColors.marginGray,
                    )
                    Text(
                        text = orcidId.ifBlank { "Not linked" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (orcidId.isBlank()) {
                            MaterialTheme.ccColors.marginGray
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }

                if (orcidId.isNotBlank()) {
                    FilledTonalButton(
                        onClick = onSync,
                        enabled = !isSyncing,
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        if (isSyncing) {
                            // Continuous rotation reads as "working" without a
                            // determinate progress value the server can't give us.
                            val rotation by rememberInfiniteTransition(label = "sync").animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                ),
                                label = "sync_rotation",
                            )
                            Icon(
                                imageVector = Icons.Outlined.Sync,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(rotation),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isSyncing -> "Syncing"
                                syncState.isResumable -> "Continue"
                                syncState.hasSynced -> "Refresh"
                                else -> "Sync"
                            },
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            if (orcidId.isBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Add your ORCID iD in Edit Profile to pull your publications and citation counts from OpenAlex.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.ccColors.marginGray,
                )
            } else {
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    StatItem(count = publicationCount, label = "Publications")
                    StatItem(count = totalCitations, label = "Citations")
                }

                syncState.lastSuccessAt?.takeIf { it > 0L }?.let { syncedAt ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Last synced ${formatSyncTime(syncedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.ccColors.marginGray,
                    )
                }

                if (syncState.isResumable) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Partially synced — tap Continue to fetch the rest.",
                        style = MaterialTheme.typography.labelSmall,
                        color = CcColors.HighlighterYellow,
                    )
                }

                if (syncState.status == SyncStatus.FAILED && syncState.lastError.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = CcColors.CoralPop,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = syncState.lastError,
                            style = MaterialTheme.typography.labelSmall,
                            color = CcColors.CoralPop,
                        )
                    }
                }
            }
        }
    }
}

/** One externally indexed work. Tapping opens the OA copy or the DOI. */
@Composable
fun PublicationCard(
    publication: Publication,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    CcCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { publication.externalUrl.takeIf { it.isNotBlank() }?.let(onOpen) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = publication.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = listOfNotNull(
                    publication.journal.takeIf { it.isNotBlank() },
                    publication.year?.toString(),
                ).joinToString(" • "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.ccColors.marginGray,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${publication.citationCount} citations",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CcColors.CircleBlue,
                )

                if (publication.isOpenAccess) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        color = CcColors.SeafoamTeal.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = "OPEN ACCESS",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CcColors.SeafoamTeal,
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (publication.externalUrl.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = "Open publication",
                        tint = MaterialTheme.ccColors.marginGray,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
    }
}

private fun formatSyncTime(epochMillis: Long): String {
    val elapsed = System.currentTimeMillis() - epochMillis
    return when {
        elapsed < 60_000 -> "just now"
        elapsed < 3_600_000 -> "${elapsed / 60_000}m ago"
        elapsed < 86_400_000 -> "${elapsed / 3_600_000}h ago"
        elapsed < 604_800_000 -> "${elapsed / 86_400_000}d ago"
        else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMillis))
    }
}
