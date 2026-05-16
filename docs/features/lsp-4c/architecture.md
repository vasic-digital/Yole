<!-- SPDX-FileCopyrightText: 2026 Milos Vasic -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# LSP Refactoring Capabilities — Architecture (iter-63, v1.6.0)

> This document records the iter-63 extension pattern and serves as the
> **template for future LSP capabilities** (Feature 4d and beyond).

---

## 1 — The "extending LspServerHost" pattern

iter-63 adds 5 new capabilities following the pattern established in iter-62.
Every capability follows the same 6-layer structure:

```
Layer 1: expect/actual suspend method on LspServerHost
Layer 2: LSP4J mapping helpers (mapLspXxx functions)
Layer 3: Requester interface (testability decoupling)
Layer 4: UI Composable(s)
Layer 5: Trigger / Orchestrator (keyboard / timer / polling)
Layer 6: IdeEditorScreen wiring
```

### Layer 1 — expect/actual suspend method

Each capability is a `suspend fun` on the `expect class LspServerHost`:

```kotlin
// shared/src/commonMain/.../lsp/LspServerHost.kt
expect class LspServerHost {
    suspend fun rename(langId: String, uri: String, line: Int, character: Int, newName: String): WorkspaceEdit?
    suspend fun codeActions(langId: String, uri: String, range: IntRange): List<CodeAction>
    suspend fun signatureHelp(langId: String, uri: String, line: Int, character: Int): SignatureHelp?
    suspend fun formatting(langId: String, uri: String, indentSize: Int = 4, useSpaces: Boolean = true): List<TextEdit>
    suspend fun references(langId: String, uri: String, line: Int, character: Int, includeDeclaration: Boolean = true): List<DefinitionLocation>
}
```

**Platform matrix:**

| Platform | Body |
|----------|------|
| `desktopMain` | LSP4J call + `withTimeout` + mapper |
| `androidMain` | Same JVM body as desktop (shared via `jvmMain` if possible, else copy) |
| `iosMain` | `return null` / `return emptyList()` |
| `wasmJsMain` | `return null` / `return emptyList()` |

**Timeout values** (chosen conservatively for user-facing responsiveness):

| Method | Timeout |
|--------|---------|
| `rename` | 2 000 ms |
| `codeActions` | 1 000 ms |
| `signatureHelp` | 300 ms |
| `formatting` | 1 000 ms |
| `references` | 2 000 ms |

`TimeoutCancellationException` is caught per method and returns the degraded
value (`null` / `emptyList()`). `CancellationException` (non-timeout) is always
rethrown — see CONST coroutine safety rules.

### Layer 2 — LSP4J mapping helpers

Mapping functions live in `LspServerHost.desktop.kt` as `internal` top-level
functions. They have no dependency on the `LspServerHost` instance and can be
unit-tested in isolation.

```kotlin
// internal — desktopMain
internal fun mapLspWorkspaceEdit(lspEdit: org.eclipse.lsp4j.WorkspaceEdit?): WorkspaceEdit
internal fun mapLspCodeAction(either: Either<Command, org.eclipse.lsp4j.CodeAction>): CodeAction
internal fun mapLspSignatureHelp(lspHelp: org.eclipse.lsp4j.SignatureHelp?): SignatureHelp?
internal fun mapLspTextEdits(lspEdits: List<org.eclipse.lsp4j.TextEdit>?, docText: String): List<TextEdit>
```

**WorkspaceEdit mapping** handles both LSP 3.x forms:
- `changes: Map<String, List<TextEdit>>` (legacy)
- `documentChanges: List<Either<TextDocumentEdit, ResourceOperation>>` (modern, LSP 3.13+)

**CodeAction mapping** handles the LSP4J `Either<Command, CodeAction>` union:
- Left (Command) → `CodeAction(title=command.title, kind=null, edit=null, command=command.command)`
- Right (CodeAction) → full mapping including nested `WorkspaceEdit`

