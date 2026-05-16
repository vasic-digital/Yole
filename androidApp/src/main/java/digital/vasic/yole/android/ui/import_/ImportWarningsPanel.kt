/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-64 Phase 9: ImportWarningsPanel — collapsible LazyColumn that
 * renders each ImportWarning as a labelled row.
 *
 * testTag conventions:
 *   root panel:      "import-warnings-panel"
 *   per warning row: "import-warning-<index>"  (0-based)
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   1. Remove testTag("import-warnings-panel") → panel tag assertion FAILS.
 *   2. Remove testTag("import-warning-$index") → per-row tag assertion FAILS.
 *   3. Stub the LazyColumn body to render nothing → per-row test FAILS.
 *   4. Remove the collapse toggle → collapseToggle assertion FAILS.
 *   Revert → all PASS.
 *
 * Cross-platform impact (CONST-037):
 *   Android: ships here.
 *   Desktop/iOS/Web: N/A this phase.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.ui.import_

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import digital.vasic.yole.import_.ImportWarning
import digital.vasic.yole.import_.Severity

/**
 * Collapsible panel listing [warnings] emitted during import.
 *
 * When collapsed only the summary header is visible. Expanding it shows
 * a [LazyColumn] with one row per warning.
 *
 * @param warnings the list of [ImportWarning] to display.
 * @param modifier optional modifier chain.
 */
@Composable
fun ImportWarningsPanel(
    warnings: List<ImportWarning>,
    modifier: Modifier = Modifier,
) {
    if (warnings.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.testTag("import-warnings-panel")) {
        // Collapse / expand toggle row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowDown
                else Icons.Filled.KeyboardArrowRight,
                contentDescription = if (expanded) "Collapse warnings" else "Expand warnings",
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${warnings.size} warning${if (warnings.size == 1) "" else "s"}",
                style = MaterialTheme.typography.labelMedium,
            )
        }

        if (expanded) {
            LazyColumn {
                itemsIndexed(warnings) { index, warning ->
                    Row(
                        modifier = Modifier
                            .testTag("import-warning-$index")
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = warning.severity.name,
                            tint = if (warning.severity == Severity.Warning) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = warning.message,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            warning.pageOrSection?.let { loc ->
                                Text(
                                    text = loc,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
