# Phase 5 Audit — Batch B14: desktopApp + iosTest + wasmJsTest + webApp

**Files: 26 | Bluff: 5 | Suspect: 11 | Clean: 10**

Scope:
- `desktopApp/src/test/kotlin/digital/vasic/yole/desktop/` (13 files)
- `desktopApp/src/test/kotlin/digital/vasic/yole/desktop/ui/` (3 files)
- `shared/src/iosTest/kotlin/digital/vasic/yole/ios/` (1 file)
- `shared/src/wasmJsTest/kotlin/digital/vasic/yole/` (5 files)
- `webApp/src/wasmJsTest/kotlin/digital/vasic/yole/web/` (4 files)

Auditor note: every file below was fully read. Prior shallow verdicts ("by pattern", "not read") have been replaced with line-cited evidence from actual file content.

---

## desktopApp Tests

### AllFormatsAutomationTest.kt

**Verdict:** SUSPECT
**Methods reviewed:** 12 (962 lines fully read)
**Unit(s) under test:** `EditorScreen`, `PreviewScreen`, `FormatRegistry.detectByFilename()`, `ParserRegistry.getParser()`
**Evidence:**
- `testAllFormatsBasicRendering` (L97–133): calls real `runComposeUiTest`, renders `EditorScreen` with real content per format, captures screenshot, validates non-black pixel count at L111–114 — real behavioral.
- `testAllFormatsParserRegistryIntegration` (L553–590): calls real `FormatRegistry.detectByFilename()`, `ParserRegistry.getParser(detected)`, `parser.parse(format.content)`, asserts `document.rawContent == format.content` at L583–585 — strongly mutation-detectable.
- `testAllFormatPreviewRendering` (L137–222): calls `onNodeWithText(format.expectedPreviewContains, substring=true).assertExists()` — content-specific assertion per format; bluff-resistant.
- `testAllFormatsLightTheme` (L395–413) and `testAllFormatsDarkTheme` (L419–436): **SUSPECT** — only assert `onNodeWithText("Editing: ${format.filename}").assertExists()`. No format-specific content check; a stub `EditorScreen` that shows "Editing: " + any filename passes trivially. Theme-variant tests verify filename display, not theme application.
**Recommended fix:** Add format-specific content assertions to theme tests (same `expectedPreviewContains` assertions used in `testAllFormatPreviewRendering`). Theme toggling should additionally assert a theme-specific style attribute or color token is applied.

---

### DesktopAppIntegrationTest.kt

**Verdict:** SUSPECT
**Methods reviewed:** 12 (542 lines fully read)
**Unit(s) under test:** Claimed: `DesktopApplication`, `DesktopFileManager`, `DesktopSettingsManager`. Actual: inner mock classes defined at L430–541.
**Evidence:**
- `should complete full markdown document workflow` (L76–141): inner `DesktopFileManager.loadFile()` returns hardcoded content; `ParserRegistry.getParser("markdown").parse(htmlContent)` call is real, `assertThat(htmlContent).contains("<h1>")` and `assertThat(htmlContent).contains("<em>")` (L122–126) are real behavioral assertions — not bluff for the parser portion.
- `should persist settings across application sessions` (L220–236): creates `new DesktopSettingsManager()` but `DesktopSettingsManager` (L481–541) stores values in a `companion object { val sharedSettings = mutableMapOf<String,String>() }` static map — this is NOT disk persistence. `DesktopSettingsManager` in this file is an inner mock class, not the production class. The test asserting "persistence across sessions" passes trivially because the static map survives within the same JVM process.
- `should handle concurrent document access` (L250–300): launches 5 coroutines reading from the same `DesktopFileManager` instance (inner mock); the mock's `loadFile()` never touches the real file system — this is a concurrency test on a stub.
- Inner `DesktopFileManager` at L430–474: overrides production behavior by delegating only to the inner class. Production `digital.vasic.yole.desktop.file.DesktopFileManager` is NOT imported or used.
**Recommended fix:** Remove the inner mock classes. Import and use `digital.vasic.yole.desktop.file.DesktopFileManager` and the real `YoleDesktopSettings` with a real temp directory for persistence tests. The persistence test must write to disk and reload from a fresh instance.

---

### DesktopAppParserTest.kt

**Verdict:** BLUFF
**Methods reviewed:** 18 (394 lines fully read)
**Unit(s) under test:** Claimed: desktop parser integration. Actual: self-referential string assertions.
**Evidence:**
- `should detect markdown format by extension` (L29–38): calls real `FormatRegistry.detectByFilename("test.md")` — correct. Then asserts only `assertNotNull(format.name)` — does NOT assert the format ID or name equals "markdown". Any format with a non-null name passes.
- `should parse markdown content correctly` (L41–60): builds a multi-line string `content`, calls `ParserRegistry.getParser("markdown").parse(content)` — real parse. Then asserts `assertTrue(content.contains("# Header 1"))` at L58 — THIS IS THE BLUFF: `content` is the string the test BUILT. The assertion verifies the test's own input, not the parser's output. Production parser output is never checked.
- `should extract markdown headers` (L62–78): same pattern. `assertTrue(content.contains("# Main Header"))` — asserts own input.
- `should handle large file content efficiently` (L317–337): builds a 10 000-line string, asserts `assertTrue(content.contains("Line 1"))` and `assertTrue(content.contains("Line 10000"))` — asserts own input strings. No parser output checked.
- `should parse formatted markdown content` (L79–108): builds string, calls parse, then asserts `assertTrue(content.contains("**bold**"))` and `assertTrue(content.contains("*italic*"))` at L106–108 — again asserts own Markdown input rather than the HTML output.
- Mutation test: replace `ParserRegistry.getParser("markdown").parse(content)` with `fun parse(x:String) = ParsedDocument("", "", mapOf(), listOf())` — every test still passes because all assertions are on `content`, not the parsed result.
**Recommended fix:** Assert on `parsedDocument.toHtml()` or `parsedDocument.parsedContent`. E.g., `assertTrue(htmlOutput.contains("<h1>"))`, `assertTrue(htmlOutput.contains("<strong>"))`. The format detection test should assert `assertEquals(TextFormat.ID_MARKDOWN, format.id)`.

