/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Wasm SSH client stub -- SFTP is not supported on web.
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.sftp

/**
 * Web (Wasm) implementation of [SshClient].
 *
 * SFTP via SSH is not supported on the web platform because the sshj library
 * is JVM-only and raw TCP sockets are unavailable in a browser environment.
 * All methods return [UnsupportedOperationException] failures.
 */
actual class SshClient actual constructor() {

    actual val isConnected: Boolean
        get() = false

    actual suspend fun connect(host: String, port: Int): Result<Unit> =
        Result.failure(UnsupportedOperationException("SFTP not supported on web"))

    actual suspend fun authenticatePassword(username: String, password: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("SFTP not supported on web"))

    actual suspend fun authenticateKey(
        username: String,
        privateKeyPath: String,
        passphrase: String?
    ): Result<Unit> =
        Result.failure(UnsupportedOperationException("SFTP not supported on web"))

    actual suspend fun openSftpChannel(): Result<SftpChannel> =
        Result.failure(UnsupportedOperationException("SFTP not supported on web"))

    actual suspend fun disconnect(): Result<Unit> =
        Result.failure(UnsupportedOperationException("SFTP not supported on web"))
}

/**
 * Web (Wasm) implementation of [SftpChannel] -- all operations unsupported.
 */
actual class SftpChannel {

    actual suspend fun list(path: String): Result<List<SftpEntry>> =
        Result.failure(UnsupportedOperationException("SFTP not supported on web"))

    actual suspend fun get(remotePath: String): Result<ByteArray> =
        Result.failure(UnsupportedOperationException("SFTP not supported on web"))

    actual suspend fun put(remotePath: String, data: ByteArray): Result<Unit> =
        Result.failure(UnsupportedOperationException("SFTP not supported on web"))

    actual suspend fun delete(path: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("SFTP not supported on web"))

    actual suspend fun mkdir(path: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("SFTP not supported on web"))

    actual suspend fun rmdir(path: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("SFTP not supported on web"))

    actual suspend fun rename(fromPath: String, toPath: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("SFTP not supported on web"))

    actual suspend fun stat(path: String): Result<SftpFileAttributes> =
        Result.failure(UnsupportedOperationException("SFTP not supported on web"))

    actual suspend fun close(): Result<Unit> =
        Result.failure(UnsupportedOperationException("SFTP not supported on web"))
}
