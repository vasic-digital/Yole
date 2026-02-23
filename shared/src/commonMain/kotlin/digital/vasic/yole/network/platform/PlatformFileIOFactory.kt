// SPDX-License-Identifier: Apache-2.0
package digital.vasic.yole.network.platform

/**
 * Platform factory for creating [PlatformFileIO] instances.
 *
 * Each platform target provides an `actual` implementation that returns
 * the appropriate file I/O backend:
 * - Desktop/Android: java.io.File-based implementation
 * - iOS: NSFileManager / NSData-based implementation
 * - Wasm/JS: localStorage with Base64 encoding
 */
expect object PlatformFileIOFactory {
    fun create(): PlatformFileIO
}
