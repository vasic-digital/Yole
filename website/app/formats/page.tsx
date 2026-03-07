// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2025 Milos Vasic

const formats = [
  {
    id: "markdown",
    name: "Markdown",
    icon: "M",
    description: "Full GitHub Flavored Markdown (GFM) support including tables, task lists, footnotes, strikethrough, and math expressions. The most popular lightweight markup language for documentation and writing.",
    extensions: [".md", ".markdown", ".mkd"],
    useCases: ["Technical documentation", "Blog posts and articles", "README files", "Note-taking", "Knowledge bases"],
    example: `# Welcome to Yole

A **cross-platform** text editor with _17+ formats_.

## Features
- [x] Markdown support
- [ ] LaTeX equations
- [ ] Cloud sync

| Platform | Status |
|----------|--------|
| Android  | Production |
| Desktop  | Beta |

> Offline-first, open source, free.

\`\`\`kotlin
fun main() = println("Hello, Yole!")
\`\`\``,
  },
  {
    id: "todotxt",
    name: "todo.txt",
    icon: "T",
    description: "Plain-text task management following the todo.txt format specification with priorities, projects, contexts, and due dates. A simple yet powerful system for managing tasks.",
    extensions: [".txt", ".todo"],
    useCases: ["Task management", "Project tracking", "Daily to-do lists", "GTD workflow", "Team task coordination"],
    example: `(A) Write project proposal +ProjectAlpha @office due:2026-04-01
(B) Review pull requests +Yole @computer
x 2026-03-01 (A) Deploy v2.15.1 +Yole @server
(C) Buy groceries @errands
Schedule team meeting +ProjectAlpha @office due:2026-03-15
x 2026-02-28 Fix markdown parser bug +Yole @computer`,
  },
  {
    id: "csv",
    name: "CSV / TSV",
    icon: "C",
    description: "Spreadsheet-style editing for comma-separated and tab-separated value files with column alignment, header row detection, and quoted field support.",
    extensions: [".csv", ".tsv"],
    useCases: ["Data analysis", "Spreadsheet editing", "Database import/export", "Configuration data", "Report generation"],
    example: `Name,Role,Platform,Status
"Yole Editor","Text Editor","Android","Production"
"Yole Desktop","Desktop App","Windows/macOS/Linux","Beta"
"Yole iOS","Mobile App","iOS","Development"
"Yole Web","Web App","Browser (Wasm)","Development"`,
  },
  {
    id: "latex",
    name: "LaTeX",
    icon: "L",
    description: "Scientific typesetting with math equations, bibliography management, cross-references, and document structuring. The gold standard for academic and scientific writing.",
    extensions: [".tex", ".latex"],
    useCases: ["Academic papers", "Scientific publications", "Math documents", "Theses and dissertations", "Technical reports"],
    example: `\\documentclass{article}
\\usepackage{amsmath}

\\title{Introduction to Yole}
\\author{Milos Vasic}

\\begin{document}
\\maketitle

\\section{Text Parsing}
The parsing pipeline processes input in $O(n)$ time.

\\begin{equation}
  E = mc^2
\\end{equation}

\\bibliographystyle{plain}
\\bibliography{refs}
\\end{document}`,
  },
  {
    id: "orgmode",
    name: "Org Mode",
    icon: "O",
    description: "Hierarchical note-taking and task management inspired by Emacs Org Mode. Supports TODO states, priorities, tags, timestamps, tables with formulas, and code blocks.",
    extensions: [".org"],
    useCases: ["Note-taking and outlining", "Task and project management", "Agenda and scheduling", "Literate programming", "Knowledge management"],
    example: `* TODO [#A] Write documentation :yole:docs:
  DEADLINE: <2026-04-01 Wed>
  :PROPERTIES:
  :EFFORT: 2h
  :END:

** DONE Set up project structure
   CLOSED: [2026-03-01 Sat 14:00]

** TODO Add format examples
   SCHEDULED: <2026-03-10 Tue>

| Format   | Status | Tests |
|----------+--------+-------|
| Markdown | Done   |   450 |
| todo.txt | Done   |   320 |`,
  },
  {
    id: "rst",
    name: "reStructuredText",
    icon: "R",
    description: "Python documentation format used by Sphinx, with directives and roles for rich document markup. The standard for Python ecosystem documentation.",
    extensions: [".rst", ".rest"],
    useCases: ["Python documentation", "Sphinx-based docs", "API documentation", "Technical manuals", "ReadTheDocs projects"],
    example: `Yole Text Editor
================

A cross-platform editor for 17+ formats.

Getting Started
---------------

Install Yole from the \`releases page\`_.

.. code-block:: kotlin

   val parser = FormatRegistry.getParser("markdown")
   val doc = parser.parse(content)

.. note::

   Yole works offline by default.

.. _releases page: https://github.com/vasic-digital/Yole/releases`,
  },
  {
    id: "asciidoc",
    name: "AsciiDoc",
    icon: "A",
    description: "Technical documentation format with support for includes, conditional content, admonition blocks, and rich formatting. A powerful alternative to Markdown for complex documents.",
    extensions: [".adoc", ".asciidoc", ".asc"],
    useCases: ["Technical documentation", "Book authoring", "API documentation", "System architecture docs", "Enterprise documentation"],
    example: `= Yole User Guide
Milos Vasic <milos@vasic.digital>
:toc: left
:icons: font

== Introduction

Yole is a *cross-platform* text editor supporting
_17+ text formats_.

=== Supported Formats

[source,kotlin]
----
val formats = FormatRegistry.allFormats()
println("Supported: \${formats.size} formats")
----

NOTE: Yole works offline by default.

TIP: Use cloud storage for syncing across devices.`,
  },
  {
    id: "textile",
    name: "Textile",
    icon: "X",
    description: "Lightweight markup language with inline formatting and block-level elements. Used historically in web publishing platforms like Redmine and Textpattern.",
    extensions: [".textile"],
    useCases: ["Web content", "Redmine wikis", "Blog platforms", "CMS content", "Quick formatting"],
    example: `h1. Yole Text Editor

A *cross-platform* editor with _17+ formats_.

h2. Features

* Offline-first editing
* Cloud storage integration
* Format auto-detection
# First download Yole
# Open any text file
# Start editing

|_. Platform |_. Status |
| Android | Production |
| Desktop | Beta |

bq. Simple, powerful, free.`,
  },
  {
    id: "wikitext",
    name: "WikiText",
    icon: "W",
    description: "MediaWiki-compatible syntax for wiki pages with templates, internal links, categories, tables, and structured content.",
    extensions: [".wiki", ".mediawiki"],
    useCases: ["Wiki pages", "Knowledge bases", "Collaborative documentation", "Encyclopedia entries", "Internal wikis"],
    example: `== Yole Text Editor ==

'''Yole''' is a [[cross-platform]] text editor
supporting 17+ [[text format]]s.

=== Features ===
* Offline-first editing
* [[Cloud storage]] integration
* {{FormatCount|17}} supported formats

{| class="wikitable"
! Platform !! Status
|-
| Android || Production
|-
| Desktop || Beta
|}

[[Category:Text Editors]]
[[Category:Open Source]]`,
  },
  {
    id: "tiddlywiki",
    name: "TiddlyWiki",
    icon: "D",
    description: "Non-linear personal wiki format with transclusion, macros, wikilinks, styled blocks, and filter expressions. Perfect for building personal knowledge systems.",
    extensions: [".tid"],
    useCases: ["Personal wikis", "Knowledge management", "Note collections", "Research notes", "Digital gardens"],
    example: `title: Yole Editor
tags: software editor cross-platform
type: text/vnd.tiddlywiki

! Yole Text Editor

Yole is a ''cross-platform'' text editor.

!! Supported Formats

<<list-links "[tag[format]]">>

* [[Markdown]]
* [[todo.txt]]
* [[LaTeX]]

{{FormatRegistry}}

<<<
Offline-first, open source, free.
<<<`,
  },
  {
    id: "fountain",
    name: "Fountain",
    icon: "F",
    description: "Screenplay writing format with automatic detection of scene headings, characters, dialogue, parentheticals, and transitions. Industry-standard for screenwriters.",
    extensions: [".fountain", ".spmd"],
    useCases: ["Screenplays", "Stage plays", "TV scripts", "Short films", "Film production"],
    example: `Title: The Editor
Author: Milos Vasic

INT. OFFICE - NIGHT

A developer sits at a desk, multiple monitors glowing.

DEVELOPER
(typing furiously)
One editor to rule them all.

COLLEAGUE
What are you working on?

DEVELOPER
Yole. Cross-platform. Seventeen formats.

> FADE TO:

EXT. CONFERENCE - DAY

A presentation screen shows "Yole v2.15.1 Released."`,
  },
  {
    id: "ledger",
    name: "Ledger",
    icon: "$",
    description: "Plain-text double-entry accounting format for personal and business financial tracking. Supports transaction records, account hierarchies, and balance assertions.",
    extensions: [".ledger", ".journal"],
    useCases: ["Personal finance", "Business accounting", "Expense tracking", "Budget management", "Financial reporting"],
    example: `; Yole Project Expenses
2026-03-01 * Server Hosting
    Expenses:Infrastructure     $49.99
    Assets:Checking

2026-03-05 * Domain Renewal
    Expenses:Infrastructure     $12.00
    Assets:Checking

2026-03-07 * Coffee for Development
    Expenses:Food:Coffee         $4.50
    Assets:Cash

P 2026-03-07 EUR $1.08`,
  },
  {
    id: "keyvalue",
    name: "Key-Value",
    icon: "K",
    description: "Configuration file formats including .properties, .env, INI-style, and similar key=value pair formats with comments and section headers.",
    extensions: [".properties", ".env", ".ini", ".cfg"],
    useCases: ["Application configuration", "Environment variables", "Build properties", "Server configuration", "Feature flags"],
    example: `# Application Configuration
app.name=Yole
app.version=2.15.1
app.formats.count=17

[editor]
theme=dark
font.family=JetBrains Mono
font.size=14
line.numbers=true

[storage]
offline.first=true
cloud.provider=dropbox
sync.interval=300`,
  },
  {
    id: "jupyter",
    name: "Jupyter",
    icon: "J",
    description: "Interactive notebook format with code cells, markdown cells, and output display. The standard format for data science and computational notebooks.",
    extensions: [".ipynb"],
    useCases: ["Data science", "Machine learning", "Research notebooks", "Teaching materials", "Data exploration"],
    example: `{
  "cells": [
    {
      "cell_type": "markdown",
      "source": ["# Yole Format Analysis"]
    },
    {
      "cell_type": "code",
      "source": [
        "formats = ['Markdown', 'todo.txt', 'CSV']",
        "print(f'Supported: {len(formats)}')"
      ],
      "outputs": [
        { "text": "Supported: 3" }
      ]
    }
  ]
}`,
  },
  {
    id: "taskpaper",
    name: "TaskPaper",
    icon: "P",
    description: "Plain-text task management with projects, tasks, and tags in a simple indentation-based format. Designed for simplicity and readability.",
    extensions: [".taskpaper"],
    useCases: ["Task management", "Project planning", "Simple to-do lists", "Sprint planning", "Daily agendas"],
    example: `Yole Development:
  - Implement cloud storage @priority(high) @due(2026-04-01)
  - Fix markdown table alignment @bug @assigned(milos)
  - Add iOS target support @feature @estimate(2w)
  - Update documentation @docs
    Notes for documentation update

Release Planning:
  - Prepare v2.16 changelog @release
  - Run full test suite @testing @done(2026-03-07)
  - Deploy to F-Droid @deploy`,
  },
  {
    id: "bibtex",
    name: "BibTeX",
    icon: "B",
    description: "Bibliography and citation management format for academic and scientific writing. Supports various entry types and integrates with LaTeX documents.",
    extensions: [".bib"],
    useCases: ["Academic citations", "Research bibliographies", "Publication management", "Thesis references", "Paper writing"],
    example: `@article{vasic2026yole,
  title     = {Yole: A Cross-Platform Text Editor},
  author    = {Vasic, Milos},
  journal   = {Open Source Software},
  year      = {2026},
  volume    = {11},
  pages     = {1--15},
  doi       = {10.1234/yole.2026}
}

@inproceedings{kmp2025,
  title     = {Kotlin Multiplatform in Practice},
  author    = {JetBrains Team},
  booktitle = {KotlinConf 2025},
  year      = {2025}
}`,
  },
  {
    id: "plaintext",
    name: "Plain Text",
    icon: "#",
    description: "Simple plain text editing with line numbers, word wrap, character/word/line counts, encoding detection, and line ending handling.",
    extensions: [".txt", ".text"],
    useCases: ["Quick notes", "Log files", "Scratch pads", "Configuration files", "General text editing"],
    example: `Yole - Cross-Platform Text Editor
==================================

Supported Platforms:
  1. Android (Production)
  2. Desktop (Beta)
  3. iOS (In Development)
  4. Web (In Development)

Quick Stats:
  - 17+ text formats
  - 4,750+ tests
  - 8 cloud protocols
  - Apache-2.0 license`,
  },
];

