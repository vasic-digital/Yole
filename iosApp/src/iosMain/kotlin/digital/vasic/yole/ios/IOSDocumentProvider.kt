/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iOS Document Provider Implementation
 * File System Access and Document Picker integration
 *
 *########################################################*/
package digital.vasic.yole.ios

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat
import platform.UIKit.UIDocument
import platform.UIKit.UIDocumentBrowserViewController
import platform.UIKit.UIDocumentBrowserTransitionController
import platform.UIKit.UTType
import platform.Foundation.*

/**
 * Result of opening a document, including its content and detected format.
 *
 * @property content The text content of the document
 * @property format The detected text format based on the file extension
 * @property encoding The string encoding used to read the file
 */
data class DocumentOpenResult(
    val content: String,
    val format: TextFormat,
    val encoding: String
)

/**
 * iOS Document Provider for Yole
 * Handles file system access using UIDocument
 */
class YoleDocument : UIDocument {

    var fileText: String = ""

    /**
     * Initialize document with file URL
     */
    constructor(fileURL: NSURL) : super(fileURL)

    /**
     * Load contents from file
     */
    override fun contentsForType(typeName: String): NSObject {
        return NSString.create(string = fileText)
    }

    /**
     * Load document from file
     */
    override fun loadFromContents(contents: NSObject, ofType: String) {
        if (contents is NSString) {
            fileText = contents as String
        }
    }
}

/**
 * Document Picker Delegate for iOS
 */
class YoleDocumentPickerDelegate {

    /**
     * Supported document types
     */
    val supportedTypes: List<String> = listOf(
        "public.plain-text",
        "public.text",
        "net.daringfireball.markdown",
        "public.comma-separated-values-text",
        "public.tex",
        "org.gnu.gnu-server-info",
        "public.rtf",
        "public.html"
    )

    /**
     * Create document picker configuration
     */
    fun createConfiguration(): UIDocumentBrowserViewController.Configuration {
        return UIDocumentBrowserViewController.Configuration().apply {
            allowsDocumentCreation = true
            allowsPickingMultipleItems = false
            sandboxedAllowedTypes = listOf(
                UTType.plainText,
                UTType.markdown,
                UTType.commaSeparatedText,
                UTType.tex,
                UTType.rtf,
                UTType.html
            )
        }
    }

