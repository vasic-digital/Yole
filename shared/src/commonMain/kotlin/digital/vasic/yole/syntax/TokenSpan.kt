/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 8: AnnotatedString-ready styled range.
 *   TokenSpan = (byte range, ARGB color int).
 *   A SyntaxHighlighter consumer wraps the color in
 *   androidx.compose.ui.graphics.Color when building a Compose
 *   AnnotatedString. The decoupling lets the data type stay
 *   Compose-free for callers that don't need Compose (e.g., headless
 *   tests, Wasm static export, future LSP server).
 *
 *########################################################*/
package digital.vasic.yole.syntax

/**
 * A single styled run in a tokenized document.
 *
 * @property startByte UTF-8 byte offset of the span start (inclusive).
 * @property endByte UTF-8 byte offset of the span end (exclusive).
 * @property color ARGB int (0xAARRGGBB). Consumer wraps in
 *   `androidx.compose.ui.graphics.Color(int)`.
 */
data class TokenSpan(
    val startByte: Int,
    val endByte: Int,
    val color: Int,
)
