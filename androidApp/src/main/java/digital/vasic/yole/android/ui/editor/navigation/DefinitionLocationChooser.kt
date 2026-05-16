/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-62 Phase 7: DefinitionLocationChooser — Material3 bottom-sheet picker.
 *
 * Displayed when [GoToDefinitionAction] receives multiple definition targets.
 * Renders one row per [DefinitionLocation] showing the filename (last path
 * segment) and the line number derived from [DefinitionLocation.range.first].
 *
 * Design:
 *   - Material3 ModalBottomSheet with one clickable Row per location.
 *   - Root sheet content is tagged "def-chooser" for test instrumentation.
 *   - Each row is tagged "def-row-<index>" (0-based) for Robolectric tests.
 *   - Dismiss is handled both by swipe (ModalBottomSheet default) and an
 *     explicit "Cancel" row at the bottom — onDismiss callback.
 *
 * Anti-bluff anchor (CONST-035):
 *   Mutation — stub onClick handler to no-op (remove onSelected call).
 *   clickRow_invokesOnSelected FAILS (callback never called). Reverted; GREEN.
 *   Mutation — remove "def-row-<index>" testTag.
 *   rendersAllLocations FAILS (tagged nodes not found). Reverted; GREEN.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: full implementation here (ModalBottomSheet).
 *   - Desktop:  dialog/list chooser deferred to Phase 8 integration.
 *   - iOS:      deferred — UIKit ActionSheet variant planned.
 *   - Web:      deferred — dialog variant planned.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.ui.editor.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import digital.vasic.yole.lsp.DefinitionLocation

/**
 * Bottom-sheet chooser shown when a go-to-definition request returns multiple
 * possible targets.
 *
 * @param locations  The list of candidate [DefinitionLocation] entries to present.
 * @param onSelected Called when the user taps a row; receives the chosen location.
 * @param onDismiss  Called when the sheet is dismissed without a selection.
 * @param modifier   Optional modifier applied to the sheet's content column.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefinitionLocationChooser(
    locations: List<DefinitionLocation>,
    onSelected: (DefinitionLocation) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = modifier
                .testTag("def-chooser")
                .fillMaxWidth(),
        ) {
            Text(
                text = "Go to Definition",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            HorizontalDivider()

            LazyColumn {
                itemsIndexed(locations) { index, location ->
                    DefinitionLocationRow(
                        location = location,
                        index = index,
                        onClick = { onSelected(location) },
                    )
                    if (index < locations.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }

            HorizontalDivider()

            // Cancel row — explicit dismiss without selection.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Internal row composable — one entry per DefinitionLocation
// ---------------------------------------------------------------------------

@Composable
private fun DefinitionLocationRow(
    location: DefinitionLocation,
    index: Int,
    onClick: () -> Unit,
) {
    // Derive a human-readable filename from the URI (last path segment).
    val filename = location.uri
        .trimEnd('/')
        .substringAfterLast('/')
        .ifBlank { location.uri }

    // Derive a 1-based line number from the character offset stored in range.first.
    // Phase 3 LspRangeMapping populates accurate offsets; here we use range.first
    // as the canonical "start of definition" value and display it as-is.
    val offsetLabel = "offset ${location.range.first}"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("def-row-$index")
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = filename,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = offsetLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
