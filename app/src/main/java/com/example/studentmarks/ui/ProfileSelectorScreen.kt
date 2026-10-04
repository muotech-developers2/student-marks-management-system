package com.example.studentmarks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.studentmarks.data.database.ClassProfileEntity

@Composable
fun ProfileSelectorScreen(
    state: AppUiState.ProfileSelector,
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val pendingDelete = state.profiles.firstOrNull { it.id == pendingDeleteId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Student Marks", style = MaterialTheme.typography.headlineMedium)
        Text("Your Classes", style = MaterialTheme.typography.titleLarge)

        if (state.errorMessage != null) {
            Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
        }

        if (state.profiles.isEmpty()) {
            Text(
                "No classes yet. Add a class to get started.",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.profiles, key = ClassProfileEntity::id) { profile ->
                    ProfileCard(
                        profile = profile,
                        isActive = profile.id == state.activeProfileId,
                        onOpen = { onOpen(profile.id) },
                        onEdit = { onEdit(profile.id) },
                        onDelete = { pendingDeleteId = profile.id },
                    )
                }
            }
        }

        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("+ Add Class")
        }
    }

    pendingDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete ${profile.classGrade}?") },
            text = { Text("All data belonging to this class will eventually be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(profile.id)
                        pendingDeleteId = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun ProfileCard(
    profile: ClassProfileEntity,
    isActive: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    profile.classGrade,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (isActive) {
                    Text("Current", color = MaterialTheme.colorScheme.primary)
                }
            }
            Text("Term ${profile.currentTerm}")
            Text(profile.teacherName)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = onOpen, modifier = Modifier.weight(1f)) {
                    Text("Open")
                }
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete")
                }
            }
        }
    }
}