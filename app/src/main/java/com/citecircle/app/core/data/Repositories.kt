package com.citecircle.app.core.data

import com.citecircle.app.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import javax.inject.Inject

// ──────────────────────────────────────────────────────────────────────────────
// Repository interfaces
// ──────────────────────────────────────────────────────────────────────────────

interface PostRepository {
    fun getFeedPosts(): Flow<List<Post>>
    fun getPostById(id: String): Flow<Post?>
    fun getPostsForCircle(circleId: String): Flow<List<Post>>
    suspend fun endorsePost(postId: String): Boolean
    suspend fun savePost(postId: String): Boolean
    suspend fun createPost(post: Post): Boolean
}

interface PaperRepository {
    fun getAllPapers(): Flow<List<Paper>>
    fun getPaperById(id: String): Flow<Paper?>
    fun getPapersForUser(userId: String): Flow<List<Paper>>
    fun getPapersForCircle(circleId: String): Flow<List<Paper>>
    suspend fun publishPaper(draft: PaperDraft): Paper
}

interface CircleRepository {
    fun getAllCircles(): Flow<List<Circle>>
    fun getJoinedCircles(): Flow<List<Circle>>
    fun getCircleById(id: String): Flow<Circle?>
    suspend fun joinCircle(circleId: String): Boolean
    suspend fun leaveCircle(circleId: String): Boolean
}

interface UserRepository {
    fun getCurrentUser(): Flow<User>
    fun getUserById(id: String): Flow<User?>
    fun getAllUsers(): Flow<List<User>>
    fun getSuggestedConnections(): Flow<List<User>>
    fun getConnectionRequests(): Flow<List<User>>
    suspend fun followUser(userId: String): Boolean
    suspend fun connectUser(userId: String): Boolean
    suspend fun acceptConnection(userId: String): Boolean
    suspend fun declineConnection(userId: String): Boolean
    suspend fun updateCurrentUser(user: User): Boolean
}

interface CommentRepository {
    fun getCommentsForPost(postId: String): Flow<List<Comment>>
    suspend fun addComment(postId: String, content: String, parentId: String?): Comment
}

interface MessageRepository {
    fun getConversations(): Flow<List<Conversation>>
    fun getMessagesForConversation(convId: String): Flow<List<Message>>
    suspend fun sendMessage(convId: String, content: String): Message
    suspend fun searchUsers(query: String): List<User>
}

interface NotificationRepository {
    fun getNotifications(): Flow<List<Notification>>
    suspend fun markAsRead(notifId: String)
    suspend fun dismissNotification(notifId: String)
}

interface AiReviewRepository {
    /** Returns staged progress messages, then the final report */
    suspend fun reviewPaper(draft: PaperDraft): AiReviewReport
    fun getReviewProgress(): Flow<AiReviewStage>
}

interface SearchRepository {
    suspend fun search(query: String): SearchResults
    fun getRecentSearches(): Flow<List<String>>
    suspend fun addRecentSearch(query: String)
    suspend fun removeRecentSearch(query: String)
}

interface AuthRepository {
    fun isLoggedIn(): Boolean
    suspend fun login(email: String, password: String): Boolean
    suspend fun signup(email: String, password: String, role: UserRole): Boolean
    suspend fun logout()
}

// ──────────────────────────────────────────────────────────────────────────────
// AI Review stage model
// ──────────────────────────────────────────────────────────────────────────────

sealed class AiReviewStage {
    data object Idle : AiReviewStage()
    data class InProgress(val message: String, val step: Int, val totalSteps: Int) : AiReviewStage()
    data class Complete(val report: AiReviewReport) : AiReviewStage()
    data class Error(val message: String) : AiReviewStage()
}

// ──────────────────────────────────────────────────────────────────────────────
// Fake implementations
// ──────────────────────────────────────────────────────────────────────────────

class FakePostRepository @Inject constructor() : PostRepository {
    private val _posts = MutableStateFlow(FakeDataSource.posts.toMutableList())

    override fun getFeedPosts(): Flow<List<Post>> = _posts.asStateFlow()

    override fun getPostById(id: String): Flow<Post?> = flow {
        emit(_posts.value.find { it.id == id })
    }

