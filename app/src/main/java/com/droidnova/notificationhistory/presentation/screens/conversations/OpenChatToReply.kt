package com.droidnova.notificationhistory.presentation.screens.conversations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.presentation.components.ReplyNoticeDialog
import com.droidnova.notificationhistory.presentation.navigation.Screens

/**
 * Returns the action for a Reply tap: opens the chat, but the very first time shows
 * [ReplyNoticeDialog] and only continues once the user taps "Got it".
 */
@Composable
fun rememberOpenChatToReply(
    mainViewModel: MainViewModel,
    navController: NavController
): (conversationKey: String) -> Unit {
    val noticeShown by mainViewModel.replyNoticeShown.collectAsState()
    var pendingKey by remember { mutableStateOf<String?>(null) }
    val openChat = { key: String ->
        navController.navigate(Screens.ConversationDetail.createRoute(key))
    }

    pendingKey?.let { key ->
        ReplyNoticeDialog(
            onGotIt = {
                mainViewModel.markReplyNoticeShown()
                pendingKey = null
                openChat(key)
            },
            // Closing it without "Got it" keeps the notice for the next tap.
            onDismiss = { pendingKey = null }
        )
    }

    return { key -> if (noticeShown) openChat(key) else pendingKey = key }
}
