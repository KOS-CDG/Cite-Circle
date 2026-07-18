package com.citecircle.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citecircle.app.core.data.AppTheme
import com.citecircle.app.core.data.ThemeRepository
import com.citecircle.app.core.designsystem.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────────────────────
// SettingsViewModel
// ──────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
) : ViewModel() {

    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            themeRepository.setTheme(theme)
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// SettingsScreen Composable
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()

    var endorsementsEnabled by remember { mutableStateOf(true) }
    var commentsEnabled by remember { mutableStateOf(true) }
    var connectionsEnabled by remember { mutableStateOf(true) }
    var citationsEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
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
            // Section: Account
            SectionTitle(title = "Account Settings")
            Spacer(modifier = Modifier.height(12.dp))

            CcCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Navigate to Edit Profile
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "Edit Profile Information", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Appearance (Light / Dark mode toggle switches)
            SectionTitle(title = "Appearance")
            Spacer(modifier = Modifier.height(12.dp))

            CcCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Theme Selection", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CcChip(
                            label = "Light Mode",
                            selected = !isDarkTheme,
                            onClick = {
                                onThemeChange(false)
                                viewModel.updateTheme(AppTheme.LIGHT)
                            },
                            modifier = Modifier.weight(1f)
                        )

                        CcChip(
                            label = "Dark Mode",
                            selected = isDarkTheme,
                            onClick = {
                                onThemeChange(true)
                                viewModel.updateTheme(AppTheme.DARK)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Notifications Switches
            SectionTitle(title = "Notification Preferences")
            Spacer(modifier = Modifier.height(12.dp))

            CcCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    NotificationSwitchRow(label = "Endorsements alerts", checked = endorsementsEnabled, onCheckedChange = { endorsementsEnabled = it })
                    Divider(color = MaterialTheme.ccColors.divider, modifier = Modifier.padding(vertical = 8.dp))
                    NotificationSwitchRow(label = "Comments notifications", checked = commentsEnabled, onCheckedChange = { commentsEnabled = it })
                    Divider(color = MaterialTheme.ccColors.divider, modifier = Modifier.padding(vertical = 8.dp))
                    NotificationSwitchRow(label = "Connection requests", checked = connectionsEnabled, onCheckedChange = { connectionsEnabled = it })
                    Divider(color = MaterialTheme.ccColors.divider, modifier = Modifier.padding(vertical = 8.dp))
                    NotificationSwitchRow(label = "Citation alerts", checked = citationsEnabled, onCheckedChange = { citationsEnabled = it })
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Log Out Button in CoralPop
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = CcColors.CoralPop),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(text = "Log Out", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.ccColors.marginGray
    )
}

@Composable
fun NotificationSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
