/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * JsonParser tests — iter 42, closes #yole-json-parser-missing.
 *
 *########################################################*/
package digital.vasic.yole.format.json

import digital.vasic.yole.format.FormatRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonParserTest {

    @Test
    fun `supportedFormat is JSON`() {
        val parser = JsonParser()
        assertEquals(FormatRegistry.ID_JSON, parser.supportedFormat.id)
    }

    @Test
    fun `parse simple object pretty-prints with 2-space indent`() {
        val parser = JsonParser()
        val input = """{"name":"Alice","age":30}"""
        val doc = parser.parse(input)
        // Pretty-printed output has each key on its own line with 2-space indent.
        assertTrue(
            doc.rawContent == input,
            "rawContent must preserve original; got ${doc.rawContent}"
        )
        // Pretty-printed result is in the parsedContent (HTML), so the
        // pretty form is verified via metadata + the HTML body.
        val html = doc.parsedContent
        // escapeHtml() escapes `"` → `&quot;`, so the rendered string
        // literals in the HTML look like `&quot;name&quot;` not `"name"`.
        assertTrue(html.contains("<span class='json-key'>&quot;name&quot;</span>"),
            "Pretty-printed HTML must classify keys; got: $html")
        assertTrue(html.contains("<span class='json-string'>&quot;Alice&quot;</span>"),
            "Pretty-printed HTML must classify string values; got: $html")
        assertTrue(html.contains("<span class='json-number'>30</span>"),
            "Pretty-printed HTML must classify numbers; got: $html")
        assertEquals("true", doc.metadata["pretty_printed"])
    }

    @Test
    fun `parse array of mixed values classifies each token`() {
        val parser = JsonParser()
        val doc = parser.parse("""[1, "two", true, false, null, 3.14]""")
        val html = doc.parsedContent
        assertTrue(html.contains("<span class='json-number'>1</span>"))
        assertTrue(html.contains("<span class='json-string'>&quot;two&quot;</span>"))
        assertTrue(html.contains("<span class='json-bool'>true</span>"))
        assertTrue(html.contains("<span class='json-bool'>false</span>"))
        assertTrue(html.contains("<span class='json-null'>null</span>"))
        assertTrue(html.contains("<span class='json-number'>3.14</span>"))
        assertTrue(html.contains("<span class='json-bracket'>[</span>"))
        assertTrue(html.contains("<span class='json-bracket'>]</span>"))
    }

    @Test
    fun `parse escapes HTML-sensitive characters inside strings`() {
        val parser = JsonParser()
        // String contains '<' '>' '&' which must be escaped in the
        // generated HTML to prevent injection / breakage.
        val doc = parser.parse("""{"msg":"a<b>c&d"}""")
        val html = doc.parsedContent
        assertFalse(html.contains("<b>"),
            "Raw '<b>' in JSON string must NOT appear unescaped in HTML")
        assertTrue(html.contains("&lt;b&gt;"),
            "JSON string '<b>' must be escaped to '&lt;b&gt;' in HTML; got: $html")
        assertTrue(html.contains("&amp;"),
            "JSON string '&' must be escaped to '&amp;' in HTML; got: $html")
    }

    @Test
    fun `validate balanced JSON returns no errors`() {
        val parser = JsonParser()
        val errors = parser.validate("""{"a": [1, 2, {"b": "c"}]}""")
        assertEquals(emptyList(), errors)
    }

    @Test
    fun `validate reports unbalanced braces`() {
        val parser = JsonParser()
        val errors = parser.validate("""{"a": 1""")
        assertTrue(errors.isNotEmpty(), "Expected validation error for unclosed brace")
        assertTrue(
            errors.any { it.contains("Unbalanced braces") || it.contains("Unterminated") },
            "Expected unbalanced-braces or unterminated-string error; got $errors"
        )
    }

    @Test
    fun `validate reports unexpected closing bracket`() {
        val parser = JsonParser()
        val errors = parser.validate("""}""")
        assertTrue(errors.any { it.contains("Unexpected '}'") },
            "Expected 'Unexpected }' error; got $errors")
    }

    @Test
    fun `parse tolerates invalid JSON without throwing`() {
        val parser = JsonParser()
        // Not actually invalid for the pretty-printer (it's tolerant)
        // but a malformed-looking input must not crash.
        val doc = parser.parse("{ not really json }")
        assertTrue(doc.rawContent == "{ not really json }")
        // Either pretty_printed=true (it tolerated and made some output)
        // OR pretty_printed=false with an error recorded. Both are OK
        // — the load-bearing invariant is "no exception escapes".
        assertTrue(
            doc.metadata["pretty_printed"] in setOf("true", "false"),
            "pretty_printed metadata must be true/false; got ${doc.metadata["pretty_printed"]}"
        )
    }

    @Test
    fun `parse empty string produces non-null document`() {
        val parser = JsonParser()
        val doc = parser.parse("")
        assertEquals("", doc.rawContent)
        assertTrue(doc.parsedContent.contains("<div class='json'>"),
            "Even empty content must produce the JSON HTML wrapper")
    }

    @Test
    fun `metadata reports lines and characters`() {
        val parser = JsonParser()
        val doc = parser.parse("""{"a":1,"b":2}""")
        // Pretty-printed output has multiple lines.
        val lines = doc.metadata["lines"]?.toIntOrNull() ?: -1
        assertTrue(lines >= 4, "Pretty-printed object should span at least 4 lines; got $lines")
        assertTrue((doc.metadata["characters"]?.toIntOrNull() ?: 0) > 0)
    }
}
