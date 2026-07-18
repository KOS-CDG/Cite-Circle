package com.citecircle.app.feature.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.Paper
import com.citecircle.app.core.model.Post
import com.citecircle.app.core.model.PostFlair
import com.citecircle.app.core.model.PostType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostCard(
    post: Post,
    onPostClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onCircleClick: (String) -> Unit,
    onEndorse: (String) -> Unit,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    CcCard(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(post.id) {
                detectTapGestures(
                    onDoubleTap = { onEndorse(post.id) },
                    onTap = { onPostClick(post.id) }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Milestone check
            if (post.type == PostType.MILESTONE && post.milestoneText != null) {
                MilestoneHeader(post.milestoneText)
                return@Column
            }

            // Normal Post header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                CcAvatar(
                    user = post.author,
                    size = 40.dp,
                    modifier = Modifier.clickable { onUserClick(post.author.id) }
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.author.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onUserClick(post.author.id) }
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Role Emoji/Badge
                        Text(
                            text = post.author.role.emoji(),
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = "${post.author.institution} • ${formatRelativeTime(post.timestamp)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.ccColors.marginGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Optional Circle destination tag
                if (post.circleId != null && post.circleName != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .clickable { onCircleClick(post.circleId) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "c/${post.circleName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Flair
            if (post.flair != PostFlair.NONE) {
                FlairChip(post.flair)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Post content body
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            // Optional attached paper
            if (post.type == PostType.PAPER_SHARE && post.attachedPaper != null) {
                Spacer(modifier = Modifier.height(12.dp))
                PaperMiniCard(paper = post.attachedPaper, onUserClick = onUserClick)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Endorse
                EndorseButton(
                    isEndorsed = post.isEndorsed,
                    endorseCount = post.endorseCount,
                    onEndorse = { onEndorse(post.id) }
                )

                // Comment
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onPostClick(post.id) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comments count",
                        tint = MaterialTheme.ccColors.marginGray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = post.commentCount.toString(),
                        color = MaterialTheme.ccColors.marginGray,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Share
                val clipboardManager = LocalClipboardManager.current
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString("https://citecircle.app/post/${post.id}"))
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Share,
                        contentDescription = "Share post",
                        tint = MaterialTheme.ccColors.marginGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Save
                IconButton(onClick = { onSave(post.id) }) {
                    Icon(
                        imageVector = if (post.isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "Save post",
                        tint = if (post.isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.ccColors.marginGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FlairChip(flair: PostFlair) {
    val text = when (flair) {
        PostFlair.QUESTION -> "Question"
        PostFlair.DISCUSSION -> "Discussion"
        PostFlair.PAPER_FEEDBACK -> "Paper Feedback"
        PostFlair.RESOURCE -> "Resource"
        PostFlair.NONE -> ""
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(CcColors.HighlighterYellow.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CcColors.InkNavy
        )
    }
}

@Composable
fun MilestoneHeader(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "🎉", fontSize = 28.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun PaperMiniCard(
    paper: Paper,
    onUserClick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(1.dp, MaterialTheme.ccColors.divider, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = paper.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = paper.authors.joinToString { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.ccColors.marginGray
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Citation count highlight block
                HighlighterSweep {
                    Box(
                        modifier = Modifier
                            .background(CcColors.HighlighterYellow)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${paper.citationCount} Citations",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CcColors.InkNavy
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Expandable abstract
            Text(
                text = paper.abstract,
                style = MaterialTheme.typography.bodySmall,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = if (expanded) "Read less" else "Read more...",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { expanded = !expanded }
                    .padding(top = 4.dp)
            )

            // Monospace DOI
            if (paper.doi.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "DOI: ${paper.doi}",
                    fontSize = 11.sp,
                    fontFamily = JetBrainsMonoFamily,
                    color = MaterialTheme.ccColors.marginGray
                )
            }
        }
    }
}

fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 30 -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
        days > 0 -> "${days}d ago"
        hours > 0 -> "${hours}h ago"
        minutes > 0 -> "${minutes}m ago"
        else -> "Just now"
    }
}
