# reStructuredText Format Guide

**Format**: reStructuredText (reST)
**Extensions**: `.rst`, `.rest`, `.restx`, `.rtxt`
**Specification**: [reStructuredText Markup](https://docutils.sourceforge.io/rst.html)
**Yole Support**: ✅ Syntax highlighting, basic parsing

---

## Overview

reStructuredText (reST) is a powerful markup language designed for technical documentation. It's the standard for Python documentation and is widely used with Sphinx for creating beautiful documentation sites.

### Why reStructuredText?

- **Python Standard**: Official markup for Python documentation
- **Sphinx Integration**: Powers Read the Docs and Python docs
- **Powerful**: Rich directive system for complex documents
- **Extensible**: Custom directives and roles
- **Professional**: Industry-standard for technical writing
- **API Documentation**: Excellent for documenting code

---

## Basic Syntax

### Headings

```rst
#####################
Document Title (Part)
#####################

*******************
Chapter (Chapter)
*******************

Section (Section)
=================

Subsection
----------

Subsubsection
^^^^^^^^^^^^^

Paragraph
"""""""""
```

**Heading Guidelines**:
- Underline (and optionally overline) with punctuation
- Punctuation line must be at least as long as text
- Common hierarchy: `#` (parts) → `*` (chapters) → `=` (sections) → `-` (subsections) → `^` → `"`

### Paragraphs

```rst
First paragraph.

Second paragraph.
Blank line separates paragraphs.

Third paragraph with
a line continuation.
```

### Text Formatting

```rst
*italic text*
**bold text**
``code/literal``
```

**Rendered**:
- *italic text*
- **bold text**
- `code/literal`

**Note**: No underline or strikethrough in standard reST.

---

## Lists

### Bulleted Lists

```rst
* Item 1
* Item 2

  * Nested item 2.1
  * Nested item 2.2

* Item 3
```

**Alternative markers**:
```rst
- Item with dash
+ Item with plus
* Item with asterisk
```

### Enumerated Lists

```rst
1. First item
2. Second item

   a. Nested item
   b. Another nested

3. Third item
```

**Auto-numbering**:
```rst
#. Auto-numbered item
#. Another auto-numbered
#. Third auto-numbered
```

### Definition Lists

```rst
Term 1
    Definition of term 1.

Term 2
    Definition of term 2.
    Can span multiple lines.

    Can include multiple paragraphs.
```

### Field Lists

```rst
:Author: John Doe
:Date: 2025-11-11
:Version: 1.0
:Status: Draft
```

### Option Lists

```rst
-a         Command line option a
-b file    Option with argument
--long     Long option
--output=file  Long option with argument
```

---

## Links and References

### External Links

```rst
`Link text <https://example.com>`_
https://example.com
```

**Named references**:
```rst
See the `Python documentation`_ for details.

.. _Python documentation: https://docs.python.org/
```

### Internal Cross-References

```rst
See section-label_ for details.

.. _section-label:

Section Title
=============
```

**Sphinx cross-references**:
```rst
:ref:`section-label`
:doc:`other-document`
:mod:`module.name`
:class:`ClassName`
:func:`function_name`
```

### Anonymous Links

```rst
`Link 1 <https://example.com>`__
`Link 2 <https://other.com>`__
```

Two underscores create anonymous (non-referenced) links.

---

## Code Blocks

### Inline Code

```rst
Use ``inline code`` for short snippets.
```

### Literal Blocks

```rst
Example code block::

    def hello():
        print("Hello, World!")
        return True

Text continues here.
```

**Note**: Double colon `::` creates literal block, indented.

### Code-Block Directive

```rst
.. code-block:: python

   def fibonacci(n):
       if n <= 1:
           return n
       return fibonacci(n-1) + fibonacci(n-2)
```

**With line numbers**:
```rst
.. code-block:: python
   :linenos:

   def greet(name):
       print(f"Hello, {name}!")
```

**With highlighting**:
```rst
.. code-block:: python
   :emphasize-lines: 2,3

   def example():
       important_line()  # highlighted
       another_one()     # highlighted
       normal_line()
```

### Doctest Blocks

```rst
>>> print("Hello, World!")
Hello, World!
>>> 2 + 2
4
```

---

## Tables

### Simple Tables

```rst
=====  =====  ======
   Inputs     Output
------------  ------
  A      B    A or B
=====  =====  ======
False  False  False
True   False  True
False  True   True
True   True   True
=====  =====  ======
```

### Grid Tables

```rst
+------------+------------+-----------+
| Header 1   | Header 2   | Header 3  |
+============+============+===========+
| Row 1      | Data       | More data |
+------------+------------+-----------+
| Row 2      | Data       | More data |
+------------+------------+-----------+
```

### CSV Tables

```rst
.. csv-table:: Table Title
   :header: "Name", "Age", "City"
   :widths: 20, 10, 20

   "Alice", "30", "New York"
   "Bob", "25", "Los Angeles"
   "Charlie", "35", "Chicago"
```

### List Tables

```rst
.. list-table:: Table Title
   :header-rows: 1
   :widths: 25 25 50

   * - Name
     - Age
     - City
   * - Alice
     - 30
     - New York
   * - Bob
     - 25
     - Los Angeles
```

---

## Directives

### Admonitions

```rst
.. note::

   This is a note.

.. warning::

   This is a warning.

.. danger::

   This is dangerous!

.. tip::

   Helpful tip here.

.. important::

   Important information.

.. caution::

   Be careful!
```

### Generic Admonition

```rst
.. admonition:: Custom Title

   This is a custom admonition with your own title.
```

### Images

```rst
.. image:: path/to/image.png
   :alt: Alternative text
   :width: 600px
   :align: center
```

### Figures

```rst
.. figure:: path/to/image.png
   :scale: 50%
   :alt: Figure caption

   This is the figure caption.
   It can span multiple lines.
```

### Topic

```rst
.. topic:: Topic Title

   This is a topic block.
   Contains related information.
```

### Sidebar

```rst
.. sidebar:: Sidebar Title

   This is a sidebar.
   Appears to the side of main content.
```

### Include

```rst
.. include:: included-file.rst
.. include:: code/example.py
   :code: python
```

---

## Roles (Inline Markup)

### Standard Roles

```rst
:emphasis:`italic text`
:strong:`bold text`
:literal:`code/literal`
:subscript:`subscript text`
:superscript:`superscript text`
:title-reference:`Book Title`
```

### Code Roles

```rst
:code:`inline code`
:py:func:`function_name`
:py:class:`ClassName`
:py:mod:`module.name`
:py:meth:`method_name`
:py:attr:`attribute_name`
```

### Sphinx Roles

```rst
:doc:`document-name`
:ref:`reference-label`
:download:`file.pdf`
:file:`/path/to/file`
:kbd:`Ctrl+C`
:mailheader:`Content-Type`
:mimetype:`text/plain`
:option:`--verbose`
```

---

## Document Structure

### Document Header

```rst
================
Document Title
================

:Author: John Doe
:Date: 2025-11-11
:Version: 1.0

.. contents:: Table of Contents
   :depth: 2

Introduction
============

Document content starts here.
```

### Table of Contents

```rst
.. contents::
   :depth: 2
   :local:
   :backlinks: top
```

**Options**:
- `:depth:` - Max heading depth
- `:local:` - Only current document
- `:backlinks:` - Add backlinks to TOC

### Sections and Anchors

```rst
.. _my-reference-label:

Section Title
=============

Content here.

Later, reference it: see my-reference-label_.
```

---

## Python Documentation

### Docstring Format

```rst
def function_name(param1, param2):
    """
    Brief description of function.

    More detailed description here.
    Can span multiple paragraphs.

    :param param1: Description of param1
    :type param1: int
    :param param2: Description of param2
    :type param2: str
    :return: Description of return value
    :rtype: bool
    :raises ValueError: When invalid input
    :raises TypeError: When wrong type

    Example usage::

        >>> function_name(10, "test")
        True
    """
    pass
```

### Class Documentation

```rst
class MyClass:
    """
    Brief class description.

    Detailed description of class purpose and usage.

    :param init_param: Initialization parameter
    :type init_param: str

    .. attribute:: attribute_name

       Description of attribute.

    Example::

        >>> obj = MyClass("value")
        >>> obj.method()
        'result'
    """

    def method(self):
        """
        Method description.

        :return: Description
        :rtype: str
        """
        pass
```

### Module Documentation

```rst
"""
Module Name
===========

Brief module description.

Detailed explanation of module purpose,
functionality, and usage.

Available Classes
-----------------

* :class:`MyClass` - Description
* :class:`OtherClass` - Description

Available Functions
-------------------

* :func:`function_name` - Description
* :func:`other_function` - Description

Example::

    from module import MyClass

    obj = MyClass()
    result = obj.method()

.. note::

   Important information about module.
"""
```

---

## Sphinx-Specific Features

### Configuration (conf.py)

```python
project = 'My Project'
author = 'John Doe'
release = '1.0'

extensions = [
    'sphinx.ext.autodoc',
    'sphinx.ext.napoleon',
    'sphinx.ext.viewcode',
]

html_theme = 'sphinx_rtd_theme'
```

### Auto-Documentation

```rst
.. automodule:: mymodule
   :members:
   :undoc-members:
   :show-inheritance:

.. autoclass:: MyClass
   :members:
   :special-members: __init__

.. autofunction:: my_function
```

### Cross-Document References

```rst
See :doc:`installation` for setup.
See :ref:`advanced-config` for configuration.
Download :download:`sample.py <../examples/sample.py>`.
```

### TOC Tree

```rst
.. toctree::
   :maxdepth: 2
   :caption: Contents:

   installation
   quickstart
   api/index
   faq
```

---

## reStructuredText in Yole

### Supported Features

✅ **Text Formatting**: Bold, italic, code
✅ **Headings**: All levels
✅ **Lists**: Bulleted, enumerated, definition
✅ **Links**: External and internal
✅ **Code Blocks**: With syntax highlighting
✅ **Tables**: Simple and grid tables
✅ **Admonitions**: Note, warning, tip, etc.
✅ **Basic Directives**: Image, include, topic
✅ **Syntax Highlighting**: reST-aware

### Syntax Highlighting

Yole highlights:
- **Headings**: Underline patterns
- **Text formatting**: Bold, italic, code
- **Lists**: Bullets and numbers
- **Links**: URL and reference highlighting
- **Directives**: Directive syntax
- **Roles**: Role markers
- **Code blocks**: Language-specific highlighting

### Limitations

❌ **Sphinx**: No Sphinx processing (autodoc, toctree, etc.)
❌ **Includes**: Files not included
❌ **Complex Directives**: Not all directives supported
❌ **Cross-References**: Not resolved
❌ **PDF Generation**: No Sphinx build

**Recommendation**: Use Sphinx for building docs, Yole for editing.

---

## Common Use Cases

### 1. Python Package Documentation

```rst
==================
MyPackage
==================

A Python package for doing amazing things.

.. contents::
   :depth: 2

Installation
============

Install via pip::

    pip install mypackage

Quick Start
===========

Basic usage example::

    from mypackage import Tool

    tool = Tool()
    result = tool.process("input")
    print(result)

API Reference
=============

Main Classes
------------

.. autoclass:: mypackage.Tool
   :members:
   :special-members: __init__

   .. method:: process(input)

      Process the input and return result.

      :param input: Input data to process
      :type input: str
      :return: Processed result
      :rtype: str

      Example::

          >>> tool = Tool()
          >>> tool.process("hello")
          'HELLO'

Utility Functions
-----------------

.. autofunction:: mypackage.helpers.validate

Configuration
=============

Configuration options:

:DEBUG: Enable debug mode (default: False)
:TIMEOUT: Request timeout in seconds (default: 30)
:MAX_RETRIES: Maximum retry attempts (default: 3)

Example configuration::

    [mypackage]
    DEBUG = True
    TIMEOUT = 60
    MAX_RETRIES = 5

Troubleshooting
===============

Common Issues
-------------

**Import Error**

.. code-block:: python

   ImportError: No module named 'mypackage'

Solution::

    pip install --upgrade mypackage

**Configuration Error**

.. warning::

   Ensure configuration file is in correct location.

See :ref:`configuration` for details.

Contributing
============

See :doc:`contributing` guide.

License
=======

MIT License. See LICENSE file for details.
```

### 2. Tutorial / User Guide

```rst
================
Getting Started
================

.. contents::
   :local:

Welcome
=======

Welcome to the tutorial! You'll learn:

* How to install the software
* Basic usage patterns
* Advanced features
* Best practices

Prerequisites
=============

Before starting, ensure you have:

* Python 3.8 or higher
* pip package manager
* Basic Python knowledge

Installation
============

Step 1: Install Package
-----------------------

.. code-block:: bash

   pip install mypackage

.. note::

   Installation takes approximately 1 minute.

Step 2: Verify Installation
----------------------------

Test the installation::

    python -c "import mypackage; print(mypackage.__version__)"

Expected output::

    1.0.0

Your First Program
==================

Create a new file ``hello.py``:

.. code-block:: python
   :linenos:

   from mypackage import Tool

   # Create tool instance
   tool = Tool()

   # Process data
   result = tool.process("Hello, World!")

   # Print result
   print(result)

Run the program:

.. code-block:: bash

   python hello.py

Output::

   HELLO, WORLD!

Explanation
-----------

1. **Import**: Import the Tool class
2. **Initialize**: Create tool instance
3. **Process**: Call process method
4. **Output**: Print the result

.. tip::

   Try different input strings to see results.

Advanced Features
=================

Custom Configuration
--------------------

Configure the tool:

.. code-block:: python

   tool = Tool(
       uppercase=True,
       remove_punctuation=True,
       max_length=100
   )

Error Handling
--------------

Handle errors properly:

.. code-block:: python

   try:
       result = tool.process(data)
   except ValueError as e:
       print(f"Invalid input: {e}")
   except Exception as e:
       print(f"Unexpected error: {e}")

.. warning::

   Always validate input before processing.

Next Steps
==========

Continue learning:

* :doc:`advanced-tutorial` - Advanced techniques
* :doc:`api-reference` - Complete API documentation
* :doc:`examples` - Example code

.. important::

   Practice the examples to master the concepts!
```

### 3. API Reference

```rst
=============
API Reference
=============

This document describes the complete API.

.. module:: mypackage

Core Classes
============

Tool Class
----------

.. class:: Tool(config=None)

   Main tool for processing data.

   :param config: Optional configuration dictionary
   :type config: dict or None

   .. attribute:: config

      Configuration dictionary.

      :type: dict

   .. method:: process(input, options=None)

      Process input data.

      :param input: Data to process
      :type input: str
      :param options: Optional processing options
      :type options: dict or None
      :return: Processed result
      :rtype: str
      :raises ValueError: If input is invalid
      :raises TypeError: If input is wrong type

      Example::

          >>> tool = Tool()
          >>> tool.process("test")
          'TEST'

   .. method:: validate(input)

      Validate input data.

      :param input: Data to validate
      :type input: str
      :return: True if valid
      :rtype: bool

      Example::

          >>> tool = Tool()
          >>> tool.validate("valid")
          True

Helper Functions
================

.. function:: format_output(data, style='default')

   Format output data.

   :param data: Data to format
   :type data: str
   :param style: Output style (default, json, xml)
   :type style: str
   :return: Formatted output
   :rtype: str

   Styles:

   :default: Plain text output
   :json: JSON formatted
   :xml: XML formatted

   Example::

       >>> format_output("test", style='json')
       '{"data": "test"}'

Exceptions
==========

.. exception:: ProcessingError

   Raised when processing fails.

   .. attribute:: message

      Error message.

   .. attribute:: error_code

      Numeric error code.

Constants
=========

.. data:: DEFAULT_TIMEOUT

   Default timeout value in seconds.

   :type: int
   :value: 30

.. data:: MAX_RETRIES

   Maximum number of retry attempts.

   :type: int
   :value: 3
```

### 4. README / Project Overview

```rst
========
MyProject
========

.. image:: https://img.shields.io/pypi/v/myproject.svg
   :target: https://pypi.org/project/myproject/

.. image:: https://img.shields.io/badge/license-MIT-blue.svg
   :target: LICENSE

A powerful tool for doing amazing things.

Features
========

* **Feature 1**: Fast processing
* **Feature 2**: Easy to use
* **Feature 3**: Highly configurable
* **Feature 4**: Extensible architecture

Quick Start
===========

Installation::

    pip install myproject

Usage::

    from myproject import Tool

    tool = Tool()
    result = tool.process("input")

Documentation
=============

* **Tutorial**: https://myproject.readthedocs.io/tutorial/
* **API Reference**: https://myproject.readthedocs.io/api/
* **Examples**: https://github.com/user/myproject/examples/

Requirements
============

* Python 3.8+
* requests >= 2.25.0
* numpy >= 1.20.0

Development
===========

Install development dependencies::

    pip install -e ".[dev]"

Run tests::

    pytest

Build documentation::

    cd docs
    make html

Contributing
============

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

See CONTRIBUTING.rst for details.

License
=======

MIT License. See LICENSE file for details.

Support
=======

* **Issues**: https://github.com/user/myproject/issues
* **Discussions**: https://github.com/user/myproject/discussions
* **Email**: support@example.com
```

---

## Best Practices

### File Organization

**Sphinx project structure**:
```
docs/
├── conf.py                # Sphinx configuration
├── index.rst              # Main entry point
├── installation.rst
├── quickstart.rst
├── api/
│   ├── index.rst
│   ├── classes.rst
│   └── functions.rst
├── tutorials/
│   ├── basic.rst
│   └── advanced.rst
├── _static/              # Static files (CSS, images)
└── _templates/           # Custom templates
```

### Consistent Heading Hierarchy

```rst
####################
Part (rarely used)
####################

******************
Chapter
******************

Section
=======

Subsection
----------

Subsubsection
^^^^^^^^^^^^^

Paragraph
"""""""""
```

### Cross-Reference Everything

```rst
See :ref:`installation` for setup instructions.
The :class:`Tool` class provides :meth:`Tool.process`.
Read :doc:`tutorial` for examples.
```

### Use Directives for Special Content

```rst
.. note:: Helpful information

.. warning:: Important warning

.. code-block:: python

   # Code example
```

---

## Tips & Tricks

### 🎯 Pro Tips

1. **Use Sphinx**: reST is best with Sphinx for building docs
2. **Cross-reference**: Link between documents liberally
3. **Autodoc**: Let Sphinx extract docs from Python code
4. **Read the Docs**: Use RTD for hosting
5. **Extensions**: Leverage Sphinx extensions

### 🚀 Power User Techniques

**Substitutions**:
```rst
.. |name| replace:: Replacement text
.. |version| replace:: 1.0

Using |name| version |version|.
```

**Custom roles**:
```rst
.. role:: custom

Use :custom:`custom formatted text`.
```

**Include with options**:
```rst
.. include:: file.rst
   :start-line: 10
   :end-line: 20
```

**Nested directives**:
```rst
.. note::

   This note contains:

   .. code-block:: python

      def example():
          pass
```

---

## External Resources

### Official Documentation
- [reStructuredText Documentation](https://docutils.sourceforge.io/rst.html)
- [Sphinx Documentation](https://www.sphinx-doc.org/)
- [reST Primer](https://www.sphinx-doc.org/en/master/usage/restructuredtext/basics.html)

### Tools
- **Sphinx**: Documentation generator
- **Read the Docs**: Documentation hosting
- **rst2html**: Convert reST to HTML
- **Yole**: Mobile reST editor

### Community
- [Sphinx Discussion](https://groups.google.com/g/sphinx-users)
- [Read the Docs Community](https://docs.readthedocs.io/)
- [Python Documentation](https://devguide.python.org/documentation/)

### Resources
- [Sphinx Themes Gallery](https://sphinx-themes.org/)
- [Awesome Sphinx](https://github.com/yoloseem/awesome-sphinxdoc)

---

## Next Steps

- **[Markdown Format →](./markdown.md)** - Simpler alternative
- **[AsciiDoc Format →](./asciidoc.md)** - Similar technical format
- **[Org Mode Format →](./orgmode.md)** - Alternative organization system
- **[Back to Getting Started →](../getting-started.md)**

---

*Last updated: November 11, 2025*
*Yole version: 2.15.1+*
