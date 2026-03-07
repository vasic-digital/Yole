// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2025 Milos Vasic

const formats = [
  { name: "Markdown", icon: "M", description: "Full GFM support with tables, task lists, footnotes, and math", id: "markdown" },
  { name: "todo.txt", icon: "T", description: "Task management with priorities, projects, contexts, and due dates", id: "todotxt" },
  { name: "CSV / TSV", icon: "C", description: "Spreadsheet-style editing with column alignment and sorting", id: "csv" },
  { name: "LaTeX", icon: "L", description: "Scientific typesetting with math equations and bibliography", id: "latex" },
  { name: "Org Mode", icon: "O", description: "Hierarchical notes with TODO tracking and agenda views", id: "orgmode" },
  { name: "reStructuredText", icon: "R", description: "Python documentation format with directives and roles", id: "rst" },
  { name: "AsciiDoc", icon: "A", description: "Technical documentation with includes and conditional content", id: "asciidoc" },
  { name: "Textile", icon: "X", description: "Lightweight markup with inline formatting and block elements", id: "textile" },
  { name: "WikiText", icon: "W", description: "MediaWiki-compatible syntax with templates and links", id: "wikitext" },
  { name: "TiddlyWiki", icon: "D", description: "Non-linear personal wiki with transclusion support", id: "tiddlywiki" },
  { name: "Creole", icon: "E", description: "Standardized wiki markup with a common syntax across wiki engines", id: "creole" },
  { name: "Key-Value", icon: "K", description: "Configuration files, .properties, and .env formats", id: "keyvalue" },
  { name: "Jupyter", icon: "J", description: "Interactive notebook format with code cells and outputs", id: "jupyter" },
  { name: "TaskPaper", icon: "P", description: "Plain-text task management with projects and tags", id: "taskpaper" },
  { name: "R Markdown", icon: "R", description: "Data science documents combining R code, output, and narrative", id: "rmarkdown" },
  { name: "Plain Text", icon: "#", description: "Simple plain text editing with line numbers and word wrap", id: "plaintext" },
  { name: "Binary", icon: "B", description: "Binary file detection with media preview for images, audio, and video", id: "binary" },
];

const platforms = [
  { name: "Android", status: "Production", icon: "phone", description: "Full-featured editor on Google Play" },
  { name: "Desktop", status: "Beta", icon: "desktop", description: "Windows, macOS, and Linux via JVM" },
  { name: "iOS", status: "Development", icon: "tablet", description: "Coming soon for iPhone and iPad" },
  { name: "Web", status: "Development", icon: "globe", description: "Browser-based editor via WebAssembly" },
];

const stats = [
  { label: "Tests", value: "9,300+" },
  { label: "Text Formats", value: "17" },
  { label: "Protocols", value: "8" },
  { label: "Platforms", value: "4" },
];

const features = [
  { title: "17 Text Formats", description: "From Markdown and todo.txt to LaTeX and Jupyter notebooks. One editor for all your text files." },
  { title: "Offline-First", description: "Everything works without an internet connection. Your data stays on your device by default." },
  { title: "8 Cloud Protocols", description: "Optional integration with Dropbox, Google Drive, OneDrive, WebDAV, FTP, SFTP, Git, and S3." },
  { title: "Cross-Platform", description: "Built with Kotlin Multiplatform. Write once, run on Android, Desktop, iOS, and Web." },
  { title: "Open Source", description: "Apache-2.0 licensed. Inspect the code, contribute, and build on top of it." },
  { title: "Syntax Highlighting", description: "Context-aware highlighting for every supported format with customizable themes." },
  { title: "Modular Architecture", description: "10 independently extracted KMP modules. Clean separation of concerns with facade bridges." },
  { title: "Comprehensive Testing", description: "9,300+ tests across 190+ test files with 63% line coverage. Mock HTTP tests for all protocols." },
  { title: "Video Course", description: "19 free video tutorials from beginner to expert. Learn Yole at your own pace." },
];

