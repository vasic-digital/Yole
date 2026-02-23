/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Platform-abstracted SSH client for SFTP operations.
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.sftp

import kotlinx.datetime.Instant

/**
 * Platform-abstracted SSH client for SFTP operations.
 *
 * On JVM platforms (Desktop/Android), this delegates to the sshj library
 * (`net.schmizz.sshj.SSHClient`). On iOS and Wasm, all methods return
 * [UnsupportedOperationException] failures.
 */
expect class SshClient() {
    /** Connect to SSH server */
    suspend fun connect(host: String, port: Int): Result<Unit>

    /** Authenticate with password */
    suspend fun authenticatePassword(username: String, password: String): Result<Unit>

    /** Authenticate with private key file */
    suspend fun authenticateKey(username: String, privateKeyPath: String, passphrase: String?): Result<Unit>

    /** Open SFTP channel */
    suspend fun openSftpChannel(): Result<SftpChannel>

    /** Disconnect and clean up */
    suspend fun disconnect(): Result<Unit>

    /** Whether currently connected */
    val isConnected: Boolean
}

/**
 * SFTP channel for file operations over SSH.
 *
 * Wraps the platform-specific SFTP subsystem client and provides
 * suspending wrappers around blocking I/O calls.
 */
expect class SftpChannel {
    /** List directory contents */
    suspend fun list(path: String): Result<List<SftpEntry>>

    /** Download file contents */
    suspend fun get(remotePath: String): Result<ByteArray>

    /** Upload file contents */
    suspend fun put(remotePath: String, data: ByteArray): Result<Unit>

    /** Delete a file */
    suspend fun delete(path: String): Result<Unit>

    /** Create directory */
    suspend fun mkdir(path: String): Result<Unit>

    /** Remove directory */
    suspend fun rmdir(path: String): Result<Unit>

    /** Rename/move file */
    suspend fun rename(fromPath: String, toPath: String): Result<Unit>

    /** Get file attributes */
    suspend fun stat(path: String): Result<SftpFileAttributes>

    /** Close this SFTP channel */
    suspend fun close(): Result<Unit>
}

/**
 * Represents a directory entry returned by an SFTP READDIR operation.
 */
data class SftpEntry(
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
    val lastModified: Instant?,
    val permissions: String? = null,
    val owner: String? = null,
    val group: String? = null
)

/**
 * Represents file attributes returned by an SFTP STAT operation.
 */
data class SftpFileAttributes(
    val size: Long,
    val isDirectory: Boolean,
    val lastModified: Instant?,
    val lastAccessed: Instant? = null,
    val permissions: String? = null,
    val owner: String? = null,
    val group: String? = null
)
