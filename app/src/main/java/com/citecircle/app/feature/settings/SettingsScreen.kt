package com.citecircle.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citecircle.app.core.data.AppTheme
import com.citecircle.app.core.data.AuthRepository
import com.citecircle.app.core.data.ThemeRepository
import com.citecircle.app.core.data.UserRepository
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.SavedAccount
import com.citecircle.app.core.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────────────────────
// Settings ViewModel & State
// ──────────────────────────────────────────────────────────────────────────────

sealed interface ActiveSettingsDialog {
    object None : ActiveSettingsDialog
    object ChangePassword : ActiveSettingsDialog
    object ChangeEmail : ActiveSettingsDialog
    object AddAccount : ActiveSettingsDialog
    object ConfirmClearCache : ActiveSettingsDialog
    object ConfirmLogoutAll : ActiveSettingsDialog
    data class ExportDataSummary(val jsonContent: String) : ActiveSettingsDialog
}

data class SettingsUiState(
    val currentUser: User? = null,
    val savedAccounts: List<SavedAccount> = emptyList(),
    val currentTheme: AppTheme = AppTheme.SYSTEM,
    val endorsementsEnabled: Boolean = true,
    val commentsEnabled: Boolean = true,
    val connectionsEnabled: Boolean = true,
    val citationsEnabled: Boolean = true,
    val aiAlertsEnabled: Boolean = true,
    val emailDigest: String = "Daily",
    val isProcessing: Boolean = false,
    val activeDialog: ActiveSettingsDialog = ActiveSettingsDialog.None,
    val messageSnackbar: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val themeRepository: ThemeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = combine(
        userRepository.getCurrentUser(),
        authRepository.getSavedAccounts(),
        themeRepository.getTheme(),
        _uiState
    ) { user, savedAccounts, theme, state ->
        state.copy(
            currentUser = user,
            savedAccounts = savedAccounts,
            currentTheme = theme
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            themeRepository.setTheme(theme)
            _uiState.value = _uiState.value.copy(
                messageSnackbar = "Theme set to ${theme.name.lowercase().replaceFirstChar { it.uppercase() }}"
            )
        }
    }

    fun openDialog(dialog: ActiveSettingsDialog) {
        _uiState.value = _uiState.value.copy(activeDialog = dialog)
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(activeDialog = ActiveSettingsDialog.None)
    }

    fun dismissSnackbar() {
        _uiState.value = _uiState.value.copy(messageSnackbar = null)
    }

    fun changePassword(oldPass: String, newPass: String, confirmPass: String) {
        if (newPass != confirmPass) {
            _uiState.value = _uiState.value.copy(messageSnackbar = "New passwords do not match")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            val result = authRepository.changePassword(oldPass, newPass)
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                activeDialog = ActiveSettingsDialog.None,
                messageSnackbar = if (result.isSuccess) "Password successfully updated!" else (result.exceptionOrNull()?.message ?: "Failed to update password")
            )
        }
    }

    fun changeEmail(newEmail: String, currentPass: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            val result = authRepository.changeEmail(newEmail, currentPass)
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                activeDialog = ActiveSettingsDialog.None,
                messageSnackbar = if (result.isSuccess) "Email address updated to $newEmail" else (result.exceptionOrNull()?.message ?: "Failed to update email")
            )
        }
    }

    fun switchAccount(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            val success = authRepository.switchAccount(userId)
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                messageSnackbar = if (success) "Switched active account session" else "Failed to switch account"
            )
        }
    }

    fun addAccount(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            val result = authRepository.addAccount(email, pass)
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                activeDialog = ActiveSettingsDialog.None,
                messageSnackbar = if (result.isSuccess) "Account $email authenticated & added!" else (result.exceptionOrNull()?.message ?: "Failed to add account")
            )
        }
    }

    fun removeAccount(userId: String) {
        viewModelScope.launch {
            authRepository.removeAccount(userId)
            _uiState.value = _uiState.value.copy(messageSnackbar = "Account session removed")
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            authRepository.clearCache()
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                activeDialog = ActiveSettingsDialog.None,
                messageSnackbar = "App cache & temporary files cleared (~14.2 MB freed)"
            )
        }
    }

    fun exportUserData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            val result = authRepository.exportUserData()
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                activeDialog = if (result.isSuccess) ActiveSettingsDialog.ExportDataSummary(result.getOrDefault("")) else ActiveSettingsDialog.None,
                messageSnackbar = if (result.isFailure) "Failed to export user data" else null
            )
        }
    }

    fun logoutCurrent(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = _uiState.value.copy(messageSnackbar = "Session logged out")
            onLogoutComplete()
        }
    }

    fun logoutAllAccounts(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logoutAll()
            _uiState.value = _uiState.value.copy(activeDialog = ActiveSettingsDialog.None)
            onLogoutComplete()
        }
    }

    fun setEndorsements(enabled: Boolean) { _uiState.value = _uiState.value.copy(endorsementsEnabled = enabled) }
    fun setComments(enabled: Boolean) { _uiState.value = _uiState.value.copy(commentsEnabled = enabled) }
    fun setConnections(enabled: Boolean) { _uiState.value = _uiState.value.copy(connectionsEnabled = enabled) }
    fun setCitations(enabled: Boolean) { _uiState.value = _uiState.value.copy(citationsEnabled = enabled) }
    fun setAiAlerts(enabled: Boolean) { _uiState.value = _uiState.value.copy(aiAlertsEnabled = enabled) }
    fun setEmailDigest(digest: String) { _uiState.value = _uiState.value.copy(emailDigest = digest) }
}

