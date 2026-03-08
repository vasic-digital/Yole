/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Simple compilation test to verify core functionality
 *
 *########################################################*/

package digital.vasic.yole.format

import kotlin.test.*

/**
 * Simple test to verify that the FormatRegistry constants exist
 * and basic format resolution works.
 */
class SimpleCompilationTest {

    @Test
    fun `test format registry constants exist`() {
        assertNotNull(FormatRegistry.ID_MARKDOWN)
        assertNotNull(FormatRegistry.ID_TODOTXT)
        assertNotNull(FormatRegistry.ID_CSV)
        assertNotNull(FormatRegistry.ID_PLAINTEXT)

        assertEquals("markdown", FormatRegistry.ID_MARKDOWN)
        assertEquals("todotxt", FormatRegistry.ID_TODOTXT)
        assertEquals("csv", FormatRegistry.ID_CSV)
        assertEquals("plaintext", FormatRegistry.ID_PLAINTEXT)
    }

    @Test
    fun `test format registry has all core formats`() {
        val allFormats = FormatRegistry.formats
        assertTrue(allFormats.isNotEmpty(), "FormatRegistry should contain formats")

        val formatIds = allFormats.map { it.id }
        assertTrue(formatIds.contains("markdown"), "Should contain markdown")
        assertTrue(formatIds.contains("todotxt"), "Should contain todotxt")
        assertTrue(formatIds.contains("csv"), "Should contain csv")
        assertTrue(formatIds.contains("plaintext"), "Should contain plaintext")
    }
}
