# SAF-First File Save Fix — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix file saving on Android 9-16 by migrating to Storage Access Framework (SAF) with persistent URIs, and add multi-API-level instrumentation tests with CI emulator definitions.

**Architecture:** Platform-agnostic `FileHandle` expect/actual for all file I/O. Android actual uses `ContentResolver` with `takePersistableUriPermission`. EditorTab stores SAF URI strings. Desktop actual uses `java.io.File`. 8 AVD configs + Docker emulator container for CI.

**Tech Stack:** Kotlin Multiplatform (expect/actual), Android ContentResolver/SAF, Docker emulator, Go challenge tests.

---

### Task 1: Create FileHandle expect declaration

**Files:**
- Create: `shared/src/commonMain/kotlin/digital/vasic/yole/util/FileStorage.kt`

- [ ] **Step 1: Write the expect declaration**

```kotlin
/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Platform-agnostic file storage interface.
 * Each platform provides an actual implementation.
 *
 *########################################################*/
package digital.vasic.yole.util

/**
 * Platform-specific file handle wrapping a URI.
 * Android uses SAF ContentResolver; Desktop uses java.io.File.
 */
expect class FileHandle(uri: String) {
    val uri: String
}

/**
 * Read all bytes from the file handle.
 * Returns null if the file cannot be read or does not exist.
 */
expect fun FileHandle.readBytes(): ByteArray?

/**
 * Write bytes to the file handle, truncating existing content.
 * Returns true on success, false on failure.
 */
expect fun FileHandle.writeBytes(data: ByteArray): Boolean

/**
 * Check whether the file exists and is accessible.
 */
expect fun FileHandle.exists(): Boolean

/**
 * Get the display name (filename) of the file, or null if unavailable.
 * On Android, this maps to OpenableColumns.DISPLAY_NAME.
 */
expect fun FileHandle.displayName(): String?
```

- [ ] **Step 2: Verify compilation fails (no actual yet)**

Run: `./gradlew :shared:compileKotlinAndroid --no-daemon 2>&1 | grep "expect.*actual" || echo "Expected compilation error"`
Expected: Compilation fails because no actual declarations exist

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/digital/vasic/yole/util/FileStorage.kt
git commit -m "feat(file): add FileHandle expect declaration for platform file I/O"
```

---

### Task 2: Create Desktop actual implementation

**Files:**
- Create: `shared/src/desktopMain/kotlin/digital/vasic/yole/util/FileStorage.desktop.kt`

- [ ] **Step 1: Write the desktop actual**

```kotlin
/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop (JVM) file storage implementation using java.io.File.
 *
 *########################################################*/
package digital.vasic.yole.util

import java.io.File

actual class FileHandle(uri: String) {
    private val file: File = File(uri)
    actual val uri: String get() = file.absolutePath
}

actual fun FileHandle.readBytes(): ByteArray? {
    return try {
        file.readBytes()
    } catch (_: Exception) {
        null
    }
}

actual fun FileHandle.writeBytes(data: ByteArray): Boolean {
    return try {
        file.parentFile?.mkdirs()
        file.writeBytes(data)
        true
    } catch (_: Exception) {
        false
    }
}

actual fun FileHandle.exists(): Boolean {
    return file.exists()
}

actual fun FileHandle.displayName(): String? {
    return file.name
}
```

- [ ] **Step 2: Verify desktop compiles**

Run: `./gradlew :shared:compileKotlinDesktop --no-daemon 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/desktopMain/kotlin/digital/vasic/yole/util/FileStorage.desktop.kt
git commit -m "feat(file): add Desktop actual for FileHandle using java.io.File"
```

---

### Task 3: Create Android actual implementation

**Files:**
- Create: `shared/src/androidMain/kotlin/digital/vasic/yole/util/FileStorage.android.kt`

- [ ] **Step 1: Write the Android actual**

```kotlin
/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Android file storage using ContentResolver (SAF).
 *
 *########################################################*/
package digital.vasic.yole.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider

actual class FileHandle(uri: String) {
    actual val uri: String = uri
    private val parsedUri: Uri? = try {
        Uri.parse(uri)
    } catch (_: Exception) {
        null
    }

    internal fun getAndroidUri(): Uri? = parsedUri
}

