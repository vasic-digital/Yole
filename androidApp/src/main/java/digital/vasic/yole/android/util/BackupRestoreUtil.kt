/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Backup and Restore Utility for Yole Android App
 *
 *########################################################*/

package digital.vasic.yole.android.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.ZipInputStream

/**
 * Data class representing a backup of user data
 */
@Serializable
data class BackupMetadata(
    val version: Int = 1,
    val createdAt: String,
    val appVersion: String,
    val deviceInfo: String,
    val fileCount: Int,
    val totalSize: Long,
    val formats: List<String>
)

/**
 * Data class representing a file in the backup
 */
@Serializable
data class BackupFile(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val format: String,
    val content: String,
    val checksum: String
)

/**
 * Data class representing app settings backup
 */
@Serializable
data class SettingsBackup(
    val themeMode: String,
    val dynamicColorsEnabled: Boolean,
    val customSeedColor: String?,
    val reduceMotion: Boolean,
    val highContrast: Boolean,
    val announceChanges: Boolean,
    val showLineNumbers: Boolean,
    val autoSave: Boolean,
    val animationsEnabled: Boolean
)

/**
 * Utility class for backup and restore operations
 */
object BackupRestoreUtil {

    private const val BACKUP_AUTHORITY = "digital.vasic.yole.android.fileprovider"
    private const val BACKUP_DIR = "backups"
    private const val BACKUP_VERSION = 1
    private const val BUFFER_SIZE = 8192

