// SPDX-License-Identifier: Apache-2.0
package digital.vasic.yole.network.platform

import java.io.File

/**
 * Android implementation of [PlatformFileIOFactory].
 * Uses [java.io.File] for all file system operations (same as Desktop JVM).
 */
actual object PlatformFileIOFactory {
    actual fun create(): PlatformFileIO = AndroidFileIO()
}

/**
 * Android file I/O implementation using java.io.File.
 */
private class AndroidFileIO : PlatformFileIO {

    override suspend fun readFileBytes(path: String): Result<ByteArray> {
        return try {
            val file = File(path)
            if (!file.exists()) {
                Result.failure(Exception("File not found: $path"))
            } else {
                Result.success(file.readBytes())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun writeFileBytes(path: String, bytes: ByteArray): Result<Unit> {
        return try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fileExists(path: String): Boolean {
        return try {
            File(path).exists()
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun fileSize(path: String): Long {
        return try {
            val file = File(path)
            if (file.exists()) file.length() else -1L
        } catch (_: Exception) {
            -1L
        }
    }

    override suspend fun ensureParentDirectories(path: String): Result<Unit> {
        return try {
            val parentDir = File(path).parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
