package com.example.studentmarks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.studentmarks.data.database.StudentEntity

@Composable
fun StudentsScreen(
    state: StudentUiState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (StudentEntity) -> Unit,
    onSearchChanged: (String) -> Unit,
    onFilterSelected: (StudentStatusFilter) -> Unit,
    onDeactivate: (StudentEntity) -> Unit,
    onReactivate: (StudentEntity) -> Unit,
    onDelete: (StudentEntity) -> Unit,
    onCancelConfirmation: () -> Unit,
    onConfirmAction: () -> Unit,
    onNameChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancelForm: () -> Unit,
    onCancelDuplicate: () -> Unit,
    onAddDuplicateAnyway: () -> Unit,
) {
    when (state) {
        StudentUiState.Closed -> Unit
        is StudentUiState.StudentList -> StudentListScreen(
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
        is StudentUiState.StudentForm -> StudentFormScreen(
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
private fun StudentListScreen(
    state: StudentUiState.StudentList,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (StudentEntity) -> Unit,
    onSearchChanged: (String) -> Unit,
    onFilterSelected: (StudentStatusFilter) -> Unit,
    onDeactivate: (StudentEntity) -> Unit,
    onReactivate: (StudentEntity) -> Unit,
    onDelete: (StudentEntity) -> Unit,
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
            Text("Students", style = MaterialTheme.typography.headlineSmall)
            BackIconButton(onClick = onBack)
        }
        Text("${state.activeCount} active  ·  ${state.inactiveCount} inactive")
        OutlinedTextField(
            value = state.query,
            onValueChange = onSearchChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search students") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.filter == StudentStatusFilter.Active,
                onClick = { onFilterSelected(StudentStatusFilter.Active) },
                label = { Text("Active (${state.activeCount})") },
            )
            FilterChip(
                selected = state.filter == StudentStatusFilter.Inactive,
                onClick = { onFilterSelected(StudentStatusFilter.Inactive) },
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
                Text("Loading students...")
            }
        } else if (state.students.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (state.query.isBlank()) {
                        if (state.filter == StudentStatusFilter.Active) {
                            "No students added yet."
                        } else {
                            "No inactive students."
                        }
                    } else {
                        "No students match your search."
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                if (state.query.isBlank() && state.filter == StudentStatusFilter.Active) {
                    Text("Add your first student to get started.")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.students, key = StudentEntity::id) { student ->
                    StudentCard(
                        student = student,
                        onEdit = { onEdit(student) },
                        onDeactivate = { onDeactivate(student) },
                        onReactivate = { onReactivate(student) },
                        onDelete = { onDelete(student) },
                    )
                }
            }
        }
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Text("+ Add Student")
        }
    }

    state.pendingConfirmation?.let { confirmation ->
        val isDelete = confirmation.action == StudentActionKind.Delete
        AlertDialog(
            onDismissRequest = onCancelConfirmation,
            title = {
                Text(
                    if (isDelete) "Delete ${confirmation.student.name}?"
                    else "Deactivate ${confirmation.student.name}?",
                )
            },
            text = {
                Text(
                    if (isDelete) {
                        "Permanent deletion will remove this student's record."
                    } else {
                        "This student will no longer appear in the active student list."
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
private fun StudentCard(
    student: StudentEntity,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
    onReactivate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by rememberSaveable(student.id) { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(student.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (student.isActive) "Active" else "Inactive",
                    color = if (student.isActive) {
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
                        text = { Text("Edit name") },
                        onClick = { menuExpanded = false; onEdit() },
                    )
                    if (student.isActive) {
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
private fun StudentFormScreen(
    state: StudentUiState.StudentForm,
    onNameChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onCancelDuplicate: () -> Unit,
    onAddDuplicateAnyway: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("${state.profile.classGrade}  |  Term ${state.profile.currentTerm}")
        Text(
            if (state.studentId == null) "Add Student" else "Edit Student",
            style = MaterialTheme.typography.headlineSmall,
        )
        OutlinedTextField(
            value = state.state.name,
            onValueChange = onNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Student Name") },
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
                Text(if (state.state.isSaving) "Saving..." else "Save Student")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    state.state.duplicateName?.let { duplicateName ->
        AlertDialog(
            onDismissRequest = onCancelDuplicate,
            title = { Text("Duplicate student name") },
            text = { Text("A student with this name already exists in this class.") },
            confirmButton = {
                TextButton(onClick = onAddDuplicateAnyway) { Text("Add Anyway") }
            },
            dismissButton = {
                TextButton(onClick = onCancelDuplicate) { Text("Cancel") }
            },
        )
    }
}