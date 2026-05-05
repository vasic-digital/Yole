# SAF-First File Save System — Design Spec

> **Feature stream #1** of 5 (Priority: Critical — blocks basic app usage)
> **Status:** Design approved

## Problem

Saving files does not work on Android 16 (API 36). The current `saveFile()` uses
direct `java.io.File` access as fallback, which is blocked by scoped storage
enforcement on Android 11+. New files created in-app have no SAF URI, so they
fall through to the broken path.

## Solution

All file I/O migrates to Storage Access Framework (SAF) via `ContentResolver`.
Direct `File.readText()`/`File.writeText()` is replaced with `ContentResolver`
streams. Every tab stores a persistent SAF URI so saves work across app restarts
and reboots.

---

## Core Architecture

**New shared contract:** `shared/src/commonMain/.../util/FileStorage.kt`
```kotlin
expect class FileHandle(uri: String) {
    val uri: String
}
expect fun FileHandle.readBytes(): ByteArray?
expect fun FileHandle.writeBytes(data: ByteArray): Boolean
expect fun FileHandle.exists(): Boolean
expect fun FileHandle.displayName(): String?
```

**Android actual (androidMain):**
- All operations via `ContentResolver`
- `openInputStream` / `openOutputStream` with `"wt"` mode (truncate)
- Persist URI via `takePersistableUriPermission` (READ + WRITE)
- Grant flags: `FLAG_GRANT_READ_URI_PERMISSION` | `FLAG_GRANT_PERSISTABLE_URI_PERMISSION`

**Desktop actual (desktopMain):**
- Uses `java.io.File` (no SAF on desktop)
- Same `FileHandle` interface

**EditorTab update:**
```kotlin
data class EditorTab(
    val fileName: String,
    val content: String,
    val isDirty: Boolean = false,
    val contentUri: String? = null,  // SAF URI string; null = unsaved
)
```

---

## Permission Flow

### Save new file (no URI)
1. Launch `Intent(ACTION_CREATE_DOCUMENT)` with MIME type `"text/*"`
2. User picks location in system picker
3. On result: `contentResolver.takePersistableUriPermission(uri, READ + WRITE)`
4. Store URI in `tab.contentUri`
5. Write content via `contentResolver.openOutputStream(uri, "wt")`

### Save existing file (has URI)
1. Verify URI accessible via `contentResolver.query()`
2. If `null` (revoked): prompt user to re-authorize via `ACTION_OPEN_DOCUMENT`
3. Write via `contentResolver.openOutputStream(uri, "wt")`

### Open existing file
1. Launch `Intent(ACTION_OPEN_DOCUMENT)` with `"text/*"` MIME
2. On result: `takePersistableUriPermission`
3. Read via `contentResolver.openInputStream(uri)`
4. Auto-detect format from `cursor.getString(OpenableColumns.DISPLAY_NAME)`

### Auto-Save
- Every 30s if `tab.isDirty`
- If `tab.contentUri != null`: write to SAF URI
- If `tab.contentUri == null`: write to app-private cache (`context.filesDir/autosave/`)
- Show Toast on save (with accessibility announcement)

---

## Android API Compatibility

| API Level | Android | SAF Support | Notes |
|-----------|---------|-------------|-------|
| 28 | 9 (Pie) | Since API 19 | Legacy file access still works |
| 29 | 10 | Since API 19 | Transition point for scoped storage |
| 30 | 11 | Required | Scoped storage enforced for shared dirs |
| 31 | 12 | Required | `MANAGE_EXTERNAL_STORAGE` needed for broad access |
| 32 | 12L | Required | Large screen optimizations |
| 33 | 13 | Required | Granular media permissions |
| 34 | 14 | Required | Full scoped storage |
| 35 | 15 | Required | Latest stable |
| 36 | 16 (preview) | Required | Pre-release; fix must work here |

---

## Emulator Test Matrix

8 AVD definitions in `Containers/images/android-test/avds/`:

| File | API | Android | Purpose |
|------|-----|---------|---------|
| `config_api28.ini` | 28 | 9 | Legacy mode baseline |
| `config_api29.ini` | 29 | 10 | Transition testing |
| `config_api30.ini` | 30 | 11 | Scoped storage starts |
| `config_api31.ini` | 31 | 12 | |
| `config_api33.ini` | 33 | 13 | |
| `config_api34.ini` | 34 | 14 | |
| `config_api35.ini` | 35 | 15 | Latest stable |
| `config_api36.ini` | 36 | 16 | Pre-release target |

Each AVD name: `yole_test_apiXX`

CI container in `Containers/images/android-test/Dockerfile`:
- Based on `ghcr.io/vasic-digital/android-emulator-base:latest`
- Runs `adb install` + `adb shell am instrument` per API level
- Collects test XMLs to `test-results/api{XX}/`

---

## Test Plan

### Shared Unit Tests (`shared/src/commonTest/`)

| Test | What it verifies |
|------|-----------------|
| `FileHandle_empty_content` | Writing zero bytes succeeds |
| `FileHandle_large_content` | Writing 1MB text succeeds |
| `FileHandle_roundtrip` | Write then read returns same content |
| `FileHandle_null_uri` | Graceful handling of null URIs |

### Android Instrumentation Tests (`androidApp/src/androidTest/`)

| Test | What it verifies |
|------|-----------------|
| `SaveNewFile_createsAndReads` | SAF-create, write, read back, assert content match |
| `SaveExistingFile_modifiesAndPersists` | Open, modify, save, reopen, verify |
| `AutoSave_writesToCache` | New file auto-saves to `filesDir/autosave/` |
| `PermissionRevoked_rePrompts` | Save to revoked URI, verify `ACTION_OPEN_DOCUMENT` fires |
| `ConcurrentSaves_noCorruption` | Two saves in parallel, final content is complete |
| `CrossApiSaveTest` | Parametrized: runs on each AVD, creates/saves/reads/verifies |

### Challenge Tests (`Challenges/`)

| Challenge | What it verifies |
|-----------|-----------------|
| `android_save_challenge.sh` | Orchestrates save tests across all 8 AVDs |
| `android_file_roundtrip.go` | Go test that writes, reads, compares on connected device |

### Anti-Bluff Anchors

Every test PROVES the feature works end-to-end:
- Creates a file with known content → saves → opens → asserts content unchanged
- Creates a file → modifies → saves → opens → asserts modification persisted
- On each API level: records API level + file size + content hash → asserts all match

Metadata-only PASS (configuration check, grep-based, absence-of-error) is forbidden per CONST-035.

---

## Files to Modify

| File | Change |
|------|--------|
| `shared/src/commonMain/.../util/FileStorage.kt` | NEW: expect FileHandle |
| `shared/src/androidMain/.../util/FileStorage.android.kt` | NEW: actual SAF impl |
| `shared/src/desktopMain/.../util/FileStorage.desktop.kt` | NEW: actual File impl |
| `shared/src/iosMain/.../util/FileStorage.ios.kt` | NEW: stub |
| `shared/src/wasmJsMain/.../util/FileStorage.wasmJs.kt` | NEW: stub |
| `androidApp/.../YoleApp.kt` | Update: saveFile/loadFile/EditorTab |
| `androidApp/src/androidTest/.../SaveTests.kt` | NEW: androidTest suite |
| `Containers/images/android-test/Dockerfile` | NEW: emulator container |
| `Containers/images/android-test/avds/config_api*.ini` | NEW: 8 AVD configs |
| `Containers/tests/integration/android_save_test.go` | NEW: Go CI test |
| `Challenges/scripts/android_save_challenge.sh` | NEW: challenge wrapper |

---

## Acceptance Criteria

1. New files save successfully on Android 9 through Android 16
2. Existing files open, modify, save, reopen with content preserved
3. Auto-save writes to cache for unsaved files
4. Permission revocation triggers re-authorization prompt
5. All 8 API levels pass instrumentation tests with evidence
6. Challenges pass with positive runtime evidence per CONST-035
7. Desktop file save continues to work unchanged
