package com.droidnova.notificationhistory.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.ads.CollapsibleAdBanner
import com.droidnova.notificationhistory.presentation.screens.app_history.AppHistoryScreen
import com.droidnova.notificationhistory.presentation.screens.app_settings.AppSettingsScreen
import com.droidnova.notificationhistory.presentation.screens.about.AboutScreen
import com.droidnova.notificationhistory.presentation.screens.conversations.ConversationDetailScreen
import com.droidnova.notificationhistory.presentation.screens.history.HistoryScreen
import com.droidnova.notificationhistory.presentation.screens.home.HomeScreen
import com.droidnova.notificationhistory.presentation.screens.insights.InsightsScreen
import com.droidnova.notificationhistory.presentation.screens.insights.InsightsViewModel
import com.droidnova.notificationhistory.presentation.screens.select_app.SelectAppScreen
import com.droidnova.notificationhistory.presentation.screens.setting.SettingScreen
import com.droidnova.notificationhistory.presentation.screens.trash.TrashScreen
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel()
    val isPremium by mainViewModel.isPremium.collectAsState()

    Column(
        modifier = Modifier.padding(WindowInsets.navigationBars.asPaddingValues())
    ){
        NavHost(
            modifier = Modifier.weight(1f),
            navController = navController,
            startDestination = Screens.Home.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                )
            }
        ) {
            composable(Screens.Home.route) {
                HomeScreen(mainViewModel, navController)
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
            ) { backStackEntry ->
                HistoryScreen(
                    mainViewmodel = mainViewModel,
                    navController = navController,
                    initialPackageFilter = backStackEntry.arguments
                        ?.getString(Screens.History.PACKAGE_ARG)
                )
            }
            composable(Screens.Insights.route) {
                val insightsViewModel: InsightsViewModel = viewModel()
                InsightsScreen(insightsViewModel, navController)
            }
            composable(Screens.ManageNotifications.route) {
                SelectAppScreen(mainViewModel, navController)
            }
            composable(Screens.AppSettings.route) {
                AppSettingsScreen(mainViewModel, navController)
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
            ) { backStackEntry ->
                val packageName =
                    backStackEntry.arguments?.getString("packageName") ?: return@composable
                AppHistoryScreen(mainViewModel, packageName, navController)
            }

            composable(
                route = Screens.ConversationDetail.route,
                arguments = listOf(navArgument("conversationKey") { type = NavType.StringType })
            ) { backStackEntry ->
                val conversationKey =
                    backStackEntry.arguments?.getString("conversationKey") ?: return@composable
                ConversationDetailScreen(mainViewModel, conversationKey, navController)
            }

            composable(
                route = Screens.SettingScreen.route,
                arguments = listOf(
                    navArgument("packageName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val pkgName = backStackEntry.arguments?.getString("packageName").orEmpty()
                SettingScreen(
                    navController = navController,
                    packageName = pkgName,
                    mainViewModel = mainViewModel
                )
            }
        }
        if (!isPremium) {
            CollapsibleAdBanner()
        }
    }
}
