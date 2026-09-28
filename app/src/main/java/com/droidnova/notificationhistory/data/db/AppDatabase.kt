package com.droidnova.notificationhistory.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [NotificationEntity::class], version = 2)
abstract class AppDatabase: RoomDatabase() {
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notification_db"
                ).addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `apps` ADD COLUMN `notificationKey` TEXT")
                db.execSQL("ALTER TABLE `apps` ADD COLUMN `contentFingerprint` TEXT")
                db.execSQL("ALTER TABLE `apps` ADD COLUMN `conversationTitle` TEXT")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_apps_receivedAt` " +
                        "ON `apps` (`receivedAt`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_apps_packageName_receivedAt` " +
                        "ON `apps` (`packageName`, `receivedAt`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                        "`index_apps_notificationKey_contentFingerprint_receivedAt` " +
                        "ON `apps` (`notificationKey`, `contentFingerprint`, `receivedAt`)"
                )
            }
        }
    }
}