    private val gson: Gson by lazy {
        GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * Create a backup of all user data
     */
    fun createBackup(
        context: Context,
        settings: digital.vasic.yole.android.ui.YoleSettings
    ): Result<Uri> {
        return try {
            // Create backup directory if it doesn't exist
            val backupDir = File(context.getExternalFilesDir(null), BACKUP_DIR)
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            // Generate backup filename with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFileName = "yole_backup_$timestamp.zip"
            val backupFile = File(backupDir, backupFileName)

            // Create backup
            val backupResult = createBackupZip(context, settings, backupFile)
            
            if (backupResult.isSuccess) {
                val uri = FileProvider.getUriForFile(context, BACKUP_AUTHORITY, backupFile)
                Result.success(uri)
            } else {
                Result.failure(backupResult.exceptionOrNull() ?: Exception("Unknown backup error"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restore data from a backup file
     */
    fun restoreBackup(
        context: Context,
        backupUri: Uri,
        settings: digital.vasic.yole.android.ui.YoleSettings
    ): Result<Unit> {
        return try {
            val inputStream = context.contentResolver.openInputStream(backupUri)
                ?: return Result.failure(Exception("Cannot open backup file"))

            val result = restoreFromBackupZip(context, inputStream, settings)
            inputStream.close()
            
            result

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a ZIP file containing the backup
     */
    private fun createBackupZip(
        context: Context,
        settings: digital.vasic.yole.android.ui.YoleSettings,
        backupFile: File
    ): Result<Unit> {
        return try {
            ZipOutputStream(FileOutputStream(backupFile)).use { zipOut ->
                // Backup documents directory
                val documentsDir = File(context.getExternalFilesDir(null)?.parentFile, "Documents")
                if (documentsDir.exists()) {
                    backupDirectory(context, documentsDir, "documents", zipOut)
                }

                // Backup app files directory
                backupDirectory(context, context.filesDir, "app_files", zipOut)

                // Backup shared preferences
                backupSharedPreferences(context, zipOut)

                // Backup settings
                backupSettings(settings, zipOut)

                // Create backup metadata
                createBackupMetadata(context, zipOut)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Backup a directory and its contents
     */
    private fun backupDirectory(
        context: Context,
        directory: File,
        backupPath: String,
        zipOut: ZipOutputStream
    ) {
        val files = directory.listFiles() ?: return

        for (file in files) {
            when {
                file.isFile -> {
                    backupFile(context, file, "$backupPath/${file.name}", zipOut)
                }
                file.isDirectory -> {
                    backupDirectory(context, file, "$backupPath/${file.name}", zipOut)
                }
            }
        }
    }

    /**
     * Backup a single file
     */
    private fun backupFile(
        context: Context,
        file: File,
        backupPath: String,
        zipOut: ZipOutputStream
    ) {
        try {
            val content = file.readText()
            val format = detectFormat(file.name)
            val checksum = calculateChecksum(content)

            val backupFile = BackupFile(
                path = backupPath,
                name = file.name,
                size = file.length(),
                lastModified = file.lastModified(),
                format = format.id,
                content = content,
                checksum = checksum
            )

            val jsonContent = gson.toJson(backupFile)
            val entry = ZipEntry("$backupPath.json")
            zipOut.putNextEntry(entry)
            zipOut.write(jsonContent.toByteArray())
            zipOut.closeEntry()
        } catch (e: Exception) {
            // Log error but continue with other files
            e.printStackTrace()
        }
    }

    /**
     * Backup shared preferences
     */
    private fun backupSharedPreferences(context: Context, zipOut: ZipOutputStream) {
        try {
            val sharedPrefsDir = File(context.filesDir.parentFile, "shared_prefs")
            if (sharedPrefsDir.exists()) {
                val prefFiles = sharedPrefsDir.listFiles { file -> file.extension == "xml" }
                prefFiles?.forEach { prefFile ->
                    val content = prefFile.readText()
                    val entry = ZipEntry("shared_prefs/${prefFile.name}")
                    zipOut.putNextEntry(entry)
                    zipOut.write(content.toByteArray())
                    zipOut.closeEntry()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Backup app settings
     */
    private fun backupSettings(
        settings: digital.vasic.yole.android.ui.YoleSettings,
        zipOut: ZipOutputStream
    ) {
        try {
            val settingsBackup = SettingsBackup(
                themeMode = settings.getThemeMode(),
                dynamicColorsEnabled = settings.getDynamicColorsEnabled(),
                customSeedColor = settings.getCustomSeedColor()?.takeIf { it.isNotEmpty() },
                reduceMotion = settings.getReduceMotion(),
                highContrast = settings.getHighContrast(),
                announceChanges = settings.getAnnounceChanges(),
                showLineNumbers = settings.getShowLineNumbers(),
                autoSave = settings.getAutoSave(),
                animationsEnabled = settings.getAnimationsEnabled()
            )

            val jsonContent = gson.toJson(settingsBackup)
            val entry = ZipEntry("settings.json")
            zipOut.putNextEntry(entry)
            zipOut.write(jsonContent.toByteArray())
            zipOut.closeEntry()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Create backup metadata
     */
    private fun createBackupMetadata(context: Context, zipOut: ZipOutputStream) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val deviceInfo = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
            
            val metadata = BackupMetadata(
                version = BACKUP_VERSION,
                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date()),
                appVersion = packageInfo.versionName ?: "unknown",
                deviceInfo = deviceInfo,
                fileCount = 0, // Will be calculated during restore
                totalSize = 0L, // Will be calculated during restore
                formats = FormatRegistry.formats.map { it.id }
            )

            val jsonContent = gson.toJson(metadata)
            val entry = ZipEntry("metadata.json")
            zipOut.putNextEntry(entry)
            zipOut.write(jsonContent.toByteArray())
            zipOut.closeEntry()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Restore from backup ZIP file
     */
    private fun restoreFromBackupZip(
        context: Context,
        inputStream: java.io.InputStream,
        settings: digital.vasic.yole.android.ui.YoleSettings
    ): Result<Unit> {
        return try {
            ZipInputStream(inputStream).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                
                while (entry != null) {
                    when {
                        entry.name == "settings.json" -> {
                            restoreSettings(zipIn, settings)
                        }
                        entry.name.startsWith("documents/") && entry.name.endsWith(".json") -> {
                            restoreFile(context, zipIn, entry.name, "documents")
                        }
                        entry.name.startsWith("app_files/") && entry.name.endsWith(".json") -> {
                            restoreFile(context, zipIn, entry.name, "app_files")
                        }
                        entry.name.startsWith("shared_prefs/") -> {
                            restoreSharedPreference(context, zipIn, entry.name)
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restore settings from backup
     */
    private fun restoreSettings(
        zipIn: ZipInputStream,
        settings: digital.vasic.yole.android.ui.YoleSettings
    ) {
        try {
            val jsonContent = zipIn.bufferedReader().use { it.readText() }
            val settingsBackup = gson.fromJson(jsonContent, SettingsBackup::class.java)

            settings.setThemeMode(settingsBackup.themeMode)
            settings.setDynamicColorsEnabled(settingsBackup.dynamicColorsEnabled)
            settingsBackup.customSeedColor?.let { settings.setCustomSeedColor(it) }
            settings.setReduceMotion(settingsBackup.reduceMotion)
            settings.setHighContrast(settingsBackup.highContrast)
            settings.setAnnounceChanges(settingsBackup.announceChanges)
            settings.setShowLineNumbers(settingsBackup.showLineNumbers)
            settings.setAutoSave(settingsBackup.autoSave)
            settings.setAnimationsEnabled(settingsBackup.animationsEnabled)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Restore a file from backup
     */
    private fun restoreFile(
        context: Context,
        zipIn: ZipInputStream,
        entryName: String,
        targetDir: String
    ) {
        try {
            val jsonContent = zipIn.bufferedReader().use { it.readText() }
            val backupFile = gson.fromJson(jsonContent, BackupFile::class.java)

            // Determine target directory
            val targetDirectory = when (targetDir) {
                "documents" -> File(context.getExternalFilesDir(null)?.parentFile, "Documents")
                "app_files" -> context.filesDir
                else -> context.filesDir
            }

            // Create target directory if it doesn't exist
            if (!targetDirectory.exists()) {
                targetDirectory.mkdirs()
            }

            // Restore file
            val restoredFile = File(targetDirectory, backupFile.name)
            restoredFile.writeText(backupFile.content)
            restoredFile.setLastModified(backupFile.lastModified)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Restore shared preference from backup
     */
    private fun restoreSharedPreference(
        context: Context,
        zipIn: ZipInputStream,
        entryName: String
    ) {
        try {
            val content = zipIn.bufferedReader().use { it.readText() }
            val sharedPrefsDir = File(context.filesDir.parentFile, "shared_prefs")
            
            if (!sharedPrefsDir.exists()) {
                sharedPrefsDir.mkdirs()
            }

            val prefFile = File(sharedPrefsDir, entryName.substringAfterLast("/"))
            prefFile.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Get all backup files
     */
    fun getBackupFiles(context: Context): List<File> {
        val backupDir = File(context.getExternalFilesDir(null), BACKUP_DIR)
        return if (backupDir.exists()) {
            backupDir.listFiles { file -> file.extension.equals("zip", ignoreCase = true) }
                ?.sortedByDescending { it.lastModified() }
                ?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    /**
     * Delete old backup files (older than 30 days)
     */
    fun cleanupOldBackups(context: Context) {
        val backupDir = File(context.getExternalFilesDir(null), BACKUP_DIR)
        if (backupDir.exists()) {
            val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000)
            backupDir.listFiles { file ->
                file.extension.equals("zip", ignoreCase = true) && file.lastModified() < thirtyDaysAgo
            }?.forEach { it.delete() }
        }
    }

    /**
     * Detect format of a file
     */
    private fun detectFormat(fileName: String): TextFormat {
        return try {
            FormatRegistry.detectByFilename(fileName)
        } catch (e: Exception) {
            FormatRegistry.formats.firstOrNull() ?: throw IllegalStateException("No formats available")
        }
    }

    /**
     * Calculate simple checksum for content validation
     */
    private fun calculateChecksum(content: String): String {
        return content.hashCode().toString(16)
    }

    /**
     * Validate backup file integrity
     */
    fun validateBackupFile(context: Context, backupUri: Uri): Result<Boolean> {
        return try {
            val inputStream = context.contentResolver.openInputStream(backupUri)
                ?: return Result.failure(Exception("Cannot open backup file"))

            ZipInputStream(inputStream).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                var hasMetadata = false
                
                while (entry != null) {
                    if (entry.name == "metadata.json") {
                        hasMetadata = true
                        break
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
                
                Result.success(hasMetadata)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}