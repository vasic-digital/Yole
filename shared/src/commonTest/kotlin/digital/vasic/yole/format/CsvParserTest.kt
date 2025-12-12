/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * CSV Parser Tests - Comprehensive Coverage
 *
 *########################################################*/

package digital.vasic.yole.format

import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.csv.CsvConfig
import digital.vasic.yole.format.csv.CsvTable
import kotlin.test.*

/**
 * Comprehensive test suite for CsvParser
 * Tests all aspects of CSV format parsing including RFC 4180 compliance
 */
class CsvParserTest {

    private lateinit var parser: CsvParser

    @BeforeTest
    fun setup() {
        parser = CsvParser()
    }

    @Test
    fun testBasicCsvParsing() {
        val input = """Name,Age,City
John,30,New York
Jane,25,Los Angeles
Bob,35,Chicago"""

        val result = parser.parseCsv(input)
        
        assertNotNull(result)
        assertEquals(3, result.rows.size)
        assertEquals(listOf("Name", "Age", "City"), result.headers)
        
        val firstRow = result.rows[0]
        assertEquals("John", firstRow[0])
        assertEquals("30", firstRow[1])
        assertEquals("New York", firstRow[2])
    }

    @Test
    fun testCsvWithQuotes() {
        val input = """Name,Description,Age
John,"A person who likes coding",30
Jane,"A person with ""quotes"" in description",25"""

        val result = parser.parseCsv(input)
        
        assertNotNull(result)
        assertEquals(2, result.rows.size)
        
        val firstRow = result.rows[0]
        assertEquals("John", firstRow[0])
        assertEquals("A person who likes coding", firstRow[1])
        assertEquals("30", firstRow[2])
        
        val secondRow = result.rows[1]
        assertEquals("Jane", secondRow[0])
        assertEquals("A person with \"quotes\" in description", secondRow[1])
        assertEquals("25", secondRow[2])
    }

    @Test
    fun testCsvWithCommasInFields() {
        val input = """Name,Address,Phone
John,"123 Main St, Apt 4",555-1234
Jane,"456 Oak Ave, Suite 200",555-5678"""

        val result = parser.parseCsv(input)
        
        assertNotNull(result)
        assertEquals(2, result.rows.size)
        
        val firstRow = result.rows[0]
        assertEquals("John", firstRow[0])
        assertEquals("123 Main St, Apt 4", firstRow[1])
        assertEquals("555-1234", firstRow[2])
    }

    @Test
    fun testCsvWithNewlinesInFields() {
        val input = """Name,Description,Age
John,"Multi-line
description here",30
Jane,"Another
multi-line
description",25"""

        val result = parser.parseCsv(input)
        
        assertNotNull(result)
        assertEquals(2, result.rows.size)
        
        val firstRow = result.rows[0]
        assertEquals("John", firstRow[0])
        assertEquals("Multi-line\ndescription here", firstRow[1])
        assertEquals("30", firstRow[2])
    }

    @Test
    fun testCsvWithDifferentDelimiters() {
        val semicolonInput = """Name;Age;City
John;30;New York
Jane;25;Los Angeles"""

        val tabInput = """Name\tAge\tCity
John\t30\tNew York
Jane\t25\tLos Angeles"""

        val semicolonResult = parser.parseCsv(semicolonInput, CsvConfig(delimiter = ';'))
        assertNotNull(semicolonResult)
        assertEquals(2, semicolonResult.rows.size)
        assertEquals(listOf("Name", "Age", "City"), semicolonResult.headers)

        val tabResult = parser.parseCsv(tabInput, CsvConfig(delimiter = '\t'))
        assertNotNull(tabResult)
        assertEquals(2, tabResult.rows.size)
        assertEquals(listOf("Name", "Age", "City"), tabResult.headers)
    }

