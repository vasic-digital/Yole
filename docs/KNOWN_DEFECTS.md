# Known Defects

Defects discovered by the anti-bluff campaign (CONST-035) that have been
documented in code but not yet fixed because the proper fix has a non-
trivial dependency. Each ticket lists the symptom, the proper fix, and the
blocker. Anyone closing a ticket here must also remove the corresponding
SKIP-OK exemption(s) from the affected test(s) so the regression guard is
re-armed.

## #iter-71-launcher-icon-missing-postmortem — FIXED 2026-05-17

**Status:** FIXED in v1.9.1 (iter-71 emergency patch).

**Symptom**
Android launcher icon was absent (invisible) on all Android 8+ (API 26+)
devices from v1.4.0 (iter-59) through v1.9.0 (iter-71). Users who installed
the app saw no icon on their launcher grid — they could launch from the app
drawer or settings but not from the launcher home screen.

4+ tester builds were distributed with this defect:
v1.4.0, v1.5.0, v1.6.0, v1.7.0, v1.8.0, v1.9.0 (versionCode 140–190).

**Root cause**
`mipmap-anydpi-v26/ic_launcher.xml` (the adaptive icon definition) was
authored in iter-59 with `@mipmap/ic_launcher` as both the `foreground` and
`monochrome` layers:

```xml
<foreground android:drawable="@mipmap/ic_launcher"/>   <!-- WRONG -->
<monochrome android:drawable="@mipmap/ic_launcher"/>   <!-- WRONG -->
```

Android's adaptive-icon system (API 26+) expects a `@drawable/` vector for
the foreground and monochrome layers — not a mipmap PNG. The system clips
the foreground to the current launcher mask shape (circle, squircle, etc.).
Using a full-density PNG as the foreground causes the icon to be clipped
aggressively at the edges, often rendering as an invisible or very small
artifact depending on the launcher.

The `@mipmap/ic_launcher_round` was also absent from the Manifest
(`android:roundIcon` attribute not declared), causing round-mask launchers
(Google Pixel, Samsung One UI) to show no icon at all.

**Why anti-bluff didn't catch it**
Pre-iter-71 challenges and tests verified code paths execute and Kotlin tests
pass. None opened the produced APK binary and verified the launcher icon
resolves to actual displayable bytes on the target API level. This is a pure
CONST-039 failure mode — metadata-only verification (source file exists, APK
exists) is insufficient; the artifact must be opened and the icon chain
verified end-to-end.

**Fix (iter-71, 2026-05-17)**
1. Created `androidApp/src/main/res/drawable/ic_launcher_foreground.xml` —
   a proper 108dp × 108dp vector with the "Y" glyph centered in the 72dp
   safe zone, per the adaptive-icon spec.
2. Created `androidApp/src/debug/res/drawable/ic_launcher_foreground_dev.xml` —
   same glyph for the DEV (debug green) variant.
3. Rewrote `mipmap-anydpi-v26/ic_launcher.xml` (both main + debug) to
   reference `@drawable/ic_launcher_foreground*` instead of `@mipmap/ic_launcher`.
4. Created `mipmap-anydpi-v26/ic_launcher_round.xml` (both main + debug).
5. Added `android:roundIcon="@mipmap/ic_launcher_round"` to AndroidManifest.xml.
6. Added `androidApp/src/main/res/raw/keep.xml` to protect all icon assets
   from future resource shrinker passes.
7. Added ProGuard rules for R$ class preservation.
8. New challenge: `yole-challenges/scripts/installable_app_icon_challenge.sh`
   (3-layer verification: source-tree static + APK-open + vector integrity).
9. Challenge wired into `make qa-all` via `qa-iter-71-gates`.
10. CONST-039 updated with installable-asset evidence requirement.

**Prevention**
`make qa-all` now runs `installable_app_icon_challenge.sh` which opens the
packaged APK and verifies: ≥5 `application-icon-*` entries in `aapt2 dump
badging`, anydpi slots present for both `ic_launcher` and `ic_launcher_round`,
`drawable/ic_launcher_foreground` in resource table, each icon file ≥100 bytes,
and the vector XML has correct 108×108 viewport with a pathData element.

**Tracker for proper branding artwork**
`#iter-71-brand-vector-foreground-tracker` — the iter-71 fix uses a geometric
"Y" approximation as the foreground vector. The design team should provide a
proper brand SVG for conversion to a 108dp Android vector drawable. Until then
the geometric approximation ships.

**Tracker for Desktop DMG icon audit**
`#iter-71-desktop-dmg-icon-audit-pending` — Desktop macOS .icns was not
audited in iter-71 (no DMG present in releases/ at time of fix). A separate
challenge for Desktop icon verification is deferred to the next iteration that
produces a Desktop DMG.

**Tracker for Desktop DMG .icns file format**
`#iter-71-desktop-icns-format-defect` — audit of `Yole-Desktop-macos-arm64-1.9.0-Release-0.0.0.1.90.dmg`
reveals `Yole.app/Contents/Resources/Yole.icns` is a 512×512 PNG file
(magic bytes: `89 50 4E 47`, 60,940 bytes) not a proper multi-resolution
`.icns` container (which should begin with `69 63 6E 73`). macOS accepts
it in many contexts but the Finder may show a low-resolution icon at
Retina display sizes. Fix: generate a proper `.icns` with `iconutil` from
a 1024×1024 source. Tracked as a separate defect; not blocking the iter-71
emergency patch.

**Tracker for Web favicon audit**
`#iter-71-web-favicon-audit-pending` — Wasm/Web bundle favicon audit deferred
as no Web bundle existed at iter-71 patch time.

---

