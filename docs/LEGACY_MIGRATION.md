# Legacy Module Migration Plan

This document assesses the legacy Android modules (`app/`, `core/`, `commons/`) and provides a migration plan for transitioning their functionality to the shared KMP module.

## Module Assessment

### 1. `app/` Directory

**Status:** Empty module (resources only)

**Contents:**
- `src/main/res/raw/readme.md` - bundled readme
- `src/main/res/raw/changelog.md` - bundled changelog
- `src/main/res/raw/contributors.md` - bundled contributors
- `src/main/res/raw/license.txt` - bundled license

**Dependencies:** None (not included in `settings.gradle.kts` as a module)

**Assessment:** The `app/` directory is a vestige of the original Markor project structure. It previously held the main Android application code, which has been fully replaced by `androidApp/`. The only remaining content is raw resource files bundled into the APK.

**Migration Plan:**
- Move `readme.md`, `changelog.md`, `contributors.md`, and `license.txt` to `androidApp/src/main/res/raw/` if they are still referenced by the Android app
- If these resources are no longer used (the new `androidApp` has its own resource handling), the `app/` directory can be removed entirely
- **Priority:** Low - no code depends on this module

---

### 2. `core/` Module

**Status:** Active legacy Android library module

**Namespace:** `digital.vasic.yole.core`

**Dependencies:**
- Depends on: `commons` module, `shared` module
- Depended on by: Nothing in `settings.gradle.kts` directly includes it as a dependency of `androidApp` (only `commons` and `shared` are direct dependencies of `androidApp`)

**Contents (Java source files):**

| File | Purpose | KMP Equivalent |
|------|---------|----------------|
| `format/FormatRegistry.java` | Format registry interface | `shared/.../format/FormatRegistry.kt` (Kotlin class) |
| `format/FormatRegistryImpl.java` | Default format registry implementation | `shared/.../format/FormatRegistry.kt` (already has full implementation) |
| `format/TextConverterBase.java` | Abstract base for HTML converters | `shared/.../format/TextParser.kt` (ParsedDocument.toHtml()) |
| `format/ActionButtonBase.java` | Abstract base for format toolbar actions | No direct equivalent; Compose UI handles actions per-format |
| `format/TextFormat.java` | Format metadata model (Java) | `shared/.../format/TextFormat.kt` (Kotlin data class) |
| `frontend/textview/SyntaxHighlighterBase.java` | Abstract syntax highlighter | No KMP equivalent yet; handled by Compose UI per platform |
| `model/Document.java` | Java Document model | `shared/.../model/Document.kt` (Kotlin KMP data class) |
| `model/DocumentManager.java` | Document lifecycle manager | Partially in shared module; UI layer handles lifecycle |
| `model/AppSettings.java` | Application settings model | Not yet in shared; Android uses Compose state/DataStore |
| `model/StorageManager.java` | Android file storage operations | `shared/.../network/platform/PlatformFileIOFactory` (KMP) |
| `thirdparty/.../JavaPasswordbasedCryption.java` | Password-based encryption | `Security-KMP` module (extracted) |
| `thirdparty/.../PasswordStore.java` | Password storage interface | `Security-KMP` module (extracted) |

**Test files:**
- `src/test/java/digital/vasic/yole/format/FormatRegistryTest.java` - tests for Java FormatRegistry

