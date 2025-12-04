package digital.vasic.yole.network.config

import digital.vasic.yole.network.NetworkStorageService
import digital.vasic.yole.network.common.*
import digital.vasic.yole.network.protocols.webdav.WebDavService
import digital.vasic.yole.network.platform.SecureStorageFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Service for configuring and managing network storage connections.
 * Provides unified interface for setting up different storage protocols.
 */
class NetworkStorageConfigService {
    
    private val mutex = Mutex()
    private val _configuredStorages = MutableStateFlow<List<NetworkStorage>>(emptyList())
    private val _activeStorage = MutableStateFlow<NetworkStorage?>(null)
    private val _connectionStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    
    private val configuredServices = mutableMapOf<String, NetworkStorageService>()
    
    /**
     * Current list of configured storage services
     */
    val configuredStorages: StateFlow<List<NetworkStorage>> = _configuredStorages
    
    /**
     * Currently active storage service
     */
    val activeStorage: StateFlow<NetworkStorage?> = _activeStorage
    
    /**
     * Connection status for all configured storages
     */
    val connectionStatus: StateFlow<Map<String, Boolean>> = _connectionStatus
    
    /**
     * Add a new storage configuration
     */
    suspend fun addStorage(storageConfig: StorageConfig): Result<NetworkStorage> = mutex.withLock {
        return try {
            // Validate configuration
            val validationResult = validateConfiguration(storageConfig)
            if (validationResult != null) {
                return Result.failure(validationResult)
            }
            
            // Create storage service
            val service = createStorageService(storageConfig).getOrThrow()
            configuredServices[storageConfig.name] = service
            
            // Connect to test the configuration
            val connectionResult = service.connect()
            if (connectionResult.isFailure) {
                configuredServices.remove(storageConfig.name)
                return Result.failure(connectionResult.exceptionOrNull() ?: Exception("Connection failed"))
            }
            
            // Create NetworkStorage object
            val networkStorage = NetworkStorage(
                id = storageConfig.name,
                name = storageConfig.name,
                type = storageConfig.storageType,
                location = when (storageConfig) {
                    is StorageConfig.WebDavConfig -> storageConfig.url
                    is StorageConfig.FtpConfig -> "ftp://${storageConfig.host}:${storageConfig.port}"
                    is StorageConfig.SftpConfig -> "sftp://${storageConfig.host}:${storageConfig.port}"
                    is StorageConfig.SmbConfig -> "smb://${storageConfig.host}/${storageConfig.share}"
                    is StorageConfig.GoogleDriveConfig -> "googledrive://"
                    is StorageConfig.DropboxConfig -> "dropbox://"
                    is StorageConfig.OneDriveConfig -> "onedrive://"
                },
                isOnline = true,
                lastSync = kotlinx.datetime.Clock.System.now(),
                supportsFolders = storageConfig.storageType.supportsFolders,
                supportsMetadata = storageConfig.storageType.supportsEncryption
            )
            
            // Store credentials securely
            val secureStorage = SecureStorageFactory.create().getOrThrow()
            when (storageConfig) {
                is StorageConfig.WebDavConfig -> {
                    secureStorage.storeCredentials("webdav_${storageConfig.name}", storageConfig.username, storageConfig.password)
                }
                is StorageConfig.FtpConfig -> {
                    secureStorage.storeCredentials("ftp_${storageConfig.name}", storageConfig.username, storageConfig.password)
                }
                is StorageConfig.SftpConfig -> {
                    storageConfig.username?.let { username ->
                        storageConfig.password?.let { password ->
                            secureStorage.storeCredentials("sftp_${storageConfig.name}", username, password)
                        }
                    }
                }
                is StorageConfig.SmbConfig -> {
                    secureStorage.storeCredentials("smb_${storageConfig.name}", storageConfig.username, storageConfig.password)
                }
                is StorageConfig.GoogleDriveConfig -> {
                    storageConfig.refreshToken?.let { token ->
                        secureStorage.storeToken("googledrive_${storageConfig.name}", token)
                    }
                }
                is StorageConfig.DropboxConfig -> {
                    secureStorage.storeToken("dropbox_${storageConfig.name}", storageConfig.accessToken)
                }
                is StorageConfig.OneDriveConfig -> {
                    storageConfig.refreshToken?.let { token ->
                        secureStorage.storeToken("onedrive_${storageConfig.name}", token)
                    }
                }
            }
            
            // Update state
            val currentStorages = _configuredStorages.value.toMutableList()
            _configuredStorages.value = currentStorages + networkStorage
            
            val currentStatus = _connectionStatus.value.toMutableMap()
            currentStatus[storageConfig.name] = true
            _connectionStatus.value = currentStatus
            
            Result.success(networkStorage)
        } catch (e: Exception) {
            Result.failure(NetworkStorageException.fromThrowable(e, "addStorage"))
        }
    }
    
