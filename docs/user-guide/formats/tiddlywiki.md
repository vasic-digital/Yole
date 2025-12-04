# TiddlyWiki Format Guide

**Format**: TiddlyWiki (Tiddler)
**Extensions**: `.tid`, `.tiddler`
**Specification**: [TiddlyWiki Documentation](https://tiddlywiki.com/)
**Yole Support**: ✅ Syntax highlighting, basic parsing

---

## Overview

TiddlyWiki is a unique non-linear personal wiki where each note (called a "tiddler") is a separate entity. The `.tid` format stores individual tiddlers as plain text files with metadata headers and content.

### Why TiddlyWiki?

- **Non-Linear**: Build your own knowledge web
- **Self-Contained**: Each tiddler is a complete unit
- **Flexible**: Organize by tags, not hierarchy
- **Portable**: Plain text files
- **Powerful**: Transclusion, macros, and filters
- **Personal**: Your own customizable wiki

---

## Tiddler File Format

### Basic Structure

```tiddler
created: 20251111120000000
modified: 20251111120500000
tags: Documentation Example
title: My First Tiddler
type: text/vnd.tiddlywiki

This is the content of the tiddler.

It can contain multiple paragraphs.
```

**Structure**:
1. **Metadata fields**: Key-value pairs at the top
2. **Blank line**: Separates metadata from content
3. **Content**: The actual tiddler text

### Required Fields

```tiddler
title: Tiddler Title
type: text/vnd.tiddlywiki

Content here.
```

**Minimum required**:
- `title`: Tiddler name
- `type`: Content type (usually `text/vnd.tiddlywiki`)

### Common Fields

```tiddler
created: 20251111120000000
modified: 20251111120500000
tags: Tag1 Tag2 [[Multi Word Tag]]
title: Example Tiddler
type: text/vnd.tiddlywiki
color: #FF0000
icon: 📝
custom-field: Custom value

Content goes here.
```

**Common metadata fields**:
- `created`: Creation timestamp
- `modified`: Last modified timestamp
- `tags`: Space-separated tags
- `title`: Tiddler title
- `type`: Content MIME type
- `color`: Custom color
- `icon`: Unicode icon
- Custom fields allowed!

---

## Timestamp Format

```tiddler
created: 20251111093045123
modified: 20251111153022456
```

**Format**: `YYYYMMDDhhmmssSSS`
- Year (4 digits)
- Month (2 digits)
- Day (2 digits)
- Hour (2 digits, 24h)
- Minute (2 digits)
- Second (2 digits)
- Milliseconds (3 digits)

---

## Tags

### Simple Tags

```tiddler
tags: Work Project Documentation
```

### Multi-Word Tags

```tiddler
tags: [[Project Alpha]] [[User Guide]] Important
```

Use double brackets `[[ ]]` for tags with spaces.

### Tag Hierarchy

```tiddler
tags: Work/Projects Work/Projects/Alpha
```

Slash creates hierarchy (displayed as nested in TiddlyWiki).

---

## Content Formatting

### Headings

```tiddlywiki
! Heading 1
!! Heading 2
!!! Heading 3
!!!! Heading 4
!!!!! Heading 5
!!!!!! Heading 6
```

### Text Formatting

```tiddlywiki
''bold text''
//italic text//
__underline__
^^superscript^^
,,subscript,,
~~strikethrough~~
`code`
```

**Rendered**:
- **bold text**
- *italic text*
- <u>underline</u>
- text^superscript^
- text~subscript~
- ~~strikethrough~~
- `code`

### Lists

**Unordered**:
```tiddlywiki
* Item 1
* Item 2
** Nested 2.1
** Nested 2.2
* Item 3
```

**Ordered**:
```tiddlywiki
# First
# Second
## Nested 2.1
## Nested 2.2
# Third
```

### Links

**Internal links**:
```tiddlywiki
[[Tiddler Name]]
[[Displayed Text|Tiddler Name]]
```

**External links**:
```tiddlywiki
[[Link Text|https://example.com]]
https://example.com
```

### Code Blocks

**Inline code**:
```tiddlywiki
Use `code` for inline.
```

**Code blocks**:
```tiddlywiki
```
def hello():
    print("Hello!")
`` `
```

**With syntax**:
```tiddlywiki
`` `python
def hello():
    print("Hello!")
`` `
```

---

## Special Features

### Transclusion

```tiddlywiki
{{TiddlerName}}
{{TiddlerName||TemplateName}}
```

Include content from another tiddler.

### Variables

```tiddlywiki
<<now>>
<<version>>
<<currentTiddler>>
```

### Macros

```tiddlywiki
<<tag TagName>>
<<list-links "[tag[Important]]">>
<<tabs "TabsTemplate">>
```

### Filters

```tiddlywiki
<$list filter="[tag[Work]sort[title]]">
<$link to=<<currentTiddler>>>
<<currentTiddler>>
</$link>
</$list>
```

---

## Content Types

### WikiText (default)

```tiddler
type: text/vnd.tiddlywiki

Standard TiddlyWiki markup content.
```

### Markdown

```tiddler
type: text/x-markdown

# Markdown Heading

**Bold** and *italic* text.
```

### Plain Text

```tiddler
type: text/plain

Plain text content
No formatting applied
```

### HTML

```tiddler
type: text/html

<h1>HTML Content</h1>
<p>Direct HTML markup.</p>
```

---

## TiddlyWiki in Yole

### Supported Features

✅ **Tiddler Format**: Metadata + content
✅ **Fields**: All standard fields
✅ **Tags**: Simple and multi-word
✅ **Text Formatting**: Bold, italic, etc.
✅ **Lists**: Unordered and ordered
✅ **Links**: Internal and external
✅ **Code Blocks**: Inline and block
✅ **Syntax Highlighting**: Tiddler-aware

### Syntax Highlighting

Yole highlights:
- **Metadata fields**: Field names and values
- **Tags**: Tag highlighting
- **Timestamps**: Date format
- **Content**: WikiText formatting
- **Links**: Link syntax
- **Code**: Code blocks
- **Headings**: Exclamation markers

### Limitations

❌ **Transclusion**: Not processed
❌ **Macros**: Not executed
❌ **Filters**: Not evaluated
❌ **Widgets**: Not rendered
❌ **TiddlyWiki Engine**: No wiki processing

**Recommendation**: Use TiddlyWiki for full features, Yole for editing individual tiddlers.

---

## Common Use Cases

### 1. Note Taking

```tiddler
created: 20251111120000000
modified: 20251111121500000
tags: Notes Learning Programming
title: Python Basics
type: text/vnd.tiddlywiki

! Python Basics

!! Variables

Variables don't need type declaration:

`` `python
x = 10
name = "Alice"
is_valid = True
`` `

!! Data Types

* ''int'': Integer numbers
* ''float'': Decimal numbers
* ''str'': Strings
* ''bool'': True/False
* ''list'': Ordered collection
* ''dict'': Key-value pairs

!! Control Flow

```python
if condition:
    # do something
elif other_condition:
    # do something else
else:
    # default action
`` `

!! References

See [[Python Functions]] and [[Python Classes]].

External: [[Python Docs|https://docs.python.org]]
```

### 2. Project Management

```tiddler
created: 20251111100000000
modified: 20251111150000000
tags: Projects [[Active Project]] Work
title: Website Redesign Project
type: text/vnd.tiddlywiki
status: In Progress
priority: High
deadline: 2025-12-01

! Website Redesign Project

!! Overview

Redesigning company website for better UX and modern look.

!! Team

* Alice - Frontend
* Bob - Backend
* Charlie - Design

!! Timeline

# Planning: Nov 1-10 {{done}}
# Design: Nov 11-20 {{in-progress}}
# Development: Nov 21-30
# Testing: Dec 1-5
# Launch: Dec 6

!! Tasks

!!! Planning Phase

* [x] Stakeholder interviews
* [x] Requirements document
* [x] Budget approval

!!! Design Phase

* [x] Wireframes
* [ ] Mockups
* [ ] Design system

!!! Development Phase

* [ ] Frontend implementation
* [ ] Backend API
* [ ] Database migration

!! Resources

* [[Design Mockups]]
* [[Technical Spec]]
* [[Budget Breakdown]]

!! Notes

Meeting with stakeholders went well. Design approved with minor changes requested.
```

### 3. Personal Knowledge Base

```tiddler
created: 20251111080000000
modified: 20251111090000000
tags: [[Knowledge Base]] Technology Reference
title: Git Commands Cheat Sheet
type: text/vnd.tiddlywiki
category: Development Tools
icon: 🔧

! Git Commands Cheat Sheet

!! Setup

```bash
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
`` `

!! Creating Repositories

| Command | Description |
| `git init` | Initialize new repo |
| `git clone <url>` | Clone existing repo |

!! Basic Workflow

# Make changes
# `git add .`
# `git commit -m "message"`
# `git push`

!! Branching

`` `bash
git branch feature-x          # Create branch
git checkout feature-x        # Switch branch
git checkout -b feature-x     # Create and switch
git merge feature-x           # Merge branch
git branch -d feature-x       # Delete branch
`` `

!! Useful Commands

* `git status` - Check status
* `git log` - View history
* `git diff` - See changes
* `git stash` - Stash changes
* `git pull` - Fetch and merge

!! Advanced

See [[Git Branching Strategies]] and [[Git Workflows]].

!! External Resources

* [[Pro Git Book|https://git-scm.com/book]]
* [[Git Documentation|https://git-scm.com/docs]]
```

### 4. Journal Entry

```tiddler
created: 20251111060000000
modified: 20251111200000000
tags: Journal [[Daily Notes]] 2025 November
title: Journal - 2025-11-11
type: text/vnd.tiddlywiki
mood: Productive

! Monday, November 11, 2025

!! Morning

Woke up at 6:00 AM feeling //refreshed//.

Completed:
* [x] Morning workout
* [x] Meditation (15 min)
* [x] Healthy breakfast

!! Work

Productive day at work:

* Finished [[Website Redesign Project]] wireframes
* Team meeting went well
* Fixed 5 bugs in [[Project Alpha]]

Notable:
> Had a breakthrough on the authentication issue. The problem was with the JWT token expiration handling.

!! Learning

Studied ''Python asyncio'' for 1 hour:

* Learned about `async/await`
* Practiced with examples
* See [[Python Asyncio Notes]]

!! Evening

Personal time:
* Read 30 pages of //Clean Code//
* Watched Python tutorial
* Prepared tomorrow's tasks

!! Gratitude

Three things I'm grateful for:
# Productive work day
# Team collaboration
# Learning progress

!! Tomorrow's Priorities

# [[Project Alpha]] - Fix remaining bugs
# [[Website Redesign]] - Start mockups
# Study session - Complete asyncio module

!! Linked Entries

* [[Journal - 2025-11-10]]
* [[Journal - 2025-11-12]]
```

---

## Best Practices

### Naming Conventions

**Good titles**:
```
Python Basics
Website Redesign Project
Meeting Notes - 2025-11-11
```

**Consistent naming**:
- Use Title Case for concepts
- Use descriptive names
- Include dates for time-based entries

### Tag Strategy

```tiddler
tags: Category [[Sub Category]] Status Type
```

**Organize with tags**:
- Subject tags: `Programming`, `Work`, `Personal`
- Status tags: `Active`, `Archived`, `Reference`
- Type tags: `Note`, `Project`, `Task`, `Journal`

### File Organization

```
tiddlers/
├── notes/
│   ├── python-basics.tid
│   ├── git-commands.tid
│   └── markdown-guide.tid
├── projects/
│   ├── website-redesign.tid
│   └── mobile-app.tid
└── journal/
    ├── 2025-11-11.tid
    ├── 2025-11-10.tid
    └── 2025-11-09.tid
```

### Metadata Consistency

**Always include**:
- `created` timestamp
- `modified` timestamp
- `tags` (at least one)
- `title`
- `type`

**Optional but useful**:
- `status`: Current status
- `priority`: Importance level
- `category`: Classification
- `icon`: Visual identifier

---

## Tips & Tricks

### 🎯 Pro Tips

1. **Tag liberally**: Tags are your organization system
2. **Link often**: Connect related tiddlers
3. **Use templates**: Create tiddler templates
4. **Regular review**: Update and archive
5. **Consistent timestamps**: Always update `modified`

### 🚀 Power User Techniques

**Template tiddler**:
```tiddler
created: 20251111120000000
tags: Template
title: Project Template
type: text/vnd.tiddlywiki

! {{!!title}}

!! Overview

Brief description

!! Goals

* Goal 1
* Goal 2

!! Timeline

# Phase 1
# Phase 2

!! Resources

* Link 1
* Link 2
```

**Tagging hierarchy**:
```tiddler
tags: Work Work/Projects Work/Projects/Active
```

**Custom fields for metadata**:
```tiddler
status: In Progress
priority: High
assignee: Alice
due-date: 2025-11-30
```

**Quick daily journal**:
```tiddler
created: <<now "YYYY0MM0DD0hh0mm0ss0XXX">>
modified: <<now "YYYY0MM0DD0hh0mm0ss0XXX">>
tags: Journal [[Daily Notes]]
title: Journal - <<now "YYYY-MM-DD">>
```

---

## External Resources

### Official
- [TiddlyWiki Website](https://tiddlywiki.com/)
- [TiddlyWiki Documentation](https://tiddlywiki.com/#GettingStarted)
- [Tiddler Format](https://tiddlywiki.com/#TiddlerFiles)

### Community
- [TiddlyWiki Talk](https://talk.tiddlywiki.org/)
- [TiddlyWiki Google Group](https://groups.google.com/g/tiddlywiki)

### Tools
- **TiddlyWiki**: Original single-file wiki
- **TiddlyDesktop**: Desktop app
- **Tw5-filesystem**: Node.js version with separate tiddlers
- **Yole**: Mobile tiddler editor

### Resources
- [Awesome TiddlyWiki](https://github.com/topics/tiddlywiki)
- [TiddlyWiki Plugins](https://tiddlywiki.com/#PluginMechanism)
- [TiddlyWiki Themes](https://tiddlywiki.com/#Themes)

---

## Next Steps

- **[WikiText Format →](./wikitext.md)** - MediaWiki markup
- **[Org Mode Format →](./orgmode.md)** - Emacs organization
- **[Markdown Format →](./markdown.md)** - General markup
- **[Back to Getting Started →](../getting-started.md)**

---

*Last updated: November 11, 2025*
*Yole version: 2.15.1+*