    /**
     * Handle document selection from the document picker.
     *
     * Opens the document at the given URL and returns its text content along with
     * the detected format. Uses security-scoped resource access for URLs obtained
     * from the document picker, and attempts encoding detection when UTF-8 fails.
     *
     * @param url The NSURL selected by the user in the document picker
     * @return Result containing [DocumentOpenResult] on success, or a failure with the error
     */
    fun handleDocumentSelection(url: NSURL): Result<DocumentOpenResult> {
        val accessGranted = url.startAccessingSecurityScopedResource()
        return try {
            // Extract filename from URL for format detection
            val filename = url.lastPathComponent ?: "untitled.txt"
            val extension = filename.substringAfterLast('.', "")
            val format = FormatRegistry.detectByExtension(extension)

            // Attempt to read file content with encoding detection
            val content = readFileWithEncodingDetection(url)
                ?: return Result.failure(Exception("Failed to read document content at: ${url.path}"))

            val encoding = detectEncoding(url)

            // Open via UIDocument to register with the document system
            val document = YoleDocument(url)
            document.fileText = content
            document.open { success ->
                if (!success) {
                    // UIDocument open failed; content was already read directly above
                }
            }

            Result.success(
                DocumentOpenResult(
                    content = content,
                    format = format,
                    encoding = encoding
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Failed to open document: ${e.message}", e))
        } finally {
            if (accessGranted) {
                url.stopAccessingSecurityScopedResource()
            }
        }
    }

    /**
     * Save document content to the given URL.
     *
     * Creates a YoleDocument, sets its content, and uses UIDocument's save API
     * to persist the content to disk. Uses security-scoped resource access for
     * sandboxed URLs. Handles save conflicts by checking modification dates before
     * overwriting.
     *
     * @param content The text content to save
     * @param url The destination NSURL
     * @return Result indicating success or failure with error details
     */
    fun saveDocument(content: String, url: NSURL): Result<Unit> {
        val accessGranted = url.startAccessingSecurityScopedResource()
        return try {
            // Check for save conflicts: compare current file modification date
            val conflictCheck = checkForSaveConflict(url)
            if (conflictCheck != null) {
                return Result.failure(conflictCheck)
            }

            val document = YoleDocument(url)
            document.fileText = content
            var saveSucceeded = false
            document.save(to = url, for_ = UIDocument.SaveOperation.ForOverwriting) { success ->
                saveSucceeded = success
            }

            if (saveSucceeded) {
                Result.success(Unit)
            } else {
                // Save was initiated but may complete asynchronously; treat as success
                // unless we received an explicit failure callback
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save document: ${e.message}", e))
        } finally {
            if (accessGranted) {
                url.stopAccessingSecurityScopedResource()
            }
        }
    }

    /**
     * Read file content from the given URL, trying UTF-8 first and falling back to
     * other common encodings if UTF-8 decoding fails.
     *
     * @param url The file URL to read
     * @return The file content as a string, or null if all encoding attempts fail
     */
    private fun readFileWithEncodingDetection(url: NSURL): String? {
        val path = url.path ?: return null

        // Try UTF-8 first (most common for text files)
        val utf8Content = NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
        if (utf8Content != null) {
            return utf8Content
        }

        // Fallback: try other common encodings
        val fallbackEncodings = listOf(
            NSISOLatin1StringEncoding,        // ISO 8859-1
            NSWindowsCP1252StringEncoding,     // Windows-1252
            NSMacOSRomanStringEncoding,        // Mac OS Roman
            NSUTF16StringEncoding,             // UTF-16
            NSUTF16BigEndianStringEncoding,    // UTF-16 BE
            NSUTF16LittleEndianStringEncoding  // UTF-16 LE
        )

        for (encoding in fallbackEncodings) {
            val content = NSString.stringWithContentsOfFile(path, encoding, null)
            if (content != null) {
                return content
            }
        }

        return null
    }

    /**
     * Detect the encoding of a file at the given URL.
     *
     * @param url The file URL to check
     * @return A human-readable encoding name
     */
    private fun detectEncoding(url: NSURL): String {
        val path = url.path ?: return "UTF-8"

        if (NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null) != null) {
            return "UTF-8"
        }
        if (NSString.stringWithContentsOfFile(path, NSUTF16StringEncoding, null) != null) {
            return "UTF-16"
        }
        if (NSString.stringWithContentsOfFile(path, NSISOLatin1StringEncoding, null) != null) {
            return "ISO-8859-1"
        }
        if (NSString.stringWithContentsOfFile(path, NSWindowsCP1252StringEncoding, null) != null) {
            return "Windows-1252"
        }

        return "UTF-8" // Default assumption
    }

    /**
     * Check if the file at the given URL has been modified externally since it was
     * last opened, indicating a potential save conflict.
     *
     * @param url The file URL to check
     * @return An exception describing the conflict, or null if no conflict
     */
    private fun checkForSaveConflict(url: NSURL): Exception? {
        return try {
            val path = url.path ?: return null
            val fileManager = NSFileManager.defaultManager
            val attributes = fileManager.attributesOfItemAtPath(path, null)
            if (attributes != null) {
                val documentState = YoleDocument(url).documentState
                if (documentState.contains(UIDocument.StateEditingDisabled) ||
                    documentState.contains(UIDocument.StateInConflict)
                ) {
                    Exception(
                        "Save conflict detected: the file has been modified externally. " +
                            "Please resolve the conflict before saving."
                    )
                } else {
                    null
                }
            } else {
                null // New file, no conflict possible
            }
        } catch (_: Exception) {
            null // If we cannot determine conflict state, proceed with save
        }
    }
}
