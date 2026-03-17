<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# UI/UX Automation Testing

Comprehensive guide to Yole's cross-platform UI automation testing framework with video recording and multi-speed execution.

## Overview

Yole's UI automation testing validates complete user flows across all three runtime platforms -- Desktop (Compose Desktop), Web (Wasm PWA via Playwright), and Android (via ADB). Every automation challenge runs at three speed modes (slow, normal, fast) and produces a video recording of the entire flow for visual verification and debugging.

The automation framework is built on the [Challenges](../Challenges/) Go module, which provides challenge definitions, execution, assertion evaluation, and reporting. UI automation challenges use the `pkg/userflow` package adapters:

- **Desktop**: `RecordedBrowserFlowChallenge` with Compose Desktop interaction via xdotool and FFmpeg recording
- **Web**: `RecordedBrowserFlowChallenge` with Playwright CDP browser automation and built-in `recordVideo`
- **Android**: `RecordedMobileFlowChallenge` and `RecordedMobileLaunchChallenge` with ADB CLI and `adb screenrecord`

## Architecture

```
+-------------------+     +-------------------+     +-------------------+
|  Desktop Flows    |     |    Web Flows      |     |  Android Flows    |
|  (12 challenges)  |     |  (12 challenges)  |     |  (12 challenges)  |
+--------+----------+     +--------+----------+     +--------+----------+
         |                         |                         |
         v                         v                         v
+--------+----------+     +--------+----------+     +--------+----------+
| ComposeDesktop    |     |   Playwright      |     |     ADB CLI       |
| Adapter (xdotool) |     |   CDP Adapter     |     |    Adapter        |
+--------+----------+     +--------+----------+     +--------+----------+
         |                         |                         |
         v                         v                         v
+--------+----------+     +--------+----------+     +--------+----------+
| FFmpeg Recording  |     | Playwright        |     | adb screenrecord  |
|                   |     | recordVideo       |     |                   |
+--------+----------+     +--------+----------+     +--------+----------+
         |                         |                         |
         +------------+------------+------------+------------+
                      |                         |
                      v                         v
              +-------+--------+       +--------+--------+
              | Speed Modes    |       | Recording       |
              | slow / normal  |       | Validation      |
              | / fast         |       | (size, duration)|
              +-------+--------+       +--------+--------+
                      |                         |
                      v                         v
              +-------+--------+       +--------+--------+
              | Assertion      |       | Output          |
              | Engine         |       | Artifacts       |
              +----------------+       +-----------------+
```

## Platform Coverage

### Desktop (12 challenges)

Challenge bank: `Challenges/banks/yole/ui-automation-desktop.json`

| Challenge | Flow |
|-----------|------|
| Cold Start | App launch and initial render timing |
| File Browser | Create folder, navigate in/out |
| Create Markdown | New document with real content, save, verify persistence |
| Create Todo.txt | New task file with priorities, projects, contexts |
| Edit Existing | Open, modify, save, verify changes |
| Theme Switching | Light, Dark, System theme cycle |
| Settings Toggles | Line numbers, auto-save, animations |
| Preview Mode | Editor to preview to editor round-trip |
| Keyboard Shortcuts | Ctrl+N, Ctrl+O, Ctrl+S, Ctrl+Z |
| Navigation Paths | Files, Todo, QuickNote, Settings |
| Format Info Dialog | Open dialog, verify format metadata |
| Window Resize | Small, medium, large layout adaptation |

**Adapter**: Compose Desktop application launched as a JAR. Window interaction via xdotool for mouse/keyboard events. Recording via FFmpeg screen capture.

### Web (12 challenges)

Challenge bank: `Challenges/banks/yole/ui-automation-web.json`

| Challenge | Flow |
|-----------|------|
| PWA Launch | Wasm initialization and app shell render |
| File Browser | Folder creation and navigation in IndexedDB |
| Create Markdown | Document creation with IndexedDB persistence |
| Create Todo.txt | Task file with priority parsing |
| Edit Document | Modify and save existing document |
| Theme/Settings | Theme switching with CSS variable verification |
| Navigation Routes | All routes with URL verification |
| Responsive Mobile | 375x667 viewport adaptation |
| Responsive Tablet | 768x1024 viewport adaptation |
| Responsive Desktop | 1920x1080 full layout |
| Offline Mode | Service worker offline operation |
| Preview Mode | Editor/preview toggle with HTML verification |

