# Markdown Format Guide

**Format**: Markdown
**Extensions**: `.md`, `.markdown`, `.mdown`, `.mkd`
**Specification**: CommonMark + GitHub Flavored Markdown (GFM)
**Yole Support**: ✅ Full (parsing, preview, syntax highlighting)

---

## Overview

Markdown is a lightweight markup language for creating formatted text using a plain-text editor. It's one of the most popular formats for documentation, README files, blog posts, and note-taking.

### Why Markdown?

- **Simple**: Easy to learn and write
- **Readable**: Plain text is readable without rendering
- **Portable**: Works everywhere, from GitHub to blogs
- **Flexible**: Supports text, links, images, code, and more
- **Universal**: Supported by thousands of tools and platforms

---

## Basic Syntax

### Headers

Use `#` for headers (H1-H6):

```markdown
# H1 - Main Title
## H2 - Section
### H3 - Subsection
#### H4 - Sub-subsection
##### H5 - Minor heading
###### H6 - Smallest heading
```

**Rendered**:
# H1 - Main Title
## H2 - Section
### H3 - Subsection

**Tips**:
- Add blank lines before and after headers for better readability
- Use H1 for document title, H2 for main sections
- Keep header hierarchy logical (don't skip levels)

---

### Text Formatting

#### Bold
```markdown
**bold text** or __bold text__
```
**bold text**

#### Italic
```markdown
*italic text* or _italic text_
```
*italic text*

#### Bold + Italic
```markdown
***bold and italic*** or ___bold and italic___
```
***bold and italic***

#### Strikethrough (GFM)
```markdown
~~strikethrough text~~
```
~~strikethrough text~~

**Tips**:
- Use `**` for bold (more common than `__`)
- Use `*` for italic (more common than `_`)
- Don't mix markers: `**bold*italic**` won't work properly

---

### Lists

#### Unordered Lists

Use `-`, `*`, or `+` for bullet points:

```markdown
- Item 1
- Item 2
  - Nested item 2.1
  - Nested item 2.2
- Item 3
```

**Rendered**:
- Item 1
- Item 2
  - Nested item 2.1
  - Nested item 2.2
- Item 3

#### Ordered Lists

Use numbers followed by periods:

```markdown
1. First item
2. Second item
   1. Nested first
   2. Nested second
3. Third item
```

**Rendered**:
1. First item
2. Second item
   1. Nested first
   2. Nested second
3. Third item

**Tips**:
- Use 2 spaces or 4 spaces for indentation (be consistent)
- Numbers don't have to be sequential (Markdown auto-numbers)
- Mix ordered and unordered lists for nested items

---

### Links

#### Inline Links
```markdown
[Link text](https://example.com)
[Link with title](https://example.com "Hover title")
```

#### Reference Links
```markdown
[Link text][reference]

[reference]: https://example.com
```

#### Auto-links
```markdown
<https://example.com>
<email@example.com>
```

**Tips**:
- Use descriptive link text (not "click here")
- Reference links keep document cleaner
- Auto-links work for URLs and email addresses

---

### Images

#### Inline Images
```markdown
![Alt text](path/to/image.jpg)
![Alt text](path/to/image.jpg "Image title")
```

#### Reference Images
```markdown
![Alt text][image-ref]

[image-ref]: path/to/image.jpg "Optional title"
```

**Tips**:
- Always provide alt text for accessibility
- Use relative paths for local images
- Consider image size for mobile viewing

---

### Code

#### Inline Code
```markdown
Use `code` for inline code snippets.
```
Use `code` for inline code snippets.

#### Code Blocks (Fenced)

Use triple backticks with optional language:

````markdown
```python
def hello():
    print("Hello, World!")
```
````

**Supported languages** (40+):
- `python`, `javascript`, `java`, `kotlin`, `cpp`, `c`, `rust`
- `go`, `swift`, `ruby`, `php`, `typescript`, `bash`, `shell`
- `sql`, `json`, `yaml`, `xml`, `html`, `css`, `markdown`
- And many more...

#### Code Blocks (Indented)
```markdown
    def hello():
        print("Hello!")
```

**Tips**:
- Specify language for syntax highlighting
- Use fenced code blocks (easier to read)
- Inline code for short snippets, blocks for multi-line

---

## Advanced Syntax

### Tables (GFM)

```markdown
| Column 1 | Column 2 | Column 3 |
|----------|----------|----------|
| Row 1    | Data     | More     |
| Row 2    | Data     | More     |
```

**Rendered**:
| Column 1 | Column 2 | Column 3 |
|----------|----------|----------|
| Row 1    | Data     | More     |
| Row 2    | Data     | More     |

**Alignment**:
```markdown
| Left | Center | Right |
|:-----|:------:|------:|
| L    | C      | R     |
```

| Left | Center | Right |
|:-----|:------:|------:|
| L    | C      | R     |

**Tips**:
- Pipes don't need to align perfectly
- Use `:` for alignment (`:---` left, `:---:` center, `---:` right)
- Keep tables simple (max 5-6 columns for readability)

---

### Task Lists (GFM)

```markdown
- [x] Completed task
- [ ] Incomplete task
- [ ] Another incomplete task
```

**Rendered**:
- [x] Completed task
- [ ] Incomplete task
- [ ] Another incomplete task

**Tips**:
- Space between brackets: `[ ]` not `[]`
- Use for TODO lists and checklists
- Great for project planning documents

---

### Blockquotes

```markdown
> This is a blockquote.
> It can span multiple lines.
>
> > Nested blockquotes are also possible.
```

**Rendered**:
> This is a blockquote.
> It can span multiple lines.
>
> > Nested blockquotes are also possible.

**Tips**:
- Use for citations, notes, or callouts
- Can contain other Markdown elements
- Add attribution below quote

---

### Horizontal Rules

Use three or more hyphens, asterisks, or underscores:

```markdown
---
***
___
```

**Rendered**:

---

**Tips**:
- Use to separate major sections
- Add blank lines before and after
- Choose one style and be consistent

---

### Escaping Characters

Use backslash `\` to escape special characters:

```markdown
\* Not italic \*
\# Not a header
\[Not a link\](url)
```

**Characters that need escaping**:
```
\ ` * _ { } [ ] ( ) # + - . ! |
```

---

## Yole-Specific Features

### Syntax Highlighting

Yole provides real-time syntax highlighting for:
- **Headers**: Bold and larger text
- **Bold/Italic**: Styled in editor
- **Links**: Underlined and colored
- **Code**: Monospace with background
- **Lists**: Bullet points and numbering visible

### Live Preview

Toggle preview mode to see rendered output:
1. Write in Edit mode
2. Tap Preview button
3. See formatted document
4. Switch back to edit anytime

### Auto-Save

Enable auto-save in settings to never lose work:
- **Settings** → **Editor** → **Auto-save**
- Saves every 30 seconds
- Also saves on app switch

---

## Best Practices

### Document Structure

```markdown
# Document Title

Brief introduction paragraph.

## Section 1

Content for section 1.

### Subsection 1.1

Details...

### Subsection 1.2

More details...

## Section 2

Content for section 2.

---

## References

Links and citations.
```

### Writing Style

1. **One sentence per line**: Easier diffs and version control
2. **Blank lines**: Add before/after headers, lists, code blocks
3. **Consistent markers**: Choose `-` or `*` for lists, stick with it
4. **Semantic headers**: Use H1 for title, H2 for sections, H3 for subsections

### File Organization

```
project/
├── README.md              # Project overview
├── docs/
│   ├── getting-started.md # User guide
│   ├── api.md            # API documentation
│   ├── faq.md            # FAQs
│   └── changelog.md      # Version history
└── examples/
    └── sample.md         # Examples
```

---

## Common Use Cases

### 1. README Files

```markdown
# Project Name

Brief description of the project.

## Features

- Feature 1
- Feature 2
- Feature 3

## Installation

\```bash
npm install project-name
\```

## Usage

\```javascript
const project = require('project-name');
\```

## License

MIT
```

### 2. Documentation

```markdown
# API Documentation

## Authentication

All API requests require authentication...

### Endpoints

#### GET /users

Returns a list of users.

**Parameters**:
- `limit` (integer): Maximum results

**Response**:
\```json
{
  "users": [...]
}
\```
```

### 3. Meeting Notes

```markdown
# Meeting: Project Review

**Date**: 2025-11-11
**Attendees**: Alice, Bob, Charlie

## Agenda

1. Project status
2. Blockers
3. Next steps

## Discussion

### Project Status

- Feature X: ✅ Complete
- Feature Y: 🚧 In progress
- Feature Z: ⏸️ Blocked

## Action Items

- [ ] Alice: Complete documentation
- [ ] Bob: Fix bug #123
- [ ] Charlie: Review PR #456

## Next Meeting

2025-11-18, 2:00 PM
```

### 4. Blog Posts

```markdown
# How to Write Great Markdown

*Published: November 11, 2025*

Markdown is the secret weapon of technical writers...

## Why Markdown?

Here are three reasons...

## Getting Started

First, you need...

---

*Tags: #markdown #writing #documentation*
```

---

## Tips & Tricks

### 🎯 Pro Tips

1. **Use a linter**: Check syntax with markdownlint
2. **Preview often**: Verify formatting as you write
3. **Keep it simple**: Markdown works best when clean
4. **Learn shortcuts**: Master common patterns
5. **Version control**: Markdown works great with Git

### 🚀 Advanced Techniques

**Footnotes** (if supported):
```markdown
Here's a sentence with a footnote[^1].

[^1]: This is the footnote content.
```

**Definition Lists** (if supported):
```markdown
Term
: Definition

Another term
: Another definition
```

**Abbreviations** (if supported):
```markdown
The HTML specification is maintained by W3C.

*[HTML]: Hyper Text Markup Language
*[W3C]: World Wide Web Consortium
```

---

## Troubleshooting

### Lists not rendering?
- Ensure blank line before list
- Check indentation (2 or 4 spaces, be consistent)
- Verify marker (-, *, +) is followed by space

### Code not highlighting?
- Verify language name is correct
- Check for typos in fence markers (```)
- Ensure fence markers on separate lines

### Links broken?
- Check URL format (http:// or https://)
- Verify relative path correctness
- Escape special characters in URLs

### Images not showing?
- Check file path (relative to document)
- Verify image file exists
- Check file permissions

---

## External Resources

### Learn More
- [CommonMark Spec](https://commonmark.org/) - Official specification
- [GitHub Flavored Markdown](https://github.github.com/gfm/) - GFM spec
- [Markdown Guide](https://www.markdownguide.org/) - Comprehensive guide
- [Daring Fireball](https://daringfireball.net/projects/markdown/) - Original Markdown

### Tools
- [markdownlint](https://github.com/markdownlint/markdownlint) - Linting tool
- [Prettier](https://prettier.io/) - Code formatter (supports Markdown)
- [Mermaid](https://mermaid-js.github.io/) - Diagrams in Markdown

### Cheat Sheets
- [Markdown Cheat Sheet](https://www.markdownguide.org/cheat-sheet/)
- [GitHub Markdown Syntax](https://docs.github.com/en/get-started/writing-on-github)

---

## Next Steps

- **[Todo.txt Format →](./todotxt.md)** - Task management format
- **[CSV Format →](./csv.md)** - Data tables
- **[LaTeX Format →](./latex.md)** - Scientific documents
- **[Back to Getting Started →](../getting-started.md)**

---

*Last updated: November 11, 2025*
*Yole version: 2.15.1+*
