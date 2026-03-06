package com.sirius.proxima.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sirius.proxima.data.model.Subject
import com.sirius.proxima.ui.theme.Border
import com.sirius.proxima.ui.theme.MutedForeground
import com.sirius.proxima.ui.theme.ProximaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTimetableEntryDialog(
    subjects: List<Subject>,
    existingSubjectName: String? = null,
    existingClassNumber: String? = null,
    onSave: (Subject, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSubject by remember {
        mutableStateOf(
            if (existingSubjectName != null) subjects.find { it.name == existingSubjectName } else null
        )
    }
    var classNumber by remember { mutableStateOf(existingClassNumber ?: "") }
    var expanded by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Text(
                text = if (existingSubjectName != null) "Edit Entry" else "Add Class",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (subjects.isEmpty()) {
                    Text(
                        text = "No subjects added yet. Add subjects from the Home screen first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedForeground
                    )
                } else {
                    // Subject dropdown
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedSubject?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Subject") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            isError = showError && selectedSubject == null,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Border
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            subjects.forEach { subject ->
                                DropdownMenuItem(
                                    text = { Text(subject.name) },
                                    onClick = {
                                        selectedSubject = subject
                                        expanded = false
                                        showError = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = classNumber,
                        onValueChange = { classNumber = it },
                        label = { Text("Class / Room Number") },
                        placeholder = { Text("e.g. Class 3, Lab 2") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Border
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val subj = selectedSubject
                    if (subj == null) {
                        showError = true
                        return@Button
                    }
                    onSave(subj, classNumber.trim())
                },
                enabled = subjects.isNotEmpty(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Border)
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    )
}

@Preview
@Composable
fun AddTimetableEntryDialogPreview() {
    ProximaTheme {
        AddTimetableEntryDialog(
            subjects = listOf(
                Subject(1, "Mathematics", 40, 35),
                Subject(2, "Physics", 30, 20)
            ),
            onSave = { _, _ -> },
            onDismiss = {}
        )
    }
}


