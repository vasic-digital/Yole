# WikiText Format Guide

**Format**: WikiText (MediaWiki)
**Extensions**: `.wiki`, `.wikitext`, `.mediawiki`
**Specification**: [MediaWiki Markup](https://www.mediawiki.org/wiki/Help:Formatting)
**Yole Support**: ✅ Syntax highlighting, basic parsing

---

## Overview

WikiText is the markup language used by MediaWiki, the software that powers Wikipedia and thousands of other wikis. It's designed for collaborative editing and creating hyperlinked documents.

### Why WikiText?

- **Wikipedia-Compatible**: Same syntax as Wikipedia
- **Link-Heavy**: Built for creating interconnected content
- **Template System**: Reusable content blocks
- **Simple Tables**: Easy table creation
- **Category System**: Organize content hierarchically
- **Widely Used**: Powers thousands of wikis worldwide

---

## Basic Syntax

### Text Formatting

```wiki
''italic text''
'''bold text'''
'''''bold and italic'''''
```

**Rendered**:
- *italic text*
- **bold text**
- ***bold and italic***

### Headings

```wiki
= Level 1 Heading =
== Level 2 Heading ==
=== Level 3 Heading ===
==== Level 4 Heading ====
===== Level 5 Heading =====
====== Level 6 Heading ======
```

**Note**: Level 1 is typically the page title (not used in content)

### Paragraphs

```wiki
First paragraph.

Second paragraph.
Two newlines create a new paragraph.

Third paragraph.
```

Blank line between paragraphs creates separation.

---

## Links

### Internal Links

```wiki
[[Article Name]]
[[Article Name|Display Text]]
[[Category:Technology]]
```

**Examples**:
```wiki
See [[Wikipedia]] for more information.
Learn about [[Markup language|markup languages]].
This page is in [[Category:Documentation]].
```

### External Links

```wiki
[https://example.com Example Website]
[https://example.com]
https://example.com
```

**Rendered**:
- [Example Website](https://example.com)
- [\[1\]](https://example.com)
- https://example.com (bare URL)

### Section Links

```wiki
[[#Section_Name]]
[[Article#Section_Name]]
[[Article#Section_Name|Link Text]]
```

---

## Lists

### Unordered Lists

```wiki
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

```wiki
# First item
# Second item
## Nested 2.1
## Nested 2.2
# Third item
```

**Rendered**:
1. First item
2. Second item
   1. Nested 2.1
   2. Nested 2.2
3. Third item

### Definition Lists

```wiki
; Term
: Definition
; Another term
: First definition
: Second definition
```

**Rendered**:
- **Term**
  - Definition
- **Another term**
  - First definition
  - Second definition

### Mixed Lists

```wiki
# Numbered item
#* Bullet sub-item
#* Another bullet
# Numbered item 2
#; Definition term
#: Definition
```

---

## Tables

### Simple Table

```wiki
{| class="wikitable"
|-
! Header 1 !! Header 2 !! Header 3
|-
| Cell 1 || Cell 2 || Cell 3
|-
| Cell 4 || Cell 5 || Cell 6
|}
```

**Rendered**:
| Header 1 | Header 2 | Header 3 |
|----------|----------|----------|
| Cell 1   | Cell 2   | Cell 3   |
| Cell 4   | Cell 5   | Cell 6   |

### Table with Caption

```wiki
{| class="wikitable"
|+ Table Caption
|-
! Column 1 !! Column 2
|-
| Data 1 || Data 2
|-
| Data 3 || Data 4
|}
```

### Table Attributes

```wiki
{| class="wikitable" style="width:100%;"
|-
! style="width:30%;" | Name
! style="width:70%;" | Description
|-
| Item 1 || Description of item 1
|-
| Item 2 || Description of item 2
|}
```

### Compact Table Syntax

```wiki
{| class="wikitable"
! Header 1 !! Header 2 !! Header 3
|-
| Row 1, Cell 1 || Row 1, Cell 2 || Row 1, Cell 3
|-
| Row 2, Cell 1 || Row 2, Cell 2 || Row 2, Cell 3
|}
```

---

## Formatting

### Horizontal Rule

```wiki
----
```

Creates a horizontal line separator.

### Line Breaks

```wiki
First line<br>
Second line<br>
Third line
```

Use `<br>` or `<br />` for explicit line breaks.

### Indentation

```wiki
: Single indent
:: Double indent
::: Triple indent
```

**Rendered**:
- Single indent
  - Double indent
    - Triple indent

### Preformatted Text

```wiki
 Line starting with space
 Another preformatted line
 Preserves    spacing
```

Each line starting with a space is preformatted (monospace).

---

## Code and Syntax Highlighting

### Inline Code

```wiki
Use <code>inline code</code> for short snippets.
```

**Rendered**: Use `inline code` for short snippets.

### Code Blocks

```wiki
<pre>
function hello() {
    console.log("Hello, World!");
}
</pre>
```

### Syntax Highlighting (with extensions)

```wiki
<syntaxhighlight lang="python">
def hello():
    print("Hello, World!")
</syntaxhighlight>
```

**Note**: Requires SyntaxHighlight extension in MediaWiki.

---

## Templates and Transclusion

### Including Templates

```wiki
{{Template Name}}
{{Template Name|parameter1|parameter2}}
{{Template Name|param1=value1|param2=value2}}
```

**Examples**:
```wiki
{{Info|This is an information box}}
{{Citation needed|date=November 2025}}
{{Main|Article Name}}
```

### Template Definition

```wiki
<noinclude>
This text appears only on the template page itself.
</noinclude>

Template content here with {{{parameter}}}

<includeonly>
This text appears only when template is transcluded.
</includeonly>
```

---

## Categories and Magic Words

### Categories

```wiki
[[Category:Documentation]]
[[Category:WikiText|Sorting Key]]
```

Adds page to category. Categories appear at bottom of page.

### Magic Words

```wiki
{{PAGENAME}}          - Current page name
{{CURRENTYEAR}}       - Current year
{{NUMBEROFARTICLES}}  - Total articles
__NOTOC__             - Hide table of contents
__TOC__               - Force TOC position
__FORCETOC__          - Show TOC even for short pages
```

**Example**:
```wiki
This article was last updated in {{CURRENTYEAR}}.
```

---

## Images and Media

### Basic Image

```wiki
[[File:Example.jpg]]
[[File:Example.jpg|thumb|Caption text]]
[[File:Example.jpg|200px|Alternative text]]
```

### Image Options

```wiki
[[File:Example.jpg|thumb|right|300px|This is the caption]]
```

**Options**:
- `thumb` - Create thumbnail
- `frame` - Framed image
- `left`, `right`, `center`, `none` - Alignment
- `200px`, `300px`, etc. - Size
- Last parameter is always caption/alt text

### Gallery

```wiki
<gallery>
File:Image1.jpg|Caption 1
File:Image2.jpg|Caption 2
File:Image3.jpg|Caption 3
</gallery>
```

---

## Special Blocks

### Quotations

```wiki
<blockquote>
This is a block quotation.
It can span multiple lines.
</blockquote>
```

### Collapsible Sections

```wiki
{| class="wikitable mw-collapsible"
|-
! Header (click to expand/collapse)
|-
| Hidden content here
| More content
|}
```

### Comments

```wiki
<!-- This is a comment, not visible in rendered output -->
```

### No Wiki

```wiki
<nowiki>This text is '''not''' formatted.</nowiki>
```

Prevents wiki markup processing.

---

## Advanced Features

### Tables of Contents

```wiki
__TOC__           Force TOC here
__NOTOC__         No TOC at all
__FORCETOC__      Show TOC for short pages
```

### Redirects

```wiki
#REDIRECT [[Target Page]]
#REDIRECT [[Target Page#Section]]
```

Must be the first line of the page.

### Signatures

```wiki
~~~~          Username and timestamp
~~~           Username only
~~~~~         Timestamp only
```

**Example**: `--User (talk) 12:34, 11 November 2025 (UTC)`

### References and Footnotes

```wiki
Text with reference.<ref>Reference text here</ref>

More text.<ref name="refname">Named reference</ref>

Reuse reference.<ref name="refname" />

== References ==
<references />
```

---

## WikiText in Yole

### Supported Features

✅ **Text Formatting**: Bold, italic, combined
✅ **Headings**: All 6 levels
✅ **Lists**: Unordered, ordered, definition, mixed
✅ **Links**: Internal, external, section links
✅ **Tables**: Basic table syntax
✅ **Code**: Inline and block code
✅ **Syntax Highlighting**: Basic patterns
✅ **Horizontal Rules**: Page separators

### Syntax Highlighting

Yole highlights:
- **Headings**: Different colors by level
- **Bold/Italic**: Text formatting
- **Links**: Internal and external
- **Lists**: Bullets and numbers
- **Tables**: Table structure
- **Code**: Inline and block code
- **Templates**: Template syntax
- **Comments**: Hidden comments

### Limitations

❌ **Templates**: Not evaluated (displayed as-is)
❌ **Magic Words**: Not processed
❌ **Parser Functions**: Not executed
❌ **Extensions**: No extension support
❌ **Categories**: Displayed but not functional
❌ **File Uploads**: No media handling

**Recommendation**: Use MediaWiki installation for full features, Yole for mobile editing.

---

## Common Use Cases

### 1. Knowledge Base Article

```wiki
= Introduction =

'''WikiText''' is a markup language used by [[MediaWiki]].

== Features ==

* Easy to learn
* Powerful linking
* Template system
* Widely adopted

== Syntax ==

Basic syntax includes:
* ''Italic text'' with two apostrophes
* '''Bold text''' with three apostrophes
* [[Links]] in double brackets

== See Also ==

* [[Markdown]]
* [[reStructuredText]]

[[Category:Markup Languages]]
```

### 2. Documentation Page

```wiki
= API Documentation =

== Overview ==

The API provides access to {{NUMBEROFARTICLES}} articles.

== Endpoints ==

{| class="wikitable"
! Endpoint !! Method !! Description
|-
| /api/articles || GET || List all articles
|-
| /api/article/{id} || GET || Get specific article
|-
| /api/search || POST || Search articles
|}

== Authentication ==

Use API key in header:
<pre>
Authorization: Bearer YOUR_API_KEY
</pre>

== Examples ==

=== Get Article ===

<syntaxhighlight lang="bash">
curl -H "Authorization: Bearer KEY" \
     https://api.example.com/api/article/123
</syntaxhighlight>

== See Also ==

* [[API Authentication]]
* [[Rate Limits]]

[[Category:API]]
[[Category:Documentation]]
```

### 3. Meeting Notes

```wiki
= Team Meeting - November 11, 2025 =

== Attendees ==

* Alice (Product Manager)
* Bob (Engineering Lead)
* Charlie (Designer)

== Agenda ==

# Q4 Planning
# New feature discussion
# Budget review

== Discussion ==

=== Q4 Planning ===

We reviewed the roadmap for Q4 2025.

'''Action items''':
* Alice: Draft Q4 OKRs {{done}}
* Bob: Update technical roadmap
* Charlie: Design mockups for Feature X

=== New Feature Discussion ===

Proposed features:
* Feature A - High priority
* Feature B - Medium priority
* Feature C - Low priority

== Next Meeting ==

* '''Date''': November 18, 2025
* '''Time''': 10:00 AM
* '''Location''': Conference Room B

[[Category:Meetings]]
[[Category:2025]]
```

### 4. Project Wiki

```wiki
= Project Alpha =

{{Info|This project is in active development}}

== Overview ==

'''Project Alpha''' is a new initiative to improve user onboarding.

== Team ==

{| class="wikitable"
! Role !! Name !! Contact
|-
| Project Manager || Alice Smith || alice@example.com
|-
| Tech Lead || Bob Johnson || bob@example.com
|-
| Designer || Charlie Brown || charlie@example.com
|}

== Timeline ==

# '''Phase 1''' (Nov 2025): Research and planning
# '''Phase 2''' (Dec 2025): Design and prototyping
# '''Phase 3''' (Jan 2026): Development
# '''Phase 4''' (Feb 2026): Testing and launch

== Technical Details ==

See [[Project Alpha/Technical Spec]] for details.

=== Architecture ===

<pre>
Frontend → API Gateway → Backend Services → Database
</pre>

== Resources ==

* [[Project Alpha/Requirements]]
* [[Project Alpha/Design Docs]]
* [[Project Alpha/Sprint Planning]]

[[Category:Projects]]
[[Category:Active]]
```

---

## Best Practices

### File Organization

**Single wiki approach**:
```
wiki/
├── Main_Page.wiki
├── Documentation.wiki
├── API_Reference.wiki
└── FAQ.wiki
```

**Category-based**:
```
wiki/
├── documentation/
│   ├── Getting_Started.wiki
│   └── Advanced_Topics.wiki
├── api/
│   ├── Overview.wiki
│   └── Endpoints.wiki
└── guides/
    ├── Tutorial_1.wiki
    └── Tutorial_2.wiki
```

### Naming Conventions

**Good page names**:
```wiki
[[Introduction to WikiText]]
[[API Reference]]
[[User Guide]]
[[FAQ]]
```

**Avoid**:
```wiki
[[intro]]              Too vague
[[api_ref_v2_final]]   Too technical
[[Page 1]]             Not descriptive
```

### Linking Strategy

**Link liberally**:
```wiki
[[WikiText]] is a [[markup language]] used by [[MediaWiki]].
Learn more about [[formatting]] and [[tables]].
```

**Use descriptive link text**:
```wiki
See [[Markup language|markup languages]] for comparison.
Read the [[MediaWiki documentation|official docs]].
```

### Template Usage

**Create reusable templates**:
```wiki
{{Info|Important information here}}
{{Warning|Be careful with this}}
{{Todo|Task to complete}}
```

**Define custom templates**:
```wiki
<!-- Template:Info -->
<div style="background: #e3f2fd; padding: 10px;">
ℹ️ {{{1}}}
</div>
```

---

## Tips & Tricks

### 🎯 Pro Tips

1. **Use templates**: Create reusable content blocks
2. **Link extensively**: Connect related pages
3. **Categorize pages**: Organize with categories
4. **Add TOC**: Include `__TOC__` for long pages
5. **Use tables**: Structure data clearly

### 🚀 Power User Techniques

**Conditional content**:
```wiki
{{#if:{{{parameter|}}}|Show if parameter exists|Show otherwise}}
```

**String functions** (with ParserFunctions extension):
```wiki
{{#len:string}}           Length
{{#pos:string|search}}    Find position
{{#sub:string|start|len}} Substring
```

**Variables**:
```wiki
{{PAGENAME}}
{{NAMESPACE}}
{{CURRENTDAY}}/{{CURRENTMONTH}}/{{CURRENTYEAR}}
```

**Info boxes**:
```wiki
{| class="wikitable" style="float:right; width:300px;"
|-
! colspan="2" | Quick Facts
|-
| '''Type''' || Markup Language
|-
| '''Created''' || 2002
|-
| '''Used by''' || Wikipedia, wikis
|}
```

---

## External Resources

### Official Documentation
- [MediaWiki Help](https://www.mediawiki.org/wiki/Help:Contents)
- [Formatting Guide](https://www.mediawiki.org/wiki/Help:Formatting)
- [Tables Guide](https://www.mediawiki.org/wiki/Help:Tables)

### Wikipedia Resources
- [Wikipedia Cheatsheet](https://en.wikipedia.org/wiki/Help:Cheatsheet)
- [Wiki Markup](https://en.wikipedia.org/wiki/Help:Wiki_markup)

### Tools
- **MediaWiki**: Full wiki software
- **DokuWiki**: Alternative wiki
- **Yole**: Mobile WikiText editor

### Extensions
- [ParserFunctions](https://www.mediawiki.org/wiki/Extension:ParserFunctions)
- [Cite](https://www.mediawiki.org/wiki/Extension:Cite)
- [SyntaxHighlight](https://www.mediawiki.org/wiki/Extension:SyntaxHighlight)

---

## Next Steps

- **[Markdown Format →](./markdown.md)** - Similar but simpler
- **[Org Mode Format →](./orgmode.md)** - Alternative organizational system
- **[reStructuredText Format →](./restructuredtext.md)** - Python documentation
- **[Back to Getting Started →](../getting-started.md)**

---

*Last updated: November 11, 2025*
*Yole version: 2.15.1+*
