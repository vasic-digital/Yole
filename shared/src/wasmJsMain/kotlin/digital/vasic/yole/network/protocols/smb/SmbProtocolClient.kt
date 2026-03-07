/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Wasm SMB client stub -- SMB is not supported on web.
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.smb

/**
 * Web (Wasm) implementation of [SmbProtocolClient].
 *
 * SMB/CIFS is not supported on the web platform because the smbj library
 * is JVM-only and raw TCP sockets are unavailable in a browser environment.
 * All methods return [UnsupportedOperationException] failures.
 *
 * This is an intentional platform limitation, not a stub awaiting implementation.
 * Browser security models fundamentally prevent raw TCP socket access required for SMB.
 *
 * TODO: If a server-side proxy becomes available, implement SMB-over-WebSocket by
 *       routing SMB commands through a WebSocket connection to a backend proxy.
 *       See the FTP stub for the same architectural pattern.
 */
actual class SmbProtocolClient actual constructor() {

    actual val isConnected: Boolean
        get() = false

    actual suspend fun connect(host: String, port: Int, shareName: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("SMB not supported on web"))

    actual suspend fun authenticate(domain: String, username: String, password: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("SMB not supported on web"))

    actual suspend fun disconnect(): Result<Unit> =
        Result.failure(UnsupportedOperationException("SMB not supported on web"))

    actual suspend fun list(path: String): Result<List<SmbEntry>> =
        Result.failure(UnsupportedOperationException("SMB not supported on web"))

    actual suspend fun read(path: String): Result<ByteArray> =
        Result.failure(UnsupportedOperationException("SMB not supported on web"))

    actual suspend fun write(path: String, data: ByteArray): Result<Unit> =
        Result.failure(UnsupportedOperationException("SMB not supported on web"))

    actual suspend fun delete(path: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("SMB not supported on web"))

    actual suspend fun mkdir(path: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("SMB not supported on web"))

    actual suspend fun rmdir(path: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("SMB not supported on web"))

    actual suspend fun rename(fromPath: String, toPath: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("SMB not supported on web"))

    actual suspend fun stat(path: String): Result<SmbFileInfo> =
        Result.failure(UnsupportedOperationException("SMB not supported on web"))

    actual suspend fun getShareInfo(): Result<SmbShareInfo> =
        Result.failure(UnsupportedOperationException("SMB not supported on web"))
}
