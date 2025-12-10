# Yole Android App

The flagship Android implementation of Yole, a powerful multi-format text editor.

## Features

- **Multi-Format Support**: Edit 17+ text formats including Markdown, LaTeX, Org Mode, and more
- **Rich Preview**: Live HTML preview with syntax highlighting and math rendering
- **File Management**: Built-in file browser with favorites, encryption, and cloud sync
- **Material Design 3**: Modern, polished UI with smooth animations
- **Offline-First**: Works without internet connection
- **Privacy-Focused**: No data collection, local processing only

## Building

```bash
# Build debug APK
./gradlew :androidApp:assembleDebug

# Install on connected device
./gradlew :androidApp:installDebug

# Run tests
./gradlew :androidApp:test
```

## Requirements

- **Min SDK**: 18 (Android 4.3)
- **Target SDK**: 35 (Android 15)
- **Java**: 11+
- **RAM**: 1GB minimum, 2GB recommended

## Architecture

- **Language**: Kotlin with Java interoperability
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with repository pattern
- **Shared Code**: Kotlin Multiplatform shared module
- **Storage**: Android Storage Access Framework

## Key Components

- `YoleApp.kt` - Main application class
- `MainActivity.kt` - Main activity
- `ui/` - Compose UI components
- `Accessibility.kt` - Android accessibility features

## Distribution

- **F-Droid**: Available
- **GitHub Releases**: APK downloads
- **Google Play**: Planned

## Contributing

See [Contributing Guide](../CONTRIBUTING.md) for development setup and contribution guidelines.