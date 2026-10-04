package com.example.studentmarks.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query(
        "SELECT * FROM subjects WHERE profileId = :profileId " +
            "AND isActive = :isActive ORDER BY name COLLATE NOCASE ASC, id ASC",
    )
    fun observeSubjects(profileId: Long, isActive: Boolean): Flow<List<SubjectEntity>>

    @Query("SELECT COUNT(*) FROM subjects WHERE profileId = :profileId AND isActive = :isActive")
    fun observeCount(profileId: Long, isActive: Boolean): Flow<Int>

    @Query("SELECT * FROM subjects WHERE profileId = :profileId ORDER BY name COLLATE NOCASE ASC, id ASC")
    suspend fun getSubjectsForProfile(profileId: Long): List<SubjectEntity>

    @Query("SELECT * FROM subjects WHERE id = :subjectId AND profileId = :profileId LIMIT 1")
    suspend fun getSubject(subjectId: Long, profileId: Long): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Query(
        "UPDATE subjects SET name = :name, updatedAtMillis = :updatedAtMillis " +
            "WHERE id = :subjectId AND profileId = :profileId",
    )
    suspend fun updateSubjectName(
        subjectId: Long,
        profileId: Long,
        name: String,
        updatedAtMillis: Long,
    ): Int

    @Query(
        "UPDATE subjects SET isActive = :isActive, updatedAtMillis = :updatedAtMillis " +
            "WHERE id = :subjectId AND profileId = :profileId",
    )
    suspend fun setSubjectActive(
        subjectId: Long,
        profileId: Long,
        isActive: Boolean,
        updatedAtMillis: Long,
    ): Int

    @Query("DELETE FROM subjects WHERE id = :subjectId AND profileId = :profileId")
    suspend fun deleteSubject(subjectId: Long, profileId: Long): Int
}
