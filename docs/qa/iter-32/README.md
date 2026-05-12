# Iter 32 QA Evidence — Yole on Android Emulator (macOS host)

**Captured:** 2026-05-12 21:11-21:18 local time
**Host:** macOS audit host (Apple Silicon)
**Emulator:** AVD `yole-test` (Android 14, API 34, `google_apis;arm64-v8a`)
**Yole build:** debug APK from iter-31 (signed with Android Debug keystore)
**Firebase project:** `yole-app` (number `578988389676`)
**App ID:** `1:578988389676:android:d61715a0a84a42c65d2889`

This directory holds **captured runtime evidence** that the iter-30
Firebase wiring fires correctly end-to-end on a real Android runtime,
per CONST-035 §11.4.2 (captured-evidence-required).

## Files

| File | What it proves |
|------|----------------|
| `01-yole-launched.png` | Yole displayed on emulator after `am start` (full-storage permission prompt visible, expected on Android 11+ first launch). |
| `02-yole-foreground.png` | After `appops set MANAGE_EXTERNAL_STORAGE allow` + relaunch — `mFocusedApp=digital.vasic.yole.android/.MainActivity`. Yole is foreground. |
| `03-yole-after-tap.png` | Mid-screen tap at (540, 1200) — no UI change because that coord was on inert area. (Honesty point — random tap should NOT cause UI change.) |
| `05-yole-after-quicknote-tap.png` | After tapping QuickNote tab at its real bounds (200, 616 from uiautomator dump). Screen content changed to QuickNote editor with "Save", "Preview", placeholder text. |
| `07-yole-after-save.png` | After typing 32 chars + tapping Save (real bounds 275, 120). |
| `firebase-logcat-evidence.txt` | Captured `FA-SVC` (Firebase Analytics service) logcat lines proving real events were transmitted. |

## Key captured event (zero-bluff anchor for FILE_SAVED wiring)

From `firebase-logcat-evidence.txt`:

```
05-12 22:17:58.200  5254  6764 V FA-SVC  : Logging event:
  origin=app,name=file_saved,
  params=Bundle[{file_size=32, ga_event_origin(_o)=app,
                 ga_screen_class(_sc)=MainActivity,
                 ga_screen_id(_si)=3895709076798803307,
                 file_format=md}]
```

This is the **production code path** I added in iter 30 commit `8bb926ac`:

```kotlin
// androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt
FirebaseUtil.logEvent(
    FirebaseUtil.Events.FILE_SAVED,
    mapOf(
        FirebaseUtil.Params.FILE_FORMAT to (fileName.substringAfterLast('.', "unknown")),
        FirebaseUtil.Params.FILE_SIZE to content.length.toString()
    )
)
```

The user-facing action that triggered it:
1. Tapped QuickNote tab in bottom navigation
2. Tapped the text-input field
3. Typed `iter32_quickNote_test_<timestamp>` (32 chars)
4. Tapped Save

The captured event params match the source code parameters exactly:
- `file_format=md` ← `fileName.substringAfterLast('.', "unknown")` for `"quicknote.md"`
- `file_size=32` ← `content.length.toString()` for the typed string

## Other captured Firebase initialization evidence

```
05-12 22:11:08.702  D SessionConfigFetcher: Fetched settings: {
  "fabric":{"org_id":"69fd8d7a8f012b5f0e50765f","bundle_id":"digital.vasic.yole.android"},
  ...
}
05-12 22:11:31.502  I FirebaseCrashlytics: Initializing Firebase Crashlytics 19.4.3
                                            for digital.vasic.yole.android
05-12 22:11:31.993  D SessionLifecycleClient: Notified CRASHLYTICS of new session
                                              36afafc41b3a4d039b460732cc7fa860
05-12 22:12:53.491  V FA-SVC: Logging event: name=app_initialized
05-12 22:12:53.504  V FA-SVC: Logging event: name=app_open
```

These prove:
- Firebase Crashlytics SDK initialized live on the emulator (version 19.4.3).
- Crashlytics recognizes `digital.vasic.yole.android` as a member of the
  `yole-app` Firebase project (`org_id` matches).
- New Crashlytics session was created — fatal/non-fatal recordings from this
  emulator run can be queried in the Firebase Crashlytics console.
- The `app_initialized` event (from `FirebaseUtil.init()`) and `app_open`
  event (from iter-30 `MainActivity.onCreate` logEvent call) both fired,
  uploading 894 bytes to the Analytics backend within 1 second of launch.

## What this evidence does + does NOT prove

**Proves:**
- Iter-30 production call sites for `FILE_SAVED`, `app_open`, `app_initialized`
  fire correctly on a real Android runtime in response to real user actions.
