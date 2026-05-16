/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-62 Phase 1: DiagnosticsCache tests.
 *
 * Mutation procedure (CONST-035):
 *   1. Stub upsert() to append-instead-of-replace.
 *   2. Re-run; assert upsert_replaces_previous + upsert_empty_clears FAIL.
 *   3. Revert; confirm 5/5 PASS.
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class DiagnosticsCacheTest {
    @Test
    fun empty_cache_returns_empty_list() = runBlocking<Unit> {
        val cache = DiagnosticsCache()
        assertEquals(emptyList(), cache.diagnosticsFor("file:///x.rs"))
    }

    @Test
    fun upsert_adds_diagnostics() = runBlocking<Unit> {
        val cache = DiagnosticsCache()
        val diag = Diagnostic(Severity.Error, 0..5, "err")
        cache.upsert("file:///x.rs", listOf(diag))
        assertEquals(listOf(diag), cache.diagnosticsFor("file:///x.rs"))
    }

    @Test
    fun upsert_replaces_previous() = runBlocking<Unit> {
        val cache = DiagnosticsCache()
        cache.upsert("file:///x.rs", listOf(Diagnostic(Severity.Error, 0..5, "old")))
        val newDiag = Diagnostic(Severity.Warning, 10..15, "new")
        cache.upsert("file:///x.rs", listOf(newDiag))
        assertEquals(listOf(newDiag), cache.diagnosticsFor("file:///x.rs"))
    }

    @Test
    fun upsert_empty_clears_uri() = runBlocking<Unit> {
        val cache = DiagnosticsCache()
        cache.upsert("file:///x.rs", listOf(Diagnostic(Severity.Error, 0..5, "err")))
        cache.upsert("file:///x.rs", emptyList())
        assertEquals(emptyList(), cache.diagnosticsFor("file:///x.rs"))
    }

    @Test
    fun cap_1000_truncates() = runBlocking<Unit> {
        val cache = DiagnosticsCache()
        val many = (0 until 1500).map {
            Diagnostic(Severity.Error, it..it, "diag $it")
        }
        cache.upsert("file:///x.rs", many)
        assertEquals(1000, cache.diagnosticsFor("file:///x.rs").size)
    }
}
