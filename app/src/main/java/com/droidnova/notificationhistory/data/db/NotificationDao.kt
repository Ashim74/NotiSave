package com.droidnova.notificationhistory.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppByPackage(packageName: String): NotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: NotificationEntity)

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM apps
            WHERE notificationKey = :notificationKey
              AND contentFingerprint = :contentFingerprint
              AND receivedAt BETWEEN :since AND :until
            LIMIT 1
        )
        """
    )
    suspend fun hasRecentDuplicate(
        notificationKey: String,
        contentFingerprint: String,
        since: Long,
        until: Long
    ): Boolean

    @Query("SELECT * FROM apps ORDER BY receivedAt DESC")
    suspend fun getAllApps(): List<NotificationEntity>

    @Query(
        """
        SELECT * FROM apps
        WHERE (:packageName IS NULL OR packageName = :packageName)
          AND (
              :searchQuery = ''
              OR title LIKE '%' || :searchQuery || '%' COLLATE NOCASE
              OR message LIKE '%' || :searchQuery || '%' COLLATE NOCASE
              OR packageName LIKE '%' || :searchQuery || '%' COLLATE NOCASE
          )
          AND receivedAt >= :startInclusive
          AND receivedAt < :endExclusive
          AND (
              :cursorReceivedAt IS NULL
              OR receivedAt < :cursorReceivedAt
              OR (receivedAt = :cursorReceivedAt AND id < :cursorId)
          )
        ORDER BY receivedAt DESC, id DESC
        LIMIT :limit
        """
    )
    suspend fun getHistoryPage(
        packageName: String?,
        searchQuery: String,
        startInclusive: Long,
        endExclusive: Long,
        cursorReceivedAt: Long?,
        cursorId: Long?,
        limit: Int
    ): List<NotificationEntity>

    // Fetch notifications for a specific package, newest first
    @Query("SELECT * FROM apps WHERE packageName = :packageName ORDER BY receivedAt DESC")
    suspend fun getNotificationsByPackage(packageName: String): List<NotificationEntity>

    @Query("SELECT * FROM apps WHERE packageName = :packageName ORDER BY receivedAt DESC")
    fun observeNotificationsByPackage(packageName: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM apps ORDER BY receivedAt DESC, id DESC LIMIT 1")
    fun observeLatestNotification(): Flow<NotificationEntity?>

    @Query(
        """
        SELECT * FROM apps AS summary
        WHERE id = (
            SELECT id FROM apps
            WHERE packageName = summary.packageName
            ORDER BY receivedAt DESC, id DESC
            LIMIT 1
        )
        ORDER BY receivedAt DESC, id DESC
        """
    )
    fun observeLatestNotificationsByApp(): Flow<List<NotificationEntity>>

    @Query("DELETE FROM apps WHERE id = :notificationId")
    suspend fun deleteNotificationById(notificationId: Long)

    @Query("DELETE FROM apps")
    suspend fun deleteAllNotifications()

    @Query("DELETE FROM apps WHERE receivedAt < :threshold")
    suspend fun deleteNotificationsOlderThan(threshold: Long): Int

    // Observe all rows, newest first
    @Query("SELECT * FROM apps ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

}
