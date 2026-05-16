/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-62 Phase 1: Diagnostic data class + Severity enum.
 *
 * Severity values mirror LSP DiagnosticSeverity 1-4
 * (Error=1, Warning=2, Information=3, Hint=4). Yole order
 * matches LSP for trivial 1:1 mapping in Phase 2.
 *
 * Cross-platform: pure commonMain. Used by JVM-only
 * LspServerHost (iOS/Wasm stubs); never returns Diagnostic
 * instances from those stubs (DiagnosticsCache stays empty).
 *#######################################################*/
package digital.vasic.yole.lsp

enum class Severity { Error, Warning, Information, Hint }

data class Diagnostic(
    val severity: Severity,
    val range: IntRange,
    val message: String,
    val source: String? = null,
    val code: String? = null,
)
