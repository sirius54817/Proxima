package com.sirius.proxima.ui.screen

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sirius.proxima.ui.theme.ProximaTheme
import com.sirius.proxima.viewmodel.StudyViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPdfViewerScreen(
    pdfId: Int,
    onBack: () -> Unit,
    viewModel: StudyViewModel = viewModel(
        factory = StudyViewModel.factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val pdf by viewModel.getStudyPdfById(pdfId).collectAsStateWithLifecycle(initialValue = null)
    var pageIndex by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(0) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pdf?.filePath, pageIndex) {
        val path = pdf?.filePath ?: return@LaunchedEffect
        runCatching {
            val file = File(path)
            if (!file.exists()) return@runCatching
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            PdfRenderer(fd).use { renderer ->
                pageCount = renderer.pageCount
                if (pageCount == 0) return@use
                val safePage = pageIndex.coerceIn(0, pageCount - 1)
                renderer.openPage(safePage).use { page ->
                    val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap = bmp
                }
            }
            fd.close()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pdf?.title ?: "PDF") },
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
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { if (pageIndex > 0) pageIndex -= 1 }, enabled = pageIndex > 0) { Text("Prev") }
                Text("Page ${if (pageCount == 0) 0 else pageIndex + 1}/$pageCount", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { if (pageIndex < pageCount - 1) pageIndex += 1 }, enabled = pageIndex < pageCount - 1) { Text("Next") }
            }
            bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "PDF page",
                    modifier = Modifier.fillMaxWidth()
                )
            } ?: Text("Unable to open PDF")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun StudyPdfViewerScreenPreview() {
    ProximaTheme {
        StudyPdfViewerScreen(pdfId = 0, onBack = {})
    }
}

