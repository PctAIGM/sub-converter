package com.subconverter.data

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SubscriptionSourceEntity::class,
        TemplateEntity::class,
        OutputProfileEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subscriptionSourceDao(): SubscriptionSourceDao
    abstract fun templateDao(): TemplateDao
    abstract fun outputProfileDao(): OutputProfileDao

    companion object {
        val Migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE templates ADD COLUMN remoteUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE templates ADD COLUMN lastRefreshAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE templates ADD COLUMN lastError TEXT NOT NULL DEFAULT ''")
            }
        }

        val Migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE subscription_sources ADD COLUMN userAgent TEXT NOT NULL DEFAULT 'ClashforWindows/0.20.39'",
                )
            }
        }

        val Migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE subscription_sources ADD COLUMN website TEXT NOT NULL DEFAULT ''")
            }
        }

        val Migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE output_profiles ADD COLUMN fetchCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        val Migration5To6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE templates ADD COLUMN enabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE templates ADD COLUMN global INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE templates ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE templates SET sortOrder = id")
                db.execSQL("ALTER TABLE output_profiles ADD COLUMN overrideIds TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
