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

    @Query("SELECT * FROM apps ORDER BY receivedAt DESC")
    suspend fun getAllApps(): List<NotificationEntity>

    // Fetch a page of notifications, newest first
    @Query("SELECT * FROM apps ORDER BY receivedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getNotifications(limit: Int, offset: Int): List<NotificationEntity>

    // Fetch notifications for a specific package, newest first
    @Query("SELECT * FROM apps WHERE packageName = :packageName ORDER BY receivedAt DESC")
    suspend fun getNotificationsByPackage(packageName: String): List<NotificationEntity>

    @Query("DELETE FROM apps")
    suspend fun deleteAllNotifications()

    // Observe all rows, newest first
    @Query("SELECT * FROM apps ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

}