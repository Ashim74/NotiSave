package com.droidnova.notificationhistory.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.presentation.screens.about.AboutScreen
import com.droidnova.notificationhistory.presentation.screens.applist.AppListScreen
import com.droidnova.notificationhistory.presentation.screens.history.HistoryScreen

import com.droidnova.notificationhistory.presentation.screens.home.HomeScreen
import com.droidnova.notificationhistory.presentation.screens.manage_notification.ManageAppNotificationScreen

@Composable
fun AppNavGraph(){
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel()

    NavHost(navController = navController, startDestination = Screens.Home.route)
    {
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