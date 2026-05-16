/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-62 Phase 1: DiagnosticsCache — single source of truth
 * for LSP-emitted diagnostics. Per-URI cap 1000.
 *
 * Populated by YoleLanguageClient.publishDiagnostics
 * (wired in Phase 2). Consumed by the 3 render surfaces
 * (Phase 5) via the states StateFlow.
 *
 * Cache semantic: LSP publishDiagnostics is full-state per URI
 * (server emits the complete current list). upsert replaces.
 *
 * Cap semantic: when the incoming list exceeds PER_URI_CAP,
 * the tail is dropped (first 1000 diagnostics kept). The
 * truncated tail is discarded silently — no sentinel entry.
 * This matches LSP spec guidance that low-severity trailing
 * diagnostics are least actionable.
 *
 * Mutation procedure (CONST-035):
 *   Stub upsert to: current + (uri to (current[uri].orEmpty() + capped))
 *   → upsert_replaces_previous + upsert_empty_clears_uri FAIL.
 *   Revert → 5/5 PASS.
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DiagnosticsCache {
    companion object {
        const val PER_URI_CAP: Int = 1000
    }

    private val _state = MutableStateFlow<Map<String, List<Diagnostic>>>(emptyMap())
    val states: StateFlow<Map<String, List<Diagnostic>>> = _state.asStateFlow()

    fun upsert(uri: String, diagnostics: List<Diagnostic>) {
        val capped = if (diagnostics.size > PER_URI_CAP) diagnostics.take(PER_URI_CAP) else diagnostics
        _state.update { current ->
            if (capped.isEmpty()) current - uri
            else current + (uri to capped)
        }
    }

    fun clear(uri: String) {
        _state.update { it - uri }
    }

    fun clearAll() {
        _state.value = emptyMap()
    }

    fun diagnosticsFor(uri: String): List<Diagnostic> = _state.value[uri].orEmpty()
}
