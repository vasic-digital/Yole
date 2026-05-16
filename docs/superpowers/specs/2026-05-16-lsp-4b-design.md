<!-- SPDX-FileCopyrightText: 2026 Milos Vasic -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# LSP Capability Expansion — Design Spec (Feature 4b of 5)

> **Status:** Brainstorm complete; awaiting operator review of written spec before invoking writing-plans.
> **Author:** brainstormed 2026-05-16 with operator.
> **Sequence:** Feature 4b — the middle iteration of Feature 4's 3-part decomposition (4a/4b/4c). Feature 4a (iter-61, LSP hosting + completion) shipped 2026-05-16 at v1.4.0. Feature 4c (rename + code actions + format + signature help) follows as iter-63.

---

## 1. Goal

Extend iter-61's LSP integration with **3 capabilities**:
- **Diagnostics** rendered in the editor across 3 surfaces (gutter markers + inline squiggles + bottom Problems panel).
- **Hover popups** on identifiers — smart Tree-Sitter-aware implicit trigger + explicit shortcut. Content rendered as sanitized Markdown via Flexmark.
- **Go-to-definition** navigation — in-place file switch + multi-result chooser. Back-stack for undo navigation.

Plugs into iter-61's `LspServerHost` — no architectural changes to the host or `LspCompletionProvider`. Diagnostics consume the cache iter-61 deliberately left as no-op in §5.5 of its design spec.

---

## 2. Locked-in scope decisions (from brainstorm)

| Decision | Choice |
|---|---|
| Diagnostics UX surface | **3-surface**: gutter markers + inline VisualTransformation underlines + bottom Problems panel. Single source of truth (DiagnosticsCache StateFlow) feeds all three. |
| Hover trigger model | **Smart implicit + explicit**: 300ms mouse-dwell on Desktop OR long-press on mobile, but ONLY when cursor is over a Tree-Sitter identifier-kind node (reuses iter-60 ScopeAwareRanker pattern). Plus explicit F1 / long-press-menu-item shortcut that bypasses the Tree-Sitter filter. |
| Go-to-definition navigation | **In-place + multi-result chooser**: 1 result → editor switches files + jumps cursor; N results → bottom-sheet (mobile) / dropdown (desktop) chooser; back-gesture pops to origin via `EditorNavigationStack`. |
| Hover content rendering | **Sanitized Markdown via Flexmark**: parse hover Markdown with existing Flexmark dep, render via Compose RichText surface, code-block syntax highlighting via iter-57 SyntaxHighlighter. Skip link/image/table for popup compactness. |

Per **CONST-037**, every commit MUST include a Cross-platform impact block. Per **CONST-038**, no Yole-specific content leaks into sibling submodules.

---

## 3. Architecture

```
shared/src/commonMain/kotlin/digital/vasic/yole/lsp/
├── Diagnostic.kt                    # data class: severity, range, message, source, code
├── DiagnosticsCache.kt              # StateFlow<Map<URI, List<Diagnostic>>>; consumes publishDiagnostics
├── HoverInfo.kt                     # data class: contents (Markdown), range
├── DefinitionLocation.kt            # data class: uri, range
└── (extends LspServerHost.kt expect class with hover() + definition() suspend methods)

shared/src/{androidMain,desktopMain}/kotlin/digital/vasic/yole/lsp/
└── (extends LspServerHost.{android,desktop}.kt with hover/definition bodies +
   YoleLanguageClient.publishDiagnostics body wiring to DiagnosticsCache)

shared/src/{iosMain,wasmJsMain}/kotlin/digital/vasic/yole/lsp/
└── (extends stubs — hover returns null, definition returns emptyList)

androidApp/src/main/.../ui/editor/diagnostics/
├── DiagnosticsGutter.kt             # per-line markers (red/yellow/blue/gray dots)
├── DiagnosticsInlineUnderline.kt    # VisualTransformation adding wavy underlines
├── DiagnosticsProblemsPanel.kt      # bottom LazyColumn listing diagnostics
└── DiagnosticsPalette.kt            # 4 severity colors + icons via iter-57 ThemeProvider

androidApp/src/main/.../ui/editor/hover/
├── HoverPopup.kt                    # Compose Popup; reuses iter-60 popup positioning
├── HoverTriggerDetector.kt          # 300ms dwell + Tree-Sitter node-at-cursor + completion-popup suppression
├── HoverMarkdownRenderer.kt         # Flexmark→Compose blocks (heading/paragraph/code-block/inline-code/bold/italic)
└── HoverShortcut.kt                 # explicit F1 / long-press menu binding

androidApp/src/main/.../ui/editor/navigation/
├── GoToDefinitionAction.kt          # invoke definition() → route on result count
├── DefinitionLocationChooser.kt     # bottom-sheet (mobile) / dropdown (desktop) for N>1
└── EditorNavigationStack.kt         # back-stack of (uri, cursorPos); cap 100
```

