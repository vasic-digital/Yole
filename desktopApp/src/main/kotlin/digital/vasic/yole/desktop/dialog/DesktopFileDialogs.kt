/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop File Dialogs for Yole
 * Native file open/save dialogs with format filtering
 *
 *########################################################*/
package digital.vasic.yole.desktop.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.window.AwtWindow
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

/**
 * Native file dialogs for desktop application.
 * Provides platform-native file open/save dialogs with format filtering.
 */
object DesktopFileDialogs {
    
    /**
     * Shows a file open dialog and returns the selected file.
     */
    @Composable
    fun showOpenDialog(
        parent: Frame? = null,
        title: String = "Open File",
        defaultDirectory: String? = null,
        allowedFormats: List<TextFormat> = FormatRegistry.formats,
        onResult: (File?) -> Unit
    ) {
        AwtWindow(
            create = {
                FileDialog(parent, title, FileDialog.LOAD).apply {
                    // Set default directory
                    defaultDirectory?.let { directory = it }
                    
                    // Set file filter for allowed formats
                    filenameFilter = createFilenameFilter(allowedFormats)
                    
                    // Add window listener to handle result
                    addWindowListener(object : java.awt.event.WindowAdapter() {
                        override fun windowClosing(e: java.awt.event.WindowEvent?) {
                            onResult(null)
                        }
                        override fun windowClosed(e: java.awt.event.WindowEvent?) {
                            val selectedFile = getSelectedFile(this@apply)
                            onResult(selectedFile)
                        }
                    })
                }
            },
            dispose = { it.dispose() }
        )
    }
    
    /**
     * Shows a file save dialog and returns the selected file.
     */
    @Composable
    fun showSaveDialog(
        parent: Frame? = null,
        title: String = "Save File",
        defaultDirectory: String? = null,
        defaultFileName: String? = null,
        allowedFormats: List<TextFormat> = FormatRegistry.formats,
        onResult: (File?) -> Unit
    ) {
        AwtWindow(
            create = {
                FileDialog(parent, title, FileDialog.SAVE).apply {
                    // Set default directory
                    defaultDirectory?.let { directory = it }
                    
                    // Set default file name
                    defaultFileName?.let { file = it }
                    
                    // Set file filter for allowed formats
                    filenameFilter = createFilenameFilter(allowedFormats)
                    
                    // Add window listener to handle result
                    addWindowListener(object : java.awt.event.WindowAdapter() {
                        override fun windowClosing(e: java.awt.event.WindowEvent?) {
                            onResult(null)
                        }
                        override fun windowClosed(e: java.awt.event.WindowEvent?) {
                            val selectedFile = getSelectedFile(this@apply)
                            onResult(selectedFile)
                        }
                    })
                }
            },
            dispose = { it.dispose() }
        )
    }
    
    /**
     * Shows a directory selection dialog.
     */
    @Composable
    fun showDirectoryDialog(
        parent: Frame? = null,
        title: String = "Select Directory",
        defaultDirectory: String? = null,
        onResult: (File?) -> Unit
    ) {
        AwtWindow(
            create = {
                FileDialog(parent, title, FileDialog.LOAD).apply {
                    // Set default directory
                    defaultDirectory?.let { directory = it }
                    
                    // Set mode to directory selection (custom implementation)
                    setFile("*.*") // Show all files in directory
                    
                    // Add window listener to handle result
                    addWindowListener(object : java.awt.event.WindowAdapter() {
                        override fun windowClosing(e: java.awt.event.WindowEvent?) {
                            onResult(null)
                        }
                        override fun windowClosed(e: java.awt.event.WindowEvent?) {
                            val selectedDirectory = directory?.let { File(it) }
                            onResult(selectedDirectory)
                        }
                    })
                }
            },
            dispose = { it.dispose() }
        )
    }
    
