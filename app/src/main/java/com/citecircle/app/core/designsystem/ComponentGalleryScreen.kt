package com.citecircle.app.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citecircle.app.core.data.FakeDataSource
import com.citecircle.app.core.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentGalleryScreen(
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var chipSelected by remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf("") }
    var isEndorsed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Component Gallery", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.ccColors.paperCream)
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            // Colors swatches
            Section("Color Tokens") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorSwatch(name = "Ink Navy", color = CcColors.InkNavy)
                    ColorSwatch(name = "Circle Blue (Primary)", color = CcColors.CircleBlue)
                    ColorSwatch(name = "Highlighter Yellow (Accent)", color = CcColors.HighlighterYellow)
                    ColorSwatch(name = "Seafoam Teal (Success)", color = CcColors.SeafoamTeal)
                    ColorSwatch(name = "Coral Pop (Alert)", color = CcColors.CoralPop)
                    ColorSwatch(name = "Paper Cream (Background)", color = CcColors.PaperCream)
                    ColorSwatch(name = "Margin Gray (Secondary Text)", color = CcColors.MarginGray)
                }
            }

            // Typography scale
            Section("Typography") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Fraunces Display Large", style = MaterialTheme.typography.displayLarge)
                    Text(text = "Fraunces Headline Large", style = MaterialTheme.typography.headlineLarge)
                    Text(text = "Plus Jakarta Sans Title Medium", style = MaterialTheme.typography.titleMedium)
                    Text(text = "Plus Jakarta Sans Body Medium", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Plus Jakarta Sans Label Large", style = MaterialTheme.typography.labelLarge)
                    Text(text = "JetBrains Mono DOI Style", fontFamily = JetBrainsMonoFamily, fontSize = 12.sp)
                }
            }

            // Buttons
            Section("Buttons") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CcPrimaryButton(text = "Primary Action Button", onClick = {}, modifier = Modifier.fillMaxWidth())
                    CcSecondaryButton(text = "Secondary Outline Button", onClick = {}, modifier = Modifier.fillMaxWidth())
                }
            }

            // Chips / Badges / Avatars
            Section("Chips, Badges & Avatars") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CcChip(label = "Select Chip", selected = chipSelected, onClick = { chipSelected = !chipSelected })
                        CcBadge(count = 7)
                        CcBadge(count = 104)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CcAvatar(user = com.citecircle.app.core.model.User(id = "g1", name = "Gallery User", role = UserRole.EDUCATOR), size = 44.dp)
                        Text("Professor ring", style = MaterialTheme.typography.labelSmall)

                        CcAvatar(user = com.citecircle.app.core.model.User(id = "g2", name = "Gallery User", isVerified = true), size = 44.dp)
                        Text("Verified researcher", style = MaterialTheme.typography.labelSmall)

                        CcAvatar(user = com.citecircle.app.core.model.User(id = "g3", name = "Gallery User", role = UserRole.STUDENT), size = 44.dp)
                        Text("Student ring", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Cards & Input fields
            Section("Card & Input Field") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CcCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "This is a CiteCircle index card surface, styled with 16dp rounded corner radii and subtle soft drop elevation shadows.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    CcTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        label = "Sample text field input",
                        placeholder = "Type here..."
                    )
                }
            }

            // Animations and signature motifs
            Section("Interactive Elements") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Signature Highlighter Motif:")
                        HighlighterSweep {
                            Text(
                                text = "Highlighted text",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Confetti Endorsement:")
                        EndorseButton(
                            isEndorsed = isEndorsed,
                            endorseCount = if (isEndorsed) 12 else 11,
                            onEndorse = { isEndorsed = !isEndorsed }
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("AI Pre-Review Writing Animator:")
                        Spacer(modifier = Modifier.height(8.dp))
                        AiPencilProgress()
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun Section(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = MaterialTheme.ccColors.divider)
    }
}

@Composable
fun ColorSwatch(name: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
