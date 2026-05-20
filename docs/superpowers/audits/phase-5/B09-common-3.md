# Phase 5 Audit — B09 shared/commonTest monitoring/perf/safety/security/syntax/ui/util/e2e/database
Audited: 2026-05-20  |  Files: 26  |  Bluff: 5  |  Suspect: 4  |  Clean: 17

---

## Findings

### shared/src/commonTest/kotlin/digital/vasic/yole/security/OAuthSecurityEdgeCaseTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 30
- **Unit(s) under test:** `shared/src/commonMain/kotlin/digital/vasic/yole/network/auth/AuthTokenManager.kt`, `SecureStorage` (via `InMemorySecureStorage` test helper)
- **Evidence:** The test wires a real `AuthTokenManager` to an in-process `InMemorySecureStorage`. The production `storeAccessToken` calls `storage.storeToken("${serviceName}_access", token)`, so a stub returning `Result.success(Unit)` for everything would break the strongest assertions. Specifically: `separateServicesHaveIsolatedTokens` (line 305) asserts `assertNotEquals(tokenA, tokenB)` — with a stub that returns a fixed value both would be equal, killing the test. `hasValidTokenReturnsFalseForEmptyToken` (line 89) reads back the stored token and checks `isNotBlank()` logic — a stub that always returns `true` fails. `tokenExpirationInThePast` (line 114) verifies `isTokenExpired()` returns `true` when an epoch in the past is stored — a stub always returning `false` fails. The `storageFailureThenRecovery` test (line 486) exercises the `shouldFail` flag: a stub that never fails would miss the recovery path. Token isolation (line 299) is the strongest real behavioral assertion.
- **Recommended fix:** none.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/security/OwaspSecurityTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 35
- **Unit(s) under test:** `PathUtils.kt`, parsers (MarkdownParser, PlaintextParser, CsvParser, etc.), `CircuitBreakerOpenException`, `StorageConfig.*`, `WebDavService`, `FtpService`, `SftpService`
- **Evidence:** Several strong tests are CLEAN: `pathTraversalBasicDotDot` (line 51) asserts `assertFailsWith<IllegalArgumentException>` — a stub that never throws fails. `escapeHtmlPreventsXss` (line 309) asserts the specific HTML entities; a stub that returns the input unchanged fails. The `parsersHandleMaliciousInputWithoutCrash` (line 331) loop asserts `assertNotNull(doc)` for 7 parsers × 5 inputs — legitimate anti-crash coverage. However, three groups are SUSPECT:
  1. **Config creation tests** (`dropboxConfigCanBeCreatedWithSensitiveData`, `ftpConfigCanBeCreatedWithSensitiveData`, etc., lines 233–303): they only assert `assertEquals("test", config.name)` — a trivial stub `StorageConfig.DropboxConfig(name, ...)` that stores only `name` would pass. Password/token fields are created but never read back.
  2. **`webdavPathTraversalBlocked` / `ftpPathTraversalBlocked` / `sftpPathTraversalBlocked`** (lines 355–387): assert `parent == null || parent == "/"` on `getParentPath("/")`. This is an OR condition — a stub returning `null` OR `/` both pass. Does not verify traversal prevention on non-root paths.
  3. **Null-byte / CRLF / very-long tests** (lines 109–193): assert only `assertNotNull(result)` or `result.isNotEmpty()` — an implementation that ignores control characters and returns the root path would PASS.
