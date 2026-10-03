package com.droidnova.notificationhistory.presentation.navigation

import android.net.Uri

sealed class Screens(val route: String) {
   data object Onboarding : Screens("onboarding")
   data object Home : Screens("home")
   data object History : Screens("History") {
      const val PACKAGE_ARG = "packageName"
      /** Pattern registered in the graph; plain [route] still navigates with no filter. */
      const val ROUTE_PATTERN = "History?packageName={packageName}"
      /** Opens History with a transient app filter (Insights top-app tap). */
      fun createRoute(packageName: String) = "History?packageName=${Uri.encode(packageName)}"
   }
   data object Insights : Screens("Insights")
   data object ManageNotifications : Screens("ManageNotifications")
   data object AppSettings : Screens("AppSettings")
   data object Trash : Screens("Trash")
   data object AboutScreen : Screens("AboutScreen")
   data object AppLock : Screens("AppLock")
   data object DeletedMessages : Screens("DeletedMessages")
   data object KeywordAlerts : Screens("KeywordAlerts")
   data object HiddenApps : Screens("HiddenApps")

   // Package names are dotted identifiers today, but encoding keeps the path segment safe
   // against any future arg (and matches how conversation keys are handled).
   data object AppsNotificationListScreen : Screens("app_notifications/{packageName}") {
      fun createRoute(packageName: String) = "app_notifications/${Uri.encode(packageName)}"
   }
   data object SettingScreen : Screens("SettingScreen/{packageName}") {
      fun createRoute(packageName: String) = "SettingScreen/${Uri.encode(packageName)}"
   }
   data object ConversationDetail : Screens("conversation/{conversationKey}") {
      // Keys contain arbitrary contact/group names, so they must be encoded as a path segment.
      fun createRoute(conversationKey: String) = "conversation/${Uri.encode(conversationKey)}"
   }

   companion object {
      /** Destinations shown in the bottom navigation bar; everything else is a pushed screen. */
      val TOP_LEVEL_ROUTES: Set<String> = setOf(
         Home.route,
         History.ROUTE_PATTERN,
         Insights.route,
         AppSettings.route
      )
   }
}
