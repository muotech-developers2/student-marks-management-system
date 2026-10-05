package com.example.studentmarks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun MarksScreen(
    state: MarksScreenState,
    onBack: () -> Unit,
    onTermSelected: (Int) -> Unit,
    onAssessmentSelected: (Long) -> Unit,
    onSubjectSelected: (Long) -> Unit,
    onSearchChanged: (String) -> Unit,
    onMarkChanged: (Long, String) -> Unit,
    onSave: () -> Unit,
    onClearMark: (Long) -> Unit,
    onCancelDiscard: () -> Unit,
    onDiscardChanges: () -> Unit,
) {
    when (state) {
        MarksScreenState.Closed -> Unit
        is MarksScreenState.Entry -> MarksEntryScreen(
            state = state.state,
            onBack = onBack,
            onTermSelected = onTermSelected,
            onAssessmentSelected = onAssessmentSelected,
            onSubjectSelected = onSubjectSelected,
            onSearchChanged = onSearchChanged,
            onMarkChanged = onMarkChanged,
            onSave = onSave,
            onClearMark = onClearMark,
            onCancelDiscard = onCancelDiscard,
            onDiscardChanges = onDiscardChanges,
        )
    }
}

@Composable
private fun MarksEntryScreen(
    state: MarksUiState,
    onBack: () -> Unit,
    onTermSelected: (Int) -> Unit,
    onAssessmentSelected: (Long) -> Unit,
    onSubjectSelected: (Long) -> Unit,
    onSearchChanged: (String) -> Unit,
    onMarkChanged: (Long, String) -> Unit,
    onSave: () -> Unit,
    onClearMark: (Long) -> Unit,
    onCancelDiscard: () -> Unit,
    onDiscardChanges: () -> Unit,
) {
    var termMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var assessmentMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var subjectMenuExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Marks", style = MaterialTheme.typography.headlineSmall)
            BackIconButton(onClick = onBack)
        }

        if (state.profile != null) {
            Text("${state.profile.classGrade}  |  ${state.profile.teacherName}")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(onClick = { termMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(state.selectedTerm?.let { "Term ${it.termNumber}" } ?: "Select term")
                }
                DropdownMenu(expanded = termMenuExpanded, onDismissRequest = { termMenuExpanded = false }) {
                    state.availableTerms.forEach { term ->
                        DropdownMenuItem(
                            text = { Text("Term ${term.termNumber}") },
                            onClick = {
                                onTermSelected(term.termNumber)
                                termMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }

        Box {
            OutlinedButton(onClick = { assessmentMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(state.selectedAssessment?.name ?: "Select assessment")
            }
            DropdownMenu(expanded = assessmentMenuExpanded, onDismissRequest = { assessmentMenuExpanded = false }) {
                state.availableAssessments.forEach { assessment ->
                    DropdownMenuItem(
                        text = { Text(assessment.name) },
                        onClick = {
                            onAssessmentSelected(assessment.id)
                            assessmentMenuExpanded = false
                        },
                    )
                }
            }
        }

        Box {
            OutlinedButton(onClick = { subjectMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(state.selectedSubject?.name ?: "Select subject")
            }
            DropdownMenu(expanded = subjectMenuExpanded, onDismissRequest = { subjectMenuExpanded = false }) {
                state.availableSubjects.forEach { subject ->
                    DropdownMenuItem(
                        text = { Text(subject.name) },
                        onClick = {
                            onSubjectSelected(subject.id)
                            subjectMenuExpanded = false
                        },
                    )
                }
            }
        }

        if (state.selectedAssessment != null && state.selectedSubject != null) {
            Text(
                "${state.selectedAssessment.name} — ${state.selectedSubject.name}",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        when {
            state.availableAssessments.isEmpty() -> Text("No assessments for this term. Add an assessment before entering marks.")
            state.availableSubjects.isEmpty() -> Text("No active subjects yet. Add a subject before entering marks.")
            !state.isLoading && state.totalCount == 0 -> Text("No active students yet. Add students before entering marks.")
            !state.isLoading && state.studentRows.isEmpty() -> Text("No students match your search.")
        }

        Text("Marks entered: ${state.completedCount} / ${state.totalCount}")

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search student") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )

        if (state.validationError != null) {
            Text(state.validationError, color = MaterialTheme.colorScheme.error)
        }
        if (state.errorMessage != null) {
            Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
        }

        if (state.isLoading) {
            Text("Loading marks...")
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Student", modifier = Modifier.weight(1.15f), style = MaterialTheme.typography.labelLarge)
                Text("Mark", modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
                Text("Action", modifier = Modifier.width(48.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.studentRows, key = { it.student.id }) { row ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                row.student.name,
                                modifier = Modifier.weight(1.15f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            OutlinedTextField(
                                value = row.value,
                                onValueChange = { onMarkChanged(row.student.id, it) },
                                modifier = Modifier.weight(0.8f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                placeholder = { Text("—") },
                            )
                            if (row.existingValue != null || row.value.isNotBlank()) {
                                IconButton(onClick = { onClearMark(row.student.id) }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear mark for ${row.student.name}")
                                }
                            } else {
                                Spacer(Modifier.width(48.dp))
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving && !state.isLoading && state.selectedAssessment != null &&
                state.selectedSubject != null && state.studentRows.isNotEmpty(),
        ) {
            if (!state.isSaving) {
                Icon(Icons.Filled.Done, contentDescription = null)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (state.isSaving) "Saving..." else "Save marks")
        }
    }

    if (state.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = onCancelDiscard,
            title = { Text("You have unsaved changes") },
            text = { Text("Do you want to save your marks before leaving?") },
            confirmButton = {
                TextButton(onClick = onSave) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onCancelDiscard) { Text("Continue editing") }
                    TextButton(onClick = onDiscardChanges) { Text("Discard") }
                }
            },
        )
    }
}
