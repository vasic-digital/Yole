# Known Defects

Defects discovered by the anti-bluff campaign (CONST-035) that have been
documented in code but not yet fixed because the proper fix has a non-
trivial dependency. Each ticket lists the symptom, the proper fix, and the
blocker. Anyone closing a ticket here must also remove the corresponding
SKIP-OK exemption(s) from the affected test(s) so the regression guard is
re-armed.

## #smb-stub-no-negotiation — CLOSED 2026-05-07 (commit `1f6472c9`)

**Resolution:** `SmbService.connect()` rewritten to perform real SMB
protocol negotiation + authentication; `_isConnected = true` only set
after both succeed. Test lambda injection (`testConnectFn` /
`testAuthenticateFn`) lets unit tests script connect/authenticate
outcomes per-case. 441/441 SMB + WebDAV tests pass after the fix.
See `CONTINUATION.md` §4 CLOSED list for canonical record.

---

## #webdav-always-online-stub — CLOSED 2026-05-07 (commit `1f6472c9`)

**Resolution:** Removed the catch block in `WebDavService.connect()`
that suppressed network errors and forced `_isConnected = true`.
`isOnline` now honestly reflects reachability per CONST-035. Same
commit + test infrastructure as SMB fix; covered by the 441/441
SMB+WebDAV test pass count.
See `CONTINUATION.md` §4 CLOSED list for canonical record.

---

## #fallback-tier-removed-needs-httptest-fixture — NEW iter 53

**Symptom**
~75 `TestGetCapabilities` / `Test*Provider_GetCapabilities` tests across
LLMProvider's 30+ provider packages assert that `caps.SupportedModels`
contains specific hardcoded model names (e.g. "llama-3.3-70b",
"glm-4.6"). The values come from each provider's `FallbackModels` list,
which was the Tier 3 discovery fallback. Plus 4 internal discovery tests
that exercised Tier 3 directly.

**Bluff classification**
Structural — assertions test a hardcoded list, not the discovery wiring.
Drift when the upstream catalogue evolves silently breaks the test
without anything in our codebase changing (e.g. Venice retired
"venice-uncensored" → `TestGetCapabilities` went red in iter-52 raw
sweep without a single line of our code having changed).

**Discovered by**
Iter-53 CONST-036 enforcement attempt: stripping the Tier 3 runtime path
in `pkg/discovery/discovery.go` immediately exposed 75 latent failures.
That count is itself the auditable evidence that the hardcoded lists
were structural bluffs.

**Proper fix**
For each affected `TestGetCapabilities`: replace
`NewProvider("k", "", "")` with an `httptest.NewServer` returning a known
catalogue, then assert against THAT catalogue. The pattern is already
shipped for `pkg/providers/ollama/ollama_test.go::TestOllamaProvider_GetCapabilities`
and `pkg/providers/venice/venice_test.go::TestGetCapabilities` — iter-53
commit `2e465c4` and the preceding LLMProvider commit `c3bccd7`. Each
remaining provider follows the same pattern.

**Blocker**
~75 individual provider tests + 4 discovery-internal tests
(`TestDiscoverModels_Tier1_APIFails_FallsToTier3`,
`TestDiscoverModels_Tier1_EmptyResponse_FallsToTier3`,
`TestDiscoverModels_NoAPIKey_SkipsTier1`, `TestGetCachedModels_Empty`).
Multi-iteration scope.

