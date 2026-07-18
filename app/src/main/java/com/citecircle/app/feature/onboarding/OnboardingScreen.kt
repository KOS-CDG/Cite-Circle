package com.citecircle.app.feature.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citecircle.app.core.designsystem.CcColors
import com.citecircle.app.core.designsystem.CcPrimaryButton
import com.citecircle.app.core.designsystem.FrauncesFamily
import com.citecircle.app.core.designsystem.ccColors

data class OnboardingPage(
    val title: String,
    val description: String,
    val illustrationEmoji: String
)

val onboardingPages = listOf(
    OnboardingPage(
        title = "Connect with researchers worldwide",
        description = "Find colleagues, track publications, and grow your academic network without institutional silos.",
        illustrationEmoji = "🌐"
    ),
    OnboardingPage(
        title = "Discuss ideas in Circles",
        description = "Join field-specific communities. Ask questions, get feedback on drafts, and share resources.",
        illustrationEmoji = "💬"
    ),
    OnboardingPage(
        title = "Publish with an AI pre-reviewer",
        description = "Upload drafts to get instant structural feedback, citation scans, and quality scores before peer review.",
        illustrationEmoji = "🤖"
    )
)

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit
) {
    var currentPage by remember { mutableStateOf(0) }
    val page = onboardingPages[currentPage]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.ccColors.paperCream)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Skip Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (currentPage < onboardingPages.size - 1) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.ccColors.marginGray,
                    modifier = Modifier
                        .clickable(onClick = onSkip)
                        .padding(8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(36.dp))
            }
        }

        Spacer(modifier = Modifier.weight(0.8f))

        // Large Illustration / Emoji representation
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = page.illustrationEmoji,
                fontSize = 80.sp
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Text Content
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineLarge,
            fontFamily = FrauncesFamily,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.ccColors.marginGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        // Page Dots Indicator
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            onboardingPages.forEachIndexed { index, _ ->
                val width by animateDpAsState(
                    targetValue = if (index == currentPage) 24.dp else 8.dp,
                    label = "dot_width"
                )
                val color = if (index == currentPage) {
                    MaterialTheme.ccColors.highlighterYellow
                } else {
                    MaterialTheme.ccColors.marginGray.copy(alpha = 0.4f)
                }

                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(height = 8.dp, width = width)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action Buttons
        if (currentPage < onboardingPages.size - 1) {
            CcPrimaryButton(
                text = "Next",
                onClick = { currentPage++ },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            CcPrimaryButton(
                text = "Get Started",
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
