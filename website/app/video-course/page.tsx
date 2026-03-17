// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2025 Milos Vasic

const levels = [
  {
    level: "Beginner",
    color: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
    description: "Get started with Yole. Learn the basics of the editor, file management, and your first text format.",
    prerequisites: "None",
    videos: [
      { id: 1, title: "Introduction to Yole", duration: "8 min", description: "Overview of Yole's features, supported platforms, and what makes it different from other text editors." },
      { id: 2, title: "Installation and First Launch", duration: "6 min", description: "Installing Yole on Android, Desktop, and Web. Initial setup and configuration walkthrough." },
      { id: 3, title: "File Management Basics", duration: "10 min", description: "Creating, opening, saving, and organizing text files. Understanding the file browser and favorites." },
      { id: 4, title: "Markdown Essentials", duration: "12 min", description: "Writing your first Markdown document. Headings, bold, italic, links, images, and lists." },
      { id: 5, title: "todo.txt for Task Management", duration: "10 min", description: "Managing tasks with todo.txt format. Priorities, projects, contexts, and completion tracking." },
      { id: 6, title: "Using the Editor", duration: "8 min", description: "Editor features: syntax highlighting, line numbers, word wrap, search, and formatting toolbar." },
      { id: 7, title: "Preview and Export", duration: "7 min", description: "Previewing formatted documents, switching between edit and view mode, and PDF export." },
    ],
  },
  {
    level: "Advanced",
    color: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400",
    description: "Master cloud storage integration, advanced format features, and power-user workflows.",
    prerequisites: "Beginner videos (1-7)",
    videos: [
      { id: 8, title: "Cloud Storage Setup", duration: "12 min", description: "Connecting Dropbox, Google Drive, and OneDrive. OAuth2 authentication and file browsing." },
      { id: 9, title: "WebDAV and Self-Hosted Storage", duration: "10 min", description: "Connecting to Nextcloud, ownCloud, and custom WebDAV servers. FTP and SFTP configuration." },
      { id: 10, title: "Advanced Markdown", duration: "14 min", description: "Tables, task lists, footnotes, math expressions, code blocks with syntax highlighting, and Mermaid diagrams." },
      { id: 11, title: "LaTeX and Scientific Writing", duration: "12 min", description: "Math equations, document structure, bibliography management, and cross-references." },
      { id: 12, title: "CSV Data Editing", duration: "8 min", description: "Working with spreadsheet data. Column alignment, header detection, and HTML table preview." },
      { id: 13, title: "Snippets and Templates", duration: "9 min", description: "Creating custom snippets, file templates, and automating repetitive text patterns." },
    ],
  },
  {
    level: "Expert",
    color: "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400",
    description: "Dive into the architecture, contribute to the project, and extend Yole with custom formats.",
    prerequisites: "Advanced videos (8-13), Kotlin knowledge recommended for 14-21, production experience recommended for 22-25",
    videos: [
      { id: 14, title: "Architecture Deep Dive", duration: "18 min", description: "Kotlin Multiplatform architecture, module structure, shared code organization, and platform targets." },
      { id: 15, title: "Building from Source", duration: "12 min", description: "Setting up the development environment. Building for Android, Desktop, and Web. Running the test suite." },
      { id: 16, title: "Adding a New Format Parser", duration: "20 min", description: "Step-by-step guide to implementing a new text format parser. TextParser interface, ParsedDocument, and FormatRegistry." },
      { id: 17, title: "Network Protocol Implementation", duration: "16 min", description: "Implementing a new cloud storage protocol. NetworkStorageService interface, authentication, and Ktor Client." },
      { id: 18, title: "Testing Best Practices", duration: "14 min", description: "Writing comprehensive tests with Kotest. Mock HTTP testing, edge cases, stress tests, and code coverage." },
      { id: 19, title: "Contributing to Yole", duration: "10 min", description: "Git workflow, commit conventions, pull request process, code review, and community guidelines." },
      { id: 20, title: "Platform-Specific Development", duration: "16 min", description: "Implementing expect/actual declarations for Android, Desktop, iOS, and Wasm. Platform-specific UI, networking, and secure storage." },
      { id: 21, title: "Stress Testing and Monitoring", duration: "14 min", description: "Writing stress tests, fuzz tests, load tests, and snapshot tests. Performance baselines, monitoring metrics, and CI integration." },
      { id: 22, title: "Concurrency Safety Patterns", duration: "16 min", description: "Thread safety with Mutex, Semaphore, @Volatile, and StateFlow.update{}. Protecting shared state in protocol services and preventing race conditions." },
      { id: 23, title: "Security Scanning Deep Dive", duration: "18 min", description: "Setting up and configuring SonarQube, Snyk, Detekt, Gitleaks, OWASP Dependency Check, and CodeQL. Interpreting scan results and fixing vulnerabilities." },
      { id: 24, title: "Performance Optimization", duration: "14 min", description: "FormatRegistry lazy initialization, StyleSheets caching, DocumentCache LRU strategy, and parsing throughput benchmarks. Profiling and optimization techniques." },
      { id: 25, title: "Complete Test Coverage", duration: "16 min", description: "Achieving comprehensive coverage with 16 test types: unit, integration, stress, fuzz, snapshot, load, E2E, accessibility, non-blocking, and more. Test organization and CI best practices." },
    ],
  },
];

