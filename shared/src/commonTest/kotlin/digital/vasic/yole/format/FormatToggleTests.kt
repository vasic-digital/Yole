/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for FormatRegistry format toggle (enable/disable) feature.
 * Verifies that only Markdown is enabled by default and that
 * toggling works correctly.
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlin.test.*

class FormatToggleTests {

    @Test
    fun `only Markdown is enabled by default`() {
        val enabledIds = FormatRegistry.getEnabledFormatIds()
        assertEquals(setOf(TextFormat.ID_MARKDOWN), enabledIds,
            "Only Markdown should be enabled by default")
    }

    @Test
    fun `Markdown is always enabled even after clear`() {
        FormatRegistry.setEnabledFormatIds(emptySet())
        assertTrue(FormatRegistry.isFormatEnabled(TextFormat.ID_MARKDOWN),
            "Markdown must always be enabled")
    }

    @Test
    fun `enableFormat adds format to enabled set`() {
        FormatRegistry.setFormatEnabled(TextFormat.ID_PLAINTEXT)
        assertTrue(FormatRegistry.isFormatEnabled(TextFormat.ID_PLAINTEXT))

        // Clean up
        FormatRegistry.setFormatDisabled(TextFormat.ID_PLAINTEXT)
    }

    @Test
    fun `disableFormat removes format from enabled set`() {
        FormatRegistry.setFormatEnabled(TextFormat.ID_PLAINTEXT)
        assertTrue(FormatRegistry.isFormatEnabled(TextFormat.ID_PLAINTEXT))

        FormatRegistry.setFormatDisabled(TextFormat.ID_PLAINTEXT)
        assertFalse(FormatRegistry.isFormatEnabled(TextFormat.ID_PLAINTEXT),
            "Plaintext should be disabled after toggle")
    }

    @Test
    fun `Markdown cannot be disabled`() {
        FormatRegistry.setFormatDisabled(TextFormat.ID_MARKDOWN)
        assertTrue(FormatRegistry.isFormatEnabled(TextFormat.ID_MARKDOWN),
            "Markdown must always remain enabled")
    }

    @Test
    fun `network formats are always enabled`() {
        assertTrue(FormatRegistry.isFormatEnabled(FormatRegistry.ID_DROPBOX))
        assertTrue(FormatRegistry.isFormatEnabled(FormatRegistry.ID_FTP))
        assertTrue(FormatRegistry.isFormatEnabled(FormatRegistry.ID_GOOGLEDRIVE))
        assertTrue(FormatRegistry.isFormatEnabled(FormatRegistry.ID_ONEDRIVE))
        assertTrue(FormatRegistry.isFormatEnabled(FormatRegistry.ID_SFTP))
    }

    @Test
    fun `network formats cannot be disabled`() {
        FormatRegistry.setFormatDisabled(FormatRegistry.ID_DROPBOX)
        assertTrue(FormatRegistry.isFormatEnabled(FormatRegistry.ID_DROPBOX),
            "Network formats must always remain enabled")
    }

    @Test
    fun `getEnabledTextFormats returns only enabled text formats`() {
        // Default: only Markdown
        val enabled = FormatRegistry.getEnabledTextFormats()
        assertEquals(1, enabled.size)
        assertEquals(TextFormat.ID_MARKDOWN, enabled[0].id)

        // Enable another format
        FormatRegistry.setFormatEnabled(TextFormat.ID_TODOTXT)
        val afterAdd = FormatRegistry.getEnabledTextFormats()
        assertEquals(2, afterAdd.size)
        assertTrue(afterAdd.any { it.id == TextFormat.ID_MARKDOWN })
        assertTrue(afterAdd.any { it.id == TextFormat.ID_TODOTXT })

        // Clean up
        FormatRegistry.setFormatDisabled(TextFormat.ID_TODOTXT)
    }

    @Test
    fun `setEnabledFormatIds bulk enables formats`() {
        FormatRegistry.setEnabledFormatIds(setOf(
            TextFormat.ID_MARKDOWN,
            TextFormat.ID_LATEX,
            TextFormat.ID_CSV
        ))
        val enabled = FormatRegistry.getEnabledFormatIds()
        assertTrue(enabled.contains(TextFormat.ID_MARKDOWN))
        assertTrue(enabled.contains(TextFormat.ID_LATEX))
        assertTrue(enabled.contains(TextFormat.ID_CSV))
        assertFalse(enabled.contains(TextFormat.ID_PLAINTEXT))

        // Reset to default
        FormatRegistry.setEnabledFormatIds(setOf(TextFormat.ID_MARKDOWN))
    }

    @Test
    fun `getEnabledFormats includes network formats always`() {
        // Default: only Markdown + network formats
        val all = FormatRegistry.getEnabledFormats()
        assertTrue(all.any { it.id == TextFormat.ID_MARKDOWN })
        assertTrue(all.any { it.id == FormatRegistry.ID_DROPBOX })
        assertTrue(all.any { it.id == FormatRegistry.ID_SFTP })
    }

    @Test
    fun `isFormatEnabled returns correct values`() {
        assertTrue(FormatRegistry.isFormatEnabled(TextFormat.ID_MARKDOWN))
        assertFalse(FormatRegistry.isFormatEnabled(TextFormat.ID_PLAINTEXT))

        FormatRegistry.setFormatEnabled(TextFormat.ID_PLAINTEXT)
        assertTrue(FormatRegistry.isFormatEnabled(TextFormat.ID_PLAINTEXT))

        FormatRegistry.setFormatDisabled(TextFormat.ID_PLAINTEXT)
        assertFalse(FormatRegistry.isFormatEnabled(TextFormat.ID_PLAINTEXT))
    }

    @Test
    fun `getTextFormats returns all text formats regardless of toggle`() {
        val all = FormatRegistry.getTextFormats()
        assertTrue(all.size >= 17, "Should return all text editor formats")
        assertTrue(all.any { it.id == TextFormat.ID_MARKDOWN })
        assertTrue(all.any { it.id == TextFormat.ID_PLAINTEXT })
    }

    @Test
    fun `getEnabledFormatIds returns current state`() {
        assertEquals(setOf(TextFormat.ID_MARKDOWN), FormatRegistry.getEnabledFormatIds())

        FormatRegistry.setFormatEnabled(TextFormat.ID_LATEX)
        val ids = FormatRegistry.getEnabledFormatIds()
        assertTrue(ids.contains(TextFormat.ID_LATEX))

        FormatRegistry.setFormatDisabled(TextFormat.ID_LATEX)
    }

    @Test
    fun `double enable is idempotent`() {
        FormatRegistry.setFormatEnabled(TextFormat.ID_PLAINTEXT)
        FormatRegistry.setFormatEnabled(TextFormat.ID_PLAINTEXT)
        assertTrue(FormatRegistry.isFormatEnabled(TextFormat.ID_PLAINTEXT))

        val count = FormatRegistry.getEnabledFormatIds().size
        FormatRegistry.setFormatEnabled(TextFormat.ID_PLAINTEXT)
        assertEquals(count, FormatRegistry.getEnabledFormatIds().size)

        FormatRegistry.setFormatDisabled(TextFormat.ID_PLAINTEXT)
    }

    @Test
    fun `double disable is idempotent`() {
        FormatRegistry.setFormatDisabled(TextFormat.ID_PLAINTEXT)
        FormatRegistry.setFormatDisabled(TextFormat.ID_PLAINTEXT)
        assertFalse(FormatRegistry.isFormatEnabled(TextFormat.ID_PLAINTEXT))
    }
}
