// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2025 Milos Vasic

const formats = [
  {
    id: "markdown",
    name: "Markdown",
    description: "Full GitHub Flavored Markdown (GFM) support including tables, task lists, footnotes, strikethrough, and math expressions.",
    features: ["Headings, paragraphs, emphasis", "Tables with alignment", "Task lists and checklists", "Footnotes and references", "Math expressions (KaTeX)", "Code blocks with syntax highlighting"],
    extensions: [".md", ".markdown", ".mkd"],
  },
  {
    id: "todotxt",
    name: "todo.txt",
    description: "Plain-text task management following the todo.txt format specification with priorities, projects, contexts, and due dates.",
    features: ["Priority levels (A)-(Z)", "Projects (+project) and contexts (@context)", "Due dates (due:YYYY-MM-DD)", "Completion tracking", "Query syntax for filtering", "Sorting by priority, date, project"],
    extensions: [".txt", ".todo"],
  },
  {
    id: "csv",
    name: "CSV / TSV",
    description: "Spreadsheet-style editing for comma-separated and tab-separated value files with column alignment.",
    features: ["Comma and tab delimiters", "Quoted field support", "Column alignment", "Header row detection", "Large file handling"],
    extensions: [".csv", ".tsv"],
  },
  {
    id: "latex",
    name: "LaTeX",
    description: "Scientific typesetting with math equations, bibliography management, and document structuring.",
    features: ["Math mode (inline and display)", "Document structure commands", "Bibliography with BibTeX", "Cross-references", "Table and figure environments"],
    extensions: [".tex", ".latex"],
  },
  {
    id: "orgmode",
    name: "Org Mode",
    description: "Hierarchical note-taking and task management inspired by Emacs Org Mode.",
    features: ["Hierarchical headings", "TODO states and priorities", "Tags and properties", "Timestamps and scheduling", "Tables with formulas", "Code blocks"],
    extensions: [".org"],
  },
  {
    id: "rst",
    name: "reStructuredText",
    description: "Python documentation format used by Sphinx, with directives and roles for rich document markup.",
    features: ["Section headings with underlines", "Directives and roles", "Cross-references", "Code blocks with highlighting", "Tables (grid and simple)"],
    extensions: [".rst", ".rest"],
  },
  {
    id: "asciidoc",
    name: "AsciiDoc",
    description: "Technical documentation format with support for includes, conditional content, and rich formatting.",
    features: ["Section headings", "Admonition blocks", "Source code blocks", "Include directives", "Cross-references and IDs"],
    extensions: [".adoc", ".asciidoc", ".asc"],
  },
  {
    id: "textile",
    name: "Textile",
    description: "Lightweight markup language with inline formatting and block-level elements.",
    features: ["Inline formatting", "Block quotes", "Lists (ordered and unordered)", "Tables", "Image and link syntax"],
    extensions: [".textile"],
  },
  {
    id: "wikitext",
    name: "WikiText",
    description: "MediaWiki-compatible syntax for wiki pages with templates, internal links, and structured content.",
    features: ["Internal and external links", "Template syntax", "Categories", "Tables", "Headings and sections"],
    extensions: [".wiki", ".mediawiki"],
  },
  {
    id: "tiddlywiki",
    name: "TiddlyWiki",
    description: "Non-linear personal wiki format with transclusion and unique tiddler-based organization.",
    features: ["Tiddler transclusion", "Macros", "Wikilinks", "Styled blocks", "Filter expressions"],
    extensions: [".tid"],
  },
  {
    id: "creole",
    name: "Creole",
    description: "Standardized wiki markup language designed to be a common syntax across different wiki engines.",
    features: ["Bold and italic formatting", "Internal and external links", "Ordered and unordered lists", "Tables", "Images and headings"],
    extensions: [".creole"],
  },
  {
    id: "rmarkdown",
    name: "R Markdown",
    description: "Data science document format combining R code, output, and narrative text with YAML front matter.",
    features: ["YAML front matter", "Code chunks with options", "Inline expressions", "Multiple output formats", "Data visualization"],
    extensions: [".Rmd", ".rmd"],
  },
  {
    id: "keyvalue",
    name: "Key-Value",
    description: "Configuration file formats including .properties, .env, and similar key=value pair formats.",
    features: [".properties format", ".env format", "Comment support", "Value quoting", "Section headers (INI-style)"],
    extensions: [".properties", ".env", ".ini", ".cfg"],
  },
  {
    id: "jupyter",
    name: "Jupyter",
    description: "Interactive notebook format with code cells, markdown cells, and output display.",
    features: ["Code cell parsing", "Markdown cells", "Output rendering", "Multi-language support", "Metadata handling"],
    extensions: [".ipynb"],
  },
  {
    id: "taskpaper",
    name: "TaskPaper",
    description: "Plain-text task management with projects, tasks, and tags in a simple indentation-based format.",
    features: ["Projects (ending with colon)", "Tasks (starting with dash)", "Tags (@tag)", "Notes (plain text)", "Hierarchical indentation"],
    extensions: [".taskpaper"],
  },
  {
    id: "binary",
    name: "Binary",
    description: "Binary file detection with media preview support for images, audio, and video files.",
    features: ["Binary file detection", "Image preview (PNG, JPG, GIF)", "Audio playback (MP3, OGG)", "Video preview (MP4, WebM)", "Byte analysis"],
    extensions: [".bin", ".dat"],
  },
  {
    id: "plaintext",
    name: "Plain Text",
    description: "Simple plain text editing with line numbers, word wrap, and basic text manipulation.",
    features: ["Line numbering", "Word wrap", "Character/word/line counts", "Encoding detection", "Line ending handling"],
    extensions: [".txt", ".text"],
  },
];

