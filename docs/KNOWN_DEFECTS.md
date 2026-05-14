# Known Defects

Defects discovered by the anti-bluff campaign (CONST-035) that have been
documented in code but not yet fixed because the proper fix has a non-
trivial dependency. Each ticket lists the symptom, the proper fix, and the
blocker. Anyone closing a ticket here must also remove the corresponding
SKIP-OK exemption(s) from the affected test(s) so the regression guard is
re-armed.

## #phase-7-blocked-on-ios-baseline — NEW iter-57 Phase 7

**Symptom**
`./gradlew :shared:compileKotlinIosArm64` (and therefore every iOS
target the `shared` module fans out to) fails at the compile stage of
the sibling submodule `:Document-KMP:compileKotlinIosArm64`. The
exact upstream errors (reproduced on master tip `c0bf3305`):

```
e: file:///Users/milosvasic/Projects/Document-KMP/src/iosMain/kotlin/digital/vasic/document/Document.ios.kt:9:50
   This declaration needs opt-in. Its usage must be marked with
   '@kotlinx.cinterop.ExperimentalForeignApi' or
   '@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)'
e: file:///Users/milosvasic/Projects/Document-KMP/src/iosMain/kotlin/digital/vasic/document/Document.ios.kt:10:30
   Unresolved reference 'objectForKey'.
e: file:///Users/milosvasic/Projects/Document-KMP/src/iosMain/kotlin/digital/vasic/document/Document.ios.kt:17:50
   This declaration needs opt-in. Its usage must be marked with
   '@kotlinx.cinterop.ExperimentalForeignApi' or
   '@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)'
e: file:///Users/milosvasic/Projects/Document-KMP/src/iosMain/kotlin/digital/vasic/document/Document.ios.kt:18:31
   Unresolved reference 'objectForKey'.
```

`Document-KMP` is one of the 10 sibling KMP modules consumed via
`includeBuild()` from `settings.gradle.kts` (path: `../Document-KMP`).
Per CONST-038 (sibling submodule decoupling) Yole cannot patch
sibling submodule source from this repo.

**End-user impact**
Phase 7 of iter-57 cannot deliver the Tree-Sitter Kotlin/Native iOS
actual. iOS users see plain text (no syntax highlighting) — the
editor falls back gracefully per spec §4 "Engine load failed at
startup". No fake tokens are emitted (CONST-035 honoured).

**Discovered by**
Iter-57 Phase 7 implementation. `./gradlew :shared:compileKotlinIosArm64`
on clean master tip `c0bf3305` (after Phase 6 closeout). Same failure
reproduces on a freshly stashed working tree — predates any Phase 7
changes.

**Why not fix Document-KMP.ios.kt directly?**
1. CONST-038: sibling submodule decoupling — `Document-KMP` lives at
   `/Users/milosvasic/Projects/Document-KMP/` and is consumed via
   `includeBuild()`. Its git history is independent; patching from
   inside Yole would violate the decoupling contract.
2. The fix is small (`@OptIn(ExperimentalForeignApi::class)` +
   replace `NSDictionary.objectForKey(...)` with
   `valueForKey(...)`-equivalent or the right `@OptIn` import) but
   must land upstream first.

**Proper fix (operator action on `Document-KMP`)**
On a branch of `Document-KMP`:
1. Edit `src/iosMain/kotlin/digital/vasic/document/Document.ios.kt`
   - Add `@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)`
     at the top of the file.
   - Replace the two `objectForKey(...)` calls with the correct
     `platform.Foundation.NSDictionary` API (likely
     `valueForKey(...)` or cast to typed accessor) — exact API
     depends on the source intent which lives in that repo.
2. Commit + push the upstream fix.
3. Re-run `./gradlew :Document-KMP:compileKotlinIosArm64` from Yole;
   it should succeed.
4. Re-run `./gradlew :shared:compileKotlinIosArm64` — Phase 7 can
   then proceed.

**Phase 7 disposition while blocked**
- `shared/src/iosMain/cinterop/tree-sitter.def` is **scaffolded with
  commented-out directives** documenting the linking strategy from
  Phase 0 research §2.2/§2.3/§2.4 — useful as soon as the upstream
  unblocks.
- `shared/src/iosMain/kotlin/digital/vasic/yole/syntax/TokenizerEngine.ios.kt`
  remains an honest `NotImplementedError` stub (same as Phase 5
  shipped); a large header comment block names this defect.
- The static libraries (`libtree-sitter.a` +
  `libtree-sitter-markdown.a`) for the three iOS slices remain a
  separate OPEN spike from Phase 0 §2.6 — operator-built XCFrameworks.
  That spike has NOT been performed either; it would have been
  pointless before this baseline unblocks.

**Blocker**
Upstream `Document-KMP` operator fix. Estimated ~30 min wallclock
once the operator opens the repo.

