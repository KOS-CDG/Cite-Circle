package com.citecircle.app.feature.papers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citecircle.app.core.designsystem.CcCard
import com.citecircle.app.core.designsystem.CcColors
import com.citecircle.app.core.designsystem.ccColors
import com.citecircle.app.core.model.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// ──────────────────────────────────────────────────────────────────────────────
// Color Palette & Field Helpers
// ──────────────────────────────────────────────────────────────────────────────

private fun getFieldColor(field: String): Color {
    val f = field.lowercase()
    return when {
        "comp" in f || "ai" in f || "machine" in f -> Color(0xFF6C63FF)
        "hci" in f || "human" in f || "interface" in f -> Color(0xFF00B4D8)
        "quantum" in f || "physic" in f -> Color(0xFF7209B7)
        "bio" in f || "gene" in f || "life" in f -> Color(0xFF2A9D8F)
        "open" in f || "meta" in f -> Color(0xFFE76F51)
        else -> Color(0xFF5D82AE)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Interactive Citation Graph Canvas (Jetpack Compose)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun CitationGraphCanvas(
    nodes: List<CitationGraphNode>,
    edges: List<CitationGraphEdge>,
    selectedNodeId: String?,
    onNodeSelected: (CitationGraphNode) -> Unit,
    modifier: Modifier = Modifier,
    depth: Int = 2,
    onDepthChanged: ((Int) -> Unit)? = null
) {
    var scale by remember { mutableFloatStateOf(0.9f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.ccColors.paperCream.copy(alpha = 0.6f))
            .border(1.dp, CcColors.AcademicGold.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
    ) {
        // Controls Overlay
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = depth == 1,
                onClick = { onDepthChanged?.invoke(1) },
                label = { Text("1-Hop", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Spacer(modifier = Modifier.width(4.dp))
            FilterChip(
                selected = depth == 2,
                onClick = { onDepthChanged?.invoke(2) },
                label = { Text("2-Hop", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        IconButton(
            onClick = { scale = 0.9f; offset = Offset.Zero },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(Color.White.copy(alpha = 0.9f), CircleShape)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = "Reset View", tint = CcColors.DeepBurgundy)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.4f, 3.0f)
                        offset += pan
                    }
                }
                .pointerInput(nodes, offset, scale) {
                    detectTapGestures { tapOffset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val adjustedTap = (tapOffset - center - offset) / scale

                        nodes.find { node ->
                            val r = if (node.isCenter) 32f else (14 + sqrt(node.citationCount.toFloat()) * 1.2f).coerceIn(18f, 38f)
                            val dist = sqrt((adjustedTap.x - node.x) * (adjustedTap.x - node.x) + (adjustedTap.y - node.y) * (adjustedTap.y - node.y))
                            dist <= r
                        }?.let { clickedNode ->
                            onNodeSelected(clickedNode)
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Hop rings
            drawCircle(
                color = CcColors.AcademicGold.copy(alpha = 0.25f),
                radius = 180f * scale,
                center = center + offset,
                style = Stroke(width = 1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
            )
            drawCircle(
                color = CcColors.AcademicGold.copy(alpha = 0.15f),
                radius = 340f * scale,
                center = center + offset,
                style = Stroke(width = 1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
            )

            val nodePosMap = nodes.associate { n ->
                val r = if (n.isCenter) 32.dp.toPx() else (14 + sqrt(n.citationCount.toFloat()) * 1.2f).coerceIn(18f, 38f)
                n.id to (center + offset + Offset(n.x, n.y) * scale to r)
            }

            // Edges with arrows
            edges.forEach { edge ->
                val srcPair = nodePosMap[edge.source]
                val tgtPair = nodePosMap[edge.target]
                if (srcPair != null && tgtPair != null) {
                    val (srcPos, srcR) = srcPair
                    val (tgtPos, tgtR) = tgtPair
                    val isHighlighted = selectedNodeId == edge.source || selectedNodeId == edge.target

                    val dx = tgtPos.x - srcPos.x
                    val dy = tgtPos.y - srcPos.y
                    val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                    val normX = dx / dist
                    val normY = dy / dist

                    val start = srcPos + Offset(normX * srcR * scale, normY * srcR * scale)
                    val end = tgtPos - Offset(normX * (tgtR + 6f) * scale, normY * (tgtR + 6f) * scale)

                    val edgeColor = if (isHighlighted) CcColors.CoralPop else CcColors.AcademicGold
                    val strokeW = if (isHighlighted) 3.dp.toPx() else 1.5.dp.toPx()

                    drawLine(
                        color = edgeColor.copy(alpha = if (isHighlighted) 0.95f else 0.45f),
                        start = start,
                        end = end,
                        strokeWidth = strokeW
                    )

                    // Draw Arrowhead
                    val angle = atan2(dy, dx)
                    val arrowLen = 14f * scale
                    val arrowAngle = Math.PI / 6
                    val path = Path().apply {
                        moveTo(end.x, end.y)
                        lineTo(
                            (end.x - arrowLen * cos(angle - arrowAngle)).toFloat(),
                            (end.y - arrowLen * sin(angle - arrowAngle)).toFloat()
                        )
                        lineTo(
                            (end.x - arrowLen * cos(angle + arrowAngle)).toFloat(),
                            (end.y - arrowLen * sin(angle + arrowAngle)).toFloat()
                        )
                        close()
                    }
                    drawPath(path, color = edgeColor)
                }
            }

            // Nodes
            nodes.forEach { node ->
                val posPair = nodePosMap[node.id] ?: return@forEach
                val (nodePos, radius) = posPair
                val isSelected = selectedNodeId == node.id
                val color = getFieldColor(node.field)

                // Aura ring for center/selected
                if (node.isCenter || isSelected) {
                    drawCircle(
                        color = if (node.isCenter) CcColors.AcademicGold else CcColors.CoralPop,
                        radius = (radius + 8f) * scale,
                        center = nodePos,
                        alpha = 0.3f
                    )
                }

                drawCircle(
                    color = color,
                    radius = radius * scale,
                    center = nodePos
                )

                drawCircle(
                    color = if (node.isCenter) CcColors.AcademicGold else if (isSelected) CcColors.CoralPop else Color.White,
                    radius = radius * scale,
                    center = nodePos,
                    style = Stroke(width = if (node.isCenter || isSelected) 3.dp.toPx() else 1.5.dp.toPx())
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Co-Author Network Canvas (Jetpack Compose)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun CoAuthorNetworkCanvas(
    nodes: List<CoauthorGraphNode>,
    edges: List<CoauthorGraphEdge>,
    clusters: List<CoauthorCluster>,
    selectedNodeId: String?,
    onNodeSelected: (CoauthorGraphNode) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(0.9f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.ccColors.paperCream.copy(alpha = 0.6f))
            .border(1.dp, CcColors.AcademicGold.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.4f, 3.0f)
                        offset += pan
                    }
                }
                .pointerInput(nodes, offset, scale) {
                    detectTapGestures { tapOffset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val adjustedTap = (tapOffset - center - offset) / scale

                        nodes.find { node ->
                            val r = if (node.isCenter) 28f else 22f
                            val dist = sqrt((adjustedTap.x - node.x) * (adjustedTap.x - node.x) + (adjustedTap.y - node.y) * (adjustedTap.y - node.y))
                            dist <= r
                        }?.let { clicked -> onNodeSelected(clicked) }
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Cluster Background Hulls
            clusters.forEach { cluster ->
                val members = nodes.filter { cluster.memberIds.contains(it.id) }
                if (members.isNotEmpty()) {
                    val avgX = members.map { it.x }.average().toFloat()
                    val avgY = members.map { it.y }.average().toFloat()
                    val clusterCenter = center + offset + Offset(avgX, avgY) * scale
                    drawCircle(
                        color = Color(0xFF6C63FF),
                        radius = 110f * scale,
                        center = clusterCenter,
                        alpha = 0.08f
                    )
                }
            }

            val nodePosMap = nodes.associate { n ->
                val r = if (n.isCenter) 28.dp.toPx() else 22.dp.toPx()
                n.id to (center + offset + Offset(n.x, n.y) * scale to r)
            }

            // Weighted Edges
            edges.forEach { edge ->
                val srcPair = nodePosMap[edge.source]
                val tgtPair = nodePosMap[edge.target]
                if (srcPair != null && tgtPair != null) {
                    val isHighlighted = selectedNodeId == edge.source || selectedNodeId == edge.target
                    val strokeW = (1.5f + edge.weight * 1.2f).coerceAtMost(6f) * scale

                    drawLine(
                        color = if (isHighlighted) CcColors.CoralPop else Color(0xFF6C63FF),
                        start = srcPair.first,
                        end = tgtPair.first,
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round,
                        alpha = if (isHighlighted) 0.9f else 0.45f
                    )
                }
            }

            // Nodes
            nodes.forEach { node ->
                val posPair = nodePosMap[node.id] ?: return@forEach
                val (nodePos, radius) = posPair
                val isSelected = selectedNodeId == node.id

                drawCircle(
                    color = Color(0xFF6C63FF),
                    radius = radius * scale,
                    center = nodePos
                )
                drawCircle(
                    color = if (node.isCenter) CcColors.AcademicGold else if (isSelected) CcColors.CoralPop else Color.White,
                    radius = radius * scale,
                    center = nodePos,
                    style = Stroke(width = if (node.isCenter || isSelected) 3.dp.toPx() else 2.dp.toPx())
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Researcher Impact Analytics Card
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun ResearcherImpactCard(
    analytics: ResearcherAnalytics,
    authorName: String = "Researcher",
    modifier: Modifier = Modifier
) {
    CcCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Impact Analytics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(authorName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ccColors.marginGray)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricBox("Total Citations", analytics.totalCitations.toString(), Color(0xFF6C63FF))
                MetricBox("h-index", analytics.hIndex.toString(), Color(0xFF2A9D8F))
                MetricBox("i10-index", analytics.i10Index.toString(), Color(0xFFE76F51))
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Citation Velocity Graph", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(8.dp))
            val velocity = analytics.citationVelocity
            val maxVal = (velocity.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(MaterialTheme.ccColors.paperCream.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                if (velocity.size > 1) {
                    val stepX = size.width / (velocity.size - 1)
                    val points = velocity.mapIndexed { i, v ->
                        Offset(
                            x = i * stepX,
                            y = size.height - (v.count.toFloat() / maxVal) * (size.height - 24f) - 12f
                        )
                    }

                    val path = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }
                    drawPath(path, color = CcColors.CoralPop, style = Stroke(width = 3.dp.toPx()))

                    points.forEach { p ->
                        drawCircle(color = CcColors.CoralPop, radius = 5.dp.toPx(), center = p)
                        drawCircle(color = Color.White, radius = 3.dp.toPx(), center = p)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.MetricBox(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 4.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, CcColors.AcademicGold.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.ccColors.marginGray)
    }
}
