package com.citecircle.app.core.data

import com.citecircle.app.core.model.*

/**
 * Data source providing current session state and empty defaults.
 * All pre-seeded placeholder data and accounts have been removed.
 * Only the account being created/authenticated by the user will exist.
 */
object FakeDataSource {

    val aiUser = User(
        id = "ai_copilot",
        name = "CiteCircle AI Copilot",
        avatarUrl = "https://api.dicebear.com/8.x/bottts/svg?seed=copilot",
        role = UserRole.RESEARCHER,
        institution = "CiteCircle AI Lab",
        fieldOfStudy = "Academic Research Assistant",
        bio = "CiteCircle's research assistant. Ask me anything about literature reviews, methodology, or academic writing.",
        orcidId = "0000-0000-1111-9999",
        followerCount = 0,
        followingCount = 0,
        citationCount = 0,
        isVerified = true,
        interests = listOf("AI", "Research", "Education", "Methodology")
    )

    var currentUser = User(
        id = "u_guest",
        name = "",
        avatarUrl = "",
        role = UserRole.STUDENT,
        institution = "",
        fieldOfStudy = "",
        bio = "",
        orcidId = "",
        followerCount = 0,
        followingCount = 0,
        citationCount = 0,
        isVerified = false,
        interests = emptyList()
    )

    val users: List<User> = emptyList()

    fun getAllUsers(): List<User> = if (currentUser.id.isNotBlank() && currentUser.id != "u_guest") listOf(currentUser) else emptyList()
    fun getUserById(id: String): User? = if (id == currentUser.id) currentUser else null

    val circles: List<Circle> = emptyList()
    val papers: List<Paper> = emptyList()
    val posts: MutableList<Post> = mutableListOf()

    fun getCommentsForPost(postId: String): List<Comment> = emptyList()

    val notifications: List<Notification> = emptyList()

    val conversations: List<Conversation> = emptyList()

    fun getMessagesForConversation(convId: String): List<Message> = emptyList()

    val sampleAiReport = AiReviewReport(
        score = 85,
        structure = 88,
        citations = 80,
        clarity = 86,
        originality = 82,
        suggestions = listOf(
            AiSuggestion(
                id = "s1",
                section = "Abstract",
                text = "State your quantitative findings clearly in the abstract.",
                severity = Severity.MODERATE,
                isAddressed = false
            ),
            AiSuggestion(
                id = "s2",
                section = "Methodology",
                text = "Ensure power analysis details align with the primary hypotheses.",
                severity = Severity.MINOR,
                isAddressed = false
            )
        )
    )

    val suggestedPeopleForOnboarding: List<User> = emptyList()
    val suggestedCirclesForOnboarding: List<Circle> = emptyList()
    val connectionRequests: List<User> = emptyList()
    val suggestedConnections: List<User> = emptyList()

    // Workspace & Collaborative Research Mock State
    val sampleWorkspace = CircleWorkspace(
        id = "ws_default",
        circleId = "cir_hci",
        pinBoardText = "📌 **Lab Workspace Pin Board**\n\n• Weekly Group Seminar: Thursdays at 4:00 PM EST.\n• Current Focus: Draft manuscript submission for NeurIPS / CHI.\n• Review checklist: Ensure power analysis and code reproducibility links are added.",
        accessType = CircleAccessType.PUBLIC,
        inviteCode = "HCI-LAB-2026"
    )

    val sampleMembers = listOf(
        CircleMember(
            userId = "usr_okafor",
            circleId = "cir_hci",
            role = CircleRole.ADMIN,
            name = "Maya Okafor",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=maya-okafor",
            institution = "MIT Media Lab"
        ),
        CircleMember(
            userId = "usr_whitmore",
            circleId = "cir_hci",
            role = CircleRole.LEAD_RESEARCHER,
            name = "Prof. James Whitmore",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=james-whitmore",
            institution = "Harvard University"
        ),
        CircleMember(
            userId = "usr_park",
            circleId = "cir_hci",
            role = CircleRole.CONTRIBUTOR,
            name = "Daniel Park",
            avatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=daniel-park",
            institution = "CMU"
        )
    )

