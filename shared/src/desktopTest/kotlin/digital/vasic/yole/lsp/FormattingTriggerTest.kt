/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 8: FormattingTriggerTest — desktopTest.
 *
 * Tests [FormattingTrigger] using fake implementations of
 * [LspFormattingRequester] and the on-type lambda.
 *
 * Placed in desktopTest because:
 *   - Uses runBlocking<Unit> (JUnit4 convention per CLAUDE.md).
 *   - kotlinx-coroutines-test has no WASM variant — unavailable in commonTest.
 *
 * Tests:
 *   1. onSave_appliesFormat_whenEnabled
 *   2. onSave_skipsFormat_whenDisabled
 *   3. onExplicit_alwaysApplies
 *   4. onType_appliesOnlyMatchingChar
 *
 * Anti-bluff mutation procedure (CONST-035):
 *
 *   onSave_appliesFormat_whenEnabled:
 *     Mutation: stub formatter to return emptyList() → assertEquals(2, result.size) FAILS.
 *
 *   onSave_skipsFormat_whenDisabled:
 *     Mutation: remove `if (!settings()) return emptyList()` guard in onSave →
 *       formatter is called (callCount=1) → assertEquals(0, formatter.callCount) FAILS.
 *
 *   onExplicit_alwaysApplies:
 *     Mutation: add `if (!settings()) return emptyList()` guard to onExplicit →
 *       result is empty → assertEquals(2, result.size) FAILS (settings=false).
 *
 *   onType_appliesOnlyMatchingChar:
 *     Mutation: remove `if (triggerChar !in serverTriggerChars) return emptyList()` guard →
 *       onTypeFormatter is invoked for non-matching char → assertEquals(0, onTypeCallCount) FAILS.
 *
 * Cross-platform impact (CONST-037):
 *   - Desktop: tested here via desktopTest JVM runner.
 *   - Common: FormattingTrigger is in commonMain — compiles on all targets.
 *   - Android: wired in IdeEditorScreen (Phase 10).
 *   - iOS/Wasm: compiles; onTypeFormatting returns emptyList().
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// ---------------------------------------------------------------------------
// Test doubles
// ---------------------------------------------------------------------------

/**
 * Fake [LspFormattingRequester] that returns [returns] and records
 * call count for mutation-verification.
 */
private class FakeFormattingRequester(
    private val returns: List<TextEdit>,
) : LspFormattingRequester {
    var callCount = 0

    override suspend fun formatting(
        langId: String,
        uri: String,
        indentSize: Int,
        useSpaces: Boolean,
    ): List<TextEdit> {
        callCount++
        return returns
    }
}

/** Shared canned edits used across tests. */
private val CANNED_EDITS = listOf(
    TextEdit(range = 0..3, newText = "    "),
    TextEdit(range = 40..43, newText = "    "),
)

// ---------------------------------------------------------------------------
// Test class
// ---------------------------------------------------------------------------

class FormattingTriggerTest {

    // -----------------------------------------------------------------------
    // Test 1: onSave_appliesFormat_whenEnabled
    // -----------------------------------------------------------------------

    /**
     * When settings() returns true, onSave MUST call formatter.formatting()
     * and return its result.
     *
     * Anti-bluff: stub formatter to emptyList() → assertEquals(2, result.size) FAILS.
     *
     * Mutation: stub onSave to always return emptyList() → assertion FAILS.
     */
    @Test
    fun onSave_appliesFormat_whenEnabled() = runBlocking<Unit> {
        val formatter = FakeFormattingRequester(returns = CANNED_EDITS)
        var onTypeCallCount = 0
        val trigger = FormattingTrigger(
            formatter = formatter,
            onTypeFormatter = { _, _, _, _, _ ->
                onTypeCallCount++
                emptyList()
            },
            settings = { true }, // format on save ENABLED
        )

        val result = trigger.onSave(
            langId = "kotlin",
            uri = "file:///src/Foo.kt",
        )

        assertEquals(
            "onSave with settings=true MUST return formatter's edits",
            2,
            result.size,
        )
        assertEquals(
            "formatter.formatting() MUST be called exactly once",
            1,
            formatter.callCount,
        )
        assertEquals(CANNED_EDITS[0], result[0])
        assertEquals(CANNED_EDITS[1], result[1])
    }

    // -----------------------------------------------------------------------
    // Test 2: onSave_skipsFormat_whenDisabled
    // -----------------------------------------------------------------------

