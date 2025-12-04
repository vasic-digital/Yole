package digital.vasic.yole.network.common

/**
 * Represents permissions that can be granted on documents and folders.
 * These permissions control what operations users can perform on network-stored files.
 */
enum class DocumentPermission {
    /**
     * Can read the document content
     */
    READ,
    
    /**
     * Can write/modify the document content
     */
    WRITE,
    
    /**
     * Can delete the document
     */
    DELETE,
    
    /**
     * Can create new documents in this folder
     */
    CREATE,
    
    /**
     * Can rename the document
     */
    RENAME,
    
    /**
     * Can move the document to another location
     */
    MOVE,
    
    /**
     * Can copy the document
     */
    COPY,
    
    /**
     * Can share the document with other users
     */
    SHARE,
    
    /**
     * Can change permissions on the document
     */
    MANAGE_PERMISSIONS,
    
    /**
     * Can view document metadata
     */
    VIEW_METADATA,
    
    /**
     * Can modify document metadata
     */
    MODIFY_METADATA,
    
    /**
     * Can execute the document (if it's executable)
     */
    EXECUTE,
    
    /**
     * Can download the document
     */
    DOWNLOAD,
    
    /**
     * Can upload new documents to this folder
     */
    UPLOAD,
    
    /**
     * Can sync the document
     */
    SYNC,
    
    /**
     * Has full administrative access
     */
    ADMIN;
    
    /**
     * Whether this permission is related to reading operations
     */
    val isReadPermission: Boolean
        get() = when (this) {
            READ, VIEW_METADATA, DOWNLOAD -> true
            else -> false
        }
    
    /**
     * Whether this permission is related to writing/modification operations
     */
    val isWritePermission: Boolean
        get() = when (this) {
            WRITE, CREATE, RENAME, MOVE, COPY, UPLOAD, MODIFY_METADATA -> true
            else -> false
        }
    
    /**
     * Whether this permission is related to administrative operations
     */
    val isAdministrativePermission: Boolean
        get() = when (this) {
            DELETE, SHARE, MANAGE_PERMISSIONS, EXECUTE, SYNC, ADMIN -> true
            else -> false
        }
    
    /**
     * Human-readable display name
     */
    val displayName: String
        get() = when (this) {
            READ -> "Read"
            WRITE -> "Write"
            DELETE -> "Delete"
            CREATE -> "Create"
            RENAME -> "Rename"
            MOVE -> "Move"
            COPY -> "Copy"
            SHARE -> "Share"
            MANAGE_PERMISSIONS -> "Manage Permissions"
            VIEW_METADATA -> "View Metadata"
            MODIFY_METADATA -> "Modify Metadata"
            EXECUTE -> "Execute"
            DOWNLOAD -> "Download"
            UPLOAD -> "Upload"
            SYNC -> "Sync"
            ADMIN -> "Administrator"
        }
    
    /**
     * Description of what this permission allows
     */
    val description: String
        get() = when (this) {
            READ -> "Can read document content"
            WRITE -> "Can modify document content"
            DELETE -> "Can delete the document"
            CREATE -> "Can create new documents in this folder"
            RENAME -> "Can rename the document"
            MOVE -> "Can move the document to another location"
            COPY -> "Can create copies of the document"
            SHARE -> "Can share the document with other users"
            MANAGE_PERMISSIONS -> "Can change permissions on the document"
            VIEW_METADATA -> "Can view document metadata"
            MODIFY_METADATA -> "Can modify document metadata"
            EXECUTE -> "Can execute the document (if executable)"
            DOWNLOAD -> "Can download the document"
            UPLOAD -> "Can upload new documents to this folder"
            SYNC -> "Can synchronize the document"
            ADMIN -> "Has full administrative access"
        }
    
    companion object {
        /**
         * Default permissions for folders
         */
        val DEFAULT_FOLDER_PERMISSIONS = setOf(
            READ, WRITE, CREATE, RENAME, MOVE, COPY, DELETE, UPLOAD, DOWNLOAD, SYNC
        )
        
        /**
         * Default permissions for documents
         */
        val DEFAULT_DOCUMENT_PERMISSIONS = setOf(
            READ, WRITE, RENAME, MOVE, COPY, DELETE, DOWNLOAD, SYNC
        )
        
        /**
         * Read-only permissions
         */
        val READ_ONLY_PERMISSIONS = setOf(READ, VIEW_METADATA, DOWNLOAD)
        
        /**
         * All permissions
         */
        val ALL_PERMISSIONS = values().toSet()
        
        /**
         * Check if a set of permissions includes a specific permission
         */
        fun hasPermission(permissions: Set<DocumentPermission>, permission: DocumentPermission): Boolean {
            return permissions.contains(permission) || permissions.contains(ADMIN)
        }
        
        /**
         * Check if a set of permissions includes any read permissions
         */
        fun hasReadPermissions(permissions: Set<DocumentPermission>): Boolean {
            return permissions.any { it.isReadPermission } || permissions.contains(ADMIN)
        }
        
        /**
         * Check if a set of permissions includes any write permissions
         */
        fun hasWritePermissions(permissions: Set<DocumentPermission>): Boolean {
            return permissions.any { it.isWritePermission } || permissions.contains(ADMIN)
        }
        
        /**
         * Check if a set of permissions includes any administrative permissions
         */
        fun hasAdministrativePermissions(permissions: Set<DocumentPermission>): Boolean {
            return permissions.any { it.isAdministrativePermission } || permissions.contains(ADMIN)
        }
        
        /**
         * Get all permissions that are a subset of the given permissions
         */
        fun getEffectivePermissions(permissions: Set<DocumentPermission>): Set<DocumentPermission> {
            return if (permissions.contains(ADMIN)) {
                ALL_PERMISSIONS
            } else {
                permissions
            }
        }
    }
}