---

### DesktopAppSettingsTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 7 (205 lines fully read)
**Unit(s) under test:** `YoleDesktopSettings` (production class)
**Evidence:**
- `should save and retrieve theme setting` (L37–44): `settings.setThemeMode("light"); assertEquals("light", settings.getThemeMode())` — real round-trip set/get.
- `should save and retrieve font size setting` (L47–58): `settings.setFontSize(18); assertEquals(18, settings.getFontSize())` — real round-trip.
- `should save and retrieve line numbers setting` (L61–72): boolean round-trip set/get.
- `should save and retrieve auto-save interval` (L75–88): integer round-trip, includes boundary value `0` and `3600`.
- `should return default values when settings not configured` (L91–111): fresh instance, verifies defaults are within valid ranges.
- `should persist multiple settings independently` (L114–138): sets 4 settings, reads back all 4 and asserts independence.
- `should handle all valid theme mode values` (L141–162): iterates `listOf("system","light","dark")`, sets each, reads back, `assertEquals` — not tautological because it asserts the exact value set was retrieved.
- All 7 tests exercise the real `YoleDesktopSettings` production class via setter/getter round-trips. Mutation (removing the setter body) would cause the getter to return the default, failing `assertEquals`.

---

### DesktopAppUITest.kt

**Verdict:** BLUFF
**Methods reviewed:** 15 (513 lines fully read)
**Unit(s) under test:** Claimed: `YoleApp`, `MainScreen`. Actual: `TestStubs.kt` stub composables.
**Evidence:**
- Import at L14: `import digital.vasic.yole.desktop.ui.*` — this resolves to `TestStubs.kt` which provides stub `SettingsScreen`, `FileBrowserScreen`, and `EditorScreen` in the same package. Production composables are shadowed.
- `should handle file selection callback` (L205–224): `var fileSelected = false; FileBrowserScreen(onFileSelected = { fileSelected = true }); onNodeWithText("test.md").performClick(); assertTrue(fileSelected)` — the stub `FileBrowserScreen` at TestStubs.kt L103–127 hardcodes a `TextButton("test.md")` that calls `onFileSelected("test.md","")`. The test defines the lambda, the stub calls it, the test asserts its own lambda was called. No production `FileBrowserScreen` is involved.
- `should handle content change callback` (L253–278): `var contentChanged = false; EditorScreen(onContentChange = { contentChanged = true }); ...performTextInput...; assert(contentChanged)` — stub `EditorScreen` at TestStubs.kt L132–178 calls `onContentChange(newContent)` from its own `TextField`'s `onValueChange`. The test verifies the stub's callback plumbing, not production editor behavior.
- `should display file browser with correct items` (L283–308): asserts `onNodeWithText("test.md").assertExists()` — the stub hardcodes "test.md" as a `TextButton` label. This assertion would pass regardless of what real files are on disk.
- Mutation test: replace production `YoleApp.kt`, `MainScreen.kt`, `FileBrowserScreen.kt`, `EditorScreen.kt` entirely with empty files — all 15 tests still pass because `TestStubs.kt` is what gets compiled into the test classpath.
**Recommended fix:** Delete `TestStubs.kt` or move it out of the production package namespace. Wire `DesktopAppUITest` to import and use the actual production composables.

---

### DesktopFileManagerTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 16 (418 lines fully read)
**Unit(s) under test:** `digital.vasic.yole.desktop.file.DesktopFileManager` (production class)
**Evidence:**
- `should save file with content successfully` (L53–62): saves to real `testFile`, reads back with `assertThat(testFile.readText()).isEqualTo(testContent)` — real file I/O.
- `should create backup before overwriting file` (L101–115): creates real file, saves again, asserts backup file exists on disk with real `assertTrue(backupFile.exists())`.
- `should prevent directory traversal attacks` (L223–228): calls production `saveFile(File(tempDir, "../../../etc/passwd"), ...)`, asserts return value is `false` — mutation-detectable: production must explicitly reject traversal paths.
- `should detect format from content when extension is ambiguous` (L287–302): calls production `FormatRegistry.detectByContent(xmlContent)` and asserts `assertEquals(TextFormat.ID_XML, detectedFormat.id)` — real registry call.
- `should handle unicode content correctly` (L345–358): writes Emoji + CJK characters to real file, reads back, asserts `assertEquals` — real I/O encoding test.
- All tests use `import digital.vasic.yole.desktop.file.DesktopFileManager` (L6) — confirmed production class.

---

### DesktopFileOperationsTest.kt

