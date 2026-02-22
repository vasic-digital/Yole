/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive Binary Detection Test Suite
 * Tests binary file detection, classification, and handling
 *
 *########################################################*/
package digital.vasic.yole.format.binary

import digital.vasic.yole.format.*
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * Comprehensive test suite for binary file detection and classification.
 * Tests magic numbers, file signatures, binary vs text classification,
 * various binary formats, large file handling, and edge cases.
 */
class BinaryDetectionTest {

    private lateinit var binaryParser: BinaryParser
    private lateinit var binaryFormat: TextFormat

    @BeforeTest
    fun setup() {
        binaryParser = BinaryParser()
        binaryFormat = FormatRegistry.getById(TextFormat.ID_BINARY)!!
    }

    @Test
    fun `test binary format registration`() {
        val format = FormatRegistry.getById(TextFormat.ID_BINARY)
        assertNotNull(format, "Binary format should be registered")
        assertEquals(TextFormat.ID_BINARY, format.id)
        assertEquals("Binary", format.name)
        assertEquals(".bin", format.defaultExtension)
        assertTrue(format.extensions.isEmpty(), "Binary format should have no extensions")
    }

    @Test
    fun `test basic binary parsing with filename`() {
        val content = "Binary file content"
        val options = mapOf(
            "filename" to "test.exe",
            "fileSize" to 1024L,
            "mimeType" to "application/x-executable"
        )

        val result = binaryParser.parse(content, options)

        assertNotNull(result)
        assertEquals(TextFormat.ID_BINARY, result.format.id)
        assertEquals(content, result.rawContent)
        assertEquals("application/x-executable", result.metadata["mime_type"])
        assertEquals("1 KB", result.metadata["file_size"])
        assertEquals("1024", result.metadata["file_size_bytes"])
        assertEquals("true", result.metadata["is_binary"])
        assertEquals("Document", result.metadata["file_type"])
        assertEquals("exe", result.metadata["extension"])
    }

    @Test
    fun `test binary vs text classification`() {
        // Test various binary file signatures
        val binarySignatures = listOf(
            "MZ" to "application/x-executable", // DOS/Windows executable
            "\u0000\u0000\u0000\u0000" to "application/octet-stream", // Null bytes
            "\u0089PNG" to "image/png", // PNG with high bit set
            "\u00FF\u00D8\u00FF" to "image/jpeg", // JPEG magic number
            "GIF87a" to "image/gif", // GIF signature
            "PK\u0003\u0004" to "application/zip", // ZIP signature
            "Rar!\u001A\u0007" to "application/x-rar", // RAR signature
            "%PDF" to "application/pdf" // PDF signature
        )

        binarySignatures.forEach { (signature, expectedMimeType) ->
            val content = signature + "some binary data"
            val result = binaryParser.parse(content, mapOf("filename" to "test.file"))
            
            assertNotNull(result)
            assertEquals(TextFormat.ID_BINARY, result.format.id)
            assertEquals(expectedMimeType, result.metadata["mime_type"])
            assertEquals("true", result.metadata["is_binary"])
        }
    }

    @Test
    fun `test executable file detection`() {
        val executables = listOf(
            "program.exe" to "application/x-executable",
            "app.dll" to "application/x-msdownload",
            "driver.sys" to "application/x-msdownload",
            "script.bat" to "application/x-bat",
            "binary.bin" to "application/octet-stream"
        )

        executables.forEach { (filename, expectedMimeType) ->
            val content = "MZ\u0090\u0000\u0003" // DOS executable header
            val result = binaryParser.parse(content, mapOf("filename" to filename))
            
            assertNotNull(result)
            assertEquals(TextFormat.ID_BINARY, result.format.id)
            assertEquals(expectedMimeType, result.metadata["mime_type"])
            assertEquals("Document", result.metadata["file_type"])
        }
    }

