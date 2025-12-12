/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Database factory for creating platform-specific database instances
 *
 *########################################################*/

package digital.vasic.yole.database

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Factory for creating platform-specific database implementations
 */
expect object DatabaseFactory {
    
    /**
     * Create a database instance for the current platform
     * 
     * @param name Database name
     * @param dispatcher Coroutine dispatcher for database operations
     * @return Platform-specific database implementation
     */
    suspend fun createDatabase(
        name: String = "yole_database",
        dispatcher: CoroutineDispatcher = Dispatchers.Default
    ): DatabaseInterface
    
    /**
     * Check if database file exists
     * 
     * @param name Database name
     * @return true if database file exists
     */
    fun databaseExists(name: String = "yole_database"): Boolean
    
    /**
     * Delete database file
     * 
     * @param name Database name
     * @return true if database was successfully deleted
     */
    fun deleteDatabase(name: String = "yole_database"): Boolean
    
    /**
     * Get database file path
     * 
     * @param name Database name
     * @return Absolute path to database file (null for web platforms)
     */
    fun getDatabasePath(name: String = "yole_database"): String?
    
    /**
     * Get available disk space for database
     * 
     * @param name Database name
     * @return Available space in bytes (null if unknown)
     */
    fun getAvailableSpace(name: String = "yole_database"): Long?
    
    /**
     * Get platform-specific database configuration
     */
    fun getPlatformConfig(): DatabasePlatformConfig
}

/**
 * Platform-specific database configuration
 */
data class DatabasePlatformConfig(
    val platform: DatabasePlatform,
    val supportsTransactions: Boolean,
    val supportsForeignKeys: Boolean,
    val maxDatabaseSize: Long?,
    val supportsEncryption: Boolean,
    val supportsWAL: Boolean,
    val recommendedCacheSize: Int,
    val supportsAsync: Boolean
)

/**
 * Supported database platforms
 */
enum class DatabasePlatform {
    ANDROID_ROOM,
    ANDROID_SQLITE,
    DESKTOP_SQLITE,
    DESKTOP_H2,
    IOS_SQLITE,
    WEB_INDEXEDDB,
    WEB_LOCALSTORAGE,
    WEB_MEMORY,
    COMMON_IN_MEMORY
}