package com.example.studentmarks.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MarkDao {
    @Query(
        "SELECT * FROM marks WHERE profileId = :profileId AND termId = :termId AND assessmentId = :assessmentId AND subjectId = :subjectId ORDER BY studentId ASC",
    )
    fun observeMarksForAssessment(
        profileId: Long,
        termId: Long,
        assessmentId: Long,
        subjectId: Long,
    ): Flow<List<MarkEntity>>

    @Query(
        "SELECT * FROM marks WHERE profileId = :profileId AND termId = :termId AND assessmentId = :assessmentId AND studentId = :studentId AND subjectId = :subjectId LIMIT 1",
    )
    suspend fun getMarkForStudentSubjectAssessment(
        profileId: Long,
        termId: Long,
        assessmentId: Long,
        studentId: Long,
        subjectId: Long,
    ): MarkEntity?

    @Query(
        "SELECT * FROM marks WHERE profileId = :profileId AND termId = :termId AND assessmentId = :assessmentId AND subjectId = :subjectId ORDER BY studentId ASC",
    )
    suspend fun getMarksForAssessment(
        profileId: Long,
        termId: Long,
        assessmentId: Long,
        subjectId: Long,
    ): List<MarkEntity>

    @Query(
        "SELECT * FROM marks WHERE profileId = :profileId AND termId = :termId ORDER BY assessmentId ASC, subjectId ASC, studentId ASC",
    )
    suspend fun getMarksForProfileAndTerm(
        profileId: Long,
        termId: Long,
    ): List<MarkEntity>

    @Query(
        "SELECT * FROM marks WHERE profileId = :profileId AND termId = :termId AND assessmentId = :assessmentId ORDER BY studentId ASC, subjectId ASC",
    )
    suspend fun getMarksForAssessmentByProfileAndTerm(
        profileId: Long,
        termId: Long,
        assessmentId: Long,
    ): List<MarkEntity>

    @Query(
        "SELECT * FROM marks WHERE profileId = :profileId AND termId = :termId AND studentId = :studentId ORDER BY assessmentId ASC, subjectId ASC",
    )
    suspend fun getMarksForStudentTerm(
        profileId: Long,
        termId: Long,
        studentId: Long,
    ): List<MarkEntity>

    @Query(
        "SELECT * FROM marks WHERE profileId = :profileId AND termId = :termId AND subjectId = :subjectId ORDER BY assessmentId ASC, studentId ASC",
    )
    suspend fun getMarksForSubjectTerm(
        profileId: Long,
        termId: Long,
        subjectId: Long,
    ): List<MarkEntity>

    @Query(
        "SELECT * FROM marks WHERE profileId = :profileId AND studentId = :studentId ORDER BY updatedAtMillis DESC",
    )
    suspend fun getMarksForStudent(profileId: Long, studentId: Long): List<MarkEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMark(mark: MarkEntity): Long

    @Query(
        "UPDATE marks SET markValue = :markValue, updatedAtMillis = :updatedAtMillis WHERE id = :markId AND profileId = :profileId AND termId = :termId AND assessmentId = :assessmentId AND studentId = :studentId AND subjectId = :subjectId",
    )
    suspend fun updateMarkValue(
        markId: Long,
        profileId: Long,
        termId: Long,
        assessmentId: Long,
        studentId: Long,
        subjectId: Long,
        markValue: Double,
        updatedAtMillis: Long,
    ): Int

    @Query(
        "DELETE FROM marks WHERE profileId = :profileId AND termId = :termId AND assessmentId = :assessmentId AND studentId = :studentId AND subjectId = :subjectId",
    )
    suspend fun deleteMark(
        profileId: Long,
        termId: Long,
        assessmentId: Long,
        studentId: Long,
        subjectId: Long,
    ): Int

    @Query(
        "SELECT COUNT(*) FROM marks WHERE profileId = :profileId AND termId = :termId AND assessmentId = :assessmentId AND subjectId = :subjectId",
    )
    suspend fun getCompletedCount(
        profileId: Long,
        termId: Long,
        assessmentId: Long,
        subjectId: Long,
    ): Int

    @Transaction
    suspend fun replaceMark(
        mark: MarkEntity,
    ): Long {
        val existing = getMarkForStudentSubjectAssessment(
            mark.profileId,
            mark.termId,
            mark.assessmentId,
            mark.studentId,
            mark.subjectId,
        )
        return if (existing == null) {
            insertMark(mark)
        } else {
            updateMarkValue(
                markId = existing.id,
                profileId = mark.profileId,
                termId = mark.termId,
                assessmentId = mark.assessmentId,
                studentId = mark.studentId,
                subjectId = mark.subjectId,
                markValue = mark.markValue,
                updatedAtMillis = mark.updatedAtMillis,
            )
            existing.id
        }
    }
}
