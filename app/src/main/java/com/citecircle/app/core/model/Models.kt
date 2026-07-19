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
    val role: UserRole = UserRole.STUDENT,
    val institution: String = "",
    val fieldOfStudy: String = "",
    val bio: String = "",
    val orcidId: String = "",
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val citationCount: Int = 0,
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
    val abstract: String,
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
    val isAddressed: Boolean = false
)

@Serializable
data class AiReviewReport(
    val score: Int,            // 0–100 overall readiness
    val structure: Int,        // 0–100
    val citations: Int,        // 0–100
    val clarity: Int,          // 0–100
    val originality: Int,      // 0–100
    val suggestions: List<AiSuggestion>
)

// ──────────────────────────────────────────────────────────────────────────────
// Publish Draft
// ──────────────────────────────────────────────────────────────────────────────

data class PaperDraft(
    val title: String = "",
    val coAuthors: List<User> = emptyList(),
    val abstract: String = "",
    val fieldTags: List<String> = emptyList(),
    val pdfFileName: String? = null,
    val pdfFileSizeKb: Long? = null
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

