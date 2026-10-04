package com.example.studentmarks.data.repository

import com.example.studentmarks.data.database.AssessmentDao
import com.example.studentmarks.data.database.AssessmentEntity
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AssessmentRepository(
    private val dao: AssessmentDao,
) {
    fun observeAssessments(profileId: Long, termId: Long, isActive: Boolean): Flow<List<AssessmentEntity>> =
        dao.observeAssessments(profileId, termId, isActive)

    fun searchAssessments(
        profileId: Long,
        termId: Long,
        query: String,
    ): Flow<List<AssessmentEntity>> = dao.observeAssessments(profileId, termId, true).map { assessments ->
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) {
            assessments
        } else {
            assessments.filter { normalize(it.name).contains(normalizedQuery) }
        }
    }

    fun observeCount(profileId: Long, termId: Long, isActive: Boolean): Flow<Int> =
        dao.observeCount(profileId, termId, isActive)

    suspend fun getAssessmentsForTerm(profileId: Long, termId: Long): List<AssessmentEntity> =
        dao.getAssessmentsForTerm(profileId, termId)

    suspend fun hasDuplicateName(
        profileId: Long,
        termId: Long,
        name: String,
        excludedAssessmentId: Long? = null,
    ): Boolean {
        val normalizedName = normalize(name)
        return dao.getAssessmentsForTerm(profileId, termId).any { assessment ->
            assessment.id != excludedAssessmentId && normalize(assessment.name) == normalizedName
        }
    }

    suspend fun addAssessment(
        profileId: Long,
        termId: Long,
        name: String,
        description: String,
        now: Long,
    ): Long = dao.insertAssessment(
        AssessmentEntity(
            profileId = profileId,
            termId = termId,
            name = name,
            description = description,
            sortOrder = 0,
            isActive = true,
            createdAtMillis = now,
            updatedAtMillis = now,
        ),
    )

    suspend fun updateAssessmentName(
        assessmentId: Long,
        profileId: Long,
        termId: Long,
        name: String,
        description: String,
        now: Long,
    ): Int = dao.updateAssessment(assessmentId, profileId, termId, name, description, 0, now)

    suspend fun setAssessmentActive(
        assessmentId: Long,
        profileId: Long,
        termId: Long,
        isActive: Boolean,
        now: Long,
    ): Int = dao.setAssessmentActive(assessmentId, profileId, termId, isActive, now)

    suspend fun deleteAssessment(assessmentId: Long, profileId: Long, termId: Long): Int =
        dao.deleteAssessment(assessmentId, profileId, termId)

    private fun normalize(value: String): String =
        value.filterNot { it.isWhitespace() }.lowercase(Locale.ROOT)
}
