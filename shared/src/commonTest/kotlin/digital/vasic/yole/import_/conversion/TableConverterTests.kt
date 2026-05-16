/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 2: anti-bluff tests for TableConverter.
 *
 * Mutation stub: replace toMarkdownTable body with `return ""`
 * → tests twoByTwo_producesValidGfmTable and
 *          pipeInCell_isEscaped all FAIL.
 *#######################################################*/
package digital.vasic.yole.import_.conversion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TableConverterTests {

    @Test
    fun twoByTwo_producesValidGfmTable() {
        val rows = listOf(
            listOf("Name", "Age"),
            listOf("Alice", "30"),
            listOf("Bob", "25"),
        )
        val result = TableConverter.toMarkdownTable(rows)

        // Header + separator + 2 data rows → 4 lines
        val lines = result.lines()
        assertEquals(4, lines.size, "Expected 4 lines (header + sep + 2 data rows)")
        assertEquals("| Name | Age |", lines[0])
        assertEquals("| --- | --- |", lines[1])
        assertEquals("| Alice | 30 |", lines[2])
        assertEquals("| Bob | 25 |", lines[3])
    }

    @Test
    fun pipeInCell_isEscaped() {
        val rows = listOf(
            listOf("A|B", "C"),
            listOf("x|y|z", "val"),
        )
        val result = TableConverter.toMarkdownTable(rows)
        val lines = result.lines()

        // Pipe chars inside cells must be escaped as \|
        assertTrue(lines[0].contains("A\\|B"), "Header pipe must be escaped: ${lines[0]}")
        assertTrue(lines[2].contains("x\\|y\\|z"), "Data pipe must be escaped: ${lines[2]}")
    }

    @Test
    fun emptyRows_returnsEmptyString() {
        val result = TableConverter.toMarkdownTable(emptyList())
        assertEquals("", result, "Empty rows must produce empty string")
    }
}
