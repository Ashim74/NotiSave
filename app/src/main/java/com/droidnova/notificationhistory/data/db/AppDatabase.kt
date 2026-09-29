package com.droidnova.notificationhistory.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [NotificationEntity::class], version = 4)
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `apps` ADD COLUMN `isTrashed` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL("ALTER TABLE `apps` ADD COLUMN `trashedAt` INTEGER")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_apps_isTrashed_receivedAt_id` " +
                        "ON `apps` (`isTrashed`, `receivedAt`, `id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_apps_isTrashed_trashedAt_id` " +
                        "ON `apps` (`isTrashed`, `trashedAt`, `id`)"
                )
            }
        }

        /**
         * Adds nullable conversation metadata. Existing rows keep every value and stay
         * unclassified (NULL): legacy data cannot identify conversations reliably.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `apps` ADD COLUMN `conversationKey` TEXT")
                db.execSQL("ALTER TABLE `apps` ADD COLUMN `conversationName` TEXT")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                        "`index_apps_conversationKey_isTrashed_receivedAt_id` " +
                        "ON `apps` (`conversationKey`, `isTrashed`, `receivedAt`, `id`)"
                )
            }
        }
    }
}
