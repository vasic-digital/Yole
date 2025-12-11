# Yole - Development Guide for AI Agents

## Project Overview

**Yole** is a cross-platform text editor built with Kotlin Multiplatform (KMP), supporting Android (production), Desktop (beta), iOS (development), and Web (development) platforms. It supports 17 text formats including Markdown, Todo.txt, CSV, LaTeX, Org Mode, and more.

**Architecture Philosophy**: Maximize code sharing through Kotlin Multiplatform, with platform-specific implementations only where necessary for optimal user experience.

## Technology Stack

- **Language**: Kotlin with Java 11+ compatibility
- **Architecture**: Kotlin Multiplatform (KMP) with platform-specific UI layers
- **Build System**: Gradle with Kotlin DSL and version catalog (`gradle/libs.versions.toml`)
- **UI Framework**: Compose Multiplatform for cross-platform UI
- **Testing**: JUnit 4/5 + Kotest + AssertJ + MockK (platform-specific)
- **Documentation**: KDoc with Dokka for API documentation
- **CI/CD**: GitHub Actions with comprehensive test and coverage reporting

## Project Structure

```
Yole/
├── shared/                    # Kotlin Multiplatform shared module
│   ├── src/commonMain/        # Platform-agnostic business logic
│   ├── src/androidMain/       # Android-specific implementations
│   ├── src/desktopMain/       # Desktop-specific implementations  
│   ├── src/iosMain/           # iOS-specific implementations
│   ├── src/wasmJsMain/        # Web-specific implementations
│   ├── src/commonTest/        # Shared test code
│   └── src/desktopBenchmark/  # Performance benchmarks
├── androidApp/               # Android application module
├── desktopApp/               # Desktop application module
├── webApp/                   # Web application module (WASM)
├── iosApp/                   # iOS application module
├── commons/                  # Legacy Android utilities
├── docs/                     # Comprehensive documentation
└── samples/                  # Example files for all formats
```

## Core Architecture

### Shared Module (`shared/`)
Contains all platform-agnostic business logic:

- **Format System**: 17 format parsers with unified interface
- **Document Model**: Cross-platform document representation
- **Network Storage**: Multi-protocol cloud storage support
- **UI Components**: Shared Compose components and theming
- **Format Registry**: Central format detection and management

### Format Support (17 formats)
- **Core**: Markdown, Plain Text, Todo.txt, CSV
- **Wiki**: WikiText, Org Mode, Creole, TiddlyWiki
- **Technical**: LaTeX, AsciiDoc, reStructuredText
- **Specialized**: Key-Value, TaskPaper, Textile
- **Data Science**: Jupyter, R Markdown
- **Binary**: Binary file detection

### Platform Status
| Platform | Status | Completion | Notes |
|----------|--------|------------|-------|
| **Android** | ✅ Production | 100% | Fully functional, F-Droid distribution |
| **Desktop** | ⚠️ Beta | 30% | Basic implementation, needs completion |
| **iOS** | 🚧 Development | 0% | Targets disabled, requires compilation fixes |
| **Web** | 🚧 Development | 0% | Build ready, no source implementation |

## Build Commands

### Platform Builds
```bash
# Android
./gradlew :androidApp:assembleDebug
make build

# Desktop  
./gradlew :desktopApp:run
make desktop

# Web (WASM)
./gradlew :webApp:wasmJsBrowserRun
make web

# iOS (requires macOS)
# Open iosApp/iosApp.xcodeproj in Xcode
```

### Development Commands
```bash
# Clean build
./gradlew clean
make clean

# Lint code
./gradlew lint
make lint

# Run tests with coverage
./gradlew test koverHtmlReport
./run_all_tests.sh

# Single test execution
./gradlew test --tests "digital.vasic.yole.format.todotxt.TodoTxtQuerySyntaxTests.ParseQuery"

# Generate API documentation
./gradlew :shared:dokkaHtml
mkdir -p docs/api && cp -r shared/build/dokka/html/* docs/api/

# Run benchmarks
./gradlew :shared:runSimpleBenchmarks
./run_simple_benchmarks.sh
```

## Testing Strategy

### Test Structure
- **Unit Tests**: Individual parser and component testing
- **Integration Tests**: Cross-component functionality testing
- **Comprehensive Tests**: Format-specific edge case testing
- **Benchmark Tests**: Performance and memory usage testing
- **Platform Tests**: Platform-specific functionality testing

### Coverage Requirements
- **Target**: 100% coverage across all modules
- **Current**: 93% overall coverage achieved
- **Minimum**: 70% coverage enforced by Kover verification

### Test Generation
```bash
# Generate format-specific tests
./scripts/generate_format_tests.sh <format-name> <extension>
./scripts/generate_format_tests.sh Markdown .md

# Available templates: ParserTest, IntegrationTest, MockKExample, 
# KotestPropertyTest, UITest, SnapshotTest
```