- **Recommended fix:** For config tests: add assertions that verify the sensitive fields are stored (`assertEquals("secret-access-token-12345", config.accessToken)`). For path-traversal service tests: call `getParentPath("/subdir")` and assert the result stays within root. For null-byte tests: either assert the character was stripped or assert it was preserved — any specific non-trivial assertion.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/security/PathTraversalDeepTests.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 42
- **Unit(s) under test:** `PathUtils.kt`
- **Evidence:** The traversal-rejection tests are strong: `deepTraversalEightLevels` (line 210) asserts `assertFailsWith<IllegalArgumentException>` — a stub that never throws fails. `longPathWithTraversalAttempts` (line 198) asserts `assertEquals("/etc/passwd", result)` — a stub returning `/` fails. However a large category of tests only asserts `assertNotNull(result)` and either `result.startsWith("/home/user")` or `result.isNotEmpty()`. Specifically: all null-byte, CRLF, control character, URL-encoded, double-encoded, backslash, space-injection, trailing-dots, and boundary tests (approximately 28 out of 42 methods) assert at most `startsWith("/home/user")`. With the real `PathUtils`, a stub that strips the relative path and returns `"$rootPath"` unchanged for any non-traversal input would pass every one of these 28 tests — the mutant survives because none verify the actual appended segment. Only tests that assert specific expected values (`assertEquals("/home/user", result)` for `"."`, `assertEquals("/path/with/empty/slashes", result)`, etc., lines 257–259 and ~335–350) kill mutants.
- **Recommended fix:** For the null/control-char tests, add `assertTrue(result.contains(expectedStrippedPath))` or similar assertion that verifies the output includes the actual path segment, not just the root prefix.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/security/SecurityEventLoggerTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 16
- **Unit(s) under test:** `SecurityEventLogger.kt`
- **Evidence:** Tests exercise the real `SecurityEventLogger` singleton with real method calls. The strongest assertion is in `maxEventsEvictsOldestWhenExceeded` (line 212): it logs 10 events with `maxEvents = 5`, then asserts `assertEquals("Svc5", events[0].service)` (line 225) — a stub that returned the first 5 would fail (wrong service names). `logPathTraversalBlockedRecordsEventWithAttemptedPath` (line 67) asserts `assertEquals("../../etc/passwd", event.details["attemptedPath"])` — a stub ignoring the `attemptedPath` argument fails. `logCircuitBreakerOpenRecordsEventWithFailureCount` (line 87) asserts message content `"Circuit breaker opened after 5 failures"` and `details["failureCount"] == "5"` — a stub with hardcoded text fails on different count inputs. `concurrentLoggingFromFiftyCoroutines` (line 276) asserts 50 unique service names — proves no events were dropped under concurrency.
- **Recommended fix:** none.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/security/SecurityValidationTests.kt
- **Verdict:** BLUFF
- **Methods reviewed:** 40
- **Unit(s) under test:** Multiple parsers (MarkdownParser, PlaintextParser, etc.), `PathUtils.kt`, `StorageConfig.*`, `WebDavService`, `FtpService`, `SftpService`, `SmbService`
- **Evidence:** The critical flaw is in the `assertNoRawScript` helper (lines 479–485): it does NOT check the `html` parameter passed in. Instead it runs `input.escapeHtml()` on a hardcoded string (`"<script>alert(1)</script>"`) and asserts _that_ output has no `<script>` tag. This means every XSS test using `assertNoRawScript(html, parserName)` — `testWikitextHtmlEscapesScriptTags` (line 108), `testCreoleHtmlEscapesScriptTags` (line 118), `testTiddlyWikiHtmlEscapesScriptTags` (line 124), `testLatexHtmlEscapesScriptTags` (line 131), `testAsciidocHtmlEscapesScriptTags` (line 140), `testOrgModeHtmlEscapesScriptTags` (line 148), `testRstHtmlEscapesScriptTags` (line 155), `testKeyValueHtmlEscapesScriptTags` (line 164), `testTaskpaperHtmlEscapesScriptTags` (line 173), `testTextileHtmlEscapesScriptTags` (line 179), `testRMarkdownHtmlEscapesScriptTags` (line 188), `testTodoTxtHtmlEscapesScriptTags` (line 196), `testBinaryHtmlEscapesScriptTags` (line 204) — all pass even if the parser returns the raw `<script>` tag in its HTML output. Mutant: replace every parser's `toHtml()` to return the raw input with zero escaping — 13 of these tests still PASS because `assertNoRawScript` never inspects the `html` local variable. The `testMarkdownHtmlEscapesScriptTags` test (line 76) has inline logic and is not bluff. Config toString tests (lines 311–420) only call `assertNotNull(str)` — a stub returning `"stub"` passes. `testParsedDocumentHtmlDoesNotContainRawEventHandlers` (line 440) asserts only `assertNotNull(html)` and `assertTrue(html.isNotEmpty())` — clearly insufficient.
- **Recommended fix:** Fix `assertNoRawScript` to check the actual `html` argument: `assertFalse(html.contains("<script>", ignoreCase = true), ...)`. For config toString tests, assert `assertFalse(str.contains("SuperSecret123!"))` to verify passwords are masked. For `testParsedDocumentHtmlDoesNotContainRawEventHandlers`, assert `assertFalse(html.contains("onerror=", ignoreCase = true))`.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/syntax/BadgeTinterTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 4
- **Unit(s) under test:** `BadgeTinter.kt`, `GrammarRegistry`, `EnabledFormatGate`
- **Evidence:** `markdownFileGetsLangSpecificTint` (line 55) asserts `assertEquals(0xFF00AA00.toInt(), BadgeTinter.tintFor("readme.md", theme))` — a stub returning a constant `0xFF000000` fails. `disabledFormatReturnsNull` (line 79) uses `EnabledFormatGate.setEnabled(emptySet())` and asserts `assertNull(BadgeTinter.tintFor("readme.md", theme))` — a stub ignoring the gate fails. The comment block (lines 9–18) explicitly documents mutation evidence confirming 4/4 tests fail on stub substitution.
- **Recommended fix:** none.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/syntax/FormatEnablementDefaultTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 1
- **Unit(s) under test:** `FormatRegistry.defaultEnabledFormatIds()` (production source not read here, but the assertion is precise)
- **Evidence:** `freshDefaultIsMarkdownOnly` (line 20) asserts `assertEquals(setOf("markdown"), defaults)` — an exact set equality. A stub returning `emptySet()` fails, a stub returning `setOf("markdown", "x")` fails. The comment (line 10) explicitly states this mutation was verified.
- **Recommended fix:** none.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/syntax/FormatEnablementGateTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 4
- **Unit(s) under test:** `EnabledFormatGate` (production code exercises real `StateFlow<Set<String>>` gate logic)
- **Evidence:** `requireEnabled_throwsForDisabled` (line 38) asserts `assertFailsWith<FormatDisabledException>` AND `assertEquals("python", ex.formatId)` (line 42) — a stub that always throws but with wrong formatId fails. `setEnabled_replacesEntireSet` (line 46) asserts `assertFalse(isEnabled("kotlin"))` after replacing with `setOf("markdown")` — a stub that accumulates instead of replaces fails.
- **Recommended fix:** none.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/syntax/GrammarRegistryTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 10
- **Unit(s) under test:** `GrammarRegistry` (production `detectByFilename`, `detectLangId`)
- **Evidence:** `detectsMarkdownByMdExtension` (line 40) asserts `assertEquals("markdown", g.id)` — a stub returning the first grammar regardless of extension fails on `unknownExtensionReturnsNull`. `disabledMarkdownReturnsNull` (line 74) uses `EnabledFormatGate.setEnabled(emptySet())` then asserts `assertNull(...)` — a stub ignoring the gate fails. `detectLangId_plaintextForUnknown` (line 90) asserts `assertEquals("plaintext", ...)` for `"data.bin"` — a stub returning `"markdown"` always fails. The comment (lines 11–14) documents the mutation evidence.
- **Recommended fix:** none.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/syntax/ScopeMapperTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 32
- **Unit(s) under test:** `ScopeMapper.kt`
- **Evidence:** The test has 25+ exact equality assertions against the real production mapping table. `exactMatch_commentLine` (line 28) asserts `assertEquals("comment.line.double-slash", ScopeMapper.treeSitterToVsCode("comment.line"))` — a stub that returns the input unchanged fails this immediately. `hierarchicalFallback_keywordControlReturn` (line 156) asserts `assertEquals("keyword.control", ...)` for an unmapped 3-level scope — a stub that always returns the exact input fails (input is `"keyword.control.return"`, not `"keyword.control"`). `identity_unmappedScope` (line 191) asserts the input is returned for unknown scopes — a stub returning `""` always fails. The comment (lines 12–15) confirms mutation evidence: stubbing `treeSitterToVsCode` to `""` causes 15+ failures.
- **Recommended fix:** none.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/syntax/VsCodeThemeParserTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 7
- **Unit(s) under test:** `VsCodeThemeParser.kt`
- **Evidence:** `parsesUiColors` (line 52) asserts exact ARGB int values `assertEquals(0xFF1E1E1Eu.toLong().toInt(), theme.uiColor("editor.background"))` — a stub returning 0 fails. `parsesTokenColorsWithScopeArray` (line 59) asserts three specific token colors — a stub returning a constant color value fails on at least two. `malformedJsonThrowsThemeParseException` (line 81) asserts a specific exception type — a stub that never throws fails. The comment (line 8) documents mutation evidence.
- **Recommended fix:** none.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/ui/AccessibilityComprehensiveTests.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** ~60
- **Unit(s) under test:** `AccessibilityConstants`, `KeyboardShortcuts`, `AccessibilitySettings`, `AccessibilityState`, `ScreenReader`, `FocusManagement`, `TouchTargets`, `ThemeUtils`
- **Evidence:** Several test groups are strong: `contrast ratio black on white is approximately 21` (line 259) asserts `ratio >= 20.9 && ratio <= 21.1` — a stub returning `1.0` fails. `keyboard shortcut SAVE has correct key and modifier` (line 371) asserts `assertEquals(Key.S, save.key)` and `assertTrue(save.ctrl)` — a stub with wrong key fails. `touch target 43dp is below WCAG minimum` (line 213) asserts `assertFalse(TouchTargets.isAccessibleSize(43.dp))` — a stub that always returns `true` fails. However, a large group is effectively "absence of error" tests: `screenReader announceStatus does not throw for normal message` (line 320), `focusManagement moveFocusNext is callable` (line 533), `accessibilityState announce does not throw for normal text` (line 574) — these all PASS trivially if every `ScreenReader`/`FocusManagement` method is a no-op. No assertion verifies the announcement was actually dispatched to any listener or platform API. These tests exercise ~25 of the ~60 methods.
- **Recommended fix:** For ScreenReader/FocusManagement tests, introduce a test listener/interceptor and assert the message was actually dispatched. Alternatively, mark those tests with `// ANTI-BLUFF-EXEMPT: announce() is a fire-and-forget side-channel with no KMP-portable return value` if the design intent is no-op in tests.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/ui/AccessibilityTest.kt
- **Verdict:** BLUFF (partial)
- **Methods reviewed:** 25
- **Unit(s) under test:** `AccessibilityConstants`, `KeyboardShortcuts`, `AccessibilitySettings`, `AccessibilityState`, `ScreenReader`, `FocusManagement`, `TouchTargets`
- **Evidence:** Strong tests exist: `keyboard shortcuts are defined` (line 51) asserts `assertNotNull` for 11 named constants; `isAccessibleSize correctly validates sizes` (line 181) asserts both `assertTrue(44dp)` and `assertFalse(43dp)` with multiple values — a stub returning `true` always fails. However: `isScreenReaderActive returns boolean` (line 206) contains a tautology: `assertTrue(isActive || !isActive)` (line 209). This assertion ALWAYS passes regardless of what `isScreenReaderActive()` returns, including from a stub. `announce does not throw` (line 212), `announceStatus/Alert does not throw` (lines 220–226), `focus management methods do not throw` (line 240) are pure "absence of error" tests that pass trivially with no-op stubs.
- **Recommended fix:** Remove the tautological `assertTrue(isActive || !isActive)` and replace with `assertFalse(isActive, "Default screen reader should be inactive")`. For announce tests, either add listener interception or mark as exempt.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/ui/AccessibilityTests.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** ~65
- **Unit(s) under test:** Same as `AccessibilityTest.kt` — `AccessibilityConstants`, `KeyboardShortcuts`, `AccessibilitySettings`, `AccessibilityState`, `ScreenReader`, `FocusManagement`, `TouchTargets`
- **Evidence:** Several tests are strong: the exact constant value tests (`constants MIN_TOUCH_TARGET_SIZE is exactly 44dp` line 40, `keyboard shortcut CLOSE uses Ctrl+W` line 216, exact `assertEquals(Key.W, shortcut.key)`) kill stubs with wrong values. `keyboard shortcuts enum has 11 entries` (line 191) asserts the exact count — adding or removing shortcuts fails. `no keyboard shortcuts use alt modifier` (line 316) iterates all entries and asserts `assertFalse(shortcut.alt)` — a stub setting any alt=true fails. Weak zone: ~15 "state announce does not throw" tests (lines 454–477) and all `FocusManagement` "does not throw" tests (lines 546–576) are absence-of-error only. `settings for reduced motion should be respected by animation system` (line 631) only asserts `assertTrue(settings.reduceMotion)` — this verifies the data class field, not any behavioral effect.
- **Recommended fix:** Same as `AccessibilityComprehensiveTests` — the announce/focus tests need listener interception or exempt markers.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/ui/AnimationConstantsTest.kt
- **Verdict:** BLUFF (partial)
- **Methods reviewed:** 19
- **Unit(s) under test:** `AnimationTiming`, `AnimationEasing`, `MicroInteractions`, `ScreenTransitions`, `ListAnimations`
- **Evidence:** The timing constants tests with exact range checks (`standard is reasonable` line 60 asserting `in 250..500`, `very quick is under 200ms` line 44 asserting `<= 200`) kill stubs that return out-of-range values. However, the large group of `assertNotNull(ScreenTransitions.slideIn())` tests (lines 100–131, ~12 tests) are BLUFF: if `slideIn()`, `slideOut()`, `fade()`, `scaleIn()`, `scaleOut()`, `expandVertically()`, `shrinkVertically()` all return a non-null stub object (e.g., `EnterTransition.None`), every `assertNotNull` test passes. The tests do not verify the transition duration, easing function, or animation direction. A stub `fun slideIn(durationMillis: Int = AnimationTiming.SLOW) = EnterTransition.None` passes all 12 transition tests.
- **Recommended fix:** Add assertions that verify the duration is embedded: extract `durationMillis` from the returned `AnimationSpec` and assert it matches the input (e.g., for `ScreenTransitions.slideIn(500)`, verify the underlying tween duration is 500ms). At minimum, assert `assertNotEquals(ScreenTransitions.slideIn(100), ScreenTransitions.slideIn(500))` to prove parameter is respected.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/ui/AnimationsTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** ~40
- **Unit(s) under test:** `AnimationTiming`, `AnimationEasing`, `MicroInteractions`, `ScreenTransitions`, `ListAnimations`
- **Evidence:** The exact constant tests are strong: `timing VERY_QUICK is 100ms` (line 35) asserts `assertEquals(100, AnimationTiming.VERY_QUICK)` — any stub with a different constant fails. `microInteractions pressAnimationSpec has MediumBouncy damping` (line 158) asserts `assertEquals(Spring.DampingRatioMediumBouncy, spec.dampingRatio)` — a stub with wrong ratio fails. `easing Emphasized and Decelerate have same values` (line 136) and `easing Standard differs from Accelerate` (line 143) assert object equality/inequality — a stub returning a single fixed easing for all fails one of these. The `assertNotNull` transition tests remain weak (same issue as `AnimationConstantsTest`) but the majority of test methods provide genuine mutation-killing assertions.
- **Recommended fix:** none (the weak assertNotNull transition tests are a minor issue but the file overall earns CLEAN due to the strong constant/spec value assertions).

