package com.tripbudget.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * All schema changes must ship with an explicit Migration here.
 * Never wipe user data with fallbackToDestructiveMigration.
 */
object Migrations {
    /**
     * v1 → v2: allow null [paidByMemberId] so shared-fund expenses need no payer.
     * SQLite cannot alter nullability in place, so the expenses table is rebuilt.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `expenses_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `tripId` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `amountPaise` INTEGER NOT NULL,
                    `category` TEXT NOT NULL,
                    `dateEpochDay` INTEGER NOT NULL,
                    `paidByMemberId` INTEGER,
                    `paymentSource` TEXT NOT NULL,
                    `notes` TEXT,
                    FOREIGN KEY(`tripId`) REFERENCES `trips`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`paidByMemberId`) REFERENCES `members`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO `expenses_new` (
                    `id`, `tripId`, `title`, `amountPaise`, `category`,
                    `dateEpochDay`, `paidByMemberId`, `paymentSource`, `notes`
                )
                SELECT
                    `id`, `tripId`, `title`, `amountPaise`, `category`,
                    `dateEpochDay`, `paidByMemberId`, `paymentSource`, `notes`
                FROM `expenses`
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `expenses`")
            db.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_expenses_tripId` ON `expenses` (`tripId`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_expenses_paidByMemberId` ON `expenses` (`paidByMemberId`)",
            )
        }
    }

    /** v2 → v3: optional trip date range (start/end epoch days). */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `trips` ADD COLUMN `startDateEpochDay` INTEGER")
            db.execSQL("ALTER TABLE `trips` ADD COLUMN `endDateEpochDay` INTEGER")
        }
    }

    /** v3 → v4: Supabase sync fields (remoteId + updatedAt) on all entities. */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `trips` ADD COLUMN `remoteId` TEXT")
            db.execSQL(
                "ALTER TABLE `trips` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_trips_remoteId` ON `trips` (`remoteId`)",
            )

            db.execSQL("ALTER TABLE `members` ADD COLUMN `remoteId` TEXT")
            db.execSQL(
                "ALTER TABLE `members` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_members_remoteId` ON `members` (`remoteId`)",
            )

            db.execSQL("ALTER TABLE `contributions` ADD COLUMN `remoteId` TEXT")
            db.execSQL(
                "ALTER TABLE `contributions` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_contributions_remoteId` ON `contributions` (`remoteId`)",
            )

            db.execSQL("ALTER TABLE `expenses` ADD COLUMN `remoteId` TEXT")
            db.execSQL(
                "ALTER TABLE `expenses` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_expenses_remoteId` ON `expenses` (`remoteId`)",
            )
        }
    }

    /** v4 → v5: trip sharing fields (remote owner + invite code). */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `trips` ADD COLUMN `remoteOwnerId` TEXT")
            db.execSQL("ALTER TABLE `trips` ADD COLUMN `inviteCode` TEXT")
        }
    }

    /** v5 → v6: route stops + trip GPS tracking flag. */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `trips` ADD COLUMN `routeTrackingActive` INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `route_stops` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `tripId` INTEGER NOT NULL,
                    `sortOrder` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `subtitle` TEXT,
                    `lat` REAL NOT NULL,
                    `lng` REAL NOT NULL,
                    `osmPlaceId` TEXT,
                    `status` TEXT NOT NULL,
                    `note` TEXT,
                    `remoteId` TEXT,
                    `updatedAt` INTEGER NOT NULL,
                    FOREIGN KEY(`tripId`) REFERENCES `trips`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_route_stops_tripId` ON `route_stops` (`tripId`)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_route_stops_remoteId` ON `route_stops` (`remoteId`)",
            )
        }
    }

    /** v6 → v7: trip place coordinates for header weather. */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `trips` ADD COLUMN `placeLat` REAL")
            db.execSQL("ALTER TABLE `trips` ADD COLUMN `placeLng` REAL")
        }
    }

    /** v7 → v8: optional device-local cover photo for the dashboard hero. */
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `trips` ADD COLUMN `coverImagePath` TEXT")
        }
    }

    /** v8 → v9: cloud object key for the shared hero photo. */
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `trips` ADD COLUMN `coverObjectKey` TEXT")
        }
    }
}