export default function FormatsPage() {
  return (
    <div className="container-page py-16">
      <h1 className="text-4xl font-bold tracking-tight mb-4">Supported Formats</h1>
      <p className="text-lg text-[var(--color-text-secondary)] mb-4 max-w-3xl">
        Yole supports 17 text formats out of the box. Each format includes full parsing,
        syntax highlighting, HTML preview, and format-specific features.
      </p>
      <p className="text-[var(--color-text-secondary)] mb-12 max-w-3xl">
        Click on any format below to see its syntax example, supported extensions, and common use cases.
      </p>

      {/* Format Grid Summary */}
      <section className="mb-16">
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
          {formats.map((format) => (
            <a
              key={format.id}
              href={`#${format.id}`}
              className="card flex flex-col items-center text-center py-4 px-3 group"
            >
              <div className="w-10 h-10 rounded-lg bg-primary-500/10 text-primary-500 flex items-center justify-center font-bold text-lg mb-2">
                {format.icon}
              </div>
              <span className="text-sm font-semibold group-hover:text-primary-500 transition-colors">
                {format.name}
              </span>
            </a>
          ))}
        </div>
      </section>

      {/* Detailed Format Cards */}
      <div className="space-y-12">
        {formats.map((format) => (
          <section key={format.id} id={format.id} className="scroll-mt-24">
            <div className="card">
              <div className="flex items-center gap-4 mb-4">
                <div className="flex-shrink-0 w-12 h-12 rounded-lg bg-primary-500/10 text-primary-500 flex items-center justify-center font-bold text-xl">
                  {format.icon}
                </div>
                <div>
                  <h2 className="text-2xl font-bold">{format.name}</h2>
                  <div className="flex flex-wrap gap-2 mt-1">
                    {format.extensions.map((ext) => (
                      <code
                        key={ext}
                        className="text-xs px-2 py-0.5 rounded bg-[var(--color-bg-secondary)] border border-[var(--color-border)]"
                      >
                        {ext}
                      </code>
                    ))}
                  </div>
                </div>
              </div>

              <p className="text-[var(--color-text-secondary)] mb-6">{format.description}</p>

              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Syntax Example */}
                <div>
                  <h3 className="font-semibold mb-3">Syntax Example</h3>
                  <pre className="text-sm bg-[var(--color-bg-secondary)] rounded-lg p-4 overflow-x-auto border border-[var(--color-border)] max-h-72 overflow-y-auto">
                    <code>{format.example}</code>
                  </pre>
                </div>

                {/* Use Cases */}
                <div>
                  <h3 className="font-semibold mb-3">Common Use Cases</h3>
                  <ul className="space-y-2">
                    {format.useCases.map((useCase) => (
                      <li
                        key={useCase}
                        className="text-sm text-[var(--color-text-secondary)] flex items-start gap-2"
                      >
                        <span className="text-primary-500 mt-0.5 flex-shrink-0">-</span>
                        {useCase}
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            </div>
          </section>
        ))}
      </div>

      {/* CTA */}
      <section className="mt-16 text-center">
        <h2 className="section-heading">Want to add a new format?</h2>
        <p className="text-[var(--color-text-secondary)] mb-6 max-w-2xl mx-auto">
          Yole is open source. You can contribute new format parsers, improve existing ones,
          or request support for additional formats.
        </p>
        <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
          <a href="/community" className="btn-primary">
            Contribute
          </a>
          <a href="/docs" className="btn-secondary">
            Read the Docs
          </a>
        </div>
      </section>
    </div>
  );
}