    override fun getPostsForCircle(circleId: String): Flow<List<Post>> = flow {
        emit(_posts.value.filter { it.circleId == circleId })
    }

    override suspend fun endorsePost(postId: String): Boolean {
        val current = _posts.value.toMutableList()
        val idx = current.indexOfFirst { it.id == postId }
        if (idx < 0) return false
        val post = current[idx]
        current[idx] = post.copy(
            isEndorsed = !post.isEndorsed,
            endorseCount = if (post.isEndorsed) post.endorseCount - 1 else post.endorseCount + 1
        )
        _posts.value = current
        return true
    }

    override suspend fun savePost(postId: String): Boolean {
        val current = _posts.value.toMutableList()
        val idx = current.indexOfFirst { it.id == postId }
        if (idx < 0) return false
        val post = current[idx]
        current[idx] = post.copy(isSaved = !post.isSaved)
        _posts.value = current
        return true
    }

    override suspend fun createPost(post: Post): Boolean {
        val current = _posts.value.toMutableList()
        current.add(0, post)
        _posts.value = current
        return true
    }
}

class FakePaperRepository @Inject constructor() : PaperRepository {
    private val _papers = MutableStateFlow(FakeDataSource.papers)

    override fun getAllPapers(): Flow<List<Paper>> = _papers.asStateFlow()

    override fun getPaperById(id: String): Flow<Paper?> = flow {
        emit(_papers.value.find { it.id == id })
    }

    override fun getPapersForUser(userId: String): Flow<List<Paper>> = flow {
        emit(_papers.value.filter { p -> p.authors.any { it.id == userId } })
    }

    override fun getPapersForCircle(circleId: String): Flow<List<Paper>> = flow {
        emit(_papers.value.filter { it.circleId == circleId })
    }

    override suspend fun publishPaper(draft: PaperDraft): Paper {
        val paper = Paper(
            id = "p${System.currentTimeMillis()}",
            title = draft.title,
            authors = listOf(FakeDataSource.currentUser) + draft.coAuthors,
            abstract = draft.abstract,
            fieldTags = draft.fieldTags,
            year = 2024,
            doi = "10.48550/fake.${System.currentTimeMillis()}",
            isPublished = true
        )
        val current = _papers.value.toMutableList()
        current.add(0, paper)
        _papers.value = current
        return paper
    }
}

class FakeCircleRepository @Inject constructor() : CircleRepository {
    private val _circles = MutableStateFlow(FakeDataSource.circles.toMutableList())

    override fun getAllCircles(): Flow<List<Circle>> = _circles.asStateFlow()

    override fun getJoinedCircles(): Flow<List<Circle>> = flow {
        emit(_circles.value.filter { it.isJoined })
    }

    override fun getCircleById(id: String): Flow<Circle?> = flow {
        emit(_circles.value.find { it.id == id })
    }

    override suspend fun joinCircle(circleId: String): Boolean {
        val current = _circles.value.toMutableList()
        val idx = current.indexOfFirst { it.id == circleId }
        if (idx < 0) return false
        current[idx] = current[idx].copy(isJoined = true, memberCount = current[idx].memberCount + 1)
        _circles.value = current
        return true
    }

    override suspend fun leaveCircle(circleId: String): Boolean {
        val current = _circles.value.toMutableList()
        val idx = current.indexOfFirst { it.id == circleId }
        if (idx < 0) return false
        current[idx] = current[idx].copy(isJoined = false, memberCount = current[idx].memberCount - 1)
        _circles.value = current
        return true
    }
}

class FakeUserRepository @Inject constructor() : UserRepository {
    private val _currentUser = MutableStateFlow(FakeDataSource.currentUser)
    private val _users = MutableStateFlow(FakeDataSource.users.toMutableList())
    private val _requests = MutableStateFlow(FakeDataSource.connectionRequests.toMutableList())

