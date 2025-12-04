/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for CSV utility functions and helpers
 *
 *########################################################*/
package digital.vasic.yole.format.csv

import digital.vasic.yole.format.ParserRegistry
import kotlin.test.*

/**
 * Tests for CSV utility functions including CsvConfig.infer() and parseLine().
 *
 * Tests cover:
 * - Delimiter detection
 * - Quote character detection
 * - Line parsing with various delimiters
 * - Escaped quotes handling
 * - Empty fields
 * - Edge cases
 */
class CsvUtilsTest {

    private lateinit var parser: CsvParser

    @BeforeTest
    fun setup() {
        parser = CsvParser()
        ParserRegistry.clear()
        ParserRegistry.register(parser)
    }

    @AfterTest
    fun teardown() {
        ParserRegistry.clear()
    }

    // ==================== CsvConfig.infer() Tests ====================

    @Test
    fun `should detect comma delimiter`() {
        val line = "col1,col2,col3"
        val config = CsvConfig.infer(line)

        assertEquals(',', config.delimiter)
    }

    @Test
    fun `should detect semicolon delimiter`() {
        val line = "col1;col2;col3"
        val config = CsvConfig.infer(line)

        assertEquals(';', config.delimiter)
    }

    @Test
    fun `should detect tab delimiter`() {
        val line = "col1\tcol2\tcol3"
        val config = CsvConfig.infer(line)

        assertEquals('\t', config.delimiter)
    }

    @Test
    fun `should detect pipe delimiter`() {
        val line = "col1|col2|col3"
        val config = CsvConfig.infer(line)

        assertEquals('|', config.delimiter)
    }

    @Test
    fun `should default to comma when no delimiter found`() {
        val line = "just some text"
        val config = CsvConfig.infer(line)

        assertEquals(',', config.delimiter)
    }

    @Test
    fun `should detect single quote character`() {
        val line = "'col1','col2','col3'"
        val config = CsvConfig.infer(line)

        assertEquals('\'', config.quote)
    }

    @Test
    fun `should default to double quote`() {
        val line = "col1,col2,col3"
        val config = CsvConfig.infer(line)

        assertEquals('"', config.quote)
    }

    @Test
    fun `should prioritize tab over other delimiters`() {
        val line = "col1\tcol2,col3"  // Has both tab and comma
        val config = CsvConfig.infer(line)

        assertEquals('\t', config.delimiter)  // Tab has priority
    }

    @Test
    fun `should prioritize semicolon over comma`() {
        val line = "col1;col2,col3"  // Has both semicolon and comma
        val config = CsvConfig.infer(line)

        assertEquals(';', config.delimiter)  // Semicolon has priority
    }

    @Test
    fun `should handle empty line in config infer`() {
        val config = CsvConfig.infer("")

        assertEquals(',', config.delimiter)  // Default to comma
        assertEquals('"', config.quote)      // Default to double quote
    }

    // ==================== parseLine() Basic Tests ====================

    @Test
    fun `should parse simple comma-separated line`() {
        val line = "val1,val2,val3"
        val fields = parser.parseLine(line, ',')

        assertEquals(listOf("val1", "val2", "val3"), fields)
    }

    @Test
    fun `should parse line with semicolon delimiter`() {
        val line = "val1;val2;val3"
        val fields = parser.parseLine(line, ';')

        assertEquals(listOf("val1", "val2", "val3"), fields)
    }

    @Test
    fun `should parse line with tab delimiter`() {
        val line = "val1\tval2\tval3"
        val fields = parser.parseLine(line, '\t')

        assertEquals(listOf("val1", "val2", "val3"), fields)
    }

    @Test
    fun `should handle empty fields`() {
        val line = "val1,,val3"
        val fields = parser.parseLine(line, ',')

        assertEquals(listOf("val1", "", "val3"), fields)
    }

    @Test
    fun `should handle leading empty field`() {
        val line = ",val2,val3"
        val fields = parser.parseLine(line, ',')

        assertEquals(listOf("", "val2", "val3"), fields)
    }

