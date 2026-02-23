// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2025 Milos Vasic

const stats = [
  { label: "Text Formats", value: "17+" },
  { label: "Tests", value: "2,200+" },
  { label: "Platforms", value: "4" },
  { label: "License", value: "Apache-2.0" },
];

const timeline = [
  { year: "2024", event: "Project inception as Android text editor" },
  { year: "2025", event: "Kotlin Multiplatform migration, Desktop and Web targets added" },
  { year: "2025", event: "17 text format parsers implemented with full test coverage" },
  { year: "2025", event: "Cloud storage protocols (Dropbox, Google Drive, OneDrive, FTP, SFTP, WebDAV)" },
  { year: "2026", event: "iOS target, comprehensive CI/CD, video courses, documentation website" },
];

export default function AboutPage() {
  return (
    <div className="container-page py-16">
      <h1 className="text-4xl font-bold tracking-tight mb-4">About Yole</h1>
      <p className="text-lg text-[var(--color-text-secondary)] mb-12 max-w-3xl">
        Yole is a cross-platform text editor built with Kotlin Multiplatform. It supports
        17+ text formats, works offline by default, and optionally syncs with cloud storage
        providers.
      </p>

      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-16">
        {stats.map((stat) => (
          <div key={stat.label} className="card text-center">
            <div className="text-3xl font-bold text-primary-500 mb-1">{stat.value}</div>
            <div className="text-sm text-[var(--color-text-secondary)]">{stat.label}</div>
          </div>
        ))}
      </div>

      {/* Mission */}
      <section className="mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-4">Mission</h2>
        <div className="card">
          <p className="text-[var(--color-text-secondary)] leading-relaxed">
            Text is the most universal data format. Yole exists to provide a single,
            high-quality editor that understands the structure of your text files — whether
            you are writing Markdown documentation, managing tasks with todo.txt,
            editing CSV data, or drafting a screenplay in Fountain format. We believe your
            editor should work offline, respect your privacy, and run on every platform
            you use.
          </p>
        </div>
      </section>

      {/* Architecture */}
      <section className="mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-4">Architecture</h2>
        <div className="card">
          <p className="text-[var(--color-text-secondary)] leading-relaxed mb-4">
            Yole uses Kotlin Multiplatform (KMP) to share business logic across Android,
            Desktop (JVM), iOS (Native), and Web (Wasm). All 17 text format parsers live in
            the shared module and are tested with a comprehensive suite of 2,200+ tests.
          </p>
          <p className="text-[var(--color-text-secondary)] leading-relaxed">
            Platform-specific code handles UI (Compose Multiplatform), file system access,
            and native integrations. This architecture ensures consistent behavior across
            all platforms while leveraging native capabilities where they matter.
          </p>
        </div>
      </section>

      {/* Timeline */}
      <section className="mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-4">Timeline</h2>
        <div className="space-y-4">
          {timeline.map((item, i) => (
            <div key={i} className="card flex items-start gap-4">
              <div className="flex-shrink-0 w-16 text-sm font-bold text-primary-500">
                {item.year}
              </div>
              <p className="text-[var(--color-text-secondary)]">{item.event}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Contributing */}
      <section className="mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-4">Contributing</h2>
        <div className="card">
          <p className="text-[var(--color-text-secondary)] leading-relaxed mb-4">
            Yole is open source under the Apache-2.0 license. Contributions are welcome —
            whether fixing bugs, adding format support, improving documentation, or
            building platform features.
          </p>
          <div className="flex flex-wrap gap-3">
            <a
              href="https://github.com/vasic-digital/Yole"
              target="_blank"
              rel="noopener noreferrer"
              className="btn-primary"
            >
              View on GitHub
            </a>
            <a
              href="https://github.com/vasic-digital/Yole/issues"
              target="_blank"
              rel="noopener noreferrer"
              className="btn-secondary"
            >
              Report an Issue
            </a>
          </div>
        </div>
      </section>

      {/* Author */}
      <section>
        <h2 className="text-3xl font-bold tracking-tight mb-4">Author</h2>
        <div className="card">
          <p className="text-[var(--color-text-secondary)] leading-relaxed">
            Yole is created and maintained by Milos Vasic.
          </p>
        </div>
      </section>
    </div>
  );
}
