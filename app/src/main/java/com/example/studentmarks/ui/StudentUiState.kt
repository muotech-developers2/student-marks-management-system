package com.example.studentmarks.ui

import com.example.studentmarks.data.database.ClassProfileEntity
import com.example.studentmarks.data.database.StudentEntity

enum class StudentStatusFilter(val isActive: Boolean) {
    Active(true),
    Inactive(false),
}

enum class StudentActionKind {
    Deactivate,
    Delete,
}

data class StudentConfirmation(
    val student: StudentEntity,
    val action: StudentActionKind,
)

sealed interface StudentUiState {
    data object Closed : StudentUiState

    data class StudentList(
        val profile: ClassProfileEntity,
        val students: List<StudentEntity>,
        val isLoading: Boolean,
        val filter: StudentStatusFilter,
        val query: String,
        val activeCount: Int,
        val inactiveCount: Int,
        val pendingConfirmation: StudentConfirmation?,
        val errorMessage: String?,
    ) : StudentUiState

    data class StudentForm(
        val profile: ClassProfileEntity,
        val studentId: Long?,
        val state: StudentFormState,
    ) : StudentUiState
}

data class StudentFormState(
    val name: String = "",
    val error: String? = null,
    val submitError: String? = null,
    val duplicateName: String? = null,
    val isSaving: Boolean = false,
)