/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * StyleSheets Synchronization Regression Tests
 *
 * Regression tests for Phase 1 concurrency fix #13:
 * platformSynchronized safety in StyleSheets object.
 *
 * Verifies that getStyleSheet returns non-empty CSS for
 * each known format, that concurrent access is safe,
 * that clearCache followed by getStyleSheet regenerates
 * correctly, and that concurrent clearCache + getStyleSheet
 * does not crash.
 *
 *########################################################*/

package digital.vasic.yole.concurrency

import digital.vasic.yole.format.StyleSheets
import digital.vasic.yole.format.TextFormat
import kotlinx.coroutines.*
import kotlin.test.*

/**
 * Regression tests for platformSynchronized safety in StyleSheets.
 *
 * Phase 1 fix #13 ensured StyleSheets uses platformSynchronized
 * for thread-safe access to the styleSheetCache. These tests verify:
 * - getStyleSheet returns non-empty CSS for known formats
 * - Concurrent getStyleSheet calls (50 coroutines) all return valid CSS
 * - clearCache followed by getStyleSheet regenerates correctly
 * - Concurrent clearCache + getStyleSheet does not crash
 * - cacheSize returns correct values
 */
class StyleSheetsSynchronizationTests {

    @BeforeTest
    fun setup() {
        // Start each test with a clean cache
        StyleSheets.clearCache()
    }

    // ==================== getStyleSheet Returns Non-Empty CSS ====================

