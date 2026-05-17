# Firebase iOS Setup

Operator runs this once after registering Yole iOS in Firebase Console.

## Steps

1. Open [Firebase Console](https://console.firebase.google.com/) → project `yole-app` → **Add app** → **iOS+**

2. Fill in:
   - **iOS bundle ID**: `digital.vasic.yole.ios`
   - **App nickname**: `Yole iOS`
   - **App Store ID**: leave empty until published on App Store

3. Click **Register app**

4. Download `GoogleService-Info.plist`

5. Place it at `iosApp/iosApp/GoogleService-Info.plist`
   - This file is gitignored (contains API keys). Never commit it.
   - The template at `iosApp/iosApp/GoogleService-Info.plist.template` shows the expected structure.

6. Add Firebase SDK to the Xcode project (choose one):
   - **Swift Package Manager** (recommended): In Xcode → File → Add Package Dependencies → `https://github.com/firebase/firebase-ios-sdk` → add `FirebaseAnalytics`, `FirebaseCrashlytics`
   - **CocoaPods**: Add to `Podfile`:
     ```ruby
     pod 'Firebase/Analytics'
     pod 'Firebase/Crashlytics'
     ```
     Then: `pod install` → open `iosApp.xcworkspace` (not `.xcodeproj`)

7. In `iOSApp.swift`, add Firebase init:
   ```swift
   import Firebase

   @main
   struct iOSApp: App {
       init() {
           FirebaseApp.configure()
       }
       // ...
   }
   ```

8. Record the iOS App ID in `.env`:
   ```
   FIREBASE_IOS_APP_ID=1:578988389676:ios:<your-suffix-here>
   ```

## Firebase CLI alternative (step 6 via CLI)

```bash
firebase apps:create IOS "Yole iOS" \
  --bundle-id digital.vasic.yole.ios \
  --project yole-app
```

The command prints the iOS App ID. Add it to `.env` as `FIREBASE_IOS_APP_ID=`.

## Firebase App Distribution (after Xcode build)

```bash
# Build IPA first (requires Xcode + signing)
xcodebuild archive \
  -project iosApp/iosApp.xcodeproj \
  -scheme Yole \
  -configuration Release \
  -archivePath /tmp/Yole.xcarchive

xcodebuild -exportArchive \
  -archivePath /tmp/Yole.xcarchive \
  -exportPath /tmp/Yole-ipa \
  -exportOptionsPlist iosApp/exportOptions/adhoc.plist

# Distribute to testers
firebase appdistribution:distribute /tmp/Yole-ipa/Yole.ipa \
  --app "$FIREBASE_IOS_APP_ID" \
  --groups "ios-testers" \
  --release-notes "$(cat CHANGELOG.md | head -30)"
```

## Notes

- `GoogleService-Info.plist` is gitignored via `.gitignore` pattern `**/GoogleService-Info.plist`
- The signing identity (Team ID) is separate — see `docs/setup/ios-signing.md`
- Release naming follows CLAUDE.md convention: `Yole-iOS-1.9.4-Release-0.0.0.1.9.4.ipa`
