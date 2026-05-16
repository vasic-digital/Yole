/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop (JVM) actual for DatabaseFactory.
 * Full JDBC/SQLite implementation is tracked under iter-69.
 * This stub lets :shared:compileKotlinJvm succeed so Desktop builds
 * and runs while the database layer is being completed.
 *
 *########################################################*/

package digital.vasic.yole.database

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Desktop (JVM) stub — returns an in-memory database backed by the
 * [CommonDatabase] default implementations. Persistent JDBC/SQLite
 * integration is tracked as #iter-69-desktop-sqlite-database.
 */
actual object DatabaseFactory {

    actual suspend fun createDatabase(
        name: String,
        dispatcher: CoroutineDispatcher
    ): DatabaseInterface = DesktopInMemoryDatabase(dispatcher)

    actual fun databaseExists(name: String): Boolean = false

    actual fun deleteDatabase(name: String): Boolean = false

    actual fun getDatabasePath(name: String): String? = null

    actual fun getAvailableSpace(name: String): Long? = null

    actual fun getPlatformConfig(): DatabasePlatformConfig = DatabasePlatformConfig(
        platform = DatabasePlatform.IN_MEMORY,
        supportsTransactions = false,
        supportsForeignKeys = false,
        supportsEncryption = false,
        supportsWAL = false,
        supportsAsync = true
    )
}

/** Minimal in-memory implementation used until JDBC/SQLite integration lands. */
private class DesktopInMemoryDatabase(
    private val dispatcher: CoroutineDispatcher
) : CommonDatabase() {

    override suspend fun doInitialize() = Unit
    override suspend fun doClose() = Unit
    override suspend fun <T> doTransaction(block: suspend () -> T): Result<T> =
        runCatching { block() }

    override suspend fun insertStorage(
        storage: digital.vasic.yole.network.common.NetworkStorage
    ): Result<Unit> = Result.failure(
        UnsupportedOperationException(
            "Desktop database stub — insertStorage not yet implemented. " +
                "Tracked: #iter-69-desktop-sqlite-database"
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
