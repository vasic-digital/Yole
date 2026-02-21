# Textile Format Guide

## Overview

Textile is a lightweight markup language that uses simple text patterns to format HTML.

## Supported Extensions

- `.textile` - Textile files

## Text Formatting

```textile
*bold*
_italic_
__underline~~
--strikethrough--
^^superscript^^
,,subscript,,
```

## Headers

```textile
h1. Level 1
h2. Level 2
h3. Level 3
```

## Lists

```textile
* Unordered
** Nested
# Ordered
## Nested
- Bullet with class[css_class]
```

## Links and Images

```textile
"Link text":http://example.com
!image_url!
!image_url(alt text)!
```

## Tables

```textile
|_. Header |_. Header |
| Cell | Cell |
|< Left | Centered |
|> Right |
```

## Code

```textile
@code inline@

bc. Code block
```

## Attributes

```textile
p(class). Paragraph with class
p(#id). Paragraph with ID
p(style:color:red). Inline style
```

## See Also

- [Textile Reference](https://textile-lang.com)
