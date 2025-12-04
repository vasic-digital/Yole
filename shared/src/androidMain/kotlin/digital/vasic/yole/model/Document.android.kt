/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Android-specific Document implementations
 *
 *########################################################*/
package digital.vasic.yole.model

import digital.vasic.yole.format.FormatRegistry
import java.io.File

/**
 * Get current time in milliseconds since epoch (Android)
 */
actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
}

/**
 * Get file modification time for the document (Android)
 */
actual fun Document.getFileModTime(): Long {
    return try {
        val file = File(path)
        file.lastModified()
    } catch (e: Exception) {
        -1L
    }
}

/**
 * Get file size in bytes (Android)
 */
actual fun Document.getFileSize(): Long {
    return try {
        val file = File(path)
        file.length()
    } catch (e: Exception) {
        0L
    }
}

/**
 * Check if file exists (Android)
 */
actual fun Document.fileExists(): Boolean {
    return try {
        val file = File(path)
        file.exists()
    } catch (e: Exception) {
        false
    }
}

/**
 * Create a Document from a file path (Android)
 */
actual fun createDocument(path: String): Document? {
    return try {
        val file = File(path)
        if (!file.exists()) {
            return null
        }

        val extension = file.extension
        val title = file.nameWithoutExtension

        // Use FormatRegistry for format detection
        val detectedFormat = FormatRegistry.detectByExtension(extension)

        Document(
            path = file.absolutePath,
            title = title,
            extension = extension,
            format = detectedFormat.id
        )
    } catch (e: Exception) {
        null
    }
}
