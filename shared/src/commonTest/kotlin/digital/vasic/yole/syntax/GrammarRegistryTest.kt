/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 8: tests for GrammarRegistry.
 *   - Filename → grammar detection (case-insensitive).
 *   - EnabledFormatGate respected: disabled grammar returns null.
 *   - Unknown extension → "plaintext".
 *
 * Anti-bluff (CONST-035): if detectByFilename were stubbed to always
 * return GrammarMetadata.markdown, the disabled-gate test FAILS. If
 * detectLangId were stubbed to return "markdown", the unknown-extension
 * test FAILS.
 *
 *########################################################*/
package digital.vasic.yole.syntax

import digital.vasic.yole.syntax.grammar.GrammarRegistry
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GrammarRegistryTest {

    @BeforeTest
    fun enableMarkdown() {
        EnabledFormatGate.setEnabled(setOf("markdown"))
    }

    @AfterTest
    fun restoreDefault() {
        EnabledFormatGate.setEnabled(setOf("markdown"))
    }

    @Test
    fun detectsMarkdownByMdExtension() {
        val g = GrammarRegistry.detectByFilename("README.md")
        assertNotNull(g)
        assertEquals("markdown", g.id)
    }

    @Test
    fun detectsMarkdownByMarkdownExtension() {
        val g = GrammarRegistry.detectByFilename("notes.markdown")
        assertNotNull(g)
        assertEquals("markdown", g.id)
    }

    @Test
    fun detectsMarkdownByMdownExtension() {
        val g = GrammarRegistry.detectByFilename("draft.mdown")
        assertNotNull(g)
        assertEquals("markdown", g.id)
    }

    @Test
    fun detectsMarkdownByMkdExtension() {
        val g = GrammarRegistry.detectByFilename("legacy.mkd")
        assertNotNull(g)
        assertEquals("markdown", g.id)
    }

    @Test
    fun caseInsensitiveExtensionMatch() {
        assertNotNull(GrammarRegistry.detectByFilename("README.MD"))
        assertNotNull(GrammarRegistry.detectByFilename("Notes.Markdown"))
    }

    @Test
    fun disabledMarkdownReturnsNull() {
        EnabledFormatGate.setEnabled(emptySet())
        assertNull(GrammarRegistry.detectByFilename("README.md"))
    }

    @Test
    fun unknownExtensionReturnsNull() {
        assertNull(GrammarRegistry.detectByFilename("data.bin"))
        assertNull(GrammarRegistry.detectByFilename("script.py")) // no Python grammar in v1
    }

    @Test
    fun detectLangId_markdownForMdFile() {
        assertEquals("markdown", GrammarRegistry.detectLangId("README.md"))
    }

    @Test
    fun detectLangId_plaintextForUnknown() {
        assertEquals("plaintext", GrammarRegistry.detectLangId("data.bin"))
        assertEquals("plaintext", GrammarRegistry.detectLangId("script.py"))
    }

    @Test
    fun detectLangId_plaintextWhenDisabled() {
        EnabledFormatGate.setEnabled(emptySet())
        assertEquals("plaintext", GrammarRegistry.detectLangId("README.md"))
    }
}
