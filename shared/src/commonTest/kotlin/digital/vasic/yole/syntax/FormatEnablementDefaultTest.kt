/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 4: anti-bluff test for the operator constraint
 * "Markdown is the only default-enabled format" (spec §3.7).
 * Mutation-verified: changing the default to setOf("markdown", "x")
 * causes this test to FAIL.
 *
 *########################################################*/
package digital.vasic.yole.syntax

import digital.vasic.yole.format.FormatRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

class FormatEnablementDefaultTest {
    @Test
    fun freshDefaultIsMarkdownOnly() {
        val defaults = FormatRegistry.defaultEnabledFormatIds()
        assertEquals(
            setOf("markdown"),
            defaults,
            "Yole's default-enabled format set MUST be exactly {markdown} (operator constraint, spec §3.7)",
        )
    }
}
