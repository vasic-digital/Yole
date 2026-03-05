/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for DocumentPermission enum and companion utilities
 *
 *########################################################*/
package digital.vasic.yole.network.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DocumentPermissionTest {

    @Test
    fun testAllPermissionsExist() {
        assertEquals(16, DocumentPermission.entries.size)
    }

    @Test
    fun testReadPermissionClassification() {
        assertTrue(DocumentPermission.READ.isReadPermission)
        assertTrue(DocumentPermission.VIEW_METADATA.isReadPermission)
        assertTrue(DocumentPermission.DOWNLOAD.isReadPermission)
        assertFalse(DocumentPermission.WRITE.isReadPermission)
        assertFalse(DocumentPermission.DELETE.isReadPermission)
        assertFalse(DocumentPermission.ADMIN.isReadPermission)
    }

    @Test
    fun testWritePermissionClassification() {
        assertTrue(DocumentPermission.WRITE.isWritePermission)
        assertTrue(DocumentPermission.CREATE.isWritePermission)
        assertTrue(DocumentPermission.RENAME.isWritePermission)
        assertTrue(DocumentPermission.MOVE.isWritePermission)
        assertTrue(DocumentPermission.COPY.isWritePermission)
        assertTrue(DocumentPermission.UPLOAD.isWritePermission)
        assertTrue(DocumentPermission.MODIFY_METADATA.isWritePermission)
        assertFalse(DocumentPermission.READ.isWritePermission)
        assertFalse(DocumentPermission.DELETE.isWritePermission)
        assertFalse(DocumentPermission.ADMIN.isWritePermission)
    }

    @Test
    fun testAdministrativePermissionClassification() {
        assertTrue(DocumentPermission.DELETE.isAdministrativePermission)
        assertTrue(DocumentPermission.SHARE.isAdministrativePermission)
        assertTrue(DocumentPermission.MANAGE_PERMISSIONS.isAdministrativePermission)
        assertTrue(DocumentPermission.EXECUTE.isAdministrativePermission)
        assertTrue(DocumentPermission.SYNC.isAdministrativePermission)
        assertTrue(DocumentPermission.ADMIN.isAdministrativePermission)
        assertFalse(DocumentPermission.READ.isAdministrativePermission)
        assertFalse(DocumentPermission.WRITE.isAdministrativePermission)
    }

    @Test
    fun testEveryPermissionIsClassified() {
        DocumentPermission.entries.forEach { permission ->
            val classified = permission.isReadPermission ||
                    permission.isWritePermission ||
                    permission.isAdministrativePermission
            assertTrue(classified, "$permission should be classified in at least one category")
        }
    }

    @Test
    fun testDisplayNames() {
        assertEquals("Read", DocumentPermission.READ.displayName)
        assertEquals("Write", DocumentPermission.WRITE.displayName)
        assertEquals("Delete", DocumentPermission.DELETE.displayName)
        assertEquals("Create", DocumentPermission.CREATE.displayName)
        assertEquals("Rename", DocumentPermission.RENAME.displayName)
        assertEquals("Move", DocumentPermission.MOVE.displayName)
        assertEquals("Copy", DocumentPermission.COPY.displayName)
        assertEquals("Share", DocumentPermission.SHARE.displayName)
        assertEquals("Manage Permissions", DocumentPermission.MANAGE_PERMISSIONS.displayName)
        assertEquals("View Metadata", DocumentPermission.VIEW_METADATA.displayName)
        assertEquals("Modify Metadata", DocumentPermission.MODIFY_METADATA.displayName)
        assertEquals("Execute", DocumentPermission.EXECUTE.displayName)
        assertEquals("Download", DocumentPermission.DOWNLOAD.displayName)
        assertEquals("Upload", DocumentPermission.UPLOAD.displayName)
        assertEquals("Sync", DocumentPermission.SYNC.displayName)
        assertEquals("Administrator", DocumentPermission.ADMIN.displayName)
    }

    @Test
    fun testDescriptions() {
        DocumentPermission.entries.forEach { permission ->
            assertTrue(permission.description.isNotBlank(), "$permission should have a description")
        }
        assertEquals("Can read document content", DocumentPermission.READ.description)
        assertEquals("Has full administrative access", DocumentPermission.ADMIN.description)
    }

    @Test
    fun testDefaultFolderPermissions() {
        val defaults = DocumentPermission.DEFAULT_FOLDER_PERMISSIONS
        assertTrue(defaults.contains(DocumentPermission.READ))
        assertTrue(defaults.contains(DocumentPermission.WRITE))
        assertTrue(defaults.contains(DocumentPermission.CREATE))
        assertTrue(defaults.contains(DocumentPermission.UPLOAD))
        assertTrue(defaults.contains(DocumentPermission.DOWNLOAD))
        assertFalse(defaults.contains(DocumentPermission.ADMIN))
    }

    @Test
    fun testDefaultDocumentPermissions() {
        val defaults = DocumentPermission.DEFAULT_DOCUMENT_PERMISSIONS
        assertTrue(defaults.contains(DocumentPermission.READ))
        assertTrue(defaults.contains(DocumentPermission.WRITE))
        assertTrue(defaults.contains(DocumentPermission.DOWNLOAD))
        assertFalse(defaults.contains(DocumentPermission.CREATE))
        assertFalse(defaults.contains(DocumentPermission.ADMIN))
    }

    @Test
    fun testReadOnlyPermissions() {
        val readOnly = DocumentPermission.READ_ONLY_PERMISSIONS
        assertEquals(3, readOnly.size)
        assertTrue(readOnly.contains(DocumentPermission.READ))
        assertTrue(readOnly.contains(DocumentPermission.VIEW_METADATA))
        assertTrue(readOnly.contains(DocumentPermission.DOWNLOAD))
        readOnly.forEach { assertTrue(it.isReadPermission) }
    }

    @Test
    fun testAllPermissionsSet() {
        val all = DocumentPermission.ALL_PERMISSIONS
        assertEquals(DocumentPermission.entries.size, all.size)
        DocumentPermission.entries.forEach { assertTrue(all.contains(it)) }
    }

    @Test
    fun testHasPermission() {
        val permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE)
        assertTrue(DocumentPermission.hasPermission(permissions, DocumentPermission.READ))
        assertTrue(DocumentPermission.hasPermission(permissions, DocumentPermission.WRITE))
        assertFalse(DocumentPermission.hasPermission(permissions, DocumentPermission.DELETE))
    }

    @Test
    fun testHasPermissionWithAdmin() {
        val adminPermissions = setOf(DocumentPermission.ADMIN)
        assertTrue(DocumentPermission.hasPermission(adminPermissions, DocumentPermission.READ))
        assertTrue(DocumentPermission.hasPermission(adminPermissions, DocumentPermission.DELETE))
        assertTrue(DocumentPermission.hasPermission(adminPermissions, DocumentPermission.SHARE))
    }

    @Test
    fun testHasReadPermissions() {
        assertTrue(DocumentPermission.hasReadPermissions(setOf(DocumentPermission.READ)))
        assertTrue(DocumentPermission.hasReadPermissions(setOf(DocumentPermission.VIEW_METADATA)))
        assertTrue(DocumentPermission.hasReadPermissions(setOf(DocumentPermission.DOWNLOAD)))
        assertTrue(DocumentPermission.hasReadPermissions(setOf(DocumentPermission.ADMIN)))
        assertFalse(DocumentPermission.hasReadPermissions(setOf(DocumentPermission.WRITE)))
        assertFalse(DocumentPermission.hasReadPermissions(emptySet()))
    }

    @Test
    fun testHasWritePermissions() {
        assertTrue(DocumentPermission.hasWritePermissions(setOf(DocumentPermission.WRITE)))
        assertTrue(DocumentPermission.hasWritePermissions(setOf(DocumentPermission.CREATE)))
        assertTrue(DocumentPermission.hasWritePermissions(setOf(DocumentPermission.ADMIN)))
        assertFalse(DocumentPermission.hasWritePermissions(setOf(DocumentPermission.READ)))
        assertFalse(DocumentPermission.hasWritePermissions(emptySet()))
    }

    @Test
    fun testHasAdministrativePermissions() {
        assertTrue(DocumentPermission.hasAdministrativePermissions(setOf(DocumentPermission.DELETE)))
        assertTrue(DocumentPermission.hasAdministrativePermissions(setOf(DocumentPermission.SHARE)))
        assertTrue(DocumentPermission.hasAdministrativePermissions(setOf(DocumentPermission.ADMIN)))
        assertFalse(DocumentPermission.hasAdministrativePermissions(setOf(DocumentPermission.READ)))
        assertFalse(DocumentPermission.hasAdministrativePermissions(emptySet()))
    }

    @Test
    fun testGetEffectivePermissionsWithAdmin() {
        val effective = DocumentPermission.getEffectivePermissions(setOf(DocumentPermission.ADMIN))
        assertEquals(DocumentPermission.ALL_PERMISSIONS, effective)
    }

    @Test
    fun testGetEffectivePermissionsWithoutAdmin() {
        val permissions = setOf(DocumentPermission.READ, DocumentPermission.WRITE)
        val effective = DocumentPermission.getEffectivePermissions(permissions)
        assertEquals(permissions, effective)
    }
}

class DocumentTypeTest {

    @Test
    fun testAllDocumentTypes() {
        assertEquals(3, DocumentType.entries.size)
        DocumentType.FILE
        DocumentType.FOLDER
        DocumentType.UNKNOWN
    }
}
