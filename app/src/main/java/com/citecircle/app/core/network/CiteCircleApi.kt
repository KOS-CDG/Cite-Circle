package com.citecircle.app.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

// ──────────────────────────────────────────────────────────────────────────────
// API Response DTOs  (mirror backend/schemas.py snake_case JSON)
// ──────────────────────────────────────────────────────────────────────────────

@Serializable
data class AuthResponseDto(
    @SerialName("user_id") val userId: String,
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class SignupRequestDto(
    val email: String,
    val password: String,
    val name: String = "",
    val institution: String = "",
    @SerialName("field_of_study") val fieldOfStudy: String = "",
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class UserDto(
    val id: String,
    val name: String,
    @SerialName("avatar_url") val avatarUrl: String = "",
    @SerialName("cover_url") val coverUrl: String = "",
    val role: String = "STUDENT",
    val institution: String = "",
    @SerialName("field_of_study") val fieldOfStudy: String = "",
    val bio: String = "",
    @SerialName("orcid_id") val orcidId: String = "",
    @SerialName("follower_count") val followerCount: Int = 0,
    @SerialName("following_count") val followingCount: Int = 0,
    @SerialName("citation_count") val citationCount: Int = 0,
    @SerialName("is_verified") val isVerified: Boolean = false,
    val interests: List<String> = emptyList(),
)

@Serializable
data class UserUpdateDto(
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    val institution: String? = null,
    @SerialName("field_of_study") val fieldOfStudy: String? = null,
    val bio: String? = null,
    @SerialName("orcid_id") val orcidId: String? = null,
    val interests: List<String>? = null,
)

@Serializable
data class PostDto(
    val id: String,
    @SerialName("author_id") val authorId: String,
    val content: String,
    val type: String = "DISCUSSION",
    val timestamp: Long = 0,
    @SerialName("endorse_count") val endorseCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("circle_id") val circleId: String? = null,
    @SerialName("attached_paper_id") val attachedPaperId: String? = null,
    @SerialName("milestone_text") val milestoneText: String? = null,
    val flair: String = "NONE",
    @SerialName("image_url") val imageUrl: String? = null,
)

@Serializable
data class PostCreateDto(
    val content: String,
    val type: String = "DISCUSSION",
    @SerialName("circle_id") val circleId: String? = null,
    @SerialName("attached_paper_id") val attachedPaperId: String? = null,
    @SerialName("milestone_text") val milestoneText: String? = null,
    val flair: String = "NONE",
    @SerialName("image_url") val imageUrl: String? = null,
)

@Serializable
data class EndorseResponseDto(
    val endorsed: Boolean,
    @SerialName("endorse_count") val endorseCount: Int,
)

@Serializable
data class FollowResponseDto(
    val following: Boolean,
)

@Serializable
data class ConnectionResponseDto(
    val status: String,
)

@Serializable
data class PaperDto(
    val id: String,
    val title: String,
    val abstract: String = "",
    @SerialName("citation_count") val citationCount: Int = 0,
    val year: Int = 2024,
    @SerialName("pdf_url") val pdfUrl: String? = null,
    val doi: String = "",
    @SerialName("circle_id") val circleId: String? = null,
    @SerialName("is_published") val isPublished: Boolean = true,
    @SerialName("ai_score") val aiScore: Int? = null,
    val journal: String = "",
)

@Serializable
data class CommentDto(
    val id: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("post_id") val postId: String,
    val content: String,
    val timestamp: Long = 0,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("parent_id") val parentId: String? = null,
)

@Serializable
data class CommentCreateDto(
    val content: String,
    @SerialName("parent_id") val parentId: String? = null,
)

@Serializable
data class ConversationDto(
    val id: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class MessageDto(
    val id: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String,
    val timestamp: Long = 0,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("attached_paper_id") val attachedPaperId: String? = null,
)

@Serializable
data class MessageCreateDto(
    val content: String,
    @SerialName("attached_paper_id") val attachedPaperId: String? = null,
)

@Serializable
data class NotificationDto(
    val id: String,
    val type: String,
    @SerialName("actor_id") val actorId: String,
    @SerialName("receiver_id") val receiverId: String,
    val content: String,
    val timestamp: Long = 0,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("target_id") val targetId: String = "",
)

@Serializable
data class ShelfDto(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    val description: String = "",
)

@Serializable
data class AiReviewReportDto(
    val id: String,
    @SerialName("paper_id") val paperId: String = "",
    @SerialName("paper_title") val paperTitle: String = "",
    val model: String = "",
    @SerialName("overall_score") val overallScore: Int = 0,
    val verdict: String = "",
    val summary: String = "",
    val strengths: List<String> = emptyList(),
    val weaknesses: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: Long = 0,
)

@Serializable
data class ReviewRequestDto(
    @SerialName("paper_id") val paperId: String? = null,
    val title: String? = null,
    val abstract: String? = null,
)

@Serializable
data class CircleDto(
    val id: String,
    val name: String,
    val description: String = "",
    @SerialName("icon_emoji") val iconEmoji: String = "📚",
    @SerialName("banner_color") val bannerColor: Long = 0xFF6C63FF,
    @SerialName("member_count") val memberCount: Int = 0,
    val category: String = "",
    @SerialName("post_count") val postCount: Int = 0,
)

@Serializable
data class PublishPaperResponseDto(
    val paper: PaperDto,
    @SerialName("thumbnail_url") val thumbnailUrl: String = ""
)

// ──────────────────────────────────────────────────────────────────────────────
// Retrofit API Interface
// ──────────────────────────────────────────────────────────────────────────────

interface CiteCircleApi {

    // ── Auth ──────────────────────────────────────────────────────────────────

    @POST("auth/signup")
    suspend fun signup(@Body body: SignupRequestDto): AuthResponseDto

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): AuthResponseDto

    // ── Users ─────────────────────────────────────────────────────────────────

    @GET("users/me")
    suspend fun getMe(): UserDto

    @PUT("users/me")
    suspend fun updateMe(@Body body: UserUpdateDto): UserDto

    @GET("users/suggested")
    suspend fun getSuggestedUsers(): List<UserDto>

    @GET("users/{userId}")
    suspend fun getUser(@Path("userId") userId: String): UserDto

    @POST("users/{userId}/follow")
    suspend fun toggleFollow(@Path("userId") userId: String): FollowResponseDto

    @POST("users/{userId}/connect")
    suspend fun requestConnection(@Path("userId") userId: String): ConnectionResponseDto

    // ── Posts ─────────────────────────────────────────────────────────────────

    @GET("posts")
    suspend fun getFeedPosts(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): List<PostDto>

    @GET("posts/circle/{circleId}")
    suspend fun getCirclePosts(
        @Path("circleId") circleId: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): List<PostDto>

    @POST("posts")
    suspend fun createPost(@Body body: PostCreateDto): PostDto

    @POST("posts/{postId}/endorse")
    suspend fun toggleEndorse(@Path("postId") postId: String): EndorseResponseDto

    // ── Comments ──────────────────────────────────────────────────────────────

    @GET("posts/{postId}/comments")
    suspend fun getComments(@Path("postId") postId: String): List<CommentDto>

    @POST("posts/{postId}/comments")
    suspend fun createComment(
        @Path("postId") postId: String,
        @Body body: CommentCreateDto,
    ): CommentDto

    // ── Papers ────────────────────────────────────────────────────────────────

    @GET("papers")
    suspend fun getPapers(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): List<PaperDto>

    @GET("papers/{paperId}")
    suspend fun getPaper(@Path("paperId") paperId: String): PaperDto

    @Multipart
    @POST("papers/publish")
    suspend fun publishPaper(
        @Part file: MultipartBody.Part,
        @Part("title") title: RequestBody,
        @Part("abstract") abstract: RequestBody,
        @Part("year") year: RequestBody,
        @Part("doi") doi: RequestBody,
        @Part("journal") journal: RequestBody,
        @Part("circle_id") circleId: RequestBody?,
    ): PublishPaperResponseDto

    // ── AI Review ─────────────────────────────────────────────────────────────

    @POST("papers/review")
    suspend fun reviewPaper(@Body body: ReviewRequestDto): AiReviewReportDto

    // ── Conversations / Messages ──────────────────────────────────────────────

    @GET("conversations")
    suspend fun getConversations(): List<ConversationDto>

    @GET("conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Path("conversationId") conversationId: String,
    ): List<MessageDto>

    @POST("conversations/{conversationId}/messages")
    suspend fun sendMessage(
        @Path("conversationId") conversationId: String,
        @Body body: MessageCreateDto,
    ): List<MessageDto>

    // ── Circles ───────────────────────────────────────────────────────────────

    @GET("circles")
    suspend fun getCircles(): List<CircleDto>

    @GET("circles/{circleId}")
    suspend fun getCircle(@Path("circleId") circleId: String): CircleDto

    @POST("circles/{circleId}/join")
    suspend fun joinCircle(@Path("circleId") circleId: String): Map<String, Boolean>

    @POST("circles/{circleId}/leave")
    suspend fun leaveCircle(@Path("circleId") circleId: String): Map<String, Boolean>

    // ── Notifications ─────────────────────────────────────────────────────────

    @GET("notifications")
    suspend fun getNotifications(): List<NotificationDto>

    @POST("notifications/{notifId}/read")
    suspend fun markNotificationRead(@Path("notifId") notifId: String): Map<String, Boolean>

    // ── Search ────────────────────────────────────────────────────────────────

    @GET("search")
    suspend fun search(@Query("q") query: String): Map<String, @JvmSuppressWildcards Any>
}