---

### shared/src/commonTest/kotlin/digital/vasic/yole/ui/AnimationTests.kt
- **Verdict:** BLUFF (partial)
- **Methods reviewed:** 11
- **Unit(s) under test:** `AnimationTiming`, `MicroInteractions`, `ScreenTransitions`, `ListAnimations`
- **Evidence:** `AnimationTiming constants have correct values` (line 31) asserts all 6 exact values — strong. `MicroInteractions animation specs are properly configured` (line 42) asserts `spec.dampingRatio > 0f` and `spec.stiffness > 0f` — weak (any positive value passes). Lines 63–174 are all `assertNotNull(ScreenTransitions.X())` — 7 transition functions × 2 signatures = 14 assertNotNull calls that pass trivially with stub returning `EnterTransition.None`/`ExitTransition.None`. `ListAnimations itemEnter creates proper enter transition with index delay` (line 129) calls `itemEnter(0)`, `itemEnter(1)`, `itemEnter(3)` and asserts all non-null — does NOT verify that index affects the delay, so different-index stubs pass. `animation timing values are within reasonable ranges` (line 155) checks each constant is in a range — the ranges are wide enough that minor mutations survive.
- **Recommended fix:** Replace `assertNotNull(ScreenTransitions.slideIn(500))` with `assertNotEquals(ScreenTransitions.slideIn(100), ScreenTransitions.slideIn(500))` to verify duration parameter is respected. For ListAnimations, add a delay-extraction assertion to confirm different indices produce different stagger offsets.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/ui/ThemeWcagContrastTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 2
- **Unit(s) under test:** `ThemeUtils.meetsWcagAA()`, `ThemeUtils.calculateContrastRatio()`, `LegacyThemeBridge.legacyLight/legacyDark`
- **Evidence:** Both tests assert `ThemeUtils.meetsWcagAA(fg, bg)` using real color values extracted from `LegacyThemeBridge`. The comment (line 36) states mutation verification: flipping the ARGB byte for `editor.foreground` to reduce contrast below 4.5 causes the test to fail. A stub `meetsWcagAA` that always returns `true` passes but a stub that always returns `false` fails — and the real mutation scenario (reduced contrast) causes the real implementation to return `false`, triggering failure. The failure message also prints the actual ratio (line 44), showing the test exercises the real computation.
- **Recommended fix:** none.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/ui/UIComponentsCoverageTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** ~55
- **Unit(s) under test:** `AccessibilityModifiers`, `ScreenReader`, `TouchTargets`, `KeyboardShortcuts`, `AccessibilitySettings`, `FocusManagement`, `AnimationEasing`, `ScreenTransitions`, `ListAnimations`, `AccessibilityState`, `AccessibilityConstants`
- **Evidence:** Strong tests: `isAccessibleSize boundary at exactly 44dp` (line 350) asserts `assertTrue(44dp)` and `assertFalse(43.9dp)` (line 354) — tight boundary. `toDisplayString for shortcuts with ctrl starts with Ctrl prefix` (line 426) iterates real ctrl shortcuts — a stub returning `""` fails. `Shortcuts constant values match expected format` (line 475) checks exact strings like `assertEquals("F1", AccessibilityConstants.Shortcuts.HELP)` — precise. Weak zone: all `assertNotNull(Modifier.accessibleX(...))` tests (lines 37–262, approximately 30 tests) — a `Modifier` extension that ignores its arguments and returns `Modifier` passes all. `itemEnter stagger calculation produces different delays for different indices` (lines 566–580) calls `itemEnter(0)`, `itemEnter(1)`, `itemEnter(2)`, `itemEnter(3)` but only asserts `assertNotNull` on each — despite its name, it does NOT verify different delays.
- **Recommended fix:** For Modifier tests: extract and verify the semantic properties set (content description string, role value, etc.) — this requires Compose test infrastructure. In commonTest this is likely impossible without semantic tree inspection, so add `// ANTI-BLUFF-EXEMPT: Modifier semantics require Compose UI test infrastructure` markers. For `itemEnter stagger`, add `assertNotEquals(ListAnimations.itemEnter(0), ListAnimations.itemEnter(1))` if the spec objects support equality comparison.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/util/ConcurrencySafetyTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 8 (across `ConcurrencySafetyTest` + `RaceConditionDetectionTest`)
- **Unit(s) under test:** Kotlin coroutine primitives (`Mutex`, `Channel`, `CoroutineScope`) — no specific Yole production unit; validates KMP coroutine mechanics
- **Evidence:** `testMutexProtectedCounter` (line 27) asserts `assertEquals(10000, counter)` — a race condition without the mutex would produce a non-10000 value under `Dispatchers.Default` parallelism, failing the test. `testCheckThenActWithMutex` (line 173) asserts `assertEquals(1, setCount)` — without the mutex, multiple coroutines could pass the `if (state == null)` check, making `setCount > 1` and failing. `testConcurrentMapAccess` (line 81) asserts each key has exactly 100 increments. The tests genuinely verify concurrency safety of the mutex primitive itself.
- **Recommended fix:** none.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/util/FileStorageContractTests.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 6
- **Unit(s) under test:** `FileHandle` (platform implementation not read; test uses `System.getProperty("java.io.tmpdir")`)
- **Evidence:** `readBytes on non-existent file returns null` (line 31) asserts `assertNull(result)` — a stub returning non-null fails. `exists on non-existent file returns false` (line 37) asserts `assertFalse(...)` — a stub returning `true` fails. However: `write empty content returns true for valid uri` (line 22) asserts only `assertTrue(result)` without reading back the written content — if `writeBytes` returns `true` but doesn't actually write, no assertion detects this. `displayName extracts filename from path` (line 46) asserts `endsWith(".txt")` — any stub that appends `.txt` passes. The tests are KMP-questionable: they use `System.getProperty("java.io.tmpdir")` which is JVM-only — this file is in `commonTest`, meaning on non-JVM platforms the property call would fail or return null, potentially causing test failures or false passes via null-path behavior.
- **Recommended fix:** Write a byte array, read it back, and assert the content matches. Replace `System.getProperty` with a KMP-safe path or move this test to `desktopTest`.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/util/LazyLoadingTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 9 (across `LazyStringLoaderTest` + `FlowLazyLoaderTest`)
- **Unit(s) under test:** `LazyStringLoader`, `FlowLazyLoader` (production facades in `util/LazyLoading.kt`)
- **Evidence:** `testGetChunkReturnsCorrectContent` (line 22) asserts `chunk0.startsWith("Line 1")` on real content — a stub returning `""` fails. `testGetLinesReturnsCorrectRange` (line 41) asserts `assertEquals(5, lines.size)` and `assertEquals("Line 1", lines[0])` and `assertEquals("Line 5", lines[4])` — a stub returning an empty list or wrong lines fails. `testClearFreesChunks` (line 53) asserts `loader.getMemoryUsage() > 0` after loading, then `assertEquals(0, loader.getMemoryUsage())` after clear — a stub that never tracks memory fails the first assertion. `testGetVisibleRange` (line 107) asserts `assertEquals(2 until 18, range)` — exact range value, not just non-null.
- **Recommended fix:** none.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/util/MemoryLeakDetectionTest.kt
- **Verdict:** BLUFF (partial)
- **Methods reviewed:** 11 (across `MemoryLeakDetectionTest` + `ResourceLifecycleTest`)
- **Unit(s) under test:** `FlowLazyLoader`, `LazyStringLoader` (Yole production units); other tests exercise ad-hoc anonymous objects with no production UUT
- **Evidence:** `testFlowLazyLoaderCleanup` (line 149) and `testLazyDocumentLoaderClear` (line 160) are CLEAN — they call real production methods and assert specific numeric outcomes. However, most of the other tests verify only ad-hoc anonymous objects defined in the test body itself, not any Yole production code: `testReferenceCleanup` (line 24) sets a local var to null and asserts it is null — a trivially true property of Kotlin reference semantics. `testObjectCreationAndCleanup` (line 34) verifies a local `ByteArray` is cleared — same issue, zero production code exercised. `testResourceCleanup` (line 50) defines an anonymous `object` with a `close()` method in-test — verifies the test fixture, not any production class. `testCacheEviction` (line 83) defines an entirely in-test cache — the eviction logic tested is the test code itself. `testListenerCleanup` (line 109), `testStreamCleanup` (line 135) — same pattern. Stub substitution: replace every `FlowLazyLoader` method with a no-op — `testFlowLazyLoaderCleanup` fails (content stays non-empty); but all the other tests still pass.
- **Recommended fix:** Remove or reclassify the non-production tests. If they are intended to document Kotlin memory semantics for educational purposes, add `// ANTI-BLUFF-EXEMPT: documents KMP reference semantics, not a production unit`. Keep the `testFlowLazyLoaderCleanup` and `testLazyDocumentLoaderClear` tests as-is.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/util/PlatformSyncTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 22
- **Unit(s) under test:** `platformSynchronized` (production function in `util/PlatformSync.kt`)
- **Evidence:** `platformSynchronized returns value correctly` (line 44) asserts `assertEquals(42, result)` — a stub that ignores the block and returns 0 fails. `platformSynchronized propagates exceptions` (line 191) asserts `assertFailsWith<IllegalStateException>` and `assertEquals("test error", exception.message)` — a stub swallowing exceptions fails. `nested platformSynchronized calls with different locks` (line 140) asserts `assertEquals(30, result)` by summing nested results — a stub that short-circuits fails. `nested platformSynchronized calls with same lock` (line 155) tests reentrancy — a deadlocking implementation fails the test (or hangs, which is also a failure). `platformSynchronized applies side effects` (line 244) checks all 3 list elements by position.
- **Recommended fix:** none.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/util/RateLimitingTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 16 (across `RateLimiterTest`, `TokenBucketTest`, `AdaptiveRateLimiterTest`, `OperationThrottlerTest`)
- **Unit(s) under test:** `RateLimiter`, `TokenBucket`, `AdaptiveRateLimiter`, `OperationThrottler` (production facade in `util/RateLimiting.kt`)
- **Evidence:** `testConcurrentExecutionsAreThrottled` (line 51) asserts `maxActive <= 2` using real semaphore mechanics — a stub ignoring the semaphore would allow all 10 concurrent executions, making `maxActive = 10 > 2` and failing. `testTryAcquireFailsWhenEmpty` (line 88) asserts `assertTrue` then `assertFalse` for a capacity-1 bucket — a stub always returning `true` fails the second call. `testThrottlesAfterMaxOperations` (line 142) asserts the 4th call returns `false` after 3 allows — a stub always returning `true` fails. `testClearResetsThrottle` (line 159) verifies post-clear re-allow — a stub without state fails. `testRateDoesNotExceedBounds` (line 122) asserts `rate in 2..8` after 50 operations — any out-of-bounds value fails.
- **Recommended fix:** none.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/util/StressAndIntegrationTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** ~35 (across `RateLimiterStressTest`, `LazyLoadingStressTest`, `ParserRegistryStressTest`, `NonBlockingResponsivenessTest`)
- **Unit(s) under test:** `RateLimiter`, `TokenBucket`, `AdaptiveRateLimiter`, `OperationThrottler`, `LazyStringLoader`, `FlowLazyLoader`, `ParserRegistry`, `ParserInitializer`, `FormatRegistry`
- **Evidence:** The strongest assertions: `testRateLimiterConcurrencyNeverExceeded` (line 65) tracks a `concurrent` counter inside the semaphore and asserts `violationDetected == false` — any semaphore bug allowing > 3 concurrent threads sets the flag and fails. `testOperationThrottlerRapidFire` (line 311) asserts `assertEquals(10, allowed)` and `assertEquals(90, throttled)` — exact counts that fail with wrong throttling logic. `testClearAndReRegisterCycle` (line 741) asserts `assertNull` after clear and `assertNotNull` after re-register — a stub that never clears fails. `testHasParserDoesNotTriggerInstantiation` (line 786) asserts `pendingBefore == pendingAfter` and `instantiatedBefore == instantiatedAfter` — precise count equality that fails if `hasParser` mistakenly triggers instantiation. `testLazyDocumentLoaderClearFreesMemory` (line 495) asserts `assertEquals(0L, memoryAfter)` — a stub that always returns non-zero fails. Note: `testRateLimiterTimeoutUnderLoad` contains a timing assertion (`elapsedMs < 1000`) which is a performance-only assertion on `executeWithTimeout` — it verifies promptness but not correctness. This is acceptable context for a timeout-behavior test.
- **Recommended fix:** none.

