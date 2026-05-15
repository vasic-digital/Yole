/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-61 Phase 8: Settings → LSP sub-screen.
 *
 * v1 scope: lists all 15 Yole-bundled LSP servers and their
 * install status. On Android, ALL servers show "Not available on
 * Android (v1)" — the honest stub in LspServerInstaller.android.kt
 * always returns NotInstalled, reflecting that no native LSP binaries
 * are bundled for Android in this release.
 *
 * This is HONEST per CONST-035 — surfacing "not available" rather
 * than hiding the feature entirely lets users understand the current
 * v1 Desktop-only limitation and sets correct expectations for v2.
 *
 * Wire-in: the "LSP language support" entry is added to SettingsScreen
 * in YoleApp.kt (see iter-61 Phase 8 changes). If the Settings nav is
 * evolved further in v2, an LSP_SETTINGS SubScreen entry should be
 * added to the SubScreen enum and the AnimatedContent when-block.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: this screen lands (honest v1 stub UX).
 *   - Desktop: unaffected (runs Gradle Sync task only; no UI change).
 *   - iOS: N/A — LspSettingsScreen is Android-specific.
 *   - Web: N/A — same.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import digital.vasic.yole.lsp.LspServerRegistry
import digital.vasic.yole.lsp.LspServerSpec

/**
 * Settings sub-screen listing all 15 Yole-bundled LSP servers and their
 * v1 install status on Android (all "not available" in this release).
 *
 * @param onBackClick Called when the user presses the back arrow in the TopAppBar.
 */
@Composable
fun LspSettingsScreen(onBackClick: () -> Unit = {}) {
    // De-duplicate specs: c and cpp share clangd. Set deduplication via
    // distinctBy(executable) so the list shows one row per server binary,
    // not one row per langId.
    val specs = remember {
        LspServerRegistry.default().allSpecs()
            .distinctBy { it.executable }
            .sortedBy { it.executable }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("lsp-settings-screen"),
    ) {
        LspSettingsTopBar(onBackClick = onBackClick)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "v1: LSP support is available on Desktop (macOS arm64). " +
                "Android LSP support is coming in a future release.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .testTag("lsp-settings-v1-disclaimer"),
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.testTag("lsp-settings-list")) {
            items(specs, key = { it.executable }) { spec ->
                LspLangRow(spec = spec)
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LspSettingsTopBar(onBackClick: () -> Unit = {}) {
    TopAppBar(
        title = { Text("LSP Language Support") },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
        },
    )
}

@Composable
private fun LspLangRow(spec: LspServerSpec) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("lsp-row-${spec.executable}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Use weight() inside RowScope via extension function.
        LspLangRowContent(spec = spec)
    }
}

@Composable
private fun RowScope.LspLangRowContent(spec: LspServerSpec) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = spec.langIds.joinToString(" + "),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = spec.executable,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
    Text(
        text = "Not available on Android (v1)",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.testTag("lsp-row-status-${spec.executable}"),
    )
}
