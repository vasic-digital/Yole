/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for CsvParser HTML generation and table parsing
 *
 *########################################################*/
package digital.vasic.yole.format.csv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CsvParserHtmlTest {

    private val parser = CsvParser()

    // ==================== HTML table structure ====================

    @Test
    fun testTableWrappedInCsvDiv() {
        val doc = parser.parse("Name,Age\nAlice,30")
        assertTrue(doc.parsedContent.contains("csv-table"))
    }

    @Test
    fun testTableContainsTableTag() {
        val doc = parser.parse("Name,Age\nAlice,30")
        assertTrue(doc.parsedContent.contains("<table>"))
        assertTrue(doc.parsedContent.contains("</table>"))
    }

    @Test
    fun testHeaderRowRendered() {
        val doc = parser.parse("Name,Age\nAlice,30")
        assertTrue(doc.parsedContent.contains("<thead>"))
        assertTrue(doc.parsedContent.contains("<th>"))
    }

    @Test
    fun testHeaderValues() {
        val doc = parser.parse("Name,Age\nAlice,30")
        assertTrue(doc.parsedContent.contains("Name"))
        assertTrue(doc.parsedContent.contains("Age"))
    }

    @Test
    fun testDataRowRendered() {
        val doc = parser.parse("Name,Age\nAlice,30")
        assertTrue(doc.parsedContent.contains("<tbody>"))
        assertTrue(doc.parsedContent.contains("<td>"))
    }

    @Test
    fun testDataValues() {
        val doc = parser.parse("Name,Age\nAlice,30")
        assertTrue(doc.parsedContent.contains("Alice"))
        assertTrue(doc.parsedContent.contains("30"))
    }

    @Test
    fun testMultipleRows() {
        val doc = parser.parse("Name,Age\nAlice,30\nBob,25")
        assertTrue(doc.parsedContent.contains("Alice"))
        assertTrue(doc.parsedContent.contains("Bob"))
    }

    // ==================== Delimiter detection ====================

    @Test
    fun testTabDelimiter() {
        val doc = parser.parse("Name\tAge\nAlice\t30")
        assertEquals("\t", doc.metadata["delimiter"])
    }

    @Test
    fun testSemicolonDelimiter() {
        val doc = parser.parse("Name;Age\nAlice;30")
        assertEquals(";", doc.metadata["delimiter"])
    }

    @Test
    fun testPipeDelimiter() {
        val doc = parser.parse("Name|Age\nAlice|30")
        assertEquals("|", doc.metadata["delimiter"])
    }

    @Test
    fun testCommaDelimiter() {
        val doc = parser.parse("Name,Age\nAlice,30")
        assertEquals(",", doc.metadata["delimiter"])
    }

    // ==================== Metadata ====================

    @Test
    fun testMetadataRowCount() {
        val doc = parser.parse("Name,Age\nAlice,30\nBob,25")
        assertEquals("2", doc.metadata["rows"])
    }

    @Test
    fun testMetadataColumnCount() {
        val doc = parser.parse("Name,Age,City\nAlice,30,NY")
        assertEquals("3", doc.metadata["columns"])
    }

    @Test
    fun testMetadataHasHeader() {
        val doc = parser.parse("Name,Age\nAlice,30")
        assertEquals("true", doc.metadata["hasHeader"])
    }

    // ==================== Quoted fields ====================

    @Test
    fun testQuotedFieldWithComma() {
        val fields = parser.parseLine("\"Doe, John\",30", ',', '"')
        assertEquals("Doe, John", fields[0])
        assertEquals("30", fields[1])
    }

    @Test
    fun testEscapedQuote() {
        val fields = parser.parseLine("\"He said \"\"hello\"\"\",30", ',', '"')
        assertTrue(fields[0].contains("hello"))
    }

    // ==================== Empty content ====================

    @Test
    fun testEmptyContent() {
        val doc = parser.parse("")
        assertNotNull(doc)
        assertEquals("0", doc.metadata["rows"])
    }

    @Test
    fun testEmptyCellRendersNbsp() {
        val doc = parser.parse("A,B\n1,")
        assertTrue(doc.parsedContent.contains("&nbsp;"))
    }

    // ==================== HTML escaping ====================

    @Test
    fun testHtmlEscapingInHeaders() {
        val doc = parser.parse("<Name>,Age\nAlice,30")
        assertTrue(doc.parsedContent.contains("&lt;Name&gt;") || !doc.parsedContent.contains("<Name>"))
    }

    @Test
    fun testHtmlEscapingInCells() {
        val doc = parser.parse("Name,Age\n<script>,30")
        assertTrue(!doc.parsedContent.contains("<script>") || doc.parsedContent.contains("&lt;script&gt;"))
    }

    // ==================== Markdown table conversion ====================

    @Test
    fun testMarkdownTableOutput() {
        val table = parser.parseCsv("Name,Age\nAlice,30")
        val md = parser.toMarkdownTable(table)
        assertTrue(md.contains("|"))
        assertTrue(md.contains("---"))
    }

    @Test
    fun testMarkdownTableHeaders() {
        val table = parser.parseCsv("Name,Age\nAlice,30")
        val md = parser.toMarkdownTable(table)
        assertTrue(md.contains("Name"))
        assertTrue(md.contains("Age"))
    }

    @Test
    fun testMarkdownTableData() {
        val table = parser.parseCsv("Name,Age\nAlice,30")
        val md = parser.toMarkdownTable(table)
        assertTrue(md.contains("Alice"))
        assertTrue(md.contains("30"))
    }

    // ==================== CsvConfig ====================

    @Test
    fun testCsvConfigDefaults() {
        val config = CsvConfig()
        assertEquals(',', config.delimiter)
        assertEquals('"', config.quote)
        assertTrue(config.hasHeader)
    }

    @Test
    fun testCsvConfigInfer() {
        val config = CsvConfig.infer("Name;Age;City")
        assertEquals(';', config.delimiter)
    }

    // ==================== CsvTable ====================

    @Test
    fun testCsvTableRowCount() {
        val table = parser.parseCsv("Name,Age\nAlice,30\nBob,25")
        assertEquals(2, table.rowCount)
    }

    @Test
    fun testCsvTableColumnCount() {
        val table = parser.parseCsv("Name,Age,City\nAlice,30,NY")
        assertEquals(3, table.columnCount)
    }

    // ==================== toHtml pass-through ====================

    @Test
    fun testToHtmlReturnsParsedContent() {
        val doc = parser.parse("Name,Age\nAlice,30")
        val html = parser.toHtml(doc, lightMode = true)
        assertEquals(doc.parsedContent, html)
    }

    // ==================== Comment lines ====================

    @Test
    fun testCommentLinesSkipped() {
        val csv = "Name,Age\n# comment\nAlice,30"
        val table = parser.parseCsv(csv)
        assertEquals(1, table.rowCount)
    }
}
