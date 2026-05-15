/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 3: iOS stub for LspServerInstaller.
 *
 * Returns NotSupported for all specs. The App Store sandbox prohibits
 * spawning subprocesses, so native LSP server binaries cannot run on
 * iOS. This is architecturally permanent — not a deferred feature.
 * Callers should disable LSP completion on iOS and rely on the
 * token/snippet providers (iter-60).
 *#######################################################*/
package digital.vasic.yole.lsp

import okio.Path

/**
 * iOS stub: always returns [LspInstallError.NotSupported].
 *
 * iOS App Store sandbox policy prohibits spawning subprocesses, making
 * native LSP server hosting architecturally impossible on this platform.
 * LSP completion is permanently unavailable on iOS; callers degrade
 * gracefully to the token/snippet completion providers from iter-60.
 */
actual class LspServerInstaller actual constructor(private val spec: LspServerSpec) {

    actual suspend fun ensureInstalled(): Result<Path> {
        val langId = spec.langIds.firstOrNull() ?: "unknown"
        return Result.failure(LspInstallError.NotSupported(langId, "iOS"))
    }
}
