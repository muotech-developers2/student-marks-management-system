package com.example.studentmarks.data.repository

import com.example.studentmarks.data.database.SubjectDao
import com.example.studentmarks.data.database.SubjectEntity
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SubjectRepository(
    private val dao: SubjectDao,
) {
    fun observeSubjects(profileId: Long, isActive: Boolean): Flow<List<SubjectEntity>> =
        dao.observeSubjects(profileId, isActive)

    fun searchSubjects(
        profileId: Long,
        isActive: Boolean,
        query: String,
    ): Flow<List<SubjectEntity>> = dao.observeSubjects(profileId, isActive).map { subjects ->
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) {
            subjects
        } else {
            subjects.filter { normalize(it.name).contains(normalizedQuery) }
        }
    }

    fun observeCount(profileId: Long, isActive: Boolean): Flow<Int> =
        dao.observeCount(profileId, isActive)

    suspend fun getActiveSubjects(profileId: Long): List<SubjectEntity> =
        dao.getSubjectsForProfile(profileId).filter { it.isActive }

    suspend fun hasDuplicateName(
        profileId: Long,
        name: String,
        excludedSubjectId: Long? = null,
    ): Boolean {
        val normalizedName = normalize(name)
        return dao.getSubjectsForProfile(profileId).any { subject ->
            subject.id != excludedSubjectId && normalize(subject.name) == normalizedName
        }
    }

    suspend fun addSubject(
        profileId: Long,
        name: String,
        now: Long,
    ): Long = dao.insertSubject(
        SubjectEntity(
            profileId = profileId,
            name = name,
            isActive = true,
            createdAtMillis = now,
            updatedAtMillis = now,
        ),
    )

    suspend fun updateSubjectName(
        subjectId: Long,
        profileId: Long,
        name: String,
        now: Long,
    ): Int = dao.updateSubjectName(subjectId, profileId, name, now)

    suspend fun setSubjectActive(
        subjectId: Long,
        profileId: Long,
        isActive: Boolean,
        now: Long,
    ): Int = dao.setSubjectActive(subjectId, profileId, isActive, now)

    suspend fun deleteSubject(subjectId: Long, profileId: Long): Int =
        dao.deleteSubject(subjectId, profileId)

    private fun normalize(value: String): String =
        value.filterNot(Char::isWhitespace).lowercase(Locale.ROOT)
}
