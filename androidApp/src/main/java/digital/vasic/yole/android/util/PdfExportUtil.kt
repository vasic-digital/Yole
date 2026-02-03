/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * PDF Export Utility for Yole Android App
 *
 *########################################################*/

package digital.vasic.yole.android.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Chunk
import com.itextpdf.text.List
import com.itextpdf.text.ListItem
import com.itextpdf.text.Element
import com.itextpdf.text.Font
import com.itextpdf.text.FontFactory
import com.itextpdf.text.BaseColor
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.PageSize
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for exporting content to PDF format
 */
object PdfExportUtil {

    private const val PDF_AUTHORITY = "digital.vasic.yole.android.fileprovider"
    private const val PDF_DIR = "exported_pdfs"

    /**
     * Export text content to PDF file
     */
    fun exportToPdf(
        context: Context,
        content: String,
        fileName: String,
        format: String = "text"
    ): Result<Uri> {
        return try {
            // Create PDF directory if it doesn't exist
            val pdfDir = File(context.getExternalFilesDir(null), PDF_DIR)
            if (!pdfDir.exists()) {
                pdfDir.mkdirs()
            }

            // Generate unique filename with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val pdfFileName = fileName.replace(Regex("\\.[^.]*$"), "") + "_$timestamp.pdf"
            val pdfFile = File(pdfDir, pdfFileName)

            // Create PDF document with proper resource management
            val document = Document(PageSize.A4)
            val outputStream = FileOutputStream(pdfFile)

            try {
                PdfWriter.getInstance(document, outputStream)
                document.open()

                // Add metadata
                document.addTitle("Yole Export - $fileName")
                document.addAuthor("Yole Text Editor")
                document.addSubject("Exported document from Yole")
                document.addCreator("Yole Android App")

                // Format and add content based on file type
                when (format.lowercase()) {
                    "markdown" -> addMarkdownContent(document, content)
                    "todotxt" -> addTodoTxtContent(document, content)
                    "csv" -> addCsvContent(document, content)
                    else -> addPlainTextContent(document, content)
                }
            } finally {
                // Ensure document is closed even if an exception occurs
                try {
                    if (document.isOpen) {
                        document.close()
                    }
                } catch (closeException: Exception) {
                    // Log but don't throw - we want to close outputStream too
                }
                // Ensure output stream is closed
                try {
                    outputStream.close()
                } catch (closeException: Exception) {
                    // Ignore close exception
                }
            }

            // Create content URI for sharing
            val uri = FileProvider.getUriForFile(context, PDF_AUTHORITY, pdfFile)
            Result.success(uri)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add plain text content to PDF
     */
    private fun addPlainTextContent(document: Document, content: String) {
        // Add title
        val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f, BaseColor.BLACK)
        val title = Paragraph("Exported Document", titleFont)
        title.alignment = Element.ALIGN_CENTER
        title.spacingAfter = 20f
        document.add(title)

        // Add export info
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val infoFont = FontFactory.getFont(FontFactory.HELVETICA, 10f, BaseColor.GRAY)
        val info = Paragraph("Exported on: ${dateFormat.format(Date())}", infoFont)
        info.alignment = Element.ALIGN_CENTER
        info.spacingAfter = 30f
        document.add(info)

        // Add content
        val paragraphs = content.split("\n\n")
        for (paragraph in paragraphs) {
            if (paragraph.isNotBlank()) {
                val contentFont = FontFactory.getFont(FontFactory.HELVETICA, 12f, BaseColor.BLACK)
                val pdfParagraph = Paragraph(paragraph.trim(), contentFont)
                pdfParagraph.alignment = Element.ALIGN_LEFT
                pdfParagraph.spacingAfter = 12f
                document.add(pdfParagraph)
            }
        }
    }

    /**
     * Add Markdown content to PDF with basic formatting
     */
    private fun addMarkdownContent(document: Document, content: String) {
        // Add title
        val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f, BaseColor.BLACK)
        val title = Paragraph("Markdown Document", titleFont)
        title.alignment = Element.ALIGN_CENTER
        title.spacingAfter = 20f
        document.add(title)

        // Add export info
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val infoFont = FontFactory.getFont(FontFactory.HELVETICA, 10f, BaseColor.GRAY)
        val info = Paragraph("Exported on: ${dateFormat.format(Date())}", infoFont)
        info.alignment = Element.ALIGN_CENTER
        info.spacingAfter = 30f
        document.add(info)

        // Process markdown lines
        val lines = content.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            when {
                line.startsWith("# ") -> {
                    // Main heading
                    val headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16f, BaseColor.BLACK)
                    val heading = Paragraph(line.substring(2), headingFont)
                    heading.spacingBefore = 20f
                    heading.spacingAfter = 10f
                    document.add(heading)
                }
                line.startsWith("## ") -> {
                    // Subheading
                    val headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f, BaseColor.BLACK)
                    val heading = Paragraph(line.substring(3), headingFont)
                    heading.spacingBefore = 15f
                    heading.spacingAfter = 8f
                    document.add(heading)
                }
                line.startsWith("### ") -> {
                    // Sub-subheading
                    val headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, BaseColor.BLACK)
                    val heading = Paragraph(line.substring(4), headingFont)
                    heading.spacingBefore = 10f
                    heading.spacingAfter = 6f
                    document.add(heading)
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    // List item
                    val list = List(List.UNORDERED)
                    while (i < lines.size && (lines[i].startsWith("- ") || lines[i].startsWith("* "))) {
                        val itemFont = FontFactory.getFont(FontFactory.HELVETICA, 12f, BaseColor.BLACK)
                        list.add(ListItem(lines[i].substring(2), itemFont))
                        i++
                    }
                    document.add(list)
                    i-- // Adjust for loop increment
                }
                line.startsWith("```") -> {
                    // Code block
                    val codeContent = StringBuilder()
                    i++
                    while (i < lines.size && !lines[i].startsWith("```")) {
                        codeContent.append(lines[i]).append("\n")
                        i++
                    }
                    val codeFont = FontFactory.getFont(FontFactory.COURIER, 10f, BaseColor.BLACK)
                    val codeParagraph = Paragraph(codeContent.toString(), codeFont)
                    codeParagraph.setLeading(12f)
                    // Add background color by creating a cell with background
                    val codeCell = PdfPCell()
                    codeCell.addElement(codeParagraph)
                    codeCell.backgroundColor = BaseColor.LIGHT_GRAY
                    codeCell.setPadding(10f)
                    codeCell.borderWidth = 0f
                    
                    val codeTable = PdfPTable(1)
                    codeTable.widthPercentage = 100f
                    codeTable.addCell(codeCell)
                    codeTable.spacingAfter = 10f
                    document.add(codeTable)
                }
                line.isNotBlank() -> {
                    // Regular paragraph with inline formatting
                    val paragraph = formatInlineMarkdown(line)
                    paragraph.spacingAfter = 10f
                    document.add(paragraph)
                }
            }
            i++
        }
    }

    /**
     * Add Todo.txt content to PDF
     */
    private fun addTodoTxtContent(document: Document, content: String) {
        // Add title
        val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f, BaseColor.BLACK)
        val title = Paragraph("Todo List", titleFont)
        title.alignment = Element.ALIGN_CENTER
        title.spacingAfter = 20f
        document.add(title)

        // Add export info
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val infoFont = FontFactory.getFont(FontFactory.HELVETICA, 10f, BaseColor.GRAY)
        val info = Paragraph("Exported on: ${dateFormat.format(Date())}", infoFont)
        info.alignment = Element.ALIGN_CENTER
        info.spacingAfter = 30f
        document.add(info)

        // Process todo items
        val lines = content.lines()
        val completedItems = mutableListOf<String>()
        val activeItems = mutableListOf<String>()

        for (line in lines) {
            when {
                line.startsWith("x ") -> completedItems.add(line.substring(2))
                line.isNotBlank() -> activeItems.add(line)
            }
        }

        // Add active items
        if (activeItems.isNotEmpty()) {
            val activeHeadingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f, BaseColor.BLACK)
            val activeHeading = Paragraph("Active Tasks", activeHeadingFont)
            activeHeading.spacingBefore = 15f
            activeHeading.spacingAfter = 10f
            document.add(activeHeading)

            val activeList = List(List.UNORDERED)
            val itemFont = FontFactory.getFont(FontFactory.HELVETICA, 12f, BaseColor.BLACK)
            for (item in activeItems) {
                activeList.add(ListItem(item, itemFont))
            }
            document.add(activeList)
        }

        // Add completed items
        if (completedItems.isNotEmpty()) {
            val completedHeadingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f, BaseColor.BLACK)
            val completedHeading = Paragraph("Completed Tasks", completedHeadingFont)
            completedHeading.spacingBefore = 15f
            completedHeading.spacingAfter = 10f
            document.add(completedHeading)

            val completedList = List(List.UNORDERED)
            val completedFont = FontFactory.getFont(FontFactory.HELVETICA, 12f, BaseColor.GRAY)
            for (item in completedItems) {
                completedList.add(ListItem(item, completedFont))
            }
            document.add(completedList)
        }

        if (activeItems.isEmpty() && completedItems.isEmpty()) {
            val noItemsFont = FontFactory.getFont(FontFactory.HELVETICA, 12f, BaseColor.BLACK)
            val noItems = Paragraph("No todo items found.", noItemsFont)
            noItems.alignment = Element.ALIGN_CENTER
            noItems.spacingBefore = 50f
            document.add(noItems)
        }
    }

    /**
     * Add CSV content to PDF as a table
     */
    private fun addCsvContent(document: Document, content: String) {
        // Add title
        val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f, BaseColor.BLACK)
        val title = Paragraph("CSV Data", titleFont)
        title.alignment = Element.ALIGN_CENTER
        title.spacingAfter = 20f
        document.add(title)

        // Add export info
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val infoFont = FontFactory.getFont(FontFactory.HELVETICA, 10f, BaseColor.GRAY)
        val info = Paragraph("Exported on: ${dateFormat.format(Date())}", infoFont)
        info.alignment = Element.ALIGN_CENTER
        info.spacingAfter = 30f
        document.add(info)

        // Parse CSV and create table
        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.isNotEmpty()) {
            val rows = lines.map { line ->
                line.split(",").map { it.trim().replace("\"", "") }
            }

            val numColumns = rows.maxOf { it.size }
            val table = PdfPTable(numColumns)
            table.widthPercentage = 100f

            // Add header row with bold text
            val headerRow = rows.firstOrNull()
            if (headerRow != null) {
                val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, BaseColor.BLACK)
                for (cell in headerRow) {
                    val cellParagraph = Paragraph(cell.toString(), headerFont)
                    val pdfCell = PdfPCell(cellParagraph)
                    pdfCell.setPadding(5f)
                    table.addCell(pdfCell)
                }
            }

            // Add data rows
            val dataFont = FontFactory.getFont(FontFactory.HELVETICA, 12f, BaseColor.BLACK)
            for (i in 1 until rows.size) {
                val row = rows[i]
                for (j in 0 until numColumns) {
                    val cell = if (j < row.size) row[j] else ""
                    val cellParagraph = Paragraph(cell.toString(), dataFont)
                    val pdfCell = PdfPCell(cellParagraph)
                    pdfCell.setPadding(5f)
                    table.addCell(pdfCell)
                }
            }

            document.add(table)
        } else {
            val noDataFont = FontFactory.getFont(FontFactory.HELVETICA, 12f, BaseColor.BLACK)
            val noData = Paragraph("No CSV data found.", noDataFont)
            noData.alignment = Element.ALIGN_CENTER
            noData.spacingBefore = 50f
            document.add(noData)
        }
    }

    /**
     * Format inline markdown elements (bold, italic, code)
     */
    private fun formatInlineMarkdown(text: String): Paragraph {
        val paragraph = Paragraph()
        var remaining = text

        // Process bold text **text**
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
        var match = boldRegex.find(remaining)
        while (match != null) {
            val before = remaining.substring(0, match.range.first)
            if (before.isNotEmpty()) {
                val beforeFont = FontFactory.getFont(FontFactory.HELVETICA, 12f, BaseColor.BLACK)
                paragraph.add(Chunk(before, beforeFont))
            }
            val boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, BaseColor.BLACK)
            paragraph.add(Chunk(match.groupValues[1], boldFont))
            remaining = remaining.substring(match.range.last + 1)
            match = boldRegex.find(remaining)
        }

        // Process italic text *text*
        val italicRegex = Regex("\\*(.*?)\\*")
        match = italicRegex.find(remaining)
        while (match != null) {
            val before = remaining.substring(0, match.range.first)
            if (before.isNotEmpty()) {
                val beforeFont = FontFactory.getFont(FontFactory.HELVETICA, 12f, BaseColor.BLACK)
                paragraph.add(Chunk(before, beforeFont))
            }
            val italicFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 12f, BaseColor.BLACK)
            paragraph.add(Chunk(match.groupValues[1], italicFont))
            remaining = remaining.substring(match.range.last + 1)
            match = italicRegex.find(remaining)
        }

        // Process code text `text`
        val codeRegex = Regex("`(.*?)`")
        match = codeRegex.find(remaining)
        while (match != null) {
            val before = remaining.substring(0, match.range.first)
            if (before.isNotEmpty()) {
                val beforeFont = FontFactory.getFont(FontFactory.HELVETICA, 12f, BaseColor.BLACK)
                paragraph.add(Chunk(before, beforeFont))
            }
            val codeFont = FontFactory.getFont(FontFactory.COURIER, 10f, BaseColor.BLACK)
            paragraph.add(Chunk(match.groupValues[1], codeFont))
            remaining = remaining.substring(match.range.last + 1)
            match = codeRegex.find(remaining)
        }

        // Add remaining text
        if (remaining.isNotEmpty()) {
            val remainingFont = FontFactory.getFont(FontFactory.HELVETICA, 12f, BaseColor.BLACK)
            paragraph.add(Chunk(remaining, remainingFont))
        }

        return paragraph
    }

    /**
     * Get all exported PDF files
     */
    fun getExportedPdfs(context: Context): kotlin.collections.List<File> {
        val pdfDir = File(context.getExternalFilesDir(null), PDF_DIR)
        return if (pdfDir.exists()) {
            pdfDir.listFiles { file -> file.extension.equals("pdf", ignoreCase = true) }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    /**
     * Delete old PDF files (older than 7 days)
     */
    fun cleanupOldPdfs(context: Context) {
        val pdfDir = File(context.getExternalFilesDir(null), PDF_DIR)
        if (pdfDir.exists()) {
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            pdfDir.listFiles { file ->
                file.extension.equals("pdf", ignoreCase = true) && file.lastModified() < sevenDaysAgo
            }?.forEach { it.delete() }
        }
    }
}