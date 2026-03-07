# API Changelog

This document tracks API changes across Yole's shared module and extracted KMP modules. It covers key API surfaces, breaking changes, and deprecation notices.

---

## v1.0.0 -- Initial KMP Extraction (March 2026)

### Overview

The shared module's business logic was extracted into 10 independent KMP modules, each with its own repository, CI/CD, and versioning. Facade bridges (typealiases) maintain backward compatibility for existing code.

### Key API Surfaces

#### FormatRegistry

Central registry for all 17 text format parsers with detection priority.

```kotlin
object FormatRegistry {
    val formats: List<TextFormat>                      // All registered formats
    fun getById(id: String): TextFormat?               // Lookup by format ID
    fun detectByExtension(filename: String): TextFormat? // Detect by file extension
    fun detectByContent(content: String): TextFormat?   // Detect by content patterns
}
```

**Format IDs:** `TextFormat.ID_MARKDOWN`, `TextFormat.ID_TODOTXT`, `TextFormat.ID_CSV`, `TextFormat.ID_LATEX`, `TextFormat.ID_ORGMODE`, `TextFormat.ID_PLAINTEXT`, `TextFormat.ID_WIKITEXT`, `TextFormat.ID_ASCIIDOC`, `TextFormat.ID_RESTRUCTUREDTEXT`, `TextFormat.ID_RMARKDOWN`, `TextFormat.ID_TASKPAPER`, `TextFormat.ID_TEXTILE`, `TextFormat.ID_CREOLE`, `TextFormat.ID_TIDDLYWIKI`, `TextFormat.ID_JUPYTER`, `TextFormat.ID_KEYVALUE`, `TextFormat.ID_BINARY`

#### TextParser

Interface for format-specific parsers:

```kotlin
interface TextParser {
    val supportedFormat: TextFormat
    fun canParse(format: TextFormat): Boolean
    fun parse(content: String, options: Map<String, Any> = emptyMap()): ParsedDocument
    fun toHtml(document: ParsedDocument, lightMode: Boolean = true): String
    fun validate(content: String): List<String>
}
```

#### ParsedDocument

Parsed document with lazy HTML caching:

```kotlin
class ParsedDocument(
    val format: TextFormat,
    val rawContent: String,
    val parsedContent: String,
    val metadata: Map<String, String> = emptyMap(),
    val errors: List<String> = emptyList()
) {
    fun toHtml(lightMode: Boolean = true): String
    fun clearHtmlCache()
    fun hasHtmlCached(lightMode: Boolean = true): Boolean
    fun copy(...): ParsedDocument
}
```

#### ParserRegistry

Registry with lazy instantiation support:

```kotlin
object ParserRegistry {
    fun register(parser: TextParser)
    fun registerLazy(formatId: String, factory: () -> TextParser)
    fun getParser(format: TextFormat): TextParser?
    fun getParser(formatId: String): TextParser?
    fun hasParser(format: TextFormat): Boolean
    fun getAllParsers(): List<TextParser>
    fun getPendingParserCount(): Int
    fun getInstantiatedParserCount(): Int
    fun clear()
}
```

#### NetworkStorageService

Unified interface for 8 cloud/network storage protocols:

```kotlin
interface NetworkStorageService {
    val config: StorageConfig
    val isOnline: Boolean
    val rootPath: String
    suspend fun connect(): Result<Unit>
    suspend fun disconnect(): Result<Unit>
    suspend fun testConnection(): Result<Boolean>
    fun listFiles(path: String = "/"): Flow<Result<List<NetworkDocument>>>
    suspend fun downloadFile(remotePath: String, localPath: String): Flow<NetworkOperation>
    suspend fun uploadFile(localPath: String, remotePath: String): Flow<NetworkOperation>
    suspend fun deleteFile(remotePath: String): Result<Unit>
    suspend fun createFolder(remotePath: String): Result<NetworkDocument>
    suspend fun renameFile(remotePath: String, newName: String): Result<Unit>
    suspend fun moveFile(sourcePath: String, destinationPath: String): Result<NetworkDocument>
    suspend fun copyFile(sourcePath: String, destinationPath: String): Result<Unit>
    suspend fun getFileInfo(remotePath: String): Result<NetworkDocument>
    fun searchFiles(query: String, path: String?, includeContent: Boolean): Flow<Result<List<NetworkDocument>>>
    suspend fun syncFile(remotePath: String, forceSync: Boolean): Flow<NetworkOperation>
    suspend fun syncAll(forceSync: Boolean): Flow<NetworkOperation>
    suspend fun exists(remotePath: String): Result<Boolean>
    suspend fun getQuotaInfo(): Result<StorageQuota>
}
```

#### Resilience Utilities

