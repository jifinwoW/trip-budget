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
}
