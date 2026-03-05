/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for StorageConfig sealed class hierarchy and related enums
 *
 *########################################################*/
package digital.vasic.yole.network.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StorageConfigWebDavTest {

    @Test
    fun testWebDavConfigCreation() {
        val config = StorageConfig.WebDavConfig(
            name = "My WebDAV",
            url = "https://dav.example.com",
            username = "user",
            password = "pass"
        )
        assertEquals("My WebDAV", config.name)
        assertEquals("https://dav.example.com", config.url)
        assertEquals(StorageType.WEBDAV, config.storageType)
        assertTrue(config.isEnabled)
        assertEquals(100, config.priority)
    }

    @Test
    fun testWebDavConfigDefaults() {
        val config = StorageConfig.WebDavConfig(
            name = "Test", url = "https://test.com",
            username = "u", password = "p"
        )
        assertEquals(WebDavAuthenticationType.BASIC, config.authenticationType)
        assertTrue(config.sslEnabled)
        assertTrue(config.verifyCertificate)
        assertEquals(30000, config.connectionTimeout)
        assertEquals(60000, config.readTimeout)
    }

    @Test
    fun testWebDavWithEnabled() {
        val config = StorageConfig.WebDavConfig(
            name = "Test", url = "https://test.com",
            username = "u", password = "p"
        )
        val disabled = config.withEnabled(false)
        assertFalse(disabled.isEnabled)
    }

    @Test
    fun testWebDavWithPriority() {
        val config = StorageConfig.WebDavConfig(
            name = "Test", url = "https://test.com",
            username = "u", password = "p"
        )
        val updated = config.withPriority(50)
        assertEquals(50, updated.priority)
    }

    @Test
    fun testWebDavWithMetadata() {
        val config = StorageConfig.WebDavConfig(
            name = "Test", url = "https://test.com",
            username = "u", password = "p"
        )
        val updated = config.withMetadata(mapOf("key" to "value"))
        assertEquals("value", (updated as StorageConfig.WebDavConfig).metadata["key"])
    }
}

class StorageConfigFtpTest {

    @Test
    fun testFtpConfigCreation() {
        val config = StorageConfig.FtpConfig(
            name = "My FTP",
            host = "ftp.example.com",
            username = "user",
            password = "pass"
        )
        assertEquals(StorageType.FTP, config.storageType)
        assertEquals(21, config.port)
        assertEquals("/", config.rootPath)
        assertTrue(config.passiveMode)
        assertFalse(config.secureFtp)
        assertEquals("UTF-8", config.encoding)
    }

    @Test
    fun testFtpWithEnabled() {
        val config = StorageConfig.FtpConfig(
            name = "Test", host = "ftp.test.com",
            username = "u", password = "p"
        )
        val disabled = config.withEnabled(false)
        assertFalse(disabled.isEnabled)
    }
}

class StorageConfigSftpTest {

    @Test
    fun testSftpConfigCreation() {
        val config = StorageConfig.SftpConfig(
            name = "My SFTP",
            host = "sftp.example.com"
        )
        assertEquals(StorageType.SFTP, config.storageType)
        assertEquals(22, config.port)
        assertNull(config.username)
        assertNull(config.password)
        assertNull(config.privateKeyPath)
        assertTrue(config.strictHostKeyChecking)
    }

    @Test
    fun testSftpConfigWithKeyAuth() {
        val config = StorageConfig.SftpConfig(
            name = "Key SFTP",
            host = "sftp.example.com",
            privateKeyPath = "/home/user/.ssh/id_rsa",
            privateKeyPassphrase = "secret"
        )
        assertEquals("/home/user/.ssh/id_rsa", config.privateKeyPath)
        assertEquals("secret", config.privateKeyPassphrase)
    }
}

class StorageConfigSmbTest {

    @Test
    fun testSmbConfigCreation() {
        val config = StorageConfig.SmbConfig(
            name = "My SMB",
            host = "smb.example.com",
            share = "documents",
            username = "user",
            password = "pass"
        )
        assertEquals(StorageType.SMB, config.storageType)
        assertEquals(445, config.port)
        assertEquals("documents", config.share)
        assertNull(config.domain)
        assertTrue(config.encryption)
        assertTrue(config.signing)
    }
}

class StorageConfigCloudTest {

    @Test
    fun testGoogleDriveConfig() {
        val config = StorageConfig.GoogleDriveConfig(
            name = "GDrive",
            clientId = "id123",
            clientSecret = "secret456"
        )
        assertEquals(StorageType.GOOGLE_DRIVE, config.storageType)
        assertNull(config.refreshToken)
        assertNull(config.rootFolderId)
        assertNull(config.teamDriveId)
    }

