/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 *########################################################*/

package digital.vasic.yole.network.protocols.ftp

import kotlinx.datetime.Instant

/**
 * Platform-abstracted FTP protocol client.
 * Handles raw FTP control/data channel communication over TCP sockets.
 *
 * On JVM platforms (Android and Desktop), this uses [java.net.Socket] for
 * both control and data channels. On iOS and Web/Wasm, FTP is not supported
 * and all operations return [Result.failure] with [UnsupportedOperationException].
 *
 * ## FTP Protocol Notes
 * - Control channel: persistent TCP connection on port 21 (by default)
 * - Data channel: separate TCP connection negotiated via PASV (passive mode)
 * - Response codes: 1xx preliminary, 2xx success, 3xx intermediate, 4xx transient, 5xx permanent
 * - Multi-line responses: line starts with "NNN-", final line starts with "NNN "
 */
expect class FtpProtocolClient() {
    /** Connect to FTP server control channel */
    suspend fun connect(host: String, port: Int): Result<String>

    /** Authenticate with USER/PASS commands */
    suspend fun login(username: String, password: String): Result<Unit>

    /** Send QUIT and close connection */
    suspend fun disconnect(): Result<Unit>

    /** Whether currently connected */
    val isConnected: Boolean

    /** LIST directory contents */
    suspend fun list(path: String): Result<List<FtpEntry>>

    /** RETR - retrieve file contents */
    suspend fun retrieve(remotePath: String): Result<ByteArray>

    /** STOR - store file contents */
    suspend fun store(remotePath: String, data: ByteArray): Result<Unit>

    /** DELE - delete a file */
    suspend fun delete(path: String): Result<Unit>

    /** MKD - make directory */
    suspend fun mkdir(path: String): Result<Unit>

    /** RMD - remove directory */
    suspend fun rmdir(path: String): Result<Unit>

    /** RNFR + RNTO - rename */
    suspend fun rename(fromPath: String, toPath: String): Result<Unit>

    /** PWD - print working directory */
    suspend fun pwd(): Result<String>

    /** CWD - change working directory */
    suspend fun cwd(path: String): Result<Unit>

    /** SIZE - get file size */
    suspend fun size(path: String): Result<Long>

    /** MDTM - get file modification time (returns YYYYMMDDHHmmss) */
    suspend fun mdtm(path: String): Result<String>

    /** TYPE - set transfer type (A=ASCII, I=Binary) */
    suspend fun setTransferType(binary: Boolean): Result<Unit>

    /** PASV - enter passive mode, returns host and port for data connection */
    suspend fun pasv(): Result<Pair<String, Int>>
}

/**
 * Represents a single entry from an FTP directory listing.
 */
data class FtpEntry(
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
    val lastModified: Instant?,
    val permissions: String? = null
)
