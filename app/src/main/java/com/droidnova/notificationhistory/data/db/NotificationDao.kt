package com.droidnova.notificationhistory.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM apps WHERE packageName = :packageName AND isTrashed = 0 LIMIT 1")
    suspend fun getAppByPackage(packageName: String): NotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: NotificationEntity)

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM apps
            WHERE notificationKey = :notificationKey
              AND contentFingerprint = :contentFingerprint
              AND isTrashed = 0
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

    @Query("SELECT * FROM apps WHERE isTrashed = 0 ORDER BY receivedAt DESC")
    suspend fun getAllApps(): List<NotificationEntity>

    @Query(
        """
        SELECT * FROM apps
        WHERE isTrashed = 0
          AND (:packageName IS NULL OR packageName = :packageName)
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
    @Query("SELECT * FROM apps WHERE packageName = :packageName AND isTrashed = 0 ORDER BY receivedAt DESC")
    suspend fun getNotificationsByPackage(packageName: String): List<NotificationEntity>

    @Query("SELECT * FROM apps WHERE packageName = :packageName AND isTrashed = 0 ORDER BY receivedAt DESC")
    fun observeNotificationsByPackage(packageName: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM apps WHERE isTrashed = 0 ORDER BY receivedAt DESC, id DESC LIMIT 1")
    fun observeLatestNotification(): Flow<NotificationEntity?>

    /** Newest active rows for the Home preview; bounded so it stays cheap to observe. */
    @Query("SELECT * FROM apps WHERE isTrashed = 0 ORDER BY receivedAt DESC, id DESC LIMIT :limit")
    fun observeRecentActive(limit: Int): Flow<List<NotificationEntity>>

    @Query(
        """
        SELECT * FROM apps AS summary
        WHERE isTrashed = 0
          AND id = (
            SELECT id FROM apps
            WHERE packageName = summary.packageName
              AND isTrashed = 0
            ORDER BY receivedAt DESC, id DESC
            LIMIT 1
        )
        ORDER BY receivedAt DESC, id DESC
        """
    )
    fun observeLatestNotificationsByApp(): Flow<List<NotificationEntity>>

    @Query(
        "UPDATE apps SET isTrashed = 1, trashedAt = :trashedAt " +
            "WHERE id = :notificationId AND isTrashed = 0"
    )
    suspend fun moveNotificationToTrash(notificationId: Long, trashedAt: Long): Int

    @Query("UPDATE apps SET isTrashed = 1, trashedAt = :trashedAt WHERE isTrashed = 0")
    suspend fun moveAllActiveToTrash(trashedAt: Long): Int

    @Query("DELETE FROM apps WHERE isTrashed = 0 AND receivedAt < :threshold")
    suspend fun deleteActiveNotificationsOlderThan(threshold: Long): Int

    @Query("SELECT * FROM apps WHERE isTrashed = 1 ORDER BY trashedAt DESC, id DESC")
    fun observeTrash(): Flow<List<NotificationEntity>>

    @Query("UPDATE apps SET isTrashed = 0, trashedAt = NULL WHERE id = :notificationId AND isTrashed = 1")
    suspend fun restoreNotification(notificationId: Long): Int

    @Query("UPDATE apps SET isTrashed = 0, trashedAt = NULL WHERE isTrashed = 1")
    suspend fun restoreAllNotifications(): Int

    @Query("DELETE FROM apps WHERE id = :notificationId AND isTrashed = 1")
    suspend fun permanentlyDeleteNotification(notificationId: Long): Int

    @Query("DELETE FROM apps WHERE isTrashed = 1")
    suspend fun emptyTrash(): Int

    // Observe all rows, newest first
    @Query("SELECT * FROM apps WHERE isTrashed = 0 ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    /**
     * Keyset page of conversations, newest first. Each conversation is represented by its
     * latest active notification matching the filters; count covers the same filters.
     */
    @Query(
        """
        SELECT latest.id AS latestId,
               latest.packageName AS packageName,
               latest.conversationKey AS conversationKey,
               latest.conversationName AS conversationName,
               latest.title AS latestTitle,
               latest.message AS latestMessage,
               latest.receivedAt AS latestReceivedAt,
               (
                   SELECT COUNT(*) FROM apps AS counted
                   WHERE counted.conversationKey = latest.conversationKey
                     AND counted.packageName = latest.packageName
                     AND counted.isTrashed = 0
                     AND counted.receivedAt >= :startInclusive
                     AND counted.receivedAt < :endExclusive
                     AND (
                         :searchQuery = ''
                         OR counted.title LIKE '%' || :searchQuery || '%' COLLATE NOCASE
                         OR counted.message LIKE '%' || :searchQuery || '%' COLLATE NOCASE
                         OR counted.packageName LIKE '%' || :searchQuery || '%' COLLATE NOCASE
                         OR counted.conversationName LIKE '%' || :searchQuery || '%' COLLATE NOCASE
                     )
               ) AS messageCount
        FROM apps AS latest
        WHERE latest.isTrashed = 0
          AND latest.conversationKey IS NOT NULL
          AND (:packageName IS NULL OR latest.packageName = :packageName)
          AND latest.receivedAt >= :startInclusive
          AND latest.receivedAt < :endExclusive
          AND (
              :searchQuery = ''
              OR latest.title LIKE '%' || :searchQuery || '%' COLLATE NOCASE
              OR latest.message LIKE '%' || :searchQuery || '%' COLLATE NOCASE
              OR latest.packageName LIKE '%' || :searchQuery || '%' COLLATE NOCASE
              OR latest.conversationName LIKE '%' || :searchQuery || '%' COLLATE NOCASE
          )
          AND (
              :cursorReceivedAt IS NULL
              OR latest.receivedAt < :cursorReceivedAt
              OR (latest.receivedAt = :cursorReceivedAt AND latest.id < :cursorId)
          )
          AND latest.id = (
              SELECT newest.id FROM apps AS newest
              WHERE newest.conversationKey = latest.conversationKey
                AND newest.packageName = latest.packageName
                AND newest.isTrashed = 0
                AND newest.receivedAt >= :startInclusive
                AND newest.receivedAt < :endExclusive
                AND (
                    :searchQuery = ''
                    OR newest.title LIKE '%' || :searchQuery || '%' COLLATE NOCASE
                    OR newest.message LIKE '%' || :searchQuery || '%' COLLATE NOCASE
                    OR newest.packageName LIKE '%' || :searchQuery || '%' COLLATE NOCASE
                    OR newest.conversationName LIKE '%' || :searchQuery || '%' COLLATE NOCASE
                )
              ORDER BY newest.receivedAt DESC, newest.id DESC
              LIMIT 1
          )
        ORDER BY latest.receivedAt DESC, latest.id DESC
        LIMIT :limit
        """
    )
    suspend fun getConversationPage(
        packageName: String?,
        searchQuery: String,
        startInclusive: Long,
        endExclusive: Long,
        cursorReceivedAt: Long?,
        cursorId: Long?,
        limit: Int
    ): List<ConversationSummaryRow>

    /** Keyset page of one conversation's active notifications, newest first. */
    @Query(
        """
        SELECT * FROM apps
        WHERE conversationKey = :conversationKey
          AND isTrashed = 0
          AND (
              :cursorReceivedAt IS NULL
              OR receivedAt < :cursorReceivedAt
              OR (receivedAt = :cursorReceivedAt AND id < :cursorId)
          )
        ORDER BY receivedAt DESC, id DESC
        LIMIT :limit
        """
    )
    suspend fun getConversationMessagesPage(
        conversationKey: String,
        cursorReceivedAt: Long?,
        cursorId: Long?,
        limit: Int
    ): List<NotificationEntity>

    @Query(
        """
        SELECT COUNT(*) AS activeCount,
               COALESCE(MAX(id), 0) AS maxId,
               COALESCE(SUM(id), 0) AS idSum
        FROM apps
        WHERE conversationKey IS NOT NULL AND isTrashed = 0
        """
    )
    fun observeConversationsSignature(): Flow<ConversationChangeSignature>

    @Query(
        """
        SELECT COUNT(*) AS activeCount,
               COALESCE(MAX(id), 0) AS maxId,
               COALESCE(SUM(id), 0) AS idSum
        FROM apps
        WHERE conversationKey = :conversationKey AND isTrashed = 0
        """
    )
    fun observeConversationSignature(conversationKey: String): Flow<ConversationChangeSignature>

    // ---- Insights (M7): every query counts each active row exactly once. ----

    @Query("SELECT MIN(receivedAt) FROM apps WHERE isTrashed = 0")
    fun observeEarliestActiveTimestamp(): Flow<Long?>

    @Query(
        """
        SELECT COUNT(*) AS total, COUNT(DISTINCT packageName) AS activeApps
        FROM apps
        WHERE isTrashed = 0
          AND receivedAt >= :startInclusive
          AND receivedAt < :endExclusive
        """
    )
    fun observeInsightsSummary(startInclusive: Long, endExclusive: Long): Flow<InsightsSummaryRow>

    @Query(
        """
        SELECT packageName, COUNT(*) AS count
        FROM apps
        WHERE isTrashed = 0
          AND receivedAt >= :startInclusive
          AND receivedAt < :endExclusive
        GROUP BY packageName
        ORDER BY count DESC, packageName ASC
        LIMIT :limit
        """
    )
    fun observeTopApps(startInclusive: Long, endExclusive: Long, limit: Int): Flow<List<AppCountRow>>

    /**
     * Bucket boundaries and local-time offsets depend on the zone rules, so the SQL is built
     * by [com.droidnova.notificationhistory.data.insights.InsightsQueries] and passed raw.
     */
    @RawQuery(observedEntities = [NotificationEntity::class])
    fun observeBucketCounts(query: SupportSQLiteQuery): Flow<List<BucketCountRow>>

    @RawQuery(observedEntities = [NotificationEntity::class])
    fun observeBusiestHour(query: SupportSQLiteQuery): Flow<List<HourCountRow>>

}
