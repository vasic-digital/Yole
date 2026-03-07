# Comprehensive Decoupling Refactoring Design

**Date:** 2026-03-06
**Status:** Phase 5 Complete. 5.1: composite builds wired. 5.2: RateLimiter, Concurrency, Auth facades done (7 deferred — Kotlin typealias limitations). 5.3: CLAUDE.md, README updated. 5.4: Final test pass — 4,750 tests, 0 failures.
**Approach:** Incremental Extraction with Facade Bridge (bottom-up)

## Overview

Extract reusable modules from Yole's KMP codebase into independent projects. Each module becomes a git submodule with its own repo on GitHub and GitLab under the `vasic-digital` organization. Parallel Go implementations extend existing Go modules or create new ones where needed.

## Decisions

| Decision | Choice |
|----------|--------|
| KMP module naming | Suffix `-KMP` (e.g., `Auth-KMP`) |
| Extraction order | Bottom-up (zero-dependency first) |
| Protocol repos | Single `Storage-KMP` repo with all protocols as sub-packages |
| Documentation level | Full suite for every module |
| Existing Go repos | Clone as-is, add new functionality alongside, preserve existing code |
| Challenges | Each module gets its own Challenges submodule |
| Platforms for new repos | GitHub + GitLab under `vasic-digital` |
| GitFlic/GitVerse | Push only for repos that already have those upstreams |

## Module Inventory

### Phase 1 — Zero-Dependency Modules (Tier 1)

| Step | KMP Module | Go Module | Source | LOC |
|------|-----------|-----------|--------|-----|
| 1.1 | `RateLimiter-KMP` | `RateLimiter` (extend) | `util/RateLimiting.kt` | 304 |
| 1.2 | `Concurrency-KMP` | `Concurrency` (extend) | `util/LazyLoading.kt`, `util/PlatformSync.kt` | 253 |
| 1.3 | `UI-Components-KMP` | *(new KMP-only)* | `ui/Theme.kt`, `ui/Animations.kt`, `ui/Accessibility.kt` | 1,181 |
| 1.4 | `Auth-KMP` | `Auth` (extend) | `network/auth/*` | ~800 |
| 1.5 | `Security-KMP` | `Security` (extend) | `network/platform/SecureStorage.kt` + actuals | ~400 |

### Phase 2 — Single-Dependency Modules (Tier 2)

| Step | KMP Module | Go Module | Source | Depends On |
|------|-----------|-----------|--------|------------|
| 2.1 | `Document-KMP` | `Document` (new) | `model/Document.kt` | None (has expect/actual) |
| 2.2 | `Config-KMP` | `Config` (extend) | `network/config/*` | Auth-KMP |
| 2.3 | `Database-KMP` | `Database` (extend) | `network/database/*` | Minimal |

### Phase 3 — Core Abstractions (Tier 3)

| Step | KMP Module | Go Module | Source | Depends On |
|------|-----------|-----------|--------|------------|
| 3.1 | `Storage-KMP` | `Storage` (extend) | `network/common/*`, `network/protocol/*`, `network/platform/PlatformFileIO*`, all 8 protocol implementations, `NetworkStorageService.kt` | Auth-KMP, Security-KMP, Config-KMP, Database-KMP |

### Phase 4 — Format System (Tier 4)

| Step | KMP Module | Go Module | Source | Depends On |
|------|-----------|-----------|--------|------------|
| 4.1 | `Formatters-KMP` | `Formatters` (extend) | `format/*` — 17 parsers + registry + stylesheets | Document-KMP |

### Phase 5 — Integration & Cleanup

| Step | Activity |
|------|----------|
| 5.1 | Rewire Yole to depend on all extracted submodules |
| 5.2 | Remove facade bridges, delete migrated source from shared/ |
| 5.3 | Update Yole's CLAUDE.md, README, and documentation |
| 5.4 | Final test pass — all 4,438+ tests passing |

**Total: 10 KMP modules, 8 Go module extensions/creations, 5 phases.**

## Extraction Pipeline Per Module

Each module goes through 7 steps:

