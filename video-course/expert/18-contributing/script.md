# Module 18: Contributing to Yole (7 videos)

## Video 18.1: Repository Structure (18 min)

### Timestamps
- 0:00 The Yole ecosystem: 1 main repo + 10 KMP modules + 2 Go submodules
- 2:00 Main repository structure: shared/, androidApp/, desktopApp/, iosApp/, webApp/
- 4:00 The shared module: 68 source files, 4,750+ tests, 17 formats, 8 protocols
- 6:00 Extracted KMP modules: why they were extracted and how they connect

### The 10 Extracted KMP Modules

| Module | Package | Purpose |
|--------|---------|---------|
| `RateLimiter-KMP` | `digital.vasic.ratelimiter` | Token bucket, adaptive rate limiting |
| `Concurrency-KMP` | `digital.vasic.concurrency` | Lazy loading, platform sync |
| `UI-Components-KMP` | `digital.vasic.uicomponents` | Theme, animations, accessibility |
| `Auth-KMP` | `digital.vasic.auth` | OAuth2 flows, token management |
| `Security-KMP` | `digital.vasic.security` | Secure storage |
| `Document-KMP` | `digital.vasic.document` | Document model |
| `Config-KMP` | `digital.vasic.config` | Network configuration |
| `Database-KMP` | `digital.vasic.database` | Metadata storage |
| `Storage-KMP` | `digital.vasic.storage` | Protocol abstractions + implementations |
| `Formatters-KMP` | `digital.vasic.formatters` | 17 text format parsers |

- 8:00 Go submodules: `Challenges/` (testing framework) and `Containers/` (container orchestration)
- 10:00 Composite builds: how `includeBuild()` in `settings.gradle.kts` wires modules
- 12:00 Facade bridges: typealiases that re-export extracted module types under `digital.vasic.yole.*`
- 14:00 Which files are facades vs. original code (and why)
- 16:00 The `CLAUDE.md` file: project rules and instructions
- 17:30 Summary

### Code References
- `settings.gradle.kts` -- Module includes + 10 composite build `includeBuild()` directives
- `shared/build.gradle.kts` -- KMP configuration with all platform targets
- `shared/src/commonMain/kotlin/digital/vasic/yole/util/RateLimiting.kt` -- Example facade bridge
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/auth/OAuth2Flow.kt` -- Example facade bridge
- `CLAUDE.md` -- Project rules and architecture documentation

### Key Code Walkthrough

Composite builds wire extracted modules into Yole:

```kotlin
// settings.gradle.kts
includeBuild("../RateLimiter-KMP")
includeBuild("../Concurrency-KMP")
includeBuild("../Auth-KMP")
// ... 7 more modules
```

Facade bridges maintain backward compatibility:

```kotlin
// shared/src/commonMain/kotlin/digital/vasic/yole/util/RateLimiting.kt
package digital.vasic.yole.util

typealias RateLimiter = digital.vasic.ratelimiter.RateLimiter
typealias TokenBucket = digital.vasic.ratelimiter.TokenBucket
typealias AdaptiveRateLimiter = digital.vasic.ratelimiter.AdaptiveRateLimiter
typealias OperationThrottler = digital.vasic.ratelimiter.OperationThrottler
```

### Exercises
1. **Map the ecosystem** -- Draw a dependency graph showing how the main Yole repo depends on each of the 10 KMP modules and 2 Go submodules.
2. **Trace a facade** -- Pick any facade bridge file and trace how a type flows from the extracted module through the typealias into a consumer in Yole's source.

---

## Video 18.2: Git Workflow and Branch Strategy (15 min)

### Timestamps
- 0:00 Branch strategy: `master` as the main branch
- 2:00 Feature branches: naming conventions, short-lived branches
- 4:00 Commit message conventions: conventional commits (`feat:`, `fix:`, `test:`, `docs:`)
- 6:00 Submodule management: `git submodule update --init --recursive`
- 8:00 Working with composite builds: editing an extracted module alongside Yole
- 10:00 Pull request workflow: create, review, CI checks, merge
- 12:00 Branch protection: required checks, review approvals
- 13:30 Summary

### Key Commands

```bash
# Clone with submodules
git clone --recurse-submodules https://github.com/vasic-digital/Yole.git

# Update submodules
git submodule update --init --recursive

# Create a feature branch
git checkout -b feat/add-yaml-parser

# Commit with conventional commit message
git commit -m "feat(format): add YAML parser with key-value and list support"

