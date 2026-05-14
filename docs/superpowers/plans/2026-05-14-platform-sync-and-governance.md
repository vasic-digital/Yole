# Platform-Sync Bugs + Cross-Platform Constitutional Rule Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix two Android UX defects (line-number/text scroll desync, duplicate File Browser), introduce a constitutional rule that every change must be reasoned about across all four platforms (Android / Desktop / iOS / Web), and prove the fixes with anti-bluff tests + challenges.

**Architecture:** Three parallel workstreams. (1) Governance — append CONST-037 "Cross-Platform Impact" to root + 9 submodules' CONSTITUTION.md / CLAUDE.md / AGENTS.md (anti-bluff covenant is already propagated everywhere — verified). (2) Android UI fixes in `androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt`: share one `ScrollState` between gutter `Column` and editor (replace `OutlinedTextField` with `BasicTextField` to expose the scroll state); delete the `SubScreen.FILE_BROWSER` path and route editor's "open file" to the canonical `Screen.FILES` tab. (3) Anti-bluff verification — Robolectric tests that exercise real scroll + assert offsets line up, plus two new shell challenges wired into `make qa-all`.

**Tech Stack:** Kotlin Multiplatform 2.0.20, Compose Multiplatform 1.7.3, Robolectric, JUnit4 + `runBlocking<Unit>`, bash 4+ for challenges, Detekt for static analysis.

---

## Investigation summary (already done)

- **Scroll desync root cause:** `androidApp/.../YoleApp.kt:1534` — gutter `Column.verticalScroll(rememberScrollState())`. `YoleApp.kt:1561` — `OutlinedTextField` with its own internal scroll state. The two states are independent; scrolling text leaves line numbers behind. Same pattern repeats in second editor at `YoleApp.kt:~2447` / `~2868` — fix once via a shared helper.
- **File Browser duplication on Android:**
  - **Keep:** `Screen.FILES → FilesScreen` (bottom-tab, canonical) at `YoleApp.kt:813`.
  - **Drop:** `SubScreen.FILE_BROWSER` enum entry (`YoleApp.kt:297`), both `FileBrowserScreen` render blocks (`YoleApp.kt:855`, `YoleApp.kt:982`), the "File browser" `Card` in `MoreScreen` (`YoleApp.kt:~4310`), and the `onFileBrowserClick` parameter (`YoleApp.kt:4258`). Reroute editor's `onOpenFileBrowser` (`YoleApp.kt:501`) to navigate to `Screen.FILES`.
  - **Desktop** has its own `Screen.FILE_BROWSER` + `FileBrowserScreen` at `desktopApp/.../YoleApp.kt:426` and an `IdeFileBrowser` in `EnhancedYoleApp.kt:1011`. Per the new CONST-037 (cross-platform impact), evaluate per-platform — Desktop UX may legitimately differ; do not delete blindly. **Decision deferred to Task 14**.
  - **iOS/Web:** confirmed no duplicate browser pattern at investigation time (only `Main.kt` entry points exist).
- **Anti-bluff covenant status (verified by grep):** End-user-quality forensic anchor + `CONST-035` reference are present in **all 30 governance files** (main + 9 submodules × 3 files). No propagation work required for anti-bluff. New work is only for the cross-platform-impact rule.
- **Active challenges:** `yole-challenges/scripts/` has 5 scripts — `anchor_manifest_challenge.sh`, `bluff_scanner_challenge.sh`, `host_no_auto_suspend_challenge.sh`, `mutation_ratchet_challenge.sh`, `no_suspend_calls_challenge.sh`. Two new ones to add: `scroll_sync_challenge.sh`, `cross_platform_parity_challenge.sh`.

---

## File Structure

**New files:**
- `androidApp/src/main/java/digital/vasic/yole/android/ui/editor/SyncedScrollEditor.kt` — extracted Composable `SyncedScrollEditor(text, showLineNumbers, isDarkTheme, ...)` that owns ONE `ScrollState` and applies it to both gutter `Column` and a `BasicTextField`. Replaces the two inline editors in `YoleApp.kt`.
- `androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/EditorScrollSyncRobolectricTest.kt` — Robolectric test that scrolls a 200-line document, captures gutter + editor scroll offsets, asserts equality.
- `androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/FileBrowserDedupRobolectricTest.kt` — asserts only one File Browser surface exists and reaches `FilesScreen`.
- `yole-challenges/scripts/scroll_sync_challenge.sh` — anti-pattern grep + Robolectric runner.
- `yole-challenges/scripts/cross_platform_parity_challenge.sh` — counts File Browser entry points per platform.

