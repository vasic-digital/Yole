/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for protocol service path utility methods
 *
 *########################################################*/
package digital.vasic.yole.network.protocols

import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.protocols.dropbox.DropboxService
import digital.vasic.yole.network.protocols.ftp.FtpService
import digital.vasic.yole.network.protocols.git.GitService
import digital.vasic.yole.network.protocols.googledrive.GoogleDriveService
import digital.vasic.yole.network.protocols.onedrive.OneDriveService
import digital.vasic.yole.network.protocols.sftp.SftpService
import digital.vasic.yole.network.protocols.smb.SmbService
import digital.vasic.yole.network.protocols.webdav.WebDavService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [getParentPath] and [validatePath] across all protocol service implementations.
 *
 * These are pure string-operation methods that require no network connectivity,
 * so each service is constructed with minimal test configs and exercised directly.
 */
class ServicePathUtilsTest {

    // ==================== Service Instances ====================

    private val dropboxService = DropboxService(
        StorageConfig.DropboxConfig(
            name = "test-dropbox",
            accessToken = "token",
            appKey = "key",
            appSecret = "secret"
        )
    )

    private val googleDriveService = GoogleDriveService(
        StorageConfig.GoogleDriveConfig(
            name = "test-gdrive",
            clientId = "client-id",
            clientSecret = "client-secret"
        )
    )

    private val oneDriveService = OneDriveService(
        StorageConfig.OneDriveConfig(
            name = "test-onedrive",
            clientId = "client-id",
            clientSecret = "client-secret"
        )
    )

    private val ftpService = FtpService(
        StorageConfig.FtpConfig(
            name = "test-ftp",
            host = "localhost",
            username = "user",
            password = "pass"
        )
    )

    private val sftpService = SftpService(
        StorageConfig.SftpConfig(
            name = "test-sftp",
            host = "localhost"
        )
    )

    private val webDavService = WebDavService(
        StorageConfig.WebDavConfig(
            name = "test-webdav",
            url = "https://localhost/dav",
            username = "user",
            password = "pass"
        )
    )

    private val gitService = GitService(
        StorageConfig.GitConfig(
            name = "test-git",
            repositoryUrl = "https://example.com/repo.git",
            localCachePath = "/tmp/test-git-cache"
        )
    )

    private val smbService = SmbService(
        StorageConfig.SmbConfig(
            name = "test-smb",
            host = "localhost",
            share = "share",
            username = "user",
            password = "pass"
        )
    )

    // ==================== DropboxService: getParentPath ====================

    @Test
    fun `DropboxService getParentPath of root returns root`() {
        val result = dropboxService.getParentPath("/")
        assertEquals("/", result, "Dropbox treats root parent as root")
    }

    @Test
    fun `DropboxService getParentPath of blank returns root`() {
        val result = dropboxService.getParentPath("")
        assertEquals("/", result, "Dropbox treats blank path parent as root")
    }

    @Test
    fun `DropboxService getParentPath of nested path returns parent`() {
        val result = dropboxService.getParentPath("/folder/subfolder")
        assertEquals("/folder", result)
    }

    @Test
    fun `DropboxService getParentPath of file in root returns root`() {
        val result = dropboxService.getParentPath("/file.txt")
        assertEquals("/", result)
    }

    @Test
    fun `DropboxService getParentPath of deeply nested file returns parent`() {
        val result = dropboxService.getParentPath("/a/b/c/file.txt")
        assertEquals("/a/b/c", result)
    }

    // ==================== DropboxService: validatePath ====================

    @Test
    fun `DropboxService validatePath accepts blank paths`() {
        val result = dropboxService.validatePath("")
        assertTrue(result.isSuccess, "Dropbox treats blank paths as root, so they are valid")
    }

