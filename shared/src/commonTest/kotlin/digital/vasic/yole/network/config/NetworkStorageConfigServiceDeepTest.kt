/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * NetworkStorageConfigService Deep Tests
 *
 * Comprehensive deep tests for storage type enumeration,
 * configuration validation, and service state management.
 *
 *########################################################*/

package digital.vasic.yole.network.config

import digital.vasic.yole.network.common.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Deep test suite for NetworkStorageConfigService.
 * Covers getSupportedStorageTypes, validateConfiguration for all 8 types
 * (valid and invalid), createStorageService type dispatch, state management,
 * and refreshConnectionStatus.
 */
class NetworkStorageConfigServiceDeepTest {

    private fun createService() = NetworkStorageConfigService()

    // ==================== getSupportedStorageTypes ====================

    @Test
    fun `getSupportedStorageTypes returns exactly 8 types`() {
        val service = createService()
        val types = service.getSupportedStorageTypes()
        assertEquals(8, types.size)
    }

    @Test
    fun `getSupportedStorageTypes contains WebDAV`() {
        val types = createService().getSupportedStorageTypes()
        val webdav = types.find { it.type == StorageType.WEBDAV }
        assertNotNull(webdav)
        assertEquals("WebDAV", webdav.name)
        assertTrue(webdav.description.isNotBlank())
        assertTrue(webdav.supportedFeatures.contains(StorageFeature.FOLDERS))
        assertTrue(webdav.supportedFeatures.contains(StorageFeature.METADATA))
        assertTrue(webdav.supportedFeatures.contains(StorageFeature.VERSIONING))
        assertTrue(webdav.supportedFeatures.contains(StorageFeature.ENCRYPTION))
        assertTrue(webdav.popularServices.contains("Nextcloud"))
        assertTrue(webdav.popularServices.contains("ownCloud"))
    }

    @Test
    fun `getSupportedStorageTypes contains FTP`() {
        val types = createService().getSupportedStorageTypes()
        val ftp = types.find { it.type == StorageType.FTP }
        assertNotNull(ftp)
        assertEquals("FTP", ftp.name)
        assertTrue(ftp.supportedFeatures.contains(StorageFeature.FILE_OPERATIONS))
        assertEquals(1, ftp.supportedFeatures.size, "FTP should only support FILE_OPERATIONS")
    }

    @Test
    fun `getSupportedStorageTypes contains SFTP`() {
        val types = createService().getSupportedStorageTypes()
        val sftp = types.find { it.type == StorageType.SFTP }
        assertNotNull(sftp)
        assertEquals("SFTP", sftp.name)
        assertTrue(sftp.supportedFeatures.contains(StorageFeature.FILE_OPERATIONS))
        assertTrue(sftp.supportedFeatures.contains(StorageFeature.ENCRYPTION))
        assertTrue(sftp.supportedFeatures.contains(StorageFeature.PERMISSIONS))
    }

    @Test
    fun `getSupportedStorageTypes contains SMB`() {
        val types = createService().getSupportedStorageTypes()
        val smb = types.find { it.type == StorageType.SMB }
        assertNotNull(smb)
        assertEquals("SMB/CIFS", smb.name)
        assertTrue(smb.supportedFeatures.contains(StorageFeature.FOLDERS))
        assertTrue(smb.supportedFeatures.contains(StorageFeature.PERMISSIONS))
        assertTrue(smb.supportedFeatures.contains(StorageFeature.NETWORK_DISCOVERY))
    }

    @Test
    fun `getSupportedStorageTypes contains Google Drive`() {
        val types = createService().getSupportedStorageTypes()
        val gdrive = types.find { it.type == StorageType.GOOGLE_DRIVE }
        assertNotNull(gdrive)
        assertEquals("Google Drive", gdrive.name)
        assertTrue(gdrive.supportedFeatures.contains(StorageFeature.FOLDERS))
        assertTrue(gdrive.supportedFeatures.contains(StorageFeature.SEARCH))
        assertTrue(gdrive.supportedFeatures.contains(StorageFeature.COLLABORATION))
        assertTrue(gdrive.supportedFeatures.contains(StorageFeature.OFFLINE_SYNC))
    }