## #iter-72-web-pwa-manifest-missing-png-icons — FIXED iter-73 (2026-05-17) — CRITICAL

**Status:** FIXED in v1.9.2 (iter-73).

**Resolution**
Generated 6 PWA PNG icons from the Yole brand SVG (blue #1a73e8, white "Y" glyph,
rounded corners) using Python/Pillow:

```
webApp/src/wasmJsMain/resources/icons/icon-48.png    (604 bytes)
webApp/src/wasmJsMain/resources/icons/icon-72.png    (843 bytes)
webApp/src/wasmJsMain/resources/icons/icon-96.png   (1138 bytes)
webApp/src/wasmJsMain/resources/icons/icon-144.png  (1723 bytes)
webApp/src/wasmJsMain/resources/icons/icon-192.png  (2324 bytes)
webApp/src/wasmJsMain/resources/icons/icon-512.png  (6148 bytes)
```

Updated `manifest.json` to declare all 6 sizes. Added `web_pwa_icon_challenge.sh`
(CONST-039 gate, 3 layers: manifest schema + source-tree PNG validity + bundle audit).
Wired into `qa-iter-73-gates` → `qa-all`.

**Iteration target:** iter-72 or iter-73

---

## #iter-72-web-pwa-icon-challenge-gap — FIXED iter-73 (2026-05-17)

**Status:** FIXED in v1.9.2 (iter-73).

**Resolution**
`yole-challenges/scripts/web_pwa_icon_challenge.sh` authored with 3 layers:
- Layer A: manifest.json schema (icons array, 192x192 + 512x512 entries present)
- Layer B: source-tree PNG presence + PNG magic bytes + size >= 500 bytes per file
- Layer C: bundle audit (conditional on releases/ Wasm bundle; SKIPped with
  `#wasmjs-production-distribution-gap` reason when no bundle exists yet)

Wired into `make qa-iter-73-gates` → `make qa-all`. Challenge passed PASS on iter-73.

**Iteration target:** iter-72 or iter-73

---

## #iter-72-android-app-name-asset-audit-gap — FIXED iter-73 (2026-05-17)

**Status:** FIXED in v1.9.2 (iter-73).

**Resolution**
`yole-challenges/scripts/app_name_survives_r8_challenge.sh` authored with 2 layers:
- Layer A: verifies `android:label` in AndroidManifest.xml and
  `manifestPlaceholders["appLabel"]` in build.gradle.kts for both Release ("Yole")
  and DEV ("Yole DEV") variants
- Layer B: runs `aapt2 dump badging` on both Release and DEV APKs from releases/ and
  asserts exact label match per variant (SKIPped with documented reason when aapt2
  unavailable or no APK present)

Challenge passed on iter-73: Release APK label='Yole', DEV APK label='Yole DEV'.
Wired into `make qa-iter-73-gates` → `make qa-all`.

**Iteration target:** iter-73

---

## #iter-72-desktop-app-name-asset-audit-gap — PARTIALLY FIXED iter-73 (2026-05-17)

**Status:** PARTIAL — version synced; DMG challenge deferred.

**Resolution**
`desktopApp/build.gradle.kts` `packageVersion` synced from `1.9.0` → `1.9.2`
(matching the v1.9.2 release tag). The version drift gap is closed.

DMG bundle challenge (`installable_desktop_icon_challenge.sh`) is deferred to
iter-74 — the DMG has not been rebuilt yet for v1.9.2 and the challenge requires
a new build artifact. See `#iter-71-desktop-icns-format-defect` for the related
ICNS quality issue.

**Remaining gap:** `#iter-74-desktop-dmg-challenge` — author DMG challenge once
v1.9.2 Desktop DMG is rebuilt and staged in releases/.

**Iteration target:** iter-73

---

## #iter-72-android-splash-screen-asset-audit-gap — N/A (confirmed iter-73, 2026-05-17)

**Status:** N/A — Yole has no splash screen; tracker is invalid.

**Finding**
Confirmed during iter-73 source inspection: Yole for Android has **no splash screen
implementation** whatsoever. There is no `windowSplashScreen*` attribute in any
theme file, no `SplashScreen` API call in any Activity, and no `Theme.SplashScreen`
parent. The system-default splash (icon on white background) is what users see.

This means there is no asset gap to gate against — you cannot write a challenge
that verifies an asset that intentionally does not exist.

**Disposition**
- CONST-039 splash screen challenge: NOT APPLICABLE (no feature, no asset to check).
- Future implementation: tracked as `#iter-74-android-splash-screen-implementation`
  (product polish, not a defect). When a splash screen is added, update
  `installable_app_icon_challenge.sh` with a Layer D for splash assets.

**Iteration target:** iter-73

---

## #iter-71-desktop-icns-format-defect — NEW iter-71 (2026-05-17)

**Symptom**
`Yole.app/Contents/Resources/Yole.icns` in the Desktop macOS DMG is a
raw PNG file, not a proper multi-resolution ICNS container. The macOS
Finder resolves it to a single 512×512 image regardless of display DPI,
causing blurry icon rendering on Retina displays at 2× scale factor.

**Root cause**
The desktop build pipeline assigns a `.icns` extension to a PNG output
from Compose Desktop's packaging step without converting it through
`iconutil`. The Compose Desktop packaging documentation requires an
`Info.plist` icon reference pointing to a proper `.icns` bundle.

**Impact**
Visual quality defect on macOS Retina displays. The icon is visible (unlike
the Android adaptive icon regression) but may appear pixelated at large sizes
(128dp in Finder sidebar, 512dp on Launchpad full-screen).

**Fix**
Generate a proper `.iconset/` directory at 1024×1024 and run `iconutil -c
icns` before packaging. Track in the next Desktop packaging iteration.

