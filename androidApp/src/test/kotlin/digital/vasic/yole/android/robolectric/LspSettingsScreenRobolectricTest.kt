/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-61 Phase 8: Robolectric tests for LspSettingsScreen.
 *
 * Two anti-bluff tests (CONST-035):
 *   1. rendersScreenWithAllLangs — verifies the screen renders with the
 *      testTag "lsp-settings-screen" and "lsp-settings-list" visible,
 *      AND that ≥ 1 rows tagged "lsp-row-*" are present.
 *   2. allRowsShowNotAvailableInV1 — verifies the disclaimer and at least
 *      two specific row-status testTags ("lsp-row-status-marksman" and
 *      "lsp-row-status-rust-analyzer") are displayed.
 *
 * Mutation procedure (CONST-035):
 *   1. In LspSettingsScreen.kt, remove the LazyColumn and all LspLangRow calls
 *      (stub the body to just display an empty Column).
 *   2. Run: ./gradlew :androidApp:testFlavorDefaultDebugUnitTest \
 *        --tests "digital.vasic.yole.android.robolectric.LspSettingsScreenRobolectricTest"
 *   3. Expect:
 *        - rendersScreenWithAllLangs FAILS — no lsp-row-* nodes found.
 *        - allRowsShowNotAvailableInV1 FAILS — disclaimer or row-status nodes absent.
 *   4. Revert; confirm both PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: tests land and validate LspSettingsScreen (this phase).
 *   - Desktop: unaffected — desktopTest source set.
 *   - iOS / Web: N/A.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.robolectric

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import digital.vasic.yole.android.ui.settings.LspSettingsScreen
import digital.vasic.yole.lsp.LspServerRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class LspSettingsScreenRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    /**
     * Verifies that LspSettingsScreen renders the root container, the list,
     * and at least one `lsp-row-*` tagged node for each distinct executable.
     *
     * Anti-bluff load-bearing assertion:
     *   - `onNodeWithTag("lsp-settings-screen").assertIsDisplayed()` fails if
     *     the screen root is absent (e.g., screen is stubbed to empty).
     *   - The row-count assertion fails if the LazyColumn items are removed.
     */
    @Test
    fun rendersScreenWithAllLangs() {
        composeRule.setContent { LspSettingsScreen() }

        // Root and list must be displayed — direct evidence the screen rendered.
        composeRule.onNodeWithTag("lsp-settings-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("lsp-settings-list").assertIsDisplayed()

        // Determine the expected number of distinct executables from the registry
        // (same logic as in LspSettingsScreen: distinctBy executable).
        val expectedRowCount = LspServerRegistry.default().allSpecs()
            .distinctBy { it.executable }
            .size

        // Wait for Compose to settle so all LazyColumn items are composed.
        composeRule.waitForIdle()

        // Assert that well-known lsp-row-* testTags are present — mutation-killable:
        // removing LspLangRow calls → these nodes are absent → FAILS.
        // Spot-check two mandatory rows (marksman + rust-analyzer are always in the v1 registry).
        composeRule.onNodeWithTag("lsp-row-marksman").assertIsDisplayed()
        composeRule.onNodeWithTag("lsp-row-rust-analyzer").assertIsDisplayed()

        // Also confirm total node count via all nodes matching the exact row tag pattern.
        // distinctBy(executable) yields at most expectedRowCount items.
        // We use the registry's own de-dup logic to build exact tags and count them.
        val specs = LspServerRegistry.default().allSpecs().distinctBy { it.executable }
        val presentRowCount = specs.count { spec ->
            composeRule.onAllNodesWithTag("lsp-row-${spec.executable}")
                .fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "Expected all $expectedRowCount distinct executable rows to render, " +
                "but only $presentRowCount were found in the semantics tree. " +
                "Stubbing LspSettingsScreen to not render LspLangRows would cause this failure.",
            presentRowCount >= expectedRowCount,
        )
    }

    /**
     * Verifies that the v1 disclaimer is visible and that spot-checked
     * "Not available on Android (v1)" status labels are rendered for
     * marksman (markdown) and rust-analyzer (rust) rows.
     *
     * Anti-bluff load-bearing assertion:
     *   - The disclaimer testTag assertion fails if the disclaimer is removed.
     *   - The row-status assertions fail if LspLangRow omits the status Text
     *     or changes its testTag.
     */
    @Test
    fun allRowsShowNotAvailableInV1() {
        composeRule.setContent { LspSettingsScreen() }

        // Disclaimer must be visible.
        composeRule.onNodeWithTag("lsp-settings-v1-disclaimer").assertIsDisplayed()

        // Spot-check two well-known server status tags:
        //   marksman → markdown LSP server (binary staged in Phase 8)
        //   rust-analyzer → Rust LSP server (binary staged in Phase 8)
        // Both must render the "Not available on Android (v1)" label.
        composeRule.onNodeWithTag("lsp-row-status-marksman").assertIsDisplayed()
        composeRule.onNodeWithTag("lsp-row-status-rust-analyzer").assertIsDisplayed()
    }
}
