package com.tripbudget.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tripbudget.app.data.dao.ContributionDao
import com.tripbudget.app.data.dao.ExpenseDao
import com.tripbudget.app.data.dao.MemberDao
import com.tripbudget.app.data.dao.TripDao
import com.tripbudget.app.data.entity.ContributionEntity
import com.tripbudget.app.data.entity.ExpenseEntity
import com.tripbudget.app.data.entity.MemberEntity
import com.tripbudget.app.data.entity.TripEntity

@Database(
    entities = [
        TripEntity::class,
        MemberEntity::class,
        ContributionEntity::class,
        ExpenseEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class TripBudgetDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun memberDao(): MemberDao
    abstract fun contributionDao(): ContributionDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        fun create(context: Context): TripBudgetDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                TripBudgetDatabase::class.java,
                "trip_budget.db",
            )
                .addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3)
                // Do not use fallbackToDestructiveMigration — that erases user data.
                .build()
        }
    }
}
