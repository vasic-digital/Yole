<!-- SPDX-FileCopyrightText: 2025-2026 Milos Vasic -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Legacy Module Migration Plan

This document assesses the legacy Android modules (`app/`, `core/`, `commons/`) and provides a
migration plan for transitioning their functionality to the shared KMP module.

## Active References (Audit Results)

The following imports from legacy modules are actively used in `androidApp/`:

### commons/ references from androidApp

| File | Import | Usage |
|------|--------|-------|
| `androidApp/src/main/java/.../ui/YoleApp.kt` | `digital.vasic.opoc.model.GsSharedPreferencesPropertyBackend` | SharedPreferences-backed property storage for app settings |

### core/ references from androidApp

| File | Import | Usage |
|------|--------|-------|
| `androidApp/src/main/java/.../util/BackupRestoreUtil.kt` | `digital.vasic.yole.format.FormatRegistry` | Format detection during backup/restore |
| `androidApp/src/main/java/.../util/BackupRestoreUtil.kt` | `digital.vasic.yole.format.TextFormat` | Format metadata for backup serialization |
| `androidApp/src/main/java/.../ui/YoleApp.kt` | `digital.vasic.yole.format.FormatRegistry` | Format registry initialization at app startup |
| `androidApp/src/androidTest/.../ui/IntegrationTest.kt` | `digital.vasic.yole.format.FormatRegistry` | Format registry in integration tests |

**Note:** The `FormatRegistry` and `TextFormat` imports above resolve to the **shared** module's
Kotlin versions (`shared/src/commonMain/.../format/FormatRegistry.kt` and `TextFormat.kt`), not the
Java versions in `core/`. The core Java classes at `core/src/main/java/digital/vasic/yole/format/`
are no longer actively imported by androidApp. The core module is included in `settings.gradle.kts`
but is not listed as a dependency in `androidApp/build.gradle.kts`.

### core/ internal dependencies

The `core` module depends on `commons` and `shared` (see `core/build.gradle.kts`), but nothing in
the project depends on `core` anymore.

---

## Module Assessment

### 1. `app/` Directory

**Status:** Empty module (resources only, no build.gradle.kts, not in settings.gradle.kts)

**Contents:**
- `src/main/res/raw/readme.md` - bundled readme
- `src/main/res/raw/changelog.md` - bundled changelog
- `src/main/res/raw/contributors.md` - bundled contributors
- `src/main/res/raw/license.txt` - bundled license

**Dependencies:** None (not included in `settings.gradle.kts`)

**Assessment:** The `app/` directory is a vestige of the original Markor project structure. It
previously held the main Android application code, which has been fully replaced by `androidApp/`.
The only remaining content is raw resource files.

**Migration Plan:**
- Move resources to `androidApp/src/main/res/raw/` if still referenced
- If unused, delete the `app/` directory entirely
- **Priority:** Low -- no code depends on this module

---

### 2. `core/` Module

**Status:** Legacy Android library module -- **no active consumers**

**Namespace:** `digital.vasic.yole.core`

**Contents (12 Java source files):**

| File | Purpose | KMP Equivalent | Status |
|------|---------|----------------|--------|
| `format/FormatRegistry.java` | Format registry interface | `shared/.../format/FormatRegistry.kt` | Superseded |
| `format/FormatRegistryImpl.java` | Default format registry | `shared/.../format/FormatRegistry.kt` | Superseded |
| `format/TextConverterBase.java` | Abstract HTML converter | `shared/.../format/TextParser.kt` | Superseded |
| `format/ActionButtonBase.java` | Format toolbar actions | Compose UI handles per-format | Superseded |
| `format/TextFormat.java` | Format metadata (Java) | `shared/.../format/TextFormat.kt` | Superseded |
| `frontend/textview/SyntaxHighlighterBase.java` | Syntax highlighter | Compose annotated strings | Superseded |
| `model/Document.java` | Java document model | `shared/.../model/Document.kt` | Superseded |
| `model/DocumentManager.java` | Document lifecycle | Shared ViewModel/use case | Partially migrated |
| `model/AppSettings.java` | Application settings | DataStore on Android | Not yet migrated |
| `model/StorageManager.java` | Android file storage | `PlatformFileIOFactory` | Superseded |
| `thirdparty/.../JavaPasswordbasedCryption.java` | Encryption | `Security-KMP` module | Superseded |
| `thirdparty/.../PasswordStore.java` | Password storage | `Security-KMP` module | Superseded |

