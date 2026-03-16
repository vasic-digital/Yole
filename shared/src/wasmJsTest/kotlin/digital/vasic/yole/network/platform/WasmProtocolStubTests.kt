/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Wasm-specific tests for protocol client stubs.
 * Validates that FTP, SFTP, and SMB protocol clients
 * correctly throw UnsupportedOperationException on the
 * web platform where raw TCP sockets are unavailable.
 *
 *########################################################*/
package digital.vasic.yole.network.platform

import digital.vasic.yole.network.protocols.ftp.FtpProtocolClient
import digital.vasic.yole.network.protocols.sftp.SshClient
import digital.vasic.yole.network.protocols.sftp.SftpChannel
import digital.vasic.yole.network.protocols.smb.SmbProtocolClient
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Tests for Wasm protocol client stubs.
 *
 * All three raw-TCP protocols (FTP, SFTP/SSH, SMB) are unsupported on the
 * web platform because browsers do not expose raw TCP socket APIs. This
 * test suite validates that:
 * - Every operation returns [Result.failure]
 * - The failure exception is [UnsupportedOperationException]
 * - Error messages are descriptive (mention the protocol and "web")
 * - The [isConnected] property always returns false
 * - Client instances can be created without error
 */
class WasmProtocolStubTests {

    // ==================== FTP Protocol Client Tests ====================

    @Test
    fun `FtpProtocolClient can be instantiated on Wasm`() {
        val client = FtpProtocolClient()
        assertNotNull(client, "FtpProtocolClient should be instantiable on Wasm")
    }

    @Test
    fun `FtpProtocolClient isConnected always returns false`() {
        val client = FtpProtocolClient()
        assertFalse(client.isConnected, "FTP should never be connected on Wasm")
    }

    @Test
    fun `FtpProtocolClient connect fails with UnsupportedOperationException`() = runTest {
        val client = FtpProtocolClient()
        val result = client.connect("example.com", 21)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertIs<UnsupportedOperationException>(exception)
        assertTrue(
            exception.message?.contains("FTP") == true,
            "Error message should mention FTP, was: ${exception.message}"
        )
        assertTrue(
            exception.message?.contains("web") == true,
            "Error message should mention web platform, was: ${exception.message}"
        )
    }

    @Test
    fun `FtpProtocolClient login fails with UnsupportedOperationException`() = runTest {
        val client = FtpProtocolClient()
        val result = client.login("user", "pass")

        assertTrue(result.isFailure)
        assertIs<UnsupportedOperationException>(result.exceptionOrNull())
    }

    @Test
    fun `FtpProtocolClient disconnect fails with UnsupportedOperationException`() = runTest {
        val client = FtpProtocolClient()
        val result = client.disconnect()

        assertTrue(result.isFailure)
        assertIs<UnsupportedOperationException>(result.exceptionOrNull())
    }

    @Test
    fun `FtpProtocolClient list fails with UnsupportedOperationException`() = runTest {
        val client = FtpProtocolClient()
        val result = client.list("/")

        assertTrue(result.isFailure)
        assertIs<UnsupportedOperationException>(result.exceptionOrNull())
    }

    @Test
    fun `FtpProtocolClient retrieve fails with UnsupportedOperationException`() = runTest {
        val client = FtpProtocolClient()
        val result = client.retrieve("/file.txt")

        assertTrue(result.isFailure)
        assertIs<UnsupportedOperationException>(result.exceptionOrNull())
    }

    @Test
    fun `FtpProtocolClient store fails with UnsupportedOperationException`() = runTest {
        val client = FtpProtocolClient()
        val result = client.store("/file.txt", byteArrayOf(1, 2, 3))

        assertTrue(result.isFailure)
        assertIs<UnsupportedOperationException>(result.exceptionOrNull())
    }

    @Test
    fun `FtpProtocolClient delete fails with UnsupportedOperationException`() = runTest {
        val client = FtpProtocolClient()
        val result = client.delete("/file.txt")

        assertTrue(result.isFailure)
        assertIs<UnsupportedOperationException>(result.exceptionOrNull())
    }

    @Test
    fun `FtpProtocolClient mkdir fails with UnsupportedOperationException`() = runTest {
        val client = FtpProtocolClient()
        val result = client.mkdir("/newdir")

        assertTrue(result.isFailure)
        assertIs<UnsupportedOperationException>(result.exceptionOrNull())
    }

    @Test
    fun `FtpProtocolClient all operations produce consistent error messages`() = runTest {
        val client = FtpProtocolClient()
        val operations = listOf(
            client.connect("host", 21),
            client.login("user", "pass"),
            client.disconnect(),
            client.list("/"),
            client.retrieve("/f"),
            client.store("/f", byteArrayOf()),
            client.delete("/f"),
            client.mkdir("/d"),
            client.rmdir("/d"),
            client.rename("/a", "/b"),
            client.pwd(),
            client.cwd("/"),
            client.size("/f"),
            client.mdtm("/f"),
            client.setTransferType(true),
            client.pasv()
        )

        operations.forEach { result ->
            assertTrue(result.isFailure, "All FTP operations should fail on Wasm")
            val ex = result.exceptionOrNull()
            assertIs<UnsupportedOperationException>(ex)
            assertEquals("FTP not supported on web", ex.message)
        }
    }

