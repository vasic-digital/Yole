// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2025 Milos Vasic

const releases = [
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