**Blocker**
Requires a 1024×1024 source artwork file for the Yole brand mark.

## #iter59-firebase-tester-groups-empty — NEW iter-59 (2026-05-15)

**Symptom**
`firebase appdistribution:groups:list --project yole-app` returns zero
groups. Distribution attempts via
`firebase appdistribution:distribute --groups internal-testers …` therefore
return:

```
Error: failed to distribute to testers/groups: Request to
https://firebaseappdistribution.googleapis.com/v1/projects/578988389676/apps/<APP_ID>/releases/<RELEASE_ID>:distribute
had HTTP Error: 404, Requested entity was not found.
```

This affects BOTH the production app
(`1:578988389676:android:d61715a0a84a42c65d2889`) and the new iter-59 DEV
app (`1:578988389676:android:5a3d47a9fb23b6465d2889`).

iter-57 / iter-58 logged "distributed to testers/groups successfully"
with the same group name — most likely the group existed at that point
and was deleted between iter-58 and iter-59 (or the project state was
migrated and group state was lost). Either way, the upload-step itself
still succeeds: binary is stored, release notes attached, console URL
is live.

**Proper fix**
Create the `internal-testers` group via Firebase Console
(`Project Settings → App Distribution → Testers & Groups`) on BOTH
the production and DEV app entries, add at least one tester email,
then re-run distribution with `--groups internal-testers`.

**Blocker**
Operator-side action — adding testers requires the project owner's
email list. No code fix possible from the build pipeline.

**Workaround applied iter-59**
Distribution runs proceed without `--groups`. Binary upload + release
notes succeed; testers with existing console access can install via
the tester-share URL. Release-notes file lists both Firebase release
IDs (`2j5cfopftric0` for production, `1fqnia7g6leio` for DEV) and
console URLs as positive evidence per CONST-035.

## #phase-7-blocked-on-ios-baseline — PARTIALLY RESOLVED 2026-05-15

**Update 2026-05-15:** Document-KMP `@OptIn` fix landed upstream
(commit `609add7` in `git@github.com:vasic-digital/Document-KMP.git`,
branch `main`). Running `./gradlew :Document-KMP:compileKotlinIosArm64`
now BUILDS SUCCESSFUL. However, `./gradlew :shared:compileKotlinIosArm64`
still fails on Yole-internal iOS K/N breakage in `shared/iosMain` —
specifically `DatabaseFactory.ios.kt` has multiple `actual`s without
matching `expect` declarations, missing `@OptIn(ExperimentalForeignApi)`
calls, and `Unresolved reference 'DatabaseInterface'`. These are
**pre-existing Yole-internal defects**, not sibling-submodule issues.

The Document-KMP layer is no longer the blocker. The iOS Phase 7
Tree-Sitter integration is still gated, but on a different defect now:
see `#shared-iosmain-databasefactory-broken` below.

**Original entry follows for historical context:**

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

## #shared-iosmain-databasefactory-broken — NEW iter-57 follow-up 2026-05-15

**Symptom** — `./gradlew :shared:compileKotlinIosArm64` fails (after the
Document-KMP `@OptIn` fix landed upstream, 2026-05-15):

```
e: .../shared/src/iosMain/kotlin/digital/vasic/yole/database/DatabaseFactory.ios.kt:
   - 'actual object DatabaseFactory : Any' has no corresponding expected declaration
   - Unresolved reference 'DatabaseInterface'
   - Unresolved reference 'path'
   - Several 'This declaration needs opt-in. ExperimentalForeignApi' warnings
```

**Origin** — pre-existing. `DatabaseFactory.kt` in `commonMain` was
refactored (or deleted entirely) without updating the iosMain actual.
Predates iter-57; iter-57 surfaced it because the Document-KMP fix
now lets the iOS compile proceed to the next failure.

**Impact** — iOS K/N compile of `:shared:compileKotlinIosArm64` is
BLOCKED. Phase 7 Tree-Sitter integration cannot proceed. iOS app
builds against shared cannot proceed.

**Fix path** — read the current `expect` shape of `DatabaseFactory` in
`shared/src/commonMain/kotlin/digital/vasic/yole/database/`. Update the
iosMain `actual` declarations to match. Add `@OptIn(ExperimentalForeignApi)`
where K/N requires it. Likely a 30-60 minute task — well-scoped but
needs careful expect/actual alignment.

**Owner / next step** — operator-flagged for next session. Yole-internal
fix (not a sibling submodule); CONST-038 doesn't gate this.

---

## #f2-phase-3-bonede-query-api-gap — NEW iter-58 F2 Phase 3 (2026-05-15)

**Symptom**
Two related items in iter-58 Feature 2 Phase 3 (commit landing
2026-05-15):

1. **iOS + Wasm `FoldQueryRunner` / `OutlineExtractor` actuals are
   stubs.** They return `emptyList()` rather than executing a real
   tree-sitter query. iOS is gated on the same upstream issue tracked
   by `#phase-7-blocked-on-ios-baseline` + `#shared-iosmain-databasefactory-broken`.
   Wasm awaits Phase 6's `web-tree-sitter` second-engine landing per
   research-report.md §6.4. Both stubs are HONEST per CONST-035 — they
   never emit fake folds / outline items; the fold-gutter / breadcrumb
   degrade gracefully to "no folds" / "no outline".

2. **iOS + Wasm `readScmResource` actuals throw `IllegalStateException`.**
   iOS bundle-resource access (NSBundle) and Wasm fetch-based asset
   loading both await Phase 6/7 wiring. The expect/actual symmetry is
   complete (the Yole `shared` module compiles on all 4 targets); the
   stubs throw on call rather than silently returning `""`.

