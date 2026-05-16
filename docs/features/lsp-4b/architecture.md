<!-- SPDX-FileCopyrightText: 2026 Milos Vasic -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# LSP Capability Expansion — Architecture

> **Audience:** Yole contributors and Feature 4c / iter-63 LSP capability authors.
> Read `docs/features/lsp/architecture.md` first — this document extends it.

iter-62 adds three new LSP capabilities on top of the iter-61 host: diagnostics (publish/subscribe), hover documentation, and go-to-definition. None of these capabilities touch the `CompletionEngine` or `CompletionProvider` interface; they are orthogonal to completion.

---

## 1. Pipeline overview

```
iter-61 foundation (unchanged)
    ┌─────────────────────────────────────────────────────────────┐
    │ LspServerHost  (.../lsp/LspServerHost.<platform>.kt)        │
    │   ├── textDocument/completion  → LspCompletionProvider      │
    │   │     └── CompletionEngine → Popup (unchanged)            │
    │   └── (NEW iter-62 below)                                   │
    └─────────────────────────────────────────────────────────────┘
            │
            ▼
    ┌─────────────────────────────────────────────────────────────┐
    │  iter-62 additions                                          │
    │                                                             │
    │  publishDiagnostics (LSP ← server push)                     │
    │    └── DiagnosticsCache.update(uri, List<Diagnostic>)        │
    │          └── StateFlow<Map<String, List<Diagnostic>>>        │
    │                ├── IdeEditorScreen.collectAsState()          │
    │                │     ├── DiagnosticsGutter (gutter dots)     │
    │                │     ├── DiagnosticsInlineUnderline (VT)     │
    │                │     └── DiagnosticsProblemsPanel (drawer)   │
    │                └── (future observers — no cache changes)     │
    │                                                             │
    │  textDocument/hover  (request → response)                   │
    │    LspServerHost.hover(uri, line, char): String?            │
    │      └── HoverMarkdownRenderer.render(markdown)             │
    │            → List<HoverBlock>                               │
    │              └── HoverPopup (Compose overlay)               │
    │                    triggered by HoverTriggerDetector         │
    │                    or HoverShortcut (F1)                     │
    │                                                             │
    │  textDocument/definition  (request → response)              │
    │    LspServerHost.definition(uri, line, char): List<...>     │
    │      └── GoToDefinitionAction.goToDefinition(...)           │
    │            routes: 0 → toast; 1 → push+open; N → chooser   │
    │              └── EditorNavigationStack (back nav)           │
    │              └── DefinitionLocationChooser (bottom sheet)   │
    └─────────────────────────────────────────────────────────────┘
```

---

## 2. Component map

### 2.1 Shared (commonMain)

| Component | File | Purpose |
|---|---|---|
| `Diagnostic` | `lsp/Diagnostic.kt` | Data class: severity, message, `IntRange` for character offsets |
| `Severity` | `lsp/Diagnostic.kt` | Enum: Error, Warning, Information, Hint |
| `DiagnosticsCache` | `lsp/DiagnosticsCache.kt` | Thread-safe `StateFlow<Map<String,List<Diagnostic>>>`. Observers subscribe; no polling needed |
| `HoverBlock` | `lsp/HoverBlock.kt` | Sealed class: Paragraph, Heading, CodeBlock, InlineCodeSpan, FallbackText |
| `HoverMarkdownRenderer` | `lsp/HoverMarkdownRenderer.kt` | `expect object`; render(String) → `List<HoverBlock>` |
| `HoverTriggerDetector` | `lsp/HoverTriggerDetector.kt` | Pure Kotlin; 300 ms dwell timer, completion-popup guard, identifier guard (stubbed v1), explicit bypass |
| `EditorNavigationStack` | `lsp/EditorNavigationStack.kt` | `NavEntry(uri, cursorOffset)` stack (max 100, consecutive-dup suppression) |
| `GoToDefinitionAction` | `lsp/GoToDefinitionAction.kt` | Object; routes 0/1/N results; takes `LspDefinitionRequester` interface for testability |
| `LspDefinitionRequester` | `lsp/LspDefinitionRequester.kt` | `suspend fun definition(langId, uri, line, char): List<DefinitionLocation>` |

### 2.2 Desktop + Android JVM (LspServerHost actuals — extended)

| Method added in iter-62 | LSP request | Return type |
|---|---|---|
| `suspend fun hover(uri, line, char): String?` | `textDocument/hover` | Raw Markdown string from `MarkupContent.value`, or null |
| `suspend fun definition(uri, line, char): List<DefinitionLocation>` | `textDocument/definition` | List of `DefinitionLocation(uri, line, char)` |
| `fun publishDiagnostics(params)` (callback) | `textDocument/publishDiagnostics` (push) | Updates `diagnosticsCache` |

