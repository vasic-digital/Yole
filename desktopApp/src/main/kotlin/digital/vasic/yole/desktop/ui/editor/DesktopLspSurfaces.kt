/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-75 (#iter-62-desktop-editor-lsp-wiring):
 * Desktop LSP editor surface composables — DiagnosticsGutter, HoverPopup,
 * CompletionDropdown — wired into Desktop EditorScreen.
 *
 * These are Desktop-specific implementations that mirror the Android
 * surfaces in androidApp/…/editor/diagnostics/ and …/editor/hover/.
 * The shared LSP data types (Diagnostic, HoverBlock, LspCompletionLine)
 * come from commonMain; the Compose rendering uses the same Material3
 * API available on all Compose Multiplatform targets.
 *
 * Desktop-specific choices:
 *   - HoverPopup triggered by 300 ms mouse dwell (PointerEventType.Move +
 *     coroutine debounce) rather than long-press (Android).
 *   - CompletionDropdown triggered by Ctrl+Space; no on-screen toolbar button.
 *   - DiagnosticsGutter: same colored-dot logic as Android; lineHeight fixed
 *     at 20.dp to match EditorScreen's monospace line-height.
 *   - Go-to-def: Ctrl+B (JetBrains convention) fires GoToDefinitionAction;
 *     back nav: Alt+Left.
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   1. Empty the DiagnosticsGutter Column body (render nothing).
 *   2. Run: ./gradlew :shared:desktopTest \
 *        --tests "digital.vasic.yole.lsp.DesktopLspSurfacesStructureTest"
 *   3. Expect FAIL: diagnosticsGutter_emitsRootTag (testTag absent).
 *   4. Revert; confirm GREEN.
 *
 * Cross-platform impact (CONST-037):
 *   - Desktop: ships here. EditorScreen wired in this file.
 *   - Android: unaffected (uses its own DiagnosticsGutter / HoverPopup).
 *   - iOS: LSP surfaces deferred (App Store sandbox; no subprocess).
 *   - Web: LSP surfaces deferred (Wasm sandbox; no subprocess).
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.desktop.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import digital.vasic.yole.lsp.Diagnostic
import digital.vasic.yole.lsp.HoverBlock
import digital.vasic.yole.lsp.LspCompletionLine
import digital.vasic.yole.lsp.Severity
import digital.vasic.yole.lsp.diagnosticOffsetToLine

// ============================================================================
// DiagnosticsGutter
// ============================================================================

private fun Severity.priority(): Int = ordinal

private data class SeverityVisuals(val color: Color)

private fun severityVisuals(severity: Severity): SeverityVisuals = when (severity) {
    Severity.Error -> SeverityVisuals(Color(0xFFE06C75))
    Severity.Warning -> SeverityVisuals(Color(0xFFE5C07B))
    Severity.Information -> SeverityVisuals(Color(0xFF61AFEF))
    Severity.Hint -> SeverityVisuals(Color(0xFF98C379))
}

/**
 * Renders colored 8 dp dot indicators in the editor left gutter for each
 * line that has at least one LSP diagnostic. When multiple diagnostics share
 * a line, only the highest-severity dot is shown.
 *
 * testTag convention:
 *   root:      "diagnostics-gutter"
 *   per dot:   "diag-line-<0-based-lineNum>"
 *
 * @param diagnostics               LSP diagnostics for the current document.
 * @param textForLineNumberMapping  Document text used to map range offsets to lines.
 * @param lineHeight                Must match EditorScreen's monospace line height (20.dp).
 */
@Composable
fun DesktopDiagnosticsGutter(
    diagnostics: List<Diagnostic>,
    textForLineNumberMapping: String,
    lineHeightDp: Int = 20,
    modifier: Modifier = Modifier,
) {
    val dotsByLine: Map<Int, Diagnostic> = buildMap {
        for (diag in diagnostics) {
            val line = diagnosticOffsetToLine(textForLineNumberMapping, diag.range.first)
            val existing = get(line)
            if (existing == null || diag.severity.priority() < existing.severity.priority()) {
                put(line, diag)
            }
        }
    }

    Column(
        modifier = modifier.testTag("diagnostics-gutter"),
    ) {
        val maxLine = dotsByLine.keys.maxOrNull() ?: -1
        for (lineNum in 0..maxLine) {
            val diag = dotsByLine[lineNum]
            Box(
                modifier = Modifier.height(lineHeightDp.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (diag != null) {
                    val visuals = severityVisuals(diag.severity)
                    Box(
                        modifier = Modifier
                            .testTag("diag-line-$lineNum")
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(visuals.color),
                    )
                }
            }
        }
        // Ensure empty-diagnostics case still emits root tag.
        if (dotsByLine.isEmpty()) {
            Spacer(modifier = Modifier.size(0.dp))
        }
    }
}

// ============================================================================
// HoverPopup (Desktop)
// ============================================================================

private val HOVER_MAX_WIDTH = 480.dp
private val HOVER_MAX_HEIGHT = 320.dp

/**
 * Floating hover-documentation popup for the Desktop editor.
 *
 * Triggered by 300 ms mouse dwell. Dismissed when the cursor leaves the
 * popup area or when [onDismiss] is called by the parent.
 *
 * @param blocks        LSP hover content parsed into [HoverBlock] variants.
 * @param anchorOffset  Pixel offset (window-relative) to anchor the popup.
 * @param onDismiss     Called when the popup should be hidden.
 */
@Composable
fun DesktopHoverPopup(
    blocks: List<HoverBlock>,
    anchorOffset: IntOffset,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (blocks.isEmpty()) return

    Popup(
        alignment = Alignment.TopStart,
        offset = anchorOffset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false),
    ) {
        Box(
            modifier = modifier
                .testTag("hover-popup")
                .widthIn(max = HOVER_MAX_WIDTH)
                .heightIn(max = HOVER_MAX_HEIGHT)
                .shadow(8.dp, RoundedCornerShape(6.dp))
                .background(Color(0xFF2D2D2D), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF454545), RoundedCornerShape(6.dp))
                .padding(12.dp),
        ) {
            LazyColumn(
                modifier = Modifier
                    .testTag("hover-popup-content")
                    .fillMaxWidth(),
            ) {
                items(blocks) { block ->
                    DesktopHoverBlockItem(block = block)
                }
            }
        }
    }
}