```kotlin
class CircuitBreaker(failureThreshold: Int = 5, resetTimeout: Duration = 30.seconds, name: String = "default") {
    val state: State          // CLOSED, OPEN, HALF_OPEN
    val failures: Int
    val successes: Int
    val calls: Long
    suspend fun <T> execute(block: suspend () -> T): Result<T>
    suspend fun reset()
}

class ConnectionLimiter(maxConcurrent: Int = 5, acquireTimeout: Duration = 30.seconds, name: String = "default") {
    val availablePermits: Int
    suspend fun <T> withConnection(block: suspend () -> T): T
}

class DocumentCache(maxSize: Int = 100) {
    val size: Int
    val hits: Long
    val misses: Long
    val hitRate: Double
    suspend fun get(key: String): ParsedDocument?
    suspend fun put(key: String, document: ParsedDocument)
    suspend fun invalidate(key: String)
    suspend fun clear()
    suspend fun contains(key: String): Boolean
}

class RateLimitedStorageService(delegate: NetworkStorageService, maxConcurrent: Int = 4) : NetworkStorageService
```

### Extracted Modules

| Module | Package | Key Types |
|--------|---------|-----------|
| RateLimiter-KMP | `digital.vasic.ratelimiter` | `RateLimiter`, `TokenBucket`, `AdaptiveRateLimiter`, `OperationThrottler` |
| Concurrency-KMP | `digital.vasic.concurrency` | `LazyDocumentLoader<T>`, `LazyStringLoader`, `FlowLazyLoader<T>`, `platformSynchronized()` |
| UI-Components-KMP | `digital.vasic.uicomponents` | Theme tokens, animation specs, accessibility utilities |
| Auth-KMP | `digital.vasic.auth` | `OAuth2Flow`, `TokenResponse`, `DropboxOAuth2Flow`, `GoogleDriveOAuth2Flow`, `OneDriveOAuth2Flow` |
| Security-KMP | `digital.vasic.security` | Secure storage abstractions |
| Document-KMP | `digital.vasic.document` | Document model |
| Config-KMP | `digital.vasic.config` | `StorageConfig` and subtypes |
| Database-KMP | `digital.vasic.database` | Metadata storage |
| Storage-KMP | `digital.vasic.storage` | Protocol abstractions |
| Formatters-KMP | `digital.vasic.formatters` | 17 text format parsers |

### Facade Bridges (Backward Compatibility)

The following typealias files re-export extracted module types under the original `digital.vasic.yole.*` package:

| Facade File | Types Re-exported |
|-------------|-------------------|
| `util/RateLimiting.kt` | `RateLimiter`, `TokenBucket`, `AdaptiveRateLimiter`, `OperationThrottler` |
| `util/LazyLoading.kt` | `LazyDocumentLoader<T>`, `LazyStringLoader`, `FlowLazyLoader<T>` |
| `util/PlatformSync.kt` | `platformSynchronized()` |
| `network/auth/OAuth2Flow.kt` | `OAuth2Flow`, `TokenResponse`, `DropboxOAuth2Flow`, `GoogleDriveOAuth2Flow`, `OneDriveOAuth2Flow` |

### Not Facaded (Kept in Yole)

Due to Kotlin typealias limitations with nested objects, sealed class pattern matching, and expect/actual declarations:

- `ui/Theme.kt`, `ui/Animations.kt`, `ui/Accessibility.kt`
- `network/common/StorageConfig.kt`
- `network/auth/AuthTokenManager.kt`
- All `network/platform/` expect/actual files

---

## Breaking Changes Log

### v1.0.0

| Change | Impact | Migration |
|--------|--------|-----------|
| `FormatRegistry` changed from `object` to `class` | Configurable instances instead of singleton | Use `FormatRegistry` (companion-delegated) or create instances |
| `ParsedDocument` changed from `data class` to `class` | Custom `equals`/`hashCode`/`copy` implementations | No code changes needed; API is identical |
| Lazy HTML caching in `ParsedDocument.toHtml()` | First call may be slower if parser is lazy-loaded | No code changes needed; behavior is transparent |
| `ParserRegistry.register()` throws on duplicate format ID | Previously allowed silent overwrite | Call `clear()` before re-registering in tests |

---

## Deprecation Notices

### v1.0.0

| Deprecated | Replacement | Removal Target |
|------------|-------------|----------------|
| `ParserInitializer.registerAllParsers()` | `ParserInitializer.registerAllParsersLazy()` | v2.0.0 |
| Legacy `net.gsantner.opoc.*` package references | `digital.vasic.yole.*` | v2.0.0 |
| `commons/` module | Extracted KMP modules | v2.0.0 |
| `core/` module | Extracted KMP modules | v2.0.0 |

---

## Versioning Policy

- **Major version** (X.0.0): Breaking API changes
- **Minor version** (0.X.0): New features, backward-compatible
- **Patch version** (0.0.X): Bug fixes, backward-compatible

All 10 extracted KMP modules are versioned independently. The main Yole project pins specific versions via composite builds in `settings.gradle.kts`.