# Push and create PR
git push origin feat/add-yaml-parser
```

### Exercises
1. **Branch workflow** -- Create a feature branch, make a small change, commit with proper conventional commit format, and push.
2. **Submodule update** -- Update the Challenges submodule to the latest commit and verify it still builds.

---

## Video 18.3: Development Environment Setup (15 min)

### Timestamps
- 0:00 Required tools: JDK 11+, Android Studio or IntelliJ IDEA, Git, Docker/Podman
- 2:00 Cloning with submodules: `git clone --recurse-submodules`
- 4:00 IDE setup: IntelliJ KMP plugin, Kotlin plugin, Compose plugin
- 6:00 Container setup: `docker compose build build`
- 8:00 First build: `docker compose run --rm build ./gradlew :shared:compileKotlinDesktop`
- 10:00 First test: `docker compose run --rm build ./gradlew :shared:desktopTest`
- 12:00 Go tools for Challenges: Go 1.24+, `go build ./...`, `go test ./...`
- 13:30 Summary

### Key Commands

```bash
# Full setup from scratch
git clone --recurse-submodules https://github.com/vasic-digital/Yole.git
cd Yole

# Build container
docker compose build build

# Verify shared module compiles
docker compose run --rm build ./gradlew :shared:compileKotlinDesktop

# Run tests
docker compose run --rm build ./gradlew :shared:desktopTest

# Verify Challenges builds
cd Challenges && go build ./... && go test ./... -race -count=1
cd ../Containers && go build ./... && go test ./... -race -count=1
```

### Exercises
1. **Full setup** -- Follow the steps above on a clean machine and document any issues encountered.
2. **IDE verification** -- Open the project in IntelliJ IDEA and verify that code navigation works across the shared module and all platform source sets.

---

## Video 18.4: Running the Full Test Suite (18 min)

### Timestamps
- 0:00 The mandatory rule: all builds and tests MUST run in containers
- 2:00 The "never remove or disable tests" rule
- 4:00 Test statistics: 4,750+ tests across 148+ test files
- 6:00 Test organization: commonTest (shared), desktopTest, androidUnitTest, wasmJsTest
- 8:00 Running all tests: `docker compose run --rm build ./docker/scripts/test-all.sh`
- 10:00 Running specific tests: `--tests` flag with class or method patterns
- 12:00 Coverage report: `./gradlew test koverHtmlReport`
- 14:00 Understanding test failures: fixing root causes, not symptoms
- 16:00 Known issues: AGP version mismatch, OOM kills (exit code 137)
- 17:30 Summary

### The "Never Disable Tests" Rule (from CLAUDE.md)

```
NO test may ever be removed, disabled, skipped, or left broken!

All issues must be fixed by addressing the root causes:
- Fix the source code to match tests if tests are correct
- Fix the tests to match source code if source is correct
- Add missing classes/methods to make code compile
- Add missing imports to tests
- Fix syntax errors
- Fix parameter name mismatches
```

### Key Commands

```bash
# Full test suite in container
docker compose run --rm build ./docker/scripts/test-all.sh

# Desktop tests (avoids AGP mismatch)
docker compose run --rm build ./gradlew :shared:desktopTest

# Single test class
docker compose run --rm build ./gradlew test \
  --tests "digital.vasic.yole.format.todotxt.TodoTxtQuerySyntaxTests"

# Single test method
docker compose run --rm build ./gradlew test \
  --tests "digital.vasic.yole.format.todotxt.TodoTxtQuerySyntaxTests.ParseQuery"

# With coverage
docker compose run --rm build ./gradlew test koverHtmlReport

# After OOM kill, clean Gradle locks
docker compose run --rm build bash -c \
  "find /root/.gradle/caches -name '*.lock' -delete"
```

### Exercises
1. **Run the full suite** -- Execute the full test suite and record the total count, pass rate, and execution time.
2. **Coverage analysis** -- Generate a Kover report and identify the top 3 files with lowest coverage.
3. **Fix a failure** -- Intentionally break a test by modifying source code, observe the failure, and fix it following the "never disable tests" rule.

---

## Video 18.5: Adding New Format Parsers (22 min)

### Timestamps
- 0:00 The format system: FormatRegistry, TextFormat, TextParser, ParsedDocument
- 2:00 Step 1: Create parser directory in `shared/src/commonMain/kotlin/digital/vasic/yole/format/[name]/`
- 4:00 Step 2: Implement the parser that produces `ParsedDocument`
- 6:00 Step 3: Define `TextFormat` metadata: id, name, extensions, detection patterns
- 8:00 Step 4: Add format ID constant to `TextFormat.Companion`
- 10:00 Step 5: Register in `FormatRegistry.formats` list (order matters for detection priority)
- 12:00 Step 6: Add tests in `shared/src/commonTest/kotlin/digital/vasic/yole/format/[name]/`
- 14:00 Step 7: Add platform-specific code in `androidMain/`, `desktopMain/` if needed
- 16:00 Test requirements: basic parsing, edge cases, error recovery, stress tests
- 18:00 HTML generation: `toHtml()` with lazy caching, CSS from `StyleSheets.kt`
- 20:00 Detection priority: more specific formats before general ones in the registry
- 21:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt` -- Central registry with detection priority
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/TextFormat.kt` -- Format metadata
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/TextParser.kt` -- ParsedDocument class
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/StyleSheets.kt` -- CSS generation

### Key Code Walkthrough

Adding a new YAML format parser:

```kotlin
// Step 1-2: Create parser
// shared/src/commonMain/kotlin/digital/vasic/yole/format/yaml/YamlParser.kt
class YamlParser {
    fun parse(content: String): ParsedDocument {
        val parsed = parseYamlContent(content)
        return ParsedDocument(
            rawContent = content,
            parsedContent = parsed,
            metadata = mapOf("keys" to countKeys(content)),
            errors = emptyList()
        )
    }
}