### 2.3 Desktop JVM (HoverMarkdownRenderer actual)

`HoverMarkdownRenderer.desktop.kt` uses Flexmark 0.64.8:

- `Parser.builder()` with default extensions.
- Visitor walks the AST node-by-node, emitting `HoverBlock` variants.
- Unsupported node types (tables, links, list items) emit `FallbackText(node.chars.toString())`.

The Android actual is identical (same Flexmark dependency added to `androidMain` in `shared/build.gradle.kts`).

iOS and Wasm stubs: `FallbackText(markdown)` if non-empty, `emptyList()` if empty.

### 2.4 Android UI (androidApp)

| Composable / modifier | File | Purpose |
|---|---|---|
| `DiagnosticsPalette` | `diagnostics/DiagnosticsPalette.kt` | `severityVisuals(Severity, isDark): SeverityVisuals` — VS Code–inspired color + icon per severity |
| `DiagnosticsGutter` | `diagnostics/DiagnosticsGutter.kt` | Composable; 8 dp dot per line with diagnostics; highest-severity wins per line |
| `DiagnosticsInlineUnderline` | `diagnostics/DiagnosticsInlineUnderline.kt` | `VisualTransformation`; applies straight colored underlines per diagnostic range; identity `OffsetMapping` |
| `DiagnosticsProblemsPanel` | `diagnostics/DiagnosticsProblemsPanel.kt` | Collapsible LazyColumn ~200 dp; sorted by range.first; click → onJumpToLine (dismisses in v1) |
| `HoverPopup` | `hover/HoverPopup.kt` | `Popup(TopStart)` overlay inside editor Box; `LazyColumn` max 400×300 dp; renders all HoverBlock variants |
| `HoverShortcut` | `hover/HoverShortcut.kt` | `Modifier.hoverShortcut(onTrigger)`: intercepts F1 `KeyDown`, calls onTrigger |
| `DefinitionLocationChooser` | `navigation/DefinitionLocationChooser.kt` | `ModalBottomSheet`; `LazyColumn` with `itemsIndexed`; testTag anchors `def-chooser` + `def-row-$idx` |

---

## 3. How to add a new LSP capability (template for iter-63 / Feature 4c)

Follow these five steps when adding a capability not present in v1.5.0 (e.g., `textDocument/rename`, `textDocument/signatureHelp`, `textDocument/references`):

### Step 1 — Add a suspend method to `LspServerHost` expect

```kotlin
// shared/src/commonMain/kotlin/digital/vasic/yole/lsp/LspServerHost.kt
expect class LspServerHost {
    // existing methods...
    suspend fun signatureHelp(uri: String, line: Int, character: Int): SignatureHelpResult?
}
```

### Step 2 — Implement in Desktop and Android actuals

```kotlin
// shared/src/desktopMain/kotlin/digital/vasic/yole/lsp/LspServerHost.desktop.kt
actual suspend fun signatureHelp(uri: String, line: Int, character: Int): SignatureHelpResult? {
    val server = acquireOrNull(langIdFor(uri)) ?: return null
    val params = SignatureHelpParams(TextDocumentIdentifier(uri), Position(line, character))
    val result = server.textDocumentService.signatureHelp(params).await()
    return result?.let { SignatureHelpResult.from(it) }
}
```

Use `.await()` from `kotlinx-coroutines-jdk8` to bridge `CompletableFuture<T>` to a Kotlin coroutine. The Android actual is typically identical to Desktop.

### Step 3 — Add iOS and Wasm stubs returning null / empty

```kotlin
// shared/src/iosMain/kotlin/digital/vasic/yole/lsp/LspServerHost.ios.kt
actual suspend fun signatureHelp(uri: String, line: Int, character: Int): SignatureHelpResult? = null
```

```kotlin
// shared/src/wasmJsMain/kotlin/digital/vasic/yole/lsp/LspServerHost.wasmJs.kt
actual suspend fun signatureHelp(uri: String, line: Int, character: Int): SignatureHelpResult? = null
```

### Step 4 — Author the response data class

Author the response data class (or sealed class) in `commonMain`:

```kotlin
// shared/src/commonMain/kotlin/digital/vasic/yole/lsp/SignatureHelpResult.kt
data class SignatureHelpResult(
    val signatures: List<SignatureInfo>,
    val activeSignature: Int,
    val activeParameter: Int,
) {
    companion object {
        fun from(lsp4jResult: SignatureHelp): SignatureHelpResult { /* map fields */ }
    }
}
```

