# Yole Sample Files

Example files demonstrating all 17 supported text formats.

**Purpose**: Test files, format examples, and reference implementations
**Last Updated**: November 11, 2025

---

## Available Samples

### Core Formats

| Format | Files | Description |
|--------|-------|-------------|
| **Markdown** | `markor-markdown-reference.md`<br/>`markor-readme-demo.md`<br/>`cooking-recipe.md`<br/>`presentation-beamer.md`<br/>`202901012359-zettelkasten.md`<br/>`2029-01-01-jekyll-post.md` | CommonMark + GFM examples, task lists, tables, code blocks, math (KaTeX), Jekyll front matter, Beamer presentations, Zettelkasten notes |
| **Plain Text** | `*.txt` files | Universal text format, various encodings |
| **Todo.txt** | `todo.sample.txt` | Task management with priorities, projects, contexts, due dates |
| **CSV** | `sample.csv` | Data tables with headers, markdown in cells |
| **LaTeX** | - | *See AsciiDoc samples for similar structure* |
| **Org Mode** | `orgmode-reference.org` | Headlines, TODO items, tables, timestamps, properties |

### Wiki Formats

| Format | Files | Description |
|--------|-------|-------------|
| **WikiText** | `zim-wiki-reference.zim.txt`<br/>`Zim-Sample-Notebook/` | MediaWiki/Zim markup, wiki links, templates |
| **Creole** | - | *Standardized wiki markup* |
| **TiddlyWiki** | - | *Tiddler format samples* |

### Technical Formats

| Format | Files | Description |
|--------|-------|-------------|
| **AsciiDoc** | `2029-01-01-jekyll-post.adoc` | Technical documentation with Jekyll integration |
| **reStructuredText** | - | *Python documentation standard* |

### Specialized Formats

| Format | Files | Description |
|--------|-------|-------------|
| **Key-Value** | - | *.properties, .ini, .env examples* |
| **TaskPaper** | `tp-todo.taskpaper` | Plain-text task management with tags |
| **Textile** | - | *Lightweight markup* |

### Data Science

| Format | Files | Description |
|--------|-------|-------------|
| **Jupyter** | - | *Interactive notebook format* |
| **R Markdown** | - | *Reproducible R research* |

### Other

| Format | Files | Description |
|--------|-------|-------------|
| **Binary Detection** | - | *Yole detects and prevents editing binary files* |

---

## File Descriptions

### Markdown Files

**`markor-markdown-reference.md`** (6,259 bytes):
- Comprehensive Markdown syntax reference
- Headers, emphasis, lists, links, images
- Code blocks with syntax highlighting
- Tables, task lists, blockquotes
- Math equations (KaTeX)
- YAML front matter
- Mermaid diagrams

**`markor-readme-demo.md`** (1,369 bytes):
- Project README example
- Badges, installation, usage
- Features list, screenshots
- Contributing guidelines

**`cooking-recipe.md`** (262 bytes):
- Simple recipe format
- Ingredients list, instructions
- Demonstrates practical use case

**`presentation-beamer.md`** (1,871 bytes):
- Beamer presentation in Markdown
- YAML metadata for slides
- Slide separators, speaker notes
- Code examples in slides

**`202901012359-zettelkasten.md`** (36 bytes):
- Zettelkasten note format
- Timestamp-based ID
- Note-taking workflow example

**`2029-01-01-jekyll-post.md`** (168 bytes):
- Jekyll blog post format
- YAML front matter with metadata
- Post content

### Todo.txt

**`todo.sample.txt`** (365 bytes):
- Task management examples
- Priorities: (A), (B), (C)
- Projects: +projectName
- Contexts: @contextName
- Due dates: due:YYYY-MM-DD
- Completed tasks with dates
- Multiple task scenarios

### CSV

**`sample.csv`** (327 bytes):
- Data table with headers
- Multiple columns (Name, Age, City, Notes)
- Markdown formatting in cells
- Demonstrates column color coding in Yole

