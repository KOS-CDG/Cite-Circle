package com.citecircle.app.feature.messages

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────────────────────
// Markdown Text Parser
// ──────────────────────────────────────────────────────────────────────────────

fun parseMarkdownToAnnotatedString(text: String): AnnotatedString {
    var cleanText = text.trim()
    if (cleanText.startsWith("```markdown")) {
        cleanText = cleanText.removeSurrounding("```markdown", "```").trim()
    } else if (cleanText.startsWith("```text")) {
        cleanText = cleanText.removeSurrounding("```text", "```").trim()
    } else if (cleanText.startsWith("```") && cleanText.endsWith("```")) {
        cleanText = cleanText.removeSurrounding("```", "```").trim()
    }

    // Filter out scratchpad lines and clean headers/bullets
    val processedLines = cleanText.lines().mapNotNull { line ->
        var trimmed = line.trim()
        val lower = trimmed.lowercase()

        if (lower.startsWith("analyze the request") ||
            lower.startsWith("identify the core content") ||
            lower.startsWith("persona:") ||
            lower.startsWith("constraints:")) {
            return@mapNotNull null
        }

        if (trimmed.startsWith("#")) {
            trimmed = trimmed.replace(Regex("^#+\\s*"), "")
        }

        if (trimmed.startsWith("* ") || trimmed.startsWith("- ")) {
            "•  " + trimmed.substring(2)
        } else {
            trimmed
        }
    }.joinToString("\n")

    return buildAnnotatedString {
        var currentIndex = 0
        val length = processedLines.length
        val regex = Regex("""(\*\*|__)(.*?)\1|(\*|_)(.*?)\3|`([^`]+)`""")
        val matches = regex.findAll(processedLines)

        for (match in matches) {
            val range = match.range
            if (range.first > currentIndex) {
                append(processedLines.substring(currentIndex, range.first))
            }

            val boldContent = match.groupValues[2].takeIf { it.isNotEmpty() }
            val italicContent = match.groupValues[4].takeIf { it.isNotEmpty() }
            val codeContent = match.groupValues[5].takeIf { it.isNotEmpty() }

            when {
                boldContent != null -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(boldContent)
                    }
                }
                italicContent != null -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(italicContent)
                    }
                }
                codeContent != null -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(codeContent)
                    }
                }
                else -> {
                    append(match.value)
                }
            }
            currentIndex = range.last + 1
        }

        if (currentIndex < length) {
            append(processedLines.substring(currentIndex))
        }
    }
}

@Composable
fun FormattedChatMessageText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val annotatedString = remember(text) {
        parseMarkdownToAnnotatedString(text)
    }
    Text(
        text = annotatedString,
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        lineHeight = 22.sp,
        modifier = modifier
    )
}

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

    val isAiChat = (state as? ChatScreenState.Success)?.recipient?.id == "ai_copilot"

    val samplePromptChips = listOf(
        "💡 Review my abstract",
        "📊 Explain P-value vs Effect Size",
        "✍️ Tips for literature review",
        "📝 How to write research methodology"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val s = state
                    if (s is ChatScreenState.Success) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (s.recipient.id == "ai_copilot") {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.AutoAwesome,
                                        contentDescription = "AI Icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                CcAvatar(user = s.recipient, size = 36.dp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = s.recipient.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (s.recipient.id == "ai_copilot") "AI Assistant • Active" else "Online",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CcColors.SeafoamTeal
                                )
                            }
                        }
                    } else {
                        Text("Chat")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Quick Action Chips for AI Copilot Chat
                    if (isAiChat) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(samplePromptChips) { chip ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.clickable {
                                        viewModel.sendMessage(chip.substringAfter(" "))
                                    }
                                ) {
                                    Text(
                                        text = chip,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = {
                                Text(
                                    if (isAiChat) "Ask CiteCircle AI Copilot..." else "Type message...",
                                    color = MaterialTheme.ccColors.marginGray
                                )
                            },
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
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message", tint = Color.White)
                        }
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
                    val listState = rememberLazyListState()
                    val isAiThinking = chatState.recipient.id == "ai_copilot" &&
                            chatState.messages.isNotEmpty() &&
                            chatState.messages.last().senderId != "ai_copilot"

                    // Auto scroll to latest message or typing indicator
                    LaunchedEffect(chatState.messages.size, isAiThinking) {
                        val totalItems = chatState.messages.size + (if (isAiThinking) 1 else 0)
                        if (totalItems > 0) {
                            listState.animateScrollToItem(totalItems - 1)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(chatState.messages, key = { it.id }) { msg ->
                            val isMine = msg.senderId != "ai_copilot" && msg.senderId != chatState.recipient.id
                            MessageBubble(message = msg, isMine = isMine)
                        }

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
    val isAiSender = message.senderId == "ai_copilot"
    val bubbleColor = when {
        isMine -> CcColors.CircleBlue
        isAiSender -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surface
    }
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
                    .widthIn(max = 320.dp)
                    .clip(bubbleShape)
                    .background(bubbleColor)
                    .then(
                        if (isAiSender) Modifier.border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            bubbleShape
                        ) else Modifier
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column {
                    if (isAiSender) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = "AI Copilot",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CiteCircle AI",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    FormattedChatMessageText(
                        text = message.content,
                        color = textColor
                    )

                    if (message.attachedPaper != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        PaperMiniCard(paper = message.attachedPaper, onUserClick = {})
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            CcTimestamp(timestamp = message.timestamp)
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

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(60.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 12.dp))
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
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "AI Copilot is analyzing & prepping response...",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
