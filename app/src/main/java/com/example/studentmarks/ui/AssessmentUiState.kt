package com.example.studentmarks.ui

import com.example.studentmarks.data.database.AssessmentEntity
import com.example.studentmarks.data.database.ClassProfileEntity
import com.example.studentmarks.data.database.TermEntity

enum class AssessmentStatusFilter(val isActive: Boolean) {
    Active(true),
    Inactive(false),
}

enum class AssessmentActionKind {
    Deactivate,
    Delete,
}

data class AssessmentConfirmation(
    val assessment: AssessmentEntity,
    val action: AssessmentActionKind,
)

sealed interface AssessmentUiState {
    data object Closed : AssessmentUiState

    data class AssessmentList(
        val profile: ClassProfileEntity,
        val term: TermEntity,
        val assessments: List<AssessmentEntity>,
        val isLoading: Boolean,
        val filter: AssessmentStatusFilter,
        val query: String,
        val activeCount: Int,
        val inactiveCount: Int,
        val pendingConfirmation: AssessmentConfirmation?,
        val errorMessage: String?,
    ) : AssessmentUiState

    data class AssessmentForm(
        val profile: ClassProfileEntity,
        val term: TermEntity,
        val assessmentId: Long?,
        val state: AssessmentFormState,
    ) : AssessmentUiState
}

data class AssessmentFormState(
    val name: String = "",
    val description: String = "",
    val error: String? = null,
    val submitError: String? = null,
    val duplicateName: String? = null,
    val isSaving: Boolean = false,
)
