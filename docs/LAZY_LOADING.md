# Lazy Loading Patterns

Yole uses lazy loading throughout the codebase to reduce startup time, lower memory consumption, and defer expensive operations until they are actually needed. This document describes each lazy loading pattern, its implementation, and its performance impact.

## 1. Format Parser Lazy Registration

**Location:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/ParserInitializer.kt`

### Problem

Yole supports 17 text format parsers. Eagerly instantiating all 17 at startup costs 30-50ms and allocates memory for parsers that may never be used (e.g., the Jupyter parser is unnecessary if the user only edits Markdown files).

### Solution

`ParserInitializer.registerAllParsersLazy()` registers factory lambdas instead of parser instances. Parsers are instantiated on first access via `ParserRegistry.getParser()`.

```kotlin
// Eager: all 17 parsers instantiated at startup (~30-50ms)
ParserInitializer.registerAllParsers()

// Lazy: only factory lambdas registered at startup (~1-2ms)
ParserInitializer.registerAllParsersLazy()
```

### How It Works

1. `registerAllParsersLazy()` stores `() -> TextParser` factory lambdas in `ParserRegistry.parserFactories`
2. `ParserRegistry.getParser(format)` checks `parsers` map first (already instantiated)
3. If not found, checks `parserFactories` for a matching factory
4. If found, calls the factory, stores the result in `parsers`, removes the factory
5. Subsequent calls return the cached instance

### Performance Impact

| Metric | Eager | Lazy | Improvement |
|--------|-------|------|-------------|
| Startup registration time | 30-50ms | 1-2ms | 95% faster |
| Initial memory allocation | All 17 parsers | 0 parsers | Deferred |
| First-use latency | 0ms | 1-3ms per parser | Imperceptible |

### Monitoring

```kotlin
// How many parsers are still waiting to be instantiated
val pending = ParserRegistry.getPendingParserCount()

// How many have been instantiated so far
val instantiated = ParserRegistry.getInstantiatedParserCount()
```

### Thread Safety

`ParserRegistry` uses `platformSynchronized(lock)` to protect both `parsers` and `parserFactories` maps. The `platformSynchronized` function delegates to the appropriate synchronization mechanism per platform:
- JVM/Android: `synchronized(lock) { }`
- Native/Wasm: single-threaded, no locking needed

---

## 2. HttpClient Lazy Initialization

**Location:** Protocol service classes (e.g., `DropboxService.kt`, `GoogleDriveService.kt`)

### Problem

Ktor `HttpClient` creation involves engine selection, plugin installation (content negotiation, logging, auth), and TLS configuration. This is expensive and unnecessary if the service is constructed but never connected.

### Solution

Each protocol service uses Kotlin's `by lazy { }` delegate for the `HttpClient`:

```kotlin
class DropboxService(...) : NetworkStorageService {
    private var _httpClientAccessed = false
    private val httpClient by lazy {
        _httpClientAccessed = true
        _injectedHttpClient ?: createHttpClient()
    }
}
```

The `_httpClientAccessed` flag tracks whether the lazy has been triggered, enabling safe cleanup in `disconnect()`:

```kotlin
override suspend fun disconnect(): Result<Unit> {
    if (_httpClientAccessed) {
        httpClient.close()
    }
    // ...
}
```

### Why `_httpClientAccessed` Is Needed

Calling `httpClient` to check if it needs closing would trigger lazy initialization, creating the client just to close it. The boolean flag avoids this.

### Thread Safety

Kotlin's `by lazy { }` uses `LazyThreadSafetyMode.SYNCHRONIZED` by default, guaranteeing the initializer runs at most once even under concurrent access.

---

## 3. OAuth2Flow Lazy Initialization

**Location:** Protocol service classes

### Problem

OAuth2 flow objects depend on the HttpClient and configuration. They should not be created until authentication is actually needed.

### Solution

```kotlin
class DropboxService(...) : NetworkStorageService {
    private val oauth2Flow by lazy {
        DropboxOAuth2Flow(
            httpClient = httpClient,
            clientId = config.appKey,
            clientSecret = config.appSecret,
            redirectUri = "http://localhost:8080/callback"
        )
    }
}
```

This chains with the HttpClient lazy initialization -- accessing `oauth2Flow` triggers `httpClient` creation if it has not happened yet.

---

## 4. ParsedDocument Lazy HTML Caching

**Location:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/TextParser.kt`