3. **Bundled `markdown/folds.scm` is Yole-authored, not the verbatim
   nvim-treesitter upstream.** The upstream targets a newer
   tree-sitter-markdown grammar that emits `(section)` / `(list)`
   node types; the bundled bonede tree-sitter-markdown 0.7.1 grammar
   does NOT (verified empirically — TSQuery rejects them with
   `TSQueryErrorField`). The Yole-authored file uses only node types
   confirmed valid in 0.7.1: `fenced_code_block`, `indented_code_block`,
   `paragraph`, `tight_list`. Attribution + the upstream reference is
   preserved in the file's SPDX header comment.

**Discovered by**
Iter-58 Feature 2 Phase 3 implementation, 2026-05-15. The TSQuery
rejection was caught by running `FoldQueryRunnerTest` on first
implementation; an ad-hoc probe test enumerated the actual node types
the bundled grammar emits. Test was honest-corrected per CONST-035
rather than the result being faked.

**End-user impact**
None on Desktop + Android (where the JVM actuals execute real
queries and emit real folds / outline items). On iOS + Web, fold +
outline affordances are unavailable until Phases 6/7 land. Editor
falls back gracefully — no faked affordances.

**Proper fix path**
1. Phase 6: ship Wasm `readScmResource` (fetch-based) + Wasm
   `FoldQueryRunner` / `OutlineExtractor` via `web-tree-sitter` second
   engine. Plan Phase 6 already scopes this.
2. Phase 7: ship iOS `readScmResource` (NSBundle) + iOS
   `FoldQueryRunner` / `OutlineExtractor` via Kotlin/Native cinterop
   against the tree-sitter C API (ts_query_new / ts_query_cursor_new /
   ts_query_cursor_next_match — see research-report.md §6.2). Gated
   on `#shared-iosmain-databasefactory-broken` clearing.
3. When Phase 6 lands `build-from-source` for all 55 grammars, the
   Yole-authored markdown/folds.scm can be replaced with the verbatim
   nvim-treesitter upstream (which will work against the newer
   grammar that emits `(section)`).
4. **iter-60 Phase 7 also covers snippet JSON bundling for iOS + Wasm.**
   `readSnippetResource` on iOS returns `null` and on Wasm returns
   `null` until Phase 7 wires NSBundle (iOS) / fetch (Wasm) access for
   `snippets/<lang>/snippets.json` files. This is benign — snippets are
   optional; the registry degrades gracefully to an empty list. See
   `shared/src/iosMain/.../SnippetRegistry.ios.kt` and
   `shared/src/wasmJsMain/.../SnippetRegistry.wasmJs.kt` for the stubs.

**Anti-bluff disposition (CONST-035)**
Honest at every layer. The desktop + Android JVM actuals execute real
queries against the bundled grammar (verified via mutation: stubbing
the body to `return emptyList()` makes the tests FAIL). The iOS + Wasm
stubs are documented + honestly return empty results. The bundled
markdown query is a Yole-authored file matched to the actual grammar
node types, with the upstream reference preserved for re-vendoring
when the grammar matrix lands in Phase 6.

**Exit criteria**
1. `web-tree-sitter` Wasm engine landed (Phase 6 of iter-58 plan).
2. iOS Kotlin/Native cinterop landed (Phase 7, after upstream
   `#shared-iosmain-databasefactory-broken` clears).
3. Per-language `folds.scm` + `outline.scm` matrix built for all 55
   grammars, vendored verbatim from nvim-treesitter + helix (Phase 6).
4. `FoldQueryRunnerTest` extended to assert ≥1 fold across all 4
   platforms (with platform-specific test infrastructure landing
   alongside Phase 6/7).
5. The Yole-authored markdown/folds.scm replaced with the upstream
   verbatim version once the grammar set supports its captures.

---

## #wasmjs-production-distribution-gap — PARTIALLY RESOLVED iter-65 2026-05-16

**Update 2026-05-16 (iter-65):**

Root cause confirmed via bytecode analysis of KGP 2.0.20 and 2.1.0 jars.
The crash is a class-initialization order bug in `ExecutableWasm`:

- `JsIrBinary.<init>` (superclass) calls `registerTask("_linkSyncTask", …)`
  at bytecode offset 279 of `JsBinaries.kt`.
- Gradle's `DefaultNamedDomainObjectCollection$AbstractDomainObjectCreatingProvider.configure`
  fires the configure callback **eagerly** during registration.
- That callback calls `syncInputConfigure`, which is virtual-dispatched to
  `ExecutableWasm.syncInputConfigure`. At this point `this.optimizeTask`
  is null because `ExecutableWasm.<init>` has not progressed past the
  `super()` call (the `optimizeTask` field is set at bytecode offsets
  127–131, AFTER `super()` at offset 22).
- This is a real KGP defect present in both KGP 2.0.20 and 2.1.0.

The bug cannot be worked around by any DSL-level configuration in
`webApp/build.gradle.kts`. A fix requires a KGP version beyond 2.1.0
(with a compatible Compose Multiplatform upgrade).

**What was fixed in iter-65:**

Two `commonMain` files used JVM-only APIs, preventing `compileKotlinWasmJs`
from succeeding even if the bundle task were available:

1. `CompletionEngine.kt` — removed `synchronized {}` / `val lock = Any()`
   (JVM-only). The `channelFlow` `repeat` loop is sequential via
   `resultsChannel.receive()`, so no lock is needed. All `CompletionEngineTest`
   tests PASS.

2. `LspWorkspaceResolver.kt` — removed `= FileSystem.SYSTEM` default on
   the `fs` parameter (`FileSystem.SYSTEM` is Okio JVM/Desktop-only,
   unavailable on Wasm). Made `fs` a required parameter. All
   `LspWorkspaceResolverTest` tests PASS.

