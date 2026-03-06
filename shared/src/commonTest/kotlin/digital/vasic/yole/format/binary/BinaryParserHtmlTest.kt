/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for BinaryParser HTML generation
 *
 *########################################################*/
package digital.vasic.yole.format.binary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BinaryParserHtmlTest {

    private val parser = BinaryParser()

    // ==================== Document structure ====================

    @Test
    fun testToHtmlContainsBinaryDiv() {
        val doc = parser.parse("", mapOf("filename" to "test.bin"))
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("binary-file"))
    }

    @Test
    fun testContainsStylesheet() {
        val doc = parser.parse("", mapOf("filename" to "test.bin"))
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<style>"))
    }

    @Test
    fun testBinaryHeader() {
        val doc = parser.parse("", mapOf("filename" to "test.bin"))
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("binary-header"))
    }

    // ==================== MIME type detection ====================

    @Test
    fun testJpegMimeType() {
        val doc = parser.parse("", mapOf("filename" to "photo.jpg"))
        assertEquals("image/jpeg", doc.metadata["mime_type"])
    }

    @Test
    fun testPngMimeType() {
        val doc = parser.parse("", mapOf("filename" to "icon.png"))
        assertEquals("image/png", doc.metadata["mime_type"])
    }

    @Test
    fun testPdfMimeType() {
        val doc = parser.parse("", mapOf("filename" to "doc.pdf"))
        assertEquals("application/pdf", doc.metadata["mime_type"])
    }

    @Test
    fun testMp3MimeType() {
        val doc = parser.parse("", mapOf("filename" to "song.mp3"))
        assertEquals("audio/mpeg", doc.metadata["mime_type"])
    }

    @Test
    fun testMp4MimeType() {
        val doc = parser.parse("", mapOf("filename" to "video.mp4"))
        assertEquals("video/mp4", doc.metadata["mime_type"])
    }

    @Test
    fun testZipMimeType() {
        val doc = parser.parse("", mapOf("filename" to "archive.zip"))
        assertEquals("application/zip", doc.metadata["mime_type"])
    }

    @Test
    fun testUnknownMimeType() {
        val doc = parser.parse("", mapOf("filename" to "file.xyz"))
        assertEquals("application/octet-stream", doc.metadata["mime_type"])
    }

    // ==================== File type classification ====================

    @Test
    fun testImageFileType() {
        val doc = parser.parse("", mapOf("filename" to "photo.png"))
        assertEquals("Image", doc.metadata["file_type"])
    }

    @Test
    fun testAudioFileType() {
        val doc = parser.parse("", mapOf("filename" to "track.wav"))
        assertEquals("Audio", doc.metadata["file_type"])
    }

    @Test
    fun testVideoFileType() {
        val doc = parser.parse("", mapOf("filename" to "clip.avi"))
        assertEquals("Video", doc.metadata["file_type"])
    }

    @Test
    fun testPdfFileType() {
        val doc = parser.parse("", mapOf("filename" to "doc.pdf"))
        assertEquals("PDF Document", doc.metadata["file_type"])
    }

    // ==================== Content preview ====================

    @Test
    fun testImagePreview() {
        val doc = parser.parse("", mapOf("filename" to "photo.jpg"))
        assertTrue(doc.parsedContent.contains("image-container"))
    }

    @Test
    fun testAudioPreview() {
        val doc = parser.parse("", mapOf("filename" to "song.mp3"))
        assertTrue(doc.parsedContent.contains("audio-container"))
    }

    @Test
    fun testVideoPreview() {
        val doc = parser.parse("", mapOf("filename" to "movie.mp4"))
        assertTrue(doc.parsedContent.contains("video-container"))
    }

    @Test
    fun testPdfPreview() {
        val doc = parser.parse("", mapOf("filename" to "doc.pdf"))
        assertTrue(doc.parsedContent.contains("pdf-container"))
    }

    @Test
    fun testGenericPreview() {
        val doc = parser.parse("", mapOf("filename" to "data.bin"))
        assertTrue(doc.parsedContent.contains("generic-preview"))
    }

    @Test
    fun testDownloadButton() {
        val doc = parser.parse("", mapOf("filename" to "file.zip"))
        assertTrue(doc.parsedContent.contains("download-button"))
    }

    // ==================== Metadata ====================

    @Test
    fun testMetadataFilename() {
        val doc = parser.parse("", mapOf("filename" to "test.bin"))
        assertEquals("test.bin", doc.metadata["filename"])
    }

    @Test
    fun testMetadataIsBinary() {
        val doc = parser.parse("", mapOf("filename" to "test.bin"))
        assertEquals("true", doc.metadata["is_binary"])
    }

    @Test
    fun testMetadataExtension() {
        val doc = parser.parse("", mapOf("filename" to "test.png"))
        assertEquals("png", doc.metadata["extension"])
    }

    @Test
    fun testMetadataFileSize() {
        val doc = parser.parse("", mapOf("filename" to "test.bin", "fileSize" to 1024L))
        assertEquals("1 KB", doc.metadata["file_size"])
    }

    // ==================== Light/dark mode ====================

    @Test
    fun testLightModeClass() {
        val doc = parser.parse("", mapOf("filename" to "test.bin"))
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("light"))
    }

    @Test
    fun testDarkModeClass() {
        val doc = parser.parse("", mapOf("filename" to "test.bin"))
        val html = parser.toHtml(doc, lightMode = false)
        assertTrue(html.contains("dark"))
    }

    @Test
    fun testLightAndDarkModeDiffer() {
        val doc = parser.parse("", mapOf("filename" to "test.bin"))
        val light = parser.toHtml(doc, lightMode = true)
        val dark = parser.toHtml(doc, lightMode = false)
        assertTrue(light != dark)
    }

    // ==================== Validation ====================

    @Test
    fun testValidationAlwaysEmpty() {
        val errors = parser.validate("anything")
        assertTrue(errors.isEmpty())
    }

    // ==================== canParse ====================

    @Test
    fun testCanParse() {
        assertTrue(parser.canParse(parser.supportedFormat))
    }

    // ==================== Empty content ====================

    @Test
    fun testEmptyContent() {
        val doc = parser.parse("")
        assertNotNull(doc)
    }
}
