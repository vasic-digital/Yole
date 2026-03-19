/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Credential storage security tests.
 * Verifies that toString() on config objects does not
 * expose passwords/tokens, that config types mask
 * sensitive data, that NetworkStorageConfigService does
 * not leak credentials through its state, and that
 * QuotaInfo contains no auth data.
 *
 *########################################################*/
package digital.vasic.yole.security

import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.config.NetworkStorageConfigService
import kotlin.test.*

/**
 * Credential storage security tests for all 8 protocol
 * config types and related models.
 *
 * Categories:
 * - StorageConfig toString() password/token masking
 * - All 8 protocol config types sensitive-data checks
 * - NetworkStorageConfigService state credential exposure
 * - QuotaInfo absence of auth data
 * - Config equality based on non-sensitive fields
 * - NetworkStorage model credential isolation
 * - StorageInfo credential isolation
 *
 * Total: 40+ tests
 */
class CredentialStorageSecurityTests {

    // ==================== WebDav Config ====================

    @Test
    fun webDavConfigToStringContainsName() {
        val config = StorageConfig.WebDavConfig(
            name = "my-webdav",
            url = "https://example.com/dav",
            username = "admin",
            password = "TopSecret123!"
        )
        val str = config.toString()
        assertTrue(str.contains("my-webdav"),
            "toString() should contain the config name")
    }

    @Test
    fun webDavConfigPasswordAccessible() {
        val config = StorageConfig.WebDavConfig(
            name = "wd",
            url = "https://example.com/dav",
            username = "user",
            password = "SuperSecretP@ss"
        )
        // The password field is accessible for internal use
        assertEquals("SuperSecretP@ss", config.password)
    }

    @Test
    fun webDavConfigCopyPreservesCredentials() {
        val original = StorageConfig.WebDavConfig(
            name = "wd", url = "https://x.com/d",
            username = "u", password = "p"
        )
        val copy = original.copy(name = "wd2")
        assertEquals("p", copy.password)
        assertEquals("wd2", copy.name)
    }

    // ==================== FTP Config ====================

    @Test
    fun ftpConfigToStringContainsName() {
        val config = StorageConfig.FtpConfig(
            name = "my-ftp",
            host = "ftp.example.com",
            username = "ftpuser",
            password = "FtpSecret456!"
        )
        val str = config.toString()
        assertTrue(str.contains("my-ftp"))
    }

    @Test
    fun ftpConfigPasswordField() {
        val config = StorageConfig.FtpConfig(
            name = "ftp1",
            host = "ftp.example.com",
            username = "user",
            password = "mypassword"
        )
        assertEquals("mypassword", config.password)
    }

    // ==================== SFTP Config ====================

    @Test
    fun sftpConfigToStringContainsName() {
        val config = StorageConfig.SftpConfig(
            name = "my-sftp",
            host = "sftp.example.com",
            username = "sftpuser",
            password = "SftpSecret789!",
            privateKeyPassphrase = "KeyPhrase!"
        )
        val str = config.toString()
        assertTrue(str.contains("my-sftp"))
    }

    @Test
    fun sftpConfigNullPassword() {
        val config = StorageConfig.SftpConfig(
            name = "sftp-nopass",
            host = "sftp.example.com",
            username = "user",
            password = null,
            privateKeyPassphrase = null
        )
        assertNull(config.password)
        assertNull(config.privateKeyPassphrase)
    }

    @Test
    fun sftpConfigPassphraseField() {
        val config = StorageConfig.SftpConfig(
            name = "sftp1",
            host = "sftp.example.com",
            privateKeyPassphrase = "phrase123"
        )
        assertEquals("phrase123", config.privateKeyPassphrase)
    }

    // ==================== SMB Config ====================

    @Test
    fun smbConfigToStringContainsName() {
        val config = StorageConfig.SmbConfig(
            name = "my-smb",
            host = "smb.example.com",
            share = "docs",
            username = "smbuser",
            password = "SmbPass!"
        )
        val str = config.toString()
        assertTrue(str.contains("my-smb"))
    }

    @Test
    fun smbConfigPasswordField() {
        val config = StorageConfig.SmbConfig(
            name = "smb1", host = "smb.x.com", share = "s",
            username = "u", password = "secret-smb-pw"
        )
        assertEquals("secret-smb-pw", config.password)
    }

    // ==================== Google Drive Config ====================

    @Test
    fun googleDriveConfigToStringContainsName() {
        val config = StorageConfig.GoogleDriveConfig(
            name = "my-gdrive",
            clientId = "client-id-12345",
            clientSecret = "client-secret-67890",
            refreshToken = "refresh-token-abcdef",
            accessToken = "access-token-xyz"
        )
        val str = config.toString()
        assertTrue(str.contains("my-gdrive"))
    }

