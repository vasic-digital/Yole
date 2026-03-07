# Deployment Guide

This guide covers building and deploying Yole on all four target platforms: Android, Desktop (Windows/macOS/Linux), iOS, and Web (Wasm PWA).

## Prerequisites

| Requirement | Version | Purpose |
|-------------|---------|---------|
| JDK | 11+ (21 for desktop app) | Kotlin compilation |
| Android SDK | API 24+ | Android builds |
| Xcode | 15+ (macOS only) | iOS builds |
| Docker/Podman | Latest | Container-based builds (mandatory) |
| Go | 1.24+ | Challenges/Containers submodules |

All builds and tests must run inside Docker/Podman containers per project policy. See `CLAUDE.md` for details.

---

## Android

### Debug Build

```bash
# In container
docker compose run --rm build ./gradlew :androidApp:assembleDebug

# Output: androidApp/build/outputs/apk/flavorDefault/debug/
```

### Release Build

```bash
# In container
docker compose run --rm build ./gradlew :androidApp:assembleRelease

# Output: androidApp/build/outputs/apk/flavorDefault/release/
```

### APK Signing

1. **Generate a keystore** (one-time):

```bash
keytool -genkey -v -keystore yole-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias yole -storepass YOUR_STORE_PASS -keypass YOUR_KEY_PASS
```

2. **Configure signing** in `androidApp/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../yole-release.jks")
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = "yole"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
}
```

### Google Play Store

1. Generate a signed AAB (Android App Bundle):
   ```bash
   docker compose run --rm build ./gradlew :androidApp:bundleRelease
   ```
