# Phase 5 Audit — B12 androidApp tests part 1
Audited: 2026-05-20  |  Files: 28  |  Bluff: 4  |  Suspect: 5  |  Clean: 19

## Findings

### androidApp/src/androidTest/kotlin/digital/vasic/yole/android/FirebaseIntegrationTests.kt
- **Verdict:** BLUFF
- **Methods reviewed:** 5
- **Unit(s) under test:** `androidApp/src/main/java/digital/vasic/yole/android/util/FirebaseUtil.kt`
- **Evidence:** Four of five test methods end with `assertTrue("...", true)` — a hard-coded tautology that passes regardless of whether Firebase actually initialised or the real code was replaced with a stub:
  - `firebaseCrashlyticsIsAvailable` (line 60): `assertTrue("Crashlytics should initialize without crash", true)`
  - `firebaseUtilInitializesWithoutCrash` (line 70): `assertTrue("FirebaseUtil should initialize without crash", true)`
  - `firebaseUtilLogsEventsWithoutCrash` (line 93): `assertTrue("All Firebase events should log without crash", true)`
  - `firebaseUtilRecordsNonFatalWithoutCrash` (line 110): `assertTrue("Non-fatal recording should not crash", true)`
  The fifth method `firebaseAnalyticsIsAvailable` (line 38) calls `assertNotNull` on the SDK instance (line 40), but the instanceId callback (line 42) runs asynchronously and the test does not await it, so the `assertNotNull("Firebase app instance ID should exist", instanceId)` at line 45 never executes before test exit.
- **Recommended fix:** Replace `assertTrue("...", true)` with the `testEventCapture` / `testNonFatalCapture` hooks already present in `FirebaseUtil.kt`. E.g., in `firebaseUtilLogsEventsWithoutCrash`, set `FirebaseUtil.testEventCapture = { name, _ -> capturedNames += name }` before calling `logEvent(...)` and assert `assertTrue(capturedNames.contains("file_saved"))`.

---

### androidApp/src/androidTest/kotlin/digital/vasic/yole/android/SaveTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 5
- **Unit(s) under test:** `FileHandle` (via `digital.vasic.yole.util` imports)
- **Evidence:** `saveToCacheAndReadBack` (line 47) writes content, reads back via `FileHandle.readBytes()`, and asserts `assertEquals(content, String(read!!))` at line 61 — a real byte-level round-trip that kills any stub returning hardcoded bytes. `writeAndReadRoundtrip` (line 116) exercises Unicode content with `assertEquals(original, String(read!!))` at line 130. `readNonExistentReturnsNull` (line 84) asserts `assertNull` on a missing file path — fails if `readBytes()` returns empty instead of null.
- **Recommended fix:** none.

---

### androidApp/src/androidTest/kotlin/digital/vasic/yole/android/test/YoleTestRunner.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 0 (infrastructure class, not a test)
- **Unit(s) under test:** N/A — custom `AndroidJUnitRunner` that pre-grants permissions before any test starts.
- **Evidence:** No test methods. The class is an essential harness prerequisite (header comment lines 7–14). Its correctness is structural — it either ships or it doesn't.
- **Recommended fix:** none.

---

### androidApp/src/androidTest/kotlin/digital/vasic/yole/android/TokenizerEngineAndroidTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 3
- **Unit(s) under test:** `digital.vasic.yole.syntax.TokenizerEngine`
- **Evidence:** `tokenizesMarkdownSnippetOnDevice` (line 75) asserts `tokens.size >= 5` (line 89), checks `first.scope.isNotBlank()` (line 93), verifies `first.startByte == 0` (line 100) and that some token's `endByte >= 15` (line 105). A stub returning an empty list fails the size check; a stub returning hardcoded unrelated tokens fails the byte-range check. `tokenizesReentrantOnSameEngine` (line 117) asserts `assertEquals(a.map { it.scope }, c.map { it.scope })` (line 128) verifying idempotency — a stub that appends state would fail.
- **Recommended fix:** none.

---

