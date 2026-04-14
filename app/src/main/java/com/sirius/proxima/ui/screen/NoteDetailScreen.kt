package com.sirius.proxima.ui.screen

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sirius.proxima.ui.theme.MutedForeground
import com.sirius.proxima.ui.theme.ProximaTheme
import com.sirius.proxima.viewmodel.StudyViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Int,
    onBack: () -> Unit,
    viewModel: StudyViewModel = viewModel(
        factory = StudyViewModel.factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val context = LocalContext.current
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val note by viewModel.getNoteById(noteId).collectAsStateWithLifecycle(initialValue = null)

    var subjectId by remember { mutableStateOf<Int?>(null) }
    var activeNoteId by remember { mutableStateOf(noteId.takeIf { it != 0 }) }
    var title by remember { mutableStateOf("") }
    var bodyField by remember { mutableStateOf(TextFieldValue("")) }
    var isChecklist by remember { mutableStateOf(false) }

    var showSearch by remember { mutableStateOf(false) }
    var showMarkdownPreview by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var selectedOccurrence by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = readTextFromUri(context, uri) ?: return@rememberLauncherForActivityResult
        bodyField = TextFieldValue(text)
        if (title.isBlank()) {
            resolveFileName(context, uri)?.substringBeforeLast('.')?.let { if (it.isNotBlank()) title = it }
        }
        val fileName = resolveFileName(context, uri).orEmpty().lowercase()
        if (fileName.endsWith(".md")) {
            showMarkdownPreview = true
        }
    }

    LaunchedEffect(note?.note?.id, subjects) {
        val current = note
        if (current != null) {
            subjectId = current.note.subjectId
            title = current.note.title
            isChecklist = current.note.isChecklist
            val loadedText = if (current.note.isChecklist) {
                current.checklistItems.sortedBy { it.position }.joinToString("\n") { it.text }
            } else {
                current.note.content
            }
            bodyField = TextFieldValue(loadedText)
        } else if (noteId == 0 && subjects.isNotEmpty()) {
            subjectId = subjectId ?: subjects.first().id
        }
    }

    val words = tokenizeSearchWords(searchText)
    val matches = remember(bodyField.text, words) { findMatchRanges(bodyField.text, words) }
    val safeSelected = if (matches.isEmpty()) -1 else selectedOccurrence.coerceIn(0, matches.lastIndex)

    LaunchedEffect(subjectId, title, bodyField.text, isChecklist) {
        val resolvedSubjectId = subjectId ?: return@LaunchedEffect
        if (title.isBlank()) return@LaunchedEffect
        delay(1200)
        activeNoteId = viewModel.upsertNoteAndGetId(
            noteId = activeNoteId,
            subjectId = resolvedSubjectId,
            title = title,
            content = if (isChecklist) "" else bodyField.text,
            isChecklist = isChecklist,
            checklistItems = if (isChecklist) bodyField.text.lines().filter { it.isNotBlank() } else emptyList()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == 0) "New Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val resolvedSubjectId = subjectId ?: return@TextButton
                            viewModel.addOrUpdateNote(
                                noteId = activeNoteId,
                                subjectId = resolvedSubjectId,
                                title = title,
                                content = if (isChecklist) "" else bodyField.text,
                                isChecklist = isChecklist,
                                checklistItems = if (isChecklist) bodyField.text.lines().filter { it.isNotBlank() } else emptyList()
                            )
                            onBack()
                        },
                        enabled = subjectId != null && title.isNotBlank()
                    ) { Text("Save", fontWeight = FontWeight.SemiBold) }

                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(if (showSearch) "Hide Search" else "Find in note") },
                            onClick = {
                                showMenu = false
                                showSearch = !showSearch
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Import .txt / .md") },
                            onClick = {
                                showMenu = false
                                importLauncher.launch(arrayOf("text/plain", "text/markdown", "text/*"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (showMarkdownPreview) "Edit Text" else "Markdown Preview") },
                            onClick = {
                                showMenu = false
                                showMarkdownPreview = !showMarkdownPreview
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isChecklist) "Checklist: On" else "Checklist: Off") },
                            onClick = {
                                showMenu = false
                                isChecklist = !isChecklist
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var expanded by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.weight(0.42f)) {
                    Text(subjects.firstOrNull { it.id == subjectId }?.name ?: "Subject")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    subjects.forEach { subject ->
                        DropdownMenuItem(
                            text = { Text(subject.name) },
                            onClick = {
                                subjectId = subject.id
                                expanded = false
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.weight(0.58f)
                )
            }

            if (showSearch) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = {
                            searchText = it
                            selectedOccurrence = 0
                        },
                        label = { Text("Search") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        if (matches.isNotEmpty()) {
                            val target = if (safeSelected <= 0) matches.lastIndex else safeSelected - 1
                            selectedOccurrence = target
                            val range = matches[target]
                            bodyField = bodyField.copy(selection = TextRange(range.first, range.second))
                        }
                    }) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous") }
                    IconButton(onClick = {
                        if (matches.isNotEmpty()) {
                            val target = if (safeSelected >= matches.lastIndex) 0 else safeSelected + 1
                            selectedOccurrence = target
                            val range = matches[target]
                            bodyField = bodyField.copy(selection = TextRange(range.first, range.second))
                        }
                    }) { Icon(Icons.Default.ChevronRight, contentDescription = "Next") }
                }
                Text(
                    text = if (matches.isEmpty()) "0 matches" else "${safeSelected + 1}/${matches.size} matches",
                    color = MutedForeground,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (showMarkdownPreview && !isChecklist) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MarkdownPreview(bodyField.text)
                }
            } else {
                TextField(
                    value = bodyField,
                    onValueChange = { bodyField = it },
                    placeholder = {
                        Text(if (isChecklist) "Checklist items, one per line" else "Start typing...")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        focusedIndicatorColor = MaterialTheme.colorScheme.background,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.background,
                        disabledIndicatorColor = MaterialTheme.colorScheme.background
                    ),
                    maxLines = Int.MAX_VALUE
                )
            }
        }
    }
}

