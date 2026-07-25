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
    @SerialName("external_citation_count") val externalCitationCount: Int = 0,
    @SerialName("publication_count") val publicationCount: Int = 0,
    @SerialName("orcid_verified") val orcidVerified: Boolean = false,
    @SerialName("is_verified") val isVerified: Boolean = false,
    val interests: List<String> = emptyList(),
)

@Serializable
data class PublicationDto(
    val id: String,
    val title: String,
    val abstract: String = "",
    val doi: String = "",
    val journal: String = "",
    @SerialName("publication_year") val publicationYear: Int? = null,
    @SerialName("citation_count") val citationCount: Int = 0,
    @SerialName("work_type") val workType: String = "",
    @SerialName("is_open_access") val isOpenAccess: Boolean = false,
    @SerialName("open_access_url") val openAccessUrl: String = "",
    @SerialName("openalex_id") val openAlexId: String = "",
    @SerialName("last_synced_at") val lastSyncedAt: Long = 0,
)

@Serializable
data class CoauthorDto(
    @SerialName("openalex_author_id") val openAlexAuthorId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("orcid_id") val orcidId: String = "",
    val institution: String = "",
    @SerialName("user_id") val userId: String? = null,
    @SerialName("shared_publications") val sharedPublications: Int = 0,
)

@Serializable
data class OrcidSyncResultDto(
    val status: String,
    @SerialName("works_synced") val worksSynced: Int = 0,
    @SerialName("total_available") val totalAvailable: Int = 0,
    val complete: Boolean = true,
    @SerialName("synced_at") val syncedAt: Long = 0,
    val message: String = "",
)

@Serializable
data class OrcidSyncStateDto(
    @SerialName("user_id") val userId: String,
    @SerialName("orcid_id") val orcidId: String = "",
    val status: String = "IDLE",
    @SerialName("works_synced") val worksSynced: Int = 0,
    @SerialName("last_success_at") val lastSuccessAt: Long? = null,
    @SerialName("last_error") val lastError: String = "",
    @SerialName("next_eligible_at") val nextEligibleAt: Long = 0,
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
    @SerialName("is_endorsed") val isEndorsed: Boolean = false,
    @SerialName("is_saved") val isSaved: Boolean = false,
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
    @SerialName("paper_ids") val paperIds: List<String> = emptyList(),
)

@Serializable
data class CreateShelfDto(
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
    val structure: Int = 0,
    val citations: Int = 0,
    val clarity: Int = 0,
    val originality: Int = 0,
    val verdict: String = "",
    val summary: String = "",
    val strengths: List<String> = emptyList(),
    val weaknesses: List<String> = emptyList(),
    val suggestions: List<AiSuggestionDto> = emptyList(),
    @SerialName("desk_rejected") val deskRejected: Boolean = false,
    @SerialName("created_at") val createdAt: Long = 0,
)

@Serializable
data class AiSuggestionDto(
    val id: String,
    val section: String = "General",
    val text: String,
    val severity: String = "MODERATE",
    @SerialName("passage_quote") val passageQuote: String? = null,
    @SerialName("is_addressed") val isAddressed: Boolean = false,
)