**Verdict:** SUSPECT
**Methods reviewed:** 15 (484 lines fully read)
**Unit(s) under test:** Claimed: desktop file operations. Actual: inner mock `DesktopFileManager` defined at L383–471 within the test file.
**Evidence:**
- The inner `DesktopFileManager` class at L383–471 is NOT `digital.vasic.yole.desktop.file.DesktopFileManager`. It is a locally-defined class with the same name inside the test file.
- Inner mock's `isValidFilePath` at L464–469: `return file.canonicalPath.startsWith(tempDir.canonicalPath)` — only allows files under `tempDir`. This makes the directory-traversal test at L220–230 pass trivially: `assertFalse(manager.isValidFilePath(File("/etc/passwd")))` is guaranteed by the mock's own implementation, not by production security logic.
- `should handle concurrent file access` (L300–340): launches concurrent operations on the inner mock — concurrency behavior of the mock, not production code.
- `should process multiple file formats` (L250–290): calls inner mock's `detectFormat()` which delegates to production `FormatRegistry.detectByFilename()` at L427 — this one call into production is real, but the surrounding file I/O is mock.
- The production `digital.vasic.yole.desktop.file.DesktopFileManager` import does NOT appear in this file.
**Recommended fix:** Remove the inner `DesktopFileManager` class and import `digital.vasic.yole.desktop.file.DesktopFileManager`. Use `tempDir` as the file root for isolation, not for blocking traversal detection.

---

### DesktopKeyboardShortcutsTest.kt

**Verdict:** SUSPECT
**Methods reviewed:** 12 (308 lines fully read)
**Unit(s) under test:** `DesktopKeyboardShortcuts` (production class)
**Evidence:**
- `should have correct new file shortcut` (L44–61): `assertEquals(Key.N, newFileShortcut.key)`, `assertTrue(newFileShortcut.ctrl)`, `assertFalse(newFileShortcut.shift)` — real behavioral assertions on exact key values.
- `should register custom shortcut` (L99–115): registers custom shortcut via `registerShortcut()`, retrieves via `getShortcut()`, asserts exact values.
- `resetShortcut restores default` (L139–147): **SUSPECT** — `val defaultShortcut = shortcuts.getShortcut(ShortcutAction.NEW_FILE)`, then `shortcuts.resetShortcut(ShortcutAction.NEW_FILE)`, then `val retrievedShortcut = shortcuts.getShortcut(ShortcutAction.NEW_FILE)`, then `assertEquals(defaultShortcut, retrievedShortcut)`. Both `defaultShortcut` and `retrievedShortcut` are fetched from `getShortcut()` — if `resetShortcut` does nothing, `getShortcut` returns the same unchanged value before and after. The test proves nothing about reset behavior.
- `should not have duplicate shortcuts` (L288–296): collects all shortcut values, checks `assertEquals(shortcuts.size, shortcuts.toSet().size)`. This passes trivially for the default factory values and does not verify that attempting to register a duplicate is rejected.
**Recommended fix:** For `resetShortcut` test: register a custom shortcut FIRST, then reset, then assert the value matches the pre-registered default (not the current value). For duplicate test: attempt to register a duplicate and assert it throws or is rejected.

---

### DesktopWindowManagerTest.kt

**Verdict:** SUSPECT
**Methods reviewed:** 11 (337 lines fully read)
**Unit(s) under test:** `DesktopWindowManager` (production class)
**Evidence:**
- `should create window with unique title` (L44–60): `assertThat(window.title).isEqualTo("Untitled 1")` — real behavioral assertion.
- `should update window content correctly` (L99–113): updates content, asserts `assertThat(window.content).isEqualTo(newContent)` — real setter/getter.
- `saveAndLoadWindowState` (L226–249): **SUSPECT** — calls `saveWindowState()`, `closeWindow()`, `loadWindowState()`, but has NO assertion on the loaded state. Comment reads: `// This test assumes window state persistence is implemented`. The method body terminates without any assertion. The test always passes regardless of whether `loadWindowState()` restores the state or is a no-op.
- `should handle window focus correctly` (L260–278): calls `focusWindow()` and `unfocusWindow()`, asserts `assertFalse(window.isFocused)` after unfocus — real behavioral, but only verifies final state, not transition.
**Recommended fix:** For `saveAndLoadWindowState`: after `loadWindowState()`, assert that the loaded window properties (title, bounds, content) match the values present before `closeWindow()`.

---

### FullUIAutomationTest.kt

**Verdict:** SUSPECT
**Methods reviewed:** 19 (1071 lines fully read)
**Unit(s) under test:** `EditorScreen`, `SettingsScreen`, `FileBrowserScreen` Compose composables (from `TestStubs.kt` — same stub import issue as `DesktopAppUITest`)
**Evidence:**
- `phaseFileBrowserNavigation` (L152–221): clicks `FileBrowserScreen` nodes, asserts `onNodeWithText("Files").assertExists()`. Because `TestStubs.kt`'s `FileBrowserScreen` hardcodes the "Files" header, this always passes.
- `phaseThemeSwitching` (L292–333): calls `onAllNodes(isSelectable())[1].performClick()`, then `assertEquals("light", currentTheme.value)` — this is a real callback equality check via a captured `MutableState`. If the stub's radio buttons are wired to the callback, this is a real assertion. However it verifies stub wiring, not production `SettingsScreen` behavior.
- `testFileBrowserStandalone` (L644–673): `assertEquals("sample.md", selectedFile)` and `assertTrue(selectedContent.contains("Sample Markdown"))`. Stub `FileBrowserScreen` calls `onFileSelected("sample.md", "Sample Markdown content")` when the "sample.md" button is clicked (TestStubs.kt L113). The test verifies the stub's hardcoded callback payload.
- `testSettingsToggles` (L811–836): asserts initial state `assertTrue(showLineNumbers)`, `assertTrue(showAutoComplete)` etc. — **NEVER performs any toggle**. There is no `performClick()` in this method. The test only verifies the initial state of the MutableState variables the test itself initializes.
- Import L13: `import digital.vasic.yole.desktop.ui.*` — confirmed stub import.
**Recommended fix:** Wire all UI automation tests to production composables. For `testSettingsToggles`: add `performClick()` on each toggle and assert the state after toggling.

