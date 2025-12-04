/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * WebAssembly-specific Document implementations
 * Platform-specific file operations for Web (Wasm)
 *
 *########################################################*/
package digital.vasic.yole.model

import kotlinx.browser.window

/**
 * WebAssembly-specific implementation of platform-agnostic functions
 * Note: Web has limited file system access, so many operations are simulated
 */

/**
 * Get current time in milliseconds since epoch
 */
actual fun currentTimeMillis(): Long {
    return (window.performance.now() * 1000).toLong()
}

/**
 * Get file modification time for the given path
 * Note: Web doesn't have direct file system access
 */
actual fun Document.getFileModTime(): Long {
    // In Web environment, we can't access file metadata directly
    // Return a default value or use localStorage to track modification times
    return try {
        val storedTime = window.localStorage.getItem("modTime_$path")
        storedTime?.toLongOrNull() ?: currentTimeMillis()
    } catch (e: Exception) {
        currentTimeMillis()
    }
}

/**
 * Get file size in bytes for the given path
 * Note: Web doesn't have direct file system access
 */
actual fun Document.getFileSize(): Long {
    // In Web environment, we can't access file metadata directly
    // Return a default value or use localStorage to track file sizes
    return try {
        val storedSize = window.localStorage.getItem("size_$path")
        storedSize?.toLongOrNull() ?: 0L
    } catch (e: Exception) {
        0L
    }
}

/**
 * Check if file exists at the given path
 * Note: Web doesn't have direct file system access
 */
actual fun Document.fileExists(): Boolean {
    // In Web environment, check if content exists in localStorage
    return try {
        window.localStorage.getItem("content_$path") != null
    } catch (e: Exception) {
        false
    }
}

/**
 * Create a Document from a file path
 * Note: Web uses virtual file paths in localStorage
 */
actual fun createDocument(path: String): Document? {
    return try {
        // Check if content exists in localStorage
        val content = window.localStorage.getItem("content_$path")
        if (content == null) return null
        
        val name = path.substringAfterLast('/', path)
        val extension = if (name.contains('.')) {
            name.substringAfterLast('.', "")
        } else {
            ""
        }
        val title = if (extension.isNotEmpty()) {
            name.substringBeforeLast(".$extension")
        } else {
            name
        }
        
        Document(
            path = path,
            title = title,
            extension = extension,
            modTime = currentTimeMillis(),
            touchTime = currentTimeMillis()
        ).apply {
            detectFormatByExtension()
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Web-specific Document extensions using localStorage
 */
fun Document.readContent(): String? {
    return try {
        window.localStorage.getItem("content_$path")
    } catch (e: Exception) {
        null
    }
}

fun Document.writeContent(content: String): Boolean {
    return try {
        window.localStorage.setItem("content_$path", content)
        window.localStorage.setItem("modTime_$path", currentTimeMillis().toString())
        window.localStorage.setItem("size_$path", content.length.toString())
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * Get all documents stored in localStorage
 */
fun getAllDocuments(): List<Document> {
    val documents = mutableListOf<Document>()
    
    try {
        for (i in 0 until window.localStorage.length) {
            val key = window.localStorage.key(i) ?: continue
            if (key.startsWith("content_")) {
                val path = key.removePrefix("content_")
                val document = createDocument(path)
                if (document != null) {
                    documents.add(document)
                }
            }
        }
    } catch (e: Exception) {
        // Ignore errors in Web environment
    }
    
    return documents
}

/**
 * Delete document from localStorage
 */
fun Document.delete(): Boolean {
    return try {
        window.localStorage.removeItem("content_$path")
        window.localStorage.removeItem("modTime_$path")
        window.localStorage.removeItem("size_$path")
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * Get virtual documents directory for Web
 */
fun getDocumentsDirectory(): String {
    return "/documents"
}

/**
 * Get Yole-specific documents directory
 */
fun getYoleDocumentsDirectory(): String {
    return "/documents/yole"
}