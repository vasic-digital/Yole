/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58 Phase 1: stub. Phase 2 adds toggleLine() and other helpers.
 *#######################################################*/
package digital.vasic.yole.language.affordance

data class CommentSyntax(
    val lineComment: String? = null,
    val blockComment: Pair<String, String>? = null,
)