**Migration Plan:**
1. **FormatRegistry.java / FormatRegistryImpl.java / TextFormat.java:** Already superseded by `shared/.../format/FormatRegistry.kt` and `shared/.../format/TextFormat.kt`. These Java files are only needed if any legacy Android code still references them via `core` module. Check `androidApp` for imports from `digital.vasic.yole.format.FormatRegistry` (Java version).
2. **TextConverterBase.java:** Superseded by the `TextParser`/`ParsedDocument` pipeline in shared. No migration needed; can be removed when no Android code references it.
3. **ActionButtonBase.java:** Android-specific UI concept. The Compose Multiplatform UI handles format-specific actions differently. No migration needed.
4. **SyntaxHighlighterBase.java:** Android-specific (uses `CharSequence`). Compose Multiplatform uses annotated strings for highlighting. No migration needed.
5. **Document.java:** Fully superseded by `shared/.../model/Document.kt`. Remove after confirming no legacy code references the Java version.
6. **DocumentManager.java:** The document lifecycle management should be migrated to a shared ViewModel or use case class in `shared/src/commonMain/`. Currently the platform apps handle document lifecycle independently.
7. **AppSettings.java:** Should be migrated to a KMP-compatible settings model in `shared/src/commonMain/`. Each platform can provide actual implementations for persistence (DataStore on Android, NSUserDefaults on iOS, localStorage on Web).
8. **StorageManager.java:** Superseded by `PlatformFileIOFactory` expect/actual pattern. Remove after confirming no legacy code references it.
9. **Encryption files:** Already extracted to `Security-KMP` module. Can be removed.
10. **FormatRegistryTest.java:** Should be retained until the core module is fully removed, then the test coverage is already provided by the 5,200+ shared module tests.

**Priority:** Medium - the `core` module is still included in `settings.gradle.kts` and depended on by `commons`. Gradual removal is recommended.

---

### 3. `commons/` Module

**Status:** Active legacy Android library module

**Namespace:** `net.gsantner.opoc` (legacy namespace from Markor/opoc)

**Dependencies:**
- Depends on: Android SDK, AndroidX, Gson, commons-io, epub-parser
- Depended on by: `core` module, `androidApp` module

**Contents (Kotlin + Java source files):**

| File | Purpose | KMP Equivalent |
|------|---------|----------------|
| `format/GsSimpleMarkdownParser.kt` | Simple markdown parser | `Formatters-KMP` module / `shared/.../format/markdown/` |
| `format/GsSimplePlaylistParser.kt` | M3U/PLS playlist parser | No equivalent (not a priority format) |
| `format/GsTextUtils.kt` | Text utility functions | Partially in shared util packages |
| `model/GsPropertyBackend.kt` | Property storage interface | `Config-KMP` module |
| `model/GsMapPropertyBackend.kt` | Map-based property storage | `Config-KMP` module |
| `model/GsSharedPreferencesPropertyBackend.kt` | SharedPreferences backend | Android-specific; use DataStore in androidApp |
| `util/GsBackupUtils.kt` | Backup/restore utilities | Not yet migrated |
| `util/GsCollectionUtils.kt` | Collection extension functions | Kotlin stdlib covers most of these |
| `util/GsContextUtils.kt` | Android Context utilities | Android-specific; no KMP equivalent needed |
| `util/GsFileUtils.kt` | File utility functions | `PlatformFileIOFactory` in shared module |
| `util/GsImageUtils.kt` | Image processing utilities | Android-specific; not needed in KMP |
| `util/GsIntentUtils.kt` | Android Intent utilities | Android-specific; no KMP equivalent needed |
| `util/GsNanoProfiler.kt` | Lightweight profiler | Could be useful in shared as KMP |
| `util/GsResourceUtils.kt` | Android resource utilities | Android-specific |
| `util/GsStorageUtils.kt` | Android storage utilities | `PlatformFileIOFactory` / `StorageManager` in shared |
| `util/GsUiUtils.kt` | Android UI utilities | Compose Multiplatform handles this |
| `util/GsCoolExperimentalStuff.java` | Experimental features | Review for anything useful |
| `web/GsNetworkUtils.kt` | Network utility functions | Ktor client in shared module |
| `web/GsWebViewClient.kt` | Custom WebView client | Android-specific |
| `wrapper/GsAndroidSpinnerOnItemSelectedAdapter.kt` | Android spinner adapter | Android-specific UI; not needed |
| `wrapper/GsCallback.kt` | Generic callback interface | Kotlin Flow/coroutines replace this |
| `wrapper/GsHashMap.kt` | Extended HashMap | Kotlin stdlib Map covers this |
| `wrapper/GsMenuItemDummy.java` | Menu item stub | Android-specific UI |
| `wrapper/GsTextWatcherAdapter.kt` | TextWatcher adapter | Android-specific UI; Compose handles text input |

**Test files:**
- `src/test/java/digital/vasic/opoc/util/GsFileUtilsTest.java` - tests for file utilities

