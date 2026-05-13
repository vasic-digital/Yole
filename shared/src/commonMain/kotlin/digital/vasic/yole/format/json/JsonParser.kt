/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * JSON Format Parser - Platform Agnostic
 *
 * Closes #yole-json-parser-missing (opened iter 39 by
 * IntegrationTest.testParserRegistryCompleteness). Before this
 * file, FormatRegistry advertised JSON as a TextFormat but
 * ParserInitializer registered no JsonParser, so users tapping a
 * `.json` file got Plain-Text rendering with no JSON awareness.
 *
 *########################################################*/
package digital.vasic.yole.format.json

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParsedDocument
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.TextFormat
import digital.vasic.yole.format.TextParser
import digital.vasic.yole.format.escapeHtml

/**
 * JSON format parser.
 *
 * Provides JSON-aware pretty-printing + HTML rendering with simple
 * token-class styling (keys, strings, numbers, booleans, brackets).
 * Tolerates invalid JSON: if pretty-printing fails, the parser falls
 * back to rendering the raw content inside a `pre` block plus an
 * entry in `errors`. This honours the CONST-035 rule that a parser
 * must always produce something the user can read — never throw.
 */
class JsonParser : TextParser {
    override val supportedFormat: TextFormat
        get() = FormatRegistry.getById(FormatRegistry.ID_JSON)
            ?: FormatRegistry.formats.last()

    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        val errors = mutableListOf<String>()

        val pretty = try {
            prettyPrintJson(content)
        } catch (t: Throwable) {
            errors.add("JSON pretty-print failed: ${t.message ?: t::class.simpleName}")
            content
        }

        val html = buildHtml(pretty, errors)

        val metadata = buildMap {
            put("lines", pretty.lines().size.toString())
            put("characters", pretty.length.toString())
            put("pretty_printed", (errors.isEmpty()).toString())
        }

