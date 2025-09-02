package com.droidnova.notificationhistory.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.presentation.screens.about.AboutScreen
import com.droidnova.notificationhistory.presentation.screens.history.HistoryScreen
import com.droidnova.notificationhistory.presentation.screens.home.HomeScreen
import com.droidnova.notificationhistory.presentation.screens.manage_notification.ManageAppNotificationScreen

@Composable
fun AppNavGraph(){
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel()

    val enter = fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing))
    val exit = fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
    val popEnter = fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing))
    val popExit = fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))


    NavHost(
        navController = navController,
        startDestination = Screens.Home.route,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        }
    ) {
        composable(Screens.Home.route) {
            HomeScreen(mainViewModel,navController)
        }
        composable(Screens.History.route) {
            HistoryScreen(mainViewModel)
        }
        composable(Screens.ManageNotifications.route) {
            ManageAppNotificationScreen(mainViewModel)
        }
        composable(Screens.AboutScreen.route) {
            AboutScreen(navController)
        }
    }
}