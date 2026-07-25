package com.citecircle.app.core.data

import com.citecircle.app.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.citecircle.app.core.network.FireworksApi
import com.citecircle.app.core.network.ChatCompletionRequest
import com.citecircle.app.core.network.ChatMessage
import javax.inject.Inject
import kotlinx.coroutines.flow.combine

// ──────────────────────────────────────────────────────────────────────────────
// Repository interfaces
// ──────────────────────────────────────────────────────────────────────────────

interface PostRepository {
    fun getFeedPosts(): Flow<List<Post>>
    fun getSavedPosts(): Flow<List<Post>>
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
    suspend fun citePaper(paperId: String): Paper
    suspend fun getPaperSummary(paperId: String, title: String, abstract: String): String
    fun getSavedPapers(): Flow<List<Paper>>
    fun getShelves(): Flow<List<Shelf>>
    suspend fun createShelf(name: String, description: String): Boolean
    suspend fun addPaperToShelf(paperId: String, shelfId: String): Boolean
    suspend fun removePaperFromShelf(paperId: String, shelfId: String): Boolean
    suspend fun toggleSavePaper(paperId: String): Boolean
    fun isPaperSaved(paperId: String): Flow<Boolean>
    fun getAnnotations(paperId: String): Flow<List<PaperAnnotation>>
    suspend fun saveAnnotation(annotation: PaperAnnotation): PaperAnnotation
    suspend fun deleteAnnotation(paperId: String, annotationId: String): Boolean
    suspend fun getAiPaperBreakdown(paperId: String): AiPaperBreakdown
    fun getCitationGraph(paperId: String, depth: Int = 2, minCitations: Int = 0): Flow<CitationGraphResponse>
}


interface CircleRepository {
    fun getAllCircles(): Flow<List<Circle>>
    fun getJoinedCircles(): Flow<List<Circle>>
    fun getCircleById(id: String): Flow<Circle?>
    suspend fun joinCircle(circleId: String): Boolean
    suspend fun leaveCircle(circleId: String): Boolean

    // Workspace & Collaborative Research
    fun getCircleWorkspace(circleId: String): Flow<CircleWorkspace?>
    suspend fun updatePinboard(circleId: String, text: String): Boolean
    fun getCircleMembers(circleId: String): Flow<List<CircleMember>>
    suspend fun updateMemberRole(circleId: String, userId: String, role: CircleRole): Boolean
    suspend fun generateInviteCode(circleId: String): String
    fun getCircleDrafts(circleId: String): Flow<List<CircleDraft>>
    fun getDraftDetails(draftId: String): Flow<CircleDraft?>
    suspend fun createCircleDraft(circleId: String, title: String, abstract: String, fileFormat: DraftFormat): CircleDraft
    fun getDraftReviewRequests(draftId: String): Flow<List<DraftReviewRequest>>
    suspend fun requestDraftReview(draftId: String, reviewerId: String, sectionTarget: String, notes: String): DraftReviewRequest
    fun getDraftComments(draftId: String): Flow<List<DraftComment>>
    suspend fun addDraftComment(draftId: String, sectionIndex: Int, paragraphOffset: Int, content: String): DraftComment
    suspend fun resolveDraftComment(commentId: String): Boolean
    fun getCircleReadingLists(circleId: String): Flow<List<CircleReadingList>>
    suspend fun createCircleReadingList(circleId: String, title: String, description: String, paperIds: List<String>): CircleReadingList
    suspend fun saveReadingListToMyLibrary(readingListId: String): Boolean
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
    fun getCoauthorGraph(userId: String): Flow<CoauthorGraphResponse>
}


interface CommentRepository {
    fun getCommentsForPost(postId: String): Flow<List<Comment>>
    suspend fun addComment(postId: String, content: String, parentId: String?): Comment
}

interface MessageRepository {
    fun getConversations(): Flow<List<Conversation>>
    fun getMessagesForConversation(convId: String): Flow<List<Message>>
    suspend fun sendMessage(convId: String, content: String): Message
    suspend fun clearChatHistory(convId: String): Boolean
    suspend fun searchUsers(query: String): List<User>
}

