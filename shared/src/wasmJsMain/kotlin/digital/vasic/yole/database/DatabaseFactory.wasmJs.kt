/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Web Wasm actual for DatabaseFactory.
 * Full IndexedDB implementation is tracked under iter-69.
 * This stub lets :shared:compileKotlinWasmJs succeed.
 *
 *########################################################*/

package digital.vasic.yole.database

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Web Wasm stub — in-memory only. IndexedDB integration is tracked as
 * #iter-69-web-indexeddb-database.
 */
actual object DatabaseFactory {

    actual suspend fun createDatabase(
        name: String,
        dispatcher: CoroutineDispatcher
    ): DatabaseInterface = WasmInMemoryDatabase()

    actual fun databaseExists(name: String): Boolean = false

    actual fun deleteDatabase(name: String): Boolean = false

    actual fun getDatabasePath(name: String): String? = null

    actual fun getAvailableSpace(name: String): Long? = null

    actual fun getPlatformConfig(): DatabasePlatformConfig = DatabasePlatformConfig(
        platform = DatabasePlatform.WEB_INDEXEDDB,
        supportsTransactions = false,
        supportsForeignKeys = false,
        supportsEncryption = false,
        supportsWAL = false,
        supportsAsync = true
    )
}

/** Minimal in-memory implementation until IndexedDB bridge lands. */
private class WasmInMemoryDatabase : CommonDatabase() {

    override suspend fun doInitialize() = Unit
    override suspend fun doClose() = Unit
    override suspend fun <T> doTransaction(block: suspend () -> T): Result<T> =
        runCatching { block() }

    override suspend fun insertStorage(
        storage: digital.vasic.yole.network.common.NetworkStorage
    ): Result<Unit> = Result.failure(
        UnsupportedOperationException(
            "Wasm database stub — insertStorage not yet implemented. " +
                "Tracked: #iter-69-web-indexeddb-database"
        )
    )

    override suspend fun getStorage(
        id: String
    ): Result<digital.vasic.yole.network.common.NetworkStorage?> = Result.success(null)

    override suspend fun getDatabaseStats(): Result<DatabaseStats> = Result.success(
        DatabaseStats(
            totalSize = 0L,
            tableCounts = emptyMap(),
            cacheSize = 0L,
            operationCount = 0L,
            lastVacuumTime = null
        )
    )

    override suspend fun vacuum(): Result<Unit> = Result.success(Unit)
}
