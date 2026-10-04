package com.example.studentmarks.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "assessments",
    foreignKeys = [
        ForeignKey(
            entity = ClassProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TermEntity::class,
            parentColumns = ["id"],
            childColumns = ["termId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["termId"]),
        Index(value = ["profileId", "termId", "isActive", "name"]),
    ],
)
data class AssessmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val termId: Long,
    val name: String,
    val description: String = "",
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