    @Test
    fun `getSupportedStorageTypes contains Dropbox`() {
        val types = createService().getSupportedStorageTypes()
        val dropbox = types.find { it.type == StorageType.DROPBOX }
        assertNotNull(dropbox)
        assertEquals("Dropbox", dropbox.name)
        assertTrue(dropbox.supportedFeatures.contains(StorageFeature.FOLDERS))
        assertTrue(dropbox.supportedFeatures.contains(StorageFeature.VERSIONING))
        assertTrue(dropbox.supportedFeatures.contains(StorageFeature.SHARING))
        assertTrue(dropbox.supportedFeatures.contains(StorageFeature.OFFLINE_SYNC))
    }

    @Test
    fun `getSupportedStorageTypes contains OneDrive`() {
        val types = createService().getSupportedStorageTypes()
        val onedrive = types.find { it.type == StorageType.ONEDRIVE }
        assertNotNull(onedrive)
        assertEquals("OneDrive", onedrive.name)
        assertTrue(onedrive.supportedFeatures.contains(StorageFeature.FOLDERS))
        assertTrue(onedrive.supportedFeatures.contains(StorageFeature.SEARCH))
        assertTrue(onedrive.supportedFeatures.contains(StorageFeature.COLLABORATION))
        assertTrue(onedrive.supportedFeatures.contains(StorageFeature.OFFICE_INTEGRATION))
    }

    @Test
    fun `getSupportedStorageTypes contains Git`() {
        val types = createService().getSupportedStorageTypes()
        val git = types.find { it.type == StorageType.GIT }
        assertNotNull(git)
        assertEquals("Git", git.name)
        assertTrue(git.supportedFeatures.contains(StorageFeature.VERSIONING))
        assertTrue(git.supportedFeatures.contains(StorageFeature.SEARCH))
        assertTrue(git.supportedFeatures.contains(StorageFeature.SYNC))
        assertTrue(git.supportedFeatures.contains(StorageFeature.BACKUP))
        assertTrue(git.supportedFeatures.contains(StorageFeature.ENCRYPTION))
        assertTrue(git.popularServices.contains("GitHub"))
        assertTrue(git.popularServices.contains("GitLab"))
    }

    @Test
    fun `getSupportedStorageTypes all entries have non-blank descriptions`() {
        val types = createService().getSupportedStorageTypes()
        types.forEach { info ->
            assertTrue(info.description.isNotBlank(), "${info.name} should have a non-blank description")
        }
    }

    @Test
    fun `getSupportedStorageTypes all entries have at least one popular service`() {
        val types = createService().getSupportedStorageTypes()
        types.forEach { info ->
            assertTrue(info.popularServices.isNotEmpty(), "${info.name} should list at least one popular service")
        }
    }

    @Test
    fun `getSupportedStorageTypes covers all StorageType enum values`() {
        val types = createService().getSupportedStorageTypes()
        val returnedTypes = types.map { it.type }.toSet()
        StorageType.entries.forEach { storageType ->
            assertTrue(
                storageType in returnedTypes,
                "StorageType.$storageType should be present in getSupportedStorageTypes()"
            )
        }
    }

    // ==================== validateConfiguration - VALID CONFIGS ====================

    @Test
    fun `validateConfiguration WebDAV valid https config returns null`() {
        val config = StorageConfig.WebDavConfig(
            name = "my-webdav",
            url = "https://cloud.example.com/remote.php/dav",
            username = "admin",
            password = "secret123"
        )
        assertNull(createService().validateConfiguration(config))
    }

    @Test
    fun `validateConfiguration WebDAV valid http config returns null`() {
        val config = StorageConfig.WebDavConfig(
            name = "my-webdav-http",
            url = "http://192.168.1.100/webdav",
            username = "user",
            password = "pass"
        )
        assertNull(createService().validateConfiguration(config))
    }

