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

    @Query("DELETE FROM apps")
    suspend fun deleteAllNotifications()

    // Observe all rows, newest first
    @Query("SELECT * FROM apps ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<NotificationEntity>>



}