# Markdown Format Guide

## Overview

Markdown is a lightweight markup language for creating formatted text using a plain-text editor. Yole provides comprehensive Markdown support including CommonMark, GitHub Flavored Markdown (GFM), and various extensions.

## Supported Extensions

- `.md` - Standard Markdown
- `.markdown` - Markdown with alternate extension
- `.mdown` - Markdown Down
- `.mkd` - Markdown
- `.mkdn` - Markdown Neutral

## Features

### Headers
```markdown
# H1
## H2
### H3
#### H4
##### H5
###### H6
```

### Text Formatting
- **Bold**: `**text**` or `__text__`
- *Italic*: `*text*` or `_text_`
- ~~Strikethrough~~: `~~text~~`
- `Code`: `` `code` ``

### Lists
- Unordered: `- item`, `* item`, `+ item`
- Ordered: `1. item`, `2. item`
- Task lists: `- [ ] uncompleted`, `- [x] completed`

### Links and Images
- Link: `[text](url)`
- Image: `![alt](url)`
- Reference: `[text][ref]`

### Code Blocks
- Inline: `` `code` ``
- Fenced: ```` ```language ````
- Indented: 4 spaces

### Tables
```markdown
| Header | Header |
|--------|--------|
| Cell   | Cell   |
```

### Blockquotes
```markdown
> Quote text
> Multiple lines
```

### Horizontal Rule
`---`, `***`, or `___`

## Parser Configuration

The Markdown parser supports:
- CommonMark specification
- GitHub Flavored Markdown (GFM)
- Tables, task lists, strikethrough
- Autolinks
- Emoji shortcodes
- Footnotes

## See Also

- [CommonMark Spec](https://commonmark.org)
- [GitHub Flavored Markdown](https://github.github.com/gfm/)
