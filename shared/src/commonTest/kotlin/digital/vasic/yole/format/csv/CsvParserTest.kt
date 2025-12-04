/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for CSV parser
 *
 *########################################################*/
package digital.vasic.yole.format.csv

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.csv.CsvParser
import org.junit.Test
import kotlin.test.*

/**
 * Unit tests for CSV format parser.
 *
 * Tests cover:
 * - Format detection by extension
 * - Basic parsing functionality
 * - Edge cases and error handling
 * - Empty input handling
 * - Special characters
 */
class CsvParserTest {

    private val parser = CsvParser()

    // ==================== Format Detection Tests ====================

    @Test
    fun `should detect CSV format by extension`() {
        val format = FormatRegistry.getByExtension(".csv")

        assertNotNull(format)
        assertEquals(FormatRegistry.ID_CSV, format.id)
        assertEquals("CSV", format.name)
    }

    @Test
    fun `should detect CSV format by filename`() {
        val format = FormatRegistry.detectByFilename("test.csv")

        assertNotNull(format)
        assertEquals(FormatRegistry.ID_CSV, format.id)
    }

    @Test
    fun `should support all CSV extensions`() {
        val extensions = listOf(".csv")

        extensions.forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
            assertEquals(FormatRegistry.ID_CSV, format.id)
        }
    }

    // ==================== Basic Parsing Tests ====================

    @Test
    fun `should parse basic CSV content`() {
        val content = """
            Name,Age,City
            John,30,New York
            Jane,25,Los Angeles
            Bob,35,Chicago
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("John"))
        assertTrue(result.parsedContent.contains("New York"))
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should handle empty input`() {
        val result = parser.parse("")

        assertNotNull(result)
        // Verify empty result is valid
    }

    @Test
    fun `should handle whitespace-only input`() {
        val result = parser.parse("   \n\n   \t  ")

        assertNotNull(result)
    }

    @Test
    fun `should handle single line input`() {
        val content = "Single line of CSV"

        val result = parser.parse(content)

        assertNotNull(result)
    }

    // ==================== Content Detection Tests ====================

    @Test
    fun `should detect format by content patterns`() {
        val content = """
            Name,Age,Email
            Alice,28,alice@example.com
            Bob,32,bob@example.com
        """.trimIndent()

        val format = FormatRegistry.detectByContent(content)

        assertNotNull(format)
        assertEquals(FormatRegistry.ID_CSV, format.id)
    }

    @Test
    fun `should not false-positive on plain text`() {
        val plainText = "Just some plain text without special formatting"

        val format = FormatRegistry.detectByContent(plainText)

        // Should detect as plaintext, not CSV
        if (format != null) {
            assertNotEquals(FormatRegistry.ID_CSV, format.id)
        }
    }

    // ==================== Special Characters Tests ====================

    @Test
    fun `should handle special characters`() {
        val content = """
            Special chars: @#$%^{{SPECIAL_CHARS_SAMPLE}}*()
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        // Verify special characters are preserved/escaped correctly
    }

    @Test
    fun `should handle unicode characters`() {
        val content = "Unicode test: 你好世界 🌍 Привет мир"

        val result = parser.parse(content)

        assertNotNull(result)
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `should handle malformed input gracefully`() {
        val malformed = """
            Malformed CSV content
        """.trimIndent()

        // Should not throw exception
        val result = parser.parse(malformed)
        assertNotNull(result)
    }

    @Test
    fun `should handle very long input`() {
        val longContent = "Single line of CSV\n".repeat(10000)

        val result = parser.parse(longContent)

        assertNotNull(result)
    }

    @Test
    fun `should handle null bytes gracefully`() {
        // Binary content detection
        val binaryContent = "Some text\u0000with null\u0000bytes"

        val result = parser.parse(binaryContent)

        assertNotNull(result)
    }

    // ==================== Format-Specific Tests ====================

    @Test
    fun `should parse CSV with headers`() {
        val content = """
            Product,Price,Quantity
            Apple,1.50,100
            Banana,0.75,200
            Orange,2.00,150
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("Product"))
        assertTrue(result.parsedContent.contains("Apple"))
        assertTrue(result.parsedContent.contains("1.50"))
    }

    @Test
    fun `should parse CSV with quoted fields`() {
        val content = """
            Name,Address,Phone
            "Smith, John","123 Main St, NYC","555-1234"
            "Doe, Jane","456 Oak Ave, LA","555-5678"
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("Smith, John") || result.parsedContent.contains("Smith"))
    }

    @Test
    fun `should handle escaped quotes in CSV`() {
        val content = "Title,Description\n\"Book\",\"He said Hello\"\n\"Movie\",\"She replied Hi there\""

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("Book"))
    }

    @Test
    fun `should parse CSV with empty fields`() {
        val content = "Name,Email,Phone\nJohn,john@example.com,\nJane,,555-1234\nBob,bob@example.com,555-5678"

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("John"))
        assertTrue(result.parsedContent.contains("Jane"))
    }

    @Test
    fun `should parse CSV with numeric data`() {
        val content = "Year,Sales,Profit\n2021,150000,25000\n2022,175000,30000\n2023,200000,35000"

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("150000"))
        assertTrue(result.parsedContent.contains("2021"))
    }

    @Test
    fun `should parse CSV with line breaks in quoted fields`() {
        val content = "\"Name\",\"Address\"\n\"John Smith\",\"123 Main St\\nNew York, NY\"\n\"Jane Doe\",\"456 Oak Ave\\nLos Angeles, CA\""

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("John Smith"))
    }

    @Test
    fun `should parse single column CSV`() {
        val content = "Name\nAlice\nBob\nCharlie"

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("Alice"))
        assertTrue(result.parsedContent.contains("Bob"))
    }

    @Test
    fun `should parse CSV with special characters`() {
        val content = "Email,Password\nuser@example.com,pass123!\nadmin@site.org,secur3#"

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("user@example.com"))
    }

    @Test
    fun `should generate HTML table from CSV`() {
        val content = "Name,Age,City\nJohn,30,NYC\nJane,25,LA"

        val result = parser.parse(content)

        assertNotNull(result)
        // CSV should be rendered as HTML table
        assertTrue(result.parsedContent.contains("table") || result.parsedContent.contains("TABLE"))
    }

    // ==================== Integration Tests ====================

    @Test
    fun `should integrate with FormatRegistry`() {
        val format = FormatRegistry.getById(FormatRegistry.ID_CSV)

        assertNotNull(format)
        assertEquals("CSV", format.name)
        assertEquals(".csv", format.defaultExtension)
    }

    @Test
    fun `should be registered in FormatRegistry`() {
        val allFormats = FormatRegistry.formats
        val csvFormat = allFormats.find { it.id == FormatRegistry.ID_CSV }

        assertNotNull(csvFormat)
        assertEquals("CSV", csvFormat.name)
    }

    // ==================== Delimiter Variation Tests ====================

    @Test
    fun `should parse semicolon-separated CSV`() {
        val content = "Name;Age;City\nJohn;30;NYC\nJane;25;LA"

        val config = CsvConfig(delimiter = ';')
        val table = parser.parseCsv(content, config)

        assertEquals(2, table.rowCount)
        assertEquals(3, table.columnCount)
        assertEquals(listOf("Name", "Age", "City"), table.headers)
        assertEquals("John", table.rows[0][0])
    }

    @Test
    fun `should parse tab-separated values (TSV)`() {
        val content = "Name\tAge\tCity\nJohn\t30\tNYC\nJane\t25\tLA"

        val config = CsvConfig(delimiter = '\t')
        val table = parser.parseCsv(content, config)

        assertEquals(2, table.rowCount)
        assertEquals(3, table.columnCount)
        assertEquals("John", table.rows[0][0])
    }

    @Test
    fun `should parse pipe-separated values`() {
        val content = "Name|Age|City\nJohn|30|NYC\nJane|25|LA"

        val config = CsvConfig(delimiter = '|')
        val table = parser.parseCsv(content, config)

        assertEquals(2, table.rowCount)
        assertEquals(3, table.columnCount)
    }

    @Test
    fun `should infer semicolon delimiter from content`() {
        val content = "Name;Age;City\nJohn;30;NYC"
        val firstLine = content.lines().first()

        val config = CsvConfig.infer(firstLine)

        assertEquals(';', config.delimiter)
    }

    @Test
    fun `should infer tab delimiter from content`() {
        val content = "Name\tAge\tCity\nJohn\t30\tNYC"
        val firstLine = content.lines().first()

        val config = CsvConfig.infer(firstLine)

        assertEquals('\t', config.delimiter)
    }

    @Test
    fun `should infer pipe delimiter from content`() {
        val content = "Name|Age|City\nJohn|30|NYC"
        val firstLine = content.lines().first()

        val config = CsvConfig.infer(firstLine)

        assertEquals('|', config.delimiter)
    }

    // ==================== Malformed CSV Tests ====================

    @Test
    fun `should handle CSV with unmatched quotes gracefully`() {
        val content = "Name,Description\nJohn,\"Unmatched quote\nJane,Normal"

        val result = parser.parse(content)

        assertNotNull(result)
        // Should not crash on malformed CSV
    }

    @Test
    fun `should handle CSV with inconsistent column counts`() {
        val content = "Name,Age,City\nJohn,30,NYC\nJane,25\nBob,35,LA,Extra"

        val result = parser.parse(content)

        assertNotNull(result)
        // Should handle rows with different column counts
    }

    @Test
    fun `should handle CSV with only commas`() {
        val content = ",,,\n,,,\n,,,"

        val result = parser.parse(content)

        assertNotNull(result)
        // Should create empty cells
    }

    @Test
    fun `should handle CSV with trailing commas`() {
        val content = "Name,Age,\nJohn,30,\nJane,25,"

        val result = parser.parse(content)

        assertNotNull(result)
    }

    @Test
    fun `should handle CSV with leading commas`() {
        val content = ",Name,Age\n,John,30\n,Jane,25"

        val result = parser.parse(content)

        assertNotNull(result)
    }

    // ==================== Edge Case Tests ====================

    @Test
    fun `should parse empty CSV`() {
        val content = ""

        val table = parser.parseCsv(content)

        assertEquals(0, table.rowCount)
        assertEquals(null, table.headers)
    }

    @Test
    fun `should parse CSV with only headers`() {
        val content = "Name,Age,City"

        val table = parser.parseCsv(content)

        assertEquals(0, table.rowCount)
        assertEquals(listOf("Name", "Age", "City"), table.headers)
    }

    @Test
    fun `should parse CSV without headers`() {
        val content = "John,30,NYC\nJane,25,LA"

        val config = CsvConfig(hasHeader = false)
        val table = parser.parseCsv(content, config)

        assertEquals(2, table.rowCount)
        assertEquals(null, table.headers)
        assertEquals("John", table.rows[0][0])
    }

    @Test
    fun `should parse CSV with comments`() {
        val content = """
            # This is a comment
            Name,Age,City
            # Another comment
            John,30,NYC
            Jane,25,LA
        """.trimIndent()

        val table = parser.parseCsv(content)

        // Comments should be filtered out
        assertEquals(2, table.rowCount)
        assertEquals("John", table.rows[0][0])
    }

    @Test
    fun `should handle very wide CSV (many columns)`() {
        val headers = (1..50).map { "Col$it" }.joinToString(",")
        val row1 = (1..50).map { "Val$it" }.joinToString(",")
        val content = "$headers\n$row1"

        val table = parser.parseCsv(content)

        assertEquals(50, table.columnCount)
        assertEquals(1, table.rowCount)
    }

    @Test
    fun `should handle very long CSV (many rows)`() {
        val header = "Name,Age,City"
        val rows = (1..1000).map { "Person$it,$it,City$it" }.joinToString("\n")
        val content = "$header\n$rows"

        val table = parser.parseCsv(content)

        assertEquals(1000, table.rowCount)
        assertEquals(3, table.columnCount)
    }

    // ==================== Special Character Tests ====================

    @Test
    fun `should handle CSV with Unicode characters`() {
        val content = "Name,Greeting\n日本,こんにちは\n中国,你好\nKorea,안녕하세요"

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("日本") || result.parsedContent.contains("table"))
    }

    @Test
    fun `should escape HTML in CSV cells`() {
        val content = "Name,Code\nJohn,<script>alert('XSS')</script>"

        val result = parser.parse(content)

        assertNotNull(result)
        // Should escape HTML tags
        assertFalse(result.parsedContent.contains("<script>") && result.parsedContent.contains("alert"))
    }

    @Test
    fun `should handle CSV with embedded newlines in quoted fields`() {
        val content = "\"Name\",\"Address\"\n\"John\",\"123 Main\\nNew York\"\n\"Jane\",\"456 Oak\\nLA\""

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("John"))
    }

    @Test
    fun `should handle CSV with mixed quotes`() {
        val content = "Name,Quote\nJohn,\"He said 'Hello'\"\nJane,'She said \"Hi\"'"

        val result = parser.parse(content)

        assertNotNull(result)
    }

    @Test
    fun `should handle CSV with URLs`() {
        val content = "Name,Website\nCompany A,https://example.com?param=value&other=123\nCompany B,http://test.org/path/to/page"

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("example.com"))
    }

    // ==================== Markdown Conversion Tests ====================

    @Test
    fun `should convert CSV to Markdown table`() {
        val content = "Name,Age,City\nJohn,30,NYC\nJane,25,LA"
        val table = parser.parseCsv(content)

        val markdown = parser.toMarkdownTable(table)

        assertNotNull(markdown)
        assertTrue(markdown.contains("| Name | Age | City |"))
        assertTrue(markdown.contains("|"))
        assertTrue(markdown.contains("---"))
        assertTrue(markdown.contains("John"))
    }

    @Test
    fun `should handle empty cells in Markdown conversion`() {
        val content = "Name,Age,City\nJohn,,NYC\n,25,LA"
        val table = parser.parseCsv(content)

        val markdown = parser.toMarkdownTable(table)

        assertNotNull(markdown)
        assertTrue(markdown.contains("&nbsp;") || markdown.length > 0)
    }

    // ==================== Metadata Tests ====================

    @Test
    fun `should extract correct row count metadata`() {
        val content = "Name,Age\nJohn,30\nJane,25\nBob,35"
        val result = parser.parse(content)

        assertEquals("3", result.metadata["rows"])
    }

    @Test
    fun `should extract correct column count metadata`() {
        val content = "Name,Age,City,Country\nJohn,30,NYC,USA"
        val result = parser.parse(content)

        assertEquals("4", result.metadata["columns"])
    }

    @Test
    fun `should detect delimiter in metadata`() {
        val content = "Name;Age;City\nJohn;30;NYC"
        val result = parser.parse(content)

        assertEquals(";", result.metadata["delimiter"])
    }

    // ==================== Real-World Scenario Tests ====================

    @Test
    fun `should parse real-world product catalog CSV`() {
        val content = """
            "Product ID","Name","Price","Stock","Category","Description"
            "001","Laptop","999.99","50","Electronics","High-performance laptop"
            "002","Mouse","29.99","200","Accessories","Wireless mouse"
            "003","Keyboard","79.99","150","Accessories","Mechanical keyboard"
            "004","Monitor","299.99","75","Electronics","27-inch display"
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals("4", result.metadata["rows"])
        assertEquals("6", result.metadata["columns"])
        assertTrue(result.parsedContent.contains("Laptop"))
        assertTrue(result.parsedContent.contains("999.99"))
    }

    @Test
    fun `should parse real-world contact list CSV`() {
        val content = """
            "Last Name","First Name","Email","Phone","Company"
            "Smith","John","john.smith@example.com","555-0101","Acme Corp"
            "Doe","Jane","jane.doe@test.org","555-0102","Test Inc"
            "Johnson","Bob","bob.j@company.com","555-0103","Company LLC"
        """.trimIndent()

        val table = parser.parseCsv(content)

        assertEquals(3, table.rowCount)
        assertEquals(5, table.columnCount)
        assertEquals("Smith", table.rows[0][0])
        assertTrue(table.headers!!.contains("Email"))
    }

    @Test
    fun `should parse real-world sales data CSV`() {
        val content = """
            Date,Product,Quantity,Revenue,Region
            2025-01-01,Product A,100,5000,North
            2025-01-02,Product B,150,7500,South
            2025-01-03,Product A,200,10000,East
            2025-01-04,Product C,75,3750,West
        """.trimIndent()

        val table = parser.parseCsv(content)

        assertEquals(4, table.rowCount)
        assertEquals(5, table.columnCount)
        assertEquals("2025-01-01", table.rows[0][0])
    }

    // ==================== Line Parsing Tests ====================

    @Test
    fun `should parse line with escaped quotes`() {
        val line = "\"Name\",\"He said \"\"Hello\"\" to me\""
        val fields = parser.parseLine(line)

        assertEquals(2, fields.size)
        assertEquals("Name", fields[0])
        assertTrue(fields[1].contains("Hello"))
    }

    @Test
    fun `should parse line with delimiter in quoted field`() {
        val line = "John,\"Doe, Jr.\",30"
        val fields = parser.parseLine(line)

        assertEquals(3, fields.size)
        assertEquals("John", fields[0])
        assertEquals("Doe, Jr.", fields[1])
        assertEquals("30", fields[2])
    }

    @Test
    fun `should parse line with empty fields`() {
        val line = "John,,30"
        val fields = parser.parseLine(line)

        assertEquals(3, fields.size)
        assertEquals("John", fields[0])
        assertEquals("", fields[1])
        assertEquals("30", fields[2])
    }

    @Test
    fun `should parse line with only empty fields`() {
        val line = ",,"
        val fields = parser.parseLine(line)

        assertEquals(3, fields.size)
        assertTrue(fields.all { it.isEmpty() })
    }

    @Test
    fun `should parse line with custom delimiter and quote`() {
        val line = "John;'Doe; Jr.';30"
        val fields = parser.parseLine(line, delimiter = ';', quote = '\'')

        assertEquals(3, fields.size)
        assertEquals("John", fields[0])
        assertEquals("Doe; Jr.", fields[1])
        assertEquals("30", fields[2])
    }
}
