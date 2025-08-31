package com.droidnova.notificationhistory.presentation.navigation

sealed class Screen(val route: String) {
   data object Home : Screen("home")
   data object History : Screen("History")
   data object ManageNotifications : Screen("ManageNotifications")
   data object AboutScreen : Screen("AboutScreen")
}