2. Upload to [Google Play Console](https://play.google.com/console)
3. Configure store listing: title, description, screenshots, feature graphic
4. Use staged rollout: 1% -> 10% -> 50% -> 100%
5. Monitor crash reports via Firebase Crashlytics

### F-Droid

F-Droid requires reproducible builds and no proprietary dependencies:

1. Create a build recipe in F-Droid metadata format
2. Ensure no Google Play Services dependencies in the `flavorDefault` variant
3. Use the `flavorAtest` variant for F-Droid if needed
4. Submit metadata to the [F-Droid Data repository](https://gitlab.com/fdroid/fdroiddata)

### Direct APK Distribution

```bash
# Build and sign the APK
docker compose run --rm build ./gradlew :androidApp:assembleRelease

# Upload to GitHub Releases
gh release create v1.0.0 \
  androidApp/build/outputs/apk/flavorDefault/release/androidApp-release.apk
```

---

## Desktop (Windows / macOS / Linux)

### Run Locally

```bash
# Requires JDK 21 on the host
./gradlew :desktopApp:run
```

### Native Packages

Compose Desktop uses `jpackage` to create native installers:

```bash
# Windows (.msi)
./gradlew :desktopApp:packageMsi

# macOS (.dmg)
./gradlew :desktopApp:packageDmg

# Linux (.deb)
./gradlew :desktopApp:packageDeb
```

Output location: `desktopApp/build/compose/binaries/main/`

### JAR Distribution

For platforms where native packaging is not needed:

```bash
./gradlew :desktopApp:packageUberJarForCurrentOS
# Output: desktopApp/build/compose/jars/
```

Users run the JAR with: `java -jar yole-desktop.jar`

### Code Signing

**Windows (Authenticode):**
```bash
signtool sign /f certificate.pfx /p PASSWORD /t http://timestamp.digicert.com \
  desktopApp/build/compose/binaries/main/msi/Yole-1.0.0.msi
```

**macOS (Notarization):**
```bash
# Sign with Developer ID
codesign --deep --force --verify --verbose \
  --sign "Developer ID Application: Your Name" \
  desktopApp/build/compose/binaries/main/dmg/Yole-1.0.0.dmg

# Submit for notarization
xcrun notarytool submit Yole-1.0.0.dmg \
  --apple-id YOUR_APPLE_ID --team-id YOUR_TEAM_ID --password YOUR_APP_PASSWORD
```

**Linux (GPG):**
```bash
gpg --detach-sign --armor yole_1.0.0_amd64.deb
```

### JDK Version Notes

- The shared module compiles with JDK 11
- The desktop app requires JDK 21 for Compose Desktop features
- Container builds use JDK 11 (for shared module tests)
- Distribution packages bundle the JRE, so users do not need to install Java

---

## iOS

### Status

iOS support is in development. The Xcode project is located at `iosApp/iosApp.xcodeproj`.

### Building

1. Open `iosApp/iosApp.xcodeproj` in Xcode
2. Select a target device or simulator
3. Build and run (Cmd+R)

### Distribution

1. **TestFlight** -- Upload to App Store Connect for beta testing
2. **App Store** -- Submit for review after TestFlight validation
3. Configure signing with an Apple Developer account
4. Create app screenshots and metadata in App Store Connect

### Shared Module Integration

The iOS app depends on the shared KMP module via a framework:

```kotlin
// shared/build.gradle.kts
kotlin {
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        framework {
            baseName = "shared"
        }
    }
}
```

---

## Web (Wasm PWA)

### Status

Web/Wasm support is in development. The web app uses Kotlin/Wasm compiled to WebAssembly.

### Development Server

```bash
./gradlew :webApp:wasmJsBrowserRun
# Opens browser at http://localhost:8080
```

### Production Build

```bash
./gradlew :webApp:wasmJsBrowserProductionWebpack
# Output: webApp/build/dist/wasmJs/productionExecutable/
```

### PWA Configuration

The production build includes:

1. **`manifest.json`** -- App name, icons, theme color, start URL
2. **Service worker** -- Offline caching of app shell and assets
3. **`index.html`** -- Meta tags for mobile viewport, theme color

### Deployment

The production build outputs static files that can be served by any web server:

```bash
# Deploy to any static hosting
rsync -avz webApp/build/dist/wasmJs/productionExecutable/ user@server:/var/www/yole/

# Or use GitHub Pages, Netlify, Vercel, Cloudflare Pages
```

### Wasm Binary Size Optimization

- Enable tree shaking in the production Webpack build
- Use Brotli compression on the server (50-70% size reduction)
- Lazy-load format parsers to reduce initial bundle size
- Use code splitting for format-specific features

### Browser Compatibility

| Browser | Status |
|---------|--------|
| Chrome 119+ | Supported (Wasm GC) |
| Firefox 120+ | Supported (Wasm GC) |
| Safari 18+ | Supported |
| Edge 119+ | Supported (Chromium-based) |

---

## Version Management

All platforms share the same version number, defined in `gradle.properties`:

```properties
version=1.0.0
versionCode=1
```

For Android, `versionCode` is incremented with each release. For Desktop and Web, the semantic version string is used directly.

---

## CI/CD Pipeline

Yole uses GitHub Actions for automated builds:

```yaml
# .github/workflows/ci.yml
name: CI
on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          submodules: recursive
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Build and Test
        run: ./gradlew test :androidApp:assembleDebug :desktopApp:packageUberJarForCurrentOS
```

### Security Scanning in CI

Six security scanners run as part of the CI/CD pipeline:

| Scanner | Trigger | What It Checks |
|---------|---------|----------------|
| SonarQube | Push | Bugs, vulnerabilities, code smells |
| Snyk | PR | Dependency vulnerabilities |
| CodeQL | Push | Semantic code analysis |
| Gitleaks | PR | Leaked secrets in git history |
| OWASP DC | Weekly | Known CVEs in dependencies |
| Detekt | Push | Kotlin-specific code issues |

See [SECURITY_SCANNING.md](SECURITY_SCANNING.md) for details.

---

## Related Documentation

- [BUILD_SYSTEM.md](BUILD_SYSTEM.md) -- Build system details
- [SECURITY_SCANNING.md](SECURITY_SCANNING.md) -- Security scanning setup
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) -- Common build issues
- [CI_SETUP_GUIDE.md](CI_SETUP_GUIDE.md) -- CI/CD pipeline setup
