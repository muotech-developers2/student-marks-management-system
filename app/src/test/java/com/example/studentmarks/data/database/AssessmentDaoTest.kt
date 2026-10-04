package com.example.studentmarks.data.database

import androidx.room.Room
import com.example.studentmarks.data.repository.AssessmentRepository
import com.example.studentmarks.data.repository.TermRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AssessmentDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var termRepository: TermRepository
    private lateinit var assessmentRepository: AssessmentRepository

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        termRepository = TermRepository(database.termDao())
        assessmentRepository = AssessmentRepository(database.assessmentDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun termAndAssessmentDataStayProfileAndTermScoped() = runBlocking {
        val profileDao = database.classProfileDao()
        val gradeOne = profileDao.insertProfile(
            ClassProfileEntity(
                teacherName = "John",
                classGrade = "Grade 1",
                currentTerm = 1,
                createdAtMillis = 1L,
            ),
        )
        val gradeTwo = profileDao.insertProfile(
            ClassProfileEntity(
                teacherName = "Jane",
                classGrade = "Grade 2",
                currentTerm = 1,
                createdAtMillis = 2L,
            ),
        )

        val termOneGradeOne = termRepository.getOrCreateTerm(gradeOne, 1)
        val termTwoGradeOne = termRepository.getOrCreateTerm(gradeOne, 2)
        val termOneGradeTwo = termRepository.getOrCreateTerm(gradeTwo, 1)

        val assessmentId = assessmentRepository.addAssessment(
            profileId = gradeOne,
            termId = termOneGradeOne,
            name = "Midterm Exam",
            description = "First test",
            now = 10L,
        )
        assessmentRepository.addAssessment(
            profileId = gradeOne,
            termId = termTwoGradeOne,
            name = "Opening Exam",
            description = "Start of term",
            now = 11L,
        )
        assessmentRepository.addAssessment(
            profileId = gradeTwo,
            termId = termOneGradeTwo,
            name = "CAT 1",
            description = "Class test",
            now = 12L,
        )

        assertEquals(listOf("Midterm Exam"), assessmentRepository.searchAssessments(gradeOne, termOneGradeOne, "midterm").first().map { it.name })
        assertEquals(listOf("Opening Exam"), assessmentRepository.searchAssessments(gradeOne, termTwoGradeOne, "opening").first().map { it.name })
        assertTrue(assessmentRepository.hasDuplicateName(gradeOne, termOneGradeOne, " midterm exam "))
        assertFalse(assessmentRepository.hasDuplicateName(gradeOne, termOneGradeOne, "CAT 1"))
        assertFalse(assessmentRepository.hasDuplicateName(gradeTwo, termOneGradeTwo, "Midterm Exam"))
        assertEquals(3, termRepository.getTermsForProfile(gradeOne).size)
    }

    @Test
    fun updateDeactivateAndDeleteAssessmentKeepIdentity() = runBlocking {
        val profileId = database.classProfileDao().insertProfile(
            ClassProfileEntity(
                teacherName = "John",
                classGrade = "Grade 1",
                currentTerm = 1,
                createdAtMillis = 1L,
            ),
        )
        val termId = termRepository.getOrCreateTerm(profileId, 1)
        val assessmentId = assessmentRepository.addAssessment(
            profileId = profileId,
            termId = termId,
            name = "CAT 1",
            description = "First assessment",
            now = 20L,
        )

        assertEquals(1, assessmentRepository.updateAssessmentName(assessmentId, profileId, termId, "CAT One", "Updated", 21L))
        val updated = database.assessmentDao().getAssessment(assessmentId, profileId, termId)
        assertEquals(assessmentId, updated?.id)
        assertEquals(profileId, updated?.profileId)
        assertEquals(termId, updated?.termId)
        assertEquals("CAT One", updated?.name)

        assessmentRepository.setAssessmentActive(assessmentId, profileId, termId, false, 22L)
        assertEquals(0, assessmentRepository.observeCount(profileId, termId, true).first())
        assertEquals(1, assessmentRepository.observeCount(profileId, termId, false).first())
        assertTrue(assessmentRepository.observeAssessments(profileId, termId, true).first().isEmpty())

        assessmentRepository.setAssessmentActive(assessmentId, profileId, termId, true, 23L)
        assertEquals(1, assessmentRepository.observeCount(profileId, termId, true).first())
        assertEquals(1, assessmentRepository.deleteAssessment(assessmentId, profileId, termId))
        assertEquals(null, database.assessmentDao().getAssessment(assessmentId, profileId, termId))
    }
}