**Invariants:**

- `DiagnosticsCache` is the SINGLE source of truth. All 3 render surfaces observe its StateFlow. No render path holds its own diagnostic list.
- `LspServerHost` gains 2 new suspend methods (hover, definition). expect/actual contract extends; iOS+Wasm stubs return null/empty per CONST-035.
- Hover and definition use `textDocument` URI + position. Reuses iter-61's URI/workspace plumbing — no new identifier semantics.
- iter-60's `CompletionEngine` and `CompletionPopupState` are unchanged. Hover + completion popups coexist via mutual-exclusion: hover suppressed when completion popup is open.
- iter-58's fold gutter coexists with diagnostics gutter: ~28dp total width (12dp dot column + existing 16dp chevron column).

## 4. Components

| Component | Responsibility | Cardinality |
|---|---|---|
| `Diagnostic` | `(severity: Severity, range: IntRange, message: String, source: String?, code: String?)` where `Severity = {Error, Warning, Information, Hint}` | many per file |
| `DiagnosticsCache` | `Map<URI, List<Diagnostic>>` populated by `LanguageClient.publishDiagnostics`. Per-URI cap 1000. Cache cleared on `LspServerHost.didClose`. | 1 |
| `HoverInfo` | `(contents: String /* Markdown */, range: IntRange?)` | 0 or 1 per cursor-position |
| `DefinitionLocation` | `(uri: String, range: IntRange)` | 0–N per cursor-position |
| `LspServerHost.hover(...)` | new suspend method: sends `textDocument/hover`, maps LSP4J `Hover` → `HoverInfo?` | extends existing host |
| `LspServerHost.definition(...)` | new suspend method: sends `textDocument/definition`, maps `Location[]` → `List<DefinitionLocation>` | extends existing host |
| `YoleLanguageClient.publishDiagnostics(params)` | was no-op in iter-61; now maps LSP4J `Diagnostic[]` → Yole `Diagnostic` list + calls `DiagnosticsCache.upsert(uri, list)` | 1 |
| `DiagnosticsGutter` | Composable: paints colored marker dots per-line based on highest-severity diagnostic on that line | 1 per editor |
| `DiagnosticsInlineUnderline` | `VisualTransformation`: wraps text spans in `SpanStyle(textDecoration = wavy underline, color = severity color)` | 1 per editor |
| `DiagnosticsProblemsPanel` | Composable: bottom `LazyColumn`; click row → cursor jump | 1 per editor |
| `DiagnosticsPalette` | 4 severity → (color, icon) via iter-57 ThemeProvider | 1 stateless |
| `HoverPopup` | Composable: anchored `Popup` with sanitized-Markdown body | 1 per editor |
| `HoverTriggerDetector` | per-event-coroutine: 300ms dwell debounce + Tree-Sitter node-at-cursor + explicit-shortcut interceptor + completion-popup-open suppression | 1 per editor |
| `HoverMarkdownRenderer` | Pure: parses hover Markdown via Flexmark → emits Compose-renderable blocks. Code-block highlighting via iter-57 `SyntaxHighlighter`. Skips link/image/table. | 1 stateless object |
| `HoverShortcut` | explicit binding (F1 / long-press menu); bypasses Tree-Sitter filter | 1 per editor |
| `GoToDefinitionAction` | Composable + state: invokes `LspServerHost.definition()`; 1 result → `EditorNavigationStack.openAt`; N results → `DefinitionLocationChooser` | 1 per editor |
| `DefinitionLocationChooser` | bottom-sheet (mobile) / dropdown (desktop). List with file path + line preview. | 1 transient |
| `EditorNavigationStack` | Back-stack of `(uri, cursorPos)`. Cap 100 (circular). Device-back pops + returns. | 1 per app session |

## 5. Data flow

### 5.1 Diagnostics (server-pushed)