---

## Summary

| File | Verdict | Key Issue |
|------|---------|-----------|
| OAuthSecurityEdgeCaseTests.kt | CLEAN | Strong isolation and round-trip assertions |
| OwaspSecurityTest.kt | SUSPECT | Config tests only assert `.name`; path-traversal OR-condition passes stubs |
| PathTraversalDeepTests.kt | SUSPECT | ~28/42 tests assert only `startsWith(rootPath)` — segment content never verified |
| SecurityEventLoggerTests.kt | CLEAN | Exact service name, details map, and eviction-order assertions |
| SecurityValidationTests.kt | **BLUFF** | `assertNoRawScript` never inspects the `html` arg — 13 XSS tests pass with raw output |
| BadgeTinterTest.kt | CLEAN | Exact color value + gate-disabled null assertion |
| FormatEnablementDefaultTest.kt | CLEAN | Exact set equality `{markdown}` |
| FormatEnablementGateTest.kt | CLEAN | `assertFailsWith` + `assertEquals(formatId)` |
| GrammarRegistryTest.kt | CLEAN | Exact id equality + gate-disabled null path |
| ScopeMapperTest.kt | CLEAN | 25+ exact string assertions from production map |
| VsCodeThemeParserTest.kt | CLEAN | Exact ARGB values from JSON parsing |
| AccessibilityComprehensiveTests.kt | SUSPECT | ~25 "does not throw" tests for announce/focus; no dispatch verification |
| AccessibilityTest.kt | **BLUFF** | `assertTrue(isActive \|\| !isActive)` tautology at line 209 |
| AccessibilityTests.kt | SUSPECT | ~15 "does not throw" tests for announce/focus; no dispatch verification |
| AnimationConstantsTest.kt | **BLUFF** | ~12 `assertNotNull(transition)` tests pass with `EnterTransition.None` stub |
| AnimationsTests.kt | CLEAN | Exact constant values and Spring param assertions dominate |
| AnimationTests.kt | **BLUFF** | 14 `assertNotNull(ScreenTransitions.X())` tests; no duration/direction verification |
| ThemeWcagContrastTest.kt | CLEAN | WCAG ratio computed from real palette values |
| UIComponentsCoverageTest.kt | SUSPECT | ~30 `assertNotNull(Modifier.X())` tests; no semantic property verification |
| ConcurrencySafetyTest.kt | CLEAN | Exact count assertions under real Dispatchers.Default parallelism |
| FileStorageContractTests.kt | SUSPECT | `writeBytes` result not read back; JVM-only `System.getProperty` in commonTest |
| LazyLoadingTest.kt | CLEAN | Content equality and exact size/range assertions |
| MemoryLeakDetectionTest.kt | **BLUFF** (partial) | Most tests verify ad-hoc in-test objects, not production code |
| PlatformSyncTest.kt | CLEAN | Return value, exception message, and side-effect assertions |
| RateLimitingTest.kt | CLEAN | Concurrency limit enforcement and count-exact throttle assertions |
| StressAndIntegrationTest.kt | CLEAN | Exact count violations, memory zero after clear, registry isolation |
