package com.citecircle.app.core.model

import kotlinx.serialization.Serializable

// ──────────────────────────────────────────────────────────────────────────────
// User
// ──────────────────────────────────────────────────────────────────────────────

enum class UserRole {
    STUDENT, EDUCATOR, RESEARCHER, ADMIN
}

@Serializable
data class User(
    val id: String,
    val name: String,
    val avatarUrl: String = "",
    val coverUrl: String = "",
    val role: UserRole = UserRole.STUDENT,
    val institution: String = "",
    val fieldOfStudy: String = "",
    val bio: String = "",
    val orcidId: String = "",
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val citationCount: Int = 0,
    /** Citations on externally indexed work, from the ORCID/OpenAlex sync. */
    val externalCitationCount: Int = 0,
    val publicationCount: Int = 0,
    val orcidVerified: Boolean = false,
    val isVerified: Boolean = false,
    val isFollowing: Boolean = false,
    val isConnected: Boolean = false,
    val connectionPending: Boolean = false,
    val interests: List<String> = emptyList()
)

// ──────────────────────────────────────────────────────────────────────────────
// Post
// ──────────────────────────────────────────────────────────────────────────────

enum class PostType {
    DISCUSSION, PAPER_SHARE, MILESTONE, CIRCLE_ACTIVITY
}

enum class PostFlair {
    QUESTION, DISCUSSION, PAPER_FEEDBACK, RESOURCE, NONE
}

data class Post(
    val id: String,
    val author: User,
    val content: String,
    val type: PostType = PostType.DISCUSSION,
    val timestamp: Long = System.currentTimeMillis(),
    val endorseCount: Int = 0,
    val commentCount: Int = 0,
    val isEndorsed: Boolean = false,
    val isSaved: Boolean = false,
    val circleId: String? = null,
    val circleName: String? = null,
    val attachedPaper: Paper? = null,
    val milestoneText: String? = null,
    val flair: PostFlair = PostFlair.NONE,
    val imageUrl: String? = null
)

// ──────────────────────────────────────────────────────────────────────────────
// Paper
// ──────────────────────────────────────────────────────────────────────────────

@Serializable
data class Paper(
    val id: String,
    val title: String,
    val authors: List<User> = emptyList(),
    val abstract: String = "",
    val fieldTags: List<String> = emptyList(),
    val citationCount: Int = 0,
    val year: Int = 2024,
    val pdfUrl: String? = null,
    val doi: String = "",
    val circleId: String? = null,
    val isPublished: Boolean = true,
    val aiScore: Int? = null,
    val journal: String = ""
)

// ──────────────────────────────────────────────────────────────────────────────
// Paper Annotation & AI Section Breakdown
// ──────────────────────────────────────────────────────────────────────────────

@Serializable
enum class AnnotationColor(val hex: Long, val label: String) {
    YELLOW(0xFFFFF59D, "Key Finding"),
    GREEN(0xFFA5D6A7, "Methodology"),
    BLUE(0xFF90CAF9, "Citation"),
    PURPLE(0xFFCE93D8, "Question / Critique")
}

