package com.example.studentmarks.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "marks",
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
        ForeignKey(
            entity = AssessmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["assessmentId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["termId"]),
        Index(value = ["assessmentId"]),
        Index(value = ["studentId"]),
        Index(value = ["subjectId"]),
        Index(value = ["profileId", "termId", "assessmentId", "studentId", "subjectId"], unique = true),
    ],
)
data class MarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val termId: Long,
    val assessmentId: Long,
    val studentId: Long,
    val subjectId: Long,
    val markValue: Double,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
