package com.citecircle.app.feature.pdf

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citecircle.app.core.designsystem.CcColors
import com.citecircle.app.core.designsystem.CcPrimaryButton
import com.citecircle.app.core.designsystem.FrauncesFamily
import com.citecircle.app.core.designsystem.ccColors
import com.citecircle.app.core.model.AiPaperBreakdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSummarizerBottomSheet(
    breakdown: AiPaperBreakdown?,
    isLoading: Boolean,
    paperTitle: String,
    onDismiss: () -> Unit,
    onShareQuote: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboardManager = LocalClipboardManager.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        "Abstract TL;DR",
        "Methodology & Setup",
        "Core Results",
        "Limitations & Future Work"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(CcColors.CircleBlue, CcColors.SeafoamTeal)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Psychology,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI Executive Summarizer",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FrauncesFamily),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Instant paper breakdown · Powered by DeepSeek",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.ccColors.marginGray
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isLoading || breakdown == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CcColors.CircleBlue)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Analyzing paper methodology and results…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.ccColors.marginGray
                        )
                    }
                }
            } else {
                // Methodology Quality Index Card
                MethodologyQualityCard(breakdown = breakdown)

                Spacer(Modifier.height(20.dp))

                // Scrollable Tab Row for Section Breakdowns
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Tab Content Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.ccColors.paperCream.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        AnimatedContent(
                            targetState = selectedTabIndex,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "section_tab_content"
                        ) { target ->
                            val text = when (target) {
                                0 -> breakdown.abstractTldr
                                1 -> breakdown.methodologySetup
                                2 -> breakdown.coreResults
                                else -> breakdown.limitationsFutureWork
                            }
                            Column {
                                Text(
                                    text = text.ifEmpty { "Breakdown available after AI indexing." },
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(onClick = {
                                        clipboardManager.setText(AnnotatedString(text))
                                    }) {
                                        Icon(
                                            Icons.Outlined.ContentCopy,
                                            contentDescription = "Copy section summary",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(onClick = { onShareQuote(text) }) {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = "Share quote to feed",
                                            modifier = Modifier.size(18.dp),
                                            tint = CcColors.CircleBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Key Takeaways List Card
                if (breakdown.keyTakeaways.isNotEmpty()) {
                    Text(
                        text = "Key Takeaways & Contributions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(10.dp))

                    breakdown.keyTakeaways.forEachIndexed { idx, takeaway ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CcColors.CircleBlue)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = takeaway,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onShareQuote(takeaway) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share takeaway",
                                    tint = CcColors.CircleBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MethodologyQualityCard(breakdown: AiPaperBreakdown) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = CcColors.EmeraldGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Methodology Quality Index",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = breakdown.qualityLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.ccColors.marginGray
                )
            }

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            colors = listOf(CcColors.SeafoamTeal, CcColors.CircleBlue)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${breakdown.methodologyQualityIndex}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CcColors.InkNavy
                        )
                    }
                }
            }
        }
    }
}
