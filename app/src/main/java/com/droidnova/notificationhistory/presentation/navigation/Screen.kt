package com.droidnova.notificationhistory.presentation.navigation

sealed class Screen(val route: String) {
   data object Home : Screen("home")
}