`./gradlew :shared:compileKotlinWasmJs` now BUILD SUCCESSFUL.

**Remaining gap:**

`binaries.executable()` still cannot be called (KGP 2.0.20 bug above).
Without it, KGP does not generate `wasmJsBrowserDevelopmentWebpack`,
`wasmJsBrowserDistribution`, or any webpack tasks. Firebase Hosting
setup and `releases/Yole-Web-*` staging are blocked until this is
resolved.

**Fix path:**

Upgrade Kotlin beyond 2.1.x (e.g., 2.2.0+) with a matching Compose
Multiplatform release that ships the `ExecutableWasm` constructor fix.
Tracked for v1.9.0 infrastructure session.

**Original entry (historical context):**

**Symptom** — adding `binaries.executable()` to `webApp/build.gradle.kts`
wasmJs block surfaces:

```
Cannot invoke "org.gradle.api.tasks.TaskProvider.flatMap(...)" because
"this.optimizeTask" is null
```

The Compose-Multiplatform wasmJs production-distribution pipeline
(`binaryen` optimization + production webpack + asset pipeline) is
incompletely configured.

**Impact** — `:webApp:wasmJsBrowserDistribution` task is not generated;
Phase 14 cannot produce a web distribution artifact. Dev mode (Karma)
works; production bundling does not.

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

## #f2-phase-6-grammar-bundling-gap — PARTIALLY RESOLVED iter-58 F2 Phase 7 (2026-05-15)

**Update 2026-05-15:** Phase 7 closes this gap for **47 of 55 langs on
Desktop (5 ABIs)**. The remaining surface is documented in 3 new tickets
below: `#f2-phase-7-no-bonede-artifact`,
`#f2-phase-7-nim-grammar-broken`, and
`#f2-phase-7-android-ndk-bulk-build-pending`. Original entry retained
for historical context.

Real evidence that the gap is partially closed (CONST-035 anti-bluff):

- `Feature2LanguageSmokeTest.realTokenizationForAllBundledLangs` is now
  GREEN with positive per-lang token counts logged (markdown(24),
  python(83), java(85), kotlin(54), cpp(92), rust(90), java(85), ...).
- `BonedeGrammarSmokeTest.allBundledLangs_loadAndParse` is GREEN with
  47/47 parse evidence.
- `Feature2LanguageSmokeTest.unsupportedLangs_throwHonestly` is GREEN —
  the 8-lang gap set throws explicitly rather than faking a grammar.

Phase 7 delivery summary:

| Platform | Coverage | Path |
|----------|----------|------|
| macOS-arm64 Desktop | 47/55 langs | bonede JARs (Gradle deps) |
| macOS-x64 Desktop   | 47/55 langs | bonede JARs (Gradle deps) |
| Linux-x64 Desktop   | 47/55 langs | bonede JARs (Gradle deps) |
| Linux-aarch64 Desktop | 47/55 langs | bonede JARs (Gradle deps) |
| Windows-x64 Desktop | 47/55 langs | bonede JARs (Gradle deps) |
| Android (any ABI)   | 1/55 (markdown only) | iter-57 NDK build |
| iOS arm64           | BLOCKED  | no Xcode in build env |

This entry remains OPEN to track the residual Android NDK bulk-build
work + the 8-lang gap set. Closes fully when 100% coverage lands on
the 4 native platforms.

---

## #f2-phase-7-no-bonede-artifact — NEW iter-58 F2 Phase 7 (2026-05-15)

**Symptom**
7 of Yole's 55 declared languages have NO published
`io.github.bonede:tree-sitter-<lang>` artifact on Maven Central as
of the 2026-05-15 snapshot. They are:

| Yole id | Why no bonede artifact |
|---------|-----------------------|
| jsx     | bonede ships `tree-sitter-javascript` (which IS the JSX grammar) but no separate `tree-sitter-jsx` artifact. Yole declares jsx as a distinct LanguageMetadata row. |
| xml     | `tree-sitter-grammars/tree-sitter-xml` upstream exists but is not published as a bonede JAR. |
| vim     | `neovim/tree-sitter-vim` upstream exists but is not published as a bonede JAR. |
| less    | `mdovale/tree-sitter-less` upstream exists but is not published as a bonede JAR. |
| crystal | `keidax/tree-sitter-crystal` upstream exists but is not published as a bonede JAR. |
| groovy  | `Decodetalkers/tree-sitter-groovy` upstream exists but is not published as a bonede JAR. |
| bibtex  | `latex-lsp/tree-sitter-bibtex` upstream exists but is not published as a bonede JAR. |

**End-user impact**
These 7 languages detect correctly (file extension routing in
LanguageRegistry) and get the host-only affordance pipeline
(CommentToggle / IndentEngine / BracketAuto via LanguageMetadata).
But they do NOT get Tree-Sitter-based syntax highlighting,
outline, or fold. The editor falls back gracefully.

Concretely: jsx receives no separate highlighting today because
Yole has no entry that aliases jsx → javascript at the engine
level. xml / vim / less / crystal / groovy / bibtex render as
plain text.

**Anti-bluff disposition (CONST-035)**
Honest. `BonedeGrammarRegistry.unsupportedLangs` enumerates the 8
unsupported langs (these 7 + nim, see next ticket).
`Feature2LanguageSmokeTest.unsupportedLangs_throwHonestly` asserts
that calling `TokenizerEngine.loadGrammar(lang)` for any of them
throws — proving the user does NOT see fake tokens.

**Proper fix path**
Two options, in order of preference:

