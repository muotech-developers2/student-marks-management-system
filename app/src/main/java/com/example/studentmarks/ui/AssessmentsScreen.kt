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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.studentmarks.data.database.AssessmentEntity

@Composable
fun AssessmentsScreen(
    state: AssessmentUiState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (AssessmentEntity) -> Unit,
    onSearchChanged: (String) -> Unit,
    onFilterSelected: (AssessmentStatusFilter) -> Unit,
    onDeactivate: (AssessmentEntity) -> Unit,
    onReactivate: (AssessmentEntity) -> Unit,
    onDelete: (AssessmentEntity) -> Unit,
    onCancelConfirmation: () -> Unit,
    onConfirmAction: () -> Unit,
    onNameChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancelForm: () -> Unit,
    onCancelDuplicate: () -> Unit,
    onAddDuplicateAnyway: () -> Unit,
    onSelectTerm: (Int) -> Unit,
    availableTerms: List<Int>,
) {
    when (state) {
        AssessmentUiState.Closed -> Unit
        is AssessmentUiState.AssessmentList -> AssessmentListScreen(
            state = state,
            onBack = onBack,
            onAdd = onAdd,
            onEdit = onEdit,
            onSearchChanged = onSearchChanged,
            onFilterSelected = onFilterSelected,
            onDeactivate = onDeactivate,
            onReactivate = onReactivate,
            onDelete = onDelete,
            onCancelConfirmation = onCancelConfirmation,
            onConfirmAction = onConfirmAction,
            onSelectTerm = onSelectTerm,
            availableTerms = availableTerms,
        )
        is AssessmentUiState.AssessmentForm -> AssessmentFormScreen(
            state = state,
            onNameChanged = onNameChanged,
            onDescriptionChanged = onDescriptionChanged,
            onSave = onSave,
            onCancel = onCancelForm,
            onCancelDuplicate = onCancelDuplicate,
            onAddDuplicateAnyway = onAddDuplicateAnyway,
        )
    }
}

@Composable
private fun AssessmentListScreen(
    state: AssessmentUiState.AssessmentList,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (AssessmentEntity) -> Unit,
    onSearchChanged: (String) -> Unit,
    onFilterSelected: (AssessmentStatusFilter) -> Unit,
    onDeactivate: (AssessmentEntity) -> Unit,
    onReactivate: (AssessmentEntity) -> Unit,
    onDelete: (AssessmentEntity) -> Unit,
    onCancelConfirmation: () -> Unit,
    onConfirmAction: () -> Unit,
    onSelectTerm: (Int) -> Unit,
    availableTerms: List<Int>,
) {
    var termMenuExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("${state.profile.classGrade}  |  ${state.profile.teacherName}")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Assessments", style = MaterialTheme.typography.headlineSmall)
            BackIconButton(onClick = onBack)
        }

        Box {
            OutlinedButton(onClick = { termMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Term ${state.term.termNumber}")
            }
            DropdownMenu(
                expanded = termMenuExpanded,
                onDismissRequest = { termMenuExpanded = false },
            ) {
                availableTerms.forEach { termNumber ->
                    DropdownMenuItem(
                        text = { Text("Term $termNumber") },
                        onClick = {
                            onSelectTerm(termNumber)
                            termMenuExpanded = false
                        },
                    )
                }
            }
        }

        Text("${state.activeCount} active  ·  ${state.inactiveCount} inactive")
        OutlinedTextField(
            value = state.query,
            onValueChange = onSearchChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search assessments") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.filter == AssessmentStatusFilter.Active,
                onClick = { onFilterSelected(AssessmentStatusFilter.Active) },
                label = { Text("Active (${state.activeCount})") },
            )
            FilterChip(
                selected = state.filter == AssessmentStatusFilter.Inactive,
                onClick = { onFilterSelected(AssessmentStatusFilter.Inactive) },
                label = { Text("Inactive (${state.inactiveCount})") },
            )
        }
        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        if (state.isLoading) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Loading assessments...")
            }
        } else if (state.assessments.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (state.query.isBlank()) {
                        if (state.filter == AssessmentStatusFilter.Active) {
                            "No assessments yet."
                        } else {
                            "No inactive assessments."
                        }
                    } else {
                        "No assessments match your search."
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                if (state.query.isBlank() && state.filter == AssessmentStatusFilter.Active) {
                    Text("Add an exam, CAT, test, or other assessment for this term.")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.assessments, key = AssessmentEntity::id) { assessment ->
                    AssessmentCard(
                        assessment = assessment,
                        onEdit = { onEdit(assessment) },
                        onDeactivate = { onDeactivate(assessment) },
                        onReactivate = { onReactivate(assessment) },
                        onDelete = { onDelete(assessment) },
                    )
                }
            }
        }

        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Text("+ Add Assessment")
        }
    }

    state.pendingConfirmation?.let { confirmation ->
        val isDelete = confirmation.action == AssessmentActionKind.Delete
        AlertDialog(
            onDismissRequest = onCancelConfirmation,
            title = {
                Text(
                    if (isDelete) "Delete ${confirmation.assessment.name}?"
                    else "Deactivate ${confirmation.assessment.name}?",
                )
            },
            text = {
                Text(
                    if (isDelete) {
                        "This will permanently remove this assessment."
                    } else {
                        "This assessment will no longer appear in the active assessment list."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmAction) {
                    Text(if (isDelete) "Delete" else "Deactivate")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelConfirmation) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun AssessmentCard(
    assessment: AssessmentEntity,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
    onReactivate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by rememberSaveable(assessment.id) { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(assessment.name, style = MaterialTheme.typography.titleMedium)
                if (assessment.description.isNotBlank()) {
                    Text(assessment.description, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    if (assessment.isActive) "Active" else "Inactive",
                    color = if (assessment.isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Box {
                TextButton(onClick = { menuExpanded = true }) { Text("Actions") }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { menuExpanded = false; onEdit() },
                    )
                    if (assessment.isActive) {
                        DropdownMenuItem(
                            text = { Text("Deactivate") },
                            onClick = { menuExpanded = false; onDeactivate() },
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Reactivate") },
                            onClick = { menuExpanded = false; onReactivate() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { menuExpanded = false; onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
private fun AssessmentFormScreen(
    state: AssessmentUiState.AssessmentForm,
    onNameChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onCancelDuplicate: () -> Unit,
    onAddDuplicateAnyway: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("${state.profile.classGrade}  |  Term ${state.term.termNumber}")
        Text(
            if (state.assessmentId == null) "Add Assessment" else "Edit Assessment",
            style = MaterialTheme.typography.headlineSmall,
        )
        OutlinedTextField(
            value = state.state.name,
            onValueChange = onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Assessment Name") },
            singleLine = true,
            isError = state.state.error != null,
            supportingText = { state.state.error?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text,
            ),
        )
        OutlinedTextField(
            value = state.state.description,
            onValueChange = onDescriptionChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Description (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Text,
            ),
        )
        state.state.submitError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                enabled = !state.state.isSaving,
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = !state.state.isSaving,
            ) {
                Text(if (state.state.isSaving) "Saving..." else "Save")
            }
        }
    }

    state.state.duplicateName?.let {
        AlertDialog(
            onDismissRequest = onCancelDuplicate,
            title = { Text("Duplicate assessment name") },
            text = { Text("An assessment with this name already exists in this term.") },
            confirmButton = {
                TextButton(onClick = onAddDuplicateAnyway) { Text("Add Anyway") }
            },
            dismissButton = {
                TextButton(onClick = onCancelDuplicate) { Text("Cancel") }
            },
        )
    }
}
