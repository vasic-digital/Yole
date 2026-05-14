/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 4: single source of truth for "is this format currently
 * enabled for the user?" Backed by the persistent Settings.enabledFormatIds
 * set. Consumers (FormatRegistry, GrammarRegistry, EditorScreen, etc.)
 * call requireEnabled()/isEnabled() before doing format-specific work.
 *
 * The gate ensures the Markdown-default constraint (spec §3.7) is
 * enforced uniformly across every entry point. Persistent storage lives
 * in the platform Settings layer (e.g., YoleSettings on Android); the
 * platform app is responsible for calling setEnabled() at startup so the
 * in-memory gate matches what's on disk.
 *
 *########################################################*/
package digital.vasic.yole.syntax

import digital.vasic.yole.format.FormatRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for whether a format is currently enabled.
 *
 * Backed by the persistent Settings.enabledFormatIds set; consumers
 * ([FormatRegistry], GrammarRegistry, EditorScreen, etc.) call
 * [requireEnabled] or [isEnabled] before doing format-specific work.
 *
 * Default state: the canonical [FormatRegistry.defaultEnabledFormatIds]
 * (Markdown only). Platform apps overwrite this at startup via [setEnabled]
 * using the Settings-persisted set so the in-memory gate matches disk.
 *
 * iter-57 Phase 4: ensures the Markdown-default constraint is enforced
 * uniformly across every callsite that consumes a format.
 */
object EnabledFormatGate {
    private val _enabled = MutableStateFlow(FormatRegistry.defaultEnabledFormatIds())

    /**
     * Observable enabled-format set. UIs that need to react to toggles
     * (e.g., the FILES tab showing/hiding badges) collect this flow.
     */
    val enabled: StateFlow<Set<String>> = _enabled.asStateFlow()

    /** Replace the entire enabled-format set. */
    fun setEnabled(ids: Set<String>) {
        _enabled.value = ids
    }

    /** Returns true if [formatId] is currently enabled. */
    fun isEnabled(formatId: String): Boolean = formatId in _enabled.value

    /**
     * Throws [FormatDisabledException] if [formatId] is not currently enabled.
     * Use this at entry points that genuinely intend to USE the format
     * (e.g., loading a grammar, invoking a parser). Callers that can
     * gracefully fall back to plain-text should use [isEnabled] instead.
     */
    fun requireEnabled(formatId: String) {
        if (!isEnabled(formatId)) throw FormatDisabledException(formatId)
    }
}
