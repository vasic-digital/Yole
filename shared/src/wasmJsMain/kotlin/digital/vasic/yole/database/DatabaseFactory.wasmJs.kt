/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Web/WASM-specific database factory implementation
 *
 *########################################################*/

package digital.vasic.yole.database

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Web/WASM-specific database factory implementation using IndexedDB/localStorage
 */
actual object DatabaseFactory {
    
    private val databaseInstances = mutableMapOf<String, WebDatabase>()
    
    actual suspend fun createDatabase(
        name: String,
        dispatcher: CoroutineDispatcher
    ): DatabaseInterface {
        return databaseInstances.getOrPut(name) {
            WebDatabase(name, dispatcher)
        }
    }
    
    actual fun databaseExists(name: String): Boolean {
        // Check if we have data in localStorage for this database
        return js("localStorage.getItem('yole_db_" + name + "_exists') !== null")
    }
    
    actual fun deleteDatabase(name: String): Boolean {
        return try {
            // Clear all localStorage keys for this database
            val keysToRemove = mutableListOf<String>()
            val storageLength = js("localStorage.length").unsafeCast<Int>()
            
            for (i in 0 until storageLength) {
                val key = js("localStorage.key(i)").unsafeCast<String?>()
                if (key != null && key.startsWith("yole_db_$name")) {
                    keysToRemove.add(key)
                }
            }
            
            keysToRemove.forEach { key ->
                js("localStorage.removeItem(key)")
            }
            
            databaseInstances.remove(name)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    actual fun getDatabasePath(name: String): String? {
        // Web databases don't have file paths
        return null
    }
    
    actual fun getAvailableSpace(name: String): Long? {
        // Estimate available space (this is very rough)
        return try {
            val storageEstimate = js("navigator.storage").unsafeCast<dynamic>()
            if (storageEstimate != null && storageEstimate.estimate != null) {
                val estimate = js("navigator.storage.estimate()")
                val quota = estimate.quota.unsafeCast<Number?>()?.toLong()
                val usage = estimate.usage.unsafeCast<Number?>()?.toLong()
                if (quota != null && usage != null) {
                    quota - usage
                } else {
                    50 * 1024 * 1024 // Default to 50MB estimate
                }
            } else {
                50 * 1024 * 1024 // Default to 50MB estimate
            }
        } catch (e: Exception) {
            50 * 1024 * 1024 // Default to 50MB estimate
        }
    }
    
    actual fun getPlatformConfig(): DatabasePlatformConfig {
        return DatabasePlatformConfig(
            platform = DatabasePlatform.WEB_LOCALSTORAGE,
            supportsTransactions = false,
            supportsForeignKeys = false,
            maxDatabaseSize = 5 * 1024 * 1024, // 5MB for localStorage
            supportsEncryption = false,
            supportsWAL = false,
            recommendedCacheSize = 1000,
            supportsAsync = true
        )
    }
}