    @Test
    fun `getStyleSheet returns non-empty CSS for markdown light`() {
        val css = StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = true)
        assertTrue(css.isNotEmpty(), "Markdown stylesheet should not be empty")
    }

    @Test
    fun `getStyleSheet returns non-empty CSS for markdown dark`() {
        val css = StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = false)
        assertTrue(css.isNotEmpty(), "Markdown dark stylesheet should not be empty")
    }

    @Test
    fun `getStyleSheet returns non-empty CSS for wikitext`() {
        val css = StyleSheets.getStyleSheet(TextFormat.ID_WIKITEXT, lightMode = true)
        assertTrue(css.isNotEmpty(), "Wikitext stylesheet should not be empty")
    }

    @Test
    fun `getStyleSheet returns non-empty CSS for restructuredtext light`() {
        val css = StyleSheets.getStyleSheet(TextFormat.ID_RESTRUCTUREDTEXT, lightMode = true)
        assertTrue(css.isNotEmpty(), "RST light stylesheet should not be empty")
    }

    @Test
    fun `getStyleSheet returns non-empty CSS for restructuredtext dark`() {
        val css = StyleSheets.getStyleSheet(TextFormat.ID_RESTRUCTUREDTEXT, lightMode = false)
        assertTrue(css.isNotEmpty(), "RST dark stylesheet should not be empty")
    }

    @Test
    fun `getStyleSheet returns non-empty CSS for orgmode light`() {
        val css = StyleSheets.getStyleSheet(TextFormat.ID_ORGMODE, lightMode = true)
        assertTrue(css.isNotEmpty(), "OrgMode light stylesheet should not be empty")
    }

    @Test
    fun `getStyleSheet returns non-empty CSS for orgmode dark`() {
        val css = StyleSheets.getStyleSheet(TextFormat.ID_ORGMODE, lightMode = false)
        assertTrue(css.isNotEmpty(), "OrgMode dark stylesheet should not be empty")
    }

    @Test
    fun `getStyleSheet returns non-empty CSS for asciidoc light`() {
        val css = StyleSheets.getStyleSheet(TextFormat.ID_ASCIIDOC, lightMode = true)
        assertTrue(css.isNotEmpty(), "AsciiDoc light stylesheet should not be empty")
    }

    @Test
    fun `getStyleSheet returns non-empty CSS for asciidoc dark`() {
        val css = StyleSheets.getStyleSheet(TextFormat.ID_ASCIIDOC, lightMode = false)
        assertTrue(css.isNotEmpty(), "AsciiDoc dark stylesheet should not be empty")
    }

    @Test
    fun `getStyleSheet returns non-empty CSS for latex light`() {
        val css = StyleSheets.getStyleSheet(TextFormat.ID_LATEX, lightMode = true)
        assertTrue(css.isNotEmpty(), "LaTeX light stylesheet should not be empty")
    }

    @Test
    fun `getStyleSheet returns non-empty CSS for latex dark`() {
        val css = StyleSheets.getStyleSheet(TextFormat.ID_LATEX, lightMode = false)
        assertTrue(css.isNotEmpty(), "LaTeX dark stylesheet should not be empty")
    }

    @Test
    fun `getStyleSheet returns empty string for unknown format`() {
        val css = StyleSheets.getStyleSheet("unknown-format-id", lightMode = true)
        assertEquals("", css, "Unknown format should return empty string")
    }

    // ==================== Concurrent getStyleSheet Calls ====================

    @Test
    fun `Concurrent getStyleSheet calls - 50 coroutines all return valid CSS`() = runBlocking<Unit> {
        val jobs = (1..50).map { i ->
            async {
                val lightMode = i % 2 == 0
                val formatId = when (i % 6) {
                    0 -> TextFormat.ID_MARKDOWN
                    1 -> TextFormat.ID_WIKITEXT
                    2 -> TextFormat.ID_RESTRUCTUREDTEXT
                    3 -> TextFormat.ID_ORGMODE
                    4 -> TextFormat.ID_ASCIIDOC
                    5 -> TextFormat.ID_LATEX
                    else -> TextFormat.ID_MARKDOWN
                }
                val css = StyleSheets.getStyleSheet(formatId, lightMode)
                // All known formats should return non-empty CSS
                assertTrue(css.isNotEmpty(), "Format $formatId ($lightMode) should return non-empty CSS")
                css
            }
        }

        val results = jobs.awaitAll()
        assertEquals(50, results.size, "All 50 coroutines should complete")
        results.forEach { css ->
            assertTrue(css.isNotEmpty(), "Each coroutine should have received non-empty CSS")
        }
    }

    @Test
    fun `Concurrent getStyleSheet calls return identical results for same format`() = runBlocking<Unit> {
        val jobs = (1..50).map {
            async {
                StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = true)
            }
        }

        val results = jobs.awaitAll()
        // All 50 calls should return the exact same string reference or content
        val firstResult = results.first()
        results.forEach { css ->
            assertEquals(firstResult, css, "All concurrent calls should return the same CSS")
        }
    }

    // ==================== clearCache + getStyleSheet ====================

    @Test
    fun `clearCache followed by getStyleSheet regenerates correctly`() {
        // First: populate cache
        val cssBefore = StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = true)
        assertTrue(cssBefore.isNotEmpty(), "CSS should be populated")
        assertTrue(StyleSheets.cacheSize > 0, "Cache should have entries after getStyleSheet")

        // Clear
        StyleSheets.clearCache()
        assertEquals(0, StyleSheets.cacheSize, "Cache should be empty after clearCache")

        // Regenerate
        val cssAfter = StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = true)
        assertTrue(cssAfter.isNotEmpty(), "CSS should regenerate after cache clear")
        assertEquals(cssBefore, cssAfter, "Regenerated CSS should be identical")
        assertTrue(StyleSheets.cacheSize > 0, "Cache should have entries after regeneration")
    }

    @Test
    fun `clearCache resets cacheSize to 0`() {
        // Populate
        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = true)
        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = false)
        StyleSheets.getStyleSheet(TextFormat.ID_WIKITEXT, lightMode = true)

        assertTrue(StyleSheets.cacheSize >= 3, "Cache should have at least 3 entries")

        // Clear
        StyleSheets.clearCache()
        assertEquals(0, StyleSheets.cacheSize, "Cache should be empty after clearCache")
    }

    // ==================== Concurrent clearCache + getStyleSheet ====================

    @Test
    fun `Concurrent clearCache and getStyleSheet does not crash`() = runBlocking<Unit> {
        // Mix of clear and get operations
        val jobs = (1..50).map { i ->
            async {
                if (i % 5 == 0) {
                    StyleSheets.clearCache()
                    "cleared"
                } else {
                    val formatId = when (i % 4) {
                        0 -> TextFormat.ID_MARKDOWN
                        1 -> TextFormat.ID_WIKITEXT
                        2 -> TextFormat.ID_ORGMODE
                        else -> TextFormat.ID_LATEX
                    }
                    StyleSheets.getStyleSheet(formatId, i % 2 == 0)
                }
            }
        }

        val results = jobs.awaitAll()
        assertEquals(50, results.size, "All 50 operations should complete")
        // No crash = success. Some results are "cleared", others are CSS strings
        val cssResults = results.filter { it != "cleared" }
        cssResults.forEach { css ->
            assertTrue(css.isNotEmpty(), "CSS results should be non-empty")
        }
    }

    @Test
    fun `Rapid alternating clearCache and getStyleSheet`() = runBlocking<Unit> {
        val jobs = (1..30).map { i ->
            async {
                if (i % 2 == 0) {
                    StyleSheets.clearCache()
                } else {
                    StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = true)
                }
            }
        }

        // All operations should complete without deadlock or crash
        withTimeout(10000) {
            jobs.awaitAll()
        }
        // Verify cache is still functional after the chaos
        val finalCss = StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = true)
        assertTrue(finalCss.isNotEmpty(), "Cache should be functional after rapid clear/get cycles")
    }

    // ==================== cacheSize Correctness ====================

    @Test
    fun `cacheSize returns 0 for empty cache`() {
        StyleSheets.clearCache()
        assertEquals(0, StyleSheets.cacheSize, "Empty cache should have size 0")
    }

    @Test
    fun `cacheSize increments as entries are added`() {
        StyleSheets.clearCache()
        assertEquals(0, StyleSheets.cacheSize)

        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = true)
        assertEquals(1, StyleSheets.cacheSize, "Cache should have 1 entry")

        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = false)
        assertEquals(2, StyleSheets.cacheSize, "Cache should have 2 entries (light + dark)")

        StyleSheets.getStyleSheet(TextFormat.ID_WIKITEXT, lightMode = true)
        assertEquals(3, StyleSheets.cacheSize, "Cache should have 3 entries")
    }

    @Test
    fun `cacheSize does not increment for repeated same-key access`() {
        StyleSheets.clearCache()
        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = true)
        val sizeAfterFirst = StyleSheets.cacheSize

        // Access same key again
        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = true)
        assertEquals(sizeAfterFirst, StyleSheets.cacheSize, "Repeated access should not grow cache")
    }

    @Test
    fun `cacheSize after populating all known format+theme combinations`() {
        StyleSheets.clearCache()

        val knownFormats = listOf(
            TextFormat.ID_MARKDOWN,
            TextFormat.ID_WIKITEXT,
            TextFormat.ID_RESTRUCTUREDTEXT,
            TextFormat.ID_ORGMODE,
            TextFormat.ID_ASCIIDOC,
            TextFormat.ID_LATEX
        )

        for (format in knownFormats) {
            StyleSheets.getStyleSheet(format, lightMode = true)
            StyleSheets.getStyleSheet(format, lightMode = false)
        }

        // 6 formats x 2 themes = 12 entries
        assertEquals(12, StyleSheets.cacheSize, "All format+theme combinations should be cached")
    }
}
