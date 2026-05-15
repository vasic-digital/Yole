/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test (iter-58 Feature 2 Phase 4, CONST-035 anti-bluff):
 *   Verifies handleEnter inserts `\n` + the indent computed by
 *   IndentRules.computeIndent on the current line.
 *
 *   Anti-bluff covenant: stubbing IndentRules.computeIndent to
 *   `return ""` would cause `indentsOneLevelAfterOpener` AND
 *   `usesLanguageIndentUnit` to FAIL because the inserted indent would
 *   be empty.
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import digital.vasic.yole.android.ui.editor.handleEnter
import digital.vasic.yole.language.LanguageFormat
import digital.vasic.yole.language.LanguageRegistry
import digital.vasic.yole.language.affordance.BracketPairs
import digital.vasic.yole.language.affordance.CommentSyntax
import digital.vasic.yole.language.affordance.IndentRules
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class IndentEngineRobolectricTest {

    /**
     * Line ends with `{` (indent token), cursor at end. Enter should
     * produce `\n    ` because kotlin's indentUnit is "    " (4 spaces).
     * Mutation guard: stubbing IndentRules.computeIndent → "" fails
     * here because the inserted text would be just "\n".
     */
    @Test
    fun indentsOneLevelAfterOpener() {
        val kotlinLang = requireNotNull(LanguageRegistry.get("kotlin"))
        val text = "if (x) {"
        val before = TextFieldValue(text, TextRange(text.length))
        val after = handleEnter(before, kotlinLang)
        assertEquals("if (x) {\n    ", after.text)
        assertEquals(after.text.length, after.selection.end)
    }

    /**
     * Line does not end with an indent-token → indent is preserved (in
     * this case empty leading whitespace, so `\n` alone). Mutation
     * guard: if computeIndent always added an indent we would see
     * trailing spaces.
     */
    @Test
    fun noIndentChangeWithoutOpener() {
        val kotlinLang = requireNotNull(LanguageRegistry.get("kotlin"))
        val text = "val x = 42"
        val before = TextFieldValue(text, TextRange(text.length))
        val after = handleEnter(before, kotlinLang)
        assertEquals("val x = 42\n", after.text)
    }

    /**
     * Per-language indent-unit override: build an ad-hoc LanguageFormat
     * with indentUnit = "  " (2 spaces) and assert the indent uses it.
     * Mutation guard: hardcoding `    ` in handleEnter fails here.
     */
    @Test
    fun usesLanguageIndentUnit() {
        val twoSpaceLang = LanguageFormat(
            id = "test-2sp",
            displayName = "TwoSpace",
            extensions = listOf(".t2"),
            mimeTypes = emptyList(),
            commentSyntax = CommentSyntax(lineComment = "// "),
            indentRules = IndentRules(),
            bracketPairs = BracketPairs(),
            indentUnit = "  ",
        )
        val text = "fun foo() {"
        val before = TextFieldValue(text, TextRange(text.length))
        val after = handleEnter(before, twoSpaceLang)
        assertEquals("fun foo() {\n  ", after.text)
    }

    /**
     * Null language falls back to plain `\n` insertion so the editor
     * remains usable. Mutation guard: throwing or returning unchanged
     * `text` would fail here.
     */
    @Test
    fun fallsBackToPlainNewlineWhenLanguageIsNull() {
        val text = "no language"
        val before = TextFieldValue(text, TextRange(text.length))
        val after = handleEnter(before, null)
        assertEquals("no language\n", after.text)
        assertEquals(after.text.length, after.selection.end)
    }
}
