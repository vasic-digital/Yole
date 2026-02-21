# Org Mode Format Guide

## Overview

Org Mode is a powerful plain-text system for notes, planning, and authoring. Originally part of Emacs, it's now widely supported across platforms.

## Supported Extensions

- `.org` - Org Mode files

## Document Structure

```org
#+TITLE: Document Title
#+AUTHOR: Author Name
#+DATE: 2024-01-15
#+EMAIL: email@example.com

* Headline 1
** Sub-headline
*** Sub-sub-headline

Content paragraph.

** Another Section
Content here.
```

## TODO Items

```org
* TODO Write documentation
* DONE Complete implementation
* WAITING Review feedback
* HOLD Deferred task
* TODO @work +project Task with context and project
```

## Priorityities

```org
* TODO [#A] High priority task
* TODO [#B] Medium priority task
* TODO [#C] Low priority task
```

## Timestamps

```org
* TODO Task with deadline
  DEADLINE: <2024-01-31 Wed>
* TODO Task with schedule
  SCHEDULED: <2024-01-20 Mon>
* TODO Task with date range
  <2024-01-15>--<2024-01-20>
```

## Properties

```org
* Task
:PROPERTIES:
:Effort: 2h
:Priority: High
:END:
```

## Tables

```org
| Name | Age | City |
|------+-----+------|
| John | 30  | NYC  |
| Jane | 25  | LA   |
```

## Code Blocks

```org
#+BEGIN_SRC kotlin
fun hello() = println("Hello")
#+END_SRC
```

## Links

```org
[[https://example.com][Link text]]
[[file:document.org]]
```

## See Also

- [Org Mode Manual](https://orgmode.org/manual/)
