package com.citecircle.app.core.data

import com.citecircle.app.core.model.*
import com.citecircle.app.core.network.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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
    coverUrl = coverUrl,
    role = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.STUDENT),
    institution = institution,
    fieldOfStudy = fieldOfStudy,
    bio = bio,
    orcidId = orcidId,
    followerCount = followerCount,
    followingCount = followingCount,
    citationCount = citationCount,
    externalCitationCount = externalCitationCount,
    publicationCount = publicationCount,
    orcidVerified = orcidVerified,
    isVerified = isVerified,
    interests = interests,
)

private fun PublicationDto.toDomain() = Publication(
    id = id,
    title = title,
    abstract = abstract,
    doi = doi,
    journal = journal,
    year = publicationYear,
    citationCount = citationCount,
    workType = workType,
    isOpenAccess = isOpenAccess,
    openAccessUrl = openAccessUrl,
    openAlexId = openAlexId,
    lastSyncedAt = lastSyncedAt,
)

private fun CoauthorDto.toDomain() = Coauthor(
    openAlexAuthorId = openAlexAuthorId,
    displayName = displayName,
    orcidId = orcidId,
    institution = institution,
    userId = userId,
    sharedPublications = sharedPublications,
)

private fun OrcidSyncStateDto.toDomain() = OrcidSyncState(
    orcidId = orcidId,
    status = runCatching { SyncStatus.valueOf(status) }.getOrDefault(SyncStatus.IDLE),
    worksSynced = worksSynced,
    lastSuccessAt = lastSuccessAt,
    lastError = lastError,
    nextEligibleAt = nextEligibleAt,
)

private fun PostDto.toDomain(author: User = User(id = authorId, name = "")) = Post(
    id = id,
    author = author,
    content = content,
    type = runCatching { PostType.valueOf(type) }.getOrDefault(PostType.DISCUSSION),
    timestamp = timestamp,
    endorseCount = endorseCount,
    commentCount = commentCount,
    isEndorsed = isEndorsed,
    isSaved = isSaved,
    circleId = circleId,
    milestoneText = milestoneText,
    flair = runCatching { PostFlair.valueOf(flair) }.getOrDefault(PostFlair.NONE),
    imageUrl = imageUrl,
)

