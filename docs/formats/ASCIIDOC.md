# AsciiDoc Format Guide

## Overview

AsciiDoc is a plain-text markup language similar to Markdown but with more powerful features for technical documentation.

## Supported Extensions

- `.adoc` - AsciiDoc
- `.asciidoc` - Full extension

## Document Structure

```asciidoc
= Document Title
Author Name <author@example.com>
:toc:

== Introduction

Content goes here.

== Section 2

=== Subsection
```

## Text Formatting

```asciidoc
*Bold*
_Italic_
`Monospace`
``Double monospace``
#Highlight#
~Subscript~
^Superscript^
```

## Lists

```asciidoc
. Ordered item 1
. Ordered item 2

* Unordered item
* Another item

-[ ] Checkbox unchecked
-[x] Checkbox checked
```

## Code Blocks

```asciidoc
[source,kotlin]
----
fun main() {
    println("Hello")
}
----
```

## Tables

```asciidoc
|===
|Header 1 |Header 2

|Cell 1
|Cell 2
|===
```

## Includes

```asciidoc
include::chapter1.adoc[]
```

## Attributes

```asciidoc
:version: 1.0
:experimental:

The current version is {version}.
```

## See Also

- [AsciiDoc Language Specification](https://asciidoc.org)
