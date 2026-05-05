/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop (JVM) file storage implementation using java.io.File.
 *
 *########################################################*/
package digital.vasic.yole.util

import java.io.File

actual class FileHandle actual constructor(uri: String) {
    actual val uri: String = uri
    private val file: File = File(uri)

    internal fun getDesktopFile(): File = file
}

actual fun FileHandle.readBytes(): ByteArray? {
    return try {
        getDesktopFile().readBytes()
    } catch (_: Exception) {
        null
    }
}

actual fun FileHandle.writeBytes(data: ByteArray): Boolean {
    return try {
        val f = getDesktopFile()
        f.parentFile?.mkdirs()
        f.writeBytes(data)
        true
    } catch (_: Exception) {
        false
    }
}

actual fun FileHandle.exists(): Boolean {
    return getDesktopFile().exists()
}

actual fun FileHandle.displayName(): String? {
    return getDesktopFile().name
}
