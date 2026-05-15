/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 2: VsCodeSnippetParser unit tests.
 *
 * Anti-bluff anchor (CONST-035):
 *   Mutation procedure applied before commit —
 *   - Mutated VsCodeSnippetParser.parseBody to always return null.
 *   - Re-ran: parses_singleLineBody FAILED (returned empty list),
 *     parses_arrayBodyAsNewlineJoined FAILED (same).
 *   - Reverted mutation; all 4 tests GREEN.
 *#######################################################*/
package digital.vasic.yole.completion

import digital.vasic.yole.completion.snippet.SnippetParseException
import digital.vasic.yole.completion.snippet.VsCodeSnippetParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VsCodeSnippetParserTest {
    // Microsoft's kotlin.json schema example (string-body form).
    private val singleLineJson = """
        {
          "Hello World": {
            "prefix": "hello",
            "body": "println(\"Hello, World!\")",
            "description": "Print a greeting"
          }
        }
    """.trimIndent()

    // Array-body form — the canonical VS Code format for multi-line snippets.
    private val multiLineJson = """
        {
          "For Loop": {
            "prefix": "for",
            "body": [
              "for (i in 0 until ${'$'}{1:size}) {",
              "    ${'$'}0",
              "}"
            ],
            "description": "For loop"
          }
        }
    """.trimIndent()

    @Test
    fun parses_singleLineBody() {
        val snippets = VsCodeSnippetParser.parse(singleLineJson)
        assertEquals(1, snippets.size)
        val s = snippets.single()
        assertEquals("hello", s.prefix)
        assertTrue(s.body.contains("Hello, World!"))
        assertEquals("Print a greeting", s.description)
    }

    @Test
    fun parses_arrayBodyAsNewlineJoined() {
        val snippets = VsCodeSnippetParser.parse(multiLineJson)
        val s = snippets.single()
        assertEquals("for", s.prefix)
        assertTrue(s.body.contains("\n"), "array body must join with newlines")
        assertTrue(s.body.contains("\${1:size}"))
        assertTrue(s.body.contains("\$0"))
    }

    @Test
    fun malformed_throws() {
        assertFailsWith<SnippetParseException> {
            VsCodeSnippetParser.parse("{ not json }")
        }
    }

    @Test
    fun missingPrefix_skipped() {
        val noPrefix = """{ "Broken": { "body": "x" } }"""
        // Skip silently — VS Code does the same.
        assertEquals(0, VsCodeSnippetParser.parse(noPrefix).size)
    }
}
