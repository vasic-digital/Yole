# TiddlyWiki Format Guide

## Overview

TiddlyWiki is a unique non-linear notebook for capturing, organizing, and sharing complex information.

## Supported Extensions

- `.tid` - Tiddler files
- `.tiddlywiki` - Wiki files

## Tiddler Format

```tid
title: My Tiddler
created: 20240115120000
modified: 20240115120000
tags: tag1 tag2

This is the content.
```

## WikiText

TiddlyWiki uses a rich wiki markup:

```tid
title: WikiText Example

! Heading
!! Subheading

**bold** //italic~~

* Bullet list
# Numbered list

[[Link Tiddler]]
[[Link|Display Text]]

{{image.png}}
{{{code}}}```
```

## Fields

```tid
title: Custom Field Example
custom.field: Custom value
number.field: 42
date.field: 2024-01-15

Content here.
```

## Macros

```tid
title: Macro Definition
\define mymacro(param)

This uses {{!!title}} and parameter {{{param}}}.

\end
```

## See Also

- [TiddlyWiki](https://tiddlywiki.com)
