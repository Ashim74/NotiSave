package com.droidnova.notificationhistory.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.presentation.screens.applist.AppsNotificationListScreen
import com.droidnova.notificationhistory.presentation.screens.about.AboutScreen
import com.droidnova.notificationhistory.presentation.screens.history.HistoryScreen
import com.droidnova.notificationhistory.presentation.screens.home.HomeScreen
import com.droidnova.notificationhistory.presentation.screens.manage_notification.ManageAppNotificationScreen
import com.droidnova.notificationhistory.presentation.screens.setting.SettingScreen
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel()

    val systemUiController = rememberSystemUiController()
    val darkTheme = isSystemInDarkTheme()

    val barColor = if (darkTheme) Color.Black else Color.White

    SideEffect {
        systemUiController.setStatusBarColor(
            color = barColor,
            darkIcons = !darkTheme
        )
        systemUiController.setNavigationBarColor(
            color = barColor,
            darkIcons = !darkTheme
        )
    }

    NavHost(
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
        composable(Screens.History.route) {
            HistoryScreen(mainViewModel, navController)
        }
        composable(Screens.ManageNotifications.route) {
            ManageAppNotificationScreen(mainViewModel, navController)
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
            AppsNotificationListScreen(mainViewModel, packageName, navController)
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
                packageName = pkgName
            )
        }
    }
}