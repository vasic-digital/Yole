# Textile Format Guide

**Format**: Textile
**Extensions**: `.textile`, `.txtl`
**Specification**: [Textile Markup Language](https://textile-lang.com/)
**Yole Support**: ✅ Syntax highlighting, basic parsing

---

## Overview

Textile is a lightweight markup language created by Dean Allen in 2002. Originally designed for use in Textpattern CMS, it provides a simple way to write structured documents with a clean, readable syntax.

### Why Textile?

- **Simple Syntax**: Easy to learn and remember
- **Clean Output**: Generates semantic HTML
- **Fast to Write**: Quick formatting with minimal keystrokes
- **CMS Integration**: Used in Textpattern, Redmine, and others
- **Readable**: Plain text remains readable
- **Legacy Content**: Still used in many existing systems

---

## Basic Syntax

### Headings

```textile
h1. Heading 1

h2. Heading 2

h3. Heading 3

h4. Heading 4

h5. Heading 5

h6. Heading 6
```

**With styling**:
```textile
h1(#id). Heading with ID

h2(class). Heading with class

h3{color:red}. Styled heading
```

### Paragraphs

```textile
First paragraph.

Second paragraph.

p. Explicit paragraph.

p<. Left-aligned paragraph.

p>. Right-aligned paragraph.

p=. Centered paragraph.

p<>. Justified paragraph.
```

### Text Formatting

```textile
*bold text*
_italic text_
**strong text**
__emphasis__
-deleted text-
+inserted text+
^superscript^
~subscript~
@code@
??citation??
```

**Rendered**:
- **bold text**
- *italic text*
- **strong text**
- *emphasis*
- ~~deleted text~~
- <ins>inserted text</ins>
- text^superscript^
- text~subscript~
- `code`
- <cite>citation</cite>

---

## Lists

### Unordered Lists

```textile
* Item 1
* Item 2
** Nested item 2.1
** Nested item 2.2
* Item 3
```

### Ordered Lists

```textile
# Item 1
# Item 2
## Nested item 2.1
## Nested item 2.2
# Item 3
```

### Definition Lists

```textile
- Term := Definition
- Another term := Another definition
```

---

## Links and Images

### Links

```textile
"Link text":https://example.com

"Link with title (hover)":https://example.com(Link Title)

"Link with class":https://example.com{class:external}
```

### Images

```textile
!image.jpg!

!image.jpg(Alt text)!

!image.jpg!:https://example.com

!<image.jpg! (left-aligned)

!>image.jpg! (right-aligned)

!{width:300px}image.jpg!
```

### Image with attributes

```textile
!{height:100px}image.jpg(Alt text)!

!/images/photo.jpg(My Photo)!

!^image.jpg! (aligned top)
```

---

## Tables

### Simple Table

```textile
|_. Header 1 |_. Header 2 |_. Header 3 |
| Cell 1 | Cell 2 | Cell 3 |
| Cell 4 | Cell 5 | Cell 6 |
```

### Table with Alignment

```textile
|_. Name |_. Age |_. City |
|<. Left-aligned | Cell | Cell |
|>. Right-aligned | Cell | Cell |
|=. Centered | Cell | Cell |
```

### Table with Spanning

```textile
|_. Header 1 |_. Header 2 |
|\2. Spans 2 columns |
| Cell 1 | Cell 2 |
|/2. Spans 2 rows | Cell |
| Cell |
```

### Styled Table

```textile
table{border:1px solid black}.
|_. Header 1 |_. Header 2 |
| Data 1 | Data 2 |
| Data 3 | Data 4 |
```

---

## Block Elements

### Block Quote

```textile
bq. This is a blockquote.

bq.:https://example.com This is a blockquote with citation.
```

### Code Block

```textile
bc. def hello():
    print("Hello, World!")
    return True
```

**With language** (if supported):
```textile
bc(python). def hello():
    print("Hello, World!")
```

### Preformatted Text

```textile
pre. Preformatted text
     preserves spaces
         and indentation
```

### Extended Block

```textile
bc.. This is the first paragraph of extended block.

This is the second paragraph.

Code continues here.

p. This ends the extended block.
```

---

## Styling and Attributes

### Classes and IDs

```textile
p(classname). Paragraph with class.

p(#id). Paragraph with ID.

p(class1 class2). Multiple classes.

p(class#id). Class and ID.
```

### Inline Styles

```textile
p{color:red}. Red paragraph.

p{color:blue;background:yellow}. Styled paragraph.

h2{font-size:2em}. Large heading.
```

### Alignment

```textile
p<. Left-aligned paragraph.

p>. Right-aligned paragraph.

p=. Centered paragraph.

p<>. Justified paragraph.
```

### Padding

```textile
p(. One em padding on left.

p). One em padding on right.

p((. Two em padding on left.
```

---

## Special Features

### Footnotes

```textile
This text has a footnote[1].

fn1. This is the footnote text.
```

### Acronyms

```textile
HTML(Hypertext Markup Language) is widely used.

First use: CSS(Cascading Style Sheets)
Later uses: CSS automatically gets abbr tag.
```

### No Textile Zones

```textile
<notextile>
This text is *not* processed.
Textile syntax is ignored here.
</notextile>

==This is also not processed==
```

### HTML Pass-through

```textile
Some text with <span class="highlight">HTML</span> embedded.

<div class="custom">
Block-level HTML is also supported.
</div>
```

---

## Advanced Syntax

### Horizontal Rule

```textile
Regular paragraph.

---

Another paragraph after horizontal rule.
```

### Line Breaks

```textile
First line
Second line (soft break)

First line
Second line (hard break using BR syntax)
```

### Span Elements

```textile
%{color:red}red text%

%{font-size:larger}larger text%

%(classname)styled span%
```

### Language Attribute

```textile
p[en]. English text.

p[fr]. French text: Bonjour!

span[de]German word% in English sentence.
```

---

## Textile in Yole

### Supported Features

✅ **Text Formatting**: Bold, italic, code, etc.
✅ **Headings**: All 6 levels
✅ **Lists**: Unordered, ordered, definition
✅ **Links**: External and titled
✅ **Images**: Basic image syntax
✅ **Tables**: Simple tables
✅ **Block Quotes**: Quote blocks
✅ **Code Blocks**: Preformatted code
✅ **Syntax Highlighting**: Textile-aware

### Syntax Highlighting

Yole highlights:
- **Headings**: h1-h6 markers
- **Text formatting**: Bold, italic, code markers
- **Lists**: List markers
- **Links**: Link syntax
- **Tables**: Table structure
- **Attributes**: Classes, IDs, styles
- **Special elements**: Footnotes, acronyms

### Limitations

❌ **HTML Generation**: No HTML output (yet)
❌ **Complex Attributes**: Some styling not rendered
❌ **Footnote Processing**: Footnotes displayed but not linked
❌ **Acronym Expansion**: Not processed
❌ **Advanced Tables**: Complex spanning not fully supported

**Recommendation**: Use Textile processor for final output, Yole for editing.

---

## Common Use Cases

### 1. Blog Post

```textile
h1. My First Textile Blog Post

p(meta). Published on November 11, 2025 by John Doe

This is the introduction to my blog post. I'm using Textile because it's *simple* and _elegant_.

h2. Main Content

Here's the main content with:

* Bullet points
* For easy reading
* And quick formatting

And numbered lists:

# First point
# Second point
# Third point

bq. "This is a great quote from someone famous."
- Famous Person

h2. Code Example

Here's some code:

bc. def example():
    print("Textile supports code blocks!")
    return True

h2. Conclusion

That's all for now! Check out more at "my website":https://example.com.

fn1. Footnotes are easy too!
```

### 2. Documentation Page

```textile
h1. User Guide

table{border:1px solid #ccc}.
|_. Version |_. Date |_. Author |
| 1.0 | 2025-11-11 | Documentation Team |

h2. Getting Started

Follow these steps:

# Download the software from "our website":https://example.com
# Install using the installer
# Run the application

h3. System Requirements

* Operating System: Windows 10+, macOS 10.14+, Linux
* RAM: 4GB minimum
* Disk Space: 500MB

h2. Configuration

Edit the config file:

bc. [settings]
debug=false
port=8080
timeout=30

h2. Usage

Basic usage example:

bc. $ myapp --input file.txt --output result.txt

h3. Command-Line Options

table.
|_. Option |_. Description |
| @--input@ | Input file path |
| @--output@ | Output file path |
| @--verbose@ | Enable verbose logging |

h2. Troubleshooting

*Common Issues:*

* If application won't start, check logs at @/var/log/myapp.log@
* For permission errors, run as administrator
* See "FAQ":faq.html for more help

h2. Support

Contact us:

* Email: support@example.com
* Forum: "community.example.com":https://community.example.com
* Docs: "docs.example.com":https://docs.example.com
```

### 3. README

```textile
h1. MyProject

!https://img.shields.io/badge/version-1.0.0-blue.svg!

A simple project using Textile for documentation.

h2. Features

* Feature 1: Fast processing
* Feature 2: Easy to use
* Feature 3: Well documented

h2. Installation

bc. npm install myproject

Or:

bc. pip install myproject

h2. Quick Start

bc. from myproject import Tool

tool = Tool()
result = tool.process("input")

h2. Documentation

See the "full documentation":https://docs.example.com for details.

h2. License

MIT License - see "LICENSE":LICENSE file.
```

### 4. Forum Post

```textile
h2. How to Use Textile

Hi everyone! I wanted to share some tips on using Textile markup.

h3. Why Textile?

Textile is great because:

* *Simple syntax* - easy to learn
* *Fast to write* - minimal typing
* *Readable* - source is clean

h3. Basic Formatting

Here's what you can do:

* Make text *bold* or _italic_
* Add "links":https://example.com
* Create `code snippets`
* And much more!

h3. Example

Try this:

bc. h2. My Heading

This is *bold* and this is _italic_.

* List item 1
* List item 2

Simple!

h3. Resources

Check out:

* "Textile Documentation":https://textile-lang.com
* "Textile Sandbox":https://txstyle.org
* "Textpattern CMS":https://textpattern.com

Hope this helps! ????
```

---

## Best Practices

### File Organization

```
docs/
├── index.textile        # Main page
├── guide.textile        # User guide
├── api.textile          # API docs
└── faq.textile          # FAQ
```

### Consistent Style

**Use consistent heading hierarchy**:
```textile
h1. Document Title

h2. Major Section

h3. Subsection

h4. Minor Section
```

**Don't skip levels** (no h3 directly after h1).

### Readable Markup

**Good**:
```textile
h2. Section Title

This is a paragraph with *bold* text.

* List item 1
* List item 2
```

**Avoid excessive styling**:
```textile
h2{color:red;font-size:24px;background:yellow}. Over-styled

Better to use classes and external CSS.
```

### Links

**Descriptive link text**:
```textile
# Good
See the "user guide":guide.html for details.

# Avoid
Click "here":guide.html for the guide.
```

---

## Tips & Tricks

### 🎯 Pro Tips

1. **Keep it simple**: Don't over-use styling attributes
2. **Use classes**: Apply styling via CSS, not inline
3. **Test output**: Preview rendered HTML
4. **Consistent format**: Pick conventions and stick to them
5. **Document templates**: Create reusable patterns

### 🚀 Power User Techniques

**Custom classes**:
```textile
p(callout). This is a special callout paragraph.

p(warning). Warning text here.

p(success). Success message.
```

**Span styling**:
```textile
This is %(badge)version 1.0% of the software.

Price: %{color:green}$19.99%
```

**Attribute combinations**:
```textile
h2(#intro.section). Section with ID and class.

p(<class). Left-aligned with class.

table{border:1px solid}.
|_. Header |
| Data |
```

---

## Textile vs. Markdown

### Similarities

- Plain text markup
- Easy to read and write
- Convert to HTML
- Support basic formatting

### Differences

| Feature | Textile | Markdown |
|---------|---------|----------|
| **Headings** | `h1.` syntax | `#` syntax |
| **Bold** | `*bold*` | `**bold**` |
| **Italic** | `_italic_` | `*italic*` |
| **Tables** | Full support | Limited (GFM) |
| **Attributes** | Rich (classes, IDs, styles) | Limited |
| **Learning Curve** | Moderate | Easy |
| **Popularity** | Lower | Higher |

### When to Use Each

**Use Textile** when:
- You need rich attributes
- You're using Textpattern/Redmine
- You want fine-grained control
- You have legacy Textile content

**Use Markdown** when:
- You want simplicity
- You need wide compatibility
- You're starting fresh
- You prefer minimal syntax

---

## External Resources

### Official
- [Textile Language](https://textile-lang.com/)
- [Textile Reference](https://textile-lang.com/doc/)

### Tools
- **Textpattern CMS**: Native Textile support
- **Redmine**: Issue tracking with Textile
- **Textile Editor**: Online editor at txstyle.org
- **Yole**: Mobile Textile editor

### Converters
- **PHP Textile**: PHP implementation
- **Python Textile**: Python implementation
- **RedCloth**: Ruby implementation

### Resources
- [Textile Try It](https://txstyle.org/)
- [Textile Syntax](https://textile-lang.com/doc/syntax-cheat-sheet)

---

## Next Steps

- **[Markdown Format →](./markdown.md)** - More popular alternative
- **[WikiText Format →](./wikitext.md)** - Wiki markup
- **[reStructuredText Format →](./restructuredtext.md)** - Technical documentation
- **[Back to Getting Started →](../getting-started.md)**

---

*Last updated: November 11, 2025*
*Yole version: 2.15.1+*
