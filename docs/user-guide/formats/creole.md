# Creole Format Guide

**Format**: Creole (Wiki Creole)
**Extensions**: `.creole`, `.wiki`
**Specification**: [Creole 1.0](http://www.wikicreole.org/wiki/Creole1.0)
**Yole Support**: ✅ Syntax highlighting, basic parsing

---

## Overview

Creole is a standardized wiki markup language designed to provide a common syntax across different wiki engines. Created in 2007, it aims to be a minimal, consistent, and easy-to-learn syntax that works everywhere.

### Why Creole?

- **Standardized**: Common syntax across different wikis
- **Simple**: Minimal, easy-to-remember syntax
- **Portable**: Write once, use in multiple wiki systems
- **Intuitive**: Natural formatting that makes sense
- **Consistent**: No conflicting syntax rules
- **Minimal**: Only essential features

---

## Basic Syntax

### Headings

```creole
= Level 1 Heading =
== Level 2 Heading ==
=== Level 3 Heading ===
==== Level 4 Heading ====
===== Level 5 Heading =====
====== Level 6 Heading ======
```

**Note**: Closing `=` is optional but recommended for consistency.

**Alternative** (without closing):
```creole
= Level 1 Heading
== Level 2 Heading
```

### Paragraphs

```creole
First paragraph.

Second paragraph.
Blank line separates paragraphs.

Third paragraph.
```

### Text Formatting

```creole
**bold text**
//italic text//
**//bold and italic//

**
```

**Rendered**:
- **bold text**
- *italic text*
- ***bold and italic***

**Note**: Creole deliberately has minimal formatting - no underline, strikethrough, or subscript/superscript.

---

## Lists

### Unordered Lists

```creole
* Item 1
* Item 2
** Nested item 2.1
** Nested item 2.2
*** Deep nested item
* Item 3
```

**Rendered**:
- Item 1
- Item 2
  - Nested item 2.1
  - Nested item 2.2
    - Deep nested item
- Item 3

### Ordered Lists

```creole
# First item
# Second item
## Nested 2.1
## Nested 2.2
### Deep nested
# Third item
```

**Rendered**:
1. First item
2. Second item
   1. Nested 2.1
   2. Nested 2.2
      1. Deep nested
3. Third item

### Mixed Lists

```creole
# Numbered item
#* Bullet sub-item
#* Another bullet
# Numbered item 2
#** Deep bullet
```

---

## Links

### Internal Links

```creole
[[PageName]]
[[PageName|Display Text]]
```

**Examples**:
```creole
See [[Introduction]] for basics.
Read the [[UserGuide|user guide]].
```

### External Links

```creole
[[https://example.com]]
[[https://example.com|Website]]
```

**Bare URLs** (auto-linked):
```creole
https://example.com
```

---

## Images

### Basic Images

```creole
{{image.jpg}}
{{image.jpg|Alt text}}
```

### External Images

```creole
{{https://example.com/image.jpg}}
{{https://example.com/image.jpg|Description}}
```

### Linked Images

```creole
[[https://example.com|{{image.jpg}}]]
```

---

## Line Breaks and Horizontal Rules

### Line Break

```creole
First line\\
Second line (on new line)
```

Double backslash `\\` creates a line break.

### Horizontal Rule

```creole
Text above.

----

Text below.
```

Four or more dashes create a horizontal rule.

---

## Preformatted Text

### Inline Code

```creole
Use {{{inline code}}} for short snippets.
```

**Rendered**: Use `inline code` for short snippets.

### Code Blocks

```creole
{{{
def hello():
    print("Hello, World!")
    return True
}}}
```

**Multi-line**:
```creole
{{{
Line 1
Line 2
Line 3
}}}
```

**Nowiki** (alternative syntax for same thing):
```creole
{{{
This text is not processed as wiki markup.
** This is not bold **
}}}
```

---

## Tables

### Simple Table

```creole
|= Header 1 |= Header 2 |= Header 3 |
| Cell 1    | Cell 2    | Cell 3    |
| Cell 4    | Cell 5    | Cell 6    |
```

**Note**: `|=` marks header cells, `|` marks regular cells.

### Table without Headers

```creole
| Cell 1 | Cell 2 | Cell 3 |
| Cell 4 | Cell 5 | Cell 6 |
```

### Multi-line Cells

```creole
|= Header |
| Cell with\\multi-line\\content |
```

---

## Advanced Features

### Escaping

```creole
~** This is not bold **

~[[Not a link]]

~{{not an image}}
```

Tilde `~` escapes the next character/markup.

### Placeholders

```creole
<<TableOfContents>>

<<UserPreferences>>

<<RecentChanges>>
```

**Note**: Placeholders are wiki-engine specific and may vary.

---

## Creole Extensions (Optional)

These are **not** in the core Creole 1.0 spec but are common extensions:

### Superscript and Subscript

```creole
Text with ^^superscript^^

Text with ,,subscript,,
```

**Note**: Not all wiki engines support these.

### Underline

```creole
__underlined text__
```

**Note**: Not in core spec.

### Monospace

```creole
##monospace text##
```

**Note**: Alternative to `{{{inline}}}`.

### Definition Lists

```creole
; Term
: Definition

; Another term
: Another definition
```

**Note**: Extension, not core Creole.

---

## Creole in Yole

### Supported Features

✅ **Headings**: All 6 levels
✅ **Text Formatting**: Bold and italic
✅ **Lists**: Unordered, ordered, mixed
✅ **Links**: Internal and external
✅ **Images**: Basic and external
✅ **Line Breaks**: Double backslash
✅ **Horizontal Rules**: Quad-dash
✅ **Code Blocks**: Triple-brace syntax
✅ **Tables**: Simple tables
✅ **Syntax Highlighting**: Creole-aware

### Syntax Highlighting

Yole highlights:
- **Headings**: Equal-sign markers
- **Bold/Italic**: Formatting markers
- **Links**: Double-bracket syntax
- **Images**: Double-brace syntax
- **Lists**: Markers (* and #)
- **Tables**: Pipe characters
- **Code**: Triple-brace blocks

### Limitations

❌ **Placeholders**: Not processed (shown as-is)
❌ **Wiki-Specific**: No wiki engine features
❌ **Extensions**: Limited extension support
❌ **Link Resolution**: Links not validated

**Recommendation**: Use wiki engine for full features, Yole for editing.

---

## Common Use Cases

### 1. Wiki Page

```creole
= Project Documentation =

Welcome to the project wiki!

== Overview ==

This project is a **simple** and //easy-to-use// tool for doing amazing things.

=== Features ===

* Feature 1: Fast processing
* Feature 2: Easy interface
* Feature 3: Great documentation

=== Requirements ===

# Operating System: Windows/Mac/Linux
# RAM: 4GB minimum
# Disk Space: 100MB

== Getting Started ==

See the [[Installation]] page to get started.

=== Quick Start ===

{{{
git clone https://github.com/user/project.git
cd project
./install.sh
}}}

== Documentation ==

* [[UserGuide|User Guide]]
* [[APIReference|API Reference]]
* [[FAQ]]

== External Resources ==

Visit our website: [[https://example.com|Project Website]]

----

//Last updated: 2025-11-11//
```

### 2. Meeting Notes

```creole
= Team Meeting - November 11, 2025 =

== Attendees ==

* Alice (Product Manager)
* Bob (Engineering)
* Charlie (Design)

== Agenda ==

# Project status update
# Q4 planning
# Budget review

== Discussion ==

=== Project Status ===

Current progress:
* Frontend: **80% complete**
* Backend: **60% complete**
* Design: **100% complete**

=== Q4 Planning ===

Goals for Q4:
# Launch beta version
# Onboard 100 users
# Gather feedback

=== Budget Review ===

|= Category      |= Budget  |= Spent   |= Remaining |
| Development    | $50,000  | $30,000  | $20,000    |
| Marketing      | $20,000  | $5,000   | $15,000    |
| Operations     | $10,000  | $8,000   | $2,000     |

== Action Items ==

* Alice: Prepare launch plan
* Bob: Fix critical bugs
* Charlie: Finalize assets

== Next Meeting ==

**Date**: November 18, 2025\\
**Time**: 10:00 AM\\
**Location**: Conference Room B

----

<<AttachFile>>
```

### 3. Knowledge Base Article

```creole
= How to Install the Software =

This guide explains the installation process.

== Before You Start ==

Make sure you have:
* Administrator access
* Internet connection
* At least 500MB free space

== Installation Steps ==

=== Windows ===

# Download installer: [[https://example.com/installer.exe|Download]]
# Run {{{installer.exe}}}
# Follow on-screen instructions
# Restart computer when prompted

=== macOS ===

# Download DMG: [[https://example.com/installer.dmg|Download]]
# Open {{{installer.dmg}}}
# Drag app to Applications folder
# Open app and grant permissions

=== Linux ===

{{{
wget https://example.com/install.sh
chmod +x install.sh
./install.sh
}}}

== Verification ==

Test the installation:

{{{
$ myapp --version
MyApp version 1.0.0
}}}

== Troubleshooting ==

**Problem**: Installation fails

**Solution**: Check:
* Administrator rights
* Antivirus settings
* Disk space

See [[Troubleshooting]] for more help.

== Next Steps ==

* [[QuickStart|Quick Start Guide]]
* [[UserManual|User Manual]]
* [[FAQ]]

----

//Need help? Contact [[support@example.com]]//
```

### 4. Personal Wiki Page

```creole
= Programming Resources =

My collection of useful programming resources.

== Languages ==

=== Python ===

**Learning Resources:**
* [[https://docs.python.org/3/tutorial/|Official Tutorial]]
* [[https://realpython.com|Real Python]]

**Libraries:**
* //requests// - HTTP library
* //pandas// - Data analysis
* //flask// - Web framework

=== JavaScript ===

**Frameworks:**
# React - UI library
# Vue - Progressive framework
# Node.js - Server-side JS

**Tools:**
* npm - Package manager
* webpack - Module bundler

== Tools ==

|= Tool      |= Purpose           |= Link                          |
| Git        | Version control    | [[https://git-scm.com]]        |
| VS Code    | Code editor        | [[https://code.visualstudio.com]] |
| Docker     | Containers         | [[https://docker.com]]         |

== Code Snippets ==

=== Python Virtual Environment ===

{{{
python -m venv venv
source venv/bin/activate  # Unix
venv\Scripts\activate     # Windows
}}}

=== Git Basics ===

{{{
git init
git add .
git commit -m "Initial commit"
git push origin main
}}}

== Books ==

* **Clean Code** by Robert Martin
* **The Pragmatic Programmer** by Hunt & Thomas
* **Design Patterns** by Gang of Four

== My Notes ==

Personal observations:
* Always write tests first
* Keep functions small
* Use meaningful names
* Comment the //why//, not the //what//

----

<<RecentChanges>>
```

---

## Best Practices

### Consistent Heading Style

**Good** (with closing):
```creole
= Title =
== Section ==
=== Subsection ===
```

**Also good** (without closing):
```creole
= Title
== Section
=== Subsection
```

**Pick one style and stick to it.**

### Readable Links

**Good**:
```creole
See the [[UserGuide|user guide]] for details.
```

**Avoid**:
```creole
Click [[UserGuide|here]].
```

### Table Formatting

**Align for readability**:
```creole
|= Name    |= Age |= City        |
| Alice    | 30   | New York     |
| Bob      | 25   | Los Angeles  |
| Charlie  | 35   | Chicago      |
```

### Minimal Markup

**Keep it simple**:
```creole
This is **important** information.
```

**Don't overdo it**:
```creole
This is **really ~very ^^super^^ //important//** information!!!
```

---

## Tips & Tricks

### 🎯 Pro Tips

1. **Stay minimal**: Creole is designed to be simple
2. **Standard syntax**: Don't rely on extensions
3. **Wiki-agnostic**: Keep content portable
4. **Consistent style**: Pick conventions early
5. **Test compatibility**: Check across wiki engines

### 🚀 Power User Techniques

**Nested lists**:
```creole
# Level 1
## Level 2
### Level 3
#### Level 4 (if supported)
```

**Combined formatting**:
```creole
**//Bold and italic//**
{{{**Not processed**}}}
```

**Escaped syntax**:
```creole
To type asterisks: ~** not bold ~**
To show code: use {{{three braces}}}
```

**Tables with links**:
```creole
|= Page        |= Description            |
| [[Home]]     | Main page               |
| [[About]]    | About this wiki         |
| [[Contact]]  | [[mailto:me@example.com]] |
```

---

## Creole vs. Other Wiki Formats

### vs. MediaWiki (WikiText)

| Feature | Creole | MediaWiki |
|---------|--------|-----------|
| **Bold** | `**text**` | `'''text'''` |
| **Italic** | `//text//` | `''text''` |
| **Headings** | `= text =` | `== text ==` |
| **Links** | `[[Page]]` | `[[Page]]` |
| **Simplicity** | High | Moderate |
| **Features** | Minimal | Rich |

### vs. Markdown

| Feature | Creole | Markdown |
|---------|--------|----------|
| **Headings** | `= text =` | `# text` |
| **Bold** | `**text**` | `**text**` |
| **Italic** | `//text//` | `*text*` |
| **Links** | `[[url\|text]]` | `[text](url)` |
| **Purpose** | Wiki interchange | General markup |

### When to Use Creole

**Use Creole** when:
- Writing for multiple wiki engines
- You want maximum portability
- Simplicity is priority
- You're building a wiki

**Use MediaWiki** when:
- Using Wikipedia/MediaWiki specifically
- You need advanced features
- Templates are required

**Use Markdown** when:
- Writing general documents
- Maximum tool support needed
- Not wiki-specific

---

## External Resources

### Official
- [WikiCreole.org](http://www.wikicreole.org/)
- [Creole 1.0 Specification](http://www.wikicreole.org/wiki/Creole1.0)
- [Creole Cheat Sheet](http://www.wikicreole.org/wiki/CheatSheet)

### Wiki Engines with Creole Support
- **DokuWiki**: Via plugin
- **MoinMoin**: Built-in support
- **XWiki**: Native support
- **JSPWiki**: Via plugin

### Tools
- **Online Editor**: Various wiki engines provide test areas
- **Parsers**: Available for Python, JavaScript, PHP, Ruby
- **Yole**: Mobile Creole editor

### Converters
- **Creole to HTML**: Multiple implementations
- **Markdown to Creole**: Available tools
- **Creole to Markdown**: Conversion utilities

---

## Next Steps

- **[WikiText Format →](./wikitext.md)** - MediaWiki markup
- **[Markdown Format →](./markdown.md)** - General markup
- **[Org Mode Format →](./orgmode.md)** - Emacs organization
- **[Back to Getting Started →](../getting-started.md)**

---

*Last updated: November 11, 2025*
*Yole version: 2.15.1+*