    override fun getCurrentUser(): Flow<User> = _currentUser.asStateFlow()
    override fun getUserById(id: String): Flow<User?> = flow {
        if (id == _currentUser.value.id) emit(_currentUser.value)
        else emit(_users.value.find { it.id == id })
    }
    override fun getAllUsers(): Flow<List<User>> = _users.asStateFlow()
    override fun getSuggestedConnections(): Flow<List<User>> = flow {
        emit(_users.value.filter { !it.isConnected && !it.connectionPending }.take(8))
    }
    override fun getConnectionRequests(): Flow<List<User>> = _requests.asStateFlow()

    override suspend fun followUser(userId: String): Boolean {
        val idx = _users.value.indexOfFirst { it.id == userId }
        if (idx < 0) return false
        val current = _users.value.toMutableList()
        current[idx] = current[idx].copy(isFollowing = !current[idx].isFollowing)
        _users.value = current
        return true
    }

    override suspend fun connectUser(userId: String): Boolean {
        val idx = _users.value.indexOfFirst { it.id == userId }
        if (idx < 0) return false
        val current = _users.value.toMutableList()
        current[idx] = current[idx].copy(connectionPending = true)
        _users.value = current
        return true
    }

    override suspend fun acceptConnection(userId: String): Boolean {
        val reqCurrent = _requests.value.toMutableList()
        reqCurrent.removeAll { it.id == userId }
        _requests.value = reqCurrent
        val idx = _users.value.indexOfFirst { it.id == userId }
        if (idx >= 0) {
            val current = _users.value.toMutableList()
            current[idx] = current[idx].copy(isConnected = true, connectionPending = false)
            _users.value = current
        }
        return true
    }

    override suspend fun declineConnection(userId: String): Boolean {
        val reqCurrent = _requests.value.toMutableList()
        reqCurrent.removeAll { it.id == userId }
        _requests.value = reqCurrent
        return true
    }

    override suspend fun updateCurrentUser(user: User): Boolean {
        FakeDataSource.currentUser = user
        _currentUser.value = user
        return true
    }
}

class FakeCommentRepository @Inject constructor() : CommentRepository {
    private val _commentsByPost = mutableMapOf<String, MutableStateFlow<List<Comment>>>()

    private fun getFlow(postId: String): MutableStateFlow<List<Comment>> {
        return _commentsByPost.getOrPut(postId) {
            MutableStateFlow(FakeDataSource.getCommentsForPost(postId))
        }
    }

    override fun getCommentsForPost(postId: String): Flow<List<Comment>> = getFlow(postId).asStateFlow()

    override suspend fun addComment(postId: String, content: String, parentId: String?): Comment {
        val newComment = Comment(
            id = "cm${System.currentTimeMillis()}",
            author = FakeDataSource.currentUser,
            content = content,
            timestamp = System.currentTimeMillis(),
            parentId = parentId
        )
        val flow = getFlow(postId)
        flow.value = if (parentId == null) {
            listOf(newComment) + flow.value
        } else {
            flow.value.map { comment ->
                if (comment.id == parentId) {
                    comment.copy(replies = comment.replies + newComment, replyCount = comment.replyCount + 1)
                } else comment
            }
        }
        return newComment
    }
}

class FakeMessageRepository @Inject constructor() : MessageRepository {
    private val _conversations = MutableStateFlow(FakeDataSource.conversations)
    private val _messagesByConv = mutableMapOf<String, MutableStateFlow<List<Message>>>()

    private fun getMsgFlow(convId: String): MutableStateFlow<List<Message>> {
        return _messagesByConv.getOrPut(convId) {
            MutableStateFlow(FakeDataSource.getMessagesForConversation(convId))
        }
    }

    override fun getConversations(): Flow<List<Conversation>> = _conversations.asStateFlow()

    override fun getMessagesForConversation(convId: String): Flow<List<Message>> = getMsgFlow(convId).asStateFlow()

    override suspend fun sendMessage(convId: String, content: String): Message {
        val msg = Message(
            id = "msg${System.currentTimeMillis()}",
            senderId = FakeDataSource.currentUser.id,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        val flow = getMsgFlow(convId)
        flow.value = flow.value + msg

        // Update conversation last message
        val current = _conversations.value.toMutableList()
        val idx = current.indexOfFirst { it.id == convId }
        if (idx >= 0) {
            current[idx] = current[idx].copy(lastMessage = msg, unreadCount = 0)
            _conversations.value = current
        }
        return msg
    }

    override suspend fun searchUsers(query: String): List<User> {
        return FakeDataSource.users.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.institution.contains(query, ignoreCase = true)
        }
    }
}