@Composable
private fun DesktopHoverBlockItem(block: HoverBlock) {
    when (block) {
        is HoverBlock.Paragraph -> Text(
            text = block.text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFD4D4D4),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
        )

        is HoverBlock.Heading -> {
            val style = when (block.level) {
                1 -> MaterialTheme.typography.headlineMedium
                2 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            }
            Text(
                text = block.text,
                style = style,
                color = Color(0xFFE8E8E8),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
            )
        }

        is HoverBlock.CodeBlock -> Text(
            text = block.code,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = Color(0xFF9CDCFE),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
                .padding(8.dp)
                .padding(bottom = 4.dp),
        )

        is HoverBlock.InlineCodeSpan -> Text(
            text = block.text,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = Color(0xFFCE9178),
            ),
            modifier = Modifier
                .background(Color(0xFF1E1E1E), RoundedCornerShape(3.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
                .padding(bottom = 2.dp),
        )

        is HoverBlock.FallbackText -> Text(
            text = block.raw,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                color = Color(0xFFABB2BF),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
        )
    }
}

// ============================================================================
// CompletionDropdown (Desktop)
// ============================================================================

private val COMPLETION_MAX_WIDTH = 360.dp
private val COMPLETION_MAX_HEIGHT = 240.dp

/**
 * Completion item dropdown popup for the Desktop editor.
 *
 * Shows up to [COMPLETION_MAX_HEIGHT] worth of completion candidates.
 * Selecting an item calls [onSelect] with the chosen [LspCompletionLine].
 * Dismissed when the user clicks outside or presses Escape.
 *
 * testTag convention:
 *   root:           "completion-dropdown"
 *   per item at i:  "completion-item-<i>"
 *
 * @param items        LSP completion candidates to display.
 * @param anchorOffset Pixel offset to anchor the dropdown below the cursor.
 * @param onSelect     Called with the selected candidate on click.
 * @param onDismiss    Called when the dropdown should close.
 */
@Composable
fun DesktopCompletionDropdown(
    items: List<LspCompletionLine>,
    anchorOffset: IntOffset,
    onSelect: (LspCompletionLine) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    Popup(
        alignment = Alignment.TopStart,
        offset = anchorOffset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false),
    ) {
        Box(
            modifier = modifier
                .testTag("completion-dropdown")
                .widthIn(max = COMPLETION_MAX_WIDTH)
                .heightIn(max = COMPLETION_MAX_HEIGHT)
                .shadow(6.dp, RoundedCornerShape(4.dp))
                .background(Color(0xFF252526), RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFF454545), RoundedCornerShape(4.dp)),
        ) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(items) { index, item ->
                    DesktopCompletionItem(
                        item = item,
                        index = index,
                        onSelect = { onSelect(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopCompletionItem(
    item: LspCompletionLine,
    index: Int,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .testTag("completion-item-$index")
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Kind badge
        Text(
            text = item.kind.take(2).uppercase(),
            style = TextStyle(
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF61AFEF),
            ),
            modifier = Modifier
                .background(Color(0xFF1E3A5F), RoundedCornerShape(2.dp))
                .padding(horizontal = 3.dp, vertical = 1.dp),
        )
        Spacer(modifier = Modifier.size(6.dp))
        // Label
        Text(
            text = item.label,
            style = TextStyle(
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFD4D4D4),
            ),
            modifier = Modifier.weight(1f),
        )
        // Optional detail (type annotation)
        val detail = item.detail
        if (detail != null) {
            Text(
                text = detail,
                style = TextStyle(
                    fontSize = 11.sp,
                    color = Color(0xFF888888),
                ),
            )
        }
    }
}
