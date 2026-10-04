package com.example.studentmarks.data.database

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigrationTest {
    @Test
    fun migrationToStudentsTableKeepsExistingProfiles() = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication()
        val databaseName = "student_marks_migration_test.db"
        context.deleteDatabase(databaseName)

        val versionOneDatabase = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        versionOneDatabase.execSQL(
            "CREATE TABLE IF NOT EXISTS `class_profiles` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`teacherName` TEXT NOT NULL, `classGrade` TEXT NOT NULL, " +
                "`currentTerm` INTEGER NOT NULL, `createdAtMillis` INTEGER NOT NULL)",
        )
        versionOneDatabase.execSQL(
            "INSERT INTO `class_profiles` (`id`, `teacherName`, `classGrade`, `currentTerm`, `createdAtMillis`) " +
                "VALUES (7, 'John', 'Grade 1', 1, 100)",
        )
        versionOneDatabase.version = 1
        versionOneDatabase.close()

        val upgradedDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            databaseName,
        ).addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
            .allowMainThreadQueries()
            .build()

        try {
            val profile = upgradedDatabase.classProfileDao().getProfileById(7L)
            assertEquals("Grade 1", profile?.classGrade)
            val studentId = upgradedDatabase.studentDao().insertStudent(
                StudentEntity(
                    profileId = 7L,
                    name = "Brian Mwangi",
                    createdAtMillis = 101L,
                    updatedAtMillis = 101L,
                ),
            )
            val subjectId = upgradedDatabase.subjectDao().insertSubject(
                SubjectEntity(
                    profileId = 7L,
                    name = "Mathematics",
                    createdAtMillis = 102L,
                    updatedAtMillis = 102L,
                ),
            )
            assertEquals("Brian Mwangi", upgradedDatabase.studentDao().getStudent(studentId, 7L)?.name)
            assertEquals("Mathematics", upgradedDatabase.subjectDao().getSubject(subjectId, 7L)?.name)
        } finally {
            upgradedDatabase.close()
            context.deleteDatabase(databaseName)
        }
    }
}