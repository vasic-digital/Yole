/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-55: IDE editor surface that keeps the line-number gutter and
 * text body locked to a single shared ScrollState, eliminating the
 * horizontal desync that occurred when both surfaces previously held
 * independent rememberScrollState() instances.
 *
 * iter-57 Phase 9: optional SyntaxHighlighter wiring. When a non-null
 * highlighter + langId is supplied AND the corresponding format is
 * enabled in EnabledFormatGate, the BasicTextField renders a colored
 * AnnotatedString via VisualTransformation, with an 80ms keystroke
 * debounce. Falls back to plain text on:
 *   - null highlighter / null langId (existing iter-55 callers unchanged),
 *   - format gated off, or
 *   - tokenize throwing (engine load failure on the platform).
 * Per spec §4 error table the editor MUST always render content even
 * when highlighting is unavailable — graceful degradation, never bluff
 * fake tokens (CONST-035).
 *
 * Anti-bluff covenants (CONST-035):
 *   (1) The iter-55 invariant: SyncedScrollEditor.kt declares EXACTLY
 *       ONE rememberScrollState() and both the gutter Column and
 *       BasicTextField apply verticalScroll() to it. Reverting to
 *       two independent ScrollStates MUST cause
 *       EditorScrollSyncRobolectricTest to fail.
 *   (2) iter-57 highlighting invariant: when a non-null highlighter +
 *       langId is passed and the format is enabled, the rendered
 *       AnnotatedString MUST carry the same span styles produced by
 *       SyntaxHighlighter.highlight(). Replacing the
 *       VisualTransformation with `VisualTransformation.None`, or
 *       hardcoding the highlighter argument to null at the call site,
 *       MUST cause EditorHighlightingRobolectricTest to fail.
 *
 *########################################################*/
package digital.vasic.yole.android.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import digital.vasic.yole.syntax.EnabledFormatGate
import digital.vasic.yole.syntax.SyntaxHighlighter
import kotlinx.coroutines.delay

@Composable
fun SyncedScrollEditor(
    textState: MutableState<String>,
    showLineNumbers: Boolean,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onTextChanged: (String) -> Unit = {},
    semanticsLabel: String? = null,
    placeholder: String? = null,
    textStyle: TextStyle = TextStyle(
        color = if (isDarkTheme) Color(0xFFD4D4D4) else Color(0xFF1E1E1E),
        fontSize = 14.sp,
        fontFamily = FontFamily.Monospace,
        lineHeight = 20.sp,
    ),
    highlighter: SyntaxHighlighter? = null,
    langId: String? = null,
) {
    val sharedScroll = rememberScrollState()

    // iter-57 Phase 9: per-recomposition highlighted AnnotatedString.
    // Tokenization runs in a LaunchedEffect with an 80ms debounce so
    // rapid keystrokes coalesce into a single tokenize. On disabled
    // format / null highlighter / tokenize failure, the value is the
    // plain-text AnnotatedString — preserving graceful degradation
    // (spec §4) without fake token styling (CONST-035).
    val highlightedText = remember(langId) {
        mutableStateOf<AnnotatedString>(AnnotatedString(textState.value))
    }
    LaunchedEffect(textState.value, langId, highlighter) {
        val text = textState.value
        if (highlighter != null && langId != null && EnabledFormatGate.isEnabled(langId)) {
            delay(80) // debounce
            highlightedText.value = try {
                highlighter.highlight(text, langId)
            } catch (_: Throwable) {
                AnnotatedString(text)
            }
        } else {
            highlightedText.value = AnnotatedString(text)
        }
    }

    Row(modifier = modifier.fillMaxSize()) {
        if (showLineNumbers) {
            val lines = textState.value.lines()
            val gutterWidth = when {
                lines.size >= 1000 -> 48.dp
                lines.size >= 100 -> 40.dp
                else -> 32.dp
            }
            val gutterBg = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFF8F8F8)
            val gutterFg = if (isDarkTheme) Color(0xFF858585) else Color(0xFF999999)

            Column(
                modifier = Modifier
                    .testTag("syncedScrollEditor.gutter")
                    .width(gutterWidth)
                    .fillMaxHeight()
                    .background(gutterBg)
                    .verticalScroll(sharedScroll)
                    .padding(top = 8.dp, end = 4.dp),
                horizontalAlignment = Alignment.End,
            ) {
                lines.forEachIndexed { idx, _ ->
                    Text(
                        text = "${idx + 1}",
                        color = gutterFg,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp,
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // iter-57 Phase 9: VisualTransformation overlays the
            // tokenized AnnotatedString on the underlying plain-text
            // value, preserving the BasicTextField's edit semantics
            // (cursor position, IME, etc.) via OffsetMapping.Identity.
            // The transformation re-reads highlightedText on each
            // recomposition; the LaunchedEffect above pushes new
            // values 80ms after a keystroke quiesces.
            val highlight = highlightedText.value
            // The LaunchedEffect debounces tokenization by 80ms, so between
            // a keystroke and the next tokenize the cached `highlight` may
            // not match the current text length. BasicTextField requires
            // OffsetMapping to be valid for the CURRENT text length; using
            // a stale AnnotatedString of a different length throws
            // IllegalStateException. Guard by applying the styled overlay
            // ONLY when lengths agree; otherwise pass through plain text
            // (no highlighting flicker for one frame — preferable to crash).
            // The flicker disappears as soon as the debounce fires.
            val highlightingTransform = remember(highlight) {
                VisualTransformation { sourceText ->
                    val overlay = if (highlight.text.length == sourceText.text.length) {
                        highlight
                    } else {
                        AnnotatedString(sourceText.text)
                    }
                    TransformedText(overlay, OffsetMapping.Identity)
                }
            }
            BasicTextField(
                value = textState.value,
                onValueChange = { newValue ->
                    textState.value = newValue
                    onTextChanged(newValue)
                },
                modifier = Modifier
                    .testTag("syncedScrollEditor.editor")
                    .fillMaxSize()
                    .verticalScroll(sharedScroll)
                    .padding(8.dp)
                    .let { m ->
                        if (semanticsLabel != null) {
                            m.semantics { contentDescription = semanticsLabel }
                        } else {
                            m
                        }
                    },
                textStyle = textStyle,
                visualTransformation = highlightingTransform,
            )
            if (placeholder != null && textState.value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = if (isDarkTheme) Color(0xFF666666) else Color(0xFF999999),
                    fontSize = textStyle.fontSize,
                    fontFamily = textStyle.fontFamily,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}
