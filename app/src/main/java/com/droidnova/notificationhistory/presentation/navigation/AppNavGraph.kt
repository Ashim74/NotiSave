package com.droidnova.notificationhistory.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.presentation.screens.applist.AppListScreen
import com.droidnova.notificationhistory.presentation.screens.history.HistoryScreen

import com.droidnova.notificationhistory.presentation.screens.home.HomeScreen
import com.droidnova.notificationhistory.presentation.screens.manage_notification.ManageAppNotificationScreen

@Composable
fun AppNavGraph(){
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel()

    NavHost(navController = navController, startDestination = Screen.Home.route)
    {
        composable(Screen.Home.route) {
            HomeScreen(mainViewModel,navController)
        }
        composable(Screen.History.route) {
            HistoryScreen(mainViewModel)
        }
        composable(Screen.ManageNotifications.route) {
            ManageAppNotificationScreen(mainViewModel)
        }

    }
}