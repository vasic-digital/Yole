/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test (iter-58 Feature 2 Phase 4, CONST-035 anti-bluff):
 *   Verifies that the Compose key handler returned by
 *   rememberCommentToggleAction actually toggles the line-comment
 *   prefix on the user-visible TextFieldValue. The handler is bound to
 *   the active LanguageFormat via LocalLanguage; here we pass it in
 *   directly to the pure helper that the handler delegates to.
 *
 *   Anti-bluff covenant: stubbing CommentSyntax.toggleLine to
 *   `return line` would cause `togglesCommentWhenLanguageHasLineComment`
 *   and `togglesCommentBackWhenAlreadyCommented` to FAIL because the
 *   asserted post-toggle text would be unchanged.
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import digital.vasic.yole.android.ui.editor.toggleCommentOnSelectedLines
import digital.vasic.yole.language.LanguageRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class CommentToggleActionRobolectricTest {

    /**
     * Kotlin's CommentSyntax has lineComment = "// ". Inserting a line
     * comment on `fun foo()` must produce `// fun foo()`. A stub that
     * returns `line` unchanged would fail this assertion.
     */
    @Test
    fun togglesCommentWhenLanguageHasLineComment() {
        val kotlinLang = requireNotNull(LanguageRegistry.get("kotlin")) {
            "kotlin language must be registered in LanguageMetadata"
        }
        val before = TextFieldValue("fun foo()", TextRange(0))
        val after = toggleCommentOnSelectedLines(before, kotlinLang)
        assertEquals("// fun foo()", after.text)
        // Cursor parks at end of toggled line
        assertEquals(after.text.length, after.selection.end)
    }

    /**
     * Round-trip: already-commented line should uncomment back to the
     * original. Catches a stub where toggleLine always prepends without
     * the un-comment branch.
     */
    @Test
    fun togglesCommentBackWhenAlreadyCommented() {
        val kotlinLang = requireNotNull(LanguageRegistry.get("kotlin"))
        val before = TextFieldValue("// fun foo", TextRange(0))
        val after = toggleCommentOnSelectedLines(before, kotlinLang)
        assertEquals("fun foo", after.text)
    }

    /**
     * When language is null, the helper returns the input unchanged so
     * the editor never fabricates a "//" out of thin air. This is the
     * graceful-degradation contract.
     */
    @Test
    fun noOpWhenLanguageIsNull() {
        val before = TextFieldValue("hello world", TextRange(5))
        val after = toggleCommentOnSelectedLines(before, null)
        assertEquals(before.text, after.text)
        assertEquals(before.selection, after.selection)
    }

    /**
     * Multi-line selection: both lines toggle. Demonstrates the helper
     * actually walks the selection range (mutation guard: if it only
     * toggled the first line, the second assertion fails).
     */
    @Test
    fun togglesEveryLineInSelection() {
        val kotlinLang = requireNotNull(LanguageRegistry.get("kotlin"))
        val text = "fun a()\nfun b()"
        // Select from start of line 1 to end of line 2 (length of text).
        val before = TextFieldValue(text, TextRange(0, text.length))
        val after = toggleCommentOnSelectedLines(before, kotlinLang)
        assertTrue(
            "expected both lines commented, got: ${after.text}",
            after.text == "// fun a()\n// fun b()",
        )
    }
}
