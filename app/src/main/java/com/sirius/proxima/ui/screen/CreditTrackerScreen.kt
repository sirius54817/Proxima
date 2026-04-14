package com.sirius.proxima.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sirius.proxima.ui.theme.ProximaTheme
import com.sirius.proxima.viewmodel.AcademicToolsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditTrackerScreen(
    onBack: () -> Unit,
    viewModel: AcademicToolsViewModel = viewModel(
        factory = AcademicToolsViewModel.factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val completedCredits by viewModel.completedCredits.collectAsStateWithLifecycle()
    val requiredCredits by viewModel.requiredCredits.collectAsStateWithLifecycle()

    val completed = completedCredits.toFloatOrNull() ?: 0f
    val required = requiredCredits.toFloatOrNull() ?: 0f
    val progress = if (required > 0) ((completed / required) * 100f).coerceIn(0f, 100f) else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Credit Tracker") },
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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = completedCredits,
                onValueChange = { viewModel.setCompletedCredits(it) },
                label = { Text("Completed credits") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = requiredCredits,
                onValueChange = { viewModel.setRequiredCredits(it) },
                label = { Text("Required credits") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("Progress: ${"%.2f".format(progress)}%", fontWeight = FontWeight.SemiBold)
            Text("Remaining: ${"%.2f".format((required - completed).coerceAtLeast(0f).toDouble())} credits")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun CreditTrackerScreenPreview() {
    ProximaTheme {
        CreditTrackerScreen(onBack = {})
    }
}