class FakeNotificationRepository @Inject constructor() : NotificationRepository {
    private val _notifications = MutableStateFlow(FakeDataSource.notifications.toMutableList())

    override fun getNotifications(): Flow<List<Notification>> = _notifications.asStateFlow()

    override suspend fun markAsRead(notifId: String) {
        val current = _notifications.value.toMutableList()
        val idx = current.indexOfFirst { it.id == notifId }
        if (idx >= 0) {
            current[idx] = current[idx].copy(isRead = true)
            _notifications.value = current
        }
    }

    override suspend fun dismissNotification(notifId: String) {
        _notifications.value = _notifications.value.filter { it.id != notifId }.toMutableList()
    }
}

class FakeAiReviewRepository @Inject constructor() : AiReviewRepository {
    private val _progress = MutableStateFlow<AiReviewStage>(AiReviewStage.Idle)

    override fun getReviewProgress(): Flow<AiReviewStage> = _progress.asStateFlow()

    override suspend fun reviewPaper(draft: PaperDraft): AiReviewReport {
        val stages = listOf(
            "Reading your manuscript…",
            "Checking document structure…",
            "Scanning citation completeness…",
            "Evaluating methodological clarity…",
            "Assessing originality signals…",
            "Synthesizing review report…"
        )
        stages.forEachIndexed { index, message ->
            _progress.value = AiReviewStage.InProgress(message, index + 1, stages.size)
            delay(600L)
        }
        val report = FakeDataSource.sampleAiReport
        _progress.value = AiReviewStage.Complete(report)
        return report
    }
}

class FakeSearchRepository @Inject constructor() : SearchRepository {
    private val _recentSearches = MutableStateFlow<List<String>>(
        listOf("causal inference LLMs", "genomics population", "quantum error correction")
    )

    override suspend fun search(query: String): SearchResults {
        if (query.isBlank()) return SearchResults()
        val q = query.lowercase()
        return SearchResults(
            people = FakeDataSource.getAllUsers().filter {
                it.name.lowercase().contains(q) ||
                        it.fieldOfStudy.lowercase().contains(q) ||
                        it.institution.lowercase().contains(q)
            },
            papers = FakeDataSource.papers.filter {
                it.title.lowercase().contains(q) ||
                        it.abstract.lowercase().contains(q) ||
                        it.fieldTags.any { tag -> tag.lowercase().contains(q) }
            },
            circles = FakeDataSource.circles.filter {
                it.name.lowercase().contains(q) ||
                        it.description.lowercase().contains(q) ||
                        it.category.lowercase().contains(q)
            },
            posts = FakeDataSource.posts.filter {
                it.content.lowercase().contains(q)
            }
        )
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

class FakeAuthRepository @Inject constructor(
    private val userRepository: UserRepository
) : AuthRepository {
    private var _loggedIn = false

    override fun isLoggedIn(): Boolean = _loggedIn

    override suspend fun login(email: String, password: String): Boolean {
        delay(800)
        _loggedIn = true
        val loggedInUser = when {
            email.contains("admin", ignoreCase = true) -> FakeDataSource.superAdminUser
            email.contains("dummy", ignoreCase = true) -> FakeDataSource.dummyUser
            else -> FakeDataSource.defaultUser
        }
        userRepository.updateCurrentUser(loggedInUser)
        return true
    }

    override suspend fun signup(email: String, password: String, role: UserRole): Boolean {
        delay(1000)
        _loggedIn = true
        val nameFromEmail = email.substringBefore("@").split(".").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        val newUser = User(
            id = "u_${System.currentTimeMillis()}",
            name = nameFromEmail,
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=$nameFromEmail",
            role = role,
            institution = "CiteCircle Affiliate",
            interests = emptyList()
        )
        userRepository.updateCurrentUser(newUser)
        return true
    }

    override suspend fun logout() {
        _loggedIn = false
    }
}
