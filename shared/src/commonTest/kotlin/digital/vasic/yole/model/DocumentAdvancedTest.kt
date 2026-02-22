/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Advanced Document model tests - comprehensive coverage
 *
 *########################################################*/
package digital.vasic.yole.model

import digital.vasic.yole.format.TextFormat
import kotlin.test.*

/**
 * Advanced comprehensive tests for Document model.
 *
 * Coverage:
 * - All format constants (18 formats)
 * - Data class features (copy, destructuring, hashCode)
 * - Format detection for all supported extensions
 * - Edge cases and special scenarios
 * - Change tracking states
 */
class DocumentAdvancedTest {

    // ==================== All Format Constants Tests ====================

    @Test
    fun `should have correct markdown format constant`() {
        assertEquals("markdown", Document.FORMAT_MARKDOWN)
        assertEquals(TextFormat.ID_MARKDOWN, Document.FORMAT_MARKDOWN)
    }

    @Test
    fun `should have correct plaintext format constant`() {
        assertEquals("plaintext", Document.FORMAT_PLAINTEXT)
        assertEquals(TextFormat.ID_PLAINTEXT, Document.FORMAT_PLAINTEXT)
    }

    @Test
    fun `should have correct todotxt format constant`() {
        assertEquals("todotxt", Document.FORMAT_TODOTXT)
        assertEquals(TextFormat.ID_TODOTXT, Document.FORMAT_TODOTXT)
    }

    @Test
    fun `should have correct csv format constant`() {
        assertEquals("csv", Document.FORMAT_CSV)
        assertEquals(TextFormat.ID_CSV, Document.FORMAT_CSV)
    }

    @Test
    fun `should have correct wikitext format constant`() {
        assertEquals("wikitext", Document.FORMAT_WIKITEXT)
        assertEquals(TextFormat.ID_WIKITEXT, Document.FORMAT_WIKITEXT)
    }

    @Test
    fun `should have correct keyvalue format constant`() {
        assertEquals("keyvalue", Document.FORMAT_KEYVALUE)
        assertEquals(TextFormat.ID_KEYVALUE, Document.FORMAT_KEYVALUE)
    }

    @Test
    fun `should have correct asciidoc format constant`() {
        assertEquals("asciidoc", Document.FORMAT_ASCIIDOC)
        assertEquals(TextFormat.ID_ASCIIDOC, Document.FORMAT_ASCIIDOC)
    }

    @Test
    fun `should have correct orgmode format constant`() {
        assertEquals("orgmode", Document.FORMAT_ORGMODE)
        assertEquals(TextFormat.ID_ORGMODE, Document.FORMAT_ORGMODE)
    }

    @Test
    fun `should have correct latex format constant`() {
        assertEquals("latex", Document.FORMAT_LATEX)
        assertEquals(TextFormat.ID_LATEX, Document.FORMAT_LATEX)
    }

    @Test
    fun `should have correct restructuredtext format constant`() {
        assertEquals("restructuredtext", Document.FORMAT_RESTRUCTUREDTEXT)
        assertEquals(TextFormat.ID_RESTRUCTUREDTEXT, Document.FORMAT_RESTRUCTUREDTEXT)
    }

    @Test
    fun `should have correct taskpaper format constant`() {
        assertEquals("taskpaper", Document.FORMAT_TASKPAPER)
        assertEquals(TextFormat.ID_TASKPAPER, Document.FORMAT_TASKPAPER)
    }

    @Test
    fun `should have correct textile format constant`() {
        assertEquals("textile", Document.FORMAT_TEXTILE)
        assertEquals(TextFormat.ID_TEXTILE, Document.FORMAT_TEXTILE)
    }

    @Test
    fun `should have correct creole format constant`() {
        assertEquals("creole", Document.FORMAT_CREOLE)
        assertEquals(TextFormat.ID_CREOLE, Document.FORMAT_CREOLE)
    }

    @Test
    fun `should have correct tiddlywiki format constant`() {
        assertEquals("tiddlywiki", Document.FORMAT_TIDDLYWIKI)
        assertEquals(TextFormat.ID_TIDDLYWIKI, Document.FORMAT_TIDDLYWIKI)
    }

    @Test
    fun `should have correct jupyter format constant`() {
        assertEquals("jupyter", Document.FORMAT_JUPYTER)
        assertEquals(TextFormat.ID_JUPYTER, Document.FORMAT_JUPYTER)
    }

    @Test
    fun `should have correct rmarkdown format constant`() {
        assertEquals("rmarkdown", Document.FORMAT_RMARKDOWN)
        assertEquals(TextFormat.ID_RMARKDOWN, Document.FORMAT_RMARKDOWN)
    }

    @Test
    fun `should have correct binary format constant`() {
        assertEquals("binary", Document.FORMAT_BINARY)
        assertEquals(TextFormat.ID_BINARY, Document.FORMAT_BINARY)
    }