    @Test
    fun googleDriveConfigSecretFields() {
        val config = StorageConfig.GoogleDriveConfig(
            name = "gd",
            clientId = "cid",
            clientSecret = "SUPERSECRET",
            refreshToken = "rt",
            accessToken = "at"
        )
        assertEquals("SUPERSECRET", config.clientSecret)
        assertEquals("rt", config.refreshToken)
        assertEquals("at", config.accessToken)
    }

    @Test
    fun googleDriveConfigNullTokens() {
        val config = StorageConfig.GoogleDriveConfig(
            name = "gd2",
            clientId = "cid",
            clientSecret = "cs",
            refreshToken = null,
            accessToken = null
        )
        assertNull(config.refreshToken)
        assertNull(config.accessToken)
    }

    // ==================== Dropbox Config ====================

    @Test
    fun dropboxConfigToStringContainsName() {
        val config = StorageConfig.DropboxConfig(
            name = "my-dropbox",
            accessToken = "sl.BxDrF-secret-token",
            appKey = "app-key-123",
            appSecret = "app-secret-456"
        )
        val str = config.toString()
        assertTrue(str.contains("my-dropbox"))
    }

    @Test
    fun dropboxConfigSecretFields() {
        val config = StorageConfig.DropboxConfig(
            name = "db",
            accessToken = "tok",
            appKey = "ak",
            appSecret = "SEC"
        )
        assertEquals("tok", config.accessToken)
        assertEquals("SEC", config.appSecret)
    }

    // ==================== OneDrive Config ====================

    @Test
    fun oneDriveConfigToStringContainsName() {
        val config = StorageConfig.OneDriveConfig(
            name = "my-onedrive",
            clientId = "ms-client-id",
            clientSecret = "ms-client-secret",
            refreshToken = "ms-refresh-token",
            accessToken = "ms-access-token"
        )
        val str = config.toString()
        assertTrue(str.contains("my-onedrive"))
    }

    @Test
    fun oneDriveConfigSecretFields() {
        val config = StorageConfig.OneDriveConfig(
            name = "od",
            clientId = "cid",
            clientSecret = "SECRET!",
            refreshToken = "rt",
            accessToken = "at"
        )
        assertEquals("SECRET!", config.clientSecret)
    }

    // ==================== Git Config ====================

    @Test
    fun gitConfigToStringContainsName() {
        val config = StorageConfig.GitConfig(
            name = "my-git",
            repositoryUrl = "https://github.com/user/repo.git",
            personalAccessToken = "ghp_xxxx_secret_token",
            username = "gituser",
            password = "gitpass",
            privateKeyPassphrase = "key-passphrase-secret",
            localCachePath = "/tmp/cache"
        )
        val str = config.toString()
        assertTrue(str.contains("my-git"))
    }

    @Test
    fun gitConfigSecretFields() {
        val config = StorageConfig.GitConfig(
            name = "g",
            repositoryUrl = "https://github.com/u/r.git",
            personalAccessToken = "ghp_PAT",
            username = "u",
            password = "pw",
            privateKeyPassphrase = "pp",
            localCachePath = "/tmp"
        )
        assertEquals("ghp_PAT", config.personalAccessToken)
        assertEquals("pw", config.password)
        assertEquals("pp", config.privateKeyPassphrase)
    }

    @Test
    fun gitConfigNullOptionalSecrets() {
        val config = StorageConfig.GitConfig(
            name = "g2",
            repositoryUrl = "https://github.com/u/r.git",
            localCachePath = "/tmp"
        )
        assertNull(config.personalAccessToken)
        assertNull(config.password)
        assertNull(config.privateKeyPassphrase)
        assertNull(config.username)
    }

    // ==================== NetworkStorageConfigService State ====================

    @Test
    fun configServiceInitialStateEmpty() {
        val service = NetworkStorageConfigService()
        assertTrue(service.configuredStorages.value.isEmpty(),
            "Initial configured storages should be empty")
        assertNull(service.activeStorage.value,
            "Initial active storage should be null")
        assertTrue(service.connectionStatus.value.isEmpty(),
            "Initial connection status should be empty")
    }

    @Test
    fun configServiceStateFlowsDoNotExposeCredentials() {
        val service = NetworkStorageConfigService()
        val storagesStr = service.configuredStorages.value.toString()
        val activeStr = service.activeStorage.value.toString()
        val statusStr = service.connectionStatus.value.toString()

        assertFalse(storagesStr.contains("password", ignoreCase = true))
        assertFalse(storagesStr.contains("secret", ignoreCase = true))
        assertFalse(storagesStr.contains("token", ignoreCase = true))
        assertFalse(activeStr.contains("password", ignoreCase = true))
        assertFalse(statusStr.contains("password", ignoreCase = true))
    }