**Test files:** `src/test/java/.../format/FormatRegistryTest.java`

**Migration plan:** This module can be removed immediately. No `androidApp` code imports from it.
The `core` module's `build.gradle.kts` depends on `:commons` and `:shared`, but nothing depends on
`:core`.

---

### 3. `commons/` Module

**Status:** Active legacy Android library module -- **1 active consumer**

**Namespace:** `net.gsantner.opoc` (legacy namespace from Markor/opoc)

**Active reference:** `GsSharedPreferencesPropertyBackend` is imported by `YoleApp.kt` for
SharedPreferences-backed settings storage.

**Contents (25 Kotlin/Java source files):**

| File | Purpose | KMP Equivalent | Referenced |
|------|---------|----------------|-----------|
| `format/GsSimpleMarkdownParser.kt` | Simple markdown parser | `Formatters-KMP` | No |
| `format/GsSimplePlaylistParser.kt` | M3U/PLS playlist parser | None | No |
| `format/GsTextUtils.kt` | Text utilities | Kotlin stdlib | No |
| `model/GsPropertyBackend.kt` | Property storage interface | `Config-KMP` | Indirectly (via GsSharedPreferencesPropertyBackend) |
| `model/GsMapPropertyBackend.kt` | Map-based property storage | `Config-KMP` | No |
| `model/GsSharedPreferencesPropertyBackend.kt` | SharedPreferences backend | DataStore | **Yes** |
| `util/GsBackupUtils.kt` | Backup/restore utilities | Not yet migrated | No |
| `util/GsCollectionUtils.kt` | Collection extensions | Kotlin stdlib | No |
| `util/GsContextUtils.kt` | Android Context utilities | Android-specific | No |
| `util/GsFileUtils.kt` | File utilities | `PlatformFileIOFactory` | No |
| `util/GsImageUtils.kt` | Image processing | Android-specific | No |
| `util/GsIntentUtils.kt` | Android Intent utilities | Android-specific | No |
| `util/GsNanoProfiler.kt` | Lightweight profiler | Could migrate to shared | No |
| `util/GsResourceUtils.kt` | Android resource utilities | Android-specific | No |
| `util/GsStorageUtils.kt` | Android storage utilities | `PlatformFileIOFactory` | No |
| `util/GsUiUtils.kt` | Android UI utilities | Compose Multiplatform | No |
| `util/GsCoolExperimentalStuff.java` | Experimental features | Review needed | No |
| `web/GsNetworkUtils.kt` | Network utilities | Ktor client | No |
| `web/GsWebViewClient.kt` | Custom WebView client | Android-specific | No |
| `wrapper/GsAndroidSpinnerOnItemSelectedAdapter.kt` | Spinner adapter | Android-specific | No |
| `wrapper/GsCallback.kt` | Generic callback | Kotlin Flow/coroutines | No |
| `wrapper/GsHashMap.kt` | Extended HashMap | Kotlin stdlib Map | No |
| `wrapper/GsMenuItemDummy.java` | Menu item stub | Android-specific | No |
| `wrapper/GsTextWatcherAdapter.kt` | TextWatcher adapter | Compose text input | No |

**Test files:** `src/test/java/.../util/GsFileUtilsTest.java`

