/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58 Phase 1: stub. Phase 2 adds computeIndent() helper.
 *#######################################################*/
package digital.vasic.yole.language.affordance

data class IndentRules(
    val indentTokens: Set<String> = setOf("{", "(", "["),
    val dedentTokens: Set<String> = setOf("}", ")", "]"),
)
