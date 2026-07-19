package com.citecircle.app.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citecircle.app.core.data.CircleRepository
import com.citecircle.app.core.data.FakeDataSource
import com.citecircle.app.core.data.UserRepository
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.Circle
import com.citecircle.app.core.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────────────────────
// ProfileSetupViewModel
// ──────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val circleRepository: CircleRepository
) : ViewModel() {
    val suggestedPeople = userRepository.getSuggestedConnections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suggestedCircles = circleRepository.getAllCircles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _followedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val followedUserIds = _followedUserIds.asStateFlow()

    private val _joinedCircleIds = MutableStateFlow<Set<String>>(emptySet())
    val joinedCircleIds = _joinedCircleIds.asStateFlow()

    fun toggleFollowUser(userId: String) {
        viewModelScope.launch {
            userRepository.followUser(userId)
            val current = _followedUserIds.value.toMutableSet()
            if (current.contains(userId)) current.remove(userId) else current.add(userId)
            _followedUserIds.value = current
        }
    }

    fun toggleJoinCircle(circleId: String) {
        viewModelScope.launch {
            val isJoined = _joinedCircleIds.value.contains(circleId)
            if (isJoined) {
                circleRepository.leaveCircle(circleId)
                _joinedCircleIds.value = _joinedCircleIds.value - circleId
            } else {
                circleRepository.joinCircle(circleId)
                _joinedCircleIds.value = _joinedCircleIds.value + circleId
            }
        }
    }

    fun saveProfile(name: String, institution: String, avatarUrl: String, interests: List<String>, onComplete: () -> Unit) {
        viewModelScope.launch {
            val user = FakeDataSource.currentUser.copy(
                name = name,
                institution = institution,
                avatarUrl = avatarUrl,
                interests = interests
            )
            userRepository.updateCurrentUser(user)
            onComplete()
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// ProfileSetupScreen Composable
// ──────────────────────────────────────────────────────────────────────────────

val suggestedInterests = listOf(
    "HCI", "Machine Learning", "NLP", "Computer Science",
    "Genomics", "Epidemiology", "Bioinformatics", "Neuroscience",
    "Climate Change", "Renewable Energy", "Materials Science", "Physics",
    "Cognitive Psychology", "Economics", "Digital Humanities", "History"
)

@Composable
fun ProfileSetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: ProfileSetupViewModel = hiltViewModel()
) {
    var step by remember { mutableStateOf(1) }

    // Step 1 states
    var name by remember { mutableStateOf(FakeDataSource.currentUser.name) }
    var institution by remember { mutableStateOf(FakeDataSource.currentUser.institution) }
    var avatarUrl by remember { mutableStateOf(FakeDataSource.currentUser.avatarUrl) }

    // Step 2 states
    var selectedInterests by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Step 3 states
    val suggestedPeople by viewModel.suggestedPeople.collectAsState()
    val suggestedCircles by viewModel.suggestedCircles.collectAsState()
    val followedUserIds by viewModel.followedUserIds.collectAsState()
    val joinedCircleIds by viewModel.joinedCircleIds.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.ccColors.paperCream)
    ) {
        // Top progress bar in HighlighterYellow
        LinearProgressIndicator(
            progress = { step.toFloat() / 3f },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = CcColors.HighlighterYellow,
            trackColor = MaterialTheme.ccColors.divider
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Setup Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Step $step of 3",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.ccColors.marginGray
            )
        }

        Divider(color = MaterialTheme.ccColors.divider)

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp)
        ) {
            AnimatedContent(targetState = step, label = "step_content") { currentStep ->
                when (currentStep) {
                    1 -> Step1Details(
                        name = name,
                        onNameChange = { name = it },
                        institution = institution,
                        onInstitutionChange = { institution = it },
                        avatarUrl = avatarUrl,
                        onAvatarChange = { avatarUrl = it }
                    )
                    2 -> Step2Interests(
                        selectedInterests = selectedInterests,
                        onToggleInterest = { interest ->
                            selectedInterests = if (selectedInterests.contains(interest)) {
                                selectedInterests - interest
                            } else {
                                selectedInterests + interest
                            }
                        }
                    )
                    3 -> Step3Connections(
                        suggestedPeople = suggestedPeople,
                        followedUserIds = followedUserIds,
                        onFollowToggle = { viewModel.toggleFollowUser(it.id) },
                        suggestedCircles = suggestedCircles,
                        joinedCircleIds = joinedCircleIds,
                        onJoinToggle = { viewModel.toggleJoinCircle(it.id) }
                    )
                }
            }
        }

        Divider(color = MaterialTheme.ccColors.divider)

        // Navigation bottom bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (step > 1) {
                CcSecondaryButton(
                    text = "Back",
                    onClick = { step-- },
                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                )
            }
            CcPrimaryButton(
                text = if (step == 3) "Finish" else "Continue",
                onClick = {
                    if (step < 3) {
                        step++
                    } else {
                        viewModel.saveProfile(name, institution, avatarUrl, selectedInterests.toList(), onSetupComplete)
                    }
                },
                enabled = when (step) {
                    1 -> name.isNotBlank() && institution.isNotBlank()
                    2 -> selectedInterests.size >= 3
                    else -> true
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun Step1Details(
    name: String,
    onNameChange: (String) -> Unit,
    institution: String,
    onInstitutionChange: (String) -> Unit,
    avatarUrl: String,
    onAvatarChange: (String) -> Unit
) {
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onAvatarChange(uri.toString())
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large Avatar Photo with click upload
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable {
                    pickMediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl.isNotEmpty()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Profile picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = "Profile photo placeholder",
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.ccColors.marginGray
                )
            }

            // Photo Camera Icon indicator overlay
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
                    contentDescription = "Upload photo",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        CcTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Full Name",
            placeholder = "e.g. Maya Okafor",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        CcTextField(
            value = institution,
            onValueChange = onInstitutionChange,
            label = "Institution / Affiliation",
            placeholder = "e.g. MIT Media Lab",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun Step2Interests(
    selectedInterests: Set<String>,
    onToggleInterest: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
    ) {
        Text(
            text = "Select your research fields:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Select at least 3 interests to personalize your feed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.ccColors.marginGray
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(suggestedInterests) { interest ->
                val isSelected = selectedInterests.contains(interest)
                CcChip(
                    label = interest,
                    selected = isSelected,
                    onClick = { onToggleInterest(interest) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun Step3Connections(
    suggestedPeople: List<User>,
    followedUserIds: Set<String>,
    onFollowToggle: (User) -> Unit,
    suggestedCircles: List<Circle>,
    joinedCircleIds: Set<String>,
    onJoinToggle: (Circle) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Circles Section
        item {
            Text(
                text = "Join research circles:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(suggestedCircles) { circle ->
                    val isJoined = joinedCircleIds.contains(circle.id)
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.ccColors.divider, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(text = circle.iconEmoji, fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = circle.name,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${circle.memberCount} members",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.ccColors.marginGray
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            CcPrimaryButton(
                                text = if (isJoined) "Joined" else "Join",
                                onClick = { onJoinToggle(circle) },
                                icon = if (isJoined) Icons.Filled.Check else Icons.Filled.Add,
                                modifier = Modifier.fillMaxWidth().height(36.dp)
                            )
                        }
                    }
                }
            }
        }

        // People Section
        item {
            Text(
                text = "Follow rising researchers:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(suggestedPeople) { person ->
            val isFollowing = followedUserIds.contains(person.id)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.ccColors.divider, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CcAvatar(user = person, size = 44.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = person.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = person.institution,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.ccColors.marginGray
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onFollowToggle(person) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) MaterialTheme.ccColors.marginGray else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = if (isFollowing) "Following" else "Follow", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
