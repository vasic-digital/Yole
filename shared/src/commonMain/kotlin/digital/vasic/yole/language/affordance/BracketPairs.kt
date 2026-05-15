/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58 Phase 2: bracket-pair lookup for auto-close.
 *#######################################################*/
package digital.vasic.yole.language.affordance

data class BracketPairs(
    val pairs: List<Pair<Char, Char>> = listOf(
        '(' to ')', '[' to ']', '{' to '}', '"' to '"', '\'' to '\'',
    ),
) {
    fun closerFor(opener: Char): Char? = pairs.firstOrNull { it.first == opener }?.second
}