**Modified files:**
- `androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt` — call `SyncedScrollEditor` instead of inline gutter + `OutlinedTextField`; delete `SubScreen.FILE_BROWSER`; delete `FileBrowserScreen` composable; remove "File browser" card from `MoreScreen`; redirect editor's `onOpenFileBrowser`.
- `CONSTITUTION.md` (main) — append CONST-037.
- `CLAUDE.md` (main) — append "Cross-Platform Impact" mandatory rule + reference to CONST-037.
- `AGENTS.md` (main) — append same rule.
- `{submodule}/CONSTITUTION.md`, `{submodule}/CLAUDE.md`, `{submodule}/AGENTS.md` for each of: Challenges, Containers, HelixQA, LLMProvider, Security, Dependencies/HelixDevelopment/{DocProcessor,LLMOrchestrator,LLMsVerifier,VisionEngine} — 27 files (each gets a short CONST-037-equivalent block).
- `Makefile` — register the two new challenges under a new `qa-extras` target and chain into `qa-all`.
- `docs/CONTINUATION.md` — final update per CONST-036.
- `CHANGELOG.md` — add iter-55 entries.

---

## Task 1: Worktree + branch hygiene

**Files:**
- None modified yet; preparation only.

- [ ] **Step 1.1: Verify clean working tree on the right branch**

Run: `git status --porcelain`
Expected: only the four `m` entries on `Dependencies/HelixDevelopment/*` from the session start, no other unstaged work. If anything else is present, stop and investigate.

- [ ] **Step 1.2: Create worktree for this iteration**

```bash
git worktree add ../Yole-iter-55-platform-sync -b iter-55-platform-sync
cd ../Yole-iter-55-platform-sync
```

Expected: worktree created at sibling path, new branch `iter-55-platform-sync` checked out.

---

## Task 2: Author CONST-037 in the root Constitution

**Files:**
- Modify: `CONSTITUTION.md` — append after CONST-036.

- [ ] **Step 2.1: Append the CONST-037 block**

Insert immediately before `## Definition of Done`:

```markdown
<!-- BEGIN cross-platform-impact addendum (CONST-037) -->

### CONST-037 — Cross-Platform Impact MUST Be Reasoned About

**Status:** Mandatory. Non-negotiable. Applies to every change in this
repository and its submodules.

**Rule:** Yole ships to four user-visible platforms — Android, Desktop
(Linux x64, Windows x64, macOS arm64), iOS, and Web (Wasm PWA). Every
change that touches shared code, UI, a screen, a navigation entry,
a data model, or a public API MUST explicitly answer:

1. **Does this compile on every target?** KMP common code, expect/actual
   surfaces, and platform-specific code paths each have their own
   compilation matrix.
2. **Does this behave identically (or by-design differently) on every
   target?** A bottom navigation bar that makes sense on Android may be
   wrong on Desktop. A file-picker that uses SAF on Android needs an
   `expect/actual` counterpart on Desktop / iOS / Web.
3. **Is the change covered by a test on every affected target?** A fix
   that lands only in `androidMain` but not in `desktopMain` / `iosMain` /
   `wasmJsMain` is incomplete by default.
4. **Are platform-specific resources (Manifest, Info.plist, web manifest,
   container packaging) updated coherently?**

**How to apply:**
- Before editing shared code, list which `*Main` / `*Test` source sets
  the change touches. If only one is touched but more are affected,
  the change is incomplete.
- Every PR description / commit body for a multi-platform change MUST
  contain a "Cross-platform impact" section enumerating each platform
  and its disposition (changed / unchanged / N/A with reason).
- Per-platform divergence is allowed when justified, but MUST be
  documented in the commit body and (for permanent divergences) in
  `docs/ARCHITECTURE.md`.

**Why:** End users experience Yole on whichever platform they install.
A regression that only shows up on iOS still ships to those users. We
have shipped fixes that worked on the host platform but silently broke
others; the only mitigation is mandatory up-front consideration.

**Enforcement:** `yole-challenges/scripts/cross_platform_parity_challenge.sh`
runs in `make qa-all` and scans for divergent surfaces (e.g. a screen
present in one platform's nav but absent in another's without a
documented reason). Detekt rules and per-target test suites enforce
compile-time parity.

**See also:** `CLAUDE.md` and `AGENTS.md` "Cross-Platform Impact"
sections; `docs/ARCHITECTURE.md` for the per-platform module map.

<!-- END cross-platform-impact addendum (CONST-037) -->
```