```
User edits file
  → SyncedScrollEditor.onValueChange
  → LspServerHost.didChange(langId, uri, version, fullText)  [iter-61]
  → LSP server processes; later (async) emits:
      textDocument/publishDiagnostics {uri, diagnostics: [...]}
  → LSP4J Launcher routes to YoleLanguageClient.publishDiagnostics(params)
  → DiagnosticsCache.upsert(uri, mapped):
      ├ emits via StateFlow<Map<URI, List<Diagnostic>>>
      └ replaces previous list (server emits full-state, not incremental)

  ┌── observers ──┐
  ▼                ▼                        ▼
DiagnosticsGutter   DiagnosticsInline      DiagnosticsProblemsPanel
  - filter URI       - filter URI            - filter URI
  - group by line    - map ranges →          - sort by line+severity
  - dot per line       AnnotatedString spans - LazyColumn rows
                     - wavy underline +      - click → cursor jump
                       severity color
```

Cache cleared on `LspServerHost.didClose(uri)`. Per-URI list cap 1000 (truncation footer).

### 5.2 Hover (smart implicit + explicit)

**Implicit dwell:**

```
User stops mouse over editor at (line, col)
  → HoverTriggerDetector receives onPointerMove with 300ms debounce
  → after 300ms quiescence:
      ├ Tree-Sitter node-at-cursor via existing TokenizerEngine
      │   (iter-60 ScopeAwareRanker pattern: descendantForByteRange)
      ├ if node.kind not in {identifier, member_access, type, function_call}:
      │   skip; return early
      ├ if CompletionPopupState.isOpen: skip (mutex)
      └ else: dispatch HoverRequest(uri, line, col)
  → LspServerHost.hover(langId, uri, line, col):
      ├ lazy spawn server [iter-61]
      ├ send textDocument/hover via LSP4J
      ├ withTimeout(500ms): map Hover.contents → HoverInfo or null
      └ return HoverInfo?
  → if non-null:
      ├ HoverMarkdownRenderer.render(info.contents):
      │   ├ Flexmark.parser.parse(markdown) → Node tree
      │   └ walk; emit Compose blocks for supported node kinds
      └ HoverPopup.show(anchorOffset = cursorChar, blocks)
```

**Explicit shortcut:**

```
User presses F1 (Desktop) OR long-press menu → "Show info" (Mobile)
  → HoverShortcut via Modifier.onPreviewKeyEvent / combinedClickable(onLongClick)
  → skip Tree-Sitter filter (explicit = "I want it here")
  → dispatch HoverRequest(uri, line, col)
  → same downstream as implicit
```

**Dismissal:** tap-outside, Esc, cursor movement >5 chars, or new hover request supersedes.

### 5.3 Go-to-definition

```
User triggers (Desktop: Ctrl+Click on identifier OR F12; Mobile: long-press → "Go to definition")
  → GoToDefinitionAction.invoke(uri, line, col)
  → LspServerHost.definition(langId, uri, line, col):
      ├ send textDocument/definition via LSP4J
      ├ withTimeout(1000ms): map Location[] → List<DefinitionLocation>
      └ return list
  → switch on result count:
      ├ 0: Toast "No definition found"
      ├ 1: EditorNavigationStack.push(currentUri, currentCursor)
      │    → openFileAt(target.uri, target.range)
      └ N: DefinitionLocationChooser.show(results)
          → tap row → push + openFileAt
  → openFileAt(uri, range):
      ├ if uri == currentUri: just move cursor
      └ else: invoke Yole file-open path (FILES tab pattern from iter-58)
              + scroll editor to range.start + set cursor
```

**Back-navigation:** device-back (Android) / Cmd+Left (Desktop) → `EditorNavigationStack.pop()` → `openFileAt(popped)`; if stack empty → default handler.

### 5.4 Cross-feature interactions

- **Diagnostics + completion popup**: different surfaces (gutter chrome vs popup overlay) — no conflict.
- **Hover + completion popup**: mutually exclusive (HoverTriggerDetector checks `CompletionPopupState.isOpen`).
- **Hover + go-to-def**: shortcuts don't overlap (F1 vs Ctrl+Click). On mobile long-press menu has both items.
- **Diagnostics + iter-58 fold gutter**: shared gutter chrome. ~12dp dot column + ~16dp fold chevron column = ~28dp total. Dot on the left.

## 6. Error handling