    @Test
    fun testDropboxConfig() {
        val config = StorageConfig.DropboxConfig(
            name = "Dropbox",
            accessToken = "token",
            appKey = "key",
            appSecret = "secret"
        )
        assertEquals(StorageType.DROPBOX, config.storageType)
        assertEquals("", config.rootPath)
        assertNull(config.refreshToken)
    }

    @Test
    fun testOneDriveConfig() {
        val config = StorageConfig.OneDriveConfig(
            name = "OneDrive",
            clientId = "id",
            clientSecret = "secret"
        )
        assertEquals(StorageType.ONEDRIVE, config.storageType)
        assertEquals(OneDriveDriveType.ME, config.driveType)
        assertNull(config.driveId)
    }

    @Test
    fun testOneDriveBusinessConfig() {
        val config = StorageConfig.OneDriveConfig(
            name = "OneDrive Business",
            clientId = "id",
            clientSecret = "secret",
            driveType = OneDriveDriveType.BUSINESS
        )
        assertEquals(OneDriveDriveType.BUSINESS, config.driveType)
    }

    @Test
    fun testGitConfig() {
        val config = StorageConfig.GitConfig(
            name = "GitHub",
            repositoryUrl = "https://github.com/user/repo.git",
            localCachePath = "/tmp/cache"
        )
        assertEquals(StorageType.GIT, config.storageType)
        assertEquals("main", config.branch)
        assertTrue(config.autoSync)
        assertEquals("Yole", config.commitAuthorName)
    }

    @Test
    fun testGitConfigWithPat() {
        val config = StorageConfig.GitConfig(
            name = "GitHub",
            repositoryUrl = "https://github.com/user/repo.git",
            personalAccessToken = "ghp_xxx",
            localCachePath = "/tmp/cache"
        )
        assertEquals("ghp_xxx", config.personalAccessToken)
    }
}

class StorageTypeTest {

    @Test
    fun testDisplayNames() {
        assertEquals("WebDAV", StorageType.WEBDAV.displayName)
        assertEquals("FTP", StorageType.FTP.displayName)
        assertEquals("SFTP", StorageType.SFTP.displayName)
        assertEquals("SMB/CIFS", StorageType.SMB.displayName)
        assertEquals("Google Drive", StorageType.GOOGLE_DRIVE.displayName)
        assertEquals("Dropbox", StorageType.DROPBOX.displayName)
        assertEquals("OneDrive", StorageType.ONEDRIVE.displayName)
        assertEquals("Git", StorageType.GIT.displayName)
    }

    @Test
    fun testSupportsFolders() {
        assertTrue(StorageType.WEBDAV.supportsFolders)
        assertFalse(StorageType.FTP.supportsFolders)
        assertTrue(StorageType.SFTP.supportsFolders)
        assertTrue(StorageType.GOOGLE_DRIVE.supportsFolders)
    }

    @Test
    fun testSupportsEncryption() {
        assertTrue(StorageType.WEBDAV.supportsEncryption)
        assertFalse(StorageType.FTP.supportsEncryption)
        assertTrue(StorageType.SFTP.supportsEncryption)
    }

    @Test
    fun testDefaultPorts() {
        assertEquals(443, StorageType.WEBDAV.defaultPort)
        assertEquals(21, StorageType.FTP.defaultPort)
        assertEquals(22, StorageType.SFTP.defaultPort)
        assertEquals(445, StorageType.SMB.defaultPort)
        assertEquals(443, StorageType.GOOGLE_DRIVE.defaultPort)
        assertEquals(443, StorageType.DROPBOX.defaultPort)
        assertEquals(443, StorageType.ONEDRIVE.defaultPort)
        assertEquals(22, StorageType.GIT.defaultPort)
    }

    @Test
    fun testAllStorageTypesExist() {
        assertEquals(8, StorageType.entries.size)
    }
}

class WebDavAuthenticationTypeTest {

    @Test
    fun testAllAuthTypes() {
        assertEquals(4, WebDavAuthenticationType.entries.size)
        WebDavAuthenticationType.BASIC
        WebDavAuthenticationType.DIGEST
        WebDavAuthenticationType.OAUTH
        WebDavAuthenticationType.NONE
    }
}

class OneDriveDriveTypeTest {

    @Test
    fun testAllDriveTypes() {
        assertEquals(4, OneDriveDriveType.entries.size)
        OneDriveDriveType.ME
        OneDriveDriveType.BUSINESS
        OneDriveDriveType.SHAREPOINT
        OneDriveDriveType.GROUP
    }
}