- [ ] **Step 2.2: Update Definition of Done to reference CONST-037**

Find the "Definition of Done" list (currently 5 items) and append:

```markdown
6. The change has been reasoned about across all four user-visible
   platforms per CONST-037, and any per-platform divergence is
   documented in the commit body.
```

- [ ] **Step 2.3: Commit**

```bash
git add CONSTITUTION.md
git commit -m "feat(iter-55): add CONST-037 cross-platform impact mandatory consideration"
```

---

## Task 3: Append cross-platform rule to root CLAUDE.md and AGENTS.md

**Files:**
- Modify: `CLAUDE.md` — add new section before "Quality Requirements".
- Modify: `AGENTS.md` — same content, with phrasing aimed at non-Claude CLI agents.

- [ ] **Step 3.1: Insert "Cross-Platform Impact" section in `CLAUDE.md`**

```markdown
## ⚠️ Cross-Platform Impact — MANDATORY Consideration (CONST-037)

Yole ships to Android, Desktop (Linux x64 / Windows x64 / macOS arm64),
iOS, and Web (Wasm PWA). Every change MUST be reasoned about across
all four targets BEFORE coding.

**Pre-edit checklist** (applies to any change in `shared/`, `*App/`,
or any module's UI / navigation / public API):

- [ ] Which `*Main` source sets does this change touch? (`commonMain`,
      `androidMain`, `desktopMain`, `iosMain`, `wasmJsMain`)
- [ ] Which `*Test` source sets cover the change? Missing coverage on
      any affected target = incomplete change.
- [ ] Does the same UX make sense on every target, or is per-platform
      divergence required? If divergent, where is it documented?
- [ ] Are platform manifests (`AndroidManifest.xml`, `Info.plist`, web
      `manifest.json`, container packaging) updated coherently?

**Commit body requirement:** any change affecting more than one
platform MUST include a "Cross-platform impact" block enumerating
each platform's disposition. Example:

```
Cross-platform impact:
- Android: fix applied, Robolectric test added
- Desktop: unaffected (uses separate editor surface)
- iOS:     N/A (component not yet ported)
- Web:     parity update required, follow-up tracked in docs/CONTINUATION.md
```

See CONST-037 in `CONSTITUTION.md` for the authoritative rule.
```

- [ ] **Step 3.2: Insert the equivalent section in `AGENTS.md`**

Paste the same content but reframe the opening: "All CLI agents
working on this repo MUST follow this rule, not only Claude Code".
Keep the same pre-edit checklist and commit body template.

- [ ] **Step 3.3: Commit**

```bash
git add CLAUDE.md AGENTS.md
git commit -m "docs(iter-55): propagate CONST-037 cross-platform impact to CLAUDE.md and AGENTS.md"
```

---

## Task 4: Propagate CONST-037 to 9 submodules

**Files:**
- Modify: 27 files (9 submodules × 3 files each).

Each submodule is a separate git repo and gets its own commit. The
content is a **condensed** version of CONST-037 — the full text lives
in the main repo's Constitution; submodules carry the binding rule
plus a pointer.

- [ ] **Step 4.1: Author the submodule-flavored CONST-037 block**

Reusable content (the same goes into each submodule's
`CONSTITUTION.md` as a new numbered rule appended at the end):

```markdown
### Cross-Platform Impact — Mandatory Consideration (mirrors Yole CONST-037)

**Status:** Mandatory. Non-negotiable. Mirrors CONST-037 in the parent
Yole project's `CONSTITUTION.md`.

**Rule:** This submodule is consumed by the Yole multi-platform
project (Android / Desktop / iOS / Web). Every change MUST be
reasoned about across all four target platforms BEFORE coding. A fix
that works on one target but silently breaks another is a regression.

**Pre-edit checklist:** see CONST-037 in the parent Yole repo
(`/CONSTITUTION.md`). Per-platform divergence MUST be justified in
the commit body.

**Commit body requirement:** every change MUST include a
"Cross-platform impact" block listing each platform's disposition
(changed / unchanged / N/A with reason).

**Why:** End users experience the integrated product, not this
submodule in isolation. Cross-platform regressions caused by
submodule-local changes have shipped to users in the past; mandatory
up-front consideration is the only mitigation.
```

For `CLAUDE.md` and `AGENTS.md` in each submodule, append:

