// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2025 Milos Vasic

const faqCategories = [
  {
    id: "general",
    name: "General",
    questions: [
      {
        q: "What is Yole?",
        a: "Yole is a cross-platform text editor built with Kotlin Multiplatform. It supports 17+ text formats including Markdown, todo.txt, CSV, LaTeX, and more. It is offline-first with optional cloud storage integration and runs on Android, Desktop (Windows, macOS, Linux), iOS, and Web.",
      },
      {
        q: "Is Yole free?",
        a: "Yes. Yole is completely free and open source, licensed under Apache-2.0. There are no premium features, no ads, and no tracking. You can use it for personal or commercial purposes.",
      },
      {
        q: "What platforms does Yole support?",
        a: "Yole runs on Android (Production), Desktop via JVM on Windows, macOS, and Linux (Beta), iOS (In Development), and Web via WebAssembly (In Development). All platforms share the same core codebase through Kotlin Multiplatform.",
      },
      {
        q: "How is Yole different from other text editors?",
        a: "Yole is specifically designed for structured text formats. Unlike general-purpose editors, it understands the syntax and structure of 17+ text formats, providing format-specific parsing, previewing, and editing features. It also works offline-first and runs on all major platforms from a single codebase.",
      },
      {
        q: "What language is Yole built with?",
        a: "Yole is built primarily with Kotlin using Kotlin Multiplatform (KMP). The shared business logic, all 17 format parsers, and network protocol clients are written in Kotlin and compiled for JVM (Android/Desktop), Native (iOS), and Wasm (Web). The UI uses Compose Multiplatform.",
      },
    ],
  },
  {
    id: "file-management",
    name: "File Management",
    questions: [
      {
        q: "Where are my files stored?",
        a: "By default, files are stored locally on your device. On Android, they are in the app's storage directory or any user-accessible folder. On Desktop, you can open files from any location on your file system. Cloud storage is optional and must be explicitly configured.",
      },
      {
        q: "How does Yole detect file formats?",
        a: "Yole uses a two-stage detection system. First, it checks the file extension against known extensions for each format (e.g., .md for Markdown, .tex for LaTeX). If the extension is ambiguous, it falls back to content-based detection using regex patterns defined for each format.",
      },
      {
        q: "Can I open files from cloud storage?",
        a: "Yes. Yole integrates with Dropbox, Google Drive, OneDrive, WebDAV, FTP, SFTP, and Git. You can browse remote files, open them for editing, and sync changes back. All cloud features are optional -- Yole works fully offline.",
      },
      {
        q: "Does Yole support file encryption?",
        a: "Yole does not encrypt files at rest by default. However, it uses secure storage for authentication credentials (OAuth2 tokens, passwords) through the Security-KMP module. For file encryption, we recommend using your operating system's built-in encryption or third-party tools.",
      },
      {
        q: "What file encodings are supported?",
        a: "Yole supports UTF-8 (default), UTF-8 with BOM, and other common text encodings. The encoding is auto-detected when opening files. Line endings (LF, CRLF, CR) are preserved by default.",
      },
    ],
  },
  {
    id: "formats",
    name: "Formats",
    questions: [
      {
        q: "Which text formats does Yole support?",
        a: "Yole supports 17 text formats: Markdown (GFM), todo.txt, CSV/TSV, LaTeX, Org Mode, reStructuredText, AsciiDoc, Textile, WikiText, TiddlyWiki, Creole, Key-Value (.properties/.env/.ini), Jupyter notebooks, TaskPaper, R Markdown, Binary, and Plain Text.",
      },
      {
        q: "Can I add custom formats?",
        a: "Yes, if you are a developer. Yole's format system is extensible. You can implement a new parser that produces a ParsedDocument, register it in the FormatRegistry, and add tests. See the Contributing guide and the Architecture page for details on adding new format parsers.",
      },
      {
        q: "Does Yole support Markdown extensions?",
        a: "Yes. Yole's Markdown parser supports GitHub Flavored Markdown (GFM) with tables, task lists, strikethrough, footnotes, math expressions (KaTeX), code blocks with syntax highlighting, admonition blocks, Mermaid diagrams, and YAML front-matter.",
      },
      {
        q: "Can I preview formatted documents?",
        a: "Yes. Yole generates HTML previews for all supported formats. You can switch between edit mode (with syntax highlighting) and preview mode (formatted HTML output). The preview supports light and dark themes.",
      },
      {
        q: "Does Yole support syntax highlighting for code blocks?",
        a: "Yes. Code blocks in Markdown and other formats are rendered with syntax highlighting in the preview. The editor also provides context-aware syntax highlighting for the format you are editing.",
      },
    ],
  },
  {
    id: "cloud-storage",
    name: "Cloud Storage",
    questions: [
      {
        q: "Which cloud storage providers are supported?",
        a: "Yole supports Dropbox, Google Drive, OneDrive, WebDAV (Nextcloud, ownCloud, etc.), FTP, SFTP, Git repositories, and S3-compatible storage (planned). Each protocol can be configured independently in the settings.",
      },
      {
        q: "Is cloud storage required?",
        a: "No. Yole is offline-first by design. All features work without any internet connection. Cloud storage is entirely optional and must be explicitly configured. Your files stay on your device by default.",
      },
      {
        q: "How does offline sync work?",
        a: "When you connect to a cloud provider, Yole caches files locally for offline access. Edits are saved locally first and synced to the cloud when a connection is available. Conflict resolution handles cases where files are modified in multiple places.",
      },
      {
        q: "Is my data secure when using cloud storage?",
        a: "All OAuth2-based connections (Dropbox, Google Drive, OneDrive) use industry-standard authentication and TLS encryption. SFTP uses SSH encryption. Credentials are stored securely using the platform's secure storage (Keystore on Android, Keychain on iOS/macOS). FTP is unencrypted -- use SFTP instead for sensitive data.",
      },
      {
        q: "Can I connect to a self-hosted server?",
        a: "Yes. WebDAV supports any WebDAV-compatible server including Nextcloud and ownCloud. FTP and SFTP work with any standard server. Git supports any Git hosting provider. You can configure custom hostnames, ports, and paths.",
      },
    ],
  },
  {
    id: "development",
    name: "Development",
    questions: [
      {
        q: "How do I build Yole from source?",
        a: "Clone the repository, initialize submodules with 'git submodule update --init --recursive', and use Gradle to build. For Android: './gradlew :androidApp:assembleDebug'. For Desktop: './gradlew :desktopApp:run'. For Web: './gradlew :webApp:wasmJsBrowserRun'. All builds can also be run in Docker containers.",
      },
      {
        q: "How do I run the tests?",
        a: "Run './gradlew test' to execute all tests, or './gradlew :shared:desktopTest' for shared module tests. You can run specific test classes with '--tests' flag. For containerized testing, use 'docker compose run --rm build ./docker/scripts/test-all.sh'. Yole has 8,800+ tests across 170+ test files.",
      },
      {
        q: "What is the module structure?",
        a: "Yole's shared logic is split into 10 independently extracted KMP modules: RateLimiter-KMP, Concurrency-KMP, UI-Components-KMP, Auth-KMP, Security-KMP, Document-KMP, Config-KMP, Database-KMP, Storage-KMP, and Formatters-KMP. These are consumed via Gradle composite builds.",
      },
      {
        q: "How do I contribute a new format parser?",
        a: "Create a parser directory in shared/src/commonMain/kotlin/digital/vasic/yole/format/[name]/, implement a parser that produces ParsedDocument, register it in FormatRegistry.kt, add tests in commonTest, and submit a pull request. See the Contributing guide for the full step-by-step process.",
      },
      {
        q: "What IDE should I use?",
        a: "Android Studio (latest stable) is recommended for Android development. IntelliJ IDEA works well for Desktop and Web targets. Xcode is required for iOS development. All IDEs support Kotlin and the Gradle build system used by Yole.",
      },
      {
        q: "Where can I report bugs or request features?",
        a: "Use GitHub Issues at github.com/vasic-digital/Yole/issues. Search for existing issues first to avoid duplicates. Use the bug report template for bugs and the feature request template for new feature ideas. For questions and discussions, use GitHub Discussions.",
      },
    ],
  },
];

