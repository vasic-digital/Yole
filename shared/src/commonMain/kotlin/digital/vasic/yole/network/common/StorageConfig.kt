package digital.vasic.yole.network.common

import kotlinx.serialization.Serializable

/**
 * Sealed class representing different types of storage configurations.
 * Each storage type has its own specific configuration parameters.
 */
@Serializable
sealed class StorageConfig {
    /**
     * Human-readable name for this storage configuration
     */
    abstract val name: String
    
    /**
     * Storage type identifier
     */
    abstract val storageType: StorageType
    
    /**
     * Whether this storage is enabled
     */
    abstract val isEnabled: Boolean
    
    /**
     * Priority for display and operations (lower number = higher priority)
     */
    abstract val priority: Int
    
    /**
     * Additional metadata
     */
    abstract val metadata: Map<String, String>
    
    /**
     * Create a copy with updated enabled status
     */
    abstract fun withEnabled(isEnabled: Boolean): StorageConfig
    
    /**
     * Create a copy with updated priority
     */
    abstract fun withPriority(priority: Int): StorageConfig
    
    /**
     * Create a copy with updated metadata
     */
    abstract fun withMetadata(metadata: Map<String, String>): StorageConfig
    
    /**
     * WebDAV storage configuration
     */
    @Serializable
    data class WebDavConfig(
        override val name: String,
        val url: String,
        val username: String,
        val password: String,
        val authenticationType: WebDavAuthenticationType = WebDavAuthenticationType.BASIC,
        val sslEnabled: Boolean = true,
        val verifyCertificate: Boolean = true,
        val connectionTimeout: Int = 30000, // milliseconds
        val readTimeout: Int = 60000, // milliseconds
        override val isEnabled: Boolean = true,
        override val priority: Int = 100,
        override val metadata: Map<String, String> = emptyMap()
    ) : StorageConfig() {
        
        override val storageType = StorageType.WEBDAV
        
        override fun withEnabled(isEnabled: Boolean): StorageConfig {
            return copy(isEnabled = isEnabled)
        }
        
        override fun withPriority(priority: Int): StorageConfig {
            return copy(priority = priority)
        }
        
        override fun withMetadata(metadata: Map<String, String>): StorageConfig {
            return copy(metadata = metadata)
        }
    }
    
    /**
     * FTP storage configuration
     */
    @Serializable
    data class FtpConfig(
        override val name: String,
        val host: String,
        val port: Int = 21,
        val username: String,
        val password: String,
        val passiveMode: Boolean = true,
        val secureFtp: Boolean = false,
        val encoding: String = "UTF-8",
        val connectionTimeout: Int = 30000, // milliseconds
        override val isEnabled: Boolean = true,
        override val priority: Int = 100,
        override val metadata: Map<String, String> = emptyMap()
    ) : StorageConfig() {
        
        override val storageType = StorageType.FTP
        
        override fun withEnabled(isEnabled: Boolean): StorageConfig {
            return copy(isEnabled = isEnabled)
        }
        
        override fun withPriority(priority: Int): StorageConfig {
            return copy(priority = priority)
        }
        
        override fun withMetadata(metadata: Map<String, String>): StorageConfig {
            return copy(metadata = metadata)
        }
    }
    
    /**
     * SFTP storage configuration
     */
    @Serializable
    data class SftpConfig(
        override val name: String,
        val host: String,
        val port: Int = 22,
        val username: String,
        val password: String? = null,
        val privateKeyPath: String? = null,
        val privateKeyPassphrase: String? = null,
        val knownHostsPath: String? = null,
        val strictHostKeyChecking: Boolean = true,
        val connectionTimeout: Int = 30000, // milliseconds
        override val isEnabled: Boolean = true,
        override val priority: Int = 100,
        override val metadata: Map<String, String> = emptyMap()
    ) : StorageConfig() {
        
        override val storageType = StorageType.SFTP
        
        override fun withEnabled(isEnabled: Boolean): StorageConfig {
            return copy(isEnabled = isEnabled)
        }
        
        override fun withPriority(priority: Int): StorageConfig {
            return copy(priority = priority)
        }
        
        override fun withMetadata(metadata: Map<String, String>): StorageConfig {
            return copy(metadata = metadata)
        }
    }
    
    /**
     * SMB/CIFS storage configuration
     */
    @Serializable
    data class SmbConfig(
        override val name: String,
        val host: String,
        val share: String,
        val domain: String? = null,
        val username: String,
        val password: String,
        val port: Int = 445,
        val encryption: Boolean = true,
        val signing: Boolean = true,
        val connectionTimeout: Int = 30000, // milliseconds
        override val isEnabled: Boolean = true,
        override val priority: Int = 100,
        override val metadata: Map<String, String> = emptyMap()
    ) : StorageConfig() {
        
        override val storageType = StorageType.SMB
        
        override fun withEnabled(isEnabled: Boolean): StorageConfig {
            return copy(isEnabled = isEnabled)
        }
        
        override fun withPriority(priority: Int): StorageConfig {
            return copy(priority = priority)
        }
        
        override fun withMetadata(metadata: Map<String, String>): StorageConfig {
            return copy(metadata = metadata)
        }
    }
    
