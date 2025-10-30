package com.droidnova.notificationhistory.presentation.navigation

sealed class Screens(val route: String) {
   data object Home : Screens("home")
   data object History : Screens("History")
   data object ManageNotifications : Screens("ManageNotifications")
   data object AboutScreen : Screens("AboutScreen")

   data object AppsNotificationListScreen : Screens("app_notifications/{packageName}") {
      fun createRoute(packageName: String) = "app_notifications/$packageName"
   }
   data object SettingScreen : Screens("SettingScreen/{packageName}") {
      fun createRoute(packageName: String) = "SettingScreen/$packageName"
   }
}