    // ==================== SFTP / SSH Client Tests ====================

    @Test
    fun `SshClient can be instantiated on Wasm`() {
        val client = SshClient()
        assertNotNull(client, "SshClient should be instantiable on Wasm")
    }

    @Test
    fun `SshClient isConnected always returns false`() {
        val client = SshClient()
        assertFalse(client.isConnected, "SSH should never be connected on Wasm")
    }

    @Test
    fun `SshClient connect fails with UnsupportedOperationException`() = runTest {
        val client = SshClient()
        val result = client.connect("example.com", 22)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertIs<UnsupportedOperationException>(exception)
        assertTrue(
            exception.message?.contains("SFTP") == true,
            "Error message should mention SFTP, was: ${exception.message}"
        )
        assertTrue(
            exception.message?.contains("web") == true,
            "Error message should mention web platform, was: ${exception.message}"
        )
    }

    @Test
    fun `SshClient authenticatePassword fails with UnsupportedOperationException`() = runTest {
        val client = SshClient()
        val result = client.authenticatePassword("user", "pass")

        assertTrue(result.isFailure)
        assertIs<UnsupportedOperationException>(result.exceptionOrNull())
    }

    @Test
    fun `SshClient authenticateKey fails with UnsupportedOperationException`() = runTest {
        val client = SshClient()
        val result = client.authenticateKey("user", "/path/to/key", "passphrase")

        assertTrue(result.isFailure)
        assertIs<UnsupportedOperationException>(result.exceptionOrNull())
    }

    @Test
    fun `SshClient authenticateKey without passphrase fails with UnsupportedOperationException`() = runTest {
        val client = SshClient()
        val result = client.authenticateKey("user", "/path/to/key", null)

        assertTrue(result.isFailure)
        assertIs<UnsupportedOperationException>(result.exceptionOrNull())
    }

    @Test
    fun `SshClient openSftpChannel fails with UnsupportedOperationException`() = runTest {
        val client = SshClient()
        val result = client.openSftpChannel()

        assertTrue(result.isFailure)
        assertIs<UnsupportedOperationException>(result.exceptionOrNull())
    }

    @Test
    fun `SshClient disconnect fails with UnsupportedOperationException`() = runTest {
        val client = SshClient()
        val result = client.disconnect()

        assertTrue(result.isFailure)
        assertIs<UnsupportedOperationException>(result.exceptionOrNull())
    }

    @Test
    fun `SshClient all operations produce consistent error messages`() = runTest {
        val client = SshClient()
        val operations = listOf(
            client.connect("host", 22),
            client.authenticatePassword("user", "pass"),
            client.authenticateKey("user", "/key", null),
            client.openSftpChannel(),
            client.disconnect()
        )

        operations.forEach { result ->
            assertTrue(result.isFailure, "All SSH operations should fail on Wasm")
            val ex = result.exceptionOrNull()
            assertIs<UnsupportedOperationException>(ex)
            assertEquals("SFTP not supported on web", ex.message)
        }
    }

    // ==================== SftpChannel Tests ====================

    @Test
    fun `SftpChannel can be instantiated on Wasm`() {
        val channel = SftpChannel()
        assertNotNull(channel, "SftpChannel should be instantiable on Wasm")
    }

    @Test
    fun `SftpChannel list fails with UnsupportedOperationException`() = runTest {
        val channel = SftpChannel()
        val result = channel.list("/")

        assertTrue(result.isFailure)
        assertIs<UnsupportedOperationException>(result.exceptionOrNull())
    }

    @Test
    fun `SftpChannel all operations produce consistent error messages`() = runTest {
        val channel = SftpChannel()
        val operations = listOf(
            channel.list("/"),
            channel.get("/file"),
            channel.put("/file", byteArrayOf()),
            channel.delete("/file"),
            channel.mkdir("/dir"),
            channel.rmdir("/dir"),
            channel.rename("/a", "/b"),
            channel.stat("/file"),
            channel.close()
        )

        operations.forEach { result ->
            assertTrue(result.isFailure, "All SftpChannel operations should fail on Wasm")
            val ex = result.exceptionOrNull()
            assertIs<UnsupportedOperationException>(ex)
            assertEquals("SFTP not supported on web", ex.message)
        }
    }

    // ==================== SMB Protocol Client Tests ====================

    @Test
    fun `SmbProtocolClient can be instantiated on Wasm`() {
        val client = SmbProtocolClient()
        assertNotNull(client, "SmbProtocolClient should be instantiable on Wasm")
    }

    @Test
    fun `SmbProtocolClient isConnected always returns false`() {
        val client = SmbProtocolClient()
        assertFalse(client.isConnected, "SMB should never be connected on Wasm")
    }