    /**
     * Shows a multi-file selection dialog.
     */
    @Composable
    fun showMultiOpenDialog(
        parent: Frame? = null,
        title: String = "Open Files",
        defaultDirectory: String? = null,
        allowedFormats: List<TextFormat> = FormatRegistry.formats,
        onResult: (List<File>) -> Unit
    ) {
        AwtWindow(
            create = {
                FileDialog(parent, title, FileDialog.LOAD).apply {
                    // Set default directory
                    defaultDirectory?.let { directory = it }
                    
                    // Enable multiple selection
                    isMultipleMode = true
                    
                    // Set file filter for allowed formats
                    filenameFilter = createFilenameFilter(allowedFormats)
                    
                    // Add window listener to handle result
                    addWindowListener(object : java.awt.event.WindowAdapter() {
                        override fun windowClosing(e: java.awt.event.WindowEvent?) {
                            onResult(emptyList())
                        }
                        override fun windowClosed(e: java.awt.event.WindowEvent?) {
                            val selectedFiles = getSelectedFiles(this@apply)?.toList() ?: emptyList()
                            onResult(selectedFiles)
                        }
                    })
                }
            },
            dispose = { it.dispose() }
        )
    }
    
    /**
     * Creates a filename filter for the given formats.
     */
    private fun createFilenameFilter(formats: List<TextFormat>): FilenameFilter {
        val extensions = formats.flatMap { format ->
            format.extensions.map { ext ->
                ext.removePrefix(".").lowercase()
            }
        }.distinct()
        
        return FilenameFilter { _, name ->
            val fileExtension = name.substringAfterLast(".", "").lowercase()
            extensions.contains(fileExtension)
        }
    }
    
    /**
     * Gets the selected file from the file dialog.
     */
    private fun getSelectedFile(dialog: FileDialog): File? {
        val selectedFile = dialog.file
        val selectedDirectory = dialog.directory
        
        return if (selectedFile != null && selectedDirectory != null) {
            File(selectedDirectory + File.separator + selectedFile)
        } else {
            null
        }
    }
    
    /**
     * Gets the selected files from the file dialog (multi-selection).
     */
    private fun getSelectedFiles(dialog: FileDialog): Array<File>? {
        val selectedFiles = dialog.files
        val selectedDirectory = dialog.directory
        
        return if (selectedFiles != null && selectedDirectory != null) {
            selectedFiles.map { file ->
                File(selectedDirectory + File.separator + file)
            }.toTypedArray()
        } else {
            null
        }
    }
    
    /**
     * Creates a file filter description for the given formats.
     */
    fun createFileFilterDescription(formats: List<TextFormat>): String {
        val extensions = formats.flatMap { it.extensions }.distinct().sorted()
        return when {
            extensions.size <= 5 -> "Supported formats (${extensions.joinToString(", ")})"
            else -> "Supported formats (${extensions.take(5).joinToString(", ")}...)"
        }
    }
    
    /**
     * Gets the default file extension for a format.
     */
    fun getDefaultExtension(format: TextFormat): String {
        return format.defaultExtension
    }
    
    /**
     * Suggests a file name based on content and format.
     */
    fun suggestFileName(content: String, format: TextFormat): String {
        val firstLine = content.lineSequence().firstOrNull()?.trim()
        val baseName = when {
            firstLine?.startsWith("#") == true -> firstLine.removePrefix("#").trim()
            firstLine?.isNotBlank() == true -> firstLine.take(50)
            else -> "Untitled"
        }
        
        return sanitizeFileName(baseName) + format.defaultExtension
    }
    
    /**
     * Sanitizes a file name for cross-platform compatibility.
     */
    fun sanitizeFileName(fileName: String): String {
        return fileName
            .replace(Regex("[<>:\"/\\\\|?*]"), "_")
            .replace(Regex("[^\\w\\-. ]"), "_")
            .trim()
            .take(200) // Reasonable length limit
    }
}

/**
 * File dialog result wrapper.
 */
sealed class FileDialogResult {
    data class Success(val file: File) : FileDialogResult()
    data class MultipleSuccess(val files: List<File>) : FileDialogResult()
    object Cancelled : FileDialogResult()
    data class Error(val message: String) : FileDialogResult()
}