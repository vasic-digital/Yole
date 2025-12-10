# Yole Desktop App

Cross-platform desktop application for Yole text editor, built with Kotlin and Compose Desktop.

## Features

- **Native Desktop Experience**: Runs on Windows, macOS, and Linux
- **Multi-Format Editing**: Support for 17+ text formats
- **Live Preview**: Real-time HTML preview with syntax highlighting
- **File Management**: Native file dialogs and system integration
- **Keyboard Shortcuts**: Full keyboard navigation support
- **Dark Theme**: System-aware theming

## Building

```bash
# Run the application
./gradlew :desktopApp:run

# Build for current OS
./gradlew :desktopApp:packageDistributionForCurrentOS

# Run tests
./gradlew :desktopApp:test
```

## Requirements

- **OS**: Windows 10+, macOS 11+, Linux (Ubuntu 20.04+)
- **Java**: 11+ (bundled in distribution)
- **RAM**: 2GB minimum, 4GB recommended
- **Display**: 1280x720 minimum

## Architecture

- **Language**: Kotlin
- **UI Framework**: Compose Desktop
- **Shared Code**: Kotlin Multiplatform shared module
- **Build System**: Gradle with compose-desktop plugin
- **Packaging**: Native installers (.msi, .dmg, .deb)

## Key Components

- `YoleApp.kt` - Main application entry point
- `ui/` - Compose UI components
- `Accessibility.kt` - Desktop accessibility features
- `YoleDesktopSettings.kt` - Desktop-specific settings

## Distribution

- **Current**: Build from source only
- **Planned**: Native installers for all platforms

## Current Status

- ✅ Basic editing functionality
- ✅ Format detection and parsing
- ✅ Settings persistence
- ✅ Dark theme support
- 🔄 Preview functionality (basic)
- 🔄 Action buttons (partial)
- ❌ PDF export
- ❌ Installer packages

## Contributing

See [Contributing Guide](../CONTRIBUTING.md) for development setup and contribution guidelines.