    val sampleDrafts = mutableListOf(
        CircleDraft(
            id = "drf_01",
            circleId = "cir_hci",
            title = "Situated Epistemic Scaffolding in Human-AI Interaction",
            abstract = "We evaluate how cognitive load and epistemic trust recalibrate when AI assistants suggest structural edits in academic writing.",
            leadAuthorId = "usr_okafor",
            leadAuthorName = "Maya Okafor",
            leadAuthorAvatar = "https://api.dicebear.com/8.x/avataaars/svg?seed=maya-okafor",
            fileFormat = DraftFormat.PDF,
            fileUrl = "https://arxiv.org/pdf/2403.19887",
            status = DraftStatus.UNDER_REVIEW,
            version = "v1.2",
            sections = listOf(
                "Abstract",
                "1. Introduction & Motivation",
                "2. Background & Related Work",
                "3. Study Design & Methodology",
                "4. Empirical Evaluation & Results",
                "5. Discussion & Ethical Considerations"
            ),
            createdAt = System.currentTimeMillis() - 86400000L * 3,
            reviewCount = 2,
            commentCount = 5
        ),
        CircleDraft(
            id = "drf_02",
            circleId = "cir_hci",
            title = "Multimodal Sonification for Low-Vision Graph Exploration",
            abstract = "Designing real-time spatial audio cues paired with haptics to render scatter plots and citation trees accessible.",
            leadAuthorId = "usr_park",
            leadAuthorName = "Daniel Park",
            leadAuthorAvatar = "https://api.dicebear.com/8.x/avataaars/svg?seed=daniel-park",
            fileFormat = DraftFormat.MARKDOWN,
            fileUrl = "",
            status = DraftStatus.DRAFT,
            version = "v0.9",
            sections = listOf(
                "Abstract",
                "1. Motivation",
                "2. Audio Encoding Pipeline",
                "3. User Evaluation"
            ),
            createdAt = System.currentTimeMillis() - 86400000L,
            reviewCount = 1,
            commentCount = 2
        )
    )

    val sampleReviewRequests = mutableListOf(
        DraftReviewRequest(
            id = "rr_01",
            draftId = "drf_01",
            reviewerId = "usr_whitmore",
            reviewerName = "Prof. James Whitmore",
            reviewerAvatar = "https://api.dicebear.com/8.x/avataaars/svg?seed=james-whitmore",
            requesterId = "usr_okafor",
            sectionTarget = "3. Study Design & Methodology",
            status = ReviewRequestStatus.PENDING,
            notes = "Please double check the power analysis equations in Section 3.2.",
            createdAt = System.currentTimeMillis() - 43200000L
        )
    )

    val sampleDraftComments = mutableListOf(
        DraftComment(
            id = "dc_01",
            draftId = "drf_01",
            authorId = "usr_whitmore",
            authorName = "Prof. James Whitmore",
            authorAvatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=james-whitmore",
            sectionIndex = 1,
            paragraphOffset = 0,
            content = "Make sure to contrast epistemic trust with automation bias here — the distinction is crucial for CHI reviewers.",
            isResolved = false,
            createdAt = System.currentTimeMillis() - 72000000L
        ),
        DraftComment(
            id = "dc_02",
            draftId = "drf_01",
            authorId = "usr_park",
            authorName = "Daniel Park",
            authorAvatarUrl = "https://api.dicebear.com/8.x/avataaars/svg?seed=daniel-park",
            sectionIndex = 3,
            paragraphOffset = 1,
            content = "The power analysis sample size looks solid! Did we include screen reader users in the pilot group?",
            isResolved = true,
            createdAt = System.currentTimeMillis() - 36000000L
        )
    )

    val sampleReadingLists = mutableListOf(
        CircleReadingList(
            id = "rl_01",
            circleId = "cir_hci",
            title = "Weekly Lab Seminar Papers",
            description = "Essential reading list for our weekly HCI lab seminar on human-AI collaboration.",
            createdById = "usr_okafor",
            createdByName = "Maya Okafor",
            createdAt = System.currentTimeMillis() - 86400000L * 5,
            paperCount = 3,
            papers = emptyList()
        ),
        CircleReadingList(
            id = "rl_02",
            circleId = "cir_hci",
            title = "Lit Review 2026: Accessible Data Viz",
            description = "Curated collection on multimodal & audio graph representations.",
            createdById = "usr_park",
            createdByName = "Daniel Park",
            createdAt = System.currentTimeMillis() - 86400000L * 2,
            paperCount = 2,
            papers = emptyList()
        )
    )
}

