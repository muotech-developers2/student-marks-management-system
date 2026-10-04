package com.example.studentmarks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studentmarks.data.database.AssessmentEntity
import com.example.studentmarks.data.database.ClassProfileEntity
import com.example.studentmarks.data.database.TermEntity
import com.example.studentmarks.data.repository.AssessmentRepository
import com.example.studentmarks.data.repository.ClassProfileRepository
import com.example.studentmarks.data.repository.TermRepository
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

private sealed interface AssessmentsRoute {
    data object Closed : AssessmentsRoute
    data object List : AssessmentsRoute
    data class Form(val assessmentId: Long?) : AssessmentsRoute
}

private data class AssessmentSearchSpec(
    val profileId: Long?,
    val termId: Long?,
    val isActive: Boolean,
    val query: String,
)

private data class AssessmentCounts(val active: Int, val inactive: Int)

private data class AssessmentListContent(
    val profile: ClassProfileEntity?,
    val term: TermEntity?,
    val filter: AssessmentStatusFilter,
    val query: String,
    val assessments: List<AssessmentEntity>?,
    val counts: AssessmentCounts,
)

@OptIn(ExperimentalCoroutinesApi::class)
class AssessmentsViewModel(
    private val assessmentRepository: AssessmentRepository,
    private val termRepository: TermRepository,
    private val profileRepository: ClassProfileRepository,
) : ViewModel() {
    private val currentProfile = MutableStateFlow<ClassProfileEntity?>(null)
    private val currentTermId = MutableStateFlow<Long?>(null)
    private val route = MutableStateFlow<AssessmentsRoute>(AssessmentsRoute.Closed)
    private val filter = MutableStateFlow(AssessmentStatusFilter.Active)
    private val query = MutableStateFlow("")
    private val formState = MutableStateFlow(AssessmentFormState())
    private val pendingConfirmation = MutableStateFlow<AssessmentConfirmation?>(null)
    private val operationError = MutableStateFlow<String?>(null)

    val terms: StateFlow<List<TermEntity>> = currentProfile
        .flatMapLatest { profile ->
            profile?.id?.let { termRepository.observeTerms(it) } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedTerm: StateFlow<TermEntity?> = combine(currentProfile, terms, currentTermId) { profile, availableTerms, selectedId ->
        val profileTerms = availableTerms.filter { it.profileId == profile?.id }
        val term = profileTerms.firstOrNull { it.id == selectedId }
            ?: profileTerms.firstOrNull { it.termNumber == profile?.currentTerm }
            ?: profileTerms.firstOrNull()
        term
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val searchSpec = combine(currentProfile, selectedTerm, filter, query) { profile, term, status, search ->
        AssessmentSearchSpec(profile?.id, term?.id, status.isActive, search)
    }

    private val assessments = searchSpec.flatMapLatest { spec ->
        if (spec.profileId == null || spec.termId == null) {
            flowOf(emptyList())
        } else {
            assessmentRepository.searchAssessments(spec.profileId, spec.termId, spec.query)
                .map { it.filter { assessment -> assessment.isActive == spec.isActive } }
        }
    }.onEach { operationError.value = null }
        .catch {
            operationError.value = "Unable to load assessments. Please try again."
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val activeCount = observeCount(AssessmentStatusFilter.Active)
    private val inactiveCount = observeCount(AssessmentStatusFilter.Inactive)
    val activeAssessmentCount: StateFlow<Int> = activeCount

    private val counts = combine(activeCount, inactiveCount, ::AssessmentCounts)
    private val listContent = combine(
        currentProfile,
        selectedTerm,
        filter,
        query,
        assessments,
    ) { profile: ClassProfileEntity?,
        term: TermEntity?,
        status: AssessmentStatusFilter,
        search: String,
        visibleAssessments: List<AssessmentEntity>? ->
        AssessmentListContent(profile, term, status, search, visibleAssessments, AssessmentCounts(0, 0))
    }.combine(counts) { content, assessmentCounts ->
        content.copy(counts = assessmentCounts)
    }

    val screenState: StateFlow<AssessmentUiState> = combine(
        route,
        listContent,
        formState,
        pendingConfirmation,
        operationError,
    ) { currentRoute, content, form, confirmation, error ->
        when (currentRoute) {
            AssessmentsRoute.Closed -> AssessmentUiState.Closed
            AssessmentsRoute.List -> content.profile?.let { profile ->
                content.term?.let { term ->
                    AssessmentUiState.AssessmentList(
                        profile = profile,
                        term = term,
                        assessments = content.assessments.orEmpty(),
                        isLoading = content.assessments == null,
                        filter = content.filter,
                        query = content.query,
                        activeCount = content.counts.active,
                        inactiveCount = content.counts.inactive,
                        pendingConfirmation = confirmation,
                        errorMessage = error,
                    )
                }
            } ?: AssessmentUiState.Closed
            is AssessmentsRoute.Form -> content.profile?.let { profile ->
                content.term?.let { term ->
                    AssessmentUiState.AssessmentForm(profile, term, currentRoute.assessmentId, form)
                }
            } ?: AssessmentUiState.Closed
        }
    }.catch { emit(AssessmentUiState.Closed) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AssessmentUiState.Closed)

    fun setCurrentProfile(profile: ClassProfileEntity) {
        currentProfile.value = profile
        currentTermId.value = null
        viewModelScope.launch {
            currentTermId.value = termRepository.getTermByProfileAndNumber(profile.id, profile.currentTerm)?.id
        }
    }

    fun openAssessments(profile: ClassProfileEntity) {
        currentProfile.value = profile
        viewModelScope.launch {
            val terms = termRepository.ensureTermsForProfile(profile.id)
            val selectedTerm = terms.firstOrNull { it.termNumber == profile.currentTerm } ?: terms.firstOrNull()
            currentTermId.value = selectedTerm?.id
            filter.value = AssessmentStatusFilter.Active
            query.value = ""
            operationError.value = null
            route.value = AssessmentsRoute.List
        }
    }

    fun selectTerm(termNumber: Int) {
        val profile = currentProfile.value ?: return
        viewModelScope.launch {
            val terms = termRepository.ensureTermsForProfile(profile.id)
            val selectedTerm = terms.firstOrNull { it.termNumber == termNumber } ?: return@launch
            currentTermId.value = selectedTerm.id
            profileRepository.updateCurrentTerm(profile.id, termNumber)
            currentProfile.value = profile.copy(currentTerm = termNumber)
        }
    }

    fun closeAssessments() {
        route.value = AssessmentsRoute.Closed
        pendingConfirmation.value = null
        operationError.value = null
    }

    fun navigateBack() {
        when (route.value) {
            AssessmentsRoute.Closed -> Unit
            AssessmentsRoute.List -> closeAssessments()
            is AssessmentsRoute.Form -> cancelForm()
        }
    }

    fun openAddAssessment() {
        val profile = currentProfile.value ?: return
        val term = selectedTerm.value ?: return
        formState.value = AssessmentFormState()
        route.value = AssessmentsRoute.Form(assessmentId = null)
        currentProfile.value = profile
        currentTermId.value = term.id
    }

    fun openEditAssessment(assessment: AssessmentEntity) {
        val profile = currentProfile.value ?: return
        val term = selectedTerm.value ?: return
        if (assessment.profileId != profile.id || assessment.termId != term.id) return
        formState.value = AssessmentFormState(
            name = assessment.name,
            description = assessment.description,
        )
        route.value = AssessmentsRoute.Form(assessmentId = assessment.id)
    }

    fun onNameChanged(value: String) {
        formState.update { it.copy(name = value, error = null, submitError = null) }
    }

    fun onDescriptionChanged(value: String) {
        formState.update { it.copy(description = value, error = null, submitError = null) }
    }

    fun onSearchChanged(value: String) {
        query.value = value
    }

    fun setFilter(value: AssessmentStatusFilter) {
        filter.value = value
    }

    fun cancelForm() {
        route.value = AssessmentsRoute.List
        formState.value = AssessmentFormState()
    }

    fun submitAssessment(addDuplicateAnyway: Boolean = false) {
        val current = formState.value
        if (current.isSaving) return
        val profile = currentProfile.value ?: return
        val term = selectedTerm.value ?: return
        val assessmentId = (route.value as? AssessmentsRoute.Form)?.assessmentId
        val name = current.name.trim()
        val description = current.description.trim()
        if (name.isBlank()) {
            formState.update { it.copy(error = "Please enter the assessment name.") }
            return
        }

        formState.update { it.copy(name = name, description = description, error = null, submitError = null, isSaving = true) }
        viewModelScope.launch {
            try {
                if (!addDuplicateAnyway && assessmentRepository.hasDuplicateName(profile.id, term.id, name, assessmentId)) {
                    formState.update { it.copy(isSaving = false, duplicateName = name) }
                    return@launch
                }

                val now = System.currentTimeMillis()
                if (assessmentId == null) {
                    assessmentRepository.addAssessment(profile.id, term.id, name, description, now)
                } else if (
                    assessmentRepository.updateAssessmentName(
                        assessmentId,
                        profile.id,
                        term.id,
                        name,
                        description,
                        now,
                    ) == 0
                ) {
                    throw IllegalStateException("Assessment no longer exists in this term")
                }
                route.value = AssessmentsRoute.List
                formState.value = AssessmentFormState()
                pendingConfirmation.value = null
                query.value = ""
                filter.value = AssessmentStatusFilter.Active
            } catch (_: Exception) {
                formState.update {
                    it.copy(isSaving = false, submitError = "Unable to save the assessment. Please try again.")
                }
            }
        }
    }

    fun cancelDuplicateWarning() {
        formState.update { it.copy(duplicateName = null) }
    }

    fun addDuplicateAnyway() {
        formState.update { it.copy(duplicateName = null) }
        submitAssessment(addDuplicateAnyway = true)
    }

    fun requestDeactivate(assessment: AssessmentEntity) {
        if (assessment.profileId != currentProfile.value?.id || assessment.termId != selectedTerm.value?.id) return
        pendingConfirmation.value = AssessmentConfirmation(assessment, AssessmentActionKind.Deactivate)
    }

    fun requestDelete(assessment: AssessmentEntity) {
        if (assessment.profileId != currentProfile.value?.id || assessment.termId != selectedTerm.value?.id) return
        pendingConfirmation.value = AssessmentConfirmation(assessment, AssessmentActionKind.Delete)
    }

    fun cancelConfirmation() {
        pendingConfirmation.value = null
    }

    fun confirmAssessmentAction() {
        val profileId = currentProfile.value?.id ?: return
        val termId = selectedTerm.value?.id ?: return
        val confirmation = pendingConfirmation.value ?: return
        pendingConfirmation.value = null
        operationError.value = null
        viewModelScope.launch {
            try {
                when (confirmation.action) {
                    AssessmentActionKind.Deactivate -> assessmentRepository.setAssessmentActive(
                        confirmation.assessment.id,
                        profileId,
                        termId,
                        false,
                        System.currentTimeMillis(),
                    )
                    AssessmentActionKind.Delete -> assessmentRepository.deleteAssessment(
                        confirmation.assessment.id,
                        profileId,
                        termId,
                    )
                }
            } catch (_: Exception) {
                operationError.value = "Unable to update this assessment. Please try again."
            }
        }
    }

    fun reactivateAssessment(assessment: AssessmentEntity) {
        val profileId = currentProfile.value?.id ?: return
        val termId = selectedTerm.value?.id ?: return
        if (assessment.profileId != profileId || assessment.termId != termId) return
        operationError.value = null
        viewModelScope.launch {
            try {
                assessmentRepository.setAssessmentActive(assessment.id, profileId, termId, true, System.currentTimeMillis())
            } catch (_: Exception) {
                operationError.value = "Unable to reactivate this assessment. Please try again."
            }
        }
    }

    private fun observeCount(status: AssessmentStatusFilter): StateFlow<Int> =
        combine(currentProfile, selectedTerm) { profile, term ->
            profile?.id to term?.id
        }.distinctUntilChanged().flatMapLatest { (profileId, termId) ->
            if (profileId == null || termId == null) {
                flowOf(0)
            } else {
                assessmentRepository.observeCount(profileId, termId, status.isActive)
            }
        }.catch { emit(0) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

class AssessmentsViewModelFactory(
    private val assessmentRepository: AssessmentRepository,
    private val termRepository: TermRepository,
    private val profileRepository: ClassProfileRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(AssessmentsViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        return AssessmentsViewModel(assessmentRepository, termRepository, profileRepository) as T
    }
}
