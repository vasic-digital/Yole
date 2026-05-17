# iOS Code Signing Setup

Operator runs this once after Xcode is installed.

## Prerequisites

- Xcode installed (via App Store)
- Apple Developer account (paid, $99/year — required for device distribution)
- macOS machine (Xcode is macOS-only)

## Step 1 — Add Apple ID to Xcode

1. Open **Xcode** → **Settings** (⌘,) → **Accounts** tab
2. Click **+** → **Apple ID** → sign in with Apple Developer account
3. Xcode downloads available certificates and provisioning profiles

## Step 2 — Verify signing identity

```bash
security find-identity -v -p codesigning
```

Expected output (example):
```
  1) ABC123DEF456 "Apple Development: developer@example.com (TEAMID12AB)"
```

The 10-character Team ID (e.g., `TEAMID12AB`) is needed for exportOptions plists.

## Step 3 — Update exportOptions plists

Replace `TEAM_ID_HERE` in all three files:

```bash
# Find and replace in all three plists
TEAM_ID="<your-10-char-team-id>"
sed -i "" "s/TEAM_ID_HERE/$TEAM_ID/g" iosApp/exportOptions/release.plist
sed -i "" "s/TEAM_ID_HERE/$TEAM_ID/g" iosApp/exportOptions/adhoc.plist
sed -i "" "s/TEAM_ID_HERE/$TEAM_ID/g" iosApp/exportOptions/development.plist
```

## Step 4 — Build the KMP shared framework

Before opening the Xcode project, build the KMP framework:

```bash
./gradlew :shared:assembleReleaseXCFramework
# Output: shared/build/XCFrameworks/release/shared.xcframework
```

## Step 5 — Link the framework in Xcode

1. Open `iosApp/iosApp.xcodeproj` in Xcode
2. Select the **Yole** target → **General** tab
3. Under **Frameworks, Libraries, and Embedded Content** → **+**
4. **Add Other...** → **Add Files...** → navigate to `shared/build/XCFrameworks/release/shared.xcframework`
5. Set **Embed** to **Embed & Sign**
6. In `iOSApp.swift`, uncomment `import shared`
7. Replace `YolePlaceholderViewController()` with `MainViewControllerKt.MainViewController()`

## Step 6 — Build and run on simulator

```bash
# List available simulators
xcrun simctl list devices --json | python3 -m json.tool | grep -A2 "iPhone 15"

# Build for simulator (no signing required)
xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme Yole \
  -sdk iphonesimulator \
  -configuration Debug \
  -destination "platform=iOS Simulator,name=iPhone 15" \
  build
```

## App Store Connect API Key (automated distribution)

For fully automated distribution without interactive Xcode GUI:

1. Go to [App Store Connect](https://appstoreconnect.apple.com/) → **Users and Access** → **Keys** → **App Manager**
2. Click **Generate API Key** → name: `Yole Distribution`
3. Download the `.p8` file — it can only be downloaded once
4. Place at: `iosApp/signing/AuthKey_<KEY_ID>.p8` (this path is gitignored)
5. Add to `.env`:
   ```
   APPSTORE_API_KEY_ID=<10-char key ID>
   APPSTORE_ISSUER_ID=<your issuer UUID>
   ```

### Using API key with xcodebuild

```bash
xcodebuild -exportArchive \
  -archivePath /tmp/Yole.xcarchive \
  -exportPath /tmp/Yole-ipa \
  -exportOptionsPlist iosApp/exportOptions/release.plist \
  -authenticationKeyPath "iosApp/signing/AuthKey_${APPSTORE_API_KEY_ID}.p8" \
  -authenticationKeyID "$APPSTORE_API_KEY_ID" \
  -authenticationKeyIssuerID "$APPSTORE_ISSUER_ID"
```

## Gitignore

The following are already gitignored (sensitive):
- `iosApp/iosApp/GoogleService-Info.plist` (Firebase)
- `iosApp/signing/` (all signing keys)
- `.env` (all env vars including Team ID, API keys)

## Checklist (operator runs once)

- [ ] Xcode installed
- [ ] Apple ID added to Xcode Settings
- [ ] `security find-identity -v -p codesigning` shows valid cert
- [ ] Team ID recorded; exportOptions plists updated
- [ ] `./gradlew :shared:assembleReleaseXCFramework` succeeds
- [ ] Framework linked in Xcode project
- [ ] `import shared` uncommented in `iOSApp.swift`
- [ ] Simulator build succeeds
- [ ] Device build tested (if device available)
- [ ] Firebase `GoogleService-Info.plist` placed
- [ ] App runs on device/simulator showing Yole UI
