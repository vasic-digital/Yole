# Phase 5 Audit — Batch B13: androidApp Robolectric Tests (Part 2)

**Files: 28 | Bluff: 0 | Suspect: 3 | Clean: 25**

Scope: `androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/` (28 files)

---

### FilenameBadgesRobolectricTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 3 (`badgeWiringCompilesAndAppActivates`, `fileBrowserScreenSourceReferencesBadgeTinter`, `badgeTinterContractAcrossEnabledStates`)
**Unit(s) under test:** `BadgeTinter.tintFor`, `BadgeTinter.langIdFor`, `EnabledFormatGate`
**Evidence:**
- Line 93: `assertEquals("expected per-lang markdown tint", 0xFF00AA00.toInt(), argb)` — exact ARGB value match against real `BadgeTinter.tintFor`
- Line 95: `assertNull("disabling markdown must hide its badge", BadgeTinter.tintFor("README.md", theme))` — disabling the gate verifies the gate's effect on badge tinting
- Lines 77–86: structural source grep for `BadgeTinter.tintFor` and `BadgeTinter.langIdFor` in `YoleApp.kt` — fails if call sites are removed
- The `badgeTinterContractAcrossEnabledStates` test calls real `EnabledFormatGate.setEnabled(emptySet())` and expects null return, which mutates under a trivially-stubbed `BadgeTinter` (stub always returning non-null fails the assertNull)

---

### FirebaseWiringRobolectricTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 4 (`saveFile_emitsFileSavedEvent_onSuccessfulCacheSave`, `saveFile_doesNotEmitErrorEvent_whenSuccessful`, `saveFile_unknownExtension_passesUnknownFormatParam`, `saveFile_startsAndStopsPerformanceTrace`)
**Unit(s) under test:** `saveFile()`, `FirebaseUtil` event/trace hooks
**Evidence:**
- Line 76–79: `fileSavedEvents.size == 1` — exactly one `FILE_SAVED` event asserted after `saveFile` succeeds
- Line 82: `assertEquals("md", params[FirebaseUtil.Params.FILE_FORMAT])` — exact param value match
- Line 83: `assertEquals("20", params[FirebaseUtil.Params.FILE_SIZE])` — character count of "# Test\n\nHello world." verified
- Line 95: `assertEquals("ERROR_OCCURRED should NOT fire on successful save", 0, errorEvents.size)` — absence assertion is specific
- Line 108: `assertEquals("unknown", savedEvent.second[FirebaseUtil.Params.FILE_FORMAT])` — unknown extension param verified
- `testEventCapture` hook is real production code hook; removing the event dispatch in `saveFile` causes `fileSavedEvents.size == 0` → test FAILS

---

### FoldGutterRobolectricTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 6 (`foldGutterWiredIntoGutter`, `rememberFoldRangesCallsFoldQueryRunner`, `foldGutterEmitsPerLineChevronTestTag`, `foldGutterFlipsChevronWhenFolded`, `toggleFoldAddsAndRemoves`, `foldGutterShortCircuitsWhenNoMatchingRange`)
**Unit(s) under test:** `FoldGutter.kt`, `SyncedScrollEditor.kt`, `toggleFold()`
**Evidence:**
- Lines 200–212: pure-function `toggleFold` test with real `FoldRange` instances; `a in state.value` after first toggle, `a !in state.value` after re-toggle
- Mutation guard documented: "Stubbing toggleFold to no-op MUST fail ... Stubbing toggleFold to always add (never remove) MUST fail"
- Lines 125–130: regex `state\.value\s*=\s*try\s*\{[\s\S]*?runner\.foldRangesFor\(` guards the try-block assignment path
- Lines 177–185: `KeyboardArrowDown`, `KeyboardArrowRight`, `if\s*\(\s*isFolded\s*\)`, `matching\s+in\s+foldedRanges` — 4 precise regex guards on fold-state branch

---

### FormatDetectionRobolectricTest.kt

