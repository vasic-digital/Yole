/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 Feature 2 Phase 4: Bracket auto-completion on insert.
 *
 * When the user types an opener that the active LanguageFormat's
 * BracketPairs knows about, this pure function inserts the matching
 * closer and parks the cursor between the pair.
 *
 * Anti-bluff covenant (CONST-035):
 *   - Activates ONLY on a single-character insertion (length delta of
 *     exactly +1) so IME composition, paste, and undo do NOT trigger
 *     accidental closers.
 *   - When language is null OR bracketPairs.closerFor(opener) is null,
 *     returns `new` unchanged (user sees what they typed; no bluff
 *     closer is fabricated).
 *   Stubbing this function to `return new` MUST cause at least one
 *   Robolectric test in BracketAutoCompleterRobolectricTest to fail.
 *
 *########################################################*/
package digital.vasic.yole.android.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import digital.vasic.yole.language.LanguageFormat

/**
 * Apply bracket auto-completion to a TextFieldValue diff.
 *
 * @param old previous TextFieldValue (before user edit)
 * @param new the value emitted by BasicTextField's onValueChange
 * @param language active language; null disables auto-completion
 * @return [new] unchanged unless an opener was inserted and a closer is
 *         known. In the activation case, the returned value has the
 *         closer inserted at the cursor and selection collapsed between
 *         opener and closer.
 */
fun applyBracketAutocomplete(
    old: TextFieldValue,
    new: TextFieldValue,
    language: LanguageFormat?,
): TextFieldValue {
    val bracketPairs = language?.bracketPairs ?: return new

    // Single-character insert only — guards against paste / IME composition.
    val delta = new.text.length - old.text.length
    if (delta != 1) return new

    // Identify the inserted character. The new cursor is positioned
    // AFTER the inserted char; the char itself is at index cursor-1.
    val cursor = new.selection.end
    if (cursor <= 0 || cursor > new.text.length) return new
    val inserted = new.text[cursor - 1]

    val closer = bracketPairs.closerFor(inserted) ?: return new

    val mutated = StringBuilder(new.text)
        .insert(cursor, closer)
        .toString()
    // Cursor parks between opener and closer (at the same offset it was
    // already at — but with the closer pushed to the right).
    return new.copy(text = mutated, selection = TextRange(cursor))
}