**SignatureHelp mapping** preserves `activeSignature` + `activeParameter` indices
and maps `ParameterInformation.label` to the Yole `ParameterInformation` type.

**TextEdit mapping** re-uses `LspRangeMapping.lineColToOffset(line, char, text)`
from iter-62 for consistent range conversion across all LSP features.

### Layer 3 — Requester interface (testability)

Each capability exposes a single-method interface that `IdeEditorScreen` depends
on instead of `LspServerHost` directly. This mirrors the iter-62
`LspDefinitionRequester` pattern:

```kotlin
// shared/src/commonMain/.../lsp/
interface LspRenameRequester     { suspend fun rename(...): WorkspaceEdit? }
interface LspCodeActionRequester { suspend fun codeActions(...): List<CodeAction> }
interface LspSignatureHelpRequester { suspend fun signatureHelp(...): SignatureHelp? }
interface LspFormattingRequester { suspend fun formatting(...): List<TextEdit> }
interface LspReferencesRequester { suspend fun references(...): List<DefinitionLocation> }
```

In production `IdeEditorScreen` creates adapters:

```kotlin
val renameRequester = LspRenameRequester { langId, uri, line, char, newName ->
    lspHost.rename(langId, uri, line, char, newName)
}
```

In tests a fake implementation returns canned data:

```kotlin
val fakeRename = LspRenameRequester { _, _, _, _, _ -> fakeEdit }
```

### Layer 4 — UI Composables

| Capability | Composable(s) | Location |
|-----------|--------------|----------|
| Rename | `RenameAction`, `RenamePreviewPanel` | `androidApp/.../rename/` |
| Code Actions | `CodeActionLightbulb`, `CodeActionMenu`, `CodeActionInvoker` | `androidApp/.../codeaction/` |
| Signature Help | `SignatureHelpPill` (mobile), `SignatureHelpPopup` (desktop) | `androidApp/.../signaturehelp/` |
| Formatting | `FormattingSettings` (Settings row) | `androidApp/.../formatting/` |
| Find References | `ReferencesPanel` | `androidApp/.../references/` |

All Composables carry `testTag` annotations enabling Robolectric structural
inspection (see §4 — Testing strategy).

### Layer 5 — Trigger / Orchestrator

Stateful orchestrators live in `shared/src/commonMain` where possible:

| Component | Module | Responsibility |
|-----------|--------|---------------|
| `SignatureHelpTrigger` | `commonMain` | Keystroke detector + debounce + auto-dismiss timer |
| `FormattingTrigger` | `commonMain` | 3 entry points: `onSave`, `onExplicit`, `onType` |
| `FindReferencesAction` | `commonMain` | Orchestrates `LspReferencesRequester` + result routing |

`RenameAction` and `CodeActionInvoker` are Composables rather than separate
orchestrators because their logic is inherently tied to UI state.

### Layer 6 — IdeEditorScreen wiring (YoleApp.kt)

`IdeEditorScreen` composes all capabilities into a single screen. The wiring
pattern per capability:

```
1. Create Requester adapter (wraps LspServerHost method)
2. Declare UI state (showX: Boolean, xResult: XType?)
3. LaunchedEffect or onTextChanged callback for automatic triggers
4. Pass Requester + state-setter to Trigger/Orchestrator/Composable
5. Overlay UI Composable in the editor Box (or as bottom drawer)
```

---

## 2 — Data model: WorkspaceEdit + TextEdit + WorkspaceEditApplier

### TextEdit

```kotlin
data class TextEdit(val range: IntRange, val newText: String) {
    fun apply(text: String): String  // clamps out-of-bounds
}
```

`IntRange` is character-offset based, converted from LSP line/character pairs by
`LspRangeMapping.lineColToOffset`.

### WorkspaceEdit