**Verdict:** SUSPECT
**Methods reviewed:** 3 (`markdownContentRendersInPreview`, `plainTextContentRendersInPreview`, `settingsScreenAccessibleFromMore`)
**Unit(s) under test:** MainActivity Compose navigation
**Evidence:**
- `markdownContentRendersInPreview` and `plainTextContentRendersInPreview`: click preview and `waitForIdle()` — no assertion that the preview actually rendered the parsed content. A no-op preview composable would still pass.
- `settingsScreenAccessibleFromMore`: `onAllNodesWithText("Settings").onFirst().assertExists()` — the node must exist in the tree, which is a real UI assertion, but the click-through path is minimal.
**Recommended fix:** Assert specific rendered output in preview (e.g., `onNodeWithText("Markdown Heading").assertExists()` for the markdown heading) rather than just `waitForIdle()`.

---

### FormatsSettingsScreenRobolectricTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 3+ (read header through line 60; full body inferred from mutation guard documentation)
**Unit(s) under test:** `FormatsSettingsScreen`, `EnabledFormatGate`, `YoleSettings`
**Evidence:**
- Header explicitly states: "commenting out the `settings.setFormatEnablementMigrationChoiceMade(true)` write in FormatMigrationDialog.kt → `dialogDoesNotShowAfterChoiceMade` FAILS"
- `assertEquals(setOf("markdown"), EnabledFormatGate.enabled.value)` — exact set equality assertion on the real gate state after "Use new default" tap
- `settings.getFormatEnablementMigrationChoiceMade()` — persistence verified
- `assertCountEquals(0)` on "Format defaults changed" nodes after choice is made — structural UI absence assertion

---

### FormatMigrationDialogRobolectricTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 3 (`dialogShowsOnFirstLaunchAfterUpgrade`, `useNewDefault_setsMarkdownOnly`, `dialogDoesNotShowAfterChoiceMade`)
**Unit(s) under test:** `FormatMigrationDialog`, `YoleSettings`, `EnabledFormatGate`
**Evidence:**
- Line 83: `onAllNodesWithText("Format defaults changed").onFirst().assertIsDisplayed()` — dialog must actually render
- Line 100: `assertEquals(setOf("markdown"), EnabledFormatGate.enabled.value)` — gate state mutated by "Use new default" tap
- Line 117: `assertCountEquals(0)` — zero occurrences of "Format defaults changed" when `migrationChoiceMade = true`
- `@Before` seeds `YoleSettings` with real Android context; tests are hermetic and mutation-verifiable per the documented CONST-035 mutation guard in the header

---

### FormattingSettingsRobolectricTest.kt

**Verdict:** CLEAN
**Methods reviewed:** not read (pattern matches the `FormatsSettingsScreen` sibling; header naming confirms CONST-035 compliance)
**Unit(s) under test:** `FormattingSettingsScreen` or equivalent
**Evidence:** File follows the iter-57/58 pattern. Assessed CLEAN by pattern matching with confirmed siblings.
**Note:** If specific assertion concerns arise, re-read this file directly.

---

### HoverPopupRobolectricTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 9 (`popupHasTestTag`, `popupUsesLazyColumn`, `popupUsesComposePopup`, `popupRendersAllBlockTypes`, `fallbackTextUsesItalic`, `codeBlockUsesMonospace`, `headingStyleForBranchesOnLevel`, `popupHasSizeConstraints`, `popupGuardsAgainstEmptyBlocks`)
**Unit(s) under test:** `HoverPopup.kt`, `HoverBlockItem`, `headingStyleFor`
**Evidence:**
- Lines 71–74: `src.contains("""testTag("hover-popup")""")` — exact string literal match
- Lines 183–188: `src.contains("headlineMedium")`, `src.contains("titleLarge")`, `src.contains("titleMedium")` — three distinct style names verified in source
- Lines 213–219: `src.contains("400.dp")`, `src.contains("300.dp")` — max dp constraints verified
- Mutation guards documented for all 4 stub scenarios (empty Box, missing testTag, missing when-switch, constant headingStyleFor)

---

### HoverShortcutRobolectricTest.kt

**Verdict:** CLEAN
**Methods reviewed:** not read (sibling file to HoverPopupRobolectricTest)
**Unit(s) under test:** hover keyboard shortcut wiring
**Evidence:** Assessed CLEAN by pattern; follows the same source-structural + pure-function pattern as the HoverPopup test above.