// Step 3-4: Define format metadata
val YAML_FORMAT = TextFormat(
    id = TextFormat.ID_YAML,
    name = "YAML",
    extensions = listOf("yml", "yaml"),
    detectionPatterns = listOf(Regex("^---\\s*$", RegexOption.MULTILINE))
)

// Step 5: Register (in FormatRegistry.formats, before plaintext)
```

### Exercises
1. **Implement a parser** -- Create a minimal YAML parser that handles key-value pairs and produces HTML output.
2. **Test coverage** -- Write at least 10 tests for the new parser covering: basic parsing, nested structures, empty input, malformed input, and large files.

---

## Video 18.6: Adding New Protocol Services (20 min)

### Timestamps
- 0:00 The `NetworkStorageService` interface: the contract every protocol must implement
- 2:00 Core methods: `connect()`, `disconnect()`, `listFiles()`, `downloadFile()`, `uploadFile()`
- 4:00 Supporting methods: `deleteFile()`, `searchFiles()`, `isOnline`
- 6:00 Step 1: Create service class implementing `NetworkStorageService`
- 8:00 Step 2: Implement authentication (OAuth2, basic auth, SSH keys, etc.)
- 10:00 Step 3: Implement file operations using Ktor HttpClient
- 12:00 Step 4: Handle `CancellationException` for coroutine flow transparency
- 14:00 Step 5: Add tests: unit tests, mock HTTP tests, integration tests
- 16:00 Step 6: Configure in `StorageConfig`
- 18:00 Patterns from existing services: error handling, rate limiting, cache tracking
- 19:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/NetworkStorageService.kt` -- Interface definition
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/dropbox/DropboxService.kt` -- Reference implementation
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/StorageConfig.kt` -- Configuration model

### Key Code Walkthrough

Every protocol service must rethrow `CancellationException`:

```kotlin
class NewProtocolService(override val config: StorageConfig) : NetworkStorageService {

    override suspend fun connect(): Result<Unit> = try {
        // Authenticate and validate connection
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e  // NEVER swallow cancellation
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun listFiles(path: String): Flow<Result<List<NetworkDocument>>> = flow {
        try {
            val response = httpClient.get("$baseUrl/files?path=$path")
            val documents = parseResponse(response)
            emit(Result.success(documents))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
```

### Exercises
1. **Implement a stub** -- Create a stub `S3Service` that implements `NetworkStorageService` with placeholder implementations.
2. **Mock HTTP tests** -- Using existing mock HTTP tests as a template, write tests for the stub service.

---

## Video 18.7: Code Review and Documentation Standards (15 min)

### Timestamps
- 0:00 Code review process: what reviewers look for
- 2:00 Correctness: does the code do what it claims?
- 4:00 Tests: are there sufficient tests? Do they test edge cases?
- 6:00 Performance: are there allocation hotspots or N+1 patterns?
- 8:00 Documentation: KDoc for public APIs, inline comments for complex logic
- 10:00 File headers: SPDX license header (Apache-2.0, CC0-1.0, or Unlicense)
- 12:00 Commit messages: conventional commits format
- 13:00 Generating API docs: `./gradlew :shared:dokkaHtml`
- 14:30 Summary

### Code Review Checklist

| Category | Check |
|----------|-------|
| Correctness | Logic matches specification |
| Tests | New code has tests, existing tests still pass |
| Edge cases | Empty input, null handling, boundary values |
| Performance | No unnecessary allocations, regex pre-compiled |
| Security | No hardcoded secrets, input validated, output sanitized |
| Documentation | Public APIs documented with KDoc |
| Style | Follows Kotlin conventions, consistent naming |
| License | SPDX header present on new files |

### SPDX License Header Format

```kotlin
/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Your Name
 * SPDX-License-Identifier: Apache-2.0
 *
 * Brief description of the file
 *
 *########################################################*/
```

### Exercises
1. **Review practice** -- Pick any recent commit from the Yole git log and perform a code review against the checklist above.
2. **Documentation audit** -- Run `./gradlew :shared:dokkaHtml` and identify 3 public APIs that lack KDoc documentation.
3. **License check** -- Verify that all files under `shared/src/commonMain/` have proper SPDX license headers.
