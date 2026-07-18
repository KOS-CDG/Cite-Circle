package com.citecircle.app.core.navigation

import kotlinx.serialization.Serializable

// ──────────────────────────────────────────────────────────────────────────────
// Type-safe Navigation Routes (Navigation 2.8+)
// ──────────────────────────────────────────────────────────────────────────────

sealed interface CcRoute

// Onboarding graph
@Serializable object SplashRoute : CcRoute
@Serializable object OnboardingRoute : CcRoute
@Serializable object AuthRoute : CcRoute
@Serializable object ProfileSetupRoute : CcRoute

// Main graph (bottom nav)
@Serializable object HomeRoute : CcRoute
@Serializable object CirclesRoute : CcRoute
@Serializable object PublishRoute : CcRoute
@Serializable object QuickPostRoute : CcRoute
@Serializable object NetworkRoute : CcRoute
@Serializable object ProfileRoute : CcRoute

// Detail screens
@Serializable data class CircleDetailRoute(val circleId: String) : CcRoute
@Serializable data class PaperDetailRoute(val paperId: String) : CcRoute
@Serializable data class CommentThreadRoute(val postId: String) : CcRoute
@Serializable data class OtherProfileRoute(val userId: String) : CcRoute
@Serializable data class ChatRoute(val conversationId: String) : CcRoute

@Serializable object MessagesRoute : CcRoute
@Serializable object NotificationsRoute : CcRoute
@Serializable object SearchRoute : CcRoute
@Serializable object SettingsRoute : CcRoute
@Serializable object EditProfileRoute : CcRoute
@Serializable object NewMessageRoute : CcRoute
@Serializable object ComponentGalleryRoute : CcRoute

// Bottom nav items metadata
data class BottomNavItem(
    val route: CcRoute,
    val labelRes: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val isPublish: Boolean = false
)
