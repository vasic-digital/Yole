/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Android file storage using ContentResolver (SAF).
 *
 *########################################################*/
package digital.vasic.yole.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

actual class FileHandle actual constructor(uri: String) {
    actual val uri: String = uri
    private val parsedUri: Uri? = try {
        Uri.parse(uri)
    } catch (_: Exception) {
        null
    }

    internal fun getAndroidUri(): Uri? = parsedUri
}

actual fun FileHandle.readBytes(): ByteArray? {
    val androidUri = getAndroidUri() ?: return null
    val context = AppContextHolder.context ?: return null
    return try {
        context.contentResolver.openInputStream(androidUri)?.use { it.readBytes() }
    } catch (_: Exception) {
        null
    }
}

actual fun FileHandle.writeBytes(data: ByteArray): Boolean {
    val androidUri = getAndroidUri() ?: return false
    val context = AppContextHolder.context ?: return false
    return try {
        context.contentResolver.openOutputStream(androidUri, "wt")?.use { out ->
            out.write(data)
        }
        true
    } catch (_: Exception) {
        false
    }
}

actual fun FileHandle.exists(): Boolean {
    val androidUri = getAndroidUri() ?: return false
    val context = AppContextHolder.context ?: return false
    return try {
        context.contentResolver.query(androidUri, null, null, null, null)?.use {
            it.count > 0
        } ?: false
    } catch (_: Exception) {
        false
    }
}

actual fun FileHandle.displayName(): String? {
    val androidUri = getAndroidUri() ?: return null
    val context = AppContextHolder.context ?: return null
    return try {
        context.contentResolver.query(androidUri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Simple Application context holder for SAF operations.
 * Must be initialized in Application.onCreate().
 */
object AppContextHolder {
    @Volatile
    var context: Context? = null
}