actual fun FileHandle.readBytes(): ByteArray? {
    val androidUri = getAndroidUri() ?: return null
    val context = AppContextHolder.context ?: return null
    return try {
        context.contentResolver.openInputStream(androidUri)?.use { it.readBytes() }
    } catch (_: Exception) {
        null
    }
}

actual fun FileHandle.writeBytes(data: ByteArray): Boolean {
    val androidUri = getAndroidUri() ?: return false
    val context = AppContextHolder.context ?: return false
    return try {
        context.contentResolver.openOutputStream(androidUri, "wt")?.use { out ->
            out.write(data)
        }
        true
    } catch (_: Exception) {
        false
    }
}

actual fun FileHandle.exists(): Boolean {
    val androidUri = getAndroidUri() ?: return false
    val context = AppContextHolder.context ?: return false
    return try {
        context.contentResolver.query(androidUri, null, null, null, null)?.use {
            it.count > 0
        } ?: false
    } catch (_: Exception) {
        false
    }
}

actual fun FileHandle.displayName(): String? {
    val androidUri = getAndroidUri() ?: return null
    val context = AppContextHolder.context ?: return null
    return try {
        context.contentResolver.query(androidUri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Simple Application context holder for SAF operations.
 * Must be initialized in Application.onCreate().
 */
object AppContextHolder {
    @Volatile
    var context: Context? = null
}
```

- [ ] **Step 2: Initialize AppContextHolder in Android app**

Modify `androidApp/src/main/java/digital/vasic/yole/android/MainActivity.kt`:
Add after `super.onCreate()`:
```kotlin
AppContextHolder.context = applicationContext
```

Add import:
```kotlin
import digital.vasic.yole.util.AppContextHolder
```

- [ ] **Step 3: Verify Android compiles**

Run: `./gradlew :shared:compileKotlinAndroid --no-daemon 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add shared/src/androidMain/kotlin/digital/vasic/yole/util/FileStorage.android.kt
git add androidApp/src/main/java/digital/vasic/yole/android/MainActivity.kt
git commit -m "feat(file): add Android actual for FileHandle using ContentResolver/SAF"
```

---

### Task 4: Create stubs for iOS and WASM

**Files:**
- Create: `shared/src/iosMain/kotlin/digital/vasic/yole/util/FileStorage.ios.kt`
- Create: `shared/src/wasmJsMain/kotlin/digital/vasic/yole/util/FileStorage.wasmJs.kt`

- [ ] **Step 1: Write iOS stub**

```kotlin
package digital.vasic.yole.util

actual class FileHandle(uri: String) {
    actual val uri: String = uri
}

actual fun FileHandle.readBytes(): ByteArray? = null
actual fun FileHandle.writeBytes(data: ByteArray): Boolean = false
actual fun FileHandle.exists(): Boolean = false
actual fun FileHandle.displayName(): String? = null
```

- [ ] **Step 2: Write WASM stub (same content, different file)**

```kotlin
package digital.vasic.yole.util

actual class FileHandle(uri: String) {
    actual val uri: String = uri
}

actual fun FileHandle.readBytes(): ByteArray? = null
actual fun FileHandle.writeBytes(data: ByteArray): Boolean = false
actual fun FileHandle.exists(): Boolean = false
actual fun FileHandle.displayName(): String? = null
```

- [ ] **Step 3: Verify all targets compile**

Run: `./gradlew :shared:compileKotlinIosArm64 :shared:compileKotlinWasmJs --no-daemon 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL (warnings about disabled iOS targets on non-Mac are expected)

- [ ] **Step 4: Commit**

```bash
git add shared/src/iosMain/kotlin/digital/vasic/yole/util/FileStorage.ios.kt
git add shared/src/wasmJsMain/kotlin/digital/vasic/yole/util/FileStorage.wasmJs.kt
git commit -m "feat(file): add iOS/WASM stubs for FileHandle"
```

---

### Task 5: Update EditorTab to use SAF URI

**Files:**
- Modify: `androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt:247-253`

- [ ] **Step 1: Update EditorTab data class**

Replace the existing `EditorTab` definition (line 247) with:

```kotlin
data class EditorTab(
    val fileName: String,
    val content: String,
    val isDirty: Boolean = false,
    val contentUri: String? = null  // SAF URI for persistent file access
)
```

- [ ] **Step 2: Update all EditorTab construction sites**

Find all `EditorTab(` and ensure they pass `contentUri`. Default `null` means none need immediate change unless they had `filePath` or `safUri` params.

Run to find all uses: `grep -n "EditorTab(" androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt`

Replace any `filePath = ...` or `safUri = ...` with `contentUri = ...`:
```kotlin
// Before:
EditorTab(fileName = name, content = content, filePath = path)
// After:
EditorTab(fileName = name, content = content, contentUri = path)
```

- [ ] **Step 3: Compile check**

Run: `./gradlew :androidApp:compileDebugKotlin --no-daemon 2>&1 | grep "^e:" | head -5`
Expected: No errors (might have unused param warnings, those are OK)

- [ ] **Step 4: Commit**

```bash
git add androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt
git commit -m "refactor(tab): replace filePath/safUri with unified contentUri in EditorTab"
```

---

### Task 6: Rewrite saveFile to use FileHandle

**Files:**
- Modify: `androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt:168-188`

- [ ] **Step 1: Replace saveFile function**

Replace the existing `saveFile` function (lines 168-188) with:

```kotlin
fun saveFile(context: Context, contentUri: String?, content: String, fileName: String): Pair<Boolean, String?> {
    return try {
        if (contentUri != null) {
            val handle = FileHandle(contentUri)
            val ok = handle.writeBytes(content.toByteArray())
            if (ok) Pair(true, contentUri) else Pair(false, null)
        } else {
            // No URI yet - write to app-private cache for auto-save
            val cacheDir = File(context.filesDir, "autosave")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val cacheFile = File(cacheDir, fileName)
            cacheFile.writeText(content)
            Pair(true, null) // true = saved, null = still no SAF URI
        }
    } catch (e: Exception) {
        Pair(false, null)
    }
}
```

Add import at top of file:
```kotlin
import digital.vasic.yole.util.FileHandle
```

- [ ] **Step 2: Update save calls throughout YoleApp.kt**

Find all `saveFile(context, ` calls and update to new signature:

```kotlin
// Before:
saveFile(context, "", fileContent, currentTab.safUri)
saveFile(context, currentTab.filePath, fileContent)
saveFile(context, filePath, fileContent)

// After:
saveFile(context, currentTab.contentUri, fileContent, currentTab.fileName)
saveFile(context, currentTab.contentUri, fileContent, currentTab.fileName)
saveFile(context, currentTab.contentUri, fileContent, fileName)
```

Update save result handling - return type is now `Pair<Boolean, String?>`:
```kotlin
val (saved, newUri) = saveFile(context, currentTab.contentUri, fileContent, currentTab.fileName)
if (saved) {
    if (newUri != null && currentTab.contentUri != newUri) {
        openTabs = openTabs.mapIndexed { index, tab ->
            if (index == activeTabIndex) tab.copy(contentUri = newUri, isDirty = false)
            else tab
        }
    } else {
        openTabs = openTabs.mapIndexed { index, tab ->
            if (index == activeTabIndex) tab.copy(isDirty = false) else tab
        }
    }
    if (settings.getAnnounceChanges()) {
        Toast.makeText(context, "File saved", Toast.LENGTH_SHORT).show()
    }
} else {
    Toast.makeText(context, "Save failed", Toast.LENGTH_LONG).show()
}
```

- [ ] **Step 3: Compile check**

Run: `./gradlew :androidApp:compileDebugKotlin --no-daemon 2>&1 | grep "^e:" | head -10`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt
git commit -m "fix(save): migrate saveFile to FileHandle with SAF support"
```

---

### Task 7: Write shared unit tests for FileHandle contract

**Files:**
- Create: `shared/src/commonTest/kotlin/digital/vasic/yole/util/FileStorageContractTests.kt`

- [ ] **Step 1: Write contract tests**

```kotlin
/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contract tests for FileHandle interface.
 *
 *########################################################*/
package digital.vasic.yole.util

import kotlin.test.*

class FileStorageContractTests {

    @Test
    fun `FileHandle stores uri`() {
        val handle = FileHandle("test://path/file.txt")
        assertEquals("test://path/file.txt", handle.uri)
    }

    @Test
    fun `null or empty content write returns false for invalid uri`() {
        val handle = FileHandle("invalid://")
        val result = handle.writeBytes(ByteArray(0))
        // Invalid URI should return false, not throw
        assertFalse(result)
    }

    @Test
    fun `readBytes on non-existent uri returns null`() {
        val handle = FileHandle("nonexistent://file.txt")
        val result = handle.readBytes()
        assertNull(result)
    }

    @Test
    fun `exists on non-existent uri returns false`() {
        val handle = FileHandle("nonexistent://file.txt")
        assertFalse(handle.exists())
    }

    @Test
    fun `displayName on non-existent uri returns null`() {
        val handle = FileHandle("nonexistent://file.txt")
        assertNull(handle.displayName())
    }

    @Test
    fun `handle uri is immutable after construction`() {
        val uri = "content://test/file.txt"
        val handle = FileHandle(uri)
        assertEquals(uri, handle.uri)
        // Re-read to verify immutability
        assertEquals(uri, handle.uri)
    }
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew :shared:desktopTest --tests "digital.vasic.yole.util.FileStorageContractTests" --no-daemon 2>&1 | tail -10`
Expected: All 6 tests PASS

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonTest/kotlin/digital/vasic/yole/util/FileStorageContractTests.kt
git commit -m "test(file): add FileHandle contract tests"
```

---

### Task 8: Write desktop-specific FileHandle tests

**Files:**
- Create: `shared/src/desktopTest/kotlin/digital/vasic/yole/util/FileStorageDesktopTests.kt`

- [ ] **Step 1: Write desktop roundtrip test**

```kotlin
/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop-specific FileHandle tests with real filesystem.
 *
 *########################################################*/
package digital.vasic.yole.util

import java.io.File
import kotlin.test.*

class FileStorageDesktopTests {

    private fun tempFile(): File {
        val tmp = File(System.getProperty("java.io.tmpdir"), "yole_test_${System.currentTimeMillis()}.txt")
        tmp.deleteOnExit()
        return tmp
    }

    @Test
    fun `write and read roundtrip on real file`() {
        val file = tempFile()
        val handle = FileHandle(file.absolutePath)
        val content = "Hello, Yole!\nSecond line."
        val bytes = content.toByteArray()

        val wrote = handle.writeBytes(bytes)
        assertTrue(wrote, "Write should succeed")
        assertTrue(file.exists(), "File should exist on disk")

        val read = handle.readBytes()
        assertNotNull(read, "Read should return data")
        assertEquals(content, String(read), "Content should match")

        val name = handle.displayName()
        assertTrue(name?.endsWith(".txt") == true, "Display name should end with .txt")
    }

    @Test
    fun `write overwrites existing content`() {
        val file = tempFile()
        file.writeText("original content")
        val handle = FileHandle(file.absolutePath)

        val newContent = "overwritten content"
        handle.writeBytes(newContent.toByteArray())

        val read = handle.readBytes()
        assertEquals(newContent, String(read!!))
    }

    @Test
    fun `readBytes returns null for non-existent file`() {
        val handle = FileHandle("/tmp/yole_nonexistent_${System.currentTimeMillis()}.txt")
        assertNull(handle.readBytes())
    }

    @Test
    fun `exists returns false for non-existent file`() {
        val handle = FileHandle("/tmp/yole_nonexistent_${System.currentTimeMillis()}.txt")
        assertFalse(handle.exists())
    }

    @Test
    fun `write creates parent directories`() {
        val dir = File(System.getProperty("java.io.tmpdir"), "yole_nested_${System.currentTimeMillis()}")
        val file = File(dir, "sub/test.txt")
        val handle = FileHandle(file.absolutePath)

        val ok = handle.writeBytes("nested".toByteArray())
        assertTrue(ok, "Write should create parent dirs and succeed")
        assertTrue(file.exists(), "File should exist at nested path")

        file.delete()
        File(file.parent).delete()
        dir.delete()
    }
}
```

- [ ] **Step 2: Run desktop tests**

Run: `./gradlew :shared:desktopTest --tests "digital.vasic.yole.util.FileStorageDesktopTests" --no-daemon 2>&1 | tail -10`
Expected: All 5 tests PASS

- [ ] **Step 3: Commit**

```bash
git add shared/src/desktopTest/kotlin/digital/vasic/yole/util/FileStorageDesktopTests.kt
git commit -m "test(file): add Desktop-specific FileHandle roundtrip tests"
```

---

### Task 9: Create AVD config files for Android 9-16

**Files:**
- Create: `Containers/images/android-test/avds/config_api28.ini`
- Create: `Containers/images/android-test/avds/config_api29.ini`
- Create: `Containers/images/android-test/avds/config_api30.ini`
- Create: `Containers/images/android-test/avds/config_api31.ini`
- Create: `Containers/images/android-test/avds/config_api33.ini`
- Create: `Containers/images/android-test/avds/config_api34.ini`
- Create: `Containers/images/android-test/avds/config_api35.ini`
- Create: `Containers/images/android-test/avds/config_api36.ini`

- [ ] **Step 1: Create AVD configs**

Each config.ini contains (example for API 28):

```ini
avd.ini.encoding=UTF-8
AvdId=yole_test_api28
abi.type=x86_64
hw.cpu.ncore=4
hw.ramSize=2048
hw.lcd.density=420
hw.lcd.height=1920
hw.lcd.width=1080
sdcard.size=512M
tag.display=Google APIs
tag.id=google_apis
image.sysdir.1=system-images/android-28/google_apis/x86_64/
```

Create the remaining 7 files with the same structure, changing:
- `AvdId`: `yole_test_apiXX`
- `image.sysdir.1`: `system-images/android-{API}/google_apis/x86_64/`

- [ ] **Step 2: Commit**

```bash
git -C Containers add images/android-test/avds/ && git -C Containers commit -m "feat(ci): add AVD configs for Android API 28-36"
```

---

### Task 10: Create Android emulator Docker container

**Files:**
- Create: `Containers/images/android-test/Dockerfile`

- [ ] **Step 1: Write Dockerfile**

```dockerfile
FROM ghcr.io/vasic-digital/android-emulator-base:latest

ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV EMULATOR_AVD_DIR=/root/.android/avd

# Install system images for each API level
RUN sdkmanager --install \
    "system-images;android-28;google_apis;x86_64" \
    "system-images;android-29;google_apis;x86_64" \
    "system-images;android-30;google_apis;x86_64" \
    "system-images;android-31;google_apis;x86_64" \
    "system-images;android-33;google_apis;x86_64" \
    "system-images;android-34;google_apis;x86_64" \
    "system-images;android-35;google_apis;x86_64"

COPY avds/ $EMULATOR_AVD_DIR/

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
```

- [ ] **Step 2: Create entrypoint.sh**

```bash
#!/bin/bash
set -e
API_LEVELS="28 29 30 31 33 34 35"
RESULTS_DIR="/test-results"
mkdir -p "$RESULTS_DIR"

for API in $API_LEVELS; do
    AVD="yole_test_api${API}"
    echo "=== Testing API ${API} on ${AVD} ==="
    emulator -avd "$AVD" -no-window -no-audio -gpu swiftshader_indirect &
    EMULATOR_PID=$!
    adb wait-for-device
    boot_completed=""
    while [[ "$boot_completed" != "1" ]]; do
        boot_completed=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
        sleep 2
    done
    adb install /apk/yole-android-debug.apk
    adb shell am instrument -w -r -e class digital.vasic.yole.android.SaveTests \
        digital.vasic.yole.android.test/androidx.test.runner.AndroidJUnitRunner \
        > "$RESULTS_DIR/api${API}_results.txt" 2>&1 || true
    adb emu kill
    wait $EMULATOR_PID 2>/dev/null || true
done

echo "All API levels tested. Results in $RESULTS_DIR"
```

- [ ] **Step 3: Commit**

```bash
git -C Containers add images/android-test/Dockerfile images/android-test/entrypoint.sh
git -C Containers commit -m "feat(ci): add Android emulator Docker container for API 28-35"
```

---

### Task 11: Create Go challenge test for save verification

**Files:**
- Create: `Challenges/challenges/android_save_challenge.go`
- Create: `Challenges/scripts/android_save_challenge.sh`

- [ ] **Step 1: Write Go challenge runner**

```go
package challenges

import (
    "context"
    "fmt"
    "os"
    "os/exec"
    "strings"
    "testing"
    "time"
)

func TestAndroidSave_AllApiLevels(t *testing.T) {
    apiLevels := []string{"28", "29", "30", "31", "33", "34", "35"}
    for _, api := range apiLevels {
        api := api
        t.Run("API"+api, func(t *testing.T) {
            t.Parallel()
            ctx, cancel := context.WithTimeout(context.Background(), 10*time.Minute)
            defer cancel()

            // Start emulator and run instrumented tests
            cmd := exec.CommandContext(ctx, "bash",
                "scripts/android_save_challenge.sh", api)
            cmd.Env = append(os.Environ(),
                fmt.Sprintf("AVD_NAME=yole_test_api%s", api),
                fmt.Sprintf("API_LEVEL=%s", api),
            )
            output, err := cmd.CombinedOutput()
            outStr := string(output)

            if err != nil {
                t.Errorf("API %s save test failed: %v\nOutput:\n%s", api, err, outStr)
                return
            }

            // Anti-bluff: must have positive evidence
            if !strings.Contains(outStr, "SAVE_VERIFIED:") {
                t.Errorf("API %s: no SAVE_VERIFIED evidence in output (CONST-035)", api)
            }
            t.Logf("API %s: PASS with evidence", api)
        })
    }
}
```

- [ ] **Step 2: Write shell wrapper**

```bash
#!/bin/bash
# android_save_challenge.sh - Run save tests on a specific API level emulator

API="${1:-28}"
AVD="${AVD_NAME:-yole_test_api${API}}"
APK="${2:-androidApp/build/outputs/apk/debug/androidApp-debug.apk}"
RESULTS_FILE="/tmp/yole_save_api${API}_results.txt"

echo "=== Android Save Challenge - API ${API} ==="
echo "AVD: ${AVD}"
echo "APK: ${APK}"

adb -e wait-for-device
adb -e install -r "${APK}"
adb -e shell am instrument -w -r \
    -e class digital.vasic.yole.android.SaveTests \
    digital.vasic.yole.android.test/androidx.test.runner.AndroidJUnitRunner \
    > "${RESULTS_FILE}" 2>&1

# Parse results for anti-bluff evidence
if grep -q "FAILURES" "${RESULTS_FILE}"; then
    echo "FAIL: Tests failed on API ${API}"
    cat "${RESULTS_FILE}"
    exit 1
fi

# Extract file size evidence
EVIDENCE=$(grep -oP 'SAVE_VERIFIED: \d+ bytes' "${RESULTS_FILE}" | head -5)
if [ -z "${EVIDENCE}" ]; then
    echo "FAIL: No SAVE_VERIFIED evidence (CONST-035 violation) on API ${API}"
    exit 1
fi

echo "${EVIDENCE}"
echo "PASS: API ${API} save tests verified"
exit 0
```

- [ ] **Step 3: Commit**

```bash
git -C Challenges add challenges/android_save_challenge.go scripts/android_save_challenge.sh
git -C Challenges commit -m "feat(challenge): add Android save verification challenge across API levels"
```

---

### Task 12: Write Android instrumentation save tests

**Files:**
- Create: `androidApp/src/androidTest/kotlin/digital/vasic/yole/android/SaveTests.kt`

- [ ] **Step 1: Write instrumented save tests**

```kotlin
/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Instrumented tests for SAF-based file saving.
 * Runs on real devices and emulators.
 *
 *########################################################*/
package digital.vasic.yole.android

import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import digital.vasic.yole.util.FileHandle
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class SaveTests {

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java, false, false)

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun saveToCacheAndReadBack() {
        val fileName = "save_test_${System.currentTimeMillis()}.txt"
        val content = "Hello from Yole save test!\nAPI level: ${android.os.Build.VERSION.SDK_INT}"
        val cacheDir = File(context.filesDir, "autosave")
        cacheDir.mkdirs()
        val cacheFile = File(cacheDir, fileName)

        // Write
        cacheFile.writeText(content)
        assertTrue("File should exist after write", cacheFile.exists())

        // Read back via FileHandle
        val handle = FileHandle(Uri.fromFile(cacheFile).toString())
        val read = handle.readBytes()
        assertNotNull("Read should return content", read)
        assertEquals(content, String(read!!))

        // Evidence
        println("SAVE_VERIFIED: ${read.size} bytes")

        // Cleanup
        cacheFile.delete()
    }

    @Test
    fun writeAndExists() {
        val fileName = "exists_test_${System.currentTimeMillis()}.txt"
        val cacheFile = File(context.filesDir, fileName)
        val uri = Uri.fromFile(cacheFile).toString()

        val handle = FileHandle(uri)

        // Write content
        val wrote = handle.writeBytes("test content".toByteArray())
        assertTrue("Write should succeed", wrote)
        assertTrue("exists() should return true", handle.exists())

        println("SAVE_VERIFIED: ${cacheFile.length()} bytes")

        cacheFile.delete()
    }

    @Test
    fun readNonExistentReturnsNull() {
        val handle = FileHandle(Uri.fromFile(
            File(context.filesDir, "nonexistent_${System.currentTimeMillis()}.txt")
        ).toString())
        assertNull("Non-existent file should return null", handle.readBytes())
    }

    @Test
    fun writeEmptyContent() {
        val f = File(context.filesDir, "empty_${System.currentTimeMillis()}.txt")
        val handle = FileHandle(Uri.fromFile(f).toString())
        val ok = handle.writeBytes(ByteArray(0))
        assertTrue("Empty write should succeed", ok)
        assertTrue("File should exist", f.exists())
        assertEquals(0, f.length().toInt())
        println("SAVE_VERIFIED: 0 bytes")
        f.delete()
    }

    @Test
    fun writeAndReadRoundtrip() {
        val f = File(context.filesDir, "roundtrip_${System.currentTimeMillis()}.txt")
        val uri = Uri.fromFile(f).toString()
        val original = "Roundtrip test content with special chars: äöü ñ 你好"
        val handle1 = FileHandle(uri)
        assertTrue(handle1.writeBytes(original.toByteArray()))

        val handle2 = FileHandle(uri)
        val read = handle2.readBytes()
        assertNotNull(read)
        assertEquals(original, String(read!!))
        println("SAVE_VERIFIED: ${read.size} bytes")
        f.delete()
    }
}
```

- [ ] **Step 2: Add test runner dependency**

In `androidApp/build.gradle.kts`, ensure:
```kotlin
defaultConfig {
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
}

dependencies {
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
```

- [ ] **Step 3: Run instrumented tests on a connected device/emulator**

Run: `./gradlew :androidApp:connectedAndroidTest --no-daemon 2>&1 | tail -20`
Expected: Tests pass on device, or skipped if no device connected

- [ ] **Step 4: Commit**

```bash
git add androidApp/src/androidTest/kotlin/digital/vasic/yole/android/SaveTests.kt
git commit -m "test(save): add Android instrumentation save tests with evidence"
```

---

### Task 13: Run full test suite and push

- [ ] **Step 1: Run all shared tests**

```bash
./gradlew :shared:desktopTest --no-daemon 2>&1 | tail -5
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Verify all platforms compile**

```bash
./gradlew :desktopApp:compileKotlin :androidApp:compileDebugKotlin :webApp:compileKotlinWasmJs --no-daemon 2>&1 | tail -5
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit any remaining changes and push**

```bash
git add -A && git commit -m "chore: finalize SAF save fix implementation"
git push origin master
git -C Containers push github main && git -C Containers push gitlab main
git -C Challenges push github main && git -C Challenges push gitlab main
```
