package com.citecircle.app.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.citecircle.app.core.model.User
import com.citecircle.app.core.model.UserRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// ──────────────────────────────────────────────────────────────────────────────
// HighlighterSweep — the signature visual motif
// A hand-drawn-feeling yellow highlight behind text
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun HighlighterSweep(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.ccColors.highlighterYellowAlpha,
    rotationDegrees: Float = -1.5f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        // The yellow highlight layer (behind content)
        Box(
            modifier = Modifier
                .matchParentSize()
                .layout { measurable, constraints ->
                    val extraWidth = 6.dp.roundToPx()
                    val extraHeight = 2.dp.roundToPx()
                    
                    val newMinWidth = (constraints.minWidth + extraWidth).coerceAtLeast(0)
                    val newMaxWidth = if (constraints.hasBoundedWidth) {
                        (constraints.maxWidth + extraWidth).coerceAtLeast(newMinWidth)
                    } else {
                        constraints.maxWidth
                    }
                    
                    val newMinHeight = (constraints.minHeight + extraHeight).coerceAtLeast(0)
                    val newMaxHeight = if (constraints.hasBoundedHeight) {
                        (constraints.maxHeight + extraHeight).coerceAtLeast(newMinHeight)
                    } else {
                        constraints.maxHeight
                    }
                    
                    val placeable = measurable.measure(
                        constraints.copy(
                            minWidth = newMinWidth,
                            maxWidth = newMaxWidth,
                            minHeight = newMinHeight,
                            maxHeight = newMaxHeight
                        )
                    )
                    
                    val layoutWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else (placeable.width - extraWidth).coerceAtLeast(0)
                    val layoutHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else (placeable.height - extraHeight).coerceAtLeast(0)
                    
                    layout(layoutWidth, layoutHeight) {
                        placeable.place(-extraWidth / 2, -extraHeight / 2)
                    }
                }
                .rotate(rotationDegrees)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        content()
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// CcAvatar — with role ring
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun CcAvatar(
    user: User,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
    showRing: Boolean = true
) {
    val ringColor = when {
        user.role == UserRole.ADMIN -> CcColors.CoralPop
        user.role == UserRole.EDUCATOR -> CcColors.ProfessorRing
        user.isVerified -> CcColors.VerifiedRing
        else -> CcColors.StudentRing
    }

    Box(
        modifier = modifier.size(if (showRing) size + 4.dp else size),
        contentAlignment = Alignment.Center
    ) {
        if (showRing) {
            Box(
                modifier = Modifier
                    .size(size + 4.dp)
                    .clip(CircleShape)
                    .background(ringColor)
            )
        }
        AsyncImage(
            model = user.avatarUrl.ifEmpty { "https://api.dicebear.com/8.x/avataaars/svg?seed=${user.id}" },
            contentDescription = "${user.name}'s avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// CcBadge — notification count badge
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun CcBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            .clip(CircleShape)
            .background(CcColors.CoralPop),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// CcCard — index card style with press-scale
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun CcCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "card_scale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 0.5.dp)
    ) {
        Column(content = content)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// CcPrimaryButton — with press-scale spring animation
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun CcPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    isLoading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "btn_scale"
    )

    Button(
        onClick = onClick,
        modifier = modifier.scale(scale).defaultMinSize(minHeight = 52.dp),
        enabled = enabled && !isLoading,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// CcSecondaryButton
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun CcSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "sec_btn_scale"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.scale(scale).defaultMinSize(minHeight = 52.dp),
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        border = ButtonDefaults.outlinedButtonBorder(enabled = enabled),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// CcChip — with optional highlight-sweep selected state
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun CcChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "chip_text"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        else
            MaterialTheme.colorScheme.surfaceVariant,
        label = "chip_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else
            Color.Transparent,
        label = "chip_border"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .then(
                if (selected) Modifier.border(
                    width = 1.5.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(20.dp)
                ) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// CcTextField
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun CcTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    helperText: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    maxLines: Int = 1,
    singleLine: Boolean = true,
    placeholder: String = ""
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = if (placeholder.isNotEmpty()) ({ Text(placeholder, color = MaterialTheme.ccColors.marginGray) }) else null,
            isError = isError,
            singleLine = singleLine,
            maxLines = maxLines,
            leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = CcColors.CoralPop
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = CcColors.CoralPop,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        } else if (helperText != null) {
            Text(
                text = helperText,
                color = MaterialTheme.ccColors.marginGray,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// CcSearchBar
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun CcSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search papers, people, circles…",
    onSearch: () -> Unit = {},
    trailingContent: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, color = MaterialTheme.ccColors.marginGray) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search", tint = MaterialTheme.ccColors.marginGray) },
        trailingIcon = if (query.isNotEmpty()) ({
            IconButton(onClick = { onQueryChange("") }) {
                Icon(Icons.Filled.Close, contentDescription = "Clear search")
            }
        }) else trailingContent,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.fillMaxWidth()
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// CcEmptyState
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun CcEmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = emoji, fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.ccColors.marginGray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (actionLabel != null) {
            Spacer(Modifier.height(24.dp))
            CcPrimaryButton(text = actionLabel, onClick = onAction, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// CcLoadingShimmer
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
    )
}

@Composable
fun CcPostShimmer() {
    CcCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerBox(Modifier.size(44.dp).clip(CircleShape))
                Spacer(Modifier.width(12.dp))
                Column {
                    ShimmerBox(Modifier.width(140.dp).height(14.dp))
                    Spacer(Modifier.height(6.dp))
                    ShimmerBox(Modifier.width(100.dp).height(10.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            ShimmerBox(Modifier.fillMaxWidth().height(14.dp))
            Spacer(Modifier.height(6.dp))
            ShimmerBox(Modifier.fillMaxWidth(0.85f).height(14.dp))
            Spacer(Modifier.height(6.dp))
            ShimmerBox(Modifier.fillMaxWidth(0.7f).height(14.dp))
            Spacer(Modifier.height(12.dp))
            Row {
                ShimmerBox(Modifier.width(60.dp).height(28.dp).clip(RoundedCornerShape(20.dp)))
                Spacer(Modifier.width(8.dp))
                ShimmerBox(Modifier.width(60.dp).height(28.dp).clip(RoundedCornerShape(20.dp)))
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// CcCircleShimmer — skeleton placeholder for circle grid cards while loading
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun CcCircleShimmer() {
    CcCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header row: emoji badge + join button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                ShimmerBox(Modifier.width(64.dp).height(28.dp).clip(RoundedCornerShape(8.dp)))
            }
            // Name and category lines
            Column {
                ShimmerBox(Modifier.fillMaxWidth(0.75f).height(16.dp))
                Spacer(Modifier.height(8.dp))
                ShimmerBox(Modifier.fillMaxWidth(0.45f).height(12.dp))
            }
            // Activity bar placeholder
            ShimmerBox(Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(4.dp)))
            // Footer stats
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShimmerBox(Modifier.width(70.dp).height(12.dp))
                ShimmerBox(Modifier.width(70.dp).height(12.dp))
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// CcTimestamp — relative time label with long-press tooltip for full date
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CcTimestamp(
    timestamp: Long,
    modifier: Modifier = Modifier
) {
    var showTooltip by remember { mutableStateOf(false) }
    val relativeText = remember(timestamp) { formatTimestamp(timestamp) }
    val absoluteText = remember(timestamp) {
        SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    Box(modifier = modifier) {
        Text(
            text = relativeText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.ccColors.marginGray,
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = { showTooltip = !showTooltip }
            )
        )
        if (showTooltip) {
            // Dismiss tooltip on click anywhere
            LaunchedEffect(Unit) {
                delay(3000)
                showTooltip = false
            }
            Box(
                modifier = Modifier
                    .offset(y = 20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.inverseSurface)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clickable { showTooltip = false }
            ) {
                Text(
                    text = absoluteText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        }
    }
}

/** Shared timestamp formatter — same logic as [formatRelativeTime] in PostCard.kt */
private fun formatTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        days > 30 -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
        days > 0  -> "${days}d ago"
        hours > 0 -> "${hours}h ago"
        minutes > 0 -> "${minutes}m ago"
        else -> "Just now"
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// EndorseButton — with confetti burst animation
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun EndorseButton(
    isEndorsed: Boolean,
    endorseCount: Int,
    onEndorse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showBurst by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val colorAnim by animateColorAsState(
        targetValue = if (isEndorsed) CcColors.HighlighterYellow else MaterialTheme.ccColors.marginGray,
        animationSpec = tween(200),
        label = "endorse_color"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (isEndorsed) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "endorse_scale"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Confetti burst
        if (showBurst) {
            ConfettiBurst(color = CcColors.HighlighterYellow)
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable {
                    onEndorse()
                    if (!isEndorsed) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showBurst = true
                        scope.launch {
                            delay(600)
                            showBurst = false
                        }
                    }
                }
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .scale(scaleAnim),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isEndorsed) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = if (isEndorsed) "Remove endorsement" else "Endorse",
                tint = colorAnim,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (endorseCount > 0) endorseCount.toString() else "Endorse",
                color = colorAnim,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ConfettiBurst(color: Color) {
    val particleCount = 8
    var target by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        target = 1f
    }
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "confetti_progress"
    )

    Canvas(modifier = Modifier.size(60.dp)) {
        val cx = size.width / 2
        val cy = size.height / 2
        repeat(particleCount) { i ->
            val angle = (i.toFloat() / particleCount) * 360f
            val radian = Math.toRadians(angle.toDouble())
            val distance = progress * 24.dp.toPx()
            val px = cx + (cos(radian) * distance).toFloat()
            val py = cy + (sin(radian) * distance).toFloat()
            val size = (4.dp.toPx() * (1 - progress))
            drawCircle(
                color = if (i % 2 == 0) color else CcColors.CoralPop,
                radius = size,
                center = Offset(px, py),
                alpha = 1 - progress
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// AiPencilProgress — animated pencil "writing" indicator
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun AiPencilProgress(
    modifier: Modifier = Modifier,
    color: Color = CcColors.HighlighterYellow
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pencil")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pencil_progress"
    )
    val bobble by infiniteTransition.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pencil_bobble"
    )

    Canvas(modifier = modifier.size(120.dp, 48.dp)) {
        val lineY = size.height / 2 + 8.dp.toPx()
        val lineStart = 20.dp.toPx()
        val lineEnd = size.width - 20.dp.toPx()
        val lineLength = lineEnd - lineStart
        val pencilX = lineStart + progress * lineLength

        // Drawn line (grows from left)
        drawLine(
            color = color,
            start = Offset(lineStart, lineY),
            end = Offset(pencilX, lineY),
            strokeWidth = 2.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        // Pencil icon at current position
        val py = lineY - 12.dp.toPx() + bobble
        drawRect(
            color = color,
            topLeft = Offset(pencilX - 3.dp.toPx(), py),
            size = Size(6.dp.toPx(), 14.dp.toPx()),
            style = Stroke(width = 1.5f)
        )
        // Pencil tip
        val path = Path().apply {
            moveTo(pencilX - 3.dp.toPx(), py + 14.dp.toPx())
            lineTo(pencilX + 3.dp.toPx(), py + 14.dp.toPx())
            lineTo(pencilX, py + 18.dp.toPx())
            close()
        }
        drawPath(path, color = color)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// CcTabRow — with highlight-sweep active tab
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun CcTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 16.dp,
        indicator = { tabPositions ->
            // Custom underline indicator in CircleBlue
            if (selectedIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    color = CcColors.CircleBlue,
                    height = 3.dp
                )
            }
        }
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    if (selectedIndex == index) {
                        HighlighterSweep {
                            Text(
                                text = tab,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            text = tab,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.ccColors.marginGray
                        )
                    }
                }
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Role badge text
// ──────────────────────────────────────────────────────────────────────────────

fun UserRole.displayName() = when (this) {
    UserRole.STUDENT -> "Student"
    UserRole.EDUCATOR -> "Educator"
    UserRole.RESEARCHER -> "Researcher"
    UserRole.ADMIN -> "Super Admin"
}

fun UserRole.emoji() = when (this) {
    UserRole.STUDENT -> "🎓"
    UserRole.EDUCATOR -> "📖"
    UserRole.RESEARCHER -> "🔬"
    UserRole.ADMIN -> "🛡️"
}