    @Test
    fun `test image format detection`() {
        val images = listOf(
            "photo.jpg" to "image/jpeg",
            "picture.jpeg" to "image/jpeg",
            "icon.png" to "image/png",
            "animation.gif" to "image/gif",
            "bitmap.bmp" to "image/bmp",
            "modern.webp" to "image/webp",
            "vector.svg" to "image/svg+xml"
        )

        images.forEach { (filename, expectedMimeType) ->
            val content = when (filename.substringAfterLast('.')) {
                "jpg", "jpeg" -> "\u00FF\u00D8\u00FF"
                "png" -> "\u0089PNG"
                "gif" -> "GIF89a"
                "bmp" -> "BM"
                "webp" -> "RIFF\u0000\u0000\u0000WEBP"
                "svg" -> "<svg></svg>"
                else -> "binary data"
            }
            
            val result = binaryParser.parse(content, mapOf("filename" to filename))
            
            assertNotNull(result)
            assertEquals(TextFormat.ID_BINARY, result.format.id)
            assertEquals(expectedMimeType, result.metadata["mime_type"])
            assertEquals("Image", result.metadata["file_type"])
        }
    }

    @Test
    fun `test audio and video format detection`() {
        val mediaFiles = listOf(
            "song.mp3" to "audio/mpeg",
            "sound.wav" to "audio/wav",
            "audio.ogg" to "audio/ogg",
            "video.mp4" to "video/mp4",
            "movie.avi" to "video/x-msvideo",
            "clip.mov" to "video/quicktime"
        )

        mediaFiles.forEach { (filename, expectedMimeType) ->
            val content = "ID3" // Common media file header
            val result = binaryParser.parse(content, mapOf("filename" to filename))
            
            assertNotNull(result)
            assertEquals(TextFormat.ID_BINARY, result.format.id)
            assertEquals(expectedMimeType, result.metadata["mime_type"])
            val expectedType = if (expectedMimeType.startsWith("audio/")) "Audio" else "Video"
            assertEquals(expectedType, result.metadata["file_type"])
        }
    }

    @Test
    fun `test archive format detection`() {
        val archives = listOf(
            "archive.zip" to "application/zip",
            "backup.tar" to "application/x-tar",
            "compressed.gz" to "application/gzip"
        )

        archives.forEach { (filename, expectedMimeType) ->
            val content = when (filename.substringAfterLast('.')) {
                "zip" -> "PK\u0003\u0004"
                "tar" -> "ustar\u0000"
                "gz" -> "\u001F\u008B"
                else -> "archive data"
            }
            
            val result = binaryParser.parse(content, mapOf("filename" to filename))
            
            assertNotNull(result)
            assertEquals(TextFormat.ID_BINARY, result.format.id)
            assertEquals(expectedMimeType, result.metadata["mime_type"])
            assertEquals("Document", result.metadata["file_type"])
        }
    }

    @Test
    fun `test PDF document detection`() {
        val content = "%PDF-1.4\n1 0 obj"
        val result = binaryParser.parse(content, mapOf("filename" to "document.pdf"))
        
        assertNotNull(result)
        assertEquals(TextFormat.ID_BINARY, result.format.id)
        assertEquals("application/pdf", result.metadata["mime_type"])
        assertEquals("PDF Document", result.metadata["file_type"])
    }

    @Test
    fun `test unknown binary format`() {
        val content = "\u0000\u0001\u0002\u0003\u0004\u0005"
        val result = binaryParser.parse(content, mapOf("filename" to "unknown.dat"))
        
        assertNotNull(result)
        assertEquals(TextFormat.ID_BINARY, result.format.id)
        assertEquals("application/octet-stream", result.metadata["mime_type"])
        assertEquals("Binary File", result.metadata["file_type"])
        assertEquals("dat", result.metadata["extension"])
    }

    @Test
    fun `test file size formatting`() {
        val sizes = listOf(
            512L to "512 B",
            1024L to "1 KB",
            1536L to "1 KB", // Should round down
            2048L to "2 KB",
            1048576L to "1 MB", // 1024 * 1024
            1073741824L to "1 GB" // 1024 * 1024 * 1024
        )

        sizes.forEach { (bytes, expectedSize) ->
            val content = "binary data"
            val result = binaryParser.parse(content, mapOf(
                "filename" to "test.bin",
                "fileSize" to bytes
            ))
            
            assertNotNull(result)
            assertEquals(expectedSize, result.metadata["file_size"])
        }
    }

