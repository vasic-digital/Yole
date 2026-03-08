/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 *########################################################*/
package digital.vasic.yole.network.common

import kotlinx.datetime.Instant

/**
 * Represents a document or folder stored on a network storage location.
 * This is the primary data class for file and folder representation.
 */
data class NetworkDocument(
    /**
     * Unique identifier for this document within the storage
     */
    val id: String,
    
    /**
     * Human-readable name of the document
     */
    val name: String,
    
    /**
     * Full path to the document within the storage
     */
    val path: String = "",
    
    /**
     * Whether this is a folder
     */
    val isFolder: Boolean = false,
    
    /**
     * File size in bytes (0 for folders)
     */
    val size: Long = 0L,
    
    /**
     * Last modification timestamp
     */
    val lastModified: Instant? = null,
    
    /**
     * Current synchronization status
     */
    val syncStatus: SyncStatus = SyncStatus.UNKNOWN,
    
    /**
     * Associated local document ID (null if not linked to local document)
     */
    val documentId: String? = null,
    
    /**
     * Content type/MIME type (null for folders)
     */
    val contentType: String? = null,
    
    /**
     * File extension without the dot (empty for folders)
     */
    val extension: String = name.substringAfterLast('.', "").ifEmpty { "" },
    
    /**
     * Parent directory path
     */
    val parentPath: String = path.substringBeforeLast('/', "").ifEmpty { "/" },
    
    /**
     * Whether the document is currently being synchronized
     */
    val isSyncing: Boolean = syncStatus == SyncStatus.SYNCING,
    
    /**
     * Whether the document has local changes pending upload
     */
    val hasPendingChanges: Boolean = syncStatus == SyncStatus.PENDING_UPLOAD,
    
    /**
     * Whether the document is available offline (cached locally)
     */
    val isAvailableOffline: Boolean = syncStatus == SyncStatus.SYNCED,
    
    /**
     * Whether the document is read-only
     */
    val isReadOnly: Boolean = false,
    
    /**
     * Whether the document is hidden
     */
    val isHidden: Boolean = false,
    
    /**
     * Additional metadata from the storage provider
     */
    val metadata: Map<String, String> = emptyMap(),
    
    /**
     * Thumbnails available for this document (URLs or paths)
     */
    val thumbnails: List<String> = emptyList(),
    
    /**
     * Tags or labels for this document
     */
    val tags: List<String> = emptyList(),
    
    /**
     * Document owner (null if unknown)
     */
    val owner: String? = null,
    
    /**
     * Document permissions
     */
    val permissions: Set<DocumentPermission> = emptySet(),
    
    /**
     * Storage ID this document belongs to
     */
    val storageId: String = "",
    
    /**
     * Author of the document
     */
    val author: String? = null
) {
    /**
     * Whether this document is a text file that can be edited
     */
    val isTextFile: Boolean
        get() = !isFolder && TEXT_EXTENSIONS.contains(extension.lowercase())
    
    /**
     * Whether this document is an image file
     */
    val isImageFile: Boolean
        get() = !isFolder && IMAGE_EXTENSIONS.contains(extension.lowercase())
    
    /**
     * Whether this document is a PDF file
     */
    val isPdfFile: Boolean
        get() = !isFolder && extension.lowercase() == "pdf"
    
    /**
     * Whether this document can be previewed in the app
     */
    val isPreviewable: Boolean
        get() = isTextFile || isImageFile || isPdfFile
    
    /**
     * Whether this document can be edited
     */
    val isEditable: Boolean
        get() = isTextFile && !isReadOnly
    
    /**
     * Formatted file size for display
     */
    val formattedSize: String
        get() = when {
            isFolder -> "—"
            size < 1024 -> "${size}B"
            size < 1024 * 1024 -> "${size / 1024}KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)}MB"
            else -> "${size / (1024 * 1024 * 1024)}GB"
        }
    
    /**
     * Check if this document is in the specified path or its subdirectories
     */
    fun isInPath(path: String): Boolean {
        val normalizedPath = path.trimEnd('/')
        val normalizedDocPath = this.path.trimEnd('/')
        return normalizedDocPath.startsWith("$normalizedPath/")
    }
    
    /**
     * Check if this document is a direct child of the specified path
     */
    fun isDirectChildOf(path: String): Boolean {
        val normalizedPath = path.trimEnd('/')
        val normalizedDocPath = this.path.trimEnd('/')
        val parentPath = normalizedDocPath.substringBeforeLast('/', "")
        return parentPath == normalizedPath
    }
    
    /**
     * Create a copy with updated sync status
     */
    fun withSyncStatus(syncStatus: SyncStatus): NetworkDocument {
        return copy(syncStatus = syncStatus)
    }
    
    /**
     * Create a copy with updated local document ID
     */
    fun withDocumentId(documentId: String?): NetworkDocument {
        return copy(documentId = documentId)
    }
    
    /**
     * Create a copy with updated metadata
     */
    fun withMetadata(metadata: Map<String, String>): NetworkDocument {
        return copy(metadata = metadata)
    }
    
    /**
     * Create a copy with updated permissions
     */
    fun withPermissions(permissions: Set<DocumentPermission>): NetworkDocument {
        return copy(permissions = permissions)
    }
    
    companion object {
        /**
         * Extensions that represent text files
         */
        private val TEXT_EXTENSIONS = setOf(
            "txt", "md", "markdown", "rst", "adoc", "org", "wiki", "tex",
            "json", "xml", "yaml", "yml", "toml", "ini", "cfg", "conf",
            "csv", "tsv", "log", "sql", "sh", "bat", "ps1", "py", "js",
            "ts", "kt", "java", "cpp", "c", "h", "hpp", "go", "rs",
            "php", "rb", "swift", "dart", "scala", "clj", "hs", "ml"
        )
        
        /**
         * Extensions that represent image files
         */
        private val IMAGE_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico",
            "tiff", "tif", "psd", "raw", "cr2", "nef", "arw"
        )
        
        /**
         * Create a mock document for testing purposes
         */
        fun mock(
            name: String = "test.txt",
            path: String = "/test.txt",
            isFolder: Boolean = false,
            size: Long = 1024L,
            syncStatus: SyncStatus = SyncStatus.SYNCED,
            storageId: String = "test-storage"
        ): NetworkDocument {
            return NetworkDocument(
                id = "mock-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}-${(0..9999).random()}",
                name = name,
                path = path,
                isFolder = isFolder,
                size = size,
                lastModified = kotlinx.datetime.Clock.System.now(),
                syncStatus = syncStatus,
                storageId = storageId
            )
        }
        
        /**
         * Create a mock folder for testing purposes
         */
        fun mockFolder(
            name: String = "folder",
            path: String = "/folder",
            storageId: String = "test-storage"
        ): NetworkDocument {
            return NetworkDocument(
                id = "mock-folder-${kotlinx.datetime.Clock.System.now().epochSeconds}",
                name = name,
                path = path,
                isFolder = true,
                lastModified = kotlinx.datetime.Clock.System.now(),
                syncStatus = SyncStatus.SYNCED,
                storageId = storageId
            )
        }
    }
}