    /**
     * Remove a storage configuration
     */
    suspend fun removeStorage(storageId: String): Result<Unit> = mutex.withLock {
        return try {
            val service = configuredServices[storageId]
                ?: return Result.failure(NetworkStorageException.GenericError(
                    message = "Storage not found",
                    context = mapOf("storageId" to storageId)
                ))
            
            // Disconnect if connected
            service.disconnect()
            
            // Remove from configured services
            configuredServices.remove(storageId)
            
            // Remove secure credentials
            val secureStorage = SecureStorageFactory.create().getOrThrow()
            when {
                storageId.startsWith("webdav_") -> secureStorage.deleteCredentials(storageId)
                storageId.startsWith("ftp_") -> secureStorage.deleteCredentials(storageId)
                storageId.startsWith("sftp_") -> secureStorage.deleteCredentials(storageId)
                storageId.startsWith("smb_") -> secureStorage.deleteCredentials(storageId)
                storageId.startsWith("googledrive_") -> secureStorage.deleteToken(storageId)
                storageId.startsWith("dropbox_") -> secureStorage.deleteToken(storageId)
                storageId.startsWith("onedrive_") -> secureStorage.deleteToken(storageId)
            }
            
            // Update state
            val currentStorages = _configuredStorages.value.filter { it.id != storageId }
            _configuredStorages.value = currentStorages
            
            val currentStatus = _connectionStatus.value.toMutableMap()
            currentStatus.remove(storageId)
            _connectionStatus.value = currentStatus
            
            // Clear active storage if it was the removed one
            if (_activeStorage.value?.id == storageId) {
                _activeStorage.value = null
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(NetworkStorageException.fromThrowable(e, "removeStorage"))
        }
    }
    
    /**
     * Set a storage as active
     */
    suspend fun setActiveStorage(storageId: String): Result<Unit> {
        return try {
            val storage = _configuredStorages.value.find { it.id == storageId }
                ?: return Result.failure(NetworkStorageException.GenericError(
                    message = "Storage not found",
                    context = mapOf("storageId" to storageId)
                ))
            
            val service = configuredServices[storageId]
                ?: return Result.failure(NetworkStorageException.GenericError(
                    message = "Storage service not found",
                    context = mapOf("storageId" to storageId)
                ))
            
            // Ensure it's connected
            if (!service.isOnline) {
                val connectResult = service.connect()
                if (connectResult.isFailure) {
                    return Result.failure(connectResult.exceptionOrNull() ?: Exception("Connection failed"))
                }
            }
            
            _activeStorage.value = storage
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(NetworkStorageException.fromThrowable(e, "setActiveStorage"))
        }
    }
    
    /**
     * Test connection to a storage without saving it
     */
    suspend fun testConnection(storageConfig: StorageConfig): Result<Boolean> {
        return try {
            val service = createStorageService(storageConfig).getOrThrow()
            val result = service.testConnection()
            service.disconnect()
            result
        } catch (e: Exception) {
            Result.failure(NetworkStorageException.fromThrowable(e, "testConnection"))
        }
    }
    
    /**
     * Get supported storage types
     */
    fun getSupportedStorageTypes(): List<StorageTypeInfo> {
        return listOf(
            StorageTypeInfo(
                type = StorageType.WEBDAV,
                name = "WebDAV",
                description = "Web-based Distributed Authoring and Versioning protocol",
                supportedFeatures = setOf(
                    StorageFeature.FOLDERS,
                    StorageFeature.METADATA,
                    StorageFeature.VERSIONING,
                    StorageFeature.ENCRYPTION
                ),
                popularServices = listOf(
                    "Nextcloud",
                    "ownCloud",
                    "SharePoint",
                    "Seafile"
                )
            ),
            StorageTypeInfo(
                type = StorageType.FTP,
                name = "FTP",
                description = "File Transfer Protocol",
                supportedFeatures = setOf(
                    StorageFeature.FILE_OPERATIONS
                ),
                popularServices = listOf(
                    "FileZilla Server",
                    "vsftpd",
                    "ProFTPD"
                )
            ),
            StorageTypeInfo(
                type = StorageType.SFTP,
                name = "SFTP",
                description = "Secure File Transfer Protocol",
                supportedFeatures = setOf(
                    StorageFeature.FILE_OPERATIONS,
                    StorageFeature.ENCRYPTION,
                    StorageFeature.PERMISSIONS
                ),
                popularServices = listOf(
                    "OpenSSH",
                    "vsftpd-sftp",
                    "ProFTPD-sftp"
                )
            ),
            StorageTypeInfo(
                type = StorageType.SMB,
                name = "SMB/CIFS",
                description = "Server Message Block protocol",
                supportedFeatures = setOf(
                    StorageFeature.FOLDERS,
                    StorageFeature.PERMISSIONS,
                    StorageFeature.NETWORK_DISCOVERY
                ),
                popularServices = listOf(
                    "Windows File Sharing",
                    "Samba",
                    "NAS devices"
                )
            ),
            StorageTypeInfo(
                type = StorageType.GOOGLE_DRIVE,
                name = "Google Drive",
                description = "Google cloud storage service",
                supportedFeatures = setOf(
                    StorageFeature.FOLDERS,
                    StorageFeature.SEARCH,
                    StorageFeature.VERSIONING,
                    StorageFeature.COLLABORATION,
                    StorageFeature.OFFLINE_SYNC
                ),
                popularServices = listOf(
                    "Google Drive",
                    "Google Workspace"
                )
            ),
            StorageTypeInfo(
                type = StorageType.DROPBOX,
                name = "Dropbox",
                description = "Dropbox cloud storage service",
                supportedFeatures = setOf(
                    StorageFeature.FOLDERS,
                    StorageFeature.VERSIONING,
                    StorageFeature.SHARING,
                    StorageFeature.OFFLINE_SYNC
                ),
                popularServices = listOf(
                    "Dropbox",
                    "Dropbox Business"
                )
            ),
            StorageTypeInfo(
                type = StorageType.ONEDRIVE,
                name = "OneDrive",
                description = "Microsoft OneDrive cloud storage",
                supportedFeatures = setOf(
                    StorageFeature.FOLDERS,
                    StorageFeature.SEARCH,
                    StorageFeature.VERSIONING,
                    StorageFeature.COLLABORATION,
                    StorageFeature.OFFICE_INTEGRATION
                ),
                popularServices = listOf(
                    "OneDrive",
                    "SharePoint Online"
                )
            )
        )
    }
    
    /**
     * Validate a storage configuration
     */
    fun validateConfiguration(storageConfig: StorageConfig): Exception? {
        return when (storageConfig) {
            is StorageConfig.WebDavConfig -> {
                when {
                    storageConfig.url.isBlank() -> Exception("URL is required")
                    storageConfig.username.isBlank() -> Exception("Username is required")
                    storageConfig.password.isBlank() -> Exception("Password is required")
                    !storageConfig.url.startsWith("http://") && !storageConfig.url.startsWith("https://") ->
                        Exception("URL must start with http:// or https://")
                    else -> null
                }
            }
            is StorageConfig.FtpConfig -> {
                when {
                    storageConfig.host.isBlank() -> Exception("Host is required")
                    storageConfig.username.isBlank() -> Exception("Username is required")
                    storageConfig.password.isBlank() -> Exception("Password is required")
                    storageConfig.port !in 1..65535 -> Exception("Port must be between 1 and 65535")
                    else -> null
                }
            }
            is StorageConfig.SftpConfig -> {
                when {
                    storageConfig.host.isBlank() -> Exception("Host is required")
                    (storageConfig.username == null || storageConfig.username.isBlank()) -> Exception("Username is required")
                    storageConfig.privateKeyPath == null && storageConfig.password == null ->
                        Exception("Either password or private key is required")
                    storageConfig.port !in 1..65535 -> Exception("Port must be between 1 and 65535")
                    else -> null
                }
            }
            is StorageConfig.SmbConfig -> {
                when {
                    storageConfig.host.isBlank() -> Exception("Host is required")
                    storageConfig.share.isBlank() -> Exception("Share name is required")
                    storageConfig.username.isBlank() -> Exception("Username is required")
                    storageConfig.password.isBlank() -> Exception("Password is required")
                    else -> null
                }
            }
            is StorageConfig.GoogleDriveConfig -> {
                when {
                    storageConfig.clientId.isBlank() -> Exception("Client ID is required")
                    storageConfig.clientSecret.isBlank() -> Exception("Client secret is required")
                    storageConfig.refreshToken == null && storageConfig.accessToken == null ->
                        Exception("Either access token or refresh token is required")
                    else -> null
                }
            }
            is StorageConfig.DropboxConfig -> {
                when {
                    storageConfig.accessToken.isBlank() -> Exception("Access token is required")
                    storageConfig.appKey.isBlank() -> Exception("App key is required")
                    storageConfig.appSecret.isBlank() -> Exception("App secret is required")
                    else -> null
                }
            }
            is StorageConfig.OneDriveConfig -> {
                when {
                    storageConfig.clientId.isBlank() -> Exception("Client ID is required")
                    storageConfig.clientSecret.isBlank() -> Exception("Client secret is required")
                    storageConfig.refreshToken == null && storageConfig.accessToken == null ->
                        Exception("Either access token or refresh token is required")
                    else -> null
                }
            }
        }
    }
    
    /**
     * Refresh connection status for all storages
     */
    suspend fun refreshConnectionStatus(): Result<Unit> {
        return try {
            val secureStorage = SecureStorageFactory.create().getOrThrow()
            val statusMap = mutableMapOf<String, Boolean>()
            
            for (storage in _configuredStorages.value) {
                val service = configuredServices[storage.id]
                val isOnline = service?.isOnline == true
                statusMap[storage.id] = isOnline
                
                val currentStatus = _connectionStatus.value[storage.id] ?: false
                if (currentStatus != isOnline) {
                    val newStatus = _connectionStatus.value.toMutableMap()
                    newStatus[storage.id] = isOnline
                    _connectionStatus.value = newStatus
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(NetworkStorageException.fromThrowable(e, "refreshConnectionStatus"))
        }
    }
    
    private fun createStorageService(storageConfig: StorageConfig): Result<NetworkStorageService> {
        return try {
            val service = when (storageConfig) {
                is StorageConfig.WebDavConfig -> WebDavService(storageConfig)
                // TODO: Implement other protocols
                else -> return Result.failure(
                    NetworkStorageException.ProtocolException.Unsupported(
                        protocol = storageConfig.storageType.name,
                        supportedProtocols = StorageType.values().map { it.name }
                    )
                )
            }
            Result.success(service)
        } catch (e: Exception) {
            Result.failure(NetworkStorageException.fromThrowable(e, "createStorageService"))
        }
    }
}

/**
 * Information about a storage type
 */
data class StorageTypeInfo(
    val type: StorageType,
    val name: String,
    val description: String,
    val supportedFeatures: Set<StorageFeature>,
    val popularServices: List<String>
)

/**
 * Features that can be supported by storage types
 */
enum class StorageFeature {
    FOLDERS,
    FILE_OPERATIONS,
    METADATA,
    VERSIONING,
    ENCRYPTION,
    PERMISSIONS,
    NETWORK_DISCOVERY,
    SEARCH,
    COLLABORATION,
    OFFLINE_SYNC,
    SHARING,
    OFFICE_INTEGRATION
}