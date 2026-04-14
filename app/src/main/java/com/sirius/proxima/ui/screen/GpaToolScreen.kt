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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sirius.proxima.ui.theme.ProximaTheme
import com.sirius.proxima.viewmodel.AcademicToolsViewModel

private data class GpaRow(var gradePoint: String = "", var credits: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpaToolScreen(
    onBack: () -> Unit,
    viewModel: AcademicToolsViewModel = viewModel(
        factory = AcademicToolsViewModel.factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val previousCgpa = viewModel.previousCgpa.collectAsStateWithLifecycle().value
    val previousCredits = viewModel.previousCredits.collectAsStateWithLifecycle().value
    val rows = remember { mutableStateListOf(GpaRow()) }
    if (rows.isEmpty()) rows.add(GpaRow())

    val gpa = calculateGpa(rows)
    val cgpa = calculateCgpa(gpa, rows, previousCgpa, previousCredits)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GPA / CGPA") },
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
            itemsIndexed(rows) { index, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = row.gradePoint,
                        onValueChange = { rows[index] = row.copy(gradePoint = it) },
                        label = { Text("Grade") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = row.credits,
                        onValueChange = { rows[index] = row.copy(credits = it) },
                        label = { Text("Credits") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            item {
                OutlinedButton(onClick = { rows.add(GpaRow()) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Subject")
                }
            }

            item {
                OutlinedTextField(
                    value = previousCgpa,
                    onValueChange = { viewModel.setPreviousCgpa(it) },
                    label = { Text("Previous CGPA") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = previousCredits,
                    onValueChange = { viewModel.setPreviousCredits(it) },
                    label = { Text("Previous Credits") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Current GPA: ${"%.2f".format(gpa)}", fontWeight = FontWeight.SemiBold)
                    Text("Estimated CGPA: ${"%.2f".format(cgpa)}", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun GpaToolScreenPreview() {
    ProximaTheme {
        GpaToolScreen(onBack = {})
    }
}

private fun calculateGpa(rows: List<GpaRow>): Double {
    var totalWeighted = 0.0
    var totalCredits = 0.0
    rows.forEach {
        val gp = it.gradePoint.toDoubleOrNull() ?: 0.0
        val cr = it.credits.toDoubleOrNull() ?: 0.0
        totalWeighted += gp * cr
        totalCredits += cr
    }
    return if (totalCredits > 0) totalWeighted / totalCredits else 0.0
}

private fun calculateCgpa(gpa: Double, rows: List<GpaRow>, prevCgpa: String, prevCredits: String): Double {
    val currentCredits = rows.sumOf { it.credits.toDoubleOrNull() ?: 0.0 }
    val previousCgpa = prevCgpa.toDoubleOrNull() ?: return gpa
    val previousCredits = prevCredits.toDoubleOrNull() ?: return gpa
    val totalCredits = currentCredits + previousCredits
    return if (totalCredits > 0) ((gpa * currentCredits) + (previousCgpa * previousCredits)) / totalCredits else gpa
}

