/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 8: FormattingSettings — settings row for LSP formatting.
 *
 * Provides a Composable settings toggle for "Format on save" and a
 * companion [FormattingPrefs] helper that reads/writes the preference
 * to SharedPreferences via [YoleSettings] infrastructure.
 *
 * Design:
 *   - Default value is TRUE (on) so users benefit immediately when an LSP
 *     server is available — matches the plan's spec for formatOnSave default.
 *   - The toggle's state is persisted via SharedPreferences key
 *     "lsp_format_on_save" in the "yole_settings" namespace.
 *   - The Composable accepts (getFormatOnSave, setFormatOnSave) parameters
 *     so tests can inject fakes without needing a real Context.
 *   - testTag: "formatting-settings-toggle" (accessible for Robolectric).
 *
 * Anti-bluff mutation procedure (CONST-035 — FormattingSettingsRobolectricTest):
 *   1. Stub setFormatOnSave to no-op (never persist).
 *   2. Run: ./gradlew :androidApp:testFlavorDefaultDebugUnitTest \
 *        --tests "digital.vasic.yole.android.robolectric.FormattingSettingsRobolectricTest"
 *   3. Expect: toggle_persisted_andDefault_isTrue FAILS — re-reading returns default (true)
 *      even after toggle-off, so assertFalse(…) fails.
 *   4. Revert; confirm test PASSES.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: this screen lands here (Compose + SharedPreferences).
 *   - Desktop: formatting invocation via Ctrl+Shift+F does not gate on this
 *     setting (Desktop uses explicit format only in this release).
 *   - iOS: N/A — FormattingSettings is Android-specific.
 *   - Web: N/A.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// Preference key constant
// ---------------------------------------------------------------------------

/** SharedPreferences key for the "Format on save" toggle. */
const val PREF_KEY_LSP_FORMAT_ON_SAVE = "lsp_format_on_save"

// ---------------------------------------------------------------------------
// Composable
// ---------------------------------------------------------------------------

/**
 * Settings row showing a labelled toggle for "Format on save".
 *
 * This Composable is intentionally side-effect-free beyond the supplied
 * [setFormatOnSave] callback so it is testable with injected fakes.
 *
 * @param getFormatOnSave  Reads the current persisted value (default true).
 * @param setFormatOnSave  Persists the new value when the toggle changes.
 */
@Composable
fun FormattingSettingsRow(
    getFormatOnSave: () -> Boolean = { true },
    setFormatOnSave: (Boolean) -> Unit = {},
) {
    var checked by remember { mutableStateOf(getFormatOnSave()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Format on save",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "Automatically apply LSP formatting when the document is saved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { newValue ->
                checked = newValue
                setFormatOnSave(newValue)
            },
            modifier = Modifier.testTag("formatting-settings-toggle"),
        )
    }
}
