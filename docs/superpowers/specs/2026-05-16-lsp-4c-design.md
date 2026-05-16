<!-- SPDX-FileCopyrightText: 2026 Milos Vasic -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# LSP Refactoring Capabilities — Design Spec (Feature 4c of 5)

> **Status:** Brainstorm complete; operator pre-approved execution.
> **Author:** brainstormed 2026-05-16 with operator.
> **Sequence:** Feature 4c — final iteration of Feature 4's 3-part decomposition. Feature 4a (iter-61) shipped LSP hosting + completion at v1.4.0. Feature 4b (iter-62) shipped diagnostics + hover + go-to-definition at v1.5.0. **iter-63 closes the LSP arc.**

---

## 1. Goal

Extend iter-61's `LspServerHost` with the final 5 LSP capabilities — **rename, code actions, signature help, formatting, find-references** — and surface each in the editor with appropriate per-platform UX. After iter-63, Yole offers a near-complete LSP client comparable to VS Code's core feature set.

---

## 2. Locked-in scope decisions (from brainstorm)

| Decision | Choice |
|---|---|
| Capability set | **All 5**: rename + code actions + signature help + formatting + find-references. Closes Feature 4 cleanly. |
| Rename UX | **Preview-diff panel + confirm**: modal panel shows all `WorkspaceEdit` edits per-file, user reviews then taps "Apply" or "Cancel". |
| Code actions UX | **Light-bulb gutter icon + long-press menu**: 3rd gutter column (left of fold chevron, right of diagnostic dot); plus `Ctrl+.` shortcut / long-press "Code actions" menu item. |
| Signature help UX | **Per-platform**: inline pill above cursor on mobile; floating tooltip near cursor on desktop. Implicit trigger on `(` and `,`; explicit `Ctrl+Shift+Space`. |
| Formatting trigger | **All three**: on-save (toggleable in Settings, default ON) + explicit (`Ctrl+Shift+F`) + on-type (server-declared `firstTriggerCharacter`). |
| Find-references UX | **Dedicated References panel** (bottom drawer, persistent, mirrors iter-62 Problems panel pattern). Click row → jump to location + push to `EditorNavigationStack`. |

Per **CONST-037**, every commit MUST include a Cross-platform impact block. Per **CONST-038**, no Yole-specific content leaks into sibling submodules.

---

## 3. Architecture

```
shared/src/commonMain/kotlin/digital/vasic/yole/lsp/
├── WorkspaceEdit.kt                  # data: per-file TextEdit list (used by rename + code actions)
├── TextEdit.kt                       # data: range + newText
├── CodeAction.kt                     # data: title, kind, edit?, command?
├── SignatureHelp.kt                  # data: signatures, activeSignature, activeParameter
├── ReferenceLocation.kt              # data: uri, range (alias of DefinitionLocation; semantic separation)
└── (extends LspServerHost with 5 suspend methods: rename, codeActions, signatureHelp, formatting, references)

shared/src/{androidMain,desktopMain}/kotlin/digital/vasic/yole/lsp/
└── (extends LspServerHost.{android,desktop}.kt with 5 new bodies + WorkspaceEdit applier helper)

shared/src/{iosMain,wasmJsMain}/kotlin/digital/vasic/yole/lsp/
└── (extends stubs — all 5 methods return null/empty)

androidApp/src/main/.../ui/editor/rename/
├── RenamePreviewPanel.kt             # modal: per-file collapsible diff list
├── RenameDiffRow.kt                  # single file's edits shown as before→after
└── RenameAction.kt                   # entry-point: invokes rename + shows panel

androidApp/src/main/.../ui/editor/codeaction/
├── CodeActionLightbulb.kt            # gutter column icon (3rd column)
├── CodeActionMenu.kt                 # dropdown with available actions
└── CodeActionInvoker.kt              # applies the selected action (edit or command)

androidApp/src/main/.../ui/editor/signaturehelp/
├── SignatureHelpPopup.kt             # desktop tooltip
├── SignatureHelpPill.kt              # mobile inline pill
└── SignatureHelpTrigger.kt           # implicit-on-( / explicit-on-Ctrl+Shift+Space

androidApp/src/main/.../ui/editor/formatting/
├── FormattingTrigger.kt              # on-save + explicit + on-type entry points
└── FormattingSettings.kt             # Settings toggle for on-save behavior

androidApp/src/main/.../ui/editor/references/
└── ReferencesPanel.kt                # bottom drawer (mirrors iter-62 ProblemsPanel)
```

