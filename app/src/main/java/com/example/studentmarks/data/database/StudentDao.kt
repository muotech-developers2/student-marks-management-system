package com.example.studentmarks.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query(
        "SELECT * FROM students WHERE profileId = :profileId " +
            "AND isActive = :isActive ORDER BY name COLLATE NOCASE ASC, id ASC",
    )
    fun observeStudents(profileId: Long, isActive: Boolean): Flow<List<StudentEntity>>

    @Query("SELECT COUNT(*) FROM students WHERE profileId = :profileId AND isActive = :isActive")
    fun observeCount(profileId: Long, isActive: Boolean): Flow<Int>

    @Query("SELECT * FROM students WHERE profileId = :profileId ORDER BY id ASC")
    suspend fun getStudentsForProfile(profileId: Long): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :studentId AND profileId = :profileId LIMIT 1")
    suspend fun getStudent(studentId: Long, profileId: Long): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStudent(student: StudentEntity): Long

    @Query(
        "UPDATE students SET name = :name, updatedAtMillis = :updatedAtMillis " +
            "WHERE id = :studentId AND profileId = :profileId",
    )
    suspend fun updateStudentName(
        studentId: Long,
        profileId: Long,
        name: String,
        updatedAtMillis: Long,
    ): Int

    @Query(
        "UPDATE students SET isActive = :isActive, updatedAtMillis = :updatedAtMillis " +
            "WHERE id = :studentId AND profileId = :profileId",
    )
    suspend fun setStudentActive(
        studentId: Long,
        profileId: Long,
        isActive: Boolean,
        updatedAtMillis: Long,
    ): Int

    @Query("DELETE FROM students WHERE id = :studentId AND profileId = :profileId")
    suspend fun deleteStudent(studentId: Long, profileId: Long): Int
}