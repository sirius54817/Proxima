package com.sirius.proxima.ui.screen

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.sirius.proxima.data.model.NoteWithChecklist
import java.io.File

object NotePdfExporter {
    fun exportAndShare(context: Context, notes: List<NoteWithChecklist>) {
        if (notes.isEmpty()) return

        val document = PdfDocument()
        val paint = Paint().apply { textSize = 12f }
        var page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        var canvas = page.canvas
        var y = 40

        fun nextPage(pageNumber: Int) {
            document.finishPage(page)
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            canvas = page.canvas
            y = 40
        }

        notes.forEachIndexed { index, noteWithChecklist ->
            val note = noteWithChecklist.note
            val lines = mutableListOf("${index + 1}. ${note.title}")
            if (note.content.isNotBlank()) lines.add(note.content)
            noteWithChecklist.checklistItems
                .sortedBy { it.position }
                .forEach { item -> lines.add("- [${if (item.isChecked) "x" else " "}] ${item.text}") }
            lines.add(" ")

            lines.forEach { line ->
                if (y > 800) nextPage(document.pages.size + 1)
                canvas.drawText(line.take(90), 30f, y.toFloat(), paint)
                y += 20
            }
        }

        document.finishPage(page)
        val file = File(context.cacheDir, "proxima-notes-${System.currentTimeMillis()}.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export Notes PDF"))
    }
}

