import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.dropbox.DropboxParser
import digital.vasic.yole.format.ftp.FtpParser
import digital.vasic.yole.format.googledrive.GoogleDriveParser
import digital.vasic.yole.format.onedrive.OneDriveParser
import digital.vasic.yole.format.sftp.SftpParser

fun main() {
    println("Testing format registry constants...")
    
    // Test that the format ID constants exist
    println("ID_DROPBOX: ${FormatRegistry.ID_DROPBOX}")
    println("ID_FTP: ${FormatRegistry.ID_FTP}")
    println("ID_GOOGLEDRIVE: ${FormatRegistry.ID_GOOGLEDRIVE}")
    println("ID_ONEDRIVE: ${FormatRegistry.ID_ONEDRIVE}")
    println("ID_SFTP: ${FormatRegistry.ID_SFTP}")
    
    println("\nTesting parser instantiation...")
    
    // Test that parsers can be instantiated
    val dropboxParser = DropboxParser()
    println("DropboxParser created: $dropboxParser")
    
    val ftpParser = FtpParser()
    println("FtpParser created: $ftpParser")
    
    val googleDriveParser = GoogleDriveParser()
    println("GoogleDriveParser created: $googleDriveParser")
    
    val oneDriveParser = OneDriveParser()
    println("OneDriveParser created: $oneDriveParser")
    
    val sftpParser = SftpParser()
    println("SftpParser created: $sftpParser")
    
    println("\nTesting basic parsing...")
    
    // Test basic parsing functionality
    val content = "Test content"
    val dropboxResult = dropboxParser.parse(content)
    println("Dropbox parse result: ${dropboxResult.format.name}")
    
    val ftpResult = ftpParser.parse(content)
    println("FTP parse result: ${ftpResult.format.name}")
    
    val googleDriveResult = googleDriveParser.parse(content)
    println("Google Drive parse result: ${googleDriveResult.format.name}")
    
    val oneDriveResult = oneDriveParser.parse(content)
    println("OneDrive parse result: ${oneDriveResult.format.name}")
    
    val sftpResult = sftpParser.parse(content)
    println("SFTP parse result: ${sftpResult.format.name}")
    
    println("\nAll tests passed! ✅")
}