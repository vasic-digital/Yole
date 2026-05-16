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
 * iter-58 Feature 2 Phase 4: editor affordances. Reads
 * LocalLanguage.current and wires three handlers:
 *   - Ctrl+/ (or Cmd+/) → CommentToggleAction.toggleCommentOnSelectedLines
 *   - Enter            → IndentEngine.handleEnter
 *   - onValueChange    → BracketAutoCompleter.applyBracketAutocomplete
 * The public API still accepts a MutableState<String> for caller
 * compatibility; internally the editor manages a TextFieldValue so
 * selection-aware affordances have a real cursor + range to act on.
 *
 * iter-58 Feature 2 Phase 5: FoldGutter integration. When a non-null
 * langId + tokenizerEngine are supplied, the gutter Column renders a
 * fold-chevron next to every line that starts a [FoldRange] returned
 * by [FoldQueryRunner]. The chevron flips between down-arrow (expanded)
 * and right-arrow (collapsed) on tap. Per CONST-035 anti-bluff: this
 * phase ships only the visible-and-functional chevron — the actual
 * text-collapse of the BasicTextField body is a deferred follow-up
 * (`#f2-phase-5-fold-region-collapse`) because the implementation
 * requires a non-trivial VisualTransformation + OffsetMapping rewrite
 * that risks regressing the existing iter-57 highlighting length-guard
 * pattern. Shipping the chevron without the body-collapse is honest
 * (the user sees a working affordance whose backend is queued, not a
 * fake outline that pretends to fold but doesn't).
 *
 * iter-60 Phase 6.4: auto-complete popup overlay. When non-null
 * [completionTrigger] and [completionPopupState] are supplied:
 *   - The BTF's onValueChange feeds trigger.onTextChanged after the
 *     existing bracket-autocomplete propagation.
 *   - A LaunchedEffect collects trigger.events, drives CompletionEngine
 *     calls, and mutates the popupState.
 *   - Ctrl+Space → trigger.onExplicitTrigger.
 *   - Esc (when popup open) → trigger.onDismiss.
 *   - Arrow-Down / Arrow-Up → popupState.moveSelection(±1).
 *   - Enter/Tab (when popup open) → commit selected item.
 *   - CompletionPopup renders as an overlay inside the editor Box.
 *
 * iter-60 Phase 8b: snippet placeholder navigation. When a committed item
 * is of kind Snippet:
 *   - commitCompletionItem runs VsCodeSnippetExpander.expand on the body,
 *     inserts strippedBody (not the raw body with `${N:...}` markers),
 *     constructs a SnippetPlaceholderNavigator, and calls advance() to
 *     select the first placeholder.
 *   - The navigator is stored in snippetNavigatorState (mutableStateOf).
 *   - Tab (when no popup open but navigator isActive) → advance() to next
 *     placeholder; if null, deactivate and fall through.
 *   - Esc (when navigator is active) → complete() + fall through.
 *
 * iter-62 Phase 8: LSP diagnostics + hover wired into the editor surface.
 *   - [diagnostics]: optional list of Diagnostic for the current file.
 *     When non-empty, DiagnosticsGutter renders severity dots next to the
 *     fold-chevron column, and DiagnosticsInlineUnderline is LAYERED on
 *     top of the syntax-highlighting VisualTransformation (highlighter
 *     first → underline on top; length-guard still applies to both).
 *   - [onHoverRequest]: optional suspend callback invoked when F1 is
 *     pressed or a long-press triggers explicit hover.
 *
 * The VisualTransformation length-guard (iter-57) is preserved: the popup
 * is a separate Popup composable and does NOT feed back into the BTF's
 * VisualTransformation. Commit inserts text via onValueChange path.
 * DiagnosticsInlineUnderline is applied AFTER the highlight layer so the
 * underline takes priority visually; the length guard in the highlight
 * transform remains the innermost guard.
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
 *   (3) iter-58 Phase 4 invariant: the BasicTextField's onValueChange
 *       MUST pipe through applyBracketAutocomplete, and the modifier
 *       MUST attach the commentToggle + indentEngine key handlers.
 *       Stubbing those helpers to return-input is caught by the
 *       three new Robolectric tests.
 *   (4) iter-58 Phase 5 invariant: when a non-null tokenizerEngine +
 *       langId are passed, the gutter row for every line that starts
 *       a FoldRange MUST render a chevron with testTag
 *       `foldGutter.chevron:line{N}`. Stubbing FoldQueryRunner to
 *       return emptyList() MUST fail `chevronsAppearOnFoldableLines`.
 *   (5) iter-60 Phase 8b invariant: commitCompletionItem for Snippet kind
 *       MUST insert strippedBody (not the raw body) and select the first
 *       placeholder. Stubbing navigator.advance() to always return null
 *       MUST cause SnippetExpansionRobolectricTest Phase 8 cases to fail.
 *   (6) iter-62 Phase 8 invariant: when diagnostics is non-empty,
 *       DiagnosticsGutter is composed in the gutter column and
 *       DiagnosticsInlineUnderline is applied as a chained
 *       VisualTransformation. Removing either call MUST cause
 *       IdeEditorScreenLspIntegrationRobolectricTest test 1 to fail.
 *
 *########################################################*/
package digital.vasic.yole.android.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import digital.vasic.yole.android.ui.editor.diagnostics.DiagnosticsGutter
import digital.vasic.yole.android.ui.editor.diagnostics.DiagnosticsInlineUnderline
import digital.vasic.yole.android.ui.editor.hover.hoverShortcut
import digital.vasic.yole.completion.CompletionItem
import digital.vasic.yole.completion.snippet.SnippetPlaceholderNavigator
import digital.vasic.yole.completion.snippet.VsCodeSnippetExpander
import digital.vasic.yole.completion.trigger.CompletionTrigger
import digital.vasic.yole.completion.trigger.TriggerEvent
import digital.vasic.yole.language.LocalLanguage
import digital.vasic.yole.language.affordance.FoldRange
import digital.vasic.yole.lsp.Diagnostic
import digital.vasic.yole.syntax.EnabledFormatGate
import digital.vasic.yole.syntax.SyntaxHighlighter
import digital.vasic.yole.syntax.TokenizerEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

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
    tokenizerEngine: TokenizerEngine? = null,
    completionTrigger: CompletionTrigger? = null,
    completionPopupState: CompletionPopupState? = null,
    completionEngine: digital.vasic.yole.completion.CompletionEngine? = null,
    // iter-62 Phase 8: LSP diagnostics for the current file. When non-empty,
    // DiagnosticsGutter renders severity dots in the gutter column and
    // DiagnosticsInlineUnderline overlays colored underlines on the BTF.
    diagnostics: List<Diagnostic> = emptyList(),
    // iter-62 Phase 8: invoked when F1 is pressed (or equivalent explicit
    // hover trigger). The cursor line/character are extracted from tfvState
    // at call time. Null = hover not wired (callers that predate Phase 8).
    onHoverRequest: (() -> Unit)? = null,
) {
    val sharedScroll = rememberScrollState()
    val activeLanguage = LocalLanguage.current

    // iter-58 Phase 5: per-editor session state of currently-collapsed
    // fold ranges. Tapping a chevron toggles the matching range in this
    // set. Body-collapse of the BasicTextField is deferred — see
    // FoldGutter.kt's KDoc and `#f2-phase-5-fold-region-collapse`.
    val foldedRanges = remember { mutableStateOf<Set<FoldRange>>(emptySet()) }

    // iter-60 Phase 8b: active snippet placeholder navigator.
    // Non-null between snippet commit and the final Tab/Esc dismissal.
    // Null when no snippet traversal is in progress (the common case).
    val snippetNavigatorState = remember { mutableStateOf<SnippetPlaceholderNavigator?>(null) }

    // iter-58 Feature 2 Phase 4: maintain an internal TextFieldValue so
    // affordances (comment-toggle, indent-on-Enter, bracket-autocomplete)
    // can act on real selection ranges. The public API stays
    // MutableState<String> — we keep them mirrored.
    val tfvState = remember { mutableStateOf(TextFieldValue(textState.value, TextRange(textState.value.length))) }
    // External callers may rewrite textState (e.g., undo/redo). Reconcile
    // by re-seeding the TextFieldValue with the new text while preserving
    // the cursor if it is still in range.
    if (tfvState.value.text != textState.value) {
        val newText = textState.value
        val safeCursor = tfvState.value.selection.end.coerceIn(0, newText.length)
        tfvState.value = TextFieldValue(newText, TextRange(safeCursor))
    }

    // iter-58: comment-toggle (Ctrl+/ or Cmd+/) and indent-on-Enter
    // handlers, parameterized by the active LanguageFormat. They are
    // remembered so the same handler instance is attached to the modifier
    // across recompositions.
    val commentToggleHandler = rememberCommentToggleAction(tfvState, activeLanguage)
    val indentEngineHandler = rememberIndentEngineAction(tfvState, activeLanguage)

    // iter-57 Phase 9: per-recomposition highlighted AnnotatedString.
    // Tokenization runs in a LaunchedEffect with an 80ms debounce so
    // rapid keystrokes coalesce into a single tokenize. On disabled
    // format / null highlighter / tokenize failure, the value is the
    // plain-text AnnotatedString — preserving graceful degradation
    // (spec §4) without fake token styling (CONST-035).
    val highlightedText = remember(langId) {
        mutableStateOf<AnnotatedString>(AnnotatedString(textState.value))
    }
    LaunchedEffect(tfvState.value.text, langId, highlighter) {
        val text = tfvState.value.text
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

    // iter-58 Phase 5: compute fold ranges for the whole document ONCE
    // per (text, langId) change, then pass per-line slices to FoldGutter
    // inside the gutter Column. When no engine is supplied (callers
    // that pre-date Phase 5 or platforms without a working tokenizer)
    // the fold list stays empty and the gutter renders only line
    // numbers — graceful degradation per CONST-035.
    val foldRangesState: androidx.compose.runtime.State<List<FoldRange>> = if (tokenizerEngine != null) {
        rememberFoldRanges(tfvState.value.text, langId, tokenizerEngine)
    } else {
        remember { mutableStateOf<List<FoldRange>>(emptyList()) }
    }
    val foldRanges by foldRangesState

    // iter-60 Phase 6.4: collect CompletionTrigger events and drive the
    // popupState + engine calls. This LaunchedEffect is a no-op when
    // completionTrigger or completionPopupState are null (existing callers
    // unchanged). The flow collection runs on Main per LaunchedEffect
    // semantics — safe to mutate Compose state directly.
    val popupStateForEffect = completionPopupState
    val triggerForEffect = completionTrigger
    val engineForEffect = completionEngine
    if (popupStateForEffect != null && triggerForEffect != null) {
        LaunchedEffect(triggerForEffect) {
            triggerForEffect.events.collect { event ->
                when (event) {
                    is TriggerEvent.Show -> {
                        if (engineForEffect != null) {
                            engineForEffect.complete(event.context).collectLatest { items ->
                                popupStateForEffect.show(items, event.context.cursorChar)
                            }
                        } else {
                            popupStateForEffect.hide()
                        }
                    }
                    is TriggerEvent.Update -> {
                        if (engineForEffect != null) {
                            engineForEffect.complete(event.context).collectLatest { items ->
                                popupStateForEffect.update(items)
                            }
                        }
                    }
                    TriggerEvent.Hide -> popupStateForEffect.hide()
                }
            }
        }
    }

    Row(modifier = modifier.fillMaxSize()) {
        if (showLineNumbers) {
            val lines = tfvState.value.text.lines()
            val gutterWidth = when {
                lines.size >= 1000 -> 72.dp
                lines.size >= 100 -> 64.dp
                else -> 56.dp
            }
            val gutterBg = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFF8F8F8)
            val gutterFg = if (isDarkTheme) Color(0xFF858585) else Color(0xFF999999)
            val chevronTint = if (isDarkTheme) Color(0xFFA0A0A0) else Color(0xFF606060)

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
                    Row(
                        modifier = Modifier.height(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // iter-58 Phase 5: fold-chevron affordance. The
                        // gutter row reserves a 16dp slot whether or not
                        // this line starts a FoldRange (see FoldGutter
                        // for the empty-slot Box) so line numbers stay
                        // vertically aligned.
                        FoldGutter(
                            lineNumber = idx + 1,
                            ranges = foldRanges,
                            foldedRanges = foldedRanges.value,
                            iconTint = chevronTint,
                            onToggleFold = { range -> toggleFold(foldedRanges, range) },
                        )
                        // iter-62 Phase 8: diagnostics severity dot for
                        // this line. DiagnosticsGutter manages its own
                        // vertical layout but here we reuse the per-line
                        // Row to emit a single dot. When no diagnostic
                        // matches this line, DiagnosticsGutter renders
                        // nothing for the slot. We pass a single-line
                        // view of diagnostics filtered to this line.
                        if (diagnostics.isNotEmpty()) {
                            val lineDiags = diagnostics.filter { diag ->
                                digital.vasic.yole.android.ui.editor.diagnostics
                                    .offsetToLine(tfvState.value.text, diag.range.first) == idx
                            }
                            DiagnosticsGutter(
                                diagnostics = lineDiags,
                                textForLineNumberMapping = tfvState.value.text,
                                lineHeight = 20.dp,
                                modifier = Modifier.testTag("diag-gutter-row-$idx"),
                            )
                        }
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
            // iter-62 Phase 8: compose DiagnosticsInlineUnderline ON TOP of the
            // syntax-highlight layer. The underline transform runs AFTER the
            // highlight transform so severity colors take visual priority over
            // token colors. The length-guard from iter-57 lives in the highlight
            // step (inner transform); the underline step only adds SpanStyles and
            // does not change text length so OffsetMapping.Identity is safe for both.
            val diagForTransform = diagnostics
            val highlightingTransform = remember(highlight, diagForTransform, isDarkTheme) {
                VisualTransformation { sourceText ->
                    // Step 1 — syntax highlight (with length guard).
                    val overlay = if (highlight.text.length == sourceText.text.length) {
                        highlight
                    } else {
                        AnnotatedString(sourceText.text)
                    }
                    val afterHighlight = TransformedText(overlay, OffsetMapping.Identity)
                    // Step 2 — diagnostics underline layered on top.
                    if (diagForTransform.isNotEmpty()) {
                        DiagnosticsInlineUnderline(diagForTransform, isDarkTheme)
                            .filter(afterHighlight.text)
                    } else {
                        afterHighlight
                    }
                }
            }
            BasicTextField(
                value = tfvState.value,
                onValueChange = { newValue ->
                    // iter-58 Phase 4: bracket auto-completion is applied
                    // BEFORE we propagate the value upward. This way
                    // pasted text (delta > 1) is unaffected, but a single
                    // typed opener triggers the closer.
                    val oldValue = tfvState.value
                    val transformed = applyBracketAutocomplete(oldValue, newValue, activeLanguage)
                    tfvState.value = transformed
                    if (transformed.text != textState.value) {
                        textState.value = transformed.text
                        onTextChanged(transformed.text)
                    }
                    // iter-60 Phase 6.4: feed completion trigger after
                    // propagation so the trigger sees the post-bracket text.
                    completionTrigger?.onTextChanged(
                        transformed.text,
                        transformed.selection.end,
                    )
                },
                modifier = Modifier
                    .testTag("syncedScrollEditor.editor")
                    .fillMaxSize()
                    .verticalScroll(sharedScroll)
                    .padding(8.dp)
                    // iter-62 Phase 8: F1 explicit hover shortcut.
                    // Attached before onPreviewKeyEvent so F1 is
                    // intercepted by hoverShortcut before other handlers.
                    .let { m ->
                        val hoverCb = onHoverRequest
                        if (hoverCb != null) m.hoverShortcut(hoverCb) else m
                    }
                    .onPreviewKeyEvent { event ->
                        // iter-60 Phase 6.4: completion keyboard handlers.
                        // Checked FIRST so popup navigation takes priority
                        // over the editor's own key handling.
                        val ps = completionPopupState
                        val trig = completionTrigger
                        if (ps != null && trig != null && event.type == KeyEventType.KeyDown) {
                            when {
                                // Ctrl+Space — explicit trigger.
                                event.isCtrlPressed && event.key == Key.Spacebar -> {
                                    trig.onExplicitTrigger()
                                    true
                                }
                                // Esc — dismiss popup if open; also clear snippet navigator.
                                event.key == Key.Escape && ps.isOpen -> {
                                    snippetNavigatorState.value?.complete()
                                    snippetNavigatorState.value = null
                                    trig.onDismiss()
                                    true
                                }
                                // Arrow-Down — move selection down (popup must be open).
                                event.key == Key.DirectionDown && ps.isOpen -> {
                                    ps.moveSelection(1)
                                    true
                                }
                                // Arrow-Up — move selection up (popup must be open).
                                event.key == Key.DirectionUp && ps.isOpen -> {
                                    ps.moveSelection(-1)
                                    true
                                }
                                // Enter or Tab — commit selected item.
                                (event.key == Key.Enter || event.key == Key.Tab) && ps.isOpen -> {
                                    val item = ps.items.getOrNull(ps.selectedIndex)
                                    if (item != null) {
                                        commitCompletionItem(
                                            item, tfvState, textState, onTextChanged, trig,
                                            snippetNavigatorState,
                                        )
                                    }
                                    true
                                }
                                else -> false
                            }
                        // iter-60 Phase 8b: snippet placeholder Tab traversal.
                        // Checked BEFORE popup handlers when popup is closed,
                        // AFTER popup block when popup is open (popup takes priority).
                        } else if (event.type == KeyEventType.KeyDown && event.key == Key.Tab) {
                            val nav = snippetNavigatorState.value
                            if (nav != null && nav.isActive()) {
                                val next = nav.advance()
                                if (next != null) {
                                    // Select the next placeholder range.
                                    tfvState.value = tfvState.value.copy(
                                        selection = TextRange(next.first, next.last + 1),
                                    )
                                    true
                                } else {
                                    // Navigation exhausted — deactivate and fall through.
                                    snippetNavigatorState.value = null
                                    false
                                }
                            } else {
                                false
                            }
                        // iter-60 Phase 8b: Esc clears snippet navigator when popup not open.
                        } else if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                            val nav = snippetNavigatorState.value
                            if (nav != null) {
                                nav.complete()
                                snippetNavigatorState.value = null
                                true
                            } else {
                                false
                            }
                        } else if (
                            // iter-58 Phase 4: try comment-toggle first
                            // (Ctrl+/), then indent-on-Enter. Either handler
                            // returns true to consume the key; otherwise we
                            // let BasicTextField receive it as normal.
                            commentToggleHandler(event)
                        ) {
                            if (tfvState.value.text != textState.value) {
                                textState.value = tfvState.value.text
                                onTextChanged(tfvState.value.text)
                            }
                            true
                        } else if (indentEngineHandler(event)) {
                            if (tfvState.value.text != textState.value) {
                                textState.value = tfvState.value.text
                                onTextChanged(tfvState.value.text)
                            }
                            true
                        } else {
                            false
                        }
                    }
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
            if (placeholder != null && tfvState.value.text.isEmpty()) {
                Text(
                    text = placeholder,
                    color = if (isDarkTheme) Color(0xFF666666) else Color(0xFF999999),
                    fontSize = textStyle.fontSize,
                    fontFamily = textStyle.fontFamily,
                    modifier = Modifier.padding(8.dp),
                )
            }
            // iter-60 Phase 6.4: completion popup overlay. Rendered as a
            // Popup so it floats above the keyboard and IME. The trigger
            // and popupState are both hoisted by the caller (IdeEditorScreen)
            // so the toolbar button can also call trigger.onExplicitTrigger.
            val popupState = completionPopupState
            val trigger = completionTrigger
            if (popupState != null && trigger != null) {
                CompletionPopup(
                    state = popupState,
                    isDarkTheme = isDarkTheme,
                    onCommit = { item ->
                        commitCompletionItem(
                            item, tfvState, textState, onTextChanged, trigger,
                            snippetNavigatorState,
                        )
                    },
                    onDismiss = { trigger.onDismiss() },
                )
            }
        }
    }
}

/**
 * Insert [item].insertText into the editor, replacing the partial prefix
 * captured by [item].range, then close the popup and reset the trigger.
 *
 * For [CompletionItem.Kind.Snippet] items the body is first expanded via
 * [VsCodeSnippetExpander]: `${N:default}` markers are stripped to their
 * default text, and a [SnippetPlaceholderNavigator] is created and stored
 * in [snippetNavigatorState]. The first placeholder is selected immediately.
 *
 * For non-snippet items the cursor is placed at the end of the inserted text.
 *
 * This function is internal to the editor package; it is extracted so both
 * the onPreviewKeyEvent handler and the CompletionPopup click callback
 * share the same implementation (DRY + single testable mutation surface).
 *
 * Anti-bluff anchor (CONST-035):
 *   Stubbing this to a no-op → SnippetExpansionRobolectricTest FAILS
 *   because the editor text remains unchanged after the commit.
 *   Stubbing navigator.advance() to always return null → Phase 8b
 *   Robolectric cases FAIL because the selection stays at end-of-insert.
 */
internal fun commitCompletionItem(
    item: CompletionItem,
    tfvState: androidx.compose.runtime.MutableState<TextFieldValue>,
    textState: androidx.compose.runtime.MutableState<String>,
    onTextChanged: (String) -> Unit,
    trigger: CompletionTrigger,
    snippetNavigatorState: androidx.compose.runtime.MutableState<SnippetPlaceholderNavigator?>? = null,
) {
    val current = tfvState.value
    val text = current.text
    // Clamp the item's range to the actual text boundaries to guard against
    // stale ranges (text may have changed between Show and commit).
    val start = item.range.first.coerceIn(0, text.length)
    val end = item.range.last.coerceIn(start, text.length)

    if (item.kind == CompletionItem.Kind.Snippet && snippetNavigatorState != null) {
        // Snippet path: expand placeholders, insert strippedBody, navigate.
        val expansion = VsCodeSnippetExpander.expand(item.insertText)
        val insertedText = expansion.strippedBody
        val newText = text.substring(0, start) + insertedText + text.substring(end)
        val endOfInsert = start + insertedText.length

        val navigator = SnippetPlaceholderNavigator(expansion, baseOffset = start)
        snippetNavigatorState.value = navigator

        // Select first placeholder, or place cursor at end if none.
        val firstRange = if (expansion.placeholders.isNotEmpty()) navigator.advance() else null
        val newSelection = if (firstRange != null) {
            TextRange(firstRange.first, firstRange.last + 1)
        } else {
            TextRange(endOfInsert)
        }
        val newValue = TextFieldValue(newText, newSelection)
        tfvState.value = newValue
        textState.value = newText
        onTextChanged(newText)
    } else {
        // Non-snippet path (or no navigator state available): plain insert.
        val newText = text.substring(0, start) + item.insertText + text.substring(end)
        val newCursor = start + item.insertText.length
        val newValue = TextFieldValue(newText, TextRange(newCursor))
        tfvState.value = newValue
        textState.value = newText
        onTextChanged(newText)
    }
    trigger.onDismiss()
}
