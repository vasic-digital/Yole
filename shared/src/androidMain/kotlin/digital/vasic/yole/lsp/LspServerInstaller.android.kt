/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 3: Android stub for LspServerInstaller.
 *
 * Returns NotInstalled for all specs. This is an HONEST stub per
 * CONST-035 — no binaries exist yet, and the SplitInstallManager-aware
 * extraction mechanism is deferred to Phase 8. Callers receive a clear
 * LspInstallError.NotInstalled so they can gracefully disable LSP
 * completion on Android without crashing.
 *
 * Phase 8 will replace this body with a SplitInstallManager-aware
 * implementation that downloads and extracts the LSP server binary
 * via the Play Core on-demand delivery API.
 *#######################################################*/
package digital.vasic.yole.lsp

import okio.Path

/**
 * Android stub: always returns [LspInstallError.NotInstalled].
 *
 * LSP server binary extraction via [SplitInstallManager] is deferred to
 * Phase 8. Until then, Android callers should disable LSP-backed
 * completion and fall back to the token/snippet providers (iter-60).
 */
actual class LspServerInstaller actual constructor(private val spec: LspServerSpec) {

    actual suspend fun ensureInstalled(): Result<Path> {
        val langId = spec.langIds.firstOrNull() ?: "unknown"
        return Result.failure(LspInstallError.NotInstalled(langId))
    }
}
