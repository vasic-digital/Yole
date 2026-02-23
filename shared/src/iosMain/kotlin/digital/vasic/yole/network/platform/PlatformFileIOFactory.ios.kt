// SPDX-License-Identifier: Apache-2.0
package digital.vasic.yole.network.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.lastPathComponent
import platform.Foundation.stringByDeletingLastPathComponent
import platform.Foundation.writeToFile
import platform.posix.memcpy

/**
 * iOS implementation of [PlatformFileIOFactory].
 * Uses [NSFileManager] and [NSData] for file system operations.
 */
actual object PlatformFileIOFactory {
    actual fun create(): PlatformFileIO = IosFileIO()
}

/**
 * iOS file I/O implementation using platform.Foundation APIs.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class IosFileIO : PlatformFileIO {

    private val fileManager = NSFileManager.defaultManager

    override suspend fun readFileBytes(path: String): Result<ByteArray> {
        return try {
            val data = NSData.dataWithContentsOfFile(path)
                ?: return Result.failure(Exception("File not found: $path"))
            val bytes = data.toByteArray()
            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun writeFileBytes(path: String, bytes: ByteArray): Result<Unit> {
        return try {
            // Ensure parent directories exist
            val parentPath = (path as NSString).stringByDeletingLastPathComponent
            if (!fileManager.fileExistsAtPath(parentPath)) {
                fileManager.createDirectoryAtPath(
                    parentPath,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null
                )
            }

            val data = bytes.toNSData()
            val success = data.writeToFile(path, atomically = true)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to write file: $path"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fileExists(path: String): Boolean {
        return try {
            fileManager.fileExistsAtPath(path)
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun fileSize(path: String): Long {
        return try {
            if (!fileManager.fileExistsAtPath(path)) return -1L
            val attributes = fileManager.attributesOfItemAtPath(path, error = null)
                ?: return -1L
            (attributes["NSFileSize"] as? Number)?.toLong() ?: -1L
        } catch (_: Exception) {
            -1L
        }
    }

    override suspend fun ensureParentDirectories(path: String): Result<Unit> {
        return try {
            val parentPath = (path as NSString).stringByDeletingLastPathComponent
            if (!fileManager.fileExistsAtPath(parentPath)) {
                fileManager.createDirectoryAtPath(
                    parentPath,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Convert [NSData] to a Kotlin [ByteArray].
     */
    private fun NSData.toByteArray(): ByteArray {
        val length = this.length.toInt()
        if (length == 0) return byteArrayOf()
        val bytes = ByteArray(length)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
        return bytes
    }

    /**
     * Convert a Kotlin [ByteArray] to [NSData].
     */
    private fun ByteArray.toNSData(): NSData = memScoped {
        if (this@toNSData.isEmpty()) {
            return NSData()
        }
        NSData.create(
            bytes = allocArrayOf(this@toNSData),
            length = this@toNSData.size.toULong()
        )
    }
}
