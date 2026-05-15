/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 3: LspServerInstaller Desktop tests.
 *
 * CONST-035 anti-bluff mutation procedure:
 *   1. In LspServerInstaller.desktop.kt, stub ensureInstalled() to
 *      always return Result.success(target) (skip resource lookup).
 *   2. Run: ./gradlew :shared:desktopTest \
 *        --tests "digital.vasic.yole.lsp.LspServerInstallerTest"
 *   3. Expect: missingBundle_returnsFailureWithExtractionFailed FAILS
 *      because the stub returns success instead of failure.
 *   4. Revert stub; confirm all tests PASS.
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Desktop-only tests for [LspServerInstaller].
 *
 * Phase 7 does not yet bundle real LSP binaries, so the only exercisable
 * code path on Desktop is the missing-bundle → ExtractionFailed path.
 * This is HONEST per CONST-035: the failure is the correct runtime
 * behaviour until Phase 7 populates lsp-bundles/ on the classpath.
 */
class LspServerInstallerTest {

    /**
     * A spec that references a langId + executable that intentionally does
     * NOT exist on the classpath (no lsp-bundles/test-lang-missing/ghost-bin
     * resource is ever added). ensureInstalled() must return failure with
     * an ExtractionFailed error.
     *
     * Mutation check: stub ensureInstalled to return Result.success(target)
     * → this test FAILS (asserts isFailure, stub returns success).
     */
    @Test
    fun missingBundle_returnsFailureWithExtractionFailed() = runBlocking<Unit> {
        val spec = LspServerSpec(
            langIds = listOf("test-lang-missing"),
            executable = "ghost-bin",
            args = emptyList(),
            projectMarkers = emptyList(),
        )
        val installer = LspServerInstaller(spec)

        val result = installer.ensureInstalled()

        assertTrue(result.isFailure, "Expected failure when bundled binary absent from classpath")
        assertIs<LspInstallError.ExtractionFailed>(
            result.exceptionOrNull(),
            "Expected ExtractionFailed but got: ${result.exceptionOrNull()}"
        )
    }

    /**
     * Ensure that the error carries the expected langId so callers can
     * log and diagnose which language's installer failed.
     *
     * Mutation check: stub ensureInstalled to return Result.success(target)
     * → this test FAILS (asserts isFailure).
     */
    @Test
    fun missingBundle_extractionFailed_carriesLangId() = runBlocking<Unit> {
        val langId = "test-lang-with-id"
        val spec = LspServerSpec(
            langIds = listOf(langId),
            executable = "nonexistent-bin",
            args = emptyList(),
            projectMarkers = emptyList(),
        )
        val installer = LspServerInstaller(spec)

        val result = installer.ensureInstalled()

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertIs<LspInstallError.ExtractionFailed>(error)
        assertTrue(
            error.langId == langId,
            "ExtractionFailed.langId expected=$langId, got=${error.langId}"
        )
    }
}
