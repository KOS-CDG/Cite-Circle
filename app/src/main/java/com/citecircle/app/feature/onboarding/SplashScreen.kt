package com.citecircle.app.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.citecircle.app.core.data.TokenManager
import com.citecircle.app.core.designsystem.CcColors
import com.citecircle.app.core.designsystem.FrauncesFamily
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenManager: TokenManager
) : ViewModel() {
    suspend fun checkAuthState(): Boolean = tokenManager.isLoggedIn()
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SplashScreen(
    onSplashComplete: (isLoggedIn: Boolean) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val infiniteTransition = rememberInfiniteTransition(label = "prepping_orbit")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val progress = remember { Animatable(0f) }
    var preppingStageText by remember { mutableStateOf("• Restoring scholar credentials...") }

    LaunchedEffect(Unit) {
        // Animate initial prepping progress
        progress.animateTo(
            targetValue = 0.35f,
            animationSpec = tween(600, easing = LinearOutSlowInEasing)
        )
        preppingStageText = "• Syncing research circles & papers..."
        delay(400)

        progress.animateTo(
            targetValue = 0.75f,
            animationSpec = tween(600, easing = LinearOutSlowInEasing)
        )
        preppingStageText = "• Initializing AI Copilot engine..."
        delay(400)

        progress.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
        preppingStageText = "• Workspace Ready!"
        
        val isLoggedIn = viewModel.checkAuthState()
        delay(300)
        onSplashComplete(isLoggedIn)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CcColors.PaperCream),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Animated logo with orbiting citation nodes & glowing ring
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = (size.width / 2) - 16.dp.toPx()

                    // Background track ring
                    drawCircle(
                        color = CcColors.CircleBlue.copy(alpha = 0.15f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 4.dp.toPx())
                    )

                    // Active progress sweep arc
                    drawArc(
                        color = CcColors.CircleBlue,
                        startAngle = -90f,
                        sweepAngle = 360f * progress.value,
                        useCenter = false,
                        style = Stroke(width = 5.dp.toPx()),
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                    )

                    // 3 Orbiting citation nodes
                    val angleRad1 = Math.toRadians(rotationAngle.toDouble())
                    val angleRad2 = Math.toRadians((rotationAngle + 120).toDouble())
                    val angleRad3 = Math.toRadians((rotationAngle + 240).toDouble())

                    val node1 = Offset(
                        (center.x + radius * cos(angleRad1)).toFloat(),
                        (center.y + radius * sin(angleRad1)).toFloat()
                    )
                    val node2 = Offset(
                        (center.x + radius * cos(angleRad2)).toFloat(),
                        (center.y + radius * sin(angleRad2)).toFloat()
                    )
                    val node3 = Offset(
                        (center.x + radius * cos(angleRad3)).toFloat(),
                        (center.y + radius * sin(angleRad3)).toFloat()
                    )

                    drawCircle(color = CcColors.HighlighterYellow, radius = 6.dp.toPx(), center = node1)
                    drawCircle(color = CcColors.CircleBlue, radius = 5.dp.toPx(), center = node2)
                    drawCircle(color = CcColors.SeafoamTeal, radius = 4.dp.toPx(), center = node3)
                }

                // Center Quotation mark icon badge with newly generated Cite Circle logo
                Surface(
                    shape = CircleShape,
                    color = CcColors.InkNavy,
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.citecircle.app.R.drawable.ic_citecircle_logo),
                            contentDescription = "Cite Circle Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Name
            Text(
                text = "CiteCircle",
                fontFamily = FrauncesFamily,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSize = 36.sp,
                color = CcColors.InkNavy
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Where ideas connect",
                style = MaterialTheme.typography.bodyMedium,
                color = CcColors.MarginGray
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Rich Prepping Card Container
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "AI Prepping",
                            tint = CcColors.CircleBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Prepping Workspace ${(progress.value * 100).toInt()}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CcColors.InkNavy
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { progress.value },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = CcColors.HighlighterYellow,
                        trackColor = CcColors.CircleBlue.copy(alpha = 0.15f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AnimatedContent(
                        targetState = preppingStageText,
                        transitionSpec = { fadeIn(tween(250)) with fadeOut(tween(200)) },
                        label = "stage_text"
                    ) { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = CcColors.MarginGray
                        )
                    }
                }
            }
        }
    }
}
