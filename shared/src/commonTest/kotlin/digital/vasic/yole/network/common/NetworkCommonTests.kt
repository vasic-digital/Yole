package digital.vasic.yole.network.common

import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.hours

class NetworkCommonTests {

    @Test
    fun `DocumentPermission enum values and properties work correctly`() {
        // Test all enum values exist
        assertEquals(16, DocumentPermission.values().size)

        // Test computed properties for READ permission
        assertTrue(DocumentPermission.READ.isReadPermission)
        assertFalse(DocumentPermission.READ.isWritePermission)
        assertFalse(DocumentPermission.READ.isAdministrativePermission)

        // Test computed properties for WRITE permission
        assertFalse(DocumentPermission.WRITE.isReadPermission)
        assertTrue(DocumentPermission.WRITE.isWritePermission)
        assertFalse(DocumentPermission.WRITE.isAdministrativePermission)

        // Test computed properties for ADMIN permission
        assertFalse(DocumentPermission.ADMIN.isReadPermission)
        assertFalse(DocumentPermission.ADMIN.isWritePermission)
        assertTrue(DocumentPermission.ADMIN.isAdministrativePermission)

        // Test display names
        assertEquals("Read", DocumentPermission.READ.displayName)
        assertEquals("Write", DocumentPermission.WRITE.displayName)
        assertEquals("Administrator", DocumentPermission.ADMIN.displayName)

        // Test descriptions
        assertEquals("Can read document content", DocumentPermission.READ.description)
        assertEquals("Can modify document content", DocumentPermission.WRITE.description)
        assertEquals("Has full administrative access", DocumentPermission.ADMIN.description)
    }

    @Test
    fun `DocumentPermission companion object functions work correctly`() {
        val readOnlyPermissions = setOf(DocumentPermission.READ, DocumentPermission.VIEW_METADATA)
        val writePermissions = setOf(DocumentPermission.WRITE, DocumentPermission.CREATE)
        val adminPermissions = setOf(DocumentPermission.ADMIN)

        // Test hasPermission
        assertTrue(DocumentPermission.hasPermission(readOnlyPermissions, DocumentPermission.READ))
        assertFalse(DocumentPermission.hasPermission(readOnlyPermissions, DocumentPermission.WRITE))
        assertTrue(DocumentPermission.hasPermission(adminPermissions, DocumentPermission.WRITE))

        // Test hasReadPermissions
        assertTrue(DocumentPermission.hasReadPermissions(readOnlyPermissions))
        assertFalse(DocumentPermission.hasReadPermissions(writePermissions))
        assertTrue(DocumentPermission.hasReadPermissions(adminPermissions))

        // Test hasWritePermissions
        assertFalse(DocumentPermission.hasWritePermissions(readOnlyPermissions))
        assertTrue(DocumentPermission.hasWritePermissions(writePermissions))
        assertTrue(DocumentPermission.hasWritePermissions(adminPermissions))

        // Test hasAdministrativePermissions
        assertFalse(DocumentPermission.hasAdministrativePermissions(readOnlyPermissions))
        assertFalse(DocumentPermission.hasAdministrativePermissions(writePermissions))
        assertTrue(DocumentPermission.hasAdministrativePermissions(adminPermissions))

        // Test getEffectivePermissions
        assertEquals(DocumentPermission.ALL_PERMISSIONS, DocumentPermission.getEffectivePermissions(adminPermissions))
        assertEquals(readOnlyPermissions, DocumentPermission.getEffectivePermissions(readOnlyPermissions))

        // Test default permission sets
        assertTrue(DocumentPermission.DEFAULT_FOLDER_PERMISSIONS.contains(DocumentPermission.READ))
        assertTrue(DocumentPermission.DEFAULT_FOLDER_PERMISSIONS.contains(DocumentPermission.WRITE))
        assertTrue(DocumentPermission.DEFAULT_DOCUMENT_PERMISSIONS.contains(DocumentPermission.READ))
        assertTrue(DocumentPermission.READ_ONLY_PERMISSIONS.contains(DocumentPermission.READ))
        assertFalse(DocumentPermission.READ_ONLY_PERMISSIONS.contains(DocumentPermission.WRITE))
    }