1. **Operator-side build-from-source via
   `tools/build-language-grammars.sh android <lang>`** — clones the
   upstream repo, runs `tree-sitter generate` (if needed), and
   compiles the parser.c + scanner.c against the chosen toolchain.
   For Desktop, the output `.dylib` / `.so` / `.dll` then needs a
   custom Kotlin loader (the bonede `TreeSitter<Lang>` class scaffolding
   is not available — Yole would have to call `tree_sitter_<lang>()`
   via JNI directly). Estimated 1-2 hour effort per language.

2. **Wait for upstream bonede to publish.** Submit an upstream PR
   to bonede with the missing grammar. Lowest-effort path long-term
   but operator-controlled. Verified bonede actively accepts new
   grammars — 116 artifacts are currently published.

For `jsx` specifically: a trivial alternative is to register
`jsx → TreeSitterJavascript` in BonedeGrammarRegistry (the bonede
tree-sitter-javascript JAR already handles JSX syntax). Phase 7
intentionally did NOT do this because it would conflate the two
LanguageMetadata identities; the alias decision is a Phase 8
concern (when the broader scope mapper lands).

**Blocker**
Operator decision: time investment for build-from-source vs
upstream PR cadence.

**Exit criteria**
1. Every Yole language id either has a bonede artifact OR
   `tools/build-language-grammars.sh` builds it from source AND
   the resulting binary is wired into the engine path.
2. `BonedeGrammarRegistry.unsupportedLangs` shrinks accordingly.
3. `Feature2LanguageSmokeTest.realTokenizationForAllBundledLangs`
   asserts 55/55 instead of today's 47/47.

---

## #f2-phase-7-nim-grammar-broken — NEW iter-58 F2 Phase 7 (2026-05-15)

**Symptom**
`io.github.bonede:tree-sitter-nim:0.6.0` (and 0.5.0) loads
successfully (`Class.forName(...).newInstance()` succeeds, returns
a TSLanguage with `version() == 14`), but immediately segfaults
the JVM with exit code 133 the moment `TSParser.parseString(...)`
is called against it. No JVM error log (`hs_err_pid*.log`) is
produced — the native code calls `_Exit(133)` directly, suggesting
an internal `abort()` in the grammar's scanner.

Reproduced against three different bonede core versions:
- `io.github.bonede:tree-sitter:0.24.4` — segfault.
- `io.github.bonede:tree-sitter:0.25.3` — segfault.
- `io.github.bonede:tree-sitter:0.26.6` — segfault.

Reproduced against the only two published nim JARs:
- `tree-sitter-nim:0.5.0` — IncompatibleClassChangeError (older ABI).
- `tree-sitter-nim:0.6.0` — segfault.

Reproduced on host: macOS 15.4 / arm64 / OpenJDK 21.

**End-user impact**
Nim files (`.nim`, `.nims`, `.nimble`) detect correctly but
receive no Tree-Sitter highlighting. Tested on operator host.
Other 47 bundled grammars are unaffected.

**Anti-bluff disposition (CONST-035)**
Honest. `nim` is excluded from
`shared/build.gradle.kts desktopMain` dependencies, excluded from
`BonedeGrammarRegistry.classNames`, and listed in
`BonedeGrammarRegistry.unsupportedLangs`.
`BonedeGrammarSmokeTest.unsupportedLangs_throwHonestly` asserts
that `loadGrammar("nim")` throws IllegalArgumentException —
proving no fake tokens are emitted.

**Proper fix path**
1. Upstream report to bonede with this segfault repro.
2. Build-from-source via
   `tools/build-language-grammars.sh android nim`. Test whether
   the upstream `alaviss/tree-sitter-nim` parser.c works against
   tree-sitter core 0.26.6 directly. If yes, the bug is in
   bonede's specific build — Yole can ship its own.

**Blocker**
None — operator-side investigation when convenient.

**Exit criteria**
`nim` moves from `unsupportedLangs` back to `classNames` in
BonedeGrammarRegistry, AND
`BonedeGrammarSmokeTest.allBundledLangs_loadAndParse` includes a
`"nim" to "proc f() = discard"` snippet that parses successfully.

---

## #f2-phase-7-android-ndk-bulk-build-pending — NEW iter-58 F2 Phase 7 (2026-05-15)

**Symptom**
Android `TokenizerEngine.android.kt` still bundles ONLY the
`markdown` grammar (the iter-57 path). The 47 bonede grammars
bundled for Desktop in iter-58 Phase 7 are NOT available on
Android because the bonede JARs ship glibc-linked `.so` files
which Android's bionic linker rejects.

The build pipeline for closing this gap is fully implemented in
`tools/build-language-grammars.sh android <lang>...`. It clones
the upstream `tree-sitter-<lang>` repo, runs `tree-sitter generate`
(when the pre-generated parser.c is absent), and compiles parser.c
+ scanner.c against the Android NDK clang toolchain for each
configured ABI (default arm64-v8a; opt-in via `NDK_ABIS` env for
armeabi-v7a + x86_64). Output `libtree-sitter-<lang>.so` files go
to `shared/native/<lang>/android-<abi>/` where AGP's jniLibs
convention packages them into the APK.

What remains:
1. Run the build script for the 47 Yole-bundled langs × 3 ABIs
   (= 141 NDK builds, ~5-15 minutes operator wall-clock).
2. Extend the iter-57 `repackageBonedeJarsForAndroid` Gradle task
   pattern to repackage the 47 additional JARs (currently it only
   handles `tree-sitter-markdown`).
3. Extend `TokenizerEngine.android.kt loadGrammar()` to use the
   same `BonedeGrammarRegistry.classNameFor(lang)` reflection
   pattern as the Desktop actual.

**End-user impact**
Android users still see plain text (no syntax highlighting) for
all 46 non-markdown languages. The host-only affordance pipeline
(comment toggle, indent, brackets) works for all 55 langs via
LanguageMetadata — that's correct behavior, just not full feature.

