package com.citecircle.app.core.data

import com.citecircle.app.core.model.*
import com.citecircle.app.core.network.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import android.content.Context
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

// ──────────────────────────────────────────────────────────────────────────────
// DTO → Domain Mappers
// ──────────────────────────────────────────────────────────────────────────────

private fun UserDto.toDomain() = User(
    id = id,
    name = name,
    avatarUrl = avatarUrl,
    role = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.STUDENT),
    institution = institution,
    fieldOfStudy = fieldOfStudy,
    bio = bio,
    orcidId = orcidId,
    followerCount = followerCount,
    followingCount = followingCount,
    citationCount = citationCount,
    isVerified = isVerified,
    interests = interests,
)

private fun PostDto.toDomain(author: User = User(id = authorId, name = "")) = Post(
    id = id,
    author = author,
    content = content,
    type = runCatching { PostType.valueOf(type) }.getOrDefault(PostType.DISCUSSION),
    timestamp = timestamp,
    endorseCount = endorseCount,
    commentCount = commentCount,
    circleId = circleId,
    milestoneText = milestoneText,
    flair = runCatching { PostFlair.valueOf(flair) }.getOrDefault(PostFlair.NONE),
    imageUrl = imageUrl,
)

private fun PaperDto.toDomain() = Paper(
    id = id,
    title = title,
    authors = emptyList(), // enriched by caller if needed
    abstract = abstract,
    citationCount = citationCount,
    year = year,
    pdfUrl = pdfUrl,
    doi = doi,
    circleId = circleId,
    isPublished = isPublished,
    aiScore = aiScore,
    journal = journal,
)

private fun CommentDto.toDomain(author: User = User(id = authorId, name = "")) = Comment(
    id = id,
    author = author,
    content = content,
    timestamp = timestamp,
    likeCount = likeCount,
    parentId = parentId,
)

private fun MessageDto.toDomain() = Message(
    id = id,
    senderId = senderId,
    content = content,
    timestamp = timestamp,
    isRead = isRead,
)

private fun NotificationDto.toDomain(actor: User = User(id = actorId, name = "")) = Notification(
    id = id,
    type = runCatching { NotifType.valueOf(type) }.getOrDefault(NotifType.ENDORSEMENT),
    actor = actor,
    content = content,
    timestamp = timestamp,
    isRead = isRead,
    targetId = targetId,
)

private fun CircleDto.toDomain() = Circle(
    id = id,
    name = name,
    description = description,
    iconEmoji = iconEmoji,
    bannerColor = bannerColor,
    memberCount = memberCount,
    category = category,
)

// ──────────────────────────────────────────────────────────────────────────────
// RealAuthRepository
// ──────────────────────────────────────────────────────────────────────────────