// ──────────────────────────────────────────────────────────────────────────────
// Settings Screen Composable
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfileClick: () -> Unit = {},
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(state.messageSnackbar) {
        state.messageSnackbar?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.ccColors.paperCream)
                    .verticalScroll(scrollState)
                    .padding(20.dp)
            ) {
                // ──────────────────────────────────────────────────────────────
                // 1. Account Credentials & Security
                // ──────────────────────────────────────────────────────────────
                SectionTitle(title = "Account & Credentials")
                Spacer(modifier = Modifier.height(10.dp))

                CcCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        state.currentUser?.let { user ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                CcAvatar(user = user, size = 48.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = user.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    Text(text = "${user.role.name} • ${user.institution.ifBlank { "CiteCircle Scholar" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ccColors.marginGray)
                                }
                                CcChip(label = "Edit Profile", selected = false, onClick = onEditProfileClick)
                            }
                            HorizontalDivider(color = MaterialTheme.ccColors.divider, modifier = Modifier.padding(bottom = 12.dp))
                        }

                        // Change Password Row
                        SettingsActionRow(
                            icon = Icons.Outlined.Lock,
                            title = "Change Password",
                            subtitle = "Update your account password with real-time strength validation",
                            onClick = { viewModel.openDialog(ActiveSettingsDialog.ChangePassword) }
                        )

                        HorizontalDivider(color = MaterialTheme.ccColors.divider, modifier = Modifier.padding(vertical = 8.dp))

                        // Change Email Row
                        SettingsActionRow(
                            icon = Icons.Outlined.Email,
                            title = "Change Email Address",
                            subtitle = "Update your primary email address for identity & notifications",
                            onClick = { viewModel.openDialog(ActiveSettingsDialog.ChangeEmail) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ──────────────────────────────────────────────────────────────
                // 2. Switch & Multi-Account Management
                // ──────────────────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle(title = "Account Switcher")
                    TextButton(onClick = { viewModel.openDialog(ActiveSettingsDialog.AddAccount) }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Account", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                CcCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (state.savedAccounts.isEmpty()) {
                            Text("No saved secondary sessions found.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ccColors.marginGray)
                        } else {
                            state.savedAccounts.forEachIndexed { index, account ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (!account.isActive) {
                                                viewModel.switchAccount(account.userId)
                                            }
                                        }
                                        .background(if (account.isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent)
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(CcColors.CircleBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = account.name.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = account.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = account.role,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                        Text(text = account.email, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ccColors.marginGray)
                                    }

                                    if (account.isActive) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Text(
                                                text = "Active",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    } else {
                                        IconButton(onClick = { viewModel.removeAccount(account.userId) }) {
                                            Icon(Icons.Outlined.Close, contentDescription = "Remove session", tint = MaterialTheme.ccColors.marginGray)
                                        }
                                    }
                                }

                                if (index < state.savedAccounts.size - 1) {
                                    HorizontalDivider(color = MaterialTheme.ccColors.divider, modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ──────────────────────────────────────────────────────────────
                // 3. Theme & Appearance
                // ──────────────────────────────────────────────────────────────
                SectionTitle(title = "Appearance & Theme")
                Spacer(modifier = Modifier.height(10.dp))

                CcCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Interface Color Theme", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CcChip(
                                label = "Light",
                                selected = state.currentTheme == AppTheme.LIGHT,
                                onClick = {
                                    onThemeChange(false)
                                    viewModel.updateTheme(AppTheme.LIGHT)
                                },
                                modifier = Modifier.weight(1f)
                            )

                            CcChip(
                                label = "Dark",
                                selected = state.currentTheme == AppTheme.DARK,
                                onClick = {
                                    onThemeChange(true)
                                    viewModel.updateTheme(AppTheme.DARK)
                                },
                                modifier = Modifier.weight(1f)
                            )

                            CcChip(
                                label = "System Default",
                                selected = state.currentTheme == AppTheme.SYSTEM,
                                onClick = {
                                    viewModel.updateTheme(AppTheme.SYSTEM)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ──────────────────────────────────────────────────────────────
                // 4. Notification Preferences & Email Digest
                // ──────────────────────────────────────────────────────────────
                SectionTitle(title = "Notification Preferences")
                Spacer(modifier = Modifier.height(10.dp))

                CcCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        NotificationSwitchRow(label = "Endorsements & Likes", checked = state.endorsementsEnabled, onCheckedChange = { viewModel.setEndorsements(it) })
                        HorizontalDivider(color = MaterialTheme.ccColors.divider, modifier = Modifier.padding(vertical = 8.dp))
                        NotificationSwitchRow(label = "Comments & Replies", checked = state.commentsEnabled, onCheckedChange = { viewModel.setComments(it) })
                        HorizontalDivider(color = MaterialTheme.ccColors.divider, modifier = Modifier.padding(vertical = 8.dp))
                        NotificationSwitchRow(label = "Connection Requests", checked = state.connectionsEnabled, onCheckedChange = { viewModel.setConnections(it) })
                        HorizontalDivider(color = MaterialTheme.ccColors.divider, modifier = Modifier.padding(vertical = 8.dp))
                        NotificationSwitchRow(label = "Paper Citations & Alerts", checked = state.citationsEnabled, onCheckedChange = { viewModel.setCitations(it) })
                        HorizontalDivider(color = MaterialTheme.ccColors.divider, modifier = Modifier.padding(vertical = 8.dp))
                        NotificationSwitchRow(label = "AI Copilot Notifications", checked = state.aiAlertsEnabled, onCheckedChange = { viewModel.setAiAlerts(it) })

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Email Digest Frequency", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Instant", "Daily", "Weekly", "Off").forEach { option ->
                                CcChip(
                                    label = option,
                                    selected = state.emailDigest == option,
                                    onClick = { viewModel.setEmailDigest(option) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ──────────────────────────────────────────────────────────────
                // 5. Storage & Privacy Data
                // ──────────────────────────────────────────────────────────────
                SectionTitle(title = "System Storage & Data")
                Spacer(modifier = Modifier.height(10.dp))

                CcCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsActionRow(
                            icon = Icons.Outlined.CleaningServices,
                            title = "Clear App Cache & Storage (~14.2 MB)",
                            subtitle = "Free up temporary PDF manuscript files, paper summaries, and search indices",
                            onClick = { viewModel.openDialog(ActiveSettingsDialog.ConfirmClearCache) }
                        )

                        HorizontalDivider(color = MaterialTheme.ccColors.divider, modifier = Modifier.padding(vertical = 8.dp))

                        SettingsActionRow(
                            icon = Icons.Outlined.Download,
                            title = "Export User Data Summary",
                            subtitle = "Generate and download a JSON summary of your profile, accounts & saved items",
                            onClick = { viewModel.exportUserData() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ──────────────────────────────────────────────────────────────
                // 6. Session Controls & Logout
                // ──────────────────────────────────────────────────────────────
                Button(
                    onClick = { viewModel.logoutCurrent(onLogout) },
                    colors = ButtonDefaults.buttonColors(containerColor = CcColors.CoralPop),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Log Out Current Account", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { viewModel.openDialog(ActiveSettingsDialog.ConfirmLogoutAll) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text(text = "Log Out of All Accounts", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            if (state.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Modal Dialogs
    // ──────────────────────────────────────────────────────────────────────────────

    when (val currentDialog = state.activeDialog) {
        ActiveSettingsDialog.ChangePassword -> {
            ChangePasswordDialog(
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { oldPass, newPass, confirmPass ->
                    viewModel.changePassword(oldPass, newPass, confirmPass)
                }
            )
        }
        ActiveSettingsDialog.ChangeEmail -> {
            ChangeEmailDialog(
                currentEmail = state.currentUser?.name ?: "",
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { newEmail, pass ->
                    viewModel.changeEmail(newEmail, pass)
                }
            )
        }
        ActiveSettingsDialog.AddAccount -> {
            AddAccountDialog(
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { email, pass ->
                    viewModel.addAccount(email, pass)
                }
            )
        }
        ActiveSettingsDialog.ConfirmClearCache -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                title = { Text("Clear App Cache?") },
                text = { Text("This will wipe ~14.2 MB of local PDF manuscript cache, paper summaries, and search indices. Your account and saved items will remain intact.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearCache() }) {
                        Text("Clear Cache", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDialog() }) { Text("Cancel") }
                }
            )
        }
        ActiveSettingsDialog.ConfirmLogoutAll -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                title = { Text("Log Out All Accounts?") },
                text = { Text("This will sign out all saved sessions from this device, wipe DataStore tokens, and return to the onboarding screen.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.logoutAllAccounts(onLogout) }) {
                        Text("Log Out All", fontWeight = FontWeight.Bold, color = CcColors.CoralPop)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDialog() }) { Text("Cancel") }
                }
            )
        }
        is ActiveSettingsDialog.ExportDataSummary -> {
            ExportDataDialog(
                jsonContent = currentDialog.jsonContent,
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        ActiveSettingsDialog.None -> {}
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Helper Components & Dialog Composables
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.ccColors.marginGray)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.ccColors.marginGray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (oldPass: String, newPass: String, confirmPass: String) -> Unit
) {
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }

    val hasMinLength = newPass.length >= 6
    val hasUpperOrDigit = newPass.any { it.isUpperCase() } || newPass.any { it.isDigit() }
    val isMatching = newPass.isNotEmpty() && newPass == confirmPass
    val isValid = oldPass.isNotBlank() && hasMinLength && hasUpperOrDigit && isMatching

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = oldPass,
                    onValueChange = { oldPass = it },
                    label = { Text("Current Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    label = { Text("New Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPass,
                    onValueChange = { confirmPass = it },
                    label = { Text("Confirm New Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                // Validation indicators checklist
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                    ValidationCheckItem(label = "At least 6 characters", checked = hasMinLength)
                    ValidationCheckItem(label = "Contains uppercase letter or number", checked = hasUpperOrDigit)
                    ValidationCheckItem(label = "Passwords match", checked = isMatching)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(oldPass, newPass, confirmPass) },
                enabled = isValid
            ) {
                Text("Update Password")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ValidationCheckItem(label: String, checked: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (checked) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = null,
            tint = if (checked) Color(0xFF2E7D32) else MaterialTheme.ccColors.marginGray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (checked) Color(0xFF2E7D32) else MaterialTheme.ccColors.marginGray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeEmailDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (newEmail: String, currentPass: String) -> Unit
) {
    var newEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Email Address", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text("New Email Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Current Password (identity check)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newEmail, password) },
                enabled = newEmail.contains("@") && password.isNotBlank()
            ) {
                Text("Update Email")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (email: String, pass: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Secondary Account", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Sign in with another CiteCircle account to switch between active profiles quickly.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Account Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(email, password) },
                enabled = email.contains("@") && password.isNotBlank()
            ) {
                Text("Sign In & Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ExportDataDialog(
    jsonContent: String,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("User Data Summary (JSON)", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Below is your exported user session & data summary:", style = MaterialTheme.typography.bodySmall)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
                ) {
                    Text(
                        text = jsonContent,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                clipboardManager.setText(AnnotatedString(jsonContent))
                onDismiss()
            }) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy JSON")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
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
