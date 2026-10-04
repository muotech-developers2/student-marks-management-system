package com.example.studentmarks.data.repository

import androidx.room.Room
import com.example.studentmarks.data.database.AppDatabase
import com.example.studentmarks.data.database.AssessmentEntity
import com.example.studentmarks.data.database.ClassProfileEntity
import com.example.studentmarks.data.database.MarkEntity
import com.example.studentmarks.data.database.StudentEntity
import com.example.studentmarks.data.database.SubjectEntity
import com.example.studentmarks.data.database.TermEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ReportsRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: ReportsRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = ReportsRepository(
            database.markDao(),
            database.studentDao(),
            database.subjectDao(),
            database.assessmentDao(),
            database.termDao(),
        )
    }

    @Test
    fun reportsRespectProfileTermAndAssessmentIsolation() = runBlocking {
        val profileA = database.classProfileDao().insertProfile(
            ClassProfileEntity(teacherName = "Jane", classGrade = "Grade 1", currentTerm = 1, createdAtMillis = 1L),
        )
        val profileB = database.classProfileDao().insertProfile(
            ClassProfileEntity(teacherName = "John", classGrade = "Grade 2", currentTerm = 1, createdAtMillis = 2L),
        )

        val termA1 = database.termDao().insertTerm(
            TermEntity(profileId = profileA, termNumber = 1, createdAtMillis = 10L, updatedAtMillis = 10L),
        )
        val termA2 = database.termDao().insertTerm(
            TermEntity(profileId = profileA, termNumber = 2, createdAtMillis = 11L, updatedAtMillis = 11L),
        )
        val termB1 = database.termDao().insertTerm(
            TermEntity(profileId = profileB, termNumber = 1, createdAtMillis = 12L, updatedAtMillis = 12L),
        )

        val assessmentA1 = database.assessmentDao().insertAssessment(
            AssessmentEntity(profileId = profileA, termId = termA1, name = "CAT 1", description = "", sortOrder = 0, isActive = true, createdAtMillis = 20L, updatedAtMillis = 20L),
        )
        val assessmentA2 = database.assessmentDao().insertAssessment(
            AssessmentEntity(profileId = profileA, termId = termA2, name = "Midterm", description = "", sortOrder = 0, isActive = true, createdAtMillis = 21L, updatedAtMillis = 21L),
        )
        val assessmentB1 = database.assessmentDao().insertAssessment(
            AssessmentEntity(profileId = profileB, termId = termB1, name = "CAT 1", description = "", sortOrder = 0, isActive = true, createdAtMillis = 22L, updatedAtMillis = 22L),
        )

        val studentA = database.studentDao().insertStudent(
            StudentEntity(profileId = profileA, name = "Brian", isActive = true, createdAtMillis = 30L, updatedAtMillis = 30L),
        )
        val studentB = database.studentDao().insertStudent(
            StudentEntity(profileId = profileB, name = "Mary", isActive = true, createdAtMillis = 31L, updatedAtMillis = 31L),
        )

        val subjectA = database.subjectDao().insertSubject(
            SubjectEntity(profileId = profileA, name = "Mathematics", isActive = true, createdAtMillis = 40L, updatedAtMillis = 40L),
        )
        val subjectB = database.subjectDao().insertSubject(
            SubjectEntity(profileId = profileB, name = "English", isActive = true, createdAtMillis = 41L, updatedAtMillis = 41L),
        )

        database.markDao().insertMark(
            MarkEntity(
                profileId = profileA,
                termId = termA1,
                assessmentId = assessmentA1,
                studentId = studentA,
                subjectId = subjectA,
                markValue = 80.0,
                createdAtMillis = 50L,
                updatedAtMillis = 50L,
            ),
        )
        database.markDao().insertMark(
            MarkEntity(
                profileId = profileA,
                termId = termA2,
                assessmentId = assessmentA2,
                studentId = studentA,
                subjectId = subjectA,
                markValue = 90.0,
                createdAtMillis = 51L,
                updatedAtMillis = 51L,
            ),
        )
        database.markDao().insertMark(
            MarkEntity(
                profileId = profileB,
                termId = termB1,
                assessmentId = assessmentB1,
                studentId = studentB,
                subjectId = subjectB,
                markValue = 55.0,
                createdAtMillis = 52L,
                updatedAtMillis = 52L,
            ),
        )

        val profileAMarks = repository.getMarksForProfileAndTerm(profileA, termA1)
        assertEquals(1, profileAMarks.size)
        assertEquals(studentA, profileAMarks.first().studentId)

        val studentTermMarks = repository.getMarksForStudentTerm(profileA, studentA, termA1)
        assertEquals(1, studentTermMarks.size)
        assertEquals(assessmentA1, studentTermMarks.first().assessmentId)

        val assessmentMarks = repository.getMarksForAssessment(profileA, termA1, assessmentA1)
        assertEquals(1, assessmentMarks.size)
        assertEquals(subjectA, assessmentMarks.first().subjectId)

        assertTrue(repository.getMarksForAssessment(profileA, termA1, assessmentB1).isEmpty())
        assertTrue(repository.getMarksForProfileAndTerm(profileB, termA1).isEmpty())
    }
}
