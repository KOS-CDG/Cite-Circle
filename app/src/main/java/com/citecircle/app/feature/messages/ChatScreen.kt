package com.citecircle.app.feature.messages

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citecircle.app.core.data.MessageRepository
import com.citecircle.app.core.data.UserRepository
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.core.model.Message
import com.citecircle.app.core.model.User
import com.citecircle.app.feature.feed.PaperMiniCard
import com.citecircle.app.feature.feed.formatRelativeTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────────────────────
// ChatViewModel
// ──────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _conversationId = MutableStateFlow("")
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    private val _recipient = MutableStateFlow<User?>(null)

    val state: StateFlow<ChatScreenState> = combine(_messages, _recipient) { messages, recipient ->
        if (recipient == null) ChatScreenState.Loading
        else ChatScreenState.Success(messages, recipient)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatScreenState.Loading)

    fun loadConversation(convId: String) {
        _conversationId.value = convId
        viewModelScope.launch {
            messageRepository.getMessagesForConversation(convId).collect {
                _messages.value = it
            }
        }
        viewModelScope.launch {
            // Find participant who isn't current user
            messageRepository.getConversations().collect { convs ->
                val conv = convs.find { it.id == convId }
                val recUser = conv?.participants?.firstOrNull { it.id != "u0" }
                _recipient.value = recUser
            }
        }
    }

    fun sendMessage(content: String) {
        viewModelScope.launch {
            messageRepository.sendMessage(_conversationId.value, content)
            loadConversation(_conversationId.value)
        }
    }
}

sealed interface ChatScreenState {
    object Loading : ChatScreenState
    data class Success(val messages: List<Message>, val recipient: User) : ChatScreenState
}

// ──────────────────────────────────────────────────────────────────────────────
// ChatScreen Composable
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
    }

    val state by viewModel.state.collectAsState()
    var messageText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val s = state
                    if (s is ChatScreenState.Success) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CcAvatar(user = s.recipient, size = 36.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = s.recipient.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(text = "Online", style = MaterialTheme.typography.labelSmall, color = CcColors.SeafoamTeal)
                            }
                        }
                    } else {
                        Text("Chat")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Type message...", color = MaterialTheme.ccColors.marginGray) },
                        maxLines = 4,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.ccColors.divider
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send message", tint = Color.White)
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.ccColors.paperCream)
                .padding(paddingValues)
        ) {
            when (val chatState = state) {
                is ChatScreenState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ChatScreenState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(chatState.messages, key = { it.id }) { msg ->
                            MessageBubble(message = msg, isMine = msg.senderId == "u0")
                        }

                        // Show typing indicator only when the AI Copilot is thinking
                        val isAiThinking = chatState.recipient.id == "ai_copilot" &&
                                chatState.messages.isNotEmpty() &&
                                chatState.messages.last().senderId == "u0"
                        if (isAiThinking) {
                            item {
                                TypingIndicatorBubble()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isMine: Boolean
) {
    val bubbleColor = if (isMine) CcColors.CircleBlue else MaterialTheme.colorScheme.surface
    val textColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
    val alignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleShape = if (isMine) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 0.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 16.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(bubbleShape)
                    .background(bubbleColor)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text = message.content,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Renders attached paper sharing mini card stubs representation
                    if (message.attachedPaper != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        PaperMiniCard(paper = message.attachedPaper, onUserClick = {})
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = formatRelativeTime(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.ccColors.marginGray
            )
        }
    }
}

@Composable
fun TypingIndicatorBubble() {
    val transition = rememberInfiniteTransition(label = "typing")
    val dot1Offset by transition.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Offset by transition.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Offset by transition.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Box(
        modifier = Modifier
            .width(60.dp)
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Canvas(modifier = Modifier.size(6.dp)) {
                drawCircle(color = CcColors.MarginGray, radius = size.width / 2, center = Offset(size.width / 2, size.height / 2 + dot1Offset))
            }
            Canvas(modifier = Modifier.size(6.dp)) {
                drawCircle(color = CcColors.MarginGray, radius = size.width / 2, center = Offset(size.width / 2, size.height / 2 + dot2Offset))
            }
            Canvas(modifier = Modifier.size(6.dp)) {
                drawCircle(color = CcColors.MarginGray, radius = size.width / 2, center = Offset(size.width / 2, size.height / 2 + dot3Offset))
            }
        }
    }
}
