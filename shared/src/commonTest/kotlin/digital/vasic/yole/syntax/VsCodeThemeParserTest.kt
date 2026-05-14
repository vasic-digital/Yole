/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 1: VsCodeThemeParser tests. Anti-bluff per CONST-035:
 *   each case asserts real parsing behavior; mutation-verified
 *   (stubbing parseHexColor() to null causes 3+ tests to FAIL).
 *
 *########################################################*/
package digital.vasic.yole.syntax

import digital.vasic.yole.syntax.theme.ThemeParseException
import digital.vasic.yole.syntax.theme.VsCodeThemeParser
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class VsCodeThemeParserTest {
    private val miniThemeJson = """
        {
          "name": "Mini Test Theme",
          "type": "dark",
          "colors": {
            "editor.background": "#1e1e1e",
            "editor.foreground": "#d4d4d4",
            "editorLineNumber.foreground": "#858585"
          },
          "tokenColors": [
            {
              "scope": "keyword",
              "settings": { "foreground": "#569cd6" }
            },
            {
              "scope": ["string", "string.quoted"],
              "settings": { "foreground": "#ce9178" }
            }
          ]
        }
    """.trimIndent()

    @Test
    fun parsesNameAndType() = runBlocking<Unit> {
        val theme = VsCodeThemeParser.parse(miniThemeJson)
        assertEquals("Mini Test Theme", theme.name)
        assertEquals("dark", theme.type)
    }

    @Test
    fun parsesUiColors() = runBlocking<Unit> {
        val theme = VsCodeThemeParser.parse(miniThemeJson)
        assertEquals(0xFF1E1E1Eu.toLong().toInt(), theme.uiColor("editor.background"))
        assertEquals(0xFFD4D4D4u.toLong().toInt(), theme.uiColor("editor.foreground"))
    }

    @Test
    fun parsesTokenColorsWithScopeArray() = runBlocking<Unit> {
        val theme = VsCodeThemeParser.parse(miniThemeJson)
        assertEquals(0xFF569CD6u.toLong().toInt(), theme.tokenColor("keyword"))
        assertEquals(0xFFCE9178u.toLong().toInt(), theme.tokenColor("string"))
        assertEquals(0xFFCE9178u.toLong().toInt(), theme.tokenColor("string.quoted"))
    }

    @Test
    fun scopeFallbackThroughHierarchy() = runBlocking<Unit> {
        val theme = VsCodeThemeParser.parse(miniThemeJson)
        // "keyword.control.return" falls back to "keyword"
        assertEquals(0xFF569CD6u.toLong().toInt(), theme.tokenColor("keyword.control.return"))
    }

    @Test
    fun unknownScopeReturnsNull() = runBlocking<Unit> {
        val theme = VsCodeThemeParser.parse(miniThemeJson)
        assertNull(theme.tokenColor("never.heard.of.this"))
    }

    @Test
    fun malformedJsonThrowsThemeParseException() {
        assertFailsWith<ThemeParseException> {
            runBlocking<Unit> { VsCodeThemeParser.parse("{ not json }") }
        }
    }

    @Test
    fun missingNameThrowsThemeParseException() {
        val noName = """{ "type": "dark", "colors": {} }"""
        assertFailsWith<ThemeParseException> {
            runBlocking<Unit> { VsCodeThemeParser.parse(noName) }
        }
    }
}