    @Test
    fun `DropboxService validatePath accepts valid paths`() {
        val result = dropboxService.validatePath("/folder/file.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `DropboxService validatePath accepts root path`() {
        val result = dropboxService.validatePath("/")
        assertTrue(result.isSuccess)
    }

    // ==================== GoogleDriveService: getParentPath ====================

    @Test
    fun `GoogleDriveService getParentPath of root returns null`() {
        val result = googleDriveService.getParentPath("/")
        assertNull(result, "Root has no parent")
    }

    @Test
    fun `GoogleDriveService getParentPath of blank returns null`() {
        val result = googleDriveService.getParentPath("")
        assertNull(result, "Blank path has no parent")
    }

    @Test
    fun `GoogleDriveService getParentPath of nested path returns parent`() {
        val result = googleDriveService.getParentPath("/folder/subfolder")
        assertEquals("/folder", result)
    }

    @Test
    fun `GoogleDriveService getParentPath of file in root returns root`() {
        val result = googleDriveService.getParentPath("/file.txt")
        assertEquals("/", result)
    }

    @Test
    fun `GoogleDriveService getParentPath of deeply nested file returns parent`() {
        val result = googleDriveService.getParentPath("/a/b/c/file.txt")
        assertEquals("/a/b/c", result)
    }

    // ==================== GoogleDriveService: validatePath ====================

    @Test
    fun `GoogleDriveService validatePath rejects blank paths`() {
        val result = googleDriveService.validatePath("")
        assertTrue(result.isFailure, "Blank path should be invalid")
    }

    @Test
    fun `GoogleDriveService validatePath accepts valid paths`() {
        val result = googleDriveService.validatePath("/folder/file.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `GoogleDriveService validatePath accepts root path`() {
        val result = googleDriveService.validatePath("/")
        assertTrue(result.isSuccess)
    }

    // ==================== OneDriveService: getParentPath ====================

    @Test
    fun `OneDriveService getParentPath of root returns null`() {
        val result = oneDriveService.getParentPath("/")
        assertNull(result, "Root has no parent")
    }

    @Test
    fun `OneDriveService getParentPath of blank returns null`() {
        val result = oneDriveService.getParentPath("")
        assertNull(result, "Blank path has no parent")
    }

    @Test
    fun `OneDriveService getParentPath of nested path returns parent`() {
        val result = oneDriveService.getParentPath("/folder/subfolder")
        assertEquals("/folder", result)
    }

    @Test
    fun `OneDriveService getParentPath of file in root returns root`() {
        val result = oneDriveService.getParentPath("/file.txt")
        assertEquals("/", result)
    }

    @Test
    fun `OneDriveService getParentPath of deeply nested file returns parent`() {
        val result = oneDriveService.getParentPath("/a/b/c/file.txt")
        assertEquals("/a/b/c", result)
    }

    // ==================== OneDriveService: validatePath ====================

    @Test
    fun `OneDriveService validatePath rejects blank paths`() {
        val result = oneDriveService.validatePath("")
        assertTrue(result.isFailure, "Blank path should be invalid")
    }

    @Test
    fun `OneDriveService validatePath accepts valid paths`() {
        val result = oneDriveService.validatePath("/folder/file.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `OneDriveService validatePath accepts root path`() {
        val result = oneDriveService.validatePath("/")
        assertTrue(result.isSuccess)
    }

    // ==================== FtpService: getParentPath ====================

    @Test
    fun `FtpService getParentPath of root returns null`() {
        val result = ftpService.getParentPath("/")
        assertNull(result, "Root has no parent")
    }

    @Test
    fun `FtpService getParentPath of blank returns null`() {
        val result = ftpService.getParentPath("")
        assertNull(result, "Blank path has no parent")
    }

    @Test
    fun `FtpService getParentPath of nested path returns parent`() {
        val result = ftpService.getParentPath("/folder/subfolder")
        assertEquals("/folder", result)
    }

    @Test
    fun `FtpService getParentPath of file in root returns root`() {
        val result = ftpService.getParentPath("/file.txt")
        assertEquals("/", result)
    }

    @Test
    fun `FtpService getParentPath of path with trailing slash returns parent`() {
        val result = ftpService.getParentPath("/folder/subfolder/")
        assertEquals("/folder", result)
    }

    @Test
    fun `FtpService getParentPath of deeply nested file returns parent`() {
        val result = ftpService.getParentPath("/a/b/c/file.txt")
        assertEquals("/a/b/c", result)
    }

    // ==================== FtpService: validatePath ====================

    @Test
    fun `FtpService validatePath rejects blank paths`() {
        val result = ftpService.validatePath("")
        assertTrue(result.isFailure, "Blank path should be invalid")
    }

    @Test
    fun `FtpService validatePath accepts valid paths`() {
        val result = ftpService.validatePath("/folder/file.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `FtpService validatePath accepts root path`() {
        val result = ftpService.validatePath("/")
        assertTrue(result.isSuccess)
    }

    // ==================== SftpService: getParentPath ====================

    @Test
    fun `SftpService getParentPath of root returns null`() {
        val result = sftpService.getParentPath("/")
        assertNull(result, "Root has no parent")
    }

    @Test
    fun `SftpService getParentPath of blank returns null`() {
        val result = sftpService.getParentPath("")
        assertNull(result, "Blank path has no parent")
    }

    @Test
    fun `SftpService getParentPath of nested path returns parent`() {
        val result = sftpService.getParentPath("/folder/subfolder")
        assertEquals("/folder", result)
    }

    @Test
    fun `SftpService getParentPath of file in root returns root`() {
        val result = sftpService.getParentPath("/file.txt")
        assertEquals("/", result)
    }

    @Test
    fun `SftpService getParentPath of path with trailing slash returns parent`() {
        val result = sftpService.getParentPath("/folder/subfolder/")
        assertEquals("/folder", result)
    }

    @Test
    fun `SftpService getParentPath of deeply nested file returns parent`() {
        val result = sftpService.getParentPath("/a/b/c/file.txt")
        assertEquals("/a/b/c", result)
    }

    // ==================== SftpService: validatePath ====================

    @Test
    fun `SftpService validatePath rejects blank paths`() {
        val result = sftpService.validatePath("")
        assertTrue(result.isFailure, "Blank path should be invalid")
    }

    @Test
    fun `SftpService validatePath accepts valid paths`() {
        val result = sftpService.validatePath("/folder/file.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `SftpService validatePath accepts root path`() {
        val result = sftpService.validatePath("/")
        assertTrue(result.isSuccess)
    }

    // ==================== WebDavService: getParentPath ====================

    @Test
    fun `WebDavService getParentPath of root returns null`() {
        val result = webDavService.getParentPath("/")
        assertNull(result, "Root has no parent")
    }

    @Test
    fun `WebDavService getParentPath of blank returns null`() {
        val result = webDavService.getParentPath("")
        assertNull(result, "Blank path has no parent")
    }

    @Test
    fun `WebDavService getParentPath of nested path returns parent`() {
        val result = webDavService.getParentPath("/folder/subfolder")
        assertEquals("/folder", result)
    }

    @Test
    fun `WebDavService getParentPath of file in root returns root`() {
        val result = webDavService.getParentPath("/file.txt")
        assertEquals("/", result)
    }

    @Test
    fun `WebDavService getParentPath of deeply nested file returns parent`() {
        val result = webDavService.getParentPath("/a/b/c/file.txt")
        assertEquals("/a/b/c", result)
    }

    // ==================== WebDavService: validatePath ====================

    @Test
    fun `WebDavService validatePath rejects blank paths`() {
        val result = webDavService.validatePath("")
        assertTrue(result.isFailure, "Blank path should be invalid")
    }

    @Test
    fun `WebDavService validatePath accepts valid paths`() {
        val result = webDavService.validatePath("/folder/file.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `WebDavService validatePath accepts root path`() {
        val result = webDavService.validatePath("/")
        assertTrue(result.isSuccess)
    }

    // ==================== GitService: getParentPath ====================

    @Test
    fun `GitService getParentPath of root returns null`() {
        val result = gitService.getParentPath("/")
        assertNull(result, "Root has no parent")
    }

    @Test
    fun `GitService getParentPath of blank returns null`() {
        val result = gitService.getParentPath("")
        assertNull(result, "Blank path has no parent")
    }

    @Test
    fun `GitService getParentPath of nested path returns parent`() {
        val result = gitService.getParentPath("/folder/subfolder")
        assertEquals("/folder", result)
    }

    @Test
    fun `GitService getParentPath of file in root returns root`() {
        val result = gitService.getParentPath("/file.txt")
        assertEquals("/", result)
    }

    @Test
    fun `GitService getParentPath of deeply nested file returns parent`() {
        val result = gitService.getParentPath("/a/b/c/file.txt")
        assertEquals("/a/b/c", result)
    }

    // ==================== GitService: validatePath ====================

    @Test
    fun `GitService validatePath rejects blank paths`() {
        val result = gitService.validatePath("")
        assertTrue(result.isFailure, "Blank path should be invalid")
    }

    @Test
    fun `GitService validatePath accepts valid paths`() {
        val result = gitService.validatePath("/folder/file.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `GitService validatePath accepts root path`() {
        val result = gitService.validatePath("/")
        assertTrue(result.isSuccess)
    }

    // ==================== SmbService: getParentPath ====================

    @Test
    fun `SmbService getParentPath of root returns null`() {
        val result = smbService.getParentPath("/")
        assertNull(result, "Root has no parent")
    }

    @Test
    fun `SmbService getParentPath of blank returns null`() {
        val result = smbService.getParentPath("")
        assertNull(result, "Blank path has no parent")
    }

    @Test
    fun `SmbService getParentPath of nested path returns parent`() {
        val result = smbService.getParentPath("/folder/subfolder")
        assertEquals("/folder", result)
    }

    @Test
    fun `SmbService getParentPath of file in root returns root`() {
        val result = smbService.getParentPath("/file.txt")
        assertEquals("/", result)
    }

    @Test
    fun `SmbService getParentPath of path with trailing slash returns parent`() {
        val result = smbService.getParentPath("/folder/subfolder/")
        assertEquals("/folder", result)
    }

    @Test
    fun `SmbService getParentPath of deeply nested file returns parent`() {
        val result = smbService.getParentPath("/a/b/c/file.txt")
        assertEquals("/a/b/c", result)
    }

    // ==================== SmbService: validatePath ====================

    @Test
    fun `SmbService validatePath rejects blank paths`() {
        val result = smbService.validatePath("")
        assertTrue(result.isFailure, "Blank path should be invalid")
    }

    @Test
    fun `SmbService validatePath accepts valid paths`() {
        val result = smbService.validatePath("/folder/file.txt")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `SmbService validatePath accepts root path`() {
        val result = smbService.validatePath("/")
        assertTrue(result.isSuccess)
    }

    // ==================== Cross-service consistency ====================

    @Test
    fun `all services except Dropbox return null for root getParentPath`() {
        assertNull(googleDriveService.getParentPath("/"))
        assertNull(oneDriveService.getParentPath("/"))
        assertNull(ftpService.getParentPath("/"))
        assertNull(sftpService.getParentPath("/"))
        assertNull(webDavService.getParentPath("/"))
        assertNull(gitService.getParentPath("/"))
        assertNull(smbService.getParentPath("/"))
    }

    @Test
    fun `Dropbox returns root for root getParentPath`() {
        assertEquals("/", dropboxService.getParentPath("/"))
    }

    @Test
    fun `all services return root as parent of top-level file`() {
        assertEquals("/", dropboxService.getParentPath("/notes.txt"))
        assertEquals("/", googleDriveService.getParentPath("/notes.txt"))
        assertEquals("/", oneDriveService.getParentPath("/notes.txt"))
        assertEquals("/", ftpService.getParentPath("/notes.txt"))
        assertEquals("/", sftpService.getParentPath("/notes.txt"))
        assertEquals("/", webDavService.getParentPath("/notes.txt"))
        assertEquals("/", gitService.getParentPath("/notes.txt"))
        assertEquals("/", smbService.getParentPath("/notes.txt"))
    }

    @Test
    fun `all services agree on parent of nested path`() {
        val path = "/documents/work/report.md"
        val expected = "/documents/work"
        assertEquals(expected, dropboxService.getParentPath(path))
        assertEquals(expected, googleDriveService.getParentPath(path))
        assertEquals(expected, oneDriveService.getParentPath(path))
        assertEquals(expected, ftpService.getParentPath(path))
        assertEquals(expected, sftpService.getParentPath(path))
        assertEquals(expected, webDavService.getParentPath(path))
        assertEquals(expected, gitService.getParentPath(path))
        assertEquals(expected, smbService.getParentPath(path))
    }

    @Test
    fun `all services except Dropbox reject blank validatePath`() {
        assertTrue(googleDriveService.validatePath("").isFailure)
        assertTrue(oneDriveService.validatePath("").isFailure)
        assertTrue(ftpService.validatePath("").isFailure)
        assertTrue(sftpService.validatePath("").isFailure)
        assertTrue(webDavService.validatePath("").isFailure)
        assertTrue(gitService.validatePath("").isFailure)
        assertTrue(smbService.validatePath("").isFailure)
    }

    @Test
    fun `Dropbox accepts blank validatePath`() {
        assertTrue(dropboxService.validatePath("").isSuccess)
    }

    @Test
    fun `all services accept valid path in validatePath`() {
        val path = "/valid/path.txt"
        assertTrue(dropboxService.validatePath(path).isSuccess)
        assertTrue(googleDriveService.validatePath(path).isSuccess)
        assertTrue(oneDriveService.validatePath(path).isSuccess)
        assertTrue(ftpService.validatePath(path).isSuccess)
        assertTrue(sftpService.validatePath(path).isSuccess)
        assertTrue(webDavService.validatePath(path).isSuccess)
        assertTrue(gitService.validatePath(path).isSuccess)
        assertTrue(smbService.validatePath(path).isSuccess)
    }

    @Test
    fun `all services except Dropbox reject whitespace-only validatePath`() {
        val whitespace = "   "
        assertTrue(googleDriveService.validatePath(whitespace).isFailure)
        assertTrue(oneDriveService.validatePath(whitespace).isFailure)
        assertTrue(ftpService.validatePath(whitespace).isFailure)
        assertTrue(sftpService.validatePath(whitespace).isFailure)
        assertTrue(webDavService.validatePath(whitespace).isFailure)
        assertTrue(gitService.validatePath(whitespace).isFailure)
        assertTrue(smbService.validatePath(whitespace).isFailure)
    }
}
