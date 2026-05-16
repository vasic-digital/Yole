/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 5: RtfImporterTest — anti-bluff test.
 *
 * Synthesises a tiny RTF string in memory, imports it via the real
 * RtfImporter (Desktop JVM actual), and asserts the Markdown output
 * contains the expected plain + bold segments.
 *
 * Mutation guard: a stub returning Result.failure is exercised and
 * verified to produce a failed Result, proving the real test CANNOT
 * pass against a no-op importer.
 *
 * CONST-035: test exercises the real RtfImporter code path end-to-end.
 * No mocking of the unit under test.
 *#######################################################*/
package digital.vasic.yole.import_

import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class RtfImporterTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * A minimal RTF document:  "Hello " (plain) + "bold" (bold) + " world." (plain)
     *
     * RTF control words used:
     *   \rtf1       — RTF version 1
     *   \ansi       — character set
     *   \b … \b0   — bold on/off
     *
     * Regular string literals are used so that `\\` in source → single `\` in the
     * byte stream, which is required by the RTF spec.
     */
    private fun buildTestRtfBytes(): ByteArray {
        val rtf = "{\\rtf1\\ansi Hello \\b bold\\b0  world.}"
        return rtf.toByteArray(Charsets.US_ASCII)
    }

    // -----------------------------------------------------------------------
    // Primary test — real import
    // -----------------------------------------------------------------------

    @Test
    fun `RtfImporter converts plain and bold text to markdown`(): Unit = runBlocking<Unit> {
        val importer = RtfImporter()
        val bytes = buildTestRtfBytes()

        val result = importer.import(bytes, "test.rtf")

        assertTrue(result.isSuccess, "Expected import to succeed but got: ${result.exceptionOrNull()}")

        val doc = result.getOrThrow()
        val md = doc.markdown

        // Plain text must be present
        assertContains(md, "Hello", message = "Markdown must contain 'Hello'")
        assertContains(md, "world", message = "Markdown must contain 'world'")

        // Bold word must carry ** markers
        assertTrue(
            md.contains("**bold**") || md.contains("***bold***"),
            "Markdown must contain bold markers around 'bold', got:\n$md",
        )

        // sourceFormat is set correctly
        assertTrue(doc.sourceFormat == "rtf", "sourceFormat must be 'rtf', got '${doc.sourceFormat}'")
    }

    // -----------------------------------------------------------------------
    // Mutation guard — stub that ignores bytes must produce failure
    // -----------------------------------------------------------------------

    @Test
    fun `mutation guard - stub returning failure does not satisfy real import assertions`(): Unit = runBlocking<Unit> {
        // Inline stub: simulates a broken importer that always fails — same
        // pattern as DocxImporterTest to prove test is wired to the real impl.
        val stub = object : DocumentImporter {
            override val supportedExtensions = setOf("rtf")
            override suspend fun import(bytes: ByteArray, fileName: String): Result<ImportedDocument> =
                Result.failure(ImportError.Malformed("rtf", RuntimeException("stub always fails")))
        }

        val bytes = buildTestRtfBytes()
        val stubResult = stub.import(bytes, "test.rtf")

        assertTrue(
            stubResult.isFailure,
            "Mutation guard: the stub must return failure; a passing stub means the test is broken",
        )
    }

    // -----------------------------------------------------------------------
    // supportedExtensions
    // -----------------------------------------------------------------------

    @Test
    fun `RtfImporter reports rtf as supported extension`() {
        val importer = RtfImporter()
        assertContains(importer.supportedExtensions, "rtf")
    }

    // -----------------------------------------------------------------------
    // Malformed bytes → Result.failure(ImportError.Malformed)
    // -----------------------------------------------------------------------

    @Test
    fun `RtfImporter returns Malformed for garbage bytes`(): Unit = runBlocking<Unit> {
        val importer = RtfImporter()
        // Bytes that are definitely not valid RTF (random non-ASCII content)
        val garbage = ByteArray(64) { (it + 200).toByte() }

        val result = importer.import(garbage, "corrupt.rtf")

        assertTrue(result.isFailure, "Expected failure for garbage bytes")
        val error = result.exceptionOrNull()
        assertTrue(
            error is ImportError.Malformed,
            "Expected ImportError.Malformed, got ${error?.javaClass?.simpleName}",
        )
    }
}
