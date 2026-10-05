package com.example.studentmarks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReportsScreen(
    state: ReportsUiState,
    exportStatusMessage: String? = null,
    onBack: () -> Unit,
    onReportTypeSelected: (ReportType) -> Unit,
    onTermSelected: (Int) -> Unit,
    onAssessmentSelected: (Long?) -> Unit,
    onStudentSelected: (Long?) -> Unit,
    onStudentSearchChanged: (String) -> Unit,
    onSubjectSelected: (Long?) -> Unit,
    onSubjectSearchChanged: (String) -> Unit,
    onToggleInactiveStudents: () -> Unit,
    onToggleInactiveSubjects: () -> Unit,
    onExportStudent: () -> Unit,
    onExportSubject: () -> Unit,
    onExportClass: () -> Unit,
    onExportTemplate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Reports", style = MaterialTheme.typography.headlineSmall)
            BackIconButton(onClick = onBack)
        }

        if (state.profile != null) {
            Text("Class: ${state.profile.classGrade}")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReportChoiceButton("Student", state.reportType == ReportType.STUDENT) { onReportTypeSelected(ReportType.STUDENT) }
            ReportChoiceButton("Subject", state.reportType == ReportType.SUBJECT) { onReportTypeSelected(ReportType.SUBJECT) }
            ReportChoiceButton("Class", state.reportType == ReportType.CLASS) { onReportTypeSelected(ReportType.CLASS) }
        }

        if (state.errorMessage != null) {
            Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
        }
        if (exportStatusMessage != null) {
            Text(exportStatusMessage)
        }

        val termMenuExpanded = remember { mutableStateOf(false) }
        val assessmentMenuExpanded = remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { termMenuExpanded.value = true }, modifier = Modifier.fillMaxWidth()) {
                Text(state.selectedTerm?.let { "Term ${it.termNumber}" } ?: "Select term")
            }
            DropdownMenu(expanded = termMenuExpanded.value, onDismissRequest = { termMenuExpanded.value = false }) {
                state.terms.forEach { term ->
                    DropdownMenuItem(
                        text = { Text("Term ${term.termNumber}") },
                        onClick = {
                            onTermSelected(term.termNumber)
                            termMenuExpanded.value = false
                        },
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { assessmentMenuExpanded.value = true }, modifier = Modifier.fillMaxWidth()) {
                Text(state.assessments.firstOrNull { it.id == state.selectedAssessmentId }?.name ?: "All Assessments")
            }
            DropdownMenu(expanded = assessmentMenuExpanded.value, onDismissRequest = { assessmentMenuExpanded.value = false }) {
                DropdownMenuItem(text = { Text("All Assessments") }, onClick = { onAssessmentSelected(null); assessmentMenuExpanded.value = false })
                state.assessments.forEach { assessment ->
                    DropdownMenuItem(
                        text = { Text(assessment.name) },
                        onClick = {
                            onAssessmentSelected(assessment.id)
                            assessmentMenuExpanded.value = false
                        },
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onExportClass, modifier = Modifier.weight(1f)) {
                Text("Export Excel")
            }
            Button(onClick = onExportTemplate, modifier = Modifier.weight(1f)) {
                Text("Blank Template")
            }
        }

        when (state.reportType) {
            ReportType.HOME -> {
                Text("Choose a report type.")
            }
            ReportType.STUDENT -> StudentReportView(
                state = state,
                onStudentSelected = onStudentSelected,
                onStudentSearchChanged = onStudentSearchChanged,
                onToggleInactiveStudents = onToggleInactiveStudents,
                onExportStudent = onExportStudent,
            )
            ReportType.SUBJECT -> SubjectReportView(
                state = state,
                onSubjectSelected = onSubjectSelected,
                onSubjectSearchChanged = onSubjectSearchChanged,
                onToggleInactiveSubjects = onToggleInactiveSubjects,
                onExportSubject = onExportSubject,
            )
            ReportType.CLASS -> ClassReportView(
                state = state,
                onExportClass = onExportClass,
            )
        }
    }
}

@Composable
private fun ReportChoiceButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !selected,
    ) {
        Text(label)
    }
}

