<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Yole Web (Wasm PWA) User Manual

## Overview

Yole Web is a Progressive Web Application (PWA) built with Kotlin/Wasm and Compose Multiplatform. It runs entirely in your browser with no server-side processing, keeping your documents private and local.

**Current Status**: Alpha -- basic format support is functional with ongoing development for full feature parity with the Android and Desktop versions.

## Browser Requirements

### Supported Browsers
| Browser | Minimum Version | Notes |
|---------|----------------|-------|
| Google Chrome | 119+ | Full support, recommended |
| Microsoft Edge | 119+ | Full support (Chromium-based) |
| Mozilla Firefox | 120+ | Full support |
| Safari | 17.4+ | Requires WebAssembly support |
| Opera | 105+ | Full support (Chromium-based) |

### Required Features
- **WebAssembly (Wasm)**: Required for running the Kotlin/Wasm application
- **JavaScript**: Must be enabled
- **Local Storage**: Required for settings persistence
- **Service Workers**: Required for PWA and offline functionality

### Checking Compatibility
Visit the Yole Web URL in your browser. If you see the application load, your browser is compatible. If you see a blank page or error, your browser may not support WebAssembly.

## Installation as PWA

### Chrome / Edge
1. Navigate to the Yole Web URL
2. Click the **Install** icon in the address bar (or the three-dot menu > "Install Yole")
3. Confirm the installation
4. Yole appears in your app launcher / Start menu / Dock

### Firefox
1. Navigate to the Yole Web URL
2. Firefox does not support full PWA installation on desktop
3. Use "Add to Home Screen" on Firefox for Android
4. Alternatively, bookmark the page for quick access

### Safari (macOS / iOS)
1. Navigate to the Yole Web URL
2. On iOS: tap the Share button > "Add to Home Screen"
3. On macOS: Safari 17.4+ supports "Add to Dock"
4. The app icon appears on your home screen or dock

### Benefits of PWA Installation
- Launch Yole from your app launcher without opening the browser first
- Works offline after initial load
- Dedicated window without browser UI clutter
- Automatic updates when online

## Using Yole Web

### Creating Documents
1. Click the **New File** button or use the file menu
2. Select a format (Markdown, Todo.txt, Plain Text, etc.)
3. Enter a filename
4. Begin editing in the main editor area

### Editing
- The editor provides syntax highlighting for all supported formats
- Changes are saved to browser local storage automatically
- Use the toolbar buttons for format-specific actions (bold, links, headers, etc.)

### Preview Mode
- Toggle between edit and preview mode using the preview button
- Preview renders your document as formatted HTML
- Supports Markdown tables, headings, code blocks, and links

### Format Support
All 17 text formats are supported with the same parsing engine as Android and Desktop:

| Format | Edit | Preview | Status |
|--------|------|---------|--------|
| Markdown | Yes | Yes | Full support |
| Plain Text | Yes | Yes | Full support |
| Todo.txt | Yes | Yes | Full support |
| CSV | Yes | Yes | Full support |
| LaTeX | Yes | Yes | Full support |
| Org Mode | Yes | Yes | Full support |
| AsciiDoc | Yes | Yes | Basic parsing |
| WikiText | Yes | Yes | Basic parsing |
| Key-Value | Yes | Yes | Full support |
| TaskPaper | Yes | Yes | Basic parsing |
| Textile | Yes | Yes | Basic parsing |
| Creole | Yes | Yes | Basic parsing |
| TiddlyWiki | Yes | Yes | Basic parsing |
| Jupyter | Yes | Yes | JSON viewing |
| R Markdown | Yes | Yes | Basic parsing |
| reStructuredText | Yes | Yes | Basic parsing |
| Binary Detection | -- | -- | Detection only |

## Offline Mode

### How It Works
1. When you first visit Yole Web, the application is cached by the service worker
2. All application assets (Wasm binary, JavaScript, CSS) are stored locally
3. After initial load, Yole works entirely offline
4. Documents are stored in browser local storage

### Offline Capabilities
- Create, edit, and preview documents
- Full syntax highlighting and format support
- Settings persistence
- All format parsers available

### Limitations in Offline Mode
- No cloud storage sync (Dropbox, Google Drive, OneDrive)
- No external image loading (referenced by URL)
- No font downloads (use system fonts)

### Storage Limits
- Browser local storage is typically limited to 5-10 MB
- For larger document collections, consider exporting regularly
- IndexedDB may be used for expanded storage in future versions

## File Management

### Importing Files
1. Click **File > Import** or drag-and-drop a file onto the editor
2. The file contents are loaded into a new document
3. The format is auto-detected from the file extension

### Exporting Files
1. Click **File > Export** or use the download button
2. The current document is downloaded as a file
3. Choose your preferred filename and location

### Browser Storage
- Documents are stored in the browser's local storage
- Data persists across browser sessions
- Clearing browser data will delete stored documents
- Consider regular exports for important documents

## Settings

### Theme
- **Light / Dark / System** -- Follows your OS preference when set to System
- Theme preference is saved in local storage

### Editor Preferences
- **Font size**: Adjustable text size
- **Line numbers**: Show/hide line numbers
- **Auto-save**: Documents are saved automatically as you type

### Keyboard Shortcuts
Standard web editor shortcuts apply:
| Shortcut | Action |
|----------|--------|
| `Ctrl+S` | Save / Export |
| `Ctrl+Z` | Undo |
| `Ctrl+Shift+Z` | Redo |
| `Ctrl+F` | Find in document |
| `Ctrl+A` | Select all |

## Privacy and Security

### Data Privacy
- All processing happens locally in your browser
- No data is sent to any server
- No analytics, tracking, or telemetry
- Documents never leave your device unless you explicitly export them

### Security
- Yole Web runs in the browser's sandboxed environment
- No file system access beyond browser APIs
- No network requests except for initial application loading
- Content Security Policy headers protect against XSS

## Troubleshooting

### Application does not load
- Verify your browser supports WebAssembly (see Browser Requirements)
- Try clearing the browser cache and reloading
- Disable browser extensions that may block Wasm execution
- Ensure JavaScript is enabled

### Performance is slow
- WebAssembly applications have a startup cost for compilation
- After initial load, performance should be comparable to native
- Close other heavy browser tabs to free memory
- On older devices, Chrome typically provides the best Wasm performance

### Documents disappeared
- Check that you have not cleared browser local storage
- Documents are stored per-origin (URL) -- ensure you are using the same URL
- Private/incognito browsing does not persist local storage

### PWA not working offline
- Ensure the service worker was registered (first visit must be online)
- Check that your browser supports service workers
- Try uninstalling and reinstalling the PWA

### Copy/paste not working
- Some browsers restrict clipboard access for web applications
- Grant clipboard permissions when prompted
- Use `Ctrl+C` / `Ctrl+V` rather than right-click menu

## Known Limitations

- **File system access**: No direct file system access; use import/export
- **Cloud storage**: Not yet available in the web version
- **Large files**: Performance may degrade with files larger than 1 MB
- **Concurrent editing**: Single-user only, no collaborative editing
- **Printing**: Use the browser's native print function (`Ctrl+P`) from preview mode
- **MockK unavailable**: Some test infrastructure is not available on Wasm target