    @Test
    fun `validateConfiguration FTP valid config returns null`() {
        val config = StorageConfig.FtpConfig(
            name = "my-ftp",
            host = "ftp.example.com",
            port = 21,
            username = "ftpuser",
            password = "ftppass"
        )
        assertNull(createService().validateConfiguration(config))
    }

    @Test
    fun `validateConfiguration FTP valid with custom port returns null`() {
        val config = StorageConfig.FtpConfig(
            name = "custom-ftp",
            host = "ftp.example.com",
            port = 2121,
            username = "user",
            password = "pass"
        )
        assertNull(createService().validateConfiguration(config))
    }

    @Test
    fun `validateConfiguration SFTP valid with password auth returns null`() {
        val config = StorageConfig.SftpConfig(
            name = "sftp-password",
            host = "sftp.example.com",
            port = 22,
            username = "sftpuser",
            password = "sftppass"
        )
        assertNull(createService().validateConfiguration(config))
    }

    @Test
    fun `validateConfiguration SFTP valid with key auth returns null`() {
        val config = StorageConfig.SftpConfig(
            name = "sftp-key",
            host = "sftp.example.com",
            port = 22,
            username = "sftpuser",
            privateKeyPath = "/home/user/.ssh/id_rsa"
        )
        assertNull(createService().validateConfiguration(config))
    }

    @Test
    fun `validateConfiguration SMB valid config returns null`() {
        val config = StorageConfig.SmbConfig(
            name = "my-smb",
            host = "nas.local",
            share = "documents",
            username = "smbuser",
            password = "smbpass"
        )
        assertNull(createService().validateConfiguration(config))
    }

    @Test
    fun `validateConfiguration GoogleDrive valid with access token returns null`() {
        val config = StorageConfig.GoogleDriveConfig(
            name = "my-gdrive",
            clientId = "123456789.apps.googleusercontent.com",
            clientSecret = "GOCSPX-secret",
            accessToken = "ya29.access-token"
        )
        assertNull(createService().validateConfiguration(config))
    }

    @Test
    fun `validateConfiguration GoogleDrive valid with refresh token returns null`() {
        val config = StorageConfig.GoogleDriveConfig(
            name = "gdrive-refresh",
            clientId = "123456789.apps.googleusercontent.com",
            clientSecret = "GOCSPX-secret",
            refreshToken = "1//refresh-token"
        )
        assertNull(createService().validateConfiguration(config))
    }

    @Test
    fun `validateConfiguration Dropbox valid config returns null`() {
        val config = StorageConfig.DropboxConfig(
            name = "my-dropbox",
            accessToken = "sl.access-token",
            appKey = "abcdef12345",
            appSecret = "xyz98765"
        )
        assertNull(createService().validateConfiguration(config))
    }

    @Test
    fun `validateConfiguration OneDrive valid with access token returns null`() {
        val config = StorageConfig.OneDriveConfig(
            name = "my-onedrive",
            clientId = "aaaabbbb-cccc-dddd-eeee-ffffgggghhhh",
            clientSecret = "secret~value",
            accessToken = "eyJ0eXAi.access-token"
        )
        assertNull(createService().validateConfiguration(config))
    }

    @Test
    fun `validateConfiguration OneDrive valid with refresh token returns null`() {
        val config = StorageConfig.OneDriveConfig(
            name = "onedrive-refresh",
            clientId = "aaaabbbb-cccc-dddd-eeee-ffffgggghhhh",
            clientSecret = "secret~value",
            refreshToken = "M.refresh-token"
        )
        assertNull(createService().validateConfiguration(config))
    }

    @Test
    fun `validateConfiguration Git valid HTTPS config returns null`() {
        val config = StorageConfig.GitConfig(
            name = "my-git",
            repositoryUrl = "https://github.com/user/repo.git",
            localCachePath = "/tmp/yole-git-cache"
        )
        assertNull(createService().validateConfiguration(config))
    }

