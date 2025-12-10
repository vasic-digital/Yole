# Yole iOS App

Native iOS implementation of Yole text editor, built with SwiftUI and Kotlin Multiplatform.

## Features

- **Native iOS Experience**: Optimized for iPhone and iPad
- **SwiftUI Interface**: Modern, native iOS design
- **iCloud Integration**: Sync documents across devices
- **Share Extensions**: Edit text from other apps
- **Widget Support**: Quick access to recent documents
- **Handoff**: Continue editing on Mac with Catalyst

## Building

**Note**: iOS development requires macOS with Xcode.

```bash
# Build for simulator
./gradlew :iosApp:build

# Open in Xcode
open iosApp/iosApp.xcodeproj

# Run tests (from Xcode)
```

## Requirements

- **macOS**: 12.0+ (for development)
- **Xcode**: 14.0+
- **iOS**: 14.0+ (runtime)
- **Device**: iPhone, iPad, or Mac with Catalyst

## Architecture

- **UI Language**: Swift with SwiftUI
- **Business Logic**: Kotlin Multiplatform shared module
- **Build System**: Xcode + Gradle (for KMP)
- **Framework**: SwiftUI + UIKit integration
- **Persistence**: Core Data + iCloud

## Key Components

- `iosApp/` - Xcode project directory
- `iosApp.xcodeproj` - Xcode project file
- `Assets.xcassets` - App icons and assets
- `Info.plist` - App configuration

## Current Status

- ❌ **Temporarily Disabled**: iOS targets commented out due to compilation issues
- 🔄 **Planned**: Re-enable once KMP iOS compilation is fixed
- 📋 **Architecture**: SwiftUI + KMP shared code

## Distribution

- **App Store**: Planned for production release
- **TestFlight**: For beta testing

## iOS-Specific Features

- **Split View**: Side-by-side editing on iPad
- **Keyboard Toolbar**: Format buttons above keyboard
- **Context Menus**: Right-click menus on iPad
- **Share Sheet**: Export and share documents
- **Shortcuts App**: Automate common tasks
- **Live Text**: OCR text recognition (iOS 15+)

## Contributing

**Note**: iOS development requires macOS. See [Contributing Guide](../CONTRIBUTING.md) for general guidelines.

For iOS-specific development:
1. Ensure you have macOS with Xcode installed
2. Run `./gradlew :iosApp:build` to verify KMP compilation
3. Open `iosApp/iosApp.xcodeproj` in Xcode
4. Build and run on simulator or device

## Known Issues

- **Compilation Blocker**: iOS targets disabled in `shared/build.gradle.kts`
- **Framework Export**: Issues with Kotlin/Native framework generation
- **Xcode Integration**: Complex setup with Gradle + Xcode build process

## Roadmap

- **Q1 2026**: Fix compilation issues, re-enable iOS targets
- **Q2 2026**: Basic SwiftUI implementation, TestFlight beta
- **Q3 2026**: Full feature set, App Store release