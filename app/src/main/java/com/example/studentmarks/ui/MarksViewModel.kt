package com.example.studentmarks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studentmarks.data.database.ClassProfileEntity
import com.example.studentmarks.data.repository.AssessmentRepository
import com.example.studentmarks.data.repository.ClassProfileRepository
import com.example.studentmarks.data.repository.MarkRepository
import com.example.studentmarks.data.repository.StudentRepository
import com.example.studentmarks.data.repository.SubjectRepository
import com.example.studentmarks.data.repository.TermRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MarksViewModel(
    private val termRepository: TermRepository,
    private val assessmentRepository: AssessmentRepository,
    private val subjectRepository: SubjectRepository,
    private val studentRepository: StudentRepository,
    private val markRepository: MarkRepository,
    private val profileRepository: ClassProfileRepository,
) : ViewModel() {

    private var currentProfile: ClassProfileEntity? = null
    private val screenFlow = MutableStateFlow<MarksScreenState>(MarksScreenState.Closed)
    val screenState: StateFlow<MarksScreenState> = screenFlow

    fun openMarks(profile: ClassProfileEntity) {
        currentProfile = profile
        viewModelScope.launch {
            val terms = termRepository.ensureTermsForProfile(profile.id)
            val selectedTerm = terms.firstOrNull { it.termNumber == profile.currentTerm } ?: terms.firstOrNull()
            val availableAssessments = selectedTerm?.let { assessmentRepository.getAssessmentsForTerm(profile.id, it.id) } ?: emptyList()
            val activeSubjects = subjectRepository.getActiveSubjects(profile.id)
            val selectedAssessment = availableAssessments.firstOrNull { it.isActive }
                ?: availableAssessments.firstOrNull()
            val selectedSubject = activeSubjects.firstOrNull()
            val nextState = MarksUiState(
                profile = profile,
                selectedTerm = selectedTerm,
                selectedAssessment = selectedAssessment,
                selectedSubject = selectedSubject,
                availableTerms = terms,
                availableAssessments = availableAssessments,
                availableSubjects = activeSubjects,
                isLoading = selectedAssessment != null && selectedSubject != null,
            )
            screenFlow.value = MarksScreenState.Entry(nextState)
            refreshRows(nextState)
        }
    }

    fun selectTerm(termNumber: Int) {
        val profile = currentProfile ?: return
        viewModelScope.launch {
            val terms = termRepository.ensureTermsForProfile(profile.id)
            val selectedTerm = terms.firstOrNull { it.termNumber == termNumber } ?: return@launch
            val availableAssessments = assessmentRepository.getAssessmentsForTerm(profile.id, selectedTerm.id)
            val selectedAssessment = availableAssessments.firstOrNull { it.isActive } ?: availableAssessments.firstOrNull()
            val activeSubjects = subjectRepository.getActiveSubjects(profile.id)
            val selectedSubject = activeSubjects.firstOrNull()
            val state = (screenState as? MarksScreenState.Entry)?.state ?: return@launch
            val updated = state.copy(
                selectedTerm = selectedTerm,
                selectedAssessment = selectedAssessment,
                selectedSubject = selectedSubject,
                availableAssessments = availableAssessments,
                availableSubjects = activeSubjects,
                hasUnsavedChanges = false,
                validationError = null,
                errorMessage = null,
                studentRows = emptyList(),
                isLoading = selectedAssessment != null && selectedSubject != null,
            )
            screenFlow.value = MarksScreenState.Entry(updated)
            profileRepository.updateCurrentTerm(profile.id, termNumber)
            refreshRows(updated)
        }
    }

    fun selectAssessment(assessmentId: Long) {
        val state = (screenState as? MarksScreenState.Entry)?.state ?: return
        val assessment = state.availableAssessments.firstOrNull { it.id == assessmentId } ?: return
        val updated = state.copy(selectedAssessment = assessment, isLoading = true)
        screenFlow.value = MarksScreenState.Entry(updated)
        refreshRows(updated)
    }

    fun selectSubject(subjectId: Long) {
        val state = (screenState as? MarksScreenState.Entry)?.state ?: return
        val subject = state.availableSubjects.firstOrNull { it.id == subjectId } ?: return
        val updated = state.copy(selectedSubject = subject, isLoading = true)
        screenFlow.value = MarksScreenState.Entry(updated)
        refreshRows(updated)
    }

    fun onSearchChanged(query: String) {
        val state = (screenState as? MarksScreenState.Entry)?.state ?: return
        screenFlow.value = MarksScreenState.Entry(state.copy(searchQuery = query))
    }

    fun onMarkChanged(studentId: Long, value: String) {
        val state = (screenState as? MarksScreenState.Entry)?.state ?: return
        val updatedRows = state.studentRows.map { row ->
            if (row.student.id == studentId) row.copy(value = value) else row
        }
        screenFlow.value = MarksScreenState.Entry(
            state.copy(
                studentRows = updatedRows,
                hasUnsavedChanges = true,
                validationError = null,
                errorMessage = null,
            ),
        )
    }

    fun clearMark(studentId: Long) {
        val state = (screenState as? MarksScreenState.Entry)?.state ?: return
        val row = state.studentRows.firstOrNull { it.student.id == studentId } ?: return
        if (row.existingValue == null && row.value.isBlank()) return
        onMarkChanged(studentId, "")
        screenFlow.value = MarksScreenState.Entry(
            (screenFlow.value as? MarksScreenState.Entry)?.state?.copy(
                pendingDeleteStudentId = studentId,
                showDiscardDialog = false,
            ) ?: state,
        )
    }

    fun saveMarks() {
        val state = (screenState as? MarksScreenState.Entry)?.state ?: return
        if (state.isSaving || state.studentRows.isEmpty()) return
        val profile = state.profile ?: return
        val term = state.selectedTerm ?: return
        val assessment = state.selectedAssessment ?: return
        val subject = state.selectedSubject ?: return
        val rows = state.studentRows
        screenFlow.value = MarksScreenState.Entry(state.copy(isSaving = true))

        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                for (row in rows) {
                    val valueText = row.value.trim()
                    if (valueText.isEmpty()) {
                        markRepository.deleteMark(profile.id, term.id, assessment.id, row.student.id, subject.id)
                        continue
                    }
                    val value = markRepository.normalizeInput(valueText) ?: run {
                        throw IllegalArgumentException("Enter a valid mark between 0 and 100.")
                    }
                    markRepository.upsertMark(profile.id, term.id, assessment.id, row.student.id, subject.id, value, now)
                }
                val refreshed = state.copy(
                    hasUnsavedChanges = false,
                    validationError = null,
                    errorMessage = null,
                    isSaving = false,
                    pendingDeleteStudentId = null,
                )
                screenFlow.value = MarksScreenState.Entry(refreshed)
                refreshRows(refreshed)
            } catch (e: IllegalArgumentException) {
                screenFlow.value = MarksScreenState.Entry(state.copy(isSaving = false, validationError = e.message ?: "Enter a valid mark."))
            } catch (_: Exception) {
                screenFlow.value = MarksScreenState.Entry(state.copy(isSaving = false, errorMessage = "Unable to save marks. Please try again."))
            }
        }
    }

    fun leaveMarks() {
        val state = (screenState as? MarksScreenState.Entry)?.state ?: return
        if (state.hasUnsavedChanges) {
            screenFlow.value = MarksScreenState.Entry(state.copy(showDiscardDialog = true))
        } else {
            screenFlow.value = MarksScreenState.Closed
        }
    }

    fun navigateBack() {
        if ((screenState as? MarksScreenState.Entry)?.state?.hasUnsavedChanges == true) {
            screenFlow.value = MarksScreenState.Entry(((screenFlow.value as? MarksScreenState.Entry)?.state ?: return).copy(showDiscardDialog = true))
        } else {
            screenFlow.value = MarksScreenState.Closed
        }
    }

    fun cancelDiscardDialog() {
        val state = (screenState as? MarksScreenState.Entry)?.state ?: return
        screenFlow.value = MarksScreenState.Entry(state.copy(showDiscardDialog = false))
    }

    fun discardChanges() {
        val state = (screenState as? MarksScreenState.Entry)?.state ?: return
        screenFlow.value = MarksScreenState.Entry(state.copy(hasUnsavedChanges = false, showDiscardDialog = false, studentRows = state.studentRows.map { it.copy(value = it.existingValue?.toString() ?: "") }))
    }

    private fun refreshRows(baseState: MarksUiState) {
        val profile = baseState.profile ?: return
        val term = baseState.selectedTerm ?: return
        val assessment = baseState.selectedAssessment
        val subject = baseState.selectedSubject
        if (assessment == null || subject == null) {
            screenFlow.value = MarksScreenState.Entry(
                baseState.copy(
                    studentRows = emptyList(),
                    completedCount = 0,
                    totalCount = 0,
                    isLoading = false,
                ),
            )
            return
        }
        viewModelScope.launch {
            try {
                val students = studentRepository.getStudentsForProfile(profile.id).filter { it.isActive }
                val currentMarks = markRepository.getMarksForAssessment(profile.id, term.id, assessment.id, subject.id)
                val existingByStudent = currentMarks.associateBy { it.studentId }
                val searchValue = baseState.searchQuery.trim().lowercase()
                val filtered = students.filter { student ->
                    searchValue.isBlank() || student.name.lowercase().contains(searchValue)
                }.map { student ->
                    val existing = existingByStudent[student.id]
                    StudentMarkRow(
                        student = student,
                        value = existing?.markValue?.toString() ?: "",
                        existingValue = existing?.markValue,
                    )
                }

                val completed = currentMarks.size
                val total = students.size
                val nextState = baseState.copy(
                    studentRows = filtered,
                    completedCount = completed,
                    totalCount = total,
                    isLoading = false,
                    errorMessage = null,
                    validationError = null,
                )
                screenFlow.value = MarksScreenState.Entry(nextState)
            } catch (_: Exception) {
                screenFlow.value = MarksScreenState.Entry(baseState.copy(isLoading = false, errorMessage = "Unable to load marks."))
            }
        }
    }
}

class MarksViewModelFactory(
    private val termRepository: TermRepository,
    private val assessmentRepository: AssessmentRepository,
    private val subjectRepository: SubjectRepository,
    private val studentRepository: StudentRepository,
    private val markRepository: MarkRepository,
    private val profileRepository: ClassProfileRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(MarksViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        return MarksViewModel(
            termRepository,
            assessmentRepository,
            subjectRepository,
            studentRepository,
            markRepository,
            profileRepository,
        ) as T
    }
}
