/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 Feature 2 Phase 5: FoldGutter — chevron icons rendered in
 * the editor's left gutter for lines that start a foldable region.
 *
 * The v1 shipped in this phase renders only the user-visible CHEVRON
 * AFFORDANCE (down-arrow when expanded, right-arrow when collapsed).
 * The actual body-collapsing of fold regions inside BasicTextField is
 * a deferred follow-up tracked as `#f2-phase-5-fold-region-collapse`
 * because the implementation requires:
 *   1. A custom VisualTransformation + OffsetMapping that elides the
 *      collapsed byte ranges from the displayed text without breaking
 *      the iter-57 highlighting length-guard pattern in
 *      SyncedScrollEditor.kt (the existing transform is conservative:
 *      when lengths disagree, fall back to plain text);
 *   2. Cursor-position re-mapping so editing across folded regions
 *      remains coherent;
 *   3. Per-fold collapsed-summary rendering (e.g. "{...}" placeholder).
 *
 * Per the user mandate ("CONST-035 anti-bluff: ship the visible-and-
 * functional chevron without faking the body-collapse behavior"), the
 * chevron toggles its visual state and invokes [onToggleFold] with the
 * actual FoldRange — the same callback any future body-collapse
 * implementation will consume. No fake collapse is performed.
 *
 * Anti-bluff covenant (CONST-035): stubbing FoldQueryRunner.foldRangesFor
 * to `return emptyList()` MUST cause the `chevronsAppearOnFoldableLines`
 * test in FoldGutterRobolectricTest to FAIL because no
 * `foldGutter.chevron:line*` test-tag nodes would be rendered for
 * a markdown document with a fenced code block.
 *
 * Cross-platform impact (CONST-037): Phase 5 ships Android only.
 * Desktop/iOS/web ports of the fold gutter (and the deferred fold-region-
 * collapse) are tracked as parallel sub-tasks in the Phase 5 plan
 * section. They reuse the same shared FoldQueryRunner API.
 *
 *########################################################*/
package digital.vasic.yole.android.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import digital.vasic.yole.language.affordance.FoldQueryRunner
import digital.vasic.yole.language.affordance.FoldRange
import digital.vasic.yole.syntax.TokenizerEngine

/**
 * Composable host for fold-range computation. Returns a [State] holding
 * the currently-known list of [FoldRange]s. The result re-computes
 * whenever (text, langId) change.
 *
 * Hoisted into its own helper so SyncedScrollEditor can compute the
 * full fold list ONCE per recomposition and pass per-line slices to
 * [FoldGutter] per row, rather than re-running the query for each
 * gutter row.
 *
 * On any failure (engine not initialized, grammar not loaded, parse
 * error) the state holds an empty list — the editor degrades to no
 * chevrons rather than crashing.
 */
@Composable
fun rememberFoldRanges(
    text: String,
    langId: String?,
    engine: TokenizerEngine,
): State<List<FoldRange>> {
    val state = remember { mutableStateOf<List<FoldRange>>(emptyList()) }
    val runner = remember { FoldQueryRunner() }
    LaunchedEffect(text, langId) {
        if (langId == null) {
            state.value = emptyList()
            return@LaunchedEffect
        }
        state.value = try {
            runner.foldRangesFor(text, langId, engine)
        } catch (_: Throwable) {
            emptyList()
        }
    }
    return state
}

/**
 * Renders the per-line fold-gutter affordance:
 *   - Nothing if no [FoldRange] starts at [lineNumber];
 *   - A down-arrow chevron when the matching fold is expanded;
 *   - A right-arrow chevron when the matching fold is collapsed.
 *
 * Tapping the chevron invokes [onToggleFold] with the matching range;
 * the editor session is responsible for tracking the folded-set
 * (`MutableState<Set<FoldRange>>` in SyncedScrollEditor).
 *
 * @param lineNumber 1-indexed line number for this gutter row.
 *                   The FoldRange list uses 0-indexed startLine, so
 *                   the check is `range.startLine == lineNumber - 1`.
 * @param foldedRanges the editor's session set of currently-collapsed
 *                   ranges. Used to pick the chevron direction.
 * @param ranges     the precomputed list of FoldRanges for the whole
 *                   document. Passed in rather than recomputed because
 *                   the gutter Column iterates lines and we want O(1)
 *                   per-row lookup, not O(parse) per row.
 * @param iconTint   chevron color (themed via LocalTheme by caller).
 * @param onToggleFold invoked with the FoldRange that starts at this
 *                   line, when the user taps the chevron.
 */
@Composable
fun FoldGutter(
    lineNumber: Int,
    ranges: List<FoldRange>,
    foldedRanges: Set<FoldRange>,
    iconTint: Color,
    onToggleFold: (FoldRange) -> Unit,
) {
    val matching = ranges.firstOrNull { it.startLine == lineNumber - 1 }
    if (matching == null) {
        // Reserve the same gutter slot so line numbers stay aligned
        // whether or not this row has a fold affordance.
        Box(modifier = Modifier.size(16.dp))
        return
    }
    val isFolded = matching in foldedRanges
    Box(
        modifier = Modifier
            .testTag("foldGutter.chevron:line$lineNumber")
            .size(16.dp)
            .clickable { onToggleFold(matching) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isFolded) {
                Icons.Filled.KeyboardArrowRight
            } else {
                Icons.Filled.KeyboardArrowDown
            },
            contentDescription = if (isFolded) {
                "Expand fold at line $lineNumber"
            } else {
                "Collapse fold at line $lineNumber"
            },
            tint = iconTint,
            modifier = Modifier.size(14.dp),
        )
    }
}

/**
 * Convenience handler factory for the gutter's onToggleFold callback.
 * Maintains a [MutableState] of currently-folded ranges. Adding /
 * removing the tapped range mutates the set so the chevron flips on
 * the next recomposition.
 *
 * NOTE: per the deferred follow-up `#f2-phase-5-fold-region-collapse`,
 * mutating this set does NOT yet drive any text-collapse in
 * BasicTextField. Future work will wire a VisualTransformation that
 * elides the collapsed byte ranges.
 */
fun toggleFold(state: MutableState<Set<FoldRange>>, range: FoldRange) {
    val current = state.value
    state.value = if (range in current) current - range else current + range
}
