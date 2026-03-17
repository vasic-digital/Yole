<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Module 26: UI/UX Automation Testing (6 videos)

## Video 26.1: Why Real Interaction Testing Matters (14 min)

### Timestamps
- 0:00 Introduction: the gap between unit tests and real user experience
- 2:00 Unit tests validate logic, but users interact with pixels and touch events
- 4:00 The three failure modes unit tests miss: rendering bugs, timing issues, layout regressions
- 6:00 Yole's approach: 36 recorded automation challenges across 3 platforms
- 8:00 Speed modes: why testing at slow, normal, and fast interaction rates catches different bugs
- 10:00 Recording as evidence: video proof that every flow works, reviewable in CI artifacts
- 12:00 How automation testing fits into the 9,400+ test suite alongside unit, integration, stress, fuzz, snapshot, load, E2E, and accessibility tests
- 13:30 Summary

### Code References
- `Challenges/banks/yole/ui-automation-desktop.json` -- Desktop challenge bank
- `Challenges/banks/yole/ui-automation-web.json` -- Web challenge bank
- `Challenges/banks/yole/ui-automation-android.json` -- Android challenge bank
- `docs/UI_AUTOMATION_TESTING.md` -- Comprehensive guide

---

## Video 26.2: Architecture Overview (16 min)

### Timestamps
- 0:00 The adapter-per-platform pattern: one interface, three implementations
- 2:00 Challenge bank JSON schema: id, steps, assertions, recording, speed_modes
- 4:00 Desktop adapter: Compose Desktop JAR + xdotool for input + FFmpeg for recording
- 6:00 Web adapter: Playwright CDP browser automation + built-in recordVideo
- 8:00 Android adapter: ADB CLI for input, screenrecord for recording, APK installation
- 10:00 The recording wrapper: RecordedBrowserFlowChallenge and RecordedMobileFlowChallenge
- 12:00 Assertion engine integration: 16 built-in evaluators + 12 userflow-specific evaluators
- 14:00 Container integration: TestEnvironment and PlatformGroup for isolated execution
- 15:30 Summary

### Code References
- `Challenges/pkg/userflow/` -- All adapter implementations
- `Challenges/pkg/userflow/templates.go` -- Challenge templates
- `docs/diagrams/ui-automation-architecture.mmd` -- Architecture diagram

---

## Video 26.3: Speed Mode Demos (18 min)

### Timestamps
- 0:00 Speed mode concept: same flow, three interaction rates
- 2:00 Demo: Desktop cold start at slow speed (800ms between steps, all animations visible)
- 4:00 Demo: Desktop cold start at normal speed (300ms, typical user pace)
- 6:00 Demo: Desktop cold start at fast speed (50ms, stress testing UI responsiveness)
- 8:00 Comparing recordings side by side: what each speed reveals
- 10:00 Slow mode catches: animation completion, visual feedback, accessibility timing
- 12:00 Normal mode catches: functional correctness under typical usage
- 14:00 Fast mode catches: race conditions, debounce failures, state corruption
- 16:00 Configuring speed mode delays per challenge and per platform
- 17:30 Summary

### Code References
- `Challenges/banks/yole/ui-automation-desktop.json` -- speed_modes field
- `Challenges/pkg/userflow/adapters.go` -- Speed mode delay injection

---

## Video 26.4: Platform-Specific Automation (20 min)

### Timestamps
- 0:00 Desktop automation: launching JAR, xdotool window targeting, keyboard shortcuts
- 3:00 Desktop demo: create Markdown document, type content, Ctrl+S to save, Ctrl+Z to undo
- 6:00 Desktop demo: theme switching and window resize layout adaptation
- 8:00 Web automation: Playwright page navigation, viewport manipulation, JavaScript evaluation
- 10:00 Web demo: PWA launch, responsive layout at mobile/tablet/desktop viewports
- 12:00 Web demo: offline mode testing with service worker and IndexedDB persistence
- 14:00 Android automation: ADB install, activity launch, tap/swipe/fling gestures
- 16:00 Android demo: FAB actions, on-screen keyboard input, rotation handling
- 18:00 Android demo: share intent handling and back button navigation stack
- 19:30 Summary

### Code References
- `Challenges/banks/yole/ui-automation-desktop.json` -- Desktop steps
- `Challenges/banks/yole/ui-automation-web.json` -- Web steps
- `Challenges/banks/yole/ui-automation-android.json` -- Android steps

---

## Video 26.5: Recording and Validation (16 min)

### Timestamps
- 0:00 Recording pipelines: FFmpeg (desktop), Playwright recordVideo (web), adb screenrecord (android)
- 2:00 FFmpeg configuration: screen capture resolution, codec, frame rate
- 4:00 Playwright recordVideo: CDP screencast protocol, WebM output
- 6:00 ADB screenrecord: device-side recording, 3-minute default limit, pull to host
- 8:00 Recording validation: file existence, non-zero size, duration bounds, frame count
- 10:00 Screenshot capture: point-in-time evidence at key moments (theme switch, layout change)
- 12:00 Output directory structure: platform/challenge-id/speed-mode files
- 14:00 Report generation: Markdown, JSON, and HTML reports with pass/fail per challenge
- 15:30 Summary

### Code References
- `Challenges/pkg/userflow/recorder.go` -- Recorder adapter interface
- `docs/UI_AUTOMATION_TESTING.md` -- Recording output structure section

---

## Video 26.6: Analyzing Results and Extending the Framework (14 min)

### Timestamps
- 0:00 Reading the automation report: challenge status, assertion results, timing metrics
- 2:00 Identifying flaky flows: comparing results across speed modes
- 4:00 Debugging failures: using recordings to pinpoint the exact step that failed
- 6:00 Performance analysis: cold start time, navigation transition time, save latency
- 8:00 Adding a new challenge: step-by-step walkthrough of adding a desktop flow
- 10:00 CI/CD integration: GitHub Actions workflow with artifact upload for recordings
- 12:00 Future directions: AI-assisted visual regression, cross-platform screenshot diff
- 13:30 Summary

### Exercises
1. **Add a desktop challenge**: Create a new challenge that tests multi-tab editing (open three files, switch between tabs, verify each tab's content).
2. **Add a web challenge**: Create a challenge that tests the browser's back/forward buttons navigating between routes.
3. **Add an Android challenge**: Create a challenge that tests notification handling (receive a notification, tap it, verify the app opens the correct document).
4. **Speed mode analysis**: Run all desktop challenges at all three speeds, compare the recordings, and document which flows behave differently at fast speed.
