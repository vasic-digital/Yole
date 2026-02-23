/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 *########################################################*/

package digital.vasic.yole.network.protocols.ftp

/**
 * Web/Wasm actual implementation of [FtpProtocolClient].
 *
 * FTP over raw TCP sockets is not supported in browser/Wasm environments.
 * Browsers do not expose raw TCP socket APIs, making FTP protocol communication
 * impossible from client-side web applications.
 * All operations return [Result.failure] with [UnsupportedOperationException].
 */
actual class FtpProtocolClient actual constructor() {

    actual val isConnected: Boolean
        get() = false

    actual suspend fun connect(host: String, port: Int): Result<String> =
        Result.failure(UnsupportedOperationException("FTP not supported on web"))

    actual suspend fun login(username: String, password: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("FTP not supported on web"))

    actual suspend fun disconnect(): Result<Unit> =
        Result.failure(UnsupportedOperationException("FTP not supported on web"))

    actual suspend fun list(path: String): Result<List<FtpEntry>> =
        Result.failure(UnsupportedOperationException("FTP not supported on web"))

    actual suspend fun retrieve(remotePath: String): Result<ByteArray> =
        Result.failure(UnsupportedOperationException("FTP not supported on web"))

    actual suspend fun store(remotePath: String, data: ByteArray): Result<Unit> =
        Result.failure(UnsupportedOperationException("FTP not supported on web"))

    actual suspend fun delete(path: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("FTP not supported on web"))

    actual suspend fun mkdir(path: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("FTP not supported on web"))

    actual suspend fun rmdir(path: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("FTP not supported on web"))

    actual suspend fun rename(fromPath: String, toPath: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("FTP not supported on web"))

    actual suspend fun pwd(): Result<String> =
        Result.failure(UnsupportedOperationException("FTP not supported on web"))

    actual suspend fun cwd(path: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("FTP not supported on web"))

    actual suspend fun size(path: String): Result<Long> =
        Result.failure(UnsupportedOperationException("FTP not supported on web"))

    actual suspend fun mdtm(path: String): Result<String> =
        Result.failure(UnsupportedOperationException("FTP not supported on web"))

    actual suspend fun setTransferType(binary: Boolean): Result<Unit> =
        Result.failure(UnsupportedOperationException("FTP not supported on web"))

    actual suspend fun pasv(): Result<Pair<String, Int>> =
        Result.failure(UnsupportedOperationException("FTP not supported on web"))
}