### Invariants

- `WorkspaceEdit` is the shared data shape for rename + code actions (both can return multi-file edits). One applier helper handles both.
- Gutter chrome grows to 3 columns: `[diagnostic-dot] [lightbulb] [fold-chevron]`. Total ~40dp wide (diagnostics 12 + lightbulb 12 + chevron 16).
- Signature help and hover popup are mutually exclusive (both occupy near-cursor space). When signature-help-active, hover suppressed.
- iter-62's `EditorNavigationStack` reused by find-references (push on jump).
- iter-62's `LspDefinitionRequester` interface pattern extended to `LspRenameRequester`, `LspCodeActionRequester`, etc. for testability without expect-class mocking.
- iOS + Wasm stubs return null/empty across all 5 new methods.

## 4. Components

| Component | Responsibility | Cardinality |
|---|---|---|
| `WorkspaceEdit` | data: `Map<String /*uri*/, List<TextEdit>>` | many per rename/codeAction |
| `TextEdit` | data: `(range: IntRange, newText: String)` | many per file |
| `CodeAction` | data: `(title, kind: String?, edit: WorkspaceEdit?, command: LspCommand?)` | 0–N per cursor position |
| `SignatureHelp` | data: `(signatures: List<SignatureInformation>, activeSignature: Int, activeParameter: Int)` | 0 or 1 per cursor position |
| `ReferenceLocation` | typealias of DefinitionLocation; semantic separation | 0–N per cursor position |
| `LspServerHost.rename(...)` | new suspend: `textDocument/rename` → `WorkspaceEdit?` | extends host |
| `LspServerHost.codeActions(...)` | new suspend: `textDocument/codeAction` → `List<CodeAction>` | extends host |
| `LspServerHost.signatureHelp(...)` | new suspend: `textDocument/signatureHelp` → `SignatureHelp?` | extends host |
| `LspServerHost.formatting(...)` | new suspend: `textDocument/formatting` → `List<TextEdit>` | extends host |
| `LspServerHost.references(...)` | new suspend: `textDocument/references` → `List<ReferenceLocation>` | extends host |
| `RenamePreviewPanel` | Composable: per-file collapsible diff list; Apply/Cancel buttons | 1 transient |
| `CodeActionLightbulb` | Composable: gutter column icon; per-line visibility based on available actions | 1 per editor |
| `CodeActionMenu` | Composable: dropdown anchored to lightbulb tap | 1 transient |
| `SignatureHelpPopup` (desktop) | Composable: floating tooltip near cursor | 1 per editor |
| `SignatureHelpPill` (mobile) | Composable: inline chip above cursor | 1 per editor |
| `FormattingTrigger` | wires save + Ctrl+Shift+F + on-type intercept | 1 per editor |
| `FormattingSettings` | Settings screen toggle for on-save behavior | 1 in Settings nav |
| `ReferencesPanel` | Composable: bottom drawer (mirrors ProblemsPanel) | 1 per editor |
| `WorkspaceEditApplier` | Pure: applies a WorkspaceEdit to a Map<URI, String> (test target) + invokes editor save/refresh | 1 stateless |

## 5. Data flow

### 5.1 Rename

```
User invokes rename (Mobile: long-press menu → "Rename"; Desktop: F2)
  → RenameAction prompts for new name (inline input or modal)
  → LspServerHost.rename(langId, uri, line, char, newName):
      ├ send textDocument/rename via LSP4J
      ├ withTimeout(2000ms): map raw WorkspaceEdit → Yole WorkspaceEdit
      └ return WorkspaceEdit?
  → if null/empty: Toast "No rename available"
  → else: RenamePreviewPanel.show(edit):
      ├ per-file collapsible diff (filename + before/after spans)
      ├ Apply → WorkspaceEditApplier.apply(edit) → re-open affected files
      └ Cancel → dismiss
```

### 5.2 Code actions

```
User has cursor on line (or selection); diagnostics may be present
  → CodeActionLightbulb (gutter): polls codeActions periodically (debounced 500ms) per active line
  → if list non-empty: render lightbulb icon
  → user taps lightbulb OR long-press → "Code actions" OR Ctrl+. shortcut:
      → CodeActionMenu.show(actions):
          → user selects → CodeActionInvoker.invoke(action):
              ├ if action.edit non-null: WorkspaceEditApplier.apply(action.edit)
              └ if action.command non-null: workspace/executeCommand request
```

