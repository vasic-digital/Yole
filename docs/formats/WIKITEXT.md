# WikiText Format Guide

## Overview

WikiText is a markup language used by MediaWiki and similar wiki systems.

## Supported Extensions

- `.wiki` - WikiText
- `.wikitext` - Full name

## Headers

```wikitext
= Heading 1 =
== Heading 2 ==
=== Heading 3 ===
```

## Text Formatting

```wikitext
'''bold'''
''italic''
<code>monospace</code>
~~strikethrough~~
```

## Lists

```wikitext
* Bullet list
** Nested bullet
# Numbered list
## Nested number
```

## Links

```wikitext
[[Page Name]]
[[Page Name|Display Text]]
[[https://example.com|External Link]]
```

## Images

```wikitext
[[File:image.png]]
[[File:image.png|thumb|Caption]]
```

## Tables

```wikitext
{| class="wikitable"
|+ Caption
! Header 1
! Header 2
|-
| Row 1
| Row 2
|}
```

## Templates

```wikitext
{{TemplateName}}
{{TemplateName|param1=value}}
```

## See Also

- [MediaWiki](https://www.mediawiki.org)
