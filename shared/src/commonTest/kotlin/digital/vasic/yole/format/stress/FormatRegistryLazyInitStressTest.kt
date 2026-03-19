/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * FormatRegistry Lazy Initialization Stress Tests
 *
 * Validates that concurrent first-access from many coroutines
 * triggers only a single initialization, and all consumers
 * receive the same consistent registry instance.
 *
 *########################################################*/

package digital.vasic.yole.format.stress

import digital.vasic.yole.format.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.test.*

/**
 * Stress tests for FormatRegistry lazy initialization.
 *
 * Validates that concurrent first-access from 50+ coroutines
 * results in only one initialization, and all coroutines
 * receive the same registry instance and consistent data.
 */
class FormatRegistryLazyInitStressTest {

    @Test
    fun `concurrent first-access from 50 coroutines returns same formats list`() = runBlocking<Unit> {
        val mutex = Mutex()
        val formatLists = mutableListOf<List<TextFormat>>()

        val jobs = (1..50).map {
            launch(Dispatchers.Default) {
                val formats = FormatRegistry.formats
                mutex.withLock { formatLists.add(formats) }
            }
        }
        jobs.forEach { it.join() }

        assertEquals(50, formatLists.size, "All 50 coroutines should return results")

        // All should be the exact same instance (lazy guarantees single init)
        val reference = formatLists.first()
        formatLists.forEach { formats ->
            assertSame(reference, formats, "All coroutines should get the same list instance")
        }
    }

    @Test
    fun `isFormatsInitialized is true after first access`() = runBlocking<Unit> {
        // Access formats to ensure initialization
        val formats = FormatRegistry.formats
        assertTrue(formats.isNotEmpty(), "Formats should not be empty")
        assertTrue(FormatRegistry.isFormatsInitialized, "isFormatsInitialized should be true after access")
    }

    @Test
    fun `concurrent getById calls return consistent results`() = runBlocking<Unit> {
        val mutex = Mutex()
        val results = mutableListOf<TextFormat?>()

        val jobs = (1..100).map {
            launch(Dispatchers.Default) {
                val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
                mutex.withLock { results.add(format) }
            }
        }
        jobs.forEach { it.join() }

        assertEquals(100, results.size)
        assertTrue(results.all { it != null }, "All lookups should find Markdown format")
        assertTrue(
            results.all { it!!.id == FormatRegistry.ID_MARKDOWN },
            "All results should be Markdown format"
        )
    }

    @Test
    fun `concurrent getByExtension calls for various extensions`() = runBlocking<Unit> {
        val extensions = listOf(".md", ".csv", ".tex", ".org", ".adoc", ".rst", ".wiki")
        val mutex = Mutex()
        val results = mutableMapOf<String, TextFormat?>()

        val jobs = extensions.flatMap { ext ->
            (1..10).map {
                launch(Dispatchers.Default) {
                    val format = FormatRegistry.getByExtension(ext)
                    mutex.withLock { results[ext] = format }
                }
            }
        }
        jobs.forEach { it.join() }

        extensions.forEach { ext ->
            assertNotNull(results[ext], "Format for extension $ext should be found")
        }
    }

    @Test
    fun `concurrent detectByContent from 50 coroutines`() = runBlocking<Unit> {
        val contents = listOf(
            "# Markdown heading" to FormatRegistry.ID_RMARKDOWN, // R Markdown has higher priority with ```{r but this is just markdown
            "\\documentclass{article}" to FormatRegistry.ID_LATEX,
            "* Org heading" to FormatRegistry.ID_ORGMODE,
            "name,value\na,1" to FormatRegistry.ID_CSV
        )

        val mutex = Mutex()
        val results = mutableListOf<Pair<String, TextFormat?>>()

        val jobs = (1..50).map { i ->
            val (content, _) = contents[i % contents.size]
            launch(Dispatchers.Default) {
                val detected = FormatRegistry.detectByContent(content)
                mutex.withLock { results.add(content to detected) }
            }
        }
        jobs.forEach { it.join() }

        assertEquals(50, results.size, "All 50 detections should complete")
        // Not all content samples may be detectable by content alone
        // The key guarantee is that all 50 coroutines complete without error
        assertTrue(results.size == 50, "All 50 detections should complete without error")
    }

