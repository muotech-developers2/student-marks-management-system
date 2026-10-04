package com.example.studentmarks.ui

import com.example.studentmarks.data.database.AssessmentEntity
import com.example.studentmarks.data.database.ClassProfileEntity
import com.example.studentmarks.data.database.StudentEntity
import com.example.studentmarks.data.database.SubjectEntity
import com.example.studentmarks.data.database.TermEntity

data class StudentMarkRow(
    val student: StudentEntity,
    val value: String = "",
    val existingValue: Double? = null,
)

data class MarksUiState(
    val profile: ClassProfileEntity? = null,
    val selectedTerm: TermEntity? = null,
    val selectedAssessment: AssessmentEntity? = null,
    val selectedSubject: SubjectEntity? = null,
    val availableTerms: List<TermEntity> = emptyList(),
    val availableAssessments: List<AssessmentEntity> = emptyList(),
    val availableSubjects: List<SubjectEntity> = emptyList(),
    val studentRows: List<StudentMarkRow> = emptyList(),
    val searchQuery: String = "",
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val errorMessage: String? = null,
    val validationError: String? = null,
    val showDiscardDialog: Boolean = false,
    val pendingDeleteStudentId: Long? = null,
)

sealed interface MarksScreenState {
    data object Closed : MarksScreenState
    data class Entry(val state: MarksUiState) : MarksScreenState
}