    @Test
    fun `should have correct unknown format constant`() {
        assertEquals("unknown", Document.FORMAT_UNKNOWN)
        assertEquals(TextFormat.ID_UNKNOWN, Document.FORMAT_UNKNOWN)
    }

    // ==================== Data Class Features Tests ====================

    @Test
    fun `should support copy with path change`() {
        val doc = Document(path = "/path/doc.md", title = "doc", extension = "md", format = Document.FORMAT_MARKDOWN)
        val copied = doc.copy(path = "/new/path/doc.md")

        assertEquals("/new/path/doc.md", copied.path)
        assertEquals("doc", copied.title)
        assertEquals("md", copied.extension)
        assertEquals(Document.FORMAT_MARKDOWN, copied.format)
    }

    @Test
    fun `should support copy with title change`() {
        val doc = Document(path = "/path/doc.md", title = "doc", extension = "md")
        val copied = doc.copy(title = "newdoc")

        assertEquals("/path/doc.md", copied.path)
        assertEquals("newdoc", copied.title)
    }

    @Test
    fun `should support copy with extension change`() {
        val doc = Document(path = "/path/doc.md", title = "doc", extension = "md")
        val copied = doc.copy(extension = "txt")

        assertEquals("txt", copied.extension)
    }

    @Test
    fun `should support copy with format change`() {
        val doc = Document(path = "/path/doc.md", title = "doc", extension = "md", format = Document.FORMAT_MARKDOWN)
        val copied = doc.copy(format = Document.FORMAT_LATEX)

        assertEquals(Document.FORMAT_LATEX, copied.format)
    }

    @Test
    fun `should support copy with no changes`() {
        val doc = Document(path = "/path/doc.md", title = "doc", extension = "md", format = Document.FORMAT_MARKDOWN)
        val copied = doc.copy()

        assertEquals(doc, copied)
        assertNotSame(doc, copied)
    }

    @Test
    fun `should generate consistent hashCode`() {
        val doc1 = Document(path = "/path/doc.md", title = "doc", extension = "md", format = Document.FORMAT_MARKDOWN)
        val doc2 = Document(path = "/path/doc.md", title = "doc", extension = "md", format = Document.FORMAT_MARKDOWN)

        assertEquals(doc1.hashCode(), doc2.hashCode())
    }

    @Test
    fun `should generate different hashCode for different documents`() {
        val doc1 = Document(path = "/path/doc1.md", title = "doc1", extension = "md")
        val doc2 = Document(path = "/path/doc2.md", title = "doc2", extension = "md")

        assertNotEquals(doc1.hashCode(), doc2.hashCode())
    }

    @Test
    fun `should support destructuring`() {
        val doc = Document(path = "/path/doc.md", title = "doc", extension = "md", format = Document.FORMAT_MARKDOWN)
        val (id, path, title, extension) = doc

        assertEquals("", id)
        assertEquals("/path/doc.md", path)
        assertEquals("doc", title)
        assertEquals("md", extension)
        assertEquals(Document.FORMAT_MARKDOWN, doc.format)
    }

    // ==================== Filename Edge Cases ====================

    @Test
    fun `should handle filename with multiple extensions`() {
        val doc = Document(path = "/path/archive.tar.gz", title = "archive.tar", extension = "gz")

        assertEquals("archive.tar.gz", doc.filename)
    }

    @Test
    fun `should handle filename with special characters`() {
        val doc = Document(path = "/path/my-file_v2.md", title = "my-file_v2", extension = "md")

        assertEquals("my-file_v2.md", doc.filename)
    }

    @Test
    fun `should handle filename with spaces`() {
        val doc = Document(path = "/path/my document.md", title = "my document", extension = "md")

        assertEquals("my document.md", doc.filename)
    }

    @Test
    fun `should handle very long filename`() {
        val longTitle = "a".repeat(200)
        val doc = Document(path = "/path/$longTitle.md", title = longTitle, extension = "md")

        assertEquals("$longTitle.md", doc.filename)
    }

    @Test
    fun `should handle filename with Unicode`() {
        val doc = Document(path = "/path/文档.md", title = "文档", extension = "md")

        assertEquals("文档.md", doc.filename)
    }

    @Test
    fun `should handle filename with emoji`() {
        val doc = Document(path = "/path/doc 🚀.md", title = "doc 🚀", extension = "md")

        assertEquals("doc 🚀.md", doc.filename)
    }

    // ==================== Format Detection Edge Cases ====================

    @Test
    fun `should detect format from csv extension`() {
        val doc = Document(path = "/path/data.csv", title = "data", extension = "csv")
        doc.detectFormatByExtension()

        assertEquals(Document.FORMAT_CSV, doc.format)
    }

