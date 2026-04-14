package com.sirius.proxima.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sirius.proxima.data.model.NoteWithChecklist
import com.sirius.proxima.ui.theme.Border
import com.sirius.proxima.ui.theme.MutedForeground
import com.sirius.proxima.ui.theme.ProximaTheme
import com.sirius.proxima.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    onBack: () -> Unit,
    onOpenNote: (Int) -> Unit,
    onCreateNote: () -> Unit,
    viewModel: StudyViewModel = viewModel(
        factory = StudyViewModel.factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val query by viewModel.noteSearchQuery.collectAsStateWithLifecycle()

    val orderedNotes = notes.sortedBy { it.note.title.lowercase() }
    var selectedMatchIndex by remember(orderedNotes.size) { mutableIntStateOf(if (orderedNotes.isNotEmpty()) 0 else -1) }
    val selectedNoteId = orderedNotes.getOrNull(selectedMatchIndex)?.note?.id

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onCreateNote) {
                        Icon(Icons.Default.Add, contentDescription = "Add note")
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
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::setNoteSearch,
                    label = { Text("Search words") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            if (orderedNotes.isNotEmpty()) {
                                selectedMatchIndex = if (selectedMatchIndex <= 0) orderedNotes.lastIndex else selectedMatchIndex - 1
                            }
                        }
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                    }
                    Text(
                        text = if (orderedNotes.isEmpty()) "0 matches" else "${selectedMatchIndex + 1}/${orderedNotes.size} matches",
                        modifier = Modifier.padding(top = 12.dp),
                        color = MutedForeground
                    )
                    IconButton(
                        onClick = {
                            if (orderedNotes.isNotEmpty()) {
                                selectedMatchIndex = if (selectedMatchIndex >= orderedNotes.lastIndex) 0 else selectedMatchIndex + 1
                            }
                        }
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                    }
                }
            }

            if (orderedNotes.isEmpty()) {
                item { Text("No notes found", color = MutedForeground) }
            } else {
                val grouped = orderedNotes.groupBy { it.note.subjectId }
                subjects.forEach { subject ->
                    val subjectNotes = grouped[subject.id].orEmpty()
                    if (subjectNotes.isEmpty()) return@forEach

                    item {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    items(subjectNotes, key = { it.note.id }) { note ->
                        NoteRow(
                            note = note,
                            isSelected = note.note.id == selectedNoteId,
                            onClick = { onOpenNote(note.note.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteRow(
    note: NoteWithChecklist,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else Border
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(note.note.title, fontWeight = FontWeight.Medium)
            if (note.note.content.isNotBlank()) {
                Text(
                    text = note.note.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedForeground,
                    maxLines = 2
                )
            }
            if (note.checklistItems.isNotEmpty()) {
                Text(
                    text = "${note.checklistItems.count { it.isChecked }}/${note.checklistItems.size} checklist done",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedForeground
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun NotesListScreenPreview() {
    ProximaTheme {
        NotesListScreen(onBack = {}, onOpenNote = {}, onCreateNote = {})
    }
}

