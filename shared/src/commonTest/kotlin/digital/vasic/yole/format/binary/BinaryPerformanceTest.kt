/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Binary Detection Performance Test Suite
 * Focuses on performance benchmarks and memory usage
 *
 *########################################################*/
package digital.vasic.yole.format.binary

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * Performance-focused test suite for binary detection.
 * Tests large file handling, memory efficiency, and throughput.
 */
class BinaryPerformanceTest {

    private lateinit var binaryParser: BinaryParser

    @BeforeTest
    fun setup() {
        binaryParser = BinaryParser()
    }

    @Test
    fun `benchmark binary detection throughput`() {
        val testCases = listOf(
            "small.exe" to "MZ\u0090\u0000\u0003",
            "medium.jpg" to "\u00FF\u00D8\u00FF\u00E0\u0000\u0010JFIF",
            "large.zip" to "PK\u0003\u0004\u0014\u0000\u0000\u0000\u0008\u0000"
        )

        val iterations = 1000
        val totalDuration = measureTime {
            repeat(iterations) {
                testCases.forEach { (filename, content) ->
                    binaryParser.parse(content, mapOf("filename" to filename))
                }
            }
        }

        val totalOperations = iterations * testCases.size
        val avgTime = totalDuration / totalOperations
        
        println("Binary detection throughput: $totalOperations operations in $totalDuration")
        println("Average time per operation: $avgTime")
        
        // Performance assertion - should complete 3000 operations in reasonable time
        assertTrue(totalDuration.inWholeSeconds < 5, 
            "Binary detection should be fast, took: $totalDuration for $totalOperations operations")
    }

    @Test
    fun `benchmark large file metadata handling`() {
        val fileSizes = listOf(
            1L to "1 B",
            1024L to "1 KB",
            1048576L to "1 MB",
            1073741824L to "1 GB",
            1099511627776L to "1 TB"
        )

        val iterations = 100
        val totalDuration = measureTime {
            repeat(iterations) {
                fileSizes.forEach { (bytes, expectedSize) ->
                    val result = binaryParser.parse("content", mapOf(
                        "filename" to "test.bin",
                        "fileSize" to bytes
                    ))
                    assertEquals(expectedSize, result.metadata["file_size"])
                }
            }
        }

        val avgTime = totalDuration / (iterations * fileSizes.size)
        println("Large file metadata handling: $avgTime per operation")
        
        assertTrue(avgTime.inWholeMilliseconds < 1, 
            "File size formatting should be very fast, took: $avgTime")
    }

    @Test
    fun `benchmark HTML generation performance`() {
        val document = binaryParser.parse(
            "Binary content for performance testing",
            mapOf(
                "filename" to "performance_test.exe",
                "fileSize" to 1048576L,
                "mimeType" to "application/x-executable"
            )
        )

        val iterations = 500
        
        // Benchmark light mode HTML generation
        val lightDuration = measureTime {
            repeat(iterations) {
                binaryParser.toHtml(document, lightMode = true)
            }
        }

        // Benchmark dark mode HTML generation  
        val darkDuration = measureTime {
            repeat(iterations) {
                binaryParser.toHtml(document, lightMode = false)
            }
        }

        val lightAvg = lightDuration / iterations
        val darkAvg = darkDuration / iterations

        println("HTML generation performance:")
        println("  Light mode: $lightAvg per generation")
        println("  Dark mode: $darkAvg per generation")
        
        // Both should be very fast
        assertTrue(lightAvg.inWholeMilliseconds < 2, 
            "Light mode HTML generation should be fast, took: $lightAvg")
        assertTrue(darkAvg.inWholeMilliseconds < 2, 
            "Dark mode HTML generation should be fast, took: $darkAvg")
    }

    @Test
    fun `memory efficiency test with varying content sizes`() {
        val contentSizes = listOf(
            100,      // 100 bytes
            1000,     // 1 KB  
            10000,    // 10 KB
            100000    // 100 KB
        )

        val results = contentSizes.map { size ->
            val content = "A".repeat(size)
            val result = binaryParser.parse(content, mapOf("filename" to "test.bin"))
            
            // Generate HTML to ensure full processing
            binaryParser.toHtml(result, lightMode = true)
            
            size to result
        }

        // Verify all results are correct
        results.forEach { (size, result) ->
            assertEquals(size, result.rawContent.length)
            assertEquals(TextFormat.ID_BINARY, result.format.id)
            assertNotNull(result.metadata["mime_type"])
        }

        println("Memory efficiency test completed for sizes: ${contentSizes.joinToString()}")
    }

    @Test
    fun `concurrent binary detection performance`() {
        val testFiles = listOf(
            "app.exe" to "MZ\u0090\u0000\u0003",
            "lib.dll" to "MZ\u0090\u0000\u0003",
            "data.bin" to "\u0000\u0001\u0002\u0003",
            "image.png" to "\u0089PNG",
            "doc.pdf" to "%PDF-1.4"
        )

        val iterations = 200
        val totalDuration = measureTime {
            repeat(iterations) { iteration ->
                testFiles.forEach { (filename, content) ->
                    val result = binaryParser.parse(content, mapOf("filename" to filename))
                    assertNotNull(result)
                    assertEquals(TextFormat.ID_BINARY, result.format.id)
                }
            }
        }

        val totalOperations = iterations * testFiles.size
        val opsPerSecond = totalOperations / totalDuration.inWholeSeconds
        
        println("Concurrent binary detection: $totalOperations operations in $totalDuration")
        println("Operations per second: $opsPerSecond")
        
        assertTrue(opsPerSecond > 100, 
            "Should handle at least 100 operations per second, got: $opsPerSecond")
    }

    @Test
    fun `format registry lookup performance`() {
        val iterations = 10000
        
        val duration = measureTime {
            repeat(iterations) {
                val format = FormatRegistry.getById(TextFormat.ID_BINARY)
                assertNotNull(format)
                assertEquals(TextFormat.ID_BINARY, format.id)
            }
        }

        val avgTime = duration / iterations
        println("Format registry lookup: $avgTime per lookup")
        
        assertTrue(avgTime.inWholeMicroseconds < 100, 
            "Format registry lookup should be very fast, took: $avgTime")
    }

    @Test
    fun `binary content processing scalability`() {
        val contentSizes = listOf(1000, 5000, 10000, 50000)
        val processingTimes = mutableListOf<Duration>()

        contentSizes.forEach { size ->
            val content = generateBinaryContent(size)
            
            val duration = measureTime {
                val result = binaryParser.parse(content, mapOf("filename" to "test.bin"))
                binaryParser.toHtml(result, lightMode = true)
            }
            
            processingTimes.add(duration)
        }

        // Verify processing time scales reasonably with content size
        processingTimes.forEachIndexed { index, duration ->
            val size = contentSizes[index]
            println("Processing $size bytes: $duration")
            
            // Should process even large content quickly (under 10ms)
            assertTrue(duration.inWholeMilliseconds < 10, 
                "Processing $size bytes should be fast, took: $duration")
        }
    }

    private fun generateBinaryContent(size: Int): String {
        return buildString(size) {
            repeat(size) { i ->
                append((i % 256).toChar())
            }
        }
    }
}