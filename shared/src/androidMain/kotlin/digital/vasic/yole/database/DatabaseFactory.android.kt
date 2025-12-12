/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Android-specific database factory implementation
 *
 *########################################################*/

package digital.vasic.yole.database

import android.content.Context
import android.os.StatFs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.File

/**
 * Android-specific database factory implementation using Room
 */
actual object DatabaseFactory {
    
    private var appContext: Context? = null
    private val databaseInstances = mutableMapOf<String, AndroidRoomDatabase>()
    
    /**
     * Initialize the database factory with Android context
     * Must be called before creating any database instances
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }
    
    actual suspend fun createDatabase(
        name: String,
        dispatcher: CoroutineDispatcher
    ): DatabaseInterface {
        val context = appContext ?: throw IllegalStateException(
            "DatabaseFactory not initialized. Call initialize() first."
        )
        
        return databaseInstances.getOrPut(name) {
            AndroidRoomDatabase(name, context, dispatcher)
        }
    }
    
    actual fun databaseExists(name: String): Boolean {
        val context = appContext ?: return false
        return context.getDatabasePath(name).exists()
    }
    
    actual fun deleteDatabase(name: String): Boolean {
        val context = appContext ?: return false
        return context.deleteDatabase(name)
    }
    
    actual fun getDatabasePath(name: String): String? {
        val context = appContext ?: return null
        return context.getDatabasePath(name).absolutePath
    }
    
    actual fun getAvailableSpace(name: String): Long? {
        val context = appContext ?: return null
        val dbPath = context.getDatabasePath(name)
        val parentDir = dbPath.parentFile ?: return null
        
        return try {
            val statFs = StatFs(parentDir.absolutePath)
            statFs.availableBytes
        } catch (e: Exception) {
            null
        }
    }
    
    actual fun getPlatformConfig(): DatabasePlatformConfig {
        return DatabasePlatformConfig(
            platform = DatabasePlatform.ANDROID_ROOM,
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