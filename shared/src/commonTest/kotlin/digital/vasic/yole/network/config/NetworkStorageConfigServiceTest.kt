package digital.vasic.yole.network.config

import digital.vasic.yole.network.common.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking

/**
 * Test suite for NetworkStorageConfigService.
 * Tests storage configuration management and validation.
 */
class NetworkStorageConfigServiceTest {

    private lateinit var configService: NetworkStorageConfigService

    @Test
    fun testServiceInitialization() {
        configService = NetworkStorageConfigService()

        assertTrue(configService.configuredStorages.value.isEmpty())
        assertEquals(null, configService.activeStorage.value)
        assertTrue(configService.connectionStatus.value.isEmpty())
    }

    @Test
    fun testGetSupportedStorageTypes() {
        configService = NetworkStorageConfigService()
        val types = configService.getSupportedStorageTypes()

        assertEquals(8, types.size, "Should support 8 storage types")

        val webdavType = types.find { it.type == StorageType.WEBDAV }
        assertEquals("WebDAV", webdavType?.name)
        assertTrue(webdavType?.supportedFeatures?.contains(StorageFeature.FOLDERS) ?: false)
        assertEquals(listOf("Nextcloud", "ownCloud", "SharePoint", "Seafile"), webdavType?.popularServices)

        val ftpType = types.find { it.type == StorageType.FTP }
        assertEquals("FTP", ftpType?.name)
        assertTrue(ftpType?.supportedFeatures?.contains(StorageFeature.FILE_OPERATIONS) ?: false)
    }

    @Test
    fun testValidateWebDavConfiguration() {
        configService = NetworkStorageConfigService()

        // Valid configuration
        val validConfig = StorageConfig.WebDavConfig(
            name = "test-webdav",
            url = "https://example.com/webdav",
            username = "user",
            password = "pass"
        )
        assertEquals(null, configService.validateConfiguration(validConfig))

        // Invalid configurations
        val blankUrlConfig = validConfig.copy(url = "")
        assertEquals("URL is required", configService.validateConfiguration(blankUrlConfig)?.message)

        val blankUsernameConfig = validConfig.copy(username = "")
        assertEquals("Username is required", configService.validateConfiguration(blankUsernameConfig)?.message)

        val blankPasswordConfig = validConfig.copy(password = "")
        assertEquals("Password is required", configService.validateConfiguration(blankPasswordConfig)?.message)

        val invalidUrlConfig = validConfig.copy(url = "ftp://example.com")
        assertEquals("URL must start with http:// or https://", configService.validateConfiguration(invalidUrlConfig)?.message)
    }

    @Test
    fun testValidateFtpConfiguration() {
        configService = NetworkStorageConfigService()

        // Valid configuration
        val validConfig = StorageConfig.FtpConfig(
            name = "test-ftp",
            host = "ftp.example.com",
            port = 21,
            username = "user",
            password = "pass"
        )
        assertEquals(null, configService.validateConfiguration(validConfig))

        // Invalid configurations
        val blankHostConfig = validConfig.copy(host = "")
        assertEquals("Host is required", configService.validateConfiguration(blankHostConfig)?.message)

        val blankUsernameConfig = validConfig.copy(username = "")
        assertEquals("Username is required", configService.validateConfiguration(blankUsernameConfig)?.message)

        val blankPasswordConfig = validConfig.copy(password = "")
        assertEquals("Password is required", configService.validateConfiguration(blankPasswordConfig)?.message)

        val invalidPortConfig = validConfig.copy(port = 0)
        assertEquals("Port must be between 1 and 65535", configService.validateConfiguration(invalidPortConfig)?.message)

        val invalidPortConfig2 = validConfig.copy(port = 70000)
        assertEquals("Port must be between 1 and 65535", configService.validateConfiguration(invalidPortConfig2)?.message)
    }

    @Test
    fun testValidateSftpConfiguration() {
        configService = NetworkStorageConfigService()

        // Valid configuration with password
        val validPasswordConfig = StorageConfig.SftpConfig(
            name = "test-sftp",
            host = "sftp.example.com",
            port = 22,
            username = "user",
            password = "pass"
        )
        assertEquals(null, configService.validateConfiguration(validPasswordConfig))

        // Valid configuration with private key
        val validKeyConfig = StorageConfig.SftpConfig(
            name = "test-sftp-key",
            host = "sftp.example.com",
            port = 22,
            username = "user",
            privateKeyPath = "/path/to/key"
        )
        assertEquals(null, configService.validateConfiguration(validKeyConfig))

        // Invalid configurations
        val blankHostConfig = validPasswordConfig.copy(host = "")
        assertEquals("Host is required", configService.validateConfiguration(blankHostConfig)?.message)

        val blankUsernameConfig = validPasswordConfig.copy(username = "")
        assertEquals("Username is required", configService.validateConfiguration(blankUsernameConfig)?.message)

        val noAuthConfig = validPasswordConfig.copy(password = null, privateKeyPath = null)
        assertEquals("Either password or private key is required", configService.validateConfiguration(noAuthConfig)?.message)

        val invalidPortConfig = validPasswordConfig.copy(port = 0)
        assertEquals("Port must be between 1 and 65535", configService.validateConfiguration(invalidPortConfig)?.message)
    }