---

### TabNavigationTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 8 (365 lines fully read)
**Unit(s) under test:** Desktop tab navigation composables (production)
**Evidence:**
- Import at L11: `import digital.vasic.yole.desktop.ui.navigation.*` — NOT the stub package. Production navigation composables are used.
- `navigateAllTabsForwardAndBack` (L57–192): iterates over 7 tabs, clicks the nav button for each, asserts `onNodeWithText(expectedScreenContent).assertExists()` per tab, takes screenshot and validates `nonBlackPixelRatio > 0.01f` (L116–120). 14 real screenshots verified.
- `rapidTabSwitching20Times` (L197–260): 20-iteration loop clicking forward/back with content assertion on each transition — real navigation stress test.
- `verifyTabStatePreservation` (L265–312): navigates to Editor, enters text, switches to Settings, switches back to Editor, asserts entered text is still present via `onNodeWithText(testInput).assertExists()`. Real state preservation check.
- All screenshot captures include pixel density validation; a blank/black screen fails the threshold check.

---

### ui/import_/DesktopImportDragDropTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 8 (122 lines fully read)
**Unit(s) under test:** `resolveFirstDroppedFile()` (production pure function)
**Evidence:**
- `resolveFirstDroppedFile returns file for valid local URI` (L48–56): creates real temp file, calls production function with `file.toURI().toString()`, asserts `assertEquals(file.canonicalPath, result.canonicalPath)` — real I/O.
- `resolveFirstDroppedFile reads correct bytes from resolved file` (L71–81): writes specific content to temp file, resolves, reads bytes back, `assertEquals(content, bytes.toString(Charsets.UTF_8))` — real byte-level verification.
- `resolveFirstDroppedFile returns null for directory URI` (L99–103): creates real directory, passes its URI, asserts null — real rejection logic.
- `resolveFirstDroppedFile returns null for malformed URI string` (L107–109): `assertNull(resolveFirstDroppedFile(listOf("not a valid uri :// bad")))` — real exception handling.
- All 8 tests create real temp files on the JVM file system. Mutation (stubbing `resolveFirstDroppedFile` to always return null) fails 3 happy-path tests.

---

### ui/TestStubs.kt

**Verdict:** BLUFF (infrastructure artifact enabling bluff in other tests)
**Methods reviewed:** full file (178 lines)
**Unit(s) under test:** N/A — this file IS the stub
**Evidence:**
- `SettingsScreen` stub (L23–98): in-memory `MutableState` only; no production settings API called. Shows checkboxes that toggle local state.
- `FileBrowserScreen` stub (L103–127): hardcodes `TextButton("test.md")` and `TextButton("sample.md")` — never reads the real file system. `onFileSelected("test.md", "")` and `onFileSelected("sample.md", "Sample Markdown content")` are hardcoded payloads.
- `EditorScreen` stub (L132–178): accepts only `(content: String, onContentChange: (String) -> Unit)` — different signature from the production `EditorScreen`. Tests calling this stub receive different behavior than production.
- This file is in package `digital.vasic.yole.desktop.ui` — the same package as the production composables. Any test file importing `digital.vasic.yole.desktop.ui.*` will compile against these stubs when the test classpath is resolved. This is the root bluff enabler for `DesktopAppUITest.kt` and `FullUIAutomationTest.kt`.
**Recommended fix:** Move stubs to a dedicated `digital.vasic.yole.desktop.ui.testfixtures` package. Never place test stubs in the same package as production composables.

---

### ui/YoleDesktopSettingsTest.kt

**Verdict:** SUSPECT
**Methods reviewed:** 10 (232 lines fully read)
**Unit(s) under test:** `YoleDesktopSettings` (production class)
**Evidence:**
- `should set and get light theme mode` (L45–49): `settings.setThemeMode("light"); assertEquals("light", settings.getThemeMode())` — real round-trip, mutation-detectable.
- `should set and get dark theme mode` (L51–55): same — real.
- `should return default theme mode` (L37–42): `val themeMode = settings.getThemeMode(); assertTrue(themeMode in listOf("system", "light", "dark"))` — **SUSPECT / TAUTOLOGICAL**: any value from the 3-element list passes. If the production method were stubbed to return `"system"` unconditionally, this test would pass. The assertion does not verify the actual default; it only verifies the returned string is one of three valid values.
- `should return default auto-save setting` (L116–122): `val autoSave = settings.isAutoSaveEnabled(); assertTrue(autoSave is Boolean)` — **TAUTOLOGICAL**: `isAutoSaveEnabled()` returns a `Boolean`; `autoSave is Boolean` is always `true` in Kotlin. This assertion is equivalent to `assertTrue(true)`.
- `should return default font size` (L155–162): `assertTrue(fontSize in 8..72)` — range check; passes with any font size from 8 to 72, which covers effectively all real defaults.
**Recommended fix:** Replace `assertTrue(themeMode in listOf(...))` with `assertEquals("system", themeMode)` (assert the specific expected default). Replace `assertTrue(autoSave is Boolean)` with `assertFalse(autoSave)` or `assertTrue(autoSave)` (assert the specific expected default).

