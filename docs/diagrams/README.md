<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2025 Milos Vasic -->

# Architecture Diagrams

This directory contains Mermaid (`.mmd`) diagrams that document the Yole system architecture. Each file can be rendered by any Mermaid-compatible viewer, including GitHub's built-in Markdown preview (wrap the content in a ` ```mermaid ` code block) or the [Mermaid Live Editor](https://mermaid.live/).

## Diagram Index

### 1. architecture-overview.mmd

**Type:** Graph (top-down)

High-level view of the entire Yole system. Shows platform applications (Android, Desktop, iOS, Web), the shared KMP module with its source sets, all 10 extracted KMP modules connected via composite builds, legacy Android modules being phased out, and the Go submodules (Challenges, Containers).

### 2. auth-flow.mmd

**Type:** Sequence diagram

OAuth2 authentication lifecycle for cloud storage providers (Dropbox, Google Drive, OneDrive). Covers the full flow: authorization URL generation, browser-based consent, authorization code exchange, secure token storage via AuthTokenManager and platform-specific SecureStorage, token refresh when expired, and token invalidation on disconnect.

### 3. ci-cd-pipeline.mmd

**Type:** Graph (top-down)

Overview of all GitHub Actions workflows and their triggers. Documents four workflows: CI (build, test, lint, coverage), Security (Gitleaks, OWASP, Snyk, CodeQL), SonarQube (quality analysis), and Release (APK/JAR builds with GitHub Release creation). Shows trigger relationships (push, PR, schedule, manual dispatch) and concurrency groups.

### 4. data-model.mmd

**Type:** Entity-Relationship diagram

Core data classes and their relationships. Covers Document, TextFormat, ParsedDocument, all eight protocol-specific config classes (WebDavConfig, FtpConfig, SftpConfig, SmbConfig, GoogleDriveConfig, DropboxConfig, OneDriveConfig, GitConfig), NetworkDocument, NetworkStorage, NetworkOperation, StorageQuota, TokenResponse, CacheEntry, and TokenInfo.

### 5. format-pipeline.mmd

**Type:** Sequence diagram

Text parsing pipeline from file open to HTML rendering. Shows format detection (by extension and by content), lazy parser instantiation, the parse phase producing a ParsedDocument, and the lazy-cached HTML generation with separate light/dark theme caches via StyleSheets.

### 6. module-dependencies.mmd

**Type:** Graph (top-down)

Dependency graph showing how all modules connect. Platform apps depend on the shared module, which depends on 10 extracted KMP modules via `implementation()`. Shows facade bridge typealiases (RateLimiter, Concurrency, Auth), inter-module dependencies (Auth depends on Security), the Go submodule relationship (Challenges depends on Containers), and key external dependencies (Ktor, Compose, Coroutines, Serialization, Okio).

### 7. network-protocol-flow.mmd

**Type:** Sequence diagram

Network protocol service lifecycle for cloud and network storage operations. Covers service construction with lazy HttpClient, the connection phase with token validation and refresh, file operations (list, upload, download) with rate limiting, error handling with backoff and retry, and disconnection with resource cleanup (coroutine cancellation, client closure, cache clearing).

### 8. sync-flow.mmd

**Type:** Sequence diagram

Offline-first file synchronization flow. Documents local change detection, upload queueing with backpressure when offline, download triggered by remote change detection via timestamp comparison, conflict detection and resolution (keep local, keep remote, merge), cache management with pinning, and bulk sync-all operations.