    @Test
    fun testValidateSmbConfiguration() {
        configService = NetworkStorageConfigService()

        // Valid configuration
        val validConfig = StorageConfig.SmbConfig(
            name = "test-smb",
            host = "smb.example.com",
            share = "shared",
            username = "user",
            password = "pass"
        )
        assertEquals(null, configService.validateConfiguration(validConfig))

        // Invalid configurations
        val blankHostConfig = validConfig.copy(host = "")
        assertEquals("Host is required", configService.validateConfiguration(blankHostConfig)?.message)

        val blankShareConfig = validConfig.copy(share = "")
        assertEquals("Share name is required", configService.validateConfiguration(blankShareConfig)?.message)

        val blankUsernameConfig = validConfig.copy(username = "")
        assertEquals("Username is required", configService.validateConfiguration(blankUsernameConfig)?.message)

        val blankPasswordConfig = validConfig.copy(password = "")
        assertEquals("Password is required", configService.validateConfiguration(blankPasswordConfig)?.message)
    }

    @Test
    fun testValidateGoogleDriveConfiguration() {
        configService = NetworkStorageConfigService()

        // Valid configuration with access token
        val validAccessTokenConfig = StorageConfig.GoogleDriveConfig(
            name = "test-gdrive",
            clientId = "client123",
            clientSecret = "secret456",
            accessToken = "token789"
        )
        assertEquals(null, configService.validateConfiguration(validAccessTokenConfig))

        // Valid configuration with refresh token
        val validRefreshTokenConfig = StorageConfig.GoogleDriveConfig(
            name = "test-gdrive-refresh",
            clientId = "client123",
            clientSecret = "secret456",
            refreshToken = "refresh_token"
        )
        assertEquals(null, configService.validateConfiguration(validRefreshTokenConfig))

        // Invalid configurations
        val blankClientIdConfig = validAccessTokenConfig.copy(clientId = "")
        assertEquals("Client ID is required", configService.validateConfiguration(blankClientIdConfig)?.message)

        val blankClientSecretConfig = validAccessTokenConfig.copy(clientSecret = "")
        assertEquals("Client secret is required", configService.validateConfiguration(blankClientSecretConfig)?.message)

        val noTokensConfig = validAccessTokenConfig.copy(accessToken = null, refreshToken = null)
        assertEquals("Either access token or refresh token is required", configService.validateConfiguration(noTokensConfig)?.message)
    }

    @Test
    fun testValidateDropboxConfiguration() {
        configService = NetworkStorageConfigService()

        // Valid configuration
        val validConfig = StorageConfig.DropboxConfig(
            name = "test-dropbox",
            appKey = "app123",
            appSecret = "secret456",
            accessToken = "token789"
        )
        assertEquals(null, configService.validateConfiguration(validConfig))

        // Invalid configurations
        val blankAccessTokenConfig = validConfig.copy(accessToken = "")
        assertEquals("Access token is required", configService.validateConfiguration(blankAccessTokenConfig)?.message)

        val blankAppKeyConfig = validConfig.copy(appKey = "")
        assertEquals("App key is required", configService.validateConfiguration(blankAppKeyConfig)?.message)

        val blankAppSecretConfig = validConfig.copy(appSecret = "")
        assertEquals("App secret is required", configService.validateConfiguration(blankAppSecretConfig)?.message)
    }

    @Test
    fun testValidateOneDriveConfiguration() {
        configService = NetworkStorageConfigService()

        // Valid configuration with access token
        val validAccessTokenConfig = StorageConfig.OneDriveConfig(
            name = "test-onedrive",
            clientId = "client123",
            clientSecret = "secret456",
            accessToken = "token789"
        )
        assertEquals(null, configService.validateConfiguration(validAccessTokenConfig))

        // Valid configuration with refresh token
        val validRefreshTokenConfig = StorageConfig.OneDriveConfig(
            name = "test-onedrive-refresh",
            clientId = "client123",
            clientSecret = "secret456",
            refreshToken = "refresh_token"
        )
        assertEquals(null, configService.validateConfiguration(validRefreshTokenConfig))

        // Invalid configurations
        val blankClientIdConfig = validAccessTokenConfig.copy(clientId = "")
        assertEquals("Client ID is required", configService.validateConfiguration(blankClientIdConfig)?.message)

        val blankClientSecretConfig = validAccessTokenConfig.copy(clientSecret = "")
        assertEquals("Client secret is required", configService.validateConfiguration(blankClientSecretConfig)?.message)

        val noTokensConfig = validAccessTokenConfig.copy(accessToken = null, refreshToken = null)
        assertEquals("Either access token or refresh token is required", configService.validateConfiguration(noTokensConfig)?.message)
    }

    @Test
    fun testValidateGitConfiguration() {
        configService = NetworkStorageConfigService()

        // Valid configuration
        val validConfig = StorageConfig.GitConfig(
            name = "test-git",
            repositoryUrl = "https://github.com/user/repo.git",
            localCachePath = "/tmp/cache"
        )
        assertEquals(null, configService.validateConfiguration(validConfig))

        // Valid SSH configuration
        val validSshConfig = StorageConfig.GitConfig(
            name = "test-git-ssh",
            repositoryUrl = "git@github.com:user/repo.git",
            localCachePath = "/tmp/cache"
        )
        assertEquals(null, configService.validateConfiguration(validSshConfig))

        // Invalid configurations
        val blankUrlConfig = validConfig.copy(repositoryUrl = "")
        assertEquals("Repository URL is required", configService.validateConfiguration(blankUrlConfig)?.message)

        val blankCachePathConfig = validConfig.copy(localCachePath = "")
        assertEquals("Local cache path is required", configService.validateConfiguration(blankCachePathConfig)?.message)

        val invalidUrlConfig = validConfig.copy(repositoryUrl = "invalid-url")
        assertEquals("Repository URL must start with http(s):// or git@", configService.validateConfiguration(invalidUrlConfig)?.message)
    }

    @Test
    fun testRefreshConnectionStatus() = runBlocking<Unit> {
        configService = NetworkStorageConfigService()

        val result = configService.refreshConnectionStatus()
        assertTrue(result.isSuccess, "Refresh connection status should succeed")
    }
}