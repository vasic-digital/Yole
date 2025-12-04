# AsciiDoc Format Guide

**Format**: AsciiDoc
**Extensions**: `.adoc`, `.asciidoc`, `.asc`
**Specification**: [AsciiDoc Language](https://docs.asciidoctor.org/)
**Yole Support**: ✅ Syntax highlighting, basic parsing

---

## Overview

AsciiDoc is a lightweight markup language for authoring technical documentation, articles, books, and more. It's designed to be easy to write while providing powerful features for complex documents.

### Why AsciiDoc?

- **Powerful**: Features for technical writing (admonitions, includes, macros)
- **Flexible Output**: Convert to HTML, PDF, EPUB, DocBook, and more
- **Version Control Friendly**: Plain text, perfect for Git
- **Book Publishing**: Used for O'Reilly books and technical manuals
- **Professional**: Industry-standard for technical documentation
- **Extensible**: Custom macros and extensions

---

## Basic Syntax

### Document Title

```asciidoc
= Document Title
Author Name <author@example.com>
v1.0, 2025-11-11
```

First line with `=` is the document title. Optional author and revision info follows.

### Headings

```asciidoc
= Document Title (Level 0)

== Level 1 Heading

=== Level 2 Heading

==== Level 3 Heading

===== Level 4 Heading

====== Level 5 Heading
```

### Paragraphs

```asciidoc
First paragraph.

Second paragraph.
Blank line separates paragraphs.

Third paragraph with
a line break.
```

### Text Formatting

```asciidoc
*bold text*
_italic text_
*_bold and italic_*
`monospace/code`
^superscript^
~subscript~
#highlighted#
```

**Rendered**:
- **bold text**
- *italic text*
- ***bold and italic***
- `monospace/code`
- Text^superscript^
- Text~subscript~
- ==highlighted==

---

## Lists

### Unordered Lists

```asciidoc
* Item 1
* Item 2
** Nested item 2.1
** Nested item 2.2
*** Deep nested item
* Item 3
```

**Alternative markers**:
```asciidoc
- Item with dash
* Item with asterisk
```

### Ordered Lists

```asciidoc
. First item
. Second item
.. Nested 2.1
.. Nested 2.2
... Deep nested
. Third item
```

**With explicit numbering**:
```asciidoc
1. First
2. Second
3. Third
```

### Labeled Lists (Definition Lists)

```asciidoc
Term 1:: Definition 1
Term 2:: Definition 2
Term 3::
  Definition 3 with more detail.
  Can span multiple lines.
```

### Checklist

```asciidoc
* [*] Checked item
* [x] Also checked
* [ ] Unchecked item
```

---

## Links and Cross-References

### External Links

```asciidoc
https://example.com[Link Text]
https://example.com
link:https://example.com[Link with link: macro]
mailto:user@example.com[Email]
```

### Internal Cross-References

```asciidoc
<<section-id>>
<<section-id,Custom Link Text>>

[[section-id]]
== Section Title
```

**Example**:
```asciidoc
See <<installation>> for setup instructions.

[[installation]]
== Installation

Installation steps here...
```

### Document Attributes in Links

```asciidoc
:project-url: https://github.com/user/project

See {project-url}[the project] for details.
```

---

## Code Blocks

### Inline Code

```asciidoc
Use `inline code` for short snippets.
```

### Code Blocks

```asciidoc
----
Basic code block
with preserved formatting
----
```

### Syntax Highlighting

```asciidoc
[source,python]
----
def hello():
    print("Hello, World!")
    return True
----
```

**With line numbers**:
```asciidoc
[source,javascript,linenums]
----
function greet(name) {
    console.log(`Hello, ${name}!`);
}
----
```

### Code Callouts

```asciidoc
[source,java]
----
public class Example {
    public static void main(String[] args) {  // <1>
        System.out.println("Hello!");         // <2>
    }
}
----
<1> Main method entry point
<2> Print greeting to console
```

---

## Tables

### Simple Table

```asciidoc
|===
| Column 1 | Column 2 | Column 3

| Row 1, Cell 1 | Row 1, Cell 2 | Row 1, Cell 3
| Row 2, Cell 1 | Row 2, Cell 2 | Row 2, Cell 3
|===
```

### Table with Header

```asciidoc
[options="header"]
|===
| Name | Age | City

| Alice | 30 | New York
| Bob | 25 | Los Angeles
| Charlie | 35 | Chicago
|===
```

### Compact Table Syntax

```asciidoc
|===
| Name    | Age | City

| Alice   | 30  | New York
| Bob     | 25  | Los Angeles
| Charlie | 35  | Chicago
|===
```

### Table with Column Specs

```asciidoc
[cols="1,2,3"]
|===
| Narrow | Medium | Wide

| A | B | C
| D | E | F
|===
```

### CSV Data in Table

```asciidoc
[%header,format=csv]
|===
Name,Age,City
Alice,30,New York
Bob,25,Los Angeles
|===
```

---

## Admonitions

### Standard Admonitions

```asciidoc
NOTE: This is a note.

TIP: This is a helpful tip.

IMPORTANT: This is important information.

WARNING: This is a warning.

CAUTION: Be careful with this.
```

### Block Admonitions

```asciidoc
[NOTE]
====
This is a note block.
It can contain multiple paragraphs.

And even code blocks.
====
```

---

## Document Structure

### Document Header

```asciidoc
= Document Title
Author Name <author@example.com>
v1.0, 2025-11-11
:toc:
:toc-title: Table of Contents
:icons: font
:numbered:
:source-highlighter: rouge

Document content starts here...
```

### Common Attributes

```asciidoc
:toc:                    Enable table of contents
:toc-title: My TOC       Custom TOC title
:numbered:               Number sections
:icons: font             Use font icons for admonitions
:source-highlighter:     Syntax highlighter (rouge, pygments, etc.)
:doctype: book           Document type (article, book, manpage)
:author: John Doe        Author name
:email: john@example.com Author email
```

### Table of Contents

```asciidoc
= Document Title
:toc:
:toc-placement: preamble

This is the preamble.
TOC will appear here.

== Chapter 1
Content...
```

**TOC Positions**:
- `preamble`: After preamble, before first section
- `left`: Left sidebar
- `right`: Right sidebar
- `macro`: Manual placement with `toc::[]`

---

## Images and Media

### Images

```asciidoc
image::path/to/image.png[]
image::path/to/image.png[Alt Text]
image::path/to/image.png[Alt Text,300,200]
```

### Block Images

```asciidoc
.Image Caption
image::screenshot.png[Screenshot,800,align="center"]
```

### Inline Images

```asciidoc
Click the image:icons/save.png[Save icon] button to save.
```

### Image Attributes

```asciidoc
image::diagram.png[Architecture Diagram,600,400,align="center",title="System Architecture"]
```

---

## Blocks and Delimiters

### Literal Block

```asciidoc
....
Literal text block
    Preserves whitespace
        and indentation
....
```

### Listing Block

```asciidoc
----
Code or terminal output
Monospace font
----
```

### Example Block

```asciidoc
====
This is an example block.
Can contain any content.
====
```

### Sidebar

```asciidoc
****
This is a sidebar.
Contains supplementary information.
****
```

### Quote Block

```asciidoc
[quote, Author Name, Book Title]
____
This is a quotation.
It can span multiple lines.
____
```

**Simplified quote**:
```asciidoc
____
Simple quote without attribution.
____
```

---

## Includes

### Include Files

```asciidoc
include::chapter1.adoc[]

include::shared/header.adoc[]

include::code/example.py[]
```

### Include with Line Selection

```asciidoc
include::document.adoc[lines=1..10]
include::document.adoc[lines=20..-1]
include::code.py[lines=5..15]
```

### Include with Tags

```asciidoc
# In source file:
# tag::snippet[]
def important_function():
    pass
# end::snippet[]

# In AsciiDoc:
[source,python]
----
include::code.py[tag=snippet]
----
```

---

## Advanced Features

### Macros

**Button**:
```asciidoc
Press btn:[OK] to continue.
```

**Keyboard**:
```asciidoc
Press kbd:[Ctrl+C] to copy.
```

**Menu**:
```asciidoc
Select menu:File[Save As] to save.
```

### Icons

```asciidoc
icon:heart[] icon:star[] icon:check[]
icon:warning[2x] icon:fire[size=3x,rotate=90]
```

**Note**: Requires `:icons: font` attribute.

### Footnotes

```asciidoc
This is text with a footnote.footnote:[Footnote text here]

Another footnote reference.footnote:fn1[Named footnote]

Reuse footnote.footnote:fn1[]
```

### Comments

```asciidoc
// Single-line comment

////
Multi-line comment block
Can span multiple lines
////
```

### Conditional Directives

```asciidoc
ifdef::env-github[]
Content for GitHub only.
endif::[]

ifndef::backend-pdf[]
Content not for PDF.
endif::[]

ifeval::["{backend}" == "html5"]
HTML-specific content.
endif::[]
```

---

## AsciiDoc in Yole

### Supported Features

✅ **Text Formatting**: Bold, italic, monospace, etc.
✅ **Headings**: All 6 levels
✅ **Lists**: Unordered, ordered, labeled, checklists
✅ **Links**: External and internal
✅ **Code Blocks**: With syntax highlighting
✅ **Tables**: Basic table syntax
✅ **Admonitions**: Note, tip, warning, etc.
✅ **Quotes**: Block quotes
✅ **Basic Structure**: Title, headings, paragraphs

### Syntax Highlighting

Yole highlights:
- **Headings**: Different colors by level
- **Text formatting**: Bold, italic, code
- **Lists**: Bullets and numbers
- **Links**: URL highlighting
- **Code blocks**: Language-specific highlighting
- **Admonitions**: Special highlighting
- **Attributes**: Document attributes

### Limitations

❌ **Includes**: Files not included (shown as-is)
❌ **Macros**: Not expanded (button, keyboard, menu)
❌ **Conditionals**: Not evaluated
❌ **Advanced Tables**: CSV import not processed
❌ **Icons**: Font icons not rendered
❌ **Conversion**: No HTML/PDF generation (yet)

**Recommendation**: Use Asciidoctor for full conversion, Yole for editing.

---

## Common Use Cases

### 1. Technical Documentation

```asciidoc
= API Documentation
Developer Team <dev@example.com>
v2.0, 2025-11-11
:toc:
:numbered:
:source-highlighter: rouge

== Introduction

This document describes the REST API for our service.

== Authentication

All requests require authentication.

[source,bash]
----
curl -H "Authorization: Bearer TOKEN" \
     https://api.example.com/endpoint
----

== Endpoints

=== GET /users

Retrieve list of users.

[cols="1,3"]
|===
| Parameter | Description

| limit | Max results (default: 100)
| offset | Pagination offset
|===

==== Response

[source,json]
----
{
  "users": [
    {"id": 1, "name": "Alice"},
    {"id": 2, "name": "Bob"}
  ]
}
----

=== POST /users

Create new user.

==== Request Body

[source,json]
----
{
  "name": "Charlie",
  "email": "charlie@example.com"
}
----

WARNING: Email must be unique.

== Error Handling

Common error codes:

[options="header"]
|===
| Code | Message | Description

| 400 | Bad Request | Invalid input
| 401 | Unauthorized | Missing/invalid token
| 404 | Not Found | Resource not found
| 500 | Server Error | Internal error
|===

== Examples

See <<common-scenarios>> for examples.

[[common-scenarios]]
=== Common Scenarios

==== Creating a User

[source,bash]
----
curl -X POST \
     -H "Content-Type: application/json" \
     -d '{"name":"Alice","email":"alice@example.com"}' \
     https://api.example.com/users
----
```

### 2. README / Project Documentation

```asciidoc
= MyProject
:toc:
:icons: font

image::logo.png[Project Logo,200]

== Overview

MyProject is a tool for doing amazing things.

=== Features

* Feature 1 - Does X
* Feature 2 - Does Y
* Feature 3 - Does Z

== Installation

=== Prerequisites

* Python 3.8+
* Node.js 14+
* PostgreSQL 12+

=== Quick Start

[source,bash]
----
git clone https://github.com/user/myproject.git
cd myproject
./install.sh
----

NOTE: Installation takes approximately 5 minutes.

== Usage

=== Basic Example

[source,python]
----
from myproject import Tool

tool = Tool()
result = tool.process("input")
print(result)
----

=== Advanced Configuration

See link:docs/configuration.adoc[Configuration Guide] for details.

== Contributing

We welcome contributions! See link:CONTRIBUTING.adoc[Contributing Guide].

== License

Licensed under MIT License. See link:LICENSE[LICENSE] file.
```

### 3. Tutorial / User Guide

```asciidoc
= Getting Started with AsciiDoc
:toc:
:toc-title: Contents
:numbered:

== Introduction

Welcome to AsciiDoc! This guide will teach you the basics.

=== What You'll Learn

By the end of this tutorial, you'll know how to:

. Write formatted text
. Create lists and tables
. Add code blocks
. Include images
. Generate beautiful documents

== Your First Document

=== Step 1: Create File

Create a new file called `hello.adoc`:

[source,asciidoc]
----
= Hello, AsciiDoc!

This is my first document.
----

=== Step 2: Add Content

Add some formatted text:

[source,asciidoc]
----
= Hello, AsciiDoc!

This is my first document with *bold* and _italic_ text.

== My First Section

This is a paragraph in a section.
----

TIP: Use blank lines to separate paragraphs.

=== Step 3: Convert to HTML

Use Asciidoctor to convert:

[source,bash]
----
asciidoctor hello.adoc
----

This creates `hello.html`.

== Text Formatting

Try these formatting options:

[cols="1,2,2"]
|===
| Syntax | Example | Result

| `*bold*` | `*important*` | *important*
| `_italic_` | `_emphasis_` | _emphasis_
| `+monospace+` | `+code+` | `code`
|===

== Practice Exercises

Try creating:

[arabic]
. A document with 3 sections
. A list with nested items
. A code block with syntax highlighting
. A table with headers

== Next Steps

Continue learning:

* link:advanced.adoc[Advanced Features]
* link:best-practices.adoc[Best Practices]
* https://docs.asciidoctor.org/[Official Documentation]

IMPORTANT: Practice regularly to master AsciiDoc!
```

### 4. Book Chapter

```asciidoc
= Chapter 3: Data Structures
:doctype: book
:chapter-number: 3

[abstract]
This chapter covers fundamental data structures including arrays, linked lists, and trees.

== Arrays

An array is a contiguous block of memory.

.Array Memory Layout
image::images/array-layout.png[Array Layout,600]

=== Properties

[cols="1,3"]
|===
| Property | Description

| Access Time | O(1) - constant time
| Insert/Delete | O(n) - linear time
| Memory | Contiguous allocation
|===

=== Implementation

[source,python]
----
class DynamicArray:
    def __init__(self):
        self.data = []

    def append(self, item):
        self.data.append(item)  # <1>

    def get(self, index):
        return self.data[index]  # <2>
----
<1> Add item to end of array
<2> Access item by index

NOTE: Python lists are actually dynamic arrays.

== Linked Lists

A linked list stores elements in nodes.

=== Node Structure

[source,python]
----
class Node:
    def __init__(self, data):
        self.data = data
        self.next = None
----

=== Advantages

* Dynamic size
* Efficient insertion/deletion
* No contiguous memory needed

=== Disadvantages

* No random access
* Extra memory for pointers
* Poor cache locality

WARNING: Use arrays when you need fast random access.

== Summary

In this chapter, we covered:

* Arrays and their properties
* Linked lists and trade-offs
* When to use each structure

.Key Takeaways
****
* Arrays: Fast access, fixed size
* Linked lists: Dynamic, slower access
* Choose based on your use case
****

== Exercises

. Implement a dynamic array from scratch
. Create a doubly-linked list
. Compare performance of both structures

See <<solutions>> for answers.
```

---

## Best Practices

### File Organization

**Project structure**:
```
docs/
├── index.adoc              # Main entry point
├── chapters/
│   ├── chapter1.adoc
│   ├── chapter2.adoc
│   └── chapter3.adoc
├── includes/
│   ├── header.adoc
│   └── footer.adoc
├── images/
│   └── *.png
└── code/
    └── *.py
```

### Consistent Formatting

**Use consistent heading levels**:
```asciidoc
= Document Title (Level 0)
== Chapter (Level 1)
=== Section (Level 2)
==== Subsection (Level 3)
```

Don't skip levels (no === directly under =).

### Attributes for Reusability

**Define once, use everywhere**:
```asciidoc
:product-name: MyProduct
:version: 2.0
:support-email: support@example.com

= {product-name} Documentation
Version {version}

For help, contact {support-email}.
```

### Include Shared Content

**Header file** (`includes/header.adoc`):
```asciidoc
:author: Documentation Team
:copyright: 2025 Company Name
:icons: font
:source-highlighter: rouge
```

**Use in documents**:
```asciidoc
= My Document
include::includes/header.adoc[]

Document content here...
```

---

## Tips & Tricks

### 🎯 Pro Tips

1. **Use attributes**: Define variables once, reuse everywhere
2. **Include files**: Break large docs into manageable pieces
3. **Consistent style**: Follow formatting conventions
4. **Table of contents**: Use `:toc:` for long documents
5. **Syntax highlighting**: Specify language for code blocks

### 🚀 Power User Techniques

**Conditional content by backend**:
```asciidoc
ifdef::backend-html5[]
HTML-specific content
endif::[]

ifdef::backend-pdf[]
PDF-specific content
endif::[]
```

**Custom attributes for environments**:
```asciidoc
:env-prod:
:api-url: https://api.production.com

ifndef::env-prod[]
:api-url: https://api.staging.com
endif::[]

API endpoint: {api-url}
```

**Reusable snippets**:
```asciidoc
// Define snippet
:snippet-warning: WARNING: Always backup before proceeding.

// Use in multiple places
{snippet-warning}
```

**Auto-generate diagrams** (with extensions):
```asciidoc
[plantuml,diagram,png]
----
@startuml
Alice -> Bob: Hello
Bob --> Alice: Hi!
@enduml
----
```

---

## External Resources

### Official Documentation
- [Asciidoctor Documentation](https://docs.asciidoctor.org/)
- [AsciiDoc Syntax Quick Reference](https://docs.asciidoctor.org/asciidoc/latest/syntax-quick-reference/)
- [AsciiDoc Writer's Guide](https://asciidoctor.org/docs/asciidoc-writers-guide/)

### Tools
- **Asciidoctor**: Ruby-based AsciiDoc processor
- **Asciidoctor.js**: JavaScript version
- **AsciidocFX**: Editor and toolchain
- **Yole**: Mobile AsciiDoc editor

### Books & Resources
- [AsciiDoc Best Practices](https://asciidoctor.org/docs/asciidoc-recommended-practices/)
- [AsciiDoc vs Markdown](https://docs.asciidoctor.org/asciidoc/latest/asciidoc-vs-markdown/)

### Community
- [Asciidoctor Discussion](https://discuss.asciidoctor.org/)
- [GitHub: Asciidoctor](https://github.com/asciidoctor)

---

## Next Steps

- **[Markdown Format →](./markdown.md)** - Simpler alternative
- **[reStructuredText Format →](./restructuredtext.md)** - Python documentation
- **[WikiText Format →](./wikitext.md)** - Wiki markup
- **[Back to Getting Started →](../getting-started.md)**

---

*Last updated: November 11, 2025*
*Yole version: 2.15.1+*
