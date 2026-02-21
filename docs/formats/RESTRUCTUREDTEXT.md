# reStructuredText Format Guide

## Overview

reStructuredText (RST) is a plaintext markup language used primarily in Python documentation.

## Supported Extensions

- `.rst` - reStructuredText
- `.rest` - Alternate

## Document Structure

```rst
============
Title
============

Section
-------

Subsection
~~~~~~~~~~
```

## Text Roles

```rst
*italic*
**bold**
``literal``
```

## Lists

```rst
- Item 1
- Item 2

#. Ordered
#. Items

term (one or more)
   Definition
```

## Code Blocks

```rst
.. code-block:: kotlin

   fun hello() = println("Hello")
```

## Tables

```rst
+------+------+
| Col1 | Col2 |
+======+======+
| A    | B    |
+------+------+
```

## Directives

```rst
.. image:: path/to/image.png
   :width: 100

.. note:: This is a note

.. warning:: This is a warning
```

## See Also

- [Docutils](https://docutils.sourceforge.io/rst.html)