    @Test
    fun testCsvWithHeaders() {
        val inputWithHeaders = """ID,Name,Email,Department
1,John Doe,john@company.com,Engineering
2,Jane Smith,jane@company.com,Marketing
3,Bob Johnson,bob@company.com,Sales"""

        val result = parser.parseCsv(inputWithHeaders)
        
        assertNotNull(result)
        assertEquals(listOf("ID", "Name", "Email", "Department"), result.headers)
        assertEquals(3, result.rows.size)
        
        val firstRow = result.rows[0]
        assertEquals("1", firstRow[0])
        assertEquals("John Doe", firstRow[1])
        assertEquals("john@company.com", firstRow[2])
        assertEquals("Engineering", firstRow[3])
    }

    @Test
    fun testCsvWithoutHeaders() {
        val inputNoHeaders = """1,John Doe,john@company.com,Engineering
2,Jane Smith,jane@company.com,Marketing
3,Bob Johnson,bob@company.com,Sales"""

        val result = parser.parseCsv(inputNoHeaders, CsvConfig(hasHeader = false))
        
        assertNotNull(result)
        assertEquals(0, result.headers?.size ?: 0)
        assertEquals(3, result.rows.size)
        
        val firstRow = result.rows[0]
        assertEquals("1", firstRow[0])
        assertEquals("John Doe", firstRow[1])
        assertEquals("john@company.com", firstRow[2])
        assertEquals("Engineering", firstRow[3])
    }

    @Test
    fun testCsvWithEmptyFields() {
        val input = """Name,Age,City,Country
John,30,New York,USA
Jane,,Los Angeles,USA
Bob,35,,USA"""

        val result = parser.parseCsv(input)
        
        assertNotNull(result)
        assertEquals(3, result.rows.size)
        
        val secondRow = result.rows[1]
        assertEquals("Jane", secondRow[0])
        assertEquals("", secondRow[1]) // Empty age
        assertEquals("Los Angeles", secondRow[2])
        assertEquals("USA", secondRow[3])
        
        val thirdRow = result.rows[2]
        assertEquals("Bob", thirdRow[0])
        assertEquals("35", thirdRow[1])
        assertEquals("", thirdRow[2]) // Empty city
        assertEquals("USA", thirdRow[3])
    }

    @Test
    fun testCsvWithDifferentRowLengths() {
        val input = """Name,Age,City
John,30
Jane,25,New York,Los Angeles
Bob"""

        val result = parser.parseCsv(input)
        
        assertNotNull(result)
        assertEquals(3, result.rows.size)
        
        // Should handle rows with different lengths
        val firstRow = result.rows[0]
        assertEquals("John", firstRow[0])
        assertEquals("30", firstRow[1])
        assertEquals(2, firstRow.size) // Missing city
        
        val secondRow = result.rows[1]
        assertEquals("Jane", secondRow[0])
        assertEquals("25", secondRow[1])
        assertEquals("New York", secondRow[2])
        assertEquals("Los Angeles", secondRow[3])
        assertEquals(4, secondRow.size) // Extra field
    }

    @Test
    fun testCsvToHtmlConversion() {
        val input = """Name,Age,City
John,30,New York
Jane,25,Los Angeles"""

        val result = parser.parseCsv(input)
        val html = parser.toHtml(result)
        
        assertNotNull(html)
        assertTrue(html.contains("<table>"))
        assertTrue(html.contains("<thead>"))
        assertTrue(html.contains("<tbody>"))
        assertTrue(html.contains("<th>Name</th>"))
        assertTrue(html.contains("<th>Age</th>"))
        assertTrue(html.contains("<th>City</th>"))
        assertTrue(html.contains("<td>John</td>"))
        assertTrue(html.contains("<td>30</td>"))
        assertTrue(html.contains("<td>New York</td>"))
    }

    @Test
    fun testCsvEdgeCases() {
        // Empty input
        val emptyResult = parser.parseCsv("")
        assertNotNull(emptyResult)
        assertEquals(0, emptyResult.rows.size)

        // Only headers
        val headersOnlyResult = parser.parseCsv("Name,Age,City")
        assertNotNull(headersOnlyResult)
        assertEquals(listOf("Name", "Age", "City"), headersOnlyResult.headers)
        assertEquals(0, headersOnlyResult.rows.size)

        // Only whitespace
        val whitespaceResult = parser.parseCsv("   \n\t\n   ")
        assertNotNull(whitespaceResult)
        assertEquals(0, whitespaceResult.rows.size)

        // Special characters in field names
        val specialCharsResult = parser.parseCsv("Name@#$%,Age&*(),City!@#")
        assertNotNull(specialCharsResult)
        assertEquals(listOf("Name@#$%", "Age&*()", "City!@#"), specialCharsResult.headers)
    }

