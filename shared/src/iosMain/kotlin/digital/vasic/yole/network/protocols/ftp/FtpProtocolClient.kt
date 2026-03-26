/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iOS FTP protocol client stub — FTP is not supported on iOS.
 *
 *########################################################*/

package digital.vasic.yole.network.protocols.ftp

import digital.vasic.yole.network.common.PlatformNotSupportedException

/**
 * iOS stub for FTP protocol client.
 *
 * **Platform limitation:** iOS does not provide raw TCP socket access for FTP.
 * All methods return `Result.failure(UnsupportedOperationException)`.
 *
 * **Future plan:** NWConnection (Network.framework) or libcurl cinterop.
 *
 * ## Why FTP Is Not Supported on iOS
 *
 * FTP requires raw TCP socket access for both control (port 21) and data channels
 * (negotiated via PASV). While iOS provides TCP socket APIs through Network.framework
 * (`NWConnection`), implementing the full FTP protocol on top of raw sockets is a
 * non-trivial effort involving:
 * - Multi-line response parsing (RFC 959 Section 4.2)
 * - Passive mode data channel negotiation
 * - Transfer encoding (ASCII vs binary)
 * - TLS upgrade for FTPS (AUTH TLS command)
 *
 * Additionally, the JVM-based FTP implementation in the desktop/Android targets uses
 * `java.net.Socket` directly, which has no equivalent in Kotlin/Native.
 *
 * ## Future Implementation Plan
 *
 * **Option A (Recommended): NWConnection via Network.framework**
 * 1. Create `NWConnection` with TCP protocol and target host/port
 * 2. Implement FTP command/response framing over the TCP channel
 * 3. Handle passive mode data connections for file transfers
 * 4. Support TLS/FTPS via `NWProtocolTLS` configuration
 * 5. Integrate with iOS background transfer service for large files
 * Reference: [Apple Network.framework](https://developer.apple.com/documentation/network)
 *
 * **Option B: libcurl via Kotlin/Native cinterop**
 * 1. Create a `.def` file for libcurl C headers in the `cinterop/` directory
 * 2. Use `curl_easy_perform()` with `CURLPROTO_FTP` for transfer operations
 * 3. Map curl error codes to [Result.failure] with descriptive messages
 * Reference: [libcurl FTP](https://curl.se/libcurl/c/ftp.html)
 *
 * All methods return [Result.failure] with [PlatformNotSupportedException].
 */
actual class FtpProtocolClient actual constructor() {

    private val notSupported: PlatformNotSupportedException
        get() = PlatformNotSupportedException.ios(
            protocol = "FTP",
            reason = "Raw TCP socket FTP requires NWConnection (Network.framework) integration via Kotlin/Native cinterop",
            alternativeApproach = "Use cloud storage protocols (Dropbox, Google Drive, OneDrive) which work via HTTPS REST APIs on all platforms"
        )

    actual val isConnected: Boolean
        get() = false

    /** @return [Result.failure] — FTP requires raw TCP sockets not yet implemented on iOS. */
    actual suspend fun connect(host: String, port: Int): Result<String> =
        Result.failure(notSupported)

    /** @return [Result.failure] — FTP requires raw TCP sockets not yet implemented on iOS. */
    actual suspend fun login(username: String, password: String): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — FTP requires raw TCP sockets not yet implemented on iOS. */
    actual suspend fun disconnect(): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — FTP requires raw TCP sockets not yet implemented on iOS. */
    actual suspend fun list(path: String): Result<List<FtpEntry>> =
        Result.failure(notSupported)

    /** @return [Result.failure] — FTP requires raw TCP sockets not yet implemented on iOS. */
    actual suspend fun retrieve(remotePath: String): Result<ByteArray> =
        Result.failure(notSupported)

    /** @return [Result.failure] — FTP requires raw TCP sockets not yet implemented on iOS. */
    actual suspend fun store(remotePath: String, data: ByteArray): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — FTP requires raw TCP sockets not yet implemented on iOS. */
    actual suspend fun delete(path: String): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — FTP requires raw TCP sockets not yet implemented on iOS. */
    actual suspend fun mkdir(path: String): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — FTP requires raw TCP sockets not yet implemented on iOS. */
    actual suspend fun rmdir(path: String): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — FTP requires raw TCP sockets not yet implemented on iOS. */
    actual suspend fun rename(fromPath: String, toPath: String): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — FTP requires raw TCP sockets not yet implemented on iOS. */
    actual suspend fun pwd(): Result<String> =
        Result.failure(notSupported)

    /** @return [Result.failure] — FTP requires raw TCP sockets not yet implemented on iOS. */
    actual suspend fun cwd(path: String): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — FTP requires raw TCP sockets not yet implemented on iOS. */
    actual suspend fun size(path: String): Result<Long> =
        Result.failure(notSupported)

    /** @return [Result.failure] — FTP requires raw TCP sockets not yet implemented on iOS. */
    actual suspend fun mdtm(path: String): Result<String> =
        Result.failure(notSupported)

    /** @return [Result.failure] — FTP requires raw TCP sockets not yet implemented on iOS. */
    actual suspend fun setTransferType(binary: Boolean): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — FTP requires raw TCP sockets not yet implemented on iOS. */
    actual suspend fun pasv(): Result<Pair<String, Int>> =
        Result.failure(notSupported)
}
