package com.droidnova.notificationhistory.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "migration-1-2-test"
    private var database: AppDatabase? = null

    @Before
    fun setUp() {
        context.deleteDatabase(databaseName)
    }

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationFrom1To2PreservesRowsAndAddsIdentitySchema() {
        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { legacyDb ->
            legacyDb.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `apps` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `packageName` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `message` TEXT NOT NULL,
                    `receivedAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            legacyDb.execSQL(
                """
                INSERT INTO `apps` (`id`, `packageName`, `title`, `message`, `receivedAt`)
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(
                    42L,
                    "com.example.app",
                    "Original title",
                    "Original message",
                    123456789L
                )
            )
            legacyDb.version = 1
        }

        database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        val migratedDb = checkNotNull(database).openHelper.writableDatabase
        migratedDb.query(
            """
            SELECT id, packageName, title, message, receivedAt,
                   notificationKey, contentFingerprint, conversationTitle
            FROM apps
            """.trimIndent()
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(42L, cursor.getLong(0))
            assertEquals("com.example.app", cursor.getString(1))
            assertEquals("Original title", cursor.getString(2))
            assertEquals("Original message", cursor.getString(3))
            assertEquals(123456789L, cursor.getLong(4))
            assertNull(cursor.getString(5))
            assertNull(cursor.getString(6))
            assertNull(cursor.getString(7))
            assertFalse(cursor.moveToNext())
        }

        val indexNames = mutableSetOf<String>()
        migratedDb.query("PRAGMA index_list(`apps`)").use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) indexNames += cursor.getString(nameColumn)
        }
        assertTrue("index_apps_receivedAt" in indexNames)
        assertTrue("index_apps_packageName_receivedAt" in indexNames)
        assertTrue("index_apps_notificationKey_contentFingerprint_receivedAt" in indexNames)
    }

    @Test
    fun persistentDuplicateLookupRequiresSameKeyAndFingerprintWithinWindow() = runBlocking {
        database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        val dao = checkNotNull(database).notificationDao()
        val receivedAt = 1_000_000L

        dao.insertApp(
            NotificationEntity(
                packageName = "com.example.app",
                title = "Title",
                message = "Message",
                receivedAt = receivedAt,
                notificationKey = "stable-key",
                contentFingerprint = "fingerprint"
            )
        )

        assertTrue(
            dao.hasRecentDuplicate("stable-key", "fingerprint", receivedAt - 1L, receivedAt)
        )
        assertFalse(dao.hasRecentDuplicate("stable-key", "changed", receivedAt - 1L, receivedAt))
        assertFalse(
            dao.hasRecentDuplicate("different-key", "fingerprint", receivedAt - 1L, receivedAt)
        )
        assertFalse(
            dao.hasRecentDuplicate("stable-key", "fingerprint", receivedAt + 1L, receivedAt + 2L)
        )
    }
}
