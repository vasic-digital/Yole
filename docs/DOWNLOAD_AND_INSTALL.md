# Download and Install Yole

Complete installation guide for all platforms.

**Current Version**: v2.15.1
**Last Updated**: November 11, 2025

---

## Quick Links

| Platform | Status | Download |
|----------|--------|----------|
| **Android** | ✓ Production | [F-Droid](#android-fdroid) \| [GitHub Releases](#android-github) |
| **Desktop** | ◐ Beta | [Build from Source](#desktop-build-from-source) |
| **iOS** | = Development | Coming Q2 2026 |
| **Web** | = Development | Coming Q3 2026 |

---

## Android Installation

### Android: F-Droid (Recommended)

**Status**: ✓ Production Ready

F-Droid is a repository of free and open-source Android apps. This is the recommended installation method.

#### Installation Steps

1. **Install F-Droid** (if not already installed):
   - Visit [https://f-droid.org/](https://f-droid.org/)
   - Download and install the F-Droid app
   - Open F-Droid and let it update repositories

2. **Install Yole from F-Droid**:
   - Open F-Droid
   - Search for "Yole"
   - Tap on **Yole - Text Editor**
   - Tap **Install**
   - Grant required permissions when prompted

3. **Launch Yole**:
   - Find Yole in your app drawer
   - Open it and start editing!

#### F-Droid Links

- **F-Droid Listing**: [https://f-droid.org/packages/digital.vasic.yole](https://f-droid.org/packages/digital.vasic.yole)
- **F-Droid Metadata**: [fdroiddata/metadata](https://gitlab.com/fdroid/fdroiddata/blob/master/metadata/digital.vasic.yole.yml)
- **Build Logs**: [F-Droid Build Status](https://f-droid.org/wiki/page/digital.vasic.yole/lastbuild)

#### Advantages of F-Droid

- ✓ Automatic updates
- ✓ Verified reproducible builds
- ✓ No tracking or analytics
- ✓ Open source infrastructure
- ✓ Security-focused

---

### Android: GitHub Releases

**Status**: ✓ Production Ready

Download APK files directly from GitHub releases.

#### Installation Steps

1. **Download APK**:
   - Visit [GitHub Releases](https://github.com/vasic-digital/Yole/releases/latest)
   - Download the latest `.apk` file
   - Recommended: `Yole-v2.15.1-flavorDefault-release.apk`

2. **Enable Installation from Unknown Sources**:
   - Go to **Settings**  **Security**
   - Enable **Install unknown apps** for your browser or file manager
   - (Android 8.0+) Grant permission when prompted

3. **Install APK**:
   - Open the downloaded APK file
   - Tap **Install**
   - Grant required permissions
   - Tap **Open** to launch

4. **Verify Installation**:
   - Check **Settings**  **Apps**  **Yole**
   - Version should be v2.15.1 or later

#### GitHub Release Links

- **Latest Release**: [https://github.com/vasic-digital/Yole/releases/latest](https://github.com/vasic-digital/Yole/releases/latest)
- **All Releases**: [https://github.com/vasic-digital/Yole/releases](https://github.com/vasic-digital/Yole/releases)
- **Changelog**: [CHANGELOG.md](https://github.com/vasic-digital/Yole/blob/master/CHANGELOG.md)

#### Build Flavors

| Flavor | Description | File Name |
|--------|-------------|-----------|
| **flavorDefault** | Recommended build | `Yole-v2.15.1-flavorDefault-release.apk` |
| **flavorGplay** | Google Play variant | `Yole-v2.15.1-flavorGplay-release.apk` |
| **flavorAtest** | Testing variant | `Yole-v2.15.1-flavorAtest-release.apk` |

**Recommendation**: Use **flavorDefault** for most users.

---

### Android: Build from Source

**Status**: ✓ Advanced Users

Build Yole from source code for maximum control and transparency.

#### Prerequisites

- **Android Studio**: Latest version (recommended)
- **JDK**: Java 11 or later
- **Git**: For cloning the repository
- **Android SDK**: API level 35 (automatically installed by Android Studio)

#### Build Steps

1. **Clone Repository**:
   ```bash
   git clone https://github.com/vasic-digital/Yole.git
   cd Yole
   ```

2. **Open in Android Studio**:
   - Launch Android Studio
   - Select **Open an Existing Project**
   - Navigate to the cloned `Yole` directory
   - Wait for Gradle sync to complete

3. **Build APK**:
   ```bash
   # Using Gradle directly
   ./gradlew assembleFlavorDefaultDebug

   # Or use the Makefile
   make build
   ```

4. **Install on Device**:
   ```bash
   # Connect your Android device via USB
   # Enable USB Debugging in Developer Options

   # Install via Gradle
   ./gradlew installFlavorDefaultDebug

   # Or use Makefile
   make install
   ```

5. **Run on Device**:
   ```bash
   # Launch the app
   make run

   # Or manually from device
   ```

#### Build Output Location

- **APK Files**: `dist/*.apk` (via Makefile) or `app/build/outputs/apk/`
- **Logs**: `dist/log/`
- **Test Results**: `dist/tests/`

#### Makefile Commands

```bash
make build          # Build APK
make install        # Install on connected device
make run            # Launch app on device
make all            # Build, install, and run
make test           # Run unit tests
make lint           # Run linter
make clean          # Clean build artifacts
```

For more details, see [Build System Guide](BUILD_SYSTEM.md).

---

## Desktop Installation

### Desktop: Build from Source

**Status**: ◐ Beta (30% Complete)

Desktop support is currently in beta. Build from source for testing.

#### Supported Platforms

- ✓ **Windows** (Windows 10/11)
- ✓ **macOS** (macOS 11 Big Sur or later)
- ✓ **Linux** (Ubuntu 20.04+, Fedora 35+, other modern distros)

#### Prerequisites

- **JDK**: Java 11 or later
- **Git**: For cloning the repository
- **Gradle**: 8.11.1 (included via wrapper)

#### Build Steps

1. **Clone Repository**:
   ```bash
   git clone https://github.com/vasic-digital/Yole.git
   cd Yole
   ```

2. **Run Desktop App**:
   ```bash
   ./gradlew :desktopApp:run
   ```

3. **Build Distributable**:
   ```bash
   # Create platform-specific package
   ./gradlew :desktopApp:packageDistributionForCurrentOS
   ```

4. **Build Universal JAR**:
   ```bash
   # Cross-platform JAR file
   ./gradlew :desktopApp:packageUberJarForCurrentOS
   ```

#### Output Locations

- **Windows Installer**: `desktopApp/build/compose/binaries/main/msi/`
- **macOS DMG**: `desktopApp/build/compose/binaries/main/dmg/`
- **Linux DEB/RPM**: `desktopApp/build/compose/binaries/main/deb/` or `rpm/`
- **Universal JAR**: `desktopApp/build/compose/jars/`

#### Known Limitations (Beta)

- ◐ Some format action buttons not implemented
- ◐ Limited preview options for certain formats
- ◐ UI polish needed
- ◐ Performance optimization ongoing

#### System Requirements

| Platform | Minimum | Recommended |
|----------|---------|-------------|
| **Windows** | Windows 10 | Windows 11 |
| **macOS** | macOS 11 (Big Sur) | macOS 13 (Ventura) or later |
| **Linux** | Ubuntu 20.04 | Ubuntu 22.04 or later |
| **RAM** | 2 GB | 4 GB+ |
| **Storage** | 100 MB | 500 MB |
| **JRE** | Java 11 | Java 17+ |

---

## iOS Installation (Coming Q2 2026)

### iOS: Current Status

**Status**: = In Development (Disabled)

iOS support is currently disabled due to compilation issues. It will be re-enabled once basic compilation is working.

#### Current Blockers

- L iOS targets commented out in `shared/build.gradle.kts:41`
- L Compilation issues with Kotlin Multiplatform iOS targets
- L TODO: "Re-enable iOS targets once basic compilation is working"

#### Planned Features

- ✓ Native iOS app with SwiftUI
- ✓ All 17 text formats supported
- ✓ iCloud Drive integration
- ✓ iOS file system access
- ✓ Share extension for text sharing
- ✓ Dark mode support
- ✓ iPad optimization

#### Target Platforms

- **iOS 14.0+**: iPhone and iPad
- **iPadOS 14.0+**: Optimized for iPad
- **Planned**: Mac Catalyst for native macOS

#### Expected Timeline

- **Q2 2026**: Beta release for testing
- **Q3 2026**: Production release on App Store

#### Follow Progress

- **GitHub Issues**: [iOS Support](https://github.com/vasic-digital/Yole/issues?q=is%3Aissue+label%3Aios)
- **Discussions**: [iOS Development](https://github.com/vasic-digital/Yole/discussions)

---

## Web Installation (Coming Q3 2026)

### Web: Current Status

**Status**: = Stub (0% Complete)

Web support has build configuration complete but no source implementation yet.

#### Planned Features

- ✓ Progressive Web App (PWA)
- ✓ Offline-first with service workers
- ✓ All 17 text formats supported
- ✓ Browser file system access
- ✓ Local storage for preferences
- ✓ Installable on desktop and mobile
- ✓ Works without internet connection

#### Technology Stack

- **Kotlin/Wasm**: WebAssembly compilation
- **Compose for Web**: UI framework
- **File System Access API**: Modern browser file access
- **Service Workers**: Offline support
- **IndexedDB**: Local storage

#### Supported Browsers

| Browser | Version | Status |
|---------|---------|--------|
| **Chrome/Edge** | 90+ | ✓ Full support |
| **Firefox** | 88+ | ✓ Full support |
| **Safari** | 15.4+ | ✓ Full support |
| **Opera** | 76+ | ✓ Full support |

#### Expected Timeline

- **Q3 2026**: Beta release at `https://yole.vasic.digital`
- **Q4 2026**: Production release with PWA installation

#### Follow Progress

- **GitHub Issues**: [Web Support](https://github.com/vasic-digital/Yole/issues?q=is%3Aissue+label%3Aweb)
- **Discussions**: [Web Development](https://github.com/vasic-digital/Yole/discussions)

---

## System Requirements

### Android Requirements

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| **Android Version** | 4.3 (API 18) | 14.0 (API 35) |
| **RAM** | 1 GB | 2 GB+ |
| **Storage** | 50 MB | 200 MB |
| **Permissions** | READ/WRITE_EXTERNAL_STORAGE | - |

### Desktop Requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| **OS** | Windows 10, macOS 11, Ubuntu 20.04 | Windows 11, macOS 13+, Ubuntu 22.04+ |
| **JRE** | Java 11 | Java 17+ |
| **RAM** | 2 GB | 4 GB+ |
| **Storage** | 100 MB | 500 MB |
| **Display** | 1280x720 | 1920x1080+ |

---

## Permissions (Android)

Yole requires the following Android permissions:

### Storage Permissions

- **READ_EXTERNAL_STORAGE**: Read files from storage
- **WRITE_EXTERNAL_STORAGE**: Write files to storage
- **Purpose**: Access and edit text files in any folder

### Network Permission

- **INTERNET**: Load external resources in user-generated content
- **Usage**: Only when user references external images/resources
- **Note**: App works completely offline

### Shortcut Permission

- **INSTALL_SHORTCUT**: Install launcher shortcuts
- **Purpose**: Quick access to specific files/folders

### Privacy Note

Yole **does not**:
- L Access your contacts
- L Access your location
- L Access your camera (except for image insertion)
- L Send data to external servers
- L Track your usage
- L Display advertisements

All data stays on your device!

---

## Updating Yole

### Android: F-Droid

1. **Automatic Updates** (Recommended):
   - Enable **Auto-update** in F-Droid settings
   - Yole will update automatically when new versions are available

2. **Manual Updates**:
   - Open F-Droid
   - Go to **Updates** tab
   - Find Yole
   - Tap **Update**

### Android: GitHub Releases

1. **Check for Updates**:
   - Visit [GitHub Releases](https://github.com/vasic-digital/Yole/releases/latest)
   - Compare version with your installed version

2. **Download and Install**:
   - Download latest APK
   - Install over existing version (data preserved)
   - No need to uninstall first

### Desktop

Rebuild from source or download new distributable when available.

---

## Uninstalling Yole

### Android

1. **Standard Uninstall**:
   - Go to **Settings**  **Apps**  **Yole**
   - Tap **Uninstall**
   - Confirm

2. **Data Preservation**:
   - Your text files are **not deleted**
   - Files remain in your Notebook directory
   - Backup recommended before uninstall

### Desktop

1. **Windows**:
   - Use **Add or Remove Programs**
   - Or delete application folder

2. **macOS**:
   - Drag Yole.app to Trash
   - Or use uninstaller if provided

3. **Linux**:
   - Use package manager: `sudo apt remove yole`
   - Or delete application directory

---

## Troubleshooting

### Android: Installation Issues

**Problem**: "App not installed"
- **Solution**: Uninstall old version, then install new version
- **Or**: Clear package installer cache

**Problem**: "Installation blocked"
- **Solution**: Enable "Install unknown apps" in Settings  Security

**Problem**: "Insufficient storage"
- **Solution**: Free up at least 50 MB of storage space

### Android: Permission Issues

**Problem**: Cannot access files
- **Solution**: Grant storage permissions in Settings  Apps  Yole  Permissions

**Problem**: Cannot write to SD card
- **Solution**: Browse to SD card root, create folder, follow SD card mount dialog

### Desktop: Build Issues

**Problem**: "Could not find or load main class"
- **Solution**: Rebuild with `./gradlew clean build`

**Problem**: Gradle sync fails
- **Solution**: Check internet connection, try `./gradlew --refresh-dependencies`

**Problem**: Out of memory during build
- **Solution**: Increase heap size in `gradle.properties`: `org.gradle.jvmargs=-Xmx4g`

### General Issues

See [FAQ](user-guide/faq.md) for more troubleshooting tips.

---

## Getting Help

### Support Resources

- **FAQ**: [docs/user-guide/faq.md](user-guide/faq.md)
- **GitHub Issues**: [Report bugs](https://github.com/vasic-digital/Yole/issues)
- **Discussions**: [Ask questions](https://github.com/vasic-digital/Yole/discussions)
- **Documentation**: [Complete guides](user-guide/)

### Reporting Issues

When reporting installation issues, please include:

1. Platform and version (Android 14, Windows 11, etc.)
2. Yole version attempting to install
3. Installation method (F-Droid, GitHub, build from source)
4. Error messages or screenshots
5. Device model (for Android)

---

## Related Documentation

- **[Getting Started Guide](user-guide/getting-started.md)** - First steps after installation
- **[Format Guides](user-guide/formats/)** - Learn about supported formats
- **[FAQ](user-guide/faq.md)** - Common questions and answers
- **[Build System Guide](BUILD_SYSTEM.md)** - Detailed build instructions
- **[Contributing Guide](../CONTRIBUTING.md)** - Contribute to development

---

## Version History

| Version | Date | Highlights |
|---------|------|------------|
| **v2.15.1** | 2025-11 | Modern Android architecture, smooth animations, settings persistence |
| **v2.11** | 2023-10 | AsciiDoc, CSV, Org-Mode support, todo.txt advanced search, line numbers |
| **v2.10** | 2023-06 | Custom file templates, URL tracking removal |
| **v2.9** | 2023-03 | Snippets, templates, graphs, charts, diagrams, YAML front-matter |

See [CHANGELOG.md](../CHANGELOG.md) for complete version history.

---

## Download Statistics

[![GitHub downloads](https://img.shields.io/github/downloads/vasic-digital/Yole/total.svg?logo=github&logoColor=lime)](https://github.com/vasic-digital/Yole/releases)

**Total Downloads**: See badge above for current count

---

*Last updated: November 11, 2025*
*Current version: v2.15.1*
*Next update: Desktop beta packages (Q1 2026)*
