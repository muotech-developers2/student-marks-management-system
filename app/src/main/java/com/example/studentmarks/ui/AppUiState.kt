package com.example.studentmarks.ui

import com.example.studentmarks.data.database.ClassProfileEntity

sealed interface AppUiState {
    data object Loading : AppUiState
    data class Setup(val form: SetupFormState) : AppUiState
    data class ProfileSelector(
        val profiles: List<ClassProfileEntity>,
        val activeProfileId: Long?,
        val errorMessage: String? = null,
    ) : AppUiState
    data class Dashboard(val profile: ClassProfileEntity) : AppUiState
    data class AddProfile(val form: SetupFormState) : AppUiState
    data class EditProfile(
        val profile: ClassProfileEntity,
        val form: SetupFormState,
        val returnToSelector: Boolean,
    ) : AppUiState
    data class Error(val message: String) : AppUiState
}

data class SetupFormState(
    val teacherName: String = "",
    val classGrade: String = "",
    val currentTerm: Int? = null,
    val teacherNameError: String? = null,
    val classGradeError: String? = null,
    val currentTermError: String? = null,
    val submitError: String? = null,
    val isSubmitting: Boolean = false,
)