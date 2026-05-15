/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test (iter-60 Phase 6.9, CONST-035 anti-bluff):
 *   Verifies end-to-end snippet expansion: the markdown "table" snippet
 *   is available through SnippetProvider, and when commitCompletionItem
 *   is called the editor's TextFieldValue is updated with the insertText.
 *
 *   Anti-bluff mutation guards:
 *     1. Removing the "table" entry from snippets/markdown/snippets.json
 *        → `markdownTableSnippetIsAvailable` FAILS because forLanguage
 *        returns no snippet with prefix "table".
 *     2. Stubbing commitCompletionItem to a no-op → the TextFieldValue
 *        is unchanged after commit → `commitInsertsSetsEditorText` FAILS.
 *     3. Replacing `item.insertText` with `item.label` in
 *        commitCompletionItem → the inserted text is the label not the
 *        full snippet body → `commitInsertsSetsEditorText` FAILS (label
 *        "Table" != expanded body containing "| Column1 |").
 *     4. Source-level: removing commitCompletionItem from SyncedScrollEditor
 *        → `commitFunctionIsWiredInEditor` FAILS.
 *
 *   Test architecture: pure-function unit tests on the SnippetProvider +
 *   commitCompletionItem helper. runBlocking<Unit> for the async provider call.
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import digital.vasic.yole.android.ui.editor.CompletionPopupState
import digital.vasic.yole.android.ui.editor.commitCompletionItem
import digital.vasic.yole.completion.CompletionContext
import digital.vasic.yole.completion.CompletionItem
import digital.vasic.yole.completion.providers.SnippetProvider
import digital.vasic.yole.completion.trigger.CompletionTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class SnippetExpansionRobolectricTest {

    private fun loadSource(relativePath: String): String {
        val candidates = listOf(
            relativePath,
            "../$relativePath",
            relativePath.removePrefix("androidApp/"),
        )
        for (path in candidates) {
            val f = File(path)
            if (f.isFile) return f.readText()
        }
        error("$relativePath not found; checked: $candidates (cwd=${File(".").absolutePath})")
    }

    private fun loadEditorSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/SyncedScrollEditor.kt"
    )

    // -----------------------------------------------------------------------
    // Layer 1: SnippetProvider returns the markdown table snippet
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: removing the "table" entry from snippets.json →
     * provider returns no item with prefix "table" → this FAILS.
     */
    @Test
    fun markdownTableSnippetIsAvailable() {
        val provider = SnippetProvider()
        val ctx = CompletionContext.of("tab", cursorChar = 3, langId = "markdown")

        val items = runBlocking<List<CompletionItem>> { provider.complete(ctx) }

        val tableItem = items.firstOrNull { it.label.contains("table", ignoreCase = true) }
        assertTrue(
            "SnippetProvider MUST return a 'table' snippet for prefix 'tab' in markdown " +
                "(got: ${items.map { it.label }})",
            tableItem != null,
        )
        // The insert text MUST contain the typical markdown table character.
        assertTrue(
            "Table snippet insertText MUST contain '| Column1 |' (got: ${tableItem?.insertText})",
            tableItem?.insertText?.contains("Column1") == true,
        )
    }

    // -----------------------------------------------------------------------
    // Layer 2: commitCompletionItem applies the insertText to the editor state
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff:
     *   - Stubbing commitCompletionItem to no-op → text unchanged → FAILS.
     *   - Replacing item.insertText with item.label → body not expanded → FAILS.
     */
    @Test
    fun commitInsertsSetsEditorText() {
        val insertText = "| Column1 | Column2 |\n| --- | --- |\n| \$0 |  |"
        val item = CompletionItem(
            label = "Table",
            insertText = insertText,
            kind = CompletionItem.Kind.Snippet,
            score = 1.0,
            range = 0..3, // replaces "tab" (3 chars)
        )

        val textState = mutableStateOf("tab")
        val tfvState = mutableStateOf(TextFieldValue("tab", TextRange(3)))
        val onChangedTexts = mutableListOf<String>()
        val onTextChanged: (String) -> Unit = { onChangedTexts.add(it) }

        val job = Job()
        val scope = CoroutineScope(job)
        val trigger = CompletionTrigger(langId = "markdown", scope = scope)

        commitCompletionItem(item, tfvState, textState, onTextChanged, trigger)

        // The editor text MUST now contain the expanded snippet body.
        assertTrue(
            "After commit, textState MUST contain '| Column1 |' (got: ${textState.value})",
            textState.value.contains("Column1"),
        )
        assertTrue(
            "After commit, tfvState MUST contain '| Column1 |' (got: ${tfvState.value.text})",
            tfvState.value.text.contains("Column1"),
        )
        assertTrue(
            "onTextChanged MUST have been called at least once",
            onChangedTexts.isNotEmpty(),
        )
        assertTrue(
            "onTextChanged MUST have been called with the expanded text",
            onChangedTexts.last().contains("Column1"),
        )

        // Cursor must be placed after the inserted text.
        val expectedCursor = 0 + insertText.length // start(0) + insertText.length
        assertTrue(
            "Cursor MUST be at end of inserted text (expected $expectedCursor, got ${tfvState.value.selection.end})",
            tfvState.value.selection.end == expectedCursor,
        )

        job.cancel()
    }

    /**
     * Anti-bluff: if commitCompletionItem uses `item.range.last` as the
     * end boundary incorrectly (off-by-one), text may be malformed.
     * This test verifies the replacement window is exactly the prefix range.
     */
    @Test
    fun commitReplacesPrefixRange() {
        val item = CompletionItem(
            label = "table",
            insertText = "TABLE_BODY",
            kind = CompletionItem.Kind.Snippet,
            score = 1.0,
            range = 3..6, // replaces chars 3-6
        )
        val initial = "ab tab rest"
        val textState = mutableStateOf(initial)
        val tfvState = mutableStateOf(TextFieldValue(initial, TextRange(6)))

        val job = Job()
        val scope = CoroutineScope(job)
        val trigger = CompletionTrigger(langId = null, scope = scope)

        commitCompletionItem(item, tfvState, textState, {}, trigger)

        // "ab " + "TABLE_BODY" + " rest" (item.range.last=6 → exclusive end)
        val expected = "ab TABLE_BODY rest"
        assertTrue(
            "commitCompletionItem MUST replace the range [3..6) with insertText " +
                "(expected '$expected', got '${textState.value}')",
            textState.value == expected,
        )

        job.cancel()
    }

    // -----------------------------------------------------------------------
    // Layer 3: source-level structural guarantee
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: removing commitCompletionItem from SyncedScrollEditor →
     * snippet expansions (keyboard Enter/Tab + popup click) are broken.
     */
    @Test
    fun commitFunctionIsWiredInEditor() {
        val src = loadEditorSource()
        assertTrue(
            "SyncedScrollEditor MUST define/call commitCompletionItem",
            src.contains("commitCompletionItem("),
        )
        // Both the key-event handler and the popup click MUST call it.
        val occurrences = "commitCompletionItem(".toRegex().findAll(src).count()
        assertTrue(
            "commitCompletionItem MUST be called from both the key-handler and the popup " +
                "(expected ≥ 2 occurrences, got $occurrences)",
            occurrences >= 2,
        )
    }
}