Keep all LSP4J imports inside the Desktop/Android actuals. The `commonMain` data class must not import LSP4J directly — it is a KMP common type.

### Step 5 — Wire the UI surface into IdeEditorScreen

Add the new surface as a composable inside `IdeEditorScreen`. Trigger it from the editor's `onPreviewKeyEvent`, a toolbar button, or a `Modifier` extension following the `HoverShortcut` pattern.

Add `desktopTest` behavioral tests (degradation path), and Robolectric structural tests for Android UI composables.

---

## 4. How to add a new diagnostic render surface

`DiagnosticsCache` exposes a `StateFlow<Map<String, List<Diagnostic>>>`. Any composable can observe it without modifying the cache itself:

```kotlin
val diagnosticsByUri by lspHost.diagnosticsCache.states.collectAsState()
val currentFileDiagnostics = diagnosticsByUri[currentFileUri] ?: emptyList()
```

New surfaces (e.g., a status-bar summary, a document minimap overlay) simply derive from `currentFileDiagnostics`. No changes to `DiagnosticsCache` are required — it is a pure observer hub.

---

## 5. Markdown → Compose pipeline (HoverMarkdownRenderer)

To add support for a new Markdown block type (e.g., tables in iter-63):

1. Add a new `HoverBlock` subclass to `HoverBlock.kt` in `commonMain`:

   ```kotlin
   sealed class HoverBlock {
       // existing variants...
       data class Table(val rows: List<List<String>>) : HoverBlock()
   }
   ```

2. In `HoverMarkdownRenderer.desktop.kt`, add a visitor branch for the Flexmark node type:

   ```kotlin
   is TableBlock -> {
       val rows = node.children.filterIsInstance<TableRow>().map { row ->
           row.children.filterIsInstance<TableCell>().map { it.text.toString() }
       }
       blocks += HoverBlock.Table(rows)
   }
   ```

3. In `HoverPopup.kt`, add a `when` branch for the new variant:

   ```kotlin
   is HoverBlock.Table -> TableComposable(block.rows)
   ```

4. Update iOS and Wasm stubs (they produce `FallbackText` regardless, so no change needed unless you want rich rendering there too).

5. Add a test in `HoverMarkdownRendererTest` covering the new node type.

The `FallbackText` catch-all in the visitor ensures unsupported nodes never crash — they degrade to raw text.

---

## 6. Per-platform notes

### Desktop macOS-arm64

`LspServerHost.desktop.kt` implements `hover()`, `definition()`, and `publishDiagnostics()` via LSP4J 1.0.0. The hover and definition calls bridge `CompletableFuture<T>` to Kotlin coroutines using `.await()`. The `publishDiagnostics` callback (fired by the server, not the client) updates `DiagnosticsCache` on the calling thread; `StateFlow.value = ...` assignment is thread-safe.

Desktop editor UI wiring (`IdeEditorScreen.desktop.kt`) is deferred in v1.5.0 (`#iter-62-desktop-editor-lsp-wiring`). The data path works end-to-end; only the Compose surface is missing.

### Desktop Linux / Windows

Same JVM code. Binary distribution gated on `#crossbuild-linux-windows-infra`.

### Android

Full UI wiring in `IdeEditorScreen` and `SyncedScrollEditor` on Android. The `publishDiagnostics` flow reaches the UI via `StateFlow` + `collectAsState()`. LSP binaries not yet cross-compiled for Android NDK (`#crossbuild-android-ndk-lsp`).

### iOS

`LspServerHost.ios.kt` stubs: `hover()` → `null`, `definition()` → `emptyList()`, `publishDiagnostics()` → no-op. Hard-blocked by App Store §2.5.2.

### Web (Wasm)

`LspServerHost.wasmJs.kt` stubs: same as iOS. Native subprocess not possible in the browser sandbox.

---

## 7. Anti-bluff invariants (iter-62)

Two challenges gate the feature in `make qa-all` via `qa-iter-62-gates`.

### `yole-challenges/scripts/lsp_diagnostics_challenge.sh`

- **Static layer:** asserts `DiagnosticsCache.kt`, `Diagnostic.kt`, all four platform `LspServerHost` actuals, `DiagnosticsGutter.kt`, `DiagnosticsInlineUnderline.kt`, `DiagnosticsProblemsPanel.kt` exist. Asserts `publishDiagnostics` call site present in Desktop actual.
- **Runtime layer:** runs `DiagnosticsCacheTest` + `DiagnosticTest` + `DiagnosticsGutterTest` + `DiagnosticsInlineUnderlineTest` + `DiagnosticsProblemsPanelTest`; asserts ≥ 20 PASSED, 0 FAILED.

