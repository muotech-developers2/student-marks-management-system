package com.example.studentmarks.data.repository

import com.example.studentmarks.data.database.TermDao
import com.example.studentmarks.data.database.TermEntity
import kotlinx.coroutines.flow.Flow

class TermRepository(
    private val dao: TermDao,
) {
    fun observeTerms(profileId: Long): Flow<List<TermEntity>> = dao.observeTerms(profileId)

    suspend fun getTermsForProfile(profileId: Long): List<TermEntity> = dao.getTermsForProfile(profileId)

    suspend fun getTermByProfileAndNumber(profileId: Long, termNumber: Int): TermEntity? =
        dao.getTerm(profileId, termNumber)

    suspend fun getOrCreateTerm(profileId: Long, termNumber: Int): Long {
        ensureTermsForProfile(profileId)
        val existing = getTermByProfileAndNumber(profileId, termNumber) ?: error("Term $termNumber should exist for profile $profileId")
        return existing.id
    }

    suspend fun ensureTermsForProfile(profileId: Long): List<TermEntity> {
        val currentTerms = dao.getTermsForProfile(profileId).associateBy { it.termNumber }
        val now = System.currentTimeMillis()
        val termNumbers = listOf(1, 2, 3)
        termNumbers.forEach { termNumber ->
            if (currentTerms[termNumber] == null) {
                dao.insertTerm(
                    TermEntity(
                        profileId = profileId,
                        termNumber = termNumber,
                        createdAtMillis = now,
                        updatedAtMillis = now,
                    ),
                )
            }
        }
        return dao.getTermsForProfile(profileId)
    }
}
