package com.sirius.proxima.ui.screen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.sirius.proxima.ui.components.ConfirmDialog
import com.sirius.proxima.ui.theme.Border
import com.sirius.proxima.ui.theme.MutedForeground
import com.sirius.proxima.ui.theme.ProximaTheme
import com.sirius.proxima.ui.theme.StudyMaterialOtherBlue
import com.sirius.proxima.ui.theme.StudyMaterialPdfRed
import com.sirius.proxima.ui.theme.StudyMaterialPptOrange
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
    val deleteInProgress by viewModel.pdfDeleteInProgress.collectAsStateWithLifecycle()
    val deleteProgress by viewModel.pdfDeleteProgress.collectAsStateWithLifecycle()
    val deleteProgressText by viewModel.pdfDeleteProgressText.collectAsStateWithLifecycle()
    var selectedSubjectId by remember(subjects) { mutableStateOf(subjects.firstOrNull()?.id) }
    var selectedPdfIds by remember { mutableStateOf(setOf<Int>()) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    val allPdfIds = remember(pdfs) { pdfs.map { it.id }.toSet() }

    val isSelectionMode = selectedPdfIds.isNotEmpty()
    val allSelected = allPdfIds.isNotEmpty() && selectedPdfIds.size == allPdfIds.size

    LaunchedEffect(allPdfIds) {
        selectedPdfIds = selectedPdfIds.intersect(allPdfIds)
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        val subjectId = selectedSubjectId ?: return@rememberLauncherForActivityResult
        if (uris.isNotEmpty()) {
            viewModel.importStudyPdfs(subjectId, uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Material") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !deleteInProgress) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(
                            onClick = {
                                showDeleteSelectedDialog = true
                            },
                            enabled = !deleteInProgress
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected materials")
                        }
                        IconButton(onClick = { selectedPdfIds = emptySet() }, enabled = !deleteInProgress) {
                            Icon(Icons.Default.Close, contentDescription = "Exit selection")
                        }
                    } else {
                        IconButton(
                            onClick = {
                                picker.launch(
                                    arrayOf(
                                        "application/pdf",
                                        "application/vnd.ms-powerpoint",
                                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                        "application/msword",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "text/plain",
                                        "text/markdown",
                                        "text/x-markdown"
                                    )
                                )
                            },
                            enabled = !deleteInProgress
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add study material")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
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
                    if (allPdfIds.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    selectedPdfIds = if (allSelected) emptySet() else allPdfIds
                                },
                                enabled = !deleteInProgress
                            ) {
                                Text(if (allSelected) "Clear all" else "Select all")
                            }
                            OutlinedButton(
                                onClick = { selectedPdfIds = emptySet() },
                                enabled = selectedPdfIds.isNotEmpty() && !deleteInProgress
                            ) {
                                Text("Clear selection")
                            }
                        }
                    }
                    if (isSelectionMode) {
                        Text(
                            text = "${selectedPdfIds.size} selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedForeground,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    var expanded by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !deleteInProgress
                    ) {
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
                        val isSelected = selectedPdfIds.contains(pdf.id)
                        StudyPdfRow(
                            pdf = pdf,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            enabled = !deleteInProgress,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedPdfIds = if (isSelected) selectedPdfIds - pdf.id else selectedPdfIds + pdf.id
                                    return@StudyPdfRow
                                }

                                val file = File(pdf.filePath)
                                if (!file.exists()) return@StudyPdfRow
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, resolveMimeTypeForPath(pdf.filePath))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: ActivityNotFoundException) {
                                    // No compatible app installed for this material type.
                                }
                            },
                            onLongPress = {
                                if (deleteInProgress) return@StudyPdfRow
                                selectedPdfIds = selectedPdfIds + pdf.id
                            }
                        )
                    }
                }

                if (pdfs.isEmpty()) {
                    item { Text("No study materials added yet", color = MutedForeground) }
                }
            }

            if (deleteInProgress) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Card {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 18.dp, vertical = 16.dp)
                                .fillMaxWidth(0.78f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp).size(18.dp), strokeWidth = 2.dp)
                                Text("Deleting Study Material", fontWeight = FontWeight.SemiBold)
                            }
                            LinearProgressIndicator(progress = { deleteProgress }, modifier = Modifier.fillMaxWidth())
                            Text(deleteProgressText, style = MaterialTheme.typography.bodySmall, color = MutedForeground)
                            Text("Please wait. Actions are locked until completion.", style = MaterialTheme.typography.bodySmall, color = MutedForeground)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteSelectedDialog) {
        val selected = pdfs.filter { it.id in selectedPdfIds }
        ConfirmDialog(
            title = "Delete Study Material",
            message = "Delete ${selected.size} selected file(s) from app and backup?",
            confirmText = "Delete",
            isDangerous = true,
            onConfirm = {
                if (selected.isNotEmpty()) {
                    viewModel.deleteStudyPdfs(selected)
                }
                selectedPdfIds = emptySet()
                showDeleteSelectedDialog = false
            },
            onDismiss = { showDeleteSelectedDialog = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StudyPdfRow(
    pdf: StudyPdf,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val materialType = detectMaterialType(pdf.filePath)
    val typeColor = when (materialType) {
        StudyMaterialType.PDF -> StudyMaterialPdfRed
        StudyMaterialType.PPT -> StudyMaterialPptOrange
        StudyMaterialType.OTHER -> StudyMaterialOtherBlue
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(enabled = enabled, onClick = onClick, onLongClick = onLongPress),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else typeColor.copy(alpha = 0.85f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceContainer else typeColor.copy(alpha = 0.14f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(pdf.title, fontWeight = FontWeight.Medium)
            Text(
                text = when (materialType) {
                    StudyMaterialType.PDF -> "PDF"
                    StudyMaterialType.PPT -> "PPT"
                    StudyMaterialType.OTHER -> "FILE"
                },
                style = MaterialTheme.typography.labelSmall,
                color = typeColor,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = pdf.filePath,
                style = MaterialTheme.typography.bodySmall,
                color = MutedForeground,
                maxLines = 1
            )
            Text(
                if (isSelectionMode) "Tap to select/unselect"
                else "Tap to open | Long press to select",
                style = MaterialTheme.typography.bodySmall,
                color = MutedForeground
            )
        }
    }
}

private enum class StudyMaterialType { PDF, PPT, OTHER }

private fun detectMaterialType(path: String): StudyMaterialType {
    val ext = File(path).extension.lowercase()
    return when (ext) {
        "pdf" -> StudyMaterialType.PDF
        "ppt", "pptx" -> StudyMaterialType.PPT
        else -> StudyMaterialType.OTHER
    }
}

private fun resolveMimeTypeForPath(path: String): String {
    return when (File(path).extension.lowercase()) {
        "pdf" -> "application/pdf"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "txt", "md" -> "text/plain"
        else -> "*/*"
    }
}



@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun StudyPdfScreenPreview() {
    ProximaTheme {
        StudyPdfScreen(onBack = {})
    }
}
