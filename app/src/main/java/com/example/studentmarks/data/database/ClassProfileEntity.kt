package com.example.studentmarks.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "class_profiles")
data class ClassProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teacherName: String,
    val classGrade: String,
    val currentTerm: Int,
    val createdAtMillis: Long,
)