    @Test
    fun `DocumentType enum works correctly`() {
        assertEquals(3, DocumentType.values().size)
        assertTrue(DocumentType.values().contains(DocumentType.FILE))
        assertTrue(DocumentType.values().contains(DocumentType.FOLDER))
        assertTrue(DocumentType.values().contains(DocumentType.UNKNOWN))
    }

    @Test
    fun `OperationStatus enum works correctly`() {
        assertEquals(6, OperationStatus.values().size)
        assertTrue(OperationStatus.values().contains(OperationStatus.PENDING))
        assertTrue(OperationStatus.values().contains(OperationStatus.IN_PROGRESS))
        assertTrue(OperationStatus.values().contains(OperationStatus.COMPLETED))
        assertTrue(OperationStatus.values().contains(OperationStatus.FAILED))
        assertTrue(OperationStatus.values().contains(OperationStatus.PAUSED))
        assertTrue(OperationStatus.values().contains(OperationStatus.CANCELLED))
    }

    @Test
    fun `OperationType enum works correctly`() {
        assertEquals(9, OperationType.values().size)
        assertTrue(OperationType.values().contains(OperationType.UPLOAD))
        assertTrue(OperationType.values().contains(OperationType.DOWNLOAD))
        assertTrue(OperationType.values().contains(OperationType.DELETE))
        assertTrue(OperationType.values().contains(OperationType.CREATE_FOLDER))
        assertTrue(OperationType.values().contains(OperationType.RENAME))
        assertTrue(OperationType.values().contains(OperationType.COPY))
        assertTrue(OperationType.values().contains(OperationType.MOVE))
        assertTrue(OperationType.values().contains(OperationType.SYNC))
        assertTrue(OperationType.values().contains(OperationType.SEARCH))
    }

    @Test
    fun `SyncStatus enum works correctly`() {
        assertEquals(11, SyncStatus.values().size)
        assertTrue(SyncStatus.values().contains(SyncStatus.UNKNOWN))
        assertTrue(SyncStatus.values().contains(SyncStatus.SYNCED))
        assertTrue(SyncStatus.values().contains(SyncStatus.PENDING_UPLOAD))
        assertTrue(SyncStatus.values().contains(SyncStatus.PENDING_DOWNLOAD))
        assertTrue(SyncStatus.values().contains(SyncStatus.SYNCING))
        assertTrue(SyncStatus.values().contains(SyncStatus.SYNC_ERROR))
        assertTrue(SyncStatus.values().contains(SyncStatus.NOT_SYNCED))
        assertTrue(SyncStatus.values().contains(SyncStatus.QUEUED))
        assertTrue(SyncStatus.values().contains(SyncStatus.CONFLICT))
        assertTrue(SyncStatus.values().contains(SyncStatus.UPLOADING))
        assertTrue(SyncStatus.values().contains(SyncStatus.DOWNLOADING))
    }

    @Test
    fun `ConflictResolution enum works correctly`() {
        assertEquals(5, ConflictResolution.values().size)
        assertTrue(ConflictResolution.values().contains(ConflictResolution.LOCAL_WINS))
        assertTrue(ConflictResolution.values().contains(ConflictResolution.REMOTE_WINS))
        assertTrue(ConflictResolution.values().contains(ConflictResolution.KEEP_BOTH))
        assertTrue(ConflictResolution.values().contains(ConflictResolution.MANUAL))
        assertTrue(ConflictResolution.values().contains(ConflictResolution.SKIP))
    }

