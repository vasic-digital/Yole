# Module 9: Advanced Architecture Patterns (12 videos)

## Video 9.1: Clean Architecture in KMP (30 min)

### Timestamps
- 0:00 Introduction to Clean Architecture for multiplatform projects
- 3:00 Layer separation: domain, data, presentation
- 6:00 Domain layer: entities, use cases, repository interfaces
- 9:00 Data layer: repository implementations, data sources
- 12:00 Presentation layer: ViewModels, state management
- 15:00 Dependency rule: inner layers know nothing about outer layers
- 18:00 Mapping between layers: domain models vs. data models vs. UI models
- 21:00 Use cases in Yole: ParseDocumentUseCase, SaveDocumentUseCase
- 24:00 Repository pattern in shared code
- 27:00 Testing each layer in isolation
- 29:00 Summary

### Code Example: Performance Optimization with Lazy Evaluation

```kotlin
// ParsedDocument uses lazy caching for HTML generation
// First call computes HTML, subsequent calls return cached result
class ParsedDocument(
    val rawContent: String,
    val parsedContent: String,
    val metadata: Map<String, Any> = emptyMap(),
    val errors: List<ParseError> = emptyList(),
) {
    private var cachedHtml: String? = null

    fun toHtml(): String {
        return cachedHtml ?: generateHtml().also { cachedHtml = it }
    }

    private fun generateHtml(): String {
        // Expensive HTML generation happens only once
        return "<html><body>$parsedContent</body></html>"
    }
}
```

---

## Video 9.2: Dependency Injection (25 min)

### Timestamps
- 0:00 DI in multiplatform: challenges and solutions
- 3:00 Manual DI with factory functions
- 6:00 Koin for KMP: module declarations
- 9:00 Platform-specific module bindings
- 12:00 Scoping: singleton vs. factory vs. scoped
- 15:00 Providing platform-specific implementations
- 18:00 Testing with mock dependencies
- 21:00 Service locator vs. constructor injection tradeoffs
- 24:00 Summary

### Code Example: Platform DI

```kotlin
// Shared module definition
val sharedModule = module {
    single { FormatRegistry() }
    factory { DocumentViewModel(get(), get()) }
    single<NetworkStorageService> { get<DropboxService>() }
}

// Platform-specific bindings
val androidModule = module {
    single<SecureStorage> { AndroidSecureStorage(androidContext()) }
}
```

---

## Video 9.3: State Management Patterns (28 min)

### Timestamps
- 0:00 State management overview for multiplatform apps
- 3:00 MVI (Model-View-Intent) pattern explained
- 6:00 Unidirectional data flow: Intent -> Model -> View
- 9:00 State reduction: pure functions for state transitions
- 12:00 Side effects: where to put I/O operations
- 15:00 StateFlow for reactive state in shared code
- 18:00 State persistence and restoration across config changes
- 21:00 Error state handling patterns
- 24:00 Testing state machines
- 27:00 Summary

### Code Example: MVI Pattern

```kotlin
// MVI state management
sealed class EditorIntent {
    data class LoadDocument(val path: String) : EditorIntent()
    data class UpdateContent(val text: String) : EditorIntent()
    object Save : EditorIntent()
}

data class EditorState(
    val content: String = "",
    val format: TextFormat? = null,
    val isDirty: Boolean = false,
    val error: String? = null,
)

// Reducer: pure function, easy to test
fun reduce(state: EditorState, intent: EditorIntent): EditorState = when (intent) {
    is EditorIntent.UpdateContent -> state.copy(content = intent.text, isDirty = true)
    is EditorIntent.Save -> state.copy(isDirty = false)
    is EditorIntent.LoadDocument -> state // handled by side effect
}
```

---

## Videos 9.4-9.12: Advanced Architecture

### Video 9.4: Event-Driven Architecture with SharedFlow (15 min)

#### Timestamps
- 0:00 Events vs. state in reactive systems
- 2:00 SharedFlow for one-time events (navigation, toasts, errors)
- 4:00 Channel vs. SharedFlow vs. StateFlow
- 6:00 Event bus patterns and when to use them
- 8:00 Testing event emissions
- 10:00 Common pitfalls: lost events, replay behavior
- 12:00 Best practices for event-driven UI
- 14:00 Summary

