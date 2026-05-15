/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58 Phase 2: anti-bluff BracketPairs.closerFor tests.
 *#######################################################*/
package digital.vasic.yole.language

import digital.vasic.yole.language.affordance.BracketPairs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BracketPairsTest {
    @Test
    fun closerFor_returnsMatchingCloser() {
        val pairs = BracketPairs()
        assertEquals(')', pairs.closerFor('('))
        assertEquals(']', pairs.closerFor('['))
        assertEquals('}', pairs.closerFor('{'))
        assertEquals('"', pairs.closerFor('"'))
    }

    @Test
    fun closerFor_returnsNullForNonOpener() {
        val pairs = BracketPairs()
        assertNull(pairs.closerFor('x'))
        assertNull(pairs.closerFor(')'))
    }

    @Test
    fun custom_pairsList() {
        val custom = BracketPairs(pairs = listOf('<' to '>'))
        assertEquals('>', custom.closerFor('<'))
        assertNull(custom.closerFor('('))
    }
}
