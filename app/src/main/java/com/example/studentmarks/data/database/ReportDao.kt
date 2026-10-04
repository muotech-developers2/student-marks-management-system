package com.example.studentmarks.data.database

import androidx.room.Dao
import androidx.room.Query

@Dao
interface ReportDao {
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
    suspend fun getMarksForAssessment(
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
}
