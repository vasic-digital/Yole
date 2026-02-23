# Module 4: Cross-Platform Note App (5 videos)

## Video 4.1: Multi-Format Architecture (15 min)
- Design FormatRegistry for dynamic format loading
- Implement TextFormat enum with metadata (name, extensions, MIME type)
- Lazy parser initialization for memory efficiency

## Video 4.2: Format Detection and Switching (12 min)
- Auto-detect format from file extension and content
- Format switching while preserving document content
- Handle format-specific metadata (frontmatter, headers)

## Video 4.3: Shared ViewModel (18 min)
- Create DocumentViewModel in commonMain
- State management with StateFlow
- Document persistence with expect/actual storage
- Undo/redo history

## Video 4.4: Platform-Specific Features (15 min)
- Android: Material 3 theming, share intent, widgets
- Desktop: System tray, drag-and-drop, native file dialogs
- iOS: Haptic feedback, NSUserDefaults persistence

## Video 4.5: Building and Distribution (12 min)
- Android: assembleRelease, signing config, Play Store
- Desktop: jpackage for Windows/macOS/Linux installers
- Web: Static export, Wasm optimization, hosting