```markdown
## Cross-Platform Impact (mirrors Yole CONST-037)

Every change in this submodule MUST be reasoned about across all
four Yole user-visible platforms (Android / Desktop / iOS / Web)
BEFORE coding. See `CONSTITUTION.md` in this submodule and CONST-037
in the parent Yole repo for the full rule.

Commit bodies for changes affecting more than one platform MUST
include a "Cross-platform impact" block.
```

- [ ] **Step 4.2: Apply to each submodule (loop)**

For each submodule path `S` in `[Challenges, Containers, HelixQA, LLMProvider, Security, Dependencies/HelixDevelopment/DocProcessor, Dependencies/HelixDevelopment/LLMOrchestrator, Dependencies/HelixDevelopment/LLMsVerifier, Dependencies/HelixDevelopment/VisionEngine]`:

```bash
cd "$S"
# Append the three blocks to the three files.
# Use Edit tool, not heredoc, to preserve file formatting.

git add CONSTITUTION.md CLAUDE.md AGENTS.md
git commit -m "docs: mirror Yole CONST-037 cross-platform impact rule

Cross-platform impact:
- Android: governance-only, no code change
- Desktop: governance-only, no code change
- iOS:     governance-only, no code change
- Web:     governance-only, no code change"
cd "$ROOT"
```

After all submodule commits, bump the submodule pointer in the parent:

```bash
git add Challenges Containers HelixQA LLMProvider Security Dependencies/HelixDevelopment/*
git commit -m "chore(iter-55): bump submodule pointers after CONST-037 propagation"
```

---

## Task 5: Write failing scroll-sync test (TDD red)

**Files:**
- Create: `androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/EditorScrollSyncRobolectricTest.kt`

- [ ] **Step 5.1: Write the failing test**

```kotlin
package digital.vasic.yole.android.robolectric

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import digital.vasic.yole.android.ui.editor.SyncedScrollEditor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class EditorScrollSyncRobolectricTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun gutterAndTextScrollOffsetsRemainEqualAfterUserScroll() {
        val content = (1..200).joinToString("\n") { "Line $it body content" }
        val text = mutableStateOf(content)

        composeRule.setContent {
            SyncedScrollEditor(
                textState = text,
                showLineNumbers = true,
                isDarkTheme = false,
            )
        }

        composeRule.onNodeWithTag("syncedScrollEditor.editor")
            .performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        val gutterScroll = composeRule.onNodeWithTag("syncedScrollEditor.gutter")
            .fetchSemanticsNode().config
            .getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange) { null }
            ?.value?.invoke() ?: 0f

        val editorScroll = composeRule.onNodeWithTag("syncedScrollEditor.editor")
            .fetchSemanticsNode().config
            .getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange) { null }
            ?.value?.invoke() ?: 0f

        assertEquals(editorScroll, gutterScroll, absoluteTolerance = 1.0f,
            "gutter scroll ($gutterScroll) must equal editor scroll ($editorScroll) after user scroll")
    }
}
```

- [ ] **Step 5.2: Run and confirm RED**

```bash
./gradlew :androidApp:testFlavorDefaultDebugUnitTest --tests "*EditorScrollSyncRobolectricTest*"
```

Expected: FAIL with either "unresolved reference: SyncedScrollEditor" or "gutter scroll != editor scroll". This is the red phase.

---

## Task 6: Extract `SyncedScrollEditor` Composable (TDD green)

**Files:**
- Create: `androidApp/src/main/java/digital/vasic/yole/android/ui/editor/SyncedScrollEditor.kt`

- [ ] **Step 6.1: Write the implementation**

```kotlin
package digital.vasic.yole.android.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SyncedScrollEditor(
    textState: MutableState<String>,
    showLineNumbers: Boolean,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val sharedScroll = rememberScrollState()

    Row(modifier = modifier.fillMaxSize()) {
        if (showLineNumbers) {
            val lines = textState.value.lines()
            val gutterWidth = when {
                lines.size >= 1000 -> 48.dp
                lines.size >= 100 -> 40.dp
                else -> 32.dp
            }
            val gutterBg = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFF8F8F8)
            val gutterFg = if (isDarkTheme) Color(0xFF858585) else Color(0xFF999999)

            Column(
                modifier = Modifier
                    .testTag("syncedScrollEditor.gutter")
                    .width(gutterWidth)
                    .fillMaxHeight()
                    .background(gutterBg)
                    .verticalScroll(sharedScroll)
                    .padding(top = 8.dp, end = 4.dp),
                horizontalAlignment = Alignment.End,
            ) {
                lines.forEachIndexed { idx, _ ->
                    androidx.compose.material3.Text(
                        text = "${idx + 1}",
                        color = gutterFg,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }

        BasicTextField(
            value = textState.value,
            onValueChange = { textState.value = it },
            modifier = Modifier
                .testTag("syncedScrollEditor.editor")
                .fillMaxSize()
                .verticalScroll(sharedScroll)
                .padding(8.dp),
            textStyle = TextStyle(
                color = if (isDarkTheme) Color(0xFFD4D4D4) else Color(0xFF1E1E1E),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
            ),
        )
    }
}
```

