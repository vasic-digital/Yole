/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 4.5: Settings → Formats sub-screen.
 *
 * Spec §3.7 — Markdown-default operator constraint. The user sees three
 * sections:
 *   1. "Default (always on)"      — Markdown only, non-toggleable.
 *   2. "Text formats (toggle)"    — every TextFormat in FormatRegistry
 *                                   except Markdown and network protocols.
 *   3. "Programming languages (toggle)" — hardcoded v1 list (since the
 *                                   source-code grammars are Phase 2 of
 *                                   the broader 5-feature plan and not
 *                                   yet shipped).
 *
 * Each toggle:
 *   - Reads state from EnabledFormatGate.enabled (observable StateFlow).
 *   - On flip, calls FormatRegistry.setFormatEnabled / setFormatDisabled
 *     (the Phase 4.4 mirror propagates into EnabledFormatGate).
 *   - Persists the new set via YoleSettings.setEnabledFormatIds.
 *
 * Colors come from LocalTheme.current.uiColor(key) via the
 * `themeUiColor("…")` helper — no hardcoded palette.
 *
 *########################################################*/
package digital.vasic.yole.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import digital.vasic.yole.android.ui.YoleSettings
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat
import digital.vasic.yole.syntax.EnabledFormatGate
import digital.vasic.yole.syntax.theme.themeUiColor

/**
 * v1 programming-language list. Phase 2 of the broader 5-feature plan
 * will replace this with a fully-loaded grammar registry; for now the IDs
 * just live in the enabled-formats set and the toggles drive the gate.
 *
 * Tuple: (formatId, displayName, listOfExtensions).
 */
private val v1ProgrammingLangs = listOf(
    Triple("kotlin", "Kotlin", listOf(".kt", ".kts")),
    Triple("java", "Java", listOf(".java")),
    Triple("python", "Python", listOf(".py", ".pyi")),
    Triple("javascript", "JavaScript", listOf(".js", ".mjs", ".cjs")),
    Triple("typescript", "TypeScript", listOf(".ts", ".tsx")),
    Triple("go", "Go", listOf(".go")),
    Triple("rust", "Rust", listOf(".rs")),
    Triple("c", "C", listOf(".c", ".h")),
    Triple("cpp", "C++", listOf(".cpp", ".cc", ".hpp")),
    Triple("html", "HTML", listOf(".html", ".htm")),
    Triple("css", "CSS", listOf(".css", ".scss", ".less")),
    Triple("sql", "SQL", listOf(".sql")),
)

/**
 * Top app bar for the Formats sub-screen. Distinct from
 * `SettingsTopBar` so the title reads "Formats" and the back button
 * returns to the Settings screen (not all the way out).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatsSettingsTopBar(onBackClick: () -> Unit) {
    TopAppBar(
        title = { Text("Formats", fontFamily = FontFamily.Monospace) },
        navigationIcon = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.semantics { contentDescription = "Back from Formats" }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    )
}

/**
 * Renders the three-section format-enablement screen described in spec
 * §3.7. The screen is reactive: changes to [EnabledFormatGate.enabled]
 * (from any source) recompose the switches immediately.
 *
 * @param settings persistence backend. The screen writes the new
 *   enabled-format set back to settings on every toggle so the gate and
 *   disk stay in sync.
 */
@Composable
fun FormatsSettingsScreen(
    settings: YoleSettings,
    onBackClick: () -> Unit = {},
) {
    val bg = themeUiColor("editor.background")
    val enabledIds by EnabledFormatGate.enabled.collectAsState()

    // Compute the "Text formats (toggle)" section once per recomposition.
    // The network-protocol IDs are filtered server-side by FormatRegistry's
    // getTextFormats() helper; we additionally drop ID_MARKDOWN since it
    // belongs to the "Default (always on)" section.
    val toggleableTextFormats = remember {
        FormatRegistry.getTextFormats()
            .filter { it.id != TextFormat.ID_MARKDOWN }
            .sortedBy { it.name.lowercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // ──────────── Section 1: Default (always on) ────────────
        SectionHeader("DEFAULT (ALWAYS ON)")
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Markdown is the canonical format and cannot be disabled.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(8.dp))

        FormatToggleRow(
            displayName = "Markdown",
            extensions = listOf(".md", ".markdown", ".mdown", ".mkd"),
            checked = true,
            enabled = false,
            systemLabel = "system",
            onToggle = { /* no-op: Markdown cannot be disabled */ },
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ──────────── Section 2: Text formats (toggle) ────────────
        SectionHeader("TEXT FORMATS")
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Toggle which text formats are available in the editor and previews.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(8.dp))

        toggleableTextFormats.forEach { format ->
            FormatToggleRow(
                displayName = format.name,
                extensions = format.extensions,
                checked = format.id in enabledIds,
                enabled = true,
                systemLabel = null,
                onToggle = { isOn ->
                    applyFormatToggle(settings, format.id, isOn)
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ──────────── Section 3: Programming languages (toggle) ────────────
        SectionHeader("PROGRAMMING LANGUAGES")
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Enable syntax highlighting for source-code files. (Engines arrive in Phase 5+.)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(8.dp))

        v1ProgrammingLangs.forEach { (id, displayName, exts) ->
            FormatToggleRow(
                displayName = displayName,
                extensions = exts,
                checked = id in enabledIds,
                enabled = true,
                systemLabel = null,
                onToggle = { isOn ->
                    applyFormatToggle(settings, id, isOn)
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Toggles a format on/off and persists the new enabled-set. Delegates
 * the gate-mirror to FormatRegistry's Phase 4.4 propagation; we then
 * write the result back to YoleSettings so the choice survives restart.
 *
 * Markdown is structurally non-togglable (see Section 1 above) and the
 * registry already enforces this; this helper is a no-op for it.
 */
private fun applyFormatToggle(
    settings: YoleSettings,
    formatId: String,
    enable: Boolean,
) {
    if (enable) {
        FormatRegistry.setFormatEnabled(formatId)
    } else {
        FormatRegistry.setFormatDisabled(formatId)
    }
    // Persist the post-mutation gate state (registry mirrored into gate).
    settings.setEnabledFormatIds(EnabledFormatGate.enabled.value)
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
    )
}

/**
 * Single row: switch + display name + extension list + optional
 * "system" tag. When [enabled] is false, the switch is rendered but
 * inert (Markdown row); we still display it so users can SEE that
 * Markdown is on.
 */
@Composable
private fun FormatToggleRow(
    displayName: String,
    extensions: List<String>,
    checked: Boolean,
    enabled: Boolean,
    systemLabel: String?,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { base ->
                // The whole row participates in toggling so that tapping
                // anywhere on the row (including the text label) flips
                // the switch. This is critical for the Robolectric test
                // (which taps the format name to verify the gate state
                // updates) AND for accessibility — TalkBack users need a
                // single target per row, not a separate Switch + label.
                if (enabled) base.clickable { onToggle(!checked) } else base
            }
            .padding(vertical = 6.dp)
            .semantics { contentDescription = "Toggle $displayName" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = extensions.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        if (systemLabel != null) {
            Text(
                text = systemLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = { isOn -> onToggle(isOn) },
        )
    }
}
