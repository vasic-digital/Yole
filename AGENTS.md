# Yole - Development Guide for AI Agents

> **Precedence:** `CONSTITUTION.md` is the authoritative rule set. When a rule here conflicts, the Constitution wins.

## MANDATORY Rules

1. **No CI/CD Pipelines** - No `.github/workflows/`, `.gitlab-ci.yml`, Jenkinsfile, etc.
2. **Never Remove/Disable Tests** - Fix root causes, don't skip tests
3. **Conventional Commits** for every commit (Constitution mandatory standard #4)
4. **SSH-only for git** (`git@…` remotes); HTTPS prohibited (Constitution mandatory standard #5)
5. **Release builds in containers** - `docker compose run --rm build ./docker/scripts/build.sh`
6. **Bootstrap fresh clone** - `make bootstrap` (installs anti-bluff pre-commit hook + submodules)
7. **Maintain Continuation Document** - `docs/CONTINUATION.md` MUST be kept in sync with current work at all times per CONST-036. After every task completion, file creation, or defect discovery, update the Continuation document so any CLI agent or LLM model can resume exactly where work left off.

## Quick Resume

To continue work from any session:
```
Read docs/CONTINUATION.md — it contains all active tasks, known defects,
uncommitted files, and the exact prompt to resume implementation.
```

## Build Commands

```bash
# Primary dev test (no Android SDK needed)
./gradlew :shared:desktopTest
make test-shared

# Single test class
./gradlew :shared:desktopTest --tests "digital.vasic.yole.format.todotxt.TodoTxtQuerySyntaxTests"

# Single test method
./gradlew :shared:desktopTest --tests "digital.vasic.yole.format.markdown.MarkdownParserTests.ParseHeaders"

# All tests (requires Android SDK)
./gradlew test
./gradlew test koverHtmlReport  # With coverage

# Platform builds
./gradlew :androidApp:assembleDebug   # Android
./gradlew :desktopApp:run             # Desktop
./gradlew :webApp:wasmJsBrowserRun    # Web

# Lint & Static Analysis
./gradlew detekt
./gradlew lintFlavorDefaultDebug

# Makefile shortcuts
make test-shared    # :shared:desktopTest
make desktop        # :desktopApp:run
make detekt         # Static analysis

# Submodule tests (requires Go 1.24+)
make challenge      # Challenges Go tests
make helixqa-test   # HelixQA Go tests
make qa-all         # Full QA pipeline
```

## Testing Constraints

- **JUnit4 runner**: Use `runBlocking<Unit> { }` (not `runTest`) - JUnit4 requires `Unit` return
- **MockK is JVM-only**: Available in `desktopTest` and `androidUnitTest`, NOT in `commonTest`
- **kotlinx-coroutines-test**: No WASM variant, unavailable in `commonTest`
- **jvmTarget**: Must be `"11"` in all JVM compilations

## Architecture

```
shared/src/commonMain/kotlin/digital/vasic/yole/
├── format/          # 18 parsers + FormatRegistry + DocumentCache + StyleSheets
├── network/         # 8 protocols (Dropbox, GDrive, OneDrive, WebDAV, FTP, SFTP, SMB, Git)
├── model/           # Document representation
├── ui/              # Shared Compose components
└── util/            # Facade bridges (LazyLoading, RateLimiting, PlatformSync)
```

**Package namespace:** `digital.vasic.yole.*` (legacy: `net.gsantner.opoc.*`)

### Key Patterns

- **FormatRegistry**: `formats` is lazy-loaded via `lazy { createFormats() }`, check `isFormatsInitialized`
- **StyleSheets**: Uses `styleSheetCache` with `clearCache()`
- **Protocol services**: CircuitBreaker, ConnectionLimiter, Mutex lock ordering, `normalizePath()` for path traversal protection
- **Concurrency lock ordering**: `scopeMutex` > `stateMutex` > `operationsMutex` > `syncMutex` > `cacheMutex` > `pauseFlagsMutex` > `activeJobsMutex` > `storageInitMutex`

## Git Submodules

| Submodule | Purpose |
|-----------|---------|
| `Challenges/` | Go testing framework: 17 packages, UI automation banks, userflow runner, FFmpeg recorder |
| `Containers/` | Container orchestration: 20 packages, remote distribution, boot manager, ctop monitoring |
| `HelixQA/` | QA orchestration: crash detection, evidence collection, LLM-powered autonomous testing |

```bash
git submodule update --init --recursive

# Build and test submodules
cd Challenges && go build ./... && go test ./... -race -count=1
cd Containers && go build ./... && go test ./... -race -count=1
cd HelixQA && go build ./... && go test ./... -race -count=1

# Run autonomous QA session
cd HelixQA && go run ./cmd/helixqa autonomous --project .. --platforms android,desktop,web
```

## Code Style Guidelines

### File Headers (Required)
```kotlin
/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Brief description
 *########################################################*/
package digital.vasic.yole.format
```

### Language Standards
- **Kotlin** primary; Java only for legacy code
- **Java 11+** compatibility required
- Use `expect/actual` pattern for platform-specific code

### Naming Conventions
| Type | Convention | Example |
|------|------------|---------|
| Classes | PascalCase | `TextFormat`, `MarkdownParser` |
| Functions | camelCase | `parseContent()`, `detectFormat()` |
| Variables | camelCase | `documentContent`, `formatRegistry` |
| Constants | UPPER_SNAKE_CASE | `ID_MARKDOWN`, `EXTENSION_MD` |
| Test classes | End with `Tests` or `Test` | `MarkdownParserTests` |

### Import Ordering
1. Kotlin standard library (`kotlin.*`, `kotlinx.*`)
2. Third-party libraries (`androidx.*`, `com.*`, `org.*`)
3. Project imports (`digital.vasic.yole.*`)

### KDoc Requirements
- Required for all public APIs
- Include `@property` for constructor parameters
- Include `@param`, `@return`, `@throws` where applicable
- Include `@example` code blocks for complex APIs

### Error Handling
- Use specific exception types (not generic `Exception`)
- Provide meaningful error messages
- Fail fast with validation at boundaries
- Always rethrow `CancellationException` in catch blocks for coroutines

### Concurrency
- Use `Mutex.withLock` for protecting mutable state
- Use `@Volatile` for lazy caches accessed from multiple threads
- Use `platformSynchronized(lock)` for simple atomic operations
- Never use `GlobalScope` (enforced by Detekt)

## Detekt Rules (config/detekt/detekt.yml)

- Max line length: 150 characters
- Max method length: 100 lines
- Max parameters: 8 (function), 10 (constructor)
- Cyclomatic complexity: 30
- No global coroutines
- No swallowed exceptions

## Adding New Formats

1. Create parser in `shared/src/commonMain/kotlin/digital/vasic/yole/format/[name]/`
2. Implement `TextParser` interface
3. Add `TextFormat` to `FormatRegistry.createFormats()` (order matters - specific before general)
4. Add format ID constant to `TextFormat.Companion`
5. Register parser via `ParserRegistry.registerLazy("formatId") { Parser() }`
6. Add tests in `shared/src/commonTest/kotlin/digital/vasic/yole/format/[name]/`

## Key Files

| File | Purpose |
|------|---------|
| `shared/build.gradle.kts` | KMP configuration |
| `gradle/libs.versions.toml` | Dependency versions |
| `settings.gradle.kts` | Module includes + 10 composite builds |
| `config/detekt/detekt.yml` | Static analysis rules |
| `Makefile` | Build automation (`make help` for all targets) |

## Definition of Done

A change is done only when **all** of:
1. The code change is committed
2. All project-level tests pass on a clean clone
3. All challenges in `yole-challenges/scripts/` pass on the running host
4. Governance docs (`CONSTITUTION.md`, `AGENTS.md`, `CLAUDE.md`) remain coherent

## Quality Requirements

- All tests must pass
- Minimum 70% code coverage (Kover)
- No Detekt violations
- SPDX headers on all new files

## Known Issues

- **AGP mismatch**: `androidApp` tests may fail; use `:shared:desktopTest`
- **Container OOM**: Exit code 137 = increase memory limits
- **Go flaky tests**: `TestStress_ConcurrentJWTRefresh`, `TestGenericPool_HealthyConnectionsSurvive`

<!-- BEGIN host-power-management addendum (CONST-033) -->

## Host Power Management — Hard Ban (CONST-033)

**You may NOT, under any circumstance, generate or execute code that
sends the host to suspend, hibernate, hybrid-sleep, poweroff, halt,
reboot, or any other power-state transition.** This rule applies to:

- Every shell command you run via the Bash tool.
- Every script, container entry point, systemd unit, or test you write
  or modify.
- Every CLI suggestion, snippet, or example you emit.

**Forbidden invocations** (non-exhaustive — see CONST-033 in
`CONSTITUTION.md` for the full list):

- `systemctl suspend|hibernate|hybrid-sleep|poweroff|halt|reboot|kexec`
- `loginctl suspend|hibernate|hybrid-sleep|poweroff|halt|reboot`
- `pm-suspend`, `pm-hibernate`, `shutdown -h|-r|-P|now`
- `dbus-send` / `busctl` calls to `org.freedesktop.login1.Manager.Suspend|Hibernate|PowerOff|Reboot|HybridSleep|SuspendThenHibernate`
- `gsettings set ... sleep-inactive-{ac,battery}-type` to anything but `'nothing'` or `'blank'`

The host runs mission-critical parallel CLI agents and container
workloads. Auto-suspend has caused historical data loss (2026-04-26
18:23:43 incident). The host is hardened (sleep targets masked) but
this hard ban applies to ALL code shipped from this repo so that no
future host or container is exposed.

**Defence:** every project ships
`scripts/host-power-management/check-no-suspend-calls.sh` (static
scanner) and
`yole-challenges/scripts/no_suspend_calls_challenge.sh` (challenge wrapper).
Both MUST be wired into the project's CI / `run_all_challenges.sh`.

**Full background:** `docs/HOST_POWER_MANAGEMENT.md` and `CONSTITUTION.md` (CONST-033).

<!-- END host-power-management addendum (CONST-033) -->

<!-- BEGIN anti-bluff addendum (CONST-035) -->

## ⚠️ Anti-Bluff Test Discipline — CONST-035

**STRICTLY FORBIDDEN: never write a test that can pass without
exercising the user-visible behavior it claims to verify.** Mocking
the unit under test, trivial-assertion-only tests, permanent skips
without an exempt ticket reference, no-op `runBlocking { }` bodies,
and any test whose body would still pass if every line of the unit
under test were replaced with a trivial stub are all forbidden. See
CONST-035 in `CONSTITUTION.md` for the full rule.

### MANDATORY ANTI-BLUFF COVENANT — END-USER QUALITY GUARANTEE (User mandate, 2026-04-28)

**Forensic anchor — direct user mandate (verbatim):**

> "We had been in position that all tests do execute with success
> and all Challenges as well, but in reality the most of the
> features does not work and can't be used! This MUST NOT be the
> case and execution of tests and Challenges MUST guarantee the
> quality, the completion and full usability by end users of the
> product!"

**Operative rule:** the bar for shipping is **not** "tests pass"
but **"users can use the feature."** Every PASS MUST carry positive
evidence captured during execution that the feature works for the
end user. Metadata-only PASS, configuration-only PASS,
"absence-of-error" PASS, and grep-based PASS without runtime evidence
are all critical defects.

**Tests AND Challenges are bound equally** — a Challenge that scores
PASS on a non-functional feature is the same class of defect as a
unit test that does.

**Verification commands** (run before claiming a fix is complete):

```bash
bash scripts/anti-bluff/bluff-scanner.sh --mode all
bash yole-challenges/scripts/anchor_manifest_challenge.sh
bash yole-challenges/scripts/mutation_ratchet_challenge.sh
```

All three must PASS. Pre-existing bluff hits are tracked in
`yole-challenges/baselines/bluff-baseline.txt`; do not extend the baseline
without an explicit justification comment.

**Skip-marker convention:** `// SKIP-OK: #<ticket>` (canonical),
`// ANTI-BLUFF-EXEMPT: <reason>` (synonym).

<!-- END anti-bluff addendum (CONST-035) -->

