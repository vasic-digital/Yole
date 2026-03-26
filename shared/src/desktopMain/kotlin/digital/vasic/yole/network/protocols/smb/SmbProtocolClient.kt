/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop (JVM) SMB client implementation using smbj.
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.smb

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.EnumSet

/**
 * Desktop (JVM) implementation of [SmbProtocolClient] using the smbj library.
 *
 * All blocking SMB operations are dispatched onto [Dispatchers.IO]
 * so they do not block coroutine threads.
 */
actual class SmbProtocolClient actual constructor() {
    private var smbClient: SMBClient? = null
    private var connection: Connection? = null
    private var session: Session? = null
    private var diskShare: DiskShare? = null

    actual val isConnected: Boolean
        get() = diskShare != null && connection?.isConnected == true

    actual suspend fun connect(host: String, port: Int, shareName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val client = SMBClient()
                val conn = client.connect(host, port)
                smbClient = client
                connection = conn
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    actual suspend fun authenticate(
        domain: String,
        username: String,
        password: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val conn = connection ?: return@withContext Result.failure(
                IllegalStateException("Not connected")
            )
            val authContext = AuthenticationContext(
                username,
                password.toCharArray(),
                domain
            )
            session = conn.authenticate(authContext)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Connect the authenticated session to a specific share.
     * Must be called after [authenticate].
     */
    private fun connectShare(shareName: String): Result<Unit> {
        return try {
            val sess = session ?: return Result.failure(
                IllegalStateException("Not authenticated")
            )
            val share = sess.connectShare(shareName)
            if (share is DiskShare) {
                diskShare = share
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Share '$shareName' is not a disk share"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            try { diskShare?.close() } catch (_: Exception) { // Resource cleanup — failure is non-fatal
            }
            try { session?.close() } catch (_: Exception) { // Resource cleanup — failure is non-fatal
            }
            try { connection?.close() } catch (_: Exception) { // Resource cleanup — failure is non-fatal
            }
            try { smbClient?.close() } catch (_: Exception) { // Resource cleanup — failure is non-fatal
            }
            diskShare = null
            session = null
            connection = null
            smbClient = null
            Result.success(Unit)
        } catch (e: Exception) {
            diskShare = null
            session = null
            connection = null
            smbClient = null
            Result.failure(e)
        }
    }

    private fun requireShare(): DiskShare {
        return diskShare ?: throw IllegalStateException("SMB share not connected")
    }

    /**
     * Normalize the path for SMB -- smbj uses backslash-separated paths
     * without a leading separator for the share root.
     */
    private fun normalizeSmbPath(path: String): String {
        val trimmed = path.replace("/", "\\").trimStart('\\').trimEnd('\\')
        return trimmed
    }

    actual suspend fun list(path: String): Result<List<SmbEntry>> = withContext(Dispatchers.IO) {
        try {
            val share = requireShare()
            val smbPath = normalizeSmbPath(path)
            val entries = share.list(smbPath)
            val result = entries
                .filter { it.fileName != "." && it.fileName != ".." }
                .map { info ->
                    val isDir = (info.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
                    SmbEntry(
                        name = info.fileName,
                        size = if (isDir) 0L else info.endOfFile,
                        isDirectory = isDir,
                        lastModified = fileTimeToInstant(info.lastWriteTime.toEpochMillis()),
                        lastAccessed = fileTimeToInstant(info.lastAccessTime.toEpochMillis()),
                        creationTime = fileTimeToInstant(info.creationTime.toEpochMillis())
                    )
                }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun read(path: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val share = requireShare()
            val smbPath = normalizeSmbPath(path)
            val file = share.openFile(
                smbPath,
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
                SMB2CreateDisposition.FILE_OPEN,
                null
            )
            val baos = ByteArrayOutputStream()
            try {
                val inputStream = file.inputStream
                val buffer = ByteArray(65536)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    baos.write(buffer, 0, bytesRead)
                }
            } finally {
                file.close()
            }
            Result.success(baos.toByteArray())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun write(path: String, data: ByteArray): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val share = requireShare()
                val smbPath = normalizeSmbPath(path)
                val file = share.openFile(
                    smbPath,
                    EnumSet.of(AccessMask.GENERIC_WRITE),
                    null,
                    EnumSet.of(SMB2ShareAccess.FILE_SHARE_WRITE),
                    SMB2CreateDisposition.FILE_OVERWRITE_IF,
                    null
                )
                try {
                    val outputStream = file.outputStream
                    val inputStream = ByteArrayInputStream(data)
                    val buffer = ByteArray(65536)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.flush()
                } finally {
                    file.close()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    actual suspend fun delete(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val share = requireShare()
            val smbPath = normalizeSmbPath(path)
            share.rm(smbPath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun mkdir(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val share = requireShare()
            val smbPath = normalizeSmbPath(path)
            share.mkdir(smbPath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun rmdir(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val share = requireShare()
            val smbPath = normalizeSmbPath(path)
            share.rmdir(smbPath, true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun rename(fromPath: String, toPath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val share = requireShare()
                val smbFrom = normalizeSmbPath(fromPath)
                val smbTo = normalizeSmbPath(toPath)

                // Open the source, rename via SMB2 SET_INFO, close
                val file = share.openFile(
                    smbFrom,
                    EnumSet.of(AccessMask.DELETE, AccessMask.GENERIC_WRITE),
                    null,
                    EnumSet.of(SMB2ShareAccess.FILE_SHARE_DELETE),
                    SMB2CreateDisposition.FILE_OPEN,
                    null
                )
                try {
                    file.rename(smbTo)
                } finally {
                    file.close()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    actual suspend fun stat(path: String): Result<SmbFileInfo> = withContext(Dispatchers.IO) {
        try {
            val share = requireShare()
            val smbPath = normalizeSmbPath(path)
            val info = share.getFileInformation(smbPath)
            val basicInfo = info.basicInformation
            val standardInfo = info.standardInformation

            val isDir = standardInfo.isDirectory
            val isReadOnly = (basicInfo.fileAttributes and FileAttributes.FILE_ATTRIBUTE_READONLY.value) != 0L

            Result.success(
                SmbFileInfo(
                    size = if (isDir) 0L else standardInfo.endOfFile,
                    isDirectory = isDir,
                    lastModified = fileTimeToInstant(basicInfo.lastWriteTime.toEpochMillis()),
                    lastAccessed = fileTimeToInstant(basicInfo.lastAccessTime.toEpochMillis()),
                    creationTime = fileTimeToInstant(basicInfo.creationTime.toEpochMillis()),
                    isReadOnly = isReadOnly
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun getShareInfo(): Result<SmbShareInfo> = withContext(Dispatchers.IO) {
        try {
            val share = requireShare()
            val shareInfo = share.shareInformation
            Result.success(
                SmbShareInfo(
                    totalSpace = shareInfo.totalSpace,
                    freeSpace = shareInfo.freeSpace,
                    shareName = share.smbPath.shareName
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Convert Windows FILETIME epoch milliseconds to kotlinx [Instant].
     */
    private fun fileTimeToInstant(epochMillis: Long): Instant {
        return if (epochMillis > 0) {
            Instant.fromEpochMilliseconds(epochMillis)
        } else {
            Instant.fromEpochMilliseconds(0)
        }
    }
}
