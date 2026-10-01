package com.droidnova.notificationhistory.presentation.navigation

import android.content.Intent

/** What the launching intent asks the app to open: a static shortcut or the reconnect alert. */
enum class LaunchAction {
    None, OpenHistory, OpenInsights, OpenSearch, Reconnect;

    companion object {
        /** Extra set by res/xml/shortcuts.xml. */
        const val EXTRA_SHORTCUT = "shortcut"
        /** Extra set by the listener-disconnected notification. */
        const val EXTRA_RECONNECT = "reconnect"

        fun from(intent: Intent?): LaunchAction = when {
            intent == null -> None
            intent.getBooleanExtra(EXTRA_RECONNECT, false) -> Reconnect
            else -> when (intent.getStringExtra(EXTRA_SHORTCUT)) {
                "history" -> OpenHistory
                "insights" -> OpenInsights
                "search" -> OpenSearch
                else -> None
            }
        }
    }
}
