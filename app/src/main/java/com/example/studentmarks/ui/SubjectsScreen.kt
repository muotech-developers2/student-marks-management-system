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
import com.example.studentmarks.data.database.SubjectEntity

@Composable
fun SubjectsScreen(
    state: SubjectUiState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (SubjectEntity) -> Unit,
    onSearchChanged: (String) -> Unit,
    onFilterSelected: (SubjectStatusFilter) -> Unit,
    onDeactivate: (SubjectEntity) -> Unit,
    onReactivate: (SubjectEntity) -> Unit,
    onDelete: (SubjectEntity) -> Unit,
    onCancelConfirmation: () -> Unit,
    onConfirmAction: () -> Unit,
    onNameChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancelForm: () -> Unit,
    onCancelDuplicate: () -> Unit,
    onAddDuplicateAnyway: () -> Unit,
) {
    when (state) {
        SubjectUiState.Closed -> Unit
        is SubjectUiState.SubjectList -> SubjectListScreen(
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
        )
        is SubjectUiState.SubjectForm -> SubjectFormScreen(
            state = state,
            onNameChanged = onNameChanged,
            onSave = onSave,
            onCancel = onCancelForm,
            onCancelDuplicate = onCancelDuplicate,
            onAddDuplicateAnyway = onAddDuplicateAnyway,
        )
    }
}

@Composable
private fun SubjectListScreen(
    state: SubjectUiState.SubjectList,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (SubjectEntity) -> Unit,
    onSearchChanged: (String) -> Unit,
    onFilterSelected: (SubjectStatusFilter) -> Unit,
    onDeactivate: (SubjectEntity) -> Unit,
    onReactivate: (SubjectEntity) -> Unit,
    onDelete: (SubjectEntity) -> Unit,
    onCancelConfirmation: () -> Unit,
    onConfirmAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("${state.profile.classGrade}  |  Term ${state.profile.currentTerm}")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Subjects", style = MaterialTheme.typography.headlineSmall)
            BackIconButton(onClick = onBack)
        }
        Text("${state.activeCount} active  ·  ${state.inactiveCount} inactive")
        OutlinedTextField(
            value = state.query,
            onValueChange = onSearchChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search subjects") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.filter == SubjectStatusFilter.Active,
                onClick = { onFilterSelected(SubjectStatusFilter.Active) },
                label = { Text("Active (${state.activeCount})") },
            )
            FilterChip(
                selected = state.filter == SubjectStatusFilter.Inactive,
                onClick = { onFilterSelected(SubjectStatusFilter.Inactive) },
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
                Text("Loading subjects...")
            }
        } else if (state.subjects.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (state.query.isBlank()) {
                        if (state.filter == SubjectStatusFilter.Active) {
                            "No subjects added yet."
                        } else {
                            "No inactive subjects."
                        }
                    } else {
                        "No subjects match your search."
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                if (state.query.isBlank() && state.filter == SubjectStatusFilter.Active) {
                    Text("Add the subjects taught in this class.")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.subjects, key = SubjectEntity::id) { subject ->
                    SubjectCard(
                        subject = subject,
                        onEdit = { onEdit(subject) },
                        onDeactivate = { onDeactivate(subject) },
                        onReactivate = { onReactivate(subject) },
                        onDelete = { onDelete(subject) },
                    )
                }
            }
        }
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Text("+ Add Subject")
        }
    }

    state.pendingConfirmation?.let { confirmation ->
        val isDelete = confirmation.action == SubjectActionKind.Delete
        AlertDialog(
            onDismissRequest = onCancelConfirmation,
            title = {
                Text(
                    if (isDelete) "Delete ${confirmation.subject.name}?"
                    else "Deactivate ${confirmation.subject.name}?",
                )
            },
            text = {
                Text(
                    if (isDelete) {
                        "Permanent deletion will remove this subject record."
                    } else {
                        "This subject will no longer appear in the active subject list."
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
private fun SubjectCard(
    subject: SubjectEntity,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
    onReactivate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by rememberSaveable(subject.id) { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(subject.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (subject.isActive) "Active" else "Inactive",
                    color = if (subject.isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Box {
                TextButton(onClick = { menuExpanded = true }) { Text("Actions") }
                androidx.compose.material3.DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Edit name") },
                        onClick = { menuExpanded = false; onEdit() },
                    )
                    if (subject.isActive) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Deactivate") },
                            onClick = { menuExpanded = false; onDeactivate() },
                        )
                    } else {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Reactivate") },
                            onClick = { menuExpanded = false; onReactivate() },
                        )
                    }
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { menuExpanded = false; onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectFormScreen(
    state: SubjectUiState.SubjectForm,
    onNameChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onCancelDuplicate: () -> Unit,
    onAddDuplicateAnyway: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("${state.profile.classGrade}  |  Term ${state.profile.currentTerm}")
        Text(
            if (state.subjectId == null) "Add Subject" else "Edit Subject",
            style = MaterialTheme.typography.headlineSmall,
        )
        OutlinedTextField(
            value = state.state.name,
            onValueChange = onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Subject Name") },
            singleLine = true,
            isError = state.state.error != null,
            supportingText = { state.state.error?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
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
                Text(if (state.state.isSaving) "Saving..." else "Save Subject")
            }
        }
    }

    state.state.duplicateName?.let { duplicateName ->
        AlertDialog(
            onDismissRequest = onCancelDuplicate,
            title = { Text("Duplicate subject name") },
            text = { Text("A subject with this name already exists in this class.") },
            confirmButton = {
                TextButton(onClick = onAddDuplicateAnyway) { Text("Add Anyway") }
            },
            dismissButton = {
                TextButton(onClick = onCancelDuplicate) { Text("Cancel") }
            },
        )
    }
}
