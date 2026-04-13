package com.sirius.proxima.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sirius.proxima.ui.theme.AttendanceRed
import com.sirius.proxima.ui.theme.MutedForeground
import com.sirius.proxima.data.model.AttendanceStatus
import com.sirius.proxima.viewmodel.HomeViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectHistoryScreen(
    subjectId: Int,
    onBack: () -> Unit,
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val history by viewModel.getAttendanceHistory(subjectId).collectAsStateWithLifecycle(initialValue = emptyList())
    val loadingSubjectId by viewModel.historyPortalLoadingSubjectId.collectAsStateWithLifecycle()
    val portalError by viewModel.historyPortalError.collectAsStateWithLifecycle()
    val sisUnlocked by viewModel.sisFeaturesUnlocked.collectAsStateWithLifecycle()

    val subject = subjects.find { it.id == subjectId }
    var visibleCount by remember(subjectId) { mutableStateOf(15) }
    var showManualAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(history.size) {
        if (history.size < visibleCount) {
            visibleCount = history.size.coerceAtLeast(15)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = subject?.name ?: "Subject History",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            val errorMessage = portalError
            if (sisUnlocked && errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = AttendanceRed.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = AttendanceRed
                        )
                        TextButton(onClick = { viewModel.clearHistoryPortalError() }) {
                            Text("Dismiss")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { visibleCount += 15 },
                    enabled = visibleCount < history.size,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Load Next 15")
                }

                if (sisUnlocked) {
                    Button(
                        onClick = { if (subject != null) viewModel.loadMoreHistoryFromPortal(subject) },
                        enabled = loadingSubjectId != subjectId && subject != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (loadingSubjectId == subjectId) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                        Text(if (loadingSubjectId == subjectId) "Loading..." else "Load from Portal")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (history.isEmpty()) "No data available" else "Showing ${history.take(visibleCount).size} of ${history.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MutedForeground
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (history.isEmpty()) {
                Text(
                    text = "No data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedForeground
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showManualAddDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add your own entry")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history.take(visibleCount), key = { it.id }) { record ->
                        val slotText = record.slotName?.takeIf { it.isNotBlank() }?.let { " - $it" } ?: ""
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Text(
                                text = "${record.date} - ${record.status}$slotText",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    if (showManualAddDialog) {
        var dateText by remember { mutableStateOf(LocalDate.now().toString()) }
        var slotText by remember { mutableStateOf("") }
        var selectedStatus by remember { mutableStateOf(AttendanceStatus.PRESENT) }

        AlertDialog(
            onDismissRequest = { showManualAddDialog = false },
            title = { Text("Add attendance entry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = slotText,
                        onValueChange = { slotText = it },
                        label = { Text("Slot (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(AttendanceStatus.PRESENT, AttendanceStatus.ABSENT, AttendanceStatus.ON_DUTY).forEach { status ->
                            OutlinedButton(onClick = { selectedStatus = status }) {
                                Text(if (selectedStatus == status) "$status *" else status)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (subject != null && dateText.isNotBlank()) {
                            viewModel.addManualAttendanceRecord(
                                subjectId = subject.id,
                                status = selectedStatus,
                                date = dateText.trim(),
                                slotName = slotText.trim().ifBlank { null }
                            )
                            showManualAddDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