- [ ] **Step 6.2: Run and confirm GREEN**

```bash
./gradlew :androidApp:testFlavorDefaultDebugUnitTest --tests "*EditorScrollSyncRobolectricTest*"
```

Expected: PASS.

- [ ] **Step 6.3: Mutation-test the fix (anti-bluff verification)**

Temporarily replace `verticalScroll(sharedScroll)` on the gutter with `verticalScroll(rememberScrollState())` (revert to the bug). Re-run the test. Expected: FAIL. Revert the mutation.

- [ ] **Step 6.4: Commit**

```bash
git add androidApp/src/main/java/digital/vasic/yole/android/ui/editor/SyncedScrollEditor.kt \
        androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/EditorScrollSyncRobolectricTest.kt
git commit -m "fix(iter-55): share ScrollState between gutter and editor on Android

Cross-platform impact:
- Android: bug fixed via SyncedScrollEditor; Robolectric test asserts gutter/editor scroll equality + mutation-verified
- Desktop: unaffected (separate editor surface, not yet known to exhibit the same defect — investigate in follow-up)
- iOS:     N/A (not yet ported)
- Web:     N/A (web editor uses CodeMirror-style component, separate code path)"
```

---

## Task 7: Replace inline editors in `YoleApp.kt` with `SyncedScrollEditor`

**Files:**
- Modify: `androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt:~1488-1620` (first IDE editor) and `~2447-2950` (second editor variant). Each currently has inline gutter + `OutlinedTextField`; both replaced by one call to `SyncedScrollEditor`.

- [ ] **Step 7.1: Identify the two call sites**

```bash
grep -n "showLineNumbers = showLineNumbers" androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt
```

Expected: 6+ matches. The two inline editor sites are the ones immediately followed by an `OutlinedTextField` block and a gutter `Column` with `verticalScroll(rememberScrollState())`.

- [ ] **Step 7.2: Replace each inline editor with SyncedScrollEditor**

For each of the two sites, replace the gutter `Column { ... }` + `OutlinedTextField { ... }` block with:

```kotlin
SyncedScrollEditor(
    textState = remember { mutableStateOf(text) }.also { state ->
        // existing onContentChanged plumbing — re-route via LaunchedEffect
    },
    showLineNumbers = showLineNumbers,
    isDarkTheme = isDarkTheme,
    modifier = Modifier.weight(1f),
)
```

Wire `onContentChanged` via `LaunchedEffect(textState.value) { onContentChanged(textState.value) }` to preserve the existing dirty-tracking + history behavior.

- [ ] **Step 7.3: Run all Android unit tests**

```bash
./gradlew :androidApp:testFlavorDefaultDebugUnitTest
make container-robolectric-test
```

Expected: all existing tests pass; new scroll sync test passes; FileBrowserSaveFunctionalityTests still pass.

- [ ] **Step 7.4: Commit**

```bash
git add androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt
git commit -m "refactor(iter-55): route both IDE editor surfaces through SyncedScrollEditor

Cross-platform impact:
- Android: in-app behaviour preserved (verified by YoleAppTest + Robolectric); scroll sync bug fixed at both editor sites
- Desktop: unchanged
- iOS:     N/A
- Web:     N/A"
```

---

## Task 8: Write failing File-Browser-dedup test (TDD red)

**Files:**
- Create: `androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/FileBrowserDedupRobolectricTest.kt`

- [ ] **Step 8.1: Write the failing test**

```kotlin
package digital.vasic.yole.android.robolectric

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import digital.vasic.yole.android.ui.MoreScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FileBrowserDedupRobolectricTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun moreScreenDoesNotShowFileBrowserCard() {
        composeRule.setContent {
            MoreScreen(
                onSettingsClick = {},
                onSearchClick = {},
                onBackupClick = {},
                onAboutClick = {},
            )
        }
        composeRule.onNodeWithText("File Browser").assertDoesNotExist()
        composeRule.onNodeWithText("Browse files").assertDoesNotExist()
    }
}
```