```kotlin
data class WorkspaceEdit(val changes: Map<String, List<TextEdit>> = emptyMap()) {
    val isEmpty: Boolean
}
```

Keys are document URIs (e.g. `file:///path/to/Foo.kt`).

### WorkspaceEditApplier

```kotlin
object WorkspaceEditApplier {
    class ApplyConflict(message: String) : RuntimeException(message)
    fun apply(edit: WorkspaceEdit, sources: Map<String, String>): Map<String, String>
}
```

- Validates non-overlapping edits per file (throws `ApplyConflict` if violated).
- Applies edits in **reverse offset order** so earlier offsets stay valid.
- Preserves URIs not mentioned in the edit.
- Skips URIs present in the edit but absent from `sources` (honest: server may
  reference files not in the current in-memory map).

---

## 3 — RenamePreviewPanel architecture

The panel is the most complex UI component in iter-63. Its state model:

```
WorkspaceEdit
    │
    ├── Map.Entry("file://A.kt", [TextEdit, TextEdit])
    │       expanded: Boolean (local state per row)
    │       visible: filename + edit count
    │       expanded content: before-text / after-text per TextEdit
    │
    └── Map.Entry("file://B.kt", [TextEdit])
            ...

Buttons:
    Apply  → onApply(WorkspaceEdit)
    Cancel → onDismiss()
```

The before/after text for each `TextEdit` is computed lazily from the
in-memory `sources: Map<String, String>` passed to the panel — it does not
re-read from disk.

---

## 4 — Testing strategy

### Unit tests (commonTest / desktopTest)

| Test class | Scope |
|-----------|-------|
| `TextEditTest` | `apply` clamps + replaces correctly |
| `WorkspaceEditTest` | isEmpty helper, round-trip |
| `WorkspaceEditApplierTest` | single-file, multi-file, conflict, unknown URI |
| `WorkspaceEditMappingTest` | LSP4J → Yole mapping (modern + legacy forms) |
| `CodeActionMappingTest` | Either<Command, CodeAction> both branches |
| `SignatureHelpMappingTest` | activeSignature + activeParameter preserved |
| `TextEditMappingTest` | lineColToOffset re-used from LspRangeMapping |
| `SignatureHelpTriggerTest` | trigger on `(`, `,`; dismiss on `)` |
| `FormattingTriggerTest` | onSave/onExplicit/onType paths |

All tests use `runBlocking<Unit> { }` (JUnit4 constraint — `runTest` returns
`TestResult`, causing a JVM `void` signature mismatch).

### Robolectric structural tests (androidUnitTest)

Structural tests use source-code reflection rather than Compose UI tree
rendering. This pattern was established in iter-62 to work around Robolectric
Compose interaction brittleness.

Each test asserts on `testTag` strings and structural source-file presence:

```kotlin
@Test
fun renamePreviewPanel_hasApplyTag() {
    val source = File("...RenamePreviewPanel.kt").readText()
    assertTrue(source.contains("testTag(\"rename-apply\")"))
}
```

Mutation guard: if the production file's `testTag` line is removed, the test
fails — satisfying CONST-035.

### Anti-bluff challenges (Phase 11)

| Challenge | What it verifies |
|-----------|-----------------|
| `lsp_refactoring_capabilities_challenge.sh` | Static: 6 source files present; `rename(`, `codeActions(`, `signatureHelp(`, `formatting(`, `references(` call sites exist in desktop actual. Runtime: 5 desktopTest test classes ≥ N PASSED, 0 FAILED. |
| `lsp_workspace_edit_applier_challenge.sh` | Static: `WorkspaceEditApplier.kt` present, `ApplyConflict` present. Runtime: `WorkspaceEditApplierTest` ≥ 4 PASSED, 0 FAILED. |

Both challenges are chained into `make qa-iter-63-gates` → `make qa-all`.

---

## 5 — Template: adding a 6th LSP capability

To add a new LSP capability (e.g. `textDocument/inlayHint`) following this
pattern:

