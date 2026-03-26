// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2025 Milos Vasic

const extractedModules = [
  { name: "RateLimiter-KMP", pkg: "digital.vasic.ratelimiter", description: "Token bucket rate limiting, adaptive rate limiters, and operation throttling utilities." },
  { name: "Concurrency-KMP", pkg: "digital.vasic.concurrency", description: "Lazy loading utilities and platform-synchronized operations for coroutine-based code." },
  { name: "UI-Components-KMP", pkg: "digital.vasic.uicomponents", description: "Theme engine, animation utilities, and accessibility helpers for Compose Multiplatform." },
  { name: "Auth-KMP", pkg: "digital.vasic.auth", description: "OAuth2 flows, token management, and authentication for Dropbox, Google Drive, and OneDrive." },
  { name: "Security-KMP", pkg: "digital.vasic.security", description: "Secure storage abstraction for sensitive credentials across all platforms." },
  { name: "Document-KMP", pkg: "digital.vasic.document", description: "Core document model shared by all format parsers and platform UI layers." },
  { name: "Config-KMP", pkg: "digital.vasic.config", description: "Network and storage configuration data classes and validation." },
  { name: "Database-KMP", pkg: "digital.vasic.database", description: "Metadata storage for network file caching and sync state." },
  { name: "Storage-KMP", pkg: "digital.vasic.storage", description: "Protocol abstractions and implementations for all cloud storage providers." },
  { name: "Formatters-KMP", pkg: "digital.vasic.formatters", description: "17 text format parsers, format registry, and detection engine." },
];

const platforms = [
  { name: "Android", target: "androidTarget", status: "Production", description: "Compose Multiplatform UI with Material Design, full Play Store distribution.", tech: "Android SDK, Compose, JVM" },
  { name: "Desktop", target: "jvm(\"desktop\")", status: "Beta", description: "Compose for Desktop on Windows, macOS, and Linux via JVM 11+.", tech: "JVM 11+, Compose Desktop" },
  { name: "iOS", target: "iosArm64 / iosX64 / iosSimulatorArm64", status: "In Development", description: "Compose Multiplatform for iPhone and iPad via Kotlin/Native.", tech: "Kotlin/Native, Compose iOS" },
  { name: "Web", target: "wasmJs", status: "In Development", description: "Browser-based PWA editor powered by Kotlin/Wasm and Compose for Web.", tech: "Kotlin/Wasm, Compose Web" },
];

const statusColors: Record<string, string> = {
  "Production": "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
  "Beta": "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400",
  "In Development": "bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400",
};

const pipelineSteps = [
  { step: 1, title: "Detection", description: "FormatRegistry.detectByExtension() or detectByContent() uses regex patterns defined in each TextFormat to identify the file format." },
  { step: 2, title: "Parsing", description: "The format-specific parser produces a ParsedDocument containing raw content, parsed structure, metadata, and any validation errors." },
  { step: 3, title: "HTML Generation", description: "ParsedDocument.toHtml() generates HTML with lazy caching -- the first call computes the output, subsequent calls return the cached result." },
  { step: 4, title: "Styling", description: "StyleSheets.kt generates CSS for light and dark themes, applied to the HTML output for rendering in the platform's preview component." },
];

const keyDependencies = [
  { name: "Kotlin", version: "2.0.20", purpose: "Primary language" },
  { name: "Compose Multiplatform", version: "1.7.3", purpose: "Cross-platform UI framework" },
  { name: "Flexmark", version: "0.64.8", purpose: "Markdown parsing (16+ extensions)" },
  { name: "Ktor Client", version: "3.0.2", purpose: "HTTP networking for cloud protocols" },
  { name: "Kotlinx Coroutines", version: "1.9.0", purpose: "Async and concurrent operations" },
  { name: "Kotlinx Serialization", version: "1.7.3", purpose: "JSON and protocol serialization" },
  { name: "Kotlinx DateTime", version: "0.6.1", purpose: "Cross-platform date and time" },
  { name: "Okio", version: "3.9.1", purpose: "File system abstraction" },
  { name: "Kotest", version: "5.9.1", purpose: "Testing framework" },
  { name: "Kover", version: "0.8.3", purpose: "Code coverage reporting" },
];