---

### ui/YoleDesktopUITest.kt

**Verdict:** SUSPECT
**Methods reviewed:** 14 (432 lines fully read)
**Unit(s) under test:** `FormatRegistry`, `StyleSheets`, `ThemeUtils`, `Screen` enum, `DesktopKeyboardShortcuts`, `YoleDesktopSettings`
**Evidence:**
- `FormatRegistry should detect Markdown by content` (L95–99): `val detected = FormatRegistry.detectByContent("# Heading\nThis is markdown\n[link](url)"); assertEquals(TextFormat.ID_MARKDOWN, detected.id)` — real detection with specific ID assertion. Mutation-detectable.
- `StyleSheets cache should grow as formats are accessed` (L347–360): `StyleSheets.clearCache(); StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, ThemeMode.LIGHT); assertTrue(StyleSheets.cacheSize() > 0); StyleSheets.clearCache(); assertEquals(0, StyleSheets.cacheSize())` — real cache lifecycle test.
- `Screen enum should have correct ordinal values` (L178–184): `assertEquals(0, Screen.FILE_BROWSER.ordinal); assertEquals(1, Screen.EDITOR.ordinal); assertEquals(2, Screen.SETTINGS.ordinal)` — **SUSPECT / METADATA**: ordinal values are compile-time constants. This asserts the declaration order of the enum, not any runtime behavior. A production refactor reordering enum constants would break this, but the test does not verify any user-visible behavior.
- `ThemeUtils should return correct font family` (L210–218): `val fontFamily = ThemeUtils.getFontFamily(false); assertNotNull(fontFamily)` — assertNotNull is not sufficient; font family could be any non-null value.
**Recommended fix:** Replace ordinal assertions with behavior assertions (e.g., the Screen enum should be navigable in tab order). Replace `assertNotNull(fontFamily)` with a specific font family name assertion.

---

## iosTest

### IOSPlatformTests.kt

**Verdict:** SUSPECT
**Methods reviewed:** 8 (102 lines fully read)
**Unit(s) under test:** `YoleIOSSettings`, `AppScreen` sealed class
**Evidence:**
- `testLineNumbersSetting` (L52–56): `settings.setShowLineNumbers(true); assertTrue(settings.getShowLineNumbers()); settings.setShowLineNumbers(false); assertFalse(settings.getShowLineNumbers())` — real round-trip, mutation-detectable.
- `testAutoSaveSetting` (L58–63): real boolean round-trip.
- `testAnimationsSetting` (L65–70): real boolean round-trip.
- `testThemeModes` (L37–49): `val defaultTheme = settings.getDefaultTheme(); assertTrue(defaultTheme in listOf("system", "light", "dark"))` — **TAUTOLOGICAL**: same pattern as `YoleDesktopSettingsTest`. Any valid string passes; the actual default value is not verified.
- `AppScreenTests.testAppScreenValues` (L78–87): `assertEquals(3, AppScreen::class.sealedSubclasses.size)` — **METADATA**: asserts the count of sealed subclasses. Passes with any 3-subclass sealed hierarchy regardless of what the subclasses represent. Does not verify navigation behavior.
- `IOSPlatformIntegrationTests.testPlatformModuleAccessible` (L92–96): `val settings = YoleIOSSettings(); assertNotNull(settings)` — compile-proof test; if `YoleIOSSettings` is importable and instantiable without crash, this always passes.
**Recommended fix:** For `testThemeModes`: assert the specific iOS platform default, e.g., `assertEquals("system", defaultTheme)`. For `testAppScreenValues`: navigate between screens and verify the navigation state changes. For `testPlatformModuleAccessible`: call at least one method and verify a meaningful result.

---

## wasmJsTest (shared)

### WasmFormatDetectionTests.kt

**Verdict:** CLEAN
**Methods reviewed:** 17 (366 lines fully read)
**Unit(s) under test:** `FormatRegistry.detectByExtension()`, `FormatRegistry.detectByContent()`, `FormatRegistry.getFormatsByExtension()` on Wasm
**Evidence:**
- `detect markdown by extension` (L31–36): `val format = FormatRegistry.detectByExtension("md"); assertEquals(FormatRegistry.ID_MARKDOWN, format.id); assertEquals("Markdown", format.name)` — exact ID + name assertion.
- `detect CSV by extension` (L38–42): `assertEquals(FormatRegistry.ID_CSV, format.id)` — specific ID.
- `getFormatsByExtension returns multiple formats for txt` (L82–89): `val formats = FormatRegistry.getFormatsByExtension("txt"); assertTrue(formats.size >= 2)` — asserts ambiguity is correctly detected.
- `detect markdown by content` (L100–107): passes real markdown content string, asserts `assertEquals(FormatRegistry.ID_MARKDOWN, result.id)`.
- All 17 tests use `assertEquals` on exact format IDs. Removing a format from `FormatRegistry` fails the corresponding test. Mutation-detectable.