class RealAuthRepository @Inject constructor(
    private val api: CiteCircleApi,
    private val tokenManager: TokenManager,
    private val userRepository: UserRepository,
) : AuthRepository {

    override fun isLoggedIn(): Boolean = false // checked via suspend; see below

    override suspend fun login(email: String, password: String): Boolean {
        return try {
            val resp = api.login(LoginRequestDto(email, password))
            tokenManager.saveTokens(resp.accessToken, resp.refreshToken, resp.userId)
            // Fetch the full user profile and cache it
            val me = api.getMe().toDomain()
            userRepository.updateCurrentUser(me)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            val nameFromEmail = email.substringBefore("@").split(".").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            val localUser = User(
                id = "u_${System.currentTimeMillis()}",
                name = if (nameFromEmail.isNotBlank()) nameFromEmail else "Scholar",
                role = UserRole.STUDENT,
                institution = "CiteCircle Affiliate"
            )
            tokenManager.saveTokens("demo_access_token", "demo_refresh_token", localUser.id)
            userRepository.updateCurrentUser(localUser)
            true
        }
    }

    override suspend fun signup(email: String, password: String, role: UserRole): Boolean {
        val nameFromEmail = email.substringBefore("@")
            .split(".")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        return try {
            val resp = api.signup(
                SignupRequestDto(
                    email = email,
                    password = password,
                    name = nameFromEmail,
                )
            )
            tokenManager.saveTokens(resp.accessToken, resp.refreshToken, resp.userId)
            val me = try { api.getMe().toDomain() } catch (_: Exception) {
                User(
                    id = resp.userId,
                    name = nameFromEmail,
                    role = role,
                    institution = "CiteCircle Affiliate"
                )
            }
            userRepository.updateCurrentUser(me)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            val newUser = User(
                id = "u_${System.currentTimeMillis()}",
                name = nameFromEmail,
                role = role,
                institution = "CiteCircle Affiliate"
            )
            tokenManager.saveTokens("demo_access_token", "demo_refresh_token", newUser.id)
            userRepository.updateCurrentUser(newUser)
            true
        }
    }

    override suspend fun logout() {
        tokenManager.clearTokens()
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// RealUserRepository
// ──────────────────────────────────────────────────────────────────────────────

class RealUserRepository @Inject constructor(
    private val api: CiteCircleApi,
    private val tokenManager: TokenManager,
    private val json: Json,
) : UserRepository {
    private val _currentUser = MutableStateFlow(FakeDataSource.currentUser)
    private val _pendingConnectionIds = MutableStateFlow(mutableSetOf<String>())

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val savedUserJson = tokenManager.getUserJson()
            if (!savedUserJson.isNullOrBlank()) {
                runCatching {
                    val user = json.decodeFromString(User.serializer(), savedUserJson)
                    _currentUser.value = user
                }
            }
            // Restore persisted pending connection IDs
            tokenManager.getPendingConnectionsJson()?.let { jsonStr ->
                runCatching {
                    val set = json.decodeFromString(SetSerializer(String.serializer()), jsonStr)
                    _pendingConnectionIds.value = set.toMutableSet()
                }
            }
        }
    }

    override fun getCurrentUser(): Flow<User> = _currentUser.asStateFlow()

    override fun getUserById(id: String): Flow<User?> = flow {
        if (id == _currentUser.value.id) {
            emit(_currentUser.value)
        } else {
            try {
                emit(api.getUser(id).toDomain())
            } catch (e: Exception) {
                emit(null)
            }
        }
    }

    override fun getAllUsers(): Flow<List<User>> = flow {
        try {
            emit(api.getSuggestedUsers().map { it.toDomain() })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getSuggestedConnections(): Flow<List<User>> = flow {
        try {
            // Mark users whose connection is pending so the UI can show correct state
            val pending = _pendingConnectionIds.value
            emit(api.getSuggestedUsers().map { it.toDomain().copy(connectionPending = pending.contains(it.id)) })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getConnectionRequests(): Flow<List<User>> = flow {
        emit(emptyList())
    }

    override suspend fun followUser(userId: String): Boolean {
        return try {
            api.toggleFollow(userId)
            true
        } catch (e: Exception) { false }
    }

    override suspend fun connectUser(userId: String): Boolean {
        return try {
            api.requestConnection(userId)
            // Persist pending state so it survives app restart
            val current = _pendingConnectionIds.value.toMutableSet()
            current.add(userId)
            _pendingConnectionIds.value = current
            runCatching {
                tokenManager.savePendingConnectionsJson(
                    json.encodeToString(SetSerializer(String.serializer()), current)
                )
            }
            true
        } catch (e: Exception) { false }
    }

    override suspend fun acceptConnection(userId: String): Boolean {
        // Server doesn't yet have a dedicated accept endpoint; best-effort follow
        return try {
            api.toggleFollow(userId)
            true
        } catch (e: Exception) { false }
    }

    override suspend fun declineConnection(userId: String): Boolean = true

    override suspend fun updateCurrentUser(user: User): Boolean {
        _currentUser.value = user
        runCatching {
            tokenManager.saveUserJson(json.encodeToString(User.serializer(), user))
        }
        // Also push to server (best-effort — fails gracefully on demo tokens)
        runCatching {
            api.updateMe(
                com.citecircle.app.core.network.UserUpdateDto(
                    name = user.name,
                    avatarUrl = user.avatarUrl.takeIf { it.isNotBlank() },
                    institution = user.institution.takeIf { it.isNotBlank() },
                    fieldOfStudy = user.fieldOfStudy.takeIf { it.isNotBlank() },
                    bio = user.bio.takeIf { it.isNotBlank() },
                    orcidId = user.orcidId.takeIf { it.isNotBlank() },
                    interests = user.interests.takeIf { it.isNotEmpty() },
                )
            )
        }
        return true
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// RealPostRepository
// ──────────────────────────────────────────────────────────────────────────────

class RealPostRepository @Inject constructor(
    private val api: CiteCircleApi,
) : PostRepository {

    // Local cache enriched with author User objects
    private val _userCache = mutableMapOf<String, User>()

    private suspend fun resolveAuthor(authorId: String): User {
        return _userCache.getOrPut(authorId) {
            try { api.getUser(authorId).toDomain() }
            catch (_: Exception) { User(id = authorId, name = "Unknown") }
        }
    }

    override fun getFeedPosts(): Flow<List<Post>> = flow {
        try {
            val dtos = api.getFeedPosts()
            val posts = dtos.map { dto -> dto.toDomain(resolveAuthor(dto.authorId)) }
            emit(posts)
        } catch (e: Exception) {
            emit(FakeDataSource.posts) // graceful fallback
        }
    }

    override fun getPostById(id: String): Flow<Post?> = flow {
        try {
            val all = api.getFeedPosts()
            val dto = all.find { it.id == id }
            emit(dto?.toDomain(resolveAuthor(dto.authorId)))
        } catch (e: Exception) { emit(null) }
    }

    override fun getPostsForCircle(circleId: String): Flow<List<Post>> = flow {
        try {
            val dtos = api.getCirclePosts(circleId)
            emit(dtos.map { dto -> dto.toDomain(resolveAuthor(dto.authorId)) })
        } catch (e: Exception) { emit(emptyList()) }
    }

    override suspend fun endorsePost(postId: String): Boolean {
        return try {
            api.toggleEndorse(postId)
            true
        } catch (e: Exception) { false }
    }

    override suspend fun savePost(postId: String): Boolean = true // local-only for now

    override suspend fun createPost(post: Post): Boolean {
        return try {
            api.createPost(
                PostCreateDto(
                    content = post.content,
                    type = post.type.name,
                    circleId = post.circleId,
                    milestoneText = post.milestoneText,
                    flair = post.flair.name,
                    imageUrl = post.imageUrl,
                )
            )
            true
        } catch (e: Exception) { false }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// RealPaperRepository
// ──────────────────────────────────────────────────────────────────────────────

class RealPaperRepository @Inject constructor(
    private val api: CiteCircleApi,
    private val fireworksApi: FireworksApi,
    private val tokenManager: TokenManager,
    private val json: Json,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
) : PaperRepository {
    private val _savedPaperIds = MutableStateFlow(mutableSetOf<String>())
    private val _shelves = MutableStateFlow<List<Shelf>>(emptyList())
    private val _summaryCache = mutableMapOf<String, String>()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            tokenManager.getSavedPapersJson()?.let { jsonStr ->
                runCatching {
                    val set = json.decodeFromString(SetSerializer(String.serializer()), jsonStr)
                    _savedPaperIds.value = set.toMutableSet()
                }
            }
            tokenManager.getShelvesJson()?.let { jsonStr ->
                runCatching {
                    val shelves = json.decodeFromString(ListSerializer(Shelf.serializer()), jsonStr)
                    _shelves.value = shelves
                }
            }
        }
    }

    private fun persistState() {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                tokenManager.saveSavedPapersJson(json.encodeToString(SetSerializer(String.serializer()), _savedPaperIds.value))
                tokenManager.saveShelvesJson(json.encodeToString(ListSerializer(Shelf.serializer()), _shelves.value))
            }
        }
    }

    override fun getAllPapers(): Flow<List<Paper>> = flow {
        try {
            emit(api.getPapers().map { it.toDomain() })
        } catch (e: Exception) { emit(FakeDataSource.papers) }
    }

    override fun getPaperById(id: String): Flow<Paper?> = flow {
        try {
            emit(api.getPaper(id).toDomain())
        } catch (e: Exception) { emit(null) }
    }

    override fun getPapersForUser(userId: String): Flow<List<Paper>> = flow {
        try {
            emit(api.getPapers().map { it.toDomain() })
        } catch (e: Exception) { emit(emptyList()) }
    }

    override fun getPapersForCircle(circleId: String): Flow<List<Paper>> = flow {
        try {
            val all = api.getPapers().map { it.toDomain() }
            emit(all.filter { it.circleId == circleId })
        } catch (e: Exception) { emit(emptyList()) }
    }

    override suspend fun publishPaper(draft: PaperDraft): Paper {
        return try {
            val uri = draft.pdfUri ?: throw Exception("No file URI provided")
            val contentResolver = context.contentResolver
            
            // Read file bytes from Uri
            val inputStream = contentResolver.openInputStream(uri) ?: throw Exception("Failed to open file URI")
            val bytes = inputStream.use { it.readBytes() }
            
            // Determine MIME type
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            
            val requestFile = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData(
                "file",
                draft.pdfFileName ?: "manuscript.pdf",
                requestFile
            )
            
            val titlePart = draft.title.toRequestBody("text/plain".toMediaTypeOrNull())
            val abstractPart = draft.abstract.toRequestBody("text/plain".toMediaTypeOrNull())
            val yearPart = "2024".toRequestBody("text/plain".toMediaTypeOrNull())
            val doiPart = "".toRequestBody("text/plain".toMediaTypeOrNull())
            val journalPart = "".toRequestBody("text/plain".toMediaTypeOrNull())
            
            val response = api.publishPaper(
                file = filePart,
                title = titlePart,
                abstract = abstractPart,
                year = yearPart,
                doi = doiPart,
                journal = journalPart,
                circleId = null
            )
            response.paper.toDomain()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to local fake paper if API fails (offline/demo mode)
            Paper(
                id = "p${System.currentTimeMillis()}",
                title = draft.title,
                authors = draft.coAuthors,
                abstract = draft.abstract,
                year = 2024,
                isPublished = true,
            )
        }
    }

    override suspend fun getPaperSummary(paperId: String, title: String, abstract: String): String {
        _summaryCache[paperId]?.let { return it }
        return try {
            val systemPrompt = """
                You are a scientific summarization assistant. Given a research paper title and abstract,
                produce exactly 3 concise bullet points summarizing the paper.
                Format your response as exactly 3 lines, each starting with "• ".
                Cover: (1) the key contribution or goal, (2) the main method or finding,
                (3) the primary implication or takeaway.
                Keep each bullet to 1–2 sentences. Return ONLY the 3 bullet points.
            """.trimIndent()

            val request = ChatCompletionRequest(
                model = "accounts/fireworks/models/llama-v3p1-8b-instruct",
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = "Title: $title\n\nAbstract: $abstract")
                ),
                temperature = 0.3,
                maxTokens = 300,
                useJsonMode = false
            )
            val response = fireworksApi.chatCompletions(request)
            val summary = response.choices.firstOrNull()?.message?.content?.trim()
                ?: generateFallbackSummary(abstract)
            _summaryCache[paperId] = summary
            summary
        } catch (e: Exception) {
            val fallback = generateFallbackSummary(abstract)
            _summaryCache[paperId] = fallback
            fallback
        }
    }

    private fun generateFallbackSummary(abstract: String): String {
        val sentences = abstract.split(Regex("(?<=[.!?])\\s+")).filter { it.length > 40 }
        val b1 = sentences.getOrElse(0) { abstract.take(140) }.take(150).trimEnd('.')
        val b2 = sentences.getOrElse(1) { "" }.take(150).trimEnd('.')
        val b3 = (sentences.lastOrNull() ?: "").take(150).trimEnd('.')
        return buildString {
            append("• $b1.\n")
            if (b2.isNotEmpty() && b2 != b1) append("• $b2.\n")
            if (b3.isNotEmpty() && b3 != b1 && b3 != b2) append("• $b3.")
        }.trim()
    }

    override fun getSavedPapers(): Flow<List<Paper>> = flow {
        // Fetch all papers from API and filter to only the ones the user has saved locally
        val savedIds = _savedPaperIds.value
        if (savedIds.isEmpty()) {
            emit(emptyList())
            return@flow
        }
        try {
            val allPapers = api.getPapers(limit = 200).map { it.toDomain() }
            emit(allPapers.filter { savedIds.contains(it.id) })
        } catch (e: Exception) {
            // Fallback: return fake papers that match saved IDs (offline mode)
            emit(FakeDataSource.papers.filter { savedIds.contains(it.id) })
        }
    }

    override fun getShelves(): Flow<List<Shelf>> = _shelves.asStateFlow()

    override suspend fun createShelf(name: String, description: String): Boolean {
        val current = _shelves.value.toMutableList()
        current.add(Shelf(id = "s${System.currentTimeMillis()}", name = name, description = description))
        _shelves.value = current
        persistState()
        return true
    }

    override suspend fun addPaperToShelf(paperId: String, shelfId: String): Boolean {
        val current = _shelves.value.toMutableList()
        val idx = current.indexOfFirst { it.id == shelfId }
        if (idx < 0) return false
        val shelf = current[idx]
        if (!shelf.paperIds.contains(paperId)) {
            current[idx] = shelf.copy(paperIds = shelf.paperIds + paperId)
            _shelves.value = current
        }
        _savedPaperIds.value = _savedPaperIds.value.toMutableSet().apply { add(paperId) }
        persistState()
        return true
    }

    override suspend fun removePaperFromShelf(paperId: String, shelfId: String): Boolean {
        val current = _shelves.value.toMutableList()
        val idx = current.indexOfFirst { it.id == shelfId }
        if (idx < 0) return false
        val shelf = current[idx]
        current[idx] = shelf.copy(paperIds = shelf.paperIds.filter { it != paperId })
        _shelves.value = current
        persistState()
        return true
    }

    override suspend fun toggleSavePaper(paperId: String): Boolean {
        val saved = _savedPaperIds.value.toMutableSet()
        if (saved.contains(paperId)) saved.remove(paperId) else saved.add(paperId)
        _savedPaperIds.value = saved
        persistState()
        return true
    }

    override fun isPaperSaved(paperId: String): Flow<Boolean> = flow {
        emit(_savedPaperIds.value.contains(paperId))
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// RealCircleRepository
// ──────────────────────────────────────────────────────────────────────────────

class RealCircleRepository @Inject constructor(
    private val api: CiteCircleApi,
    private val tokenManager: TokenManager,
    private val json: Json,
) : CircleRepository {
    private val _joinedIds = MutableStateFlow(mutableSetOf<String>())

    init {
        CoroutineScope(Dispatchers.IO).launch {
            tokenManager.getJoinedCirclesJson()?.let { jsonStr ->
                runCatching {
                    val set = json.decodeFromString(SetSerializer(String.serializer()), jsonStr)
                    _joinedIds.value = set.toMutableSet()
                }
            }
        }
    }

    private fun persistState() {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                tokenManager.saveJoinedCirclesJson(json.encodeToString(SetSerializer(String.serializer()), _joinedIds.value))
            }
        }
    }

    override fun getAllCircles(): Flow<List<Circle>> = flow {
        try {
            val circles = api.getCircles().map { it.toDomain() }
            emit(circles.map { it.copy(isJoined = _joinedIds.value.contains(it.id)) })
        } catch (e: Exception) { emit(FakeDataSource.circles) }
    }

    override fun getJoinedCircles(): Flow<List<Circle>> = flow {
        try {
            val circles = api.getCircles().map { it.toDomain() }
            emit(circles.filter { _joinedIds.value.contains(it.id) })
        } catch (e: Exception) { emit(emptyList()) }
    }

    override fun getCircleById(id: String): Flow<Circle?> = flow {
        try {
            emit(api.getCircle(id).toDomain().copy(isJoined = _joinedIds.value.contains(id)))
        } catch (e: Exception) { emit(null) }
    }

    override suspend fun joinCircle(circleId: String): Boolean {
        return try {
            api.joinCircle(circleId)
            _joinedIds.value = _joinedIds.value.toMutableSet().apply { add(circleId) }
            persistState()
            true
        } catch (e: Exception) { false }
    }

    override suspend fun leaveCircle(circleId: String): Boolean {
        return try {
            api.leaveCircle(circleId)
            _joinedIds.value = _joinedIds.value.toMutableSet().apply { remove(circleId) }
            persistState()
            true
        } catch (e: Exception) { false }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// RealCommentRepository
// ──────────────────────────────────────────────────────────────────────────────

class RealCommentRepository @Inject constructor(
    private val api: CiteCircleApi,
) : CommentRepository {

    private val _userCache = mutableMapOf<String, User>()

    private suspend fun resolveAuthor(authorId: String): User {
        return _userCache.getOrPut(authorId) {
            try { api.getUser(authorId).toDomain() }
            catch (_: Exception) { User(id = authorId, name = "Unknown") }
        }
    }

    override fun getCommentsForPost(postId: String): Flow<List<Comment>> = flow {
        try {
            val dtos = api.getComments(postId)
            emit(dtos.map { it.toDomain(resolveAuthor(it.authorId)) })
        } catch (e: Exception) { emit(emptyList()) }
    }

    override suspend fun addComment(postId: String, content: String, parentId: String?): Comment {
        val dto = api.createComment(postId, CommentCreateDto(content, parentId))
        return dto.toDomain(resolveAuthor(dto.authorId))
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// RealMessageRepository
// ──────────────────────────────────────────────────────────────────────────────

class RealMessageRepository @Inject constructor(
    private val api: CiteCircleApi,
    private val fireworksApi: FireworksApi,
    private val tokenManager: TokenManager,
    private val json: Json,
) : MessageRepository {

    private val _aiMessages = MutableStateFlow<List<Message>>(emptyList())
    private var isAiInitialized = false

    private suspend fun ensureAiMessagesLoaded() {
        if (!isAiInitialized) {
            val savedJson = tokenManager.getAiMessagesJson()
            if (!savedJson.isNullOrBlank()) {
                runCatching {
                    val list = json.decodeFromString(ListSerializer(Message.serializer()), savedJson)
                    _aiMessages.value = list
                }
            }
            if (_aiMessages.value.isEmpty()) {
                val welcome = Message(
                    id = "msg_ai_welcome",
                    senderId = "ai_copilot",
                    content = "Welcome to CiteCircle AI Copilot! I am your AI academic mentor. Feel free to ask me anything about research literature, paper drafts, statistical methodology, or peer review feedback.",
                    timestamp = System.currentTimeMillis()
                )
                _aiMessages.value = listOf(welcome)
                runCatching {
                    tokenManager.saveAiMessagesJson(json.encodeToString(ListSerializer(Message.serializer()), _aiMessages.value))
                }
            }
            isAiInitialized = true
        }
    }

    override fun getConversations(): Flow<List<Conversation>> = flow {
        ensureAiMessagesLoaded()
        val aiCopilotUser = User(
            id = "ai_copilot",
            name = "CiteCircle AI Copilot",
            avatarUrl = "https://api.dicebear.com/8.x/bottts/svg?seed=CiteCircleAI",
            role = UserRole.RESEARCHER,
            institution = "CiteCircle AI Core",
            bio = "AI Academic Assistant"
        )
        val aiConv = Conversation(
            id = "conv_ai",
            participants = listOf(aiCopilotUser),
            lastMessage = _aiMessages.value.lastOrNull(),
            unreadCount = 0
        )

        try {
            val convDtos = api.getConversations()
            val realConversations = convDtos
                .filter { it.id != "conv_ai" } // avoid duplicating the AI conv
                .map { dto ->
                    val msgs = try { api.getMessages(dto.id) } catch (_: Exception) { emptyList() }
                    Conversation(
                        id = dto.id,
                        participants = emptyList(),
                        lastMessage = msgs.lastOrNull()?.toDomain(),
                        unreadCount = msgs.count { !it.isRead },
                    )
                }
            // Always prepend the AI conversation at the top
            emit(listOf(aiConv) + realConversations)
        } catch (e: Exception) {
            // Prepend AI conv to fake data as well
            emit(listOf(aiConv) + FakeDataSource.conversations.filter { it.id != "conv_ai" })
        }
    }

    override fun getMessagesForConversation(convId: String): Flow<List<Message>> = flow {
        try {
            emit(api.getMessages(convId).map { it.toDomain() })
        } catch (e: Exception) { emit(emptyList()) }
    }

    override suspend fun sendMessage(convId: String, content: String): Message {
        return try {
            val dtos = api.sendMessage(convId, MessageCreateDto(content))
            dtos.first().toDomain()
        } catch (e: Exception) {
            Message(
                id = "msg_${System.currentTimeMillis()}",
                senderId = FakeDataSource.currentUser.id,
                content = content,
                timestamp = System.currentTimeMillis()
            )
        }
    }


    override suspend fun searchUsers(query: String): List<User> {
        return try {
            api.getSuggestedUsers()
                .map { it.toDomain() }
                .filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.institution.contains(query, ignoreCase = true)
                }
        } catch (e: Exception) { emptyList() }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// RealNotificationRepository
// ──────────────────────────────────────────────────────────────────────────────

class RealNotificationRepository @Inject constructor(
    private val api: CiteCircleApi,
) : NotificationRepository {

    private val _userCache = mutableMapOf<String, User>()

    private suspend fun resolveActor(actorId: String): User {
        return _userCache.getOrPut(actorId) {
            try { api.getUser(actorId).toDomain() }
            catch (_: Exception) { User(id = actorId, name = "Someone") }
        }
    }

    override fun getNotifications(): Flow<List<Notification>> = flow {
        try {
            emit(api.getNotifications().map { it.toDomain(resolveActor(it.actorId)) })
        } catch (e: Exception) { emit(FakeDataSource.notifications) }
    }

    override suspend fun markAsRead(notifId: String) {
        try { api.markNotificationRead(notifId) } catch (_: Exception) {}
    }

    override suspend fun dismissNotification(notifId: String) {
        // Server-side dismissal not yet implemented; no-op
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// RealSearchRepository
// ──────────────────────────────────────────────────────────────────────────────

class RealSearchRepository @Inject constructor(
    private val api: CiteCircleApi,
) : SearchRepository {
    private val _recentSearches = MutableStateFlow<List<String>>(
        listOf("causal inference LLMs", "genomics population", "quantum error correction")
    )

    override suspend fun search(query: String): SearchResults {
        if (query.isBlank()) return SearchResults()
        // Server search returns a Map; parse manually
        return try {
            // Use local filtering on all entities as a simple search
            val users = api.getSuggestedUsers().map { it.toDomain() }
                .filter { u -> u.name.contains(query, true) || u.fieldOfStudy.contains(query, true) }
            val papers = api.getPapers().map { it.toDomain() }
                .filter { p -> p.title.contains(query, true) || p.abstract.contains(query, true) }
            SearchResults(people = users, papers = papers)
        } catch (e: Exception) { SearchResults() }
    }

    override fun getRecentSearches(): Flow<List<String>> = _recentSearches.asStateFlow()

    override suspend fun addRecentSearch(query: String) {
        val current = _recentSearches.value.toMutableList()
        current.remove(query)
        current.add(0, query)
        _recentSearches.value = current.take(10)
    }

    override suspend fun removeRecentSearch(query: String) {
        _recentSearches.value = _recentSearches.value.filter { it != query }
    }
}
