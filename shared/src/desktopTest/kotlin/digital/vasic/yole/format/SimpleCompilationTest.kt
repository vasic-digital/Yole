/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Simple compilation test to verify core functionality
 *
 *########################################################*/

package digital.vasic.yole.format

import digital.vasic.yole.format.dropbox.DropboxParser
import digital.vasic.yole.format.ftp.FtpParser
import digital.vasic.yole.format.googledrive.GoogleDriveParser
import digital.vasic.yole.format.onedrive.OneDriveParser
import digital.vasic.yole.format.sftp.SftpParser
import kotlin.test.*

/**
 * Simple test to verify that all the format parsers can be instantiated
 * and basic parsing functionality works.
 */
class SimpleCompilationTest {

    @Test
    fun `test format registry constants exist`() {
        assertNotNull(FormatRegistry.ID_DROPBOX)
        assertNotNull(FormatRegistry.ID_FTP)
        assertNotNull(FormatRegistry.ID_GOOGLEDRIVE)
        assertNotNull(FormatRegistry.ID_ONEDRIVE)
        assertNotNull(FormatRegistry.ID_SFTP)
        
        assertEquals("dropbox", FormatRegistry.ID_DROPBOX)
        assertEquals("ftp", FormatRegistry.ID_FTP)
        assertEquals("googledrive", FormatRegistry.ID_GOOGLEDRIVE)
        assertEquals("onedrive", FormatRegistry.ID_ONEDRIVE)
        assertEquals("sftp", FormatRegistry.ID_SFTP)
    }

    @Test
    fun `test parsers can be instantiated`() {
        val dropboxParser = DropboxParser()
        assertNotNull(dropboxParser)
        assertEquals("dropbox", dropboxParser.supportedFormat.id)
        
        val ftpParser = FtpParser()
        assertNotNull(ftpParser)
        assertEquals("ftp", ftpParser.supportedFormat.id)
        
        val googleDriveParser = GoogleDriveParser()
        assertNotNull(googleDriveParser)
        assertEquals("googledrive", googleDriveParser.supportedFormat.id)
        
        val oneDriveParser = OneDriveParser()
        assertNotNull(oneDriveParser)
        assertEquals("onedrive", oneDriveParser.supportedFormat.id)
        
        val sftpParser = SftpParser()
        assertNotNull(sftpParser)
        assertEquals("sftp", sftpParser.supportedFormat.id)
    }

    @Test
    fun `test basic parsing functionality`() {
        val content = "Test content for parsing"
        
        val dropboxParser = DropboxParser()
        val dropboxResult = dropboxParser.parse(content)
        assertNotNull(dropboxResult)
        assertEquals("Dropbox", dropboxResult.format.name)
        assertEquals(content, dropboxResult.rawContent)
        
        val ftpParser = FtpParser()
        val ftpResult = ftpParser.parse(content)
        assertNotNull(ftpResult)
        assertEquals("FTP", ftpResult.format.name)
        assertEquals(content, ftpResult.rawContent)
        
        val googleDriveParser = GoogleDriveParser()
        val googleDriveResult = googleDriveParser.parse(content)
        assertNotNull(googleDriveResult)
        assertEquals("Google Drive", googleDriveResult.format.name)
        assertEquals(content, googleDriveResult.rawContent)
        
        val oneDriveParser = OneDriveParser()
        val oneDriveResult = oneDriveParser.parse(content)
        assertNotNull(oneDriveResult)
        assertEquals("OneDrive", oneDriveResult.format.name)
        assertEquals(content, oneDriveResult.rawContent)
        
        val sftpParser = SftpParser()
        val sftpResult = sftpParser.parse(content)
        assertNotNull(sftpResult)
        assertEquals("SFTP", sftpResult.format.name)
        assertEquals(content, sftpResult.rawContent)
    }

    @Test
    fun `test format registry integration`() {
        // Test that the formats are registered in the FormatRegistry
        val dropboxFormat = FormatRegistry.getById(FormatRegistry.ID_DROPBOX)
        assertNotNull(dropboxFormat)
        assertEquals("Dropbox", dropboxFormat.name)
        
        val ftpFormat = FormatRegistry.getById(FormatRegistry.ID_FTP)
        assertNotNull(ftpFormat)
        assertEquals("FTP", ftpFormat.name)
        
        val googleDriveFormat = FormatRegistry.getById(FormatRegistry.ID_GOOGLEDRIVE)
        assertNotNull(googleDriveFormat)
        assertEquals("Google Drive", googleDriveFormat.name)
        
        val oneDriveFormat = FormatRegistry.getById(FormatRegistry.ID_ONEDRIVE)
        assertNotNull(oneDriveFormat)
        assertEquals("OneDrive", oneDriveFormat.name)
        
        val sftpFormat = FormatRegistry.getById(FormatRegistry.ID_SFTP)
        assertNotNull(sftpFormat)
        assertEquals("SFTP", sftpFormat.name)
    }
}