package com.example.studentmarks.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentDao {
    @Query(
        "SELECT * FROM assessments WHERE profileId = :profileId AND termId = :termId " +
            "AND isActive = :isActive ORDER BY sortOrder ASC, id ASC",
    )
    fun observeAssessments(profileId: Long, termId: Long, isActive: Boolean): Flow<List<AssessmentEntity>>

    @Query("SELECT COUNT(*) FROM assessments WHERE profileId = :profileId AND termId = :termId AND isActive = :isActive")
    fun observeCount(profileId: Long, termId: Long, isActive: Boolean): Flow<Int>

    @Query("SELECT * FROM assessments WHERE profileId = :profileId AND termId = :termId ORDER BY sortOrder ASC, id ASC")
    suspend fun getAssessmentsForTerm(profileId: Long, termId: Long): List<AssessmentEntity>

    @Query("SELECT * FROM assessments WHERE id = :assessmentId AND profileId = :profileId AND termId = :termId LIMIT 1")
    suspend fun getAssessment(assessmentId: Long, profileId: Long, termId: Long): AssessmentEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAssessment(assessment: AssessmentEntity): Long

    @Query(
        "UPDATE assessments SET name = :name, description = :description, sortOrder = :sortOrder, updatedAtMillis = :updatedAtMillis " +
            "WHERE id = :assessmentId AND profileId = :profileId AND termId = :termId",
    )
    suspend fun updateAssessment(
        assessmentId: Long,
        profileId: Long,
        termId: Long,
        name: String,
        description: String,
        sortOrder: Int,
        updatedAtMillis: Long,
    ): Int

    @Query(
        "UPDATE assessments SET isActive = :isActive, updatedAtMillis = :updatedAtMillis " +
            "WHERE id = :assessmentId AND profileId = :profileId AND termId = :termId",
    )
    suspend fun setAssessmentActive(
        assessmentId: Long,
        profileId: Long,
        termId: Long,
        isActive: Boolean,
        updatedAtMillis: Long,
    ): Int

    @Query("DELETE FROM assessments WHERE id = :assessmentId AND profileId = :profileId AND termId = :termId")
    suspend fun deleteAssessment(assessmentId: Long, profileId: Long, termId: Long): Int
}
