/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for CSV Parser
 *
 *########################################################*/
package digital.vasic.yole.format.csv

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CsvParserTest {

    private val parser = CsvParser()

    // ==================== Basic Parsing Tests ====================

    @Test
    fun `test basic CSV parsing with headers`() {
        val content = """
            Name,Age,City
            John,30,New York
            Jane,25,Los Angeles
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_CSV, result.format.id)
        assertEquals(content, result.rawContent)
        assertEquals("2", result.metadata["rows"])
        assertEquals("3", result.metadata["columns"])
        assertEquals(",", result.metadata["delimiter"])
    }

    @Test
    fun `test CSV parsing without headers`() {
        val content = """
            John,30,New York
            Jane,25,Los Angeles
        """.trimIndent()

        val config = CsvConfig(hasHeader = false)
        val table = parser.parseCsv(content, config)

        assertEquals(2, table.rowCount)
        assertEquals(3, table.columnCount)
        assertNull(table.headers)
        assertEquals("John", table.rows[0][0])
    }

    @Test
    fun `test empty CSV`() {
        val content = ""

        val table = parser.parseCsv(content)

        assertEquals(0, table.rowCount)
        assertEquals(0, table.columnCount)
    }

    @Test
    fun `test single row CSV`() {
        val content = "Name,Age,City"

        val table = parser.parseCsv(content, CsvConfig(hasHeader = true))

        assertEquals(0, table.rowCount)
        assertEquals(3, table.columnCount)
        assertNotNull(table.headers)
        assertEquals(listOf("Name", "Age", "City"), table.headers)
    }

    // ==================== Delimiter Tests ====================

    @Test
    fun `test semicolon-separated values`() {
        val content = """
            Name;Age;City
            John;30;New York
        """.trimIndent()

        val result = parser.parse(content)

        assertEquals(";", result.metadata["delimiter"])
        assertEquals("1", result.metadata["rows"])
    }

    @Test
    fun `test tab-separated values`() {
        val content = "Name\tAge\tCity\nJohn\t30\tNew York"

        val table = parser.parseCsv(content, CsvConfig(delimiter = '\t'))

        assertEquals(1, table.rowCount)
        assertEquals("John", table.rows[0][0])
        assertEquals("30", table.rows[0][1])
    }

    @Test
    fun `test pipe-separated values`() {
        val content = "Name|Age|City\nJohn|30|New York"

        val table = parser.parseCsv(content, CsvConfig(delimiter = '|'))

        assertEquals(1, table.rowCount)
        assertEquals(listOf("Name", "Age", "City"), table.headers)
    }

    // ==================== Quoted Fields Tests ====================

    @Test
    fun `test quoted fields with commas`() {
        val content = """
            Name,Description,Price
            "Widget","A small, handy tool",9.99
        """.trimIndent()

        val table = parser.parseCsv(content)

        assertEquals(1, table.rowCount)
        assertEquals("A small, handy tool", table.rows[0][1])
    }

    @Test
    fun `test escaped quotes`() {
        val content = """
            Name,Quote
            John,"He said ""Hello"""
        """.trimIndent()

        val table = parser.parseCsv(content)

        assertEquals(1, table.rowCount)
        assertEquals("He said \"Hello\"", table.rows[0][1])
    }

    @Test
    fun `test empty quoted fields`() {
        val content = """
            Name,Value
            John,""
        """.trimIndent()

        val table = parser.parseCsv(content)

        assertEquals(1, table.rowCount)
        assertEquals("", table.rows[0][1])
    }

    // ==================== Edge Cases ====================

    @Test
    fun `test empty cells`() {
        val content = """
            A,B,C
            1,,3
            ,2,
        """.trimIndent()

        val table = parser.parseCsv(content)

        assertEquals(2, table.rowCount)
        assertEquals("", table.rows[0][1])
        assertEquals("", table.rows[1][0])
        assertEquals("", table.rows[1][2])
    }

    @Test
    fun `test whitespace handling`() {
        val content = """
            Name , Age , City
            John , 30 , New York
        """.trimIndent()

        val table = parser.parseCsv(content)

        // Fields should contain whitespace as-is
        assertNotNull(table.headers)
        assertEquals(" Age ", table.headers!![1])
    }

    @Test
    fun `test comments are skipped`() {
        val content = """
            # This is a comment
            Name,Age
            John,30
            # Another comment
        """.trimIndent()

        val table = parser.parseCsv(content)

        assertEquals(1, table.rowCount)
        assertEquals("John", table.rows[0][0])
    }

    @Test
    fun `test blank lines are skipped`() {
        val content = """
            Name,Age

            John,30

            Jane,25
        """.trimIndent()

        val table = parser.parseCsv(content)

        assertEquals(2, table.rowCount)
    }

    // ==================== Config Inference Tests ====================

    @Test
    fun `test config inference for comma`() {
        val config = CsvConfig.infer("name,age,city")

        assertEquals(',', config.delimiter)
        assertEquals('"', config.quote)
    }

    @Test
    fun `test config inference for semicolon`() {
        val config = CsvConfig.infer("name;age;city")

        assertEquals(';', config.delimiter)
    }

    @Test
    fun `test config inference for tab`() {
        val config = CsvConfig.infer("name\tage\tcity")

        assertEquals('\t', config.delimiter)
    }

    @Test
    fun `test config inference for single quote`() {
        val config = CsvConfig.infer("'name','age','city'")

        assertEquals('\'', config.quote)
    }

    // ==================== HTML Conversion Tests ====================

    @Test
    fun `test HTML conversion contains table structure`() {
        val content = """
            Name,Age
            John,30
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertTrue(html.contains("<table>"))
        assertTrue(html.contains("<thead>"))
        assertTrue(html.contains("<tbody>"))
        assertTrue(html.contains("<th>"))
        assertTrue(html.contains("<td>"))
        assertTrue(html.contains("</table>"))
    }

    @Test
    fun `test HTML escaping`() {
        val content = """
            Name,Code
            Test,<script>alert('xss')</script>
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        // Should not contain raw script tag
        assertTrue(!html.contains("<script>"))
        assertTrue(html.contains("&lt;script&gt;") || html.contains("&lt;"))
    }

    // ==================== Markdown Conversion Tests ====================

    @Test
    fun `test Markdown table conversion`() {
        val content = """
            Name,Age
            John,30
            Jane,25
        """.trimIndent()

        val table = parser.parseCsv(content)
        val markdown = parser.toMarkdownTable(table)

        assertTrue(markdown.contains("| Name | Age |"))
        assertTrue(markdown.contains("| --- | --- |"))
        assertTrue(markdown.contains("| John | 30 |"))
    }

    // ==================== Format Detection Tests ====================

    @Test
    fun `test format detection by extension`() {
        val format = FormatRegistry.getByExtension(".csv")

        assertNotNull(format)
        assertEquals(TextFormat.ID_CSV, format.id)
        assertEquals("CSV", format.name)
    }

    @Test
    fun `test parser supported format`() {
        assertEquals(TextFormat.ID_CSV, parser.supportedFormat.id)
    }

    // ==================== Large Data Tests ====================

    @Test
    fun `test parsing many rows`() {
        val header = "Col1,Col2,Col3"
        val rows = (1..100).map { "Value$it,Data$it,Info$it" }
        val content = (listOf(header) + rows).joinToString("\n")

        val table = parser.parseCsv(content)

        assertEquals(100, table.rowCount)
        assertEquals(3, table.columnCount)
    }

    @Test
    fun `test parsing many columns`() {
        val headers = (1..50).map { "Col$it" }
        val values = (1..50).map { "Val$it" }
        val content = headers.joinToString(",") + "\n" + values.joinToString(",")

        val table = parser.parseCsv(content)

        assertEquals(1, table.rowCount)
        assertEquals(50, table.columnCount)
    }

    // ==================== parseLine Tests ====================

    @Test
    fun `test parseLine simple`() {
        val fields = parser.parseLine("a,b,c")

        assertEquals(3, fields.size)
        assertEquals(listOf("a", "b", "c"), fields)
    }

    @Test
    fun `test parseLine with quoted field containing delimiter`() {
        val fields = parser.parseLine("a,\"b,c\",d")

        assertEquals(3, fields.size)
        assertEquals("a", fields[0])
        assertEquals("b,c", fields[1])
        assertEquals("d", fields[2])
    }

    @Test
    fun `test parseLine with escaped quotes`() {
        val fields = parser.parseLine("a,\"b\"\"c\",d")

        assertEquals(3, fields.size)
        assertEquals("b\"c", fields[1])
    }

    @Test
    fun `test parseLine empty string`() {
        val fields = parser.parseLine("")

        assertEquals(1, fields.size)
        assertEquals("", fields[0])
    }
}