**Anti-bluff disposition (CONST-035)**
Honest. The iOS `TokenizerEngine.initialize()` returns
`Result.failure` with an explicit ticket reference in the error
message. No fake tokens, no `PASS` claimed for iOS in any
Challenge or test. Phase 7 status is officially BLOCKED in
`docs/CONTINUATION.md`.

**Exit criteria**
1. `:Document-KMP:compileKotlinIosArm64` compiles cleanly on a fresh
   clone.
2. `:shared:compileKotlinIosArm64` compiles cleanly.
3. Operator provides `libtree-sitter.a` + `libtree-sitter-markdown.a`
   static libs at `shared/src/iosMain/nativeLibs/{ios_arm64,
   ios_simulator_arm64,macos_arm64}/` (Phase 0 §2.6 spike).
4. `shared/src/iosMain/cinterop/tree-sitter.def` directives are
   uncommented + wired into `shared/build.gradle.kts` via
   `cinterops.create("tree_sitter") { ... }`.
5. `TokenizerEngine.ios.kt` actual replaced with real cinterop
   bindings calling `ts_parser_new` /
   `ts_parser_set_language` / `ts_parser_parse_string` /
   `ts_tree_root_node` / cursor walk.
6. On a real iOS device or simulator, the future
   `tokenizer_ios_real_tokens_challenge.sh` (Phase 12) runs
   `TokenizerEngine.tokenize("# Heading", "markdown")` and asserts
   `tokens.size >= 5` with a non-blank first scope — same bar as
   Desktop `TokenizerEngineJvmTest.tokenizesMarkdownSnippet`.

---

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

## #wasmjs-test-baseline-broken — NEW iter-57 Phase 6

**Symptom**
`./gradlew :shared:compileTestKotlinWasmJs` (and therefore
`:shared:wasmJsBrowserTest`) fails at the compile stage because ~50
files under `shared/src/commonTest/` import `kotlinx.coroutines.runBlocking`,
which does not exist on the `wasmJs` target (only `kotlinx-coroutines-test`
provides a runtime, and that has no WASM variant per `gradle/libs.versions.toml`
comments + `shared/build.gradle.kts` wasmJsTest source set). The
target was therefore never able to execute a single test on master —
this is a baseline state that pre-dates iter-57 Phase 6.

**Discovered by**
Iter-57 Phase 6 implementation. The new `TokenizerEngineWasmTest.kt`
compiles cleanly in isolation (`grep TokenizerEngineWasmTest` against
the compile error list returns zero hits); the pre-existing
~11,000 errors are all in commonTest source files that use
`runBlocking` directly. Verified via `git stash && :shared:compileTestKotlinWasmJs`
on clean master tip `2eafc2de` — same compile failure, predating
any Phase 6 changes.

**End-user impact**
None for production. The webApp ships fine via
`:shared:compileKotlinWasmJs` (main code, no test sources) which
DOES succeed. Only the in-browser test runner is unavailable.

**Proper fix (choose ONE)**
(a) **Migrate all commonTest `runBlocking` calls to suspend test bodies**
    using `kotlin.test.Test` + a Promise/runTest equivalent that is
    target-agnostic. The cleanest path is `runTest` from
    `kotlinx-coroutines-test`, gated behind a `wasmJsTest` exclusion
    that re-routes to a custom `GlobalScope.promise { }` adapter on
    Wasm. Estimated 4-8 hours.
(b) **Move all commonTest `runBlocking` files to `jvmTest`** (i.e.
    `desktopTest` + `androidUnitTest`) where the runtime is available.
    Cleanest split but requires moving ~50 files. Estimated 2-4 hours.
(c) **Suppress wasmJsTest compilation entirely** via
    `kotlin.sourceSets["wasmJsTest"].kotlin.exclude(...)`. Quickest
    workaround; means iter-57 Phase 6's `TokenizerEngineWasmTest`
    cannot run either. Not recommended.

**Blocker**
Pre-existing baseline, larger than Phase 6 scope. Phase 6 ships the
real Wasm tokenizer code (compiled, verified clean) and the matching
test source; running the test in-browser requires this ticket's fix.

**Anti-bluff disposition (CONST-035)**
Honest. The Phase 6 commit explicitly documents this constraint;
no fake tokens or fake PASS were emitted. The Wasm actual replaces
the Phase 5 `Result.failure` stub with a real vscode-textmate-backed
implementation. The test source is shipped, compiles cleanly in
isolation, and will execute as soon as this baseline ticket closes.

**Exit criteria**
`./gradlew :shared:wasmJsBrowserTest --tests "*TokenizerEngineWasmTest*"`
runs and the test asserts `tokens.isNotEmpty()` against
`# Heading\n\nA paragraph.\n`. Mutation step: stub the inner
`for (i in 0 until tokenCount)` loop in
`TokenizerEngine.wasmJs.kt::tokenize` to a no-op; the test MUST fail.