| Failure | Detection | Recovery |
|---|---|---|
| Server crashes mid-session with diagnostics in flight | iter-61 LspServerHost restart-on-crash backoff | `DiagnosticsCache.clear(uri)`; UI re-renders empty. Next successful publishDiagnostics repopulates. No silent stale state. |
| `textDocument/hover` times out (500ms) | `withTimeout` | Returns null; popup doesn't appear. Best-effort; no error toast. |
| `textDocument/definition` times out (1000ms) | `withTimeout` | Toast "Definition request timed out — retry?". User-explicit invocation deserves user-visible failure. |
| `Hover.contents` null/empty | mapping detects | Don't show popup. Don't show empty state. |
| Hover Markdown contains unsupported feature (table, image, link) | `HoverMarkdownRenderer` walk encounters unrecognized kind | Render the raw `Node.chars` text for that block. Anti-bluff: don't silently drop. |
| Hover Markdown malformed | Flexmark is forgiving | Render whatever Flexmark returns. Worst case: raw text. |
| Hover code-block lang fence unknown to Yole | `SyntaxHighlighter.highlightForLang` returns plain | Code block renders unstyled but readable monospace. |
| `definition` returns 0 | result check | Toast "No definition found at cursor". |
| Definition target URI on different filesystem (e.g., `~/.cargo/registry/`) | Yole file-open resolves arbitrary paths since iter-58 | Open read-only via FILES tab. |
| Definition target URI uses non-`file://` scheme (e.g., `jdt://` from jdtls) | scheme check | v1: Toast "Cannot navigate to `<uri>` — non-file URI not supported". Track as `#iter-62-jdt-uri-scheme-unsupported`. Real jdtls support → 4c via `jdt/textDocument/getDocumentContents`. |
| Definition target range out of bounds (file changed since LSP indexed) | clamp range.start to file.length | Cursor at end-of-file. No crash. |
| Rapid hover spam | `$/cancelRequest` for in-flight | Only latest survives. Server cancels cheaply. |
| Diagnostics > 1000 per URI | cache cap | Truncate with `[N more suppressed]` footer. |
| `EditorNavigationStack` > 100 entries | push cap | Drop oldest (circular). |
| Back gesture, empty stack | pop returns null | Default handler (close editor / exit). |
| Chooser target file deleted between request + tap | openFileAt returns Result.failure | Toast "Target file no longer exists". |
| Completion popup + hover trigger simultaneously | mutex check | Hover suppressed; re-eligible after completion closes. |

`CancellationException` rethrown in every catch block (CONST-035 + Detekt).

## 7. Per-platform feasibility matrix

| Platform | Diagnostics 3-surface | Hover | Go-to-def | Status |
|---|---|---|---|---|
| **Android** | Full | Full (long-press + menu shortcut) | Full (in-place + bottom-sheet chooser; device-back) | Full code support; binary-blocked in v1 (0 Android LSP binaries from iter-61). UI tested against fake responses. |
| **Desktop macOS-arm64** | Full | Full (mouse-dwell + F1) | Full (in-place + dropdown; Cmd+Left back-nav) | Full — works against iter-61's 8 staged binaries. |
| **Desktop Linux/Windows** | Code-ready | Code-ready | Code-ready | Deferred per `#crossbuild-linux-windows-infra`. |
| **iOS** | Stub (cache empty) | Stub (returns null) | Stub (returns empty) | Hard-blocked (App Store 2.5.2). |
| **Web Wasm** | Stub | Stub | Stub | Deferred. |

Matches iter-61's reality: code-ready everywhere, binary distribution gated per existing tickets.

## 8. Deep-research checklist

Phase 0 equivalent. Output: `docs/features/lsp-4b/research-report.md`.

1. **Compose dwell-hover gesture pattern** — `Modifier.pointerInput { detectHover }` is enter-only. Implementing 300ms dwell requires manual debounce via `awaitPointerEventScope` or `pointerInteropFilter`. Find canonical pattern + verify Compose 1.7 support per-platform.
2. **Flexmark → Compose RichText rendering** — Yole's preview path renders Markdown to HTML. For popup, need Compose-native walker. Survey `mikepenz/multiplatform-markdown-renderer` (Apache-2.0) vs roll-own walker. Recommendation required.
3. **LSP4J 1.0.0 `Hover.contents` typing** — iter-61 §8 noted `Diagnostic.message` became Either-typed. Verify `Hover.contents` typing in 1.0.0; adjust mapping.
4. **`jdt://` URI scheme** — Eclipse JDT URI convention reference; confirm v1 Toast-and-defer rationale.
5. **Android Compose long-press menu pattern** — `combinedClickable(onLongClick) → ContextMenu` flow with multi-item menu (Show info / Go to definition / future Go to references). Use Material3 if available.
6. **Compose Multiplatform VisualTransformation for wavy underlines** — `SpanStyle.textDecoration` limits. Custom `TextDecoration` for wavy; verify Compose 1.7 + per-platform consistency. If unsupported, fallback to straight underline.
7. **Android `BackHandler` + `EditorNavigationStack`** — consistent behavior across compact + expanded layouts; ensure stack survives configuration changes (rotation).
8. **`publishDiagnostics` rate during heavy edits** — verify server-side throttling; confirm Yole-side debounce of cache updates unnecessary.