### Video 9.5: Error Handling Strategies Across Layers (15 min)

#### Timestamps
- 0:00 Error types: domain errors, infrastructure errors, UI errors
- 2:00 Result type vs. exceptions in Kotlin
- 4:00 Error propagation through layers
- 6:00 User-facing error messages
- 8:00 Retry strategies: automatic vs. user-initiated
- 10:00 Logging and error reporting
- 12:00 Network error handling in Yole
- 14:00 Summary

### Video 9.6: Feature Modules and Modularization (15 min)

#### Timestamps
- 0:00 Why modularize: build speed, team scaling, code isolation
- 2:00 Yole's module structure: shared + 10 extracted KMP modules + androidApp, desktopApp, webApp, iosApp
- 3:00 Composite builds: `includeBuild()` in `settings.gradle.kts` wires 10 KMP modules
- 4:00 Feature module boundaries
- 6:00 Inter-module communication
- 8:00 Dependency graph management
- 10:00 Build performance impact
- 12:00 Migration strategy for existing monolithic code
- 14:00 Summary

### Video 9.7: Plugin Architecture for Format Extensions (15 min)

#### Timestamps
- 0:00 Plugin architecture design goals
- 2:00 Plugin interface: lifecycle, capabilities
- 4:00 Plugin discovery and registration
- 6:00 Plugin sandboxing and security
- 8:00 Third-party format plugins
- 10:00 Plugin versioning and compatibility
- 12:00 Testing plugins in isolation
- 14:00 Summary

### Video 9.8: Database Design with SQLDelight (15 min)

#### Timestamps
- 0:00 SQLDelight for KMP: type-safe SQL
- 2:00 Schema definition and migrations
- 4:00 Query generation and type mapping
- 6:00 Platform drivers: Android, JVM, iOS, JS
- 8:00 Testing with in-memory databases
- 10:00 Document metadata storage design
- 12:00 Network cache database
- 14:00 Summary

### Video 9.9: Background Processing with Coroutines (15 min)

#### Timestamps
- 0:00 Structured concurrency in KMP
- 2:00 CoroutineScope management: viewModelScope, lifecycleScope
- 4:00 Dispatchers: Main, IO, Default across platforms
- 6:00 Cancellation and timeout handling
- 8:00 Flow-based reactive pipelines
- 10:00 Background file sync with coroutines
- 12:00 Testing coroutines with TestDispatcher
- 14:00 Summary

### Video 9.10: Memory Management and Leak Prevention (15 min)

#### Timestamps
- 0:00 Memory management across KMP targets
- 2:00 JVM garbage collection considerations
- 4:00 iOS/Native memory model
- 6:00 Wasm memory constraints
- 8:00 Common leak patterns: retained coroutine scopes, observer leaks, `serviceScope` lifecycle (see lock ordering: `scopeMutex` -> `stateMutex`)
- 10:00 Detecting leaks: LeakCanary (Android), Instruments (iOS)
- 12:00 Prevention patterns: weak references, scope management
- 14:00 Summary

### Video 9.11: API Design for Library Consumers (12 min)

#### Timestamps
- 0:00 Yole as a library: exposing the format system
- 2:00 Public API surface: what to expose, what to keep internal
- 4:00 API stability: @OptIn, @ExperimentalApi annotations
- 6:00 Binary compatibility and versioning
- 8:00 Documentation with KDoc and Dokka
- 10:00 Publishing KMP libraries to Maven Central
- 11:30 Summary

### Video 9.12: Architecture Decision Records (12 min)

#### Timestamps
- 0:00 What are ADRs and why document decisions
- 2:00 ADR template: context, decision, consequences
- 4:00 Yole's key architecture decisions documented
- 6:00 When to write a new ADR
- 8:00 Reviewing and superseding old decisions
- 10:00 ADR tooling and integration with code review
- 11:30 Summary
