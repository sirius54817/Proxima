package com.sirius.proxima.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sirius.proxima.data.model.Subject
import com.sirius.proxima.ui.theme.Border
import com.sirius.proxima.ui.theme.MutedForeground
import com.sirius.proxima.ui.theme.ProximaTheme

@Composable
fun AddEditSubjectDialog(
    subject: Subject? = null,
    onSave: (name: String, totalClasses: Int, attendedClasses: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(subject?.name ?: "") }
    var totalClasses by remember { mutableStateOf(subject?.totalClasses?.toString() ?: "0") }
    var attendedClasses by remember { mutableStateOf(subject?.attendedClasses?.toString() ?: "0") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Text(
                text = if (subject != null) "Edit Subject" else "Add Subject",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Subject Name") },
                    isError = nameError,
                    supportingText = if (nameError) {{ Text("Name is required") }} else null,
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Border
                    )
                )

                OutlinedTextField(
                    value = totalClasses,
                    onValueChange = { totalClasses = it.filter { c -> c.isDigit() } },
                    label = { Text("Total Classes") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Border
                    )
                )

                OutlinedTextField(
                    value = attendedClasses,
                    onValueChange = { attendedClasses = it.filter { c -> c.isDigit() } },
                    label = { Text("Attended Classes") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Border
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val total = totalClasses.toIntOrNull() ?: 0
                    val attended = attendedClasses.toIntOrNull() ?: 0
                    onSave(name.trim(), total, attended)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (subject != null) "Update" else "Add")
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
fun AddSubjectDialogPreview() {
    ProximaTheme {
        AddEditSubjectDialog(
            onSave = { _, _, _ -> },
            onDismiss = {}
        )
    }
}

@Preview
@Composable
fun EditSubjectDialogPreview() {
    ProximaTheme {
        AddEditSubjectDialog(
            subject = Subject(id = 1, name = "Mathematics", totalClasses = 40, attendedClasses = 35),
            onSave = { _, _, _ -> },
            onDismiss = {}
        )
    }
}

