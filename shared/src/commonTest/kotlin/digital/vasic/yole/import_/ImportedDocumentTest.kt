/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 1: anti-bluff tests for ImportedDocument + ImportWarning.
 *#######################################################*/
package digital.vasic.yole.import_

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImportedDocumentTest {

    @Test
    fun roundTrip_allFieldsPreserved() {
        val warning = ImportWarning(
            severity = Severity.Warning,
            message = "Image could not be extracted",
            pageOrSection = "page 3",
        )
        val doc = ImportedDocument(
            sourceFormat = "docx",
            markdown = "# Hello\n\nWorld",
            assetsDir = "/tmp/assets",
            warnings = listOf(warning),
        )

        assertEquals("docx", doc.sourceFormat)
        assertEquals("# Hello\n\nWorld", doc.markdown)
        assertEquals("/tmp/assets", doc.assetsDir)
        assertEquals(1, doc.warnings.size)
        assertEquals(Severity.Warning, doc.warnings[0].severity)
        assertEquals("Image could not be extracted", doc.warnings[0].message)
        assertEquals("page 3", doc.warnings[0].pageOrSection)
    }

    @Test
    fun warningList_mixedSeverities_preserved() {
        val info = ImportWarning(Severity.Info, "Skipped element: drawing")
        val warn = ImportWarning(Severity.Warning, "Heading level capped at H6", pageOrSection = "section 7")

        val doc = ImportedDocument(
            sourceFormat = "odt",
            markdown = "body",
            warnings = listOf(info, warn),
        )

        assertEquals(2, doc.warnings.size)
        assertEquals(Severity.Info, doc.warnings[0].severity)
        assertNull(doc.warnings[0].pageOrSection)
        assertEquals(Severity.Warning, doc.warnings[1].severity)
        assertEquals("section 7", doc.warnings[1].pageOrSection)
        // Severity enum must contain exactly 2 cases
        val severities = Severity.values().toSet()
        assertTrue(Severity.Info in severities)
        assertTrue(Severity.Warning in severities)
        assertEquals(2, severities.size)
    }
}