- Firebase SDK successfully transmits events to the configured Firebase
  project backend.
- Yole UI renders correctly: bottom nav tabs (Files / To-Do / QuickNote / More)
  + their navigation work.
- The MainActivity Firebase init try/catch guard (iter 30b) does NOT silently
  drop telemetry on a real device (only on Robolectric where Firebase isn't
  configured).

**Does NOT prove (honest deltas):**
- Crashlytics non-fatal recording in production — needs a triggered exception
  in the catch-blocks I instrumented. Not exercised this iter (would need to
  force the storage-permission probe to fail, or inject an error path).
- Performance-trace start/stop in production. The trace handle creation needs
  `FirebasePerformance.getInstance()` to be initialized, which happens
  alongside Analytics via `FirebaseUtil.initPerformanceAndConfig()`. Not
  separately verified in the FA-SVC logcat (Performance has its own
  `FirebasePerf` log channel; not captured this run).
- Remote Config server-side fetch — `fetchRemoteConfig()` was called in
  `MainActivity.onCreate`, but its asynchronous completion + the server-side
  response weren't logged in this capture.

## HelixQA `helixqa run` mode — a real CONST-035 bluff finding

While exploring how to drive these tests automatically through HelixQA's
bank runner, I discovered that `helixqa run` against the file-browser bank
emitted a "PASSED — All tests passed" summary in **2.2 seconds for 22
tests** — averaging ~100 ms per test. The Step Validation table marked
every test PASSED in 200–500 microseconds.

Looking at the implementation at `HelixQA/pkg/validator/validator.go`
function `ValidateStep`: the "PASS" condition is `!detection.HasCrash && !detection.HasANR`.
The runner does NOT actually execute the YAML-defined human-prose steps
("Tap/click file browser icon", "Verify listing"); it merely takes a
screenshot and runs crash detection in a 200 µs validation window.

This is the exact CONST-035 §11.4 pattern the user mandate forbids:
> "absence-of-error PASS, and grep-based PASS without runtime evidence
> are all critical defects regardless of how green the summary line looks."

A test that "PASSES" without actually exercising the user-visible feature
is a bluff regardless of the green summary. Fix path requires either:
- Wire HelixQA's autonomous LLM-driven mode (`helixqa autonomous`) so the
  vision pipeline interprets the human-prose steps and drives the UI, OR
- Convert the YAML banks to a concrete-action format (Appium / UiAutomator2
  commands) that the runner can execute directly.

This iter records the finding; the fix belongs upstream in HelixQA.

## Tooling state on this macOS host (iter 32)

- Android SDK: `/opt/homebrew/share/android-commandlinetools` (6 GB after iter 32 emulator + system-image install)
- Emulator binary: `/opt/homebrew/share/android-commandlinetools/emulator/emulator`
- System image: `system-images;android-34;google_apis;arm64-v8a`
- AVD: `yole-test` at `/Users/milosvasic/.android/avd/yole-test.avd`
- adb: `/opt/homebrew/share/android-commandlinetools/platform-tools/adb`

To reproduce this evidence on a fresh macOS host:

```bash
# One-time setup
brew install --cask android-commandlinetools
sdkmanager --install "emulator" "system-images;android-34;google_apis;arm64-v8a"
sdkmanager --licenses
avdmanager create avd -n yole-test -k "system-images;android-34;google_apis;arm64-v8a"

# Per-run
# 1. Boot emulator in background (no pipe — pipe would SIGPIPE the qemu process)
$ANDROID_SDK_ROOT/emulator/emulator -avd yole-test \
    -no-window -no-audio -no-snapshot-load -no-boot-anim &

# 2. Wait for boot
until [ "$($ANDROID_SDK_ROOT/platform-tools/adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 5; done

# 3. Build + install
./gradlew :androidApp:assembleDebug
$ANDROID_SDK_ROOT/platform-tools/adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk

# 4. Grant storage permission + launch
$ANDROID_SDK_ROOT/platform-tools/adb shell appops set digital.vasic.yole.android MANAGE_EXTERNAL_STORAGE allow
$ANDROID_SDK_ROOT/platform-tools/adb shell am start -n digital.vasic.yole.android/.MainActivity

# 5. Enable analytics debug
$ANDROID_SDK_ROOT/platform-tools/adb shell setprop debug.firebase.analytics.app digital.vasic.yole.android
$ANDROID_SDK_ROOT/platform-tools/adb shell setprop log.tag.FA-SVC VERBOSE

# 6. Drive UI via uiautomator dump + input tap → capture screenshots + logcat
```
