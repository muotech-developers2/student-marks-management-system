package com.example.studentmarks.data.database

import androidx.room.Room
import com.example.studentmarks.data.repository.MarkRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MarkDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: MarkRepository

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = MarkRepository(database.markDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun markForProfileTermAssessmentStudentAndSubjectIsUnique() = runBlocking {
        val profileId = database.classProfileDao().insertProfile(
            ClassProfileEntity(
                teacherName = "John",
                classGrade = "Grade 1",
                currentTerm = 1,
                createdAtMillis = 1L,
            ),
        )
        val termId = database.termDao().insertTerm(
            TermEntity(profileId = profileId, termNumber = 1, createdAtMillis = 2L, updatedAtMillis = 2L),
        )
        val assessmentId = database.assessmentDao().insertAssessment(
            AssessmentEntity(
                profileId = profileId,
                termId = termId,
                name = "Midterm",
                description = "Midterm test",
                sortOrder = 0,
                isActive = true,
                createdAtMillis = 3L,
                updatedAtMillis = 3L,
            ),
        )
        val studentId = database.studentDao().insertStudent(
            StudentEntity(profileId = profileId, name = "Brian", isActive = true, createdAtMillis = 4L, updatedAtMillis = 4L),
        )
        val subjectId = database.subjectDao().insertSubject(
            SubjectEntity(profileId = profileId, name = "Mathematics", isActive = true, createdAtMillis = 5L, updatedAtMillis = 5L),
        )

        val id = repository.upsertMark(
            profileId = profileId,
            termId = termId,
            assessmentId = assessmentId,
            studentId = studentId,
            subjectId = subjectId,
            value = 78.0,
            now = 10L,
        )

        val updatedId = repository.upsertMark(
            profileId = profileId,
            termId = termId,
            assessmentId = assessmentId,
            studentId = studentId,
            subjectId = subjectId,
            value = 85.0,
            now = 11L,
        )

        assertEquals(id, updatedId)
        val row = database.markDao().getMarkForStudentSubjectAssessment(profileId, termId, assessmentId, studentId, subjectId)
        assertNotNull(row)
        assertEquals(85.0, row!!.markValue, 0.0)
        assertEquals(1, repository.getMarksForAssessment(profileId, termId, assessmentId, subjectId).size)
    }

    @Test
    fun zeroAndMissingMarksAreDistinct() = runBlocking {
        val profileId = database.classProfileDao().insertProfile(
            ClassProfileEntity(
                teacherName = "John",
                classGrade = "Grade 1",
                currentTerm = 1,
                createdAtMillis = 1L,
            ),
        )
        val termId = database.termDao().insertTerm(
            TermEntity(profileId = profileId, termNumber = 1, createdAtMillis = 2L, updatedAtMillis = 2L),
        )
        val assessmentId = database.assessmentDao().insertAssessment(
            AssessmentEntity(
                profileId = profileId,
                termId = termId,
                name = "CAT 1",
                description = "Test",
                sortOrder = 0,
                isActive = true,
                createdAtMillis = 3L,
                updatedAtMillis = 3L,
            ),
        )
        val studentZero = database.studentDao().insertStudent(
            StudentEntity(profileId = profileId, name = "John", isActive = true, createdAtMillis = 4L, updatedAtMillis = 4L),
        )
        val studentMissing = database.studentDao().insertStudent(
            StudentEntity(profileId = profileId, name = "Mary", isActive = true, createdAtMillis = 5L, updatedAtMillis = 5L),
        )
        val subjectId = database.subjectDao().insertSubject(
            SubjectEntity(profileId = profileId, name = "Mathematics", isActive = true, createdAtMillis = 6L, updatedAtMillis = 6L),
        )

        repository.upsertMark(profileId, termId, assessmentId, studentZero, subjectId, 0.0, 10L)
        assertEquals(0.0, database.markDao().getMarkForStudentSubjectAssessment(profileId, termId, assessmentId, studentZero, subjectId)!!.markValue, 0.0)
        assertNull(database.markDao().getMarkForStudentSubjectAssessment(profileId, termId, assessmentId, studentMissing, subjectId))
        assertEquals(1, repository.getCompletedCount(profileId, termId, assessmentId, subjectId))
    }
}