### 5.3 Signature help

```
User types `(` or `,` inside a function call
  → SignatureHelpTrigger intercepts via Modifier.onPreviewKeyEvent
  → LspServerHost.signatureHelp(langId, uri, line, char):
      ├ send textDocument/signatureHelp via LSP4J
      ├ withTimeout(300ms)
      └ return SignatureHelp?
  → if non-null:
      ├ Desktop: SignatureHelpPopup.show(info, anchor = cursorPixel)
      └ Mobile: SignatureHelpPill.show(info, anchor = above-cursor-line)
  → as user types parameters, re-trigger on `,` → updates activeParameter
  → dismiss on `)` or Esc or 30s timeout
```

### 5.4 Formatting

**On-save:**
```
User saves file (Ctrl+S / app navigation away with autosave)
  → if Settings.formatOnSave: 
      → LspServerHost.formatting(langId, uri, indentSize, useSpaces)
      → returns List<TextEdit>
      → WorkspaceEditApplier.applyToCurrentFile(edits)
  → proceed with save
```

**Explicit:**
```
User invokes Ctrl+Shift+F or menu "Format document"
  → same as on-save body
```

**On-type:**
```
User types a character
  → check spec.serverCapabilities.documentOnTypeFormattingProvider.firstTriggerCharacter
  → if matches: LspServerHost.onTypeFormatting(langId, uri, line, char, char)
  → apply returned edits silently
```

### 5.5 Find-references

```
User invokes find-references (Mobile: long-press menu → "Find references"; Desktop: Shift+F12)
  → LspServerHost.references(langId, uri, line, char):
      ├ send textDocument/references with includeDeclaration=true
      ├ withTimeout(2000ms)
      └ return List<ReferenceLocation>
  → if empty: Toast "No references found"
  → else: ReferencesPanel.show(list, currentUri, currentCursor):
      ├ persistent bottom drawer; rows show filename:line + context line preview
      └ click row → push to EditorNavigationStack + openFileAt(target)
  → drawer stays open across navigation (user can jump around)
  → close via X button or device-back when drawer has focus
```

## 6. Error handling

| Failure | Detection | Recovery |
|---|---|---|
| Server returns null/empty rename WorkspaceEdit | mapping detects | Toast "No rename available" |
| User cancels rename in preview | UI Cancel button | dismiss; no edits applied |
| `textDocument/rename` times out (2s) | withTimeout | Toast "Rename request timed out — retry?" |
| `textDocument/codeAction` times out (1s) | withTimeout | lightbulb hides; gutter empty |
| Code action `command` field requires server execution that fails | server returns error response | Toast with error message; preserve editor state |
| Signature help times out (300ms) | withTimeout | Pill/popup doesn't appear; user continues typing |
| Signature help arrives after cursor moved past `(` | check cursor still inside parens | suppress display |
| Formatting times out (1s explicit, 2s on-save) | withTimeout | Toast "Format request timed out"; save proceeds without format |
| Formatting returns edits that conflict with concurrent edits | comparing baseVersion vs current | reject + Toast "Document changed; retry format" |
| References times out (2s) | withTimeout | Toast "References request timed out" |
| WorkspaceEdit applies to a file that's been modified since LSP indexed it | version mismatch on textDocument | Reject the edit; Toast "File changed; re-run rename" |
| WorkspaceEdit targets a file outside the workspace | URI scheme check | Same `jdt://` handling as iter-62: Toast "Cannot edit `<uri>`" |
| User invokes rename while another rename is in progress | RenameAction state lock | Toast "Rename already in progress" |
| Lightbulb polling produces N requests/sec under heavy editing | debounce 500ms + cancel in-flight | Only latest survives |

`CancellationException` rethrown in every catch (CONST-035 + Detekt).

## 7. Per-platform feasibility

| Platform | Rename | Code actions | Signature help | Formatting | References | Status |
|---|---|---|---|---|---|---|
| **Android** | Full (UI surface; binary-blocked in v1) | Full (3-column gutter) | Full (pill variant) | Full (on-save toggle in Settings) | Full (bottom drawer) | All UI wired; awaits Android binaries |
| **Desktop macOS-arm64** | Full | Full | Full (tooltip variant) | Full | Full | Full support against iter-61 8 servers |
| **Desktop Linux/Windows** | Code-ready | Code-ready | Code-ready | Code-ready | Code-ready | Deferred per `#crossbuild-linux-windows-infra` |
| **iOS** | Stub | Stub | Stub | Stub | Stub | Hard-blocked (App Store 2.5.2) |
| **Web Wasm** | Stub | Stub | Stub | Stub | Stub | Deferred |

