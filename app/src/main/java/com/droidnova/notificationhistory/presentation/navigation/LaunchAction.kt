package com.droidnova.notificationhistory.presentation.navigation

import android.content.Intent

/**
 * What the launching intent asks the app to open: a static shortcut, the reconnect alert, or a
 * Premium alert (deleted message, keyword).
 */
enum class LaunchAction {
    None, OpenHistory, OpenInsights, OpenSearch, OpenDeleted, Reconnect;

    companion object {
        /** Extra set by res/xml/shortcuts.xml. */
        const val EXTRA_SHORTCUT = "shortcut"
        /** Extra set by the listener-disconnected notification. */
        const val EXTRA_RECONNECT = "reconnect"
        /** Extra set by the Premium alerts; one of the OPEN_ values. */
        const val EXTRA_OPEN = "open"
        const val OPEN_DELETED = "deleted"
        const val OPEN_HISTORY = "history"

        fun from(intent: Intent?): LaunchAction = when {
            intent == null -> None
            intent.getBooleanExtra(EXTRA_RECONNECT, false) -> Reconnect
            intent.getStringExtra(EXTRA_OPEN) == OPEN_DELETED -> OpenDeleted
            intent.getStringExtra(EXTRA_OPEN) == OPEN_HISTORY -> OpenHistory
            else -> when (intent.getStringExtra(EXTRA_SHORTCUT)) {
                "history" -> OpenHistory
                "insights" -> OpenInsights
                "search" -> OpenSearch
                else -> None
            }
        }
    }
}
