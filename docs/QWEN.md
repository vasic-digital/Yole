# Yole Project Context

## Project Overview
Yole is a cross-platform text editor supporting Android, Desktop, iOS, and Web platforms. It's designed as a simple, lightweight, and versatile tool that supports 18+ markup formats including Markdown, todo.txt, CSV, LaTeX, reStructuredText, TaskPaper, Textile, Creole, TiddlyWiki, Jupyter, R Markdown, AsciiDoc, Org-mode, and more. The project emphasizes privacy, working completely offline with no accounts, tracking, or ads.

## Architecture & Technology Stack
- **Primary Language**: Kotlin with Kotlin Multiplatform (KMP) for shared business logic
- **Build System**: Gradle with Gradle Version Catalog for dependency management
- **Kotlin Version**: 2.1.0
- **KMP Target Platforms**: Android, JVM (Desktop), WebAssembly (Web), and iOS (currently disabled)
- **UI Frameworks**:
  - Android: Jetpack Compose with Material3
  - Desktop: Compose Multiplatform
  - Web: Compose Multiplatform for WebAssembly
  - iOS: Native implementation (currently disabled)
- **Module Structure**:
  - `androidApp`: Native Android application with Compose
  - `desktopApp`: Desktop application for Windows/macOS/Linux using Compose Multiplatform
  - `webApp`: Progressive Web App using Kotlin/Wasm
  - `iosApp`: Native iOS application (currently disabled in build)
  - `shared`: Kotlin Multiplatform module with common code shared across platforms
  - `core`: Core business logic for Android
  - `commons`: Common utilities for Android
  - Format modules (e.g., `format-markdown`, `format-todotxt`, etc.): Android-specific implementations of text format parsing
- **Dependencies Management**: libs.versions.toml for centralized dependency management

## Building and Running
### Android
```bash
./gradlew :androidApp:assembleDebug
adb install androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

### Desktop
```bash
./gradlew :desktopApp:run
```

### Web
```bash
./gradlew :webApp:wasmJsBrowserRun
```

### iOS
iOS builds are currently disabled in the build configuration but can be enabled by uncommenting iOS targets in the shared module's build.gradle.kts.

### General Commands
```bash
# Build all platforms
./gradlew clean build

# Run tests
./gradlew test

# Build for specific platform
./gradlew :shared:build  # Build shared module
./gradlew :shared:compileKotlinWasmJs  # Compile to WebAssembly
```

## Development Architecture
Yole uses a hybrid approach that combines:
1. **Kotlin Multiplatform** for shared business logic in the `shared` module
2. **Platform-specific native implementations** in respective app modules
3. **Expect/Actual mechanism** for platform-specific implementations (e.g., `getCurrentDate()` function)

### Shared Module Structure
- `commonMain`: Common code for all platforms using Kotlin Multiplatform
- `androidMain`: Android-specific implementations of expect functions
- `desktopMain`: Desktop-specific implementations of expect functions
- `wasmJsMain`: Web-specific implementations of expect functions
- Platform-specific implementations of actual functions for each target

### Format Parsing
- Each format has implementations both in the shared KMP module and in Android-specific format modules
- Shared implementations use expect/actual for platform-specific functionality
- Android format modules implement traditional Android libraries (using Flexmark for Markdown, etc.)

## Development Setup
### Prerequisites
- Android Studio/IntelliJ IDEA (for Android, Desktop, and shared module development)
- Xcode (for iOS development, if enabled)
- Git for version control
- Kotlin 2.1.0+ (managed via Gradle)
- JDK 11+ (required by project configuration)

### Setup
```bash
git clone https://github.com/yourusername/yole.git
cd yole
./gradlew build  # Initialize and build project
```

## Key Features
- Rich format support with syntax highlighting
- Works completely offline
- File synchronization compatibility (Syncthing, Dropbox, Nextcloud, etc.)
- Highly customizable with dark theme option
- Notebook organization with folder support
- Clipboard integration for quick note-taking
- Auto-save with undo/redo options
- Cross-platform compatibility

## Migration History
The project initially started as a fork of Yole, then underwent a Kotlin Multiplatform migration which was later partially reversed. Current architecture maintains KMP shared code for cross-platform functionality but also includes platform-specific native implementations for optimal performance.

## Development Conventions
- Follow Kotlin coding conventions
- Use camelCase for variables and functions
- Use PascalCase for classes
- Include SPDX license headers
- Use expect/actual for platform-specific functionality in shared module
- Development workflow: Fork → Create feature branch → Make changes → Run tests → Submit pull request

## Documentation Structure
The documentation includes:
- README: Project overview and features
- QUICK_START: Setup guide for all platforms
- ARCHITECTURE: Technical overview and module structure
- CONTRIBUTING: Guidelines for contributing to the project
- User guides for specific features and formats in the `doc/` subdirectory

## Current Version
v2.15.1

## Repository
The source code is available at:
- GitHub: https://github.com/vasic-digital/Yole
- F-Droid: https://f-droid.org/repository/browse/?fdid=digital.vasic.yole

This project appears to be a fork or alternative name for the Yole project, which is a well-known open-source note-taking app, with extensive Kotlin Multiplatform support added for cross-platform functionality.