package com.droidnova.notificationhistory.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun historyPagingWalksEveryRowOnceWithTiedTimestamps() = runBlocking {
        // Several rows share a timestamp: the (receivedAt, id) cursor must not skip or repeat.
        repeat(7) { i -> insert("com.a", "t$i", receivedAt = 1_000L + (i / 3)) }

        val seen = mutableListOf<Long>()
        var cursorAt: Long? = null
        var cursorId: Long? = null
        while (true) {
            val page = dao.getHistoryPage(
                packageName = null, searchQuery = "",
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
            packageName = "com.a", searchQuery = "hello",
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
    fun recentActiveIsNewestFirstAndBounded() = runBlocking {
        repeat(8) { i -> insert("com.a", "n$i", receivedAt = i.toLong()) }
        val recent = dao.observeRecentActive(limit = 5).first()
        assertEquals(listOf("n7", "n6", "n5", "n4", "n3"), recent.map { it.title })
    }
}