**Anti-bluff disposition (CONST-035)**
Honest.
`TokenizerEngine.android.kt loadGrammar()` throws
IllegalArgumentException with an explicit message naming this
ticket ("grammar `$lang` is not yet bundled for Android — only
`markdown` ships an NDK-built .so today. See
KNOWN_DEFECTS#f2-phase-7-android-ndk-bulk-build-pending."). The
editor falls back to plain text per spec §4.

**Blocker**
Operator time to run the NDK builds + author the
repackage-for-N-jars Gradle generalisation.

**Exit criteria**
1. `shared/native/<lang>/android-arm64-v8a/libtree-sitter-<lang>.so`
   exists for all 47 bonede langs.
2. The Gradle build task fan-out packages all 48 JARs (the iter-57
   tree-sitter-markdown + 47 new) with their NativeUtils swapped.
3. A new `:shared:androidUnitTest` analogue of
   `Feature2LanguageSmokeTest.realTokenizationForAllBundledLangs`
   asserts 47/47 langs parse on Android.

---

## #f2-phase-7-ios-xcode-required — NEW iter-58 F2 Phase 7 (2026-05-15)

**Symptom**
The Phase 7 iOS path is fully scaffolded in
`tools/build-language-grammars.sh ios <lang>...`, but
`xcrun --sdk iphoneos --show-sdk-path` returns
`error: SDK "iphoneos" cannot be located` on the operator host
(macOS 15.4 / Command-Line-Tools only). Xcode + the iOS SDK is
required to compile parser.c into a static `libtree-sitter-<lang>.a`
that Kotlin/Native can link via `tree-sitter.def`.

**End-user impact**
iOS gets no Tree-Sitter highlighting for any of the 55 languages
including markdown. The iter-57 Phase 7 (iOS Tree-Sitter K/N
engine) is itself blocked on a different defect
(`#shared-iosmain-databasefactory-broken`) — when that unblocks,
this one becomes the next gate.

**Anti-bluff disposition (CONST-035)**
Honest. iOS `TokenizerEngine.ios.kt` remains the iter-57
NotImplementedError stub.

**Proper fix path**
1. Install Xcode + iOS SDK on a build host.
2. Run `tools/build-language-grammars.sh ios markdown kotlin ...`.
3. Wire the produced `.a` files into the Kotlin/Native cinterop
   `tree-sitter.def` configuration (scaffold already in place).

**Blocker**
Operator-host Xcode installation. Also blocked downstream of
`#shared-iosmain-databasefactory-broken` per iter-57 Phase 7.

**Exit criteria**
1. `shared/native/<lang>/ios-arm64-device/libtree-sitter-<lang>.a`
   exists for the operator's chosen tier.
2. `:shared:linkPodReleaseFrameworkIosArm64` succeeds with the
   .a files declared in cinterop.

---

## #f2-phase-6-grammar-bundling-gap — NEW iter-58 F2 Phase 6 (2026-05-15)

**Symptom**
F2 Phase 6 ships LanguageMetadata + vendored .scm query files +
fixtures for 55 languages, but only `markdown` actually exercises the
full editor pipeline end-to-end. The other 54 languages have:
  - Real LanguageMetadata entries (comment/indent/bracket/extension).
  - Real upstream `.scm` query files (highlights + folds + outline)
    with SPDX attribution.
  - Real test fixtures.

But their Tree-Sitter native grammars (the JAR / shared-lib that
maps .scm queries to runtime captures) are NOT bundled in
`TokenizerEngine.desktop.kt` / `TokenizerEngine.android.kt`. The
engine's `loadGrammar()` throws `IllegalArgumentException` for any
lang other than `markdown` (per Phase 5 of Feature 1's scope —
55-grammar bundling was always tracked as a separate concern).

**Concrete consequences**
1. The editor can detect a `.py` file as Python (LanguageRegistry
   resolves it) but cannot syntax-highlight it via Tree-Sitter
   captures — falls back to no highlighting.
2. The outline/fold runners throw if invoked for any non-markdown
   lang.
3. `Feature2LanguageSmokeTest.inputSmokeCheckForAllLanguages` does
   NOT run the engine end-to-end — it asserts only that the input
   side (fixture + .scm content) is coherent. This is an honest
   limitation explicitly disclosed in the test's docstring.

**Anti-bluff disposition (CONST-035)**
Honest at every layer.
  - The Feature2LanguageSmokeTest docstring explicitly states
    "we do NOT fake the smoke test by mocking the engine or
    stubbing out the affordance runners". The test ASSERTS the
    inputs are coherent — it does NOT assert the editor highlights
    those 54 langs (because it can't, honestly).
  - LanguageMetadataCompletenessTest catches missing .scm files at
    test time (verified by mutation — deleting python/highlights.scm
    causes test FAILURE).
  - Yole-authored stub files (e.g. crystal/outline.scm, the various
    folds.scm gaps documented in batch commit bodies) carry an
    explicit "Yole-authored stub" header; they emit zero matches
    rather than faking coverage.

**Proper fix path**
1. Bundle per-language Tree-Sitter native grammars via Gradle
   dependencies — analogous to how `tree-sitter-markdown` is
   bundled today. Each new grammar adds ~50-200 KB per platform
   per arch to the artifact.
2. Extend `TokenizerEngine.{desktop,android,ios,wasmJs}.kt`'s
   `loadGrammar()` switch with a case per bundled grammar.
3. Extend Feature2LanguageSmokeTest to run the real engine
   pipeline for every bundled grammar (drop the
   inputSmokeCheckForAllLanguages-only mode).

This is a substantial effort (55 grammars × 4 platforms = up to
220 native libraries to source/build/ship). Scoping that work is
out of F2 Phase 6 — it lives as a separate plan item that the
operator can prioritize against the Feature-3/4/5 roadmap.

**Tracking signals**
  - The 55-row LanguageMetadata + 55-dir grammars/ + 55-dir
    test-fixtures/ tree on master tip `8f8b01ef`.
  - `Feature2LanguageSmokeTest.inputSmokeCheckForAllLanguages` is
    the regression guard for the inputs.
  - `LanguageMetadataCompletenessTest` is the regression guard for
    file presence + SPDX attribution.
  - This entry closes when the grammar bundling matrix lands and
    `Feature2LanguageSmokeTest` extends to per-lang end-to-end
    smoke for every bundled grammar.

**Honesty bar (per the user's 2026-04-28 mandate)**
The bar for shipping is "users can use the feature" not just "tests
pass". F2 Phase 6 explicitly ships the SCAFFOLD for 55 languages
(metadata + queries + fixtures) but does NOT claim the editor
highlights / outlines / folds 55 langs today. The end-user-visible
feature is "markdown works end-to-end; other 54 langs detect
correctly + have correct comment/indent/bracket behaviour via the
host-only affordance pipeline (CommentToggle, IndentEngine,
BracketAuto) which does NOT depend on Tree-Sitter". This is the
honest disposition; users get part of the feature today, the rest
when the grammar matrix lands.

---

## #iter-68-iosapp-ui-kn-api-errors — NEW iter-68 (2026-05-17)

**Status:** OPEN
**Scope:** `iosApp/src/iosMain/kotlin/digital/vasic/yole/ios/`
**Root cause:** `IOSBackgroundSync.kt` and `IOSDocumentProvider.kt` contain
64 K/N API compilation errors — wrong parameter names for
`registerForTaskWithIdentifier`, missing `error:` argument in
`submitTaskRequest`, `isDiscretionary` treated as function instead of
property, `UTType` unresolved, `UIDocumentBrowserViewController.Configuration`
type mismatch. These are in the `iosApp` UI layer, separate from the
`shared/iosMain` K/N errors resolved in iter-68.
**Impact:** `iosApp` framework cannot be linked; iOS IPA distribution is
blocked. The `shared/iosMain` KMP library now compiles clean (all fixes
from iter-68 applied); only the app UI layer is broken.
**Fix path:** Fix K/N API calls in `IOSBackgroundSync.kt` and
`IOSDocumentProvider.kt` to match the K/N-bridged Apple SDK signatures.
**Iteration target:** iter-69

---

## #iter-69-ios-sqlite-cinterop — NEW iter-68 (2026-05-17)

**Status:** OPEN
**Scope:** `shared/src/iosMain/kotlin/digital/vasic/yole/database/`
**Root cause:** `IosSQLiteDatabase.kt` references `cnames.structs.sqlite3`
which requires a SQLite cinterop `.def` file registered in `shared/build.gradle.kts`.
No `.def` file exists. The original implementation is preserved as comments in
`IosSQLiteDatabase.kt`.
**Impact:** iOS uses an in-memory stub `DatabaseFactory` (no persistence).
**Fix path:** Add `sqlite3.def` cinterop definition + register it in
`shared/build.gradle.kts`; restore `IosSQLiteDatabase.kt` implementation.
**Iteration target:** iter-69

---

## #iter-69-android-room-database — NEW iter-68 (2026-05-17)

**Status:** OPEN
**Scope:** `shared/src/androidMain/kotlin/digital/vasic/yole/database/`
**Root cause:** Android uses an in-memory stub `DatabaseFactory` — Room or
SQLite integration was deferred.
**Fix path:** Implement Room-backed `DatabaseFactory` for Android.
**Iteration target:** iter-69

---

## #iter-69-desktop-sqlite-database — NEW iter-68 (2026-05-17)

**Status:** OPEN
**Scope:** `shared/src/desktopMain/kotlin/digital/vasic/yole/database/`
**Root cause:** Desktop uses an in-memory stub `DatabaseFactory` — SQLite
integration was deferred.
**Fix path:** Implement SQLite (via Okio / JDBC) `DatabaseFactory` for Desktop.
**Iteration target:** iter-69

---

## #iter-69-web-indexeddb-database — NEW iter-68 (2026-05-17)

**Status:** OPEN
**Scope:** `shared/src/wasmJsMain/kotlin/digital/vasic/yole/database/`
**Root cause:** Web (Wasm) uses an in-memory stub `DatabaseFactory` — IndexedDB
integration was deferred.
**Fix path:** Implement IndexedDB-backed `DatabaseFactory` for Wasm.
**Iteration target:** iter-69

---

## #iter-69-linux-container-deb-build — NEW iter-68 (2026-05-17)

**Status:** OPEN
**Scope:** Linux distribution, `Containers/pkg/crossbuild/`
**Root cause:** Compose Desktop `packageDeb` (via `jpackage`) produces a binary
that targets the JVM running Gradle, not a cross-compiled binary. On the macOS
host, even inside a `linux/arm64` container, `packageDeb` needs access to
Compose Desktop packaging tools that require native Linux execution. The
`linux_container.Containerfile` (iter-67) successfully provisions a JDK17 +
Gradle container; the packaging step fails because `packageDeb` internally
invokes a macOS `jpackage` from the host JDK.
**Fix path:** Run the full Gradle build (`:desktopApp:packageDeb`) on a native
Linux host (VM or remote machine) using the iter-67 container image.
Alternatively: explore Compose Desktop cross-compilation support in Compose
MP releases newer than 1.7.3.
**Iteration target:** iter-69

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
