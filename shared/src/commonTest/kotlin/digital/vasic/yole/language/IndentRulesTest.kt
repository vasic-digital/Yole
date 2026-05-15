/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58 Phase 2: anti-bluff IndentRules.computeIndent tests.
 *#######################################################*/
package digital.vasic.yole.language

import digital.vasic.yole.language.affordance.IndentRules
import kotlin.test.Test
import kotlin.test.assertEquals

class IndentRulesTest {
    @Test
    fun computeIndent_returnsBaseWhenNoOpener() {
        val rules = IndentRules()
        val next = rules.computeIndent("    val x = 42", indentUnit = "    ")
        assertEquals("    ", next)
    }

    @Test
    fun computeIndent_addsOneLevelAfterOpener() {
        val rules = IndentRules()
        val next = rules.computeIndent("    fun foo() {", indentUnit = "    ")
        assertEquals("        ", next)
    }

    @Test
    fun computeIndent_handlesNestedOpener() {
        val rules = IndentRules()
        val next = rules.computeIndent("if (x) { do {", indentUnit = "  ")
        // Last token is "{" → one level deeper than current line's "" indent
        assertEquals("  ", next)
    }

    @Test
    fun computeIndent_emptyLineReturnsEmpty() {
        val rules = IndentRules()
        assertEquals("", rules.computeIndent("", indentUnit = "    "))
    }
}
