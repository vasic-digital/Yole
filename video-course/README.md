# Yole Video Course

Comprehensive video course across beginner, advanced, and expert levels for Kotlin Multiplatform development with Yole. 36 modules and 255+ videos covering everything from first steps to autonomous QA, security scanning, stress testing, challenge-driven development, and project completion.

## Course Overview

| Level | Modules | Topics |
|-------|---------|--------|
| Beginner | 5 modules | Getting started, Markdown, Todo.txt, cross-platform app, migration |
| Advanced | 7 modules | Custom formats, performance, network storage, UI, cloud, containers, security |
| Expert | 24 modules | Architecture, deployment, testing, community, challenges, monitoring, contributing, platform-specific, stress testing, concurrency safety, security scanning deep dive, performance optimization, complete test coverage, UI automation, non-blocking architecture, test coverage mastery, autonomous QA, project completion, concurrency safety patterns, security scanning pipeline, stress testing & performance, challenge development, project completion & quality gates |

## Structure

```
video-course/
├── beginner/
│   ├── 01-getting-started/          # Module 1: Getting Started (5 videos)
│   ├── 02-markdown-editor/          # Module 2: Building a Markdown Editor (8 videos)
│   ├── 03-todotxt-manager/          # Module 3: Todo.txt Manager (7 videos)
│   ├── 04-note-app/                 # Module 4: Cross-Platform Note App (5 videos)
│   └── 19-migration-from-markor/    # Module 19: Migration from Markor (5 videos)
├── advanced/
│   ├── 05-custom-formats/           # Module 5: Custom Format Development (10 videos)
│   ├── 06-performance/              # Module 6: Performance Optimization (8 videos)
│   ├── 07-network-storage/          # Module 7: Network Storage Integration (12 videos)
│   ├── 08-ui-customization/         # Module 8: Advanced UI Customization (10 videos)
│   ├── 13-cloud-storage/            # Module 13: Cloud Storage Integration (7 videos)
│   ├── 14-container-development/    # Module 14: Container-Based Development (6 videos)
│   └── 15-security-scanning/        # Module 15: Security Scanning (7 videos)
└── expert/
    ├── 09-architecture/             # Module 9: Advanced Architecture Patterns (12 videos)
    ├── 10-deployment/               # Module 10: Production Deployment (10 videos)
    ├── 11-testing/                   # Module 11: Testing Strategies (8 videos)
    ├── 12-community/                # Module 12: Community Contribution (5 videos)
    ├── 16-challenges-framework/     # Module 16: Challenges Framework (6 videos)
    ├── 17-monitoring-performance/   # Module 17: Monitoring & Performance (7 videos)
    ├── 18-contributing/             # Module 18: Contributing to Yole (7 videos)
    ├── 20-platform-specific/        # Module 20: Platform-Specific Development (7 videos)
    ├── 21-stress-testing/           # Module 21: Stress Testing & Responsiveness (8 videos)
    ├── 22-concurrency-safety/       # Module 22: Concurrency Safety in KMP (8 videos)
    ├── 23-security-scanning-deep-dive/ # Module 23: Security Scanning Deep Dive (7 videos)
    ├── 24-performance-optimization/ # Module 24: Performance Optimization (8 videos)
    ├── 25-complete-test-coverage/   # Module 25: Complete Test Coverage (8 videos)
    ├── 26-ui-automation-testing/    # Module 26: UI/UX Automation Testing (6 videos)
    ├── 27-non-blocking-architecture/ # Module 27: Non-Blocking Architecture (8 videos)
    ├── 28-test-coverage-mastery/    # Module 28: Test Coverage Mastery (8 videos)
    ├── 29-performance-optimization-advanced/ # Module 29: Performance Optimization Advanced (8 videos)
    ├── 30-autonomous-qa/            # Module 30: Autonomous QA (8 videos)
    ├── 31-project-completion-guide/ # Module 31: Project Completion Guide (8 videos)
    ├── 32-concurrency-safety-patterns/ # Module 32: Concurrency Safety Patterns in KMP (8 videos)
    ├── 33-security-scanning-pipeline/  # Module 33: Security Scanning Pipeline (8 videos)
    ├── 34-stress-testing-performance/  # Module 34: Stress Testing & Performance Monitoring (8 videos)
    ├── 35-challenge-development/       # Module 35: Challenge-Driven Development (8 videos)
    └── 36-project-completion/          # Module 36: Project Completion & Quality Gates (8 videos)
```