- [ ] **Step 8.2: Run and confirm RED**

```bash
./gradlew :androidApp:testFlavorDefaultDebugUnitTest --tests "*FileBrowserDedupRobolectricTest*"
```

Expected: FAIL — "File Browser" text node exists in MoreScreen today.

---

## Task 9: Remove duplicate File Browser (TDD green)

**Files:**
- Modify: `androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt`
  - Line 297: drop `FILE_BROWSER` from `enum class SubScreen`.
  - Line ~501: change `onOpenFileBrowser = { currentSubScreen = SubScreen.FILE_BROWSER }` to navigate to `Screen.FILES` instead: `onOpenFileBrowser = { currentSubScreen = null; currentScreen = Screen.FILES }`.
  - Lines ~855 and ~982: delete the `SubScreen.FILE_BROWSER -> FileBrowserScreen(...)` branches.
  - Line ~846 and ~972: drop `onFileBrowserClick` argument from both `MoreScreen(...)` invocations.
  - Line ~4258: remove the `onFileBrowserClick: () -> Unit = {}` parameter from `MoreScreen`.
  - Lines ~4300-4315 (the "File browser option" `Card`): delete the entire block.
  - Line 2192: delete the `fun FileBrowserScreen(...)` composable entirely.
  - Line 3870: delete the `FileBrowserScreen(...)` preview invocation if present.

- [ ] **Step 9.1: Apply deletions**

Use Edit tool, one removal at a time, smallest unique surrounding context per Edit. After each deletion, run `./gradlew :androidApp:compileFlavorDefaultDebugKotlin` to catch dangling references early.

- [ ] **Step 9.2: Run dedup test and full Android test suite**

```bash
./gradlew :androidApp:testFlavorDefaultDebugUnitTest --tests "*FileBrowserDedupRobolectricTest*"
./gradlew :androidApp:testFlavorDefaultDebugUnitTest
```

Expected: dedup test PASS; entire Android suite PASS (no regressions).

- [ ] **Step 9.3: Manual cross-platform compile check**

```bash
./gradlew :shared:desktopTest
./gradlew :desktopApp:compileKotlin
./gradlew :webApp:compileWasmJsMainKotlinWasmJs
./gradlew :shared:compileIosArm64MainKotlinMetadata
```

Each must compile. The intent is to PROVE the Android-only refactor did not touch shared code that other platforms depend on.

- [ ] **Step 9.4: Commit**

```bash
git add androidApp/
git commit -m "refactor(iter-55): remove duplicate Android File Browser (keep FILES bottom tab)

Cross-platform impact:
- Android: SubScreen.FILE_BROWSER + secondary entry point removed; canonical Screen.FILES tab unchanged; editor's 'open file' now navigates to FILES
- Desktop: unaffected — Desktop's own Screen.FILE_BROWSER preserved (separate UX, decision deferred to follow-up after design review per CONST-037)
- iOS:     N/A
- Web:     N/A"
```

---

## Task 10: Author `scroll_sync_challenge.sh`

**Files:**
- Create: `yole-challenges/scripts/scroll_sync_challenge.sh`

- [ ] **Step 10.1: Write the challenge**

```bash
#!/usr/bin/env bash
# CONST-037 scroll-sync challenge.
# Verifies the editor renders line numbers and text body using a SHARED ScrollState.
#
# Two layers:
#   (a) static  — forbid the dual-ScrollState anti-pattern in editor sources
#   (b) runtime — run the Robolectric test that asserts gutter/editor scroll equality
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

EDITOR_SRC="androidApp/src/main/java/digital/vasic/yole/android/ui/editor/SyncedScrollEditor.kt"
if [[ ! -f "${EDITOR_SRC}" ]]; then
  echo "FAIL: ${EDITOR_SRC} missing — SyncedScrollEditor must be the canonical editor."
  exit 1
fi

# Layer (a): exactly one rememberScrollState() in the editor — shared.
scroll_states=$(grep -c "rememberScrollState()" "${EDITOR_SRC}")
if (( scroll_states != 1 )); then
  echo "FAIL: SyncedScrollEditor.kt must declare exactly ONE rememberScrollState() (found ${scroll_states})."
  exit 2
fi

# Layer (b): run the Robolectric test and capture the PASS line (positive evidence).
log="$(mktemp)"
if ! ./gradlew --quiet :androidApp:testFlavorDefaultDebugUnitTest \
      --tests "*EditorScrollSyncRobolectricTest*" > "${log}" 2>&1; then
  echo "FAIL: EditorScrollSyncRobolectricTest did not pass. See ${log}."
  exit 3
fi

if ! grep -q "EditorScrollSyncRobolectricTest" "${log}"; then
  echo "FAIL: test runner produced no evidence of executing EditorScrollSyncRobolectricTest. See ${log}."
  exit 4
fi

echo "PASS: scroll_sync_challenge — shared ScrollState verified, Robolectric assertion passed (${log})."
```