private fun ShelfDto.toDomain() = Shelf(
    id = id,
    name = name,
    description = description,
    paperIds = paperIds,
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

private fun CircleMemberDto.toDomain() = CircleMember(
    userId = userId,
    circleId = circleId,
    role = runCatching { CircleRole.valueOf(role) }.getOrDefault(CircleRole.CONTRIBUTOR),
    joinedAt = joinedAt,
    name = name,
    avatarUrl = avatarUrl,
    institution = institution
)

private fun CircleWorkspaceDto.toDomain() = CircleWorkspace(
    id = id,
    circleId = circleId,
    pinBoardText = pinBoardText,
    accessType = runCatching { CircleAccessType.valueOf(accessType) }.getOrDefault(CircleAccessType.PUBLIC),
    inviteCode = inviteCode,
    updatedAt = updatedAt
)

private fun CircleDraftDto.toDomain() = CircleDraft(
    id = id,
    circleId = circleId,
    title = title,
    abstract = abstract,
    leadAuthorId = leadAuthorId,
    leadAuthorName = leadAuthorName,
    leadAuthorAvatar = leadAuthorAvatar,
    fileFormat = runCatching { DraftFormat.valueOf(fileFormat) }.getOrDefault(DraftFormat.PDF),
    fileUrl = fileUrl,
    status = runCatching { DraftStatus.valueOf(status) }.getOrDefault(DraftStatus.DRAFT),
    version = version,
    sections = sections,
    createdAt = createdAt,
    reviewCount = reviewCount,
    commentCount = commentCount
)

private fun DraftReviewRequestDto.toDomain() = DraftReviewRequest(
    id = id,
    draftId = draftId,
    reviewerId = reviewerId,
    reviewerName = reviewerName,
    reviewerAvatar = reviewerAvatar,
    requesterId = requesterId,
    sectionTarget = sectionTarget,
    status = runCatching { ReviewRequestStatus.valueOf(status) }.getOrDefault(ReviewRequestStatus.PENDING),
    notes = notes,
    createdAt = createdAt
)

private fun DraftCommentDto.toDomain() = DraftComment(
    id = id,
    draftId = draftId,
    authorId = authorId,
    authorName = authorName,
    authorAvatarUrl = authorAvatarUrl,
    sectionIndex = sectionIndex,
    paragraphOffset = paragraphOffset,
    content = content,
    isResolved = isResolved,
    createdAt = createdAt
)

private fun CircleReadingListDto.toDomain() = CircleReadingList(
    id = id,
    circleId = circleId,
    title = title,
    description = description,
    createdById = createdById,
    createdByName = createdByName,
    createdAt = createdAt,
    paperCount = paperCount,
    papers = papers.map { it.toDomain() }
)

private fun CitationGraphNodeDto.toDomain() = CitationGraphNode(
    id = id,
    title = title,
    abstract = abstract,
    citationCount = citationCount,
    year = year,
    circleId = circleId,
    field = field,
    authors = authors.map { it.toDomain() },
    doi = doi,
    journal = journal,
    isCenter = isCenter,
    hopDistance = hopDistance,
    x = x,
    y = y
)

private fun CitationGraphEdgeDto.toDomain() = CitationGraphEdge(
    source = source,
    target = target,
    type = type
)

private fun CitationGraphSummaryDto.toDomain() = CitationGraphSummary(
    totalPapers = totalPapers,
    totalCitations = totalCitations,
    maxDepth = maxDepth
)

private fun CitationGraphResponseDto.toDomain() = CitationGraphResponse(
    nodes = nodes.map { it.toDomain() },
    edges = edges.map { it.toDomain() },
    summary = summary.toDomain()
)

private fun CoauthorGraphNodeDto.toDomain() = CoauthorGraphNode(
    id = id,
    name = name,
    avatarUrl = avatarUrl,
    institution = institution,
    fieldOfStudy = fieldOfStudy,
    citationCount = citationCount,
    hIndex = hIndex,
    i10Index = i10Index,
    clusterId = clusterId,
    isCenter = isCenter,
    x = x,
    y = y
)

private fun CoauthorGraphEdgeDto.toDomain() = CoauthorGraphEdge(
    source = source,
    target = target,
    weight = weight,
    publications = publications
)

private fun CoauthorClusterDto.toDomain() = CoauthorCluster(
    id = id,
    name = name,
    color = color,
    memberIds = memberIds
)

private fun CitationVelocityPointDto.toDomain() = CitationVelocityPoint(
    year = year,
    count = count
)

private fun ResearcherAnalyticsDto.toDomain() = ResearcherAnalytics(
    totalCitations = totalCitations,
    hIndex = hIndex,
    i10Index = i10Index,
    citationVelocity = citationVelocity.map { it.toDomain() }
)

private fun CoauthorGraphResponseDto.toDomain() = CoauthorGraphResponse(
    nodes = nodes.map { it.toDomain() },
    edges = edges.map { it.toDomain() },
    clusters = clusters.map { it.toDomain() },
    analytics = analytics.toDomain()
)

// ──────────────────────────────────────────────────────────────────────────────
// RealAuthRepository
// ──────────────────────────────────────────────────────────────────────────────


class RealAuthRepository @Inject constructor(
    private val api: CiteCircleApi,
    private val tokenManager: TokenManager,
    private val userRepository: UserRepository,
    private val json: Json,
) : AuthRepository {

    private val _savedAccounts = MutableStateFlow<List<SavedAccount>>(emptyList())

    init {
        CoroutineScope(Dispatchers.IO).launch {
            tokenManager.getSavedAccountsJson()?.let { jsonStr ->
                runCatching {
                    val list = json.decodeFromString(ListSerializer(SavedAccount.serializer()), jsonStr)
                    _savedAccounts.value = list
                }
            }
        }
    }

    private fun persistAccounts(accounts: List<SavedAccount>) {
        _savedAccounts.value = accounts
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                tokenManager.saveSavedAccountsJson(json.encodeToString(ListSerializer(SavedAccount.serializer()), accounts))
            }
        }
    }

    override fun isLoggedIn(): Boolean = runBlocking { tokenManager.isLoggedIn() }

    override suspend fun login(email: String, password: String): Boolean {
        return try {
            val resp = api.login(LoginRequestDto(email, password))
            tokenManager.saveTokens(resp.accessToken, resp.refreshToken, resp.userId)
            tokenManager.saveUserEmail(email)
            val me = api.getMe().toDomain()
            userRepository.updateCurrentUser(me)
            addSavedAccountSession(me, email, resp.accessToken, resp.refreshToken)
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
            tokenManager.saveUserEmail(email)
            userRepository.updateCurrentUser(localUser)
            addSavedAccountSession(localUser, email, "demo_access_token", "demo_refresh_token")
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
            tokenManager.saveUserEmail(email)
            val me = try { api.getMe().toDomain() } catch (_: Exception) {
                User(
                    id = resp.userId,
                    name = nameFromEmail,
                    role = role,
                    institution = "CiteCircle Affiliate"
                )
            }
            userRepository.updateCurrentUser(me)
            addSavedAccountSession(me, email, resp.accessToken, resp.refreshToken)
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
            tokenManager.saveUserEmail(email)
            userRepository.updateCurrentUser(newUser)
            addSavedAccountSession(newUser, email, "demo_access_token", "demo_refresh_token")
            true
        }
    }

    override suspend fun logout() {
        val currentUserId = tokenManager.getCurrentUserId()
        val accounts = _savedAccounts.value
        val remaining = accounts.filter { it.userId != currentUserId }

        if (remaining.isNotEmpty()) {
            val nextActive = remaining.first()
            tokenManager.saveTokens(nextActive.accessToken, nextActive.refreshToken, nextActive.userId)
            tokenManager.saveUserEmail(nextActive.email)

            val updated = remaining.map { acc ->
                acc.copy(isActive = (acc.userId == nextActive.userId))
            }
            persistAccounts(updated)

            val switchedUser = User(
                id = nextActive.userId,
                name = nextActive.name,
                avatarUrl = nextActive.avatarUrl,
                role = runCatching { UserRole.valueOf(nextActive.role) }.getOrDefault(UserRole.STUDENT),
                institution = "CiteCircle Network"
            )
            userRepository.updateCurrentUser(switchedUser)
        } else {
            persistAccounts(emptyList())
            tokenManager.clearTokens()
        }
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> {
        if (oldPassword.isBlank()) {
            return Result.failure(Exception("Current password is required."))
        }
        if (newPassword.length < 6 || (!newPassword.any { it.isUpperCase() } && !newPassword.any { it.isDigit() })) {
            return Result.failure(Exception("Password must be >= 6 chars with at least one uppercase letter or number."))
        }
        delay(600)
        return Result.success(Unit)
    }

    override suspend fun changeEmail(newEmail: String, currentPassword: String): Result<Unit> {
        if (!newEmail.contains("@") || !newEmail.contains(".")) {
            return Result.failure(Exception("Please enter a valid email address."))
        }
        if (currentPassword.isBlank()) {
            return Result.failure(Exception("Current password is required to verify identity."))
        }
        delay(600)
        tokenManager.saveUserEmail(newEmail)
        return Result.success(Unit)
    }

    override fun getSavedAccounts(): Flow<List<SavedAccount>> = _savedAccounts.asStateFlow()

    override suspend fun switchAccount(userId: String): Boolean {
        val accounts = _savedAccounts.value
        val target = accounts.find { it.userId == userId } ?: return false

        tokenManager.saveTokens(target.accessToken, target.refreshToken, target.userId)
        tokenManager.saveUserEmail(target.email)

        val updated = accounts.map { acc ->
            acc.copy(isActive = (acc.userId == userId))
        }
        persistAccounts(updated)

        val switchedUser = User(
            id = target.userId,
            name = target.name,
            avatarUrl = target.avatarUrl,
            role = runCatching { UserRole.valueOf(target.role) }.getOrDefault(UserRole.STUDENT),
            institution = "CiteCircle Network"
        )
        userRepository.updateCurrentUser(switchedUser)
        return true
    }

    override suspend fun addAccount(email: String, password: String): Result<Boolean> {
        if (!email.contains("@") || password.length < 4) {
            return Result.failure(Exception("Please enter a valid email and password."))
        }
        val success = login(email, password)
        return if (success) Result.success(true) else Result.failure(Exception("Authentication failed for $email"))
    }

    override suspend fun removeAccount(userId: String): Boolean {
        val updated = _savedAccounts.value.filter { it.userId != userId }
        persistAccounts(updated)
        return true
    }

    override suspend fun clearCache(): Boolean {
        delay(400)
        return true
    }

    override suspend fun exportUserData(): Result<String> {
        delay(500)
        val activeAcc = _savedAccounts.value.find { it.isActive }
        val jsonSummary = """
            {
              "exportTimestamp": ${System.currentTimeMillis()},
              "appName": "CiteCircle",
              "savedAccountsCount": ${_savedAccounts.value.size},
              "activeUser": {
                "userId": "${activeAcc?.userId ?: ""}",
                "name": "${activeAcc?.name ?: ""}",
                "email": "${activeAcc?.email ?: ""}",
                "role": "${activeAcc?.role ?: ""}"
              }
            }
        """.trimIndent()
        return Result.success(jsonSummary)
    }

    override suspend fun logoutAll() {
        persistAccounts(emptyList())
        tokenManager.clearAll()
    }

    private fun addSavedAccountSession(user: User, email: String, access: String, refresh: String) {
        val current = _savedAccounts.value.map { it.copy(isActive = false) }.toMutableList()
        val existingIdx = current.indexOfFirst { it.userId == user.id }
        val account = SavedAccount(
            userId = user.id,
            email = email,
            name = user.name,
            avatarUrl = user.avatarUrl,
            role = user.role.name,
            accessToken = access,
            refreshToken = refresh,
            isActive = true
        )
        if (existingIdx >= 0) {
            current[existingIdx] = account
        } else {
            current.add(0, account)
        }
        persistAccounts(current)
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
            val user = try {
                api.getUser(id).toDomain()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            emit(user)
        }
    }

    override fun getAllUsers(): Flow<List<User>> = flow {
        val users = try {
            api.getSuggestedUsers().map { it.toDomain() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
        emit(users)
    }

    override fun getSuggestedConnections(): Flow<List<User>> = flow {
        val users = try {
            val pending = _pendingConnectionIds.value
            api.getSuggestedUsers().map { it.toDomain().copy(connectionPending = pending.contains(it.id)) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
        emit(users)
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
        return try {
            api.acceptConnection(userId)
            true
        } catch (e: Exception) { false }
    }

    override suspend fun declineConnection(userId: String): Boolean {
        return try {
            api.declineConnection(userId)
            true
        } catch (e: Exception) { false }
    }

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
                    coverUrl = user.coverUrl.takeIf { it.isNotBlank() },
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

    override fun getCoauthorGraph(userId: String): Flow<CoauthorGraphResponse> = flow {
        val result = try {
            api.getUserCoauthorGraph(userId).toDomain()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            var fallback: CoauthorGraphResponse? = null
            FakeUserRepository().getCoauthorGraph(userId).collect { fallback = it }
            fallback ?: CoauthorGraphResponse()
        }
        emit(result)
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
        val posts = try {
            val dtos = api.getFeedPosts()
            dtos.map { dto -> dto.toDomain(resolveAuthor(dto.authorId)) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FakeDataSource.posts
        }
        emit(posts)
    }

    override fun getSavedPosts(): Flow<List<Post>> = flow {
        val posts = try {
            val dtos = api.getSavedPosts()
            dtos.map { dto -> dto.toDomain(resolveAuthor(dto.authorId)) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
        emit(posts)
    }

    override fun getPostById(id: String): Flow<Post?> = flow {
        val post = try {
            val dto = api.getPost(id)
            dto.toDomain(resolveAuthor(dto.authorId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            runCatching {
                val all = api.getFeedPosts()
                val dto = all.find { it.id == id }
                dto?.toDomain(resolveAuthor(dto.authorId))
            }.getOrNull()
        }
        emit(post)
    }

    override fun getPostsForCircle(circleId: String): Flow<List<Post>> = flow {
        val posts = try {
            val dtos = api.getCirclePosts(circleId)
            dtos.map { dto -> dto.toDomain(resolveAuthor(dto.authorId)) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
        emit(posts)
    }



    override suspend fun endorsePost(postId: String): Boolean {
        return try {
            api.toggleEndorse(postId)
            true
        } catch (e: Exception) { false }
    }

    override suspend fun savePost(postId: String): Boolean {
        return try {
            api.toggleSavePost(postId)
            true
        } catch (e: Exception) { false }
    }


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
    private val _savedPaperIds = MutableStateFlow<Set<String>>(emptySet())
    private val _shelves = MutableStateFlow<List<Shelf>>(emptyList())
    private val _summaryCache = mutableMapOf<String, String>()

    private companion object {
        const val DEFAULT_SHELF_NAME = "Saved"
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            tokenManager.getSavedPapersJson()?.let { jsonStr ->
                runCatching {
                    val set = json.decodeFromString(SetSerializer(String.serializer()), jsonStr)
                    _savedPaperIds.value = set.toSet()
                }
            }
            tokenManager.getShelvesJson()?.let { jsonStr ->
                runCatching {
                    val shelves = json.decodeFromString(ListSerializer(Shelf.serializer()), jsonStr)
                    _shelves.value = shelves.distinctBy { it.id }
                }
            }
            // Local cache is only a warm start; the server is authoritative when reachable.
            refreshShelves()
        }
    }

    /** Pulls shelves from the server. Returns false (leaving the cache intact) when offline. */
    private suspend fun refreshShelves(): Boolean = runCatching {
        val remote = api.getShelves().map { it.toDomain() }
        _shelves.value = remote.distinctBy { it.id }
        _savedPaperIds.value = remote.flatMap { it.paperIds }.toSet()
        persistState()
        true
    }.getOrDefault(false)

    /**
     * Resolves the shelf backing the plain "save paper" action. The backend models
     * bookmarks as shelf membership, so a toggle needs a concrete shelf to write to.
     */
    private suspend fun defaultShelf(): Shelf? {
        _shelves.value.firstOrNull { it.name == DEFAULT_SHELF_NAME }?.let { return it }
        return runCatching {
            api.createShelf(CreateShelfDto(DEFAULT_SHELF_NAME, "Papers you saved")).toDomain()
        }.getOrNull()?.also { shelf ->
            _shelves.value = (_shelves.value + shelf).distinctBy { it.id }
            persistState()
        }
    }

    private fun persistState() {
        val savedPaperIdsSnapshot = _savedPaperIds.value.toSet()
        val shelvesSnapshot = _shelves.value.toList()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                tokenManager.saveSavedPapersJson(json.encodeToString(SetSerializer(String.serializer()), savedPaperIdsSnapshot))
                tokenManager.saveShelvesJson(json.encodeToString(ListSerializer(Shelf.serializer()), shelvesSnapshot))
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
        val uri = draft.pdfUri ?: throw IllegalStateException("No manuscript file was selected")
        val contentResolver = context.contentResolver

        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Couldn't read the selected file")
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
        return response.paper.toDomain()
    }

    override suspend fun citePaper(paperId: String): Paper {
        return try {
            api.citePaper(paperId)
            api.getPaper(paperId).toDomain()
        } catch (e: Exception) {
            Paper(id = paperId, title = "")
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
        // Saved state lives in shelf membership server-side, so sync before filtering.
        refreshShelves()
        val savedIds = _savedPaperIds.value
        if (savedIds.isEmpty()) {
            emit(emptyList())
            return@flow
        }
        try {
            val allPapers = api.getPapers(limit = 200).map { it.toDomain() }.distinctBy { it.id }
            emit(allPapers.filter { savedIds.contains(it.id) })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getShelves(): Flow<List<Shelf>> =
        _shelves.asStateFlow().onStart { refreshShelves() }

    override suspend fun createShelf(name: String, description: String): Boolean {
        // Created server-side first so the row carries the backend's id; a locally
        // minted id would never match the shelf that later comes back from /shelves.
        val remote = runCatching { api.createShelf(CreateShelfDto(name, description)).toDomain() }
            .getOrNull()
        if (remote == null) return false
        _shelves.value = (_shelves.value + remote).distinctBy { it.id }
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
            _shelves.value = current.distinctBy { it.id }
        }
        _savedPaperIds.value = _savedPaperIds.value + paperId
        persistState()
        return runCatching { api.addPaperToShelf(shelfId, paperId); true }.getOrDefault(false)
    }

    override suspend fun removePaperFromShelf(paperId: String, shelfId: String): Boolean {
        val current = _shelves.value.toMutableList()
        val idx = current.indexOfFirst { it.id == shelfId }
        if (idx < 0) return false
        val shelf = current[idx]
        current[idx] = shelf.copy(paperIds = shelf.paperIds.filter { it != paperId })
        _shelves.value = current.distinctBy { it.id }
        if (_shelves.value.none { it.paperIds.contains(paperId) }) {
            _savedPaperIds.value = _savedPaperIds.value - paperId
        }
        persistState()
        return runCatching { api.removePaperFromShelf(shelfId, paperId); true }.getOrDefault(false)
    }

    override suspend fun toggleSavePaper(paperId: String): Boolean {
        val shelf = defaultShelf() ?: return false
        return if (_savedPaperIds.value.contains(paperId)) {
            removePaperFromShelf(paperId, shelf.id)
        } else {
            addPaperToShelf(paperId, shelf.id)
        }
    }

    override fun isPaperSaved(paperId: String): Flow<Boolean> =
        _savedPaperIds.map { it.contains(paperId) }

    override fun getAnnotations(paperId: String): Flow<List<PaperAnnotation>> = flow {
        try {
            val dtos = api.getPaperAnnotations(paperId)
            emit(dtos.map { it.toDomain() })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun saveAnnotation(annotation: PaperAnnotation): PaperAnnotation {
        return try {
            val dto = api.createPaperAnnotation(
                annotation.paperId,
                PaperAnnotationCreateDto(
                    pageNumber = annotation.pageNumber,
                    selectedText = annotation.selectedText,
                    color = annotation.color.name,
                    noteText = annotation.noteText,
                    xRatio = annotation.xRatio,
                    yRatio = annotation.yRatio
                )
            )
            dto.toDomain()
        } catch (e: Exception) {
            annotation
        }
    }

    override suspend fun deleteAnnotation(paperId: String, annotationId: String): Boolean {
        return try {
            api.deletePaperAnnotation(paperId, annotationId)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getAiPaperBreakdown(paperId: String): AiPaperBreakdown {
        return try {
            api.getAiPaperBreakdown(paperId).toDomain()
        } catch (e: Exception) {
            AiPaperBreakdown(
                paperId = paperId,
                abstractTldr = "Core paper breakdown analyzing key technical contributions and methodology.",
                methodologySetup = "Standardized test suite and evaluation protocol.",
                coreResults = "Strong empirical performance across target benchmark indicators.",
                limitationsFutureWork = "Expansion of experimental coverage and low-latency optimizations.",
                keyTakeaways = listOf(
                    "Demonstrates practical applicability in modern workflows.",
                    "Presents structured methodology and clear evaluation criteria.",
                    "Suggests promising directions for follow-up research."
                ),
                methodologyQualityIndex = 88,
                qualityLabel = "High Methodological Rigor"
            )
        }
    }

    override fun getCitationGraph(paperId: String, depth: Int, minCitations: Int): Flow<CitationGraphResponse> = flow {
        val result = try {
            api.getPaperCitationGraph(paperId, depth, minCitations).toDomain()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            var fallback: CitationGraphResponse? = null
            FakePaperRepository(fireworksApi).getCitationGraph(paperId, depth, minCitations).collect { fallback = it }
            fallback ?: CitationGraphResponse()
        }
        emit(result)
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
        } catch (e: Exception) { emit(emptyList()) }
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

    override fun getCircleWorkspace(circleId: String): Flow<CircleWorkspace?> = flow {
        val ws = try {
            api.getCircleWorkspace(circleId).toDomain()
        } catch (e: Exception) {
            CircleWorkspace(id = "ws_$circleId", circleId = circleId, pinBoardText = "")
        }
        emit(ws)
    }

    override suspend fun updatePinboard(circleId: String, text: String): Boolean {
        return try {
            api.updatePinboard(circleId, mapOf("pin_board_text" to text))
            true
        } catch (e: Exception) { false }
    }

    override fun getCircleMembers(circleId: String): Flow<List<CircleMember>> = flow {
        val members = try {
            api.getCircleMembers(circleId).map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
        emit(members)
    }

    override suspend fun updateMemberRole(circleId: String, userId: String, role: CircleRole): Boolean {
        return try {
            api.updateMemberRole(circleId, userId, mapOf("role" to role.name))
            true
        } catch (e: Exception) { false }
    }

    override suspend fun generateInviteCode(circleId: String): String {
        return try {
            val res = api.generateInviteCode(circleId)
            res["invite_code"] ?: "INV-${circleId.take(4).uppercase()}-2026"
        } catch (e: Exception) {
            "INV-${circleId.take(4).uppercase()}-2026"
        }
    }

    override fun getCircleDrafts(circleId: String): Flow<List<CircleDraft>> = flow {
        val drafts = try {
            api.getCircleDrafts(circleId).map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
        emit(drafts)
    }

    override fun getDraftDetails(draftId: String): Flow<CircleDraft?> = flow {
        val draft = try {
            api.getDraft(draftId).toDomain()
        } catch (e: Exception) {
            null
        }
        emit(draft)
    }

    override suspend fun createCircleDraft(
        circleId: String,
        title: String,
        abstract: String,
        fileFormat: DraftFormat
    ): CircleDraft {
        return try {
            api.createCircleDraft(
                circleId,
                CircleDraftCreateDto(title = title, abstract = abstract, fileFormat = fileFormat.name)
            ).toDomain()
        } catch (e: Exception) {
            CircleDraft(
                id = "drf_${System.currentTimeMillis()}",
                circleId = circleId,
                title = title,
                abstract = abstract,
                leadAuthorId = "usr_me",
                fileFormat = fileFormat
            )
        }
    }

    override fun getDraftReviewRequests(draftId: String): Flow<List<DraftReviewRequest>> = flow {
        val reqs = try {
            api.getDraftReviewRequests(draftId).map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
        emit(reqs)
    }

    override suspend fun requestDraftReview(
        draftId: String,
        reviewerId: String,
        sectionTarget: String,
        notes: String
    ): DraftReviewRequest {
        return try {
            api.createDraftReviewRequest(
                draftId,
                DraftReviewRequestCreateDto(reviewerId = reviewerId, sectionTarget = sectionTarget, notes = notes)
            ).toDomain()
        } catch (e: Exception) {
            DraftReviewRequest(
                id = "rr_${System.currentTimeMillis()}",
                draftId = draftId,
                reviewerId = reviewerId,
                requesterId = "usr_me",
                sectionTarget = sectionTarget,
                notes = notes
            )
        }
    }

    override fun getDraftComments(draftId: String): Flow<List<DraftComment>> = flow {
        val cmts = try {
            api.getDraftComments(draftId).map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
        emit(cmts)
    }

    override suspend fun addDraftComment(
        draftId: String,
        sectionIndex: Int,
        paragraphOffset: Int,
        content: String
    ): DraftComment {
        return try {
            api.createDraftComment(
                draftId,
                DraftCommentCreateDto(sectionIndex = sectionIndex, paragraphOffset = paragraphOffset, content = content)
            ).toDomain()
        } catch (e: Exception) {
            DraftComment(
                id = "dc_${System.currentTimeMillis()}",
                draftId = draftId,
                authorId = "usr_me",
                sectionIndex = sectionIndex,
                paragraphOffset = paragraphOffset,
                content = content
            )
        }
    }

    override suspend fun resolveDraftComment(commentId: String): Boolean {
        return try {
            api.resolveDraftComment(commentId)
            true
        } catch (e: Exception) { false }
    }

    override fun getCircleReadingLists(circleId: String): Flow<List<CircleReadingList>> = flow {
        val lists = try {
            api.getCircleReadingLists(circleId).map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
        emit(lists)
    }

    override suspend fun createCircleReadingList(
        circleId: String,
        title: String,
        description: String,
        paperIds: List<String>
    ): CircleReadingList {
        return try {
            api.createCircleReadingList(
                circleId,
                CircleReadingListCreateDto(title = title, description = description, paperIds = paperIds)
            ).toDomain()
        } catch (e: Exception) {
            CircleReadingList(
                id = "rl_${System.currentTimeMillis()}",
                circleId = circleId,
                title = title,
                description = description,
                createdById = "usr_me"
            )
        }
    }

    override suspend fun saveReadingListToMyLibrary(readingListId: String): Boolean {
        return try {
            api.saveReadingListToLibrary(readingListId)
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
            // Prepend AI conv only
            emit(listOf(aiConv))
        }
    }

    override fun getMessagesForConversation(convId: String): Flow<List<Message>> = flow {
        if (convId == "conv_ai") {
            ensureAiMessagesLoaded()
            emit(_aiMessages.value)
            try {
                val remoteMsgs = api.getMessages(convId).map { it.toDomain() }
                if (remoteMsgs.isNotEmpty()) {
                    _aiMessages.value = remoteMsgs
                    runCatching {
                        tokenManager.saveAiMessagesJson(json.encodeToString(ListSerializer(Message.serializer()), remoteMsgs))
                    }
                    emit(remoteMsgs)
                }
            } catch (_: Exception) {}
        } else {
            try {
                emit(api.getMessages(convId).map { it.toDomain() })
            } catch (e: Exception) { emit(emptyList()) }
        }
    }

    override suspend fun sendMessage(convId: String, content: String): Message {
        if (convId == "conv_ai") {
            ensureAiMessagesLoaded()
            val now = System.currentTimeMillis()
            val userMsg = Message(
                id = "msg_u_$now",
                senderId = FakeDataSource.currentUser.id,
                content = content,
                timestamp = now
            )

            // Instantly append user message to local state
            val listWithUser = _aiMessages.value.filterNot { it.id.startsWith("msg_optimistic_") } + userMsg
            _aiMessages.value = listWithUser

            val aiReplyContent = try {
                val request = ChatCompletionRequest(
                    model = "accounts/fireworks/models/deepseek-v4-pro",
                    messages = listOf(
                        ChatMessage(
                            role = "system",
                            content = "You are the CiteCircle Research Copilot, a concise assistant for academics. Help with literature questions, methodology, and writing. Keep answers under 250 words."
                        ),
                        ChatMessage(role = "user", content = content)
                    ),
                    temperature = 0.7,
                    maxTokens = 600,
                    useJsonMode = false
                )
                val response = fireworksApi.chatCompletions(request)
                response.choices.firstOrNull()?.message?.content?.trim()
                    ?: "I'm sorry, I couldn't process that request right now."
            } catch (e: Exception) {
                "AI Copilot is temporarily unavailable (${e.localizedMessage}). Please try again."
            }

            val aiMsg = Message(
                id = "msg_ai_${System.currentTimeMillis()}",
                senderId = "ai_copilot",
                content = aiReplyContent,
                timestamp = System.currentTimeMillis()
            )

            val updatedList = listWithUser + aiMsg
            _aiMessages.value = updatedList
            runCatching {
                tokenManager.saveAiMessagesJson(json.encodeToString(ListSerializer(Message.serializer()), updatedList))
            }

            // Sync with server asynchronously in the background so UI never waits on Render
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { api.sendMessage(convId, MessageCreateDto(content)) }
            }

            return userMsg
        }

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

    override suspend fun clearChatHistory(convId: String): Boolean {
        if (convId == "conv_ai") {
            val welcome = Message(
                id = "msg_ai_welcome_${System.currentTimeMillis()}",
                senderId = "ai_copilot",
                content = "Welcome to CiteCircle AI Copilot! Chat history has been cleared. Feel free to ask me anything about research literature, paper drafts, statistical methodology, or peer review feedback.",
                timestamp = System.currentTimeMillis()
            )
            _aiMessages.value = listOf(welcome)
            runCatching {
                tokenManager.saveAiMessagesJson(json.encodeToString(ListSerializer(Message.serializer()), _aiMessages.value))
            }
        }
        return try {
            api.clearMessages(convId)
            true
        } catch (e: Exception) {
            true
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
        } catch (e: Exception) { emit(emptyList()) }
    }

    override suspend fun markAsRead(notifId: String) {
        try { api.markNotificationRead(notifId) } catch (_: Exception) {}
    }

    override suspend fun dismissNotification(notifId: String) {
        try { api.dismissNotification(notifId) } catch (_: Exception) {}
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// RealSearchRepository
// ──────────────────────────────────────────────────────────────────────────────

class RealSearchRepository @Inject constructor(
    private val api: CiteCircleApi,
) : SearchRepository {
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())

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

// ──────────────────────────────────────────────────────────────────────────────
// Publications (ORCID / OpenAlex sync)
// ──────────────────────────────────────────────────────────────────────────────

class RealPublicationRepository @Inject constructor(
    private val api: CiteCircleApi,
) : PublicationRepository {

    private val _syncState = MutableStateFlow(OrcidSyncState())

    override fun getPublications(userId: String?): Flow<List<Publication>> = flow {
        try {
            val dtos = if (userId == null) api.getMyPublications() else api.getUserPublications(userId)
            emit(dtos.map { it.toDomain() })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getCoauthors(userId: String?): Flow<List<Coauthor>> = flow {
        try {
            val dtos = if (userId == null) api.getMyCoauthors() else api.getUserCoauthors(userId)
            emit(dtos.map { it.toDomain() })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getSyncState(): Flow<OrcidSyncState> = _syncState.asStateFlow().onStart {
        try {
            _syncState.value = api.getMySyncState().toDomain()
        } catch (e: Exception) {
            // Keep the last known state; the screen still renders a sync button.
        }
    }

    override suspend fun syncPublications(force: Boolean): OrcidSyncResult {
        _syncState.value = _syncState.value.copy(status = SyncStatus.RUNNING, lastError = "")
        return try {
            val result = api.syncMyPublications(force)
            refreshState()
            OrcidSyncResult(
                success = true,
                worksSynced = result.worksSynced,
                totalAvailable = result.totalAvailable,
                complete = result.complete,
                message = result.message,
            )
        } catch (e: Exception) {
            val message = e.serverDetail() ?: "Could not sync publications. Check your connection."
            _syncState.value = _syncState.value.copy(status = SyncStatus.FAILED, lastError = message)
            OrcidSyncResult(success = false, message = message)
        }
    }

    private suspend fun refreshState() {
        runCatching { api.getMySyncState().toDomain() }.onSuccess { _syncState.value = it }
    }
}

/**
 * Pulls the `detail` string out of a FastAPI error body.
 *
 * The sync endpoint rejects with actionable 400s — no ORCID on the profile, a
 * malformed iD, an active cooldown — and those messages are worth showing
 * verbatim rather than replacing with a generic failure.
 */
private fun Exception.serverDetail(): String? {
    val http = this as? retrofit2.HttpException ?: return null
    val body = runCatching { http.response()?.errorBody()?.string() }.getOrNull() ?: return null
    return runCatching {
        Json { ignoreUnknownKeys = true }
            .parseToJsonElement(body)
            .let { it as? kotlinx.serialization.json.JsonObject }
            ?.get("detail")
            ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
    }.getOrNull()
}
