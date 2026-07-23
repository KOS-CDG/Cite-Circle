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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.citecircle.app.feature.circles.DraftDetailScreen
import com.citecircle.app.feature.circles.CirclesScreen
import com.citecircle.app.feature.feed.CommentThreadScreen
import com.citecircle.app.feature.feed.FeedScreen
import com.citecircle.app.feature.messages.ChatScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.citecircle.app.feature.messages.ConversationListScreen
import com.citecircle.app.feature.messages.MessagesViewModel
import com.citecircle.app.feature.messages.NewMessageScreen
import com.citecircle.app.feature.network.NetworkScreen
import com.citecircle.app.feature.notifications.NotificationsScreen
import com.citecircle.app.feature.notifications.NotificationsViewModel
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
import com.citecircle.app.feature.library.LibraryScreen
import com.citecircle.app.feature.pdf.PdfViewerScreen


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

    val notificationsViewModel: NotificationsViewModel = hiltViewModel()
    val messagesViewModel: MessagesViewModel = hiltViewModel()

    val notifications by notificationsViewModel.notifications.collectAsState()
    val conversations by messagesViewModel.conversations.collectAsState()

    val unreadNotifications = remember(notifications) {
        notifications.count { !it.isRead }
    }
    val unreadMessages = remember(conversations) {
        conversations.sumOf { it.unreadCount }
    }

    Scaffold(
        topBar = {},   // Floating bar is drawn inside Box below
        bottomBar = {}, // Floating bar is drawn inside Box below
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val topPadding = if (showTopBar) {
            WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 80.dp
        } else {
            innerPadding.calculateTopPadding()
        }
        Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                // Extra top padding so content starts below the floating top bar
                .padding(
                    top = topPadding,
                    bottom = if (showBottomBar) 100.dp else innerPadding.calculateBottomPadding()
                ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it / 3 }) + fadeIn(tween(250)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut(tween(200)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn(tween(250)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 3 }) + fadeOut(tween(200)) }
        ) {
            // ── Onboarding ──
            composable<SplashRoute> {
                SplashScreen(onSplashComplete = { isLoggedIn ->
                    val target = if (isLoggedIn) HomeRoute else OnboardingRoute
                    navController.navigate(target) {
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
                    onCircleClick = { circleId -> navController.navigate(CircleDetailRoute(circleId)) },
                    onComposePost = { navController.navigate(QuickPostRoute) }
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
                    onSettingsClick = { navController.navigate(SettingsRoute) },
                    onPaperClick = { paperId -> navController.navigate(PaperDetailRoute(paperId)) },
                    onCircleClick = { circleId -> navController.navigate(CircleDetailRoute(circleId)) }
                )
            }
            composable<LibraryRoute> {
                LibraryScreen(
                    onBack = { navController.popBackStack() },
                    onPaperClick = { paperId -> navController.navigate(PaperDetailRoute(paperId)) }
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
                    onUserClick = { userId -> navController.navigate(OtherProfileRoute(userId)) },
                    onDraftClick = { draftId -> navController.navigate(DraftDetailRoute(draftId)) }
                )
            }
            composable<DraftDetailRoute> { backEntry ->
                val route = backEntry.toRoute<DraftDetailRoute>()
                DraftDetailScreen(
                    draftId = route.draftId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable<PaperDetailRoute> { backEntry ->
                val route = backEntry.toRoute<PaperDetailRoute>()
                PaperDetailScreen(
                    paperId = route.paperId,
                    onBack = { navController.popBackStack() },
                    onAuthorClick = { userId -> navController.navigate(OtherProfileRoute(userId)) },
                    onReadPaperClick = { paperId -> navController.navigate(PdfViewerRoute(paperId)) }
                )
            }
            composable<PdfViewerRoute> { backEntry ->
                val route = backEntry.toRoute<PdfViewerRoute>()
                PdfViewerScreen(
                    paperId = route.paperId,
                    initialPage = route.initialPage,
                    onBack = { navController.popBackStack() },
                    onShareQuoteToFeed = { quote, pageNumber, paperId ->
                        navController.navigate(QuickPostRoute)
                    }
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
                    onEditProfileClick = { navController.navigate(EditProfileRoute) },
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

        // ── Floating Top Bar ──
        if (showTopBar) {
            CcTopBar(
                onSearchClick = { navController.navigate(SearchRoute) },
                onLibraryClick = {
                    navController.navigate(LibraryRoute) {
                        launchSingleTop = true
                    }
                },
                onNotificationsClick = { navController.navigate(NotificationsRoute) },
                notificationCount = unreadNotifications,
                onMessagesClick = { navController.navigate(MessagesRoute) },
                messageCount = unreadMessages,
                onAiChatClick = { navController.navigate(ChatRoute("conv_ai")) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // ── Floating Bottom Nav ──
        if (showBottomBar) {
            CcBottomNav(
                navController = navController,
                currentDestination = currentRoute,
                onPublishClick = { navController.navigate(PublishRoute) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
        } // end Box
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
    onLibraryClick: () -> Unit = {},
    onAiChatClick: () -> Unit = {},
    notificationCount: Int = 0,
    messageCount: Int = 0,
    modifier: Modifier = Modifier
) {
    // Detect dark mode: dark surface has red channel near 0, light surface is near 1
    val isDark = MaterialTheme.colorScheme.background.red < 0.3f
    val glassColor = if (isDark)
        Color(0xFF1E2B42).copy(alpha = 0.82f)
    else
        Color(0xFFFFFFFF).copy(alpha = 0.78f)
    val borderColor = if (isDark)
        Color(0xFF3E5070).copy(alpha = 0.6f)
    else
        Color(0xFFFFFFFF).copy(alpha = 0.9f)

    Row(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = 0.18f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(glassColor)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Wordmark — tapping opens the AI Copilot chat directly
        Text(
            text = "CiteCircle",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FrauncesFamily,
                fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Italic
            ),
            color = MaterialTheme.ccColors.inkNavy,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onAiChatClick)
                .semantics { contentDescription = "Open AI Copilot" }
        )

        // Actions
        IconButton(
            onClick = onSearchClick,
            modifier = Modifier.semantics { contentDescription = "Search" }
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = "Search",
                tint = MaterialTheme.ccColors.inkNavy
            )
        }

        IconButton(
            onClick = onLibraryClick,
            modifier = Modifier.semantics { contentDescription = "Library" }
        ) {
            Icon(
                Icons.Outlined.BookmarkBorder,
                contentDescription = "Library",
                tint = MaterialTheme.ccColors.inkNavy
            )
        }

        Box {
            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier.semantics { contentDescription = "Notifications" }
            ) {
                Icon(
                    Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.ccColors.inkNavy
                )
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

        Spacer(modifier = Modifier.width(4.dp))

        Box {
            IconButton(
                onClick = onMessagesClick,
                modifier = Modifier.semantics { contentDescription = "Messages" }
            ) {
                Icon(
                    Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    tint = MaterialTheme.ccColors.inkNavy
                )
            }
            if (messageCount > 0) {
                CcBadge(
                    count = messageCount,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = 4.dp)
                )
            }
        }
    }
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
    onPublishClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Detect dark mode: dark surface has red channel near 0, light surface is near 1
    val isDark = MaterialTheme.colorScheme.background.red < 0.3f
    val glassColor = if (isDark)
        Color(0xFF1E2B42).copy(alpha = 0.88f)
    else
        Color(0xFFFFFFFF).copy(alpha = 0.82f)

    Row(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.14f),
                spotColor = Color.Black.copy(alpha = 0.22f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(glassColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = currentDestination?.hasRoute(item.route::class) ?: false

            if (item.isPublish) {
                // Center FAB-style Publish button — elevated glowing pill
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            ambientColor = CcColors.HighlighterYellow.copy(alpha = 0.4f),
                            spotColor = CcColors.HighlighterYellow.copy(alpha = 0.5f)
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    CcColors.HighlighterYellow,
                                    CcColors.HighlighterYellow.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .clickable(onClick = onPublishClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Publish new paper or post",
                        tint = CcColors.InkNavy,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                // Regular nav item — NO indicator pill, only icon/text brightness changes
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) CcColors.CircleBlue else MaterialTheme.ccColors.marginGray.copy(alpha = 0.5f),
                    animationSpec = tween(200),
                    label = "nav_icon_color_${item.labelRes}"
                )
                val labelColor by animateColorAsState(
                    targetValue = if (isSelected) CcColors.CircleBlue else MaterialTheme.ccColors.marginGray.copy(alpha = 0.45f),
                    animationSpec = tween(200),
                    label = "nav_label_color_${item.labelRes}"
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.12f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "nav_scale_${item.labelRes}"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            if (!isSelected) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Icon — just brightened, no pill background
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.labelRes,
                        tint = iconColor,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                    )
                    Spacer(Modifier.height(2.dp))
                    // Label — only the text brightens, no background pill
                    if (isSelected) {
                        // Subtle text glow via just full-brightness color
                        Text(
                            text = item.labelRes,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = labelColor
                        )
                    } else {
                        Text(
                            text = item.labelRes,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Normal,
                            color = labelColor
                        )
                    }
                }
            }
        }
    }
}
