package com.citecircle.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citecircle.app.core.data.FakeDataSource
import com.citecircle.app.core.data.UserRepository
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage

// ──────────────────────────────────────────────────────────────────────────────
// EditProfileViewModel
// ──────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getCurrentUser().collect {
                _currentUser.value = it
            }
        }
    }

    fun saveProfile(user: User, onComplete: () -> Unit) {
        viewModelScope.launch {
            userRepository.updateCurrentUser(user)
            onComplete()
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// EditProfileScreen Composable
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val currentUserState by viewModel.currentUser.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.ccColors.paperCream)
                .padding(paddingValues)
        ) {
            val user = currentUserState
            if (user == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                var name by remember { mutableStateOf(user.name) }
                var institution by remember { mutableStateOf(user.institution) }
                var bio by remember { mutableStateOf(user.bio) }
                var orcid by remember { mutableStateOf(user.orcidId) }
                var fieldOfStudy by remember { mutableStateOf(user.fieldOfStudy) }
                var avatarUrl by remember { mutableStateOf(user.avatarUrl) }
                var coverUrl by remember { mutableStateOf(user.coverUrl) }

                val pickMediaLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) {
                        avatarUrl = uri.toString()
                    }
                }

                val pickCoverLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) {
                        coverUrl = uri.toString()
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(24.dp)
                ) {
                    // Cover Photo Preview Box
                    Text(text = "Profile Cover Banner", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                pickCoverLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        if (coverUrl.isNotBlank()) {
                            AsyncImage(
                                model = coverUrl,
                                contentDescription = "Cover preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(CcColors.InkNavy, CcColors.CircleBlue)
                                        )
                                    )
                            )
                        }
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = CircleShape,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .clip(CircleShape)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change Cover Banner", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Avatar Photo Preview
                    Text(text = "Profile Avatar", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clickable {
                                pickMediaLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        CcAvatar(
                            user = user.copy(avatarUrl = avatarUrl),
                            size = 84.dp,
                            showRing = true
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.BottomEnd)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "Change profile photo",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    CcTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Full Name",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CcTextField(
                        value = institution,
                        onValueChange = { institution = it },
                        label = "Institution",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CcTextField(
                        value = fieldOfStudy,
                        onValueChange = { fieldOfStudy = it },
                        label = "Field of Study",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CcTextField(
                        value = orcid,
                        onValueChange = { orcid = it },
                        label = "ORCID iD",
                        modifier = Modifier.fillMaxWidth(),
                        helperText = "Format: 0000-0000-0000-0000"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CcTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = "Biography",
                        singleLine = false,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    CcPrimaryButton(
                        text = "Save Changes",
                        onClick = {
                            val updated = user.copy(
                                name = name,
                                institution = institution,
                                bio = bio,
                                orcidId = orcid,
                                fieldOfStudy = fieldOfStudy,
                                avatarUrl = avatarUrl,
                                coverUrl = coverUrl
                            )
                            viewModel.saveProfile(updated, onBack)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
