/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 Feature 2 Phase 4: Auto-indent on Enter.
 *
 * When the user presses Enter, this engine inserts `\n` plus the indent
 * computed by IndentRules.computeIndent on the current line. The indent
 * width is taken from LanguageFormat.indentUnit.
 *
 * Anti-bluff covenant (CONST-035):
 *   - When [language] is null we fall back to plain "\n" insertion so
 *     the editor remains usable for unsupported formats.
 *   - The current-line extraction is based on the actual TextFieldValue
 *     selection — replacing it with a hardcoded line MUST cause a
 *     Robolectric test to fail.
 *   - Stubbing IndentRules.computeIndent to `return ""` MUST cause at
 *     least two Robolectric tests in IndentEngineRobolectricTest to
 *     fail (the indent-after-opener case AND the per-language
 *     indentUnit case).
 *
 *########################################################*/
package digital.vasic.yole.android.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import digital.vasic.yole.language.LanguageFormat

/**
 * Pure function — apply an Enter keystroke to [text] using
 * [language]'s indent rules. The current line is the line containing
 * the selection start. If [language] is null, the result is a plain
 * `\n` insertion (the engine degrades gracefully).
 */
fun handleEnter(
    text: TextFieldValue,
    language: LanguageFormat?,
): TextFieldValue {
    val raw = text.text
    val selStart = text.selection.min
    val selEnd = text.selection.max

    // Current-line extraction: start = char after last \n (or 0).
    val lineStart = raw.lastIndexOf('\n', (selStart - 1).coerceAtLeast(0))
        .let { if (it < 0) 0 else it + 1 }
    val currentLine = raw.substring(lineStart, selStart)

    val indent = if (language != null) {
        language.indentRules.computeIndent(currentLine, language.indentUnit)
    } else {
        ""
    }

    val insertion = "\n" + indent
    val newText = raw.substring(0, selStart) + insertion + raw.substring(selEnd)
    val newCursor = selStart + insertion.length
    return text.copy(text = newText, selection = TextRange(newCursor))
}

/**
 * Compose-aware handler factory. Returns `(KeyEvent) -> Boolean` for
 * `Modifier.onKeyEvent`. Consumes the Enter key (KeyDown only); returns
 * false for everything else so unrelated key events propagate.
 */
@Composable
fun rememberIndentEngineAction(
    textState: MutableState<TextFieldValue>,
    language: LanguageFormat?,
): (KeyEvent) -> Boolean = remember(language) {
    handler@{ event: KeyEvent ->
        if (event.type != KeyEventType.KeyDown) return@handler false
        if (event.key != Key.Enter && event.key != Key.NumPadEnter) return@handler false
        textState.value = handleEnter(textState.value, language)
        true
    }
}
