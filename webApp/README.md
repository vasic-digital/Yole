# Yole Web App

Progressive Web App (PWA) implementation of Yole text editor, built with Kotlin/Wasm and Compose for Web.

## Features

- **Browser-Based**: Runs in any modern web browser
- **Progressive Web App**: Installable on desktop and mobile
- **Offline Support**: Works without internet connection
- **File System Access**: Modern browser APIs for local file access
- **Responsive Design**: Adapts to any screen size
- **Cross-Platform**: Works on Windows, macOS, Linux, Android, iOS

## Building

```bash
# Run development server
./gradlew :webApp:wasmJsBrowserRun

# Build for production
./gradlew :webApp:wasmJsBrowserProductionWebpack

# Run tests
./gradlew :webApp:wasmJsTest
```

## Requirements

- **Browsers**: Chrome 90+, Firefox 88+, Safari 15.4+
- **JavaScript**: Enabled (required for Wasm)
- **Storage**: 50MB browser cache
- **RAM**: 2GB minimum

## Architecture

- **Language**: Kotlin compiled to WebAssembly
- **UI Framework**: Compose for Web
- **Shared Code**: Kotlin Multiplatform shared module
- **Build Target**: WebAssembly (wasmJs)
- **Hosting**: Static files served from web server

## Key Components

- `Main.kt` - Application entry point
- `YoleWebApp.kt` - Main UI composable
- `FormatList.kt` - Format selection component
- `MarkdownPreview.kt` - HTML preview component

## Current Status

- ✅ Basic UI structure
- ✅ Format selection
- ✅ Document templates
- ✅ Live preview (basic)
- ✅ Theme switching
- 🔄 File download (placeholder)
- ❌ File system integration
- ❌ Advanced preview features

## Distribution

- **Planned URL**: https://yole.vasic.digital
- **Hosting**: Static hosting (GitHub Pages, Netlify, or similar)
- **CDN**: Cloudflare or similar

## Browser APIs Used

- **File System Access API**: For local file access
- **IndexedDB**: For offline storage
- **Service Workers**: For offline functionality
- **WebAssembly**: For high-performance code execution

## Contributing

See [Contributing Guide](../CONTRIBUTING.md) for development setup and contribution guidelines.