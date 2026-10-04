package com.example.studentmarks.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "terms",
    foreignKeys = [
        ForeignKey(
            entity = ClassProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["profileId", "termNumber"], unique = true),
    ],
)
data class TermEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val termNumber: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
