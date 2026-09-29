package com.droidnova.notificationhistory.presentation.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import com.droidnova.notificationhistory.presentation.screens.about.AboutScreen
import com.droidnova.notificationhistory.presentation.screens.app_history.AppHistoryScreen
import com.droidnova.notificationhistory.presentation.screens.app_settings.AppSettingsScreen
import com.droidnova.notificationhistory.presentation.screens.conversations.ConversationDetailScreen
import com.droidnova.notificationhistory.presentation.screens.history.HistoryScreen
import com.droidnova.notificationhistory.presentation.screens.home.HomeScreen
import com.droidnova.notificationhistory.presentation.screens.home.HomeViewModel
import com.droidnova.notificationhistory.presentation.screens.insights.InsightsScreen
import com.droidnova.notificationhistory.presentation.screens.insights.InsightsViewModel
import com.droidnova.notificationhistory.presentation.screens.onboarding.OnboardingScreen
import com.droidnova.notificationhistory.presentation.screens.select_app.SelectAppScreen
import com.droidnova.notificationhistory.presentation.screens.setting.SettingScreen
import com.droidnova.notificationhistory.presentation.screens.trash.TrashScreen

private const val TRANSITION_MS = 300
private const val TAB_FADE_MS = 200

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
    @StringRes val labelRes: Int,
    val iconVector: ImageVector? = null,
    @DrawableRes val iconRes: Int? = null
) {
    Home(Screens.Home.route, R.string.nav_home, iconVector = Icons.Default.Home),
    History(Screens.History.ROUTE_PATTERN, R.string.history_title, iconRes = R.drawable.ic_history),
    Insights(Screens.Insights.route, R.string.insights_title, iconRes = R.drawable.ic_insights),
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
        fadeIn(tween(TAB_FADE_MS))
    } else {
        slideInHorizontally(
            initialOffsetX = { if (forward) it else -it },
            animationSpec = tween(TRANSITION_MS)
        )
    }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(forward: Boolean): ExitTransition =
    if (isTabSwitch()) {
        fadeOut(tween(TAB_FADE_MS))
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
                    InsightsScreen(insightsViewModel, navController)
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
        if (!isPremium) {
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

@Composable
private fun AppNavigationBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            val label = stringResource(destination.labelRes)
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onNavigate(destination.navigateRoute) },
                icon = {
                    when {
                        destination.iconVector != null ->
                            Icon(destination.iconVector, contentDescription = null)
                        destination.iconRes != null ->
                            Icon(painterResource(destination.iconRes), contentDescription = null)
                    }
                },
                label = { Text(label) }
            )
        }
    }
}
