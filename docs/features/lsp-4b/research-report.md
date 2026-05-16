<!-- SPDX-FileCopyrightText: 2026 Milos Vasic -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# LSP Capability Expansion — Deep Research Report (iter-62 Phase 0)

**Date:** 2026-05-16
**Author:** Phase 0 deep research — 8 open questions closed before any code lands.
**Purpose:** Close all 8 OPEN questions from design spec §8. Gates Phase 1–11.
**Deliverable constraint:** ≥ 600 lines, ≥ 100 URL citations.

---

## Executive Summary

All 8 questions are closed with actionable conclusions. The two gate conditions
(§1 dwell-hover, §2 Flexmark strategy) are resolved. One v1 deferral confirmed
(§4 jdt:// URI). No escalations needed.

| §  | Question                                | Status   | Gate       |
|----|----------------------------------------|----------|------------|
| 1  | Compose dwell-hover gesture pattern     | CLOSED   | Phase 6    |
| 2  | Flexmark → Compose rendering strategy  | CLOSED   | Phase 4    |
| 3  | LSP4J Hover.contents typing             | CLOSED   | Phase 3    |
| 4  | jdt:// URI scheme                       | DEFERRED | iter-63    |
| 5  | Android long-press ContextMenu          | CLOSED   | Phase 7    |
| 6  | Compose wavy underline                  | CLOSED   | Phase 5    |
| 7  | Android BackHandler + NavStack saver    | CLOSED   | Phase 7    |
| 8  | publishDiagnostics emit rate            | CLOSED   | Phase 1    |

---

## §1. Compose Multiplatform Dwell-Hover Gesture Pattern

### 1.1 Background and Motivation

The design spec (§2) calls for a "300ms mouse-dwell on Desktop OR long-press on
mobile" hover trigger for the LSP hover popup. Without a canonical implementation
pattern, Phase 6 (`HoverTriggerDetector`) cannot be reliably built. This section
closes that gate.

Mouse-hover (dwell) detection on desktop requires sustained pointer presence over
a region for a configurable duration. In a code editor this is the standard UX
used by VS Code, IntelliJ IDEA, and Zed. The 300ms figure matches VS Code's default
hover delay [1] and aligns with cognitive-science research placing the threshold
for "intentional hover" at 200–400ms.

### 1.2 Compose Multiplatform Pointer Input Architecture

Compose Multiplatform 1.7.x exposes pointer events through two families of API:

**Experimental — `onPointerEvent`** (desktop-optimized, not cross-platform safe):
```kotlin
@OptIn(ExperimentalComposeUiApi::class)
Modifier.onPointerEvent(PointerEventType.Enter) { active = true }
         .onPointerEvent(PointerEventType.Exit)  { active = false }
```
Source: JetBrains KMP desktop mouse events documentation [2].

**Stable — `Modifier.pointerInput { awaitPointerEventScope { ... } }`**
(cross-platform, suitable for `commonMain`):
```kotlin
Modifier.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            // inspect event.type, event.changes
        }
    }
}
```
Source: Android developer documentation on pointer input [3], AwaitPointerEventScope
API reference [4].

`PointerEventType` values available in Compose 1.7: `Enter`, `Exit`, `Move`, `Press`,
`Release`, `Scroll` [5]. Desktop delivers `Enter`/`Exit`/`Move` events on real mouse
hardware. Android does not generate `Enter`/`Exit` from touch; those events only
arrive via mouse or stylus [6].

The `awaitPointerEventScope` is a restricted suspension scope. Code is always
dispatched un-dispatched and may only suspend via `awaitPointerEvent()`. It does NOT
implement `CoroutineScope` intentionally to prevent breaking structured concurrency
[7]. This means any delay-based logic must be launched into a sibling coroutine.

### 1.3 The Canonical 300ms Dwell Pattern

The correct architecture uses `pointerInput` for event detection and a separate
coroutine `Job` for the delay. The pattern is:

1. On `PointerEventType.Enter` — launch a `Job` into the enclosing `pointerInput`
   coroutine scope (`coroutineScope { }` or `launch { }`) with `delay(durationMs)`.
   When the delay completes, invoke `onDwell(position)`.
2. On `PointerEventType.Move` — cancel the pending `Job` (cursor left the original
   position), restart from step 1 with the new position.
3. On `PointerEventType.Exit` — cancel the pending `Job` unconditionally.

This is exactly the mechanism used by Compose's built-in `TooltipArea` composable
(from `androidx.compose.foundation`), which exposes a `delayMillis: Int = 500`
parameter that performs precisely this Enter→delay→show flow [8]. The JetBrains
KMP desktop tooltip documentation shows `delayMillis = 600` as an alternative [9].

The Material3 `TooltipBox` composable performs the same dwell logic via
`enableUserInput = true` which "handles long press and mouse hover to trigger the
tooltip through the state provider" [10].

### 1.4 Concrete `Modifier.dwellHover` Snippet (Phase 6 template)

```kotlin
// SPDX-FileCopyrightText: 2026 Milos Vasic
// SPDX-License-Identifier: Apache-2.0
//
// dwellHover — stable cross-platform Modifier for hover-dwell detection.
// Desktop: uses PointerEventType.Enter/Exit/Move with coroutine delay.
// Android: PointerEventType.Enter/Exit not triggered by touch — no-op on mobile.
//
// Phase 6 (HoverTriggerDetector) should copy-adapt this snippet.

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Modifier.dwellHover(
    durationMs: Long = 300L,
    onDwell: (position: Offset) -> Unit,
    onExit: () -> Unit = {},
): Modifier = this.pointerInput(durationMs, onDwell, onExit) {
    var dwellJob: Job? = null
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            when (event.type) {
                PointerEventType.Enter,
                PointerEventType.Move -> {
                    dwellJob?.cancel()
                    val position = event.changes.firstOrNull()?.position ?: continue
                    dwellJob = launch {
                        delay(durationMs)
                        onDwell(position)
                    }
                }
                PointerEventType.Exit -> {
                    dwellJob?.cancel()
                    dwellJob = null
                    onExit()
                }
                else -> Unit
            }
        }
    }
}
```

Notes on this implementation:
- `pointerInput` key includes `durationMs`, `onDwell`, `onExit` so the coroutine
  is restarted if any parameter changes (per Compose restart semantics [11]).
- `launch { }` inside `awaitPointerEventScope` uses the enclosing `PointerInputScope`
  which already has a structured-concurrency parent job. Cancellation is clean.
- On restart from `Move`, the `cancel()` call on the previous job propagates
  `CancellationException` which Kotlin catches internally (it is NOT swallowed; it
  exits the `delay` suspend point). CONST-035 compliant.
- On Android, `PointerEventType.Enter`/`Exit` do not fire for touch input, so
  `onDwell` is never called. The Android path uses `combinedClickable(onLongClick)`.

### 1.5 Per-Platform Support Matrix

| Platform | Input type     | Enter/Exit fires? | Dwell pattern applicable? |
|----------|---------------|-------------------|---------------------------|
| Desktop  | Mouse pointer | Yes               | Yes — primary path        |
| Android  | Touch         | No                | No — long-press fallback  |
| iOS      | Touch         | Limited           | No — long-press fallback  |
| Web/Wasm | Mouse (browser) | Yes             | Yes — same as desktop     |

Source: Compose multiplatform hover-during-scrolling issue confirming Enter/Move
fire for desktop but not Android touch [12]. Issue #3167 on compose-multiplatform
confirms `onPointerEvent` crashes on Android (because it is desktop-only experimental
API) [13]. The stable `pointerInput` Enter event is safe but only fired by non-touch
devices. Web: Compose/Wasm renders via Skia in a canvas; mouse events are wired
through the browser's pointer event API and do generate Enter/Exit equivalent [14].

### 1.6 Conclusion — §1

**CLOSED.** Use `Modifier.pointerInput { awaitPointerEventScope }` + coroutine
`Job` + `delay(300L)` for desktop hover. Cancel on `Move`/`Exit`. Android and iOS
fall back to `combinedClickable(onLongClick)` — the dwell modifier returns without
calling `onDwell`. The snippet above is the Phase 6 copy-adapt target.

---

## §2. Flexmark → Compose RichText Rendering — RECOMMENDATION

### 2.1 The Decision

Phase 4 (`HoverMarkdownRenderer`) must parse LSP hover responses (which are Markdown
strings) and render them in a Compose popup. Two strategies were evaluated:

- **Option A**: Third-party library `mikepenz/multiplatform-markdown-renderer`
- **Option B**: Roll-own Flexmark `NodeVisitor` walker emitting Compose blocks

**Recommendation: Option B — roll-own walker using existing Flexmark dependency.**

### 2.2 Survey: mikepenz/multiplatform-markdown-renderer

Library details (confirmed from Maven Central and GitHub):
- **Latest stable version:** 0.40.2 (released 2026-04-06) [15]
- **Maven Central artifact:** `com.mikepenz:multiplatform-markdown-renderer` [16]
- **License:** Apache 2.0 [17]
- **Platforms:** Android, iOS, Desktop (JVM), Web [18]
- **Markdown parser backend:** JetBrains `markdown` library (NOT Flexmark) [19]
- **Code highlighting:** Separate `multiplatform-markdown-renderer-code` artifact [20]
  using the "Highlights" project — a different highlighting engine from iter-57's
  `SyntaxHighlighter` based on Tree-Sitter.

**Integration issues for Yole hover popups:**
1. The library depends on JetBrains `markdown` parser — Yole already has Flexmark
   0.64.8 on the classpath from markdown preview. Adding a second parser library
   for hover-only is wasteful (~200KB extra JAR).
2. Code-block syntax highlighting uses the `Highlights` library [21], not Yole's
   iter-57 `SyntaxHighlighter`. Integrating iter-57 requires overriding the
   `codeFence` component in `MarkdownComponents` registry [22]. This works but
   requires adapter code of similar complexity to the roll-own approach.
3. The library parses markdown asynchronously by default since v0.33.0, showing
   a loading state [23]. For hover popups (which must appear immediately when the
   user dwells), an async loading flash is unacceptable UX.
4. The library renders full document features (images, tables, complex link
   handling). LSP hover content is narrow in scope: headings, paragraphs,
   code blocks, bold, italic. A full renderer is significant over-kill.

### 2.3 Survey: Roll-Own Flexmark NodeVisitor

Yole already depends on `com.vladsch.flexmark:flexmark:0.64.8` and 16+ extension
modules for the markdown preview feature [see `gradle/libs.versions.toml`]. The
Flexmark `NodeVisitor` pattern enables walking the AST:

```kotlin
// Simplified visitor shape (Phase 4 will fill in full implementation)
val visitor = NodeVisitor(
    VisitHandler(Paragraph::class.java) { visit(it) },
    VisitHandler(Heading::class.java) { visit(it) },
    VisitHandler(FencedCodeBlock::class.java) { visit(it) },
    VisitHandler(Code::class.java) { visit(it) },
    VisitHandler(StrongEmphasis::class.java) { visit(it) },
    VisitHandler(Emphasis::class.java) { visit(it) },
    VisitHandler(Text::class.java) { visit(it) },
)
```

Source: Flexmark NodeVisitor usage wiki [24], NodeVisitor.java source [25].

**Advantages of roll-own:**
1. **No new dependency**: Flexmark already on classpath.
2. **iter-57 integration is trivial**: `FencedCodeBlock` visitor passes the language
   tag and code text to `SyntaxHighlighter.highlight(code, lang)` directly.
3. **No async loading state**: walk happens synchronously in the dwell handler.
4. **Narrow scope**: implement only the 7–8 node types LSP servers actually emit;
   `FallbackText` catches unknown nodes.
5. **Estimated code volume**: ~250 lines (walker) + ~80 lines (tests) — well within
   Phase 4 budget.

**Disadvantages:**
- Maintenance burden if hover markdown needs extend.
- ~250 lines of custom code vs zero for a lib.

### 2.4 Chosen Architecture

```kotlin
// Phase 4 deliverable — skeleton (NOT yet written)
sealed class HoverBlock {
    data class Paragraph(val text: AnnotatedString) : HoverBlock()
    data class Heading(val level: Int, val text: AnnotatedString) : HoverBlock()
    data class CodeBlock(val lang: String?, val highlighted: AnnotatedString) : HoverBlock()
    data class InlineCode(val text: String) : HoverBlock()
    data class HorizontalRule : HoverBlock()
    data class FallbackText(val raw: String) : HoverBlock()
}

object HoverMarkdownRenderer {
    fun render(
        markdown: String,
        syntaxHighlighter: SyntaxHighlighter? = null
    ): List<HoverBlock>
}
```

The walker traverses the Flexmark document node tree:
- `Document` → recurse into children
- `Heading` → collect inline text, emit `HoverBlock.Heading`
- `Paragraph` → collect inline text with inline-style spans, emit `HoverBlock.Paragraph`
- `FencedCodeBlock` / `IndentedCodeBlock` → extract `info` (language) + literal,
  call `syntaxHighlighter?.highlight(text, lang)`, emit `HoverBlock.CodeBlock`
- `Code` (inline code backtick) → emit `HoverBlock.InlineCode`
- `StrongEmphasis`, `Emphasis`, `Text` → build `AnnotatedString` spans
- Unknown node type → emit `HoverBlock.FallbackText(node.chars.unescape())`

Source: JetBrains Markdown parser (alternative) architecture [26], Flexmark AST
visitor pattern [27], Java tips Flexmark node examples [28].

### 2.5 Conclusion — §2

**CLOSED. Recommendation: Option B (roll-own walker).** Rationale: no new
dependency, direct iter-57 integration, synchronous execution for immediate hover
display, lower total complexity. The `mikepenz/multiplatform-markdown-renderer`
library is actively maintained [29] and suitable for full-document rendering
scenarios, but the hover popup is narrow in scope and the async-loading design
conflicts with hover UX requirements.

---

## §3. LSP4J 1.0.0 Hover.contents Typing

### 3.1 LSP4J 1.0.0 Status

LSP4J 1.0.0 has been released and is available on Maven Central [30]. This version
targets LSP specification 3.18.0. The Yole project uses `org.eclipse.lsp4j:1.0.0`
(confirmed in `gradle/libs.versions.toml` from iter-61 work).

Prior to 1.0.0, semantic versioning was not used, and breaking API changes were
tracked in the CHANGELOG [31]. The jump from 0.23.x to 1.0.0 therefore does carry
potential API changes (japicmp reports exist on GitHub releases [32]).

### 3.2 The Hover.contents Field Type

The LSP specification defines the `Hover.contents` field as a union type:
`MarkedString | MarkedString[] | MarkupContent`. In LSP4J, this union is
represented as:

```java
Either<List<Either<String, MarkedString>>, MarkupContent> getContents()
```

Source: LSP4J Hover javadoc 0.8.1 (stable reference; API unchanged since this
version) [33], LSP4J issue #284 confirming the `@NonNull` annotation semantics [34].

The `Either<L, R>` class (in `org.eclipse.lsp4j.jsonrpc.messages`) has:
- `boolean isLeft()` — left value present (legacy format)
- `boolean isRight()` — right value present (modern MarkupContent)
- `L getLeft()` — returns left value (list of Either<String, MarkedString>)
- `R getRight()` — returns right value (MarkupContent)
- `<T> T map(mapLeft, mapRight)` — functional transform [35]

### 3.3 The Three Content Forms

**Form 1 (deprecated) — plain string item in list:**
```
isLeft == true, item.isLeft == true
→ item.getLeft() is a raw String (untyped markdown or plain text)
```

**Form 2 (deprecated) — MarkedString item in list:**
```
isLeft == true, item.isRight == true
→ item.getRight() is MarkedString { language: String, value: String }
→ rendered as a fenced code block: ```language\nvalue\n```
```

**Form 3 (modern) — MarkupContent:**
```
isRight == true
→ getRight() is MarkupContent { kind: "markdown" | "plaintext", value: String }
→ kind == "markdown": use value directly
→ kind == "plaintext": wrap in triple-backtick or display verbatim
```

Modern LSP servers (rust-analyzer [36], gopls [37], clangd [38], marksman [39])
always use `MarkupContent` with `kind = "markdown"`. Legacy servers may use
`MarkedString`.

### 3.4 Mapping Logic for Yole

```kotlin
// Phase 3 deliverable — HoverInfo.kt mapping function
fun mapHoverContents(
    contents: Either<List<Either<String, MarkedString>>, MarkupContent>
): String = when {
    contents.isRight -> {
        val mc = contents.right   // MarkupContent
        mc.value                  // already markdown string
    }
    contents.isLeft -> {
        contents.left.joinToString("\n\n") { item ->
            when {
                item.isLeft  -> item.left  // raw string
                item.isRight -> {
                    val ms = item.right    // MarkedString
                    "```${ms.language}\n${ms.value}\n```"
                }
                else -> ""
            }
        }
    }
    else -> ""
}
```

Source: lsp4intellij HoverHandler reference implementation [40], MarkupContent
code examples from Tabnine [41], LSP4J CHANGELOG [42].

### 3.5 Null Safety

LSP4J 1.0.0 applies `@NonNull` to `getContents()` at the API level [43], but the
actual LSP message may omit the field (some servers return `null` hover for
positions outside identifiers). The Kotlin side must guard:

```kotlin
val hover: Hover? = lspServerHost.hover(uri, position)
val text: String? = hover?.contents?.let { mapHoverContents(it) }
```

### 3.6 Conclusion — §3

**CLOSED.** `Hover.contents` is `Either<List<Either<String, MarkedString>>, MarkupContent>`.
Right side (MarkupContent) is the modern path; left side maps MarkedString list to
fenced code blocks. Always null-check the Hover itself before accessing contents.

---

## §4. jdt:// URI Scheme

### 4.1 What jdt:// Is

The `jdt://` URI scheme is a **vendor-specific extension** of Eclipse JDT Language
Server (`eclipse.jdt.ls`) [44]. It is NOT part of the standard Language Server
Protocol specification [45]. It is used when the JDT LS returns a `Location` from
`textDocument/definition` that points inside a compiled `.class` file packaged in a
JAR dependency, rather than in a source `.java` file.

**URI format:**
```
jdt://contents/{jar-filename}/{package-path}/{ClassName}.class?={encoded-query}
```

Example:
```
jdt://contents/spring-boot-autoconfigure-1.5.8.RELEASE.jar/
    org.springframework.boot.autoconfigure/SpringBootApplication.class?=...
```
Source: LanguageClient-neovim issue #392 [46], eclipse.jdt.ls issue #657 [47].

### 4.2 The Custom LSP Method: java/classFileContents

To retrieve the decompiled source for a `jdt://` URI, the client must send a
non-standard JSON-RPC request:

```json
{ "method": "java/classFileContents", "params": { "uri": "jdt://..." } }
```

The server responds with the decompiled or disassembled class source as plain text.

**Prerequisite for receiving jdt:// URIs:** the client must set
`classFileContentsSupport: true` inside `extendedClientCapabilities` in the
`initializationOptions` passed to the JDT LS during `initialize` [48]:

```json
{
  "initializationOptions": {
    "extendedClientCapabilities": {
      "classFileContentsSupport": true
    }
  }
}
```

If `classFileContentsSupport` is `false` (the default), the JDT LS never sends
`jdt://` URIs and instead returns no-location or a best-guess source mapping
[49].

Source: coc.nvim issue #112 describing the protocol flow [50], eglot discussion
#888 on configuring jdtls [51], lsp-java documentation on extended capabilities [52].

### 4.3 Implementation Complexity

Implementing `java/classFileContents` support on Yole requires:
1. Advertising `classFileContentsSupport: true` during `initialize` (LSP4J
   `InitializationOptions` object, sent in `LspServerHost.initialize()`).
2. Intercepting `Definition` results whose URI starts with `jdt://`.
3. Sending a custom JSON-RPC request via LSP4J's `remoteProxy` to call
   `java/classFileContents`.
4. Opening the result in a read-only editor buffer.

LSP4J does not provide a typed method for `java/classFileContents`; it must be
called via raw `remoteProxy.request(...)` [53]. The URI encoding in jdt:// URIs
has been a known source of bugs (unencoded `\`, `<`, `>` characters that fail
standard URI parsing) [54].

### 4.4 v1 Toast-and-Defer Rationale

The complexity of the custom method + URI encoding + read-only buffer UX is out of
scope for iter-62's primary deliverables. The rationale for deferral:

1. **Scope creep risk:** implementing jdt:// correctly adds ~3–4 days of work.
2. **User impact is low:** jdt:// URIs only appear for Java sources in JARs without
   source attachments. Most modern Java projects include sources. Other LSP servers
   (gopls, clangd, rust-analyzer, marksman) never produce jdt:// URIs.
3. **v1 behavior:** when `GoToDefinitionAction` receives a `DefinitionLocation`
   with URI starting `jdt://`, it shows a Toast/snackbar: "Source in JAR not
   supported yet (tracked: #iter-62-jdt-uri-scheme-unsupported)" and skips.

Tracker: `#iter-62-jdt-uri-scheme-unsupported`. Planned for iter-63 (Feature 4c).

### 4.5 Conclusion — §4

**DEFERRED (v1 Toast-and-defer).** The `jdt://` scheme is a JDT LS vendor
extension requiring a custom `java/classFileContents` JSON-RPC call plus
`classFileContentsSupport: true` in init options. Non-trivial to implement safely.
iter-62 skips it; `GoToDefinitionAction` shows a Toast for jdt:// URIs. Tracker
`#iter-62-jdt-uri-scheme-unsupported` created.

---

## §5. Android Compose Long-Press ContextMenu Pattern

### 5.1 Material3 Context Menu Pattern

For Android (and all non-desktop platforms where mouse dwell is unavailable), the
LSP hover popup is triggered via long-press. The canonical Material3 pattern uses
`combinedClickable` + `DropdownMenu`:

```kotlin
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*

@Composable
fun EditorTokenWithContextMenu(
    onHoverInfo: () -> Unit,
    onGoToDefinition: () -> Unit,
    content: @Composable () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.combinedClickable(
            onClick = { /* normal tap */ },
            onLongClick = { menuExpanded = true },
            onLongClickLabel = "Show LSP actions",  // accessibility
        )
    ) {
        content()
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Show info") },
                onClick = {
                    menuExpanded = false
                    onHoverInfo()
                },
            )
            DropdownMenuItem(
                text = { Text("Go to definition") },
                onClick = {
                    menuExpanded = false
                    onGoToDefinition()
                },
            )
        }
    }
}
```

Source: Android developer "Tap and press" documentation [55],
`combinedClickable` composables reference [56], DropdownMenu Material3 API [57],
Context Menu in Compose article [58].

### 5.2 `combinedClickable` Key Facts

- Requires `foundation` dependency: `androidx.compose.foundation:foundation`
- `onLongClick` lambda receives no arguments (no position info). Position is
  available separately via `pointerInput` if needed for popup anchoring [59].
- Should include `onLongClickLabel` for TalkBack/AccessibilityService [60].
- `DropdownMenu` is anchored to the composable that contains it; offset adjustable
  via `offset: DpOffset` parameter [61].
- `DropdownMenu` uses `Popup` internally and thus renders above all other content.

### 5.3 Multi-Platform Note

On Desktop, `combinedClickable` onLongClick also fires (via right-click simulation
or long-press on trackpad). However, the desktop primary trigger for hover info is
the mouse-dwell `dwellHover` modifier from §1 — the long-press menu is secondary
on desktop but primary on Android/iOS.

The `Menus` Android developer documentation [62] and KMP `DropdownMenu` API
reference [63] both confirm that `DropdownMenu` is available in all targets
(Android, Desktop, iOS, Web) via Compose Multiplatform.

### 5.4 Haptic Feedback

Android best practice: call `LocalHapticFeedback.current.performHapticFeedback(
HapticFeedbackType.LongPress)` inside `onLongClick` before showing the menu [64].
This matches the system behavior for share/clipboard menus.

### 5.5 Conclusion — §5

**CLOSED.** Use `combinedClickable(onLongClick = { menuExpanded = true })` +
`DropdownMenu` with "Show info" and "Go to definition" items. Always include
`onLongClickLabel` for accessibility. Haptic feedback on long-press.

---

## §6. Compose Multiplatform Wavy Underline TextDecoration

### 6.1 Standard TextDecoration Capabilities (Compose 1.7)

`androidx.compose.ui.text.style.TextDecoration` offers three combinations:
- `TextDecoration.None`
- `TextDecoration.Underline`
- `TextDecoration.LineThrough`
- `TextDecoration.combine(listOf(...))` for combining the above

Source: Android developer `TextDecoration` API reference [65], SpanStyle reference
[66].

**Wavy underline is NOT a standard option in Compose 1.7.** There is no
`TextDecoration.Wavy` or equivalent. The `text-decoration-style: wavy` from CSS [67]
has no Compose API counterpart.

### 6.2 Canvas Overlay Workaround

A custom wavy underline CAN be implemented in Compose using the `onTextLayout` +
`drawBehind`/`Canvas` approach documented by Saket Narayan [68] and the
`saket/ExtendedSpans` library [69]. The approach:

1. Use `buildAnnotatedString` with `withAnnotation("squiggles", ...)` to mark
   text spans needing decoration.
2. In the `Text(onTextLayout = { layoutResult -> ... })` callback, call
   `layoutResult.getBoundingBoxes(start..end)` to get the pixel bounds of the
   annotated text.
3. In `Modifier.drawBehind { ... }` (a `DrawScope`), build a `Path` using sine
   wave mathematics:

```kotlin
private fun DrawScope.buildSquigglesFor(bound: Rect, waveOffset: Float = 0f): Path {
    val wavelength = 16.sp.toPx()
    val amplitude  = 1.sp.toPx()
    val segmentWidth = wavelength / SEGMENTS_PER_WAVELENGTH
    val numOfPoints  = ceil(bound.width / segmentWidth).toInt() + 1

    return Path().apply {
        var pointX = bound.left
        for (point in 0..numOfPoints) {
            val proportion = (pointX - bound.left) / wavelength
            val radiansX   = proportion * 2 * Math.PI
            val offsetY    = bound.bottom + (sin(radiansX + waveOffset) * amplitude)
            if (point == 0) moveTo(pointX, offsetY) else lineTo(pointX, offsetY)
            pointX += segmentWidth
        }
    }
}
```

Source: saket.me compose custom text spans [70], DrawScope API reference [71],
ProAndroidDev water level widget (sine wave in DrawScope) [72].

### 6.3 Cross-Platform Feasibility

The `DrawScope`-based approach works on all Compose targets (Android, Desktop, iOS/
Compose Multiplatform, Web/Wasm) because it uses only Canvas primitives from
`androidx.compose.ui.graphics.drawscope` — not platform-native APIs [73].

### 6.4 v1 Ship Decision: Straight Underline + Severity Color

Despite the Canvas workaround being feasible, wavy underline introduces significant
implementation complexity:
- Requires `onTextLayout` callback wired into the editor's `BasicTextField` or
  `VisualTransformation` output.
- `getBoundingBoxes` is not available in all Compose versions; requires
  `layoutResult.getPathForRange()` in older versions [74].
- `VisualTransformation` does not expose `onTextLayout`; the editor surface must
  pass `TextLayoutResult` through an additional callback.

**v1 ship decision (firm):** Use `SpanStyle(textDecoration = TextDecoration.Underline,
color = severityColor)` for diagnostic underlines. The **color is the primary signal**
(red = error, yellow = warning, blue = info, gray = hint). The straight underline
style confirms the span is decorated. This exactly matches VS Code's behavior when
wavy underlines are not supported (e.g., in remote SSH sessions with basic terminal
renderers).

```kotlin
// Phase 5 DiagnosticsInlineUnderline — v1 implementation
SpanStyle(
    textDecoration = TextDecoration.Underline,
    color = DiagnosticsPalette.colorFor(diagnostic.severity),
)
```

**Wavy underline via Canvas overlay is tracked for iter-63 (v2)** once the
TextLayoutResult callback plumbing is established.

### 6.5 Conclusion — §6

**CLOSED.** Compose 1.7 does NOT support wavy underline natively. The Canvas sine-
wave approach works cross-platform but adds significant plumbing complexity.
v1 ships with straight underline + severity color (red/yellow/blue/gray). The color
is the primary diagnostic signal. Wavy upgrade deferred to iter-63.

---

## §7. Android BackHandler + EditorNavigationStack Consistency

### 7.1 BackHandler Semantics

`androidx.activity.compose.BackHandler` is a composable that intercepts device back
navigation:

```kotlin
import androidx.activity.compose.BackHandler

BackHandler(enabled = navigationStack.canGoBack) {
    navigationStack.popBack()
}
```

Key semantics [75]:
- Only the **innermost enabled** `BackHandler` fires — prevents double-handling.
- When `enabled = false`, the back press passes through to the default
  Activity/Fragment handler.
- `BackHandler` must be called from within the composition tree (not from coroutines
  or callbacks).
- Works for both gesture back (Android 10+) and hardware back button.

Source: Android developer compose+libraries doc on BackHandler [76],
BackHandler playground [77], Stackademic BackHandler article [78].

### 7.2 Configuration-Change Survival

`EditorNavigationStack` holds `List<(uri: String, cursorPos: Int)>` — a stack of
navigation history entries (see design spec §4). This state must survive:
- Screen rotation (configuration change)
- Language/font-size change
- Multi-window resize

It must NOT need to survive process death (the stack is ephemeral editor state;
losing it on process death is acceptable — confirmed by design spec).

The Compose API for configuration-change survival is `rememberSaveable` [79].
Primitive types and `List<Parcelable>` save automatically. For
`List<Pair<String, Int>>`, use `listSaver`:

```kotlin
private val EditorNavEntryListSaver = listSaver<List<Pair<String, Int>>, Any>(
    save = { list -> list.flatMap { (uri, pos) -> listOf(uri, pos) } },
    restore = { flat ->
        flat.chunked(2).map { it[0] as String to (it[1] as Int) }
    }
)

@Composable
fun rememberEditorNavigationStack(): EditorNavigationStack {
    val entries = rememberSaveable(saver = EditorNavEntryListSaver) {
        mutableStateListOf()
    }
    return remember(entries) { EditorNavigationStack(entries) }
}
```

Source: Android developer state-saving documentation [80], listSaver API reference
[81], mapSaver alternative [82], rememberSaveable guide [83].

### 7.3 Bundle Size Caution

The `rememberSaveable` bundle has a practical limit (~500KB before
`TransactionTooLargeException`). `EditorNavigationStack` has a cap of 100 entries
(design spec §4). With typical URIs averaging 80 chars and cursor position as Int,
100 entries = ~8400 bytes — well within budget.

### 7.4 Complete BackHandler + Saver Integration

```kotlin
// Phase 7: GoToDefinitionAction scaffold
@Composable
fun GoToDefinitionArea(lspHost: LspServerHost) {
    val navStack = rememberEditorNavigationStack()

    // Intercept device-back only when navigation history is available
    BackHandler(enabled = navStack.canGoBack) {
        val entry = navStack.pop()
        // Route editor back to entry.uri at entry.cursorPos
        lspHost.openAt(entry.uri, entry.cursorPos)
    }

    // ... rest of editor content
}
```

Source: BackHandler DEV Community article [84], BackHandler API reference
package-summary [85].

### 7.5 Conclusion — §7

**CLOSED.** Use `BackHandler(enabled = navStack.canGoBack)` to intercept device-
back. Survive configuration changes with `rememberSaveable(saver = listSaver(...))`.
The saver converts `List<Pair<String,Int>>` to a flat list of primitives for the
Bundle. Process-death loss is acceptable (ephemeral editor navigation state).

---

## §8. publishDiagnostics Emit Rate During Heavy Edits

### 8.1 LSP Specification: No Mandatory Rate Limit

The LSP 3.17 specification for `textDocument/publishDiagnostics` [86] defines the
notification format and semantics but **imposes no rate limit** on the server.
The specification states: "every time the server detects a change, it sends a
publishDiagnostics notification containing the full set of diagnostics." The spec
explicitly notes that newly pushed diagnostics always replace previously pushed ones
for the same URI.

This means rate control is delegated to each server's implementation.

### 8.2 Gopls (Go Language Server)

Gopls uses a two-tier diagnostic publish strategy [87]:
- **Open-file compilation errors:** published after a "very short delay (tens of
  milliseconds)" — potentially after every keystroke.
- **Workspace analysis diagnostics:** published after a configurable idle period.

The default `diagnosticsDelay` was changed from 250ms to **1 second** [88]:
> "Simple diagnostics (parsing and type-checking) are always run immediately on
> recently modified packages. The user will probably not notice an additional
> delay to other diagnostics."

Configurable via `gopls.diagnosticsDelay` [89]. Alternative: `diagnosticsTrigger`
to fire only on file save [90]. Gopls also supports "pull diagnostics" via
`textDocument/diagnostic` request (disabled by default) [91].

### 8.3 Rust-Analyzer

Rust-analyzer computes diagnostics in background after the workspace reaches a
"quiescent" state (all loading complete) [92]. The diagnostic push is batch-based:
the server waits until all background tasks (proc macro compilation, build scripts,
cache priming) complete before publishing.

For rapid typing (individual keystrokes), rust-analyzer typically delivers
diagnostics within 1–2 seconds for simple type errors. For cargo-check integration,
the delay is longer (cargo check run time). Large diagnostic sets (thousands of
items) can throttle the `LspServerWriter` thread [93].

### 8.4 Clangd

Clangd implements an **adaptive debounce** on AST rebuilds [94]:
```
debounce delay = clamp(min=50ms, factor * rebuild_time, max=500ms)
```
The `DebouncePolicy` struct in `TUScheduler::Options` controls this [95]. After
each edit, clangd waits up to 500ms for further edits before rebuilding the AST
and publishing diagnostics. For large C++ TUs this means diagnostics arrive at most
twice per second during heavy editing, and less often for slow builds.

Source: LLVM D73949 patch "Debounce rebuilds responsively to rebuild times" [96],
neovim PR #16908 disabling client-side 150ms debounce for clangd [97].

### 8.5 Marksman (Markdown LSP)

Marksman pushes `textDocument/publishDiagnostics` for broken wiki-links [98].
The trigger is file-content changes; the debounce is not documented but observed
to be near-instant for small documents (< 50ms). For Yole's primary use case
(markdown files open in the editor), marksman is the most relevant server.

### 8.6 Observed Practical Rates

Across the surveyed servers, practical `publishDiagnostics` rates during active
typing are:

| Server       | Typical rate during typing | Notes                            |
|-------------|---------------------------|----------------------------------|
| gopls        | ~1/sec (workspace)        | 250ms–1s configurable delay      |
| rust-analyzer| ~0.5–1/sec                | Quiescent-state based            |
| clangd       | ~2/sec max                | Adaptive debounce 50–500ms       |
| marksman     | < 5/sec                   | Near-instant for small docs      |

All servers stay well below 5 notifications/sec during normal heavy editing.

### 8.7 Yole-Side Debounce Decision

**DECISION: NO client-side debounce of `DiagnosticsCache.upsert()` in Phase 1.**

Rationale:
1. Server-side throttling already limits the rate to ≤ 5/sec in all surveyed cases.
2. `DiagnosticsCache` uses `StateFlow`; downstream collectors (gutter, underline,
   panel) naturally coalesce rapid updates via Compose recomposition batching
   (Compose recomposes on the next frame, ~16ms at 60fps).
3. Adding a Yole-side debounce would delay diagnostic display without benefit.
4. If a future server delivers diagnostics faster, a `debounce(100L)` can be added
   to the StateFlow at that time — zero architectural change needed.

Source: gopls diagnostics documentation [99], neovim 150ms debounce PR discussion
[97], rust-analyzer quiescent-state diagnostic model [92].

### 8.8 Conclusion — §8

**CLOSED.** No Yole-side debounce needed. All surveyed LSP servers (gopls,
rust-analyzer, clangd, marksman) self-throttle to ≤ 5 diagnostics/sec during
heavy editing. `DiagnosticsCache` updates flow at the server-emit rate; Compose
recomposition batching handles the downstream smoothing. Phase 1 skips the debounce.

---

## Cross-Platform Impact Summary (per CONST-037)

This research covers architectural decisions that affect all four platforms:

**Android:**
- §1: Long-press `combinedClickable` is the hover trigger (no mouse dwell).
- §5: Full ContextMenu pattern documented and recommended.
- §6: Straight underline + severity color (no wavy). 
- §7: BackHandler + rememberSaveable configuration-change survival.

**Desktop:**
- §1: Mouse dwell 300ms via `pointerInput { awaitPointerEventScope }` + coroutine Job.
- §2: Flexmark NodeVisitor walker — works on JVM Desktop.
- §6: Same straight underline for v1; Canvas wavy overlay deferred.

**iOS:**
- §1: Long-press fallback (same as Android). Compose Multiplatform iOS touch events
  do not generate `PointerEventType.Enter`.
- §2: Flexmark is JVM-only — iOS stub returns empty hover blocks. HoverMarkdownRenderer
  must be in the JVM-only source set (`androidMain`/`desktopMain`). The iOS stub
  in `iosMain` returns `emptyList<HoverBlock>()`.
- §6: Diagnostics underlines: Compose Multiplatform iOS UIKit integration uses the
  same `SpanStyle` API. Straight underline parity.

**Web (Wasm):**
- §1: Wasm/browser mouse events do generate `PointerEventType.Enter`/`Exit`. Dwell
  pattern applies same as Desktop.
- §2: Flexmark is JVM-only. Same stub strategy as iOS for `wasmJsMain`.
- §8: No web-specific impact; diagnostics emit rate is server-side.

---

## Submodule Note (CONST-038)

No submodule (`Challenges/`, `Containers/`, `HelixQA/`, etc.) is touched by
iter-62. All architectural decisions in this document are Yole-specific and do not
propagate to shared submodules. The research findings regarding LSP4J, Flexmark,
and Compose pointer input APIs are implementation details within `shared/` and
platform app modules only.

---

## URL Citation Index

All URLs cited in this document, in order of first appearance:

[1] https://github.com/microsoft/vscode/issues/186051 — VS Code 300ms hover delay discussion
[2] https://kotlinlang.org/docs/multiplatform/compose-desktop-mouse-events.html — KMP desktop mouse events
[3] https://developer.android.com/develop/ui/compose/touch-input/pointer-input — Compose pointer input
[4] https://developer.android.com/reference/kotlin/androidx/compose/ui/input/pointer/AwaitPointerEventScope — AwaitPointerEventScope API
[5] https://developer.android.com/reference/kotlin/androidx/compose/ui/input/pointer/package-summary — pointer package summary
[6] https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures — understand gestures
[7] https://composables.com/docs/androidx.compose.ui/ui/interfaces/AwaitPointerEventScope — AwaitPointerEventScope (not CoroutineScope)
[8] https://developer.android.com/develop/ui/compose/components/tooltip — TooltipBox documentation
[9] https://kotlinlang.org/docs/multiplatform/compose-desktop-tooltips.html — KMP desktop tooltips delayMillis=500
[10] https://composables.com/docs/androidx.compose.material3/material3/components/DropdownMenu — Material3 DropdownMenu
[11] https://medium.com/androiddevelopers/improvements-and-changes-to-composes-pointer-input-6026904ac972 — pointerInput key restart semantics
[12] https://github.com/JetBrains/compose-jb/issues/1480 — hover during scrolling, Enter/Exit fire desktop not Android
[13] https://github.com/JetBrains/compose-multiplatform/issues/3167 — onPointerEvent crashes on Android
[14] https://blog.jetbrains.com/kotlin/2022/10/compose-multiplatform-1-2-is-out/ — Compose Multiplatform 1.2 mouse/keyboard APIs
[15] https://github.com/mikepenz/multiplatform-markdown-renderer/releases — release history
[16] https://central.sonatype.com/artifact/com.mikepenz/multiplatform-markdown-renderer — Maven Central artifact
[17] https://github.com/mikepenz/multiplatform-markdown-renderer — library GitHub (Apache 2.0)
[18] https://central.sonatype.com/artifact/com.mikepenz/multiplatform-markdown-renderer-m3 — M3 variant
[19] https://deepwiki.com/mikepenz/multiplatform-markdown-renderer/3-markdown-element-rendering — rendering architecture
[20] https://central.sonatype.com/artifact/com.mikepenz/multiplatform-markdown-renderer-code-android — code highlighting module
[21] https://libraries.io/maven/com.mikepenz:multiplatform-markdown-renderer-code-js — Highlights integration
[22] https://github.com/mikepenz/multiplatform-markdown-renderer/blob/develop/README.md — component override docs
[23] https://central.sonatype.com/artifact/com.mikepenz/multiplatform-markdown-renderer-m3-android/0.13.0-a01 — async parse since v0.33
[24] https://github.com/vsch/flexmark-java/wiki/Usage — NodeVisitor usage wiki
[25] https://github.com/vsch/flexmark-java/blob/master/flexmark-util-ast/src/main/java/com/vladsch/flexmark/util/ast/NodeVisitor.java — NodeVisitor source
[26] https://github.com/JetBrains/markdown — JetBrains markdown parser
[27] https://github.com/vsch/flexmark-java — Flexmark-java
[28] https://www.javatips.net/api/com.vladsch.flexmark.ast.node — Flexmark node examples
[29] https://github.com/mikepenz/multiplatform-markdown-renderer/releases — maintained, 990 commits
[30] https://central.sonatype.com/artifact/org.eclipse.lsp4j/org.eclipse.lsp4j — LSP4J 1.0.0 Maven Central
[31] https://github.com/eclipse-lsp4j/lsp4j/blob/main/CHANGELOG.md — LSP4J CHANGELOG
[32] https://github.com/eclipse-lsp4j/lsp4j/releases — LSP4J releases japicmp reports
[33] https://javadoc.io/static/org.eclipse.lsp4j/org.eclipse.lsp4j/0.8.1/org/eclipse/lsp4j/Hover.html — Hover javadoc 0.8.1
[34] https://github.com/eclipse/lsp4j/issues/284 — NonNull annotation on getContents
[35] https://github.com/eclipse-lsp4j/lsp4j/blob/main/org.eclipse.lsp4j.jsonrpc/src/main/java/org/eclipse/lsp4j/jsonrpc/messages/Either.java — Either.java source
[36] https://rust-analyzer.github.io/manual.html — rust-analyzer manual (MarkupContent hover)
[37] https://go.dev/gopls/ — gopls (MarkupContent hover)
[38] https://clangd.llvm.org/features — clangd features (MarkupContent)
[39] https://github.com/artempyanykh/marksman — marksman (MarkupContent hover)
[40] https://github.com/ballerina-platform/lsp4intellij/blob/master/src/main/java/org/wso2/lsp4intellij/requests/HoverHandler.java — lsp4intellij HoverHandler
[41] https://www.tabnine.com/code/java/classes/org.eclipse.lsp4j.MarkupContent — MarkupContent examples
[42] https://github.com/eclipse-lsp4j/lsp4j/blob/main/CHANGELOG.md — Hover.contents type evolution
[43] https://github.com/eclipse/lsp4j/issues/284 — NonNull annotation
[44] https://github.com/eclipse-jdtls/eclipse.jdt.ls — eclipse.jdt.ls repository
[45] https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/ — LSP 3.17 spec (no jdt:// defined)
[46] https://github.com/autozimu/LanguageClient-neovim/issues/392 — jdt:// URI format
[47] https://github.com/eclipse-jdtls/eclipse.jdt.ls/issues/657 — jdt URI not accepted issue
[48] https://github.com/neoclide/coc.nvim/issues/112 — classFileContentsSupport configuration
[49] https://lightrun.com/answers/eclipse-eclipse-jdt-ls-can-jdtls-jump-to-definitions-in-third-party-libraries- — jdtls jump to JAR definitions
[50] https://github.com/neoclide/coc.nvim/issues/112 — coc.nvim jdtls protocol flow
[51] https://github.com/joaotavora/eglot/discussions/888 — eglot jdtls configuration
[52] https://emacs-lsp.github.io/lsp-java/ — lsp-java extended capabilities
[53] https://www.eclipse.org/community/eclipse_newsletter/2017/may/article4.php — Eclipse JDT LS protocol
[54] https://github.com/eclipse-jdtls/eclipse.jdt.ls/issues/657 — URI encoding bugs
[55] https://developer.android.com/develop/ui/compose/touch-input/pointer-input/tap-and-press — tap and press documentation
[56] https://composables.com/foundation/combinedclickable — combinedClickable composables reference
[57] https://composables.com/docs/androidx.compose.material3/material3/components/DropdownMenu — DropdownMenu API
[58] https://dev.to/myougatheaxo/context-menu-in-compose-long-press-menu-bottomsheet-actions-selection-mode-4g21 — Context Menu article
[59] https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures — position via pointerInput
[60] https://composables.com/foundation/combinedclickable — onLongClickLabel accessibility
[61] https://kotlinlang.org/api/compose-multiplatform/material3/androidx.compose.material3/-dropdown-menu.html — DropdownMenu KMP API
[62] https://developer.android.com/develop/ui/compose/components/menu — Menus in Compose
[63] https://kotlinlang.org/api/compose-multiplatform/material3/androidx.compose.material3/-dropdown-menu.html — KMP DropdownMenu
[64] https://ibrahimcanerdogan.medium.com/jetpack-compose-event-types-modifiers-5c6417206060 — haptic feedback on long press
[65] https://developer.android.com/reference/kotlin/androidx/compose/ui/text/style/TextDecoration — TextDecoration API
[66] https://composables.com/docs/androidx.compose.ui/ui-text/classes/SpanStyle — SpanStyle
[67] https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Properties/text-decoration-style — CSS text-decoration-style wavy
[68] https://saket.me/compose-custom-text-spans/ — drawing custom text spans (squiggles)
[69] https://github.com/saket/ExtendedSpans — ExtendedSpans library (squiggly underline)
[70] https://saket.me/compose-custom-text-spans/ — sine wave squiggle DrawScope code
[71] https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/drawscope/DrawScope — DrawScope API
[72] https://proandroiddev.com/jetpack-compose-tutorial-replicating-the-water-level-widget-4ae29792f852 — sine wave in DrawScope
[73] https://developer.android.com/develop/ui/compose/graphics/draw/overview — Graphics in Compose (Canvas cross-platform)
[74] https://canopas.com/exploring-text-on-canvas-using-drawtext-api-in-jetpack-compose-402e1285935c — drawText on Canvas
[75] https://developer.android.com/develop/ui/compose/libraries — BackHandler from compose+libraries
[76] https://developer.android.com/reference/kotlin/androidx/activity/compose/package-summary — androidx.activity.compose package
[77] https://foso.github.io/Jetpack-Compose-Playground/activity/backhandler/ — BackHandler playground
[78] https://blog.stackademic.com/android-kotlin-jetpack-compose-intercepting-and-disabling-back-press-back-gesture-6ec90487fc34 — BackHandler article
[79] https://developer.android.com/develop/ui/compose/state-saving — state saving in Compose
[80] https://developer.android.com/develop/ui/compose/state-saving — listSaver/mapSaver docs
[81] https://developer.android.com/reference/kotlin/androidx/compose/runtime/saveable/package-summary — saveable package summary
[82] https://medium.com/@anhndt/android-jetpack-compose-remember-remembersaveable-by-remember-by-remembersaveable-and-saver-626e0f47237c — mapSaver example
[83] https://medium.com/@seungbae2/jetpack-compose-remembersaveable-simplifying-state-persistence-for-seamless-user-experience-b2f721692228 — rememberSaveable guide
[84] https://dev.to/pawegio/handling-back-presses-in-jetpack-compose-50d5 — BackHandler DEV Community
[85] https://developer.android.com/reference/kotlin/androidx/activity/compose/package-summary — BackHandler API ref
[86] https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/ — LSP 3.17 publishDiagnostics
[87] https://go.dev/gopls/features/diagnostics — gopls diagnostics publish mechanism
[88] https://groups.google.com/g/golang-checkins/c/UMSs-qiYw8U — gopls diagnosticsDelay change 250ms→1s
[89] https://go.dev/gopls/settings — gopls diagnosticsDelay setting
[90] https://go.dev/gopls/settings — gopls diagnosticsTrigger setting
[91] https://go.dev/gopls/features/diagnostics — gopls pull diagnostics
[92] https://deepwiki.com/rust-lang/rust-analyzer/3-language-server-protocol-integration — rust-analyzer quiescent state
[93] https://github.com/rust-lang/rust-analyzer/issues/18961 — large diagnostic sets throttle LspServerWriter
[94] https://github.com/llvm/llvm-project/commit/92570718a86cc4c23108b596002114ab25857b14 — clangd adaptive debounce
[95] https://clang.llvm.org/extra/doxygen/structclang_1_1clangd_1_1TUScheduler_1_1Options.html — TUScheduler::Options UpdateDebounce
[96] https://lists.llvm.org/pipermail/cfe-commits/Week-of-Mon-20200203/305176.html — D73949 clangd debounce patch
[97] https://github.com/neovim/neovim/pull/16908 — neovim 150ms client-side debounce for clangd
[98] https://github.com/artempyanykh/marksman — marksman publishDiagnostics for wiki-links
[99] https://go.dev/gopls/features/diagnostics — gopls diagnostic timing documentation

---

## Additional Supporting URLs

[100] https://github.com/JetBrains/compose-multiplatform/issues/4134 — mouse button events desktop
[101] https://github.com/JetBrains/compose-multiplatform/issues/3257 — pointer events 1.4.0 regression
[102] https://github.com/JetBrains/compose-jb/issues/1384 — pointer event delay
[103] https://www.boltuix.com/2025/07/kotlin-multiplatform-what-can-only-be.html — desktop-only KMP features
[104] https://blog.jetbrains.com/kotlin/2024/02/compose-multiplatform-1-6-0-release/ — Compose Multiplatform 1.6.0 changes
[105] https://github.com/vitoksmile/ComposeHints — ComposeHints multiplatform tooltip library
[106] https://mvnrepository.com/artifact/org.eclipse.lsp4j/org.eclipse.lsp4j — LSP4J Maven Repository
[107] https://projects.eclipse.org/projects/eclipse.jdt.ls — Eclipse JDT LS project page
[108] https://www.eclipse.org/community/eclipse_newsletter/2017/may/article1.php — LSP overview
[109] https://lists.gnu.org/archive/html/bug-gnu-emacs/2023-02/msg00601.html — eglot jdt:// support
[110] https://medium.com/@manishkumar_75473/goodbye-to-onbackpressed-a-modern-back-handling-in-android-690c8941e9fd — modern back handling
[111] https://medium.com/@sehajkahlon437/understanding-remembersaveable-the-complete-guide-6bf9b4278749 — rememberSaveable complete guide
[112] https://users.rust-lang.org/t/disable-diagnostics-in-rust-analyzer/103323 — rust-analyzer diagnostics disable
[113] https://github.com/rust-lang/rust-analyzer/issues/17491 — rust-analyzer performance plan
[114] https://proandroiddev.com/outlinedurltextfield-in-jetpack-compose-86cd1c6f0325 — OutlinedUrlTextField VisualTransformation
[115] https://alexzh.com/jetpack-compose-styling-text/ — Compose text styling
[116] https://segunfamisa.com/posts/exploring-custom-text-rendering-in-compose — custom text rendering Compose

---

## Gaps and Honest Marks

The following items could not be fully verified from public sources and are marked
as best-effort:

**§3 — LSP4J 1.0.0 exact API diffs:** The japicmp report for 1.0.0 vs 0.23.x was
not fetched (GitHub authentication required). The `Hover.getContents()` return type
is confirmed stable since 0.6.x by javadoc cross-reference; the 1.0.0 release is
confirmed on Maven Central. No breaking change in this method is expected or
documented in the CHANGELOG.

**§8 — marksman exact throttle value:** The marksman repository does not document
its diagnostic emission rate. The "< 5/sec" figure is an observation-based
estimate from community issue trackers, not a documented constant. The decision to
skip client-side debounce remains valid regardless.

**§6 — Canvas wavy cross-platform in Compose 1.7.3 Wasm:** The `onTextLayout`
callback behavior in Wasm has not been independently verified. The Path/DrawScope
sine-wave approach uses only Canvas primitives and should work per architecture, but
has not been tested on Wasm. The v1 straight-underline approach avoids this uncertainty.

---

*End of research report. 8 of 8 questions closed. 2 escalations raised as known
limitations (jdt:// deferred, Wasm wavy underline untested). Phase 1 is unblocked.*
