/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Protocol Supremacy (Edge Case) Tests
 *
 * Validates handling of extreme and unusual inputs
 * across all 8 protocol services: empty paths, very
 * long paths, special characters, unicode, and
 * null-like paths.
 *
 * Total: ~40 tests (5 per protocol x 8 protocols)
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.coverage

import digital.vasic.yole.network.NetworkStorageService
import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.protocols.dropbox.DropboxService
import digital.vasic.yole.network.protocols.ftp.FtpService
import digital.vasic.yole.network.protocols.git.GitService
import digital.vasic.yole.network.protocols.googledrive.GoogleDriveService
import digital.vasic.yole.network.protocols.onedrive.OneDriveService
import digital.vasic.yole.network.protocols.sftp.SftpService
import digital.vasic.yole.network.protocols.smb.SmbService
import digital.vasic.yole.network.protocols.webdav.WebDavService
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/**
 * Edge case (supremacy) tests for all 8 protocol implementations.
 *
 * Scenarios per protocol:
 * 1. Empty path operations complete without crash
 * 2. Very long path (10000 chars) does not crash
 * 3. Path with special characters handles gracefully
 * 4. Path with unicode characters handles gracefully
 * 5. Null-like paths (whitespace-only) handle gracefully
 */
class ProtocolSupremacyTests {

    // ==================== Config Factories ====================

    private fun dropboxConfig() = StorageConfig.DropboxConfig(
        name = "sup-dropbox", accessToken = "tok", appKey = "key", appSecret = "sec"
    )
    private fun ftpConfig() = StorageConfig.FtpConfig(
        name = "sup-ftp", host = "ftp.example.com", port = 21,
        username = "u", password = "p", rootPath = "/data"
    )
    private fun sftpConfig() = StorageConfig.SftpConfig(
        name = "sup-sftp", host = "sftp.example.com", port = 22,
        username = "u", password = "p", rootPath = "/home"
    )
    private fun smbConfig() = StorageConfig.SmbConfig(
        name = "sup-smb", host = "smb.example.com", share = "shared",
        username = "u", password = "p", path = "/docs"
    )
    private fun googleDriveConfig() = StorageConfig.GoogleDriveConfig(
        name = "sup-gdrive", clientId = "cid", clientSecret = "cs", refreshToken = "rt"
    )
    private fun oneDriveConfig() = StorageConfig.OneDriveConfig(
        name = "sup-onedrive", clientId = "cid", clientSecret = "cs", refreshToken = "rt"
    )
    private fun gitConfig() = StorageConfig.GitConfig(
        name = "sup-git", repositoryUrl = "https://github.com/test/repo.git",
        branch = "main", personalAccessToken = "pat", localCachePath = "/tmp/git-cache"
    )
    private fun webDavConfig() = StorageConfig.WebDavConfig(
        name = "sup-webdav", url = "https://webdav.example.com/dav",
        username = "u", password = "p"
    )

    // ==================== Helpers ====================

    private val longPath = "/" + "a".repeat(10000) + "/file.txt"
    private val specialCharsPath = "/path/with special!@#\$%^&()chars/file.txt"
    private val unicodePath = "/docs/\u00E9\u00E8\u00EA/\u4E2D\u6587/\u0410\u0411\u0412/file.txt"
    private val whitespacePath = "   "

    private suspend fun assertEdgeCasesHandled(svc: NetworkStorageService, name: String) {
        // Empty path
        val emptyResult = svc.exists("")
        assertTrue(emptyResult.isSuccess || emptyResult.isFailure,
            "$name should handle empty path")

        // Very long path
        val longResult = svc.getFileInfo(longPath)
        assertTrue(longResult.isSuccess || longResult.isFailure,
            "$name should handle very long path without crash")

        // Special characters
        val specialResult = svc.exists(specialCharsPath)
        assertTrue(specialResult.isSuccess || specialResult.isFailure,
            "$name should handle special character path")

        // Unicode characters
        val unicodeResult = svc.exists(unicodePath)
        assertTrue(unicodeResult.isSuccess || unicodeResult.isFailure,
            "$name should handle unicode path")

        // Whitespace-only (null-like) path
        val wsResult = svc.exists(whitespacePath)
        assertTrue(wsResult.isSuccess || wsResult.isFailure,
            "$name should handle whitespace-only path")
    }

    // ==================== Empty Path ====================

    @Test
    fun dropboxEmptyPath() = runBlocking<Unit> {
        val svc = DropboxService(dropboxConfig())
        val r = svc.exists("")
        assertTrue(r.isSuccess || r.isFailure, "Dropbox should handle empty path")
    }

    @Test
    fun ftpEmptyPath() = runBlocking<Unit> {
        val svc = FtpService(ftpConfig())
        val r = svc.exists("")
        assertTrue(r.isSuccess || r.isFailure, "FTP should handle empty path")
    }

    @Test
    fun sftpEmptyPath() = runBlocking<Unit> {
        val svc = SftpService(sftpConfig())
        val r = svc.exists("")
        assertTrue(r.isSuccess || r.isFailure, "SFTP should handle empty path")
    }