## Beginner Level (5 modules)

### Module 1: Getting Started (5 videos)
Introduction to Yole and Kotlin Multiplatform. Covers environment setup, first build, understanding the build system, and debugging.

**Prerequisites:** Basic Kotlin knowledge.

### Module 2: Building a Markdown Editor (8 videos)
Deep dive into the Markdown parser, Flexmark extensions, and building an editor with live preview.

**Prerequisites:** Module 1.

### Module 3: Todo.txt Manager (7 videos)
Building a Todo.txt task manager with query syntax, priorities, contexts, and projects.

**Prerequisites:** Module 1.

### Module 4: Cross-Platform Note App (5 videos)
Building a cross-platform note-taking app targeting Android, Desktop, and Web from a single codebase.

**Prerequisites:** Modules 1-3.

### Module 19: Migration from Markor (5 videos)
Guide for existing Markor users migrating to Yole. Covers file compatibility, new formats, cross-platform usage, and step-by-step migration.

**Prerequisites:** None (standalone introduction for Markor users).

## Advanced Level (7 modules)

### Module 5: Custom Format Development (10 videos)
Format architecture deep dive. Building custom parsers from scratch including YAML, TOML, Mermaid, and Graphviz DOT.

**Prerequisites:** Module 1, basic understanding of text parsing.

### Module 6: Performance Optimization (8 videos)
Profiling, parser optimization, UI performance, platform-specific tuning, and benchmarking.

**Prerequisites:** Module 5.

### Module 7: Network Storage Integration (12 videos)
Complete network storage integration: OAuth2, all 8 protocol implementations, sync conflict resolution, offline queue, and encryption.

**Prerequisites:** Module 1, basic networking knowledge.

### Module 8: Advanced UI Customization (10 videos)
Compose Multiplatform UI development: custom themes, animations, accessibility, and responsive layouts.

**Prerequisites:** Module 4.

### Module 13: Cloud Storage Integration (7 videos)
Hands-on tutorial for setting up each cloud storage protocol: Dropbox, Google Drive, OneDrive, WebDAV, FTP/SFTP, and Git. OAuth2 walkthrough and sync operations.

**Prerequisites:** Module 7 (or basic understanding of REST APIs and OAuth2).

### Module 14: Container-Based Development (6 videos)
Docker/Podman setup, docker-compose.yml deep dive, building and testing in containers, SonarQube integration, and security scanning workflow.

**Prerequisites:** Module 1, basic Docker/Podman knowledge.

### Module 15: Security Scanning (7 videos)
Comprehensive security scanning with SonarQube, Snyk, Gitleaks, CodeQL, OWASP Dependency Check, and Detekt. Interpreting findings and creating remediation plans.

**Prerequisites:** Module 14.

## Expert Level (24 modules)

### Module 9: Advanced Architecture Patterns (12 videos)
Clean architecture, dependency injection, state management (MVI), event-driven patterns, modularization, plugin architecture, and database design.

**Prerequisites:** Modules 1-8 (solid understanding of the codebase).

### Module 10: Production Deployment (10 videos)
CI/CD pipeline setup, code signing, app store deployment (Google Play, Apple App Store, F-Droid), crash reporting, analytics, feature flags, and disaster recovery.

**Prerequisites:** Module 9.

### Module 11: Testing Strategies (8 videos)
Comprehensive testing strategy, property-based testing, UI testing, concurrency testing, performance testing, security testing, mock strategies, and continuous testing infrastructure.

**Prerequisites:** Module 9, experience writing tests.

### Module 12: Community Contribution (5 videos)
Open source best practices, documentation standards, issue management, code review process, and building community.

**Prerequisites:** Module 11.

### Module 16: Challenges Framework (6 videos)
The Go-based Challenges framework: challenge banks (JSON), running challenges against Yole, the 14 Yole challenge categories (including concurrency, resilience, monitoring, lazy-loading, memory), creating custom challenges, and CI/CD integration.

**Prerequisites:** Modules 9-12, basic Go knowledge.

### Module 17: Monitoring & Performance (7 videos)
Resilience patterns (CircuitBreaker, ConnectionLimiter, DocumentCache, RateLimitedStorageService), document caching with hit/miss tracking, connection management, performance metrics (10,000+ test methods, 63.0% coverage), memory profiling, and benchmark testing.

