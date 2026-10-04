package com.example.studentmarks.data.repository

import com.example.studentmarks.data.database.MarkDao
import com.example.studentmarks.data.database.MarkEntity
import java.util.Locale
import kotlinx.coroutines.flow.Flow

class MarkRepository(
    private val dao: MarkDao,
) {
    fun observeMarksForAssessment(
        profileId: Long,
        termId: Long,
        assessmentId: Long,
        subjectId: Long,
    ): Flow<List<MarkEntity>> = dao.observeMarksForAssessment(profileId, termId, assessmentId, subjectId)

    suspend fun getMarkForStudentSubjectAssessment(
        profileId: Long,
        termId: Long,
        assessmentId: Long,
        studentId: Long,
        subjectId: Long,
    ): MarkEntity? = dao.getMarkForStudentSubjectAssessment(profileId, termId, assessmentId, studentId, subjectId)

    suspend fun getMarksForAssessment(
        profileId: Long,
        termId: Long,
        assessmentId: Long,
        subjectId: Long,
    ): List<MarkEntity> = dao.getMarksForAssessment(profileId, termId, assessmentId, subjectId)

    suspend fun getMarksForStudent(profileId: Long, studentId: Long): List<MarkEntity> =
        dao.getMarksForStudent(profileId, studentId)

    suspend fun upsertMark(
        profileId: Long,
        termId: Long,
        assessmentId: Long,
        studentId: Long,
        subjectId: Long,
        value: Double,
        now: Long,
    ): Long {
        val normalizedValue = normalizeValue(value)
        val mark = MarkEntity(
            profileId = profileId,
            termId = termId,
            assessmentId = assessmentId,
            studentId = studentId,
            subjectId = subjectId,
            markValue = normalizedValue,
            createdAtMillis = now,
            updatedAtMillis = now,
        )
        return dao.replaceMark(mark)
    }

    suspend fun deleteMark(
        profileId: Long,
        termId: Long,
        assessmentId: Long,
        studentId: Long,
        subjectId: Long,
    ): Int = dao.deleteMark(profileId, termId, assessmentId, studentId, subjectId)

    suspend fun getCompletedCount(
        profileId: Long,
        termId: Long,
        assessmentId: Long,
        subjectId: Long,
    ): Int = dao.getCompletedCount(profileId, termId, assessmentId, subjectId)

    private fun normalizeValue(value: Double): Double {
        if (!value.isFinite()) {
            throw IllegalArgumentException("Mark must be a valid number.")
        }
        if (value < 0.0 || value > 100.0) {
            throw IllegalArgumentException("Mark must be between 0 and 100.")
        }
        return value
    }

    fun normalizeInput(value: String): Double? {
        if (value.isBlank()) return null
        val trimmed = value.trim()
        val amount = trimmed.lowercase(Locale.ROOT)
        if (amount == "-" || amount == "." || amount == "-." || amount == "--") return null
        val parsed = amount.toDoubleOrNull() ?: return null
        return normalizeValue(parsed)
    }
}
