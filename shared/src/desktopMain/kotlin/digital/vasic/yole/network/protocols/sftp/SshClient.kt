/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop (JVM) SSH client implementation using sshj.
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.sftp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.OpenMode
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.EnumSet

/**
 * Desktop (JVM) implementation of [SshClient] using the sshj library.
 *
 * All blocking SSH/SFTP operations are dispatched onto [Dispatchers.IO]
 * so they do not block coroutine threads.
 */
actual class SshClient actual constructor() {
    private var client: SSHClient? = null

    actual val isConnected: Boolean
        get() = client?.isConnected == true

    actual suspend fun connect(host: String, port: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ssh = SSHClient()
            ssh.addHostKeyVerifier(PromiscuousVerifier())
            ssh.connect(host, port)
            client = ssh
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun authenticatePassword(username: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val ssh = client ?: return@withContext Result.failure(
                    IllegalStateException("Not connected")
                )
                ssh.authPassword(username, password)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    actual suspend fun authenticateKey(
        username: String,
        privateKeyPath: String,
        passphrase: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ssh = client ?: return@withContext Result.failure(
                IllegalStateException("Not connected")
            )
            val keyProvider: KeyProvider = if (passphrase != null) {
                ssh.loadKeys(privateKeyPath, passphrase)
            } else {
                ssh.loadKeys(privateKeyPath)
            }
            ssh.authPublickey(username, keyProvider)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun openSftpChannel(): Result<SftpChannel> = withContext(Dispatchers.IO) {
        try {
            val ssh = client ?: return@withContext Result.failure(
                IllegalStateException("Not connected")
            )
            val sftpClient = ssh.newSFTPClient()
            Result.success(SftpChannel(sftpClient))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client?.disconnect()
            client = null
            Result.success(Unit)
        } catch (e: Exception) {
            client = null
            Result.failure(e)
        }
    }
}

/**
 * Desktop (JVM) implementation of [SftpChannel] wrapping sshj's [SFTPClient].
 */
actual class SftpChannel(private val sftpClient: SFTPClient) {

    actual suspend fun list(path: String): Result<List<SftpEntry>> = withContext(Dispatchers.IO) {
        try {
            val entries = sftpClient.ls(path).map { remoteResource ->
                val attrs = remoteResource.attributes
                SftpEntry(
                    name = remoteResource.name,
                    size = attrs.size,
                    isDirectory = attrs.type == FileMode.Type.DIRECTORY,
                    lastModified = Instant.fromEpochSeconds(attrs.mtime),
                    permissions = attrs.permissions?.joinToString("") ?: null,
                    owner = attrs.uid.toString(),
                    group = attrs.gid.toString()
                )
            }
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun get(remotePath: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val remoteFile = sftpClient.open(remotePath, EnumSet.of(OpenMode.READ))
            val baos = ByteArrayOutputStream()
            try {
                val inputStream = remoteFile.RemoteFileInputStream()
                val buffer = ByteArray(65536)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    baos.write(buffer, 0, bytesRead)
                }
            } finally {
                remoteFile.close()
            }
            Result.success(baos.toByteArray())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun put(remotePath: String, data: ByteArray): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val remoteFile = sftpClient.open(
                    remotePath,
                    EnumSet.of(OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC)
                )
                try {
                    val outputStream = remoteFile.RemoteFileOutputStream()
                    val inputStream = ByteArrayInputStream(data)
                    val buffer = ByteArray(65536)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.flush()
                } finally {
                    remoteFile.close()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    actual suspend fun delete(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sftpClient.rm(path)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun mkdir(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sftpClient.mkdir(path)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun rmdir(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sftpClient.rmdir(path)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun rename(fromPath: String, toPath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                sftpClient.rename(fromPath, toPath)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    actual suspend fun stat(path: String): Result<SftpFileAttributes> = withContext(Dispatchers.IO) {
        try {
            val attrs = sftpClient.stat(path)
            Result.success(
                SftpFileAttributes(
                    size = attrs.size,
                    isDirectory = attrs.type == FileMode.Type.DIRECTORY,
                    lastModified = Instant.fromEpochSeconds(attrs.mtime),
                    lastAccessed = Instant.fromEpochSeconds(attrs.atime),
                    permissions = attrs.permissions?.joinToString("") ?: null,
                    owner = attrs.uid.toString(),
                    group = attrs.gid.toString()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun close(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sftpClient.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
