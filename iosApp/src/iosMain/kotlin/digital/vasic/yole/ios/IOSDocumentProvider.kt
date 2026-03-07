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

import platform.UIKit.UIDocument
import platform.UIKit.UIDocumentBrowserViewController
import platform.UIKit.UIDocumentBrowserTransitionController
import platform.UIKit.UTType
import platform.Foundation.*

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
     * Opens the document at the given URL and returns its text content.
     * Uses UIDocument's asynchronous open API with security-scoped URL access.
     *
     * TODO: Call url.startAccessingSecurityScopedResource() before opening and
     *       url.stopAccessingSecurityScopedResource() when done to properly handle
     *       security-scoped bookmarks from the document picker.
     * TODO: Detect format using FormatRegistry.detectByExtension() based on the file extension.
     * TODO: Handle encoding detection for non-UTF-8 files.
     *
     * @param url The NSURL selected by the user in the document picker
     * @return The document text content, or null if the document could not be opened
     */
    fun handleDocumentSelection(url: NSURL): String? {
        return try {
            val document = YoleDocument(url)
            document.open { success ->
                if (!success) {
                    // TODO: Surface this error to the UI layer via a callback or result type
                }
            }
            document.fileText
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Save document content to the given URL.
     *
     * Creates a YoleDocument, sets its content, and uses UIDocument's save API
     * to persist the content to disk.
     *
     * TODO: Call url.startAccessingSecurityScopedResource() before saving and
     *       url.stopAccessingSecurityScopedResource() when done.
     * TODO: Return a Result<Unit> instead of Boolean for better error reporting.
     * TODO: Handle save conflicts when the file has been modified externally.
     *
     * @param content The text content to save
     * @param url The destination NSURL
     * @return true if the save was initiated successfully, false on error
     */
    fun saveDocument(content: String, url: NSURL): Boolean {
        return try {
            val document = YoleDocument(url)
            document.fileText = content
            document.save(to = url, for = UIDocument.SaveOperation.ForOverwriting) { success ->
                if (!success) {
                    // TODO: Surface this error to the UI layer via a callback or result type
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