    @Test
    fun `should handle trailing empty field`() {
        val line = "val1,val2,"
        val fields = parser.parseLine(line, ',')

        assertEquals(listOf("val1", "val2", ""), fields)
    }

    @Test
    fun `should handle all empty fields`() {
        val line = ",,"
        val fields = parser.parseLine(line, ',')

        assertEquals(listOf("", "", ""), fields)
    }

    // ==================== Quoted Fields Tests ====================

    @Test
    fun `should parse quoted field`() {
        val line = "\"quoted value\",other"
        val fields = parser.parseLine(line, ',', '"')

        assertEquals(listOf("quoted value", "other"), fields)
    }

    @Test
    fun `should handle delimiter inside quoted field`() {
        val line = "\"value,with,commas\",other"
        val fields = parser.parseLine(line, ',', '"')

        assertEquals(listOf("value,with,commas", "other"), fields)
    }

    @Test
    fun `should handle quotes inside quoted field`() {
        val line = "\"value with \"\"escaped\"\" quotes\",other"
        val fields = parser.parseLine(line, ',', '"')

        assertEquals(listOf("value with \"escaped\" quotes", "other"), fields)
    }

    @Test
    fun `should handle multiple escaped quotes`() {
        val line = "\"\"\"quoted\"\"\",other"
        val fields = parser.parseLine(line, ',', '"')

        assertEquals(listOf("\"quoted\"", "other"), fields)
    }

    @Test
    fun `should handle quote at start of field`() {
        val line = "\"start,middle"
        val fields = parser.parseLine(line, ',', '"')

        // Parser should handle this gracefully
        assertNotNull(fields)
        assertTrue(fields.isNotEmpty())
    }

    @Test
    fun `should handle quote at end of field`() {
        val line = "middle,end\""
        val fields = parser.parseLine(line, ',', '"')

        // Parser should handle this gracefully
        assertNotNull(fields)
        assertEquals(2, fields.size)
    }

    @Test
    fun `should handle empty quoted field`() {
        val line = "\"\",value"
        val fields = parser.parseLine(line, ',', '"')

        assertEquals(listOf("", "value"), fields)
    }

    @Test
    fun `should handle all quoted fields`() {
        val line = "\"val1\",\"val2\",\"val3\""
        val fields = parser.parseLine(line, ',', '"')

        assertEquals(listOf("val1", "val2", "val3"), fields)
    }

    // ==================== Mixed Quoted/Unquoted Tests ====================

    @Test
    fun `should handle mixed quoted and unquoted fields`() {
        val line = "unquoted,\"quoted\",unquoted"
        val fields = parser.parseLine(line, ',', '"')

        assertEquals(listOf("unquoted", "quoted", "unquoted"), fields)
    }

    @Test
    fun `should handle whitespace around unquoted fields`() {
        val line = "  val1  ,  val2  ,  val3  "
        val fields = parser.parseLine(line, ',')

        assertEquals(listOf("  val1  ", "  val2  ", "  val3  "), fields)
    }