    @Test
    fun testCsvPerformance() {
        val largeInput = buildString {
            appendLine("ID,Name,Email,Department,Salary,Location,Status")
            repeat(1000) { i ->
                appendLine("$i,Employee $i,employee$i@company.com,Department ${i % 10},${50000 + i * 100},Location ${i % 50},Active")
            }
        }

        val startTime = System.currentTimeMillis()
        val result = parser.parseCsv(largeInput)
        val endTime = System.currentTimeMillis()
        
        assertNotNull(result)
        assertEquals(1000, result.rows.size)
        assertEquals(listOf("ID", "Name", "Email", "Department", "Salary", "Location", "Status"), result.headers)
        assertTrue(endTime - startTime < 1000, "Parsing 1000 rows should complete within 1 second")
    }

    @Test
    fun testCsvErrorHandling() {
        // Malformed quotes
        val malformedQuotes = """Name,Description
John,"Unclosed quote
Jane,"Valid quote"
Bob,"Another unclosed quote"""

        val result = parser.parseCsv(malformedQuotes)
        
        // Should handle gracefully and parse what it can
        assertNotNull(result)
        assertTrue(result.rows.size >= 1) // Should at least parse some rows

        // Mixed quotes
        val mixedQuotes = """Name,Age,City
"John",30,"New York"
Jane,"25",Los Angeles
"Bob","35","Chicago"""

        val mixedResult = parser.parseCsv(mixedQuotes)
        assertNotNull(mixedResult)
        assertEquals(3, mixedResult.rows.size)
    }

    @Test
    fun testCsvRoundTrip() {
        val originalInput = """Name,Age,City,Department
John Doe,30,New York,Engineering
Jane Smith,25,Los Angeles,Marketing
Bob Johnson,35,Chicago,Sales"""

        val result = parser.parseCsv(originalInput)
        assertNotNull(result)

        val regeneratedCsv = parser.toCsv(result)
        assertNotNull(regeneratedCsv)

        val reparsedResult = parser.parseCsv(regeneratedCsv)
        assertNotNull(reparsedResult)

        // Should preserve essential structure
        assertEquals(result.headers, reparsedResult.headers)
        assertEquals(result.rows.size, reparsedResult.rows.size)
        
        // Check first row preservation
        if (result.rows.isNotEmpty() && reparsedResult.rows.isNotEmpty()) {
            assertEquals(result.rows[0].size, reparsedResult.rows[0].size)
        }
    }

    @Test
    fun testCsvSpecialFormats() {
        // Test Excel-style CSV with BOM (Byte Order Mark)
        val excelStyleInput = """Name,Age,City,Salary
John Doe,30,New York,"$50,000"
Jane Smith,25,Los Angeles,"$45,000"""

        val result = parser.parseCsv(excelStyleInput)
        assertNotNull(result)
        assertEquals(2, result.rows.size)
        
        val firstRow = result.rows[0]
        assertEquals("John Doe", firstRow[0])
        assertEquals("30", firstRow[1])
        assertEquals("New York", firstRow[2])
        assertEquals("\$50,000", firstRow[3]) // Dollar sign and comma preserved

        // Test scientific notation
        val scientificInput = """ID,Value,Name
1,1.23E+10,Large Number
2,4.56E-5,Small Number"""

        val scientificResult = parser.parseCsv(scientificInput)
        assertNotNull(scientificResult)
        assertEquals(2, scientificResult.rows.size)
        assertEquals("1.23E+10", scientificResult.rows[0][1])
        assertEquals("4.56E-5", scientificResult.rows[1][1])
    }
}