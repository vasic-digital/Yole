# Module 4: Cross-Platform Note App (5 videos)

## Video 4.1: Multi-Format Architecture (15 min)

### Timestamps
- 0:00 Introduction to the format system
- 2:00 `FormatRegistry` design: centralized list of all formats
- 4:00 `TextFormat` data class: id, name, extensions, MIME type, detection patterns
- 6:00 Registration order: more specific formats before general ones
- 8:00 Lazy parser initialization for memory efficiency
- 10:00 Dynamic format loading at runtime
- 12:00 Adding new formats to the registry
- 14:00 Summary

### Code Example: Format Detection

```kotlin
// FormatRegistry detects format by file extension or content analysis
val format = FormatRegistry.detectByExtension("notes.md")
// Returns TextFormat with id = TextFormat.ID_MARKDOWN

val formatByContent = FormatRegistry.detectByContent("# Hello World\n\nSome text")
// Returns TextFormat with id = TextFormat.ID_MARKDOWN (matched heading pattern)
```

---

## Video 4.2: Format Detection and Switching (12 min)

### Timestamps
- 0:00 Auto-detection from file extension
- 2:00 Content-based detection using regex patterns
- 4:00 Detection priority: extension match first, then content analysis
- 6:00 Format switching while preserving document content
- 8:00 Handling format-specific metadata (frontmatter, headers)
- 10:00 Edge cases: ambiguous extensions (`.txt` can be Plain Text or Todo.txt)
- 11:30 Summary

### Code Example: Detection Priority

```kotlin
// Extension-based detection is tried first (fast, unambiguous)
val byExtension = FormatRegistry.detectByExtension("report.tex")
// Returns TextFormat.ID_LATEX

// Content-based detection kicks in for ambiguous cases
// e.g., .txt files are checked for todo.txt patterns like "(A) Task description"
val byContent = FormatRegistry.detectByContent("(A) 2025-01-01 Buy groceries +Shopping @store")
// Returns TextFormat.ID_TODOTXT (matches priority + project + context pattern)
```

---

## Video 4.3: Shared ViewModel (18 min)

### Timestamps
- 0:00 Why put the ViewModel in commonMain
- 2:00 DocumentViewModel design with StateFlow
- 4:00 State management: document content, format, dirty flag
- 6:00 StateFlow vs. SharedFlow for UI state
- 8:00 Document persistence with expect/actual storage
- 10:00 Undo/redo history implementation
- 12:00 File change detection and auto-save
- 14:00 Error state handling and recovery
- 16:00 Testing the ViewModel with fake dependencies
- 17:30 Summary

### Code Example: State Management

```kotlin
// StateFlow-based state management in shared code
class DocumentViewModel {
    private val _state = MutableStateFlow(DocumentState())
    val state: StateFlow<DocumentState> = _state.asStateFlow()

    fun loadDocument(path: String) {
        _state.update { it.copy(loading = true) }
        // Parse document, detect format, update state
    }
}
```

---

## Video 4.4: Platform-Specific Features (15 min)

### Timestamps
- 0:00 expect/actual pattern for platform abstraction
- 2:00 Android: Material 3 theming and share intent
- 4:30 Android: home screen widgets
- 6:00 Desktop: system tray integration
- 7:30 Desktop: drag-and-drop file opening
- 9:00 Desktop: native file dialogs
- 10:30 iOS: haptic feedback and NSUserDefaults
- 12:00 Web: IndexedDB for persistence, PWA manifest
- 14:00 Summary

### Code Example: expect/actual Pattern

```kotlin
// In commonMain: declare the interface
expect class PlatformStorage {
    fun save(key: String, value: String)
    fun load(key: String): String?
}

// In androidMain: implement with SharedPreferences
actual class PlatformStorage(private val context: Context) {
    actual fun save(key: String, value: String) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).apply()
    }
    actual fun load(key: String): String? {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key, null)
    }
}
```

---

## Video 4.5: Building and Distribution (12 min)

### Timestamps
- 0:00 Overview of build targets
- 1:30 Android: assembleRelease, signing config, Play Store metadata
- 3:30 Desktop: jpackage for Windows (.msi), macOS (.dmg), Linux (.deb)
- 5:30 Web: static export, Wasm binary size optimization
- 7:30 iOS: Xcode project setup, archive, and distribution
- 9:30 Version management across platforms
- 11:00 Summary

### Code Example: Gradle Build Commands

```bash
# Android release build
./gradlew :androidApp:assembleRelease

# Desktop installers via jpackage
./gradlew :desktopApp:packageMsi   # Windows
./gradlew :desktopApp:packageDmg   # macOS
./gradlew :desktopApp:packageDeb   # Linux

# Web production build
./gradlew :webApp:wasmJsBrowserProductionWebpack
```
