package com.example.studentmarks.data.database

import android.content.Context
import androidx.room.Room
import com.example.studentmarks.data.repository.ClassProfileRepository
import com.example.studentmarks.ui.AppViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ClassProfileDaoTest {
    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() {
        RuntimeEnvironment.getApplication()
            .getSharedPreferences("student_marks_preferences", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun profilesHaveIndependentGeneratedIds() = runBlocking {
        val dao = database.classProfileDao()
        val firstId = dao.insertProfile(
            ClassProfileEntity(
                teacherName = "John Mwangi",
                classGrade = "Grade 1",
                currentTerm = 1,
                createdAtMillis = 1L,
            ),
        )
        val secondId = dao.insertProfile(
            ClassProfileEntity(
                teacherName = "John Mwangi",
                classGrade = "Grade 2",
                currentTerm = 2,
                createdAtMillis = 2L,
            ),
        )

        val profiles = dao.getAllProfiles()

        assertNotEquals(firstId, secondId)
        assertEquals(2, profiles.size)
        assertEquals(firstId, profiles[0].id)
        assertEquals("Grade 1", profiles[0].classGrade)
        assertEquals(1, profiles[0].currentTerm)
        assertEquals(secondId, profiles[1].id)
        assertEquals("Grade 2", profiles[1].classGrade)
        assertEquals(2, profiles[1].currentTerm)
    }

    @Test
    fun setupRejectsWhitespaceNamesAndMissingTerm() = runBlocking {
        val dao = database.classProfileDao()
        val viewModel = AppViewModel(
            ClassProfileRepository(dao, RuntimeEnvironment.getApplication()),
        )

        viewModel.onTeacherNameChanged("   ")
        viewModel.onClassGradeChanged("\t")
        viewModel.submitProfile()

        val form = viewModel.formState.value
        assertEquals("Please enter the teacher name.", form.teacherNameError)
        assertEquals("Please enter the class or grade.", form.classGradeError)
        assertEquals("Please select a term.", form.currentTermError)
        assertEquals(0, dao.getAllProfiles().size)
    }

    @Test
    fun editDeleteAndActiveSelectionAreScopedToProfileId() = runBlocking {
        val dao = database.classProfileDao()
        val context = RuntimeEnvironment.getApplication()
        val repository = ClassProfileRepository(dao, context)
        val firstId = repository.createProfile(
            ClassProfileEntity(
                teacherName = "John",
                classGrade = "Grade 1",
                currentTerm = 1,
                createdAtMillis = 1L,
            ),
        )
        val secondId = repository.createProfile(
            ClassProfileEntity(
                teacherName = "John",
                classGrade = "Grade 2",
                currentTerm = 1,
                createdAtMillis = 2L,
            ),
        )
        repository.setActiveProfileId(secondId)

        val relaunchedRepository = ClassProfileRepository(dao, context)
        assertEquals(secondId, relaunchedRepository.activeProfileId.value)
        assertEquals(1, relaunchedRepository.updateProfile(firstId, "John", "Grade 1 Updated", 2))

        val firstProfile = dao.getProfileById(firstId)
        val secondProfile = dao.getProfileById(secondId)
        assertEquals("Grade 1 Updated", firstProfile?.classGrade)
        assertEquals(2, firstProfile?.currentTerm)
        assertEquals("Grade 2", secondProfile?.classGrade)
        assertEquals(1, secondProfile?.currentTerm)

        assertEquals(1, relaunchedRepository.deleteProfile(firstId))
        assertEquals(null, dao.getProfileById(firstId))
        assertEquals("Grade 2", dao.getProfileById(secondId)?.classGrade)
        assertEquals(secondId, relaunchedRepository.activeProfileId.value)
    }

}