    @Test
    fun `test empty file handling`() {
        val result = binaryParser.parse("", mapOf("filename" to "empty.bin"))
        
        assertNotNull(result)
        assertEquals(TextFormat.ID_BINARY, result.format.id)
        assertEquals("", result.rawContent)
        assertEquals("0 B", result.metadata["file_size"])
        assertEquals("application/octet-stream", result.metadata["mime_type"])
    }

    @Test
    fun `test corrupted binary data`() {
        val corruptedData = listOf(
            "\u0000\u0000\u0000\u0000", // All nulls
            "\u00FF\u00FF\u00FF\u00FF", // All high bits
            "\u0000\u00FF\u0000\u00FF", // Alternating patterns
            "\u0080\u0080\u0080\u0080" // High bit set
        )

        corruptedData.forEach { content ->
            val result = binaryParser.parse(content, mapOf("filename" to "corrupted.bin"))
            
            assertNotNull(result)
            assertEquals(TextFormat.ID_BINARY, result.format.id)
            assertEquals("application/octet-stream", result.metadata["mime_type"])
            assertEquals("true", result.metadata["is_binary"])
        }
    }

    @Test
    fun `test round-trip detection`() {
        val originalContent = "Binary file with magic number MZ"
        val originalOptions = mapOf(
            "filename" to "program.exe",
            "fileSize" to 2048L,
            "mimeType" to "application/x-executable"
        )

        // Parse the content
        val document = binaryParser.parse(originalContent, originalOptions)
        
        // Verify the parsed document
        assertNotNull(document)
        assertEquals(TextFormat.ID_BINARY, document.format.id)
        assertEquals(originalContent, document.rawContent)
        
        // Extract metadata for "detection"
        val detectedMimeType = document.metadata["mime_type"]
        val detectedFileType = document.metadata["file_type"]
        val detectedExtension = document.metadata["extension"]
        val isBinary = document.metadata["is_binary"]
        
        // Verify classification is correct
        assertEquals("application/x-executable", detectedMimeType)
        assertEquals("Document", detectedFileType)
        assertEquals("exe", detectedExtension)
        assertEquals("true", isBinary)
        
        // Verify the content can be converted back (round-trip)
        assertEquals(originalContent, document.rawContent)
    }

    @Test
    fun `test HTML generation for different binary types`() {
        val testCases = listOf(
            "image.jpg" to "image/jpeg",
            "audio.mp3" to "audio/mpeg", 
            "video.mp4" to "video/mp4",
            "document.pdf" to "application/pdf",
            "archive.zip" to "application/zip"
        )

        testCases.forEach { (filename, mimeType) ->
            val content = "Binary content for $filename"
            val document = binaryParser.parse(content, mapOf(
                "filename" to filename,
                "mimeType" to mimeType,
                "fileSize" to 1024L
            ))
            
            val html = binaryParser.toHtml(document, lightMode = true)
            
            assertNotNull(html)
            assertTrue(html.contains("Binary File Preview"), "HTML should contain preview title")
            assertTrue(html.contains(filename), "HTML should contain filename")
            assertTrue(html.contains(mimeType), "HTML should contain MIME type")
            assertTrue(html.contains("1 KB"), "HTML should contain formatted file size")
            
            // Test dark mode
            val darkHtml = binaryParser.toHtml(document, lightMode = false)
            assertNotNull(darkHtml)
            assertTrue(darkHtml.contains("dark"), "Dark mode HTML should contain dark theme class")
        }
    }

    @Test
    fun `test parser validation`() {
        val content = "Any binary content"
        val errors = binaryParser.validate(content)
        
        assertTrue(errors.isEmpty(), "Binary parser should not report validation errors")
    }

    @Test
    fun `test parser canParse method`() {
        assertTrue(binaryParser.canParse(binaryFormat), "Parser should handle binary format")
        
        val markdownFormat = FormatRegistry.getById(TextFormat.ID_MARKDOWN)!!
        assertFalse(binaryParser.canParse(markdownFormat), "Parser should not handle markdown format")
    }