**Migration plan:** Only `GsSharedPreferencesPropertyBackend` (and its interface
`GsPropertyBackend`) need migration. Replace with AndroidX DataStore or move these two files into
`androidApp/` directly.

---

## Recommended Migration Order

### Phase 1: Remove `core/` Module (No Risk)

**Effort:** 1 hour | **Risk:** None

No androidApp code imports from `core/`. Steps:
1. Remove `include(":core")` from `settings.gradle.kts`
2. Delete `core/` directory
3. Verify `./gradlew :androidApp:assembleDebug` still builds

### Phase 2: Remove `app/` Directory (No Risk)

**Effort:** 30 minutes | **Risk:** None

1. Check if `androidApp` references any files from `app/src/main/res/raw/`
2. Move referenced resources to `androidApp/src/main/res/raw/` if needed
3. Delete `app/` directory

### Phase 3: Decouple `commons/` from androidApp (Low Risk)

**Effort:** 2-4 hours | **Risk:** Low

1. Copy `GsSharedPreferencesPropertyBackend.kt` and `GsPropertyBackend.kt` into
   `androidApp/src/main/java/digital/vasic/opoc/model/`
2. Update imports in `YoleApp.kt` to point to the local copy
3. Remove `implementation(project(":commons"))` from `androidApp/build.gradle.kts`
4. Verify build succeeds

### Phase 4: Remove `commons/` Module (Low Risk)

**Effort:** 30 minutes | **Risk:** Low (after Phase 3)

1. Remove `include(":commons")` from `settings.gradle.kts`
2. Delete `commons/` directory
3. Full build verification

### Phase 5: Modernize Settings Storage (Medium Effort)

**Effort:** 1-2 days | **Risk:** Medium

Replace the copied `GsSharedPreferencesPropertyBackend` with modern AndroidX DataStore:
1. Create `shared/src/commonMain/.../model/AppSettings.kt` with expect/actual pattern
2. Implement Android actual using DataStore in `shared/src/androidMain/`
3. Implement Desktop actual using java.util.prefs in `shared/src/desktopMain/`
4. Implement iOS actual using NSUserDefaults in `shared/src/iosMain/`
5. Implement Wasm actual using localStorage in `shared/src/wasmJsMain/`
6. Remove the copied legacy files from `androidApp/`

---

## Timeline

| Phase | Target Date | Status |
|-------|------------|--------|
| Phase 1: Remove `core/` | Q2 2026 | Ready to execute |
| Phase 2: Remove `app/` | Q2 2026 | Ready to execute |
| Phase 3: Decouple `commons/` | Q2 2026 | Ready to execute |
| Phase 4: Remove `commons/` | Q2 2026 | Blocked on Phase 3 |
| Phase 5: Modernize settings | Q3 2026 | Design phase |

## Dependencies to Update

When removing legacy modules, update these files:
- `settings.gradle.kts` -- Remove `include(":commons")` and `include(":core")`
- `androidApp/build.gradle.kts` -- Remove `implementation(project(":commons"))`
- `core/build.gradle.kts` -- Delete entirely
- `commons/build.gradle.kts` -- Delete entirely

## Risk Assessment

| Module | Risk Level | Reason |
|--------|-----------|--------|
| `app/` | None | No code, only raw resources, not in settings.gradle.kts |
| `core/` | None | No active consumers; can remove immediately |
| `commons/` | Low | Only 1 class referenced by androidApp; easy to copy locally |

---

## Current Deprecation Status (as of March 2026)

### `commons/` Module — `GsContextUtils` and Related Classes

**Status:** Deprecated, scheduled for removal in Q2 2026.

