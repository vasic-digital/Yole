/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iOS-specific Document implementations
 * Platform-specific file operations for iOS
 *
 *########################################################*/
package digital.vasic.yole.model

import platform.Foundation.*

/**
 * iOS-specific implementation of platform-agnostic functions
 */

/**
 * Get current time in milliseconds since epoch
 */
actual fun currentTimeMillis(): Long {
    return (NSDate.date().timeIntervalSince1970 * 1000).toLong()
}

/**
 * Get file modification time for the given path
 */
actual fun Document.getFileModTime(): Long {
    return try {
        val fileManager = NSFileManager.defaultManager
        val attributes = fileManager.attributesOfItemAtPath(path, null)
        val modDate = attributes?.objectForKey(NSFileModificationDate) as? NSDate
        modDate?.timeIntervalSince1970?.times(1000)?.toLong() ?: -1L
    } catch (e: Exception) {
        -1L
    }
}

/**
 * Get file size in bytes for the given path
 */
actual fun Document.getFileSize(): Long {
    return try {
        val fileManager = NSFileManager.defaultManager
        val attributes = fileManager.attributesOfItemAtPath(path, null)
        val fileSize = attributes?.objectForKey(NSFileSize) as? NSNumber
        fileSize?.longValue ?: -1L
    } catch (e: Exception) {
        -1L
    }
}

/**
 * Check if file exists at the given path
 */
actual fun Document.fileExists(): Boolean {
    return try {
        val fileManager = NSFileManager.defaultManager
        fileManager.fileExistsAtPath(path)
    } catch (e: Exception) {
        false
    }
}

/**
 * Create a Document from a file path
 */
actual fun createDocument(path: String): Document? {
    return try {
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(path)) return null
        
        val url = NSURL.fileURLWithPath(path)
        val fileName = url.lastPathComponent ?: ""
        
        val extension = if (fileName.contains(".")) {
            fileName.substringAfterLast(".", "")
        } else {
            ""
        }
        val title = if (extension.isNotEmpty()) {
            fileName.substringBeforeLast(".$extension")
        } else {
            fileName
        }
        
        Document(
            path = path,
            title = title,
            extension = extension,
            modTime = getFileModTime(),
            touchTime = currentTimeMillis()
        ).apply {
            detectFormatByExtension()
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * iOS-specific Document extensions
 */
fun Document.readContent(): String? {
    return try {
        NSString.stringWithContentsOfFile(path, encoding = NSUTF8StringEncoding, error = null) as? String
    } catch (e: Exception) {
        null
    }
}

fun Document.writeContent(content: String): Boolean {
    return try {
        content.writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    } catch (e: Exception) {
        false
    }
}

/**
 * Get documents directory for iOS
 */
fun getDocumentsDirectory(): String {
    val fileManager = NSFileManager.defaultManager
    val urls = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
    val documentsURL = urls.lastObject as? NSURL
    return documentsURL?.path ?: ""
}

/**
 * Get Yole-specific documents directory
 */
fun getYoleDocumentsDirectory(): String {
    val documents = getDocumentsDirectory()
    val yoleDir = documents + "/Yole"
    
    // Create if doesn't exist
    val fileManager = NSFileManager.defaultManager
    if (!fileManager.fileExistsAtPath(yoleDir)) {
        fileManager.createDirectoryAtPath(yoleDir, withIntermediateDirectories = true, attributes = null, error = null)
    }
    
    return yoleDir
}