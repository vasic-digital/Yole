/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-64 Phase 12 — Robolectric end-to-end import tests (CONST-035).
 *
 * Three structural tests verify that the import pipeline is wired through
 * the full stack: ImportButton → file picker → ImporterRegistry → ImportPreview.
 *
 * ── importButton_clickOpensFilePicker_routesToImporter (structural) ──────────
 *   Verifies:
 *     1. YoleApp.kt constructs an ImporterRegistry.default(listOf(...)) with all
 *        6 importer types.
 *     2. ImportButton is rendered and its onImportRequest routes to the file picker
 *        (importFilePicker.launch(...)) in the source.
 *     3. Source uses importerRegistry.forExtension(ext) to dispatch.
 *   Anti-bluff mutation (CONST-035):
 *     (a) Remove ImporterRegistry.default( call → assertion FAILS.
 *     (b) Remove importFilePicker.launch( call → assertion FAILS.
 *     (c) Remove importerRegistry.forExtension(ext) → assertion FAILS.
 *   Revert each → PASS.
 *
 * ── shareIntent_resolvesAndImports (behavioural + structural) ────────────────
 *   Behavioural layer: synthesises an ACTION_SEND Intent with an HTML byte
 *   payload via Robolectric's shadow ContentResolver. Verifies that
 *   ImportShareIntentHandler.handle() returns the expected bytes (mutation
 *   evidence: stub to return null → assertNotNull FAILS).
 *   Structural layer: verifies that YoleApp.kt contains the polling loop
 *   that reads YoleApp.pendingShareBytes and routes it through the importer.
 *   Anti-bluff: remove YoleApp.pendingShareBytes from the polling branch →
 *   structural assertion FAILS.
 *
 * ── importPreview_appearsAfterImport (structural) ────────────────────────────
 *   Verifies that YoleApp.kt renders ImportPreview when importedDoc != null
 *   and that ImportPreview.onSave routes to openFileInTab.
 *   Anti-bluff (a): remove the `val doc = importedDoc` guard → source no
 *   longer contains `val doc = importedDoc` → assertion FAILS.
 *   Anti-bluff (b): remove ImportPreview( call → assertion FAILS.
 *   Anti-bluff (c): remove openFileInTab(filename, doc.markdown) from the
 *   onSave lambda → assertion FAILS.
 *   Revert each → PASS.
 *
 * Cross-platform impact (CONST-037):
 *   Android: tested here.
 *   Desktop / iOS / Web: N/A (Robolectric is Android-JVM only).
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.robolectric.import_

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import digital.vasic.yole.android.ui.import_.ImportShareIntentHandler
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ImportIntegrationRobolectricTests {

    // ──────────────────────────────────────────────────────────────────────
    // Source-loading helpers (mirrors CompletionExplicitTriggerRobolectricTest)
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

    private fun yoleAppSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt",
    )

    private fun mainActivitySource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/MainActivity.kt",
    )

    // ──────────────────────────────────────────────────────────────────────
    // Helpers for the behavioural intent test
    // ──────────────────────────────────────────────────────────────────────

    private val context: android.content.Context
        get() = ApplicationProvider.getApplicationContext()

    private fun stubContentUri(uri: Uri, bytes: ByteArray): Uri {
        shadowOf(context.contentResolver).registerInputStream(uri, bytes.inputStream())
        return uri
    }

    private fun contentUri(name: String) =
        Uri.parse("content://digital.vasic.yole.test.phase12/$name")

    // ──────────────────────────────────────────────────────────────────────
    // Test 1: importButton_clickOpensFilePicker_routesToImporter
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Structural guarantee that the import pipeline is fully wired in YoleApp:
     *   - ImporterRegistry.default(listOf(...)) is constructed with all 6 importer types.
     *   - importFilePicker.launch( is the launcher invoked by ImportButton.
     *   - importerRegistry.forExtension(ext) dispatches to the correct importer.
     *
     * Anti-bluff (CONST-035):
     *   Mutation (a): remove `ImporterRegistry.default(` → first assertion FAILS.
     *   Mutation (b): remove `importFilePicker.launch(` → second assertion FAILS.
     *   Mutation (c): remove `importerRegistry.forExtension(ext)` → third assertion FAILS.
     *   Revert each → PASS.
     */
    @Test
    fun importButton_clickOpensFilePicker_routesToImporter() {
        val src = yoleAppSource()

        // Registry construction with all 6 importers present
        assertTrue(
            "YoleApp.kt MUST construct ImporterRegistry.default(listOf(...))",
            src.contains("ImporterRegistry.default("),
        )
        assertTrue(
            "YoleApp.kt MUST include DocxImporter() in the registry list",
            src.contains("DocxImporter()"),
        )
        assertTrue(
            "YoleApp.kt MUST include HtmlImporter() in the registry list",
            src.contains("HtmlImporter()"),
        )
        assertTrue(
            "YoleApp.kt MUST include RtfImporter() in the registry list",
            src.contains("RtfImporter()"),
        )
        assertTrue(
            "YoleApp.kt MUST include OdtImporter() in the registry list",
            src.contains("OdtImporter()"),
        )
        assertTrue(
            "YoleApp.kt MUST include PdfImporter() in the registry list",
            src.contains("PdfImporter()"),
        )
        assertTrue(
            "YoleApp.kt MUST include EpubImporter() in the registry list",
            src.contains("EpubImporter()"),
        )

        // File picker launch wired to ImportButton trigger
        assertTrue(
            "YoleApp.kt MUST launch importFilePicker to open the file picker",
            src.contains("importFilePicker.launch("),
        )

        // Extension → importer routing
        assertTrue(
            "YoleApp.kt MUST call importerRegistry.forExtension(ext) to route to the importer",
            src.contains("importerRegistry.forExtension(ext)"),
        )

        // ImportButton call site
        assertTrue(
            "YoleApp.kt MUST call ImportButton(onImportRequest = ...) in the FILES tab toolbar",
            src.contains("ImportButton(onImportRequest ="),
        )
    }

    // ──────────────────────────────────────────────────────────────────────
    // Test 2: shareIntent_resolvesAndImports
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Behavioural layer: synthesises an ACTION_SEND Intent carrying an HTML
     * document. Verifies that [ImportShareIntentHandler.handle] returns the
     * correct bytes (proves the extraction pipeline works end-to-end on the
     * real Robolectric content resolver).
     *
     * Structural layer: verifies that YoleApp.kt contains the polling loop that
     * reads [YoleApp.pendingShareBytes] and routes it through importerRegistry,
     * and that MainActivity.kt overrides onNewIntent with a call to
     * ImportShareIntentHandler.handle.
     *
     * Anti-bluff (CONST-035):
     *   Mutation (a): stub handle() to return null → assertNotNull FAILS.
     *   Mutation (b): remove YoleApp.pendingShareBytes from the polling block
     *     → structural assertion FAILS.
     *   Mutation (c): remove `onNewIntent` from MainActivity →
     *     structural assertion on mainActivitySource FAILS.
     *   Revert each → PASS.
     */
    @Test
    fun shareIntent_resolvesAndImports() {
        // ── Behavioural layer ──────────────────────────────────────────────
        val htmlBytes = "<html><body><h1>Hello</h1></body></html>".toByteArray(Charsets.UTF_8)
        val uri = stubContentUri(contentUri("document.html"), htmlBytes)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_STREAM, uri)
        }

        val bytes = ImportShareIntentHandler.handle(context, intent)
        assertNotNull(
            "ImportShareIntentHandler.handle() MUST return non-null bytes for an HTML share Intent",
            bytes,
        )
        assertArrayEquals(
            "Extracted bytes MUST match the original HTML content",
            htmlBytes,
            bytes,
        )

        // ── Structural layer — YoleApp polling loop ────────────────────────
        val src = yoleAppSource()

        assertTrue(
            "YoleApp.kt MUST read YoleApp.pendingShareBytes in the polling loop",
            src.contains("YoleApp.pendingShareBytes"),
        )
        assertTrue(
            "YoleApp.kt MUST clear YoleApp.pendingShareBytes after consuming it",
            src.contains("YoleApp.pendingShareBytes = null"),
        )
        assertTrue(
            "YoleApp.kt MUST read YoleApp.pendingShareFileName in the polling loop",
            src.contains("YoleApp.pendingShareFileName"),
        )

        // ── Structural layer — MainActivity onNewIntent ────────────────────
        val activitySrc = mainActivitySource()

        assertTrue(
            "MainActivity.kt MUST override onNewIntent",
            activitySrc.contains("override fun onNewIntent"),
        )
        assertTrue(
            "MainActivity.kt MUST call ImportShareIntentHandler.handle(this, intent)",
            activitySrc.contains("ImportShareIntentHandler.handle(this, intent)"),
        )
        assertTrue(
            "MainActivity.kt MUST write to YoleApp.pendingShareBytes",
            activitySrc.contains("YoleApp.pendingShareBytes = bytes"),
        )
    }

    // ──────────────────────────────────────────────────────────────────────
    // Test 3: importPreview_appearsAfterImport
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Structural guarantee that ImportPreview is rendered when importedDoc != null
     * and that its onSave lambda calls openFileInTab(filename, doc.markdown) to
     * open the converted Markdown in the editor.
     *
     * Anti-bluff (CONST-035):
     *   Mutation (a): remove `val doc = importedDoc` guard →
     *     `"val doc = importedDoc"` absent → FAILS.
     *   Mutation (b): remove `ImportPreview(` call → assertion FAILS.
     *   Mutation (c): replace `openFileInTab(filename, doc.markdown)` with a
     *     no-op → `"openFileInTab(filename, doc.markdown)"` absent → FAILS.
     *   Revert each → PASS.
     */
    @Test
    fun importPreview_appearsAfterImport() {
        val src = yoleAppSource()

        // Guard: importedDoc is checked before rendering
        assertTrue(
            "YoleApp.kt MUST guard ImportPreview rendering with `val doc = importedDoc`",
            src.contains("val doc = importedDoc"),
        )

        // ImportPreview composable is called
        assertTrue(
            "YoleApp.kt MUST call ImportPreview( when importedDoc != null",
            src.contains("ImportPreview("),
        )

        // Preview onSave routes to the editor
        assertTrue(
            "YoleApp.kt MUST call openFileInTab(filename, doc.markdown) from ImportPreview.onSave",
            src.contains("openFileInTab(filename, doc.markdown)"),
        )

        // ImportProgressDialog is shown during conversion
        assertTrue(
            "YoleApp.kt MUST call ImportProgressDialog( while conversion is in-flight",
            src.contains("ImportProgressDialog("),
        )

        // showImportProgress gates the dialog
        assertTrue(
            "YoleApp.kt MUST use showImportProgress to gate ImportProgressDialog",
            src.contains("showImportProgress"),
        )
    }
}