**Prerequisites:** Module 9, Module 6.

### Module 18: Contributing to Yole (7 videos)
Complete contributor guide: repository structure (main + 10 KMP modules + 2 Go submodules), git workflow, development environment, running the full test suite (10,000+ test methods across ~215 files), adding formats, adding protocols, resilience patterns, security scanning, and code review standards.

**Prerequisites:** Modules 9-12 (or equivalent familiarity with the codebase).

### Module 20: Platform-Specific Development (7 videos)
The expect/actual pattern in depth, platform-specific implementations for all four targets (Android, Desktop, iOS, Wasm), browser sandbox constraints, platform factory patterns, and testing strategies for platform-specific code including MockK JVM-only limitations.

**Prerequisites:** Module 9, Module 11.

### Module 21: Stress Testing & Responsiveness (8 videos)
Stress testing philosophy, concurrent format parsing, protocol overload and circuit breaker testing, cache overflow and LRU stress testing, rate limiter saturation, end-to-end responsiveness validation (<100ms p99), non-blocking guarantee verification, and memory leak regression testing patterns.

**Prerequisites:** Module 11, Module 17.

### Module 22: Concurrency Safety in KMP (8 videos)
KMP concurrency challenges, Mutex and lock ordering convention (8 priority levels), @Volatile and lazy initialization patterns, StateFlow for reactive state, Semaphore and rate limiting, SupervisorJob and structured concurrency, CancellationException handling rules, and concurrency testing patterns.

**Prerequisites:** Module 9, Module 11.

### Module 23: Security Scanning Deep Dive (7 videos)
Security architecture overview, SonarQube deep dive (setup, scanning, interpreting results), Snyk dependency vulnerability management, CodeQL semantic analysis, Gitleaks secret detection, OWASP Dependency Check with SBOM generation, and building a security-first development workflow.

**Prerequisites:** Module 15 (or basic security knowledge).

### Module 24: Performance Optimization (8 videos)
Performance architecture overview, FormatRegistry lazy initialization (benchmarks), ParsedDocument lazy HTML caching, DocumentCache LRU strategy, StyleSheets caching, network protocol performance tuning, parser-specific optimization, and performance monitoring in CI.

**Prerequisites:** Module 6, Module 9.

### Module 25: Complete Test Coverage (8 videos)
The 10,000+ test suite architecture, unit and integration testing patterns, fuzz/snapshot/property-based testing, stress and load testing, E2E and non-blocking testing, security and resilience testing, accessibility and platform-specific testing, and CI coverage strategy.

**Prerequisites:** Module 11, Module 21.

### Module 26: UI/UX Automation Testing (6 videos)
Real interaction testing across platforms with recorded automation challenges. Speed mode testing (slow, normal, fast), platform-specific adapters (Desktop xdotool, Web Playwright, Android ADB), recording pipelines, and validation.

**Prerequisites:** Module 20, Module 21.

### Module 27: Non-Blocking Architecture (8 videos)
Non-blocking patterns in Yole: Dispatchers.IO/Default/Main usage, suspend function design, lazy initialization cascade (FormatRegistry, StyleSheets, HttpClient, OAuth2Flow), Wasm single-thread safety, platformSynchronized rationale, and protocol service non-blocking patterns.

**Prerequisites:** Module 9, Module 22.

### Module 28: Test Coverage Mastery (8 videos)
All 16 test types explained: unit, integration, stress, fuzz, snapshot, load, E2E, performance, accessibility, security, resilience, property-based, contract, non-blocking, supremacy, and mock HTTP. When to use each type, examples from Yole's 10,000+ test suite, and scaling strategies.

**Prerequisites:** Module 11, Module 25.

### Module 29: Performance Optimization Advanced (8 videos)
PerformanceMetrics infrastructure, MetricsSnapshot data model, tiered caching (DocumentCache LRU, StyleSheets cache, ParsedDocument lazy HTML), platform-aware semaphores, configurable parse concurrency, async format detection for 17 text formats plus binary detection, and parser-specific optimization.

**Prerequisites:** Module 24, Module 27.

### Module 30: Autonomous QA (8 videos)
HelixQA pipeline: DocProcessor (219 tests) for code analysis, LLMOrchestrator (247 tests) for multi-agent coordination, VisionEngine (262 tests) for visual analysis. SessionCoordinator, PlatformWorker, issue detection, ticket generation, and evidence collection across all platforms.