1. **Spec the LSP4J API** — identify the method signature on
   `TextDocumentService` and the response type(s).
2. **Add mapping helpers** in `LspServerHost.desktop.kt` as `internal fun
   mapLspXxx(...)`.
3. **Add `suspend fun xxx(...)` to the `expect class`** in commonMain; add
   JVM body to both `desktopMain` and `androidMain` actuals; add stub
   (`return null` / `return emptyList()`) to `iosMain` and `wasmJsMain`.
4. **Add a `LspXxxRequester` interface** in commonMain for testability.
5. **Write a `Xxx` Composable** in `androidApp/.../xxx/` with `testTag`
   annotations. Place the desktop variant under `desktopApp/` if divergent.
6. **Write a `XxxTrigger` or orchestrator** in commonMain if the capability
   has debounce / timer logic; otherwise keep in `IdeEditorScreen`.
7. **Wire in `IdeEditorScreen`**: Requester adapter → state → LaunchedEffect /
   callback → Composable overlay.
8. **Tests**: mapping unit tests (desktopTest), trigger tests (desktopTest or
   commonTest), Robolectric structural tests (androidUnitTest).
9. **Anti-bluff challenge**: new `.sh` in `yole-challenges/scripts/`; wire into
   `qa-iter-NNN-gates` and then into `qa-all`.
10. **Document**: update user-guide.md, architecture.md (this file), CHANGELOG,
    CONTINUATION.md per CONST-036/037/038.

---

## 6 — Cross-platform disposition (CONST-037)

| Platform | iter-63 disposition |
|----------|---------------------|
| Android | All 5 capabilities fully wired in `IdeEditorScreen`. F2 / Shift+F12 key handlers via `SyncedScrollEditor.onPreviewKeyEvent`. Toolbar buttons for mobile reach. 14+ Robolectric + unit tests pass. |
| Desktop macOS-arm64 | `LspServerHost` actual methods implemented and tested. F2 / Shift+F12 `onPreviewKeyEvent` handlers compile. Desktop UI wiring (popup / drawer rendering) deferred — see `#iter-63-desktop-signature-help-popup-deferred`. |
| Desktop Linux / Windows | Same JVM code as macOS; distribution gated on `#crossbuild-linux-windows-infra` (iter-62 tracker). |
| iOS | All 5 new `LspServerHost` methods return null/emptyList via stub actuals. No UI changes. Hard-blocked by App Store §2.5.2 native subprocess restriction. |
| Web (Wasm) | Same as iOS — stubs throughout. Native subprocess not possible in browser sandbox. |

---

## 7 — Known gaps consolidated (CONST-035 honesty)

| Tracker ID | Description | Severity |
|-----------|-------------|---------|
| `#iter-63-longpress-gesture-detector` | Full long-press context menu on `BasicTextField` for Rename + Find References. Toolbar buttons are the v1 access surface on mobile. | Low — feature accessible via toolbar |
| `#iter-63-desktop-signature-help-popup-deferred` | Desktop editor does not yet render SignatureHelpPopup, RenamePreviewPanel, or ReferencesPanel. All server-side calls work; UI wiring deferred. | Medium |
| `#iter-63-server-trigger-chars-hardcoded` | On-type formatting trigger chars `{';', '}', '\n'}` and signature-help trigger chars `(`, `,` are hardcoded. Server capability query deferred. | Low |
| `#iter-63-format-on-save-settings-toggle` | `FormattingTrigger` passes `settings = { false }`. Settings toggle UI deferred. Format-on-save is effectively always on. | Low |
| `#iter-63-on-type-edit-apply` | On-type formatting: `List<TextEdit>` obtained from server but not applied to editor buffer. Buffer-update step deferred. | Medium |
| `#iter-63-explicit-format-edit-apply` | Explicit (Ctrl+Shift+F) formatting: same as above. | Medium |