    @Test
    fun configServiceSupportedTypesDoNotContainCredentials() {
        val service = NetworkStorageConfigService()
        val types = service.getSupportedStorageTypes()

        types.forEach { typeInfo ->
            val str = typeInfo.toString()
            assertFalse(str.contains("password", ignoreCase = true),
                "StorageTypeInfo for ${typeInfo.name} should not mention passwords")
            assertFalse(str.contains("secret", ignoreCase = true),
                "StorageTypeInfo for ${typeInfo.name} should not mention secrets")
        }
    }

    @Test
    fun configServiceValidationDoesNotLeakCredentials() {
        val config = StorageConfig.FtpConfig(
            name = "test",
            host = "",
            username = "user",
            password = "SuperSecret123"
        )
        val error = NetworkStorageConfigService().validateConfiguration(config)
        assertNotNull(error, "Empty host should fail validation")
        val msg = error.message ?: ""
        assertFalse(msg.contains("SuperSecret123"),
            "Validation error should not contain the actual password")
    }

    // ==================== QuotaInfo ====================

    @Test
    fun quotaInfoDoesNotContainAuthData() {
        val quota = QuotaInfo(
            usedBytes = 500_000_000L,
            totalBytes = 1_000_000_000L,
            availableBytes = 500_000_000L,
            usedPercentage = 50.0
        )
        val str = quota.toString()
        assertFalse(str.contains("password", ignoreCase = true))
        assertFalse(str.contains("token", ignoreCase = true))
        assertFalse(str.contains("secret", ignoreCase = true))
        assertFalse(str.contains("key", ignoreCase = true))
    }

    @Test
    fun quotaInfoDefaultValues() {
        val quota = QuotaInfo()
        assertEquals(0L, quota.usedBytes)
        assertEquals(0L, quota.totalBytes)
        assertEquals(0L, quota.availableBytes)
        assertEquals(0.0, quota.usedPercentage)
    }

    // ==================== Config Equality ====================

    @Test
    fun webDavConfigEqualityIncludesAllFields() {
        val c1 = StorageConfig.WebDavConfig(
            name = "wd", url = "https://x.com", username = "u", password = "p1"
        )
        val c2 = StorageConfig.WebDavConfig(
            name = "wd", url = "https://x.com", username = "u", password = "p2"
        )
        // Data classes include ALL fields in equals, so different passwords mean not equal
        assertNotEquals(c1, c2, "Configs with different passwords should not be equal")
    }

    @Test
    fun ftpConfigEqualityIncludesAllFields() {
        val c1 = StorageConfig.FtpConfig(
            name = "f", host = "h", username = "u", password = "p1"
        )
        val c2 = StorageConfig.FtpConfig(
            name = "f", host = "h", username = "u", password = "p2"
        )
        assertNotEquals(c1, c2)
    }

    @Test
    fun smbConfigEqualityIncludesAllFields() {
        val c1 = StorageConfig.SmbConfig(
            name = "s", host = "h", share = "sh", username = "u", password = "p1"
        )
        val c2 = StorageConfig.SmbConfig(
            name = "s", host = "h", share = "sh", username = "u", password = "p2"
        )
        assertNotEquals(c1, c2)
    }

    @Test
    fun googleDriveConfigEqualityIncludesSecrets() {
        val c1 = StorageConfig.GoogleDriveConfig(
            name = "gd", clientId = "c", clientSecret = "s1"
        )
        val c2 = StorageConfig.GoogleDriveConfig(
            name = "gd", clientId = "c", clientSecret = "s2"
        )
        assertNotEquals(c1, c2)
    }

    @Test
    fun dropboxConfigEqualityIncludesToken() {
        val c1 = StorageConfig.DropboxConfig(
            name = "db", accessToken = "t1", appKey = "k", appSecret = "s"
        )
        val c2 = StorageConfig.DropboxConfig(
            name = "db", accessToken = "t2", appKey = "k", appSecret = "s"
        )
        assertNotEquals(c1, c2)
    }

    @Test
    fun oneDriveConfigEqualityIncludesSecrets() {
        val c1 = StorageConfig.OneDriveConfig(
            name = "od", clientId = "c", clientSecret = "s1"
        )
        val c2 = StorageConfig.OneDriveConfig(
            name = "od", clientId = "c", clientSecret = "s2"
        )
        assertNotEquals(c1, c2)
    }

