/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 Feature 2 Phase 5: OutlineDrawer — slide-in left panel
 * showing the active file's outline (section headings, functions,
 * classes, methods, fields, etc.) extracted by Phase 3's
 * OutlineExtractor.
 *
 * Lifecycle:
 *   - The drawer is rendered inside the editor surface (IdeEditorScreen).
 *   - When [isOpen] is true, a LaunchedEffect keyed on (text, langId)
 *     invokes OutlineExtractor.outlineFor() and stores the result.
 *   - Each captured OutlineItem is rendered as a clickable row in a
 *     LazyColumn with a kind-appropriate Material icon.
 *   - Clicking a row calls [onItemClick]; the caller decides how to
 *     scroll to / select the item.
 *
 * Anti-bluff covenant (CONST-035): the drawer MUST drive the
 * user-visible behavior end-to-end. Stubbing OutlineExtractor.outlineFor
 * to `return emptyList()` MUST cause the
 * `rendersOutlineItemsForMarkdownHeadings` test in
 * OutlineDrawerRobolectricTest to FAIL because no `outline.item:*`
 * test-tag nodes would be rendered for `# H1\n## H2\n`.
 *
 * Cross-platform impact (CONST-037): Phase 5 ships Android only.
 * Desktop/iOS/web ports are deferred sub-tasks documented in the
 * Phase 5 plan section — they re-use the same shared OutlineExtractor
 * API but wire their own Compose surface (desktopApp's editor screen
 * is Compose-for-Desktop; iOS Compose Multiplatform; webApp Compose Wasm).
 *
 *########################################################*/
package digital.vasic.yole.android.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import digital.vasic.yole.language.affordance.OutlineExtractor
import digital.vasic.yole.language.affordance.OutlineItem
import digital.vasic.yole.syntax.TokenizerEngine
import digital.vasic.yole.syntax.theme.LocalTheme

/**
 * Map an outline-item kind (the suffix after `definition.` in the helix
 * tags.scm captures) to a Material icon. Limited to the core
 * material-icons-core set to avoid pulling in the ~10MB
 * material-icons-extended dependency. Each branch maps to a visually
 * distinct icon so kindToIcon(a) != kindToIcon(b) for the kinds the
 * test asserts. Unknown kinds fall back to [Icons.Filled.Info] which
 * is also visually distinct from the others.
 *
 * Visible for testing.
 */
internal fun kindToIcon(kind: String): ImageVector = when (kind.lowercase()) {
    "section", "heading" -> Icons.Filled.Menu       // a "heading bar" glyph
    "function", "method" -> Icons.Filled.Build      // a "wrench / function" glyph
    "class", "interface", "struct", "type" -> Icons.Filled.Star      // a "class / star" glyph
    "field", "property", "constant", "variable" -> Icons.Filled.Create   // a "pen / value" glyph
    "module", "namespace" -> Icons.Filled.List      // a "module list" glyph
    else -> Icons.Filled.Info
}

/**
 * Slide-in left panel showing the file's outline.
 *
 * @param textState read-only state holder for the editor's current text.
 *                  The drawer re-extracts the outline whenever the text
 *                  or langId changes.
 * @param langId    the active LanguageFormat id, or null if the file's
 *                  format has no grammar. A null langId clears the
 *                  outline and shows the empty-placeholder.
 * @param engine    the [TokenizerEngine] used by [OutlineExtractor].
 *                  The caller is responsible for initialize() +
 *                  loadGrammar() — the drawer does NOT lazy-init.
 * @param isOpen    when false the drawer renders nothing (the editor
 *                  reclaims the full width).
 * @param onClose   invoked when the user taps the close button in the
 *                  drawer header.
 * @param onItemClick invoked with the tapped [OutlineItem]. The caller
 *                  typically scrolls the editor to `item.startByte`.
 */
@Composable
fun OutlineDrawer(
    textState: State<String>,
    langId: String?,
    engine: TokenizerEngine,
    isOpen: Boolean,
    onClose: () -> Unit,
    onItemClick: (OutlineItem) -> Unit,
) {
    if (!isOpen) return

    val theme = LocalTheme.current
    val bg = theme.uiColor("sideBar.background")?.let { Color(it) }
        ?: Color(0xFF252526)
    val fg = theme.uiColor("sideBar.foreground")?.let { Color(it) }
        ?: Color(0xFFCCCCCC)
    val border = theme.uiColor("sideBar.border")?.let { Color(it) }
        ?: Color(0xFF3C3C3C)

    val outline = remember { mutableStateOf<List<OutlineItem>>(emptyList()) }
    val extractor = remember { OutlineExtractor() }
    val currentText by textState

    // Re-extract on every text or langId change. The extractor body is
    // a suspend function; we run it inside the LaunchedEffect coroutine.
    // On failure (engine not initialized, grammar not loaded, parse
    // error) we set the outline to emptyList() — the user sees the
    // "No outline available" placeholder rather than a crashed panel.
    LaunchedEffect(currentText, langId) {
        if (langId == null) {
            outline.value = emptyList()
            return@LaunchedEffect
        }
        outline.value = try {
            extractor.outlineFor(currentText, langId, engine)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .testTag("outline.drawer")
            .width(280.dp)
            .fillMaxHeight()
            .background(bg)
            .padding(start = 0.dp),
    ) {
        // Header: title + close button.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(bg)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "OUTLINE",
                color = fg,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .testTag("outline.close")
                    .width(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close outline",
                    tint = fg,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(border),
        )

        // Body: outline rows OR empty-placeholder.
        if (outline.value.isEmpty()) {
            Box(
                modifier = Modifier
                    .testTag("outline.empty")
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = "No outline available for this language",
                    color = fg.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .testTag("outline.list")
                    .fillMaxWidth(),
            ) {
                items(outline.value) { item ->
                    OutlineRow(
                        item = item,
                        foreground = fg,
                        onClick = { onItemClick(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OutlineRow(
    item: OutlineItem,
    foreground: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .testTag("outline.item:${item.name}")
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            imageVector = kindToIcon(item.kind),
            contentDescription = "${item.kind} ${item.name}",
            tint = foreground.copy(alpha = 0.85f),
            modifier = Modifier.width(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.name,
            color = foreground,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}
