# Yole 1.2.1 — Release Notes (iter-59)

**Release date:** 2026-05-15
**Version:** 1.2.1 (versionCode 121, dotted 0.0.0.1.21)
**Headline change:** Android DEV/DEBUG variant with `.dev` package suffix +
green-tinted launcher icon, registered with Firebase as a distinct app.
Production release behavior is unchanged from v1.2.0.

## What's new

### Android DEV variant
- **`applicationIdSuffix = ".dev"`** on the `debug` build type, producing
  the standalone package `digital.vasic.yole.android.dev` so DEV builds
  install side-by-side with production on the same device.
- **`versionNameSuffix = " DEV"`** — the debug APK reports `1.2.1 DEV`
  (e.g., in About / settings).
- **Green-tinted adaptive launcher icon** — `#FF00FF00` background on
  API 26+ adaptive icons (`src/debug/res/mipmap-anydpi-v26/ic_launcher.xml`)
  plus pre-tinted legacy PNGs for API 24-25 fallback at all 5 densities.
- **Per-variant `appLabel` manifest placeholder** — DEV launcher label
  reads "Yole DEV" while release reads "Yole", driven by
  `manifestPlaceholders["appLabel"]` per build type.

### Firebase registration
- New Android Firebase app entry **"Yole DEV"** under project `yole-app`
  with App ID `1:578988389676:android:5a3d47a9fb23b6465d2889` and package
  `digital.vasic.yole.android.dev`.
- `androidApp/google-services.json` regenerated via `firebase apps:sdkconfig`
  to contain both client entries (production + .dev) — verified by
  `grep -c '"package_name"' = 2`.

### Quality
- **`IterB59VariantConfigTest`** — 6-test structural anchor at
  `androidApp/src/test/kotlin/digital/vasic/yole/android/IterB59VariantConfigTest.kt`
  asserting (a) `applicationIdSuffix = ".dev"`, (b)
  `versionNameSuffix = " DEV"`, (c) green colors.xml entry, (d)
  adaptive-icon references `@color/ic_launcher_background_dev`, (e)
  `appLabel` is `"Yole DEV"`, (f) `google-services.json` registers both
  packages.
- **Mutation verification**: temporarily commenting `applicationIdSuffix`
  in `androidApp/build.gradle.kts` caused `debugVariantHasApplicationIdSuffixDotDev`
  to FAIL (build report under `androidApp/build/reports/tests/testDebugUnitTest/index.html`).
  Reverting restored PASS. CONST-035 mutation contract honored.

## Distributed artifacts (this release)

| Platform | Variant | Filename | SHA-256 |
|----------|---------|----------|---------|
| Android | Release | Yole-Android-1.2.1-Release-0.0.0.1.21.apk | 4bab87802b306931c0f9e7be61d2469015e28bd4dc04eaf75aa16c43734ae15a |
| Android | Debug | Yole-Android-1.2.1-Debug-0.0.0.1.21.apk | 726fe27dbfd8f4e586e12a10fb1313d9e42495816427fb515b6b07f45dcbe751 |

APK aapt verification (positive evidence per CONST-035):
- Debug: `package=digital.vasic.yole.android.dev versionName=1.2.1 DEV
  application-label=Yole DEV`
- Release: `package=digital.vasic.yole.android versionName=1.2.1
  application-label=Yole`

## Firebase distribution URLs

### Android Release — release ID `2j5cfopftric0`
- App ID: `1:578988389676:android:d61715a0a84a42c65d2889` (existing)
- Console: https://console.firebase.google.com/project/yole-app/appdistribution/app/android:digital.vasic.yole.android/releases/2j5cfopftric0
- Tester share: https://appdistribution.firebase.google.com/testerapps/1:578988389676:android:d61715a0a84a42c65d2889/releases/2j5cfopftric0
- Status: Binary uploaded + release notes attached. Group `internal-testers`
  distribution call returned HTTP 404 (group lookup empty on
  `firebase appdistribution:groups:list`); release is visible in console
  for testers with manual access. **No silent fallback** — see
  `firebase-distribution-android-release.txt` for honest log.

### Android Debug (.dev) — release ID `1fqnia7g6leio`
- App ID: `1:578988389676:android:5a3d47a9fb23b6465d2889` (NEW, iter-59)
- Console: https://console.firebase.google.com/project/yole-app/appdistribution/app/android:digital.vasic.yole.android.dev/releases/1fqnia7g6leio
- Tester share: https://appdistribution.firebase.google.com/testerapps/1:578988389676:android:5a3d47a9fb23b6465d2889/releases/1fqnia7g6leio
- Status: Binary uploaded + release notes attached. Group distribution
  skipped (no groups yet for the new .dev app — must be created via
  Firebase Console first).

## Cross-platform impact summary (CONST-037)

- **Android:** DEV variant + green-icon launcher introduced. Production
  package + label unchanged. Both variants installable side-by-side.
- **Desktop (linux-x64 / windows-x64 / macos-arm64):** Not affected —
  the .dev suffix is Android `applicationId` semantics; desktop
  Compose Multiplatform package naming is independent. Carry-over
  blockers from iter-54 / iter-57 / iter-58 still apply unchanged.
- **iOS:** Not affected — iOS bundle ID is set in `iosApp.xcodeproj`,
  separate concern. iter-57 blockers unchanged.
- **Web Wasm:** Not affected — web app has no package-suffix concept.
  iter-54 `#wasmjs-production-distribution-gap` unchanged.

## Known limitations (new + carry-over)

### New in iter-59
- **`#iter59-firebase-tester-groups-empty`** — `firebase appdistribution:groups:list`
  returns zero groups for `yole-app`. The `--groups internal-testers`
  argument therefore returns HTTP 404. Releases are still uploaded
  with release notes (operator can add testers via Console). Tracker
  only; does not gate release.

### Carry-over (unchanged from iter-58)
- `#f2-phase-7-android-ndk-bulk-build-pending`
- `#f2-phase-7-no-bonede-artifact`
- `#f2-phase-7-nim-grammar-broken`
- `#f2-phase-7-ios-xcode-required`
- `#crossbuild-windows-image-provisioning`
- `#wasmjs-production-distribution-gap`
- `#robolectric-compose-ui-tests-brittle`
- `#helixqa-missing-sibling-repos`

## Anti-bluff anchor

Every claim in this document carries positive evidence:
- Artifact hashes: `docs/qa/iter-59/artifact-hashes.txt`
- aapt verification: `docs/qa/iter-59/apk-aapt-verification.txt`
- Build logs: `docs/qa/iter-59/build-android-debug.txt`,
  `docs/qa/iter-59/build-android-release.txt`
- Firebase distribution logs:
  `docs/qa/iter-59/firebase-distribution-android-release.txt`,
  `docs/qa/iter-59/firebase-distribution-android-debug.txt`
- Structural test: `IterB59VariantConfigTest` (6 PASS, mutation
  verified by temporarily commenting the suffix → FAIL)
