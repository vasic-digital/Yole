// SPDX-License-Identifier: Apache-2.0
package digital.vasic.yole.network.platform

import kotlinx.browser.localStorage
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Wasm/JS implementation of [PlatformFileIOFactory].
 * Uses [localStorage] with Base64 encoding since Wasm has no filesystem.
 *
 * Files are stored as Base64-encoded strings keyed by their path.
 * This approach is suitable for small files typical in a text editor;
 * localStorage has a ~5MB limit per origin in most browsers.
 */
actual object PlatformFileIOFactory {
    actual fun create(): PlatformFileIO = WasmFileIO()
}

/**
 * Wasm file I/O implementation using localStorage + Base64 encoding.
 */
@OptIn(ExperimentalEncodingApi::class)
private class WasmFileIO : PlatformFileIO {

    private val keyPrefix = "yole_file_"

    private fun storageKey(path: String): String = "$keyPrefix$path"

    override suspend fun readFileBytes(path: String): Result<ByteArray> {
        return try {
            val encoded = localStorage.getItem(storageKey(path))
                ?: return Result.failure(Exception("File not found: $path"))
            val bytes = Base64.decode(encoded)
            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun writeFileBytes(path: String, bytes: ByteArray): Result<Unit> {
        return try {
            val encoded = Base64.encode(bytes)
            localStorage.setItem(storageKey(path), encoded)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fileExists(path: String): Boolean {
        return try {
            localStorage.getItem(storageKey(path)) != null
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun fileSize(path: String): Long {
        return try {
            val encoded = localStorage.getItem(storageKey(path)) ?: return -1L
            val bytes = Base64.decode(encoded)
            bytes.size.toLong()
        } catch (_: Exception) {
            -1L
        }
    }

    override suspend fun ensureParentDirectories(path: String): Result<Unit> {
        // No-op: Wasm/JS localStorage has no directory structure
        return Result.success(Unit)
    }
}