**Exit criteria**
Every `TestGetCapabilities` uses an httptest server (or honest
`SKIP-OK: #fallback-tier-removed-needs-httptest-fixture` if the
provider's discovery is intentionally unmocked). Then the Tier 3 runtime
path in `pkg/discovery/discovery.go` can be removed and the
`FallbackModels` field can be deleted from `ProviderConfig`. The raw
strip log at
`docs/qa/iter-52/submodule-llmprovider-tier3-strip.log` shows the
expected pre-fix test output.

---

## #robolectric-compose-ui-tests-brittle

**Symptom**
~25 Robolectric Compose UI tests in `androidApp/src/test/kotlin/.../robolectric/`
match against runtime-evolving UI strings (`onNodeWithText("Start typing...").performTextInput(...)`,
`assertIsDisplayed`). Every UI string change or composition reorder causes
flap. Tests have been passing locally then failing in container builds
because of subtle composition timing differences.

**Discovered by**
The clean container-release build (iter 26) — the tests had been
silently broken, picked up only when the build script's full
`run_tests` step (re-enabled by removing `SKIP_TESTS=1`) actually ran
them.

**Proper fix**
Migrate these Robolectric UI tests to on-device automation via HelixQA
(which is already the project's primary UI testing path per
`CLAUDE.md` — "tests must validate user-visible behaviour"). Once
HelixQA covers the same ground (most likely already does), delete the
Robolectric copies. Alternatively, switch matching from string-based
to test-tag based (`Modifier.testTag(...)` plus `onNodeWithTag(...)`)
so renames don't break tests.

**Blocker**
Multi-day work: identify which Robolectric tests have HelixQA equivalents,
remove duplicates, port the rest to test-tag matching, update test
helpers. Out of scope for any single iteration.

**Exemptions in build config** (must be removed when this is closed):
- `androidApp/build.gradle.kts` — `tasks.withType<Test>().configureEach`
  excludes `"*.robolectric.*"`. Search for `SKIP-OK:
  #robolectric-compose-ui-tests-brittle`.

---

## #yole-json-parser-missing — FIXED iter 42 (2026-05-13)

**Symptom (historical)**
`FormatRegistry.formats` advertised `ID_JSON` (a TextFormat with id
`json`) but `ParserInitializer.registerAllParsers()` /
`registerAllParsersLazy()` registered no JSON parser. Users tapping
on a `.json` file saw Plain-Text rendering instead of a JSON-aware
view.

**Discovered by**
Iter-39 — `IntegrationTest.testParserRegistryCompleteness`:

```
java.lang.AssertionError: No parser registered for format JSON (json)
```

**Fix applied (iter 42, commit see CONTINUATION.md §26)**
Created `shared/src/commonMain/kotlin/digital/vasic/yole/format/json/JsonParser.kt`:
- Implements `TextParser`.
- `parse(content)` pretty-prints with 2-space indent, builds HTML with
  `<span class='json-{key|string|number|bool|null|bracket}'>` tokens
  for stylesheet-driven syntax highlighting.
- HTML-sensitive characters (`<`, `>`, `&`, `"`) are escaped via
  `escapeHtml()` so a JSON string `"a<b>"` renders as `&quot;a&lt;b&gt;&quot;`
  inside its span, never as live HTML.
- `validate(content)` reports unbalanced braces / brackets /
  unterminated strings without throwing.
- Parser tolerates malformed input: pretty-printing returns the raw
  string on failure with an entry in `errors`. No exception escapes.

Wired into `ParserInitializer` (both eager + lazy paths). The
`registerAllParsers registers all N format parsers` test counts
updated 17 → 18.

**Verification (positive runtime evidence per CONST-035 §11.4.2)**
- `docs/qa/iter-42/desktopTest-JsonParser-51-pass.log` — 10 dedicated
  JsonParserTest cases + 41 ParserInitializerTest cases all pass on
  host JVM (51 PASS / 0 FAIL).
- `docs/qa/iter-42/adb-IntegrationTest-19-pass.log` — 19/19
  IntegrationTest pass on device with json REMOVED from the
  `knownGaps` allowlist (the test now strictly asserts JSON has a
  parser; previously it allowed the gap).
- `docs/qa/iter-42/connectedDebugAndroidTest-iter42.xml` — full
  76-test Gradle run, `tests="76" failures="0" errors="0" skipped="17"`.

---

## #yole-todotxt-compound-extension-detection — FIXED iter 40 (2026-05-13)

**Symptom (historical)**
`FormatRegistry.detectByFilename("todo.txt")` returned PLAIN TEXT
instead of TODO.TXT even though TodoTxt advertises `.todo.txt` as an
extension. End-user impact: a file named `todo.txt` (the canonical
Todo.txt filename) opened without Todo.txt highlighting.

**Discovered by**
Iter-39 — `IntegrationTest.testFormatDetectionIntegration` initially
asserted `detectByFilename("todo.txt") == todotxt` and failed:

```
java.lang.AssertionError: Todo.txt detection regression: 'todo.txt'
  resolved to plaintext instead of todotxt
```

**Root cause (forensic)**
`FormatRegistry.detectByFilename` only tried suffixes anchored at the
FIRST `.` in the filename, so for `todo.txt` it never tested whether
the WHOLE filename (preceded by a `.`, i.e. `.todo.txt`) matched any
advertised extension. Two formats claimed `.txt` (PlainText + TodoTxt);
PlainText won by registration order.

**Fix applied (iter 40, commit see CONTINUATION.md §24)**
Three-pass algorithm in `detectByFilename`:
1. **Whole-filename match** — try `"." + filename` against every format's
   extensions list. For `todo.txt`, this checks `.todo.txt` directly →
   matches TodoTxt.
2. **Compound-extension longest-first** — iterate dot-positions
   left-to-right (earlier positions yield longer suffixes) and try
   each. Closes `<prefix>.todo.txt → todotxt`.
3. **Bare-extension fallback** — preserves the prior contract via
   `detectByExtension`.

Generic `.txt` filenames (`notes.txt`, `scratch.txt`) still resolve to
PlainText because there is no whole-filename or compound match — the
fallback takes over and PlainText is first.

**Verification**
- New paired tests in `shared/src/commonTest/.../FormatRegistryStressTest.kt`:
  `detectByFilename resolves todo dot txt to todotxt not plaintext`
  + `detectByFilename resolves prefixed todo dot txt to todotxt`. Both PASS.
- All 140 FormatRegistry tests across StressTest / EdgeCaseTest /
  UnitTest / LazyInitStressTest pass without regression.
- `IntegrationTest.testFormatDetectionIntegration` strengthened to
  assert `todo.txt → todotxt` AND `work.todo.txt → todotxt` AND
  `notes.txt → plaintext` (the expected behaviors after the fix).

---

## #yole-firebase-remote-config-fetch-crash — FIXED iter 41 (2026-05-13)

**Symptom (historical)**
`FirebaseUtil.fetchRemoteConfig` unconditionally accessed `task.result`
in the `addOnCompleteListener` callback. When the Firebase fetch
failed (e.g. Firebase Installations Service unreachable due to
network issues, blocked DNS, or unauthorised emulator AVD),
`task.result` threw a `RuntimeExecutionException`. The exception
propagated to the main Looper and **crashed the entire process** on
every RC fetch failure.

End-user impact: any user on poor / intermittent / restricted
network — corporate firewall blocking Firebase, offline-mode usage,
travel — would experience a hard crash on app launch within seconds
of `MainActivity.onCreate`. **Severe defect.**

**Discovered by**
Iter-41 (2026-05-13) — `IntegrationTest.testCsvParserIntegration` ran
on the emulator with Firebase Installations Service unreachable. The
test runner reported:

```
Process crashed while executing testCsvParserIntegration:
com.google.android.gms.tasks.RuntimeExecutionException:
com.google.firebase.remoteconfig.FirebaseRemoteConfigClientException:
  Firebase Installations failed to get installation auth token for fetch.
    at digital.vasic.yole.android.util.FirebaseUtil.fetchRemoteConfig$lambda$5(FirebaseUtil.kt:171)
Caused by: com.google.firebase.installations.FirebaseInstallationsException:
  Firebase Installations Service is unavailable. Please try again later.
```

This was a latent bug — the iter-30 instrumentation that wrote the
RC fetch path only handled the happy path. The fix is in
`FirebaseUtil.kt:169-198`:

- Always check `task.isSuccessful` BEFORE accessing `task.result`.
- If the task failed, log `task.exception` (the proper failure
  channel) and treat `activated` as `false`.
- Wrap `task.result` in a `try/catch` even on success-path because
  `RuntimeExecutionException` can theoretically still be thrown.

**Verification (positive runtime evidence per CONST-035 §11.4.2)**
- `docs/qa/iter-41/adb-IntegrationTest-pre-fix-CRASH.log` — pre-fix
  crash trace showing `RuntimeExecutionException` and `Process crashed`.
- `docs/qa/iter-41/adb-IntegrationTest-19-pass.log` — post-fix, all
  19 IntegrationTest cases pass without crash.
- `docs/qa/iter-41/gradle-fullsuite.log` — `BUILD SUCCESSFUL in 2m 1s`
  with 59 PASS / 17 SKIP-OK / 0 FAIL across all 76 instrumented tests
  (was 26 with process crash interrupting the run pre-fix).

---

## #yole-android-formats-settings-section-removed — CLOSED iter 49 (2026-05-13, tests deleted)

**Historical symptom**
Two YoleAppTest methods (`testFormatRegistryIntegration`,
`testFormatInformationDisplay`) targeted a Settings-screen surface
that listed every supported text format — "Formats" section header,
"Supported formats: N" count line, and per-format display names. The
iter-27 Settings layout has no such section.

**Resolution (iter 49)**
Both YoleAppTest cases DELETED. Data-layer equivalent is preserved
by `IntegrationTest.testFormatRegistryIntegrationWithUI` +
`testParserRegistryCompleteness`. If the Formats UI surface is
restored to Settings in a future product iteration, write FRESH
UI-layer tests for the new surface — do not resurrect the deleted
methods (which targeted specific removed labels). git history at
SHAs prior to iter 49 has the original bodies.

---

## #yole-android-fab-new-file-flow-removed — CLOSED iter 49 (2026-05-13, tests deleted)

**Historical symptom**
Six instrumented tests (4 in YoleAppTest, 2 in EndToEndTest)
targeted a UI flow that no longer exists in the shipped build:
a global FAB (`onNodeWithContentDescription("Add")`) that, when
tapped from the Files screen, opened an editor sub-screen titled
`"Editing: untitled.txt"` with a `"Back"` content-description in
the top app bar. The iter-27 redesign removed this entry path
entirely.

Affected tests (all deleted iter 49):
- `YoleAppTest.testFloatingActionButtonFunctionality`
- `YoleAppTest.testFileBrowserBasicFunctionality`
- `YoleAppTest.testEditorScreenNavigation`
- `YoleAppTest.testScreenNavigationWithAnimations`
- `EndToEndTest.testCompleteFileEditingWorkflow`
- `EndToEndTest.testErrorRecoveryWorkflow`

**Resolution (iter 49)**
All 6 cases DELETED per the "do EVERYTHING / no-bluff-policy
everywhere" user mandate. The features are confirmed-removed by
iter-27 redesign and the data-layer / multi-screen state-
preservation invariants the tests claimed to verify are already
covered by other rewritten tests (e.g. `testPerformanceUnderLoad`,
`testCompleteUserJourney`, `testFormatRegistryIntegrationWithUI`).
If a new file-creation entry point is added later (e.g. a menu item
under More, or a long-press on a folder chip), write fresh tests
for the **new** flow under a fresh test method name — do not
resurrect these six. git history at SHAs prior to iter 49 has the
original bodies.

---

## #yole-android-gradle-utp-single-class-filter — FIXED iter 38 (2026-05-13)

**Symptom (historical)**
`./gradlew :androidApp:connectedDebugAndroidTest` emitted XML / HTML
results for **only one** test class (`YoleAppTest`) even though the
APK contained five (`YoleAppTest`, `IntegrationTest`, `EndToEndTest`,
`SaveTests`, `FirebaseIntegrationTests`). Gradle exited 0 + reported
BUILD SUCCESSFUL, so a casual observer would conclude that 26 PASS /
8 SKIP-OK across one class was the entire suite. It was not: the
other four classes' 23 PASS + 19 SKIP-OK were silently dropped from
the Gradle report.

**Discovered by**
Iter-38 (2026-05-13). Direct adb invocation (`adb shell am instrument
-w -e class digital.vasic.yole.android.ui.IntegrationTest …`)
verified that `IntegrationTest` ran 12 tests, `EndToEndTest` 1 test,
`SaveTests` 5 tests, `FirebaseIntegrationTests` 5 tests — all
returning `OK` exit codes with full per-test PASS output. Persisted
evidence: `docs/qa/iter-38/adb-*.log`. Root cause visible in
`androidApp/build/outputs/androidTest-results/connected/debug/yole-test(AVD) - 14/utp.0.log`
where the AGP-generated UTP test plan contained:

```
args_map { key: "class" value: "digital.vasic.yole.android.ui.YoleAppTest" }
```

i.e. AGP was dispatching only one class even though no `--tests` flag
or `testFilter` selector was specified on the command line.

**Root cause (forensic)**
`tasks.withType<Test>().configureEach { filter { excludeTestsMatching("*.robolectric.*") } }`
in `androidApp/build.gradle.kts`. AGP 8.x makes
`DeviceProviderInstrumentTestTask` (the type of `connectedDebugAndroidTest`)
extend `Test`, so the `withType<Test>` matcher swept in the connected-
test variant and its filter logic over-translated into UTP's `class`
arg_map narrowing, restricting the run to one class. Verified by
running with `-PincludeRobolectric=true` (which bypassed the filter
via the existing escape clause) — the very same APK + emulator
produced a full 76-test report with all 5 classes in the XML.

**Fix applied (commit see CONTINUATION.md §22)**
Scoped the filter to JVM unit-test tasks only via
`val isJvmUnitTest = name.endsWith("UnitTest")`. Robolectric tests
live in `androidApp/src/test/`, so their tasks are named
`testDebugUnitTest` / `testReleaseUnitTest` — unaffected by the
narrowing. Connected tasks (`connectedDebugAndroidTest`,
`connectedReleaseAndroidTest`) no longer match the predicate, so no
filter is applied to them and all 5 test classes dispatch normally.

**Verification (positive runtime evidence captured per CONST-035 §11.4.2)**
- `docs/qa/iter-38/adb-*.log` — direct adb instrumentation runs per
  class, all `OK (N tests)`.
- `docs/qa/iter-38/connectedDebugAndroidTest-fix-verified.xml` —
  Gradle XML after fix: `tests="76" failures="0" errors="0" skipped="27"`
  with all 5 classname values present (5 + 5 + 13 + 19 + 34 testcase
  entries).
- `docs/qa/iter-38/connectedDebugAndroidTest-fix-verified.log` — full
  Gradle stdout, BUILD SUCCESSFUL in 2m 21s.

---

## How CONST-035 catches stubs like these

This document exists because of the very pattern CONST-035 forbids:
test passes / Challenge passes, but the feature doesn't actually work
for an end user. Both stubs above were silently passing for months. The
iter-7 anti-bluff assertion (added during this campaign) caught both
within seconds of running on the actual rebuild.

If a future change introduces a similar stub without a paired
`KNOWN_DEFECTS.md` ticket, the next CONST-035 audit will catch it the
same way. That's the rule working as intended.
