package com.droidnova.notificationhistory.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: NotificationDao

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.notificationDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insert(pkg: String, title: String, receivedAt: Long) {
        dao.insertApp(
            NotificationEntity(packageName = pkg, title = title, message = "m", receivedAt = receivedAt)
        )
    }

    @Test
    fun ownReplyIsFoundOnlyForKeylessRowsOfThatChat() = runBlocking {
        val chat = "com.whatsapp|s:alice"
        dao.insertApp(
            NotificationEntity(
                packageName = "com.whatsapp", title = "You", message = "On my way",
                receivedAt = 1_000, conversationKey = chat
            )
        )
        // The contact saying the same words is a captured row (with a key), never an echo.
        dao.insertApp(
            NotificationEntity(
                packageName = "com.whatsapp", title = "Alice", message = "Same here",
                receivedAt = 1_000, notificationKey = "k", conversationKey = chat
            )
        )

        assertTrue(dao.hasOwnReplyBetween(chat, "On my way", from = 900, to = 1_100))
        assertFalse(dao.hasOwnReplyBetween(chat, "On my way", from = 1_001, to = 2_000))
        assertFalse(dao.hasOwnReplyBetween("com.whatsapp|s:bob", "On my way", from = 900, to = 1_100))
        assertFalse(dao.hasOwnReplyBetween(chat, "Same here", from = 900, to = 1_100))
    }

    @Test
    fun historyPagingWalksEveryRowOnceWithTiedTimestamps() = runBlocking {
        // Several rows share a timestamp: the (receivedAt, id) cursor must not skip or repeat.
        repeat(7) { i -> insert("com.a", "t$i", receivedAt = 1_000L + (i / 3)) }

        val seen = mutableListOf<Long>()
        var cursorAt: Long? = null
        var cursorId: Long? = null
        while (true) {
            val page = dao.getHistoryPage(
                packageName = null, excludedPackages = emptyList(), searchQuery = "",
                startInclusive = Long.MIN_VALUE, endExclusive = Long.MAX_VALUE,
                cursorReceivedAt = cursorAt, cursorId = cursorId, limit = 3
            )
            if (page.isEmpty()) break
            seen += page.map { it.id }
            cursorAt = page.last().receivedAt
            cursorId = page.last().id
        }
        assertEquals(7, seen.size)
        assertEquals(7, seen.toSet().size)
    }

    @Test
    fun historyPageAppliesPackageSearchAndDateFilters() = runBlocking {
        insert("com.a", "Hello world", 100)
        insert("com.a", "Other", 200)
        insert("com.b", "Hello again", 300)

        val page = dao.getHistoryPage(
            packageName = "com.a", excludedPackages = emptyList(), searchQuery = "hello",
            startInclusive = 50, endExclusive = 250,
            cursorReceivedAt = null, cursorId = null, limit = 10
        )
        assertEquals(listOf("Hello world"), page.map { it.title })
    }

    @Test
    fun trashedRowsLeaveHistoryAndPurgeByTrashedAt() = runBlocking {
        insert("com.a", "keep", 100)
        insert("com.a", "old trash", 200)
        insert("com.a", "new trash", 300)
        val rows = dao.getAllApps().associateBy { it.title }
        dao.moveNotificationToTrash(rows.getValue("old trash").id, trashedAt = 1_000)
        dao.moveNotificationToTrash(rows.getValue("new trash").id, trashedAt = 5_000)

        assertEquals(listOf("keep"), dao.getAllApps().map { it.title })

        val purged = dao.deleteTrashedBefore(threshold = 2_000)
        assertEquals(1, purged)
        assertEquals(listOf("new trash"), dao.observeTrash().first().map { it.title })
    }

    @Test
    fun activeRetentionNeverTouchesTrash() = runBlocking {
        insert("com.a", "old active", 100)
        insert("com.a", "old trashed", 100)
        val trashed = dao.getAllApps().first { it.title == "old trashed" }
        dao.moveNotificationToTrash(trashed.id, trashedAt = 150)

        dao.deleteActiveNotificationsOlderThan(threshold = 1_000)

        assertTrue(dao.getAllApps().isEmpty())
        assertEquals(1, dao.observeTrash().first().size)
    }

    @Test
    fun deleteAllRemovesActiveAndTrashedRows() = runBlocking {
        insert("com.a", "active", 100)
        insert("com.b", "trashed", 200)
        val trashed = dao.getAllApps().first { it.title == "trashed" }
        dao.moveNotificationToTrash(trashed.id, trashedAt = 300)

        assertEquals(2, dao.deleteAllNotifications())

        assertTrue(dao.getAllApps().isEmpty())
        assertTrue(dao.observeTrash().first().isEmpty())
    }

    @Test
    fun hiddenAppsLeaveHistoryAndTopApps() = runBlocking {
        insert("com.a", "visible", 100)
        insert("com.secret", "hidden", 200)
        insert("com.secret", "hidden again", 300)

        val page = dao.getHistoryPage(
            packageName = null, excludedPackages = listOf("com.secret"), searchQuery = "",
            startInclusive = Long.MIN_VALUE, endExclusive = Long.MAX_VALUE,
            cursorReceivedAt = null, cursorId = null, limit = 10
        )
        assertEquals(listOf("visible"), page.map { it.title })
        assertEquals(
            listOf("com.a"),
            dao.observeTopApps(0, Long.MAX_VALUE, 5, listOf("com.secret")).first().map { it.packageName }
        )
    }

    @Test
    fun deletedMessageIsFoundByKeyAndTimeAndMarkedOnce() = runBlocking {
        dao.insertApp(
            NotificationEntity(
                packageName = "com.whatsapp", title = "Ali", message = "secret plan",
                receivedAt = 500, notificationKey = "thread"
            )
        )
        val original = checkNotNull(dao.findStandingMessage("thread", 500))
        assertEquals(1, dao.markDeleted(original.id, deletedAt = 900))
        // A second re-post of the same deletion finds nothing left to mark.
        assertEquals(null, dao.findStandingMessage("thread", 500))
        assertEquals(0, dao.markDeleted(original.id, deletedAt = 950))

        val deleted = dao.observeDeleted().first()
        assertEquals(listOf("secret plan"), deleted.map { it.message })
        assertEquals(900L, deleted.single().deletedAt)
    }
}
