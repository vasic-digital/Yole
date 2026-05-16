/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iOS SQLite database implementation scaffold.
 *
 * STATUS: SCAFFOLD ONLY — cannot compile without a SQLite cinterop
 * `.def` file that bridges `cnames.structs.sqlite3` / `sqlite3_open`
 * etc. into Kotlin/Native. The full implementation is preserved below
 * as documentation; activate it when the cinterop layer lands.
 *
 * Tracked: #iter-69-ios-sqlite-cinterop
 *
 *########################################################*/

package digital.vasic.yole.database

// Scaffold file — no executable code until cinterop wiring lands.
// See iter-69 for the full implementation plan:
//   1. Add shared/src/iosMain/cinterop/sqlite3.def
//   2. Register cinterop in shared/build.gradle.kts for iosMain
//   3. Uncomment the class body below
//   4. Wire DatabaseFactory.ios.kt to use IosSQLiteDatabase instead of IosInMemoryDatabase

/*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import digital.vasic.yole.network.common.*
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import cnames.structs.sqlite3
import cnames.structs.sqlite3_stmt
import kotlinx.cinterop.*

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class IosSQLiteDatabase(
    private val name: String,
    private val documentsDirectory: String,
    private val dispatcher: CoroutineDispatcher
) : CommonDatabase() {
    ... (full implementation retained in git history, commit f7e681be and ancestors) ...
}
*/