    /**
     * Google Drive storage configuration
     */
    @Serializable
    data class GoogleDriveConfig(
        override val name: String,
        val clientId: String,
        val clientSecret: String,
        val refreshToken: String? = null,
        val accessToken: String? = null,
        val rootFolderId: String? = null,
        val teamDriveId: String? = null,
        override val isEnabled: Boolean = true,
        override val priority: Int = 100,
        override val metadata: Map<String, String> = emptyMap()
    ) : StorageConfig() {
        
        override val storageType = StorageType.GOOGLE_DRIVE
        
        override fun withEnabled(isEnabled: Boolean): StorageConfig {
            return copy(isEnabled = isEnabled)
        }
        
        override fun withPriority(priority: Int): StorageConfig {
            return copy(priority = priority)
        }
        
        override fun withMetadata(metadata: Map<String, String>): StorageConfig {
            return copy(metadata = metadata)
        }
    }
    
    /**
     * Dropbox storage configuration
     */
    @Serializable
    data class DropboxConfig(
        override val name: String,
        val accessToken: String,
        val appKey: String,
        val appSecret: String,
        val refreshToken: String? = null,
        val rootPath: String = "",
        override val isEnabled: Boolean = true,
        override val priority: Int = 100,
        override val metadata: Map<String, String> = emptyMap()
    ) : StorageConfig() {
        
        override val storageType = StorageType.DROPBOX
        
        override fun withEnabled(isEnabled: Boolean): StorageConfig {
            return copy(isEnabled = isEnabled)
        }
        
        override fun withPriority(priority: Int): StorageConfig {
            return copy(priority = priority)
        }
        
        override fun withMetadata(metadata: Map<String, String>): StorageConfig {
            return copy(metadata = metadata)
        }
    }
    
    /**
     * OneDrive storage configuration
     */
    @Serializable
    data class OneDriveConfig(
        override val name: String,
        val clientId: String,
        val clientSecret: String,
        val refreshToken: String? = null,
        val accessToken: String? = null,
        val driveType: OneDriveDriveType = OneDriveDriveType.ME,
        val driveId: String? = null,
        override val isEnabled: Boolean = true,
        override val priority: Int = 100,
        override val metadata: Map<String, String> = emptyMap()
    ) : StorageConfig() {
        
        override val storageType = StorageType.ONEDRIVE
        
        override fun withEnabled(isEnabled: Boolean): StorageConfig {
            return copy(isEnabled = isEnabled)
        }
        
        override fun withPriority(priority: Int): StorageConfig {
            return copy(priority = priority)
        }
        
        override fun withMetadata(metadata: Map<String, String>): StorageConfig {
            return copy(metadata = metadata)
        }
    }
}

/**
 * Enum representing different storage types
 */
@Serializable
enum class StorageType {
    WEBDAV,
    FTP,
    SFTP,
    SMB,
    GOOGLE_DRIVE,
    DROPBOX,
    ONEDRIVE;
    
    /**
     * Human-readable display name
     */
    val displayName: String
        get() = when (this) {
            WEBDAV -> "WebDAV"
            FTP -> "FTP"
            SFTP -> "SFTP"
            SMB -> "SMB/CIFS"
            GOOGLE_DRIVE -> "Google Drive"
            DROPBOX -> "Dropbox"
            ONEDRIVE -> "OneDrive"
        }
    
    /**
     * Whether this storage type supports folders
     */
    val supportsFolders: Boolean
        get() = when (this) {
            WEBDAV, SFTP, SMB, GOOGLE_DRIVE, DROPBOX, ONEDRIVE -> true
            FTP -> false // Basic FTP doesn't have reliable folder support
        }
    
    /**
     * Whether this storage type supports encryption
     */
    val supportsEncryption: Boolean
        get() = when (this) {
            WEBDAV, SFTP, SMB, GOOGLE_DRIVE, DROPBOX, ONEDRIVE -> true
            FTP -> false
        }
    
    /**
     * Default port for this storage type
     */
    val defaultPort: Int
        get() = when (this) {
            WEBDAV -> 443 // HTTPS
            FTP -> 21
            SFTP -> 22
            SMB -> 445
            GOOGLE_DRIVE -> 443 // HTTPS
            DROPBOX -> 443 // HTTPS
            ONEDRIVE -> 443 // HTTPS
        }
}

/**
 * WebDAV authentication types
 */
@Serializable
enum class WebDavAuthenticationType {
    BASIC,
    DIGEST,
    OAUTH,
    NONE
}

/**
 * OneDrive drive types
 */
@Serializable
enum class OneDriveDriveType {
    ME, // Personal OneDrive
    BUSINESS, // Business OneDrive
    SHAREPOINT, // SharePoint document library
    GROUP // Microsoft 365 group
}