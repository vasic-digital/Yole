// SPDX-License-Identifier: Apache-2.0
package digital.vasic.yole.network.platform

/**
 * Platform-abstracted file I/O for network protocol services.
 * Provides read/write operations that work across all KMP targets.
 *
 * Each platform provides its own implementation:
 * - Desktop/Android: java.io.File
 * - iOS: NSFileManager / NSData
 * - Wasm/JS: localStorage with Base64 encoding
 *
 * All operations return [Result] to encapsulate errors without propagating exceptions.
 */
interface PlatformFileIO {
    /** Read all bytes from a local file path. */
    suspend fun readFileBytes(path: String): Result<ByteArray>

    /** Write bytes to a local file path, creating parent directories as needed. */
    suspend fun writeFileBytes(path: String, bytes: ByteArray): Result<Unit>

    /** Check if a file exists at the given path. */
    suspend fun fileExists(path: String): Boolean

    /** Get the size of a file in bytes, or -1 if not found. */
    suspend fun fileSize(path: String): Long

    /** Ensure parent directories exist for the given path. */
    suspend fun ensureParentDirectories(path: String): Result<Unit>
}
