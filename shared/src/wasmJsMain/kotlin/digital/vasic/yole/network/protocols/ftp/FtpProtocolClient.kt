/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 *########################################################*/

package digital.vasic.yole.network.protocols.ftp

/**
 * Wasm stub for FTP protocol client.
 *
 * **Platform limitation:** Browser security model prevents raw TCP socket access.
 * All methods return `Result.failure(UnsupportedOperationException)`.
 *
 * **Future plan:** Server-side FTP-over-WebSocket proxy.
 *
 * This is an intentional platform limitation, not a stub awaiting implementation.
 * Browser security models fundamentally prevent raw TCP socket access.
 *
 * TODO: If a server-side proxy becomes available, implement FTP-over-WebSocket by
 *       routing FTP commands through a WebSocket connection to a backend proxy that
 *       translates them into actual FTP protocol traffic. This would require:
 *       1. A companion server component (e.g., Node.js FTP proxy with WebSocket endpoint)
 *       2. WebSocket client in this class to communicate with the proxy
 *       3. JSON-based command/response protocol between browser and proxy
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