### Coverage Analysis
```bash
# Generate coverage reports
./gradlew koverHtmlReport        # HTML report → build/reports/kover/html/
./gradlew koverXmlReport         # XML report → build/reports/kover/report.xml

# Analyze coverage with Python script
python3 analyze_coverage.py      # Identifies low-coverage parsers
```

## Code Style Guidelines

### Language Standards
- **Kotlin**: Follow official Kotlin coding conventions
- **Java Compatibility**: Java 11+ for all JVM targets
- **Multiplatform**: Use `expect/actual` pattern for platform-specific code

### Package Structure
- `digital.vasic.yole.*` - Main application code
- `digital.vasic.yole.format.*` - Format parsers and related code
- `digital.vasic.yole.network.*` - Network storage protocols
- `digital.vasic.yole.ui.*` - UI components and theming
- `net.gsantner.opoc.*` - Legacy utility code (being phased out)

### Naming Conventions
- **Classes**: PascalCase (e.g., `TextFormat`, `MarkdownParser`)
- **Functions**: camelCase (e.g., `parseContent()`, `detectFormat()`)
- **Variables**: camelCase (e.g., `documentContent`, `formatRegistry`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `ID_MARKDOWN`, `EXTENSION_MD`)
- **Packages**: lowercase with dots (e.g., `digital.vasic.yole.format`)

### Code Organization
- **Headers**: SPDX license header + maintainer info required
- **Imports**: Group standard library, third-party, then project imports
- **Documentation**: KDoc for all public APIs with examples
- **Error Handling**: Comprehensive error handling with specific exception types
- **Null Safety**: Leverage Kotlin null safety features

### File Structure
```kotlin
/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic <milos.vasic@example.com>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Brief description of the file
 *########################################################*/
package digital.vasic.yole.format

// Imports grouped by: standard, third-party, project

/**
 * Detailed class documentation with examples
 */
class ExampleClass {
    // Implementation
}
```

## Development Workflow

### Adding New Formats
1. Create parser in `shared/src/commonMain/kotlin/digital/vasic/yole/format/`
2. Add format to `FormatRegistry.kt`
3. Implement platform-specific optimizations if needed
4. Add comprehensive tests
5. Update documentation

### Platform-Specific Code
Use Kotlin Multiplatform `expect/actual` pattern:
```kotlin
// commonMain
expect fun platformSpecificFunction(): String

// androidMain  
actual fun platformSpecificFunction() = "Android"

// desktopMain
actual fun platformSpecificFunction() = "Desktop"
```

### Network Protocol Support
- Dropbox, Google Drive, OneDrive
- FTP, SFTP, WebDAV
- Git repositories
- SMB/CIFS network shares

### Performance Considerations
- Use coroutines for async operations
- Implement lazy loading for large documents
- Cache parsed documents when appropriate
- Benchmark critical paths with JMH

## Security Considerations

### File Encryption
- AES256 encryption for sensitive documents
- Password-based key derivation
- Platform-specific secure storage integration

### Network Security
- HTTPS for all cloud storage protocols
- Certificate validation
- Secure credential storage per platform

### Privacy
- No telemetry or data collection
- Local-only processing by default
- Optional cloud sync with user consent

## Dependencies Management

### Version Catalog
All dependencies managed in `gradle/libs.versions.toml`:
- Kotlin and core libraries
- AndroidX components
- Compose Multiplatform
- Format-specific libraries (Flexmark, OpenCSV)
- Testing frameworks

### Key Dependencies
- **Kotlin**: 2.0.20
- **Compose**: 1.7.3
- **Android SDK**: 35 (compile), 21 (min)
- **Coroutines**: 1.9.0
- **Flexmark**: 0.64.8 (Markdown parsing)

## Documentation Standards

### API Documentation
- 100% KDoc coverage for public APIs
- Include usage examples
- Document platform-specific behavior
- Cross-reference related functionality

### User Documentation
- Comprehensive format guides (400-1000+ lines each)
- Getting started tutorials
- FAQ and troubleshooting
- Platform-specific instructions

## CI/CD Pipeline

### GitHub Actions Workflows
- **Tests & Coverage**: Comprehensive test execution
- **Lint & Docs**: Code quality and documentation
- **PR Validation**: Pull request verification
- **Build**: Platform-specific builds

### Quality Gates
- All tests must pass
- Minimum 70% code coverage
- No lint violations
- Documentation up-to-date

## Troubleshooting

### Common Issues
1. **iOS Compilation**: Targets currently disabled due to framework issues
2. **Web Implementation**: No source code despite build configuration
3. **Desktop Completion**: Basic functionality needs enhancement

### Build Issues
- Check Java version (requires Java 11+)
- Verify Android SDK installation
- Clear Gradle cache if needed: `./gradlew clean`
- Check platform-specific requirements (Xcode for iOS)

### Performance Issues
- Run benchmarks to identify bottlenecks
- Profile memory usage for large documents
- Check coroutine usage for proper concurrency
- Verify format detection efficiency

This guide serves as the comprehensive reference for AI agents working on the Yole project. Always refer to the latest project documentation and source code for the most current implementation details.