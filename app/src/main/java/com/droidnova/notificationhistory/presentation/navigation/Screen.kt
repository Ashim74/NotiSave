package com.droidnova.notificationhistory.presentation.navigation

import android.net.Uri

sealed class Screens(val route: String) {
   data object Home : Screens("home")
   data object History : Screens("History") {
      const val PACKAGE_ARG = "packageName"
      /** Pattern registered in the graph; plain [route] still navigates with no filter. */
      const val ROUTE_PATTERN = "History?packageName={packageName}"
      /** Opens History with a transient app filter (M7 top-app tap). */
      fun createRoute(packageName: String) = "History?packageName=${Uri.encode(packageName)}"
   }
   data object Insights : Screens("Insights")
   data object ManageNotifications : Screens("ManageNotifications")
   data object AppSettings : Screens("AppSettings")
   data object Trash : Screens("Trash")
   data object AboutScreen : Screens("AboutScreen")

   data object AppsNotificationListScreen : Screens("app_notifications/{packageName}") {
      fun createRoute(packageName: String) = "app_notifications/$packageName"
   }
   data object SettingScreen : Screens("SettingScreen/{packageName}") {
      fun createRoute(packageName: String) = "SettingScreen/$packageName"
   }
   data object ConversationDetail : Screens("conversation/{conversationKey}") {
      // Keys contain arbitrary contact/group names, so they must be encoded as a path segment.
      fun createRoute(conversationKey: String) = "conversation/${Uri.encode(conversationKey)}"
   }
}
