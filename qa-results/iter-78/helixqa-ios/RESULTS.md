# HelixQA iOS Simulator Baseline — iter-78

**Date:** 2026-05-17  
**Simulator:** iPhone 16 Pro (UDID: B17BD9A0-6A18-48AD-86D6-80488202DDB7) — iOS 18.3 Booted  
**App:** Yole v1.9.5 (CFBundleVersion=195, bundle: digital.vasic.yole.ios)  
**Method:** Manual scenario execution via `xcrun simctl` (HelixQA binary does not yet support `--platform ios`)

> **CONST-039 honest disclosure:** HelixQA binary (`helixqa run`) only supports
> `android|web|desktop|all` platforms. Full XCUITest/Appium automation is
> deferred to a future iteration. These scenarios are executed manually via
> `xcrun simctl` with screenshot evidence. Per CONST-039, each PASS carries
> positive runtime evidence.
> Tracker: `#iter-78-helixqa-ios-xcuitest-deferred`

---

## Scenarios Executed

| ID | Scenario | Method | Result | Evidence |
|----|----------|--------|--------|----------|
| IOS-SIM-001 | App installs and launches (first launch) | `xcrun simctl launch` → PID 28925 | **PASS** | scenario-01-app-launch.png |
| IOS-SIM-002 | Compose Multiplatform UI renders in foreground | `launch` → PID 34214 → screenshot | **PASS** | scenario-02-compose-ui-visible.png |
| IOS-SIM-003 | Cold start after terminate | `terminate` → `launch` → PID 34711 | **PASS** | scenario-03-cold-start.png |
| IOS-SIM-004 | Dark mode appearance change survives | `ui appearance dark` → screenshot | **PASS** | scenario-04-dark-mode.png |
| IOS-SIM-005 | Return to light mode | `ui appearance light` → screenshot | **PASS** | scenario-05-light-mode.png |
| IOS-SIM-006 | Zero new crashes during session | DiagnosticReports: all Yole crashes at 15:10-15:11 (pre-session, v1.9.4); zero crashes at ≥15:12 | **PASS** | (crash log timestamps verified) |

---

## Known Limitations / Deferred

- `#iter-78-helixqa-ios-xcuitest-deferred` — Full XCUITest gesture automation not yet wired into helixqa binary
- `#iter-78-ios-paid-dev-program-needed-for-firebase` — Device .ipa export blocked: Xcode signed in with wrong Apple ID. Operator must log in with `milos85vasic.2nd@gmail.com` to get provisioning profile for `digital.vasic.yole.ios`
- `#iter-78-ios-ui-feature-parity-pending` — Full Compose Multiplatform feature set (formats, cloud storage, editor) confirmed on Android/Desktop; iOS shows the KMP entry point. Per CONST-039: iOS shows the Compose host but the full feature surface has not been validated on-device

---

## Summary

**6 / 6 scenarios PASS** with photographic evidence.  
Yole v1.9.5 launches, renders Compose Multiplatform UI, survives cold starts, and handles appearance changes without crashing on iPhone 16 Pro simulator.
