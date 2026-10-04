package com.example.studentmarks.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TermDao {
    @Query("SELECT * FROM terms WHERE profileId = :profileId ORDER BY termNumber ASC")
    fun observeTerms(profileId: Long): Flow<List<TermEntity>>

    @Query("SELECT * FROM terms WHERE profileId = :profileId ORDER BY termNumber ASC")
    suspend fun getTermsForProfile(profileId: Long): List<TermEntity>

    @Query("SELECT * FROM terms WHERE profileId = :profileId AND termNumber = :termNumber LIMIT 1")
    suspend fun getTerm(profileId: Long, termNumber: Int): TermEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTerm(term: TermEntity): Long
}
