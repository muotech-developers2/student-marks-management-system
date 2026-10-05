package com.example.studentmarks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.studentmarks.data.database.ClassProfileEntity
import com.example.studentmarks.data.database.TermEntity

@Composable
fun DashboardScreen(
    profile: ClassProfileEntity,
    activeStudentCount: Int,
    activeSubjectCount: Int,
    activeAssessmentCount: Int,
    selectedTerm: TermEntity?,
    workSummary: DashboardWorkSummary?,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onAboutApp: () -> Unit,
    onShowClasses: () -> Unit,
    onEdit: () -> Unit,
    onOpenStudents: () -> Unit,
    onOpenSubjects: () -> Unit,
    onOpenAssessments: () -> Unit,
    onOpenMarks: () -> Unit,
    onOpenReports: () -> Unit,
    onTermSelected: (Int) -> Unit,
) {
    var termMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var profileMenuExpanded by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 680.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1080.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.screen, vertical = AppSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.section),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.card),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "TEACHER WORKSPACE",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                            )
                            Text(
                                profile.classGrade,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                "Teacher: ${profile.teacherName}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { profileMenuExpanded = true },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface,
                                ) {
                                    Icon(
                                        Icons.Filled.AccountCircle,
                                        contentDescription = "Profile and app menu",
                                        modifier = Modifier.size(44.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = profileMenuExpanded,
                                onDismissRequest = { profileMenuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Switch class") },
                                    onClick = { profileMenuExpanded = false; onShowClasses() },
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit class") },
                                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                    onClick = { profileMenuExpanded = false; onEdit() },
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isDarkTheme) "Switch to light mode" else "Switch to dark mode") },
                                    onClick = onToggleTheme,
                                )
                                DropdownMenuItem(
                                    text = { Text("About app") },
                                    leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                                    onClick = { profileMenuExpanded = false; onAboutApp() },
                                )
                            }
                        }
                    }
                    Box {
                        OutlinedButton(
                            onClick = { termMenuExpanded = true },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text("Term ${selectedTerm?.termNumber ?: profile.currentTerm}  ▾")
                        }
                        DropdownMenu(
                            expanded = termMenuExpanded,
                            onDismissRequest = { termMenuExpanded = false },
                        ) {
                            (1..3).forEach { termNumber ->
                                DropdownMenuItem(
                                    text = { Text("Term $termNumber") },
                                    onClick = {
                                        onTermSelected(termNumber)
                                        termMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
                SectionHeading("Overview", "Live totals for this class and term")
                if (isWide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                        StatCard("Students", activeStudentCount.toString(), "Active learners", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, onOpenStudents, Modifier.weight(1f))
                        StatCard("Subjects", activeSubjectCount.toString(), "Being taught", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, onOpenSubjects, Modifier.weight(1f))
                        StatCard("Assessments", activeAssessmentCount.toString(), "In this term", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, onOpenAssessments, Modifier.weight(1f))
                        val missingColor = if ((workSummary?.missingMarkCount ?: 0) > 0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
                        val missingTextColor = if ((workSummary?.missingMarkCount ?: 0) > 0) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        StatCard("Missing marks", workSummary?.takeIf { it.isAvailable }?.missingMarkCount?.toString() ?: "—", missingCaption(workSummary), missingColor, missingTextColor, onOpenMarks, Modifier.weight(1f))
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                        StatCard("Students", activeStudentCount.toString(), "Active learners", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, onOpenStudents, Modifier.weight(1f))
                        StatCard("Subjects", activeSubjectCount.toString(), "Being taught", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, onOpenSubjects, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                        StatCard("Assessments", activeAssessmentCount.toString(), "In this term", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, onOpenAssessments, Modifier.weight(1f))
                        val missingColor = if ((workSummary?.missingMarkCount ?: 0) > 0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
                        val missingTextColor = if ((workSummary?.missingMarkCount ?: 0) > 0) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        StatCard("Missing marks", workSummary?.takeIf { it.isAvailable }?.missingMarkCount?.toString() ?: "—", missingCaption(workSummary), missingColor, missingTextColor, onOpenMarks, Modifier.weight(1f))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
                SectionHeading("Quick actions", "Pick up where your class needs you")
                Button(
                    onClick = onOpenMarks,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Enter marks", style = MaterialTheme.typography.titleMedium)
                }
                if (isWide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                        ActionButton("Students", onOpenStudents, Modifier.weight(1f))
                        ActionButton("Subjects", onOpenSubjects, Modifier.weight(1f))
                        ActionButton("Assessments", onOpenAssessments, Modifier.weight(1f))
                        ActionButton("Reports & Excel", onOpenReports, Modifier.weight(1f))
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                        ActionButton("Students", onOpenStudents, Modifier.weight(1f))
                        ActionButton("Subjects", onOpenSubjects, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
                        ActionButton("Assessments", onOpenAssessments, Modifier.weight(1f))
                        ActionButton("Reports & Excel", onOpenReports, Modifier.weight(1f))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
                SectionHeading("Current work", "A quick read on this term")
                Text(
                    text = when {
                        workSummary == null -> "Checking marks for this term..."
                        !workSummary.isAvailable -> "Marks summary is temporarily unavailable."
                        workSummary.enteredMarkCount == 0 && workSummary.expectedMarkCount > 0 -> "No marks entered yet. Choose an assessment to start entering marks."
                        workSummary.expectedMarkCount == 0 -> "No marks to enter yet. Add students, subjects, and an assessment to get started."
                        workSummary.missingMarkCount == 0 -> "All marks are entered for the active assessments in this term."
                        else -> "${workSummary.missingMarkCount} marks are still missing across active assessments."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = when {
                        workSummary?.isAvailable == false -> MaterialTheme.colorScheme.error
                        (workSummary?.missingMarkCount ?: 0) > 0 -> AppColors.warning
                        workSummary?.expectedMarkCount?.let { it > 0 } == true -> AppColors.success
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
                workSummary?.recentAssessmentName?.let { assessmentName ->
                    Text(
                        "Most recent mark activity: $assessmentName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                when {
                    activeStudentCount == 0 -> EmptyPrompt(
                        title = "No students added yet",
                        detail = "Add students to start recording marks.",
                        action = "Add students",
                        onAction = onOpenStudents,
                    )
                    activeSubjectCount == 0 -> EmptyPrompt(
                        title = "No subjects added yet",
                        detail = "Add the subjects taught in this class.",
                        action = "Add subjects",
                        onAction = onOpenSubjects,
                    )
                    activeAssessmentCount == 0 -> EmptyPrompt(
                        title = "No assessments for this term",
                        detail = "Create an assessment to begin entering marks.",
                        action = "Create assessment",
                        onAction = onOpenAssessments,
                    )
                    workSummary?.enteredMarkCount == 0 && (workSummary.expectedMarkCount > 0) -> EmptyPrompt(
                        title = "No marks entered yet",
                        detail = "Choose an assessment and subject to begin.",
                        action = "Enter marks",
                        onAction = onOpenMarks,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
                SectionHeading("Reports & exports", "Review results or create an Excel workbook")
                OutlinedButton(
                    onClick = onOpenReports,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Open reports and Excel exports")
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, supportingText: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(supportingText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    detail: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 124.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(AppSpacing.card),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = contentColor)
            Text(value, style = MaterialTheme.typography.displaySmall, color = contentColor, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = contentColor)
        }
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun EmptyPrompt(title: String, detail: String, action: String, onAction: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.small),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.small),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onAction, modifier = Modifier.align(Alignment.Start)) {
            Text(action)
        }
    }
}

private fun missingCaption(summary: DashboardWorkSummary?): String = when {
    summary == null -> "Calculating"
    !summary.isAvailable -> "Unavailable"
    summary.expectedMarkCount == 0 -> "Nothing due yet"
    summary.missingMarkCount == 0 -> "Complete"
    else -> "Across this term"
}