- [ ] **Step 10.2: Make it executable and run**

```bash
chmod +x yole-challenges/scripts/scroll_sync_challenge.sh
bash yole-challenges/scripts/scroll_sync_challenge.sh
```

Expected: `PASS: scroll_sync_challenge — ...`

- [ ] **Step 10.3: Commit**

```bash
git add yole-challenges/scripts/scroll_sync_challenge.sh
git commit -m "test(iter-55): add scroll_sync_challenge.sh anti-bluff runtime+static probe"
```

---

## Task 11: Author `cross_platform_parity_challenge.sh`

**Files:**
- Create: `yole-challenges/scripts/cross_platform_parity_challenge.sh`

- [ ] **Step 11.1: Write the challenge**

```bash
#!/usr/bin/env bash
# CONST-037 cross-platform parity challenge.
# Counts File Browser entry points per platform; fails if any platform
# exposes more than one user-visible File Browser surface.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

check_platform() {
  local platform="$1"
  local source_glob="$2"
  local max_allowed="$3"
  local count
  count=$(grep -rE "fun (FileBrowserScreen|IdeFileBrowser)\\b" ${source_glob} 2>/dev/null | wc -l | tr -d ' ')
  if (( count > max_allowed )); then
    echo "FAIL: ${platform} exposes ${count} File Browser composables (max ${max_allowed} per CONST-037)."
    return 1
  fi
  echo "OK:   ${platform} — ${count} File Browser surface(s)."
}

failed=0
check_platform "Android" "androidApp/src/main/java" 0 || failed=1
check_platform "Desktop" "desktopApp/src/main/kotlin" 2 || failed=1
check_platform "iOS"     "iosApp/src/iosMain"          0 || failed=1
check_platform "Web"     "webApp/src/wasmJsMain"       0 || failed=1

if (( failed )); then
  echo "FAIL: cross_platform_parity_challenge — see above."
  exit 1
fi
echo "PASS: cross_platform_parity_challenge — per-platform File Browser counts within limits."
```

(Android max 0 because canonical entry lives in `FilesScreen`, not `FileBrowserScreen`. Desktop max 2 reflects current intentional `FileBrowserScreen` + `IdeFileBrowser` until design review concludes.)

- [ ] **Step 11.2: Make executable and run**

```bash
chmod +x yole-challenges/scripts/cross_platform_parity_challenge.sh
bash yole-challenges/scripts/cross_platform_parity_challenge.sh
```

Expected: PASS line.

- [ ] **Step 11.3: Commit**

```bash
git add yole-challenges/scripts/cross_platform_parity_challenge.sh
git commit -m "test(iter-55): add cross_platform_parity_challenge.sh for CONST-037 enforcement"
```

---

## Task 12: Wire new challenges into `make qa-all`

**Files:**
- Modify: `Makefile` — add the two new challenges to the existing `qa-all` target's invocation list.

- [ ] **Step 12.1: Locate `qa-all`**

```bash
grep -n "^qa-all:" Makefile
```

- [ ] **Step 12.2: Append the two new challenge invocations**

After the existing challenge calls under `qa-all`, add:

```makefile
	bash yole-challenges/scripts/scroll_sync_challenge.sh
	bash yole-challenges/scripts/cross_platform_parity_challenge.sh
```

- [ ] **Step 12.3: Run `make qa-all`**

```bash
make qa-all
```

Expected: all challenges PASS, all tests PASS.

- [ ] **Step 12.4: Commit**

```bash
git add Makefile
git commit -m "build(iter-55): include new challenges in qa-all target"
```

---

## Task 13: Update CHANGELOG.md and docs/CONTINUATION.md

**Files:**
- Modify: `CHANGELOG.md` — add iter-55 entry.
- Modify: `docs/CONTINUATION.md` — refresh Section 1 (How to Resume) + Section 6 (Repo State) + Section 7 (Phases).

- [ ] **Step 13.1: Add iter-55 entry to CHANGELOG.md**

