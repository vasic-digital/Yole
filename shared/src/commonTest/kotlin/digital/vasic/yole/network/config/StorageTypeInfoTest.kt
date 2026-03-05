/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for StorageTypeInfo, StorageFeature, and
 * NetworkStorageConfigService.getSupportedStorageTypes()
 *
 *########################################################*/
package digital.vasic.yole.network.config

import digital.vasic.yole.network.common.StorageType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StorageTypeInfoTest {

    private val configService = NetworkStorageConfigService()
    private val supportedTypes = configService.getSupportedStorageTypes()

    @Test
    fun testAllStorageFeatureValues() {
        assertEquals(14, StorageFeature.entries.size)
        StorageFeature.FOLDERS
        StorageFeature.FILE_OPERATIONS
        StorageFeature.METADATA
        StorageFeature.VERSIONING
        StorageFeature.ENCRYPTION
        StorageFeature.PERMISSIONS
        StorageFeature.NETWORK_DISCOVERY
        StorageFeature.SEARCH
        StorageFeature.COLLABORATION
        StorageFeature.OFFLINE_SYNC
        StorageFeature.SHARING
        StorageFeature.OFFICE_INTEGRATION
        StorageFeature.SYNC
        StorageFeature.BACKUP
    }

    @Test
    fun testSupportedTypesCount() {
        assertEquals(8, supportedTypes.size)
    }

    @Test
    fun testEachTypeHasNameAndDescription() {
        supportedTypes.forEach { info ->
            assertTrue(info.name.isNotBlank(), "${info.type} should have a name")
            assertTrue(info.description.isNotBlank(), "${info.type} should have a description")
        }
    }

    @Test
    fun testEachTypeHasFeatures() {
        supportedTypes.forEach { info ->
            assertTrue(info.supportedFeatures.isNotEmpty(), "${info.type} should have features")
        }
    }

    @Test
    fun testEachTypeHasPopularServices() {
        supportedTypes.forEach { info ->
            assertTrue(info.popularServices.isNotEmpty(), "${info.type} should have popular services")
        }
    }

    @Test
    fun testWebDavTypeInfo() {
        val webdav = supportedTypes.find { it.type == StorageType.WEBDAV }
        assertNotNull(webdav)
        assertEquals("WebDAV", webdav.name)
        assertTrue(webdav.supportedFeatures.contains(StorageFeature.FOLDERS))
        assertTrue(webdav.supportedFeatures.contains(StorageFeature.METADATA))
        assertTrue(webdav.supportedFeatures.contains(StorageFeature.VERSIONING))
        assertTrue(webdav.supportedFeatures.contains(StorageFeature.ENCRYPTION))
        assertTrue(webdav.popularServices.contains("Nextcloud"))
        assertTrue(webdav.popularServices.contains("ownCloud"))
    }

    @Test
    fun testFtpTypeInfo() {
        val ftp = supportedTypes.find { it.type == StorageType.FTP }
        assertNotNull(ftp)
        assertEquals("FTP", ftp.name)
        assertTrue(ftp.supportedFeatures.contains(StorageFeature.FILE_OPERATIONS))
        assertEquals(1, ftp.supportedFeatures.size, "FTP should only support FILE_OPERATIONS")
    }

    @Test
    fun testSftpTypeInfo() {
        val sftp = supportedTypes.find { it.type == StorageType.SFTP }
        assertNotNull(sftp)
        assertEquals("SFTP", sftp.name)
        assertTrue(sftp.supportedFeatures.contains(StorageFeature.FILE_OPERATIONS))
        assertTrue(sftp.supportedFeatures.contains(StorageFeature.ENCRYPTION))
        assertTrue(sftp.supportedFeatures.contains(StorageFeature.PERMISSIONS))
    }

    @Test
    fun testSmbTypeInfo() {
        val smb = supportedTypes.find { it.type == StorageType.SMB }
        assertNotNull(smb)
        assertEquals("SMB/CIFS", smb.name)
        assertTrue(smb.supportedFeatures.contains(StorageFeature.FOLDERS))
        assertTrue(smb.supportedFeatures.contains(StorageFeature.PERMISSIONS))
        assertTrue(smb.supportedFeatures.contains(StorageFeature.NETWORK_DISCOVERY))
    }

    @Test
    fun testGoogleDriveTypeInfo() {
        val gdrive = supportedTypes.find { it.type == StorageType.GOOGLE_DRIVE }
        assertNotNull(gdrive)
        assertEquals("Google Drive", gdrive.name)
        assertTrue(gdrive.supportedFeatures.contains(StorageFeature.FOLDERS))
        assertTrue(gdrive.supportedFeatures.contains(StorageFeature.SEARCH))
        assertTrue(gdrive.supportedFeatures.contains(StorageFeature.COLLABORATION))
        assertTrue(gdrive.supportedFeatures.contains(StorageFeature.OFFLINE_SYNC))
    }

    @Test
    fun testDropboxTypeInfo() {
        val dropbox = supportedTypes.find { it.type == StorageType.DROPBOX }
        assertNotNull(dropbox)
        assertEquals("Dropbox", dropbox.name)
        assertTrue(dropbox.supportedFeatures.contains(StorageFeature.FOLDERS))
        assertTrue(dropbox.supportedFeatures.contains(StorageFeature.VERSIONING))
        assertTrue(dropbox.supportedFeatures.contains(StorageFeature.SHARING))
        assertTrue(dropbox.supportedFeatures.contains(StorageFeature.OFFLINE_SYNC))
    }

    @Test
    fun testOneDriveTypeInfo() {
        val onedrive = supportedTypes.find { it.type == StorageType.ONEDRIVE }
        assertNotNull(onedrive)
        assertEquals("OneDrive", onedrive.name)
        assertTrue(onedrive.supportedFeatures.contains(StorageFeature.FOLDERS))
        assertTrue(onedrive.supportedFeatures.contains(StorageFeature.SEARCH))
        assertTrue(onedrive.supportedFeatures.contains(StorageFeature.COLLABORATION))
        assertTrue(onedrive.supportedFeatures.contains(StorageFeature.OFFICE_INTEGRATION))
    }

    @Test
    fun testGitTypeInfo() {
        val git = supportedTypes.find { it.type == StorageType.GIT }
        assertNotNull(git)
        assertEquals("Git", git.name)
        assertTrue(git.supportedFeatures.contains(StorageFeature.VERSIONING))
        assertTrue(git.supportedFeatures.contains(StorageFeature.SEARCH))
        assertTrue(git.supportedFeatures.contains(StorageFeature.SYNC))
        assertTrue(git.supportedFeatures.contains(StorageFeature.BACKUP))
        assertTrue(git.supportedFeatures.contains(StorageFeature.ENCRYPTION))
        assertTrue(git.popularServices.contains("GitHub"))
        assertTrue(git.popularServices.contains("GitLab"))
    }

    @Test
    fun testStorageTypeInfoEquality() {
        val info1 = StorageTypeInfo(
            type = StorageType.FTP,
            name = "FTP",
            description = "desc",
            supportedFeatures = setOf(StorageFeature.FILE_OPERATIONS),
            popularServices = listOf("vsftpd")
        )
        val info2 = StorageTypeInfo(
            type = StorageType.FTP,
            name = "FTP",
            description = "desc",
            supportedFeatures = setOf(StorageFeature.FILE_OPERATIONS),
            popularServices = listOf("vsftpd")
        )
        assertEquals(info1, info2)
        assertEquals(info1.hashCode(), info2.hashCode())
    }

    @Test
    fun testStorageTypeInfoCopy() {
        val info = StorageTypeInfo(
            type = StorageType.FTP,
            name = "FTP",
            description = "desc",
            supportedFeatures = setOf(StorageFeature.FILE_OPERATIONS),
            popularServices = listOf("vsftpd")
        )
        val copied = info.copy(name = "Updated FTP")
        assertEquals("Updated FTP", copied.name)
        assertEquals(StorageType.FTP, copied.type)
    }

    @Test
    fun testAllStorageTypesRepresented() {
        val representedTypes = supportedTypes.map { it.type }.toSet()
        assertTrue(representedTypes.contains(StorageType.WEBDAV))
        assertTrue(representedTypes.contains(StorageType.FTP))
        assertTrue(representedTypes.contains(StorageType.SFTP))
        assertTrue(representedTypes.contains(StorageType.SMB))
        assertTrue(representedTypes.contains(StorageType.GOOGLE_DRIVE))
        assertTrue(representedTypes.contains(StorageType.DROPBOX))
        assertTrue(representedTypes.contains(StorageType.ONEDRIVE))
        assertTrue(representedTypes.contains(StorageType.GIT))
    }
}