    @Test
    fun `should detect format from txt extension`() {
        val doc = Document(path = "/path/notes.txt", title = "notes", extension = "txt")
        doc.detectFormatByExtension()

        assertEquals(Document.FORMAT_PLAINTEXT, doc.format)
    }

    @Test
    fun `should detect format from org extension`() {
        val doc = Document(path = "/path/tasks.org", title = "tasks", extension = "org")
        doc.detectFormatByExtension()

        assertEquals(Document.FORMAT_ORGMODE, doc.format)
    }

    @Test
    fun `should detect format from wiki extension`() {
        val doc = Document(path = "/path/page.wiki", title = "page", extension = "wiki")
        doc.detectFormatByExtension()

        assertEquals(Document.FORMAT_WIKITEXT, doc.format)
    }

    @Test
    fun `should detect format from rst extension`() {
        val doc = Document(path = "/path/README.rst", title = "README", extension = "rst")
        doc.detectFormatByExtension()

        assertEquals(Document.FORMAT_RESTRUCTUREDTEXT, doc.format)
    }

    @Test
    fun `should detect format from asciidoc extension`() {
        val doc = Document(path = "/path/doc.adoc", title = "doc", extension = "adoc")
        doc.detectFormatByExtension()

        assertEquals(Document.FORMAT_ASCIIDOC, doc.format)
    }

    @Test
    fun `should detect format from taskpaper extension`() {
        val doc = Document(path = "/path/tasks.taskpaper", title = "tasks", extension = "taskpaper")
        doc.detectFormatByExtension()

        assertEquals(Document.FORMAT_TASKPAPER, doc.format)
    }

    @Test
    fun `should detect format from uppercase extension`() {
        val doc = Document(path = "/path/DOC.MD", title = "DOC", extension = "MD")
        doc.detectFormatByExtension()

        assertEquals(Document.FORMAT_MARKDOWN, doc.format)
    }

    @Test
    fun `should detect latex content`() {
        val doc = Document(path = "/path/doc.txt", title = "doc", extension = "txt")
        val content = "\\documentclass{article}\n\\begin{document}\nTest\\end{document}"

        val detected = doc.detectFormatByContent(content)

        assertTrue(detected)
        assertEquals(Document.FORMAT_LATEX, doc.format)
    }

    @Test
    fun `should detect todotxt content`() {
        val doc = Document(path = "/path/tasks.txt", title = "tasks", extension = "txt")
        val content = "(A) Important task\nx 2024-01-01 Completed task"

        val detected = doc.detectFormatByContent(content)

        assertTrue(detected)
        assertEquals(Document.FORMAT_TODOTXT, doc.format)
    }

    @Test
    fun `should detect CSV content`() {
        val doc = Document(path = "/path/data.txt", title = "data", extension = "txt")
        val content = "Name,Age,City\nJohn,30,NYC\nJane,25,LA"

        val detected = doc.detectFormatByContent(content)

        assertTrue(detected)
        assertEquals(Document.FORMAT_CSV, doc.format)
    }

    @Test
    fun `should not detect format from plain text`() {
        val doc = Document(path = "/path/notes.txt", title = "notes", extension = "txt")
        val content = "Just some ordinary plain text without any special formatting."

        val detected = doc.detectFormatByContent(content)

        assertFalse(detected)
        assertEquals(Document.FORMAT_UNKNOWN, doc.format)
    }

    @Test
    fun `should not detect format from empty content`() {
        val doc = Document(path = "/path/empty.txt", title = "empty", extension = "txt")

        val detected = doc.detectFormatByContent("")

        assertFalse(detected)
    }

    // ==================== Change Tracking Edge Cases ====================

    @Test
    fun `should indicate changed when modTime is negative`() {
        val doc = Document(path = "/path/doc.md", title = "doc", extension = "md")
        doc.modTime = -1
        doc.touchTime = 1000

        assertTrue(doc.hasChanged())
    }

    @Test
    fun `should indicate changed when touchTime is negative`() {
        val doc = Document(path = "/path/doc.md", title = "doc", extension = "md")
        doc.modTime = 1000
        doc.touchTime = -1

        assertTrue(doc.hasChanged())
    }

    @Test
    fun `should reset both times when reset is called`() {
        val doc = Document(path = "/path/doc.md", title = "doc", extension = "md")
        doc.modTime = 5000
        doc.touchTime = 6000

        doc.resetChangeTracking()

        assertEquals(-1L, doc.modTime)
        assertEquals(-1L, doc.touchTime)
    }

    @Test
    fun `should update touchTime when touched`() {
        val doc = Document(path = "/path/doc.md", title = "doc", extension = "md")
        val before = currentTimeMillis()

        doc.touch()

        assertTrue(doc.touchTime >= before)
        assertTrue(doc.touchTime <= currentTimeMillis())
    }