### Problem

HTML generation from parsed content is expensive (5-10KB output, string concatenation, CSS injection). Many code paths parse documents without ever displaying them (metadata extraction, format detection, validation, batch processing).

### Solution

`ParsedDocument.toHtml()` generates HTML on first call and caches it:

```kotlin
class ParsedDocument(...) {
    private var _cachedHtmlLight: String? = null
    private var _cachedHtmlDark: String? = null

    fun toHtml(lightMode: Boolean = true): String {
        if (lightMode) {
            return _cachedHtmlLight ?: run {
                val parser = ParserRegistry.getParser(format)
                    ?: throw IllegalStateException("No parser found for format: ${format.id}")
                parser.toHtml(this, lightMode).also { _cachedHtmlLight = it }
            }
        } else {
            return _cachedHtmlDark ?: run {
                val parser = ParserRegistry.getParser(format)
                    ?: throw IllegalStateException("No parser found for format: ${format.id}")
                parser.toHtml(this, lightMode).also { _cachedHtmlDark = it }
            }
        }
    }

    fun clearHtmlCache() {
        _cachedHtmlLight = null
        _cachedHtmlDark = null
    }

    fun hasHtmlCached(lightMode: Boolean = true): Boolean {
        return if (lightMode) _cachedHtmlLight != null else _cachedHtmlDark != null
    }
}
```

### Key Design Decisions

1. **Separate light/dark caches** -- Prevents regeneration when toggling themes
2. **Nullable backing fields** -- Zero allocation until first `toHtml()` call
3. **`clearHtmlCache()`** -- Allows explicit memory reclamation in memory-constrained scenarios
4. **`hasHtmlCached()`** -- Monitoring hook for cache efficiency analysis

### Memory Savings

| Scenario | Without caching | With caching |
|----------|----------------|--------------|
| Parse only (no display) | HTML generated, wasted | No HTML generated (0 allocation) |
| Display once | HTML generated once | HTML generated once |
| Display 10 times | HTML generated 10 times | HTML generated once, returned 9 times from cache |
| Light + dark toggle | 2 generations per toggle | 2 generations total (both cached) |

---

## 5. Concurrency-KMP Lazy Loaders

**Location:** `shared/src/commonMain/kotlin/digital/vasic/yole/util/LazyLoading.kt` (facade to Concurrency-KMP module)

### LazyDocumentLoader\<T\>

Defers document loading until the `value` property is first accessed:

```kotlin
val loader = LazyDocumentLoader<ParsedDocument> {
    parser.parse(readFileContent(path))
}

// Later, when needed:
val document = loader.value  // Triggers loading on first access
```

### LazyStringLoader

Specialized loader for string content, optimized for file reading scenarios:

```kotlin
val content = LazyStringLoader {
    readFileContent("/path/to/large/file.md")
}

// String is not read until .value is accessed
println(content.value)
```

### FlowLazyLoader\<T\>

Flow-based lazy loading with reactive updates. Emits values as they become available:

```kotlin
val loader = FlowLazyLoader<ParsedDocument> {
    flow {
        emit(parser.parse(content))
    }
}

// Collect results reactively
loader.flow.collect { document ->
    updateUI(document)
}
```

---

## 6. PlatformFileIO Lazy Initialization

**Location:** Protocol service classes

### Problem

`PlatformFileIOFactory.create()` returns a platform-specific file I/O implementation. Not all protocol operations need file I/O (e.g., listing files, getting metadata).

### Solution

