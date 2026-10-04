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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ExcelExportRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: ExcelExportRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()

        repository = ExcelExportRepository(
            markDao = database.markDao(),
            studentDao = database.studentDao(),
            subjectDao = database.subjectDao(),
            assessmentDao = database.assessmentDao(),
            termDao = database.termDao(),
            profileDao = database.classProfileDao(),
        )
    }

    @Test
    fun studentExportContainsStudentSubjectMarksAndMissingValues() = runBlocking {
        val profileId = database.classProfileDao().insertProfile(
            ClassProfileEntity(teacherName = "Brian", classGrade = "Grade 1", currentTerm = 1, createdAtMillis = 1L),
        )
        val termId = database.termDao().insertTerm(
            TermEntity(profileId = profileId, termNumber = 1, createdAtMillis = 2L, updatedAtMillis = 2L),
        )
        val assessmentId = database.assessmentDao().insertAssessment(
            AssessmentEntity(profileId = profileId, termId = termId, name = "Midterm", description = "", sortOrder = 0, isActive = true, createdAtMillis = 3L, updatedAtMillis = 3L),
        )
        val studentId = database.studentDao().insertStudent(
            StudentEntity(profileId = profileId, name = "Mary", isActive = true, createdAtMillis = 4L, updatedAtMillis = 4L),
        )
        val mathematicsId = database.subjectDao().insertSubject(
            SubjectEntity(profileId = profileId, name = "Mathematics", isActive = true, createdAtMillis = 5L, updatedAtMillis = 5L),
        )
        val englishId = database.subjectDao().insertSubject(
            SubjectEntity(profileId = profileId, name = "English", isActive = true, createdAtMillis = 6L, updatedAtMillis = 6L),
        )
        database.markDao().insertMark(
            MarkEntity(profileId = profileId, termId = termId, assessmentId = assessmentId, studentId = studentId, subjectId = mathematicsId, markValue = 80.0, createdAtMillis = 7L, updatedAtMillis = 7L),
        )
        database.markDao().insertMark(
            MarkEntity(profileId = profileId, termId = termId, assessmentId = assessmentId, studentId = studentId, subjectId = englishId, markValue = 0.0, createdAtMillis = 8L, updatedAtMillis = 8L),
        )

        val bytes = repository.generateStudentWorkbook(profileId = profileId, termId = termId, studentId = studentId, assessmentId = assessmentId)
        val entries = bytes.readZipEntries()
        val studentSheet = entries["xl/worksheets/sheet1.xml"] ?: ""
        assertTrue(studentSheet.contains("Student"))
        assertTrue(studentSheet.contains("Mathematics"))
        assertTrue(studentSheet.contains("80"))
        assertTrue(studentSheet.contains("0"))
    }

    @Test
    fun classExportRespectsProfileAndTermIsolation() = runBlocking {
        val profileA = database.classProfileDao().insertProfile(
            ClassProfileEntity(teacherName = "Brian", classGrade = "Grade 1", currentTerm = 1, createdAtMillis = 10L),
        )
        val profileB = database.classProfileDao().insertProfile(
            ClassProfileEntity(teacherName = "Mary", classGrade = "Grade 2", currentTerm = 1, createdAtMillis = 11L),
        )
        val termA = database.termDao().insertTerm(
            TermEntity(profileId = profileA, termNumber = 1, createdAtMillis = 12L, updatedAtMillis = 12L),
        )
        val termB = database.termDao().insertTerm(
            TermEntity(profileId = profileB, termNumber = 1, createdAtMillis = 13L, updatedAtMillis = 13L),
        )
        val assessmentA = database.assessmentDao().insertAssessment(
            AssessmentEntity(profileId = profileA, termId = termA, name = "CAT 1", description = "", sortOrder = 0, isActive = true, createdAtMillis = 14L, updatedAtMillis = 14L),
        )
        val assessmentB = database.assessmentDao().insertAssessment(
            AssessmentEntity(profileId = profileB, termId = termB, name = "CAT 1", description = "", sortOrder = 0, isActive = true, createdAtMillis = 15L, updatedAtMillis = 15L),
        )
        val studentA = database.studentDao().insertStudent(
            StudentEntity(profileId = profileA, name = "Brian", isActive = true, createdAtMillis = 16L, updatedAtMillis = 16L),
        )
        val studentB = database.studentDao().insertStudent(
            StudentEntity(profileId = profileB, name = "Anna", isActive = true, createdAtMillis = 17L, updatedAtMillis = 17L),
        )
        val subjectA = database.subjectDao().insertSubject(
            SubjectEntity(profileId = profileA, name = "Science", isActive = true, createdAtMillis = 18L, updatedAtMillis = 18L),
        )
        val subjectB = database.subjectDao().insertSubject(
            SubjectEntity(profileId = profileB, name = "History", isActive = true, createdAtMillis = 19L, updatedAtMillis = 19L),
        )
        database.markDao().insertMark(
            MarkEntity(profileId = profileA, termId = termA, assessmentId = assessmentA, studentId = studentA, subjectId = subjectA, markValue = 91.0, createdAtMillis = 20L, updatedAtMillis = 20L),
        )
        database.markDao().insertMark(
            MarkEntity(profileId = profileB, termId = termB, assessmentId = assessmentB, studentId = studentB, subjectId = subjectB, markValue = 72.0, createdAtMillis = 21L, updatedAtMillis = 21L),
        )

        val bytes = repository.generateClassWorkbook(profileId = profileA, termId = termA, assessmentId = assessmentA)
        val entries = bytes.readZipEntries()
        val classSheet = entries["xl/worksheets/sheet1.xml"] ?: ""
        assertTrue(classSheet.contains("Brian"))
        assertFalse(classSheet.contains("Anna"))
        assertFalse(classSheet.contains("History"))
    }

    @Test
    fun blankTemplateHasStudentAndSubjectHeadersWithBlankMarks() = runBlocking {
        val profileId = database.classProfileDao().insertProfile(
            ClassProfileEntity(teacherName = "Brian", classGrade = "Grade 1", currentTerm = 2, createdAtMillis = 30L),
        )
        val termId = database.termDao().insertTerm(
            TermEntity(profileId = profileId, termNumber = 2, createdAtMillis = 31L, updatedAtMillis = 31L),
        )
        val assessmentId = database.assessmentDao().insertAssessment(
            AssessmentEntity(profileId = profileId, termId = termId, name = "CAT 1", description = "", sortOrder = 0, isActive = true, createdAtMillis = 32L, updatedAtMillis = 32L),
        )
        val studentA = database.studentDao().insertStudent(
            StudentEntity(profileId = profileId, name = "Kiswahili", isActive = true, createdAtMillis = 33L, updatedAtMillis = 33L),
        )
        val studentB = database.studentDao().insertStudent(
            StudentEntity(profileId = profileId, name = "Mary", isActive = true, createdAtMillis = 34L, updatedAtMillis = 34L),
        )
        val subjectA = database.subjectDao().insertSubject(
            SubjectEntity(profileId = profileId, name = "Mathematics", isActive = true, createdAtMillis = 35L, updatedAtMillis = 35L),
        )
        val subjectB = database.subjectDao().insertSubject(
            SubjectEntity(profileId = profileId, name = "Science", isActive = true, createdAtMillis = 36L, updatedAtMillis = 36L),
        )
        database.markDao().insertMark(
            MarkEntity(profileId = profileId, termId = termId, assessmentId = assessmentId, studentId = studentA, subjectId = subjectA, markValue = 55.0, createdAtMillis = 37L, updatedAtMillis = 37L),
        )

        val bytes = repository.generateBlankTemplate(profileId = profileId, termId = termId, assessmentId = assessmentId)
        val entries = bytes.readZipEntries()
        val worksheet = entries["xl/worksheets/sheet1.xml"] ?: ""
        assertTrue(worksheet.contains("Student"))
        assertTrue(worksheet.contains("Mathematics"))
        assertTrue(worksheet.contains("Science"))
    }

    private fun ByteArray.readZipEntries(): Map<String, String> {
        val entries = linkedMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(this)).use { zip ->
            var nextEntry = zip.nextEntry
            while (nextEntry != null) {
                entries[nextEntry.name] = zip.readBytes().toString(Charsets.UTF_8)
                nextEntry = zip.nextEntry
            }
        }
        return entries
    }
}
