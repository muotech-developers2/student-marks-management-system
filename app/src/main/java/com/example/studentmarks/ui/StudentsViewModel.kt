package com.example.studentmarks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studentmarks.data.database.ClassProfileEntity
import com.example.studentmarks.data.database.StudentEntity
import com.example.studentmarks.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private sealed interface StudentsRoute {
    data object Closed : StudentsRoute
    data object List : StudentsRoute
    data class Form(val studentId: Long?) : StudentsRoute
}

private data class StudentSearchSpec(
    val profileId: Long?,
    val isActive: Boolean,
    val query: String,
)

private data class StudentCounts(val active: Int, val inactive: Int)

private data class StudentListContent(
    val profile: ClassProfileEntity?,
    val filter: StudentStatusFilter,
    val query: String,
    val students: List<StudentEntity>?,
    val counts: StudentCounts,
)

@OptIn(ExperimentalCoroutinesApi::class)
class StudentsViewModel(
    private val repository: StudentRepository,
) : ViewModel() {
    private val currentProfile = MutableStateFlow<ClassProfileEntity?>(null)
    private val route = MutableStateFlow<StudentsRoute>(StudentsRoute.Closed)
    private val filter = MutableStateFlow(StudentStatusFilter.Active)
    private val query = MutableStateFlow("")
    private val formState = MutableStateFlow(StudentFormState())
    private val pendingConfirmation = MutableStateFlow<StudentConfirmation?>(null)
    private val operationError = MutableStateFlow<String?>(null)

    private val searchSpec = combine(currentProfile, filter, query) { profile, status, search ->
        StudentSearchSpec(profile?.id, status.isActive, search)
    }
    private val students = searchSpec.flatMapLatest { spec ->
        spec.profileId?.let { repository.searchStudents(it, spec.isActive, spec.query) }
            ?: flowOf(emptyList())
    }.onEach { operationError.value = null }
        .catch {
            operationError.value = "Unable to load students. Please try again."
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val activeCount = observeCount(StudentStatusFilter.Active)
    private val inactiveCount = observeCount(StudentStatusFilter.Inactive)
    val activeStudentCount: StateFlow<Int> = activeCount

    private val counts = combine(activeCount, inactiveCount, ::StudentCounts)
    private val listContent = combine(currentProfile, filter, query, students, counts) {
            profile, status, search, visibleStudents, studentCounts ->
        StudentListContent(profile, status, search, visibleStudents, studentCounts)
    }

    val screenState: StateFlow<StudentUiState> = combine(
        route,
        listContent,
        formState,
        pendingConfirmation,
        operationError,
    ) { currentRoute, content, form, confirmation, error ->
        when (currentRoute) {
            StudentsRoute.Closed -> StudentUiState.Closed
            StudentsRoute.List -> content.profile?.let {
                StudentUiState.StudentList(
                    profile = it,
                    students = content.students.orEmpty(),
                    isLoading = content.students == null,
                    filter = content.filter,
                    query = content.query,
                    activeCount = content.counts.active,
                    inactiveCount = content.counts.inactive,
                    pendingConfirmation = confirmation,
                    errorMessage = error,
                )
            } ?: StudentUiState.Closed
            is StudentsRoute.Form -> content.profile?.let {
                StudentUiState.StudentForm(it, currentRoute.studentId, form)
            } ?: StudentUiState.Closed
        }
    }.catch { emit(StudentUiState.Closed) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudentUiState.Closed)

    fun setCurrentProfile(profile: ClassProfileEntity) {
        currentProfile.value = profile
    }

    fun openStudents(profile: ClassProfileEntity) {
        currentProfile.value = profile
        filter.value = StudentStatusFilter.Active
        query.value = ""
        operationError.value = null
        route.value = StudentsRoute.List
    }

    fun closeStudents() {
        route.value = StudentsRoute.Closed
        pendingConfirmation.value = null
        operationError.value = null
    }

    fun navigateBack() {
        when (route.value) {
            StudentsRoute.Closed -> Unit
            StudentsRoute.List -> closeStudents()
            is StudentsRoute.Form -> cancelForm()
        }
    }

    fun openAddStudent() {
        formState.value = StudentFormState()
        route.value = StudentsRoute.Form(studentId = null)
    }

    fun openEditStudent(student: StudentEntity) {
        if (student.profileId != currentProfile.value?.id) return
        formState.value = StudentFormState(name = student.name)
        route.value = StudentsRoute.Form(studentId = student.id)
    }

    fun onNameChanged(value: String) {
        formState.update { it.copy(name = value, error = null, submitError = null) }
    }

    fun onSearchChanged(value: String) {
        query.value = value
    }

    fun setFilter(value: StudentStatusFilter) {
        filter.value = value
    }

    fun cancelForm() {
        route.value = StudentsRoute.List
        formState.value = StudentFormState()
    }

    fun submitStudent(addDuplicateAnyway: Boolean = false) {
        val current = formState.value
        if (current.isSaving) return
        val profile = currentProfile.value ?: return
        val studentId = (route.value as? StudentsRoute.Form)?.studentId ?: run {
            if (route.value !is StudentsRoute.Form) return
            null
        }
        val name = current.name.trim()
        if (name.isBlank()) {
            formState.update { it.copy(error = "Please enter the student name.") }
            return
        }

        formState.update { it.copy(name = name, error = null, submitError = null, isSaving = true) }
        viewModelScope.launch {
            try {
                if (!addDuplicateAnyway && repository.hasDuplicateName(profile.id, name, studentId)) {
                    formState.update { it.copy(isSaving = false, duplicateName = name) }
                    return@launch
                }

                val now = System.currentTimeMillis()
                if (studentId == null) {
                    repository.addStudent(profile.id, name, now)
                } else if (repository.updateStudentName(studentId, profile.id, name, now) == 0) {
                    throw IllegalStateException("Student no longer exists in this class")
                }
                route.value = StudentsRoute.List
                formState.value = StudentFormState()
                pendingConfirmation.value = null
                query.value = ""
                filter.value = StudentStatusFilter.Active
            } catch (_: Exception) {
                formState.update {
                    it.copy(isSaving = false, submitError = "Unable to save the student. Please try again.")
                }
            }
        }
    }

    fun cancelDuplicateWarning() {
        formState.update { it.copy(duplicateName = null) }
    }

    fun addDuplicateAnyway() {
        formState.update { it.copy(duplicateName = null) }
        submitStudent(addDuplicateAnyway = true)
    }

    fun requestDeactivate(student: StudentEntity) {
        if (student.profileId != currentProfile.value?.id) return
        pendingConfirmation.value = StudentConfirmation(student, StudentActionKind.Deactivate)
    }

    fun requestDelete(student: StudentEntity) {
        if (student.profileId != currentProfile.value?.id) return
        pendingConfirmation.value = StudentConfirmation(student, StudentActionKind.Delete)
    }

    fun cancelConfirmation() {
        pendingConfirmation.value = null
    }

    fun confirmStudentAction() {
        val profileId = currentProfile.value?.id ?: return
        val confirmation = pendingConfirmation.value ?: return
        pendingConfirmation.value = null
        operationError.value = null
        viewModelScope.launch {
            try {
                when (confirmation.action) {
                    StudentActionKind.Deactivate -> repository.setStudentActive(
                        confirmation.student.id,
                        profileId,
                        false,
                        System.currentTimeMillis(),
                    )
                    StudentActionKind.Delete -> repository.deleteStudent(confirmation.student.id, profileId)
                }
            } catch (_: Exception) {
                operationError.value = "Unable to update this student. Please try again."
            }
        }
    }

    fun reactivateStudent(student: StudentEntity) {
        val profileId = currentProfile.value?.id ?: return
        if (student.profileId != profileId) return
        operationError.value = null
        viewModelScope.launch {
            try {
                repository.setStudentActive(student.id, profileId, true, System.currentTimeMillis())
            } catch (_: Exception) {
                operationError.value = "Unable to reactivate this student. Please try again."
            }
        }
    }

    private fun observeCount(status: StudentStatusFilter): StateFlow<Int> =
        currentProfile.map { it?.id }.distinctUntilChanged().flatMapLatest { profileId ->
            profileId?.let { repository.observeCount(it, status.isActive) } ?: flowOf(0)
        }.catch { emit(0) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

class StudentsViewModelFactory(
    private val repository: StudentRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(StudentsViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        return StudentsViewModel(repository) as T
    }
}