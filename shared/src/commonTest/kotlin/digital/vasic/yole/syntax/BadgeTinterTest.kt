/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 11 anti-bluff tests (CONST-035) for BadgeTinter.
 *
 * Mutation evidence (recorded for the anchor manifest):
 *   - Stubbing BadgeTinter.tintFor to return a constant 0xFF000000
 *     causes markdownFileGetsLangSpecificTint and fallsBackToGenericBadgeBg
 *     to FAIL (wrong color), and unknownExtensionReturnsNull /
 *     disabledFormatReturnsNull to FAIL (non-null != null). 4/4 fail.
 *   - Stubbing GrammarRegistry.detectByFilename to always return null
 *     causes markdownFileGetsLangSpecificTint to FAIL (expected
 *     0xFF00AA00, got null). 1/4 fails — kept because it exercises
 *     the disabled-format branch separately.
 *
 *########################################################*/
package digital.vasic.yole.syntax

import digital.vasic.yole.syntax.render.BadgeTinter
import digital.vasic.yole.syntax.theme.Theme
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BadgeTinterTest {
    private val theme = Theme(
        name = "T",
        type = "dark",
        uiColors = mapOf(
            "badge.background" to 0xFF888888.toInt(),
            "badge.background.markdown" to 0xFF00AA00.toInt(),
        ),
        tokenColors = emptyMap(),
    )

    // Snapshot the gate state to restore between tests so tests stay
    // independent of execution order and the global default.
    private lateinit var savedEnabled: Set<String>

    @BeforeTest
    fun saveGate() {
        savedEnabled = EnabledFormatGate.enabled.value
    }

    @AfterTest
    fun restoreGate() {
        EnabledFormatGate.setEnabled(savedEnabled)
    }

    @Test
    fun markdownFileGetsLangSpecificTint() {
        EnabledFormatGate.setEnabled(setOf("markdown"))
        assertEquals(0xFF00AA00.toInt(), BadgeTinter.tintFor("readme.md", theme))
        assertEquals("markdown", BadgeTinter.langIdFor("readme.md"))
    }

    @Test
    fun fallsBackToGenericBadgeBg() {
        EnabledFormatGate.setEnabled(setOf("markdown"))
        // Drop the per-language override; the generic key must be used.
        val themeNoPerLang = theme.copy(
            uiColors = mapOf("badge.background" to 0xFF888888.toInt()),
        )
        assertEquals(0xFF888888.toInt(), BadgeTinter.tintFor("readme.md", themeNoPerLang))
    }

    @Test
    fun unknownExtensionReturnsNull() {
        EnabledFormatGate.setEnabled(setOf("markdown"))
        assertNull(BadgeTinter.tintFor("readme.xyz", theme))
        assertNull(BadgeTinter.langIdFor("readme.xyz"))
    }

    @Test
    fun disabledFormatReturnsNull() {
        EnabledFormatGate.setEnabled(emptySet())
        assertNull(BadgeTinter.tintFor("readme.md", theme))
        assertNull(BadgeTinter.langIdFor("readme.md"))
    }
}
