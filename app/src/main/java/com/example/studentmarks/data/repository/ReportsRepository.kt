package com.example.studentmarks.data.repository

import com.example.studentmarks.data.database.AssessmentDao
import com.example.studentmarks.data.database.AssessmentEntity
import com.example.studentmarks.data.database.MarkDao
import com.example.studentmarks.data.database.MarkEntity
import com.example.studentmarks.data.database.StudentDao
import com.example.studentmarks.data.database.StudentEntity
import com.example.studentmarks.data.database.SubjectDao
import com.example.studentmarks.data.database.SubjectEntity
import com.example.studentmarks.data.database.TermDao
import com.example.studentmarks.data.database.TermEntity

class ReportsRepository(
    private val markDao: MarkDao,
    private val studentDao: StudentDao,
    private val subjectDao: SubjectDao,
    private val assessmentDao: AssessmentDao,
    private val termDao: TermDao,
) {
    suspend fun getMarksForProfileAndTerm(profileId: Long, termId: Long): List<MarkEntity> =
        markDao.getMarksForProfileAndTerm(profileId, termId)

    suspend fun getMarksForStudentTerm(profileId: Long, studentId: Long, termId: Long): List<MarkEntity> =
        markDao.getMarksForStudentTerm(profileId, termId, studentId)

    suspend fun getMarksForSubjectTerm(profileId: Long, subjectId: Long, termId: Long): List<MarkEntity> =
        markDao.getMarksForSubjectTerm(profileId, termId, subjectId)

    suspend fun getMarksForAssessment(profileId: Long, termId: Long, assessmentId: Long): List<MarkEntity> =
        markDao.getMarksForAssessmentByProfileAndTerm(profileId, termId, assessmentId)

    suspend fun getStudentsForProfile(profileId: Long, includeInactive: Boolean): List<StudentEntity> {
        val students = studentDao.getStudentsForProfile(profileId)
        return if (includeInactive) students else students.filter { it.isActive }
    }

    suspend fun getSubjectsForProfile(profileId: Long, includeInactive: Boolean): List<SubjectEntity> {
        val subjects = subjectDao.getSubjectsForProfile(profileId)
        return if (includeInactive) subjects else subjects.filter { it.isActive }
    }

    suspend fun getAssessmentsForTerm(profileId: Long, termId: Long): List<AssessmentEntity> =
        assessmentDao.getAssessmentsForTerm(profileId, termId)

    suspend fun getTermsForProfile(profileId: Long): List<TermEntity> = termDao.getTermsForProfile(profileId)
}