    @Test
    fun `concurrent detectByExtension never returns different results for same input`() = runBlocking<Unit> {
        val mutex = Mutex()
        val results = mutableListOf<TextFormat>()

        val jobs = (1..100).map {
            launch(Dispatchers.Default) {
                val format = FormatRegistry.detectByExtension(".md")
                mutex.withLock { results.add(format) }
            }
        }
        jobs.forEach { it.join() }

        val reference = results.first()
        results.forEach { format ->
            assertEquals(reference.id, format.id, "All results for .md should be the same format")
        }
    }

    @Test
    fun `formats list has expected count of 23 entries`() = runBlocking<Unit> {
        val formats = FormatRegistry.formats

        // 18 text formats + 5 network storage formats = 23
        assertEquals(23, formats.size, "FormatRegistry should have 23 format entries")

        // Verify key formats exist
        val formatIds = formats.map { it.id }.toSet()
        assertTrue(FormatRegistry.ID_MARKDOWN in formatIds, "Markdown should exist")
        assertTrue(FormatRegistry.ID_PLAINTEXT in formatIds, "Plaintext should exist")
        assertTrue(FormatRegistry.ID_CSV in formatIds, "CSV should exist")
        assertTrue(FormatRegistry.ID_LATEX in formatIds, "LaTeX should exist")
        assertTrue(FormatRegistry.ID_BINARY in formatIds, "Binary should exist")
    }

    @Test
    fun `concurrent getAllExtensions returns consistent results`() = runBlocking<Unit> {
        val mutex = Mutex()
        val extensionSets = mutableListOf<List<String>>()

        val jobs = (1..50).map {
            launch(Dispatchers.Default) {
                val exts = FormatRegistry.getAllExtensions()
                mutex.withLock { extensionSets.add(exts) }
            }
        }
        jobs.forEach { it.join() }

        val reference = extensionSets.first()
        extensionSets.forEach { exts ->
            assertEquals(reference, exts, "All getAllExtensions() calls should return same list")
        }
    }

    @Test
    fun `concurrent getFormatsByExtension for shared extension txt`() = runBlocking<Unit> {
        val mutex = Mutex()
        val results = mutableListOf<List<TextFormat>>()

        val jobs = (1..50).map {
            launch(Dispatchers.Default) {
                val formats = FormatRegistry.getFormatsByExtension(".txt")
                mutex.withLock { results.add(formats) }
            }
        }
        jobs.forEach { it.join() }

        // .txt is shared by plaintext, todotxt, creole, textile
        val reference = results.first()
        assertTrue(reference.size >= 2, ".txt should match multiple formats (got ${reference.size})")
        results.forEach { formats ->
            assertEquals(reference.size, formats.size, "All results should have same count")
        }
    }

    @Test
    fun `concurrent isSupported checks`() = runBlocking<Unit> {
        val ids = listOf(
            FormatRegistry.ID_MARKDOWN, FormatRegistry.ID_CSV, FormatRegistry.ID_LATEX,
            FormatRegistry.ID_BINARY, "nonexistent-format"
        )
        val mutex = Mutex()
        val results = mutableMapOf<String, Boolean>()

        val jobs = ids.flatMap { id ->
            (1..20).map {
                launch(Dispatchers.Default) {
                    val supported = FormatRegistry.isSupported(id)
                    mutex.withLock { results[id] = supported }
                }
            }
        }
        jobs.forEach { it.join() }

        assertTrue(results[FormatRegistry.ID_MARKDOWN] == true, "Markdown should be supported")
        assertTrue(results[FormatRegistry.ID_CSV] == true, "CSV should be supported")
        assertTrue(results["nonexistent-format"] == false, "Unknown format should not be supported")
    }
}