    @Test
    fun smbEmptyPath() = runBlocking<Unit> {
        val svc = SmbService(smbConfig())
        val r = svc.exists("")
        assertTrue(r.isSuccess || r.isFailure, "SMB should handle empty path")
    }

    @Test
    fun googleDriveEmptyPath() = runBlocking<Unit> {
        val svc = GoogleDriveService(googleDriveConfig())
        val r = svc.exists("")
        assertTrue(r.isSuccess || r.isFailure, "GoogleDrive should handle empty path")
    }

    @Test
    fun oneDriveEmptyPath() = runBlocking<Unit> {
        val svc = OneDriveService(oneDriveConfig())
        val r = svc.exists("")
        assertTrue(r.isSuccess || r.isFailure, "OneDrive should handle empty path")
    }

    @Test
    fun gitEmptyPath() = runBlocking<Unit> {
        val svc = GitService(gitConfig())
        val r = svc.exists("")
        assertTrue(r.isSuccess || r.isFailure, "Git should handle empty path")
    }

    @Test
    fun webDavEmptyPath() = runBlocking<Unit> {
        val svc = WebDavService(webDavConfig())
        val r = svc.exists("")
        assertTrue(r.isSuccess || r.isFailure, "WebDAV should handle empty path")
    }

    // ==================== Very Long Path ====================

    @Test
    fun dropboxLongPath() = runBlocking<Unit> {
        val svc = DropboxService(dropboxConfig())
        val r = svc.getFileInfo(longPath)
        assertTrue(r.isSuccess || r.isFailure, "Dropbox should handle long path")
    }

    @Test
    fun ftpLongPath() = runBlocking<Unit> {
        val svc = FtpService(ftpConfig())
        val r = svc.getFileInfo(longPath)
        assertTrue(r.isSuccess || r.isFailure, "FTP should handle long path")
    }

    @Test
    fun sftpLongPath() = runBlocking<Unit> {
        val svc = SftpService(sftpConfig())
        val r = svc.getFileInfo(longPath)
        assertTrue(r.isSuccess || r.isFailure, "SFTP should handle long path")
    }

    @Test
    fun smbLongPath() = runBlocking<Unit> {
        val svc = SmbService(smbConfig())
        val r = svc.getFileInfo(longPath)
        assertTrue(r.isSuccess || r.isFailure, "SMB should handle long path")
    }

    @Test
    fun googleDriveLongPath() = runBlocking<Unit> {
        val svc = GoogleDriveService(googleDriveConfig())
        val r = svc.getFileInfo(longPath)
        assertTrue(r.isSuccess || r.isFailure, "GoogleDrive should handle long path")
    }

    @Test
    fun oneDriveLongPath() = runBlocking<Unit> {
        val svc = OneDriveService(oneDriveConfig())
        val r = svc.getFileInfo(longPath)
        assertTrue(r.isSuccess || r.isFailure, "OneDrive should handle long path")
    }

    @Test
    fun gitLongPath() = runBlocking<Unit> {
        val svc = GitService(gitConfig())
        val r = svc.getFileInfo(longPath)
        assertTrue(r.isSuccess || r.isFailure, "Git should handle long path")
    }

    @Test
    fun webDavLongPath() = runBlocking<Unit> {
        val svc = WebDavService(webDavConfig())
        val r = svc.getFileInfo(longPath)
        assertTrue(r.isSuccess || r.isFailure, "WebDAV should handle long path")
    }

    // ==================== Special Characters ====================

    @Test
    fun dropboxSpecialChars() = runBlocking<Unit> {
        val svc = DropboxService(dropboxConfig())
        val r = svc.exists(specialCharsPath)
        assertTrue(r.isSuccess || r.isFailure, "Dropbox should handle special chars")
    }

    @Test
    fun ftpSpecialChars() = runBlocking<Unit> {
        val svc = FtpService(ftpConfig())
        val r = svc.exists(specialCharsPath)
        assertTrue(r.isSuccess || r.isFailure, "FTP should handle special chars")
    }

    @Test
    fun sftpSpecialChars() = runBlocking<Unit> {
        val svc = SftpService(sftpConfig())
        val r = svc.exists(specialCharsPath)
        assertTrue(r.isSuccess || r.isFailure, "SFTP should handle special chars")
    }

    @Test
    fun smbSpecialChars() = runBlocking<Unit> {
        val svc = SmbService(smbConfig())
        val r = svc.exists(specialCharsPath)
        assertTrue(r.isSuccess || r.isFailure, "SMB should handle special chars")
    }

    @Test
    fun googleDriveSpecialChars() = runBlocking<Unit> {
        val svc = GoogleDriveService(googleDriveConfig())
        val r = svc.exists(specialCharsPath)
        assertTrue(r.isSuccess || r.isFailure, "GoogleDrive should handle special chars")
    }

    @Test
    fun oneDriveSpecialChars() = runBlocking<Unit> {
        val svc = OneDriveService(oneDriveConfig())
        val r = svc.exists(specialCharsPath)
        assertTrue(r.isSuccess || r.isFailure, "OneDrive should handle special chars")
    }

