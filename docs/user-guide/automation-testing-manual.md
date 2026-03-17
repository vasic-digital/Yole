<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Automation Testing Manual

Step-by-step guide for setting up, running, and extending Yole's UI/UX automation tests.

## Prerequisites

### Required Tools

| Tool | Version | Purpose | Install |
|------|---------|---------|---------|
| Go | 1.24+ | Challenge runner | [golang.org/dl](https://golang.org/dl/) |
| FFmpeg | 6.0+ | Desktop screen recording | `apt install ffmpeg` / `brew install ffmpeg` |
| xdotool | 3.0+ | Desktop window interaction | `apt install xdotool` |
| Node.js | 18+ | Playwright for web automation | [nodejs.org](https://nodejs.org/) |
| Playwright | latest | Browser automation | `npx playwright install chromium` |
| Android SDK | platform-tools 34+ | ADB for Android automation | [developer.android.com](https://developer.android.com/studio) |
| Java | 11+ | Desktop app (JAR) | `apt install openjdk-11-jdk` |

### Platform-Specific Setup

#### Desktop (Linux)

```bash
# Install dependencies
sudo apt install xdotool ffmpeg openjdk-11-jdk

# Build the desktop app
./gradlew :desktopApp:packageUberJarForCurrentOS

# Verify xdotool can access the display
xdotool getactivewindow
```

#### Desktop (macOS)

```bash
# Install dependencies
brew install ffmpeg
# xdotool is Linux-only; macOS uses cliclick or AppleScript
brew install cliclick

# Build the desktop app
./gradlew :desktopApp:packageUberJarForCurrentOS
```

#### Web

```bash
# Install Playwright
npm init -y
npx playwright install chromium

# Build the web app
./gradlew :webApp:wasmJsBrowserDistribution

# Serve locally (for testing)
npx serve webApp/build/dist/wasmJs/productionExecutable -l 3000
```

#### Android

```bash
# Verify ADB is available
adb version

# Start an emulator or connect a device
adb devices

# Build the APK
./gradlew :androidApp:assembleDebug

# Install on device
adb install androidApp/build/outputs/apk/flavorDefault/debug/androidApp-flavorDefault-debug.apk
```

## Running Your First Automation Test

### Step 1: Build the Challenge Runner

```bash
cd Challenges
go build -o bin/userflow-runner cmd/userflow-runner/main.go
```

### Step 2: Run a Single Desktop Challenge

```bash
./bin/userflow-runner \
  --platform desktop \
  --root banks/yole/ui-automation-desktop.json \
  --output ../recordings/desktop/ \
  --timeout 5m \
  --verbose
```

### Step 3: Check the Output

```bash
# View the report
cat ../recordings/desktop/report.md

# List recordings
ls -la ../recordings/desktop/yole-ui-desktop-cold-start/

# Play a recording
ffplay ../recordings/desktop/yole-ui-desktop-cold-start/normal.mp4
```

### Step 4: Run All Desktop Challenges

```bash
./bin/userflow-runner \
  --platform desktop \
  --root banks/yole/ui-automation-desktop.json \
  --output ../recordings/desktop/ \
  --report markdown \
  --timeout 30m
```

### Step 5: Run Web Challenges

```bash
# Start the web app in the background
npx serve ../webApp/build/dist/wasmJs/productionExecutable -l 3000 &

# Run web challenges
./bin/userflow-runner \
  --platform web \
  --root banks/yole/ui-automation-web.json \
  --output ../recordings/web/ \
  --report markdown \
  --timeout 30m
```

### Step 6: Run Android Challenges

```bash
# Ensure device is connected
adb devices

# Run Android challenges
./bin/userflow-runner \
  --platform android \
  --root banks/yole/ui-automation-android.json \
  --output ../recordings/android/ \
  --report markdown \
  --timeout 30m
```

## Viewing Recordings

### Desktop (MP4 via FFmpeg)

```bash
# Play with ffplay
ffplay recordings/desktop/yole-ui-desktop-theme-switching/slow.mp4

# Or open in any video player
xdg-open recordings/desktop/yole-ui-desktop-theme-switching/normal.mp4
```

### Web (WebM via Playwright)

```bash
# Play with ffplay
ffplay recordings/web/yole-ui-web-pwa-launch/normal.webm

# Convert to MP4 if needed
ffmpeg -i recordings/web/yole-ui-web-pwa-launch/normal.webm -c:v libx264 output.mp4
```

### Android (MP4 via ADB screenrecord)

```bash
# Play directly
ffplay recordings/android/yole-ui-android-app-launch/normal.mp4
```

## Adding Custom Flows

### 1. Define the Challenge

Create or edit a challenge bank JSON file. Each challenge follows this structure:

```json
{
  "id": "yole-ui-desktop-my-flow",
  "name": "Desktop: My Custom Flow",
  "description": "Description of what the flow tests",
  "category": "ui-automation",
  "platform": "desktop",
  "type": "RecordedBrowserFlowChallenge",
  "dependencies": ["yole-ui-desktop-cold-start"],
  "estimated_duration": "3m",
  "speed_modes": ["slow", "normal", "fast"],
  "inputs": [
    {"name": "jar_path", "source": "dependency:yole-build-desktop-jar", "required": true},
    {"name": "speed_mode", "source": "config", "required": true}
  ],
  "steps": [
    {"action": "click", "target": "element_id", "value": ""},
    {"action": "type", "target": "input_field", "value": "text to type"},
    {"action": "assert_visible", "target": "result_element", "value": "true"}
  ],
  "outputs": [
    {"name": "result", "type": "string", "description": "Whether the action succeeded"}
  ],
  "assertions": [
    {"type": "equals", "target": "result", "value": "true", "message": "Action must succeed"}
  ],
  "recording": {"enabled": true, "validate": true},
  "metrics": ["action_time_ms"]
}
```

### 2. Available Step Actions

#### Common Actions (All Platforms)

| Action | Description | Example |
|--------|-------------|---------|
| `click` | Click/tap an element | `{"action": "click", "target": "button_id", "value": ""}` |
| `type` / `fill` | Enter text | `{"action": "type", "target": "input", "value": "hello"}` |
| `assert_visible` | Verify element is visible | `{"action": "assert_visible", "target": "panel", "value": "true"}` |
| `assert_contains` | Verify text content | `{"action": "assert_contains", "target": "editor", "value": "expected"}` |
| `wait` | Wait for a condition | `{"action": "wait", "target": "indicator", "value": "saved"}` |
| `screenshot` | Capture a screenshot | `{"action": "screenshot", "target": "window", "value": "name.png"}` |
| `measure` | Record a timing metric | `{"action": "measure", "target": "metric_name", "value": "elapsed"}` |

#### Desktop-Specific Actions

| Action | Description |
|--------|-------------|
| `keyboard_shortcut` | Send key combination (e.g., `Ctrl+S`) |
| `double_click` | Double-click an element |
| `right_click` | Right-click for context menu |
| `resize_window` | Resize the app window (e.g., `1920x1080`) |

#### Web-Specific Actions

| Action | Description |
|--------|-------------|
| `navigate` | Navigate to a URL |
| `set_viewport` | Set browser viewport size |
| `evaluate_js` | Execute JavaScript in the page |
| `set_offline` | Toggle network offline mode |
| `assert_url_contains` | Verify URL contains a string |

#### Android-Specific Actions

| Action | Description |
|--------|-------------|
| `tap` | Tap an element |
| `long_press` | Long-press an element |
| `swipe_up` / `swipe_down` | Swipe gesture |
| `fling` | Fling gesture with momentum |
| `press_back` | Press the system back button |
| `input_text` | Enter text via ADB |
| `adb_install` | Install an APK |
| `adb_launch` | Launch an activity |
| `adb_rotate` | Rotate the device |
| `adb_screenshot` | Capture via ADB |
| `adb_hide_keyboard` | Dismiss the soft keyboard |
| `adb_broadcast` | Send an intent |

### 3. Run Your Custom Challenge

```bash
./bin/userflow-runner \
  --platform desktop \
  --root banks/yole/ui-automation-desktop.json \
  --output ../recordings/desktop/ \
  --verbose
```

## CI/CD Integration

### GitHub Actions

```yaml
name: UI Automation Tests

on:
  push:
    branches: [master]
  pull_request:

jobs:
  desktop-automation:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          submodules: recursive

      - name: Set up JDK 11
        uses: actions/setup-java@v4
        with:
          java-version: '11'
          distribution: 'temurin'

      - name: Set up Go
        uses: actions/setup-go@v5
        with:
          go-version: '1.24'

      - name: Install xdotool and FFmpeg
        run: sudo apt-get install -y xdotool ffmpeg xvfb

      - name: Build Desktop App
        run: ./gradlew :desktopApp:packageUberJarForCurrentOS

      - name: Run Desktop UI Automation
        run: |
          cd Challenges
          xvfb-run go run cmd/userflow-runner/main.go \
            --platform desktop \
            --root banks/yole/ui-automation-desktop.json \
            --output ../recordings/desktop/ \
            --report markdown

      - name: Upload Recordings
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: desktop-recordings
          path: recordings/desktop/

  web-automation:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          submodules: recursive

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '18'

      - name: Install Playwright
        run: npx playwright install chromium

      - name: Set up Go
        uses: actions/setup-go@v5
        with:
          go-version: '1.24'

      - name: Build Web App
        run: ./gradlew :webApp:wasmJsBrowserDistribution

      - name: Run Web UI Automation
        run: |
          npx serve webApp/build/dist/wasmJs/productionExecutable -l 3000 &
          sleep 5
          cd Challenges
          go run cmd/userflow-runner/main.go \
            --platform web \
            --root banks/yole/ui-automation-web.json \
            --output ../recordings/web/ \
            --report markdown

      - name: Upload Recordings
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: web-recordings
          path: recordings/web/

  android-automation:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          submodules: recursive

      - name: Set up JDK 11
        uses: actions/setup-java@v4
        with:
          java-version: '11'
          distribution: 'temurin'

      - name: Set up Go
        uses: actions/setup-go@v5
        with:
          go-version: '1.24'

      - name: Set up Android Emulator
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 33
          script: |
            ./gradlew :androidApp:assembleDebug
            cd Challenges
            go run cmd/userflow-runner/main.go \
              --platform android \
              --root banks/yole/ui-automation-android.json \
              --output ../recordings/android/ \
              --report markdown

      - name: Upload Recordings
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: android-recordings
          path: recordings/android/
```

### Local CI Simulation

```bash
# Run all platforms locally (requires all prerequisites)
make test-ui-automation

# Or run individually
make test-ui-desktop
make test-ui-web
make test-ui-android
```

## Speed Mode Strategy

When writing new challenges, consider how each speed mode affects your flow:

- **slow**: Use for flows that involve animations, transitions, or visual feedback that must complete before the next step. Good for accessibility verification.
- **normal**: The default validation mode. Most functional assertions should pass at normal speed.
- **fast**: Use to detect race conditions, debounce failures, and UI state corruption under rapid interaction. Not all visual assertions need to pass at fast speed -- focus on data integrity.

### Recommended Approach

1. Write the flow and test it at `normal` speed first.
2. Add animation-sensitive assertions that only run at `slow` speed.
3. Add stress-oriented assertions that specifically target `fast` speed (e.g., no crash, no data loss).
