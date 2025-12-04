# Yole - Quick Start Guide

## 🚀 Get Started in 5 Minutes

Yole is a **cross-platform text editor** supporting Android, iOS, Desktop (Windows/macOS/Linux), and Web with **18+ markup formats**.

## 📱 Platform-Specific Setup

### Android
```bash
# Build and install
./gradlew :androidApp:assembleDebug
adb install androidApp/build/outputs/apk/debug/androidApp-debug.apk

# Or run directly
./gradlew :androidApp:installDebug
```

### Desktop (Windows/macOS/Linux)
```bash
# Run desktop app
./gradlew :desktopApp:run

# Build installers
./gradlew :desktopApp:package
```

### Web
```bash
# Start development server
./gradlew :webApp:wasmJsBrowserRun

# Build for production
./gradlew :webApp:wasmJsBrowserDistribution
```

### iOS
```bash
# Open in Xcode
open iosApp/iosApp.xcodeproj

# Build and run from Xcode
```

## 📝 Supported Formats

Yole supports **18 markup formats**:

### Core Formats
- **Markdown** (.md, .markdown)
- **Todo.txt** (.txt)
- **CSV** (.csv)
- **Plain Text** (.txt, .text, .log)

### Wiki Formats
- **WikiText** (.wiki, .wikitext)
- **Creole** (.creole)
- **TiddlyWiki** (.tid, .tiddly)

### Technical Formats
- **LaTeX** (.tex, .latex)
- **AsciiDoc** (.adoc, .asciidoc)
- **Org Mode** (.org)
- **reStructuredText** (.rst, .rest)

### Specialized Formats
- **Key-Value** (.keyvalue, .properties, .ini)
- **TaskPaper** (.taskpaper)
- **Textile** (.textile)

### Data Science
- **Jupyter Notebook** (.ipynb)
- **R Markdown** (.rmd, .rmarkdown)

### Binary
- **Binary Files** (all other formats)

## 🔧 Development Commands

### Build & Test
```bash
# Build everything
./gradlew clean build

# Run all tests
./gradlew test

# Run specific tests
./gradlew test --tests "digital.vasic.yole.format.markdown.*"

# Generate coverage report
./gradlew koverHtmlReport
```

### Platform Development
```bash
# Android development
./gradlew :androidApp:assembleDebug

# Desktop development
./gradlew :desktopApp:run

# Web development
./gradlew :webApp:wasmJsBrowserRun

# iOS development (requires Xcode)
# Build from Xcode
```

## 🏗️ Architecture Overview

Yole uses **modern cross-platform development** with native applications for each platform:

```
androidApp/     # Native Android app
├── src/main/kotlin/
└── build.gradle.kts

desktopApp/     # Native Desktop app
├── src/main/kotlin/
└── build.gradle.kts

iosApp/         # Native iOS app
├── src/
└── build.gradle.kts

webApp/         # Progressive Web App
├── src/main/kotlin/
└── build.gradle.kts
```

### Key Features
- **Cross-Platform**: Native applications for each platform
- **Modern UI**: Platform-specific UI frameworks
- **Extensible**: Modular format support
- **Performance**: Optimized for each platform
- **Accessibility**: Full accessibility support

## 📚 Documentation

- **Architecture**: `ARCHITECTURE.md`
- **Testing Strategy**: `TESTING_STRATEGY.md`
- **Development Guide**: `AGENTS.md`
- **Migration Archive**: `MIGRATION_ARCHIVE.md`

## 🎯 Getting Help

### Common Issues
1. **Build failures**: Run `./gradlew clean` and try again
2. **iOS issues**: Ensure Xcode and iOS SDK are installed
3. **Web issues**: Check browser compatibility
4. **Format detection**: Check file extensions and content

### Support
- Check existing documentation
- Review migration status
- Test with sample files in `samples/` directory

## 🚀 Next Steps

1. **Try the apps**: Build and run on your target platform
2. **Test formats**: Open sample files from `samples/` directory
3. **Customize**: Modify settings and preferences
4. **Extend**: Add custom formats or features

---

**Yole is now a modern, cross-platform text editor ready for production use!** 🎉