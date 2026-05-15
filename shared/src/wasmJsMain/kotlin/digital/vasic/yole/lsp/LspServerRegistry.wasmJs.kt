/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 1: Wasm/JS stub — returns null since native LSP
 * server binaries cannot run inside a browser sandbox.
 * Honest empty per CONST-035; LspCompletionProvider Wasm actual
 * (Phase 5) returns emptyList() so this never matters in practice.
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * Wasm/JS stub: returns `null` — native LSP binaries cannot be
 * launched inside a browser Wasm sandbox. The [LspServerRegistry]
 * degrades to empty, and the LSP completion provider returns an
 * empty list on this platform.
 */
actual fun readLspServerResource(path: String): String? = null