export default function HomePage() {
  return (
    <>
      {/* Hero Section */}
      <section className="py-20 md:py-32">
        <div className="container-page text-center">
          <h1 className="text-5xl md:text-7xl font-extrabold tracking-tight mb-6">
            <span className="text-primary-500">Yole</span>
          </h1>
          <p className="text-xl md:text-2xl text-[var(--color-text-secondary)] max-w-3xl mx-auto mb-4">
            Cross-Platform Text Editor
          </p>
          <p className="text-lg text-[var(--color-text-secondary)] max-w-2xl mx-auto mb-10">
            A powerful editor supporting 17 text formats and 8 cloud storage protocols.
            Offline-first with optional cloud sync. Built with Kotlin Multiplatform for
            Android, Desktop, iOS, and Web.
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4 mb-12">
            <a href="/download" className="btn-primary text-lg px-8 py-4">
              Download Yole
            </a>
            <a href="/video-course" className="btn-secondary text-lg px-8 py-4">
              Video Course
            </a>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 max-w-2xl mx-auto">
            {stats.map((stat) => (
              <div key={stat.label} className="text-center">
                <div className="text-2xl md:text-3xl font-bold text-primary-500">{stat.value}</div>
                <div className="text-sm text-[var(--color-text-secondary)]">{stat.label}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-20 bg-[var(--color-bg-secondary)]">
        <div className="container-page">
          <h2 className="section-heading text-center">Why Yole?</h2>
          <p className="section-subheading text-center">
            Built for writers, developers, and anyone who works with text.
          </p>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {features.map((feature) => (
              <div key={feature.title} className="card">
                <h3 className="font-semibold text-lg mb-2">{feature.title}</h3>
                <p className="text-sm text-[var(--color-text-secondary)]">{feature.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Format Showcase */}
      <section className="py-20">
        <div className="container-page">
          <h2 className="section-heading text-center">17 Text Formats</h2>
          <p className="section-subheading text-center">
            One editor for Markdown, todo.txt, CSV, LaTeX, and many more.
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {formats.map((format) => (
              <a
                key={format.id}
                href={`/formats#${format.id}`}
                className="card flex items-start gap-4 group"
              >
                <div className="flex-shrink-0 w-10 h-10 rounded-lg bg-primary-500/10 text-primary-500 flex items-center justify-center font-bold text-lg">
                  {format.icon}
                </div>
                <div className="min-w-0">
                  <h3 className="font-semibold group-hover:text-primary-500 transition-colors">
                    {format.name}
                  </h3>
                  <p className="text-xs text-[var(--color-text-secondary)] mt-1 line-clamp-2">
                    {format.description}
                  </p>
                </div>
              </a>
            ))}
          </div>
        </div>
      </section>

      {/* Platform Downloads */}
      <section className="py-20 bg-[var(--color-bg-secondary)]">
        <div className="container-page">
          <h2 className="section-heading text-center">Available Everywhere</h2>
          <p className="section-subheading text-center">
            One codebase, multiple platforms. Download for your device.
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {platforms.map((platform) => (
              <div key={platform.name} className="card text-center">
                <div className="w-12 h-12 rounded-full bg-primary-500/10 text-primary-500 flex items-center justify-center mx-auto mb-4">
                  <span className="text-xl font-bold">{platform.name[0]}</span>
                </div>
                <h3 className="font-semibold text-lg">{platform.name}</h3>
                <span className={`inline-block mt-1 text-xs px-2 py-0.5 rounded-full ${
                  platform.status === "Production"
                    ? "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400"
                    : platform.status === "Beta"
                    ? "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400"
                    : "bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400"
                }`}>
                  {platform.status}
                </span>
                <p className="text-sm text-[var(--color-text-secondary)] mt-3">
                  {platform.description}
                </p>
              </div>
            ))}
          </div>
          <div className="text-center mt-10">
            <a href="/download" className="btn-primary">
              View All Downloads
            </a>
          </div>
        </div>
      </section>

      {/* Explore Section */}
      <section className="py-20">
        <div className="container-page">
          <h2 className="section-heading text-center">Explore Yole</h2>
          <p className="section-subheading text-center">
            Dive deeper into formats, cloud storage, architecture, and more.
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            <a href="/formats" className="card text-center group">
              <h3 className="font-semibold text-lg mb-2 group-hover:text-primary-500 transition-colors">Formats</h3>
              <p className="text-sm text-[var(--color-text-secondary)]">17 text formats with syntax examples and use cases.</p>
            </a>
            <a href="/cloud-storage" className="card text-center group">
              <h3 className="font-semibold text-lg mb-2 group-hover:text-primary-500 transition-colors">Cloud Storage</h3>
              <p className="text-sm text-[var(--color-text-secondary)]">8 protocols with feature comparison and setup guides.</p>
            </a>
            <a href="/video-course" className="card text-center group">
              <h3 className="font-semibold text-lg mb-2 group-hover:text-primary-500 transition-colors">Video Course</h3>
              <p className="text-sm text-[var(--color-text-secondary)]">19 free videos from beginner to expert level.</p>
            </a>
            <a href="/architecture" className="card text-center group">
              <h3 className="font-semibold text-lg mb-2 group-hover:text-primary-500 transition-colors">Architecture</h3>
              <p className="text-sm text-[var(--color-text-secondary)]">KMP modules, parsing pipeline, and system design.</p>
            </a>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 bg-[var(--color-bg-secondary)]">
        <div className="container-page text-center">
          <h2 className="section-heading">Open Source and Free</h2>
          <p className="text-lg text-[var(--color-text-secondary)] max-w-2xl mx-auto mb-8">
            Yole is licensed under Apache-2.0. Contribute on GitHub, report issues,
            or build your own integrations.
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
              Join the Community
            </a>
          </div>
        </div>
      </section>
    </>
  );
}
