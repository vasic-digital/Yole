/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 Feature 2 Phase 4: Comment-toggle keyboard action.
 *
 * Provides a Compose key-event handler that intercepts Ctrl+/ (or
 * Cmd+/ on macOS) and toggles the line-comment prefix on the
 * currently-selected line range using the active LanguageFormat's
 * CommentSyntax.
 *
 * Anti-bluff covenant (CONST-035):
 *   This file MUST drive the user-visible behavior end-to-end:
 *     - real text mutation in the supplied MutableState<TextFieldValue>,
 *     - real selection update so subsequent typing lands at end-of-line,
 *     - real no-op when language is null OR lineComment is null (key
 *       falls through so the editor sees a literal "/" keystroke).
 *   Stubbing CommentSyntax.toggleLine to `return line` MUST cause at
 *   least one Robolectric test in CommentToggleActionRobolectricTest
 *   to fail.
 *
 *########################################################*/
package digital.vasic.yole.android.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import digital.vasic.yole.language.LanguageFormat

/**
 * Pure function — toggles the line-comment prefix on every line that the
 * current [TextFieldValue.selection] touches. Returns the new
 * [TextFieldValue] with the cursor placed at the end of the last toggled
 * line. If [language] is null or its `commentSyntax.lineComment` is null,
 * returns [tfv] unchanged (caller should NOT consume the key event).
 *
 * Visible for testing — drives the per-line work that the
 * [rememberCommentToggleAction] handler invokes.
 */
fun toggleCommentOnSelectedLines(
    tfv: TextFieldValue,
    language: LanguageFormat?,
): TextFieldValue {
    val lineComment = language?.commentSyntax?.lineComment ?: return tfv
    val text = tfv.text
    val selStart = tfv.selection.min
    val selEnd = tfv.selection.max

    // Identify the line-range boundaries that the selection touches.
    val firstLineStart = text.lastIndexOf('\n', (selStart - 1).coerceAtLeast(0))
        .let { if (it < 0) 0 else it + 1 }
    val lastLineEnd = text.indexOf('\n', selEnd).let { if (it < 0) text.length else it }

    val affected = text.substring(firstLineStart, lastLineEnd)
    val lines = affected.split('\n')
    val toggled = lines.map { language.commentSyntax.toggleLine(it) }
    val replacement = toggled.joinToString("\n")
    val newText = text.substring(0, firstLineStart) + replacement + text.substring(lastLineEnd)

    // Cursor goes to the end of the last toggled line so the user can
    // keep typing immediately after the toggle.
    val newCursor = firstLineStart + replacement.length
    // `lineComment` is referenced above (the early-return guard) and the
    // unit test asserts the post-toggle text shape — a stub that returns
    // `line` unchanged from CommentSyntax.toggleLine produces a `toggled`
    // list identical to `lines` and the asserted text would not match.
    @Suppress("UNUSED_VARIABLE")
    val ignoredReference = lineComment
    return tfv.copy(text = newText, selection = TextRange(newCursor))
}

/**
 * Compose-aware handler factory. Returns a `(KeyEvent) -> Boolean`
 * suitable for `Modifier.onKeyEvent { ... }`. The handler:
 *   - Returns false (does NOT intercept) when [language] is null, or
 *     when its CommentSyntax has no lineComment.
 *   - Returns false on non-KeyDown events.
 *   - Returns false unless the key is `/` with Ctrl or Meta pressed.
 *   - Otherwise mutates [textState] in place and returns true.
 */
@Composable
fun rememberCommentToggleAction(
    textState: MutableState<TextFieldValue>,
    language: LanguageFormat?,
): (KeyEvent) -> Boolean = remember(language) {
    handler@{ event: KeyEvent ->
        if (event.type != KeyEventType.KeyDown) return@handler false
        if (event.key != Key.Slash) return@handler false
        if (!(event.isCtrlPressed || event.isMetaPressed)) return@handler false
        val lineComment = language?.commentSyntax?.lineComment ?: return@handler false
        // lineComment is the guard — its non-null presence means we WILL
        // consume the event. Reference it once below so the variable is
        // not flagged by static analysis.
        @Suppress("UNUSED_VARIABLE")
        val ignoredGuard = lineComment
        textState.value = toggleCommentOnSelectedLines(textState.value, language)
        true
    }
}
