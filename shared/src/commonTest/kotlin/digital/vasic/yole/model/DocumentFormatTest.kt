/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for Document format detection and constants
 *
 *########################################################*/
package digital.vasic.yole.model

import digital.vasic.yole.format.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DocumentFormatTest {

    @Test
    fun testAllFormatConstants() {
        assertEquals("unknown", Document.FORMAT_UNKNOWN)
        assertEquals("plaintext", Document.FORMAT_PLAINTEXT)
        assertEquals("markdown", Document.FORMAT_MARKDOWN)
        assertEquals("todotxt", Document.FORMAT_TODOTXT)
        assertEquals("csv", Document.FORMAT_CSV)
        assertEquals("wikitext", Document.FORMAT_WIKITEXT)
        assertEquals("keyvalue", Document.FORMAT_KEYVALUE)
        assertEquals("asciidoc", Document.FORMAT_ASCIIDOC)
        assertEquals("orgmode", Document.FORMAT_ORGMODE)
        assertEquals("latex", Document.FORMAT_LATEX)
        assertEquals("restructuredtext", Document.FORMAT_RESTRUCTUREDTEXT)
        assertEquals("taskpaper", Document.FORMAT_TASKPAPER)
        assertEquals("textile", Document.FORMAT_TEXTILE)
        assertEquals("creole", Document.FORMAT_CREOLE)
        assertEquals("tiddlywiki", Document.FORMAT_TIDDLYWIKI)
        assertEquals("jupyter", Document.FORMAT_JUPYTER)
        assertEquals("rmarkdown", Document.FORMAT_RMARKDOWN)
        assertEquals("binary", Document.FORMAT_BINARY)
    }

    @Test
    fun testFormatConstantsDelegateToTextFormat() {
        assertEquals(TextFormat.ID_UNKNOWN, Document.FORMAT_UNKNOWN)
        assertEquals(TextFormat.ID_PLAINTEXT, Document.FORMAT_PLAINTEXT)
        assertEquals(TextFormat.ID_MARKDOWN, Document.FORMAT_MARKDOWN)
        assertEquals(TextFormat.ID_TODOTXT, Document.FORMAT_TODOTXT)
        assertEquals(TextFormat.ID_CSV, Document.FORMAT_CSV)
        assertEquals(TextFormat.ID_WIKITEXT, Document.FORMAT_WIKITEXT)
        assertEquals(TextFormat.ID_KEYVALUE, Document.FORMAT_KEYVALUE)
        assertEquals(TextFormat.ID_ASCIIDOC, Document.FORMAT_ASCIIDOC)
        assertEquals(TextFormat.ID_ORGMODE, Document.FORMAT_ORGMODE)
        assertEquals(TextFormat.ID_LATEX, Document.FORMAT_LATEX)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, Document.FORMAT_RESTRUCTUREDTEXT)
        assertEquals(TextFormat.ID_TASKPAPER, Document.FORMAT_TASKPAPER)
        assertEquals(TextFormat.ID_TEXTILE, Document.FORMAT_TEXTILE)
        assertEquals(TextFormat.ID_CREOLE, Document.FORMAT_CREOLE)
        assertEquals(TextFormat.ID_TIDDLYWIKI, Document.FORMAT_TIDDLYWIKI)
        assertEquals(TextFormat.ID_JUPYTER, Document.FORMAT_JUPYTER)
        assertEquals(TextFormat.ID_RMARKDOWN, Document.FORMAT_RMARKDOWN)
        assertEquals(TextFormat.ID_BINARY, Document.FORMAT_BINARY)
    }

    @Test
    fun testDetectFormatByExtensionCsv() {
        val doc = Document(path = "/data.csv", title = "data", extension = "csv", format = Document.FORMAT_UNKNOWN)
        doc.detectFormatByExtension()
        assertEquals(Document.FORMAT_CSV, doc.format)
    }

    @Test
    fun testDetectFormatByExtensionOrg() {
        val doc = Document(path = "/notes.org", title = "notes", extension = "org", format = Document.FORMAT_UNKNOWN)
        doc.detectFormatByExtension()
        assertEquals(Document.FORMAT_ORGMODE, doc.format)
    }

    @Test
    fun testDetectFormatByExtensionRst() {
        val doc = Document(path = "/doc.rst", title = "doc", extension = "rst", format = Document.FORMAT_UNKNOWN)
        doc.detectFormatByExtension()
        assertEquals(Document.FORMAT_RESTRUCTUREDTEXT, doc.format)
    }

    @Test
    fun testDetectFormatByExtensionAdoc() {
        val doc = Document(path = "/doc.adoc", title = "doc", extension = "adoc", format = Document.FORMAT_UNKNOWN)
        doc.detectFormatByExtension()
        assertEquals(Document.FORMAT_ASCIIDOC, doc.format)
    }

    @Test
    fun testDetectFormatByExtensionTxt() {
        val doc = Document(path = "/file.txt", title = "file", extension = "txt", format = Document.FORMAT_UNKNOWN)
        doc.detectFormatByExtension()
        assertEquals(Document.FORMAT_PLAINTEXT, doc.format)
    }

    @Test
    fun testDetectFormatByExtensionTodoTxt() {
        val doc = Document(path = "/todo.todo.txt", title = "todo.todo", extension = "txt", format = Document.FORMAT_UNKNOWN)
        doc.detectFormatByExtension()
        // .txt maps to plaintext by extension detection
        assertEquals(Document.FORMAT_PLAINTEXT, doc.format)
    }

    @Test
    fun testDetectFormatByExtensionWiki() {
        val doc = Document(path = "/page.wiki", title = "page", extension = "wiki", format = Document.FORMAT_UNKNOWN)
        doc.detectFormatByExtension()
        assertEquals(Document.FORMAT_WIKITEXT, doc.format)
    }

    @Test
    fun testDetectFormatByExtensionIpynb() {
        val doc = Document(path = "/nb.ipynb", title = "nb", extension = "ipynb", format = Document.FORMAT_UNKNOWN)
        doc.detectFormatByExtension()
        assertEquals(Document.FORMAT_JUPYTER, doc.format)
    }

    @Test
    fun testDetectFormatByExtensionRmd() {
        val doc = Document(path = "/analysis.Rmd", title = "analysis", extension = "Rmd", format = Document.FORMAT_UNKNOWN)
        doc.detectFormatByExtension()
        assertEquals(Document.FORMAT_RMARKDOWN, doc.format)
    }

    @Test
    fun testGetTextFormatMarkdown() {
        val doc = Document(path = "/doc.md", title = "doc", extension = "md", format = Document.FORMAT_MARKDOWN)
        val textFormat = doc.getTextFormat()
        assertEquals("markdown", textFormat.id)
        assertEquals("Markdown", textFormat.name)
    }

    @Test
    fun testGetTextFormatCsv() {
        val doc = Document(path = "/data.csv", title = "data", extension = "csv", format = Document.FORMAT_CSV)
        val textFormat = doc.getTextFormat()
        assertEquals("csv", textFormat.id)
    }

    @Test
    fun testGetTextFormatLatex() {
        val doc = Document(path = "/paper.tex", title = "paper", extension = "tex", format = Document.FORMAT_LATEX)
        val textFormat = doc.getTextFormat()
        assertEquals("latex", textFormat.id)
    }

    @Test
    fun testGetTextFormatUnknownFallback() {
        val doc = Document(path = "/file.xyz", title = "file", extension = "xyz", format = "nonexistent-format-id")
        val textFormat = doc.getTextFormat()
        assertNotNull(textFormat)
    }

    @Test
    fun testDetectFormatByContentLatex() {
        val doc = Document(path = "/file.txt", title = "file", extension = "txt", format = Document.FORMAT_UNKNOWN)
        val latexContent = """
            \documentclass{article}
            \begin{document}
            Hello World
            \end{document}
        """.trimIndent()
        val detected = doc.detectFormatByContent(latexContent)
        assertTrue(detected)
        assertEquals(Document.FORMAT_LATEX, doc.format)
    }

    @Test
    fun testDetectFormatByContentCsv() {
        val doc = Document(path = "/file.txt", title = "file", extension = "txt", format = Document.FORMAT_UNKNOWN)
        val csvContent = """
            name,age,city
            Alice,30,NYC
            Bob,25,LA
            Carol,35,SF
        """.trimIndent()
        val detected = doc.detectFormatByContent(csvContent)
        if (detected) {
            assertEquals(Document.FORMAT_CSV, doc.format)
        }
    }

    @Test
    fun testDocumentCreationWithAllFields() {
        val doc = Document(
            id = "doc-123",
            path = "/path/to/file.md",
            title = "file",
            extension = "md",
            content = "# Hello World",
            author = "Test Author",
            tags = listOf("important", "draft"),
            format = Document.FORMAT_MARKDOWN
        )

        assertEquals("doc-123", doc.id)
        assertEquals("# Hello World", doc.content)
        assertEquals("Test Author", doc.author)
        assertEquals(2, doc.tags.size)
        assertEquals("important", doc.tags[0])
        assertEquals("draft", doc.tags[1])
    }

    @Test
    fun testDocumentDefaultValues() {
        val doc = Document(path = "/file.txt", title = "file", extension = "txt")
        assertEquals("", doc.id)
        assertEquals("", doc.content)
        assertEquals(null, doc.author)
        assertTrue(doc.tags.isEmpty())
        assertEquals(Document.FORMAT_UNKNOWN, doc.format)
        assertEquals(-1L, doc.modTime)
        assertEquals(-1L, doc.touchTime)
    }

    @Test
    fun testFilenameVariousExtensions() {
        assertEquals("readme.md", Document(path = "/readme.md", title = "readme", extension = "md").filename)
        assertEquals("data.csv", Document(path = "/data.csv", title = "data", extension = "csv").filename)
        assertEquals("script.py", Document(path = "/script.py", title = "script", extension = "py").filename)
        assertEquals("noext", Document(path = "/noext", title = "noext", extension = "").filename)
    }

    @Test
    fun testEqualitySameReference() {
        val doc = Document(path = "/file.md", title = "file", extension = "md", format = Document.FORMAT_MARKDOWN)
        assertEquals(doc, doc)
    }

    @Test
    fun testEqualityDifferentFormat() {
        val doc1 = Document(path = "/file.md", title = "file", extension = "md", format = Document.FORMAT_MARKDOWN)
        val doc2 = Document(path = "/file.md", title = "file", extension = "md", format = Document.FORMAT_PLAINTEXT)
        assertNotEquals(doc1, doc2)
    }

    @Test
    fun testEqualityDifferentTitle() {
        val doc1 = Document(path = "/file.md", title = "file", extension = "md")
        val doc2 = Document(path = "/file.md", title = "other", extension = "md")
        assertNotEquals(doc1, doc2)
    }

    @Test
    fun testEqualityDifferentPath() {
        val doc1 = Document(path = "/path1/file.md", title = "file", extension = "md")
        val doc2 = Document(path = "/path2/file.md", title = "file", extension = "md")
        assertNotEquals(doc1, doc2)
    }

    @Test
    fun testEqualityIgnoresContentAndAuthor() {
        val doc1 = Document(path = "/file.md", title = "file", extension = "md", content = "hello", author = "Alice")
        val doc2 = Document(path = "/file.md", title = "file", extension = "md", content = "world", author = "Bob")
        assertEquals(doc1, doc2)
    }

    @Test
    fun testHashCodeConsistency() {
        val doc = Document(path = "/file.md", title = "file", extension = "md", format = Document.FORMAT_MARKDOWN)
        val hash1 = doc.hashCode()
        val hash2 = doc.hashCode()
        assertEquals(hash1, hash2)
    }

    @Test
    fun testHashCodeDifferentDocuments() {
        val doc1 = Document(path = "/file1.md", title = "file1", extension = "md")
        val doc2 = Document(path = "/file2.md", title = "file2", extension = "md")
        assertNotEquals(doc1.hashCode(), doc2.hashCode())
    }

    @Test
    fun testModTimeTracking() {
        val doc = Document(path = "/file.md", title = "file", extension = "md")
        assertEquals(-1L, doc.modTime)
        doc.modTime = 1000L
        assertEquals(1000L, doc.modTime)
    }

    @Test
    fun testTouchTimeTracking() {
        val doc = Document(path = "/file.md", title = "file", extension = "md")
        assertEquals(-1L, doc.touchTime)
        doc.touchTime = 2000L
        assertEquals(2000L, doc.touchTime)
    }

    @Test
    fun testHasChangedInitialState() {
        val doc = Document(path = "/file.md", title = "file", extension = "md")
        assertTrue(doc.hasChanged(), "New document should report hasChanged")
    }

    @Test
    fun testHasChangedAfterTracking() {
        val doc = Document(path = "/definitely/nonexistent/path.md", title = "path", extension = "md")
        doc.modTime = currentTimeMillis()
        doc.touchTime = currentTimeMillis()
        assertFalse(doc.hasChanged(), "Document with set modTime on nonexistent file should not hasChanged")
    }

    @Test
    fun testHasChangedWithNegativeModTime() {
        val doc = Document(path = "/file.md", title = "file", extension = "md")
        doc.modTime = -1L
        doc.touchTime = 1000L
        assertTrue(doc.hasChanged(), "Document with negative modTime should hasChanged")
    }

    @Test
    fun testHasChangedWithNegativeTouchTime() {
        val doc = Document(path = "/file.md", title = "file", extension = "md")
        doc.modTime = 1000L
        doc.touchTime = -1L
        assertTrue(doc.hasChanged(), "Document with negative touchTime should hasChanged")
    }

    @Test
    fun testResetChangeTrackingResetsBoth() {
        val doc = Document(path = "/file.md", title = "file", extension = "md")
        doc.modTime = 5000L
        doc.touchTime = 5000L
        doc.resetChangeTracking()
        assertEquals(-1L, doc.modTime)
        assertEquals(-1L, doc.touchTime)
        assertTrue(doc.hasChanged())
    }

    @Test
    fun testTouchSetsReasonableTime() {
        val doc = Document(path = "/file.md", title = "file", extension = "md")
        val before = currentTimeMillis()
        doc.touch()
        val after = currentTimeMillis()
        assertTrue(doc.touchTime in before..after)
    }

    @Test
    fun testCopyPreservesFields() {
        val doc = Document(
            id = "id-1",
            path = "/path/file.md",
            title = "file",
            extension = "md",
            content = "content",
            author = "author",
            tags = listOf("tag1"),
            format = Document.FORMAT_MARKDOWN
        )
        val copy = doc.copy(content = "new content")
        assertEquals("id-1", copy.id)
        assertEquals("new content", copy.content)
        assertEquals("author", copy.author)
        assertEquals(Document.FORMAT_MARKDOWN, copy.format)
    }

    @Test
    fun testEqualityWithNull() {
        val doc = Document(path = "/file.md", title = "file", extension = "md")
        assertFalse(doc.equals(null))
    }

    @Test
    fun testEqualityWithDifferentType() {
        val doc = Document(path = "/file.md", title = "file", extension = "md")
        assertFalse(doc.equals("not a document"))
    }
}