## 8. Deep-research checklist

Phase 0 output: `docs/features/lsp-4c/research-report.md` (≥ 600 lines, ≥ 100 citations).

1. **LSP4J 1.0.0 typing for the 5 new methods** — verify `WorkspaceEdit`, `CodeAction`, `SignatureHelp`, `TextEdit[]`, `Location[]` shapes. Document any Either-typed surprises.
2. **WorkspaceEdit application semantics** — text-document-edits vs document-changes vs old-style "changes" field. LSP 3.18 deprecation status of legacy field.
3. **Server-declared on-type formatting trigger characters** — how to read `serverCapabilities.documentOnTypeFormattingProvider.firstTriggerCharacter` per server (rust-analyzer? gopls?).
4. **Compose multi-pane modal panel pattern** — for RenamePreviewPanel (per-file expandable). Material3 `BottomSheetScaffold` vs `ModalNavigationDrawer` vs custom.
5. **Code action command vs edit dichotomy** — when does a CodeAction return only a `command` (requiring `workspace/executeCommand`) vs an inline `edit`? Document per-server behavior.
6. **Signature help cursor-anchor strategy in Compose** — pixel-precise anchor like iter-62 hover; revisit deferred `#iter-62-phase-8-hover-precise-anchor`.
7. **`textDocument/references` `includeDeclaration` flag** — whether to include the symbol's own definition in results. Standard behavior: true.
8. **Format-on-save race condition** — what if user saves twice in rapid succession + format takes longer than save round-trip? Document the lock semantic.

## 9. Testing strategy (anti-bluff per CONST-035)

### 9.1 Unit tests (commonTest pure; desktopTest JVM-bound)

1. `WorkspaceEditTest` — round-trip + apply to in-memory map. Mutation: stub apply to no-op → 2 tests FAIL.
2. `TextEditTest` — apply single edit; conflicting-range protection; range clamping. Mutation: apply stub → 2 FAIL.
3. `CodeActionTest` — mapping LSP4J `Either<Command, CodeAction>` → Yole CodeAction. Mutation: stub mapping → 2 FAIL.
4. `SignatureHelpTest` — mapping LSP4J SignatureHelp → Yole SignatureHelp; activeSignature/activeParameter index handling.
5. `WorkspaceEditApplierTest` — apply to multi-file Map; preserves untouched files; mutates targeted files correctly; conflict detection.

### 9.2 Per-host method tests (desktopTest, degradation pattern)

6. `LspServerHostRenameTest` — no spec → null; with spec but server returns null → null. Mutation: stub rename → fake WorkspaceEdit → no-spec test FAILS.
7. `LspServerHostCodeActionsTest` — similar.
8. `LspServerHostSignatureHelpTest` — similar.
9. `LspServerHostFormattingTest` — similar.
10. `LspServerHostReferencesTest` — similar.

### 9.3 Compose render tests (Robolectric)

11. `RenamePreviewPanelRobolectricTest` — feed canned WorkspaceEdit with 2 files → 2 file rows displayed; Apply button invokes callback.
12. `CodeActionLightbulbRobolectricTest` — line has ≥ 1 action → lightbulb visible; line has 0 → hidden.
13. `CodeActionMenuRobolectricTest` — 3 actions → 3 menu items; click invokes callback.
14. `SignatureHelpPillRobolectricTest` — canned SignatureHelp → pill displayed with active parameter highlighted.
15. `SignatureHelpPopupRobolectricTest` — desktop variant.
16. `ReferencesPanelRobolectricTest` — 3 references → 3 rows; click invokes callback with target URI.
17. `RenameActionRobolectricTest` — long-press menu → "Rename" → input prompt opens.

### 9.4 Integration (desktopTest, real LSP servers)

18. `RealServerRenameSmokeTest` — rust-analyzer on a trivial fixture → rename produces non-empty WorkspaceEdit. `assumeTrue` if binaries absent.
19. `RealServerFormattingSmokeTest` — marksman on misformatted Markdown → returns TextEdit list.
20. `RealServerSignatureHelpSmokeTest` — rust-analyzer at `println!(` position → returns SignatureHelp.

### 9.5 Structural anti-bluff

