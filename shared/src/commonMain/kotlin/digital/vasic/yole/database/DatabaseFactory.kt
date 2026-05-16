/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Cross-platform expect declaration for database factory.
 * Each platform provides an actual that creates a platform-native
 * database instance (Room on Android, SQLite on iOS/Desktop, IndexedDB on Wasm).
 *
 *########################################################*/

package digital.vasic.yole.database

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Platform-specific database factory.
 *
 * Call [createDatabase] to obtain a [DatabaseInterface] implementation
 * backed by the platform's native storage engine.  On platforms that have
 * not yet received a full implementation the factory returns a stub that
 * throws [UnsupportedOperationException] — the caller must handle that
 * gracefully (the network-storage layer already degrades to no-op when the
 * database is unavailable).
 */
expect object DatabaseFactory {

    /** Create (or open) the named database, backed by a platform dispatcher. */
    suspend fun createDatabase(
        name: String,
        dispatcher: CoroutineDispatcher = Dispatchers.Default
    ): DatabaseInterface

    /** True if a persisted database file exists for the given name. */
    fun databaseExists(name: String): Boolean

    /** Delete the persisted database file. Returns false if it did not exist. */
    fun deleteDatabase(name: String): Boolean

    /** Absolute path to the database file, or null on in-memory / web platforms. */
    fun getDatabasePath(name: String): String?

    /** Available storage space in bytes, or null if not determinable. */
    fun getAvailableSpace(name: String): Long?

    /** Platform-specific capabilities and recommended settings. */
    fun getPlatformConfig(): DatabasePlatformConfig
}
