/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric tests (iter-64 Phase 10, CONST-035 anti-bluff):
 *   Verifies that ImportShareIntentHandler correctly extracts bytes from
 *   an ACTION_SEND intent (EXTRA_STREAM URI) and an ACTION_VIEW intent
 *   (intent.data URI).
 *
 * ── importShareIntentHandlerExtractsBytesFromSend ────────────────────────
 *   Mutation: stub handle() to always return null → assertions on the
 *   returned ByteArray immediately FAIL (bytes is null → isNotNull fails).
 *   Revert → PASS.
 *
 * ── importShareIntentHandlerExtractsBytesFromView ────────────────────────
 *   Mutation: remove the Intent.ACTION_VIEW / intent.data branch →
 *   the VIEW path returns null → isNotNull assertion FAILS.
 *   Revert → PASS.
 *
 * ── importShareIntentHandlerReturnsNullForUnknownAction ──────────────────
 *   Structural guard: action not in (SEND, VIEW) MUST produce null.
 *
 * ── importShareIntentHandlerStructuralSourceGuard ─────────────────────────
 *   Source-level anti-bluff: verifies the handler wires EXTRA_STREAM,
 *   intent.data, and openInputStream(uri) in the implementation file.
 *   Mutation: remove any of these tokens → assertion FAILS.
 *   Revert → PASS.
 *
 * Cross-platform impact (CONST-037):
 *   Android: tested here. Android intent system is Android-only.
 *   Desktop / iOS / Web: N/A.
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ImportShareIntentHandlerRobolectricTest {

    // ──────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────

    private val context: android.content.Context
        get() = ApplicationProvider.getApplicationContext()

    /**
     * Registers [bytes] with Robolectric's shadow ContentResolver so that
     * [android.content.ContentResolver.openInputStream] for [uri] returns
     * an InputStream over [bytes].
     */
    private fun stubContentUri(uri: Uri, bytes: ByteArray): Uri {
        shadowOf(context.contentResolver).registerInputStream(uri, bytes.inputStream())
        return uri
    }

    private fun contentUri(name: String) =
        Uri.parse("content://digital.vasic.yole.test/$name")

    // ──────────────────────────────────────────────────────────────────────
    // Test 1 — ACTION_SEND with EXTRA_STREAM
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Anti-bluff (behavioural): synthesises an ACTION_SEND Intent carrying a
     * content URI via EXTRA_STREAM. The handler MUST read the bytes correctly.
     *
     * Mutation evidence: stub handle() to return null → bytes is null →
     * assertNotNull(bytes) FAILS immediately.
     */
    @Test
    fun importShareIntentHandlerExtractsBytesFromSend() {
        val expected = "Hello docx content".toByteArray(Charsets.UTF_8)
        val uri = stubContentUri(contentUri("test.docx"), expected)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            putExtra(Intent.EXTRA_STREAM, uri)
        }

        val bytes = ImportShareIntentHandler.handle(context, intent)

        assertNotNull("handle() MUST return non-null bytes for a valid EXTRA_STREAM URI", bytes)
        assertArrayEquals(
            "handle() MUST return the exact bytes from the content URI",
            expected,
            bytes,
        )
    }

    // ──────────────────────────────────────────────────────────────────────
    // Test 2 — ACTION_VIEW with intent.data
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Anti-bluff (behavioural): synthesises an ACTION_VIEW Intent whose data
     * URI points to a PDF stub. The handler MUST read bytes via intent.data.
     *
     * Mutation evidence: remove the intent.data branch → returns null →
     * assertNotNull FAILS.
     */
    @Test
    fun importShareIntentHandlerExtractsBytesFromView() {
        val expected = "%PDF-1.4 stub".toByteArray(Charsets.UTF_8)
        val uri = stubContentUri(contentUri("test.pdf"), expected)

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            type = "application/pdf"
        }

        val bytes = ImportShareIntentHandler.handle(context, intent)

        assertNotNull("handle() MUST return non-null bytes for a valid ACTION_VIEW URI", bytes)
        assertArrayEquals(
            "handle() MUST return the exact bytes from the content URI (VIEW path)",
            expected,
            bytes,
        )
    }

    // ──────────────────────────────────────────────────────────────────────
    // Test 3 — unknown action returns null
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Structural guard: any intent with an action other than SEND/VIEW MUST
     * produce null (no crash, no bogus bytes).
     */
    @Test
    fun importShareIntentHandlerReturnsNullForUnknownAction() {
        val intent = Intent("android.intent.action.SOME_OTHER_ACTION")
        val bytes = ImportShareIntentHandler.handle(context, intent)
        assertNull(
            "handle() MUST return null for an unrecognised intent action",
            bytes,
        )
    }

    // ──────────────────────────────────────────────────────────────────────
    // Test 4 — source-level structural guard (anti-bluff anchor)
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Source-level anti-bluff: verifies that the implementation file contains
     * the three critical tokens that make the behavioural tests meaningful:
     *   - Intent.EXTRA_STREAM   → SEND path reads the parcelable extra
     *   - intent.data           → VIEW path reads intent.data
     *   - openInputStream(uri)  → bytes are actually read from the URI
     *
     * Mutation: removing any of these tokens makes the corresponding
     * behavioural test also FAIL, but this structural check provides an
     * independent, fast-failing guard that names the missing token explicitly.
     */
    @Test
    fun importShareIntentHandlerStructuralSourceGuard() {
        val src = loadHandlerSource()

        assertTrue(
            "ImportShareIntentHandler MUST reference Intent.EXTRA_STREAM for the SEND path",
            src.contains("Intent.EXTRA_STREAM"),
        )
        assertTrue(
            "ImportShareIntentHandler MUST reference intent.data for the VIEW path",
            src.contains("intent.data"),
        )
        assertTrue(
            "ImportShareIntentHandler MUST call openInputStream(uri) to read bytes",
            src.contains("openInputStream(uri)"),
        )
        assertTrue(
            "ImportShareIntentHandler MUST handle Intent.ACTION_SEND",
            src.contains("Intent.ACTION_SEND"),
        )
        assertTrue(
            "ImportShareIntentHandler MUST handle Intent.ACTION_VIEW",
            src.contains("Intent.ACTION_VIEW"),
        )
    }

    // ──────────────────────────────────────────────────────────────────────
    // Source loader (mirrors CompletionExplicitTriggerRobolectricTest pattern)
    // ──────────────────────────────────────────────────────────────────────

    private fun loadHandlerSource(): String {
        val relativePath =
            "androidApp/src/main/java/digital/vasic/yole/android/ui/import_/ImportShareIntentHandler.kt"
        val candidates = listOf(
            relativePath,
            "../$relativePath",
            relativePath.removePrefix("androidApp/"),
        )
        for (path in candidates) {
            val f = File(path)
            if (f.isFile) return f.readText()
        }
        error(
            "ImportShareIntentHandler.kt not found; checked: $candidates " +
                "(cwd=${File(".").absolutePath})",
        )
    }
}