21. `Iter63FeatureWiringTest` — `LspServerHost` expect class declares all 5 new methods; iOS + Wasm stubs include all 5; UI files exist.

### 9.6 Challenges (Phase 9)

22. `lsp_refactoring_capabilities_challenge.sh` — STATIC: foundation + UI files exist; RUNTIME: ≥ 20 desktopTest PASS + ≥ 6 Robolectric PASS.
23. `lsp_workspace_edit_applier_challenge.sh` — STATIC: applier file exists; RUNTIME: applier-specific test suite ≥ 5 PASS.

Every test mutation-verified; procedure in KDoc.

## 10. Phase breakdown

| Phase | Scope | TDD depth |
|---|---|---|
| 0 | Deep research → `docs/features/lsp-4c/research-report.md` | Research only |
| 1 | `WorkspaceEdit` + `TextEdit` + `WorkspaceEditApplier` | Full TDD |
| 2 | Extend `LspServerHost` with 5 suspend methods + stub bodies | Full TDD |
| 3 | `CodeAction` + `SignatureHelp` + `ReferenceLocation` LSP4J mapping | Full TDD |
| 4 | Per-host JVM bodies (rename, codeActions, signatureHelp, formatting, references) | Full TDD with degradation tests |
| 5 | `RenamePreviewPanel` + `RenameAction` + `WorkspaceEditApplier` UI invocation | Full TDD |
| 6 | `CodeActionLightbulb` + `CodeActionMenu` + `CodeActionInvoker` (3rd gutter column) | Full TDD |
| 7 | `SignatureHelpPill` (mobile) + `SignatureHelpPopup` (desktop) + `SignatureHelpTrigger` | Full TDD |
| 8 | `FormattingTrigger` (on-save + explicit + on-type) + `FormattingSettings` toggle | Full TDD |
| 9 | `ReferencesPanel` (bottom drawer) | Full TDD |
| 10 | `IdeEditorScreen` integration: wire all 5 surfaces + gesture/shortcut bindings | Task-level + Robolectric |
| 11 | 2 anti-bluff challenges + `qa-iter-63-gates` chained into `qa-all` | Full TDD on shell scripts |
| 12 | Documentation: user-guide, architecture, CHANGELOG v1.6.0, CONTINUATION.md | Authoring only |
| 13 | Firebase distribution v1.6.0 | Mechanical |

**14 phases** vs iter-62's 12 — the 5-capability scope demands more granular UI phases.

## 11. Documentation deliverables

- `docs/features/lsp-4c/user-guide.md`
- `docs/features/lsp-4c/architecture.md`
- `docs/features/lsp-4c/research-report.md`
- CHANGELOG.md v1.6.0 entry
- docs/CONTINUATION.md per CONST-036

## 12. Firebase distribution

Same pattern. Version target: **1.6.0 / versionCode 160 / dotted `0.0.0.1.60`**.

## 13. Out of scope for 4c

- Workspace symbols (workspace/symbol) — searching by symbol across workspace.
- Inlay hints — parameter names rendered next to call sites.
- Semantic highlighting — beyond syntax-highlighting from iter-57.
- Code lens — actionable runnable annotations.
- Document highlight — usage of symbol-under-cursor within current file (mini find-references).
- Linked editing range — rename-style live editing without invoking rename.
- LLM-driven anything.

These remain future work; not part of any iteration in the original 5-feature mandate.

## 14. Open questions for the implementation plan

- WorkspaceEditApplier needs the editor's open-files map. How does it get it? Phase 0 §4 resolves via Yole's existing file-open infrastructure.
- Lightbulb polling cadence: 500ms debounce + cancel-in-flight. Phase 0 §5 confirms acceptable.
- Format-on-save lock semantic. Phase 0 §8 resolves.

## 15. Forensic anchor

Brainstorm session 2026-05-16 with operator. Operator chose at every round:

1. iter-63 scope = "All 5 capabilities" (heaviest).
2. Rename UX = "Preview-diff panel + confirm" (heaviest, safest).
3. Code actions = "Light-bulb gutter icon + long-press menu" (3-column gutter).
4. Signature help = "Inline pill on mobile + tooltip popup on desktop" (smart per-platform).
5. Formatting = "All three: on-save + explicit + on-type" (Comprehensive).
6. References = "Dedicated References panel (bottom drawer, persistent)".

No defaults selected; every choice explicit.

---

**Next steps:** writing-plans creates the 14-phase plan; subagent-driven execution proceeds autonomously per operator's pre-approval.
