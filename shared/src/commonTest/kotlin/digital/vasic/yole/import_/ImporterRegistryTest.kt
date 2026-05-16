/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 1: anti-bluff tests for ImporterRegistry.
 *#######################################################*/
package digital.vasic.yole.import_

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Minimal stub importer used only in these tests.
 * Not a mock — [import] is a real, non-trivial body that
 * exercises [supportedExtensions] and returns a live [Result].
 */
private class StubImporter(vararg extensions: String) : DocumentImporter {
    override val supportedExtensions: Set<String> = extensions.toSet()
    override suspend fun import(bytes: ByteArray, fileName: String): Result<ImportedDocument> =
        Result.success(
            ImportedDocument(
                sourceFormat = supportedExtensions.first(),
                markdown = "stub: ${fileName} (${bytes.size} bytes)",
            ),
        )
}

class ImporterRegistryTest {

    @Test
    fun lookupByExactExtension_returnsImporter() {
        val docxImporter = StubImporter("docx")
        val registry = ImporterRegistry.default(listOf(docxImporter))

        val found = registry.forExtension("docx")
        assertNotNull(found, "Expected a non-null importer for 'docx'")
        assertSame(docxImporter, found)
    }

    @Test
    fun normalization_dotPrefixAndCase_resolvedCorrectly() {
        val pdfImporter = StubImporter("pdf")
        val registry = ImporterRegistry.default(listOf(pdfImporter))

        // ".PDF" must normalise to "pdf"
        val foundWithDot = registry.forExtension(".PDF")
        assertNotNull(foundWithDot, "Expected a non-null importer for '.PDF'")
        assertSame(pdfImporter, foundWithDot)

        // "PDF" (upper-case, no dot) must normalise to "pdf"
        val foundUpperCase = registry.forExtension("PDF")
        assertNotNull(foundUpperCase, "Expected a non-null importer for 'PDF'")
        assertSame(pdfImporter, foundUpperCase)

        // Verify the supported set contains the key
        assertTrue("pdf" in registry.supported())
    }

    @Test
    fun unsupportedExtension_returnsNull() {
        val registry = ImporterRegistry.default(listOf(StubImporter("docx")))

        val result = registry.forExtension("xyz")
        assertNull(result, "Expected null for an unregistered extension 'xyz'")

        val resultWithDot = registry.forExtension(".unknown")
        assertNull(resultWithDot, "Expected null for an unregistered extension '.unknown'")
    }
}