interface NotificationRepository {
    fun getNotifications(): Flow<List<Notification>>
    suspend fun markAsRead(notifId: String)
    suspend fun dismissNotification(notifId: String)
}

interface PublicationRepository {
    /** Publications for a user; pass null for the signed-in user. */
    fun getPublications(userId: String? = null): Flow<List<Publication>>
    fun getCoauthors(userId: String? = null): Flow<List<Coauthor>>
    fun getSyncState(): Flow<OrcidSyncState>

    /**
     * Pull the signed-in user's bibliography from OpenAlex via their ORCID iD.
     * Set [force] to bypass the server-side refresh cooldown.
     */
    suspend fun syncPublications(force: Boolean = false): OrcidSyncResult
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
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit>
    suspend fun changeEmail(newEmail: String, currentPassword: String): Result<Unit>
    fun getSavedAccounts(): Flow<List<SavedAccount>>
    suspend fun switchAccount(userId: String): Boolean
    suspend fun addAccount(email: String, password: String): Result<Boolean>
    suspend fun removeAccount(userId: String): Boolean
    suspend fun clearCache(): Boolean
    suspend fun exportUserData(): Result<String>
    suspend fun logoutAll()
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

    override fun getSavedPosts(): Flow<List<Post>> = flow {
        emit(_posts.value.filter { it.isSaved })
    }

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

class FakePaperRepository @Inject constructor(
    private val api: FireworksApi
) : PaperRepository {
    private val _papers = MutableStateFlow(FakeDataSource.papers)
    private val _summaryCache = mutableMapOf<String, String>()
    private val _savedPaperIds = MutableStateFlow(mutableSetOf<String>())
    private val _shelves = MutableStateFlow<List<Shelf>>(emptyList())

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

    override suspend fun citePaper(paperId: String): Paper {
        val current = _papers.value.toMutableList()
        val idx = current.indexOfFirst { it.id == paperId }
        if (idx >= 0) {
            val updated = current[idx].copy(citationCount = current[idx].citationCount + 1)
            current[idx] = updated
            _papers.value = current
            return updated
        }
        val paper = Paper(id = paperId, title = "Cited Paper", citationCount = 1)
        return paper
    }

    override suspend fun getPaperSummary(paperId: String, title: String, abstract: String): String {
        // Return cached result immediately
        _summaryCache[paperId]?.let { return it }

        return try {
            val systemPrompt = """
                You are a scientific summarization assistant. Given a research paper title and abstract,
                produce exactly 3 concise bullet points summarizing the paper.
                Format your response as exactly 3 lines, each starting with "• ".
                Cover: (1) the key contribution or goal, (2) the main method or finding,
                (3) the primary implication or takeaway.
                Keep each bullet to 1–2 sentences. Return ONLY the 3 bullet points — no preamble, no extra text.
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

            val response = api.chatCompletions(request)
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

    /** Extracts 3 key sentences from the abstract when the AI call is unavailable. */
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

    override fun getSavedPapers(): Flow<List<Paper>> = combine(_papers, _savedPaperIds) { papers, savedIds ->
        papers.filter { savedIds.contains(it.id) }
    }

    override fun getShelves(): Flow<List<Shelf>> = _shelves.asStateFlow()

    override suspend fun createShelf(name: String, description: String): Boolean {
        val current = _shelves.value.toMutableList()
        val newShelf = Shelf(
            id = "s${System.currentTimeMillis()}",
            name = name,
            description = description,
            paperIds = emptyList()
        )
        current.add(newShelf)
        _shelves.value = current
        return true
    }

    override suspend fun addPaperToShelf(paperId: String, shelfId: String): Boolean {
        val current = _shelves.value.toMutableList()
        val idx = current.indexOfFirst { it.id == shelfId }
        if (idx < 0) return false
        val shelf = current[idx]
        if (!shelf.paperIds.contains(paperId)) {
            val updatedPaperIds = shelf.paperIds.toMutableList().apply { add(paperId) }
            current[idx] = shelf.copy(paperIds = updatedPaperIds)
            _shelves.value = current
        }
        // Also auto-save the paper if added to a shelf
        val saved = _savedPaperIds.value.toMutableSet()
        if (saved.add(paperId)) {
            _savedPaperIds.value = saved
        }
        return true
    }

    override suspend fun removePaperFromShelf(paperId: String, shelfId: String): Boolean {
        val current = _shelves.value.toMutableList()
        val idx = current.indexOfFirst { it.id == shelfId }
        if (idx < 0) return false
        val shelf = current[idx]
        if (shelf.paperIds.contains(paperId)) {
            val updatedPaperIds = shelf.paperIds.toMutableList().apply { remove(paperId) }
            current[idx] = shelf.copy(paperIds = updatedPaperIds)
            _shelves.value = current
        }
        return true
    }

    override suspend fun toggleSavePaper(paperId: String): Boolean {
        val currentSaved = _savedPaperIds.value.toMutableSet()
        val wasSaved = currentSaved.contains(paperId)
        if (wasSaved) {
            currentSaved.remove(paperId)
            // Also clean up from all shelves if unsaved from general library
            val currentShelves = _shelves.value.toMutableList()
            for (i in currentShelves.indices) {
                val shelf = currentShelves[i]
                if (shelf.paperIds.contains(paperId)) {
                    currentShelves[i] = shelf.copy(paperIds = shelf.paperIds.toMutableList().apply { remove(paperId) })
                }
            }
            _shelves.value = currentShelves
        } else {
            currentSaved.add(paperId)
        }
        _savedPaperIds.value = currentSaved
        return true
    }

    private val _annotationsMap = MutableStateFlow<Map<String, List<PaperAnnotation>>>(emptyMap())

    override fun isPaperSaved(paperId: String): Flow<Boolean> = flow {
        _savedPaperIds.collect { emit(it.contains(paperId)) }
    }

    override fun getAnnotations(paperId: String): Flow<List<PaperAnnotation>> = flow {
        _annotationsMap.collect { map ->
            emit(map[paperId] ?: emptyList())
        }
    }

    override suspend fun saveAnnotation(annotation: PaperAnnotation): PaperAnnotation {
        val currentMap = _annotationsMap.value.toMutableMap()
        val list = (currentMap[annotation.paperId] ?: emptyList()).toMutableList()
        val existingIdx = list.indexOfFirst { it.id == annotation.id }
        if (existingIdx >= 0) {
            list[existingIdx] = annotation
        } else {
            list.add(annotation)
        }
        currentMap[annotation.paperId] = list
        _annotationsMap.value = currentMap
        return annotation
    }

    override suspend fun deleteAnnotation(paperId: String, annotationId: String): Boolean {
        val currentMap = _annotationsMap.value.toMutableMap()
        val list = (currentMap[paperId] ?: emptyList()).filter { it.id != annotationId }
        currentMap[paperId] = list
        _annotationsMap.value = currentMap
        return true
    }

    override suspend fun getAiPaperBreakdown(paperId: String): AiPaperBreakdown {
        val paper = _papers.value.find { it.id == paperId }
        val title = paper?.title ?: "Selected Research Paper"
        val abstract = paper?.abstract ?: ""

        return AiPaperBreakdown(
            paperId = paperId,
            abstractTldr = "Executive Summary: $title introduces an empirical evaluation model and scalable architectural pattern.",
            methodologySetup = "Experimental dataset benchmarked against baseline models with 5-fold cross-validation.",
            coreResults = "Achieves statistical significance with lower latency and improved accuracy metrics.",
            limitationsFutureWork = "Constrained by memory overhead during peak batch ingestion; planned optimization includes quantized models.",
            keyTakeaways = listOf(
                "Provides robust benchmarks across standardized test scenarios.",
                "Reduces inference complexity while preserving accuracy.",
                "Establishes a foundational paradigm for future iterations."
            ),
            methodologyQualityIndex = 91,
            qualityLabel = "Very High Methodological Rigor"
        )
    }

    override fun getCitationGraph(paperId: String, depth: Int, minCitations: Int): Flow<CitationGraphResponse> = flow {
        val center = _papers.value.find { it.id == paperId } ?: _papers.value.firstOrNull() ?: Paper(id = paperId, title = "Research Paper")
        val otherPapers = _papers.value.filter { it.id != center.id }.take(6)
        
        val nodes = mutableListOf<CitationGraphNode>()
        nodes.add(
            CitationGraphNode(
                id = center.id,
                title = center.title,
                abstract = center.abstract,
                citationCount = center.citationCount,
                year = center.year,
                circleId = center.circleId,
                field = "Computer Science",
                authors = center.authors,
                doi = center.doi,
                journal = center.journal,
                isCenter = true,
                hopDistance = 0,
                x = 0f,
                y = 0f
            )
        )

        val edges = mutableListOf<CitationGraphEdge>()
        otherPapers.forEachIndexed { i, p ->
            val angle = (i * 2 * Math.PI / otherPapers.size) - (Math.PI / 2)
            val radius = if (i % 2 == 0) 180f else 320f
            val hop = if (i % 2 == 0) 1 else 2
            nodes.add(
                CitationGraphNode(
                    id = p.id,
                    title = p.title,
                    abstract = p.abstract,
                    citationCount = p.citationCount,
                    year = p.year,
                    circleId = p.circleId,
                    field = if (i % 2 == 0) "Artificial Intelligence" else "HCI",
                    authors = p.authors,
                    doi = p.doi,
                    journal = p.journal,
                    isCenter = false,
                    hopDistance = hop,
                    x = (radius * Math.cos(angle)).toFloat(),
                    y = (radius * Math.sin(angle)).toFloat()
                )
            )
            edges.add(CitationGraphEdge(source = center.id, target = p.id, type = "CITES"))
        }

        emit(CitationGraphResponse(nodes = nodes, edges = edges, summary = CitationGraphSummary(nodes.size, nodes.sumOf { it.citationCount }, depth)))
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

    private val _workspaceState = MutableStateFlow(FakeDataSource.sampleWorkspace)
    private val _membersState = MutableStateFlow(FakeDataSource.sampleMembers)
    private val _draftsState = MutableStateFlow(FakeDataSource.sampleDrafts)
    private val _reviewRequestsState = MutableStateFlow(FakeDataSource.sampleReviewRequests)
    private val _commentsState = MutableStateFlow(FakeDataSource.sampleDraftComments)
    private val _readingListsState = MutableStateFlow(FakeDataSource.sampleReadingLists)

    override fun getCircleWorkspace(circleId: String): Flow<CircleWorkspace?> = flow {
        emit(_workspaceState.value.copy(circleId = circleId))
    }

    override suspend fun updatePinboard(circleId: String, text: String): Boolean {
        _workspaceState.value = _workspaceState.value.copy(pinBoardText = text, updatedAt = System.currentTimeMillis())
        return true
    }

    override fun getCircleMembers(circleId: String): Flow<List<CircleMember>> = _membersState.asStateFlow()

    override suspend fun updateMemberRole(circleId: String, userId: String, role: CircleRole): Boolean {
        val current = _membersState.value.toMutableList()
        val idx = current.indexOfFirst { it.userId == userId }
        if (idx >= 0) {
            current[idx] = current[idx].copy(role = role)
            _membersState.value = current
        }
        return true
    }

    override suspend fun generateInviteCode(circleId: String): String {
        val code = "INV-${circleId.take(4).uppercase()}-${System.currentTimeMillis() % 10000}"
        _workspaceState.value = _workspaceState.value.copy(inviteCode = code)
        return code
    }

    override fun getCircleDrafts(circleId: String): Flow<List<CircleDraft>> = flow {
        emit(_draftsState.value.filter { it.circleId == circleId || circleId == "cir_hci" })
    }

    override fun getDraftDetails(draftId: String): Flow<CircleDraft?> = flow {
        emit(_draftsState.value.find { it.id == draftId })
    }

    override suspend fun createCircleDraft(
        circleId: String,
        title: String,
        abstract: String,
        fileFormat: DraftFormat
    ): CircleDraft {
        val newDraft = CircleDraft(
            id = "drf_${System.currentTimeMillis()}",
            circleId = circleId,
            title = title,
            abstract = abstract,
            leadAuthorId = FakeDataSource.currentUser.id,
            leadAuthorName = FakeDataSource.currentUser.name.ifBlank { "Scholar" },
            leadAuthorAvatar = FakeDataSource.currentUser.avatarUrl,
            fileFormat = fileFormat,
            status = DraftStatus.DRAFT,
            version = "v1.0",
            sections = listOf(
                "Abstract",
                "1. Introduction & Background",
                "2. System Architecture & Methodology",
                "3. Experimental Evaluation",
                "4. Discussion & Conclusion"
            )
        )
        val current = _draftsState.value.toMutableList()
        current.add(0, newDraft)
        _draftsState.value = current
        return newDraft
    }

    override fun getDraftReviewRequests(draftId: String): Flow<List<DraftReviewRequest>> = flow {
        emit(_reviewRequestsState.value.filter { it.draftId == draftId })
    }

    override suspend fun requestDraftReview(
        draftId: String,
        reviewerId: String,
        sectionTarget: String,
        notes: String
    ): DraftReviewRequest {
        val req = DraftReviewRequest(
            id = "rr_${System.currentTimeMillis()}",
            draftId = draftId,
            reviewerId = reviewerId,
            reviewerName = "Reviewer",
            reviewerAvatar = "",
            requesterId = FakeDataSource.currentUser.id,
            sectionTarget = sectionTarget,
            notes = notes,
            status = ReviewRequestStatus.PENDING
        )
        val current = _reviewRequestsState.value.toMutableList()
        current.add(0, req)
        _reviewRequestsState.value = current
        return req
    }

    override fun getDraftComments(draftId: String): Flow<List<DraftComment>> = flow {
        emit(_commentsState.value.filter { it.draftId == draftId })
    }

    override suspend fun addDraftComment(
        draftId: String,
        sectionIndex: Int,
        paragraphOffset: Int,
        content: String
    ): DraftComment {
        val comment = DraftComment(
            id = "dc_${System.currentTimeMillis()}",
            draftId = draftId,
            authorId = FakeDataSource.currentUser.id,
            authorName = FakeDataSource.currentUser.name.ifBlank { "Scholar" },
            authorAvatarUrl = FakeDataSource.currentUser.avatarUrl,
            sectionIndex = sectionIndex,
            paragraphOffset = paragraphOffset,
            content = content,
            isResolved = false
        )
        val current = _commentsState.value.toMutableList()
        current.add(comment)
        _commentsState.value = current
        return comment
    }

    override suspend fun resolveDraftComment(commentId: String): Boolean {
        val current = _commentsState.value.toMutableList()
        val idx = current.indexOfFirst { it.id == commentId }
        if (idx >= 0) {
            current[idx] = current[idx].copy(isResolved = true)
            _commentsState.value = current
        }
        return true
    }

    override fun getCircleReadingLists(circleId: String): Flow<List<CircleReadingList>> = flow {
        emit(_readingListsState.value.filter { it.circleId == circleId || circleId == "cir_hci" })
    }

    override suspend fun createCircleReadingList(
        circleId: String,
        title: String,
        description: String,
        paperIds: List<String>
    ): CircleReadingList {
        val list = CircleReadingList(
            id = "rl_${System.currentTimeMillis()}",
            circleId = circleId,
            title = title,
            description = description,
            createdById = FakeDataSource.currentUser.id,
            createdByName = FakeDataSource.currentUser.name.ifBlank { "Scholar" },
            paperCount = paperIds.size,
            papers = emptyList()
        )
        val current = _readingListsState.value.toMutableList()
        current.add(0, list)
        _readingListsState.value = current
        return list
    }

    override suspend fun saveReadingListToMyLibrary(readingListId: String): Boolean {
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

    override fun getCoauthorGraph(userId: String): Flow<CoauthorGraphResponse> = flow {
        val center = _users.value.find { it.id == userId } ?: _users.value.firstOrNull() ?: User(id = userId, name = "Researcher")
        val collaborators = _users.value.filter { it.id != center.id }.take(5)

        val nodes = mutableListOf<CoauthorGraphNode>()
        nodes.add(
            CoauthorGraphNode(
                id = center.id,
                name = center.name,
                avatarUrl = center.avatarUrl,
                institution = center.institution,
                fieldOfStudy = center.fieldOfStudy,
                citationCount = center.citationCount,
                hIndex = 14,
                i10Index = 22,
                clusterId = "cluster_main",
                isCenter = true,
                x = 0f,
                y = 0f
            )
        )

        val edges = mutableListOf<CoauthorGraphEdge>()
        collaborators.forEachIndexed { i, u ->
            val angle = (i * 2 * Math.PI / collaborators.size) - (Math.PI / 2)
            val radius = 220f
            nodes.add(
                CoauthorGraphNode(
                    id = u.id,
                    name = u.name,
                    avatarUrl = u.avatarUrl,
                    institution = u.institution,
                    fieldOfStudy = u.fieldOfStudy,
                    citationCount = u.citationCount,
                    hIndex = 8 + i,
                    i10Index = 12 + i,
                    clusterId = if (i % 2 == 0) "cluster_mit" else "cluster_eth",
                    isCenter = false,
                    x = (radius * Math.cos(angle)).toFloat(),
                    y = (radius * Math.sin(angle)).toFloat()
                )
            )
            edges.add(CoauthorGraphEdge(source = center.id, target = u.id, weight = 2 + i, publications = listOf("Joint Publication Paper")))
        }

        val clusters = listOf(
            CoauthorCluster(id = "cluster_main", name = center.institution.ifBlank { "Primary Lab" }, color = "#6C63FF", memberIds = listOf(center.id)),
            CoauthorCluster(id = "cluster_mit", name = "Collaborating Institute", color = "#00B4D8", memberIds = collaborators.map { it.id })
        )

        val analytics = ResearcherAnalytics(
            totalCitations = center.citationCount,
            hIndex = 14,
            i10Index = 22,
            citationVelocity = listOf(
                CitationVelocityPoint(2020, 25),
                CitationVelocityPoint(2021, 60),
                CitationVelocityPoint(2022, 110),
                CitationVelocityPoint(2023, 190),
                CitationVelocityPoint(2024, 320)
            )
        )

        emit(CoauthorGraphResponse(nodes = nodes, edges = edges, clusters = clusters, analytics = analytics))
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

class FakeMessageRepository @Inject constructor(
    private val api: FireworksApi
) : MessageRepository {
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

        if (convId == "conv_ai") {
            CoroutineScope(Dispatchers.IO).launch {
                getAiReply(flow)
            }
        }

        return msg
    }

    override suspend fun clearChatHistory(convId: String): Boolean {
        val flow = getMsgFlow(convId)
        if (convId == "conv_ai") {
            val welcome = Message(
                id = "msg_ai_welcome_${System.currentTimeMillis()}",
                senderId = "ai_copilot",
                content = "Welcome to CiteCircle AI Copilot! Chat history has been cleared. Feel free to ask me anything about research literature, paper drafts, statistical methodology, or peer review feedback.",
                timestamp = System.currentTimeMillis()
            )
            flow.value = listOf(welcome)
        } else {
            flow.value = emptyList()
        }
        val current = _conversations.value.toMutableList()
        val idx = current.indexOfFirst { it.id == convId }
        if (idx >= 0) {
            current[idx] = current[idx].copy(lastMessage = flow.value.lastOrNull(), unreadCount = 0)
            _conversations.value = current
        }
        return true
    }


    private suspend fun getAiReply(flow: MutableStateFlow<List<Message>>) {
        try {
            val modelId = "accounts/fireworks/models/deepseek-v4-pro"
            val systemPrompt = """
                You are CiteCircle AI Copilot — a warm, encouraging, and knowledgeable academic mentor.
                Your role is to help students, scientists, and researchers understand academic topics,
                improve their writing, review literature, understand statistics, and navigate publishing.

                Respond like a great teacher or tutor would:
                • Speak in clear, natural, friendly English that any student can understand.
                • Break complex ideas into simple steps or short bullet points when helpful.
                • Use encouraging, supportive language — the user should feel guided, not lectured.
                • If giving a list, use bullet points (starting with • or -), not numbered brackets like [1] or JSON-style arrays.
                • Bold important terms using **bold** markdown.
                • Keep responses focused and concise — no unnecessary filler.

                STRICT RULES — never break these:
                • Never output raw JSON, code blocks, or brackets like { } [ ] unless the user specifically asked for code.
                • Never reveal your internal reasoning, planning steps, or meta-analysis (e.g., do NOT write lines like "Analyze the Request:", "Step 1:", "Identify the core content:", "Persona:", or "Constraints:").
                • Never wrap your reply in markdown code fences like ```text or ```markdown.
                • Start your response directly with the answer — no preamble or greeting unless the user greeted you first.
            """.trimIndent()

            val history = flow.value.takeLast(10).map { msg ->
                ChatMessage(
                    role = if (msg.senderId == FakeDataSource.currentUser.id) "user" else "assistant",
                    content = msg.content
                )
            }

            val request = ChatCompletionRequest(
                model = modelId,
                messages = listOf(ChatMessage(role = "system", content = systemPrompt)) + history,
                temperature = 0.7,
                useJsonMode = false
            )

            val response = api.chatCompletions(request)
            var replyText = response.choices.firstOrNull()?.message?.content
                ?: "I apologize, but I couldn't formulate a reply. Please try again."

            // Filter out any accidental scratchpad planning lines or meta analysis
            val cleanLines = replyText.lines().filterNot { line ->
                val lower = line.trim().lowercase()
                lower.startsWith("analyze the request") ||
                lower.startsWith("identify the core content") ||
                lower.startsWith("persona:") ||
                lower.startsWith("constraints:")
            }
            replyText = cleanLines.joinToString("\n").trim()

            if (replyText.startsWith("```markdown")) {
                replyText = replyText.removeSurrounding("```markdown", "```").trim()
            } else if (replyText.startsWith("```text")) {
                replyText = replyText.removeSurrounding("```text", "```").trim()
            } else if (replyText.startsWith("```") && replyText.endsWith("```")) {
                replyText = replyText.removeSurrounding("```", "```").trim()
            }

            val replyMsg = Message(
                id = "msg_ai_${System.currentTimeMillis()}",
                senderId = "ai_copilot",
                content = replyText,
                timestamp = System.currentTimeMillis()
            )

            flow.value = flow.value + replyMsg

            val current = _conversations.value.toMutableList()
            val idx = current.indexOfFirst { it.id == "conv_ai" }
            if (idx >= 0) {
                current[idx] = current[idx].copy(lastMessage = replyMsg, unreadCount = 0)
                _conversations.value = current
            }
        } catch (e: Exception) {
            val errorMsg = Message(
                id = "msg_ai_err_${System.currentTimeMillis()}",
                senderId = "ai_copilot",
                content = "Sorry, I had trouble connecting to the network: ${e.localizedMessage ?: "Unknown error"}. Please check your connection.",
                timestamp = System.currentTimeMillis()
            )
            flow.value = flow.value + errorMsg
        }
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
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())

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
    private val _savedAccounts = MutableStateFlow<List<SavedAccount>>(emptyList())

    override fun isLoggedIn(): Boolean = _loggedIn

    override suspend fun login(email: String, password: String): Boolean {
        delay(800)
        _loggedIn = true
        val nameFromEmail = email.substringBefore("@").split(".").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        val loggedInUser = User(
            id = "u_${kotlin.math.abs(email.hashCode())}",
            name = if (nameFromEmail.isNotBlank()) nameFromEmail else "Scholar",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=$email",
            role = UserRole.STUDENT,
            institution = "CiteCircle Affiliate"
        )
        userRepository.updateCurrentUser(loggedInUser)
        _savedAccounts.value = listOf(
            SavedAccount(
                userId = loggedInUser.id,
                email = email,
                name = loggedInUser.name,
                avatarUrl = loggedInUser.avatarUrl,
                role = loggedInUser.role.name,
                isActive = true
            )
        )
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
        _savedAccounts.value = listOf(
            SavedAccount(
                userId = newUser.id,
                email = email,
                name = newUser.name,
                avatarUrl = newUser.avatarUrl,
                role = newUser.role.name,
                isActive = true
            )
        )
        return true
    }

    override suspend fun logout() {
        val currentAccounts = _savedAccounts.value
        val activeAccount = currentAccounts.find { it.isActive }
        val remaining = currentAccounts.filter { it.userId != activeAccount?.userId }
        if (remaining.isNotEmpty()) {
            val nextActive = remaining.first().copy(isActive = true)
            _savedAccounts.value = remaining.map { if (it.userId == nextActive.userId) nextActive else it.copy(isActive = false) }
            val matchingUser = FakeDataSource.users.find { it.id == nextActive.userId }
                ?: User(id = nextActive.userId, name = nextActive.name, avatarUrl = nextActive.avatarUrl)
            userRepository.updateCurrentUser(matchingUser)
        } else {
            _savedAccounts.value = emptyList()
            _loggedIn = false
        }
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> {
        delay(600)
        if (oldPassword.isBlank()) {
            return Result.failure(Exception("Current password is required."))
        }
        if (newPassword.length < 6 || (!newPassword.any { it.isUpperCase() } && !newPassword.any { it.isDigit() })) {
            return Result.failure(Exception("Password must be >= 6 chars with at least one uppercase letter or number."))
        }
        return Result.success(Unit)
    }

    override suspend fun changeEmail(newEmail: String, currentPassword: String): Result<Unit> {
        delay(600)
        if (!newEmail.contains("@") || currentPassword.isBlank()) {
            return Result.failure(Exception("Please enter a valid email address and password."))
        }
        return Result.success(Unit)
    }

    override fun getSavedAccounts(): Flow<List<SavedAccount>> = _savedAccounts.asStateFlow()

    override suspend fun switchAccount(userId: String): Boolean {
        delay(500)
        val current = _savedAccounts.value.toMutableList()
        val targetIdx = current.indexOfFirst { it.userId == userId }
        if (targetIdx < 0) return false

        val updated = current.map { acc ->
            acc.copy(isActive = (acc.userId == userId))
        }
        _savedAccounts.value = updated

        val targetAccount = current[targetIdx]
        val matchingUser = FakeDataSource.users.find { it.id == userId }
            ?: User(id = targetAccount.userId, name = targetAccount.name, avatarUrl = targetAccount.avatarUrl)
        userRepository.updateCurrentUser(matchingUser)
        return true
    }

    override suspend fun addAccount(email: String, password: String): Result<Boolean> {
        delay(800)
        if (!email.contains("@") || password.length < 4) {
            return Result.failure(Exception("Invalid credentials provided for secondary account."))
        }
        val nameFromEmail = email.substringBefore("@").split(".").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        val newUser = User(
            id = "u_${System.currentTimeMillis()}",
            name = nameFromEmail,
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=$nameFromEmail",
            role = UserRole.RESEARCHER,
            institution = "CiteCircle Network"
        )
        updateSavedAccountList(newUser, email)
        userRepository.updateCurrentUser(newUser)
        return Result.success(true)
    }

    override suspend fun removeAccount(userId: String): Boolean {
        val current = _savedAccounts.value.filter { it.userId != userId }
        _savedAccounts.value = current
        return true
    }

    override suspend fun clearCache(): Boolean {
        delay(500)
        return true
    }

    override suspend fun exportUserData(): Result<String> {
        delay(400)
        val jsonSummary = """
            {
              "exportTimestamp": ${System.currentTimeMillis()},
              "appName": "CiteCircle",
              "userAccounts": ${_savedAccounts.value.size},
              "activeUser": "${_savedAccounts.value.find { it.isActive }?.email ?: "Unknown"}"
            }
        """.trimIndent()
        return Result.success(jsonSummary)
    }

    override suspend fun logoutAll() {
        _savedAccounts.value = emptyList()
        _loggedIn = false
    }

    private fun updateSavedAccountList(user: User, email: String) {
        val current = _savedAccounts.value.map { it.copy(isActive = false) }.toMutableList()
        val existingIdx = current.indexOfFirst { it.userId == user.id }
        val newAcc = SavedAccount(
            userId = user.id,
            email = email,
            name = user.name,
            avatarUrl = user.avatarUrl,
            role = user.role.name,
            isActive = true
        )
        if (existingIdx >= 0) {
            current[existingIdx] = newAcc
        } else {
            current.add(0, newAcc)
        }
        _savedAccounts.value = current
    }
}
