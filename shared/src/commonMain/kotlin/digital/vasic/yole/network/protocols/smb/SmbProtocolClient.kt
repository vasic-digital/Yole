/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Platform-abstracted SMB/CIFS protocol client.
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.smb

import kotlinx.datetime.Instant

/**
 * Platform-abstracted SMB/CIFS protocol client.
 *
 * On JVM platforms (Desktop/Android), this delegates to the smbj library
 * (`com.hierynomus.smbj.SMBClient`). On iOS and Wasm, all methods return
 * [UnsupportedOperationException] failures.
 */
expect class SmbProtocolClient() {
    /** Connect to SMB server and specified share */
    suspend fun connect(host: String, port: Int, shareName: String): Result<Unit>

    /** Authenticate with domain\user and password */
    suspend fun authenticate(domain: String, username: String, password: String): Result<Unit>

    /** Disconnect and clean up */
    suspend fun disconnect(): Result<Unit>

    /** Whether currently connected */
    val isConnected: Boolean

    /** List directory contents */
    suspend fun list(path: String): Result<List<SmbEntry>>

    /** Read file contents */
    suspend fun read(path: String): Result<ByteArray>

    /** Write file contents */
    suspend fun write(path: String, data: ByteArray): Result<Unit>

    /** Delete a file */
    suspend fun delete(path: String): Result<Unit>

    /** Create directory */
    suspend fun mkdir(path: String): Result<Unit>

    /** Remove directory */
    suspend fun rmdir(path: String): Result<Unit>

    /** Rename/move file or directory */
    suspend fun rename(fromPath: String, toPath: String): Result<Unit>

    /** Get file info */
    suspend fun stat(path: String): Result<SmbFileInfo>

    /** Get share info including quota */
    suspend fun getShareInfo(): Result<SmbShareInfo>
}

/**
 * Represents a directory entry returned by an SMB directory listing.
 */
data class SmbEntry(
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
    val lastModified: Instant?,
    val lastAccessed: Instant? = null,
    val creationTime: Instant? = null
)

/**
 * Represents file information returned by an SMB QUERY_INFO operation.
 */
data class SmbFileInfo(
    val size: Long,
    val isDirectory: Boolean,
    val lastModified: Instant?,
    val lastAccessed: Instant? = null,
    val creationTime: Instant? = null,
    val isReadOnly: Boolean = false
)

/**
 * Represents SMB share information including disk quota.
 */
data class SmbShareInfo(
    val totalSpace: Long,
    val freeSpace: Long,
    val shareName: String
)