export default function ArchitecturePage() {
  return (
    <div className="container-page py-16">
      <h1 className="text-4xl font-bold tracking-tight mb-4">Architecture</h1>
      <p className="text-lg text-[var(--color-text-secondary)] mb-12 max-w-3xl">
        Yole is built with Kotlin Multiplatform (KMP) to share business logic across
        Android, Desktop, iOS, and Web. Here is an overview of the system design.
      </p>

      {/* High-Level Overview */}
      <section className="mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-4">Kotlin Multiplatform</h2>
        <div className="card">
          <p className="text-[var(--color-text-secondary)] leading-relaxed mb-4">
            Yole uses Kotlin Multiplatform to write shared business logic once and compile it
            for Android (JVM), Desktop (JVM), iOS (Native), and Web (Wasm). All 17 text format
            parsers, network protocol clients, authentication flows, and utility code live in
            the shared module. Platform-specific code is limited to UI rendering and native
            integrations.
          </p>
          <p className="text-[var(--color-text-secondary)] leading-relaxed">
            The architecture follows a modular design with 10 independently extracted KMP
            modules consumed via Gradle composite builds. This ensures clean separation of
            concerns, independent testing, and reusability across projects.
          </p>
        </div>
      </section>

      {/* Module Structure */}
      <section className="mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-4">Module Structure</h2>
        <p className="text-[var(--color-text-secondary)] mb-6 max-w-3xl">
          The shared module is decomposed into 10 independently versioned KMP libraries.
          Each module has its own repository, tests, and documentation.
        </p>

        <div className="card mb-6">
          <h3 className="font-semibold mb-4">Project Layout</h3>
          <pre className="text-sm bg-[var(--color-bg-secondary)] rounded-lg p-4 overflow-x-auto border border-[var(--color-border)]"><code>{`Yole/
├── shared/                  # Shared KMP module (depends on extracted modules)
│   ├── src/commonMain/      # Common Kotlin code
│   ├── src/commonTest/      # 10,000+ tests
│   ├── src/androidMain/     # Android expect/actual
│   ├── src/desktopMain/     # Desktop (JVM) expect/actual
│   ├── src/iosMain/         # iOS expect/actual
│   └── src/wasmJsMain/      # Web/Wasm expect/actual
├── androidApp/              # Android application (Compose)
├── desktopApp/              # Desktop application (Compose Desktop)
├── iosApp/                  # iOS application (Xcode project)
├── webApp/                  # Web application (Wasm PWA)
├── Challenges/              # Go-based testing framework (submodule)
└── Containers/              # Container orchestration (submodule)`}</code></pre>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {extractedModules.map((module) => (
            <div key={module.name} className="card">
              <h3 className="font-semibold text-primary-500 mb-1">{module.name}</h3>
              <code className="text-xs text-[var(--color-text-secondary)] block mb-2">{module.pkg}</code>
              <p className="text-sm text-[var(--color-text-secondary)]">{module.description}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Platform Support */}
      <section className="mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-4">Platform Support</h2>
        <p className="text-[var(--color-text-secondary)] mb-6 max-w-3xl">
          One codebase compiles to four platform targets. Each platform app is a thin
          wrapper that depends on the shared module for all business logic.
        </p>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {platforms.map((platform) => (
            <div key={platform.name} className="card">
              <div className="flex items-center gap-3 mb-3">
                <h3 className="text-xl font-bold">{platform.name}</h3>
                <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${statusColors[platform.status]}`}>
                  {platform.status}
                </span>
              </div>
              <p className="text-sm text-[var(--color-text-secondary)] mb-3">{platform.description}</p>
              <div className="flex flex-wrap gap-2">
                <code className="text-xs px-2 py-1 rounded bg-[var(--color-bg-secondary)] border border-[var(--color-border)]">
                  {platform.target}
                </code>
                <code className="text-xs px-2 py-1 rounded bg-[var(--color-bg-secondary)] border border-[var(--color-border)]">
                  {platform.tech}
                </code>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Text Parsing Pipeline */}
      <section className="mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-4">Text Parsing Pipeline</h2>
        <p className="text-[var(--color-text-secondary)] mb-6 max-w-3xl">
          Every file opened in Yole goes through a four-stage pipeline from raw text to
          styled HTML output.
        </p>
        <div className="space-y-4">
          {pipelineSteps.map((step) => (
            <div key={step.step} className="card flex items-start gap-4">
              <div className="flex-shrink-0 w-10 h-10 rounded-full bg-primary-500/10 text-primary-500 flex items-center justify-center font-bold">
                {step.step}
              </div>
              <div>
                <h3 className="font-semibold text-lg mb-1">{step.title}</h3>
                <p className="text-sm text-[var(--color-text-secondary)]">{step.description}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Package Layout */}
      <section className="mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-4">Package Layout</h2>
        <div className="card">
          <pre className="text-sm bg-[var(--color-bg-secondary)] rounded-lg p-4 overflow-x-auto border border-[var(--color-border)]"><code>{`shared/src/commonMain/kotlin/digital/vasic/yole/
├── format/                    # Format system -- the core of the app
│   ├── FormatRegistry.kt      # Central registry: all formats with detection priority
│   ├── TextFormat.kt          # Format metadata (id, name, extensions, patterns)
│   ├── TextParser.kt          # ParsedDocument class with lazy HTML caching
│   ├── ParserInitializer.kt   # Format initialization
│   ├── StyleSheets.kt         # CSS generation for HTML rendering
│   ├── markdown/              # 17 text format parsers (one directory each)
│   ├── todotxt/
│   ├── csv/ latex/ orgmode/ plaintext/ wikitext/ asciidoc/
│   ├── restructuredtext/ rmarkdown/ taskpaper/ textile/
│   └── creole/ tiddlywiki/ jupyter/ keyvalue/ binary/
├── model/                     # Document model (Document.kt)
├── network/                   # Network storage system
│   ├── NetworkStorageService.kt
│   ├── auth/                  # Authentication (OAuth2, token management)
│   ├── common/                # Shared network utilities
│   ├── config/                # Network configuration
│   ├── database/              # Metadata storage
│   ├── platform/              # Platform-specific networking (expect/actual)
│   ├── protocol/              # Protocol abstractions
│   └── protocols/             # Implementations (Dropbox, Drive, OneDrive, etc.)
├── ui/                        # Shared UI (Compose Multiplatform)
│   ├── Theme.kt               # Theme engine with light/dark modes
│   ├── Accessibility.kt       # Accessibility helpers
│   └── Animations.kt          # Animation utilities
└── util/                      # Utilities
    ├── LazyLoading.kt         # Lazy document loaders
    ├── RateLimiting.kt        # Rate limiter facades
    └── PlatformSync.kt        # Platform-synchronized operations`}</code></pre>
        </div>
      </section>

      {/* Network Protocol System */}
      <section className="mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-4">Network Protocol System</h2>
        <div className="card">
          <p className="text-[var(--color-text-secondary)] leading-relaxed mb-4">
            The network layer implements 8 cloud storage protocols through a unified
            abstraction. Each protocol service extends a common interface providing
            file listing, reading, writing, and deletion operations.
          </p>
          <p className="text-[var(--color-text-secondary)] leading-relaxed mb-4">
            OAuth2-based protocols (Dropbox, Google Drive, OneDrive) use platform-specific
            browser-based authorization flows with token refresh. Direct protocols (FTP, SFTP,
            WebDAV) use credential-based authentication. All HTTP-based protocols are built
            on Ktor Client with configurable rate limiting.
          </p>
          <pre className="text-sm bg-[var(--color-bg-secondary)] rounded-lg p-4 overflow-x-auto border border-[var(--color-border)]"><code>{`NetworkStorageService (interface)
├── DropboxService      -- Dropbox API v2 via OAuth2
├── GoogleDriveService  -- Google Drive API v3 via OAuth2
├── OneDriveService     -- Microsoft Graph API via OAuth2
├── WebDavService       -- WebDAV (PROPFIND, GET, PUT, DELETE)
├── FtpService          -- FTP with active/passive mode
├── SftpService         -- SFTP over SSH
├── SmbService          -- SMB/CIFS network file shares
└── GitService          -- Git clone/pull/push via HTTPS/SSH`}</code></pre>
        </div>
      </section>

      {/* Monitoring and Stress Testing */}
      <section className="mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-4">Monitoring and Stress Testing</h2>
        <div className="card">
          <p className="text-[var(--color-text-secondary)] leading-relaxed mb-4">
            Yole includes a monitoring and metrics layer that tracks performance baselines,
            cache hit rates, connection pool utilization, and parsing throughput across all
            format parsers and network protocols.
          </p>
          <p className="text-[var(--color-text-secondary)] leading-relaxed mb-4">
            The stress testing infrastructure validates system behavior under extreme conditions
            including concurrent file operations, large document parsing, rapid connection
            cycling, and high-frequency rate limiter contention. Fuzz tests exercise parsers
            with randomized inputs to catch edge cases and prevent regressions.
          </p>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-4">
            <div className="text-center">
              <div className="text-lg font-bold text-primary-500">Fuzz</div>
              <div className="text-xs text-[var(--color-text-secondary)]">Randomized input testing</div>
            </div>
            <div className="text-center">
              <div className="text-lg font-bold text-primary-500">Snapshot</div>
              <div className="text-xs text-[var(--color-text-secondary)]">Output regression checks</div>
            </div>
            <div className="text-center">
              <div className="text-lg font-bold text-primary-500">Load</div>
              <div className="text-xs text-[var(--color-text-secondary)]">Throughput benchmarks</div>
            </div>
            <div className="text-center">
              <div className="text-lg font-bold text-primary-500">Monitoring</div>
              <div className="text-xs text-[var(--color-text-secondary)]">Performance baselines</div>
            </div>
          </div>
        </div>
      </section>

      {/* Concurrency Safety */}
      <section className="mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-4">Concurrency Safety</h2>
        <div className="card">
          <p className="text-[var(--color-text-secondary)] leading-relaxed mb-4">
            Yole enforces thread safety across all shared state and protocol services using
            a layered concurrency strategy. Critical sections are protected by Kotlin coroutine
            Mutex guards, ensuring that concurrent operations on shared resources are serialized
            without blocking threads.
          </p>
          <p className="text-[var(--color-text-secondary)] leading-relaxed mb-4">
            All 8 network protocol services use scopeMutex for lifecycle management and
            pauseFlagsMutex for pause/resume state. Volatile annotations on shared fields
            ensure visibility across threads. StateFlow.update&#123;&#125; provides atomic state
            transitions for UI state. Semaphore-based connection limiters cap concurrent
            connections per protocol.
          </p>
          <p className="text-[var(--color-text-secondary)] leading-relaxed mb-4">
            A strict lock ordering convention is enforced across all services to prevent
            deadlocks: scopeMutex must be acquired before stateMutex, which must be acquired
            before operationsMutex, then syncMutex, cacheMutex, pauseFlagsMutex, activeJobsMutex,
            and storageInitMutex. @Volatile is applied to all lazy-initialized shared fields
            (HttpClient, cache flags, connection state) in WebDAV, Git, and platform-specific
            secure storage. HttpTimeout is configured on all Ktor clients to prevent indefinite
            hangs under network partition conditions.
          </p>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-4">
            <div className="text-center">
              <div className="text-lg font-bold text-primary-500">Mutex</div>
              <div className="text-xs text-[var(--color-text-secondary)]">Coroutine-safe critical sections</div>
            </div>
            <div className="text-center">
              <div className="text-lg font-bold text-primary-500">@Volatile</div>
              <div className="text-xs text-[var(--color-text-secondary)]">Cross-thread field visibility</div>
            </div>
            <div className="text-center">
              <div className="text-lg font-bold text-primary-500">StateFlow</div>
              <div className="text-xs text-[var(--color-text-secondary)]">Atomic state transitions</div>
            </div>
            <div className="text-center">
              <div className="text-lg font-bold text-primary-500">Semaphore</div>
              <div className="text-xs text-[var(--color-text-secondary)]">Connection pool limiting</div>
            </div>
          </div>
        </div>
      </section>

      {/* Security Scanning Pipeline */}
      <section className="mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-4">Security Scanning Pipeline</h2>
        <div className="card">
          <p className="text-[var(--color-text-secondary)] leading-relaxed mb-4">
            Yole integrates 6 security scanning tools into its CI/CD pipeline to detect
            vulnerabilities, secrets, and code quality issues before they reach production.
            Scans run automatically on every pull request and release build.
          </p>
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mt-4">
            <div className="text-center">
              <div className="text-lg font-bold text-primary-500">SonarQube</div>
              <div className="text-xs text-[var(--color-text-secondary)]">Code quality and security hotspots</div>
            </div>
            <div className="text-center">
              <div className="text-lg font-bold text-primary-500">Snyk</div>
              <div className="text-xs text-[var(--color-text-secondary)]">Dependency vulnerability scanning</div>
            </div>
            <div className="text-center">
              <div className="text-lg font-bold text-primary-500">Detekt</div>
              <div className="text-xs text-[var(--color-text-secondary)]">Kotlin static analysis</div>
            </div>
            <div className="text-center">
              <div className="text-lg font-bold text-primary-500">Gitleaks</div>
              <div className="text-xs text-[var(--color-text-secondary)]">Secret and credential detection</div>
            </div>
            <div className="text-center">
              <div className="text-lg font-bold text-primary-500">OWASP</div>
              <div className="text-xs text-[var(--color-text-secondary)]">Dependency check for known CVEs</div>
            </div>
            <div className="text-center">
              <div className="text-lg font-bold text-primary-500">CodeQL</div>
              <div className="text-xs text-[var(--color-text-secondary)]">Semantic code analysis</div>
            </div>
          </div>
        </div>
      </section>

      {/* UI/UX Automation Testing */}
      <section className="mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-4">UI/UX Automation Testing</h2>
        <div className="card">
          <p className="text-[var(--color-text-secondary)] leading-relaxed mb-4">
            Yole validates complete user flows across all three runtime platforms using
            recorded UI automation. Each flow runs at three speed modes (slow, normal, fast)
            to catch animation timing issues, functional regressions, and race conditions
            under rapid interaction. Every test produces a video recording for visual
            verification and CI artifact review.
          </p>
          <p className="text-[var(--color-text-secondary)] leading-relaxed mb-4">
            The framework uses platform-specific adapters: xdotool + FFmpeg for Desktop,
            Playwright CDP with recordVideo for Web, and ADB CLI with screenrecord for
            Android. 36 challenges cover app launch, file browser navigation, document
            creation and editing, theme switching, settings, preview mode, keyboard
            shortcuts, responsive layout, offline operation, rotation handling, and share
            intents.
          </p>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-4">
            <div className="text-center">
              <div className="text-2xl font-bold text-primary-500">Desktop</div>
              <div className="text-sm text-[var(--color-text-secondary)] mb-2">12 flows</div>
              <div className="text-xs text-[var(--color-text-secondary)]">Compose Desktop + xdotool + FFmpeg</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-primary-500">Web</div>
              <div className="text-sm text-[var(--color-text-secondary)] mb-2">12 flows</div>
              <div className="text-xs text-[var(--color-text-secondary)]">Wasm PWA + Playwright + recordVideo</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-primary-500">Android</div>
              <div className="text-sm text-[var(--color-text-secondary)] mb-2">12 flows</div>
              <div className="text-xs text-[var(--color-text-secondary)]">ADB CLI + screenrecord</div>
            </div>
          </div>
          <div className="grid grid-cols-3 gap-4 mt-6">
            <div className="text-center">
              <div className="text-lg font-bold text-primary-500">Slow</div>
              <div className="text-xs text-[var(--color-text-secondary)]">800-1200ms delay, animation verification</div>
            </div>
            <div className="text-center">
              <div className="text-lg font-bold text-primary-500">Normal</div>
              <div className="text-xs text-[var(--color-text-secondary)]">300-500ms delay, functional validation</div>
            </div>
            <div className="text-center">
              <div className="text-lg font-bold text-primary-500">Fast</div>
              <div className="text-xs text-[var(--color-text-secondary)]">50-100ms delay, race condition detection</div>
            </div>
          </div>
        </div>
      </section>

      {/* Key Dependencies */}
      <section className="mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-4">Key Dependencies</h2>
        <div className="card overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[var(--color-border)]">
                <th className="text-left py-3 pr-4 font-semibold">Library</th>
                <th className="text-left py-3 pr-4 font-semibold">Version</th>
                <th className="text-left py-3 font-semibold">Purpose</th>
              </tr>
            </thead>
            <tbody>
              {keyDependencies.map((dep) => (
                <tr key={dep.name} className="border-b border-[var(--color-border)] last:border-0">
                  <td className="py-2.5 pr-4 font-medium">{dep.name}</td>
                  <td className="py-2.5 pr-4">
                    <code className="text-xs px-2 py-0.5 rounded bg-[var(--color-bg-secondary)] border border-[var(--color-border)]">
                      {dep.version}
                    </code>
                  </td>
                  <td className="py-2.5 text-[var(--color-text-secondary)]">{dep.purpose}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* Testing */}
      <section className="mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-4">Testing</h2>
        <div className="card">
          <p className="text-[var(--color-text-secondary)] leading-relaxed mb-4">
            Yole has a comprehensive test suite of 10,000+ tests across 215+ test files.
            Tests cover all 17 format parsers, network protocol clients, authentication
            flows, rate limiting, concurrency, UI components, stress testing, monitoring
            metrics, fuzz testing, snapshot testing, and load testing.
          </p>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-4">
            <div className="text-center">
              <div className="text-2xl font-bold text-primary-500">10,000+</div>
              <div className="text-xs text-[var(--color-text-secondary)]">Total Tests</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-primary-500">215+</div>
              <div className="text-xs text-[var(--color-text-secondary)]">Test Files</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-primary-500">63%</div>
              <div className="text-xs text-[var(--color-text-secondary)]">Line Coverage</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-primary-500">4</div>
              <div className="text-xs text-[var(--color-text-secondary)]">Platform Targets</div>
            </div>
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="text-center">
        <h2 className="section-heading">Explore the Code</h2>
        <p className="text-lg text-[var(--color-text-secondary)] max-w-2xl mx-auto mb-8">
          Yole is fully open source. Explore the codebase, contribute modules, or use
          the extracted KMP libraries in your own projects.
        </p>
        <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
          <a
            href="https://github.com/vasic-digital/Yole"
            target="_blank"
            rel="noopener noreferrer"
            className="btn-primary"
          >
            View on GitHub
          </a>
          <a href="/community" className="btn-secondary">
            Contributing Guide
          </a>
        </div>
      </section>
    </div>
  );
}
