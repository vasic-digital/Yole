/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iOS SSH client stub -- SFTP is not supported on iOS.
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.sftp

/**
 * iOS implementation of [SshClient].
 *
 * SFTP via SSH is not currently supported on iOS because the sshj library
 * is JVM-only. All methods return [UnsupportedOperationException] failures.
 */
actual class SshClient actual constructor() {

    actual val isConnected: Boolean
        get() = false

    actual suspend fun connect(host: String, port: Int): Result<Unit> =
        Result.failure(UnsupportedOperationException("SFTP not supported on iOS"))

    actual suspend fun authenticatePassword(username: String, password: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("SFTP not supported on iOS"))

    actual suspend fun authenticateKey(
        username: String,
        privateKeyPath: String,
        passphrase: String?
    ): Result<Unit> =
        Result.failure(UnsupportedOperationException("SFTP not supported on iOS"))

    actual suspend fun openSftpChannel(): Result<SftpChannel> =
        Result.failure(UnsupportedOperationException("SFTP not supported on iOS"))

    actual suspend fun disconnect(): Result<Unit> =
        Result.failure(UnsupportedOperationException("SFTP not supported on iOS"))
}

/**
 * iOS implementation of [SftpChannel] -- all operations unsupported.
 */
actual class SftpChannel {

    actual suspend fun list(path: String): Result<List<SftpEntry>> =
        Result.failure(UnsupportedOperationException("SFTP not supported on iOS"))

    actual suspend fun get(remotePath: String): Result<ByteArray> =
        Result.failure(UnsupportedOperationException("SFTP not supported on iOS"))

    actual suspend fun put(remotePath: String, data: ByteArray): Result<Unit> =
        Result.failure(UnsupportedOperationException("SFTP not supported on iOS"))

    actual suspend fun delete(path: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("SFTP not supported on iOS"))

    actual suspend fun mkdir(path: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("SFTP not supported on iOS"))

    actual suspend fun rmdir(path: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("SFTP not supported on iOS"))

    actual suspend fun rename(fromPath: String, toPath: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("SFTP not supported on iOS"))

    actual suspend fun stat(path: String): Result<SftpFileAttributes> =
        Result.failure(UnsupportedOperationException("SFTP not supported on iOS"))

    actual suspend fun close(): Result<Unit> =
        Result.failure(UnsupportedOperationException("SFTP not supported on iOS"))
}
