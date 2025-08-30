package com.droidnova.notificationhistory.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotificationDao {
    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppByPackage(packageName: String): NotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: NotificationEntity)

    @Query("SELECT * FROM apps ORDER BY receivedAt DESC")
    suspend fun getAllApps(): List<NotificationEntity>

    

}