@Composable
private fun MarkdownPreview(text: String) {
    val lines = text.lines()
    lines.forEach { line ->
        when {
            line.startsWith("### ") -> Text(line.removePrefix("### "), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            line.startsWith("## ") -> Text(line.removePrefix("## "), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            line.startsWith("# ") -> Text(line.removePrefix("# "), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            line.startsWith("- ") -> Text(text = "• ${line.removePrefix("- ")}", style = MaterialTheme.typography.bodyLarge)
            else -> Text(text = parseInlineMarkdown(line), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun parseInlineMarkdown(line: String): AnnotatedString {
    val out = AnnotatedString.Builder()
    var i = 0
    while (i < line.length) {
        if (i + 1 < line.length && line[i] == '*' && line[i + 1] == '*') {
            val end = line.indexOf("**", i + 2)
            if (end > i) {
                val boldText = line.substring(i + 2, end)
                out.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                out.append(boldText)
                out.pop()
                i = end + 2
                continue
            }
        }
        if (line[i] == '*') {
            val end = line.indexOf('*', i + 1)
            if (end > i) {
                val italicText = line.substring(i + 1, end)
                out.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                out.append(italicText)
                out.pop()
                i = end + 1
                continue
            }
        }
        out.append(line[i])
        i += 1
    }
    return out.toAnnotatedString()
}

private fun tokenizeSearchWords(query: String): List<String> =
    query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }.distinct()

private fun findMatchRanges(text: String, words: List<String>): List<Pair<Int, Int>> {
    if (words.isEmpty() || text.isBlank()) return emptyList()
    val ranges = mutableListOf<Pair<Int, Int>>()
    words.forEach { word ->
        var start = 0
        while (start < text.length) {
            val idx = text.indexOf(word, startIndex = start, ignoreCase = true)
            if (idx < 0) break
            ranges += idx to (idx + word.length)
            start = idx + word.length
        }
    }
    return ranges.sortedBy { it.first }
}

private fun resolveFileName(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) return it.getString(idx)
        }
    }
    return null
}

private fun readTextFromUri(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    }.getOrNull()
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun NoteDetailScreenPreview() {
    ProximaTheme {
        NoteDetailScreen(noteId = 0, onBack = {})
    }
}