### Step 1: Create Repos
```
1a. Create GitHub repo: vasic-digital/ModuleName(-KMP)
1b. Create GitLab repo: vasic-digital/modulename(-kmp)
1c. Clone locally, set up standard structure
1d. Run install_upstreams
1e. Initial commit + push to all upstreams
```

### Step 2: Extract Code (KMP Module)
```
2a. Copy source files from Yole shared/ into new module
2b. Copy corresponding test files
2c. Copy expect/actual implementations (all 4 platforms)
2d. Set up build.gradle.kts with dependencies
2e. Module compiles standalone
2f. All extracted tests pass standalone
```

### Step 3: Extend Go Module (SAFE MERGE PROTOCOL)
```
3a. Clone/pull latest from origin
3b. Run existing tests: go test ./... -race -count=1 (MUST PASS)
3c. Read existing structure, understand current API
3d. Add new Go packages in NEW files (never modify existing)
3e. Run ALL tests (existing + new): go test ./... -race -count=1
3f. Run go vet ./...
3g. Only if all pass: commit
```

### Step 4: Add Challenges
```
4a. Add Challenges as git submodule
4b. Create banks/modulename/ with challenge definitions
4c. Run challenges, verify all pass
```

### Step 5: Documentation
```
5a. README.md (badges, overview, installation, quickstart)
5b. CLAUDE.md (build commands, architecture, conventions)
5c. AGENTS.md (agent capabilities)
5d. docs/user-guide.md
5e. docs/api-reference.md
5f. docs/architecture.md
5g. docs/diagrams/ (architecture, data-flow, class-diagram in Mermaid)
5h. docs/sql/schema.sql (where applicable)
5i. docs/video-course/outline.md + episode scripts
5j. docs/website/index.html
```

### Step 6: Bridge into Yole
```
6a. Add new module as git submodule in Yole
6b. Create facade (typealias) in Yole's shared/
6c. Update Yole's build.gradle.kts
6d. Run ALL Yole tests — must still pass (4,438+)
6e. Commit + push Yole
```

### Step 7: Commit & Push Everything
```
7a. Commit module repo
7b. Push to all upstreams (GitHub, GitLab)
7c. Commit Yole with updated .gitmodules
7d. Push Yole to all upstreams
```

## Safe Merge Protocol for Existing Repos

**MANDATORY: Every existing repo must be pulled, merged, and verified before any push.**

### Merge Safety Rules

| Rule | Rationale |
|------|-----------|
| Never force push | Preserves upstream history |
| Always pull before push | Prevents divergent histories |
| New code in new files | Zero merge conflict risk |
| Run existing tests first | Baseline proof repo was healthy |
| Run all tests after changes | Proof we didn't break anything |
| If merge conflict: stop and ask | Never auto-resolve conflicts |

## Per-Module Standard Structure

### Common (both KMP and Go)
```
ModuleName/
├── CLAUDE.md
├── AGENTS.md
├── README.md
├── LICENSE.txt
├── commit
├── env.properties
├── Upstreams/
│   ├── GitHub.sh
│   └── GitLab.sh
├── Challenges/                  # Git submodule
│   └── banks/modulename/
├── docs/
│   ├── user-guide.md
│   ├── api-reference.md
│   ├── architecture.md
│   ├── diagrams/
│   │   ├── architecture.mermaid
│   │   ├── data-flow.mermaid
│   │   └── class-diagram.mermaid
│   ├── sql/schema.sql           # Where applicable
│   ├── video-course/
│   │   ├── outline.md
│   │   └── scripts/
│   └── website/
│       └── index.html
```

### KMP-specific
```
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml
├── src/
│   ├── commonMain/kotlin/digital/vasic/modulename/
│   ├── commonTest/kotlin/digital/vasic/modulename/
│   ├── androidMain/
│   ├── desktopMain/
│   ├── iosMain/
│   └── wasmJsMain/
```

### Go-specific
```
├── go.mod
├── go.sum
├── pkg/
├── internal/
├── cmd/                         # CLI tools if any
```

## Design Patterns