    @Test
    fun `should handle multiple touches`() {
        val doc = Document(path = "/path/doc.md", title = "doc", extension = "md")

        doc.touch()
        val firstTouch = doc.touchTime

        // Small delay to ensure time difference
        doc.touch()
        val secondTouch = doc.touchTime

        assertTrue(secondTouch >= firstTouch)
    }

    // ==================== TextFormat Integration ====================

    @Test
    fun `should get correct TextFormat for markdown`() {
        val doc = Document(path = "/path/doc.md", title = "doc", extension = "md", format = Document.FORMAT_MARKDOWN)

        val format = doc.getTextFormat()

        assertEquals("markdown", format.id)
        assertEquals("Markdown", format.name)
        assertTrue(format.extensions.contains(".md"))
    }

    @Test
    fun `should get correct TextFormat for latex`() {
        val doc = Document(path = "/path/paper.tex", title = "paper", extension = "tex", format = Document.FORMAT_LATEX)

        val format = doc.getTextFormat()

        assertEquals("latex", format.id)
        assertEquals("LaTeX", format.name)
    }

    @Test
    fun `should get fallback format for invalid format ID`() {
        val doc = Document(path = "/path/doc.txt", title = "doc", extension = "txt", format = "invalid_format_id")

        val format = doc.getTextFormat()

        // Should return last format in registry as fallback
        assertNotNull(format)
    }

    // ==================== Equality Edge Cases ====================

    @Test
    fun `should be equal to itself`() {
        val doc = Document(path = "/path/doc.md", title = "doc", extension = "md")

        assertEquals(doc, doc)
    }

    @Test
    fun `should not be equal to null`() {
        val doc = Document(path = "/path/doc.md", title = "doc", extension = "md")

        assertFalse(doc.equals(null))
    }

    @Test
    fun `should not be equal to different type`() {
        val doc = Document(path = "/path/doc.md", title = "doc", extension = "md")

        assertFalse(doc.equals("string"))
    }

    @Test
    fun `should not be equal when path differs`() {
        val doc1 = Document(path = "/path1/doc.md", title = "doc", extension = "md", format = Document.FORMAT_MARKDOWN)
        val doc2 = Document(path = "/path2/doc.md", title = "doc", extension = "md", format = Document.FORMAT_MARKDOWN)

        assertNotEquals(doc1, doc2)
    }

    @Test
    fun `should not be equal when title differs`() {
        val doc1 = Document(path = "/path/doc1.md", title = "doc1", extension = "md", format = Document.FORMAT_MARKDOWN)
        val doc2 = Document(path = "/path/doc2.md", title = "doc2", extension = "md", format = Document.FORMAT_MARKDOWN)

        assertNotEquals(doc1, doc2)
    }

    @Test
    fun `should not be equal when format differs`() {
        val doc1 = Document(path = "/path/doc.md", title = "doc", extension = "md", format = Document.FORMAT_MARKDOWN)
        val doc2 = Document(path = "/path/doc.md", title = "doc", extension = "md", format = Document.FORMAT_LATEX)

        assertNotEquals(doc1, doc2)
    }

    @Test
    fun `should be equal when all key properties match`() {
        val doc1 = Document(path = "/path/doc.md", title = "doc", extension = "md", format = Document.FORMAT_MARKDOWN, modTime = 1000, touchTime = 2000)
        val doc2 = Document(path = "/path/doc.md", title = "doc", extension = "md", format = Document.FORMAT_MARKDOWN, modTime = 3000, touchTime = 4000)

        // Equality doesn't consider timestamps (they're @Transient)
        assertEquals(doc1, doc2)
    }

    // ==================== Path Edge Cases ====================

    @Test
    fun `should handle absolute Unix path`() {
        val doc = Document(path = "/home/user/documents/file.md", title = "file", extension = "md")

        assertEquals("/home/user/documents/file.md", doc.path)
    }

    @Test
    fun `should handle absolute Windows path`() {
        val doc = Document(path = "C:\\Users\\user\\Documents\\file.md", title = "file", extension = "md")

        assertEquals("C:\\Users\\user\\Documents\\file.md", doc.path)
    }

    @Test
    fun `should handle relative path`() {
        val doc = Document(path = "../documents/file.md", title = "file", extension = "md")

        assertEquals("../documents/file.md", doc.path)
    }

    @Test
    fun `should handle path with spaces`() {
        val doc = Document(path = "/path with spaces/my file.md", title = "my file", extension = "md")

        assertEquals("/path with spaces/my file.md", doc.path)
    }

    @Test
    fun `should handle very long path`() {
        val longPath = "/a".repeat(100) + "/file.md"
        val doc = Document(path = longPath, title = "file", extension = "md")

        assertEquals(longPath, doc.path)
    }
}
