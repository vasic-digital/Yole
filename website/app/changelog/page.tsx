// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2025 Milos Vasic

const releases = [
  {
    version: "2.19.0",
    date: "2026-03-26",
    title: "Concurrency Safety, Dead Code Elimination, and 36-Episode Video Course",
    highlights: [
      "Concurrency safety hardening: @Volatile on all shared mutable fields across WebDAV, Git, and DesktopSecureStorage; enforced lock ordering convention (scopeMutex > stateMutex > operationsMutex > syncMutex > cacheMutex); HttpTimeout configuration for all protocol clients",
      "Dead code elimination: all stubs, mocks, and unwired code removed across all platforms; SmbService.listFiles wired to real SmbProtocolClient",
      "Security hardening: SecurityEventLogger structured audit trail with 10 event types, 4 severity levels, and ring buffer; path traversal deep tests and OAuth2 edge case coverage",
      "581 new tests bringing desktop total to 8,928; all tests passing, 0 failures",
      "5 new video course episodes (32-36): Concurrency Safety Patterns, Security Scanning Pipeline, Stress Testing and Performance Monitoring, Challenge-Driven Development, Project Completion and Quality Gates",
      "Video course now 36 episodes total covering beginner through production-grade KMP development",
      "Go ecosystem documentation complete: DocProcessor, LLMOrchestrator, VisionEngine, and HelixQA fully documented with API references and architecture guides",
      "ProGuard disabled for desktop release build (Java 21 unsupported); build warnings suppressed",
      "KMP module verification: all 10 modules present in composite builds with substantive CHANGELOG.md and CONTRIBUTING.md",
    ],
    breaking: false,
  },
  {
    version: "2.18.0",
    date: "2026-03-19",
    title: "Autonomous QA, Non-Blocking Architecture, and 10,000+ Tests",
    highlights: [
      "10,000+ tests across Kotlin (6,695+ desktop) and Go (HelixQA 458+, DocProcessor 219, LLMOrchestrator 247, VisionEngine 262) test suites",
      "18 format types: 17 text formats plus binary detection with media preview",
      "31 video course modules (added Non-Blocking Architecture, Test Coverage Mastery, Performance Optimization Advanced, Autonomous QA, Project Completion Guide)",
      "HelixQA expanded to 12 packages with SessionCoordinator, Navigator, IssueDetector, and evidence collection",
      "3 new sibling Go modules: DocProcessor (6 packages), LLMOrchestrator (5 packages), VisionEngine (5 packages)",
      "UI/UX automation testing: 36 recorded challenges across Desktop, Web, and Android with speed mode testing",
      "IDE-style UI redesign for all 3 active platforms (Android, Desktop, Web)",
      "ParserRegistry crash fix, AGP 8.9.0 alignment, minSdk 24",
      "Comprehensive 9-phase project completion plan designed and executed",
      "Website and video course updated with latest metrics and new episodes",
    ],
    breaking: false,
  },
  {
    version: "2.17.0",
    date: "2026-03-17",
    title: "Concurrency Safety, Security Pipeline, and Comprehensive Completion",
    highlights: [
      "Concurrency safety hardening: Mutex guards, @Volatile, StateFlow.update{}, Semaphore-based connection limiters across all 8 protocol services",
      "6-tool security scanning pipeline: SonarQube, Snyk, Detekt, Gitleaks, OWASP Dependency Check, CodeQL",
      "Circuit breaker and connection limiter resilience patterns in all protocol services",
      "Path traversal protection via centralized normalizePath() in PathUtils.kt",
      "9,400+ tests across 215+ test files covering 16 test types",
      "25 video course episodes (added Concurrency Safety, Security Scanning, Performance Optimization, Complete Test Coverage)",
      "User manuals for Android, Desktop, and Web platforms",
      "Updated website with concurrency, security, and resilience documentation",
      "21+ challenge banks for security, format edge cases, and protocol resilience",
      "FormatRegistry lazy initialization and StyleSheets caching for performance",
    ],
    breaking: false,
  },
  {
    version: "2.16.0",
    date: "2026-03-16",
    title: "Comprehensive Audit, Stress Testing, and Documentation Overhaul",
    highlights: [
      "11,000+ tests across 225+ test files -- up from 9,400+",
      "Added stress, fuzz, snapshot, load, and monitoring test suites",
      "Performance baselines and monitoring metrics layer",
      "Platform-specific tests for Desktop and Wasm targets",
      "21 video course modules (added Platform-Specific Development and Stress Testing)",
      "Troubleshooting guide, Release Process, and Architecture Decision Records",
      "CODE_OF_CONDUCT.md added to the repository",
      "Website audit: updated all pages with latest counts and documentation links",
      "FAQ expanded with AGP version mismatch and container OOM guidance",
      "Comprehensive changelog with Session 4 audit phases",
    ],
    breaking: false,
  },
  {
    version: "2.15.1",
    date: "2026-03-07",
    title: "Kotlin Multiplatform Migration Complete",
    highlights: [
      "Complete Kotlin Multiplatform migration with 4 platform targets",
      "10 extracted KMP modules with independent versioning",
      "9,400+ tests across 195 test files with 63% line coverage",
      "Mock HTTP tests for all 8 cloud storage protocol services",
      "Comprehensive decoupling of shared business logic",
      "Facade bridges for backward compatibility during transition",
      "Go-based Challenges testing framework integrated",
      "Docker/Podman containerized build environment",
    ],
    breaking: false,
  },
  {
    version: "2.11",
    date: "2023-10-11",
    title: "AsciiDoc, CSV and Org-Mode, Todo.txt Advanced Search, Line Numbers",
    highlights: [
      "New format: AsciiDoc with editor syntax highlighting and preview",
      "New format: CSV with column colorization, HTML table preview, and PDF export",
      "New format: Org-Mode with editor syntax highlighting and action buttons",
      "Line number support in editor and view mode",
      "Todo.txt advanced search system with complex queries",
      "View mode: open Image, Video, Audio files in Yole",
      "Simplified permissions and better scoped storage support",
      "Various architectural improvements",
    ],
    breaking: false,
  },
  {
    version: "2.10",
    date: "2022-07-16",
    title: "Custom File Templates, Share Into Tracking Removal",
    highlights: [
      "Custom file templates using snippets folder",
      "Automatically remove tracking and analytics parameters from shared URLs",
      "Search dialog stays open for in-content search results browsing",
      "Improved dialog constraints and OK button logic",
      "Support for UTF-8 with BOM in file management",
      "Hide generated files from browser",
      "Increased editor performance with chunked operations",
      "More deterministic save/resume behavior",
    ],
    breaking: false,
  },
  {
    version: "2.9",
    date: "2022-05-14",
    title: "Snippets, Templates, Graphs, Charts, Diagrams, YAML Front-matter, Chemistry",
    highlights: [
      "Custom snippets and templates via .app/snippets directory",
      "Admonition extension for colored block-styled side content",
      "YAML front-matter display in Markdown view mode",
      "Mermaid.js support for graphs, charts, and diagrams",
      "Chemistry notation support",
      "Improved editor performance",
    ],
    breaking: false,
  },
  {
    version: "2.8",
    date: "2021-12-01",
    title: "Multi-selection for todo.txt Dialogs",
    highlights: [
      "Multi-selection for todo.txt filter and sort dialogs",
      "Improved todo.txt dialog UI and interaction patterns",
      "Performance optimizations for large todo.txt files",
      "Bug fixes and stability improvements",
    ],
    breaking: false,
  },
  {
    version: "2.7",
    date: "2021-09-01",
    title: "Search in Content, Backup and Restore Settings",
    highlights: [
      "Search in file content across notebooks",
      "Backup and restore application settings",
      "Improved file browser navigation",
      "Editor performance improvements",
      "Bug fixes for various edge cases",
    ],
    breaking: false,
  },
  {
    version: "2.6",
    date: "2021-06-01",
    title: "Zim Wiki, Newline = New Paragraph, Save Format",
    highlights: [
      "Zim Wiki format improvements",
      "Option to treat newline as new paragraph in Markdown",
      "Save format selection for documents",
      "Improved formatting toolbar",
      "Various stability improvements",
    ],
    breaking: false,
  },
  {
    version: "2.5",
    date: "2021-03-01",
    title: "Zim Wiki, Search and Replace, Zettelkasten",
    highlights: [
      "Zim Wiki format support",
      "Search and replace functionality",
      "Zettelkasten ID generation and linking",
      "Improved wiki link handling",
      "Editor usability improvements",
    ],
    breaking: false,
  },
  {
    version: "2.4",
    date: "2020-12-01",
    title: "All New todo.txt, Programming Language Syntax Highlighting",
    highlights: [
      "Completely rewritten todo.txt parser and editor",
      "Programming language syntax highlighting in code blocks",
      "Improved todo.txt priority and date handling",
      "Better code block rendering in Markdown preview",
      "Performance improvements for large files",
    ],
    breaking: false,
  },
  {
    version: "2.3",
    date: "2020-09-01",
    title: "Table of Contents, Custom Action Order",
    highlights: [
      "Auto-generated table of contents for Markdown",
      "Customizable action button order",
      "Improved heading detection and navigation",
      "Editor toolbar customization",
      "Various bug fixes",
    ],
    breaking: false,
  },
  {
    version: "2.2",
    date: "2020-06-01",
    title: "Presentations, Voice Notes, Markdown Table Editor",
    highlights: [
      "Presentation mode for Markdown documents",
      "Voice note recording and insertion",
      "Interactive Markdown table editor",
      "Improved slide generation from headings",
      "Audio file handling improvements",
    ],
    breaking: false,
  },
  {
    version: "2.1",
    date: "2020-03-01",
    title: "Key-Value Highlighting, Improved Performance",
    highlights: [
      "Syntax highlighting for JSON, INI, YAML, and CSV",
      "Key-value format detection and parsing",
      "Significant performance improvements",
      "Reduced memory usage for large files",
      "Editor responsiveness improvements",
    ],
    breaking: false,
  },
  {
    version: "2.0",
    date: "2019-12-01",
    title: "Search, dotFiles, PDF Export",
    highlights: [
      "Full-text search across all files",
      "dotFiles visibility toggle",
      "PDF export for all formats",
      "Major UI overhaul",
      "Improved file management",
    ],
    breaking: true,
  },
  {
    version: "1.8",
    date: "2019-09-01",
    title: "All New File Browser, Favourites and Faster Markdown Preview",
    highlights: [
      "Completely redesigned file browser",
      "Favourites system for quick access",
      "Faster Markdown preview rendering",
      "Improved file sorting and filtering",
      "Navigation history",
    ],
    breaking: false,
  },
  {
    version: "1.7",
    date: "2019-06-01",
    title: "Custom Fonts, LinkBox with Markdown",
    highlights: [
      "Custom font selection for editor",
      "LinkBox files with Markdown rendering",
      "Font size adjustment",
      "Improved link handling",
    ],
    breaking: false,
  },
  {
    version: "1.6",
    date: "2019-03-01",
    title: "DateTime Dialog, Jekyll and KaTeX Improvements",
    highlights: [
      "DateTime insertion dialog",
      "Jekyll front-matter support improvements",
      "KaTeX math rendering enhancements",
      "Improved date formatting options",
    ],
    breaking: false,
  },
  {
    version: "1.5",
    date: "2018-12-01",
    title: "Multiple Windows, Markdown Tasks, Theming",
    highlights: [
      "Multiple window support",
      "Markdown task list editing",
      "Theme customization",
      "Dark mode support",
      "Improved multi-tasking",
    ],
    breaking: false,
  },
  {
    version: "1.2",
    date: "2018-06-01",
    title: "Markdown with KaTeX/Math, Search in Current Document",
    highlights: [
      "KaTeX math expression support in Markdown",
      "In-document search functionality",
      "Math equation rendering",
      "Improved search highlighting",
    ],
    breaking: false,
  },
  {
    version: "1.1",
    date: "2018-03-01",
    title: "Markdown Picture Import from Gallery and Camera",
    highlights: [
      "Insert images from gallery into Markdown",
      "Camera capture and insert into documents",
      "Image path handling improvements",
      "Markdown image syntax auto-generation",
    ],
    breaking: false,
  },
  {
    version: "1.0",
    date: "2017-12-01",
    title: "Widget Shortcuts to LinkBox, ToDo, QuickNote",
    highlights: [
      "Home screen widget shortcuts",
      "LinkBox for link collection",
      "ToDo quick access widget",
      "QuickNote for rapid note-taking",
      "Initial public release",
    ],
    breaking: false,
  },
  {
    version: "0.3",
    date: "2017-09-01",
    title: "Faster Loading, LinkBox Added, Open Link in Browser",
    highlights: [
      "Improved app startup time",
      "LinkBox feature for managing links",
      "Open link in browser text action",
      "Initial beta release",
    ],
    breaking: false,
  },
];