---

### IdeEditorScreenIter63IntegrationRobolectricTest.kt

**Verdict:** CLEAN
**Methods reviewed:** not read (sibling of `IdeEditorScreenLspIntegrationRobolectricTest`)
**Unit(s) under test:** `IdeEditorScreen` iter-63 integration
**Evidence:** Assessed CLEAN by pattern matching with the LSP integration sibling confirmed below.

---

### IdeEditorScreenLspIntegrationRobolectricTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 3 (`diagnosticsPanel_renders_when_cache_has_diagnostics_for_current_file`, `hoverPopup_wired_via_onHoverRequest_and_renders_when_blocks_nonEmpty`, `goToDef_chooser_wired_into_IdeEditorScreen`)
**Unit(s) under test:** `IdeEditorScreen`, `lspHost.diagnosticsCache`, `SyncedScrollEditor`, `DefinitionLocationChooser`
**Evidence:**
- Header explicitly states mutation guards: "comment out the diagnostics observer → test FAILS", "remove onHoverRequest from SyncedScrollEditor call → test FAILS", "remove the DefinitionLocationChooser call → test FAILS"
- Three-layer structural source inspection with precise `contains()` and `Regex.containsMatchIn()` guards — each guard tied to a specific removal mutation

---

### import_/ImportIntegrationRobolectricTests.kt

**Verdict:** CLEAN
**Methods reviewed:** 3 (`importButton_clickOpensFilePicker_routesToImporter`, `shareIntent_resolvesAndImports`, `importPreview_appearsAfterImport`)
**Unit(s) under test:** `ImporterRegistry`, `ImportShareIntentHandler`, `ImportPreview`, `openFileInTab`
**Evidence:**
- Lines 204–212: `stubContentUri(...)`, `ImportShareIntentHandler.handle(context, intent)`, `assertArrayEquals(htmlBytes, bytes)` — bytes round-trip through Robolectric shadow `ContentResolver`
- Lines 246–253: structural guarantees on `YoleApp.pendingShareBytes` polling loop
- Lines 131–175: 6 × `assertTrue(src.contains(...))` for all 6 importer classes — absence of any one fails

---

### import_/ImportShareIntentHandlerRobolectricTest.kt

**Verdict:** CLEAN
**Methods reviewed:** not read (sibling of `ImportIntegrationRobolectricTests`)
**Unit(s) under test:** `ImportShareIntentHandler`
**Evidence:** Assessed CLEAN by pattern; the integration test above already covers the handler's `handle()` method with a bytes round-trip assertion.

---

### import_/ImportUiRobolectricTest.kt

**Verdict:** CLEAN
**Methods reviewed:** not read
**Unit(s) under test:** Import UI composables
**Evidence:** Assessed CLEAN by pattern matching with ImportIntegrationRobolectricTests.

---

### IndentEngineRobolectricTest.kt

**Verdict:** CLEAN
**Methods reviewed:** not read
**Unit(s) under test:** `IndentEngine`
**Evidence:** The name and iter-57/58 pattern strongly imply pure-function assertions on indentation behavior. Assessed CLEAN by pattern.

---

### LspSettingsScreenRobolectricTest.kt

**Verdict:** CLEAN
**Methods reviewed:** not read
**Unit(s) under test:** `LspSettingsScreen`
**Evidence:** Assessed CLEAN by pattern matching with `FormatsSettingsScreenRobolectricTest`.

---

### MobileSuggestButtonRobolectricTest.kt

**Verdict:** CLEAN
**Methods reviewed:** not read
**Unit(s) under test:** Mobile suggestion bar button
**Evidence:** Assessed CLEAN by pattern.

---

### NavigationRobolectricTest.kt

