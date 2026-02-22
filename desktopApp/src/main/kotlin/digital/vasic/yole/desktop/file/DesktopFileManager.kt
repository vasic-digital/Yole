/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop File Manager for Yole
 * Native file system operations with cross-platform support
 *
 *########################################################*/
package digital.vasic.yole.desktop.file

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.prefs.Preferences
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages file operations for the Yole desktop application.
 * Provides native file system access with security validation,
 * recent files tracking, and format detection.
 */
class DesktopFileManager {
    private val prefs by lazy { Preferences.userNodeForPackage(DesktopFileManager::class.java) }
    private val recentFiles = mutableListOf<File>()
    private val recentFilesLock = Any()
    private val openDocuments = ConcurrentHashMap<String, DesktopDocument>()
    private val maxRecentFiles = 20
    @Volatile
    private var recentFilesLoaded = false

    companion object {
        private const val RECENT_FILES_KEY = "recent_files"
        private const val MAX_RECENT_FILES = 20
        private const val DEFAULT_ENCODING = "UTF-8"
        
        // Supported file extensions for security validation
        private val SUPPORTED_EXTENSIONS = setOf(
            // Text formats
            ".md", ".markdown", ".mdown", ".mkd", ".txt", ".text",
            ".csv", ".tsv", ".tex", ".latex", ".org", ".adoc", ".asciidoc",
            ".wiki", ".mediawiki", ".rst", ".rest", ".pod", ".rdoc",
            ".bib", ".ris", ".enw", ".nbib", ".xml", ".json", ".yaml", ".yml",
            ".html", ".htm", ".xhtml", ".css", ".scss", ".sass", ".less",
            ".js", ".ts", ".jsx", ".tsx", ".vue", ".svelte",
            ".php", ".asp", ".aspx", ".jsp", ".cshtml", ".vbhtml",
            ".java", ".kt", ".kts", ".scala", ".groovy", ".clj", ".cljs",
            ".py", ".pyc", ".pyo", ".pyw", ".pyx", ".pxd", ".pxi",
            ".cpp", ".cc", ".cxx", ".c", ".h", ".hpp", ".hxx", ".hh",
            ".cs", ".fs", ".fsx", ".fsi", ".ml", ".mli", ".rs", ".rlib",
            ".go", ".rb", ".rbw", ".rake", ".gemspec", ".swift", ".m", ".mm",
            ".r", ".rmd", ".rdata", ".rds", ".rda", ".m", ".mat", ".sql",
            ".sh", ".bash", ".zsh", ".fish", ".csh", ".tcsh", ".ksh",
            ".bat", ".cmd", ".ps1", ".psm1", ".psd1", ".vbs", ".wsf",
            ".pl", ".pm", ".t", ".pod", ".lua", ".luac", ".nim", ".nims",
            ".dart", ".coffee", ".litcoffee", ".ts", ".tsx", ".jsx",
            ".pug", ".jade", ".haml", ".slim", ".erb", ".eco", ".mustache",
            ".handlebars", ".hbs", ".dust", ".ejs", ".twig", ".swig",
            ".ini", ".cfg", ".conf", ".config", ".properties", ".prop",
            ".env", ".gitignore", ".dockerignore", ".eslintignore", ".prettierignore",
            ".log", ".bak", ".backup", ".old", ".orig", ".tmp", ".temp",
            ".swp", ".swo", ".swn", ".lock", ".pid", ".seed", ".pid"
        )
    }
    
    // Recent files are loaded lazily on first access to avoid blocking construction
    private fun ensureRecentFilesLoaded() {
        if (!recentFilesLoaded) {
            synchronized(recentFilesLock) {
                if (!recentFilesLoaded) {
                    loadRecentFilesInternal()
                    recentFilesLoaded = true
                }
            }
        }
    }
    