The `commons/` module originated from the [opoc](https://github.com/gsantner/opoc) library
bundled with the original Markor project. It contains 25 Kotlin/Java source files under the
`net.gsantner.opoc` namespace, of which only **one** is still actively referenced:

- **`GsSharedPreferencesPropertyBackend`** — Used by `YoleApp.kt` for SharedPreferences-backed
  application settings storage. This is the sole remaining dependency between `androidApp/` and
  `commons/`.

All other classes in `commons/` have been superseded by KMP equivalents:

| Legacy Class | Replacement | Status |
|-------------|-------------|--------|
| `GsSimpleMarkdownParser` | `Formatters-KMP` module | Superseded |
| `GsTextUtils` | Kotlin stdlib extensions | Superseded |
| `GsPropertyBackend` | `Config-KMP` module | Superseded (except as interface for GsSharedPreferences) |
| `GsFileUtils`, `GsStorageUtils` | `PlatformFileIOFactory` | Superseded |
| `GsContextUtils` | Android Compose + KMP utilities | Superseded |
| `GsNetworkUtils` | Ktor client | Superseded |
| `GsUiUtils` | Compose Multiplatform | Superseded |
| `GsCallback`, `GsHashMap` | Kotlin Flow, stdlib Map | Superseded |

**Migration path for remaining callers:**
1. `YoleApp.kt` imports `GsSharedPreferencesPropertyBackend` for settings
2. Copy `GsSharedPreferencesPropertyBackend.kt` + `GsPropertyBackend.kt` into `androidApp/`
3. Update the import path in `YoleApp.kt`
4. Remove `implementation(project(":commons"))` from `androidApp/build.gradle.kts`

### `core/` Module — Java Format System

**Status:** Deprecated, **ready for immediate removal**.

The `core/` module contains 12 Java source files that were the original format parsing system.
Every class has been fully superseded by Kotlin equivalents in the `shared` module:

- `FormatRegistry.java` / `FormatRegistryImpl.java` -> `shared/.../format/FormatRegistry.kt`
- `TextConverterBase.java` -> `shared/.../format/TextParser.kt`
- `TextFormat.java` -> `shared/.../format/TextFormat.kt`
- `Document.java` -> `shared/.../model/Document.kt`
- `JavaPasswordbasedCryption.java` / `PasswordStore.java` -> `Security-KMP` module

**No code in the project imports from `core/`.** The module is included in `settings.gradle.kts`
but is not listed as a dependency in any `build.gradle.kts`. It can be deleted without any code
changes.

### `app/` Module — Empty Resource Shell

**Status:** Deprecated, **ready for immediate removal**.

The `app/` directory contains only four raw resource files (readme, changelog, contributors,
license). It has no `build.gradle.kts` and is not registered in `settings.gradle.kts`. These
resource files are artifacts of the original Markor project structure and are not referenced by
the current `androidApp/` module.

---

## Deprecation Timeline

| Module | Deprecation Date | Removal Target | Blocking Dependencies | Current Status |
|--------|-----------------|----------------|----------------------|----------------|
| `core/` | March 2026 | **Q2 2026** | None | Ready for removal |
| `app/` | March 2026 | **Q2 2026** | None | Ready for removal |
| `commons/` | March 2026 | **Q2 2026** | `GsSharedPreferencesPropertyBackend` in YoleApp.kt | Requires Phase 3 migration first |

### Post-Removal Cleanup

After all three legacy modules are removed:

1. **Build configuration cleanup:**
   - Remove `include(":commons")` and `include(":core")` from `settings.gradle.kts`
   - Remove `implementation(project(":commons"))` from `androidApp/build.gradle.kts`
   - Delete `core/build.gradle.kts` and `commons/build.gradle.kts`

2. **Namespace cleanup:**
   - The `net.gsantner.opoc` namespace will be fully retired
   - All code will live under `digital.vasic.yole.*` or extracted KMP module packages

3. **CI/CD updates:**
   - Remove any legacy module references from CI workflows
   - Update Detekt/lint configurations if they reference legacy module paths

4. **Documentation updates:**
   - Update `CLAUDE.md` architecture section to remove legacy module references
   - Update video course scripts and website content if they reference legacy modules