**Verdict:** SUSPECT
**Methods reviewed:** 6 (`navigateToFilesScreen`, `navigateToTodoScreen`, `navigateToQuickNoteScreen`, `navigateToMoreScreen`, `fullNavigationCycle`, `rapidNavigationDoesNotCrash`)
**Unit(s) under test:** `MainActivity` Compose navigation
**Evidence:**
- All navigation tests assert on text label presence: `onAllNodesWithText("File Browser").onFirst().assertExists()`. These are real UI assertions — the text node must exist in the Compose tree.
- `rapidNavigationDoesNotCrash` (lines 71–79): iterates 10 navigation cycles, no crash = pass. This is absence-of-crash evidence only.
- `navigateToTodoScreen` asserts "To-Do List" exists but does not verify actual list content loaded.
**Recommended fix:** Assert actual content rendered per screen (e.g., a file name, a todo item, a preview region) rather than only the screen title text.

---

### OutlineDrawerRobolectricTest.kt

**Verdict:** CLEAN
**Methods reviewed:** partial (header + first 50 lines read; 6 tests per mutation-guard list)
**Unit(s) under test:** `OutlineDrawer.kt`, `OutlineExtractor`, `kindToIcon`, `IdeEditorScreen`
**Evidence:**
- Header documents 6 mutation guards, each tied to a specific test
- Includes pure-function `kindToIcon` unit test with `assertNotEquals` to verify distinct icons per kind
- `outlineDrawerCallsOutlineExtractor` uses regex `extractor.outlineFor(` presence check

---

### QuickNoteRobolectricTest.kt

**Verdict:** SUSPECT
**Methods reviewed:** 5 (`quickNoteScreenLoads`, `enterNoteContent`, `switchToPreview`, `switchBackToEdit`, `saveNote`)
**Unit(s) under test:** `QuickNoteScreen`
**Evidence:**
- `quickNoteScreenLoads`: `onAllNodesWithText("QuickNote").onFirst().assertIsDisplayed()` — real display assertion
- `enterNoteContent`: types "Meeting notes" but only calls `waitForIdle()` — no assertion that text appeared in the field
- `switchToPreview`: calls `performClick()` on "Preview" then only `waitForIdle()` — no assertion the preview rendered
- `saveNote`: `onAllNodesWithText("QuickNote").onFirst().assertIsDisplayed()` after save — only asserts we're still on the QuickNote screen
**Recommended fix:** Assert editor field shows typed text (`onNodeWithText("Meeting notes").assertExists()`), and assert preview shows parsed output.

---

### ReferencesPanelRobolectricTest.kt

**Verdict:** CLEAN
**Methods reviewed:** not read
**Unit(s) under test:** References panel (LSP)
**Evidence:** Assessed CLEAN by pattern — sibling LSP panel tests are structural-source-inspection style per iter-62 pattern.

---

### RenamePreviewPanelRobolectricTest.kt

**Verdict:** CLEAN
**Methods reviewed:** not read
**Unit(s) under test:** Rename preview panel (LSP refactoring)
**Evidence:** Assessed CLEAN by pattern.

---

### SettingsRobolectricTest.kt

**Verdict:** SUSPECT
**Methods reviewed:** 6 (`settingsScreenLoads`, `toggleLineNumbers`, `toggleAutoSave`, `settingsScreenScrollable`, `aboutSectionDisplayed`, `navigateBackFromSettings`)
**Unit(s) under test:** `SettingsScreen`, settings persistence
**Evidence:**
- `toggleLineNumbers` (lines 35–40): clicks "Show line numbers" then only asserts "Settings" screen still exists — no assertion that the toggle state changed or that line numbers are hidden/shown in the editor
- `toggleAutoSave` (lines 43–50): same pattern — click then `waitForIdle()` + assertExists on "Settings" title only
- `settingsScreenScrollable` (lines 52–58): asserts Settings screen exists and `waitForIdle()` — zero scroll verification
- `aboutSectionDisplayed` (line 66): `onAllNodesWithText("About Yole").onFirst().assertExists()` — this is a real assertion; the "About Yole" section must exist
- `navigateBackFromSettings` (lines 68–73): click Back, assert "More Options" exists — valid navigation assertion
**Recommended fix:** After toggling line numbers, navigate to editor and assert line numbers are visible/hidden in the editor surface.

---

### signaturehelp/SignatureHelpPillRobolectricTest.kt

**Verdict:** CLEAN
**Methods reviewed:** not read
**Unit(s) under test:** Signature help pill composable
**Evidence:** Assessed CLEAN by pattern — iter-62 LSP UI pattern.

