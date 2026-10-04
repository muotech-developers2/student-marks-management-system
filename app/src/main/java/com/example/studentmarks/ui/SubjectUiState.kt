package com.example.studentmarks.ui

import com.example.studentmarks.data.database.ClassProfileEntity
import com.example.studentmarks.data.database.SubjectEntity

enum class SubjectStatusFilter(val isActive: Boolean) {
    Active(true),
    Inactive(false),
}

enum class SubjectActionKind {
    Deactivate,
    Delete,
}

data class SubjectConfirmation(
    val subject: SubjectEntity,
    val action: SubjectActionKind,
)

sealed interface SubjectUiState {
    data object Closed : SubjectUiState

    data class SubjectList(
        val profile: ClassProfileEntity,
        val subjects: List<SubjectEntity>,
        val isLoading: Boolean,
        val filter: SubjectStatusFilter,
        val query: String,
        val activeCount: Int,
        val inactiveCount: Int,
        val pendingConfirmation: SubjectConfirmation?,
        val errorMessage: String?,
    ) : SubjectUiState

    data class SubjectForm(
        val profile: ClassProfileEntity,
        val subjectId: Long?,
        val state: SubjectFormState,
    ) : SubjectUiState
}

data class SubjectFormState(
    val name: String = "",
    val error: String? = null,
    val submitError: String? = null,
    val duplicateName: String? = null,
    val isSaving: Boolean = false,
)