| Pattern | Where Applied |
|---------|--------------|
| Factory | Platform-specific implementations |
| Abstract Factory | SecureStorageFactory, HttpClientFactory, PlatformFileIOFactory |
| Facade | Single entry-point API per module |
| Proxy | Remote protocol implementations |
| Observer | Sync status, operation progress |
| Mediator | NetworkStorageService coordination |
| Strategy | Pluggable parsers, auth providers, protocols |
| Template Method | BaseChallenge lifecycle |
| Builder | Config construction, document creation |
| Decorator | RateLimitedStorageService wrapping |
| Singleton | FormatRegistry |
| Adapter | Protocol-specific API adapters |

## Software Principles

| Principle | How Enforced |
|-----------|-------------|
| KISS | Each module does one thing; minimal API surface |
| DRY | Shared abstractions in common modules |
| SRP | One responsibility per module |
| OCP | New formats/protocols via registry pattern |
| LSP | All implementations substitutable via interfaces |
| ISP | Small, focused interfaces |
| DIP | Depend on abstractions, not concretes |
| YAGNI | Extract only what exists |
| Composition over Inheritance | Decorator, delegation |
| Separation of Concerns | Platform code in expect/actual |

## Dependency Graph

```
Phase 1 (zero deps):
  RateLimiter-KMP
  Concurrency-KMP
  UI-Components-KMP
  Auth-KMP
  Security-KMP

Phase 2:
  Document-KMP (standalone)
  Config-KMP → Auth-KMP
  Database-KMP (standalone)

Phase 3:
  Storage-KMP → Auth-KMP, Security-KMP, Config-KMP, Database-KMP

Phase 4:
  Formatters-KMP → Document-KMP

Phase 5:
  Yole → ALL of the above
```

## Integration into Yole

KMP modules consumed via Gradle composite builds:
```kotlin
// settings.gradle.kts
includeBuild("RateLimiter-KMP")
// ... etc
```

Facade bridges during transition:
```kotlin
package digital.vasic.yole.util
typealias RateLimiter = digital.vasic.ratelimiter.RateLimiter
```

Facades removed in Phase 5 when all modules are extracted.

## Testing Requirements

### Per Module Minimums

| Module | KMP Tests | Go Tests | Challenges |
|--------|----------|----------|------------|
| RateLimiter-KMP | 30 | 30 | 5 |
| Concurrency-KMP | 25 | 25 | 5 |
| UI-Components-KMP | 20 | N/A | 3 |
| Auth-KMP | 40 | 40 | 8 |
| Security-KMP | 30 | 30 | 6 |
| Document-KMP | 25 | 25 | 5 |
| Config-KMP | 30 | 30 | 5 |
| Database-KMP | 20 | 20 | 4 |
| Storage-KMP | 150 | 100 | 20 |
| Formatters-KMP | 200 | 100 | 25 |

### Verification Gate (before proceeding to next module)

```
✓ New KMP module compiles on all 4 targets
✓ All KMP unit tests pass
✓ Go module: go build, go vet, go test -race -count=1 pass
✓ Existing Go tests still pass (safe merge verified)
✓ All challenge banks pass
✓ Yole's full test suite (4,438+ tests) still passes
✓ Documentation complete
```

## Upstream Management

### Repos Created on GitHub + GitLab
10 new KMP repos + 1 new Go repo (Document) = 11 new repos per platform = 22 total

### Existing Go repos extended
8 repos cloned, extended with safe merge protocol, pushed

### Upstream Script Pattern
```bash
# Upstreams/GitHub.sh
#!/bin/bash
export UPSTREAMABLE_REPOSITORY="git@github.com:vasic-digital/ModuleName.git"

# Upstreams/GitLab.sh
#!/bin/bash
export UPSTREAMABLE_REPOSITORY="git@gitlab.com:vasic-digital/modulename.git"
```

After scripts created: `install_upstreams` then `commit "message"` pushes to all.

## Version Strategy

- All modules start at `v1.0.0`
- Semantic versioning
- Yole pins versions via git submodule commits
