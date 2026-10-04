package com.example.studentmarks.data.database

import androidx.room.Room
import com.example.studentmarks.data.repository.SubjectRepository
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
class SubjectDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: SubjectRepository

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = SubjectRepository(database.subjectDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun searchAndDuplicateChecksStayWithinProfile() = runBlocking {
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

        repository.addSubject(gradeOne, "Mathematics", 10L)
        repository.addSubject(gradeOne, "English", 11L)
        repository.addSubject(gradeOne, "Social Studies", 12L)
        repository.addSubject(gradeTwo, "Mathematics", 13L)
        repository.addSubject(gradeTwo, "Kiswahili", 14L)

        val gradeOneSearch = repository.searchSubjects(gradeOne, true, " math ").first()
        val gradeTwoSearch = repository.searchSubjects(gradeTwo, true, "math").first()

        assertEquals(listOf("Mathematics"), gradeOneSearch.map { it.name })
        assertEquals(listOf("Mathematics"), gradeTwoSearch.map { it.name })
        assertTrue(repository.hasDuplicateName(gradeOne, " mathematics "))
        assertFalse(repository.hasDuplicateName(gradeOne, "Kiswahili"))
        assertFalse(repository.hasDuplicateName(gradeTwo, "English"))
    }

    @Test
    fun editStatusAndDeletePreserveSubjectIdentityAndCounts() = runBlocking {
        val profileId = database.classProfileDao().insertProfile(
            ClassProfileEntity(
                teacherName = "John",
                classGrade = "Grade 1",
                currentTerm = 1,
                createdAtMillis = 1L,
            ),
        )
        val otherProfileId = database.classProfileDao().insertProfile(
            ClassProfileEntity(
                teacherName = "John",
                classGrade = "Grade 2",
                currentTerm = 1,
                createdAtMillis = 2L,
            ),
        )

        val subjectId = repository.addSubject(profileId, "Science", 20L)
        val otherSubjectId = repository.addSubject(otherProfileId, "Science", 21L)

        assertEquals(1, repository.updateSubjectName(subjectId, profileId, "Biology", 22L))
        val edited = database.subjectDao().getSubject(subjectId, profileId)
        assertEquals(subjectId, edited?.id)
        assertEquals(profileId, edited?.profileId)
        assertEquals("Biology", edited?.name)

        repository.setSubjectActive(subjectId, profileId, false, 23L)
        assertEquals(0, repository.observeCount(profileId, true).first())
        assertEquals(1, repository.observeCount(profileId, false).first())
        assertEquals("Biology", repository.observeSubjects(profileId, false).first().single().name)

        repository.setSubjectActive(subjectId, profileId, true, 24L)
        assertEquals(1, repository.observeCount(profileId, true).first())
        assertEquals(1, repository.deleteSubject(subjectId, profileId))
        assertEquals(null, database.subjectDao().getSubject(subjectId, profileId))
        assertEquals("Science", database.subjectDao().getSubject(otherSubjectId, otherProfileId)?.name)
    }
}