    @Test
    fun `should handle whitespace inside quoted fields`() {
        val line = "\"  val1  \",\"  val2  \""
        val fields = parser.parseLine(line, ',', '"')

        assertEquals(listOf("  val1  ", "  val2  "), fields)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle single field`() {
        val line = "onlyfield"
        val fields = parser.parseLine(line, ',')

        assertEquals(listOf("onlyfield"), fields)
    }

    @Test
    fun `should handle empty line in parse`() {
        val line = ""
        val fields = parser.parseLine(line, ',')

        assertEquals(listOf(""), fields)
    }

    @Test
    fun `should handle line with only delimiter`() {
        val line = ","
        val fields = parser.parseLine(line, ',')

        assertEquals(listOf("", ""), fields)
    }

    @Test
    fun `should handle line with multiple consecutive delimiters`() {
        val line = ",,,"
        val fields = parser.parseLine(line, ',')

        assertEquals(listOf("", "", "", ""), fields)
    }

    @Test
    fun `should handle very long field`() {
        val longValue = "A".repeat(10000)
        val line = "$longValue,other"
        val fields = parser.parseLine(line, ',')

        assertEquals(2, fields.size)
        assertEquals(10000, fields[0].length)
    }

    @Test
    fun `should handle many fields`() {
        val line = (1..1000).joinToString(",") { "val$it" }
        val fields = parser.parseLine(line, ',')

        assertEquals(1000, fields.size)
        assertEquals("val1", fields[0])
        assertEquals("val1000", fields[999])
    }

    // ==================== Special Characters Tests ====================

    @Test
    fun `should handle newline in quoted field`() {
        val line = "\"value with\nnewline\",other"
        val fields = parser.parseLine(line, ',', '"')

        assertEquals(listOf("value with\nnewline", "other"), fields)
    }

    @Test
    fun `should handle tab in quoted field`() {
        val line = "\"value\twith\ttabs\",other"
        val fields = parser.parseLine(line, ',', '"')

        assertEquals(listOf("value\twith\ttabs", "other"), fields)
    }

    @Test
    fun `should handle unicode in fields`() {
        val line = "你好,世界,🌍"
        val fields = parser.parseLine(line, ',')

        assertEquals(listOf("你好", "世界", "🌍"), fields)
    }

    @Test
    fun `should handle null bytes`() {
        val line = "val\u0000ue,other"
        val fields = parser.parseLine(line, ',')

        assertEquals(listOf("val\u0000ue", "other"), fields)
    }

    // ==================== CsvTable Tests ====================

    @Test
    fun `should create CsvTable with correct row count`() {
        val rows = listOf(
            listOf("val1", "val2"),
            listOf("val3", "val4")
        )
        val table = CsvTable(rows)

        assertEquals(2, table.rowCount)
    }

    @Test
    fun `should create CsvTable with correct column count from headers`() {
        val headers = listOf("col1", "col2", "col3")
        val rows = listOf(listOf("val1", "val2", "val3"))
        val table = CsvTable(rows, headers)

        assertEquals(3, table.columnCount)
    }

    @Test
    fun `should create CsvTable with column count from first row when no headers`() {
        val rows = listOf(
            listOf("val1", "val2", "val3"),
            listOf("val4", "val5", "val6")
        )
        val table = CsvTable(rows)

        assertEquals(3, table.columnCount)
    }

    @Test
    fun `should handle empty CsvTable`() {
        val table = CsvTable(emptyList())

        assertEquals(0, table.rowCount)
        assertEquals(0, table.columnCount)
    }

    @Test
    fun `should handle CsvTable with headers but no data`() {
        val headers = listOf("col1", "col2")
        val table = CsvTable(emptyList(), headers)

        assertEquals(0, table.rowCount)
        assertEquals(2, table.columnCount)
    }

    // ==================== CsvConfig Tests ====================

    @Test
    fun `should create CsvConfig with defaults`() {
        val config = CsvConfig()

        assertEquals(',', config.delimiter)
        assertEquals('"', config.quote)
        assertTrue(config.hasHeader)
    }

    @Test
    fun `should create CsvConfig with custom values`() {
        val config = CsvConfig(
            delimiter = ';',
            quote = '\'',
            hasHeader = false
        )

        assertEquals(';', config.delimiter)
        assertEquals('\'', config.quote)
        assertFalse(config.hasHeader)
    }

    @Test
    fun `should support data class copy`() {
        val original = CsvConfig(',', '"', true)
        val modified = original.copy(delimiter = ';')

        assertEquals(';', modified.delimiter)
        assertEquals('"', modified.quote)
        assertTrue(modified.hasHeader)
    }

    @Test
    fun `should support equality comparison`() {
        val config1 = CsvConfig(',', '"', true)
        val config2 = CsvConfig(',', '"', true)
        val config3 = CsvConfig(';', '"', true)

        assertEquals(config1, config2)
        assertNotEquals(config1, config3)
    }
}
