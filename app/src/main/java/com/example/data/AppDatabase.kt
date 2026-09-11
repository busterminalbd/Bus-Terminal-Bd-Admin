package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        FoodBillEntity::class,
        AdvanceSalaryEntity::class,
        MedicalRecordEntity::class,
        CodeGroupEntity::class,
        CodeGroupItemEntity::class,
        PresetMedicalCodeEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodBillDao(): FoodBillDao
    abstract fun advanceSalaryDao(): AdvanceSalaryDao
    abstract fun medicalDao(): MedicalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food_bills ADD COLUMN centerName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE food_bills ADD COLUMN subtitle TEXT NOT NULL DEFAULT ''")
            }
        }

        // Version 3 briefly added a "Medical Work" feature (now removed) with its own
        // tables; it made no changes to food_bills itself, so users still on version 2
        // can jump straight to version 4 with no schema change needed.
        private val MIGRATION_2_4 = object : Migration(2, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No food_bills schema change between v2 and v4.
            }
        }

        // Users who briefly installed the version-3 build will have the now-unused
        // Medical tables on disk; drop them cleanly instead of leaving orphaned tables
        // or wiping the whole database via fallbackToDestructiveMigration.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS medical_records")
                db.execSQL("DROP TABLE IF EXISTS code_groups")
                db.execSQL("DROP TABLE IF EXISTS code_group_items")
                db.execSQL("DROP TABLE IF EXISTS preset_medical_codes")
            }
        }

        // Adds the "ধরন" (bill type) field so a bill remembers whether it's a market
        // list or a transport-fare memo, and re-opens with the matching form.
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food_bills ADD COLUMN billType TEXT NOT NULL DEFAULT 'market'")
            }
        }

        // Adds a "showSignature" flag so a bill remembers whether the signature
        // box/line should be printed, or hidden for quick informal memos.
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food_bills ADD COLUMN showSignature INTEGER NOT NULL DEFAULT 1")
            }
        }

        // Adds the "advance_salaries" table for Advance Salary Application tool
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `advance_salaries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `companyName` TEXT NOT NULL DEFAULT '',
                        `companySubtitle` TEXT NOT NULL DEFAULT '',
                        `applicationNo` TEXT NOT NULL DEFAULT '',
                        `dateString` TEXT NOT NULL DEFAULT '',
                        `applicantName` TEXT NOT NULL DEFAULT '',
                        `employeeId` TEXT NOT NULL DEFAULT '',
                        `designation` TEXT NOT NULL DEFAULT '',
                        `department` TEXT NOT NULL DEFAULT '',
                        `contactNumber` TEXT NOT NULL DEFAULT '',
                        `monthlySalary` REAL NOT NULL DEFAULT 0.0,
                        `advanceAmount` REAL NOT NULL DEFAULT 0.0,
                        `advanceAmountInWords` TEXT NOT NULL DEFAULT '',
                        `reason` TEXT NOT NULL DEFAULT '',
                        `repaymentType` TEXT NOT NULL DEFAULT 'one_time',
                        `installmentCount` INTEGER NOT NULL DEFAULT 1,
                        `installmentAmountPerMonth` REAL NOT NULL DEFAULT 0.0,
                        `deductionStartMonth` TEXT NOT NULL DEFAULT '',
                        `previousAdvancePending` REAL NOT NULL DEFAULT 0.0,
                        `guarantorOrRecommendedBy` TEXT NOT NULL DEFAULT '',
                        `remarks` TEXT NOT NULL DEFAULT '',
                        `status` TEXT NOT NULL DEFAULT 'APPROVED',
                        `showSignatures` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        // Re-adds the Medical tables for Medical Work Report tool
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `medical_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `patientId` TEXT NOT NULL,
                        `code` TEXT NOT NULL,
                        `patientName` TEXT NOT NULL DEFAULT '',
                        `timestamp` INTEGER NOT NULL DEFAULT 0,
                        `notes` TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `code_groups` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `groupName` TEXT NOT NULL,
                        `description` TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `code_group_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `groupId` INTEGER NOT NULL,
                        `code` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `preset_medical_codes` (
                        `code` TEXT PRIMARY KEY NOT NULL,
                        `name` TEXT NOT NULL DEFAULT '',
                        `category` TEXT NOT NULL DEFAULT 'General'
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "albaraka_food_bill_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_4, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    // Safety net only: if some other unexpected version gap is hit,
                    // fall back to a clean database rather than crashing on launch.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
