/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 8: FormattingTrigger — on-save, explicit, and on-type entry points.
 *
 * Three formatting invocation modes:
 *   1. onSave    — checks settings().formatOnSave before calling formatter.formatting().
 *   2. onExplicit — always calls formatter.formatting() regardless of settings.
 *   3. onType    — calls onTypeFormatter() only when triggerChar is in serverTriggerChars.
 *
 * A Mutex guards concurrent invocations per Phase 0 §8: on-save and explicit
 * calls both acquire the lock before calling the back-end. If an on-save call
 * is already in progress when an explicit call arrives, the explicit call waits
 * its turn — it does NOT cancel the on-save (conservative, avoids edit conflicts).
 *
 * onType is intentionally NOT guarded by the mutex: it fires on every keystroke,
 * must return quickly, and does not conflict with on-save / explicit because the
 * server handles the LSP-level sequencing.
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   onSave_appliesFormat_whenEnabled:
 *     Mutation: stub onSave to always return emptyList() → assertNotEquals FAILS.
 *   onSave_skipsFormat_whenDisabled:
 *     Mutation: remove `if (!settings()) return emptyList()` guard →
 *       formatter.callCount becomes 1 → assertEquals(0, callCount) FAILS.
 *   onExplicit_alwaysApplies:
 *     Mutation: add `if (!settings())` guard to onExplicit →
 *       result is emptyList() → assertEquals(2, size) FAILS.
 *   onType_appliesOnlyMatchingChar:
 *     Mutation: remove `if (triggerChar !in serverTriggerChars) return emptyList()` guard →
 *       onTypeFormatter is called for non-matching char → assertEquals(0, callCount) FAILS.
 *
 * Cross-platform impact (CONST-037):
 *   - Common: pure Kotlin / coroutines — compiles on all targets.
 *   - Desktop: wired to LspServerHost.onTypeFormatting in Phase 10.
 *   - Android: wired via IdeEditorScreen in Phase 10.
 *   - iOS/Wasm: compiles; LspServerHost.onTypeFormatting returns emptyList().
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Central dispatch for the three formatting invocation modes:
 * on-save (settings-gated), explicit (always), and on-type (trigger-char-gated).
 *
 * @param formatter        LSP formatting back-end seam — see [LspFormattingRequester].
 * @param onTypeFormatter  Lambda to invoke for on-type formatting. Parameters:
 *                         (langId, uri, line, character, triggerChar) → List<TextEdit>.
 *                         Provided as a lambda (not interface) because onTypeFormatting
 *                         is a 6th capability added in Phase 8, beyond the 5 requester
 *                         interfaces introduced in Phase 4.
 * @param settings         Returns true when "Format on save" is enabled (per user setting).
 *                         Evaluated at call time — not cached.
 */
class FormattingTrigger(
    private val formatter: LspFormattingRequester,
    private val onTypeFormatter: suspend (langId: String, uri: String, line: Int, character: Int, triggerChar: Char) -> List<TextEdit>,
    private val settings: () -> Boolean,
) {

    /**
     * Mutex protecting concurrent on-save and explicit format invocations.
     * Prevents two overlapping full-document format requests from producing
     * conflicting edit lists. See Phase 0 §8 atomic version-stamp requirement.
     */
    private val mutex = Mutex()

    /**
     * On-save formatting gate.
     *
     * Acquires [mutex], then returns `formatter.formatting(...)` when
     * `settings()` is true, or an empty list when it is false.
     *
     * @param langId    Language identifier for the document.
     * @param uri       Document URI.
     * @param indentSize Indent level width (default 4).
     * @param useSpaces  True → spaces; false → tabs.
     * @return List of [TextEdit] to apply, or empty list when formatting is
     *         disabled or the server returns no edits.
     */
    suspend fun onSave(
        langId: String,
        uri: String,
        indentSize: Int = 4,
        useSpaces: Boolean = true,
    ): List<TextEdit> = mutex.withLock {
        if (!settings()) return@withLock emptyList()
        formatter.formatting(langId, uri, indentSize, useSpaces)
    }

    /**
     * Explicit formatting — bypasses the settings gate.
     *
     * Always acquires [mutex] and calls `formatter.formatting(...)`, regardless
     * of the `settings()` value. Used for Ctrl+Shift+F keyboard shortcut.
     *
     * @param langId    Language identifier for the document.
     * @param uri       Document URI.
     * @param indentSize Indent level width (default 4).
     * @param useSpaces  True → spaces; false → tabs.
     * @return List of [TextEdit] to apply, or empty list when the server
     *         returns no edits or is unavailable.
     */
    suspend fun onExplicit(
        langId: String,
        uri: String,
        indentSize: Int = 4,
        useSpaces: Boolean = true,
    ): List<TextEdit> = mutex.withLock {
        formatter.formatting(langId, uri, indentSize, useSpaces)
    }

    /**
     * On-type formatting gate.
     *
     * Returns `onTypeFormatter(...)` when [triggerChar] is contained in
     * [serverTriggerChars], or an empty list when it is not. Does NOT acquire
     * the mutex (see class KDoc for rationale).
     *
     * Per Phase 0 §3: rust-analyzer supports 8 trigger chars, clangd only `}`,
     * gopls has no on-type support → callers should pass an empty set for gopls.
     *
     * @param langId             Language identifier.
     * @param uri                Document URI.
     * @param line               0-based cursor line after the keystroke.
     * @param character          0-based cursor character offset after the keystroke.
     * @param triggerChar        The character that was typed.
     * @param serverTriggerChars Set of chars the LSP server declared as
     *                           `documentOnTypeFormattingProvider.firstTriggerCharacter`
     *                           plus `moreTriggerCharacter` entries.
     * @return List of [TextEdit] to apply, or empty list when [triggerChar]
     *         is not in [serverTriggerChars].
     */
    suspend fun onType(
        langId: String,
        uri: String,
        line: Int,
        character: Int,
        triggerChar: Char,
        serverTriggerChars: Set<Char>,
    ): List<TextEdit> {
        if (triggerChar !in serverTriggerChars) return emptyList()
        return onTypeFormatter(langId, uri, line, character, triggerChar)
    }
}