---

### WasmPlatformIntegrationTests.kt

**Verdict:** CLEAN
**Methods reviewed:** 15 (355 lines fully read)
**Unit(s) under test:** `FormatRegistry`, `StyleSheets`, `ParsedDocument` on Wasm
**Evidence:**
- `StyleSheets returns different styles for light vs dark RST` (L176–183): `assertNotEquals(lightCss, darkCss)` — both modes produce non-blank CSS that differs; mutation-detectable (a stub returning a constant fails `assertNotEquals`).
- `StyleSheets caches results` (L202–212): `StyleSheets.clearCache(); access format CSS; assertTrue(cacheSize > 0); access same format CSS again; assertEquals(cacheSize, newSize)` — real cache lifecycle, verifies no double-insertion.
- `ParsedDocument HTML caching works on Wasm` (L244–257): `assertFalse(doc.hasHtmlCached)` before first call, `assertTrue(doc.hasHtmlCached)` after — real state transition check.
- `FormatRegistry formats list is initialized on Wasm` (L41–48): `assertTrue(FormatRegistry.isFormatsInitialized); assertTrue(FormatRegistry.formats.size >= 17)` — minimum size assertion on real registry.

---

### WasmProtocolStubTests.kt

**Verdict:** CLEAN
**Methods reviewed:** 18 (439 lines fully read)
**Unit(s) under test:** `FtpProtocolClient`, `SshClient`, `SmbProtocolClient` Wasm stubs
**Evidence:**
- `FTP connect returns failure with UnsupportedOperationException` (L44–58): `val result = client.connect(); assertTrue(result.isFailure); val exception = result.exceptionOrNull(); assertIs<UnsupportedOperationException>(exception); assertTrue(exception.message?.contains("FTP") == true); assertTrue(exception.message?.contains("web") == true)` — typed exception assertion + message content. A stub returning a different exception type fails `assertIs<>`.
- `FtpProtocolClient all operations produce consistent error messages` (L131–158): calls all 16 FTP operations in a loop, asserts each throws `UnsupportedOperationException` with message `"FTP not supported on web"` — exact string match. A stub returning a different message fails.
- SFTP and SMB follow identical patterns with their respective messages.
- Mutation test: change error message to `"FTP not supported"` (drop "on web") — 16 assertions fail.

---

### WebSecureStorageTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 14 (369 lines fully read)
**Unit(s) under test:** `WebSecureStorage` (production Wasm class)
**Evidence:**
- `should store encrypted data in localStorage` (L66–83): calls `storage.store("key", "plaintext")`, then directly reads `localStorage.getItem(storageKey)`, asserts `assertNotEquals("plaintext", storedValue)` (verifies encryption occurred) AND `assertTrue(storedValue.matches(Regex("[A-Za-z0-9+/=]+")))` (verifies base64 encoding).
- `should encrypt and decrypt correctly` (L87–95): real round-trip store + retrieve, `assertEquals("original value", retrieved)`.
- `should persist crypto key in localStorage` (L357–368): reads the JWK JSON from `localStorage`, asserts it contains `"kty"` and `"k"` fields — verifies key material is persisted.
- `should handle key not found` (L100–108): `assertNull(storage.retrieve("nonexistent_key"))` — real absence check.
- All tests use `kotlinx.browser.localStorage` directly to inspect storage side-effects, not just the API surface.

---

### TokenizerEngineWasmTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 1 (77 lines fully read)
**Unit(s) under test:** `TokenizerEngine` Wasm variant (syntax highlighting)
**Evidence:**
- `tokenizesMarkdownSnippet` (L46–76): initializes real `TokenizerEngine()`, calls `engine.initialize()` (asserts `isSuccess`), `loadGrammar("markdown")` (asserts `isGrammarLoaded`), `tokenize("# Heading\n\nA paragraph.\n", "markdown")`, asserts `tokens.isNotEmpty()` and `tokens.firstOrNull { it.scope.isNotBlank() } != null`.
- Anti-bluff comment at L43: "a stubbed TokenizerEngine.tokenize returning an empty list MUST fail this test on the tokens.isNotEmpty() assertion" — explicit mutation-verification note.
- `GlobalScope.promise` bridge used correctly for Wasm/coroutines-test compatibility gap.

---

## webApp wasmJsTest

### FileOperationsTest.kt

**Verdict:** BLUFF
**Methods reviewed:** 14 (396 lines fully read)
**Unit(s) under test:** Claimed: web file operations. Actual: self-defined lambda callbacks and local helper functions.
**Evidence:**
- `should download file with correct content and filename` (L20–37): `var downloadTriggered = false; val mockDownloadFile: (String, String) -> Unit = { content, filename -> downloadTriggered = true; assertEquals(testContent, content); assertEquals(testFilename, filename) }; mockDownloadFile(testContent, testFilename); assertTrue(downloadTriggered)` — the test defines `mockDownloadFile`, calls it directly with `testContent`/`testFilename`, asserts `downloadTriggered`. No production download code is called.
- `should handle download errors gracefully` (L75–96): `val mockDownloadWithError: (String) -> Unit = { _ -> throw Exception("Simulated download error") }; try { mockDownloadWithError("bad") } catch (e: Exception) { errorHandled = true; assertEquals("Simulated download error", e.message) }; assertTrue(errorHandled)` — catches the exception the test itself threw from its own lambda.
- `isValidFilename()` at L371–380 and `sanitizeFilename()` at L382–396: these are HELPER FUNCTIONS DEFINED WITHIN THE TEST FILE, not imports from production code. Tests at L320–367 call these local functions; they do not test any production file operation code.
- Mutation test: remove the entire production web file operations module — all 14 tests still pass because no production code is referenced.
**Recommended fix:** Import real web file operation functions from the `webApp` production source set. Test them by passing real inputs and verifying real side effects (DOM mutations via `kotlinx.browser.document`, `window.URL.createObjectURL` invocations, or `localStorage` state).

