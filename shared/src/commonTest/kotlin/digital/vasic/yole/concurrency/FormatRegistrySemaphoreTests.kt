/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * FormatRegistry Semaphore Regression Tests
 *
 * Regression tests for Phase 1 concurrency fix #15:
 * Configurable parseSemaphore in FormatRegistry.
 *
 * Verifies that DEFAULT_PARSE_CONCURRENCY is 4,
 * configureParseConcurrency accepts valid ranges (1-16),
 * and rejects invalid values with IllegalArgumentException.
 *
 *########################################################*/

package digital.vasic.yole.concurrency

import digital.vasic.yole.format.FormatRegistry
import kotlin.test.*

/**
 * Regression tests for configurable parseSemaphore in FormatRegistry.
 *
 * Phase 1 fix #15 made the parseSemaphore configurable via
 * [FormatRegistry.configureParseConcurrency]. The semaphore
 * limits concurrent parse operations (default 4, range 1-16).
 *
 * These tests verify:
 * - DEFAULT_PARSE_CONCURRENCY constant is 4
 * - Valid values (1, 2, 4, 16) are accepted
 * - Invalid values (0, -1, 17) throw IllegalArgumentException
 */
class FormatRegistrySemaphoreTests {

    // ==================== Default Value ====================

    @Test
    fun `DEFAULT_PARSE_CONCURRENCY is 4`() {
        assertEquals(
            4,
            FormatRegistry.DEFAULT_PARSE_CONCURRENCY,
            "Default parse concurrency should be 4"
        )
    }

    // ==================== Valid Configurations ====================

    @Test
    fun `configureParseConcurrency with 2 does not throw`() {
        // Should not throw
        FormatRegistry.configureParseConcurrency(2)
        // If we got here, it succeeded
        assertTrue(true, "configureParseConcurrency(2) should succeed")

        // Reset to default
        FormatRegistry.configureParseConcurrency(FormatRegistry.DEFAULT_PARSE_CONCURRENCY)
    }

    @Test
    fun `configureParseConcurrency with 1 minimum does not throw`() {
        FormatRegistry.configureParseConcurrency(1)
        assertTrue(true, "configureParseConcurrency(1) should succeed")

        // Reset to default
        FormatRegistry.configureParseConcurrency(FormatRegistry.DEFAULT_PARSE_CONCURRENCY)
    }

    @Test
    fun `configureParseConcurrency with 16 maximum does not throw`() {
        FormatRegistry.configureParseConcurrency(16)
        assertTrue(true, "configureParseConcurrency(16) should succeed")

        // Reset to default
        FormatRegistry.configureParseConcurrency(FormatRegistry.DEFAULT_PARSE_CONCURRENCY)
    }

    @Test
    fun `configureParseConcurrency with 4 default value does not throw`() {
        FormatRegistry.configureParseConcurrency(4)
        assertTrue(true, "configureParseConcurrency(4) should succeed")
    }

    @Test
    fun `configureParseConcurrency with 8 mid-range value does not throw`() {
        FormatRegistry.configureParseConcurrency(8)
        assertTrue(true, "configureParseConcurrency(8) should succeed")

        // Reset to default
        FormatRegistry.configureParseConcurrency(FormatRegistry.DEFAULT_PARSE_CONCURRENCY)
    }

    // ==================== Invalid Configurations ====================

    @Test
    fun `configureParseConcurrency with 0 throws IllegalArgumentException`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            FormatRegistry.configureParseConcurrency(0)
        }
        assertNotNull(exception.message, "Exception should have a message")
        assertTrue(
            exception.message!!.contains("0"),
            "Exception message should mention the invalid value: ${exception.message}"
        )
    }

    @Test
    fun `configureParseConcurrency with 17 throws IllegalArgumentException`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            FormatRegistry.configureParseConcurrency(17)
        }
        assertNotNull(exception.message, "Exception should have a message")
        assertTrue(
            exception.message!!.contains("17"),
            "Exception message should mention the invalid value: ${exception.message}"
        )
    }

    @Test
    fun `configureParseConcurrency with negative 1 throws IllegalArgumentException`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            FormatRegistry.configureParseConcurrency(-1)
        }
        assertNotNull(exception.message, "Exception should have a message")
        assertTrue(
            exception.message!!.contains("-1"),
            "Exception message should mention the invalid value: ${exception.message}"
        )
    }

    @Test
    fun `configureParseConcurrency with large negative throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            FormatRegistry.configureParseConcurrency(-100)
        }
    }

    @Test
    fun `configureParseConcurrency with very large value throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            FormatRegistry.configureParseConcurrency(1000)
        }
    }

    @Test
    fun `configureParseConcurrency with Int MAX_VALUE throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            FormatRegistry.configureParseConcurrency(Int.MAX_VALUE)
        }
    }

    @Test
    fun `configureParseConcurrency with Int MIN_VALUE throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            FormatRegistry.configureParseConcurrency(Int.MIN_VALUE)
        }
    }

    // ==================== Boundary Verification ====================

    @Test
    fun `Boundary - 1 is valid, 0 is invalid`() {
        // 1 should succeed
        FormatRegistry.configureParseConcurrency(1)
        assertTrue(true)

        // 0 should fail
        assertFailsWith<IllegalArgumentException> {
            FormatRegistry.configureParseConcurrency(0)
        }

        // Reset
        FormatRegistry.configureParseConcurrency(FormatRegistry.DEFAULT_PARSE_CONCURRENCY)
    }

    @Test
    fun `Boundary - 16 is valid, 17 is invalid`() {
        // 16 should succeed
        FormatRegistry.configureParseConcurrency(16)
        assertTrue(true)

        // 17 should fail
        assertFailsWith<IllegalArgumentException> {
            FormatRegistry.configureParseConcurrency(17)
        }

        // Reset
        FormatRegistry.configureParseConcurrency(FormatRegistry.DEFAULT_PARSE_CONCURRENCY)
    }

    // ==================== Sequential Reconfiguration ====================

    @Test
    fun `configureParseConcurrency can be called multiple times with valid values`() {
        // Should be able to reconfigure multiple times
        FormatRegistry.configureParseConcurrency(1)
        FormatRegistry.configureParseConcurrency(8)
        FormatRegistry.configureParseConcurrency(16)
        FormatRegistry.configureParseConcurrency(4)
        assertTrue(true, "Multiple reconfigurations should succeed")
    }

    @Test
    fun `configureParseConcurrency all valid values from 1 to 16`() {
        for (permits in 1..16) {
            FormatRegistry.configureParseConcurrency(permits)
        }
        assertTrue(true, "All values from 1 to 16 should be valid")

        // Reset
        FormatRegistry.configureParseConcurrency(FormatRegistry.DEFAULT_PARSE_CONCURRENCY)
    }
}