### `yole-challenges/scripts/lsp_hover_definition_challenge.sh`

- **Static layer:** asserts `HoverMarkdownRenderer` expect + all 4 platform actuals present; asserts `hover()` + `definition()` method signatures exist in Desktop actual; asserts `GoToDefinitionAction.kt`, `EditorNavigationStack.kt`, `DefinitionLocationChooser.kt` exist; asserts `HoverTriggerDetector.kt` present with `300` ms constant.
- **Runtime layer:** runs `HoverMarkdownRendererTest` + `HoverTriggerDetectorTest` + `GoToDefinitionActionTests` + `EditorNavigationStackTests`; asserts ≥ 23 PASSED, 0 FAILED.

### `lsp_hover_definition_challenge.sh` anti-bluff note for `#iter-62-gopls-no-go-toolchain`

The gopls smoke test is wrapped in `// SKIP-OK: #iter-62-gopls-no-go-toolchain`. This is an environment dependency (Go toolchain must be installed for gopls to start), not a code defect. The skip is legitimate and does not exempt any feature code from being tested via the other challenge tests.

---

## 8. Package layout (iter-62 additions)

```
shared/src/commonMain/kotlin/digital/vasic/yole/lsp/
├── Diagnostic.kt                   (Diagnostic data class + Severity enum)
├── DiagnosticsCache.kt             (StateFlow<Map<String,List<Diagnostic>>>)
├── EditorNavigationStack.kt        (NavEntry + stack, max 100, dup-suppression)
├── GoToDefinitionAction.kt         (routes 0/1/N results)
├── HoverBlock.kt                   (sealed class: 5 variants)
├── HoverMarkdownRenderer.kt        (expect object)
├── HoverTriggerDetector.kt         (300 ms dwell + guards)
└── LspDefinitionRequester.kt       (testability interface)

shared/src/desktopMain/kotlin/digital/vasic/yole/lsp/
└── HoverMarkdownRenderer.desktop.kt  (Flexmark walker → HoverBlock)

shared/src/androidMain/kotlin/digital/vasic/yole/lsp/
└── HoverMarkdownRenderer.android.kt  (identical Flexmark walker)

shared/src/iosMain/kotlin/digital/vasic/yole/lsp/
└── HoverMarkdownRenderer.ios.kt      (FallbackText stub)

shared/src/wasmJsMain/kotlin/digital/vasic/yole/lsp/
└── HoverMarkdownRenderer.wasmJs.kt   (FallbackText stub)

androidApp/src/main/java/digital/vasic/yole/android/ui/editor/
├── diagnostics/
│   ├── DiagnosticsPalette.kt         (severityVisuals helper)
│   ├── DiagnosticsGutter.kt          (colored 8 dp gutter dots)
│   ├── DiagnosticsInlineUnderline.kt (VisualTransformation straight underlines)
│   └── DiagnosticsProblemsPanel.kt   (collapsible bottom drawer LazyColumn)
├── hover/
│   ├── HoverPopup.kt                 (Popup overlay, max 400×300 dp)
│   └── HoverShortcut.kt              (Modifier.hoverShortcut: F1 intercept)
└── navigation/
    └── DefinitionLocationChooser.kt  (ModalBottomSheet chooser)
```

---

## 9. Cross-platform impact (CONST-037)

Every change to iter-62 components MUST be evaluated against all four targets before coding.

- **Android:** Full UI wiring of all three capabilities in `IdeEditorScreen` + `SyncedScrollEditor`. Diagnostics, hover, go-to-definition surfaces fully rendered. LSP server binaries still pending NDK cross-compilation.
- **Desktop macOS-arm64:** LSP host connected (iter-61); new methods (`hover`, `definition`, `publishDiagnostics`) implemented. Desktop editor UI wiring deferred (`#iter-62-desktop-editor-lsp-wiring`).
- **Desktop Linux / Windows:** Same JVM code as macOS; binary distribution gated.
- **iOS:** All new `LspServerHost` methods stubbed (null/emptyList/no-op). Hard-blocked by App Store §2.5.2.
- **Web (Wasm):** All new `LspServerHost` methods stubbed. Native subprocess not possible.

Changes to `commonMain` components (`DiagnosticsCache`, `HoverBlock`, `HoverMarkdownRenderer`, `HoverTriggerDetector`, `GoToDefinitionAction`, `EditorNavigationStack`) affect all four platform compilations and MUST include a "Cross-platform impact" block in the commit body per CONST-037.