**Prerequisites:** Module 16, Module 25.

### Module 31: Project Completion Guide (8 videos)
How the 9-phase comprehensive completion plan was designed and executed. Build-Test-Document pipeline methodology, audit methodology with 6 parallel exploration agents, phase dependencies, verification approach, and lessons learned for applying to your own KMP projects.

**Prerequisites:** Modules 9-30 (or equivalent familiarity with the full project).

### Module 32: Concurrency Safety Patterns in KMP (8 videos)
Deep dive into @Volatile usage in protocol services and FormatRegistry, lock ordering conventions across 8 mutex priorities, HttpTimeout configuration in platform HttpClientFactory implementations, CancellationException rethrow rules, SupervisorJob and structured concurrency, and writing concurrency regression tests. Covers the Session 7 audit findings and fixes applied across all 8 protocol services.

**Prerequisites:** Module 9, Module 22.

### Module 33: Security Scanning Pipeline (8 videos)
End-to-end walkthrough of Yole's 6-tool security pipeline: Detekt (zero violations at scale), SonarQube project versioning and dashboard interpretation, Snyk dependency triage, keystore password parameterization, OWASP Dependency Check with SBOM generation, and CodeQL/Gitleaks for semantic analysis and secret detection. Covers building a security-first development workflow with per-phase scan cadences.

**Prerequisites:** Module 15, Module 23.

### Module 34: Stress Testing & Performance Monitoring (8 videos)
Per-protocol stress test design, parser overload testing with the parseSemaphore guard, timeout recovery verification using MockEngine, PerformanceMetrics/MetricsSnapshot/MetricsReporter infrastructure, performance baseline tests with percentile thresholds, memory leak detection for DocumentCache and StyleSheets, and integrating stress tests into daily vs weekly vs pre-release cadences.

**Prerequisites:** Module 11, Module 21, Module 17.

### Module 35: Challenge-Driven Development (8 videos)
Challenge banks as executable specifications: schema design (id, platform, priority, scenarios), the three existing banks (security, format-edge-cases, protocol-resilience), writing a new bank, the runChallenges Gradle task, ChallengeValidationTests in the Kotlin suite, HelixQA test bank integration, and growing the challenge framework alongside new features.

**Prerequisites:** Module 16, Module 25.

### Module 36: Project Completion & Quality Gates (8 videos)
Defining "complete" across five dimensions (correctness, safety, security, documentation, observability), the 6-phase session structure (audit, implement, test, document, verify, commit), the zero-defect verification workflow, Kover coverage gates at 70% minimum, the full 7-session history of Yole's hardening journey, documentation as a quality gate, and adapting these gates to your own KMP project.

**Prerequisites:** Modules 9-35 (or equivalent familiarity with the full project).

## Production Details

See [VIDEO_COURSE_PRODUCTION_PLAN.md](../VIDEO_COURSE_PRODUCTION_PLAN.md) for the full production plan including:
- Detailed content outlines for each video
- Technical specifications (4K video, 48kHz audio)
- Production schedule and workflow
- Budget and team requirements

## Getting Started

Each module directory contains:
- `script.md` -- Narration script and content outline for all videos in the module
- Code examples are referenced from the main Yole source tree

## Recommended Learning Paths

### New to Yole
Modules 1 -> 2 -> 3 -> 4 -> 5 -> 7 -> 9

### Markor User
Module 19 -> 1 -> 13 -> 4

### DevOps / Infrastructure Focus
Modules 1 -> 14 -> 15 -> 16 -> 10

### Performance / Architecture Focus
Modules 1 -> 5 -> 6 -> 9 -> 17 -> 21 -> 24 -> 29

### Platform / KMP Focus
Modules 1 -> 4 -> 9 -> 20 -> 11 -> 21

### Contributor
Modules 1 -> 14 -> 11 -> 18 -> 16

### Security Focus
Modules 1 -> 14 -> 15 -> 23 -> 10

### Concurrency / Testing Deep Dive
Modules 1 -> 9 -> 22 -> 11 -> 21 -> 25 -> 27 -> 28

### Autonomous QA
Modules 1 -> 9 -> 11 -> 25 -> 30

### Project Completion
Modules 1 -> 9 -> 11 -> 18 -> 31
