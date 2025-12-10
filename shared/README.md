# Yole Shared Module

Cross-platform business logic for Yole text editor, built with Kotlin Multiplatform (KMP).

## Overview

The shared module contains all platform-agnostic code that powers Yole across Android, Desktop, iOS, and Web platforms. This includes format parsers, document models, utilities, and common interfaces.

## Features

- **17 Format Parsers**: Complete parsing and HTML generation for all supported formats
- **Format Registry**: Centralized format detection and management
- **Document Model**: Cross-platform document representation
- **Text Processing**: Common text manipulation utilities
- **Network Protocols**: HTTP client abstractions and file sync protocols
- **Platform Interfaces**: Expect/actual pattern for platform-specific code

## Supported Formats

| Format | Parser | HTML Export | Tests |
|--------|--------|-------------|-------|
| Markdown | ✅ | ✅ | ✅ |
| LaTeX | ✅ | ✅ | ✅ |
| Org Mode | ✅ | ✅ | ✅ |
| Todo.txt | ✅ | ✅ | ✅ |
| CSV | ✅ | ✅ | ✅ |
| WikiText | ✅ | ✅ | ✅ |
| AsciiDoc | ✅ | ✅ | ✅ |
| reStructuredText | ✅ | ✅ | ✅ |
| Textile | ✅ | ✅ | ✅ |
| TaskPaper | ✅ | ✅ | ✅ |
| Creole | ✅ | ✅ | ✅ |
| TiddlyWiki | ✅ | ✅ | ✅ |
| Jupyter | ✅ | ✅ | ✅ |
| R Markdown | ✅ | ✅ | ✅ |
| Key-Value | ✅ | ✅ | ✅ |
| Plain Text | ✅ | ✅ | ✅ |
| Binary | ✅ | ✅ | ✅ |

## Architecture

### Directory Structure

```
shared/src/
├── commonMain/          # Platform-agnostic code
│   ├── kotlin/digital/vasic/yole/
│   │   ├── format/      # Format parsers and registry
│   │   ├── model/       # Document and data models
│   │   ├── network/     # HTTP and sync protocols
│   │   └── ui/          # Common UI utilities
├── commonTest/          # Shared unit tests
├── androidMain/         # Android-specific implementations
├── desktopMain/         # Desktop-specific implementations
├── iosMain/             # iOS-specific implementations (disabled)
└── wasmJsMain/          # Web-specific implementations
```

### Key Components

- **`FormatRegistry`**: Central registry for all format parsers
- **`ParserRegistry`**: Factory for creating parser instances
- **`TextFormat`**: Interface for format parsers
- **`Document`**: Cross-platform document model
- **`FormatDetection`**: Automatic format detection by content/extension

## Building

```bash
# Build all targets
./gradlew :shared:build

# Run tests
./gradlew :shared:test

# Generate API docs
./gradlew :shared:dokkaHtml
```

## Platform Support

| Platform | Status | Implementation |
|----------|--------|----------------|
| **Android** | ✅ Complete | Full platform integration |
| **Desktop** | ✅ Complete | JVM-based implementation |
| **iOS** | ❌ Disabled | Compilation issues (Kotlin/Native) |
| **Web** | ✅ Complete | WebAssembly implementation |

## Testing

- **Test Count**: 850+ unit tests
- **Coverage**: 93%+ line coverage
- **Framework**: Kotlin Test, JUnit 4
- **CI/CD**: GitHub Actions with coverage reporting

## Dependencies

### Common Dependencies
- **Kotlinx.Coroutines**: Asynchronous programming
- **Kotlinx.Datetime**: Date/time handling
- **Kotlinx.Serialization**: JSON parsing

### Platform-Specific
- **Android**: AndroidX libraries, Room (planned)
- **Desktop**: Java standard library, Preferences API
- **Web**: Browser APIs, IndexedDB
- **iOS**: Foundation, CoreData (planned)

## Code Quality

- **KDoc**: 100% documentation coverage
- **Linting**: Detekt configuration
- **Formatting**: Kotlin coding standards
- **Security**: Input validation, secure defaults

## Contributing

The shared module is the core of Yole. When contributing:

1. **Platform-Agnostic**: Keep code platform-independent
2. **Test Coverage**: Add tests for new functionality
3. **Documentation**: Document all public APIs
4. **Performance**: Consider performance implications
5. **Security**: Validate inputs and handle errors safely

See [Contributing Guide](../CONTRIBUTING.md) for detailed guidelines.