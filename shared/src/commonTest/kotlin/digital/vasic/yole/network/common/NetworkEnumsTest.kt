/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for network common enum types
 *
 *########################################################*/
package digital.vasic.yole.network.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NetworkEnumsTest {

    // ==================== DocumentType ====================

    @Test
    fun testDocumentTypeEntries() {
        assertEquals(3, DocumentType.entries.size)
    }

    @Test
    fun testDocumentTypeValues() {
        DocumentType.FILE
        DocumentType.FOLDER
        DocumentType.UNKNOWN
    }

    @Test
    fun testDocumentTypeValueOf() {
        assertEquals(DocumentType.FILE, DocumentType.valueOf("FILE"))
        assertEquals(DocumentType.FOLDER, DocumentType.valueOf("FOLDER"))
        assertEquals(DocumentType.UNKNOWN, DocumentType.valueOf("UNKNOWN"))
    }

    // ==================== OperationStatus ====================

    @Test
    fun testOperationStatusEntries() {
        assertEquals(6, OperationStatus.entries.size)
    }

    @Test
    fun testOperationStatusValues() {
        OperationStatus.PENDING
        OperationStatus.IN_PROGRESS
        OperationStatus.COMPLETED
        OperationStatus.FAILED
        OperationStatus.PAUSED
        OperationStatus.CANCELLED
    }

    @Test
    fun testOperationStatusValueOf() {
        assertEquals(OperationStatus.PENDING, OperationStatus.valueOf("PENDING"))
        assertEquals(OperationStatus.IN_PROGRESS, OperationStatus.valueOf("IN_PROGRESS"))
        assertEquals(OperationStatus.COMPLETED, OperationStatus.valueOf("COMPLETED"))
        assertEquals(OperationStatus.FAILED, OperationStatus.valueOf("FAILED"))
    }

    // ==================== OperationType ====================

    @Test
    fun testOperationTypeEntries() {
        assertEquals(9, OperationType.entries.size)
    }

    @Test
    fun testOperationTypeValues() {
        OperationType.UPLOAD
        OperationType.DOWNLOAD
        OperationType.DELETE
        OperationType.CREATE_FOLDER
        OperationType.RENAME
        OperationType.MOVE
        OperationType.COPY
        OperationType.SYNC
        OperationType.SEARCH
    }

    @Test
    fun testOperationTypeValueOf() {
        assertEquals(OperationType.UPLOAD, OperationType.valueOf("UPLOAD"))
        assertEquals(OperationType.DOWNLOAD, OperationType.valueOf("DOWNLOAD"))
        assertEquals(OperationType.SYNC, OperationType.valueOf("SYNC"))
        assertEquals(OperationType.SEARCH, OperationType.valueOf("SEARCH"))
    }

    // ==================== Name consistency ====================

    @Test
    fun testDocumentTypeNames() {
        assertTrue(DocumentType.entries.all { it.name == it.name.uppercase() })
    }

    @Test
    fun testOperationStatusNames() {
        assertTrue(OperationStatus.entries.all { it.name == it.name.uppercase() })
    }

    @Test
    fun testOperationTypeNames() {
        assertTrue(OperationType.entries.all { it.name == it.name.uppercase() })
    }
}
