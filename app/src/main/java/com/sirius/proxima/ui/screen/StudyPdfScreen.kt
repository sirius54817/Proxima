package com.sirius.proxima.ui.screen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sirius.proxima.data.model.StudyPdf
import com.sirius.proxima.ui.theme.Border
import com.sirius.proxima.ui.theme.MutedForeground
import com.sirius.proxima.ui.theme.ProximaTheme
import com.sirius.proxima.viewmodel.StudyViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPdfScreen(
    onBack: () -> Unit,
    viewModel: StudyViewModel = viewModel(
        factory = StudyViewModel.factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val context = LocalContext.current
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val pdfs by viewModel.studyPdfs.collectAsStateWithLifecycle()
    val importStatus by viewModel.pdfImportStatus.collectAsStateWithLifecycle()
    var selectedSubjectId by remember(subjects) { mutableStateOf(subjects.firstOrNull()?.id) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val subjectId = selectedSubjectId ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            viewModel.importStudyPdf(subjectId, uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study PDF") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { picker.launch(arrayOf("application/pdf")) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add PDF")
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
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                importStatus?.let { status ->
                    Text(status, color = MutedForeground)
                }
                var expanded by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(subjects.firstOrNull { it.id == selectedSubjectId }?.name ?: "Select Subject")
                }
                androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    subjects.forEach { subject ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(subject.name) },
                            onClick = {
                                selectedSubjectId = subject.id
                                expanded = false
                            }
                        )
                    }
                }
            }

            val grouped = pdfs.groupBy { it.subjectId }
            subjects.forEach { subject ->
                val list = grouped[subject.id].orEmpty()
                if (list.isEmpty()) return@forEach
                item { Text(subject.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                items(list, key = { it.id }) { pdf ->
                    StudyPdfRow(
                        pdf = pdf,
                        onClick = {
                            val file = File(pdf.filePath)
                            if (!file.exists()) return@StudyPdfRow
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: ActivityNotFoundException) {
                                // No PDF app installed; silently ignore for now.
                            }
                        }
                    )
                }
            }

            if (pdfs.isEmpty()) {
                item { Text("No PDFs added yet", color = MutedForeground) }
            }
        }
    }
}

@Composable
private fun StudyPdfRow(pdf: StudyPdf, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        border = BorderStroke(1.dp, Border),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(pdf.title, fontWeight = FontWeight.Medium)
            Text(
                text = pdf.filePath,
                style = MaterialTheme.typography.bodySmall,
                color = MutedForeground,
                maxLines = 1
            )
            Text("Tap to open with PDF app", style = MaterialTheme.typography.bodySmall, color = MutedForeground)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun StudyPdfScreenPreview() {
    ProximaTheme {
        StudyPdfScreen(onBack = {})
    }
}