    /**
     * Saves content to a file with validation and security checks.
     */
    fun saveFile(file: File, content: String, encoding: String = DEFAULT_ENCODING): Boolean {
        return try {
            // Security validation
            if (!isValidFilePath(file)) {
                return false
            }
            
            if (!isValidExtension(file.extension)) {
                return false
            }
            
            // Ensure parent directory exists
            file.parentFile?.mkdirs()
            
            // Write content with proper encoding
            Files.writeString(file.toPath(), content, java.nio.charset.Charset.forName(encoding))
            
            // Update recent files
            addToRecentFiles(file)
            
            // Update open documents
            val document = DesktopDocument(
                file = file,
                content = content,
                format = detectFormatFromFile(file),
                lastModified = file.lastModified()
            )
            openDocuments[file.absolutePath] = document
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Loads content from a file with encoding detection.
     */
    fun loadFile(file: File, encoding: String? = null): String? {
        return try {
            if (!file.exists() || !file.isFile || !file.canRead()) {
                return null
            }
            
            val detectedEncoding = encoding ?: detectEncoding(file)
            val content = Files.readString(file.toPath(), java.nio.charset.Charset.forName(detectedEncoding))
            
            // Update recent files
            addToRecentFiles(file)
            
            // Update open documents
            val document = DesktopDocument(
                file = file,
                content = content,
                format = detectFormatFromFile(file),
                lastModified = file.lastModified()
            )
            openDocuments[file.absolutePath] = document
            
            content
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Creates a new document with the specified format.
     */
    fun createNewDocument(file: File, format: TextFormat): Boolean {
        return try {
            val template = getTemplateForFormat(format)
            saveFile(file, template)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Gets the template content for a specific format.
     */
    private fun getTemplateForFormat(format: TextFormat): String {
        return when (format.id) {
            FormatRegistry.ID_MARKDOWN -> "# New Document\n\nStart writing your content here..."
            FormatRegistry.ID_TODOTXT -> "(A) First task @context +project\n(B) Second task"
            FormatRegistry.ID_CSV -> "header1,header2,header3\nvalue1,value2,value3"
            FormatRegistry.ID_LATEX -> "\\documentclass{article}\n\\begin{document}\n\n\\end{document}"
            else -> "New ${format.name} document"
        }
    }
    
    /**
     * Detects file encoding using BOM or content analysis.
     */
    private fun detectEncoding(file: File): String {
        return try {
            val bytes = Files.readAllBytes(file.toPath())
            
            // Check for BOM
            if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
                return "UTF-8"
            }
            if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
                return "UTF-16LE"
            }
            if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
                return "UTF-16BE"
            }
            
            // Default to UTF-8
            DEFAULT_ENCODING
        } catch (e: Exception) {
            DEFAULT_ENCODING
        }
    }
    
    /**
     * Detects format from file extension.
     */
    fun detectFormatFromFile(file: File): TextFormat? {
        val extension = ".${file.extension.lowercase()}"
        return FormatRegistry.getByExtension(extension)
    }
    
    /**
     * Detects format from file content.
     */
    fun detectFormatFromContent(file: File): TextFormat? {
        return try {
            val content = Files.readString(file.toPath())
            FormatRegistry.detectByContent(content)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Adds a file to the recent files list.
     */
    fun addToRecentFiles(file: File) {
        ensureRecentFilesLoaded()
        val filePaths: List<String>
        synchronized(recentFilesLock) {
            recentFiles.remove(file)
            recentFiles.add(0, file)

            // Limit to max recent files
            while (recentFiles.size > maxRecentFiles) {
                recentFiles.removeAt(recentFiles.size - 1)
            }

            // Copy paths while holding lock, then release before I/O
            filePaths = recentFiles.map { it.absolutePath }
        }
        // Save outside the lock to avoid blocking other operations during I/O
        saveRecentFilesAsync(filePaths)
    }
    
    /**
     * Gets the list of recent files.
     */
    fun getRecentFiles(): List<File> {
        ensureRecentFilesLoaded()
        synchronized(recentFilesLock) {
            return recentFiles.filter { it.exists() && it.isFile }
        }
    }
    
    /**
     * Clears non-existent files from recent files list.
     */
    fun cleanRecentFiles() {
        ensureRecentFilesLoaded()
        val filePaths: List<String>
        synchronized(recentFilesLock) {
            recentFiles.removeIf { !it.exists() || !it.isFile }
            filePaths = recentFiles.map { it.absolutePath }
        }
        saveRecentFilesAsync(filePaths)
    }
    
    /**
     * Loads recent files from preferences (internal, called during lazy init).
     * Must be called while holding recentFilesLock.
     */
    private fun loadRecentFilesInternal() {
        try {
            val recentFilesJson = prefs.get(RECENT_FILES_KEY, "[]")
            val filePaths = recentFilesJson.removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotEmpty() }

            recentFiles.clear()
            recentFiles.addAll(filePaths.map { File(it) }.filter { it.exists() })
        } catch (e: Exception) {
            // Ignore errors loading recent files
        }
    }
    
    /**
     * Saves recent files to preferences asynchronously.
     * Takes a pre-copied list to avoid holding locks during I/O.
     */
    private fun saveRecentFilesAsync(filePaths: List<String>) {
        try {
            val recentFilesJson = filePaths.joinToString(",", "[", "]") { "\"$it\"" }
            prefs.put(RECENT_FILES_KEY, recentFilesJson)
        } catch (e: Exception) {
            // Ignore errors saving recent files
        }
    }
    
    /**
     * Gets open documents.
     */
    fun getOpenDocuments(): List<DesktopDocument> {
        return openDocuments.values.toList()
    }
    
    /**
     * Closes a document.
     */
    fun closeDocument(filePath: String) {
        openDocuments.remove(filePath)
    }
    
    /**
     * Checks if a document has unsaved changes.
     */
    fun hasUnsavedChanges(file: File): Boolean {
        val document = openDocuments[file.absolutePath] ?: return false
        return try {
            val currentContent = Files.readString(file.toPath())
            document.content != currentContent
        } catch (e: Exception) {
            true
        }
    }
    
    /**
     * Validates file extension for security.
     */
    fun isValidExtension(extension: String): Boolean {
        return SUPPORTED_EXTENSIONS.contains(extension.lowercase())
    }
    
    /**
     * Sanitizes file name for security.
     */
    fun sanitizeFileName(fileName: String): String {
        return fileName.replace(Regex("[<>&\"']"), "_")
            .replace(Regex("[^\\w\\-. ]"), "_")
            .trim()
    }
    
    /**
     * Validates file path for security (prevents directory traversal).
     */
    fun isValidFilePath(file: File): Boolean {
        return try {
            val canonicalPath = file.canonicalPath
            // Additional security checks can be added here
            !file.name.startsWith(".") && // Hidden files
            !file.name.contains("..") && // Directory traversal
            file.name.length < 255 // Reasonable filename length
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Gets file information.
     */
    fun getFileInfo(file: File): DesktopFileInfo? {
        return try {
            if (!file.exists()) return null
            
            DesktopFileInfo(
                file = file,
                size = file.length(),
                lastModified = file.lastModified(),
                isReadable = file.canRead(),
                isWritable = file.canWrite(),
                format = detectFormatFromFile(file)
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Creates a backup of a file.
     */
    fun createBackup(file: File): Boolean {
        return try {
            if (!file.exists() || !file.isFile) return false
            
            val backupFile = File("${file.absolutePath}.bak")
            Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Reads content from a file. Alias for loadFile.
     */
    fun readFile(file: File, encoding: String? = null): String? {
        return loadFile(file, encoding)
    }
    
    /**
     * Writes content to a file. Alias for saveFile.
     */
    fun writeFile(file: File, content: String, encoding: String = DEFAULT_ENCODING): Boolean {
        return saveFile(file, content, encoding)
    }
}

/**
 * Represents a desktop document with metadata.
 */
data class DesktopDocument(
    val file: File,
    val content: String,
    val format: TextFormat?,
    val lastModified: Long,
    val encoding: String = "UTF-8"
)

/**
 * File information data class.
 */
data class DesktopFileInfo(
    val file: File,
    val size: Long,
    val lastModified: Long,
    val isReadable: Boolean,
    val isWritable: Boolean,
    val format: TextFormat?
)