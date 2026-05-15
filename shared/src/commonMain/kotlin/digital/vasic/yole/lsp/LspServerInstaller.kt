/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 3: LspServerInstaller — expect class + sealed error
 * hierarchy. Platform actuals resolve langId → writable executable
 * path. Binaries arrive in Phase 7; Phase 3 installs the plumbing.
 *
 * Mutation procedure (CONST-035):
 *   1. In the Desktop actual (LspServerInstaller.desktop.kt), stub
 *      ensureInstalled() to always return Result.success(target) without
 *      checking whether the bundled resource exists.
 *   2. Run: ./gradlew :shared:desktopTest \
 *        --tests "digital.vasic.yole.lsp.LspServerInstallerTest"
 *   3. Expect: missingBundle_returnsFailureWithExtractionFailed FAILS
 *      (the test asserts isFailure but the stub always returns success).
 *   4. Revert; confirm all LspServerInstallerTest tests PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Desktop:  Full impl — extracts bundled binary from JVM classpath
 *               to user-data dir and chmod +x. Awaits Phase 7 binaries.
 *   - Android:  Honest stub — NotInstalled. Phase 8 wires
 *               SplitInstallManager-aware extraction.
 *   - iOS:      Honest stub — NotSupported. App Store sandbox prohibits
 *               spawning subprocesses; LSP on iOS is architecturally
 *               excluded.
 *   - Web/Wasm: Honest stub — NotSupported. Native binaries cannot run
 *               inside a browser Wasm sandbox.
 *#######################################################*/
package digital.vasic.yole.lsp

import okio.Path

/**
 * Errors that [LspServerInstaller.ensureInstalled] may produce.
 *
 * Three variants cover the full failure space across all four platforms:
 * - [NotInstalled]      — binary absent and no extraction mechanism exists yet.
 * - [NotSupported]      — the current platform architecturally cannot run LSP.
 * - [ExtractionFailed]  — binary extraction / chmod was attempted but failed.
 */
sealed class LspInstallError : Throwable() {
    /** Binary not present and no install mechanism available on this platform. */
    data class NotInstalled(val langId: String) : LspInstallError() {
        override val message: String = "LSP server not installed for lang=$langId"
    }

    /** This platform cannot run LSP server subprocesses at all. */
    data class NotSupported(val langId: String, val platform: String) : LspInstallError() {
        override val message: String =
            "LSP server not supported on platform=$platform for lang=$langId"
    }

    /** Binary was found in the bundle but extraction or chmod failed. */
    data class ExtractionFailed(
        val langId: String,
        val rootCause: Throwable? = null,
    ) : LspInstallError() {
        override val message: String = "LSP server extraction failed for lang=$langId"
        override val cause: Throwable? get() = rootCause
    }
}

/**
 * Platform-specific resolver that either extracts a bundled LSP binary
 * into a writable user-data directory (Desktop) or returns an appropriate
 * error (Android stub, iOS/Wasm unsupported stubs).
 *
 * The returned [Path] on success is an absolute path to an executable
 * that can be passed directly to [ProcessBuilder] (Desktop Phase 4).
 *
 * Usage:
 * ```kotlin
 * val installer = LspServerInstaller(spec)
 * val result = installer.ensureInstalled()
 * result.getOrNull()    // okio.Path to the executable, or null on failure
 * result.exceptionOrNull() // LspInstallError subtype on failure
 * ```
 *
 * Idempotent: calling [ensureInstalled] repeatedly for an already-extracted
 * binary is cheap — the Desktop actual skips extraction when the target
 * file already exists.
 */
expect class LspServerInstaller(spec: LspServerSpec) {
    /**
     * Ensures the LSP server binary is available and executable.
     *
     * @return [Result.success] wrapping the absolute [Path] to the
     *   executable on Desktop, or [Result.failure] wrapping a
     *   [LspInstallError] on all other platforms / error conditions.
     */
    suspend fun ensureInstalled(): Result<Path>
}