```markdown
## iter-55 — Platform sync & governance (2026-05-14)

### Added
- `CONST-037` cross-platform impact mandatory consideration rule (CONSTITUTION.md + CLAUDE.md + AGENTS.md, root + 9 submodules).
- `yole-challenges/scripts/scroll_sync_challenge.sh` and `cross_platform_parity_challenge.sh`.
- `SyncedScrollEditor` composable extracted from inline IDE editor.

### Fixed
- Android editor: gutter (line numbers) and text body now share a single `ScrollState`, eliminating horizontal desync on vertical scroll.

### Removed
- Android: duplicate `SubScreen.FILE_BROWSER` / `FileBrowserScreen` / `MoreScreen` "File browser" card. Canonical entry remains the `Screen.FILES` bottom-nav tab.

### Cross-platform impact
- Android: fixes + dedup applied, Robolectric coverage added.
- Desktop: governance only; UX dedup deferred pending design review.
- iOS: governance only.
- Web:  governance only.
```

- [ ] **Step 13.2: Update CONTINUATION.md sections per CONST-036**

Section 1 "How to Resume": reflect that iter-55 is closed and iter-56 is next.

Section 6 "Repo State": list new commit SHAs from this iteration (parent + submodules).

Section 7 "Phases": add iter-55 row with brief disposition.

- [ ] **Step 13.3: Commit**

```bash
git add CHANGELOG.md docs/CONTINUATION.md
git commit -m "docs(iter-55): closeout — CHANGELOG + CONTINUATION updated"
```

---

## Task 14: Follow-up tracking — Desktop File Browser

**Files:**
- Modify: `docs/CONTINUATION.md` — add a follow-up item in Section 4 (Known Defects) or Section 7 (Phases) noting the Desktop File Browser dedup decision is deferred to a later iteration after design review.

- [ ] **Step 14.1: Add follow-up entry**

Append to CONTINUATION.md:

```markdown
- **#desktop-file-browser-dedup (deferred)** — Desktop currently has both
  `Screen.FILE_BROWSER → FileBrowserScreen` and `IdeFileBrowser` (in
  `EnhancedYoleApp.kt`). Per CONST-037, deferred decision: keep both
  (intentional separate UX) or unify behind a single surface. Owner:
  TBD. Resolution: design review.
```

- [ ] **Step 14.2: Commit**

```bash
git add docs/CONTINUATION.md
git commit -m "docs(iter-55): track Desktop file-browser dedup as deferred follow-up"
```

---

## Self-review checklist (run after the plan is fully drafted)

- [x] **Spec coverage:** Each user request has a task — scroll sync (5/6/7), File Browser dedup (8/9), cross-platform constitutional rule (2/3/4), anti-bluff tests (5/8), anti-bluff challenges (10/11/12), governance propagation (4), verification + Continuation (13).
- [x] **Placeholder scan:** No "TBD" / "implement later" / "similar to Task N" outside the explicit Task-14 follow-up (which IS the deferred item, not a placeholder).
- [x] **Type consistency:** `SyncedScrollEditor` is used identically across Tasks 5, 6, 7, 10. `Screen.FILES`, `SubScreen.FILE_BROWSER`, `MoreScreen` referenced consistently across Tasks 7, 8, 9.

---

## Risks & mitigations

- **Risk:** Replacing `OutlinedTextField` with `BasicTextField` loses Material outline styling. **Mitigation:** wrap `SyncedScrollEditor` in a `Box` with the same border drawing the previous `OutlinedTextField` provided; cover with a visual-regression test if one exists.
- **Risk:** Editor's `onOpenFileBrowser` callbacks expected `SubScreen.FILE_BROWSER` state; rerouting to `Screen.FILES` may surprise users who pressed "open file" expecting a modal. **Mitigation:** verify in Task 9.2 that the existing `IdeEditorScreen` test suite still passes; add a redirect-navigation test if needed.
- **Risk:** Submodule commits authored without push access. **Mitigation:** confirm SSH access to each submodule before Task 4; if missing, raise to user before continuing.
- **Risk:** Mutation-test step (6.3) flaps. **Mitigation:** the mutation must be confined to one line; revert immediately after observing FAIL.

---

## Execution handoff

Plan complete. Two execution options:

1. **Inline Execution (recommended for this plan)** — single session, ordered by Task ID, checkpoint after each phase (governance → bug fix → dedup → challenges → verification). Use `superpowers:executing-plans`.
2. **Subagent-Driven** — dispatch a fresh subagent per task with two-stage review. Slower; preferred if user wants per-task code review.

Pick approach.