```kotlin
class DropboxService(...) : NetworkStorageService {
    private val fileIO by lazy { PlatformFileIOFactory.create() }
}
```

The `fileIO` object is only created when a file download or upload operation first needs to read from or write to the local filesystem.

---

## Summary of Lazy Loading Points

| Component | Trigger | Cost Deferred |
|-----------|---------|---------------|
| Format parsers | First `getParser()` call for that format | Parser instantiation (1-3ms each) |
| HttpClient | First network operation | Engine creation, TLS setup |
| OAuth2Flow | First authentication attempt | Flow object creation |
| ParsedDocument HTML | First `toHtml()` call | HTML generation (5-10KB allocation) |
| LazyDocumentLoader | First `.value` access | File I/O + parsing |
| LazyStringLoader | First `.value` access | File I/O |
| FlowLazyLoader | First `.flow.collect()` | Async computation |
| PlatformFileIO | First upload/download | Platform factory |

## 7. FormatRegistry Lazy Format List

**Location:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt`

### Problem

Constructing all 17 `TextFormat` entries at class load time costs 30-50ms and happens before any format detection is actually needed.

### Solution

```kotlin
object FormatRegistry {
    private val formats: List<TextFormat> by lazy { createFormats() }

    val isFormatsInitialized: Boolean
        get() = /* check lazy state */

    private fun createFormats(): List<TextFormat> {
        // Constructs all 17 TextFormat entries with detection patterns
    }
}
```

### Key Design Decision

The `isFormatsInitialized` guard allows callers to check whether the lazy list has been triggered without accidentally triggering it. This is used in tests and monitoring code.

### Thread Safety

Kotlin's `by lazy { }` defaults to `LazyThreadSafetyMode.SYNCHRONIZED`, so the format list is constructed at most once even under concurrent first-access from multiple coroutines.

---

## 8. StyleSheets Cache

**Location:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/StyleSheets.kt`

### Problem

Generating CSS for format-specific styling is moderately expensive. The same CSS is needed every time a document is rendered in the same theme mode.

### Solution

```kotlin
object StyleSheets {
    private val styleSheetCache = mutableMapOf<String, String>()

    fun getStyleSheet(formatId: String, lightMode: Boolean): String {
        val key = "$formatId:$lightMode"
        return styleSheetCache.getOrPut(key) {
            generateStyleSheet(formatId, lightMode)
        }
    }

    fun clearCache() {
        styleSheetCache.clear()
    }
}
```

### When to Clear

Call `clearCache()` when the user changes themes or when custom CSS settings are modified. This forces regeneration of all style sheets on next access.

---

## Summary of Lazy Loading Points

| Component | Trigger | Cost Deferred |
|-----------|---------|---------------|
| Format parsers | First `getParser()` call for that format | Parser instantiation (1-3ms each) |
| HttpClient | First network operation | Engine creation, TLS setup |
| OAuth2Flow | First authentication attempt | Flow object creation |
| ParsedDocument HTML | First `toHtml()` call | HTML generation (5-10KB allocation) |
| LazyDocumentLoader | First `.value` access | File I/O + parsing |
| LazyStringLoader | First `.value` access | File I/O |
| FlowLazyLoader | First `.flow.collect()` | Async computation |
| PlatformFileIO | First upload/download | Platform factory |
| FormatRegistry formats | First format detection | 17 TextFormat entries |
| StyleSheets cache | First CSS request per format/theme | CSS generation |

## Testing

Lazy loading behavior is verified by:

- `MonitoringMetricsTests.kt` -- 42 tests including lazy loading timing and cache efficiency
- `ComprehensiveStressTests.kt` -- 30 tests for concurrent access to lazily-loaded resources
- `ResilienceTests.kt` -- 53 tests including DocumentCache behavior
- Format parser tests -- verify lazy registration produces identical results to eager registration
- `ConcurrentFormatParsingStressTest.kt` -- verifies `FormatRegistry.formats` lazy init under 100+ concurrent coroutines