    @Test
    fun `validateConfiguration Git valid SSH config returns null`() {
        val config = StorageConfig.GitConfig(
            name = "git-ssh",
            repositoryUrl = "git@github.com:user/repo.git",
            localCachePath = "/tmp/yole-git-cache"
        )
        assertNull(createService().validateConfiguration(config))
    }

    // ==================== validateConfiguration - INVALID CONFIGS ====================

    @Test
    fun `validateConfiguration WebDAV blank URL returns error`() {
        val config = StorageConfig.WebDavConfig(name = "wd", url = "", username = "u", password = "p")
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("URL is required", error.message)
    }

    @Test
    fun `validateConfiguration WebDAV blank username returns error`() {
        val config = StorageConfig.WebDavConfig(name = "wd", url = "https://x.com/dav", username = "", password = "p")
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Username is required", error.message)
    }

    @Test
    fun `validateConfiguration WebDAV blank password returns error`() {
        val config = StorageConfig.WebDavConfig(name = "wd", url = "https://x.com/dav", username = "u", password = "")
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Password is required", error.message)
    }

    @Test
    fun `validateConfiguration WebDAV invalid URL scheme returns error`() {
        val config = StorageConfig.WebDavConfig(name = "wd", url = "ftp://x.com/dav", username = "u", password = "p")
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("URL must start with http:// or https://", error.message)
    }

    @Test
    fun `validateConfiguration FTP blank host returns error`() {
        val config = StorageConfig.FtpConfig(name = "ftp", host = "", port = 21, username = "u", password = "p")
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Host is required", error.message)
    }

    @Test
    fun `validateConfiguration FTP port zero returns error`() {
        val config = StorageConfig.FtpConfig(name = "ftp", host = "host", port = 0, username = "u", password = "p")
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Port must be between 1 and 65535", error.message)
    }

    @Test
    fun `validateConfiguration FTP port above 65535 returns error`() {
        val config = StorageConfig.FtpConfig(name = "ftp", host = "host", port = 70000, username = "u", password = "p")
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Port must be between 1 and 65535", error.message)
    }

    @Test
    fun `validateConfiguration FTP blank username returns error`() {
        val config = StorageConfig.FtpConfig(name = "ftp", host = "host", port = 21, username = "", password = "p")
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Username is required", error.message)
    }

    @Test
    fun `validateConfiguration FTP blank password returns error`() {
        val config = StorageConfig.FtpConfig(name = "ftp", host = "host", port = 21, username = "u", password = "")
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Password is required", error.message)
    }

    @Test
    fun `validateConfiguration SFTP blank host returns error`() {
        val config = StorageConfig.SftpConfig(name = "sftp", host = "", username = "u", password = "p")
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Host is required", error.message)
    }

    @Test
    fun `validateConfiguration SFTP null username returns error`() {
        val config = StorageConfig.SftpConfig(name = "sftp", host = "h", username = null, password = "p")
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Username is required", error.message)
    }

    @Test
    fun `validateConfiguration SFTP blank username returns error`() {
        val config = StorageConfig.SftpConfig(name = "sftp", host = "h", username = "", password = "p")
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Username is required", error.message)
    }

    @Test
    fun `validateConfiguration SFTP no auth returns error`() {
        val config = StorageConfig.SftpConfig(
            name = "sftp", host = "h", username = "u",
            password = null, privateKeyPath = null
        )
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Either password or private key is required", error.message)
    }

    @Test
    fun `validateConfiguration SFTP invalid port returns error`() {
        val config = StorageConfig.SftpConfig(name = "sftp", host = "h", port = 0, username = "u", password = "p")
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Port must be between 1 and 65535", error.message)
    }

    @Test
    fun `validateConfiguration SMB blank host returns error`() {
        val config = StorageConfig.SmbConfig(name = "smb", host = "", share = "s", username = "u", password = "p")
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Host is required", error.message)
    }