---

### FormatDetectionTest.kt

**Verdict:** SUSPECT
**Methods reviewed:** 12 (329 lines fully read)
**Unit(s) under test:** `detectFormatFromFilename()` (web utility function)
**Evidence:**
- `should detect markdown formats correctly` (L18–35): `assertEquals("markdown", detectedFormat, "Should detect markdown from $filename")` for extensions `.md`, `.markdown`, `.mdown`, `.mkd`, `.mdx` — real assertions with exact format ID.
- `should detect plaintext formats correctly` (L39–56): `assertEquals("plaintext", detectedFormat)` — real.
- `should detect web development formats correctly` (L83–99): asserts `assertEquals("css", ...)`, `assertEquals("javascript", ...)`, `assertEquals("typescript", ...)`, `assertEquals("python", ...)`, `assertEquals("cpp", ...)`, `assertEquals("go", ...)`, `assertEquals("ruby", ...)` — **SUSPECT**: Yole's `FormatRegistry` format IDs as defined in `TextFormat.Companion` do not include `"css"`, `"javascript"`, `"typescript"`, `"python"`, `"cpp"`, `"go"`, `"ruby"`. The registered IDs are `"markdown"`, `"csv"`, `"xml"`, `"json"`, `"yaml"`, `"todotxt"`, `"wikitext"`, etc. If `detectFormatFromFilename()` returns `"plaintext"` for CSS files (a reasonable fallback), this test fails — OR the web app has a separate format detection function with different IDs from the shared `FormatRegistry`. The ambiguity means the test may be verifying a locally-defined function with no tie to the production format system.
**Recommended fix:** Verify that `detectFormatFromFilename()` is the production function from the `webApp` source set, not a local test helper. Assert format IDs that exist in the shared `FormatRegistry` or document why the web app uses a different ID vocabulary.

---

### WebAppIntegrationTest.kt

**Verdict:** SUSPECT
**Methods reviewed:** 8 (187 lines fully read)
**Unit(s) under test:** Mixed — some real production calls, some boolean self-verification bluff.
**Evidence:**
- `should integrate file download with document content` (L22–39): **BLUFF** — `var downloadTriggered = false; val mockDownloadFile: (String, String) -> Unit = { _, _ -> downloadTriggered = true }; mockDownloadFile(testContent, testFilename); assertTrue(downloadTriggered)` — identical self-verifying lambda pattern from `FileOperationsTest`.
- `should integrate file upload with format detection` (L41–63): **BLUFF** — `var uploadProcessed = false; val mockProcessUpload: (String, String) -> String = { content, _ -> uploadProcessed = true; "markdown" }; val detectedFormat = mockProcessUpload(content, filename); assertTrue(uploadProcessed); assertEquals("markdown", detectedFormat)` — the lambda returns hardcoded `"markdown"`, the test asserts that hardcoded value.
- `should integrate parser registry with preview functionality` (L83–99): `val parser = ParserRegistry.getParser("markdown"); val parsedDoc = parser.parse(testContent); val htmlOutput = parser.toHtml(parsedDoc); assertTrue(htmlOutput.contains("Test Header"))` — **REAL**: calls production parser, asserts HTML contains header text. Mutation-detectable.
- `should handle large documents efficiently` (L144–167): real `ParserRegistry.getParser("markdown").parse(largeMd)`, asserts `htmlOutput.contains("Section 0")` and `htmlOutput.contains("Section 999")` — real behavioral.
- File is SUSPECT (not BLUFF) because 2 of 8 tests are real; the pattern is mixed.
**Recommended fix:** Replace `mockDownloadFile` and `mockProcessUpload` lambdas with real production function calls from the `webApp` source set.

---

### WebAppParserTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 10 (230 lines fully read)
**Unit(s) under test:** `ParserRegistry.getParser("markdown")`, `ParserRegistry.getParser("plaintext")`
**Evidence:**
- `should parse markdown for web display` (L21–58): `parser.parse(markdownContent)` → `parser.toHtml(parsedDoc)` → 6 specific HTML content assertions: `contains("Web Test Header")`, `contains("bold") || contains("strong")`, `contains("italic") || contains("em")`, `contains("List item")`, `contains("kotlin") || contains("code")`, `contains("https://example.com")`.
- `should generate clean HTML for web display` (L89–102): `assertFalse(htmlOutput.contains("<script"))`, `assertFalse(htmlOutput.contains("javascript:"))` — real security assertions on parser output.
- `should handle HTML escaping for web security` (L104–127): passes `<script>alert('xss')</script>` and `javascript:void(0)` in markdown content, asserts escaped output does not contain `<script` — real XSS-prevention verification.
- `should handle empty content gracefully` (L130–137): `val parsed = parser.parse(""); assertNotNull(parsed); val html = parser.toHtml(parsed); assertNotNull(html)` — real empty-input boundary.
- All tests use `ParserRegistry.getParser()` — confirmed production class.