@Serializable
data class PaperAnnotation(
    val id: String,
    val paperId: String,
    val userId: String = "",
    val pageNumber: Int,
    val selectedText: String = "",
    val color: AnnotationColor = AnnotationColor.YELLOW,
    val noteText: String = "",
    val xRatio: Float = 0f,
    val yRatio: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class AiPaperBreakdown(
    val paperId: String,
    val abstractTldr: String = "",
    val methodologySetup: String = "",
    val coreResults: String = "",
    val limitationsFutureWork: String = "",
    val keyTakeaways: List<String> = emptyList(),
    val methodologyQualityIndex: Int = 85,
    val qualityLabel: String = "High Methodological Rigor"
)


// ──────────────────────────────────────────────────────────────────────────────
// Circle (Community)
// ──────────────────────────────────────────────────────────────────────────────

@Serializable
data class Circle(
    val id: String,
    val name: String,
    val description: String,
    val iconEmoji: String,
    val bannerColor: Long,        // packed ARGB color
    val memberCount: Int = 0,
    val category: String = "",
    val isJoined: Boolean = false,
    val postCount: Int = 0,
    val weeklyPostCount: Int = 0,
    val weeklyActivity: List<Int> = emptyList()  // sparkline data
)

// ──────────────────────────────────────────────────────────────────────────────
// Comment
// ──────────────────────────────────────────────────────────────────────────────

data class Comment(
    val id: String,
    val author: User,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val replyCount: Int = 0,
    val replies: List<Comment> = emptyList(),
    val isLiked: Boolean = false,
    val likeCount: Int = 0,
    val parentId: String? = null
)

// ──────────────────────────────────────────────────────────────────────────────
// Notifications
// ──────────────────────────────────────────────────────────────────────────────

enum class NotifType {
    ENDORSEMENT, COMMENT, CONNECTION, CIRCLE_INVITE, AI_APPROVED, CITATION, NEW_FOLLOWER
}

data class Notification(
    val id: String,
    val type: NotifType,
    val actor: User,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val targetId: String = ""
)

// ──────────────────────────────────────────────────────────────────────────────
// Messaging
// ──────────────────────────────────────────────────────────────────────────────

@Serializable
data class Message(
    val id: String,
    val senderId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val attachedPaper: Paper? = null
)

@Serializable
data class Conversation(
    val id: String,
    val participants: List<User>,
    val lastMessage: Message?,
    val unreadCount: Int = 0
)

// ──────────────────────────────────────────────────────────────────────────────
// AI Review
// ──────────────────────────────────────────────────────────────────────────────

@Serializable
enum class Severity {
    MINOR, MODERATE, NEEDS_ATTENTION
}

@Serializable
data class AiSuggestion(
    val id: String,
    val section: String,
    val text: String,
    val severity: Severity,
    val passageQuote: String? = null,
    val isAddressed: Boolean = false
)

@Serializable
data class AiReviewReport(
    val score: Int,            // 0–100 overall readiness
    val structure: Int,        // 0–100
    val citations: Int,        // 0–100
    val clarity: Int,          // 0–100
    val originality: Int,      // 0–100
    val verdict: String = "",
    val summary: String = "",
    val strengths: List<String> = emptyList(),
    val weaknesses: List<String> = emptyList(),
    val suggestions: List<AiSuggestion> = emptyList(),
    val deskRejected: Boolean = false
)

// ──────────────────────────────────────────────────────────────────────────────
// Publish Draft
// ──────────────────────────────────────────────────────────────────────────────

data class PaperDraft(
    val title: String = "",
    val coAuthors: List<User> = emptyList(),
    val abstract: String = "",
    val fullText: String = "",
    val sections: Map<String, String> = emptyMap(),
    val fieldTags: List<String> = emptyList(),
    val pdfFileName: String? = null,
    val pdfFileSizeKb: Long? = null,
    val pdfUri: android.net.Uri? = null
)

// ──────────────────────────────────────────────────────────────────────────────
// Search
// ──────────────────────────────────────────────────────────────────────────────

data class SearchResults(
    val people: List<User> = emptyList(),
    val papers: List<Paper> = emptyList(),
    val circles: List<Circle> = emptyList(),
    val posts: List<Post> = emptyList()
)

// ──────────────────────────────────────────────────────────────────────────────
// Shelf (Library Folder)
// ──────────────────────────────────────────────────────────────────────────────

@Serializable
data class Shelf(
    val id: String,
    val name: String,
    val description: String = "",
    val paperIds: List<String> = emptyList()
)

// ──────────────────────────────────────────────────────────────────────────────
// Publications (ORCID / OpenAlex bibliography)
//
// Distinct from Paper: a Paper is in-app content that lives in circles and the
// library, a Publication mirrors an externally indexed work from OpenAlex.
// ──────────────────────────────────────────────────────────────────────────────

@Serializable
data class Publication(
    val id: String,
    val title: String,
    val abstract: String = "",
    val doi: String = "",
    val journal: String = "",
    val year: Int? = null,
    val citationCount: Int = 0,
    val workType: String = "",
    val isOpenAccess: Boolean = false,
    val openAccessUrl: String = "",
    val openAlexId: String = "",
    val lastSyncedAt: Long = 0L,
) {
    /** Resolvable link to the work, preferring the OA copy when one exists. */
    val externalUrl: String
        get() = when {
            openAccessUrl.isNotBlank() -> openAccessUrl
            doi.isNotBlank() -> "https://doi.org/$doi"
            else -> openAlexId
        }
}

@Serializable
data class Coauthor(
    val openAlexAuthorId: String,
    val displayName: String,
    val orcidId: String = "",
    val institution: String = "",
    /** Non-null when this co-author is also a Cite Circle member. */
    val userId: String? = null,
    val sharedPublications: Int = 0,
)

enum class SyncStatus {
    /** Never synced. */
    IDLE,

    /** A sync is in flight. */
    RUNNING,

    /** Paged out mid-bibliography; calling sync again resumes from the cursor. */
    PARTIAL,

    SUCCESS,
    FAILED,
}

@Serializable
data class OrcidSyncState(
    val orcidId: String = "",
    val status: SyncStatus = SyncStatus.IDLE,
    val worksSynced: Int = 0,
    val lastSuccessAt: Long? = null,
    val lastError: String = "",
    val nextEligibleAt: Long = 0L,
) {
    val hasSynced: Boolean get() = lastSuccessAt != null && lastSuccessAt > 0L
    val isResumable: Boolean get() = status == SyncStatus.PARTIAL
}

/** Outcome of one sync run, surfaced to the user as a snackbar. */
data class OrcidSyncResult(
    val success: Boolean,
    val worksSynced: Int = 0,
    val totalAvailable: Int = 0,
    val complete: Boolean = true,
    val message: String = "",
)

// ──────────────────────────────────────────────────────────────────────────────
// Saved Account Session (Multi-Account Support)
// ──────────────────────────────────────────────────────────────────────────────

@Serializable
data class SavedAccount(
    val userId: String,
    val email: String,
    val name: String,
    val avatarUrl: String = "",
    val role: String = "STUDENT",
    val accessToken: String = "",
    val refreshToken: String = "",
    val isActive: Boolean = false
)

// ──────────────────────────────────────────────────────────────────────────────
// Collaborative Research Workspace (Circle Workspace, Drafts, Reading Lists)
// ──────────────────────────────────────────────────────────────────────────────

enum class CircleRole {
    ADMIN, LEAD_RESEARCHER, CONTRIBUTOR, GUEST_OBSERVER;

    fun displayName(): String = when (this) {
        ADMIN -> "Circle Admin"
        LEAD_RESEARCHER -> "Lead Researcher"
        CONTRIBUTOR -> "Contributor"
        GUEST_OBSERVER -> "Guest Observer"
    }
}

enum class CircleAccessType {
    PUBLIC, PRIVATE_INVITE, REQUEST_APPROVAL
}

enum class DraftStatus {
    DRAFT, UNDER_REVIEW, REVISION, READY_TO_SUBMIT;

    fun displayName(): String = when (this) {
        DRAFT -> "Draft"
        UNDER_REVIEW -> "Under Review"
        REVISION -> "Revision Needed"
        READY_TO_SUBMIT -> "Ready to Submit"
    }
}

enum class DraftFormat {
    PDF, DOCX, MARKDOWN
}

enum class ReviewRequestStatus {
    PENDING, APPROVED, CHANGES_REQUESTED
}

@Serializable
data class CircleMember(
    val userId: String,
    val circleId: String,
    val role: CircleRole = CircleRole.CONTRIBUTOR,
    val joinedAt: Long = 0,
    val name: String = "",
    val avatarUrl: String = "",
    val institution: String = ""
)

@Serializable
data class CircleWorkspace(
    val id: String,
    val circleId: String,
    val pinBoardText: String = "",
    val accessType: CircleAccessType = CircleAccessType.PUBLIC,
    val inviteCode: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class CircleDraft(
    val id: String,
    val circleId: String,
    val title: String,
    val abstract: String = "",
    val leadAuthorId: String,
    val leadAuthorName: String = "",
    val leadAuthorAvatar: String = "",
    val fileFormat: DraftFormat = DraftFormat.PDF,
    val fileUrl: String = "",
    val status: DraftStatus = DraftStatus.DRAFT,
    val version: String = "v1.0",
    val sections: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val reviewCount: Int = 0,
    val commentCount: Int = 0
)

@Serializable
data class DraftReviewRequest(
    val id: String,
    val draftId: String,
    val reviewerId: String,
    val reviewerName: String = "",
    val reviewerAvatar: String = "",
    val requesterId: String,
    val sectionTarget: String = "",
    val status: ReviewRequestStatus = ReviewRequestStatus.PENDING,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class DraftComment(
    val id: String,
    val draftId: String,
    val authorId: String,
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val sectionIndex: Int = 0,
    val paragraphOffset: Int = 0,
    val content: String,
    val isResolved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class CircleReadingList(
    val id: String,
    val circleId: String,
    val title: String,
    val description: String = "",
    val createdById: String,
    val createdByName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val paperCount: Int = 0,
    val papers: List<Paper> = emptyList()
)

@Serializable
data class CircleJoinRequest(
    val id: String,
    val circleId: String,
    val userId: String,
    val userName: String = "",
    val userAvatarUrl: String = "",
    val status: String = "PENDING",
    val message: String = "",
    val createdAt: Long = System.currentTimeMillis()
)



