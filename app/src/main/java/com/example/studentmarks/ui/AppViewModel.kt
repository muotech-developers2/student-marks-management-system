package com.example.studentmarks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studentmarks.data.repository.ClassProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private sealed interface AppRoute {
    data object Automatic : AppRoute
    data object ProfileSelector : AppRoute
    data class Form(val profileId: Long?, val returnToSelector: Boolean) : AppRoute
}

class AppViewModel(
    private val repository: ClassProfileRepository,
) : ViewModel() {
    private val route = MutableStateFlow<AppRoute>(AppRoute.Automatic)
    private val _formState = MutableStateFlow(SetupFormState())
    val formState: StateFlow<SetupFormState> = _formState.asStateFlow()
    private val selectorError = MutableStateFlow<String?>(null)

    val appState: StateFlow<AppUiState> = combine(
        repository.observeProfiles(),
        repository.activeProfileId,
        route,
        _formState,
        selectorError,
    ) { profiles, activeProfileId, currentRoute, form, errorMessage ->
        if (profiles.isEmpty()) {
            AppUiState.Setup(form)
        } else {
            when (currentRoute) {
                AppRoute.Automatic -> profiles.firstOrNull { it.id == activeProfileId }
                    ?.let(AppUiState::Dashboard)
                    ?: AppUiState.ProfileSelector(profiles, activeProfileId, errorMessage)
                AppRoute.ProfileSelector ->
                    AppUiState.ProfileSelector(profiles, activeProfileId, errorMessage)
                is AppRoute.Form -> {
                    if (currentRoute.profileId == null) {
                        AppUiState.AddProfile(form)
                    } else {
                        profiles.firstOrNull { it.id == currentRoute.profileId }
                            ?.let { AppUiState.EditProfile(it, form, currentRoute.returnToSelector) }
                            ?: AppUiState.ProfileSelector(profiles, activeProfileId, errorMessage)
                    }
                }
            }
        }
    }
        .catch { emit(AppUiState.Error("Unable to load your class profiles.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState.Loading)

    fun onTeacherNameChanged(value: String) {
        _formState.update {
            it.copy(teacherName = value, teacherNameError = null, submitError = null)
        }
    }

    fun onClassGradeChanged(value: String) {
        _formState.update {
            it.copy(classGrade = value, classGradeError = null, submitError = null)
        }
    }

    fun onCurrentTermSelected(term: Int) {
        if (term !in 1..3) return
        _formState.update {
            it.copy(currentTerm = term, currentTermError = null, submitError = null)
        }
    }

    fun showProfileSelector() {
        selectorError.value = null
        route.value = AppRoute.ProfileSelector
    }

    fun addProfile() {
        val profiles = when (val state = appState.value) {
            is AppUiState.ProfileSelector -> state.profiles
            is AppUiState.Dashboard -> listOf(state.profile)
            else -> emptyList()
        }
        val activeId = repository.activeProfileId.value
        val teacherName = profiles.firstOrNull { it.id == activeId }?.teacherName
            ?: profiles.firstOrNull()?.teacherName.orEmpty()
        _formState.value = SetupFormState(teacherName = teacherName)
        route.value = AppRoute.Form(profileId = null, returnToSelector = true)
    }

    fun editProfile(profileId: Long, returnToSelector: Boolean) {
        val profile = when (val state = appState.value) {
            is AppUiState.ProfileSelector -> state.profiles.firstOrNull { it.id == profileId }
            is AppUiState.Dashboard -> state.profile.takeIf { it.id == profileId }
            else -> null
        } ?: return

        _formState.value = SetupFormState(
            teacherName = profile.teacherName,
            classGrade = profile.classGrade,
            currentTerm = profile.currentTerm,
        )
        route.value = AppRoute.Form(profileId, returnToSelector)
    }

    fun navigateBack() {
        route.value = when (val current = route.value) {
            AppRoute.Automatic -> AppRoute.Automatic
            AppRoute.ProfileSelector -> AppRoute.Automatic
            is AppRoute.Form -> {
                if (current.returnToSelector) AppRoute.ProfileSelector else AppRoute.Automatic
            }
        }
    }

    fun openProfile(profileId: Long) {
        val profiles = (appState.value as? AppUiState.ProfileSelector)?.profiles.orEmpty()
        if (profiles.none { it.id == profileId }) return
        repository.setActiveProfileId(profileId)
        route.value = AppRoute.Automatic
    }

    fun deleteProfile(profileId: Long) {
        selectorError.value = null
        viewModelScope.launch {
            try {
                val deletedCount = repository.deleteProfile(profileId)
                if (deletedCount > 0 && repository.activeProfileId.value == profileId) {
                    val nextProfileId = repository.getAllProfiles().firstOrNull()?.id
                    repository.setActiveProfileId(nextProfileId)
                }
            } catch (_: Exception) {
                selectorError.value = "Unable to delete this class. Please try again."
            }
        }
    }

    fun submitProfile() {
        val current = formState.value
        if (current.isSubmitting) return

        val teacherName = current.teacherName.trim()
        val classGrade = current.classGrade.trim()
        val validated = current.copy(
            teacherName = teacherName,
            classGrade = classGrade,
            teacherNameError = if (teacherName.isBlank()) "Please enter the teacher name." else null,
            classGradeError = if (classGrade.isBlank()) "Please enter the class or grade." else null,
            currentTermError = if (current.currentTerm == null) "Please select a term." else null,
            submitError = null,
        )
        _formState.value = validated
        if (validated.teacherNameError != null ||
            validated.classGradeError != null ||
            validated.currentTermError != null
        ) {
            return
        }

        val currentRoute = route.value
        val profileId = when (currentRoute) {
            is AppRoute.Form -> currentRoute.profileId
            AppRoute.Automatic -> null
            AppRoute.ProfileSelector -> return
        }
        _formState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            try {
                if (profileId == null) {
                    val newId = repository.createProfile(
                        com.example.studentmarks.data.database.ClassProfileEntity(
                            teacherName = teacherName,
                            classGrade = classGrade,
                            currentTerm = requireNotNull(current.currentTerm),
                            createdAtMillis = System.currentTimeMillis(),
                        ),
                    )
                    repository.setActiveProfileId(newId)
                    route.value = AppRoute.Automatic
                } else {
                    val updatedCount = repository.updateProfile(
                        profileId = profileId,
                        teacherName = teacherName,
                        classGrade = classGrade,
                        currentTerm = requireNotNull(current.currentTerm),
                    )
                    if (updatedCount == 0) {
                        throw IllegalStateException("Profile no longer exists")
                    }
                    route.value = when (currentRoute) {
                        is AppRoute.Form -> if (currentRoute.returnToSelector) {
                            AppRoute.ProfileSelector
                        } else {
                            AppRoute.Automatic
                        }
                        else -> AppRoute.Automatic
                    }
                }
                _formState.value = SetupFormState()
            } catch (_: Exception) {
                _formState.update {
                    it.copy(
                        isSubmitting = false,
                        submitError = "Unable to save the class. Please try again.",
                    )
                }
            }
        }
    }
}

class AppViewModelFactory(
    private val repository: ClassProfileRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(AppViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        return AppViewModel(repository) as T
    }
}