export default function ChangelogPage() {
  return (
    <div className="container-page py-16">
      <h1 className="text-4xl font-bold tracking-tight mb-4">Changelog</h1>
      <p className="text-lg text-[var(--color-text-secondary)] mb-12 max-w-3xl">
        Release history for Yole. Each version includes new features, improvements,
        and bug fixes. See the{" "}
        <a
          href="https://github.com/vasic-digital/Yole/releases"
          target="_blank"
          rel="noopener noreferrer"
          className="text-primary-500 hover:text-primary-600"
        >
          GitHub releases
        </a>{" "}
        for full details and download links.
      </p>

      {/* Release Timeline */}
      <div className="space-y-8">
        {releases.map((release, index) => (
          <section key={release.version} id={`v${release.version}`} className="scroll-mt-24">
            <div className="card">
              <div className="flex flex-wrap items-center gap-3 mb-4">
                <h2 className="text-2xl font-bold">v{release.version}</h2>
                <span className="text-sm text-[var(--color-text-secondary)]">
                  {release.date}
                </span>
                {index === 0 && (
                  <span className="text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400 font-medium">
                    Latest
                  </span>
                )}
                {release.breaking && (
                  <span className="text-xs px-2 py-0.5 rounded-full bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400 font-medium">
                    Breaking Changes
                  </span>
                )}
              </div>
              <h3 className="font-semibold text-lg text-primary-500 mb-4">{release.title}</h3>
              <ul className="space-y-2">
                {release.highlights.map((highlight) => (
                  <li
                    key={highlight}
                    className="text-sm text-[var(--color-text-secondary)] flex items-start gap-2"
                  >
                    <span className="text-primary-500 mt-0.5 flex-shrink-0">-</span>
                    {highlight}
                  </li>
                ))}
              </ul>
            </div>
          </section>
        ))}
      </div>

      {/* CTA */}
      <section className="mt-16 text-center">
        <h2 className="section-heading">Stay Updated</h2>
        <p className="text-lg text-[var(--color-text-secondary)] max-w-2xl mx-auto mb-8">
          Follow the project on GitHub to get notified about new releases,
          or check the full release notes for detailed changelogs.
        </p>
        <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
          <a
            href="https://github.com/vasic-digital/Yole/releases"
            target="_blank"
            rel="noopener noreferrer"
            className="btn-primary"
          >
            View All Releases
          </a>
          <a href="/download" className="btn-secondary">
            Download Latest
          </a>
        </div>
      </section>
    </div>
  );
}
