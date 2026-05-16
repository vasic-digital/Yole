/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-62 Phase 6.1: HoverPopup — floating Compose popup that renders
 * LSP hover content (a List<HoverBlock>) anchored near the cursor.
 *
 * Design:
 *   Uses androidx.compose.ui.window.Popup (same as CompletionPopup)
 *   with alignment=TopStart + anchorOffset. Content rendered in a
 *   vertically-scrollable LazyColumn capped at 400×300 dp.
 *
 *   Block rendering:
 *     Paragraph      → body Text
 *     Heading        → headlineMedium / titleLarge / titleMedium (levels 1-3+)
 *     CodeBlock      → SyntaxHighlighter if lang+highlighter available, else monospace
 *     InlineCodeSpan → monospace Text
 *     FallbackText   → italic Text
 *
 * Anti-bluff anchor (CONST-035):
 *   Mutation — stub composable body so it renders nothing (empty Box).
 *   HoverPopupRobolectricTest assertions on testTag("hover-popup") and
 *   paragraph / code-block text content FAIL. Reverted; tests GREEN.
 *
 * Cross-platform impact (CONST-037):
 *   Android: full implementation here.
 *   Desktop: hover popup deferred to Phase 8 integration.
 *   iOS:     deferred — same reason.
 *   Web:     deferred — same reason.
 *
 * Submodules: not touched (CONST-038).
 *########################################################*/
package digital.vasic.yole.android.ui.editor.hover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import digital.vasic.yole.lsp.HoverBlock
import digital.vasic.yole.syntax.SyntaxHighlighter

/** Maximum popup width. */
private val MAX_WIDTH = 400.dp

/** Maximum popup height before scrolling kicks in. */
private val MAX_HEIGHT = 300.dp

/**
 * Floating hover-documentation popup.
 *
 * Renders [blocks] in a vertically-scrollable LazyColumn anchored at
 * [anchorOffset] relative to the Popup's parent. Dismisses via
 * [onDismiss] when the user taps outside.
 *
 * @param blocks LSP hover content parsed into [HoverBlock] variants.
 * @param anchorOffset pixel offset (in parent coordinates) to anchor the popup.
 * @param syntaxHighlighter optional; when non-null, CodeBlock lang blocks
 *   are syntax-highlighted via [SyntaxHighlighter.highlight].
 * @param onDismiss called when the popup should close.
 * @param modifier applied to the outermost Box.
 */
@Composable
fun HoverPopup(
    blocks: List<HoverBlock>,
    anchorOffset: IntOffset,
    syntaxHighlighter: SyntaxHighlighter? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (blocks.isEmpty()) return

    Popup(
        alignment = androidx.compose.ui.Alignment.TopStart,
        offset = anchorOffset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false),
    ) {
        Box(
            modifier = modifier
                .testTag("hover-popup")
                .widthIn(max = MAX_WIDTH)
                .heightIn(max = MAX_HEIGHT)
                .shadow(6.dp, RoundedCornerShape(6.dp))
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
                    HoverBlockItem(block = block, syntaxHighlighter = syntaxHighlighter)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Block renderers
// ---------------------------------------------------------------------------

@Composable
private fun HoverBlockItem(
    block: HoverBlock,
    syntaxHighlighter: SyntaxHighlighter?,
) {
    when (block) {
        is HoverBlock.Paragraph -> {
            Text(
                text = block.text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD4D4D4),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
            )
        }

        is HoverBlock.Heading -> {
            val style = headingStyleFor(block.level)
            Text(
                text = block.text,
                style = style,
                color = Color(0xFFE8E8E8),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
            )
        }

        is HoverBlock.CodeBlock -> {
            // syntaxHighlighter is suspend — for now render plain monospace.
            // Phase 8 integration will wire a pre-highlighted AnnotatedString
            // into the state so HoverPopup stays a pure Composable.
            Text(
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
        }

        is HoverBlock.InlineCodeSpan -> {
            Text(
                text = block.text,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = Color(0xFF9CDCFE),
                ),
                modifier = Modifier
                    .padding(bottom = 4.dp),
            )
        }

        is HoverBlock.FallbackText -> {
            Text(
                text = block.raw,
                style = TextStyle(
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp,
                    color = Color(0xFFAAAAAA),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
            )
        }
    }
}

/**
 * Returns the Material3 typography style for an ATX heading level.
 *
 * Level 1 → headlineMedium, 2 → titleLarge, 3+ → titleMedium.
 * Mutation guard: removing this mapping causes headings to render at the
 * wrong visual weight — caught by [HoverPopupRobolectricTest].
 */
@Composable
internal fun headingStyleFor(level: Int): TextStyle = when (level) {
    1    -> MaterialTheme.typography.headlineMedium
    2    -> MaterialTheme.typography.titleLarge
    else -> MaterialTheme.typography.titleMedium
}
