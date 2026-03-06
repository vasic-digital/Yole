/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for KeyValueParser HTML generation
 *
 *########################################################*/
package digital.vasic.yole.format.keyvalue

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KeyValueParserHtmlTest {

    private val parser = KeyValueParser()

    // ==================== Document structure ====================

    @Test
    fun testWrappedInKeyvalueDiv() {
        val doc = parser.parse("key=value")
        assertTrue(doc.parsedContent.contains("class='keyvalue'"))
    }

    @Test
    fun testContainsPreTag() {
        val doc = parser.parse("key=value")
        assertTrue(doc.parsedContent.contains("<pre"))
    }

    // ==================== Section headers ====================

    @Test
    fun testIniSectionHighlighted() {
        val doc = parser.parse("[section]")
        assertTrue(doc.parsedContent.contains("color: #ef6d00"))
    }

    @Test
    fun testIniSectionEscaped() {
        val doc = parser.parse("[section]")
        assertTrue(doc.parsedContent.contains("[section]"))
    }

    // ==================== Comments ====================

    @Test
    fun testHashComment() {
        val doc = parser.parse("# This is a comment")
        assertTrue(doc.parsedContent.contains("color: #88b04b"))
    }

    @Test
    fun testSemicolonComment() {
        val doc = parser.parse("; Another comment")
        assertTrue(doc.parsedContent.contains("color: #88b04b"))
    }

    @Test
    fun testDoubleSlashComment() {
        val doc = parser.parse("// JS-style comment")
        assertTrue(doc.parsedContent.contains("color: #88b04b"))
    }

    // ==================== Key-value pairs ====================

    @Test
    fun testKeyHighlightedBold() {
        val doc = parser.parse("mykey=myvalue")
        assertTrue(doc.parsedContent.contains("font-weight: bold"))
    }

    @Test
    fun testEqualsDelimiter() {
        val doc = parser.parse("database=postgres")
        assertTrue(doc.parsedContent.contains("database"))
        assertTrue(doc.parsedContent.contains("postgres"))
    }

    @Test
    fun testColonDelimiter() {
        val doc = parser.parse("host: localhost")
        assertTrue(doc.parsedContent.contains("host"))
        assertTrue(doc.parsedContent.contains("localhost"))
    }

    // ==================== Metadata ====================

    @Test
    fun testMetadataEntryCount() {
        val doc = parser.parse("key1=val1\nkey2=val2\nkey3=val3")
        assertEquals("3", doc.metadata["entries"])
    }

    @Test
    fun testMetadataLineCount() {
        val doc = parser.parse("key1=val1\nkey2=val2")
        assertEquals("2", doc.metadata["lines"])
    }

    @Test
    fun testMetadataSectionCount() {
        val doc = parser.parse("[section1]\nkey=val\n[section2]\nkey=val")
        assertEquals("2", doc.metadata["sections"])
    }

    @Test
    fun testMetadataTypeDefault() {
        val doc = parser.parse("key=value")
        assertEquals("generic", doc.metadata["type"])
    }

    @Test
    fun testMetadataTypeIni() {
        val doc = parser.parse("key=value", mapOf("filename" to "config.ini"))
        assertEquals("ini", doc.metadata["type"])
    }

    @Test
    fun testMetadataTypeYaml() {
        val doc = parser.parse("key: value", mapOf("filename" to "config.yaml"))
        assertEquals("yaml", doc.metadata["type"])
    }

    @Test
    fun testMetadataTypeToml() {
        val doc = parser.parse("key=value", mapOf("filename" to "config.toml"))
        assertEquals("toml", doc.metadata["type"])
    }

    @Test
    fun testMetadataTypeProperties() {
        val doc = parser.parse("key=value", mapOf("filename" to "app.properties"))
        assertEquals("properties", doc.metadata["type"])
    }

    // ==================== Type detection ====================

    @Test
    fun testDetectsVcard() {
        val doc = parser.parse("FN:John", mapOf("filename" to "contact.vcf"))
        assertEquals("vcard", doc.metadata["type"])
    }

    @Test
    fun testDetectsIcalendar() {
        val doc = parser.parse("DTSTART:20250101", mapOf("filename" to "event.ics"))
        assertEquals("icalendar", doc.metadata["type"])
    }

    // ==================== Validation ====================

    @Test
    fun testValidContent() {
        val errors = parser.validate("key=value\n# comment")
        assertTrue(errors.isEmpty())
    }

    @Test
    fun testValidSectionHeader() {
        val errors = parser.validate("[section]\nkey=value")
        assertTrue(errors.isEmpty())
    }

    @Test
    fun testInvalidLineNoSeparator() {
        val errors = parser.validate("this has no separator")
        assertTrue(errors.any { it.contains("No key-value separator") })
    }

    // ==================== HTML escaping ====================

    @Test
    fun testHtmlEscaping() {
        val doc = parser.parse("key=<script>alert(1)</script>")
        assertTrue(doc.parsedContent.contains("&lt;script&gt;"))
    }

    // ==================== Empty content ====================

    @Test
    fun testEmptyContent() {
        val doc = parser.parse("")
        assertNotNull(doc)
    }

    // ==================== toHtml pass-through ====================

    @Test
    fun testToHtmlReturnsParsedContent() {
        val doc = parser.parse("key=value")
        val html = parser.toHtml(doc, lightMode = true)
        assertEquals(doc.parsedContent, html)
    }

    // ==================== Comments not counted as entries ====================

    @Test
    fun testCommentsNotCountedAsEntries() {
        val doc = parser.parse("# comment\nkey=value\n; another comment")
        assertEquals("1", doc.metadata["entries"])
    }

    // ==================== KeyValueType enum ====================

    @Test
    fun testKeyValueTypeEntries() {
        assertEquals(9, KeyValueType.entries.size)
    }

    // ==================== KeyValueEntry data class ====================

    @Test
    fun testKeyValueEntryCreation() {
        val entry = KeyValueEntry(
            key = "name",
            value = "test",
            section = "main",
            lineNumber = 1
        )
        assertEquals("name", entry.key)
        assertEquals("test", entry.value)
        assertEquals("main", entry.section)
        assertEquals(1, entry.lineNumber)
    }
}