Output: ≥ 600 lines, ≥ 100 URL citations. Phase 0 is a hard gate before Phase 1.

## 9. Testing strategy (anti-bluff per CONST-035)

### 9.1 Unit tests (commonTest pure; desktopTest JVM-bound)

1. `DiagnosticTest` — sealed `Severity` round-trip; LSP4J `DiagnosticSeverity` → Yole `Severity` mapping.
2. `DiagnosticsCacheTest` — upsert replace semantic, upsert empty clears, clear removes, StateFlow emits on upsert, cap 1000 truncates.
3. `HoverInfoTest` — LSP4J `Hover.contents` (`MarkupContent | MarkedString | MarkedString[]`) → Yole single-Markdown mapping.
4. `DefinitionLocationTest` — `Location[]` → Yole list; `Either<Location, LocationLink>` LSP 3.18 union handling; range conversion respects file content.

### 9.2 Compose render tests (Robolectric)

5. `DiagnosticsGutterRobolectricTest` — 3 diagnostics × 2 lines → 2 markers with correct severity colors via testTag.
6. `DiagnosticsInlineUnderlineRobolectricTest` — VisualTransformation on "let x = foo()" with diagnostic on `foo` → wavy-underline SpanStyle over [8, 11].
7. `DiagnosticsProblemsPanelRobolectricTest` — 3 rows; click row 2 → cursor jumps (callback testTag).
8. `HoverPopupRobolectricTest` — canned HoverInfo → popup renders with code-block + paragraph; testTag `hover-popup` displayed.
9. `HoverMarkdownRendererTest` (desktopTest) — table-driven 8 Markdown inputs; emitted block kinds match expected.
10. `HoverTriggerDetectorTest` — 300ms dwell dispatches; movement cancels; explicit bypasses dwell; Tree-Sitter whitespace skip; completion-open suppression.
11. `GoToDefinitionActionTest` — 0/1/N result branching.
12. `EditorNavigationStackTest` — push/pop semantics; empty pop returns null; cap 100; consecutive-duplicate suppression.
13. `DefinitionLocationChooserRobolectricTest` — 3-result chooser; tap row 2 → callback.

### 9.3 Integration tests (desktopTest, real LSP servers)

