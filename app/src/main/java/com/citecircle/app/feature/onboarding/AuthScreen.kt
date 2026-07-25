package com.citecircle.app.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citecircle.app.core.data.AuthRepository
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────────────────────
// AuthViewModel
// ──────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isLoginMode = MutableStateFlow(true)
    val isLoginMode = _isLoginMode.asStateFlow()

    private val _selectedRole = MutableStateFlow(UserRole.STUDENT)
    val selectedRole = _selectedRole.asStateFlow()

    fun toggleMode() {
        _isLoginMode.value = !_isLoginMode.value
    }

    fun selectRole(role: UserRole) {
        _selectedRole.value = role
    }

    fun authenticate(email: String, passwordInput: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = if (_isLoginMode.value) {
                authRepository.login(email, passwordInput)
            } else {
                authRepository.signup(email, passwordInput, _selectedRole.value)
            }
            _isLoading.value = false
            onResult(success)
        }
    }

}

// ──────────────────────────────────────────────────────────────────────────────
// AuthScreen Composable
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onSkipSetup: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoginMode by viewModel.isLoginMode.collectAsState()
    val selectedRole by viewModel.selectedRole.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.ccColors.paperCream)
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Heading wordmark
        Text(
            text = "CiteCircle",
            fontFamily = FrauncesFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isLoginMode) "Welcome back, Scholar." else "Join the academic circle.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.ccColors.marginGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Large Tappable Role Cards (Sign Up only)
        if (!isLoginMode) {
            Text(
                text = "Choose your scholarly role:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            RoleSelectorCard(
                role = UserRole.STUDENT,
                title = "Student",
                description = "Undergrad / Graduate exploring and discussing research.",
                emoji = "🎓",
                isSelected = selectedRole == UserRole.STUDENT,
                onClick = { viewModel.selectRole(UserRole.STUDENT) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            RoleSelectorCard(
                role = UserRole.RESEARCHER,
                title = "Researcher",
                description = "PhD / Postdoc publishing and pre-reviewing manuscripts.",
                emoji = "🔬",
                isSelected = selectedRole == UserRole.RESEARCHER,
                onClick = { viewModel.selectRole(UserRole.RESEARCHER) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            RoleSelectorCard(
                role = UserRole.EDUCATOR,
                title = "Educator",
                description = "Professor / Lecturer organizing circles and teaching classes.",
                emoji = "📖",
                isSelected = selectedRole == UserRole.EDUCATOR,
                onClick = { viewModel.selectRole(UserRole.EDUCATOR) }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Email field
        CcTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = null
            },
            label = "Email Address",
            placeholder = "name@institution.edu",
            isError = emailError != null,
            errorMessage = emailError,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password field
        CcTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            placeholder = "••••••••",
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Authenticate Button
        CcPrimaryButton(
            text = if (isLoginMode) "Log In" else "Create Account",
            isLoading = isLoading,
            onClick = {
                if (email.isBlank() || !email.contains("@")) {
                    emailError = "Please enter a valid institution email"
                } else if (password.isBlank()) {
                    emailError = "Please enter your password"
                } else {
                    viewModel.authenticate(email, password) { success ->
                        if (success) {
                            if (isLoginMode) {
                                onSkipSetup() // bypass setup wizard for login
                            } else {
                                onAuthSuccess() // trigger wizard for new signup
                            }
                        } else {
                            emailError = "Invalid email or password. Please check your credentials."
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mode toggle
        Text(
            text = if (isLoginMode) "Don't have an account? Sign up" else "Already have an account? Log in",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable { viewModel.toggleMode() }
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Divider(color = MaterialTheme.ccColors.divider)

        Spacer(modifier = Modifier.height(24.dp))

        // Social Buttons
        CcSecondaryButton(
            text = "Continue with ORCID iD",
            onClick = { viewModel.authenticate("orcid@orcid.org", "password") { onAuthSuccess() } },
            icon = null,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        CcSecondaryButton(
            text = "Continue with Google",
            onClick = { viewModel.authenticate("google@gmail.com", "password") { onAuthSuccess() } },
            icon = null,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Divider(color = MaterialTheme.ccColors.divider)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Quick Demo Access",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CcSecondaryButton(
                text = "🛡️ Super Admin",
                onClick = {
                    email = "admin@citecircle.com"
                    password = "password"
                    viewModel.authenticate(email, password) {
                        onSkipSetup()
                    }
                },
                modifier = Modifier.weight(1f)
            )

            CcSecondaryButton(
                text = "🎓 Dummy Scholar",
                onClick = {
                    email = "dummy@citecircle.com"
                    password = "password"
                    viewModel.authenticate(email, password) {
                        onSkipSetup()
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun RoleSelectorCard(
    role: UserRole,
    title: String,
    description: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.ccColors.divider
    val borderStrokeWidth = if (isSelected) 2.dp else 1.dp
    val containerBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerBg)
            .border(borderStrokeWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = emoji, fontSize = 36.sp, modifier = Modifier.padding(end = 16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.ccColors.marginGray
                )
            }
        }
    }
}
