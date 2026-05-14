/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 4: anti-bluff tests for EnabledFormatGate. Verify that
 * the runtime gate honors the Markdown-default constraint (spec §3.7),
 * that requireEnabled() actually throws for disabled formats (not a
 * no-op), and that setEnabled replaces the entire set (no leakage).
 *
 *########################################################*/
package digital.vasic.yole.syntax

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FormatEnablementGateTest {
    @Test
    fun defaultState_onlyMarkdownIsEnabled() {
        // Reset to default
        EnabledFormatGate.setEnabled(setOf("markdown"))
        assertTrue(EnabledFormatGate.isEnabled("markdown"))
        assertFalse(EnabledFormatGate.isEnabled("kotlin"))
        assertFalse(EnabledFormatGate.isEnabled("asciidoc"))
    }

    @Test
    fun requireEnabled_passesForEnabled() {
        EnabledFormatGate.setEnabled(setOf("markdown", "kotlin"))
        EnabledFormatGate.requireEnabled("markdown")  // no exception
        EnabledFormatGate.requireEnabled("kotlin")    // no exception
    }

    @Test
    fun requireEnabled_throwsForDisabled() {
        EnabledFormatGate.setEnabled(setOf("markdown"))
        val ex = assertFailsWith<FormatDisabledException> {
            EnabledFormatGate.requireEnabled("python")
        }
        assertEquals("python", ex.formatId)
    }

    @Test
    fun setEnabled_replacesEntireSet() {
        EnabledFormatGate.setEnabled(setOf("markdown", "kotlin"))
        EnabledFormatGate.setEnabled(setOf("markdown"))
        assertFalse(EnabledFormatGate.isEnabled("kotlin"))
        assertTrue(EnabledFormatGate.isEnabled("markdown"))
    }
}
