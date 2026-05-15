/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test (iter-58 Feature 2 Phase 4, CONST-035 anti-bluff):
 *   Verifies applyBracketAutocomplete inserts a matching closer when a
 *   single opener is typed, parks the cursor between the pair, and is
 *   a strict no-op otherwise.
 *
 *   Anti-bluff covenant: stubbing applyBracketAutocomplete to
 *   `return new` would cause `insertsCloserForOpener` to FAIL because
 *   the result text would be "(" not "()".
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import digital.vasic.yole.android.ui.editor.applyBracketAutocomplete
import digital.vasic.yole.language.LanguageRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BracketAutoCompleterRobolectricTest {

    /**
     * Typing `(` with kotlin active produces `()` and cursor parks
     * between the two characters at offset 1. Mutation guard: stub
     * applyBracketAutocomplete → fails here (text stays "(").
     */
    @Test
    fun insertsCloserForOpener() {
        val kotlinLang = requireNotNull(LanguageRegistry.get("kotlin"))
        val old = TextFieldValue("", TextRange(0))
        val new = TextFieldValue("(", TextRange(1))
        val result = applyBracketAutocomplete(old, new, kotlinLang)
        assertEquals("()", result.text)
        assertEquals(1, result.selection.start)
        assertEquals(1, result.selection.end)
    }

    /**
     * Null language disables the feature → returns `new` unchanged.
     * Mutation guard: a "feature always-on" implementation fails here.
     */
    @Test
    fun noOpWhenLanguageIsNull() {
        val old = TextFieldValue("", TextRange(0))
        val new = TextFieldValue("(", TextRange(1))
        val result = applyBracketAutocomplete(old, new, null)
        assertEquals(new.text, result.text)
        assertEquals(new.selection, result.selection)
    }

    /**
     * Paste of multi-char content (delta > 1) must NOT trigger auto-
     * completion. Mutation guard: removing the delta == 1 check fails
     * here because the result would gain an extra ")" at the cursor.
     */
    @Test
    fun noOpOnPaste() {
        val kotlinLang = requireNotNull(LanguageRegistry.get("kotlin"))
        val old = TextFieldValue("", TextRange(0))
        val new = TextFieldValue("(hello", TextRange(6))
        val result = applyBracketAutocomplete(old, new, kotlinLang)
        assertEquals("(hello", result.text)
        assertEquals(6, result.selection.end)
    }
}