**Migration Plan:**
1. **GsSimpleMarkdownParser.kt:** Already superseded by the comprehensive Markdown parser in `shared/.../format/markdown/` and `Formatters-KMP`. Remove.
2. **GsTextUtils.kt:** Review for any utility functions not covered by Kotlin stdlib or the shared module. Migrate any unique functionality.
3. **GsPropertyBackend.kt / GsMapPropertyBackend.kt:** Superseded by `Config-KMP` module. Remove.
4. **GsSharedPreferencesPropertyBackend.kt:** Android-specific. If still used by `androidApp`, keep in `androidApp/` directly. Otherwise remove.
5. **GsBackupUtils.kt:** Review for potential migration to a KMP-compatible backup utility in shared module. Currently Android-only functionality.
6. **GsCollectionUtils.kt:** Most functionality is in Kotlin stdlib. Migrate any unique extensions to shared module utils.
7. **GsFileUtils.kt / GsStorageUtils.kt:** Superseded by `PlatformFileIOFactory`. Remove.
8. **GsNanoProfiler.kt:** Lightweight profiler could be useful in KMP. Consider migrating to shared module.
9. **All Android-specific utils (GsContextUtils, GsImageUtils, GsIntentUtils, GsResourceUtils, GsUiUtils, WebViewClient, wrappers):** These are Android View system utilities that have no equivalent in Compose Multiplatform. They should stay in `androidApp/` if still needed, otherwise remove.
10. **GsNetworkUtils.kt:** Superseded by Ktor client configuration in shared module. Remove.
11. **GsFileUtilsTest.java:** Coverage is provided by shared module tests. Can be removed when commons is removed.

**Priority:** Medium-High - `androidApp` directly depends on `commons`. Must audit `androidApp` imports to determine which commons utilities are still referenced before removal.

---

## Recommended Migration Order

### Phase 1: Audit and Decouple (Low Risk)
1. Audit `androidApp` source code for imports from `digital.vasic.yole.format.*` (core Java classes) and `digital.vasic.opoc.*` (commons classes)
2. Replace any `core` module imports with equivalent `shared` module imports
3. Move any still-needed commons utilities directly into `androidApp/` source

### Phase 2: Remove `app/` Directory (No Risk)
1. Verify `app/src/main/res/raw/` resources are not referenced by `androidApp`
2. If not referenced, delete `app/` directory entirely
3. If referenced, move the raw resources to `androidApp/src/main/res/raw/`

### Phase 3: Remove `core/` Module (Medium Risk)
1. After Phase 1 confirms no imports from `core`, remove `core` from `settings.gradle.kts`
2. Delete `core/` directory
3. Remove `implementation(project(":core"))` from any build.gradle.kts files

### Phase 4: Remove `commons/` Module (Higher Risk)
1. After Phase 1 confirms all commons utilities are replaced or moved
2. Remove `implementation(project(":commons"))` from `androidApp/build.gradle.kts` and `core/build.gradle.kts`
3. Remove `include(":commons")` from `settings.gradle.kts`
4. Delete `commons/` directory

### Phase 5: Migrate Remaining Concepts to KMP
1. **AppSettings** - Create `shared/src/commonMain/.../model/AppSettings.kt` with expect/actual for persistence
2. **DocumentManager** - Create `shared/src/commonMain/.../model/DocumentManager.kt` for document lifecycle
3. **GsNanoProfiler** - Migrate to `shared/src/commonMain/.../util/NanoProfiler.kt`
4. **GsBackupUtils** - Create KMP backup/restore abstraction if needed

## Dependencies to Update

When removing legacy modules, update these files:
- `settings.gradle.kts` - Remove `include(":commons")` and `include(":core")`
- `androidApp/build.gradle.kts` - Remove `implementation(project(":commons"))`
- `core/build.gradle.kts` - Remove entirely
- `commons/build.gradle.kts` - Remove entirely

## Risk Assessment

| Module | Risk Level | Reason |
|--------|-----------|--------|
| `app/` | None | No code, only raw resources that may not be referenced |
| `core/` | Low | Functionality fully duplicated in shared module |
| `commons/` | Medium | `androidApp` has a direct dependency; requires import audit |
