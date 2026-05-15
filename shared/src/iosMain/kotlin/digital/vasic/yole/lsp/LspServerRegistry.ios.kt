/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 1: iOS stub — returns null since LSP integration
 * is not supported on iOS (App Store subprocess prohibition).
 * Honest empty per CONST-035; LspCompletionProvider iOS actual
 * (Phase 5) returns emptyList() so this never matters in practice.
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * iOS stub: returns `null` — LSP server binaries cannot be spawned as
 * subprocesses on iOS due to App Store sandbox policy. The
 * [LspServerRegistry] degrades to empty, and the LSP completion
 * provider returns an empty list on this platform.
 */
actual fun readLspServerResource(path: String): String? = null