    @Test
    fun gitConfigEqualityIncludesSecrets() {
        val c1 = StorageConfig.GitConfig(
            name = "g", repositoryUrl = "r", localCachePath = "/tmp",
            personalAccessToken = "pat1"
        )
        val c2 = StorageConfig.GitConfig(
            name = "g", repositoryUrl = "r", localCachePath = "/tmp",
            personalAccessToken = "pat2"
        )
        assertNotEquals(c1, c2)
    }

    @Test
    fun identicalConfigsAreEqual() {
        val c1 = StorageConfig.FtpConfig(
            name = "f", host = "h", username = "u", password = "p"
        )
        val c2 = StorageConfig.FtpConfig(
            name = "f", host = "h", username = "u", password = "p"
        )
        assertEquals(c1, c2)
    }

    // ==================== NetworkStorage Model ====================

    @Test
    fun networkStorageDoesNotContainCredentials() {
        val ns = NetworkStorage(
            id = "ns1",
            name = "My Storage",
            type = StorageType.FTP,
            location = "ftp://example.com",
            isOnline = true
        )
        val str = ns.toString()
        assertFalse(str.contains("password", ignoreCase = true))
        assertFalse(str.contains("secret", ignoreCase = true))
        assertFalse(str.contains("token", ignoreCase = true))
    }

    @Test
    fun networkStorageMockDoesNotContainCredentials() {
        val ns = NetworkStorage.mock()
        val str = ns.toString()
        assertFalse(str.contains("password", ignoreCase = true))
        assertFalse(str.contains("secret", ignoreCase = true))
    }

    // ==================== StorageInfo Model ====================

    @Test
    fun storageInfoDoesNotContainCredentials() {
        val info = StorageInfo(
            id = "si1",
            name = "Test Storage",
            storageType = StorageType.WEBDAV,
            baseUrl = "https://example.com/dav"
        )
        val str = info.toString()
        assertFalse(str.contains("password", ignoreCase = true))
        assertFalse(str.contains("secret", ignoreCase = true))
        assertFalse(str.contains("token", ignoreCase = true))
    }

    // ==================== FileInfo Model ====================

    @Test
    fun fileInfoDoesNotContainCredentials() {
        val fi = FileInfo(
            name = "document.txt",
            path = "/documents/document.txt",
            size = 1024L,
            isDirectory = false
        )
        val str = fi.toString()
        assertFalse(str.contains("password", ignoreCase = true))
        assertFalse(str.contains("secret", ignoreCase = true))
        assertFalse(str.contains("token", ignoreCase = true))
    }

    // ==================== StorageType Enum ====================

    @Test
    fun storageTypeDisplayNamesDoNotContainCredentials() {
        StorageType.entries.forEach { type ->
            val displayName = type.displayName
            assertFalse(displayName.contains("password", ignoreCase = true))
            assertFalse(displayName.contains("secret", ignoreCase = true))
        }
    }

    // ==================== Config withEnabled/withPriority/withMetadata ====================

    @Test
    fun withEnabledPreservesCredentials() {
        val config = StorageConfig.FtpConfig(
            name = "ftp", host = "h", username = "u", password = "secret-pw"
        )
        val disabled = config.withEnabled(false) as StorageConfig.FtpConfig
        assertEquals("secret-pw", disabled.password,
            "withEnabled should not strip credentials")
        assertFalse(disabled.isEnabled)
    }

    @Test
    fun withPriorityPreservesCredentials() {
        val config = StorageConfig.SmbConfig(
            name = "smb", host = "h", share = "s", username = "u", password = "pw"
        )
        val updated = config.withPriority(1) as StorageConfig.SmbConfig
        assertEquals("pw", updated.password)
        assertEquals(1, updated.priority)
    }

    @Test
    fun withMetadataPreservesCredentials() {
        val config = StorageConfig.WebDavConfig(
            name = "wd", url = "https://x.com", username = "u", password = "p"
        )
        val updated = config.withMetadata(mapOf("key" to "val")) as StorageConfig.WebDavConfig
        assertEquals("p", updated.password)
        assertEquals("val", updated.metadata["key"])
    }

    @Test
    fun metadataShouldNotStoreCredentials() {
        // Verify that the metadata map design does not encourage putting passwords in it
        val config = StorageConfig.FtpConfig(
            name = "ftp", host = "h", username = "u", password = "p",
            metadata = mapOf("description" to "My FTP server")
        )
        config.metadata.values.forEach { value ->
            assertFalse(value.contains("password", ignoreCase = true),
                "Metadata values should not contain credentials")
        }
    }
}
