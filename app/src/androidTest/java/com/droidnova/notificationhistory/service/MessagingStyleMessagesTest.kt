package com.droidnova.notificationhistory.service

import android.app.Notification
import android.app.Person
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on every API level the app supports; on Android 8–10 this is the case the platform
 * parser couldn't handle. On Android 11+ it is also checked against the platform parser.
 */
@RunWith(AndroidJUnit4::class)
class MessagingStyleMessagesTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Suppress("DEPRECATION")
    private fun chatNotification(): Notification {
        val style = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Notification.MessagingStyle(Person.Builder().setName("Me").build())
                .addMessage("Hi there", 1_000L, Person.Builder().setName("Asha").build())
                .addMessage("On my way", 2_000L, null as Person?)
        } else {
            Notification.MessagingStyle("Me")
                .addMessage("Hi there", 1_000L, "Asha")
                .addMessage("On my way", 2_000L, null as CharSequence?)
        }
        return Notification.Builder(context, "test")
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setStyle(style)
            .build()
    }

    @Test
    fun readsEveryMessageWithSenderAndTime() {
        val messages = readMessagingStyleMessages(chatNotification().extras)

        assertEquals(2, messages.size)
        assertEquals("Hi there", messages[0].text.toString())
        assertEquals("Asha", messages[0].sender.toString())
        assertEquals(1_000L, messages[0].timestamp)
        assertEquals("On my way", messages[1].text.toString())
        // Sent by the user: no sender.
        assertNull(messages[1].sender)
    }

    @Test
    fun matchesThePlatformParserWhereItExists() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val extras = chatNotification().extras
        val platform = Notification.MessagingStyle.Message.getMessagesFromBundleArray(
            extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        )
        val ours = readMessagingStyleMessages(extras)

        assertEquals(platform.size, ours.size)
        platform.zip(ours).forEach { (expected, actual) ->
            assertEquals(expected.text.toString(), actual.text.toString())
            assertEquals(expected.timestamp, actual.timestamp)
            assertEquals(expected.senderPerson?.name?.toString(), actual.sender?.toString())
        }
    }

    @Test
    fun missingOrMalformedExtrasGiveNoMessages() {
        assertTrue(readMessagingStyleMessages(null).isEmpty())
        assertTrue(readMessagingStyleMessages(android.os.Bundle()).isEmpty())
    }
}
