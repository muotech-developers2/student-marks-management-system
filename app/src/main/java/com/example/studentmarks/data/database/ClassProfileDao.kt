package com.example.studentmarks.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassProfileDao {
    @Query("SELECT * FROM class_profiles ORDER BY id ASC")
    fun observeProfiles(): Flow<List<ClassProfileEntity>>

    @Query("SELECT * FROM class_profiles WHERE id = :profileId LIMIT 1")
    suspend fun getProfileById(profileId: Long): ClassProfileEntity?

    @Query("SELECT * FROM class_profiles ORDER BY id ASC")
    suspend fun getAllProfiles(): List<ClassProfileEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProfile(profile: ClassProfileEntity): Long

    @Query(
        "UPDATE class_profiles SET teacherName = :teacherName, " +
            "classGrade = :classGrade, currentTerm = :currentTerm WHERE id = :profileId",
    )
    suspend fun updateProfileById(
        profileId: Long,
        teacherName: String,
        classGrade: String,
        currentTerm: Int,
    ): Int

    @Query("UPDATE class_profiles SET currentTerm = :currentTerm WHERE id = :profileId")
    suspend fun updateCurrentTerm(profileId: Long, currentTerm: Int): Int

    @Query("DELETE FROM class_profiles WHERE id = :profileId")
    suspend fun deleteProfileById(profileId: Long): Int
}