    @Test
    fun `StorageType enum properties work correctly`() {
        // Test display names
        assertEquals("WebDAV", StorageType.WEBDAV.displayName)
        assertEquals("FTP", StorageType.FTP.displayName)
        assertEquals("SFTP", StorageType.SFTP.displayName)
        assertEquals("SMB/CIFS", StorageType.SMB.displayName)
        assertEquals("Google Drive", StorageType.GOOGLE_DRIVE.displayName)
        assertEquals("Dropbox", StorageType.DROPBOX.displayName)
        assertEquals("OneDrive", StorageType.ONEDRIVE.displayName)
        assertEquals("Git", StorageType.GIT.displayName)

        // Test supportsFolders
        assertTrue(StorageType.WEBDAV.supportsFolders)
        assertTrue(StorageType.SFTP.supportsFolders)
        assertTrue(StorageType.SMB.supportsFolders)
        assertTrue(StorageType.GOOGLE_DRIVE.supportsFolders)
        assertTrue(StorageType.DROPBOX.supportsFolders)
        assertTrue(StorageType.ONEDRIVE.supportsFolders)
        assertTrue(StorageType.GIT.supportsFolders)
        assertFalse(StorageType.FTP.supportsFolders)

        // Test supportsEncryption
        assertTrue(StorageType.WEBDAV.supportsEncryption)
        assertTrue(StorageType.SFTP.supportsEncryption)
        assertTrue(StorageType.SMB.supportsEncryption)
        assertTrue(StorageType.GOOGLE_DRIVE.supportsEncryption)
        assertTrue(StorageType.DROPBOX.supportsEncryption)
        assertTrue(StorageType.ONEDRIVE.supportsEncryption)
        assertTrue(StorageType.GIT.supportsEncryption)
        assertFalse(StorageType.FTP.supportsEncryption)

        // Test default ports
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
    fun `WebDavAuthenticationType enum works correctly`() {
        assertEquals(4, WebDavAuthenticationType.values().size)
        assertTrue(WebDavAuthenticationType.values().contains(WebDavAuthenticationType.BASIC))
        assertTrue(WebDavAuthenticationType.values().contains(WebDavAuthenticationType.DIGEST))
        assertTrue(WebDavAuthenticationType.values().contains(WebDavAuthenticationType.OAUTH))
        assertTrue(WebDavAuthenticationType.values().contains(WebDavAuthenticationType.NONE))
    }

    @Test
    fun `OneDriveDriveType enum works correctly`() {
        assertEquals(4, OneDriveDriveType.values().size)
        assertTrue(OneDriveDriveType.values().contains(OneDriveDriveType.ME))
        assertTrue(OneDriveDriveType.values().contains(OneDriveDriveType.BUSINESS))
        assertTrue(OneDriveDriveType.values().contains(OneDriveDriveType.SHAREPOINT))
        assertTrue(OneDriveDriveType.values().contains(OneDriveDriveType.GROUP))
    }

    @Test
    fun `CacheEntry data class works correctly`() {
        val now = Clock.System.now()
        val cacheEntry = CacheEntry(
            id = "test-cache",
            remoteDocumentId = "doc1",
            localPath = "/cache/test.txt",
            remotePath = "/test.txt",
            size = 1024L,
            createdAt = now,
            lastAccessed = now,
            lastModified = now,
            expiresAt = now.plus(1.hours),
            isPinned = false,
            accessCount = 5
        )

        // Test basic properties
        assertEquals("test-cache", cacheEntry.id)
        assertEquals("doc1", cacheEntry.remoteDocumentId)
        assertEquals("/cache/test.txt", cacheEntry.localPath)
        assertEquals("/test.txt", cacheEntry.remotePath)
        assertEquals(1024L, cacheEntry.size)
        assertEquals(now, cacheEntry.createdAt)
        assertEquals(now, cacheEntry.lastAccessed)
        assertEquals(now, cacheEntry.lastModified)
        assertEquals(5, cacheEntry.accessCount)

        // Test computed properties
        assertTrue(cacheEntry.isValid)
        assertFalse(cacheEntry.isExpired)
        assertTrue(cacheEntry.canBeEvicted)
        assertNull(cacheEntry.compressionRatio)
        assertTrue(cacheEntry.age >= 0)
        assertTrue(cacheEntry.timeSinceLastAccess >= 0)
        assertTrue(cacheEntry.accessFrequency >= 0.0)

        // Test copy methods
        val accessedEntry = cacheEntry.withAccess()
        assertEquals(6, accessedEntry.accessCount)
        assertTrue(accessedEntry.isInUse)

        val notInUseEntry = accessedEntry.withNotInUse()
        assertFalse(notInUseEntry.isInUse)

        val expiredEntry = cacheEntry.withExpiration(null)
        assertTrue(expiredEntry.isValid)

        val updatedEntry = cacheEntry.withContent(2048L, "new-checksum", now)
        assertEquals(2048L, updatedEntry.size)
        assertEquals("new-checksum", updatedEntry.checksum)

        val pinnedEntry = cacheEntry.withPinStatus(true)
        assertTrue(pinnedEntry.isPinned)
        assertFalse(pinnedEntry.canBeEvicted)

        val priorityEntry = cacheEntry.withPriority(50)
        assertEquals(50, priorityEntry.priority)
    }

    @Test
    fun `CacheEntry companion object functions work correctly`() {
        val cacheEntry = CacheEntry.create(
            remoteDocumentId = "doc1",
            localPath = "/cache/test.txt",
            remotePath = "/test.txt",
            size = 1024L,
            ttl = 3600000L, // 1 hour
            isPinned = true
        )

        assertNotNull(cacheEntry.id)
        assertEquals("doc1", cacheEntry.remoteDocumentId)
        assertEquals("/cache/test.txt", cacheEntry.localPath)
        assertEquals("/test.txt", cacheEntry.remotePath)
        assertEquals(1024L, cacheEntry.size)
        assertTrue(cacheEntry.isPinned)
        assertNotNull(cacheEntry.expiresAt)

        val mockEntry = CacheEntry.mock()
        assertNotNull(mockEntry.id)
        assertEquals("doc1", mockEntry.remoteDocumentId)
        assertEquals("/cache/test.txt", mockEntry.localPath)
        assertEquals("/test.txt", mockEntry.remotePath)
        assertEquals(1024L, mockEntry.size)
        assertEquals(5, mockEntry.accessCount)
    }

    @Test
    fun `NetworkOperation data class works correctly`() {
        val now = Clock.System.now()
        val operation = NetworkOperation(
            id = 1L,
            type = NetworkOperation.Type.UPLOAD,
            remotePath = "/test.txt",
            localPath = "/local/test.txt",
            status = NetworkOperation.Status.IN_PROGRESS,
            progress = 0.5,
            totalSize = 1024L,
            bytesTransferred = 512L,
            createdAt = now,
            startedAt = now,
            priority = 50,
            canPause = true,
            canCancel = true,
            maxRetries = 5
        )

        // Test basic properties
        assertEquals(1L, operation.id)
        assertEquals(NetworkOperation.Type.UPLOAD, operation.type)
        assertEquals("/test.txt", operation.remotePath)
        assertEquals("/local/test.txt", operation.localPath)
        assertEquals(NetworkOperation.Status.IN_PROGRESS, operation.status)
        assertEquals(0.5, operation.progress)
        assertEquals(1024L, operation.totalSize)
        assertEquals(512L, operation.bytesTransferred)
        assertEquals(now, operation.createdAt)
        assertEquals(now, operation.startedAt)
        assertEquals(50, operation.priority)
        assertEquals(5, operation.maxRetries)

        // Test computed properties
        assertTrue(operation.isRunning)
        assertFalse(operation.isCompleted)
        assertFalse(operation.hasFailed)
        assertFalse(operation.canRetry)
        assertFalse(operation.isPending)
        assertEquals(50, operation.progressPercentage)
        assertNotNull(operation.duration)

        // Test copy methods
        val completedOperation = operation.withStatus(NetworkOperation.Status.COMPLETED)
        assertEquals(NetworkOperation.Status.COMPLETED, completedOperation.status)
        assertNotNull(completedOperation.completedAt)

        val progressOperation = operation.withProgress(0.75, 768L)
        assertEquals(0.75, progressOperation.progress)
        assertEquals(768L, progressOperation.bytesTransferred)

        val errorOperation = operation.withError("Test error")
        assertEquals(NetworkOperation.Status.FAILED, errorOperation.status)
        assertEquals("Test error", errorOperation.error)
        assertEquals(1, errorOperation.retryCount)

        val pausedOperation = operation.withPauseStatus(true)
        assertTrue(pausedOperation.isPaused)

        val priorityOperation = operation.withPriority(25)
        assertEquals(25, priorityOperation.priority)
    }

    @Test
    fun `NetworkOperation companion object functions work correctly`() {
        val errorOperation = NetworkOperation.error(
            id = 1L,
            operationType = NetworkOperation.Type.DOWNLOAD,
            remotePath = "/test.txt",
            localPath = "/local/test.txt",
            error = "Download failed"
        )

        assertEquals(1L, errorOperation.id)
        assertEquals(NetworkOperation.Type.DOWNLOAD, errorOperation.type)
        assertEquals("/test.txt", errorOperation.remotePath)
        assertEquals("/local/test.txt", errorOperation.localPath)
        assertEquals(NetworkOperation.Status.FAILED, errorOperation.status)
        assertEquals("Download failed", errorOperation.error)
        assertNotNull(errorOperation.createdAt)
        assertNotNull(errorOperation.startedAt)
        assertNotNull(errorOperation.completedAt)

        val uploadOperation = NetworkOperation.createUpload(
            id = "upload1",
            localPath = "/local/test.txt",
            remotePath = "/test.txt",
            totalSize = 1024L,
            priority = 75
        )

        assertEquals(NetworkOperation.Type.UPLOAD, uploadOperation.type)
        assertEquals("/local/test.txt", uploadOperation.localPath)
        assertEquals("/test.txt", uploadOperation.remotePath)
        assertEquals(1024L, uploadOperation.totalSize)
        assertEquals(75, uploadOperation.priority)

        val downloadOperation = NetworkOperation.createDownload(
            id = "download1",
            remotePath = "/test.txt",
            localPath = "/local/test.txt",
            totalSize = 2048L,
            priority = 80
        )

        assertEquals(NetworkOperation.Type.DOWNLOAD, downloadOperation.type)
        assertEquals("/test.txt", downloadOperation.remotePath)
        assertEquals("/local/test.txt", downloadOperation.localPath)
        assertEquals(2048L, downloadOperation.totalSize)
        assertEquals(80, downloadOperation.priority)

        val deleteOperation = NetworkOperation.createDelete(
            id = 2L,
            remotePath = "/test.txt",
            priority = 90
        )

        assertEquals(NetworkOperation.Type.DELETE, deleteOperation.type)
        assertEquals("/test.txt", deleteOperation.remotePath)
        assertNull(deleteOperation.localPath)
        assertFalse(deleteOperation.canPause)
        assertEquals(90, deleteOperation.priority)

        val folderOperation = NetworkOperation.createFolder(
            id = 3L,
            remotePath = "/new-folder",
            priority = 85
        )

        assertEquals(NetworkOperation.Type.CREATE_FOLDER, folderOperation.type)
        assertEquals("/new-folder", folderOperation.remotePath)
        assertFalse(folderOperation.canPause)
        assertEquals(85, folderOperation.priority)

        val syncOperation = NetworkOperation.createSync(
            id = "sync1",
            remotePath = "/sync-folder",
            priority = 70
        )

        assertEquals(NetworkOperation.Type.SYNC, syncOperation.type)
        assertEquals("/sync-folder", syncOperation.remotePath)
        assertTrue(syncOperation.canPause)
        assertEquals(70, syncOperation.priority)

        val mockOperation = NetworkOperation.mock()
        assertEquals(1L, mockOperation.id)
        assertEquals(NetworkOperation.Type.UPLOAD, mockOperation.type)
        assertEquals("/test.txt", mockOperation.remotePath)
        assertEquals("/local/test.txt", mockOperation.localPath)
        assertEquals(0.5, mockOperation.progress)
        assertEquals(1024L, mockOperation.totalSize)
        assertEquals(512L, mockOperation.bytesTransferred)
    }

    @Test
    fun `NetworkStorage data class works correctly`() {
        val storage = NetworkStorage(
            id = "test-storage",
            name = "Test Storage",
            type = StorageType.WEBDAV,
            location = "https://example.com/webdav",
            totalSpace = 1000000L,
            usedSpace = 500000L,
            isOnline = true,
            lastSync = Clock.System.now(),
            priority = 50,
            supportsFolders = true,
            supportsMetadata = true,
            maxFileSize = 100000L,
            supportedExtensions = listOf("txt", "md", "pdf")
        )

        // Test basic properties
        assertEquals("test-storage", storage.id)
        assertEquals("Test Storage", storage.name)
        assertEquals(StorageType.WEBDAV, storage.type)
        assertEquals("https://example.com/webdav", storage.location)
        assertEquals(1000000L, storage.totalSpace)
        assertEquals(500000L, storage.usedSpace)
        assertTrue(storage.isOnline)
        assertNotNull(storage.lastSync)
        assertEquals(50, storage.priority)
        assertTrue(storage.supportsFolders)
        assertTrue(storage.supportsMetadata)
        assertEquals(100000L, storage.maxFileSize)
        assertEquals(listOf("txt", "md", "pdf"), storage.supportedExtensions)

        // Test computed properties
        assertEquals(500000L, storage.availableSpace)
        assertEquals(0.5, storage.usagePercentage)
        assertFalse(storage.isFull)
        assertFalse(storage.isLowOnSpace)

        // Test methods
        assertTrue(storage.supportsExtension("txt"))
        assertTrue(storage.supportsExtension("MD")) // case insensitive
        assertFalse(storage.supportsExtension("doc"))

        assertTrue(storage.supportsFile("test.txt"))
        assertTrue(storage.supportsFile("document.md"))
        assertFalse(storage.supportsFile("document.doc"))

        // Test copy methods
        val offlineStorage = storage.withOnlineStatus(false)
        assertFalse(offlineStorage.isOnline)

        val syncedStorage = storage.withLastSync(Clock.System.now())
        assertNotNull(syncedStorage.lastSync)

        val spaceStorage = storage.withSpaceInfo(2000000L, 750000L)
        assertEquals(2000000L, spaceStorage.totalSpace)
        assertEquals(750000L, spaceStorage.usedSpace)
    }

    @Test
    fun `NetworkStorage companion object functions work correctly`() {
        val mockStorage = NetworkStorage.mock()

        assertEquals("mock-storage", mockStorage.id)
        assertEquals("Mock Storage", mockStorage.name)
        assertEquals(StorageType.WEBDAV, mockStorage.type)
        assertEquals("mock://localhost", mockStorage.location)
        assertEquals(1000000000L, mockStorage.totalSpace)
        assertEquals(100000000L, mockStorage.usedSpace)
        assertTrue(mockStorage.isOnline)
        assertNotNull(mockStorage.lastSync)
    }

    @Test
    fun `StorageConfig sealed class works correctly`() {
        // Test WebDavConfig
        val webdavConfig = StorageConfig.WebDavConfig(
            name = "Test WebDAV",
            url = "https://example.com/webdav",
            username = "user",
            password = "pass",
            authenticationType = WebDavAuthenticationType.BASIC,
            sslEnabled = true,
            verifyCertificate = true,
            connectionTimeout = 30000,
            readTimeout = 60000,
            isEnabled = true,
            priority = 100
        )

        assertEquals("Test WebDAV", webdavConfig.name)
        assertEquals("https://example.com/webdav", webdavConfig.url)
        assertEquals("user", webdavConfig.username)
        assertEquals("pass", webdavConfig.password)
        assertEquals(WebDavAuthenticationType.BASIC, webdavConfig.authenticationType)
        assertTrue(webdavConfig.sslEnabled)
        assertTrue(webdavConfig.verifyCertificate)
        assertEquals(30000, webdavConfig.connectionTimeout)
        assertEquals(60000, webdavConfig.readTimeout)
        assertTrue(webdavConfig.isEnabled)
        assertEquals(100, webdavConfig.priority)
        assertEquals(StorageType.WEBDAV, webdavConfig.storageType)

        // Test copy methods
        val disabledConfig = webdavConfig.withEnabled(false)
        assertFalse(disabledConfig.isEnabled)

        val priorityConfig = webdavConfig.withPriority(50)
        assertEquals(50, priorityConfig.priority)

        val metadataConfig = webdavConfig.withMetadata(mapOf("key" to "value"))
        assertEquals(mapOf("key" to "value"), metadataConfig.metadata)

        // Test FtpConfig
        val ftpConfig = StorageConfig.FtpConfig(
            name = "Test FTP",
            host = "ftp.example.com",
            port = 21,
            username = "user",
            password = "pass",
            rootPath = "/",
            passiveMode = true,
            secureFtp = false,
            encoding = "UTF-8",
            connectionTimeout = 30000,
            isEnabled = true,
            priority = 100
        )

        assertEquals("Test FTP", ftpConfig.name)
        assertEquals("ftp.example.com", ftpConfig.host)
        assertEquals(21, ftpConfig.port)
        assertEquals("user", ftpConfig.username)
        assertEquals("pass", ftpConfig.password)
        assertEquals("/", ftpConfig.rootPath)
        assertTrue(ftpConfig.passiveMode)
        assertFalse(ftpConfig.secureFtp)
        assertEquals("UTF-8", ftpConfig.encoding)
        assertEquals(30000, ftpConfig.connectionTimeout)
        assertTrue(ftpConfig.isEnabled)
        assertEquals(100, ftpConfig.priority)
        assertEquals(StorageType.FTP, ftpConfig.storageType)

        // Test SftpConfig
        val sftpConfig = StorageConfig.SftpConfig(
            name = "Test SFTP",
            host = "sftp.example.com",
            port = 22,
            username = "user",
            password = "pass",
            privateKeyPath = "/path/to/key",
            privateKeyPassphrase = "passphrase",
            knownHostsPath = "/path/to/known_hosts",
            strictHostKeyChecking = true,
            rootPath = "/",
            useSsl = true,
            connectionTimeout = 30000,
            isEnabled = true,
            priority = 100
        )

        assertEquals("Test SFTP", sftpConfig.name)
        assertEquals("sftp.example.com", sftpConfig.host)
        assertEquals(22, sftpConfig.port)
        assertEquals("user", sftpConfig.username)
        assertEquals("pass", sftpConfig.password)
        assertEquals("/path/to/key", sftpConfig.privateKeyPath)
        assertEquals("passphrase", sftpConfig.privateKeyPassphrase)
        assertEquals("/path/to/known_hosts", sftpConfig.knownHostsPath)
        assertTrue(sftpConfig.strictHostKeyChecking)
        assertEquals("/", sftpConfig.rootPath)
        assertTrue(sftpConfig.useSsl)
        assertEquals(30000, sftpConfig.connectionTimeout)
        assertTrue(sftpConfig.isEnabled)
        assertEquals(100, sftpConfig.priority)
        assertEquals(StorageType.SFTP, sftpConfig.storageType)

        // Test other config types briefly
        val smbConfig = StorageConfig.SmbConfig(
            name = "Test SMB",
            host = "smb.example.com",
            share = "share",
            username = "user",
            password = "pass"
        )
        assertEquals(StorageType.SMB, smbConfig.storageType)

        val googleDriveConfig = StorageConfig.GoogleDriveConfig(
            name = "Test Google Drive",
            clientId = "client_id",
            clientSecret = "client_secret"
        )
        assertEquals(StorageType.GOOGLE_DRIVE, googleDriveConfig.storageType)

        val dropboxConfig = StorageConfig.DropboxConfig(
            name = "Test Dropbox",
            accessToken = "access_token",
            appKey = "app_key",
            appSecret = "app_secret"
        )
        assertEquals(StorageType.DROPBOX, dropboxConfig.storageType)

        val oneDriveConfig = StorageConfig.OneDriveConfig(
            name = "Test OneDrive",
            clientId = "client_id",
            clientSecret = "client_secret"
        )
        assertEquals(StorageType.ONEDRIVE, oneDriveConfig.storageType)

        val gitConfig = StorageConfig.GitConfig(
            name = "Test Git",
            repositoryUrl = "https://github.com/user/repo.git",
            localCachePath = "/cache/git"
        )
        assertEquals(StorageType.GIT, gitConfig.storageType)
    }
}