    /**
     * When settings() returns false, onSave MUST return emptyList() without
     * calling formatter.formatting() at all.
     *
     * Anti-bluff: remove the settings check → formatter is called (callCount=1)
     * → assertEquals(0, formatter.callCount) FAILS.
     *
     * Mutation: remove `if (!settings()) return emptyList()` → FAIL.
     */
    @Test
    fun onSave_skipsFormat_whenDisabled() = runBlocking<Unit> {
        val formatter = FakeFormattingRequester(returns = CANNED_EDITS)
        val trigger = FormattingTrigger(
            formatter = formatter,
            onTypeFormatter = { _, _, _, _, _ -> emptyList() },
            settings = { false }, // format on save DISABLED
        )

        val result = trigger.onSave(
            langId = "kotlin",
            uri = "file:///src/Bar.kt",
        )

        assertTrue(
            "onSave with settings=false MUST return emptyList()",
            result.isEmpty(),
        )
        assertEquals(
            "formatter.formatting() MUST NOT be called when settings=false",
            0,
            formatter.callCount,
        )
    }

    // -----------------------------------------------------------------------
    // Test 3: onExplicit_alwaysApplies
    // -----------------------------------------------------------------------

    /**
     * onExplicit MUST call formatter.formatting() regardless of settings().
     * Even when settings() returns false, the explicit Ctrl+Shift+F invocation
     * MUST apply formatting.
     *
     * Anti-bluff: add `if (!settings()) return emptyList()` to onExplicit →
     * result.size == 0 → assertEquals(2, result.size) FAILS.
     *
     * Mutation: add settings gate to onExplicit → FAIL.
     */
    @Test
    fun onExplicit_alwaysApplies() = runBlocking<Unit> {
        val formatter = FakeFormattingRequester(returns = CANNED_EDITS)
        val trigger = FormattingTrigger(
            formatter = formatter,
            onTypeFormatter = { _, _, _, _, _ -> emptyList() },
            settings = { false }, // settings=false — must NOT block explicit
        )

        val result = trigger.onExplicit(
            langId = "rust",
            uri = "file:///src/main.rs",
        )

        assertEquals(
            "onExplicit MUST return formatter's edits even when settings=false",
            2,
            result.size,
        )
        assertEquals(
            "formatter.formatting() MUST be called exactly once from onExplicit",
            1,
            formatter.callCount,
        )
    }

    // -----------------------------------------------------------------------
    // Test 4: onType_appliesOnlyMatchingChar
    // -----------------------------------------------------------------------

    /**
     * onType MUST invoke onTypeFormatter ONLY when triggerChar is in
     * serverTriggerChars. For chars not in the set, emptyList() is returned
     * without calling onTypeFormatter.
     *
     * Anti-bluff: remove the `if (triggerChar !in serverTriggerChars)` guard →
     * onTypeFormatter is invoked for ','; assertEquals(0, onTypeCallCount) FAILS.
     *
     * Mutation: remove trigger-char guard → FAIL on non-matching char test.
     */
    @Test
    fun onType_appliesOnlyMatchingChar() = runBlocking<Unit> {
        val expectedEdits = listOf(TextEdit(range = 10..12, newText = " "))
        var onTypeCallCount = 0
        val trigger = FormattingTrigger(
            formatter = FakeFormattingRequester(returns = emptyList()),
            onTypeFormatter = { _, _, _, _, _ ->
                onTypeCallCount++
                expectedEdits
            },
            settings = { true },
        )

        val serverTriggerChars = setOf(';', '}')

        // Matching char: ';' IN the server set → should invoke onTypeFormatter.
        val resultMatching = trigger.onType(
            langId = "cpp",
            uri = "file:///src/main.cpp",
            line = 5,
            character = 10,
            triggerChar = ';',
            serverTriggerChars = serverTriggerChars,
        )
        assertEquals(
            "onType with matching triggerChar MUST return onTypeFormatter's edits",
            1,
            resultMatching.size,
        )
        assertEquals(
            "onTypeFormatter MUST be called exactly once for matching char",
            1,
            onTypeCallCount,
        )

        // Non-matching char: ',' NOT in the server set → should return emptyList().
        val resultNonMatching = trigger.onType(
            langId = "cpp",
            uri = "file:///src/main.cpp",
            line = 5,
            character = 11,
            triggerChar = ',',
            serverTriggerChars = serverTriggerChars,
        )
        assertTrue(
            "onType with non-matching triggerChar MUST return emptyList()",
            resultNonMatching.isEmpty(),
        )
        assertEquals(
            "onTypeFormatter MUST NOT be called again for non-matching char",
            1,
            onTypeCallCount, // still 1, not 2
        )
    }
}
