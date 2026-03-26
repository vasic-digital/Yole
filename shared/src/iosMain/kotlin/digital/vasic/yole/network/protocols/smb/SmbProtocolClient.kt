/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iOS SMB client stub — SMB is not supported on iOS.
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.smb

import digital.vasic.yole.network.common.PlatformNotSupportedException

/**
 * iOS stub for SMB protocol client.
 *
 * **Platform limitation:** smbj library is JVM-only.
 * All methods return `Result.failure(UnsupportedOperationException)`.
 *
 * **Future plan:** libsmb2 via Kotlin/Native cinterop.
 *
 * ## Why SMB Is Not Supported on iOS
 *
 * SMB/CIFS (Server Message Block) requires a complex binary protocol implementation
 * including NTLM or Kerberos authentication, tree connect negotiation, and file
 * operation framing. On JVM platforms (Desktop/Android), Yole uses the
 * [smbj](https://github.com/hierynomus/smbj) library which is JVM-only and
 * cannot be used with Kotlin/Native.
 *
 * iOS does not expose a public SMB client API. While macOS has `NetFS.framework`
 * and the `SMBClient` framework, these are not available on iOS. The Files app
 * supports SMB mounting internally, but this functionality is not exposed to
 * third-party apps via a public SDK.
 *
 * ## Future Implementation Plan
 *
 * **Option A (Recommended): libsmb2 via Kotlin/Native cinterop**
 * 1. Create a `.def` file for [libsmb2](https://github.com/sahlberg/libsmb2) C headers
 *    in the `cinterop/` directory
 * 2. Implement SMB2/3 session setup: `smb2_connect_share()`,
 *    `smb2_set_authentication()` with NTLM credentials
 * 3. Implement file operations: `smb2_opendir()`, `smb2_readdir()`,
 *    `smb2_open()`, `smb2_read()`, `smb2_write()`, `smb2_unlink()`,
 *    `smb2_mkdir()`, `smb2_rmdir()`, `smb2_rename()`
 * 4. Support SMB3 encryption for secure communication
 * 5. Handle reconnection on network changes (iOS switches networks frequently
 *    between Wi-Fi and cellular, which can drop TCP connections)
 * Reference: [libsmb2 GitHub](https://github.com/sahlberg/libsmb2)
 *
 * **Option B: SMBClient framework for macOS Catalyst**
 * 1. Use Apple's `SMBClient` framework via Kotlin/Native Objective-C interop
 * 2. Only available for macOS Catalyst builds (iPad apps running on Mac)
 * 3. Would require conditional compilation for Catalyst vs. native iOS
 * Reference: [Apple SMBClient](https://developer.apple.com/documentation/smbclient)
 *
 * All methods return [Result.failure] with [PlatformNotSupportedException].
 */
actual class SmbProtocolClient actual constructor() {

    private val notSupported: PlatformNotSupportedException
        get() = PlatformNotSupportedException.ios(
            protocol = "SMB/CIFS",
            reason = "The smbj library is JVM-only; iOS requires libsmb2 integration via Kotlin/Native cinterop",
            alternativeApproach = "Use cloud storage protocols (Dropbox, Google Drive, OneDrive) which work via HTTPS REST APIs on all platforms"
        )

    actual val isConnected: Boolean
        get() = false

    /** @return [Result.failure] — SMB requires libsmb2 cinterop not yet implemented on iOS. */
    actual suspend fun connect(host: String, port: Int, shareName: String): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — SMB requires libsmb2 cinterop not yet implemented on iOS. */
    actual suspend fun authenticate(domain: String, username: String, password: String): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — SMB requires libsmb2 cinterop not yet implemented on iOS. */
    actual suspend fun disconnect(): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — SMB requires libsmb2 cinterop not yet implemented on iOS. */
    actual suspend fun list(path: String): Result<List<SmbEntry>> =
        Result.failure(notSupported)

    /** @return [Result.failure] — SMB requires libsmb2 cinterop not yet implemented on iOS. */
    actual suspend fun read(path: String): Result<ByteArray> =
        Result.failure(notSupported)

    /** @return [Result.failure] — SMB requires libsmb2 cinterop not yet implemented on iOS. */
    actual suspend fun write(path: String, data: ByteArray): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — SMB requires libsmb2 cinterop not yet implemented on iOS. */
    actual suspend fun delete(path: String): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — SMB requires libsmb2 cinterop not yet implemented on iOS. */
    actual suspend fun mkdir(path: String): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — SMB requires libsmb2 cinterop not yet implemented on iOS. */
    actual suspend fun rmdir(path: String): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — SMB requires libsmb2 cinterop not yet implemented on iOS. */
    actual suspend fun rename(fromPath: String, toPath: String): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — SMB requires libsmb2 cinterop not yet implemented on iOS. */
    actual suspend fun stat(path: String): Result<SmbFileInfo> =
        Result.failure(notSupported)

    /** @return [Result.failure] — SMB requires libsmb2 cinterop not yet implemented on iOS. */
    actual suspend fun getShareInfo(): Result<SmbShareInfo> =
        Result.failure(notSupported)
}