### Org Mode

**`orgmode-reference.org`** (1,631 bytes):
- Headline hierarchy (* ** ***)
- TODO keywords (TODO, DONE, IN-PROGRESS)
- Properties drawer (:PROPERTIES:)
- Timestamps and scheduling
- Tables with alignment
- Code blocks (#+BEGIN_SRC)
- Tags (:tag1:tag2:)
- Links ([[link][description]])

### AsciiDoc

**`2029-01-01-jekyll-post.adoc`** (888 bytes):
- Jekyll-compatible AsciiDoc
- Front matter (YAML)
- Document attributes
- Sections and subsections
- Code blocks, lists
- Cross-references

### WikiText

**`zim-wiki-reference.zim.txt`** (1,710 bytes):
- Zim Wiki markup syntax
- Wiki links [[page]]
- Emphasis, headers, lists
- Code blocks, tables
- Checkboxes, images
- Zim-specific formatting

**`Zim-Sample-Notebook/`**:
- Complete Zim notebook structure
- Multiple interconnected pages
- Demonstrates wiki organization

### TaskPaper

**`tp-todo.taskpaper`** (318 bytes):
- Projects (ending with :)
- Tasks (starting with -)
- Tags (@tag, @due(2025-11-15))
- @done for completed tasks
- Hierarchical structure
- Notes and subtasks

---

## Using Sample Files

### Testing

Use these samples to test:
- Format detection accuracy
- Syntax highlighting correctness
- HTML preview rendering
- PDF export quality
- Action button functionality

### Reference

Use these samples as:
- Format syntax reference
- Real-world examples
- Template for your own files
- Learning resources

### Development

Use these samples for:
- Unit test data
- Integration test scenarios
- Performance benchmarks
- Format parser validation

---

## Creating Custom Samples

When creating new sample files:

1. **Name clearly**: `format-feature.ext` (e.g., `markdown-tables.md`)
2. **Include variety**: Basic and advanced features
3. **Add comments**: Explain syntax where helpful
4. **Keep concise**: Demonstrate features, not write essays
5. **Test validity**: Ensure format syntax is correct
6. **Document**: Add entry to this README

---

## Format Coverage

| Category | Formats | Sample Files Available |
|----------|---------|----------------------|
| **Core** | 6 | 6/6 (100%) |
| **Wiki** | 3 | 1/3 (33%) |
| **Technical** | 2 | 1/2 (50%) |
| **Specialized** | 3 | 1/3 (33%) |
| **Data Science** | 2 | 0/2 (0%) |
| **Other** | 1 | 0/1 (0%) |
| **Total** | **17** | **9/17 (53%)** |

---

## Adding More Samples

We welcome contributions of sample files for:
- LaTeX (.tex)
- reStructuredText (.rst)
- Key-Value formats (.properties, .ini, .env)
- Textile (.textile)
- Creole (.creole)
- TiddlyWiki (.tid)
- Jupyter (.ipynb)
- R Markdown (.Rmd)

See [Contributing Guide](../CONTRIBUTING.md) for details.

---

## License

Sample files are licensed under **CC0 1.0 Universal (Public Domain)**.

See [LICENSE_OF_SAMPLES.md](LICENSE_OF_SAMPLES.md) for details.

You can:
-  Use commercially
-  Modify freely
-  Distribute
-  Use privately
- L No attribution required
- L No warranty provided

---

## Related Documentation

- **[Format Guides](../docs/user-guide/formats/)** - Complete format documentation
- **[API Examples](../examples/api-usage/)** - Code examples for using Yole APIs
- **[Tutorials](../examples/tutorials/)** - Step-by-step tutorials
- **[Integration Examples](../examples/integration/)** - Integrating Yole into your app

---

**Sample Collection Version**: 1.0
**Last Updated**: November 11, 2025
**Total Files**: 15+ sample files
**Format Coverage**: 9/17 formats (53%)