14. `RealServerDiagnosticsSmokeTest` — marksman on Markdown with intentional broken link → `DiagnosticsCache` receives ≥ 1 diagnostic within 5s. `assumeTrue` if binaries absent.
15. `RealServerHoverSmokeTest` — rust-analyzer hover over `println!` → non-null HoverInfo with "macro" substring.
16. `RealServerDefinitionSmokeTest` — gopls definition on known symbol → non-empty list. (Phase 10 of iter-61 found gopls wasn't staged — iter-62 Phase 9 extends acquire-script.)

### 9.4 Structural anti-bluff

17. `Iter62FeatureWiringTest` — asserts `LspServerHost.kt` expect class declares `hover` + `definition`; iOS + Wasm stubs include both methods returning null/empty. Catches forgot-to-extend-stub regressions.

### 9.5 Challenges (Phase 9, wired into `make qa-all`)

18. `lsp_diagnostics_render_challenge.sh` — STATIC: 3 render files + DiagnosticsCache referenced. RUNTIME: ≥ 8 PASS on shared diagnostics tests; ≥ 3 PASS on Android Robolectric.
19. `lsp_hover_definition_challenge.sh` — STATIC: 4 component files + iter-61 host extended. RUNTIME: ≥ 10 PASS on shared hover/definition tests.

Every test mutation-verified; procedure in test class KDoc.

## 10. Phase breakdown

| Phase | Scope | TDD depth |
|---|---|---|
| 0 | Deep research → `docs/features/lsp-4b/research-report.md` | Research only |
| 1 | `Diagnostic` + `Severity` + `DiagnosticsCache` + LSP4J mapping | Full TDD |
| 2 | Extend `LspServerHost` with `hover()` + `definition()` + iOS/Wasm stubs + `YoleLanguageClient.publishDiagnostics` wired | Full TDD |
| 3 | `HoverInfo` + `DefinitionLocation` data classes + LSP4J mapping | Full TDD |
| 4 | `HoverMarkdownRenderer` (Flexmark→Compose blocks) | Full TDD |
| 5 | `DiagnosticsGutter` + `DiagnosticsInlineUnderline` + `DiagnosticsProblemsPanel` + Robolectric tests | Full TDD |
| 6 | `HoverPopup` + `HoverTriggerDetector` + Tree-Sitter node lookup integration | Full TDD |
| 7 | `GoToDefinitionAction` + `DefinitionLocationChooser` + `EditorNavigationStack` + device-back gesture | Full TDD |
| 8 | `IdeEditorScreen` integration (observer wiring + trigger + gesture + Problems panel as bottom drawer) | Task-level |
| 9 | 2 anti-bluff challenges + `qa-iter-62-gates` chained into `qa-all`; extend iter-61 acquire-script to include `gopls` | Full TDD on shell scripts |
| 10 | Documentation | Authoring only |
| 11 | Firebase distribution v1.5.0 | Mechanical |

## 11. Documentation deliverables

- `docs/features/lsp-4b/user-guide.md` — end-user: 3 diagnostic surfaces, hover trigger modalities, go-to-def navigation + back-stack, jdtls `jdt://` deferral.
- `docs/features/lsp-4b/architecture.md` — contributor: how to extend the host with new LSP capabilities (template for 4c), add new diagnostic render surfaces, the Markdown→Compose pipeline.
- `docs/features/lsp-4b/research-report.md` — Phase 0 output.
- CHANGELOG.md v1.5.0 entry.
- docs/CONTINUATION.md per CONST-036.

## 12. Firebase distribution

Same pattern as iter-60 / iter-61:
- Android Release + Debug + DEV: AAB via Firebase App Distribution.
- Desktop macOS-arm64: DMG staged in `releases/`.
- Linux/Windows: pending `#crossbuild-linux-windows-infra`.
- Web Wasm: pending `#wasmjs-production-distribution-gap`.
- iOS: hard-blocked.

Version target: **1.5.0 / versionCode 150 / dotted `0.0.0.1.50`**.

## 13. Out of scope for 4b

- Rename (`textDocument/rename`) → 4c.
- Code actions / quick-fix (`textDocument/codeAction`) → 4c.
- Signature help (`textDocument/signatureHelp`) → 4c.
- Formatting (`textDocument/formatting`) → 4c.
- Find-all-references (`textDocument/references`) → 4c (often paired with go-to-def).
- Workspace-wide diagnostics panel (cross-file rollup) → deferred follow-up.
- jdtls `jdt://` URI navigation → Toast-and-defer per §6.
- Inlay hints, semantic highlighting → future.
- LLM-driven hover augmentation → future.

## 14. Open questions for the implementation plan

- Compose dwell-hover gesture: per-platform consistency on Desktop vs Android. Phase 0 §1 resolves.
- Markdown renderer: third-party (multiplatform-markdown-renderer) vs roll-own walker. Phase 0 §2 recommends.
- Wavy underline cross-platform support. Phase 0 §6 confirms with fallback strategy.
- Multi-result chooser bottom-sheet API on Compose Multiplatform vs Material3-specific. Phase 0 §5 resolves.

## 15. Forensic anchor

Brainstorm session 2026-05-16 with operator. Operator chose at every round:

1. Diagnostics UX = "Gutter + inline underlines + Problems panel" (3-surface, heaviest).
2. Hover trigger = "Smart implicit (Tree-Sitter node-aware) + explicit".
3. Go-to-def navigation = "In-place + multi-result chooser".
4. Hover rendering = "Sanitized Markdown via Flexmark".

No defaults selected; every choice explicit.

---

**Next steps after operator review:**

1. Operator reviews + requests any changes.
2. Invoke `superpowers:writing-plans` to produce `docs/superpowers/plans/2026-05-16-lsp-4b-plan.md`.
3. Plan begins with Phase 0 deep-research (§8) before any code lands.
4. Implementation phases follow with bite-sized TDD, mutation-verified anti-bluff tests, CONST-037 cross-platform-impact tracking, and Firebase distribution at the end.
