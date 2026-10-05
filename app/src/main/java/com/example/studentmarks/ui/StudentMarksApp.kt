package com.example.studentmarks.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@Composable
fun StudentMarksApp(
    viewModel: AppViewModel,
    studentsViewModel: StudentsViewModel,
    subjectsViewModel: SubjectsViewModel,
    assessmentsViewModel: AssessmentsViewModel,
    marksViewModel: MarksViewModel,
    reportsViewModel: ReportsViewModel,
) {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("student_marks_preferences", 0) }
    var isDarkTheme by rememberSaveable { mutableStateOf(preferences.getBoolean("dark_theme", false)) }
    var showAboutApp by rememberSaveable { mutableStateOf(false) }
    val appVersion = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull()
            ?: "Unknown"
    }
    val toggleTheme = {
        isDarkTheme = !isDarkTheme
        preferences.edit().putBoolean("dark_theme", isDarkTheme).apply()
    }
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val studentsState by studentsViewModel.screenState.collectAsStateWithLifecycle()
    val subjectsState by subjectsViewModel.screenState.collectAsStateWithLifecycle()
    val assessmentsState by assessmentsViewModel.screenState.collectAsStateWithLifecycle()
    val marksState by marksViewModel.screenState.collectAsStateWithLifecycle()
    val reportsState by reportsViewModel.screenState.collectAsStateWithLifecycle()
    val activeStudentCount by studentsViewModel.activeStudentCount.collectAsStateWithLifecycle()
    val activeSubjectCount by subjectsViewModel.activeSubjectCount.collectAsStateWithLifecycle()
    val activeAssessmentCount by assessmentsViewModel.activeAssessmentCount.collectAsStateWithLifecycle()
    val selectedTerm by assessmentsViewModel.selectedTerm.collectAsStateWithLifecycle()
    val dashboardWorkSummary by reportsViewModel.dashboardWorkSummary.collectAsStateWithLifecycle()
    var exportMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingExport by remember { mutableStateOf<WorkbookExport?>(null) }
    val exportScope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        onResult = { uri ->
            val workbook = pendingExport ?: return@rememberLauncherForActivityResult
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(workbook.bytes)
                }
                exportMessage = "Excel file created successfully."
            } else {
                exportMessage = "Excel export cancelled."
            }
            pendingExport = null
        },
    )
    val dashboardProfile = (appState as? AppUiState.Dashboard)?.profile
    LaunchedEffect(dashboardProfile) {
        dashboardProfile?.let {
            studentsViewModel.setCurrentProfile(it)
            subjectsViewModel.setCurrentProfile(it)
            assessmentsViewModel.setCurrentProfile(it)
        }
    }
    LaunchedEffect(dashboardProfile?.id, selectedTerm?.id, marksState == MarksScreenState.Closed) {
        dashboardProfile?.takeIf { marksState == MarksScreenState.Closed }?.let { profile ->
            reportsViewModel.refreshDashboardSummary(profile, selectedTerm)
        }
    }

    val canNavigateBack = studentsState != StudentUiState.Closed ||
        subjectsState != SubjectUiState.Closed ||
        assessmentsState != AssessmentUiState.Closed ||
        marksState != MarksScreenState.Closed ||
        reportsState != ReportsScreenState.Closed ||
        appState is AppUiState.ProfileSelector ||
        appState is AppUiState.AddProfile ||
        appState is AppUiState.EditProfile
    BackHandler(enabled = canNavigateBack) {
        when {
            studentsState != StudentUiState.Closed -> studentsViewModel.navigateBack()
            subjectsState != SubjectUiState.Closed -> subjectsViewModel.navigateBack()
            assessmentsState != AssessmentUiState.Closed -> assessmentsViewModel.navigateBack()
            marksState != MarksScreenState.Closed -> marksViewModel.navigateBack()
            reportsState != ReportsScreenState.Closed -> reportsViewModel.navigateBack()
            else -> viewModel.navigateBack()
        }
    }

    StudentMarksTheme(darkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (val state = appState) {
                AppUiState.Loading -> MessageScreen("Loading your class...")
                is AppUiState.Setup -> ProfileFormScreen(
                    state = state.form,
                    title = "Set up your class",
                    submitLabel = "Continue",
                    showCancel = false,
                    onTeacherNameChanged = viewModel::onTeacherNameChanged,
                    onClassGradeChanged = viewModel::onClassGradeChanged,
                    onTermSelected = viewModel::onCurrentTermSelected,
                    onSubmit = viewModel::submitProfile,
                    onCancel = viewModel::navigateBack,
                )
                is AppUiState.ProfileSelector -> ProfileSelectorScreen(
                    state = state,
                    onOpen = viewModel::openProfile,
                    onAdd = viewModel::addProfile,
                    onEdit = { viewModel.editProfile(it, returnToSelector = true) },
                    onDelete = viewModel::deleteProfile,
                )
                is AppUiState.Dashboard -> when {
                    reportsState != ReportsScreenState.Closed -> ReportsScreen(
                        state = (reportsState as? ReportsScreenState.Open)?.state ?: ReportsUiState(profile = state.profile),
                        exportStatusMessage = exportMessage,
                        onBack = reportsViewModel::navigateBack,
                        onReportTypeSelected = reportsViewModel::selectReportType,
                        onTermSelected = reportsViewModel::setSelectedTerm,
                        onAssessmentSelected = reportsViewModel::setSelectedAssessment,
                        onStudentSelected = reportsViewModel::setStudentSelection,
                        onStudentSearchChanged = reportsViewModel::onStudentSearchChanged,
                        onSubjectSelected = reportsViewModel::setSubjectSelection,
                        onSubjectSearchChanged = reportsViewModel::onSubjectSearchChanged,
                        onToggleInactiveStudents = reportsViewModel::toggleIncludeInactiveStudents,
                        onToggleInactiveSubjects = reportsViewModel::toggleIncludeInactiveSubjects,
                        onExportStudent = {
                            exportScope.launch {
                                val workbook = reportsViewModel.exportStudentWorkbook() ?: return@launch
                                pendingExport = workbook
                                exportMessage = "Generating Excel..."
                                exportLauncher.launch(workbook.fileName)
                            }
                        },
                        onExportSubject = {
                            exportScope.launch {
                                val workbook = reportsViewModel.exportSubjectWorkbook() ?: return@launch
                                pendingExport = workbook
                                exportMessage = "Generating Excel..."
                                exportLauncher.launch(workbook.fileName)
                            }
                        },
                        onExportClass = {
                            exportScope.launch {
                                val workbook = reportsViewModel.exportClassWorkbook() ?: return@launch
                                pendingExport = workbook
                                exportMessage = "Generating Excel..."
                                exportLauncher.launch(workbook.fileName)
                            }
                        },
                        onExportTemplate = {
                            exportScope.launch {
                                val workbook = reportsViewModel.exportBlankTemplate() ?: return@launch
                                pendingExport = workbook
                                exportMessage = "Generating Excel..."
                                exportLauncher.launch(workbook.fileName)
                            }
                        },
                    )
                    marksState != MarksScreenState.Closed -> MarksScreen(
                        state = marksState,
                        onBack = marksViewModel::navigateBack,
                        onTermSelected = marksViewModel::selectTerm,
                        onAssessmentSelected = marksViewModel::selectAssessment,
                        onSubjectSelected = marksViewModel::selectSubject,
                        onSearchChanged = marksViewModel::onSearchChanged,
                        onMarkChanged = marksViewModel::onMarkChanged,
                        onSave = marksViewModel::saveMarks,
                        onClearMark = marksViewModel::clearMark,
                        onCancelDiscard = marksViewModel::cancelDiscardDialog,
                        onDiscardChanges = marksViewModel::discardChanges,
                    )
                    assessmentsState != AssessmentUiState.Closed -> AssessmentsScreen(
                        state = assessmentsState,
                        onBack = assessmentsViewModel::navigateBack,
                        onAdd = assessmentsViewModel::openAddAssessment,
                        onEdit = assessmentsViewModel::openEditAssessment,
                        onSearchChanged = assessmentsViewModel::onSearchChanged,
                        onFilterSelected = assessmentsViewModel::setFilter,
                        onDeactivate = assessmentsViewModel::requestDeactivate,
                        onReactivate = assessmentsViewModel::reactivateAssessment,
                        onDelete = assessmentsViewModel::requestDelete,
                        onCancelConfirmation = assessmentsViewModel::cancelConfirmation,
                        onConfirmAction = assessmentsViewModel::confirmAssessmentAction,
                        onNameChanged = assessmentsViewModel::onNameChanged,
                        onDescriptionChanged = assessmentsViewModel::onDescriptionChanged,
                        onSave = assessmentsViewModel::submitAssessment,
                        onCancelForm = assessmentsViewModel::cancelForm,
                        onCancelDuplicate = assessmentsViewModel::cancelDuplicateWarning,
                        onAddDuplicateAnyway = assessmentsViewModel::addDuplicateAnyway,
                        onSelectTerm = assessmentsViewModel::selectTerm,
                        availableTerms = listOf(1, 2, 3),
                    )
                    subjectsState != SubjectUiState.Closed -> SubjectsScreen(
                        state = subjectsState,
                        onBack = subjectsViewModel::navigateBack,
                        onAdd = subjectsViewModel::openAddSubject,
                        onEdit = subjectsViewModel::openEditSubject,
                        onSearchChanged = subjectsViewModel::onSearchChanged,
                        onFilterSelected = subjectsViewModel::setFilter,
                        onDeactivate = subjectsViewModel::requestDeactivate,
                        onReactivate = subjectsViewModel::reactivateSubject,
                        onDelete = subjectsViewModel::requestDelete,
                        onCancelConfirmation = subjectsViewModel::cancelConfirmation,
                        onConfirmAction = subjectsViewModel::confirmSubjectAction,
                        onNameChanged = subjectsViewModel::onNameChanged,
                        onSave = subjectsViewModel::submitSubject,
                        onCancelForm = subjectsViewModel::cancelForm,
                        onCancelDuplicate = subjectsViewModel::cancelDuplicateWarning,
                        onAddDuplicateAnyway = subjectsViewModel::addDuplicateAnyway,
                    )
                    studentsState != StudentUiState.Closed -> StudentsScreen(
                        state = studentsState,
                        onBack = studentsViewModel::navigateBack,
                        onAdd = studentsViewModel::openAddStudent,
                        onEdit = studentsViewModel::openEditStudent,
                        onSearchChanged = studentsViewModel::onSearchChanged,
                        onFilterSelected = studentsViewModel::setFilter,
                        onDeactivate = studentsViewModel::requestDeactivate,
                        onReactivate = studentsViewModel::reactivateStudent,
                        onDelete = studentsViewModel::requestDelete,
                        onCancelConfirmation = studentsViewModel::cancelConfirmation,
                        onConfirmAction = studentsViewModel::confirmStudentAction,
                        onNameChanged = studentsViewModel::onNameChanged,
                        onSave = studentsViewModel::submitStudent,
                        onCancelForm = studentsViewModel::cancelForm,
                        onCancelDuplicate = studentsViewModel::cancelDuplicateWarning,
                        onAddDuplicateAnyway = studentsViewModel::addDuplicateAnyway,
                    )
                    else -> DashboardScreen(
                        profile = state.profile,
                        activeStudentCount = activeStudentCount,
                        activeSubjectCount = activeSubjectCount,
                        activeAssessmentCount = activeAssessmentCount,
                        selectedTerm = selectedTerm,
                        workSummary = dashboardWorkSummary,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = toggleTheme,
                        onAboutApp = { showAboutApp = true },
                        onShowClasses = viewModel::showProfileSelector,
                        onEdit = { viewModel.editProfile(state.profile.id, returnToSelector = false) },
                        onOpenStudents = { studentsViewModel.openStudents(state.profile) },
                        onOpenSubjects = { subjectsViewModel.openSubjects(state.profile) },
                        onOpenAssessments = { assessmentsViewModel.openAssessments(state.profile) },
                        onOpenMarks = { marksViewModel.openMarks(state.profile) },
                        onOpenReports = { reportsViewModel.openReports(state.profile) },
                        onTermSelected = assessmentsViewModel::selectTerm,
                    )
                }
                is AppUiState.AddProfile -> ProfileFormScreen(
                    state = state.form,
                    title = "Add Class",
                    submitLabel = "Save Class",
                    showCancel = true,
                    onTeacherNameChanged = viewModel::onTeacherNameChanged,
                    onClassGradeChanged = viewModel::onClassGradeChanged,
                    onTermSelected = viewModel::onCurrentTermSelected,
                    onSubmit = viewModel::submitProfile,
                    onCancel = viewModel::navigateBack,
                )
                is AppUiState.EditProfile -> ProfileFormScreen(
                    state = state.form,
                    title = "Edit Class",
                    submitLabel = "Save Changes",
                    showCancel = true,
                    onTeacherNameChanged = viewModel::onTeacherNameChanged,
                    onClassGradeChanged = viewModel::onClassGradeChanged,
                    onTermSelected = viewModel::onCurrentTermSelected,
                    onSubmit = viewModel::submitProfile,
                    onCancel = viewModel::navigateBack,
                )
                is AppUiState.Error -> MessageScreen(state.message)
            }
        }
        if (showAboutApp) {
            AlertDialog(
                onDismissRequest = { showAboutApp = false },
                title = { Text("About Student Marks") },
                text = {
                    Text("Student Marks Management System\nVersion $appVersion\n\nManage class profiles, student marks, reports, and Excel exports.")
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { showAboutApp = false }) {
                        Text("Close")
                    }
                },
            )
        }
    }
}

