package com.example.studentmarks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studentmarks.data.database.AssessmentEntity
import com.example.studentmarks.data.database.ClassProfileEntity
import com.example.studentmarks.data.database.StudentEntity
import com.example.studentmarks.data.database.SubjectEntity
import com.example.studentmarks.data.database.TermEntity
import com.example.studentmarks.data.repository.ExcelExportRepository
import com.example.studentmarks.data.repository.ReportsRepository
import com.example.studentmarks.data.repository.TermRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class ReportType { HOME, STUDENT, SUBJECT, CLASS }

data class ReportStatSummary(
    val entered: Int = 0,
    val missing: Int = 0,
    val average: Double? = null,
    val highest: Double? = null,
    val lowest: Double? = null,
)

data class StudentSubjectMark(
    val subject: SubjectEntity,
    val mark: Double?,
)

data class StudentReportDetail(
    val student: StudentEntity,
    val profile: ClassProfileEntity,
    val term: TermEntity,
    val selectedAssessment: AssessmentEntity?,
    val subjectMarks: List<StudentSubjectMark>,
    val summary: ReportStatSummary,
)

data class SubjectStudentMarkRow(
    val student: StudentEntity,
    val mark: Double?,
)

data class SubjectReportDetail(
    val subject: SubjectEntity,
    val profile: ClassProfileEntity,
    val term: TermEntity,
    val selectedAssessment: AssessmentEntity?,
    val rows: List<SubjectStudentMarkRow>,
    val summary: ReportStatSummary,
)

data class ClassSubjectStats(
    val subject: SubjectEntity,
    val entered: Int,
    val missing: Int,
    val average: Double?,
    val highest: Double?,
    val lowest: Double?,
)

data class ClassAssessmentStats(
    val assessment: AssessmentEntity,
    val entered: Int,
    val missing: Int,
    val average: Double?,
    val highest: Double?,
    val lowest: Double?,
)

data class StudentOverviewRow(
    val student: StudentEntity,
    val entered: Int,
    val missing: Int,
)

data class ClassReportDetail(
    val profile: ClassProfileEntity,
    val term: TermEntity,
    val selectedAssessment: AssessmentEntity?,
    val activeStudents: Int,
    val activeSubjects: Int,
    val marksEntered: Int,
    val marksMissing: Int,
    val subjectStats: List<ClassSubjectStats>,
    val assessmentStats: List<ClassAssessmentStats>,
    val studentOverview: List<StudentOverviewRow>,
)

data class WorkbookExport(
    val fileName: String,
    val bytes: ByteArray,
)

data class DashboardWorkSummary(
    val enteredMarkCount: Int = 0,
    val expectedMarkCount: Int = 0,
    val missingMarkCount: Int = 0,
    val recentAssessmentName: String? = null,
    val isAvailable: Boolean = true,
)

data class ReportsUiState(
    val profile: ClassProfileEntity? = null,
    val reportType: ReportType = ReportType.HOME,
    val terms: List<TermEntity> = emptyList(),
    val selectedTerm: TermEntity? = null,
    val assessments: List<AssessmentEntity> = emptyList(),
    val selectedAssessmentId: Long? = null,
    val students: List<StudentEntity> = emptyList(),
    val selectedStudentId: Long? = null,
    val studentSearchQuery: String = "",
    val subjects: List<SubjectEntity> = emptyList(),
    val selectedSubjectId: Long? = null,
    val subjectSearchQuery: String = "",
    val includeInactiveStudents: Boolean = false,
    val includeInactiveSubjects: Boolean = false,
    val studentReport: StudentReportDetail? = null,
    val subjectReport: SubjectReportDetail? = null,
    val classReport: ClassReportDetail? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val exportMessage: String? = null,
    val isExporting: Boolean = false,
)

sealed interface ReportsScreenState {
    data object Closed : ReportsScreenState
    data class Open(val state: ReportsUiState) : ReportsScreenState
}

