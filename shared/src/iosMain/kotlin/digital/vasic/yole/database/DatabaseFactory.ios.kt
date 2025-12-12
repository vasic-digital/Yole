/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iOS-specific database factory implementation
 *
 *########################################################*/

package digital.vasic.yole.database

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSFileManager
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask

/**
 * iOS-specific database factory implementation using native SQLite
 */
actual object DatabaseFactory {
    
    private val databaseInstances = mutableMapOf<String, IosSQLiteDatabase>()
    private var documentsDirectory: String? = null
    
    /**
     * Initialize the database factory
     * Must be called before creating any database instances
     */
    fun initialize() {
        val fileManager = NSFileManager.defaultManager
        val urls = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
        documentsDirectory = urls.firstOrNull()?.path
    }
    
    actual suspend fun createDatabase(
        name: String,
        dispatcher: CoroutineDispatcher
    ): DatabaseInterface {
        val documentsDir = documentsDirectory ?: throw IllegalStateException(
            "DatabaseFactory not initialized or documents directory not available"
        )
        
        return databaseInstances.getOrPut(name) {
            IosSQLiteDatabase(name, documentsDir, dispatcher)
        }
    }
    
    actual fun databaseExists(name: String): Boolean {
        val documentsDir = documentsDirectory ?: return false
        val dbPath = "$documentsDir/$name.db"
        val fileManager = NSFileManager.defaultManager
        return fileManager.fileExistsAtPath(dbPath)
    }
    
    actual fun deleteDatabase(name: String): Boolean {
        val documentsDir = documentsDirectory ?: return false
        val dbPath = "$documentsDir/$name.db"
        val fileManager = NSFileManager.defaultManager
        return if (fileManager.fileExistsAtPath(dbPath)) {
            fileManager.removeItemAtPath(dbPath, null)
        } else {
            false
        }
    }
    
    actual fun getDatabasePath(name: String): String? {
        val documentsDir = documentsDirectory ?: return null
        return "$documentsDir/$name.db"
    }
    
    actual fun getAvailableSpace(name: String): Long? {
        val documentsDir = documentsDirectory ?: return null
        val fileManager = NSFileManager.defaultManager
        
        return try {
            val attributes = fileManager.attributesOfFileSystemForPath(documentsDir, null)
            val freeSpace = attributes?.get("NSFileSystemFreeSize") as? Long
            freeSpace
        } catch (e: Exception) {
            null
        }
    }
    
    actual fun getPlatformConfig(): DatabasePlatformConfig {
        return DatabasePlatformConfig(
            platform = DatabasePlatform.IOS_SQLITE,
            supportsTransactions = true,
            supportsForeignKeys = true,
            maxDatabaseSize = 281474976710655L, // SQLite max size (2^48 - 1)
            supportsEncryption = true,
            supportsWAL = true,
            recommendedCacheSize = 10000, // pages
            supportsAsync = true
        )
    }
}