---

### WebAppUITest.kt

**Verdict:** BLUFF
**Methods reviewed:** 8 (192 lines fully read)
**Unit(s) under test:** Claimed: web app UI. Actual: local boolean state machines.
**Evidence:**
- `should render main web app correctly` (L20–30): `var appRendered = false; try { appRendered = true } catch (e: Exception) { fail("App rendering failed: ${e.message}") }; assertTrue(appRendered)` — sets `appRendered = true` unconditionally inside an unconstrained `try` block. No production code called. This passes even if the production web app module is entirely deleted.
- `should display theme toggle button` (L35–47): `var themeToggleAvailable = false; try { themeToggleAvailable = true } catch (e: Exception) { fail(...) }; assertTrue(themeToggleAvailable)` — identical pattern.
- `should render format list correctly` (L67–81): `var formatListRendered = false; try { formatListRendered = true } catch (e: Exception) { fail(...) }; assertTrue(formatListRendered)` — identical.
- `should render preview component correctly` (L132–150): only test in the file with production interaction — calls `ParserRegistry.getParser("markdown")`. One real call among 8 tests.
- Mutation test: delete every `.kt` file in `webApp/src/wasmJsMain/` — 7 of 8 tests still pass.
**Recommended fix:** Use `kotlinx.browser.document.getElementById()` or `kotlinx.browser.document.querySelector()` to verify DOM elements are present after rendering. Alternatively use the Kotlin/Wasm Compose test API when available. Remove the try-catch-set-flag-assertTrue pattern entirely.

---

## Summary

| Verdict | Count | Files |
|---------|-------|-------|
| BLUFF   | 5     | `DesktopAppParserTest.kt`, `DesktopAppUITest.kt`, `ui/TestStubs.kt`, `webApp/FileOperationsTest.kt`, `webApp/WebAppUITest.kt` |
| SUSPECT | 11    | `AllFormatsAutomationTest.kt`, `DesktopAppIntegrationTest.kt`, `DesktopFileOperationsTest.kt`, `DesktopKeyboardShortcutsTest.kt`, `DesktopWindowManagerTest.kt`, `FullUIAutomationTest.kt`, `ui/YoleDesktopSettingsTest.kt`, `ui/YoleDesktopUITest.kt`, `iosTest/IOSPlatformTests.kt`, `webApp/FormatDetectionTest.kt`, `webApp/WebAppIntegrationTest.kt` |
| CLEAN   | 10    | `DesktopAppSettingsTest.kt`, `DesktopFileManagerTest.kt`, `TabNavigationTest.kt`, `ui/import_/DesktopImportDragDropTest.kt`, `WasmFormatDetectionTests.kt`, `WasmPlatformIntegrationTests.kt`, `WasmProtocolStubTests.kt`, `WebSecureStorageTest.kt`, `TokenizerEngineWasmTest.kt`, `webApp/WebAppParserTest.kt` |

---

### Critical Finding 1: TestStubs.kt root-cause bluff enabler

`desktopApp/src/test/.../ui/TestStubs.kt` (L1–178) defines stub composables `SettingsScreen`, `FileBrowserScreen`, and `EditorScreen` in package `digital.vasic.yole.desktop.ui` — the same package as the production composables. Any test importing `digital.vasic.yole.desktop.ui.*` resolves to these stubs, not the production implementations. This silently makes `DesktopAppUITest.kt` and `FullUIAutomationTest.kt` exercise stub behavior.

**Fix:** Move stubs to `digital.vasic.yole.desktop.ui.testfixtures` package. Never co-locate stubs with production composables.

---

### Critical Finding 2: Boolean self-verification pattern in webApp tests

`webApp/FileOperationsTest.kt`, `webApp/WebAppUITest.kt`, and 2 methods in `webApp/WebAppIntegrationTest.kt` share an identical BLUFF pattern:

```kotlin
var flag = false
try {
    flag = true
} catch (e: Exception) { fail(...) }
assertTrue(flag)
```

This pattern is structurally equivalent to `assertTrue(true)`. The try block never contains any production code call that could throw, so `flag = true` is always reached. Removing the entire production module does not affect these tests.

---

### Critical Finding 3: Self-referential input assertions in DesktopAppParserTest

The dominant pattern in `DesktopAppParserTest.kt` is asserting against the test's own input string:

```kotlin
val content = "# Header 1\n..."  // test builds this
ParserRegistry.getParser("markdown").parse(content)  // production call (correct)
assertTrue(content.contains("# Header 1"))  // asserts OWN INPUT, not parser output
```

The parser call is present but its result is never inspected. Replacing the parser with a no-op does not change any assertion outcome.

---

### Tautological Default Assertions (SUSPECT pattern)

`YoleDesktopSettingsTest.kt`, `YoleDesktopUITest.kt`, and `IOSPlatformTests.kt` all use:

```kotlin
assertTrue(themeMode in listOf("system", "light", "dark"))
assertTrue(autoSave is Boolean)
assertTrue(fontSize in 8..72)
```

These pass with any conforming implementation and with trivial stubs that return `"system"`, `false`, and `12` respectively. They verify the return type and valid-range envelope, not the actual configured default.