### androidApp/src/androidTest/kotlin/digital/vasic/yole/android/ui/EndToEndTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 9
- **Unit(s) under test:** `digital.vasic.yole.android.ui.YoleApp` (via MainActivity compose rule)
- **Evidence:** Each rewritten method (iter-41 through iter-46) exercises concrete user gesture sequences with post-action state assertions. `testCompleteTodoWorkflow` (line 41) asserts `onNodeWithText("Buy groceries").assertDoesNotExist()` (line 95) after delete — a real mutation verification. `testDataPersistenceAcrossSessions` (line 265) calls `activityRule.scenario.recreate()` (line 290) and asserts `onNodeWithText("Persistent todo").assertDoesNotExist()` (line 305) as an explicit regression guard. Filter-cycle tests verify all three label state transitions (lines 76–86).
- **Recommended fix:** none.

---

### androidApp/src/androidTest/kotlin/digital/vasic/yole/android/ui/IntegrationTest.kt
- **Verdict:** BLUFF
- **Methods reviewed:** 14
- **Unit(s) under test:** `FormatRegistry`, `ParserRegistry`, `digital.vasic.yole.android.ui.YoleApp` (via MainActivity)
- **Evidence:** `testValidationIntegration` (lines 386–403) calls `parser.validate(validContent)` at line 399 and stores the result in `errors` but NEVER asserts anything about `errors`. The comment on line 400 reads: "Valid content should have no errors, but some parsers might not implement validation — This is acceptable for this test." This is a CONST-035 no-assertion bluff: the test body would pass identically if `validate()` threw, returned null, returned 50 errors, or was a no-op stub. Replacing every line of the validate implementations with `return listOf(ValidationError("stub"))` would still pass this test. The other 13 methods are behaviorally sound.
- **Recommended fix:** Replace the discard of `errors` with: `assertTrue("Valid ${parser.supportedFormat.id} content should have 0 validation errors, got ${errors.size}", errors.isEmpty())`.

---

### androidApp/src/androidTest/kotlin/digital/vasic/yole/android/ui/YoleAppTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 21
- **Unit(s) under test:** `digital.vasic.yole.android.ui.YoleApp` (via MainActivity)
- **Evidence:** Five methods assert only the initial/idle state of other screens rather than exercising the named feature:
  1. `testPreviewScreenNavigation` (lines 313–318): Comment says "This would require setting up a file first, which is complex in UI tests. For now, just verify the basic navigation structure works." Only asserts `onNodeWithText("Files").assertIsDisplayed()` — the Files tab is always displayed; preview functionality is never exercised.
  2. `testSearchFunctionality` (lines 339–346): Navigates to Files and asserts `"File Browser"` is displayed — no search operation performed.
  3. `testSortFunctionality` (lines 349–356): Same pattern — asserts Files screen presence only.
  4. `testMarkdownActionButtons` (lines 367–372): Comment says "This would require navigating to editor with markdown file." Only asserts `onNodeWithText("Files").assertIsDisplayed()`.
  5. `testErrorHandling` (lines 549–557): Only asserts `"File Browser"` is displayed.
  All five pass if their named features are completely removed.
- **Recommended fix:** Either implement the actual gesture flows or annotate each with `@Ignore // SKIP-OK: #<ticket>` documenting the gap. Current state violates CONST-035 (named feature never exercised).

---

### androidApp/src/test/java/digital/vasic/yole/android/FileBrowserSaveFunctionalityTests.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 14
- **Unit(s) under test:** `digital.vasic.yole.android.ui.saveFile`, `loadFile`, `deleteFile` (via YoleAppKt reflection)
- **Evidence:** The majority of methods (`testSaveFileWithDirectAccess` line 44, `testLoadFileExists` line 113, `testDeleteFile` line 135) are genuine IO tests with content assertions. Two methods are structurally suspect:
  1. `testSaveFileFunctionSignature` (lines 244–253): Uses `Class.forName(...).getMethod(...)` reflection to assert that `saveFile` exists with a specific signature. This verifies the function exists but does NOT invoke it. A stub `saveFile` that always returns `Pair(false, "")` would still pass.
  2. `testCreateFileWithSAFFunctionSignature` (lines 256–267): Same pattern — asserts `createFileWithSAF` method exists by reflection without invoking it.
  Additionally, `testVersionCodeIsIncremented` (line 226) asserts `versionCode!! >= 7` — a floor that passes for any future versionCode, degrading to always-passing.
- **Recommended fix:** Replace the reflection-only signature checks with actual invocations that assert expected return values. Change `testVersionCodeIsIncremented` to an exact equality check against the current versionCode.

---