const quickStartSteps = [
  {
    step: 1,
    title: "Install",
    icon: "\u2B07",
    description:
      "Download from the releases page or build from source. Yole runs on Android, Desktop (Windows, macOS, Linux), and Web.",
    link: { label: "Go to Downloads", href: "/download" },
  },
  {
    step: 2,
    title: "Open a File",
    icon: "\uD83D\uDCC4",
    description:
      "Launch Yole and open any text file. Yole supports 17+ formats out of the box, including Markdown, Todo.txt, LaTeX, CSV, and more.",
  },
  {
    step: 3,
    title: "Explore Formats",
    icon: "\uD83D\uDD0D",
    description:
      "Yole auto-detects the file format based on extension and content. Try opening different file types to see format-specific parsing and HTML preview.",
  },
  {
    step: 4,
    title: "Cloud Storage",
    icon: "\u2601\uFE0F",
    description:
      "Connect to Dropbox, Google Drive, OneDrive, FTP, or SFTP for seamless remote file access. Yole works offline-first and syncs when connected.",
  },
];

export default function DocsPage() {
  return (
    <div className="container-page py-16">
      <h1 className="text-4xl font-bold tracking-tight mb-4">Documentation</h1>
      <p className="text-lg text-[var(--color-text-secondary)] mb-12 max-w-3xl">
        Yole supports 17+ text formats out of the box. Each format includes parsing,
        syntax highlighting, and format-specific features.
      </p>

      {/* Quick Start */}
      <section className="mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-6">Quick Start</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {quickStartSteps.map((item) => (
            <div key={item.step} className="card flex items-start gap-4">
              <div className="flex-shrink-0 w-12 h-12 rounded-full bg-primary-500/10 text-primary-500 flex items-center justify-center font-bold text-lg">
                {item.step}
              </div>
              <div className="flex-1">
                <h3 className="font-bold text-lg mb-1">
                  <span className="mr-2">{item.icon}</span>
                  {item.title}
                </h3>
                <p className="text-sm text-[var(--color-text-secondary)] leading-relaxed">
                  {item.description}
                </p>
                {item.link && (
                  <a
                    href={item.link.href}
                    className="inline-block mt-2 text-sm font-medium text-primary-500 hover:text-primary-600 transition-colors"
                  >
                    {item.link.label} &rarr;
                  </a>
                )}
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Supported Formats */}
      <h2 className="text-3xl font-bold tracking-tight mb-6">Supported Formats</h2>
      <div className="space-y-12">
        {formats.map((format) => (
          <section key={format.id} id={format.id} className="scroll-mt-24">
            <div className="card">
              <h2 className="text-2xl font-bold mb-2">{format.name}</h2>
              <p className="text-[var(--color-text-secondary)] mb-4">{format.description}</p>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <h3 className="font-semibold mb-2">Features</h3>
                  <ul className="space-y-1">
                    {format.features.map((feature) => (
                      <li key={feature} className="text-sm text-[var(--color-text-secondary)] flex items-start gap-2">
                        <span className="text-primary-500 mt-0.5">-</span>
                        {feature}
                      </li>
                    ))}
                  </ul>
                </div>
                <div>
                  <h3 className="font-semibold mb-2">File Extensions</h3>
                  <div className="flex flex-wrap gap-2">
                    {format.extensions.map((ext) => (
                      <code key={ext} className="text-sm px-2 py-1 rounded bg-[var(--color-bg-secondary)] border border-[var(--color-border)]">
                        {ext}
                      </code>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </section>
        ))}
      </div>

      {/* Additional Documentation */}
      <section className="mt-16 mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-6">Guides and References</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <a href="https://github.com/vasic-digital/Yole/blob/master/TROUBLESHOOTING.md" target="_blank" rel="noopener noreferrer" className="card group">
            <h3 className="font-semibold text-lg mb-1 group-hover:text-primary-500 transition-colors">Troubleshooting Guide</h3>
            <p className="text-sm text-[var(--color-text-secondary)]">Common issues and solutions for building, testing, and running Yole across all platforms.</p>
          </a>
          <a href="https://github.com/vasic-digital/Yole/blob/master/RELEASE_PROCESS.md" target="_blank" rel="noopener noreferrer" className="card group">
            <h3 className="font-semibold text-lg mb-1 group-hover:text-primary-500 transition-colors">Release Process</h3>
            <p className="text-sm text-[var(--color-text-secondary)]">How releases are versioned, built, and published. Includes CI/CD pipeline and distribution details.</p>
          </a>
          <a href="https://github.com/vasic-digital/Yole/blob/master/docs/adr/" target="_blank" rel="noopener noreferrer" className="card group">
            <h3 className="font-semibold text-lg mb-1 group-hover:text-primary-500 transition-colors">Architecture Decision Records</h3>
            <p className="text-sm text-[var(--color-text-secondary)]">ADRs documenting key architectural decisions, trade-offs, and rationale behind the design choices.</p>
          </a>
        </div>
      </section>

      <section className="mt-16">
        <h2 className="text-3xl font-bold tracking-tight mb-4">Architecture</h2>
        <p className="text-[var(--color-text-secondary)] mb-6 max-w-3xl">
          Yole is built with Kotlin Multiplatform. All format parsing logic lives in the shared
          module, while platform-specific UI and features are implemented per target.
        </p>
        <div className="card">
          <pre className="text-sm overflow-x-auto"><code>{`shared/src/commonMain/kotlin/digital/vasic/yole/
├── format/                    # Format system (17 text formats)
│   ├── FormatRegistry.kt      # Central format registry
│   ├── TextFormat.kt          # Format metadata
│   ├── TextParser.kt          # Parser interface
│   ├── markdown/              # Markdown parser
│   ├── todotxt/               # todo.txt parser
│   ├── csv/                   # CSV/TSV parser
│   └── ...                    # Other format parsers
├── model/                     # Document model
└── network/                   # Cloud storage protocols`}</code></pre>
        </div>
      </section>
    </div>
  );
}
