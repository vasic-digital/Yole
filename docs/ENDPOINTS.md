# Yole — Distribution Endpoints

Living index of every shipped artifact's public URL, kept in sync with `CHANGELOG.md`.
Last updated: **v2.0.6 (iter-91)** — 2026-05-18.

---

## Web (PWA)

| Endpoint | URL | Notes |
|---|---|---|
| **Production** | https://yole-app.web.app | Firebase Hosting. Network-first SW (iter-89); responsive layout (iter-90). |
| Firebase project console | https://console.firebase.google.com/project/yole-app/overview | Operator-only. |

Tested device matrix (responsive-suite gate, iter-90):

| Class | Viewport | Layout |
|---|---|---|
| Phone XS | 320 × 568 | Editor full-screen; sidebar + preview hidden; toolbar horizontally scrollable |
| Phone SM | 375 × 667 | Same as XS |
| Phone LG | 414 × 896 | Same as XS |
| Tablet portrait | 768 × 1024 | Sidebar + editor; preview toggleable |
| Tablet landscape | 1024 × 768 | Three-pane (sidebar + editor + preview) |
| Desktop | 1280 × 800+ | Three-pane (sidebar + editor + preview) |

---

## Android

Two Firebase Apps because the DEV variant uses `applicationIdSuffix = ".dev"`
(package `digital.vasic.yole.android.dev`) which is a different Firebase App
from the production `digital.vasic.yole.android`.

| Variant | Package | Firebase App ID | v2.0.6 release ID |
|---|---|---|---|
| **Release** | `digital.vasic.yole.android` | `1:578988389676:android:d61715a0a84a42c65d2889` | `1qep5papjgmo8` |
| **Debug (DEV)** | `digital.vasic.yole.android.dev` | `1:578988389676:android:5a3d47a9fb23b6465d2889` | `11ac9q6ck1olo` |

### Tester install URLs (v2.0.6)

- **Release**: https://appdistribution.firebase.google.com/testerapps/1:578988389676:android:d61715a0a84a42c65d2889/releases/1qep5papjgmo8
- **Debug (DEV)**: https://appdistribution.firebase.google.com/testerapps/1:578988389676:android:5a3d47a9fb23b6465d2889/releases/11ac9q6ck1olo

### Firebase console (operator-only)

- **Release**: https://console.firebase.google.com/project/yole-app/appdistribution/app/android:digital.vasic.yole.android/releases
- **Debug**: https://console.firebase.google.com/project/yole-app/appdistribution/app/android:digital.vasic.yole.android.dev/releases

### Local APK paths

- `releases/Yole-Android-2.0.6-Release-0.0.0.2.6.apk` (44 MB)
- `releases/Yole-Android-2.0.6-Debug-0.0.0.2.6.apk` (56 MB)

---

## Desktop

| Platform | Artifact | Status |
|---|---|---|
| macOS arm64 | `releases/Yole-Desktop-macos-arm64-2.0.6-Release-0.0.0.2.6.dmg` (526 MB) | Staged locally |
| macOS x64 | not built this iteration | Cross-build host needed |
| Linux x64 | not built this iteration | `#iter-69-linux-container-deb-build` |
| Windows x64 | not built this iteration | `#crossbuild-windows-image-provisioning` |

---

## iOS

| Status | Notes |
|---|---|
| Simulator build | OK on macOS-arm64 host with Xcode installed |
| Device `.ipa` | **Blocked** by `#iter-78-ios-paid-dev-program-needed-for-firebase` — needs Apple Developer Program enrollment + Xcode sign-in with `milos85vasic.2nd@gmail.com` |

---

## Source repositories

| Repo | URL |
|---|---|
| Yole (main) | git@github.com:vasic-digital/Yole.git |
| Tags | https://github.com/vasic-digital/Yole/tags |
| Releases | https://github.com/vasic-digital/Yole/releases |

Latest tag: `yole-2.0.6`.

---

## Verification commands

To verify the live web URL works for end users (anti-bluff per CONST-039
/ §11.4) — run any of the iter-90 gates:

```bash
# Three foundational gates (single-viewport)
node tools/node-render-gate/render-gate.js https://yole-app.web.app
node tools/node-render-gate/full-ui-suite.js https://yole-app.web.app
node tools/node-render-gate/interactive-flow-suite.js https://yole-app.web.app

# Mobile responsiveness (6 viewports)
node tools/node-render-gate/responsive-suite.js https://yole-app.web.app

# SW cache hygiene + logo presence
bash yole-challenges/scripts/web_sw_cache_version_challenge.sh
bash yole-challenges/scripts/web_logo_presence_challenge.sh

# Full QA chain (19 iter-gates)
make qa-all
```