    @Test
    fun `validateConfiguration SMB blank share returns error`() {
        val config = StorageConfig.SmbConfig(name = "smb", host = "h", share = "", username = "u", password = "p")
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Share name is required", error.message)
    }

    @Test
    fun `validateConfiguration SMB blank username returns error`() {
        val config = StorageConfig.SmbConfig(name = "smb", host = "h", share = "s", username = "", password = "p")
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Username is required", error.message)
    }

    @Test
    fun `validateConfiguration SMB blank password returns error`() {
        val config = StorageConfig.SmbConfig(name = "smb", host = "h", share = "s", username = "u", password = "")
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Password is required", error.message)
    }

    @Test
    fun `validateConfiguration GoogleDrive blank clientId returns error`() {
        val config = StorageConfig.GoogleDriveConfig(
            name = "gd", clientId = "", clientSecret = "s", accessToken = "t"
        )
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Client ID is required", error.message)
    }

    @Test
    fun `validateConfiguration GoogleDrive blank clientSecret returns error`() {
        val config = StorageConfig.GoogleDriveConfig(
            name = "gd", clientId = "c", clientSecret = "", accessToken = "t"
        )
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Client secret is required", error.message)
    }

    @Test
    fun `validateConfiguration GoogleDrive no tokens returns error`() {
        val config = StorageConfig.GoogleDriveConfig(
            name = "gd", clientId = "c", clientSecret = "s",
            accessToken = null, refreshToken = null
        )
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Either access token or refresh token is required", error.message)
    }

    @Test
    fun `validateConfiguration Dropbox blank accessToken returns error`() {
        val config = StorageConfig.DropboxConfig(
            name = "db", accessToken = "", appKey = "k", appSecret = "s"
        )
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Access token is required", error.message)
    }

    @Test
    fun `validateConfiguration Dropbox blank appKey returns error`() {
        val config = StorageConfig.DropboxConfig(
            name = "db", accessToken = "t", appKey = "", appSecret = "s"
        )
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("App key is required", error.message)
    }

    @Test
    fun `validateConfiguration Dropbox blank appSecret returns error`() {
        val config = StorageConfig.DropboxConfig(
            name = "db", accessToken = "t", appKey = "k", appSecret = ""
        )
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("App secret is required", error.message)
    }

    @Test
    fun `validateConfiguration OneDrive blank clientId returns error`() {
        val config = StorageConfig.OneDriveConfig(
            name = "od", clientId = "", clientSecret = "s", accessToken = "t"
        )
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Client ID is required", error.message)
    }

    @Test
    fun `validateConfiguration OneDrive blank clientSecret returns error`() {
        val config = StorageConfig.OneDriveConfig(
            name = "od", clientId = "c", clientSecret = "", accessToken = "t"
        )
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Client secret is required", error.message)
    }

    @Test
    fun `validateConfiguration OneDrive no tokens returns error`() {
        val config = StorageConfig.OneDriveConfig(
            name = "od", clientId = "c", clientSecret = "s",
            accessToken = null, refreshToken = null
        )
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Either access token or refresh token is required", error.message)
    }

    @Test
    fun `validateConfiguration Git blank repositoryUrl returns error`() {
        val config = StorageConfig.GitConfig(
            name = "git", repositoryUrl = "", localCachePath = "/tmp/cache"
        )
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Repository URL is required", error.message)
    }

    @Test
    fun `validateConfiguration Git blank localCachePath returns error`() {
        val config = StorageConfig.GitConfig(
            name = "git", repositoryUrl = "https://github.com/user/repo.git", localCachePath = ""
        )
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Local cache path is required", error.message)
    }

    @Test
    fun `validateConfiguration Git invalid URL scheme returns error`() {
        val config = StorageConfig.GitConfig(
            name = "git", repositoryUrl = "ftp://badscheme.com/repo.git", localCachePath = "/tmp/cache"
        )
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Repository URL must start with http(s):// or git@", error.message)
    }

