package com.example.studentmarks.data.database

import androidx.room.Room
import com.example.studentmarks.data.repository.StudentRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class StudentDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: StudentRepository

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = StudentRepository(database.studentDao())
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
                teacherName = "John",
                classGrade = "Grade 2",
                currentTerm = 1,
                createdAtMillis = 2L,
            ),
        )
        repository.addStudent(gradeOne, "Brian Mwangi", 1L)
        repository.addStudent(gradeOne, "Mary Wanjiku", 2L)
        repository.addStudent(gradeOne, "Brian Otieno", 3L)
        repository.addStudent(gradeOne, "Élodie Martin", 6L)
        repository.addStudent(gradeTwo, "Peter Kamau", 4L)
        repository.addStudent(gradeTwo, "Brian Mwangi", 5L)

        val gradeOneSearch = repository.searchStudents(gradeOne, true, " bRiAn ").first()
        val gradeTwoSearch = repository.searchStudents(gradeTwo, true, "brian").first()

        assertEquals(listOf("Brian Mwangi", "Brian Otieno"), gradeOneSearch.map { it.name })
        assertEquals(listOf("Brian Mwangi"), gradeTwoSearch.map { it.name })
        assertEquals(
            listOf("Élodie Martin"),
            repository.searchStudents(gradeOne, true, " éLODIE ").first().map { it.name },
        )
        assertTrue(repository.hasDuplicateName(gradeOne, " brian mwangi "))
        assertFalse(repository.hasDuplicateName(gradeOne, "Peter Kamau"))
        assertFalse(repository.hasDuplicateName(gradeTwo, "Mary Wanjiku"))
        assertNotEquals(gradeOne, gradeTwo)
    }

    @Test
    fun editStatusAndDeletePreserveStudentIdentityAndCounts() = runBlocking {
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
        val studentId = repository.addStudent(profileId, "Brian Mwangi", 10L)
        val otherStudentId = repository.addStudent(otherProfileId, "Peter Kamau", 11L)

        assertEquals(1, repository.updateStudentName(studentId, profileId, "Brian Maina", 12L))
        val edited = database.studentDao().getStudent(studentId, profileId)
        assertEquals(studentId, edited?.id)
        assertEquals(profileId, edited?.profileId)
        assertEquals("Brian Maina", edited?.name)

        repository.setStudentActive(studentId, profileId, false, 13L)
        assertEquals(0, repository.observeCount(profileId, true).first())
        assertEquals(1, repository.observeCount(profileId, false).first())
        assertEquals(0, repository.observeStudents(profileId, true).first().size)
        assertEquals("Brian Maina", repository.observeStudents(profileId, false).first().single().name)

        repository.setStudentActive(studentId, profileId, true, 14L)
        assertEquals(1, repository.observeCount(profileId, true).first())
        assertEquals(0, repository.observeCount(profileId, false).first())
        assertEquals(1, repository.deleteStudent(studentId, profileId))
        assertEquals(null, database.studentDao().getStudent(studentId, profileId))
        assertEquals("Peter Kamau", database.studentDao().getStudent(otherStudentId, otherProfileId)?.name)
    }
}