---

## #android-tree-sitter-ndk-so-missing — RESOLVED 2026-05-14 (post-Phase 13)

**Resolution** (forensic anchor — operator-built Android NDK fix landed):

Two changes in shared/build.gradle.kts + shared/src/androidMain/ +
shared/native/android-tree-sitter/:

1. **Android NDK .so files built and committed.**
   `shared/native/android-tree-sitter/{arm64-v8a,armeabi-v7a,x86_64}/lib{tree-sitter,tree-sitter-markdown}.so`
   are compiled from upstream tree-sitter v0.22.6 C sources +
   ikatyang/tree-sitter-markdown v0.7.1 grammar (parser.c +
   scanner.cc) via NDK r29 clang/clang++ targeting
   `aarch64-linux-android21`, `armv7a-linux-androideabi21`, and
   `x86_64-linux-android21`. The JNI glue is bonede's own
   `org_treesitter_TSParser.c` + `org_treesitter_TreeSitterMarkdown.c`
   at the v0.22.6 tag (so the JNI ABI matches the published Java
   classes). All three ABIs verified ELF-correct with `file` and
   exports correct JNI symbols (`llvm-nm -D | grep Java_org`).

2. **`org.treesitter.utils.NativeUtils` replaced for Android.**
   The bonede 0.22.6 NativeUtils on Android tries to read
   `lib/aarch64-linux-gnu-tree-sitter.so` from classpath (the JAR
   only ships a glibc binary — won't dlopen on bionic), writes it
   to `${user.home}/.tree-sitter/` (unwritable in app sandboxes),
   and CRC-overwrites any operator-placed Android .so. A Yole-written
   drop-in replacement at
   `shared/native/android-tree-sitter/java/org/treesitter/utils/NativeUtils.java`
   (same FQCN, same `loadLib(String)` signature, JDK 11 bytecode)
   detects Android at static-init via `java.vm.vendor` / Dalvik / ART
   and routes through `System.loadLibrary(name)`. The Android linker
   resolves to `<apk>/lib/<abi>/lib<name>.so` placed there by AGP via
   the shared module's `android.sourceSets.main.jniLibs.srcDirs`
   pointing at `shared/native/android-tree-sitter/`. On Desktop /
   Server JVMs the replacement preserves bonede's
   extract-classpath + System.load-absolute flow byte-for-byte.

   The swap is performed by a Gradle task family
   (`compileYoleAndroidNativeUtils` + `repackageTreeSitterJarForAndroid`
   + `repackageTreeSitterMarkdownJarForAndroid`) at build time:
   resolves the bonede JARs from Maven, replaces
   `org/treesitter/utils/NativeUtils.class` with the Yole-compiled
   one, emits the patched JARs to `shared/build/repackaged-libs/`.
   The Android source set depends on those files() outputs (with the
   explicit Task#dependsOn wiring AGP 8.9 requires) instead of the
   raw Maven coordinates.

**Verification**
- `:shared:desktopTest --tests "TokenizerEngineJvmTest"` continues to
  pass 5/5 — Desktop is unaffected by the Android-only repackage.
- `:shared:compileDebugKotlinAndroid` + `:androidApp:compileDebugKotlin`
  + `:androidApp:compileDebugAndroidTestSources` + `:androidApp:assembleDebug`
  all succeed; APK now contains:
  ```
  lib/arm64-v8a/libtree-sitter.so          (246 536 B)
  lib/arm64-v8a/libtree-sitter-markdown.so (519 144 B)
  lib/armeabi-v7a/libtree-sitter.so        (246 312 B)
  lib/armeabi-v7a/libtree-sitter-markdown.so (465 644 B)
  lib/x86_64/libtree-sitter.so             (251 360 B)
  lib/x86_64/libtree-sitter-markdown.so    (485 816 B)
  ```
  (each `file ...so` reports the correct Android ABI ELF triple).
- `:androidApp:connectedDebugAndroidTest --tests "TokenizerEngineAndroidTest"`
  exercises:
  - `initializeSucceedsOnAndroidDevice` — Engine.initialize() returns
    `Result.success` on a live emulator (proves the .so dlopens via
    System.loadLibrary on bionic).
  - `tokenizesMarkdownSnippetOnDevice` — tokenize("# Heading\n\nA paragraph.\n",
    "markdown") emits ≥ 5 tokens with a non-blank first scope and at
    least one token whose end-byte reaches into the paragraph (proves
    the byte-range output truly tracks the input — bluff guard).
  - `tokenizesReentrantOnSameEngine` — re-tokenizing the same input
    yields identical scope sequences (catches use-after-close /
    state-corruption regressions).
- The exit-criteria bar matches `TokenizerEngineJvmTest.tokenizesMarkdownSnippet`.

**Why this beats alternatives investigated 2026-05-14:**
- `com.itsaky.androidide.treesitter` — archived 2024-10-18; no
  markdown grammar published.
- `io.github.tree-sitter:jtreesitter` — requires JDK 23 (FFM); Yole
  desktop pins JDK 11.
- Operator-built lib via raw bonede JAR — the classpath-CRC-overwrite
  flow destroys any placed .so file at first runtime use.
- Vendor lib in jniLibs without the NativeUtils swap — bonede's static
  initialiser still triggers extract-and-System.load of the linux-gnu
  binary which fails on bionic.

**Architecture coverage:** arm64-v8a, armeabi-v7a, x86_64. Sticks with
bonede 0.22.6 (the version pinned in libs.versions.toml) since
bumping introduces TSParser API surface drift that requires Desktop
test churn.

---

---

## #linux-build-host-jdk-jmods-bootstrap — NEW iter 54

**Symptom**
The dedicated Linux x86_64 build host (hostname pinned in `.env`
under `LINUX_BUILD_HOST` — gitignored, NEVER hardcoded in tracked
code or docs) carries an ALT Linux `openjdk-21-alt1` package that
does NOT ship a `jmods/` directory. Compose Desktop's
`createRuntimeImage` (jlink) fails with
`Error: --module-path is not specified and this runtime image does
not contain jmods directory.` Build cannot produce the Linux .deb
artifact until the host has a JDK distribution with jmods.

**Discovered by**
Iter-54 attempt to produce
`Yole-Desktop-linux-x64-1.0.1-Release-0.0.0.1.1.deb` on the
configured build host. Logs in
`~/Yole/desktopApp/build/compose/logs/createRuntimeImage/jlink-*.err.txt`
on the host.

**Proper fix (choose ONE)**
(a) Copy a Temurin 17 tarball from the macOS audit host via scp,
    extract under `~/jdk17/`, set `JAVA_HOME` to point at it
    before invoking gradlew. (Recommended — user-level, no sudo,
    no host network access to github.com needed.)
(b) Mirror a Debian/Ubuntu apt repo onto the host's filesystem
    that ships `openjdk-21-jdk` (full distribution incl. jmods).
    More invasive.
(c) Install JDK via the host's package manager
    (`apt-get install java-21-openjdk-devel` or equivalent) if such
    a package exists. Requires sudo → forbidden by CONST.
(d) **Now also available**: use the Containers submodule's
    `LinuxContainerBackend` (commits `5059c75` + iter-54
    follow-up) — runs the build inside a JDK-17 + Gradle Linux
    container on any host with rootless podman/docker. Removes the
    dependency on the build-host's system JDK entirely. See
    `Submodules/Containers/docs/crossbuild/linux-image-provisioning.md`.

**Blocker**
The build host cannot currently resolve
`release-assets.githubusercontent.com` (DNS/firewall), so a direct
curl-the-Temurin-tarball approach failed. The scp-from-mac path
needs the operator to either grant that DNS resolution or sit
through one scp copy. The container path (option d) sidesteps the
issue entirely once the operator provisions the
`crossbuild-linux:jdk17-amd64` image once.

---

## #crossbuild-windows-image-provisioning — NEW iter 54

**Symptom**
`pkg/crossbuild/WineContainerBackend.Build()` (in Containers
submodule, commit `5059c75`) returns an actionable error pointing
at this ticket when the `ghcr.io/vasic-digital/crossbuild-wine:latest`
image is not present on the host. The orchestration code + tests
are complete; the image itself is operator-provisioned per
`Submodules/Containers/docs/crossbuild/windows-image-provisioning.md`.

**Bluff classification**
None — Backend honestly fails with a clear pointer to the
provisioning steps rather than silently returning a stub artifact.
This ticket is the documented SKIP-OK marker for the real-stack
Windows-build Challenge until provisioning completes.

**Proper fix**
On a Linux x86_64 host with rootless podman:

```
cd Submodules/Containers/pkg/crossbuild
podman build -t ghcr.io/vasic-digital/crossbuild-wine:latest \
    -f windows_wine.Containerfile .
podman run --rm ghcr.io/vasic-digital/crossbuild-wine:latest gradle --version
```

Then re-run the crossbuild_windows_msi_challenge.sh; it should
produce a real .msi.

**Blocker**
Operator must perform the provisioning. The configured Linux build
host (`.env` `LINUX_BUILD_HOST`) has rootless podman + sufficient
disk to host the image, so it can serve as the provisioning host
once `#linux-build-host-jdk-jmods-bootstrap` unblocks (or earlier
via option (d) above — running the provisioning steps inside the
already-functional `crossbuild-linux` image, bootstrap-style).

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
