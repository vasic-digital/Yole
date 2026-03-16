# Yole Video Course

Comprehensive video course across beginner, advanced, and expert levels for Kotlin Multiplatform development with Yole. 21 modules and 147 videos covering everything from first steps to contributing to the project.

## Course Overview

| Level | Modules | Topics |
|-------|---------|--------|
| Beginner | 5 modules | Getting started, Markdown, Todo.txt, cross-platform app, migration |
| Advanced | 7 modules | Custom formats, performance, network storage, UI, cloud, containers, security |
| Expert | 9 modules | Architecture, deployment, testing, community, challenges, monitoring, contributing, platform-specific, stress testing |

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
    └── 21-stress-testing/           # Module 21: Stress Testing & Responsiveness (8 videos)
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

## Expert Level (9 modules)

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
Resilience patterns (CircuitBreaker, ConnectionLimiter, DocumentCache, RateLimitedStorageService), document caching with hit/miss tracking, connection management, performance metrics (11,000+ test methods, 63.0% coverage), memory profiling, and benchmark testing.

**Prerequisites:** Module 9, Module 6.

### Module 18: Contributing to Yole (7 videos)
Complete contributor guide: repository structure (main + 10 KMP modules + 2 Go submodules), git workflow, development environment, running the full test suite (11,000+ test methods across 195 files), adding formats, adding protocols, resilience patterns, security scanning, and code review standards.

**Prerequisites:** Modules 9-12 (or equivalent familiarity with the codebase).

### Module 20: Platform-Specific Development (7 videos)
The expect/actual pattern in depth, platform-specific implementations for all four targets (Android, Desktop, iOS, Wasm), browser sandbox constraints, platform factory patterns, and testing strategies for platform-specific code including MockK JVM-only limitations.

**Prerequisites:** Module 9, Module 11.

### Module 21: Stress Testing & Responsiveness (8 videos)
Stress testing philosophy, concurrent format parsing, protocol overload and circuit breaker testing, cache overflow and LRU stress testing, rate limiter saturation, end-to-end responsiveness validation (<100ms p99), non-blocking guarantee verification, and memory leak regression testing patterns.

**Prerequisites:** Module 11, Module 17.

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
Modules 1 -> 5 -> 6 -> 9 -> 17 -> 21

### Platform / KMP Focus
Modules 1 -> 4 -> 9 -> 20 -> 11 -> 21

### Contributor
Modules 1 -> 14 -> 11 -> 18 -> 16
