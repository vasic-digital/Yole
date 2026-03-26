/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iOS SSH/SFTP client stub — SFTP is not supported on iOS.
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.sftp

import digital.vasic.yole.network.common.PlatformNotSupportedException

/**
 * iOS stub for SFTP/SSH protocol client.
 *
 * **Platform limitation:** sshj library is JVM-only.
 * All methods return `Result.failure(UnsupportedOperationException)`.
 *
 * **Future plan:** libssh2 via Kotlin/Native cinterop.
 *
 * ## Why SFTP Is Not Supported on iOS
 *
 * SFTP operates over the SSH protocol, which requires a full SSH implementation
 * including key exchange, encryption negotiation, and channel multiplexing. On
 * JVM platforms (Desktop/Android), Yole uses the [sshj](https://github.com/hierynomus/sshj)
 * library which is JVM-only and cannot be used with Kotlin/Native.
 *
 * iOS does not provide a built-in SSH client API. Network.framework (`NWConnection`)
 * supports TLS but not the SSH protocol, so implementing SSH from scratch on raw
 * sockets would be extremely complex.
 *
 * ## Future Implementation Plan
 *
 * **Option A (Recommended): libssh2 via Kotlin/Native cinterop**
 * 1. Create a `.def` file for [libssh2](https://www.libssh2.org/) C headers
 *    in the `cinterop/` directory
 * 2. Implement SSH session management: `libssh2_session_init()`, `libssh2_session_handshake()`,
 *    `libssh2_userauth_password()` / `libssh2_userauth_publickey_fromfile()`
 * 3. Implement SFTP subsystem: `libssh2_sftp_init()`, `libssh2_sftp_open()`,
 *    `libssh2_sftp_read()`, `libssh2_sftp_write()`, `libssh2_sftp_stat()`,
 *    `libssh2_sftp_readdir()`
 * 4. Support both password and public key authentication methods
 * 5. Handle host key verification against a local known_hosts store
 * 6. Wrap all C API calls with proper error handling and resource cleanup
 *    using `try`/`finally` to prevent resource leaks
 * Reference: [libssh2 API](https://www.libssh2.org/docs.html)
 *
 * **Option B: NMSSH (Objective-C wrapper around libssh2)**
 * 1. Use [NMSSH](https://github.com/NMSSH/NMSSH) via Kotlin/Native Objective-C interop
 * 2. NMSSH provides a higher-level API than raw libssh2, reducing integration effort
 * 3. Requires adding the NMSSH CocoaPod dependency to the iOS target
 * Reference: [NMSSH GitHub](https://github.com/NMSSH/NMSSH)
 *
 * All methods return [Result.failure] with [PlatformNotSupportedException].
 */
actual class SshClient actual constructor() {

    private val notSupported: PlatformNotSupportedException
        get() = PlatformNotSupportedException.ios(
            protocol = "SFTP/SSH",
            reason = "The sshj library is JVM-only; iOS requires libssh2 integration via Kotlin/Native cinterop",
            alternativeApproach = "Use cloud storage protocols (Dropbox, Google Drive, OneDrive) which work via HTTPS REST APIs on all platforms"
        )

    actual val isConnected: Boolean
        get() = false

    /** @return [Result.failure] — SSH requires libssh2 cinterop not yet implemented on iOS. */
    actual suspend fun connect(host: String, port: Int): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — SSH requires libssh2 cinterop not yet implemented on iOS. */
    actual suspend fun authenticatePassword(username: String, password: String): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — SSH requires libssh2 cinterop not yet implemented on iOS. */
    actual suspend fun authenticateKey(
        username: String,
        privateKeyPath: String,
        passphrase: String?
    ): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — SSH requires libssh2 cinterop not yet implemented on iOS. */
    actual suspend fun openSftpChannel(): Result<SftpChannel> =
        Result.failure(notSupported)

    /** @return [Result.failure] — SSH requires libssh2 cinterop not yet implemented on iOS. */
    actual suspend fun disconnect(): Result<Unit> =
        Result.failure(notSupported)
}

/**
 * iOS implementation of [SftpChannel].
 *
 * All SFTP file operations are unsupported on iOS because the SSH transport
 * layer (provided by sshj on JVM) is not available in Kotlin/Native.
 * See [SshClient] documentation for the full implementation plan.
 *
 * All methods return [Result.failure] with [PlatformNotSupportedException].
 */
actual class SftpChannel {

    private val notSupported: PlatformNotSupportedException
        get() = PlatformNotSupportedException.ios(
            protocol = "SFTP",
            reason = "SFTP channel requires SSH transport (libssh2) not yet available on iOS"
        )

    /** @return [Result.failure] — SFTP channel requires SSH transport not yet available on iOS. */
    actual suspend fun list(path: String): Result<List<SftpEntry>> =
        Result.failure(notSupported)

    /** @return [Result.failure] — SFTP channel requires SSH transport not yet available on iOS. */
    actual suspend fun get(remotePath: String): Result<ByteArray> =
        Result.failure(notSupported)

    /** @return [Result.failure] — SFTP channel requires SSH transport not yet available on iOS. */
    actual suspend fun put(remotePath: String, data: ByteArray): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — SFTP channel requires SSH transport not yet available on iOS. */
    actual suspend fun delete(path: String): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — SFTP channel requires SSH transport not yet available on iOS. */
    actual suspend fun mkdir(path: String): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — SFTP channel requires SSH transport not yet available on iOS. */
    actual suspend fun rmdir(path: String): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — SFTP channel requires SSH transport not yet available on iOS. */
    actual suspend fun rename(fromPath: String, toPath: String): Result<Unit> =
        Result.failure(notSupported)

    /** @return [Result.failure] — SFTP channel requires SSH transport not yet available on iOS. */
    actual suspend fun stat(path: String): Result<SftpFileAttributes> =
        Result.failure(notSupported)

    /** @return [Result.failure] — SFTP channel requires SSH transport not yet available on iOS. */
    actual suspend fun close(): Result<Unit> =
        Result.failure(notSupported)
}
