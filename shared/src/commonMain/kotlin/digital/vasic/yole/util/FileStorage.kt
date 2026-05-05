/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Platform-agnostic file storage interface.
 * Each platform provides an actual implementation.
 *
 *########################################################*/
package digital.vasic.yole.util

/**
 * Platform-specific file handle wrapping a URI.
 * Android uses SAF ContentResolver; Desktop uses java.io.File.
 */
expect class FileHandle(uri: String) {
    val uri: String
}

/**
 * Read all bytes from the file handle.
 * Returns null if the file cannot be read or does not exist.
 */
expect fun FileHandle.readBytes(): ByteArray?

/**
 * Write bytes to the file handle, truncating existing content.
 * Returns true on success, false on failure.
 */
expect fun FileHandle.writeBytes(data: ByteArray): Boolean

/**
 * Check whether the file exists and is accessible.
 */
expect fun FileHandle.exists(): Boolean

/**
 * Get the display name (filename) of the file, or null if unavailable.
 * On Android, this maps to OpenableColumns.DISPLAY_NAME.
 */
expect fun FileHandle.displayName(): String?