@Composable
private fun StudentReportView(
    state: ReportsUiState,
    onStudentSelected: (Long?) -> Unit,
    onStudentSearchChanged: (String) -> Unit,
    onToggleInactiveStudents: () -> Unit,
    onExportStudent: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = state.studentSearchQuery,
                onValueChange = onStudentSearchChanged,
                label = { Text("Search student") },
                modifier = Modifier.weight(1f),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            )
            TextButton(onClick = onToggleInactiveStudents) {
                Text(if (state.includeInactiveStudents) "Hide inactive" else "Include inactive")
            }
        }
        if (state.students.isEmpty()) {
            Text("No students available for this profile.")
        } else {
            state.students.forEach { student ->
                val selected = state.selectedStudentId == student.id
                Button(
                    onClick = { onStudentSelected(student.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (selected) "${student.name} (selected)" else student.name)
                }
            }
        }
        state.studentReport?.let { report ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Student: ${report.student.name}")
                    Text("Class: ${report.profile.classGrade}")
                    Text("Term: ${report.term.termNumber}")
                    Text("Assessment: ${report.selectedAssessment?.name ?: "All Assessments"}")
                    SummaryRow(report.summary)
                    report.subjectMarks.forEach { subjectMark ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(subjectMark.subject.name)
                            Text(subjectMark.mark?.let { value -> if (value == 0.0) "0" else value.toString() } ?: "—")
                        }
                    }
                }
            }
        }
        Button(onClick = onExportStudent, modifier = Modifier.fillMaxWidth()) {
            Text("Export Student Excel")
        }
    }
}

@Composable
private fun SubjectReportView(
    state: ReportsUiState,
    onSubjectSelected: (Long?) -> Unit,
    onSubjectSearchChanged: (String) -> Unit,
    onToggleInactiveSubjects: () -> Unit,
    onExportSubject: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = state.subjectSearchQuery,
                onValueChange = onSubjectSearchChanged,
                label = { Text("Search subject") },
                modifier = Modifier.weight(1f),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            )
            TextButton(onClick = onToggleInactiveSubjects) {
                Text(if (state.includeInactiveSubjects) "Hide inactive" else "Include inactive")
            }
        }
        if (state.subjects.isEmpty()) {
            Text("No subjects available for this profile.")
        } else {
            state.subjects.forEach { subject ->
                val selected = state.selectedSubjectId == subject.id
                Button(
                    onClick = { onSubjectSelected(subject.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (selected) "${subject.name} (selected)" else subject.name)
                }
            }
        }

        state.subjectReport?.let { report ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Subject: ${report.subject.name}")
                    Text("Class: ${report.profile.classGrade}")
                    Text("Term: ${report.term.termNumber}")
                    Text("Assessment: ${report.selectedAssessment?.name ?: "All Assessments"}")
                    SummaryRow(report.summary)
                    report.rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(row.student.name)
                            Text(row.mark?.let { value -> if (value == 0.0) "0" else value.toString() } ?: "—")
                        }
                    }
                }
            }
        }
        Button(onClick = onExportSubject, modifier = Modifier.fillMaxWidth()) {
            Text("Export Subject Excel")
        }
    }
}

@Composable
private fun ClassReportView(
    state: ReportsUiState,
    onExportClass: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.classReport?.let { report ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Class: ${report.profile.classGrade}")
                    Text("Term: ${report.term.termNumber}")
                    Text("Assessment: ${report.selectedAssessment?.name ?: "All Assessments"}")
                    Text("Active students: ${report.activeStudents}")
                    Text("Active subjects: ${report.activeSubjects}")
                    Text("Marks entered: ${report.marksEntered}")
                    Text("Marks missing: ${report.marksMissing}")
                }
            }

            report.subjectStats.forEach { stat ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stat.subject.name)
                        Text("Entered: ${stat.entered}   Missing: ${stat.missing}")
                        Text("Average: ${stat.average?.let { String.format("%.1f", it) } ?: "No marks entered"}")
                        Text("Highest: ${stat.highest?.let { formatValue(it) } ?: "—"}   Lowest: ${stat.lowest?.let { formatValue(it) } ?: "—"}")
                    }
                }
            }

            report.assessmentStats.forEach { stat ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Assessment: ${stat.assessment.name}")
                        Text("Entered: ${stat.entered}   Missing: ${stat.missing}")
                        Text("Average: ${stat.average?.let { String.format("%.1f", it) } ?: "No marks entered"}")
                        Text("Highest: ${stat.highest?.let { formatValue(it) } ?: "—"}   Lowest: ${stat.lowest?.let { formatValue(it) } ?: "—"}")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Student overview")
                    report.studentOverview.forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(row.student.name)
                            Text("${row.entered} entered / ${row.missing} missing")
                        }
                    }
                }
            }
        }
        Button(onClick = onExportClass, modifier = Modifier.fillMaxWidth()) {
            Text("Export Class Excel")
        }
    }
}

@Composable
private fun SummaryRow(summary: ReportStatSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Marks entered: ${summary.entered}")
        Text("Average: ${summary.average?.let { String.format("%.1f", it) } ?: "No marks entered"}")
        Text("Highest: ${summary.highest?.let { formatValue(it) } ?: "—"}")
        Text("Lowest: ${summary.lowest?.let { formatValue(it) } ?: "—"}")
    }
}

private fun formatValue(value: Double): String = if (value == 0.0) "0" else value.toString()