const totalVideos = levels.reduce((sum, level) => sum + level.videos.length, 0);
const totalDuration = levels.reduce((sum, level) =>
  sum + level.videos.reduce((vSum, v) => vSum + parseInt(v.duration), 0), 0
);

export default function VideoCoursePage() {
  return (
    <div className="container-page py-16">
      <h1 className="text-4xl font-bold tracking-tight mb-4">Video Course</h1>
      <p className="text-lg text-[var(--color-text-secondary)] mb-8 max-w-3xl">
        Learn Yole from the ground up with our comprehensive video course. From basic
        editing to building custom format parsers, these videos cover everything you
        need to master Yole.
      </p>

      {/* Course Overview Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-16">
        <div className="card text-center">
          <div className="text-3xl font-bold text-primary-500 mb-1">{totalVideos}</div>
          <div className="text-sm text-[var(--color-text-secondary)]">Videos</div>
        </div>
        <div className="card text-center">
          <div className="text-3xl font-bold text-primary-500 mb-1">~{Math.round(totalDuration / 60)}h {totalDuration % 60}m</div>
          <div className="text-sm text-[var(--color-text-secondary)]">Total Duration</div>
        </div>
        <div className="card text-center">
          <div className="text-3xl font-bold text-primary-500 mb-1">3</div>
          <div className="text-sm text-[var(--color-text-secondary)]">Skill Levels</div>
        </div>
        <div className="card text-center">
          <div className="text-3xl font-bold text-primary-500 mb-1">Free</div>
          <div className="text-sm text-[var(--color-text-secondary)]">Open Access</div>
        </div>
      </div>

      {/* Course Levels */}
      {levels.map((level) => (
        <section key={level.level} className="mb-16">
          <div className="flex items-center gap-3 mb-2">
            <h2 className="text-3xl font-bold tracking-tight">{level.level}</h2>
            <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${level.color}`}>
              {level.videos.length} videos
            </span>
          </div>
          <p className="text-[var(--color-text-secondary)] mb-2 max-w-3xl">
            {level.description}
          </p>
          <p className="text-sm text-[var(--color-text-secondary)] mb-6">
            <span className="font-medium">Prerequisites:</span> {level.prerequisites}
          </p>

          <div className="space-y-4">
            {level.videos.map((video) => (
              <div key={video.id} className="card flex items-start gap-4">
                <div className="flex-shrink-0 w-12 h-12 rounded-lg bg-primary-500/10 text-primary-500 flex items-center justify-center font-bold">
                  {video.id}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-3 mb-1">
                    <h3 className="font-semibold text-lg">{video.title}</h3>
                    <span className="text-xs px-2 py-0.5 rounded-full bg-[var(--color-bg-secondary)] border border-[var(--color-border)] text-[var(--color-text-secondary)] whitespace-nowrap">
                      {video.duration}
                    </span>
                  </div>
                  <p className="text-sm text-[var(--color-text-secondary)]">{video.description}</p>
                </div>
              </div>
            ))}
          </div>
        </section>
      ))}

      {/* Learning Path */}
      <section className="mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-4">Recommended Learning Path</h2>
        <div className="card">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            <div>
              <h3 className="font-semibold text-lg mb-2 text-green-600 dark:text-green-400">Week 1: Foundations</h3>
              <p className="text-sm text-[var(--color-text-secondary)] mb-3">
                Complete the Beginner videos (1-7). Learn the editor, file management,
                and basic Markdown and todo.txt usage.
              </p>
              <p className="text-xs text-[var(--color-text-secondary)]">~61 minutes total</p>
            </div>
            <div>
              <h3 className="font-semibold text-lg mb-2 text-yellow-600 dark:text-yellow-400">Week 2: Integration</h3>
              <p className="text-sm text-[var(--color-text-secondary)] mb-3">
                Work through the Advanced videos (8-13). Set up cloud storage,
                explore advanced format features, and build custom templates.
              </p>
              <p className="text-xs text-[var(--color-text-secondary)]">~65 minutes total</p>
            </div>
            <div>
              <h3 className="font-semibold text-lg mb-2 text-red-600 dark:text-red-400">Week 3: Architecture</h3>
              <p className="text-sm text-[var(--color-text-secondary)] mb-3">
                Dive into the Expert videos (14-21). Understand the architecture,
                contribute to the project, and build custom extensions.
              </p>
              <p className="text-xs text-[var(--color-text-secondary)]">~120 minutes total</p>
            </div>
            <div>
              <h3 className="font-semibold text-lg mb-2 text-red-600 dark:text-red-400">Week 4: Mastery</h3>
              <p className="text-sm text-[var(--color-text-secondary)] mb-3">
                Complete the advanced Expert videos (22-25). Master concurrency safety,
                security scanning, performance optimization, and complete test coverage.
              </p>
              <p className="text-xs text-[var(--color-text-secondary)]">~64 minutes total</p>
            </div>
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="text-center">
        <h2 className="section-heading">Ready to Start?</h2>
        <p className="text-lg text-[var(--color-text-secondary)] max-w-2xl mx-auto mb-8">
          Download Yole and follow along with the video course.
          The entire course is free and open access.
        </p>
        <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
          <a href="/download" className="btn-primary">
            Download Yole
          </a>
          <a href="/docs" className="btn-secondary">
            Read the Docs
          </a>
        </div>
      </section>
    </div>
  );
}
