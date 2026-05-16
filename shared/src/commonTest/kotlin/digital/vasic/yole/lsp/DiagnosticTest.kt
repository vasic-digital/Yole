/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-62 Phase 1: Diagnostic data class + Severity enum.
 *
 * Mutation procedure (CONST-035):
 *   1. Add a 5th Severity case (e.g., Catastrophic).
 *   2. Re-run; assert all_4_severities_present FAILS.
 *   3. Revert; confirm 3/3 PASS.
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DiagnosticTest {
    @Test
    fun all_4_severities_present() {
        val all = Severity.entries
        assertEquals(4, all.size)
        assertEquals(setOf("Error", "Warning", "Information", "Hint"), all.map { it.name }.toSet())
    }

    @Test
    fun diagnostic_with_required_fields_only() {
        val diag = Diagnostic(
            severity = Severity.Error,
            range = 10..15,
            message = "cannot find function `foo`",
        )
        assertEquals(Severity.Error, diag.severity)
        assertEquals(10..15, diag.range)
        assertEquals("cannot find function `foo`", diag.message)
        assertNull(diag.source)
        assertNull(diag.code)
    }

    @Test
    fun diagnostic_with_full_fields() {
        val diag = Diagnostic(
            severity = Severity.Warning,
            range = 0..3,
            message = "unused import",
            source = "rust-analyzer",
            code = "unused_imports",
        )
        assertEquals("rust-analyzer", diag.source)
        assertEquals("unused_imports", diag.code)
    }
}
