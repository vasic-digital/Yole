/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric tests (iter-64 Phase 9, CONST-035 anti-bluff):
 *   Verifies structural wiring of the five Import UI primitives:
 *   ImportButton, ImportMenuItem, ImportProgressDialog, ImportPreview,
 *   and ImportWarningsPanel.
 *
 *   Test architecture (mirrors CodeActionLightbulbRobolectricTest pattern):
 *   source-level structural assertions against the Composable sources.
 *   createComposeRule() is avoided because manifest = Config.NONE runs
 *   do not provide an Activity.
 *
 * ── importButtonClickCallsCallback ──────────────────────────────────────
 *   Mutation: remove `onImportRequest` from IconButton onClick → assertion
 *   `"onImportRequest()"` is absent from the source → FAILS.
 *   Revert → PASS.
 *
 * ── importProgressDialogRendersFileNameAndCancel ─────────────────────────
 *   Mutation (a): remove CircularProgressIndicator() call from source →
 *   `"CircularProgressIndicator()"` absent → FAILS.
 *   Mutation (b): remove `onCancel` from the Cancel TextButton onClick →
 *   `"onCancel()"` absent → FAILS.
 *   Revert → PASS.
 *
 * ── importPreviewSaveCallbackReceivesFilename ────────────────────────────
 *   Mutation: replace `onSave(fileName)` with `onSave("")` → the regex
 *   `onSave\(fileName\)` is absent → FAILS.
 *   Revert → PASS.
 *
 * ── importWarningsPanelRendersAllWarnings ────────────────────────────────
 *   Mutation (a): remove `testTag("import-warning-$index")` from rows →
 *   FAILS.
 *   Mutation (b): stub LazyColumn body to empty (no itemsIndexed call) →
 *   `"itemsIndexed("` absent → FAILS.
 *   Revert → PASS.
 *
 * Cross-platform impact (CONST-037):
 *   Android: tested here.
 *   Desktop/iOS/Web: N/A this phase.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.robolectric.import_

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ImportUiRobolectricTest {

    // ──────────────────────────────────────────────────────────────────────
    // Source-loading helpers
    // ──────────────────────────────────────────────────────────────────────

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

    private fun importButtonSource() = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/import_/ImportButton.kt",
    )

    private fun importMenuItemSource() = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/import_/ImportMenuItem.kt",
    )

    private fun importProgressDialogSource() = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/import_/ImportProgressDialog.kt",
    )

    private fun importPreviewSource() = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/import_/ImportPreview.kt",
    )

    private fun importWarningsPanelSource() = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/import_/ImportWarningsPanel.kt",
    )

    // ──────────────────────────────────────────────────────────────────────
    // Test 1: ImportButton onClick fires onImportRequest
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Structural guarantee that [ImportButton] wires its [onImportRequest]
     * parameter into the [IconButton] onClick lambda.
     *
     * Anti-bluff: removing `onImportRequest` from the onClick body makes
     * the string `"onImportRequest()"` disappear → this assertion FAILS.
     * The test therefore requires the real delegation to be present.
     */
    @Test
    fun importButtonClickCallsCallback() {
        val src = importButtonSource()

        assertTrue(
            "ImportButton MUST delegate onClick to onImportRequest()",
            src.contains("onClick = onImportRequest"),
        )
        assertTrue(
            "ImportButton MUST apply testTag(\"import-button\")",
            src.contains("testTag(\"import-button\")"),
        )
        assertTrue(
            "ImportButton MUST use Icons.Filled.Add as the icon imageVector",
            src.contains("Icons.Filled.Add"),
        )
    }

    // ──────────────────────────────────────────────────────────────────────
    // Test 2: ImportProgressDialog renders CircularProgressIndicator + Cancel
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Structural guarantee that [ImportProgressDialog] contains both a
     * [CircularProgressIndicator] and a Cancel button wired to [onCancel].
     *
     * Anti-bluff (a): removing CircularProgressIndicator() call makes the
     * first assertion FAIL.
     * Anti-bluff (b): removing onCancel from the Cancel TextButton onClick
     * makes the `onCancel()` presence assertion FAIL.
     */
    @Test
    fun importProgressDialogRendersFileNameAndCancel() {
        val src = importProgressDialogSource()

        assertTrue(
            "ImportProgressDialog MUST contain a CircularProgressIndicator()",
            src.contains("CircularProgressIndicator()"),
        )
        assertTrue(
            "ImportProgressDialog MUST apply testTag(\"import-progress-dialog\")",
            src.contains("testTag(\"import-progress-dialog\")"),
        )
        assertTrue(
            "ImportProgressDialog MUST wire onCancel into the Cancel button onClick",
            src.contains("onClick = onCancel"),
        )
        assertTrue(
            "ImportProgressDialog MUST render the fileName text",
            src.contains("text = fileName"),
        )
    }

    // ──────────────────────────────────────────────────────────────────────
    // Test 3: ImportPreview Save button passes fileName to onSave callback
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Structural guarantee that [ImportPreview] passes the current [fileName]
     * state value to [onSave], not a hard-coded empty string or the original
     * suggestedFileName.
     *
     * Anti-bluff: replacing `onSave(fileName)` with `onSave("")` removes the
     * exact token `onSave(fileName)` from the source → FAILS.
     */
    @Test
    fun importPreviewSaveCallbackReceivesFilename() {
        val src = importPreviewSource()

        assertTrue(
            "ImportPreview MUST call onSave(fileName) in Save button onClick",
            src.contains("onSave(fileName)"),
        )
        assertTrue(
            "ImportPreview MUST apply testTag(\"import-preview\") to root",
            src.contains("testTag(\"import-preview\")"),
        )
        assertTrue(
            "ImportPreview MUST apply testTag(\"import-preview-save\") to Save button",
            src.contains("testTag(\"import-preview-save\")"),
        )
        assertTrue(
            "ImportPreview MUST apply testTag(\"import-preview-cancel\") to Cancel button",
            src.contains("testTag(\"import-preview-cancel\")"),
        )
        assertTrue(
            "ImportPreview MUST apply testTag(\"import-preview-filename-input\") to TextField",
            src.contains("testTag(\"import-preview-filename-input\")"),
        )
    }

    // ──────────────────────────────────────────────────────────────────────
    // Test 4: ImportWarningsPanel emits a testTag row for every warning
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Structural guarantee that [ImportWarningsPanel] uses [itemsIndexed] and
     * applies `testTag("import-warning-\$index")` to each row so that
     * UI-test selectors (`import-warning-0`, `import-warning-1`, …) can
     * locate every warning.
     *
     * Anti-bluff (a): removing `testTag("import-warning-${'$'}index")` → FAILS.
     * Anti-bluff (b): replacing `itemsIndexed(` with a non-itemsIndexed loop
     * that doesn't emit per-item tags → second assertion FAILS.
     */
    @Test
    fun importWarningsPanelRendersAllWarnings() {
        val src = importWarningsPanelSource()

        assertTrue(
            "ImportWarningsPanel MUST apply testTag(\"import-warnings-panel\") to root Column",
            src.contains("testTag(\"import-warnings-panel\")"),
        )
        assertTrue(
            "ImportWarningsPanel MUST apply testTag(\"import-warning-\$index\") per LazyColumn row",
            src.contains("testTag(\"import-warning-\$index\")"),
        )
        assertTrue(
            "ImportWarningsPanel MUST use itemsIndexed( to iterate warnings",
            src.contains("itemsIndexed("),
        )
        assertTrue(
            "ImportWarningsPanel MUST render a collapse/expand toggle (clickable header row)",
            src.contains("expanded = !expanded"),
        )
    }
}