**Adapter**: Playwright CDP browser automation. The Wasm PWA runs in headless or headed Chrome. Recording via Playwright's built-in `recordVideo` option. Viewport manipulation via `page.setViewportSize()`.

### Android (12 challenges)

Challenge bank: `Challenges/banks/yole/ui-automation-android.json`

| Challenge | Flow |
|-----------|------|
| App Launch | Install APK, launch, verify bottom navigation |
| Bottom Navigation | Tap all four tabs, verify content |
| FAB Actions | New file and quick note via FAB |
| Document Creation | On-screen keyboard content entry |
| Swipe and Scroll | Swipe gestures, fling scrolling |
| Settings Navigation | Settings categories, theme toggle |
| Back Button | System back navigation across depth |
| Rotation Handling | Portrait/landscape content preservation |
| Share Intent | Receive text via ACTION_SEND |
| File Browser | Folder creation and navigation |
| Create Todo.txt | Task file with priority highlighting |
| Preview Mode | Editor/preview toggle in WebView |

**Adapter**: ADB CLI adapter with `adb shell input` for taps, swipes, and key events. Recording via `adb screenrecord`. APK installation via `adb install`.

## Speed Modes

Every challenge runs at three speed modes to verify the app handles different interaction rates:

| Mode | Delay Between Steps | Use Case |
|------|---------------------|----------|
| `slow` | 800-1200ms | Simulates careful, deliberate user interaction. Tests animation completion, visual feedback, and accessibility timing. |
| `normal` | 300-500ms | Simulates typical user interaction speed. The primary validation mode for functional correctness. |
| `fast` | 50-100ms | Simulates rapid interaction. Stress-tests UI responsiveness, race condition detection, and debounce logic. |

Speed mode is injected as a `speed_mode` config input to each challenge. The adapter inserts the appropriate delay between each step in the flow.

### Timing Guarantees

- **slow**: All animations must complete between steps. No UI state inconsistencies.
- **normal**: Common animations may overlap with the next step, but UI must remain functional.
- **fast**: The app must not crash, lose data, or enter an inconsistent state even under rapid interaction.

## Running Automation Tests

### Prerequisites

- **Desktop**: Java 11+, xdotool, FFmpeg, X11 or Wayland display server
- **Web**: Node.js 18+, Playwright (`npx playwright install chromium`)
- **Android**: Android SDK with platform-tools (adb), connected device or emulator

### Run All Platforms

```bash
cd Challenges
go run cmd/userflow-runner/main.go \
  --platform all \
  --root ../Challenges/banks/yole/ \
  --output ../recordings/ \
  --report json
```

### Run a Single Platform

```bash
# Desktop only
go run cmd/userflow-runner/main.go \
  --platform desktop \
  --root ../Challenges/banks/yole/ui-automation-desktop.json \
  --output ../recordings/desktop/ \
  --report markdown

# Web only
go run cmd/userflow-runner/main.go \
  --platform web \
  --root ../Challenges/banks/yole/ui-automation-web.json \
  --output ../recordings/web/ \
  --report markdown

# Android only
go run cmd/userflow-runner/main.go \
  --platform android \
  --root ../Challenges/banks/yole/ui-automation-android.json \
  --output ../recordings/android/ \
  --report markdown
```

### Run via Gradle

```bash
./gradlew runChallenges -Pbank=ui-automation-desktop
./gradlew runChallenges -Pbank=ui-automation-web
./gradlew runChallenges -Pbank=ui-automation-android
```

## Recording Output Structure

