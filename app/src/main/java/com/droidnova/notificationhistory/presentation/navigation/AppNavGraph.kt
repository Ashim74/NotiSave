package com.droidnova.notificationhistory.presentation.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.droidnova.notificationhistory.presentation.components.pressScale
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.ads.CollapsibleAdBanner
import com.droidnova.notificationhistory.ads.rememberShowAds
import com.droidnova.notificationhistory.presentation.screens.about.AboutScreen
import com.droidnova.notificationhistory.presentation.screens.alerts.KeywordAlertsScreen
import com.droidnova.notificationhistory.presentation.screens.deleted.DeletedMessagesScreen
import com.droidnova.notificationhistory.presentation.screens.hidden.HiddenAppsScreen
import com.droidnova.notificationhistory.presentation.screens.app_history.AppHistoryScreen
import com.droidnova.notificationhistory.presentation.screens.app_settings.AppSettingsScreen
import com.droidnova.notificationhistory.presentation.screens.conversations.ConversationDetailScreen
import com.droidnova.notificationhistory.presentation.screens.history.HistoryScreen
import com.droidnova.notificationhistory.presentation.screens.home.HomeScreen
import com.droidnova.notificationhistory.presentation.screens.home.HomeViewModel
import com.droidnova.notificationhistory.presentation.screens.insights.InsightsScreen
import com.droidnova.notificationhistory.presentation.screens.insights.InsightsViewModel
import com.droidnova.notificationhistory.presentation.screens.lock.AppLockSettingsScreen
import com.droidnova.notificationhistory.presentation.screens.onboarding.OnboardingScreen
import com.droidnova.notificationhistory.presentation.screens.select_app.SelectAppScreen
import com.droidnova.notificationhistory.presentation.screens.setting.SettingScreen
import com.droidnova.notificationhistory.presentation.screens.trash.TrashScreen

private const val TRANSITION_MS = 300
// Material "fade through" for tab switches: the old tab fades out quickly, then the new one
// fades in while scaling up slightly, so the swap reads as one motion instead of a cross-dissolve.
private const val TAB_FADE_OUT_MS = 90
private const val TAB_FADE_IN_MS = 210
private const val TAB_SCALE_FROM = 0.96f

/**
 * Switches to a bottom-navigation tab: single instance, state saved/restored per tab, back
 * always returns to Home. [restoreState] = false forces fresh arguments (Insights → filtered History).
 */
fun NavController.navigateToTab(route: String, restoreState: Boolean = true) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        this.restoreState = restoreState
    }
}