@Serializable
data class ReviewRequestDto(
    @SerialName("paper_id") val paperId: String? = null,
    val title: String? = null,
    val abstract: String? = null,
    @SerialName("full_text") val fullText: String? = null,
    val sections: Map<String, String>? = null,
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

@Serializable
data class PaperAnnotationDto(
    val id: String,
    @SerialName("paper_id") val paperId: String,
    @SerialName("user_id") val userId: String = "",
    @SerialName("page_number") val pageNumber: Int,
    @SerialName("selected_text") val selectedText: String = "",
    val color: String = "YELLOW",
    @SerialName("note_text") val noteText: String = "",
    @SerialName("x_ratio") val xRatio: Float = 0f,
    @SerialName("y_ratio") val yRatio: Float = 0f,
    val timestamp: Long = 0
) {
    fun toDomain(): com.citecircle.app.core.model.PaperAnnotation =
        com.citecircle.app.core.model.PaperAnnotation(
            id = id,
            paperId = paperId,
            userId = userId,
            pageNumber = pageNumber,
            selectedText = selectedText,
            color = runCatching { com.citecircle.app.core.model.AnnotationColor.valueOf(color) }
                .getOrDefault(com.citecircle.app.core.model.AnnotationColor.YELLOW),
            noteText = noteText,
            xRatio = xRatio,
            yRatio = yRatio,
            timestamp = if (timestamp > 0) timestamp else System.currentTimeMillis()
        )
}

@Serializable
data class PaperAnnotationCreateDto(
    @SerialName("page_number") val pageNumber: Int,
    @SerialName("selected_text") val selectedText: String = "",
    val color: String = "YELLOW",
    @SerialName("note_text") val noteText: String = "",
    @SerialName("x_ratio") val xRatio: Float = 0f,
    @SerialName("y_ratio") val yRatio: Float = 0f
)

@Serializable
data class AiPaperBreakdownDto(
    @SerialName("paper_id") val paperId: String,
    @SerialName("abstract_tldr") val abstractTldr: String = "",
    @SerialName("methodology_setup") val methodologySetup: String = "",
    @SerialName("core_results") val coreResults: String = "",
    @SerialName("limitations_future_work") val limitationsFutureWork: String = "",
    @SerialName("key_takeaways") val keyTakeaways: List<String> = emptyList(),
    @SerialName("methodology_quality_index") val methodologyQualityIndex: Int = 85,
    @SerialName("quality_label") val qualityLabel: String = "High Methodological Rigor"
) {
    fun toDomain(): com.citecircle.app.core.model.AiPaperBreakdown =
        com.citecircle.app.core.model.AiPaperBreakdown(
            paperId = paperId,
            abstractTldr = abstractTldr,
            methodologySetup = methodologySetup,
            coreResults = coreResults,
            limitationsFutureWork = limitationsFutureWork,
            keyTakeaways = keyTakeaways,
            methodologyQualityIndex = methodologyQualityIndex,
            qualityLabel = qualityLabel
        )
}


@Serializable
data class CircleMemberDto(
    @SerialName("user_id") val userId: String,
    @SerialName("circle_id") val circleId: String,
    val role: String = "CONTRIBUTOR",
    @SerialName("joined_at") val joinedAt: Long = 0,
    val name: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    val institution: String = ""
)

@Serializable
data class CircleWorkspaceDto(
    val id: String,
    @SerialName("circle_id") val circleId: String,
    @SerialName("pin_board_text") val pinBoardText: String = "",
    @SerialName("access_type") val accessType: String = "PUBLIC",
    @SerialName("invite_code") val inviteCode: String = "",
    @SerialName("updated_at") val updatedAt: Long = 0
)

@Serializable
data class CircleDraftDto(
    val id: String,
    @SerialName("circle_id") val circleId: String,
    val title: String,
    val abstract: String = "",
    @SerialName("lead_author_id") val leadAuthorId: String,
    @SerialName("lead_author_name") val leadAuthorName: String = "",
    @SerialName("lead_author_avatar") val leadAuthorAvatar: String = "",
    @SerialName("file_format") val fileFormat: String = "PDF",
    @SerialName("file_url") val fileUrl: String = "",
    val status: String = "DRAFT",
    val version: String = "v1.0",
    val sections: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("review_count") val reviewCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0
)

@Serializable
data class CircleDraftCreateDto(
    val title: String,
    val abstract: String = "",
    @SerialName("file_format") val fileFormat: String = "PDF",
    @SerialName("file_url") val fileUrl: String = "",
    val sections: List<String> = emptyList()
)

@Serializable
data class DraftReviewRequestDto(
    val id: String,
    @SerialName("draft_id") val draftId: String,
    @SerialName("reviewer_id") val reviewerId: String,
    @SerialName("reviewer_name") val reviewerName: String = "",
    @SerialName("reviewer_avatar") val reviewerAvatar: String = "",
    @SerialName("requester_id") val requesterId: String,
    @SerialName("section_target") val sectionTarget: String = "",
    val status: String = "PENDING",
    val notes: String = "",
    @SerialName("created_at") val createdAt: Long = 0
)

@Serializable
data class DraftReviewRequestCreateDto(
    @SerialName("reviewer_id") val reviewerId: String,
    @SerialName("section_target") val sectionTarget: String = "",
    val notes: String = ""
)

@Serializable
data class DraftCommentDto(
    val id: String,
    @SerialName("draft_id") val draftId: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("author_name") val authorName: String = "",
    @SerialName("author_avatar_url") val authorAvatarUrl: String = "",
    @SerialName("section_index") val sectionIndex: Int = 0,
    @SerialName("paragraph_offset") val paragraphOffset: Int = 0,
    val content: String,
    @SerialName("is_resolved") val isResolved: Boolean = false,
    @SerialName("created_at") val createdAt: Long = 0
)

@Serializable
data class DraftCommentCreateDto(
    @SerialName("section_index") val sectionIndex: Int = 0,
    @SerialName("paragraph_offset") val paragraphOffset: Int = 0,
    val content: String
)

@Serializable
data class CircleReadingListDto(
    val id: String,
    @SerialName("circle_id") val circleId: String,
    val title: String,
    val description: String = "",
    @SerialName("created_by_id") val createdById: String,
    @SerialName("created_by_name") val createdByName: String = "",
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("paper_count") val paperCount: Int = 0,
    val papers: List<PaperDto> = emptyList()
)

@Serializable
data class CircleReadingListCreateDto(
    val title: String,
    val description: String = "",
    @SerialName("paper_ids") val paperIds: List<String> = emptyList()
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

    @POST("users/{userId}/accept")
    suspend fun acceptConnection(@Path("userId") userId: String): ConnectionResponseDto

    @POST("users/{userId}/decline")
    suspend fun declineConnection(@Path("userId") userId: String): ConnectionResponseDto

    // ── Publications (ORCID / OpenAlex) ───────────────────────────────────────

    @POST("users/me/orcid/sync")
    suspend fun syncMyPublications(@Query("force") force: Boolean = false): OrcidSyncResultDto

    @GET("users/me/orcid/state")
    suspend fun getMySyncState(): OrcidSyncStateDto

    @GET("users/me/publications")
    suspend fun getMyPublications(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): List<PublicationDto>

    @GET("users/me/coauthors")
    suspend fun getMyCoauthors(): List<CoauthorDto>

    @GET("users/{userId}/publications")
    suspend fun getUserPublications(
        @Path("userId") userId: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): List<PublicationDto>

    @GET("users/{userId}/coauthors")
    suspend fun getUserCoauthors(@Path("userId") userId: String): List<CoauthorDto>

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

    @POST("posts/{postId}/save")
    suspend fun toggleSavePost(@Path("postId") postId: String): Map<String, Boolean>

    @GET("posts/saved")
    suspend fun getSavedPosts(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): List<PostDto>

    @GET("posts/{postId}")
    suspend fun getPost(@Path("postId") postId: String): PostDto


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

    @POST("papers/{paperId}/cite")
    suspend fun citePaper(@Path("paperId") paperId: String): Map<String, @JvmSuppressWildcards Any>

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

    @GET("papers/{paperId}/annotations")
    suspend fun getPaperAnnotations(@Path("paperId") paperId: String): List<PaperAnnotationDto>

    @POST("papers/{paperId}/annotations")
    suspend fun createPaperAnnotation(
        @Path("paperId") paperId: String,
        @Body body: PaperAnnotationCreateDto
    ): PaperAnnotationDto

    @DELETE("papers/{paperId}/annotations/{annotationId}")
    suspend fun deletePaperAnnotation(
        @Path("paperId") paperId: String,
        @Path("annotationId") annotationId: String
    ): Map<String, Boolean>

    @POST("papers/{paperId}/ai-breakdown")
    suspend fun getAiPaperBreakdown(@Path("paperId") paperId: String): AiPaperBreakdownDto


    // ── Shelves ───────────────────────────────────────────────────────────────

    @GET("shelves")
    suspend fun getShelves(): List<ShelfDto>

    @POST("shelves")
    suspend fun createShelf(@Body body: CreateShelfDto): ShelfDto

    @POST("shelves/{shelfId}/papers/{paperId}")
    suspend fun addPaperToShelf(
        @Path("shelfId") shelfId: String,
        @Path("paperId") paperId: String,
    ): Map<String, Boolean>

    @DELETE("shelves/{shelfId}/papers/{paperId}")
    suspend fun removePaperFromShelf(
        @Path("shelfId") shelfId: String,
        @Path("paperId") paperId: String,
    ): Map<String, Boolean>

    // ── AI Review ─────────────────────────────────────────────────────────────

    @POST("papers/review")
    suspend fun reviewPaper(@Body body: ReviewRequestDto): AiReviewReportDto

    @Multipart
    @POST("papers/review-file")
    suspend fun reviewPaperFile(
        @Part file: MultipartBody.Part,
        @Part("title") title: RequestBody? = null,
        @Part("abstract") abstract: RequestBody? = null
    ): AiReviewReportDto

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

    @DELETE("conversations/{conversationId}/messages")
    suspend fun clearMessages(
        @Path("conversationId") conversationId: String,
    ): Map<String, Boolean>


    // ── Circles ───────────────────────────────────────────────────────────────

    @GET("circles")
    suspend fun getCircles(): List<CircleDto>

    @GET("circles/{circleId}")
    suspend fun getCircle(@Path("circleId") circleId: String): CircleDto

    @POST("circles/{circleId}/join")
    suspend fun joinCircle(@Path("circleId") circleId: String): Map<String, Boolean>

    @POST("circles/{circleId}/leave")
    suspend fun leaveCircle(@Path("circleId") circleId: String): Map<String, Boolean>

    @GET("circles/{circleId}/workspace")
    suspend fun getCircleWorkspace(@Path("circleId") circleId: String): CircleWorkspaceDto

    @PUT("circles/{circleId}/pinboard")
    suspend fun updatePinboard(@Path("circleId") circleId: String, @Body body: Map<String, String>): Map<String, @JvmSuppressWildcards Any>

    @GET("circles/{circleId}/members")
    suspend fun getCircleMembers(@Path("circleId") circleId: String): List<CircleMemberDto>

    @PUT("circles/{circleId}/members/{userId}/role")
    suspend fun updateMemberRole(@Path("circleId") circleId: String, @Path("userId") userId: String, @Body body: Map<String, String>): Map<String, @JvmSuppressWildcards Any>

    @POST("circles/{circleId}/invite-code")
    suspend fun generateInviteCode(@Path("circleId") circleId: String): Map<String, String>

    @GET("circles/{circleId}/drafts")
    suspend fun getCircleDrafts(@Path("circleId") circleId: String): List<CircleDraftDto>

    @POST("circles/{circleId}/drafts")
    suspend fun createCircleDraft(@Path("circleId") circleId: String, @Body body: CircleDraftCreateDto): CircleDraftDto

    @GET("drafts/{draftId}")
    suspend fun getDraft(@Path("draftId") draftId: String): CircleDraftDto

    @GET("drafts/{draftId}/review-requests")
    suspend fun getDraftReviewRequests(@Path("draftId") draftId: String): List<DraftReviewRequestDto>

    @POST("drafts/{draftId}/review-requests")
    suspend fun createDraftReviewRequest(@Path("draftId") draftId: String, @Body body: DraftReviewRequestCreateDto): DraftReviewRequestDto

    @GET("drafts/{draftId}/comments")
    suspend fun getDraftComments(@Path("draftId") draftId: String): List<DraftCommentDto>

    @POST("drafts/{draftId}/comments")
    suspend fun createDraftComment(@Path("draftId") draftId: String, @Body body: DraftCommentCreateDto): DraftCommentDto

    @POST("drafts/comments/{commentId}/resolve")
    suspend fun resolveDraftComment(@Path("commentId") commentId: String): Map<String, Boolean>

    @GET("circles/{circleId}/reading-lists")
    suspend fun getCircleReadingLists(@Path("circleId") circleId: String): List<CircleReadingListDto>

    @POST("circles/{circleId}/reading-lists")
    suspend fun createCircleReadingList(@Path("circleId") circleId: String, @Body body: CircleReadingListCreateDto): CircleReadingListDto

    @POST("circles/reading-lists/{listId}/save-to-library")
    suspend fun saveReadingListToLibrary(@Path("listId") listId: String): Map<String, Boolean>

    // ── Notifications ─────────────────────────────────────────────────────────

    @GET("notifications")
    suspend fun getNotifications(): List<NotificationDto>

    @POST("notifications/{notifId}/read")
    suspend fun markNotificationRead(@Path("notifId") notifId: String): Map<String, Boolean>

    @DELETE("notifications/{notifId}")
    suspend fun dismissNotification(@Path("notifId") notifId: String): Map<String, Boolean>

    @GET("papers/{paperId}/citation-graph")
    suspend fun getPaperCitationGraph(
        @Path("paperId") paperId: String,
        @Query("depth") depth: Int = 2,
        @Query("min_citations") minCitations: Int = 0,
        @Query("year_start") yearStart: Int? = null,
        @Query("year_end") yearEnd: Int? = null
    ): CitationGraphResponseDto

    @GET("users/{userId}/coauthor-graph")
    suspend fun getUserCoauthorGraph(
        @Path("userId") userId: String
    ): CoauthorGraphResponseDto

    // ── Search ────────────────────────────────────────────────────────────────

    @GET("search")
    suspend fun search(@Query("q") query: String): Map<String, @JvmSuppressWildcards Any>
}


// ── Graph DTOs ────────────────────────────────────────────────────────────

@Serializable
data class CitationGraphNodeDto(
    val id: String,
    val title: String,
    val abstract: String = "",
    @SerialName("citation_count") val citationCount: Int = 0,
    val year: Int = 2024,
    @SerialName("circle_id") val circleId: String? = null,
    val field: String = "General",
    val authors: List<UserDto> = emptyList(),
    val doi: String = "",
    val journal: String = "",
    @SerialName("is_center") val isCenter: Boolean = false,
    @SerialName("hop_distance") val hopDistance: Int = 0,
    val x: Float = 0f,
    val y: Float = 0f
)

@Serializable
data class CitationGraphEdgeDto(
    val source: String,
    val target: String,
    val type: String = "CITES"
)

@Serializable
data class CitationGraphSummaryDto(
    @SerialName("total_papers") val totalPapers: Int = 0,
    @SerialName("total_citations") val totalCitations: Int = 0,
    @SerialName("max_depth") val maxDepth: Int = 2
)

@Serializable
data class CitationGraphResponseDto(
    val nodes: List<CitationGraphNodeDto> = emptyList(),
    val edges: List<CitationGraphEdgeDto> = emptyList(),
    val summary: CitationGraphSummaryDto = CitationGraphSummaryDto()
)

@Serializable
data class CoauthorGraphNodeDto(
    val id: String,
    val name: String,
    @SerialName("avatar_url") val avatarUrl: String = "",
    val institution: String = "",
    @SerialName("field_of_study") val fieldOfStudy: String = "",
    @SerialName("citation_count") val citationCount: Int = 0,
    @SerialName("h_index") val hIndex: Int = 0,
    @SerialName("i10_index") val i10Index: Int = 0,
    @SerialName("cluster_id") val clusterId: String = "",
    @SerialName("is_center") val isCenter: Boolean = false,
    val x: Float = 0f,
    val y: Float = 0f
)

@Serializable
data class CoauthorGraphEdgeDto(
    val source: String,
    val target: String,
    val weight: Int = 1,
    val publications: List<String> = emptyList()
)

@Serializable
data class CoauthorClusterDto(
    val id: String,
    val name: String,
    val color: String = "#6C63FF",
    @SerialName("member_ids") val memberIds: List<String> = emptyList()
)

@Serializable
data class CitationVelocityPointDto(
    val year: Int,
    val count: Int
)

@Serializable
data class ResearcherAnalyticsDto(
    @SerialName("total_citations") val totalCitations: Int = 0,
    @SerialName("h_index") val hIndex: Int = 0,
    @SerialName("i10_index") val i10Index: Int = 0,
    @SerialName("citation_velocity") val citationVelocity: List<CitationVelocityPointDto> = emptyList()
)

@Serializable
data class CoauthorGraphResponseDto(
    val nodes: List<CoauthorGraphNodeDto> = emptyList(),
    val edges: List<CoauthorGraphEdgeDto> = emptyList(),
    val clusters: List<CoauthorClusterDto> = emptyList(),
    val analytics: ResearcherAnalyticsDto = ResearcherAnalyticsDto()
)