```
recordings/
├── desktop/
│   ├── yole-ui-desktop-cold-start/
│   │   ├── slow.mp4
│   │   ├── normal.mp4
│   │   ├── fast.mp4
│   │   └── screenshots/
│   │       ├── theme_light.png
│   │       └── theme_dark.png
│   ├── yole-ui-desktop-file-browser-navigation/
│   │   ├── slow.mp4
│   │   ├── normal.mp4
│   │   └── fast.mp4
│   └── ...
├── web/
│   ├── yole-ui-web-pwa-launch/
│   │   ├── slow.webm
│   │   ├── normal.webm
│   │   └── fast.webm
│   └── ...
├── android/
│   ├── yole-ui-android-app-launch/
│   │   ├── slow.mp4
│   │   ├── normal.mp4
│   │   └── fast.mp4
│   └── ...
└── reports/
    ├── desktop-report.md
    ├── web-report.md
    └── android-report.md
```

## Recording Validation

Every recorded challenge includes `"recording": {"enabled": true, "validate": true}`. The validation checks:

1. **File exists**: The recording file was created at the expected path.
2. **Non-zero size**: The file is larger than 0 bytes (eliminates empty recordings).
3. **Duration check**: The recording duration is within the expected range for the speed mode.
4. **Frame count**: The recording contains at least the minimum expected frame count.

Validation failures produce a distinct assertion failure in the challenge report, making it clear whether the flow itself failed or only the recording was corrupted.

## Extending with New Flows

### Adding a Desktop Challenge

1. Open `Challenges/banks/yole/ui-automation-desktop.json`.
2. Add a new entry to the `challenges` array following the existing schema.
3. Set `"type": "RecordedBrowserFlowChallenge"` and `"platform": "desktop"`.
4. Include `"speed_modes": ["slow", "normal", "fast"]` and `"recording": {"enabled": true, "validate": true}`.
5. Define `steps` with `action`, `target`, and `value` fields.
6. Define `assertions` to validate the expected outcomes.

### Adding a Web Challenge

Same as desktop, but use `"platform": "web"`. Web-specific actions include `navigate`, `set_viewport`, `evaluate_js`, and `set_offline`.

### Adding an Android Challenge

Use `"type": "RecordedMobileFlowChallenge"` (or `RecordedMobileLaunchChallenge` for launch flows) and `"platform": "android"`. Android-specific actions include `adb_install`, `adb_launch`, `adb_rotate`, `adb_screenshot`, `adb_hide_keyboard`, `tap`, `long_press`, `swipe_up`, `swipe_down`, `fling`, `press_back`, and `input_text`.

## Troubleshooting

### Desktop

| Issue | Solution |
|-------|----------|
| xdotool cannot find window | Ensure the Compose Desktop app is running on the same X11 display. Set `DISPLAY=:0` if running in a container. |
| FFmpeg recording is blank | Verify FFmpeg has access to the X11 display. Use `-video_size` matching your screen resolution. |
| Keyboard shortcuts not recognized | xdotool sends X11 key events. Verify Compose Desktop is focused with `xdotool windowfocus`. |

### Web

| Issue | Solution |
|-------|----------|
| Wasm module fails to load | Ensure the web app is built with `./gradlew :webApp:wasmJsBrowserDistribution` before running. |
| Playwright cannot connect | Run `npx playwright install chromium` to install the browser binary. |
| Viewport size not applied | Playwright sets viewport on the page context, not the browser. Ensure `page.setViewportSize()` is called before navigation. |
| Service worker not ready | The PWA must be loaded over HTTPS or localhost for the service worker to activate. |

### Android

| Issue | Solution |
|-------|----------|
| ADB device not found | Run `adb devices` to verify the device is connected. For emulators, ensure the emulator is running. |
| APK install fails | Verify the APK was built with `./gradlew :androidApp:assembleDebug`. Check that the device API level is compatible. |
| screenrecord stops early | `adb screenrecord` has a default 3-minute limit. For longer flows, use `--time-limit` or chain recordings. |
| On-screen keyboard not shown | Some emulators disable the soft keyboard by default. Enable via Settings > System > Languages > Virtual keyboard. |

### General

| Issue | Solution |
|-------|----------|
| Recording files are empty | Check disk space. Verify the output directory exists and is writable. |
| Speed mode timing issues | Increase the base delay for `slow` mode if animations are not completing. Decrease `fast` mode delay if the app handles it without issues. |
| Container OOM (exit 137) | UI automation with recording is memory-intensive. Allocate at least 4 GB to the container. |