private enum class TopLevelDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val iconVector: ImageVector? = null,
    @param:DrawableRes val iconRes: Int? = null
) {
    Home(Screens.Home.route, R.string.nav_home, iconVector = Icons.Default.Home),
    History(Screens.History.ROUTE_PATTERN, R.string.history_title, iconRes = R.drawable.ic_nh_history),
    Insights(Screens.Insights.route, R.string.insights_title, iconRes = R.drawable.ic_nh_insights),
    Settings(Screens.AppSettings.route, R.string.settings_title, iconVector = Icons.Default.Settings);

    /** The route to navigate to; History's pattern must be navigated without its arg. */
    val navigateRoute: String
        get() = if (this == History) Screens.History.route else route
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
    initialState.destination.route in Screens.TOP_LEVEL_ROUTES &&
        targetState.destination.route in Screens.TOP_LEVEL_ROUTES

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enter(forward: Boolean): EnterTransition =
    if (isTabSwitch()) {
        val spec = tween<Float>(TAB_FADE_IN_MS, delayMillis = TAB_FADE_OUT_MS, easing = LinearOutSlowInEasing)
        fadeIn(spec) + scaleIn(spec, initialScale = TAB_SCALE_FROM)
    } else {
        slideInHorizontally(
            initialOffsetX = { if (forward) it else -it },
            animationSpec = tween(TRANSITION_MS)
        )
    }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(forward: Boolean): ExitTransition =
    if (isTabSwitch()) {
        fadeOut(tween(TAB_FADE_OUT_MS, easing = FastOutLinearInEasing))
    } else {
        slideOutHorizontally(
            targetOffsetX = { if (forward) -it else it },
            animationSpec = tween(TRANSITION_MS)
        )
    }

@Composable
fun AppNavGraph(launchAction: LaunchAction = LaunchAction.None) {
    val mainViewModel: MainViewModel = viewModel()
    // null: DataStore not read yet (splash is still covering the window).
    val onboardingComplete by mainViewModel.onboardingComplete.collectAsState()
    when (onboardingComplete) {
        null -> return
        false -> OnboardingScreen(
            mainViewModel = mainViewModel,
            onFinished = mainViewModel::completeOnboarding
        )
        // Completing onboarding flips the flag, which composes the main shell fresh with Home
        // as its start destination; no cross-graph navigation needed.
        true -> MainShell(mainViewModel, launchAction)
    }
}

@Composable
private fun MainShell(mainViewModel: MainViewModel, launchAction: LaunchAction) {
    val navController = rememberNavController()
    val isPremium by mainViewModel.isPremium.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = currentRoute in Screens.TOP_LEVEL_ROUTES

    // Shortcut / alert intents are honored once per process, after the graph exists.
    var launchHandled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(launchAction) {
        if (launchHandled) return@LaunchedEffect
        launchHandled = true
        when (launchAction) {
            LaunchAction.OpenHistory -> navController.navigateToTab(Screens.History.route)
            LaunchAction.OpenInsights -> navController.navigateToTab(Screens.Insights.route)
            LaunchAction.OpenSearch -> {
                mainViewModel.requestHistorySearchFocus()
                navController.navigateToTab(Screens.History.route)
            }
            LaunchAction.OpenDeleted -> navController.navigate(Screens.DeletedMessages.route)
            LaunchAction.Reconnect, LaunchAction.None -> Unit
        }
    }

    Column(
        // The NavigationBar pads itself for the gesture bar; without it the column must.
        modifier = if (isTopLevel) Modifier else Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                // Screens below sit above the bar/banner, so they must not pad for the inset again.
                .consumeWindowInsets(WindowInsets.navigationBars)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screens.Home.route,
                enterTransition = { enter(forward = true) },
                exitTransition = { exit(forward = true) },
                popEnterTransition = { enter(forward = false) },
                popExitTransition = { exit(forward = false) }
            ) {
                composable(Screens.Home.route) {
                    val homeViewModel: HomeViewModel = viewModel()
                    HomeScreen(mainViewModel, homeViewModel, navController)
                }
                composable(
                    route = Screens.History.ROUTE_PATTERN,
                    arguments = listOf(
                        navArgument(Screens.History.PACKAGE_ARG) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { entry ->
                    HistoryScreen(
                        mainViewmodel = mainViewModel,
                        navController = navController,
                        initialPackageFilter = entry.arguments?.getString(Screens.History.PACKAGE_ARG)
                    )
                }
                composable(Screens.Insights.route) {
                    val insightsViewModel: InsightsViewModel = viewModel()
                    InsightsScreen(insightsViewModel, navController, mainViewModel)
                }
                composable(Screens.AppSettings.route) {
                    AppSettingsScreen(mainViewModel, navController)
                }

                composable(Screens.ManageNotifications.route) {
                    SelectAppScreen(mainViewModel, navController)
                }
                composable(Screens.Trash.route) {
                    TrashScreen(mainViewModel, navController)
                }
                composable(Screens.AboutScreen.route) {
                    AboutScreen(navController)
                }
                composable(Screens.AppLock.route) {
                    AppLockSettingsScreen(navController, mainViewModel)
                }
                composable(Screens.DeletedMessages.route) {
                    DeletedMessagesScreen(navController, mainViewModel)
                }
                composable(Screens.KeywordAlerts.route) {
                    KeywordAlertsScreen(navController, mainViewModel)
                }
                composable(Screens.HiddenApps.route) {
                    HiddenAppsScreen(navController, mainViewModel)
                }
                composable(
                    route = Screens.AppsNotificationListScreen.route,
                    arguments = listOf(navArgument("packageName") { type = NavType.StringType })
                ) { entry ->
                    val packageName = entry.arguments?.getString("packageName") ?: return@composable
                    AppHistoryScreen(mainViewModel, packageName, navController)
                }
                composable(
                    route = Screens.ConversationDetail.route,
                    arguments = listOf(navArgument("conversationKey") { type = NavType.StringType })
                ) { entry ->
                    val conversationKey =
                        entry.arguments?.getString("conversationKey") ?: return@composable
                    ConversationDetailScreen(mainViewModel, conversationKey, navController)
                }
                composable(
                    route = Screens.SettingScreen.route,
                    arguments = listOf(navArgument("packageName") { type = NavType.StringType })
                ) { entry ->
                    SettingScreen(
                        navController = navController,
                        packageName = entry.arguments?.getString("packageName").orEmpty(),
                        mainViewModel = mainViewModel
                    )
                }
            }
        }
        // Hidden for premium users and during an ad-free period earned from a rewarded ad
        if (rememberShowAds(isPremium)) {
            CollapsibleAdBanner()
        }
        if (isTopLevel) {
            AppNavigationBar(
                currentRoute = currentRoute,
                onNavigate = { route -> navController.navigateToTab(route) }
            )
        }
    }
}

/**
 * Compact bottom bar: four icons on a raised surface. The selected tab grows into a tinted pill
 * with its label, with a springy bounce, so the bar stays short and needs little reading.
 */
@Composable
private fun AppNavigationBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(64.dp)
                .padding(horizontal = 10.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopLevelDestination.entries.forEach { destination ->
                NavPill(
                    destination = destination,
                    selected = currentRoute == destination.route,
                    onClick = { onNavigate(destination.navigateRoute) }
                )
            }
        }
    }
}

@Composable
private fun NavPill(destination: TopLevelDestination, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val label = stringResource(destination.labelRes)
    val interaction = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        if (selected) colors.primaryContainer.copy(alpha = 0.75f) else Color.Transparent,
        label = "navContainer"
    )
    val content by animateColorAsState(
        if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        label = "navContent"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "navIconScale"
    )
    Row(
        modifier = Modifier
            .pressScale(interaction, 0.9f)
            .clip(RoundedCornerShape(50))
            .background(container)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interaction,
                indication = ripple()
            )
            .semantics { contentDescription = label }
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconModifier = Modifier
            .size(22.dp)
            .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
        when {
            destination.iconVector != null ->
                Icon(destination.iconVector, contentDescription = null, tint = content, modifier = iconModifier)
            destination.iconRes != null ->
                Icon(painterResource(destination.iconRes), contentDescription = null, tint = content, modifier = iconModifier)
        }
        if (selected) {
            Text(
                text = label,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = content,
                maxLines = 1
            )
        }
    }
}
