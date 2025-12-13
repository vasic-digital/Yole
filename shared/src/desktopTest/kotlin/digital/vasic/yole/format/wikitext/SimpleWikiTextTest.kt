/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Simple test for WikiText parser
 *
 *########################################################*/
package digital.vasic.yole.format.wikitext

import digital.vasic.yole.format.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SimpleWikiTextTest {

    @Test
    fun `test basic WikiText parsing`() {
        val parser = WikitextParser()
        val content = """
            = Main Title =
            
            This is a paragraph.
            
            == Section ==
            
            * Item 1
            * Item 2
        """.trimIndent()
        
        val result = parser.parse(content)
        
        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }
}