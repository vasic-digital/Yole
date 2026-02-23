/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Document model tests
 *
 *########################################################*/
package digital.vasic.yole.model

import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNull

class DocumentTest {

    @Test
    fun testDocumentCreation() {
        val doc = Document(
            path = "/test/document.md",
            title = "document",
            extension = "md",
            format = Document.FORMAT_MARKDOWN
        )

        assertEquals("/test/document.md", doc.path)
        assertEquals("document", doc.title)
        assertEquals("md", doc.extension)
        assertEquals(Document.FORMAT_MARKDOWN, doc.format)
    }

    @Test
    fun testFilename() {
        val doc = Document(
            path = "/test/document.md",
            title = "document",
            extension = "md"
        )

        assertEquals("document.md", doc.filename)
    }

    @Test
    fun testFilenameWithoutExtension() {
        val doc = Document(
            path = "/test/document",
            title = "document",
            extension = ""
        )

        assertEquals("document", doc.filename)
    }

    @Test
    fun testEquality() {
        val doc1 = Document(
            path = "/test/document.md",
            title = "document",
            extension = "md",
            format = Document.FORMAT_MARKDOWN
        )

        val doc2 = Document(
            path = "/test/document.md",
            title = "document",
            extension = "md",
            format = Document.FORMAT_MARKDOWN
        )

        assertEquals(doc1, doc2)
    }

    @Test
    fun testInequality() {
        val doc1 = Document(
            path = "/test/document1.md",
            title = "document1",
            extension = "md"
        )

        val doc2 = Document(
            path = "/test/document2.md",
            title = "document2",
            extension = "md"
        )

        assertFalse(doc1 == doc2)
    }

    @Test
    fun testFormatConstants() {
        assertEquals("markdown", Document.FORMAT_MARKDOWN)
        assertEquals("plaintext", Document.FORMAT_PLAINTEXT)
        assertEquals("todotxt", Document.FORMAT_TODOTXT)
        assertEquals("latex", Document.FORMAT_LATEX)
        assertEquals("unknown", Document.FORMAT_UNKNOWN)
    }

    @Test
    fun testResetChangeTracking() {
        val doc = Document(
            path = "/test/document.md",
            title = "document",
            extension = "md"
        )

        doc.modTime = 123456L
        doc.touchTime = 789012L

        doc.resetChangeTracking()

        assertEquals(-1L, doc.modTime)
        assertEquals(-1L, doc.touchTime)
    }

    @Test
    fun testTouch() {
        val doc = Document(
            path = "/test/document.md",
            title = "document",
            extension = "md"
        )

        val beforeTouch = currentTimeMillis()
        doc.touch()
        val afterTouch = currentTimeMillis()

        assertTrue(doc.touchTime >= beforeTouch)
        assertTrue(doc.touchTime <= afterTouch)
    }

    @Test
    fun testGetTextFormat() {
        val doc = Document(
            path = "/test/document.md",
            title = "document",
            extension = "md",
            format = Document.FORMAT_MARKDOWN
        )

        val textFormat = doc.getTextFormat()
        assertEquals("markdown", textFormat.id)
        assertEquals("Markdown", textFormat.name)
    }

    @Test
    fun testDetectFormatByExtension() {
        val doc = Document(
            path = "/test/document.md",
            title = "document",
            extension = "md",
            format = Document.FORMAT_UNKNOWN
        )

        doc.detectFormatByExtension()
        assertEquals(Document.FORMAT_MARKDOWN, doc.format)
    }

    @Test
    fun testDetectFormatByExtensionTex() {
        val doc = Document(
            path = "/test/paper.tex",
            title = "paper",
            extension = "tex",
            format = Document.FORMAT_UNKNOWN
        )

        doc.detectFormatByExtension()
        assertEquals(Document.FORMAT_LATEX, doc.format)
    }

    @Test
    fun testDetectFormatByContent() {
        val doc = Document(
            path = "/test/document.txt",
            title = "document",
            extension = "txt",
            format = Document.FORMAT_UNKNOWN
        )

        val markdownContent = """
            # Heading 1
            ## Heading 2
            This is markdown content
        """.trimIndent()

        val detected = doc.detectFormatByContent(markdownContent)
        assertTrue(detected)
        assertEquals(Document.FORMAT_MARKDOWN, doc.format)
    }

    @Test
    fun testDetectFormatByContentNoMatch() {
        val doc = Document(
            path = "/test/document.txt",
            title = "document",
            extension = "txt",
            format = Document.FORMAT_UNKNOWN
        )

        val plainContent = "Just some plain text"

        val detected = doc.detectFormatByContent(plainContent)
        assertFalse(detected)
        assertEquals(Document.FORMAT_UNKNOWN, doc.format)
    }

    @Test
    fun testFormatConstantsDelegation() {
        // Verify that Document constants match TextFormat constants
        assertEquals("markdown", Document.FORMAT_MARKDOWN)
        assertEquals("latex", Document.FORMAT_LATEX)
        assertEquals("todotxt", Document.FORMAT_TODOTXT)
        assertEquals("unknown", Document.FORMAT_UNKNOWN)
    }

    @Test
    fun testCurrentTimeMillis() {
        val time1 = currentTimeMillis()
        val time2 = currentTimeMillis()

        // Time should be monotonically increasing
        assertTrue(time2 >= time1)

        // Time should be reasonable (after 2020)
        assertTrue(time1 > 1577836800000L) // 2020-01-01 00:00:00 UTC

        // Time should be close to current time (within 1 second)
        val currentTime = Clock.System.now().toEpochMilliseconds()
        assertTrue(time1 <= currentTime + 1000)
        assertTrue(time1 >= currentTime - 1000)
    }

    @Test
    fun testDocumentFileOperations() {
        val doc = Document(
            path = "/definitely/does/not/exist/anywhere/on/any/system/file.txt",
            title = "file",
            extension = "txt"
        )

        // Test file operations on non-existent file
        assertFalse(doc.fileExists())
        // File mod time should be -1 for non-existent files
        assertTrue(doc.getFileModTime() <= 0L)
        assertTrue(doc.getFileSize() >= 0L) // Size should be non-negative
    }

    @Test
    fun testCreateDocument() {
        // Test creating document from non-existent path
        val doc = createDocument("/nonexistent/file.txt")
        assertNull(doc)
    }

    @Test
    fun testHasChanged() {
        val doc = Document(
            path = "/test/document.md",
            title = "document",
            extension = "md"
        )

        // Initially should have changed (both times are -1, indicating untracked)
        assertTrue(doc.hasChanged())

        // Set both times to indicate tracking is active
        doc.modTime = currentTimeMillis()
        doc.touchTime = currentTimeMillis()
        // Should not have changed since file doesn't exist (getFileModTime() returns -1)
        assertFalse(doc.hasChanged())

        // Reset tracking
        doc.resetChangeTracking()
        assertTrue(doc.hasChanged())
    }
}
