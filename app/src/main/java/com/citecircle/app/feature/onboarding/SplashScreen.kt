package com.citecircle.app.feature.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citecircle.app.core.designsystem.CcColors
import com.citecircle.app.core.designsystem.FrauncesFamily
import kotlinx.coroutines.delay

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.citecircle.app.core.data.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenManager: TokenManager
) : ViewModel() {
    suspend fun checkAuthState(): Boolean = tokenManager.isLoggedIn()
}

@Composable
fun SplashScreen(
    onSplashComplete: (isLoggedIn: Boolean) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val transition = rememberInfiniteTransition(label = "splash")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val drawProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Draw the circle over 1.2s
        drawProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(1200, easing = FastOutSlowInEasing)
        )
        val isLoggedIn = viewModel.checkAuthState()
        delay(400) // Keep visible
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
            verticalArrangement = Arrangement.Center
        ) {
            // Animated logo: A circle drawing itself around a quotation mark
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 5.dp.toPx()
                    val radius = size.width / 2 - strokeWidth
                    val center = Offset(size.width / 2, size.height / 2)

                    // Draw the circular frame path
                    drawArc(
                        color = CcColors.CircleBlue,
                        startAngle = -90f,
                        sweepAngle = 360f * drawProgress.value,
                        useCenter = false,
                        style = Stroke(width = strokeWidth),
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                    )
                }

                // Quotation mark inside
                Text(
                    text = "“",
                    fontSize = 68.sp,
                    fontFamily = FrauncesFamily,
                    color = CcColors.InkNavy,
                    modifier = Modifier.offset(y = (-4).dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Name
            Text(
                text = "CiteCircle",
                fontFamily = FrauncesFamily,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSize = 32.sp,
                color = CcColors.InkNavy
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Where ideas connect",
                style = MaterialTheme.typography.bodyMedium,
                color = CcColors.MarginGray
            )
        }
    }
}
