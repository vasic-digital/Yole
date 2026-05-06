/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iOS file storage stub.
 * Real implementation pending platform integration.
 *
 *########################################################*/
package digital.vasic.yole.util

actual class FileHandle(uri: String) {
    actual val uri: String = uri
}

actual fun FileHandle.readBytes(): ByteArray? = null
actual fun FileHandle.writeBytes(data: ByteArray): Boolean = false
actual fun FileHandle.exists(): Boolean = false
actual fun FileHandle.displayName(): String? = null