### androidApp/src/test/java/digital/vasic/yole/android/VersionConsistencyTests.kt
- **Verdict:** BLUFF
- **Methods reviewed:** 8
- **Unit(s) under test:** `androidApp/build.gradle.kts`, `desktopApp/build.gradle.kts`, `YoleApp.kt`, `EnhancedWebApp.kt`
- **Evidence:** The test constant `EXPECTED_VERSION = "2.0.0"` (line 25) and `EXPECTED_VERSION_CODE = 200` (line 26) are stale. The actual build file has `versionCode = 206` and `versionName = "2.0.6"` (confirmed at `androidApp/build.gradle.kts` lines 50–51). Therefore:
  - `testAndroidBuildGradleVersion` (line 49): asserts `content.contains("versionName = \"2.0.0\"")` (line 54) and `content.contains("versionCode = 200")` (line 56) — both will FAIL at runtime because the build file contains `"2.0.6"` and `206`.
  - `testDesktopBuildGradleVersion` (line 61): asserts `content.contains("packageVersion = \"2.0.0\"")` — will fail if the desktop build is also at 2.0.6.
  The test suite cannot coherently audit version consistency when its own expected constants are out of sync with the build. `testNoOldVersionReferencesInCode` (line 163) is a legitimate check and unaffected.
- **Recommended fix:** Update `EXPECTED_VERSION = "2.0.6"` and `EXPECTED_VERSION_CODE = 206`. Add a Gradle-generated constants file or a test that reads the version dynamically from `build.gradle.kts` to prevent future drift.

---

### androidApp/src/test/kotlin/digital/vasic/yole/android/firebase/FirebaseUtilHookTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 9
- **Unit(s) under test:** `androidApp/src/main/java/digital/vasic/yole/android/util/FirebaseUtil.kt`
- **Evidence:** `logEvent_invokesCaptureHook_withEventNameAndParams` (line 44) installs a capture lambda, calls `logEvent(...)`, then asserts `assertEquals(1, captured.size)` (line 57), `assertEquals("file_saved", name)` (line 59), and `assertEquals("markdown", params[...])` (line 60). A stub `logEvent` ignoring the capture lambda leaves `captured` empty and the size assertion fails. `recordNonFatal_invokesCaptureHook_withThrowableAndContext` (line 81) asserts `assertSame(ex, t)` (line 91) — identity comparison kills any stub. `remoteConfigDefaults_returnedWhenConfigNotInitialized` (line 146) asserts specific defaults including `assertEquals(42L, ...)` (line 152).
- **Recommended fix:** none.

---

### androidApp/src/test/kotlin/digital/vasic/yole/android/IterB59VariantConfigTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 6
- **Unit(s) under test:** `androidApp/build.gradle.kts`, `androidApp/src/debug/res/values/colors.xml`, `androidApp/src/debug/res/mipmap-anydpi-v26/ic_launcher.xml`, `androidApp/google-services.json`
- **Evidence:** `debugVariantHasApplicationIdSuffixDotDev` (line 35) asserts the exact string `applicationIdSuffix = ".dev"` — removing it fails immediately. `debugLauncherIconHasGreenBackground` (line 61) asserts `#FF00FF00` or `#00FF00` (line 69). `googleServicesJsonRegistersDevPackage` (line 102) asserts both package name strings at lines 110 and 114. These are genuine structural guards that kill the named mutations.
- **Recommended fix:** none.

---

### androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/AccessibilityRobolectricTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 5
- **Unit(s) under test:** `digital.vasic.yole.android.ui.YoleApp` (via MainActivity)
- **Evidence:** `addButtonHasContentDescription` (line 37) asserts `onNodeWithContentDescription("Add").assertIsDisplayed()` — removing the FAB's `contentDescription = "Add"` (YoleApp.kt line 933) would fail with NoSuchElement. `allInteractiveElementsClickable` (line 53) performs real clicks on all four navigation items and waits for idle.
- **Recommended fix:** none.

---

### androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/AppLaunchRobolectricTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 5
- **Unit(s) under test:** `digital.vasic.yole.android.ui.YoleApp` (via MainActivity)
- **Evidence:** `appSurvivesActivityRecreation` (line 49) calls `activityRule.scenario.recreate()` and then asserts `onAllNodesWithText("Files").onFirst().assertIsDisplayed()` — a real lifecycle probe. `appInitializesThemeWithoutCrash` (line 42) specifically tests the Theme.kt fix by waiting for idle then asserting the Files tab is displayed.
- **Recommended fix:** none.

