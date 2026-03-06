/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for StorageConfig validation and creation
 *
 *########################################################*/
package digital.vasic.yole.network.config

import digital.vasic.yole.network.common.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StorageConfigValidationTest {

    // ====================================================================
    // Creating each StorageConfig type with valid data
    // ====================================================================

    @Test
    fun createWebDavConfigWithValidData() {
        val config = StorageConfig.WebDavConfig(
            name = "My WebDAV",
            url = "https://dav.example.com/files",
            username = "admin",
            password = "secret123"
        )
        assertNotNull(config)
        assertEquals("My WebDAV", config.name)
        assertEquals("https://dav.example.com/files", config.url)
        assertEquals("admin", config.username)
        assertEquals("secret123", config.password)
        assertEquals(StorageType.WEBDAV, config.storageType)
        assertEquals(WebDavAuthenticationType.BASIC, config.authenticationType)
        assertTrue(config.sslEnabled)
        assertTrue(config.verifyCertificate)
        assertEquals(30000, config.connectionTimeout)
        assertEquals(60000, config.readTimeout)
        assertTrue(config.isEnabled)
        assertEquals(100, config.priority)
        assertTrue(config.metadata.isEmpty())
    }

    @Test
    fun createFtpConfigWithValidData() {
        val config = StorageConfig.FtpConfig(
            name = "My FTP",
            host = "ftp.example.com",
            username = "ftpuser",
            password = "ftppass"
        )
        assertNotNull(config)
        assertEquals("My FTP", config.name)
        assertEquals("ftp.example.com", config.host)
        assertEquals(21, config.port)
        assertEquals("ftpuser", config.username)
        assertEquals("ftppass", config.password)
        assertEquals("/", config.rootPath)
        assertTrue(config.passiveMode)
        assertEquals(false, config.secureFtp)
        assertEquals("UTF-8", config.encoding)
        assertEquals(30000, config.connectionTimeout)
        assertEquals(StorageType.FTP, config.storageType)
        assertTrue(config.isEnabled)
        assertEquals(100, config.priority)
        assertTrue(config.metadata.isEmpty())
    }

    @Test
    fun createSftpConfigWithValidData() {
        val config = StorageConfig.SftpConfig(
            name = "My SFTP",
            host = "sftp.example.com",
            username = "sftpuser",
            password = "sftppass"
        )
        assertNotNull(config)
        assertEquals("My SFTP", config.name)
        assertEquals("sftp.example.com", config.host)
        assertEquals(22, config.port)
        assertEquals("sftpuser", config.username)
        assertEquals("sftppass", config.password)
        assertNull(config.privateKeyPath)
        assertNull(config.privateKeyPassphrase)
        assertNull(config.knownHostsPath)
        assertTrue(config.strictHostKeyChecking)
        assertEquals("/", config.rootPath)
        assertTrue(config.useSsl)
        assertEquals(30000, config.connectionTimeout)
        assertEquals(StorageType.SFTP, config.storageType)
        assertTrue(config.isEnabled)
        assertEquals(100, config.priority)
        assertTrue(config.metadata.isEmpty())
    }

    @Test
    fun createSmbConfigWithValidData() {
        val config = StorageConfig.SmbConfig(
            name = "My SMB",
            host = "nas.local",
            share = "documents",
            username = "smbuser",
            password = "smbpass"
        )
        assertNotNull(config)
        assertEquals("My SMB", config.name)
        assertEquals("nas.local", config.host)
        assertEquals("documents", config.share)
        assertNull(config.domain)
        assertEquals("smbuser", config.username)
        assertEquals("smbpass", config.password)
        assertEquals("/", config.path)
        assertEquals(445, config.port)
        assertTrue(config.encryption)
        assertTrue(config.signing)
        assertEquals(false, config.useSsl)
        assertEquals(30000, config.connectionTimeout)
        assertEquals(StorageType.SMB, config.storageType)
        assertTrue(config.isEnabled)
        assertEquals(100, config.priority)
        assertTrue(config.metadata.isEmpty())
    }

    @Test
    fun createGoogleDriveConfigWithValidData() {
        val config = StorageConfig.GoogleDriveConfig(
            name = "My Google Drive",
            clientId = "client-id-123",
            clientSecret = "client-secret-456",
            accessToken = "access-token-789"
        )
        assertNotNull(config)
        assertEquals("My Google Drive", config.name)
        assertEquals("client-id-123", config.clientId)
        assertEquals("client-secret-456", config.clientSecret)
        assertEquals("access-token-789", config.accessToken)
        assertNull(config.refreshToken)
        assertNull(config.rootFolderId)
        assertNull(config.teamDriveId)
        assertEquals(StorageType.GOOGLE_DRIVE, config.storageType)
        assertTrue(config.isEnabled)
        assertEquals(100, config.priority)
        assertTrue(config.metadata.isEmpty())
    }

    @Test
    fun createDropboxConfigWithValidData() {
        val config = StorageConfig.DropboxConfig(
            name = "My Dropbox",
            accessToken = "dbx-token-123",
            appKey = "app-key-456",
            appSecret = "app-secret-789"
        )
        assertNotNull(config)
        assertEquals("My Dropbox", config.name)
        assertEquals("dbx-token-123", config.accessToken)
        assertEquals("app-key-456", config.appKey)
        assertEquals("app-secret-789", config.appSecret)
        assertNull(config.refreshToken)
        assertEquals("", config.rootPath)
        assertEquals(StorageType.DROPBOX, config.storageType)
        assertTrue(config.isEnabled)
        assertEquals(100, config.priority)
        assertTrue(config.metadata.isEmpty())
    }

    @Test
    fun createOneDriveConfigWithValidData() {
        val config = StorageConfig.OneDriveConfig(
            name = "My OneDrive",
            clientId = "od-client-id",
            clientSecret = "od-client-secret",
            refreshToken = "od-refresh-token"
        )
        assertNotNull(config)
        assertEquals("My OneDrive", config.name)
        assertEquals("od-client-id", config.clientId)
        assertEquals("od-client-secret", config.clientSecret)
        assertEquals("od-refresh-token", config.refreshToken)
        assertNull(config.accessToken)
        assertEquals(OneDriveDriveType.ME, config.driveType)
        assertNull(config.driveId)
        assertNull(config.rootFolderId)
        assertEquals(StorageType.ONEDRIVE, config.storageType)
        assertTrue(config.isEnabled)
        assertEquals(100, config.priority)
        assertTrue(config.metadata.isEmpty())
    }

    @Test
    fun createGitConfigWithValidData() {
        val config = StorageConfig.GitConfig(
            name = "My Git Repo",
            repositoryUrl = "https://github.com/user/repo.git",
            localCachePath = "/tmp/yole-cache/repo"
        )
        assertNotNull(config)
        assertEquals("My Git Repo", config.name)
        assertEquals("https://github.com/user/repo.git", config.repositoryUrl)
        assertEquals("main", config.branch)
        assertNull(config.username)
        assertNull(config.password)
        assertNull(config.personalAccessToken)
        assertNull(config.privateKeyPath)
        assertNull(config.privateKeyPassphrase)
        assertEquals("/tmp/yole-cache/repo", config.localCachePath)
        assertTrue(config.autoSync)
        assertEquals("Yole", config.commitAuthorName)
        assertEquals("yole@example.com", config.commitAuthorEmail)
        assertEquals(30000, config.connectionTimeout)
        assertEquals(StorageType.GIT, config.storageType)
        assertTrue(config.isEnabled)
        assertEquals(100, config.priority)
        assertTrue(config.metadata.isEmpty())
    }

    // ====================================================================
    // StorageConfig property access (storageType, name, etc.)
    // ====================================================================

    @Test
    fun storageTypePropertyReturnsCorrectValues() {
        val webdav = StorageConfig.WebDavConfig(name = "w", url = "https://x.com", username = "u", password = "p")
        val ftp = StorageConfig.FtpConfig(name = "f", host = "h", username = "u", password = "p")
        val sftp = StorageConfig.SftpConfig(name = "s", host = "h", username = "u", password = "p")
        val smb = StorageConfig.SmbConfig(name = "m", host = "h", share = "s", username = "u", password = "p")
        val gdrive = StorageConfig.GoogleDriveConfig(name = "g", clientId = "c", clientSecret = "s", accessToken = "t")
        val dropbox = StorageConfig.DropboxConfig(name = "d", accessToken = "t", appKey = "k", appSecret = "s")
        val onedrive = StorageConfig.OneDriveConfig(name = "o", clientId = "c", clientSecret = "s", accessToken = "t")
        val git = StorageConfig.GitConfig(name = "r", repositoryUrl = "https://github.com/u/r.git", localCachePath = "/tmp/c")

        assertEquals(StorageType.WEBDAV, webdav.storageType)
        assertEquals(StorageType.FTP, ftp.storageType)
        assertEquals(StorageType.SFTP, sftp.storageType)
        assertEquals(StorageType.SMB, smb.storageType)
        assertEquals(StorageType.GOOGLE_DRIVE, gdrive.storageType)
        assertEquals(StorageType.DROPBOX, dropbox.storageType)
        assertEquals(StorageType.ONEDRIVE, onedrive.storageType)
        assertEquals(StorageType.GIT, git.storageType)
    }

    @Test
    fun namePropertyIsAccessibleAcrossAllTypes() {
        val configs: List<StorageConfig> = listOf(
            StorageConfig.WebDavConfig(name = "WebDAV-1", url = "https://x.com", username = "u", password = "p"),
            StorageConfig.FtpConfig(name = "FTP-1", host = "h", username = "u", password = "p"),
            StorageConfig.SftpConfig(name = "SFTP-1", host = "h", username = "u", password = "p"),
            StorageConfig.SmbConfig(name = "SMB-1", host = "h", share = "s", username = "u", password = "p"),
            StorageConfig.GoogleDriveConfig(name = "GDrive-1", clientId = "c", clientSecret = "s", accessToken = "t"),
            StorageConfig.DropboxConfig(name = "Dropbox-1", accessToken = "t", appKey = "k", appSecret = "s"),
            StorageConfig.OneDriveConfig(name = "OneDrive-1", clientId = "c", clientSecret = "s", accessToken = "t"),
            StorageConfig.GitConfig(name = "Git-1", repositoryUrl = "https://github.com/u/r.git", localCachePath = "/tmp/c")
        )

        val expectedNames = listOf("WebDAV-1", "FTP-1", "SFTP-1", "SMB-1", "GDrive-1", "Dropbox-1", "OneDrive-1", "Git-1")
        configs.forEachIndexed { index, config ->
            assertEquals(expectedNames[index], config.name)
        }
    }

    @Test
    fun isEnabledDefaultsToTrueForAllTypes() {
        val configs: List<StorageConfig> = listOf(
            StorageConfig.WebDavConfig(name = "w", url = "https://x.com", username = "u", password = "p"),
            StorageConfig.FtpConfig(name = "f", host = "h", username = "u", password = "p"),
            StorageConfig.SftpConfig(name = "s", host = "h", username = "u", password = "p"),
            StorageConfig.SmbConfig(name = "m", host = "h", share = "s", username = "u", password = "p"),
            StorageConfig.GoogleDriveConfig(name = "g", clientId = "c", clientSecret = "s", accessToken = "t"),
            StorageConfig.DropboxConfig(name = "d", accessToken = "t", appKey = "k", appSecret = "s"),
            StorageConfig.OneDriveConfig(name = "o", clientId = "c", clientSecret = "s", accessToken = "t"),
            StorageConfig.GitConfig(name = "r", repositoryUrl = "https://github.com/u/r.git", localCachePath = "/tmp/c")
        )
        configs.forEach { config ->
            assertTrue(config.isEnabled, "isEnabled should default to true for ${config.storageType}")
        }
    }

    @Test
    fun priorityDefaultsTo100ForAllTypes() {
        val configs: List<StorageConfig> = listOf(
            StorageConfig.WebDavConfig(name = "w", url = "https://x.com", username = "u", password = "p"),
            StorageConfig.FtpConfig(name = "f", host = "h", username = "u", password = "p"),
            StorageConfig.SftpConfig(name = "s", host = "h", username = "u", password = "p"),
            StorageConfig.SmbConfig(name = "m", host = "h", share = "s", username = "u", password = "p"),
            StorageConfig.GoogleDriveConfig(name = "g", clientId = "c", clientSecret = "s", accessToken = "t"),
            StorageConfig.DropboxConfig(name = "d", accessToken = "t", appKey = "k", appSecret = "s"),
            StorageConfig.OneDriveConfig(name = "o", clientId = "c", clientSecret = "s", accessToken = "t"),
            StorageConfig.GitConfig(name = "r", repositoryUrl = "https://github.com/u/r.git", localCachePath = "/tmp/c")
        )
        configs.forEach { config ->
            assertEquals(100, config.priority, "priority should default to 100 for ${config.storageType}")
        }
    }

    @Test
    fun metadataDefaultsToEmptyMapForAllTypes() {
        val configs: List<StorageConfig> = listOf(
            StorageConfig.WebDavConfig(name = "w", url = "https://x.com", username = "u", password = "p"),
            StorageConfig.FtpConfig(name = "f", host = "h", username = "u", password = "p"),
            StorageConfig.SftpConfig(name = "s", host = "h", username = "u", password = "p"),
            StorageConfig.SmbConfig(name = "m", host = "h", share = "s", username = "u", password = "p"),
            StorageConfig.GoogleDriveConfig(name = "g", clientId = "c", clientSecret = "s", accessToken = "t"),
            StorageConfig.DropboxConfig(name = "d", accessToken = "t", appKey = "k", appSecret = "s"),
            StorageConfig.OneDriveConfig(name = "o", clientId = "c", clientSecret = "s", accessToken = "t"),
            StorageConfig.GitConfig(name = "r", repositoryUrl = "https://github.com/u/r.git", localCachePath = "/tmp/c")
        )
        configs.forEach { config ->
            assertTrue(config.metadata.isEmpty(), "metadata should default to empty for ${config.storageType}")
        }
    }

    // ====================================================================
    // StorageConfig equality and copy
    // ====================================================================

    @Test
    fun webDavConfigEqualityAndCopy() {
        val config = StorageConfig.WebDavConfig(
            name = "dav",
            url = "https://dav.example.com",
            username = "user",
            password = "pass"
        )
        val same = StorageConfig.WebDavConfig(
            name = "dav",
            url = "https://dav.example.com",
            username = "user",
            password = "pass"
        )
        assertEquals(config, same)

        val copied = config.copy(name = "dav-copy")
        assertEquals("dav-copy", copied.name)
        assertEquals(config.url, copied.url)
        assertEquals(config.username, copied.username)
        assertEquals(config.password, copied.password)
    }

    @Test
    fun ftpConfigEqualityAndCopy() {
        val config = StorageConfig.FtpConfig(
            name = "ftp1",
            host = "ftp.example.com",
            username = "user",
            password = "pass"
        )
        val same = StorageConfig.FtpConfig(
            name = "ftp1",
            host = "ftp.example.com",
            username = "user",
            password = "pass"
        )
        assertEquals(config, same)

        val copied = config.copy(port = 2121)
        assertEquals(2121, copied.port)
        assertEquals(config.host, copied.host)
    }

    @Test
    fun sftpConfigEqualityAndCopy() {
        val config = StorageConfig.SftpConfig(
            name = "sftp1",
            host = "sftp.example.com",
            username = "user",
            password = "pass"
        )
        val same = StorageConfig.SftpConfig(
            name = "sftp1",
            host = "sftp.example.com",
            username = "user",
            password = "pass"
        )
        assertEquals(config, same)

        val copied = config.copy(port = 2222, strictHostKeyChecking = false)
        assertEquals(2222, copied.port)
        assertEquals(false, copied.strictHostKeyChecking)
        assertEquals(config.host, copied.host)
    }

    @Test
    fun smbConfigEqualityAndCopy() {
        val config = StorageConfig.SmbConfig(
            name = "smb1",
            host = "nas.local",
            share = "docs",
            username = "user",
            password = "pass"
        )
        val same = StorageConfig.SmbConfig(
            name = "smb1",
            host = "nas.local",
            share = "docs",
            username = "user",
            password = "pass"
        )
        assertEquals(config, same)

        val copied = config.copy(domain = "WORKGROUP")
        assertEquals("WORKGROUP", copied.domain)
        assertEquals(config.share, copied.share)
    }

    @Test
    fun googleDriveConfigEqualityAndCopy() {
        val config = StorageConfig.GoogleDriveConfig(
            name = "gdrive1",
            clientId = "cid",
            clientSecret = "csecret",
            accessToken = "atok"
        )
        val same = StorageConfig.GoogleDriveConfig(
            name = "gdrive1",
            clientId = "cid",
            clientSecret = "csecret",
            accessToken = "atok"
        )
        assertEquals(config, same)

        val copied = config.copy(rootFolderId = "folder-123")
        assertEquals("folder-123", copied.rootFolderId)
        assertEquals(config.clientId, copied.clientId)
    }

    @Test
    fun dropboxConfigEqualityAndCopy() {
        val config = StorageConfig.DropboxConfig(
            name = "dbx1",
            accessToken = "token",
            appKey = "key",
            appSecret = "secret"
        )
        val same = StorageConfig.DropboxConfig(
            name = "dbx1",
            accessToken = "token",
            appKey = "key",
            appSecret = "secret"
        )
        assertEquals(config, same)

        val copied = config.copy(rootPath = "/subfolder")
        assertEquals("/subfolder", copied.rootPath)
        assertEquals(config.appKey, copied.appKey)
    }

    @Test
    fun oneDriveConfigEqualityAndCopy() {
        val config = StorageConfig.OneDriveConfig(
            name = "od1",
            clientId = "cid",
            clientSecret = "csecret",
            refreshToken = "rtok"
        )
        val same = StorageConfig.OneDriveConfig(
            name = "od1",
            clientId = "cid",
            clientSecret = "csecret",
            refreshToken = "rtok"
        )
        assertEquals(config, same)

        val copied = config.copy(driveType = OneDriveDriveType.BUSINESS)
        assertEquals(OneDriveDriveType.BUSINESS, copied.driveType)
        assertEquals(config.clientId, copied.clientId)
    }

    @Test
    fun gitConfigEqualityAndCopy() {
        val config = StorageConfig.GitConfig(
            name = "git1",
            repositoryUrl = "https://github.com/user/repo.git",
            localCachePath = "/tmp/cache"
        )
        val same = StorageConfig.GitConfig(
            name = "git1",
            repositoryUrl = "https://github.com/user/repo.git",
            localCachePath = "/tmp/cache"
        )
        assertEquals(config, same)

        val copied = config.copy(branch = "develop", autoSync = false)
        assertEquals("develop", copied.branch)
        assertEquals(false, copied.autoSync)
        assertEquals(config.repositoryUrl, copied.repositoryUrl)
    }

    @Test
    fun withEnabledReturnsNewInstanceWithUpdatedValue() {
        val config = StorageConfig.WebDavConfig(
            name = "test",
            url = "https://x.com",
            username = "u",
            password = "p",
            isEnabled = true
        )
        val disabled = config.withEnabled(false)
        assertEquals(false, disabled.isEnabled)
        assertTrue(config.isEnabled)
    }

    @Test
    fun withPriorityReturnsNewInstanceWithUpdatedValue() {
        val config = StorageConfig.FtpConfig(
            name = "test",
            host = "h",
            username = "u",
            password = "p",
            priority = 100
        )
        val updated = config.withPriority(1)
        assertEquals(1, updated.priority)
        assertEquals(100, config.priority)
    }

    @Test
    fun withMetadataReturnsNewInstanceWithUpdatedValue() {
        val config = StorageConfig.SftpConfig(
            name = "test",
            host = "h",
            username = "u",
            password = "p"
        )
        val newMeta = mapOf("key1" to "value1", "key2" to "value2")
        val updated = config.withMetadata(newMeta)
        assertEquals(newMeta, updated.metadata)
        assertTrue(config.metadata.isEmpty())
    }

    // ====================================================================
    // NetworkStorageConfigService instantiation
    // ====================================================================

    @Test
    fun configServiceConfiguredStoragesStartsEmpty() {
        val service = NetworkStorageConfigService()
        assertTrue(service.configuredStorages.value.isEmpty())
    }

    @Test
    fun configServiceActiveStorageStartsNull() {
        val service = NetworkStorageConfigService()
        assertNull(service.activeStorage.value)
    }

    @Test
    fun configServiceConnectionStatusStartsEmpty() {
        val service = NetworkStorageConfigService()
        assertTrue(service.connectionStatus.value.isEmpty())
    }

    @Test
    fun configServiceValidateConfigurationReturnsNullForValidConfigs() {
        val service = NetworkStorageConfigService()

        assertNull(service.validateConfiguration(
            StorageConfig.WebDavConfig(name = "w", url = "https://x.com", username = "u", password = "p")
        ))
        assertNull(service.validateConfiguration(
            StorageConfig.FtpConfig(name = "f", host = "h", username = "u", password = "p")
        ))
        assertNull(service.validateConfiguration(
            StorageConfig.SftpConfig(name = "s", host = "h", username = "u", password = "p")
        ))
        assertNull(service.validateConfiguration(
            StorageConfig.SmbConfig(name = "m", host = "h", share = "s", username = "u", password = "p")
        ))
        assertNull(service.validateConfiguration(
            StorageConfig.GoogleDriveConfig(name = "g", clientId = "c", clientSecret = "s", accessToken = "t")
        ))
        assertNull(service.validateConfiguration(
            StorageConfig.DropboxConfig(name = "d", accessToken = "t", appKey = "k", appSecret = "s")
        ))
        assertNull(service.validateConfiguration(
            StorageConfig.OneDriveConfig(name = "o", clientId = "c", clientSecret = "s", refreshToken = "r")
        ))
        assertNull(service.validateConfiguration(
            StorageConfig.GitConfig(name = "g", repositoryUrl = "https://github.com/u/r.git", localCachePath = "/tmp/c")
        ))
    }

    @Test
    fun configServiceValidateConfigurationReturnsErrorForInvalidConfigs() {
        val service = NetworkStorageConfigService()

        assertNotNull(service.validateConfiguration(
            StorageConfig.WebDavConfig(name = "w", url = "", username = "u", password = "p")
        ))
        assertNotNull(service.validateConfiguration(
            StorageConfig.FtpConfig(name = "f", host = "", username = "u", password = "p")
        ))
        assertNotNull(service.validateConfiguration(
            StorageConfig.SftpConfig(name = "s", host = "", username = "u", password = "p")
        ))
        assertNotNull(service.validateConfiguration(
            StorageConfig.SmbConfig(name = "m", host = "", share = "s", username = "u", password = "p")
        ))
        assertNotNull(service.validateConfiguration(
            StorageConfig.GoogleDriveConfig(name = "g", clientId = "", clientSecret = "s", accessToken = "t")
        ))
        assertNotNull(service.validateConfiguration(
            StorageConfig.DropboxConfig(name = "d", accessToken = "", appKey = "k", appSecret = "s")
        ))
        assertNotNull(service.validateConfiguration(
            StorageConfig.OneDriveConfig(name = "o", clientId = "", clientSecret = "s", accessToken = "t")
        ))
        assertNotNull(service.validateConfiguration(
            StorageConfig.GitConfig(name = "g", repositoryUrl = "", localCachePath = "/tmp/c")
        ))
    }

    // ====================================================================
    // Edge case values: empty strings, special characters, very long names
    // ====================================================================

    @Test
    fun storageConfigWithEmptyName() {
        val config = StorageConfig.WebDavConfig(
            name = "",
            url = "https://x.com",
            username = "u",
            password = "p"
        )
        assertEquals("", config.name)
    }

    @Test
    fun storageConfigWithSpecialCharactersInName() {
        val specialName = "My Storage @#\$%^&*() <>/\\ \"quotes\" 'apos' tab\there"
        val config = StorageConfig.FtpConfig(
            name = specialName,
            host = "ftp.example.com",
            username = "user",
            password = "pass"
        )
        assertEquals(specialName, config.name)
    }

    @Test
    fun storageConfigWithUnicodeInName() {
        val unicodeName = "Mein Speicher \u00E4\u00F6\u00FC\u00DF \u4E2D\u6587 \u65E5\u672C\u8A9E \uD83D\uDCC1"
        val config = StorageConfig.SftpConfig(
            name = unicodeName,
            host = "sftp.example.com",
            username = "user",
            password = "pass"
        )
        assertEquals(unicodeName, config.name)
    }

    @Test
    fun storageConfigWithVeryLongName() {
        val longName = "A".repeat(10000)
        val config = StorageConfig.SmbConfig(
            name = longName,
            host = "nas.local",
            share = "docs",
            username = "user",
            password = "pass"
        )
        assertEquals(longName, config.name)
        assertEquals(10000, config.name.length)
    }

    @Test
    fun storageConfigWithWhitespaceOnlyName() {
        val config = StorageConfig.GoogleDriveConfig(
            name = "   \t\n  ",
            clientId = "cid",
            clientSecret = "csecret",
            accessToken = "token"
        )
        assertEquals("   \t\n  ", config.name)
    }

    @Test
    fun storageConfigWithSpecialCharactersInCredentials() {
        val config = StorageConfig.WebDavConfig(
            name = "test",
            url = "https://dav.example.com/path?query=1&other=2#fragment",
            username = "user@domain.com",
            password = "p@ss!w0rd#\$%"
        )
        assertEquals("user@domain.com", config.username)
        assertEquals("p@ss!w0rd#\$%", config.password)
    }

    @Test
    fun storageConfigWithVeryLongUrl() {
        val longPath = "/segment".repeat(1000)
        val longUrl = "https://example.com$longPath"
        val config = StorageConfig.WebDavConfig(
            name = "long-url",
            url = longUrl,
            username = "user",
            password = "pass"
        )
        assertEquals(longUrl, config.url)
    }

    @Test
    fun storageConfigWithCustomNonDefaultValues() {
        val config = StorageConfig.WebDavConfig(
            name = "custom",
            url = "http://internal.local/dav",
            username = "admin",
            password = "admin",
            authenticationType = WebDavAuthenticationType.DIGEST,
            sslEnabled = false,
            verifyCertificate = false,
            connectionTimeout = 5000,
            readTimeout = 10000,
            isEnabled = false,
            priority = 1,
            metadata = mapOf("env" to "staging", "region" to "us-east")
        )
        assertEquals(WebDavAuthenticationType.DIGEST, config.authenticationType)
        assertEquals(false, config.sslEnabled)
        assertEquals(false, config.verifyCertificate)
        assertEquals(5000, config.connectionTimeout)
        assertEquals(10000, config.readTimeout)
        assertEquals(false, config.isEnabled)
        assertEquals(1, config.priority)
        assertEquals(2, config.metadata.size)
        assertEquals("staging", config.metadata["env"])
        assertEquals("us-east", config.metadata["region"])
    }

    @Test
    fun ftpConfigWithEdgePorts() {
        val service = NetworkStorageConfigService()

        val configPort1 = StorageConfig.FtpConfig(
            name = "ftp-port1",
            host = "ftp.example.com",
            port = 1,
            username = "user",
            password = "pass"
        )
        assertNull(service.validateConfiguration(configPort1))

        val configPort65535 = StorageConfig.FtpConfig(
            name = "ftp-port65535",
            host = "ftp.example.com",
            port = 65535,
            username = "user",
            password = "pass"
        )
        assertNull(service.validateConfiguration(configPort65535))

        val configPort0 = StorageConfig.FtpConfig(
            name = "ftp-port0",
            host = "ftp.example.com",
            port = 0,
            username = "user",
            password = "pass"
        )
        assertNotNull(service.validateConfiguration(configPort0))
        assertEquals("Port must be between 1 and 65535", service.validateConfiguration(configPort0)?.message)

        val configPortNegative = StorageConfig.FtpConfig(
            name = "ftp-neg",
            host = "ftp.example.com",
            port = -1,
            username = "user",
            password = "pass"
        )
        assertNotNull(service.validateConfiguration(configPortNegative))

        val configPortTooHigh = StorageConfig.FtpConfig(
            name = "ftp-high",
            host = "ftp.example.com",
            port = 65536,
            username = "user",
            password = "pass"
        )
        assertNotNull(service.validateConfiguration(configPortTooHigh))
    }

    @Test
    fun sftpConfigWithEdgePorts() {
        val service = NetworkStorageConfigService()

        val configPort1 = StorageConfig.SftpConfig(
            name = "sftp-port1",
            host = "sftp.example.com",
            port = 1,
            username = "user",
            password = "pass"
        )
        assertNull(service.validateConfiguration(configPort1))

        val configPort0 = StorageConfig.SftpConfig(
            name = "sftp-port0",
            host = "sftp.example.com",
            port = 0,
            username = "user",
            password = "pass"
        )
        assertNotNull(service.validateConfiguration(configPort0))
        assertEquals("Port must be between 1 and 65535", service.validateConfiguration(configPort0)?.message)
    }

    @Test
    fun gitConfigValidatesUrlScheme() {
        val service = NetworkStorageConfigService()

        // Valid HTTPS
        assertNull(service.validateConfiguration(
            StorageConfig.GitConfig(name = "g", repositoryUrl = "https://github.com/u/r.git", localCachePath = "/tmp/c")
        ))

        // Valid HTTP
        assertNull(service.validateConfiguration(
            StorageConfig.GitConfig(name = "g", repositoryUrl = "http://gitlab.local/u/r.git", localCachePath = "/tmp/c")
        ))

        // Valid git@ SSH
        assertNull(service.validateConfiguration(
            StorageConfig.GitConfig(name = "g", repositoryUrl = "git@github.com:u/r.git", localCachePath = "/tmp/c")
        ))

        // Invalid scheme
        val invalidResult = service.validateConfiguration(
            StorageConfig.GitConfig(name = "g", repositoryUrl = "ftp://invalid.com/r.git", localCachePath = "/tmp/c")
        )
        assertNotNull(invalidResult)
        assertEquals("Repository URL must start with http(s):// or git@", invalidResult.message)
    }

    @Test
    fun webDavConfigValidatesUrlScheme() {
        val service = NetworkStorageConfigService()

        // Valid HTTPS
        assertNull(service.validateConfiguration(
            StorageConfig.WebDavConfig(name = "w", url = "https://dav.example.com", username = "u", password = "p")
        ))

        // Valid HTTP
        assertNull(service.validateConfiguration(
            StorageConfig.WebDavConfig(name = "w", url = "http://dav.local", username = "u", password = "p")
        ))

        // Invalid scheme
        val invalidResult = service.validateConfiguration(
            StorageConfig.WebDavConfig(name = "w", url = "ftp://dav.example.com", username = "u", password = "p")
        )
        assertNotNull(invalidResult)
        assertEquals("URL must start with http:// or https://", invalidResult.message)
    }

    @Test
    fun storageConfigWithEmptyMetadataMap() {
        val config = StorageConfig.DropboxConfig(
            name = "dbx",
            accessToken = "tok",
            appKey = "key",
            appSecret = "secret",
            metadata = emptyMap()
        )
        assertTrue(config.metadata.isEmpty())
    }

    @Test
    fun storageConfigWithLargeMetadataMap() {
        val largeMeta = (1..100).associate { "key_$it" to "value_$it" }
        val config = StorageConfig.OneDriveConfig(
            name = "od",
            clientId = "cid",
            clientSecret = "csecret",
            accessToken = "tok",
            metadata = largeMeta
        )
        assertEquals(100, config.metadata.size)
        assertEquals("value_50", config.metadata["key_50"])
    }

    @Test
    fun storageConfigCopyPreservesAllFields() {
        val meta = mapOf("a" to "1")
        val original = StorageConfig.GitConfig(
            name = "orig",
            repositoryUrl = "https://github.com/user/repo.git",
            branch = "develop",
            username = "gituser",
            password = "gitpass",
            personalAccessToken = "pat-123",
            privateKeyPath = "/home/user/.ssh/id_rsa",
            privateKeyPassphrase = "keypass",
            localCachePath = "/cache/repo",
            autoSync = false,
            commitAuthorName = "Test User",
            commitAuthorEmail = "test@example.com",
            connectionTimeout = 15000,
            isEnabled = false,
            priority = 5,
            metadata = meta
        )

        val copied = original.copy()
        assertEquals(original, copied)
        assertEquals(original.name, copied.name)
        assertEquals(original.repositoryUrl, copied.repositoryUrl)
        assertEquals(original.branch, copied.branch)
        assertEquals(original.username, copied.username)
        assertEquals(original.password, copied.password)
        assertEquals(original.personalAccessToken, copied.personalAccessToken)
        assertEquals(original.privateKeyPath, copied.privateKeyPath)
        assertEquals(original.privateKeyPassphrase, copied.privateKeyPassphrase)
        assertEquals(original.localCachePath, copied.localCachePath)
        assertEquals(original.autoSync, copied.autoSync)
        assertEquals(original.commitAuthorName, copied.commitAuthorName)
        assertEquals(original.commitAuthorEmail, copied.commitAuthorEmail)
        assertEquals(original.connectionTimeout, copied.connectionTimeout)
        assertEquals(original.isEnabled, copied.isEnabled)
        assertEquals(original.priority, copied.priority)
        assertEquals(original.metadata, copied.metadata)
    }
}