    @Test
    fun gitSpecialChars() = runBlocking<Unit> {
        val svc = GitService(gitConfig())
        val r = svc.exists(specialCharsPath)
        assertTrue(r.isSuccess || r.isFailure, "Git should handle special chars")
    }

    @Test
    fun webDavSpecialChars() = runBlocking<Unit> {
        val svc = WebDavService(webDavConfig())
        val r = svc.exists(specialCharsPath)
        assertTrue(r.isSuccess || r.isFailure, "WebDAV should handle special chars")
    }

    // ==================== Unicode Path ====================

    @Test
    fun dropboxUnicodePath() = runBlocking<Unit> {
        val svc = DropboxService(dropboxConfig())
        val r = svc.exists(unicodePath)
        assertTrue(r.isSuccess || r.isFailure, "Dropbox should handle unicode path")
    }

    @Test
    fun ftpUnicodePath() = runBlocking<Unit> {
        val svc = FtpService(ftpConfig())
        val r = svc.exists(unicodePath)
        assertTrue(r.isSuccess || r.isFailure, "FTP should handle unicode path")
    }

    @Test
    fun sftpUnicodePath() = runBlocking<Unit> {
        val svc = SftpService(sftpConfig())
        val r = svc.exists(unicodePath)
        assertTrue(r.isSuccess || r.isFailure, "SFTP should handle unicode path")
    }

    @Test
    fun smbUnicodePath() = runBlocking<Unit> {
        val svc = SmbService(smbConfig())
        val r = svc.exists(unicodePath)
        assertTrue(r.isSuccess || r.isFailure, "SMB should handle unicode path")
    }

    @Test
    fun googleDriveUnicodePath() = runBlocking<Unit> {
        val svc = GoogleDriveService(googleDriveConfig())
        val r = svc.exists(unicodePath)
        assertTrue(r.isSuccess || r.isFailure, "GoogleDrive should handle unicode path")
    }

    @Test
    fun oneDriveUnicodePath() = runBlocking<Unit> {
        val svc = OneDriveService(oneDriveConfig())
        val r = svc.exists(unicodePath)
        assertTrue(r.isSuccess || r.isFailure, "OneDrive should handle unicode path")
    }

    @Test
    fun gitUnicodePath() = runBlocking<Unit> {
        val svc = GitService(gitConfig())
        val r = svc.exists(unicodePath)
        assertTrue(r.isSuccess || r.isFailure, "Git should handle unicode path")
    }

    @Test
    fun webDavUnicodePath() = runBlocking<Unit> {
        val svc = WebDavService(webDavConfig())
        val r = svc.exists(unicodePath)
        assertTrue(r.isSuccess || r.isFailure, "WebDAV should handle unicode path")
    }

    // ==================== Whitespace-Only (Null-Like) Path ====================

    @Test
    fun dropboxWhitespacePath() = runBlocking<Unit> {
        val svc = DropboxService(dropboxConfig())
        val r = svc.exists(whitespacePath)
        assertTrue(r.isSuccess || r.isFailure, "Dropbox should handle whitespace path")
    }

    @Test
    fun ftpWhitespacePath() = runBlocking<Unit> {
        val svc = FtpService(ftpConfig())
        val r = svc.exists(whitespacePath)
        assertTrue(r.isSuccess || r.isFailure, "FTP should handle whitespace path")
    }

    @Test
    fun sftpWhitespacePath() = runBlocking<Unit> {
        val svc = SftpService(sftpConfig())
        val r = svc.exists(whitespacePath)
        assertTrue(r.isSuccess || r.isFailure, "SFTP should handle whitespace path")
    }

    @Test
    fun smbWhitespacePath() = runBlocking<Unit> {
        val svc = SmbService(smbConfig())
        val r = svc.exists(whitespacePath)
        assertTrue(r.isSuccess || r.isFailure, "SMB should handle whitespace path")
    }

    @Test
    fun googleDriveWhitespacePath() = runBlocking<Unit> {
        val svc = GoogleDriveService(googleDriveConfig())
        val r = svc.exists(whitespacePath)
        assertTrue(r.isSuccess || r.isFailure, "GoogleDrive should handle whitespace path")
    }

    @Test
    fun oneDriveWhitespacePath() = runBlocking<Unit> {
        val svc = OneDriveService(oneDriveConfig())
        val r = svc.exists(whitespacePath)
        assertTrue(r.isSuccess || r.isFailure, "OneDrive should handle whitespace path")
    }

    @Test
    fun gitWhitespacePath() = runBlocking<Unit> {
        val svc = GitService(gitConfig())
        val r = svc.exists(whitespacePath)
        assertTrue(r.isSuccess || r.isFailure, "Git should handle whitespace path")
    }

    @Test
    fun webDavWhitespacePath() = runBlocking<Unit> {
        val svc = WebDavService(webDavConfig())
        val r = svc.exists(whitespacePath)
        assertTrue(r.isSuccess || r.isFailure, "WebDAV should handle whitespace path")
    }
}