---

### androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/BackupRestoreRobolectricTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 3
- **Unit(s) under test:** `digital.vasic.yole.android.ui.YoleApp` (via MainActivity — More screen)
- **Evidence:** `backupRestoreUIAccessible` (line 28) navigates to More and asserts `"Backup & Restore"` exists — confirms the entry point is rendered. However `moreScreenNavigable` (line 34) and `moreScreenRetainsStateOnRevisit` (line 42) both end with `onAllNodesWithText("More").onFirst().assertIsDisplayed()`. The "More" tab is always rendered in the bottom navigation regardless of current screen (it's a nav bar item), so asserting its presence after navigating away and back is a tautology. If MoreScreen's body content were completely broken, these tests would still pass.
- **Recommended fix:** Replace the final assertions in `moreScreenNavigable` and `moreScreenRetainsStateOnRevisit` with `onNodeWithText("More Options").assertIsDisplayed()` — the More screen content header that only renders when the More screen is active.

---

### androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/BracketAutoCompleterRobolectricTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 3
- **Unit(s) under test:** `digital.vasic.yole.android.ui.editor.applyBracketAutocomplete`
- **Evidence:** `insertsCloserForOpener` (line 38) asserts `assertEquals("()", result.text)` (line 43) and cursor position (line 44) — a stub returning `new` unchanged produces `result.text == "("` and fails both. `noOpOnPaste` (line 67) asserts `assertEquals("(hello", result.text)` (line 73) — a stub that always inserts a closer produces `"(hello)"` and fails.
- **Recommended fix:** none.

---

### androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/codeaction/CodeActionLightbulbRobolectricTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 4
- **Unit(s) under test:** `CodeActionLightbulb.kt`, `CodeActionMenu.kt`, `CodeActionInvoker.kt`, `SyncedScrollEditor.kt`
- **Evidence:** All four tests are source-level structural assertions (grep-based). The mutation guards described in the class header are valid, but the `contains()` checks test for string presence in source, not runtime behavior. If `CodeActionLightbulb.kt` retained the strings `"code-action-lightbulb"` and `"lightbulb-line-$lineNum"` as dead code (e.g. in a commented-out block that was stripped by `stripComments`) but removed the actual rendering logic, `lightbulb_visible_when_actions_present` (line 94) could still pass. The test class does strip block comments (via loadSource), but line-comments and string literals in non-executed branches would survive. These are solid structural scaffolds but not behavioral proofs.
- **Recommended fix:** Add at least one runtime assertion using `createComposeRule` with a real `actionsByLine` map to assert `onNodeWithTag("lightbulb-line-0").assertIsDisplayed()`. Until then, annotate the source-only methods with `// ANTI-BLUFF-EXEMPT: structural-scaffold-only` per project convention.

---

### androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/CommentToggleActionRobolectricTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 4
- **Unit(s) under test:** `digital.vasic.yole.android.ui.editor.toggleCommentOnSelectedLines`
- **Evidence:** `togglesCommentWhenLanguageHasLineComment` (line 42) asserts `assertEquals("// fun foo()", after.text)` (line 48) — a stub returning input unchanged produces `"fun foo()"` and fails. `togglesCommentBackWhenAlreadyCommented` (line 58) asserts `assertEquals("fun foo", after.text)` — a stub without an un-comment branch produces `"// // fun foo"` and fails. `togglesEveryLineInSelection` (line 85) asserts `after.text == "// fun a()\n// fun b()"` (line 92) — a stub toggling only the first line produces `"// fun a()\nfun b()"` and fails.
- **Recommended fix:** none.

---

### androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/CompletionExplicitTriggerRobolectricTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 6
- **Unit(s) under test:** `digital.vasic.yole.completion.trigger.CompletionTrigger`, `SyncedScrollEditor.kt`, `YoleApp.kt`
- **Evidence:** `explicitTriggerEmitsShowEvenOnEmptyText` (line 87) collects events from a real `CompletionTrigger` instance with `debounceMillis = 0`, calls `trigger.onExplicitTrigger()`, and uses `withTimeout(500L)` (line 105) to await a `TriggerEvent.Show`. A stub routing through the debounce path for empty text would not emit within the timeout. The structural tests (`ctrlSpaceHandlerWiredInEditor`, `ctrlSpaceCallsOnExplicitTrigger`) are load-bearing anchors backed by the runtime test.
- **Recommended fix:** none.

---

### androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/CompletionPopupRobolectricTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 9
- **Unit(s) under test:** `digital.vasic.yole.android.ui.editor.CompletionPopupState`, `CompletionPopup.kt`, `CompletionToolbarButton.kt`, `SyncedScrollEditor.kt`
- **Evidence:** `popupOpensOnShow` (line 89) asserts `assertTrue("isOpen after show()", state.isOpen)` (line 93) and `assertEquals(1, state.items.size)` (line 94) against a real `CompletionPopupState` — a stub `show()` would fail both. `selectionMoves` (line 133) verifies wrap-around: `assertEquals(0, state.selectedIndex)` (line 143) after three moves on a 3-item list — any stub that ignores direction fails. `showWithEmptyListIsNoOp` (line 152) asserts `assertFalse("isOpen must stay false", state.isOpen)` — a stub unconditionally opening fails.
- **Recommended fix:** none.

---

### androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/DefinitionLocationChooserRobolectricTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 5
- **Unit(s) under test:** `androidApp/src/main/java/digital/vasic/yole/android/ui/editor/navigation/DefinitionLocationChooser.kt`
- **Evidence:** Structural assertions with tight behavioral coupling. `rowDerivesFilenameFromUri` (line 169) asserts `src.contains("substringAfterLast('/')")` — removing this call means URIs display as full paths. `chooserHasCancelRow` (line 148) asserts both the `"Cancel"` string AND `"clickable(onClick = onDismiss)"` — both must be present together. `chooserHasExperimentalAnnotation` (line 177) uses `assertFalse(...contains("@Suppress") && ...contains("ExperimentalMaterial3Api"))` (line 189) as a negative guard.
- **Recommended fix:** none.

---

### androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/diagnostics/DiagnosticsGutterTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 5
- **Unit(s) under test:** `digital.vasic.yole.android.ui.editor.diagnostics.offsetToLine`, `DiagnosticsGutter.kt`
- **Evidence:** `offsetToLine_countsNewlines` (line 69) is a pure-function test: asserts `assertEquals(1, offsetToLine(text, 6))` (line 72) and `assertEquals(2, offsetToLine(text, 12))` (line 74). A stub always returning 0 fails both. `offsetToLine_firstLine` (line 58) asserts two specific values — a stub returning `input.count('\n')` for any offset fails offset 4 on the first line.
- **Recommended fix:** none.

---

### androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/diagnostics/DiagnosticsInlineUnderlineTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 4
- **Unit(s) under test:** `androidApp/src/main/java/digital/vasic/yole/android/ui/editor/diagnostics/DiagnosticsInlineUnderline.kt`
- **Evidence:** `outOfBoundsRange_clamped` (line 117) asserts three distinct structural guards: `coerceIn(0, text.length)` (line 120), `coerceIn(start, text.length)` (line 124), and `if (start < end)` (line 128). `emptyDiagnostics_identityTransform` (line 59) asserts the specific pattern `TransformedText(text, OffsetMapping.Identity)` at line 65 — a stub returning a different identity form fails.
- **Recommended fix:** none.

---

### androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/diagnostics/DiagnosticsPaletteTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 5
- **Unit(s) under test:** `digital.vasic.yole.android.ui.editor.diagnostics.severityVisuals`
- **Evidence:** `warningColorIsDifferentFromError` (line 62) calls real `severityVisuals(Severity.Error, ...)` and `severityVisuals(Severity.Warning, ...)` and asserts `assertFalse(..., error.color == warning.color)` (line 68) — a stub always returning red fails. `darkModeColorsAreDifferentFromLightMode` (line 122) asserts light and dark Error colors differ — a stub ignoring `isDark` fails.
- **Recommended fix:** none.

---

### androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/diagnostics/DiagnosticsProblemsPanelTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 5
- **Unit(s) under test:** `androidApp/src/main/java/digital/vasic/yole/android/ui/editor/diagnostics/DiagnosticsProblemsPanel.kt`
- **Evidence:** `rowMessageIncludesLineNumber` (line 127) asserts `src.contains("line + 1")` (line 130) AND `src.contains("offsetToLine(")` (line 134) — both must coexist for the 1-based line number contract. `diagnosticsAreSortedByOffset` (line 96) asserts `src.contains("sortedBy { it.range.first }")` — changing to `sortedByDescending` fails.
- **Recommended fix:** none.

---

### androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/EditorHighlightingRobolectricTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 6
- **Unit(s) under test:** `SyncedScrollEditor.kt`, `YoleApp.kt`, `digital.vasic.yole.syntax.render.AnnotatedStringBuilder`
- **Evidence:** `annotatedStringBuilderProducesSpansForKnownTokenAndTheme` (line 244) is a runtime test: parses real theme JSON, creates a `Token` with `scope = "heading"`, calls `AnnotatedStringBuilder.build(text, tokens, theme)` and asserts `annotated.spanStyles.isNotEmpty()` (line 265). A stub builder returning `AnnotatedString(text)` with no spans fails this. `ideEditorScreenPassesHighlighterAndLangIdToSyncedScrollEditor` (line 176) applies a negative regex assertion `!Regex("""highlighter\s*=\s*null""").containsMatchIn(ideEditorBlock)` (line 206) as an explicit anti-bluff negative guard.
- **Recommended fix:** none.

---

### androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/EditorScrollSyncRobolectricTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 4
- **Unit(s) under test:** `SyncedScrollEditor.kt`
- **Evidence:** `syncedScrollEditorDeclaresExactlyOneRememberScrollState` (line 70) strips comments then uses `Regex(...).findAll(codeOnly).count()` and asserts `assertEquals(1, count)` (line 80) — a regression to two instances changes the count to 2 and fails. `scrollStateIsObservedIdenticallyByMultipleConsumers` (line 117) is a runtime test: calls `shared.scrollTo(150)` and asserts `assertEquals(150, consumerA.value)` and `assertEquals(consumerA.value, consumerB.value)` (line 125) — any implementation creating two separate states fails.
- **Recommended fix:** none.

---

### androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/FileBrowserDedupRobolectricTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 5
- **Unit(s) under test:** `YoleApp.kt` (SubScreen enum, MoreScreen function, FilesScreen function)
- **Evidence:** `subScreenEnumHasNoFileBrowserEntry` (line 58) extracts the SubScreen enum body via regex and asserts `assertFalse(..., enumBlock.contains("FILE_BROWSER"))` — re-adding `FILE_BROWSER` fails immediately. `noSourceReferencesToOnFileBrowserClick` (line 124) asserts `assertEquals(0, count)` — any re-introduction fails. `filesScreenStillExists` (line 139) guards against over-deletion.
- **Recommended fix:** none.

---

### androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/FileEditingRobolectricTest.kt
- **Verdict:** BLUFF
- **Methods reviewed:** 6
- **Unit(s) under test:** `YoleApp.kt` (FAB → New Doc dialog → Create flow, IdeEditorScreen)
- **Evidence:** Two methods contain post-action assertions that are either absent or trivially tautological:
  1. `saveFile` (line 69): Calls `performClick()` on the `"Save file"` content description (confirmed present at YoleApp.kt line 1607) and then `waitForIdle()` with NO post-save assertion. The comment "save (this may not work in test environment due to file system)" is a self-admission that the test intentionally omits the critical assertion. A stub `onSaveClick` handler that is a complete no-op would make this test pass — matching the CONST-035 BLUFF-K-002 pattern.
  2. `switchToPreviewMode` (line 49): Calls Preview button click and `waitForIdle()` but asserts nothing about the preview state. A stub `onPreviewClick` that is a no-op would pass.
  The other four methods (`createNewFile`, `editFileContent`, `switchBackToEditMode`, `navigateBackFromEditor`) have real behavioral assertions or at least verify that widgets appear/disappear.
- **Recommended fix:** `saveFile`: After clicking Save, assert the autosave file exists: `assertTrue(File(context.filesDir, "autosave/untitled.md").exists())` or assert via `onNodeWithText("Saved").assertIsDisplayed()` if the UI shows a confirmation snackbar. `switchToPreviewMode`: After clicking Preview, assert `onNodeWithContentDescription("Code editor for untitled.md").assertDoesNotExist()` (editor hidden in preview mode).