    @Test
    fun `test large binary file handling`() {
        // Simulate large file metadata
        val largeFileSize = 100 * 1024 * 1024L // 100 MB
        val content = "Large binary file content simulation"
        
        val result = binaryParser.parse(content, mapOf(
            "filename" to "large_file.bin",
            "fileSize" to largeFileSize
        ))
        
        assertNotNull(result)
        assertEquals("100 MB", result.metadata["file_size"])
        assertEquals(TextFormat.ID_BINARY, result.format.id)
        
        // Verify HTML generation works for large files
        val html = binaryParser.toHtml(result, lightMode = true)
        assertNotNull(html)
        assertTrue(html.contains("100 MB"))
    }

    @Test
    fun `test performance benchmark for binary detection`() {
        val iterations = 1000
        val testContent = "MZ\u0090\u0000\u0003Binary executable content"
        
        val duration = measureTime {
            repeat(iterations) {
                binaryParser.parse(testContent, mapOf("filename" to "test.exe"))
            }
        }
        
        val avgTime = duration / iterations
        println("Binary detection performance: $avgTime per operation")
        
        // Should be very fast - less than 1ms per operation
        assertTrue(avgTime.inWholeMilliseconds < 1, 
            "Binary detection should be fast, took: $avgTime")
    }

    @Test
    fun `test memory efficiency with large content`() {
        val largeContent = "A".repeat(10000) // 10KB of data
        
        val result = binaryParser.parse(largeContent, mapOf("filename" to "large.bin"))
        
        assertNotNull(result)
        assertEquals(largeContent, result.rawContent)
        
        // Test that HTML generation doesn't cause memory issues
        val html = binaryParser.toHtml(result, lightMode = true)
        assertNotNull(html)
        assertTrue(html.length > 0)
    }

    @Test
    fun `test format detection priority`() {
        // Binary format should be detectable even without specific patterns
        val format = FormatRegistry.getById(TextFormat.ID_BINARY)
        assertNotNull(format)
        
        // Binary format should not be detected by content patterns (it has none)
        val content = "Some random content"
        val detectedFormat = FormatRegistry.detectByContent(content)
        assertNotEquals(TextFormat.ID_BINARY, detectedFormat?.id, 
            "Binary format should not be detected by content patterns")
    }

    @Test
    fun `test mixed content handling`() {
        // Test content that has both text and binary characteristics
        val mixedContent = "Some text\u0000\u0001\u0002Binary data"
        
        val result = binaryParser.parse(mixedContent, mapOf("filename" to "mixed.bin"))
        
        assertNotNull(result)
        assertEquals(TextFormat.ID_BINARY, result.format.id)
        assertEquals(mixedContent, result.rawContent)
        assertEquals("application/octet-stream", result.metadata["mime_type"])
    }

    @Test
    fun `test special character handling in filenames`() {
        val specialFilenames = listOf(
            "file with spaces.exe",
            "file-with-dashes.bin",
            "file_with_underscores.dat",
            "file.multiple.dots.zip",
            "UPPERCASE.JPG",
            "MixedCase.Png"
        )

        specialFilenames.forEach { filename ->
            val content = "Binary content"
            val result = binaryParser.parse(content, mapOf("filename" to filename))
            
            assertNotNull(result)
            assertEquals(TextFormat.ID_BINARY, result.format.id)
            val expectedExtension = filename.substringAfterLast('.').lowercase()
            assertEquals(expectedExtension, result.metadata["extension"])
        }
    }

    @Test
    fun `test HTML escaping in binary content`() {
        val contentWithHtml = "<script>alert('xss')</script>"
        val filename = "malicious.bin"
        
        val document = binaryParser.parse(contentWithHtml, mapOf("filename" to filename))
        val html = binaryParser.toHtml(document, lightMode = true)
        
        assertNotNull(html)
        // HTML should be escaped to prevent XSS
        assertFalse(html.contains("<script>"), "HTML should not contain unescaped script tags")
        assertTrue(html.contains("&lt;script&gt;") || html.contains("malicious.bin"), 
            "HTML should either escape scripts or safely handle filename")
    }
}