@Composable
private fun ProfileFormScreen(
    state: SetupFormState,
    title: String,
    submitLabel: String,
    showCancel: Boolean,
    onTeacherNameChanged: (String) -> Unit,
    onClassGradeChanged: (String) -> Unit,
    onTermSelected: (Int) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    var termMenuExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Student Marks", style = MaterialTheme.typography.headlineMedium)
        Text(title, style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = state.teacherName,
            onValueChange = onTeacherNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Teacher Name") },
            singleLine = true,
            isError = state.teacherNameError != null,
            supportingText = { state.teacherNameError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text,
            ),
        )

        OutlinedTextField(
            value = state.classGrade,
            onValueChange = onClassGradeChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Class / Grade") },
            singleLine = true,
            isError = state.classGradeError != null,
            supportingText = { state.classGradeError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text,
            ),
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Current Term", style = MaterialTheme.typography.labelLarge)
            Box {
                OutlinedButton(
                    onClick = { termMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSubmitting,
                ) {
                    Text(
                        text = state.currentTerm?.let { "Term $it" } ?: "Select a term",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                    )
                }
                DropdownMenu(
                    expanded = termMenuExpanded,
                    onDismissRequest = { termMenuExpanded = false },
                ) {
                    (1..3).forEach { term ->
                        DropdownMenuItem(
                            text = { Text("Term $term") },
                            onClick = {
                                onTermSelected(term)
                                termMenuExpanded = false
                            },
                        )
                    }
                }
            }
            state.currentTermError?.let { ValidationMessage(it) }
        }

        state.submitError?.let { ValidationMessage(it) }

        if (showCancel) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isSubmitting,
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isSubmitting,
                ) {
                    Text(if (state.isSubmitting) "Saving..." else submitLabel)
                }
            }
        } else {
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSubmitting,
            ) {
                Text(if (state.isSubmitting) "Saving..." else submitLabel)
            }
        }
    }
}

@Composable
private fun ValidationMessage(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun MessageScreen(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(message, modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
    }
}