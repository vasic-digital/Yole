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
     * Handle document selection
     */
    fun handleDocumentSelection(url: NSURL): String? {
        return try {
            val document = YoleDocument(url)
            document.open { success ->
                if (success) {
                    println("Document opened: ${url.lastPathComponent}")
                }
            }
            document.fileText
        } catch (e: Exception) {
            println("Error opening document: ${e.message}")
            null
        }
    }
    
    /**
     * Save document to URL
     */
    fun saveDocument(content: String, url: NSURL): Boolean {
        return try {
            val document = YoleDocument(url)
            document.fileText = content
            document.save(to = url, for = UIDocument.SaveOperation.ForOverwriting) { success ->
                if (success) {
                    println("Document saved: ${url.lastPathComponent}")
                }
            }
            true
        } catch (e: Exception) {
            println("Error saving document: ${e.message}")
            false
        }
    }
}
