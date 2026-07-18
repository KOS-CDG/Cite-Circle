package com.citecircle.app.core.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.citecircle.app.core.designsystem.*
import com.citecircle.app.feature.circles.CircleDetailScreen
import com.citecircle.app.feature.circles.CirclesScreen
import com.citecircle.app.feature.feed.CommentThreadScreen
import com.citecircle.app.feature.feed.FeedScreen
import com.citecircle.app.feature.messages.ChatScreen
import com.citecircle.app.feature.messages.ConversationListScreen
import com.citecircle.app.feature.messages.NewMessageScreen
import com.citecircle.app.feature.network.NetworkScreen
import com.citecircle.app.feature.notifications.NotificationsScreen
import com.citecircle.app.feature.onboarding.AuthScreen
import com.citecircle.app.feature.onboarding.OnboardingScreen
import com.citecircle.app.feature.onboarding.ProfileSetupScreen
import com.citecircle.app.feature.onboarding.SplashScreen
import com.citecircle.app.feature.papers.PaperDetailScreen
import com.citecircle.app.feature.profile.EditProfileScreen
import com.citecircle.app.feature.profile.OtherProfileScreen
import com.citecircle.app.feature.profile.ProfileScreen
import com.citecircle.app.feature.publish.PublishFlowScreen
import com.citecircle.app.feature.publish.QuickPostScreen
import com.citecircle.app.feature.search.SearchScreen
import com.citecircle.app.feature.settings.SettingsScreen