    @Test
    fun `SmbProtocolClient connect fails with UnsupportedOperationException`() = runTest {
        val client = SmbProtocolClient()
        val result = client.connect("example.com", 445, "share")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertIs<UnsupportedOperationException>(exception)
        assertTrue(
            exception.message?.contains("SMB") == true,
            "Error message should mention SMB, was: ${exception.message}"
        )
        assertTrue(
            exception.message?.contains("web") == true,
            "Error message should mention web platform, was: ${exception.message}"
        )
    }

    @Test
    fun `SmbProtocolClient authenticate fails with UnsupportedOperationException`() = runTest {
        val client = SmbProtocolClient()
        val result = client.authenticate("DOMAIN", "user", "pass")

        assertTrue(result.isFailure)
        assertIs<UnsupportedOperationException>(result.exceptionOrNull())
    }

    @Test
    fun `SmbProtocolClient disconnect fails with UnsupportedOperationException`() = runTest {
        val client = SmbProtocolClient()
        val result = client.disconnect()

        assertTrue(result.isFailure)
        assertIs<UnsupportedOperationException>(result.exceptionOrNull())
    }

    @Test
    fun `SmbProtocolClient read fails with UnsupportedOperationException`() = runTest {
        val client = SmbProtocolClient()
        val result = client.read("/file.txt")

        assertTrue(result.isFailure)
        assertIs<UnsupportedOperationException>(result.exceptionOrNull())
    }

    @Test
    fun `SmbProtocolClient write fails with UnsupportedOperationException`() = runTest {
        val client = SmbProtocolClient()
        val result = client.write("/file.txt", byteArrayOf(1, 2, 3))

        assertTrue(result.isFailure)
        assertIs<UnsupportedOperationException>(result.exceptionOrNull())
    }

    @Test
    fun `SmbProtocolClient all operations produce consistent error messages`() = runTest {
        val client = SmbProtocolClient()
        val operations = listOf(
            client.connect("host", 445, "share"),
            client.authenticate("DOMAIN", "user", "pass"),
            client.disconnect(),
            client.list("/"),
            client.read("/f"),
            client.write("/f", byteArrayOf()),
            client.delete("/f"),
            client.mkdir("/d"),
            client.rmdir("/d"),
            client.rename("/a", "/b"),
            client.stat("/f"),
            client.getShareInfo()
        )

        operations.forEach { result ->
            assertTrue(result.isFailure, "All SMB operations should fail on Wasm")
            val ex = result.exceptionOrNull()
            assertIs<UnsupportedOperationException>(ex)
            assertEquals("SMB not supported on web", ex.message)
        }
    }

    // ==================== Cross-Protocol Consistency Tests ====================

    @Test
    fun `all protocol stubs report not connected`() {
        val ftp = FtpProtocolClient()
        val ssh = SshClient()
        val smb = SmbProtocolClient()

        assertFalse(ftp.isConnected, "FTP isConnected should be false")
        assertFalse(ssh.isConnected, "SSH isConnected should be false")
        assertFalse(smb.isConnected, "SMB isConnected should be false")
    }

    @Test
    fun `all protocol stubs use UnsupportedOperationException consistently`() = runTest {
        val ftpResult = FtpProtocolClient().connect("host", 21)
        val sshResult = SshClient().connect("host", 22)
        val smbResult = SmbProtocolClient().connect("host", 445, "share")

        listOf(ftpResult, sshResult, smbResult).forEach { result ->
            assertTrue(result.isFailure)
            assertIs<UnsupportedOperationException>(result.exceptionOrNull())
        }
    }

    @Test
    fun `all protocol error messages mention web platform`() = runTest {
        val ftpMsg = FtpProtocolClient().connect("h", 21).exceptionOrNull()?.message ?: ""
        val sshMsg = SshClient().connect("h", 22).exceptionOrNull()?.message ?: ""
        val smbMsg = SmbProtocolClient().connect("h", 445, "s").exceptionOrNull()?.message ?: ""

        listOf(ftpMsg, sshMsg, smbMsg).forEach { msg ->
            assertTrue(
                msg.contains("web", ignoreCase = true),
                "Error message should mention web platform: '$msg'"
            )
        }
    }

    @Test
    fun `all protocol error messages mention their protocol name`() = runTest {
        val ftpMsg = FtpProtocolClient().connect("h", 21).exceptionOrNull()?.message ?: ""
        val sshMsg = SshClient().connect("h", 22).exceptionOrNull()?.message ?: ""
        val smbMsg = SmbProtocolClient().connect("h", 445, "s").exceptionOrNull()?.message ?: ""

        assertTrue(ftpMsg.contains("FTP"), "FTP error should mention FTP: '$ftpMsg'")
        assertTrue(sshMsg.contains("SFTP"), "SSH error should mention SFTP: '$sshMsg'")
        assertTrue(smbMsg.contains("SMB"), "SMB error should mention SMB: '$smbMsg'")
    }
}
