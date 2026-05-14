/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 4.6: one-time migration dialog for users upgrading from
 * a pre-iter-57 build (where every format was enabled by default) to an
 * iter-57+ build (where ONLY Markdown is enabled by default — spec §3.7).
 *
 * Behavior:
 *   - Trigger condition: persisted enabledFormatIds is non-empty AND
 *     different from {markdown} AND migration choice not yet made.
 *   - On trigger: snapshot the prior set into priorEnabledFormatIds
 *     (so "Keep mine" can restore it), then surface the dialog.
 *   - Dialog is non-dismissable (no outside-tap, no back-press) until
 *     the user picks one of two options:
 *       • "Keep mine"        — restores priorEnabledFormatIds.
 *       • "Use new default"  — resets to {markdown} only.
 *   - Either choice flips formatEnablementMigrationChoiceMade = true,
 *     so the dialog never shows again.
 *
 *########################################################*/
package digital.vasic.yole.android.ui.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import digital.vasic.yole.android.ui.YoleSettings
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.syntax.EnabledFormatGate

/**
 * Composable wrapper that surfaces [FormatMigrationDialog] iff the
 * migration trigger condition holds. Drop this near the top of the
 * app's root Composable (after YoleSettings is constructed); it
 * renders nothing on the steady-state path.
 *
 * The trigger check runs once per Composable composition via
 * [LaunchedEffect]; the dialog visibility is local state so the
 * Composable doesn't need to re-trigger the snapshot on dismiss.
 */
@Composable
fun MaybeShowFormatMigrationDialog(settings: YoleSettings) {
    var show by remember { mutableStateOf(false) }
    // Read the live gate so any external change (e.g., test setup that
    // bypasses the dialog) reflects in the post-dismiss state. We also
    // read this so the test can recreate the activity and re-trigger.
    val enabledIds by EnabledFormatGate.enabled.collectAsState()
    @Suppress("UNUSED_EXPRESSION") enabledIds  // touch for recomposition wiring

    LaunchedEffect(Unit) {
        if (settings.getFormatEnablementMigrationChoiceMade()) {
            return@LaunchedEffect
        }
        val persisted = settings.getEnabledFormatIds()
        // Trigger only if the user actually had a non-default set
        // persisted from a prior install. "Default" here is exactly
        // {markdown} per FormatRegistry.defaultEnabledFormatIds().
        val isNonDefault = persisted.isNotEmpty() && persisted != setOf("markdown")
        if (isNonDefault) {
            // Capture the prior set BEFORE the user picks — both buttons
            // must be able to act on it ("Keep mine" restores it; "Use
            // new default" doesn't need it but harmless to record).
            if (settings.getPriorEnabledFormatIds().isEmpty()) {
                settings.setPriorEnabledFormatIds(persisted)
            }
            show = true
        }
    }

    if (show) {
        FormatMigrationDialog(
            settings = settings,
            onChosen = { show = false },
        )
    }
}

/**
 * The actual dialog. Two buttons, no dismissal. Either choice records
 * `formatEnablementMigrationChoiceMade = true` then invokes [onChosen]
 * so the host Composable hides the dialog.
 *
 * Public-visible for direct testing — though tests typically drive it
 * via [MaybeShowFormatMigrationDialog].
 */
@Composable
fun FormatMigrationDialog(
    settings: YoleSettings,
    onChosen: () -> Unit,
) {
    AlertDialog(
        // Non-dismissable per the spec — the user MUST make a choice.
        onDismissRequest = { /* intentionally swallow back-press / outside-tap */ },
        title = { Text("Format defaults changed") },
        text = {
            Text(
                "Yole now enables only Markdown by default. " +
                "Keep your previous formats enabled, or adopt the new default?"
            )
        },
        confirmButton = {
            TextButton(onClick = {
                // "Use new default" → reset to Markdown-only.
                FormatRegistry.setEnabledFormatIds(setOf("markdown"))
                settings.setEnabledFormatIds(setOf("markdown"))
                settings.setFormatEnablementMigrationChoiceMade(true)
                onChosen()
            }) {
                Text("Use new default")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                // "Keep mine" → restore the snapshot taken at trigger time.
                val prior = settings.getPriorEnabledFormatIds()
                val restored = if (prior.isEmpty()) setOf("markdown") else prior
                FormatRegistry.setEnabledFormatIds(restored)
                settings.setEnabledFormatIds(restored)
                settings.setFormatEnablementMigrationChoiceMade(true)
                onChosen()
            }) {
                Text("Keep mine")
            }
        },
    )
}