// ──────────────────────────────────────────────────────────────────────────────
// Main App Navigation Shell
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun CiteCircleNavHost(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    startDestination: CcRoute = SplashRoute
) {
    val navController = rememberNavController()
    val backstackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backstackEntry?.destination

    val showBottomBar = currentRoute?.let { dest ->
        bottomNavItems.any { dest.hasRoute(it.route::class) }
    } ?: false

    val showTopBar = currentRoute?.let { dest ->
        dest.hasRoute(HomeRoute::class) ||
                dest.hasRoute(CirclesRoute::class) ||
                dest.hasRoute(NetworkRoute::class)
    } ?: false

    val unreadNotifications = 3 // In real app this comes from a ViewModel

    Scaffold(
        topBar = {
            if (showTopBar) {
                CcTopBar(
                    onSearchClick = { navController.navigate(SearchRoute) },
                    onNotificationsClick = { navController.navigate(NotificationsRoute) },
                    onMessagesClick = { navController.navigate(MessagesRoute) },
                    notificationCount = unreadNotifications
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                CcBottomNav(
                    navController = navController,
                    currentDestination = currentRoute,
                    onPublishClick = { navController.navigate(PublishRoute) }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { slideInHorizontally(initialOffsetX = { it / 3 }) + fadeIn(tween(250)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut(tween(200)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn(tween(250)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 3 }) + fadeOut(tween(200)) }
        ) {
            // ── Onboarding ──
            composable<SplashRoute> {
                SplashScreen(onSplashComplete = {
                    navController.navigate(OnboardingRoute) {
                        popUpTo(SplashRoute::class) { inclusive = true }
                    }
                })
            }
            composable<OnboardingRoute> {
                OnboardingScreen(
                    onGetStarted = { navController.navigate(AuthRoute) },
                    onSkip = { navController.navigate(AuthRoute) }
                )
            }
            composable<AuthRoute> {
                AuthScreen(
                    onAuthSuccess = { navController.navigate(ProfileSetupRoute) },
                    onSkipSetup = {
                        navController.navigate(HomeRoute) {
                            popUpTo(AuthRoute::class) { inclusive = true }
                        }
                    }
                )
            }
            composable<ProfileSetupRoute> {
                ProfileSetupScreen(
                    onSetupComplete = {
                        navController.navigate(HomeRoute) {
                            popUpTo(OnboardingRoute::class) { inclusive = true }
                        }
                    }
                )
            }

            // ── Main screens ──
            composable<HomeRoute> {
                FeedScreen(
                    onPostClick = { postId -> navController.navigate(CommentThreadRoute(postId)) },
                    onPaperClick = { paperId -> navController.navigate(PaperDetailRoute(paperId)) },
                    onUserClick = { userId -> navController.navigate(OtherProfileRoute(userId)) },
                    onCircleClick = { circleId -> navController.navigate(CircleDetailRoute(circleId)) }
                )
            }
            composable<CirclesRoute> {
                CirclesScreen(
                    onCircleClick = { circleId -> navController.navigate(CircleDetailRoute(circleId)) }
                )
            }
            composable<PublishRoute> {
                PublishFlowScreen(
                    onDismiss = { navController.popBackStack() },
                    onQuickPost = { navController.navigate(QuickPostRoute) }
                )
            }
            composable<QuickPostRoute> {
                QuickPostScreen(
                    onDismiss = { navController.popBackStack() },
                    onPostCreated = {
                        navController.navigate(HomeRoute) {
                            popUpTo(HomeRoute::class) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onViewPost = { postId ->
                        navController.navigate(CommentThreadRoute(postId)) {
                            popUpTo(HomeRoute::class)
                        }
                    }
                )
            }
            composable<NetworkRoute> {
                NetworkScreen(
                    onUserClick = { userId -> navController.navigate(OtherProfileRoute(userId)) }
                )
            }
            composable<ProfileRoute> {
                ProfileScreen(
                    onEditProfile = { navController.navigate(EditProfileRoute) },
                    onPaperClick = { paperId -> navController.navigate(PaperDetailRoute(paperId)) },
                    onCircleClick = { circleId -> navController.navigate(CircleDetailRoute(circleId)) }
                )
            }

            // ── Detail screens ──
            composable<CircleDetailRoute> { backEntry ->
                val route = backEntry.toRoute<CircleDetailRoute>()
                CircleDetailScreen(
                    circleId = route.circleId,
                    onBack = { navController.popBackStack() },
                    onPostClick = { postId -> navController.navigate(CommentThreadRoute(postId)) },
                    onPaperClick = { paperId -> navController.navigate(PaperDetailRoute(paperId)) },
                    onUserClick = { userId -> navController.navigate(OtherProfileRoute(userId)) }
                )
            }
            composable<PaperDetailRoute> { backEntry ->
                val route = backEntry.toRoute<PaperDetailRoute>()
                PaperDetailScreen(
                    paperId = route.paperId,
                    onBack = { navController.popBackStack() },
                    onAuthorClick = { userId -> navController.navigate(OtherProfileRoute(userId)) }
                )
            }
            composable<CommentThreadRoute> { backEntry ->
                val route = backEntry.toRoute<CommentThreadRoute>()
                CommentThreadScreen(
                    postId = route.postId,
                    onBack = { navController.popBackStack() },
                    onUserClick = { userId -> navController.navigate(OtherProfileRoute(userId)) }
                )
            }
            composable<OtherProfileRoute> { backEntry ->
                val route = backEntry.toRoute<OtherProfileRoute>()
                OtherProfileScreen(
                    userId = route.userId,
                    onBack = { navController.popBackStack() },
                    onPaperClick = { paperId -> navController.navigate(PaperDetailRoute(paperId)) },
                    onMessageClick = { convId -> navController.navigate(ChatRoute(convId)) }
                )
            }
            composable<MessagesRoute> {
                ConversationListScreen(
                    onBack = { navController.popBackStack() },
                    onConversationClick = { convId -> navController.navigate(ChatRoute(convId)) },
                    onNewMessage = { navController.navigate(NewMessageRoute) }
                )
            }
            composable<ChatRoute> { backEntry ->
                val route = backEntry.toRoute<ChatRoute>()
                ChatScreen(
                    conversationId = route.conversationId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable<NewMessageRoute> {
                NewMessageScreen(
                    onBack = { navController.popBackStack() },
                    onConversationCreated = { convId ->
                        navController.navigate(ChatRoute(convId)) {
                            popUpTo(NewMessageRoute::class) { inclusive = true }
                        }
                    }
                )
            }
            composable<NotificationsRoute> {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onNotificationClick = { targetId ->
                        navController.navigate(CommentThreadRoute(targetId))
                    }
                )
            }
            composable<SearchRoute> {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onUserClick = { userId -> navController.navigate(OtherProfileRoute(userId)) },
                    onPaperClick = { paperId -> navController.navigate(PaperDetailRoute(paperId)) },
                    onCircleClick = { circleId -> navController.navigate(CircleDetailRoute(circleId)) },
                    onPostClick = { postId -> navController.navigate(CommentThreadRoute(postId)) }
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    isDarkTheme = isDarkTheme,
                    onThemeChange = onThemeChange,
                    onLogout = {
                        navController.navigate(AuthRoute) {
                            popUpTo(HomeRoute::class) { inclusive = true }
                        }
                    }
                )
            }
            composable<EditProfileRoute> {
                EditProfileScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable<ComponentGalleryRoute> {
                ComponentGalleryScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// CcTopBar
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CcTopBar(
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onMessagesClick: () -> Unit,
    notificationCount: Int = 0
) {
    TopAppBar(
        title = {
            // CiteCircle wordmark in Fraunces
            Text(
                text = "CiteCircle",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FrauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic
                ),
                color = MaterialTheme.ccColors.inkNavy
            )
        },
        actions = {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.semantics { contentDescription = "Search" }
            ) {
                Icon(Icons.Outlined.Search, contentDescription = "Search", tint = MaterialTheme.ccColors.inkNavy)
            }
            // Notifications with badge
            Box {
                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier.semantics { contentDescription = "Notifications" }
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = MaterialTheme.ccColors.inkNavy)
                }
                if (notificationCount > 0) {
                    CcBadge(
                        count = notificationCount,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 4.dp)
                    )
                }
            }
            IconButton(
                onClick = onMessagesClick,
                modifier = Modifier.semantics { contentDescription = "Messages" }
            ) {
                Icon(Icons.Outlined.MailOutline, contentDescription = "Messages", tint = MaterialTheme.ccColors.inkNavy)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// CcBottomNav — 5 destinations with center FAB-style Publish
// ──────────────────────────────────────────────────────────────────────────────

val bottomNavItems = listOf(
    BottomNavItem(HomeRoute, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(CirclesRoute, "Circles", Icons.Filled.Groups, Icons.Outlined.Groups),
    BottomNavItem(PublishRoute, "Publish", Icons.Filled.Add, Icons.Filled.Add, isPublish = true),
    BottomNavItem(NetworkRoute, "Network", Icons.Filled.People, Icons.Outlined.People),
    BottomNavItem(ProfileRoute, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
)

@Composable
fun CcBottomNav(
    navController: NavController,
    currentDestination: NavDestination?,
    onPublishClick: () -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        tonalElevation = 2.dp
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = currentDestination?.hasRoute(item.route::class) ?: false

            if (item.isPublish) {
                // Center FAB-style Publish button
                NavigationBarItem(
                    selected = false,
                    onClick = onPublishClick,
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(CcColors.HighlighterYellow),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Publish new paper or post",
                                tint = CcColors.InkNavy,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            "Publish",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.ccColors.inkNavy,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent
                    )
                )
            } else {
                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.labelRes,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        if (isSelected) {
                            HighlighterSweep(rotationDegrees = -1f) {
                                Text(
                                    item.labelRes,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Text(item.labelRes, style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CcColors.CircleBlue,
                        unselectedIconColor = MaterialTheme.ccColors.marginGray,
                        selectedTextColor = CcColors.CircleBlue,
                        unselectedTextColor = MaterialTheme.ccColors.marginGray,
                        indicatorColor = CcColors.CircleBlue.copy(alpha = 0.12f)
                    )
                )
            }
        }
    }
}
