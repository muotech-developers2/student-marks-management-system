package com.example.studentmarks.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ClassProfileEntity::class, StudentEntity::class, SubjectEntity::class, TermEntity::class, AssessmentEntity::class, MarkEntity::class],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun classProfileDao(): ClassProfileDao
    abstract fun studentDao(): StudentDao
    abstract fun subjectDao(): SubjectDao
    abstract fun termDao(): TermDao
    abstract fun assessmentDao(): AssessmentDao
    abstract fun markDao(): MarkDao
    abstract fun reportDao(): ReportDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `students` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`profileId` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`isActive` INTEGER NOT NULL, " +
                        "`createdAtMillis` INTEGER NOT NULL, " +
                        "`updatedAtMillis` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`profileId`) REFERENCES `class_profiles`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_students_profileId` ON `students` (`profileId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_students_profileId_isActive_name` " +
                        "ON `students` (`profileId`, `isActive`, `name`)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `subjects` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`profileId` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`isActive` INTEGER NOT NULL, " +
                        "`createdAtMillis` INTEGER NOT NULL, " +
                        "`updatedAtMillis` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`profileId`) REFERENCES `class_profiles`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_subjects_profileId` ON `subjects` (`profileId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_subjects_profileId_isActive_name` " +
                        "ON `subjects` (`profileId`, `isActive`, `name`)",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `terms` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`profileId` INTEGER NOT NULL, " +
                        "`termNumber` INTEGER NOT NULL, " +
                        "`createdAtMillis` INTEGER NOT NULL, " +
                        "`updatedAtMillis` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`profileId`) REFERENCES `class_profiles`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_terms_profileId` ON `terms` (`profileId`)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_terms_profileId_termNumber` ON `terms` (`profileId`, `termNumber`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `assessments` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`profileId` INTEGER NOT NULL, " +
                        "`termId` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`description` TEXT NOT NULL, " +
                        "`sortOrder` INTEGER NOT NULL, " +
                        "`isActive` INTEGER NOT NULL, " +
                        "`createdAtMillis` INTEGER NOT NULL, " +
                        "`updatedAtMillis` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`profileId`) REFERENCES `class_profiles`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`termId`) REFERENCES `terms`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_assessments_profileId` ON `assessments` (`profileId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_assessments_termId` ON `assessments` (`termId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_assessments_profileId_termId_isActive_name` " +
                        "ON `assessments` (`profileId`, `termId`, `isActive`, `name`)",
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `marks` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`profileId` INTEGER NOT NULL, " +
                        "`termId` INTEGER NOT NULL, " +
                        "`assessmentId` INTEGER NOT NULL, " +
                        "`studentId` INTEGER NOT NULL, " +
                        "`subjectId` INTEGER NOT NULL, " +
                        "`markValue` REAL NOT NULL, " +
                        "`createdAtMillis` INTEGER NOT NULL, " +
                        "`updatedAtMillis` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`profileId`) REFERENCES `class_profiles`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`termId`) REFERENCES `terms`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`assessmentId`) REFERENCES `assessments`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`studentId`) REFERENCES `students`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_marks_profileId` ON `marks` (`profileId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_marks_termId` ON `marks` (`termId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_marks_assessmentId` ON `marks` (`assessmentId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_marks_studentId` ON `marks` (`studentId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_marks_subjectId` ON `marks` (`subjectId`)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_marks_profileId_termId_assessmentId_studentId_subjectId` " +
                        "ON `marks` (`profileId`, `termId`, `assessmentId`, `studentId`, `subjectId`)",
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "student_marks.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build().also { instance = it }
            }
    }
}