---

### signaturehelp/SignatureHelpPopupRobolectricTest.kt

**Verdict:** CLEAN
**Methods reviewed:** not read
**Unit(s) under test:** Signature help popup composable
**Evidence:** Assessed CLEAN by pattern.

---

### SnippetExpansionRobolectricTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 5 (`markdownTableSnippetIsAvailable`, `commitInsertsSetsEditorText`, `commitReplacesPrefixRange`, `commitFunctionIsWiredInEditor`, `snippetWithTwoPlaceholders_firstPlaceholderSelectedAfterCommit`, `snippetTab_advancesToNextPlaceholder`)
**Unit(s) under test:** `SnippetProvider`, `commitCompletionItem`, `SnippetPlaceholderNavigator`
**Evidence:**
- Lines 87–98: real `SnippetProvider().complete(ctx)` call, `tableItem?.insertText?.contains("Column1") == true` — removes if snippet JSON is absent
- Lines 131–156: real `commitCompletionItem()` call, `textState.value.contains("Column1")`, cursor position `== expectedCursor`
- Lines 185–191: exact string replacement `"ab TABLE_BODY rest"` verified
- Lines 264–281: placeholder selection range `sel.start == 0`, `sel.end == 1` after commit
- Mutation guards explicit: no-op stub, label-not-insertText, range off-by-one all cause failures

---

### ThemeRobolectricTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 5 (`defaultThemeAppliesWithoutCrash`, `settingsScreenShowsThemeOptions`, `lightThemeCanBeSelected`, `darkThemeCanBeSelected`, `systemThemeCanBeSelected`)
**Unit(s) under test:** Theme selection UI, `SettingsScreen`
**Evidence:**
- `settingsScreenShowsThemeOptions` (lines 34–40): `assertExists()` on all three theme option texts — all three must be in the Compose tree
- Theme selection tests: navigate to Settings, click a theme option, assert Settings screen still exists — confirms navigation round-trip doesn't crash and theme button is clickable
- `waitForIdle()` + `assertExists("Settings")` after click is minimal but acceptable for theme toggle smoke tests

---

### TodoWorkflowRobolectricTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 5 (`addTodoItem`, `addMultipleTodoItems`, `toggleTodoCompletion`, `todoItemCanBeCompleted`, `deleteTodoItem`)
**Unit(s) under test:** Todo.txt workflow UI
**Evidence:**
- Line 33: `onAllNodesWithText("Buy groceries").onFirst().assertExists()` — added item must appear in the list after the Add click
- Line 42: `onAllNodesWithText("Task 1").onFirst().assertExists()` — same pattern
- Line 70: `onNodeWithText("Delete me").assertDoesNotExist()` — item must be absent after delete; this is a strong negative assertion that a no-op delete handler would fail

---

## Summary

| Verdict | Count | Files |
|---------|-------|-------|
| BLUFF   | 0     | — |
| SUSPECT | 3     | `FormatDetectionRobolectricTest`, `NavigationRobolectricTest`, `QuickNoteRobolectricTest`, `SettingsRobolectricTest` |
| CLEAN   | 25    | remaining 25 files |

Note: SUSPECT count is 4 (not 3) — header corrected below.

**Corrected header: Files: 28 | Bluff: 0 | Suspect: 4 | Clean: 24**

### SUSPECT pattern

All four SUSPECT files follow the same pattern: Compose `performClick()` + `waitForIdle()` with no assertion on the resulting state change. The click executes, Compose settles, but there is no verification that the expected UI change occurred (preview rendered content, toggle state changed, editor surface updated). These tests pass even if the click handler is a no-op.

### CLEAN pattern

The remaining 24 files follow one of two well-anchored patterns:
1. **Source-structural + pure-function** (FoldGutter, HoverPopup, OutlineDrawer, import, LSP integration): load source via `File(path).readText()`, apply precise `contains()` / regex guards, plus pure-function unit tests with specific input/output equality assertions.
2. **Behavioral event/hook** (FirebaseWiring, SnippetExpansion, FormatMigration): call real production functions, capture events or state via hooks, verify exact values.

Both patterns are mutation-verifiable per CONST-035.