export default function FaqPage() {
  return (
    <div className="container-page py-16">
      <h1 className="text-4xl font-bold tracking-tight mb-4">Frequently Asked Questions</h1>
      <p className="text-lg text-[var(--color-text-secondary)] mb-12 max-w-3xl">
        Answers to the most common questions about Yole. Can&apos;t find what you&apos;re looking
        for? Check{" "}
        <a
          href="https://github.com/vasic-digital/Yole/discussions"
          target="_blank"
          rel="noopener noreferrer"
          className="text-primary-500 hover:text-primary-600"
        >
          GitHub Discussions
        </a>{" "}
        or{" "}
        <a
          href="https://github.com/vasic-digital/Yole/issues"
          target="_blank"
          rel="noopener noreferrer"
          className="text-primary-500 hover:text-primary-600"
        >
          open an issue
        </a>.
      </p>

      {/* Category Navigation */}
      <div className="flex flex-wrap gap-2 mb-12">
        {faqCategories.map((category) => (
          <a
            key={category.id}
            href={`#${category.id}`}
            className="text-sm px-4 py-2 rounded-lg border border-[var(--color-border)] bg-[var(--color-bg)] hover:bg-primary-500/10 hover:border-primary-500/30 hover:text-primary-500 transition-colors font-medium"
          >
            {category.name}
            <span className="ml-1.5 text-xs text-[var(--color-text-secondary)]">
              ({category.questions.length})
            </span>
          </a>
        ))}
      </div>

      {/* FAQ Sections */}
      <div className="space-y-16">
        {faqCategories.map((category) => (
          <section key={category.id} id={category.id} className="scroll-mt-24">
            <h2 className="text-3xl font-bold tracking-tight mb-6">{category.name}</h2>
            <div className="space-y-4">
              {category.questions.map((faq, i) => (
                <details key={i} className="card group">
                  <summary className="cursor-pointer font-semibold text-lg list-none flex items-center justify-between gap-4">
                    <span>{faq.q}</span>
                    <span className="flex-shrink-0 text-primary-500 text-xl font-light group-open:rotate-45 transition-transform duration-200">
                      +
                    </span>
                  </summary>
                  <p className="mt-4 text-[var(--color-text-secondary)] leading-relaxed">
                    {faq.a}
                  </p>
                </details>
              ))}
            </div>
          </section>
        ))}
      </div>

      {/* Still Have Questions */}
      <section className="mt-16 text-center">
        <h2 className="section-heading">Still Have Questions?</h2>
        <p className="text-lg text-[var(--color-text-secondary)] max-w-2xl mx-auto mb-8">
          The community is here to help. Ask on GitHub Discussions, report issues,
          or check the documentation for detailed guides.
        </p>
        <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
          <a
            href="https://github.com/vasic-digital/Yole/discussions"
            target="_blank"
            rel="noopener noreferrer"
            className="btn-primary"
          >
            Ask a Question
          </a>
          <a href="/docs" className="btn-secondary">
            Read the Docs
          </a>
        </div>
      </section>
    </div>
  );
}