        return ParsedDocument(
            format = supportedFormat,
            rawContent = content,
            parsedContent = html,
            metadata = metadata,
            errors = errors
        )
    }

    override fun toHtml(document: ParsedDocument, lightMode: Boolean): String {
        return document.parsedContent
    }

    override fun validate(content: String): List<String> {
        // Minimal structural check: must have matching braces / brackets
        // OUTSIDE of string literals. Not a full JSON validator — that
        // belongs in a dedicated JSON validation pass; this is the
        // cheap "is this even close to JSON" smoke check.
        var braces = 0
        var brackets = 0
        var inString = false
        var escaped = false
        val errors = mutableListOf<String>()

        for (c in content) {
            when {
                escaped -> escaped = false
                inString && c == '\\' -> escaped = true
                c == '"' -> inString = !inString
                !inString && c == '{' -> braces++
                !inString && c == '}' -> braces--
                !inString && c == '[' -> brackets++
                !inString && c == ']' -> brackets--
            }
            if (braces < 0) {
                errors.add("Unexpected '}' (more closing than opening braces)")
                return errors
            }
            if (brackets < 0) {
                errors.add("Unexpected ']' (more closing than opening brackets)")
                return errors
            }
        }
        if (braces != 0) errors.add("Unbalanced braces: $braces unclosed")
        if (brackets != 0) errors.add("Unbalanced brackets: $brackets unclosed")
        if (inString) errors.add("Unterminated string literal")
        return errors
    }

    private fun buildHtml(pretty: String, errors: List<String>): String = buildString {
        append("<div class='json'>")
        if (errors.isNotEmpty()) {
            append("<div class='json-errors'>")
            errors.forEach { err ->
                append("<div class='json-error'>")
                append(err.escapeHtml())
                append("</div>")
            }
            append("</div>")
        }
        append("<pre><code class='language-json'>")
        append(tokenizeAndEscape(pretty))
        append("</code></pre>")
        append("</div>")
    }

    /**
     * Walk the (already pretty-printed) JSON char-by-char and wrap
     * tokens in span classes so a stylesheet can colour keys, strings,
     * numbers, booleans, and bracket delimiters distinctly. The
     * escapeHtml call is interleaved so we never produce broken HTML
     * if the input contains `<`, `>`, or `&`.
     */
    private fun tokenizeAndEscape(s: String): String = buildString {
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c == '"' -> {
                    // String literal — may be a key (followed by ':')
                    // or a value. Scan to closing quote, then peek for ':'.
                    val start = i
                    i++
                    while (i < s.length) {
                        val cc = s[i]
                        if (cc == '\\' && i + 1 < s.length) {
                            i += 2
                            continue
                        }
                        if (cc == '"') break
                        i++
                    }
                    val end = if (i < s.length) i + 1 else s.length
                    val token = s.substring(start, end)
                    // Peek past whitespace for ':'
                    var j = end
                    while (j < s.length && s[j].isWhitespace()) j++
                    val isKey = j < s.length && s[j] == ':'
                    val cls = if (isKey) "json-key" else "json-string"
                    append("<span class='$cls'>")
                    append(token.escapeHtml())
                    append("</span>")
                    i = end
                }

                c.isDigit() || (c == '-' && i + 1 < s.length && s[i + 1].isDigit()) -> {
                    // Number — scan while digit / dot / e / sign.
                    val start = i
                    if (c == '-') i++
                    while (i < s.length && (s[i].isDigit() || s[i] == '.' ||
                            s[i] == 'e' || s[i] == 'E' || s[i] == '+' || s[i] == '-')) {
                        i++
                    }
                    append("<span class='json-number'>")
                    append(s.substring(start, i).escapeHtml())
                    append("</span>")
                }

                c == 't' && s.startsWith("true", i) -> {
                    append("<span class='json-bool'>true</span>")
                    i += 4
                }
                c == 'f' && s.startsWith("false", i) -> {
                    append("<span class='json-bool'>false</span>")
                    i += 5
                }
                c == 'n' && s.startsWith("null", i) -> {
                    append("<span class='json-null'>null</span>")
                    i += 4
                }

                c == '{' || c == '}' || c == '[' || c == ']' -> {
                    append("<span class='json-bracket'>")
                    append(c)
                    append("</span>")
                    i++
                }

                else -> {
                    // Whitespace, commas, colons — pass through with escaping.
                    when (c) {
                        '<' -> append("&lt;")
                        '>' -> append("&gt;")
                        '&' -> append("&amp;")
                        else -> append(c)
                    }
                    i++
                }
            }
        }
    }

    /**
     * Pretty-print JSON content with 2-space indentation.
     *
     * Iter-42: behaviourally identical to PlaintextParser.prettyPrintJson
     * (which is private to that class and currently handles JSON files
     * routed through Plaintext detection). Duplicated here rather than
     * extracted to keep the JsonParser self-contained — a future
     * iter could extract a shared `JsonPrettyPrinter` utility once
     * both call-sites need updates in lock-step.
     */
    private fun prettyPrintJson(content: String): String {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return ""
        var indent = 0
        val output = StringBuilder()
        var inString = false
        var escaped = false

        for (char in trimmed) {
            when {
                escaped -> {
                    output.append(char)
                    escaped = false
                }
                char == '\\' && inString -> {
                    output.append(char)
                    escaped = true
                }
                char == '"' -> {
                    output.append(char)
                    inString = !inString
                }
                !inString && (char == '{' || char == '[') -> {
                    output.append(char)
                    indent++
                    output.append('\n')
                    output.append("  ".repeat(indent))
                }
                !inString && (char == '}' || char == ']') -> {
                    indent = (indent - 1).coerceAtLeast(0)
                    output.append('\n')
                    output.append("  ".repeat(indent))
                    output.append(char)
                }
                !inString && char == ',' -> {
                    output.append(char)
                    output.append('\n')
                    output.append("  ".repeat(indent))
                }
                !inString && char == ':' -> {
                    output.append(char)
                    output.append(' ')
                }
                !inString && char.isWhitespace() -> {
                    // Skip whitespace outside strings
                }
                else -> {
                    output.append(char)
                }
            }
        }
        return output.toString()
    }
}

/**
 * Register the JSON parser with the registry. Eager-instantiation path —
 * mirrors the existing registerPlaintextParser() convention.
 */
fun registerJsonParser() {
    ParserRegistry.register(JsonParser())
}
