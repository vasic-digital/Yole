/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop-specific database factory implementation
 *
 *########################################################*/

package digital.vasic.yole.database

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.File

/**
 * Desktop-specific database factory implementation using SQLite JDBC
 */
actual object DatabaseFactory {
    
    private val databaseInstances = mutableMapOf<String, DesktopSQLiteDatabase>()
    private var baseDirectory: File? = null
    
    /**
     * Initialize the database factory with a base directory for database files
     * Must be called before creating any database instances
     */
    fun initialize(baseDir: File) {
        baseDirectory = baseDir.apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    actual suspend fun createDatabase(
        name: String,
        dispatcher: CoroutineDispatcher
    ): DatabaseInterface {
        val baseDir = baseDirectory ?: throw IllegalStateException(
            "DatabaseFactory not initialized. Call initialize() first with a valid directory."
        )
        
        return databaseInstances.getOrPut(name) {
            DesktopSQLiteDatabase(name, baseDir, dispatcher)
        }
    }
    
    actual fun databaseExists(name: String): Boolean {
        val baseDir = baseDirectory ?: return false
        return File(baseDir, "$name.db").exists()
    }
    
    actual fun deleteDatabase(name: String): Boolean {
        val baseDir = baseDirectory ?: return false
        val dbFile = File(baseDir, "$name.db")
        return if (dbFile.exists()) {
            dbFile.delete()
        } else {
            false
        }
    }
    
    actual fun getDatabasePath(name: String): String? {
        val baseDir = baseDirectory ?: return null
        return File(baseDir, "$name.db").absolutePath
    }
    
    actual fun getAvailableSpace(name: String): Long? {
        val baseDir = baseDirectory ?: return null
        return try {
            baseDir.freeSpace
        } catch (e: Exception) {
            null
        }
    }
    
    actual fun getPlatformConfig(): DatabasePlatformConfig {
        return DatabasePlatformConfig(
            platform = DatabasePlatform.DESKTOP_SQLITE,
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