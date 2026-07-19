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
}
