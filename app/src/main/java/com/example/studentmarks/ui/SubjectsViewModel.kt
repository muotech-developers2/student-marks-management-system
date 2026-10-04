package com.example.studentmarks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studentmarks.data.database.ClassProfileEntity
import com.example.studentmarks.data.database.SubjectEntity
import com.example.studentmarks.data.repository.SubjectRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private sealed interface SubjectsRoute {
    data object Closed : SubjectsRoute
    data object List : SubjectsRoute
    data class Form(val subjectId: Long?) : SubjectsRoute
}

private data class SubjectSearchSpec(
    val profileId: Long?,
    val isActive: Boolean,
    val query: String,
)

private data class SubjectCounts(val active: Int, val inactive: Int)

private data class SubjectListContent(
    val profile: ClassProfileEntity?,
    val filter: SubjectStatusFilter,
    val query: String,
    val subjects: List<SubjectEntity>?,
    val counts: SubjectCounts,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SubjectsViewModel(
    private val repository: SubjectRepository,
) : ViewModel() {
    private val currentProfile = MutableStateFlow<ClassProfileEntity?>(null)
    private val route = MutableStateFlow<SubjectsRoute>(SubjectsRoute.Closed)
    private val filter = MutableStateFlow(SubjectStatusFilter.Active)
    private val query = MutableStateFlow("")
    private val formState = MutableStateFlow(SubjectFormState())
    private val pendingConfirmation = MutableStateFlow<SubjectConfirmation?>(null)
    private val operationError = MutableStateFlow<String?>(null)

    private val searchSpec = combine(currentProfile, filter, query) { profile, status, search ->
        SubjectSearchSpec(profile?.id, status.isActive, search)
    }

    private val subjects = searchSpec.flatMapLatest { spec ->
        spec.profileId?.let { repository.searchSubjects(it, spec.isActive, spec.query) }
            ?: flowOf(emptyList())
    }.onEach { operationError.value = null }
        .catch {
            operationError.value = "Unable to load subjects. Please try again."
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val activeCount = observeCount(SubjectStatusFilter.Active)
    private val inactiveCount = observeCount(SubjectStatusFilter.Inactive)
    val activeSubjectCount: StateFlow<Int> = activeCount

    private val counts = combine(activeCount, inactiveCount, ::SubjectCounts)
    private val listContent = combine(currentProfile, filter, query, subjects, counts) {
            profile, status, search, visibleSubjects, subjectCounts ->
        SubjectListContent(profile, status, search, visibleSubjects, subjectCounts)
    }

    val screenState: StateFlow<SubjectUiState> = combine(
        route,
        listContent,
        formState,
        pendingConfirmation,
        operationError,
    ) { currentRoute, content, form, confirmation, error ->
        when (currentRoute) {
            SubjectsRoute.Closed -> SubjectUiState.Closed
            SubjectsRoute.List -> content.profile?.let {
                SubjectUiState.SubjectList(
                    profile = it,
                    subjects = content.subjects.orEmpty(),
                    isLoading = content.subjects == null,
                    filter = content.filter,
                    query = content.query,
                    activeCount = content.counts.active,
                    inactiveCount = content.counts.inactive,
                    pendingConfirmation = confirmation,
                    errorMessage = error,
                )
            } ?: SubjectUiState.Closed
            is SubjectsRoute.Form -> content.profile?.let {
                SubjectUiState.SubjectForm(it, currentRoute.subjectId, form)
            } ?: SubjectUiState.Closed
        }
    }.catch { emit(SubjectUiState.Closed) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubjectUiState.Closed)

    fun setCurrentProfile(profile: ClassProfileEntity) {
        currentProfile.value = profile
    }

    fun openSubjects(profile: ClassProfileEntity) {
        currentProfile.value = profile
        filter.value = SubjectStatusFilter.Active
        query.value = ""
        operationError.value = null
        route.value = SubjectsRoute.List
    }

    fun closeSubjects() {
        route.value = SubjectsRoute.Closed
        pendingConfirmation.value = null
        operationError.value = null
    }

    fun navigateBack() {
        when (route.value) {
            SubjectsRoute.Closed -> Unit
            SubjectsRoute.List -> closeSubjects()
            is SubjectsRoute.Form -> cancelForm()
        }
    }

    fun openAddSubject() {
        formState.value = SubjectFormState()
        route.value = SubjectsRoute.Form(subjectId = null)
    }

    fun openEditSubject(subject: SubjectEntity) {
        if (subject.profileId != currentProfile.value?.id) return
        formState.value = SubjectFormState(name = subject.name)
        route.value = SubjectsRoute.Form(subjectId = subject.id)
    }

    fun onNameChanged(value: String) {
        formState.update { it.copy(name = value, error = null, submitError = null) }
    }

    fun onSearchChanged(value: String) {
        query.value = value
    }

    fun setFilter(value: SubjectStatusFilter) {
        filter.value = value
    }

    fun cancelForm() {
        route.value = SubjectsRoute.List
        formState.value = SubjectFormState()
    }

    fun submitSubject(addDuplicateAnyway: Boolean = false) {
        val current = formState.value
        if (current.isSaving) return
        val profile = currentProfile.value ?: return
        val subjectId = (route.value as? SubjectsRoute.Form)?.subjectId ?: run {
            if (route.value !is SubjectsRoute.Form) return
            null
        }

        val name = current.name.trim()
        if (name.isBlank()) {
            formState.update { it.copy(error = "Please enter the subject name.") }
            return
        }

        formState.update { it.copy(name = name, error = null, submitError = null, isSaving = true) }
        viewModelScope.launch {
            try {
                if (!addDuplicateAnyway && repository.hasDuplicateName(profile.id, name, subjectId)) {
                    formState.update { it.copy(isSaving = false, duplicateName = name) }
                    return@launch
                }

                val now = System.currentTimeMillis()
                if (subjectId == null) {
                    repository.addSubject(profile.id, name, now)
                } else if (repository.updateSubjectName(subjectId, profile.id, name, now) == 0) {
                    throw IllegalStateException("Subject no longer exists in this class")
                }
                route.value = SubjectsRoute.List
                formState.value = SubjectFormState()
                pendingConfirmation.value = null
                query.value = ""
                filter.value = SubjectStatusFilter.Active
            } catch (_: Exception) {
                formState.update {
                    it.copy(isSaving = false, submitError = "Unable to save the subject. Please try again.")
                }
            }
        }
    }

    fun cancelDuplicateWarning() {
        formState.update { it.copy(duplicateName = null) }
    }

    fun addDuplicateAnyway() {
        formState.update { it.copy(duplicateName = null) }
        submitSubject(addDuplicateAnyway = true)
    }

    fun requestDeactivate(subject: SubjectEntity) {
        if (subject.profileId != currentProfile.value?.id) return
        pendingConfirmation.value = SubjectConfirmation(subject, SubjectActionKind.Deactivate)
    }

    fun requestDelete(subject: SubjectEntity) {
        if (subject.profileId != currentProfile.value?.id) return
        pendingConfirmation.value = SubjectConfirmation(subject, SubjectActionKind.Delete)
    }

    fun cancelConfirmation() {
        pendingConfirmation.value = null
    }

    fun confirmSubjectAction() {
        val profileId = currentProfile.value?.id ?: return
        val confirmation = pendingConfirmation.value ?: return
        pendingConfirmation.value = null
        operationError.value = null
        viewModelScope.launch {
            try {
                when (confirmation.action) {
                    SubjectActionKind.Deactivate -> repository.setSubjectActive(
                        confirmation.subject.id,
                        profileId,
                        false,
                        System.currentTimeMillis(),
                    )
                    SubjectActionKind.Delete -> repository.deleteSubject(confirmation.subject.id, profileId)
                }
            } catch (_: Exception) {
                operationError.value = "Unable to update this subject. Please try again."
            }
        }
    }

    fun reactivateSubject(subject: SubjectEntity) {
        val profileId = currentProfile.value?.id ?: return
        if (subject.profileId != profileId) return
        operationError.value = null
        viewModelScope.launch {
            try {
                repository.setSubjectActive(subject.id, profileId, true, System.currentTimeMillis())
            } catch (_: Exception) {
                operationError.value = "Unable to reactivate this subject. Please try again."
            }
        }
    }

    private fun observeCount(status: SubjectStatusFilter): StateFlow<Int> =
        currentProfile.map { it?.id }.distinctUntilChanged().flatMapLatest { profileId ->
            profileId?.let { repository.observeCount(it, status.isActive) } ?: flowOf(0)
        }.catch { emit(0) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

class SubjectsViewModelFactory(
    private val repository: SubjectRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(SubjectsViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        return SubjectsViewModel(repository) as T
    }
}