    // ==================== INITIAL STATE ====================

    @Test
    fun `initial state has no configured storages`() {
        val service = createService()
        assertTrue(service.configuredStorages.value.isEmpty())
    }

    @Test
    fun `initial state has no active storage`() {
        val service = createService()
        assertNull(service.activeStorage.value)
    }

    @Test
    fun `initial state has empty connection status map`() {
        val service = createService()
        assertTrue(service.connectionStatus.value.isEmpty())
    }

    // ==================== STATE MANAGEMENT ====================

    @Test
    fun `removeStorage on non-existent storage returns failure`() = runBlocking {
        val service = createService()
        val result = service.removeStorage("non-existent-id")
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception.message?.contains("Storage not found") == true)
    }

    @Test
    fun `setActiveStorage on non-existent storage returns failure`() = runBlocking {
        val service = createService()
        val result = service.setActiveStorage("non-existent-id")
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception.message?.contains("Storage not found") == true)
    }

    // ==================== refreshConnectionStatus ====================

    @Test
    fun `refreshConnectionStatus succeeds with empty storages`() = runBlocking {
        val service = createService()
        val result = service.refreshConnectionStatus()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `refreshConnectionStatus returns empty map when no storages configured`() = runBlocking {
        val service = createService()
        service.refreshConnectionStatus()
        assertTrue(service.connectionStatus.value.isEmpty())
    }

    // ==================== VALIDATION EDGE CASES ====================

    @Test
    fun `validateConfiguration FTP port boundary 1 is valid`() {
        val config = StorageConfig.FtpConfig(name = "ftp", host = "h", port = 1, username = "u", password = "p")
        assertNull(createService().validateConfiguration(config))
    }

    @Test
    fun `validateConfiguration FTP port boundary 65535 is valid`() {
        val config = StorageConfig.FtpConfig(name = "ftp", host = "h", port = 65535, username = "u", password = "p")
        assertNull(createService().validateConfiguration(config))
    }

    @Test
    fun `validateConfiguration FTP negative port returns error`() {
        val config = StorageConfig.FtpConfig(name = "ftp", host = "h", port = -1, username = "u", password = "p")
        val error = createService().validateConfiguration(config)
        assertNotNull(error)
        assertEquals("Port must be between 1 and 65535", error.message)
    }

    @Test
    fun `validateConfiguration SFTP port boundary 1 is valid`() {
        val config = StorageConfig.SftpConfig(name = "sftp", host = "h", port = 1, username = "u", password = "p")
        assertNull(createService().validateConfiguration(config))
    }

    @Test
    fun `validateConfiguration SFTP port boundary 65535 is valid`() {
        val config = StorageConfig.SftpConfig(name = "sftp", host = "h", port = 65535, username = "u", password = "p")
        assertNull(createService().validateConfiguration(config))
    }

    @Test
    fun `validateConfiguration SFTP with both password and key auth is valid`() {
        val config = StorageConfig.SftpConfig(
            name = "sftp-both",
            host = "sftp.example.com",
            port = 22,
            username = "user",
            password = "pass",
            privateKeyPath = "/home/user/.ssh/id_rsa"
        )
        assertNull(createService().validateConfiguration(config))
    }

    @Test
    fun `validateConfiguration GoogleDrive with both tokens is valid`() {
        val config = StorageConfig.GoogleDriveConfig(
            name = "gd-both",
            clientId = "client",
            clientSecret = "secret",
            accessToken = "access",
            refreshToken = "refresh"
        )
        assertNull(createService().validateConfiguration(config))
    }

    @Test
    fun `validateConfiguration OneDrive with both tokens is valid`() {
        val config = StorageConfig.OneDriveConfig(
            name = "od-both",
            clientId = "client",
            clientSecret = "secret",
            accessToken = "access",
            refreshToken = "refresh"
        )
        assertNull(createService().validateConfiguration(config))
    }
}
