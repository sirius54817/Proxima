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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sirius.proxima.ui.theme.ProximaTheme
import com.sirius.proxima.viewmodel.AcademicToolsViewModel
import com.sirius.proxima.viewmodel.millisToLocalDateTimeText
import java.time.Duration
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamTimetableScreen(
    onBack: () -> Unit,
    viewModel: AcademicToolsViewModel = viewModel(
        factory = AcademicToolsViewModel.factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    var subject by remember { mutableStateOf("") }
    var dateTime by remember { mutableStateOf("") }
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            now = LocalDateTime.now()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exam Timetable") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Exam subject") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = dateTime,
                    onValueChange = { dateTime = it },
                    label = { Text("DateTime (YYYY-MM-DDTHH:MM)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                Button(
                    onClick = {
                        if (subject.isNotBlank() && dateTime.isNotBlank()) {
                            viewModel.addExam(subject.trim(), dateTime.trim())
                            subject = ""
                            dateTime = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Exam")
                }
            }

            items(exams, key = { it.id }) { item ->
                val examText = millisToLocalDateTimeText(item.examAtMillis)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.subject)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("$examText (${countdown(examText, now)})")
                    }
                    TextButton(onClick = { viewModel.deleteExam(item) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun ExamTimetableScreenPreview() {
    ProximaTheme {
        ExamTimetableScreen(onBack = {})
    }
}

private fun countdown(dateTime: String, now: LocalDateTime): String {
    val exam = runCatching { LocalDateTime.parse(dateTime) }.getOrNull() ?: return "Invalid time"
    val minutes = Duration.between(now, exam).toMinutes()
    return when {
        minutes < 0 -> "Started"
        else -> {
            val days = minutes / (60 * 24)
            val hours = (minutes % (60 * 24)) / 60
            val mins = minutes % 60
            "${days}d ${hours}h ${mins}m"
        }
    }
}