class ReportsViewModel(
    private val termRepository: TermRepository,
    private val repository: ReportsRepository,
    private val excelExportRepository: ExcelExportRepository,
) : ViewModel() {

    private val screenFlow = MutableStateFlow<ReportsScreenState>(ReportsScreenState.Closed)
    val screenState: StateFlow<ReportsScreenState> = screenFlow
    private val _dashboardWorkSummary = MutableStateFlow<DashboardWorkSummary?>(null)
    val dashboardWorkSummary: StateFlow<DashboardWorkSummary?> = _dashboardWorkSummary
    private var dashboardSummaryJob: Job? = null

    fun refreshDashboardSummary(profile: ClassProfileEntity, term: TermEntity?) {
        dashboardSummaryJob?.cancel()
        if (term == null) {
            _dashboardWorkSummary.value = DashboardWorkSummary()
            return
        }
        _dashboardWorkSummary.value = null
        dashboardSummaryJob = viewModelScope.launch {
            try {
                val students = repository.getStudentsForProfile(profile.id, includeInactive = false)
                val subjects = repository.getSubjectsForProfile(profile.id, includeInactive = false)
                val assessments = repository.getAssessmentsForTerm(profile.id, term.id).filter { it.isActive }
                val studentIds = students.mapTo(mutableSetOf()) { it.id }
                val subjectIds = subjects.mapTo(mutableSetOf()) { it.id }
                val marks = assessments.flatMap { assessment ->
                    repository.getMarksForAssessment(profile.id, term.id, assessment.id)
                }.filter { it.studentId in studentIds && it.subjectId in subjectIds }
                val latestMark = marks.maxByOrNull { it.updatedAtMillis }
                val expectedCount = students.size * subjects.size * assessments.size

                _dashboardWorkSummary.value = DashboardWorkSummary(
                    enteredMarkCount = marks.size,
                    expectedMarkCount = expectedCount,
                    missingMarkCount = maxOf(expectedCount - marks.size, 0),
                    recentAssessmentName = assessments.firstOrNull { it.id == latestMark?.assessmentId }?.name,
                )
            } catch (_: Exception) {
                _dashboardWorkSummary.value = DashboardWorkSummary(isAvailable = false)
            }
        }
    }

    fun openReports(profile: ClassProfileEntity) {
        viewModelScope.launch {
            val terms = termRepository.ensureTermsForProfile(profile.id)
            val selectedTerm = terms.firstOrNull { it.termNumber == profile.currentTerm } ?: terms.firstOrNull() ?: return@launch
            val initial = ReportsUiState(
                profile = profile,
                reportType = ReportType.HOME,
                terms = terms,
                selectedTerm = selectedTerm,
            )
            screenFlow.value = ReportsScreenState.Open(initial)
            refreshReports(initial)
        }
    }

    fun selectReportType(type: ReportType) {
        val current = (screenFlow.value as? ReportsScreenState.Open)?.state ?: return
        val next = current.copy(reportType = type)
        screenFlow.value = ReportsScreenState.Open(next)
        refreshReports(next)
    }

    fun setSelectedTerm(termNumber: Int) {
        val current = (screenFlow.value as? ReportsScreenState.Open)?.state ?: return
        val selected = current.terms.firstOrNull { it.termNumber == termNumber } ?: return
        val next = current.copy(selectedTerm = selected, selectedAssessmentId = null)
        screenFlow.value = ReportsScreenState.Open(next)
        refreshReports(next)
    }

    fun setSelectedAssessment(assessmentId: Long?) {
        val current = (screenFlow.value as? ReportsScreenState.Open)?.state ?: return
        screenFlow.value = ReportsScreenState.Open(current.copy(selectedAssessmentId = assessmentId))
        refreshReports(current.copy(selectedAssessmentId = assessmentId))
    }

    fun setStudentSelection(studentId: Long?) {
        val current = (screenFlow.value as? ReportsScreenState.Open)?.state ?: return
        val next = current.copy(selectedStudentId = studentId)
        screenFlow.value = ReportsScreenState.Open(next)
        refreshReports(next)
    }

    fun setSubjectSelection(subjectId: Long?) {
        val current = (screenFlow.value as? ReportsScreenState.Open)?.state ?: return
        val next = current.copy(selectedSubjectId = subjectId)
        screenFlow.value = ReportsScreenState.Open(next)
        refreshReports(next)
    }

    fun onStudentSearchChanged(value: String) {
        val current = (screenFlow.value as? ReportsScreenState.Open)?.state ?: return
        screenFlow.value = ReportsScreenState.Open(current.copy(studentSearchQuery = value))
    }

    fun onSubjectSearchChanged(value: String) {
        val current = (screenFlow.value as? ReportsScreenState.Open)?.state ?: return
        screenFlow.value = ReportsScreenState.Open(current.copy(subjectSearchQuery = value))
    }

    fun toggleIncludeInactiveStudents() {
        val current = (screenFlow.value as? ReportsScreenState.Open)?.state ?: return
        val next = current.copy(includeInactiveStudents = !current.includeInactiveStudents, selectedStudentId = null)
        screenFlow.value = ReportsScreenState.Open(next)
        refreshReports(next)
    }

    fun toggleIncludeInactiveSubjects() {
        val current = (screenFlow.value as? ReportsScreenState.Open)?.state ?: return
        val next = current.copy(includeInactiveSubjects = !current.includeInactiveSubjects, selectedSubjectId = null)
        screenFlow.value = ReportsScreenState.Open(next)
        refreshReports(next)
    }

    fun navigateBack() {
        screenFlow.value = ReportsScreenState.Closed
    }

    private fun refreshReports(baseState: ReportsUiState) {
        val profile = baseState.profile ?: return
        val term = baseState.selectedTerm ?: return
        viewModelScope.launch {
            try {
                val assessments = repository.getAssessmentsForTerm(profile.id, term.id)
                val students = repository.getStudentsForProfile(profile.id, baseState.includeInactiveStudents)
                val subjects = repository.getSubjectsForProfile(profile.id, baseState.includeInactiveSubjects)
                val filteredStudents = students.filter { it.name.contains(baseState.studentSearchQuery, ignoreCase = true) }
                val filteredSubjects = subjects.filter { it.name.contains(baseState.subjectSearchQuery, ignoreCase = true) }
                val selectedStudentId = if (baseState.selectedStudentId != null && filteredStudents.any { it.id == baseState.selectedStudentId }) {
                    baseState.selectedStudentId
                } else if (filteredStudents.isNotEmpty()) {
                    filteredStudents.first().id
                } else {
                    null
                }
                val selectedSubjectId = if (baseState.selectedSubjectId != null && filteredSubjects.any { it.id == baseState.selectedSubjectId }) {
                    baseState.selectedSubjectId
                } else if (filteredSubjects.isNotEmpty()) {
                    filteredSubjects.first().id
                } else {
                    null
                }
                val selectedAssessment = assessments.firstOrNull { it.id == baseState.selectedAssessmentId }
                val studentReport = if (selectedStudentId != null) buildStudentReport(profile, term, selectedAssessment, filteredStudents, filteredSubjects, selectedStudentId) else null
                val subjectReport = if (selectedSubjectId != null) buildSubjectReport(profile, term, selectedAssessment, filteredStudents, selectedSubjectId) else null
                val classReport = buildClassReport(profile, term, selectedAssessment, filteredStudents, filteredSubjects)

                screenFlow.value = ReportsScreenState.Open(
                    baseState.copy(
                        assessments = assessments,
                        students = filteredStudents,
                        selectedStudentId = selectedStudentId,
                        subjects = filteredSubjects,
                        selectedSubjectId = selectedSubjectId,
                        studentReport = studentReport,
                        subjectReport = subjectReport,
                        classReport = classReport,
                        isLoading = false,
                        errorMessage = null,
                    ),
                )
            } catch (_: Exception) {
                screenFlow.value = ReportsScreenState.Open(
                    baseState.copy(isLoading = false, errorMessage = "Unable to load this report."),
                )
            }
        }
    }

    private suspend fun buildStudentReport(
        profile: ClassProfileEntity,
        term: TermEntity,
        selectedAssessment: AssessmentEntity?,
        students: List<StudentEntity>,
        subjects: List<SubjectEntity>,
        studentId: Long,
    ): StudentReportDetail {
        val student = students.firstOrNull { it.id == studentId }
            ?: return StudentReportDetail(
                student = StudentEntity(profileId = profile.id, name = "Unknown", isActive = true, createdAtMillis = 0L, updatedAtMillis = 0L),
                profile = profile,
                term = term,
                selectedAssessment = selectedAssessment,
                subjectMarks = emptyList(),
                summary = ReportStatSummary(),
            )
        val marks = if (selectedAssessment != null) {
            repository.getMarksForAssessment(profile.id, term.id, selectedAssessment.id)
        } else {
            repository.getMarksForStudentTerm(profile.id, studentId, term.id)
        }
        val subjectMarks = subjects.map { subject ->
            val mark = marks.firstOrNull { it.studentId == studentId && it.subjectId == subject.id }?.markValue
            StudentSubjectMark(subject = subject, mark = mark)
        }
        val values = subjectMarks.mapNotNull { it.mark }
        return StudentReportDetail(
            student = student,
            profile = profile,
            term = term,
            selectedAssessment = selectedAssessment,
            subjectMarks = subjectMarks,
            summary = summaryFromMarks(values),
        )
    }

    private suspend fun buildSubjectReport(
        profile: ClassProfileEntity,
        term: TermEntity,
        selectedAssessment: AssessmentEntity?,
        students: List<StudentEntity>,
        subjectId: Long,
    ): SubjectReportDetail {
        val subject = repository.getSubjectsForProfile(profile.id, true).firstOrNull { it.id == subjectId }
            ?: return SubjectReportDetail(
                subject = SubjectEntity(profileId = profile.id, name = "Unknown", isActive = true, createdAtMillis = 0L, updatedAtMillis = 0L),
                profile = profile,
                term = term,
                selectedAssessment = selectedAssessment,
                rows = emptyList(),
                summary = ReportStatSummary(),
            )
        val relevantMarks = if (selectedAssessment == null) {
            repository.getMarksForSubjectTerm(profile.id, subjectId, term.id)
        } else {
            repository.getMarksForAssessment(profile.id, term.id, selectedAssessment.id).filter { it.subjectId == subjectId }
        }
        val rows = students.map { student ->
            SubjectStudentMarkRow(student = student, mark = relevantMarks.firstOrNull { it.studentId == student.id }?.markValue)
        }
        val values = rows.mapNotNull { it.mark }
        return SubjectReportDetail(
            subject = subject,
            profile = profile,
            term = term,
            selectedAssessment = selectedAssessment,
            rows = rows,
            summary = summaryFromMarks(values),
        )
    }

    private suspend fun buildClassReport(
        profile: ClassProfileEntity,
        term: TermEntity,
        selectedAssessment: AssessmentEntity?,
        students: List<StudentEntity>,
        subjects: List<SubjectEntity>,
    ): ClassReportDetail {
        val allMarks = if (selectedAssessment == null) {
            repository.getMarksForProfileAndTerm(profile.id, term.id)
        } else {
            repository.getMarksForAssessment(profile.id, term.id, selectedAssessment.id)
        }
        val subjectStats = subjects.map { subject ->
            val values = allMarks.filter { it.subjectId == subject.id }.map { it.markValue }
            ClassSubjectStats(
                subject = subject,
                entered = values.size,
                missing = maxOf(students.size - values.size, 0),
                average = values.averageOrNull(),
                highest = values.maxOrNull(),
                lowest = values.minOrNull(),
            )
        }
        val assessmentStats = if (selectedAssessment == null) {
            repository.getAssessmentsForTerm(profile.id, term.id).map { assessment ->
                val values = repository.getMarksForAssessment(profile.id, term.id, assessment.id).map { it.markValue }
                ClassAssessmentStats(
                    assessment = assessment,
                    entered = values.size,
                    missing = maxOf((students.size * subjects.size) - values.size, 0),
                    average = values.averageOrNull(),
                    highest = values.maxOrNull(),
                    lowest = values.minOrNull(),
                )
            }
        } else {
            val values = allMarks.map { it.markValue }
            listOf(
                ClassAssessmentStats(
                    assessment = selectedAssessment,
                    entered = values.size,
                    missing = maxOf((students.size * subjects.size) - values.size, 0),
                    average = values.averageOrNull(),
                    highest = values.maxOrNull(),
                    lowest = values.minOrNull(),
                ),
            )
        }
        val studentOverview = students.map { student ->
            val entered = if (selectedAssessment == null) {
                repository.getMarksForStudentTerm(profile.id, student.id, term.id).size
            } else {
                repository.getMarksForAssessment(profile.id, term.id, selectedAssessment.id).count { it.studentId == student.id }
            }
            StudentOverviewRow(student = student, entered = entered, missing = maxOf(subjects.size - entered, 0))
        }
        return ClassReportDetail(
            profile = profile,
            term = term,
            selectedAssessment = selectedAssessment,
            activeStudents = students.size,
            activeSubjects = subjects.size,
            marksEntered = allMarks.size,
            marksMissing = maxOf((students.size * subjects.size) - allMarks.size, 0),
            subjectStats = subjectStats,
            assessmentStats = assessmentStats,
            studentOverview = studentOverview,
        )
    }

    suspend fun exportStudentWorkbook(): WorkbookExport? {
        val current = (screenFlow.value as? ReportsScreenState.Open)?.state ?: return null
        val profile = current.profile ?: return null
        val term = current.selectedTerm ?: return null
        val studentId = current.selectedStudentId ?: return null
        val assessmentId = current.selectedAssessmentId
        val fileName = excelExportRepository.buildFileName(
            profile.classGrade,
            "Student_Marks",
            term.termNumber,
            current.assessments.firstOrNull { it.id == assessmentId }?.name,
        )
        val bytes = excelExportRepository.generateStudentWorkbook(profile.id, term.id, studentId, assessmentId)
        return WorkbookExport(fileName, bytes)
    }

    suspend fun exportSubjectWorkbook(): WorkbookExport? {
        val current = (screenFlow.value as? ReportsScreenState.Open)?.state ?: return null
        val profile = current.profile ?: return null
        val term = current.selectedTerm ?: return null
        val subjectId = current.selectedSubjectId ?: return null
        val assessmentId = current.selectedAssessmentId
        val fileName = excelExportRepository.buildFileName(
            profile.classGrade,
            "Subject_Marks",
            term.termNumber,
            current.assessments.firstOrNull { it.id == assessmentId }?.name,
        )
        val bytes = excelExportRepository.generateSubjectWorkbook(profile.id, term.id, subjectId, assessmentId)
        return WorkbookExport(fileName, bytes)
    }

    suspend fun exportClassWorkbook(): WorkbookExport? {
        val current = (screenFlow.value as? ReportsScreenState.Open)?.state ?: return null
        val profile = current.profile ?: return null
        val term = current.selectedTerm ?: return null
        val assessmentId = current.selectedAssessmentId
        val fileName = excelExportRepository.buildFileName(
            profile.classGrade,
            "Class_Marks",
            term.termNumber,
            current.assessments.firstOrNull { it.id == assessmentId }?.name,
        )
        val bytes = excelExportRepository.generateClassWorkbook(profile.id, term.id, assessmentId)
        return WorkbookExport(fileName, bytes)
    }

    suspend fun exportBlankTemplate(): WorkbookExport? {
        val current = (screenFlow.value as? ReportsScreenState.Open)?.state ?: return null
        val profile = current.profile ?: return null
        val term = current.selectedTerm ?: return null
        val assessmentId = current.selectedAssessmentId
        val fileName = excelExportRepository.buildFileName(
            profile.classGrade,
            "Blank_Template",
            term.termNumber,
            current.assessments.firstOrNull { it.id == assessmentId }?.name,
        )
        val bytes = excelExportRepository.generateBlankTemplate(profile.id, term.id, assessmentId)
        return WorkbookExport(fileName, bytes)
    }

    private fun summaryFromMarks(values: List<Double>): ReportStatSummary {
        val averageValue = values.averageOrNull()
        return ReportStatSummary(
            entered = values.size,
            missing = 0,
            average = averageValue,
            highest = values.maxOrNull(),
            lowest = values.minOrNull(),
        )
    }

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()
}

class ReportsViewModelFactory(
    private val termRepository: TermRepository,
    private val repository: ReportsRepository,
    private val excelExportRepository: ExcelExportRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(ReportsViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        return ReportsViewModel(termRepository, repository, excelExportRepository) as T
    }
}
