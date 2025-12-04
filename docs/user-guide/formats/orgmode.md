# Org Mode Format Guide

**Format**: Org Mode
**Extensions**: `.org`
**Specification**: [Org Mode Manual](https://orgmode.org/manual/)
**Yole Support**: ✅ Syntax highlighting, basic parsing

---

## Overview

Org Mode is a powerful plain-text organizational system originally created for Emacs. It combines note-taking, task management, project planning, and authoring in a single, elegant format.

### Why Org Mode?

- **All-in-One**: Notes, todos, schedules, and documents in one format
- **Plain Text**: Future-proof, portable, version-control friendly
- **Powerful**: From simple notes to complex project management
- **Flexible**: Adapts to your workflow
- **Outliner**: Hierarchical structure with folding
- **Emacs Ecosystem**: Vast ecosystem of tools (though Yole works independently)

---

## Basic Syntax

### Headlines

Use asterisks for headlines (similar to Markdown but different):

```org
* Top Level Headline
** Second Level
*** Third Level
**** Fourth Level
***** Fifth Level
```

**Key difference from Markdown**: Space after asterisks is required.

### Text Formatting

```org
*bold*
/italic/
_underline_
=verbatim=
~code~
+strikethrough+
```

**Rendered**:
- **bold**
- *italic*
- underline (underlined text)
- =verbatim= (as-is text)
- ~code~ (monospace)
- ~~strikethrough~~

### Lists

#### Unordered Lists
```org
- Item 1
- Item 2
  - Nested item 2.1
  - Nested item 2.2
- Item 3
```

Or use `+`:
```org
+ Item 1
+ Item 2
  + Nested item
```

#### Ordered Lists
```org
1. First item
2. Second item
   1. Nested first
   2. Nested second
3. Third item
```

#### Description Lists
```org
- Term :: Definition
- Another term :: Another definition
```

---

## TODO Items

### Basic TODO

```org
* TODO Write documentation
* DONE Finish feature implementation
```

**States**: `TODO` (not done) and `DONE` (completed)

### Custom TODO Keywords

```org
#+TODO: TODO IN-PROGRESS WAITING | DONE CANCELED

* TODO Start project
* IN-PROGRESS Writing code
* WAITING Waiting for review
* DONE Project completed
* CANCELED Project canceled
```

The `|` separates incomplete (left) from complete (right) states.

### TODO with Priority

```org
* TODO [#A] High priority task
* TODO [#B] Medium priority task
* TODO [#C] Low priority task
```

**Priorities**: `[#A]` (highest) to `[#C]` (lowest)

### TODO with Deadline

```org
* TODO Submit report
  DEADLINE: <2025-11-15 Fri>

* TODO Pay bills
  SCHEDULED: <2025-11-10 Sun>
```

**DEADLINE**: When task must be completed
**SCHEDULED**: When to start working on task

---

## Timestamps and Scheduling

### Plain Timestamp

```org
<2025-11-11 Mon>
<2025-11-15 Fri 14:00>
<2025-11-20 Wed 09:00-10:30>
```

### Date Range

```org
<2025-11-11 Mon>--<2025-11-15 Fri>
```

### Repeating Timestamps

```org
<2025-11-11 Mon +1w>     Every week
<2025-11-01 Wed +1m>     Every month
<2025-11-15 Fri +1d>     Every day
<2025-11-11 Mon +1y>     Every year
```

### Scheduling Examples

```org
* TODO Daily standup
  SCHEDULED: <2025-11-11 Mon 09:00 +1d>

* TODO Monthly report
  DEADLINE: <2025-11-30 Thu +1m>

* TODO Quarterly review
  SCHEDULED: <2025-12-01 Fri +3m>
```

---

## Links and Images

### Internal Links

```org
[[#custom-id][Link text]]
[[*Headline name][Link to headline]]
```

### External Links

```org
[[https://example.com][Website]]
[[file:~/documents/notes.org][Another org file]]
[[file:image.png][Image link]]
```

### Images

```org
[[file:image.png]]
[[https://example.com/image.jpg]]
```

---

## Tables

### Simple Table

```org
| Name    | Age | City        |
|---------+-----+-------------|
| Alice   |  30 | New York    |
| Bob     |  25 | Los Angeles |
| Charlie |  35 | Chicago     |
```

**Features**:
- Use `|` for columns
- Use `|-` for horizontal separator
- Emacs Org Mode auto-aligns (not in Yole)

### Table with Formulas

```org
| Item   | Quantity | Price | Total |
|--------+----------+-------+-------|
| Apples |        5 | 2.00  | 10.00 |
| Oranges |       3 | 3.00  |  9.00 |
|--------+----------+-------+-------|
| Total  |          |       | 19.00 |
#+TBLFM: @4$4=vsum(@2..@3)
```

**Note**: Formulas work in Emacs Org Mode, not Yole.

---

## Code Blocks

### Source Code Blocks

```org
#+BEGIN_SRC python
def hello():
    print("Hello, World!")
    return True
#+END_SRC
```

### With Results

```org
#+BEGIN_SRC python :results output
print("Hello from Org Mode!")
#+END_SRC

#+RESULTS:
: Hello from Org Mode!
```

### Multiple Languages

```org
#+BEGIN_SRC bash
echo "Bash script"
ls -la
#+END_SRC

#+BEGIN_SRC javascript
console.log("JavaScript code");
#+END_SRC

#+BEGIN_SRC sql
SELECT * FROM users WHERE active = 1;
#+END_SRC
```

---

## Document Structure

### Title and Metadata

```org
#+TITLE: My Document Title
#+AUTHOR: John Doe
#+DATE: 2025-11-11
#+EMAIL: john@example.com
#+DESCRIPTION: Document description
#+KEYWORDS: keyword1, keyword2
#+LANGUAGE: en
```

### Table of Contents

```org
#+TOC: headlines 2
```

Generates TOC with 2 levels of headlines.

### Options

```org
#+OPTIONS: toc:2           Table of contents (2 levels)
#+OPTIONS: num:t           Number headlines
#+OPTIONS: ^:nil           Don't use subscripts
#+OPTIONS: author:t        Include author
#+OPTIONS: timestamp:nil   Don't include timestamp
```

---

## Tags

### Headline Tags

```org
* Meeting :work:important:
* Personal project :personal:hobby:
* Bug fix :bug:urgent:
** Sub-task :bug:
```

**Inheritance**: Sub-headlines inherit parent tags.

### Tag Search

Search for headlines with specific tags:
```org
:work:          All work items
:urgent:        All urgent items
:work+urgent:   Work AND urgent
:work|personal: Work OR personal
```

---

## Properties

### Headline Properties

```org
* TODO Project Task
  :PROPERTIES:
  :CATEGORY: ProjectA
  :Effort:   2:00
  :OWNER:    Alice
  :END:
```

### Property Drawers

Properties are stored in a `:PROPERTIES:` drawer under headlines.

---

## Checkboxes

### Simple Checkboxes

```org
- [ ] Unchecked item
- [X] Checked item
- [-] Partially checked (some sub-items done)
  - [X] Sub-item 1 done
  - [ ] Sub-item 2 pending
```

### With Percentages

```org
* TODO Project [1/3]
  - [X] Task 1
  - [ ] Task 2
  - [ ] Task 3

* TODO Project [33%]
  - [X] Task 1
  - [ ] Task 2
  - [ ] Task 3
```

---

## Org Mode in Yole

### Supported Features

✅ **Headlines**: Full support with levels
✅ **Text Formatting**: Bold, italic, code, etc.
✅ **Lists**: Ordered, unordered, description
✅ **TODO keywords**: Basic TODO/DONE
✅ **Timestamps**: Display and parsing
✅ **Links**: Internal and external
✅ **Tables**: Display (no formulas)
✅ **Code blocks**: Syntax highlighting
✅ **Tags**: Display
✅ **Checkboxes**: Display

### Syntax Highlighting

Yole highlights:
- **Headlines**: Different colors by level
- **TODO keywords**: Colored by state
- **Timestamps**: Date/time formatting
- **Tags**: Tag highlighting
- **Emphasis**: Bold, italic, etc.
- **Links**: URL highlighting
- **Code blocks**: Language-specific highlighting

### Limitations

❌ **Agenda**: No agenda views (Emacs feature)
❌ **Formulas**: Table formulas not evaluated
❌ **Babel**: Code execution not supported
❌ **Export**: No export functionality (yet)
❌ **Advanced features**: Some advanced Emacs features not supported

**Recommendation**: Use Emacs for advanced Org Mode features, Yole for mobile/quick editing.

---

## Common Use Cases

### 1. Project Planning

```org
#+TITLE: Website Redesign Project
#+AUTHOR: Team Lead
#+DATE: 2025-11-11

* TODO Planning Phase [0/3]
** TODO Define requirements
   DEADLINE: <2025-11-15 Fri>
** TODO Create mockups
   SCHEDULED: <2025-11-20 Mon>
** TODO Get client approval
   DEADLINE: <2025-11-30 Thu>

* TODO Development Phase [0/4]
** TODO Setup repository
** TODO Implement frontend
** TODO Implement backend
** TODO Testing

* TODO Launch Phase [0/2]
** TODO Deploy to staging
** TODO Deploy to production
```

### 2. Meeting Notes

```org
#+TITLE: Team Meeting - Weekly Sync
#+DATE: 2025-11-11

* Attendees
- Alice (Product)
- Bob (Engineering)
- Charlie (Design)

* Agenda
1. Project status updates
2. Blocker discussion
3. Next week planning

* Discussion Notes
** Project Status :update:
- Frontend: 80% complete
- Backend: 60% complete
- Design: 100% complete

** Blockers :urgent:
- API integration delayed
- Need database review

* Action Items
- [ ] Alice: Follow up with API team
- [ ] Bob: Schedule database review
- [ ] Charlie: Prepare design handoff

* Next Meeting
SCHEDULED: <2025-11-18 Mon 10:00>
```

### 3. Personal Knowledge Base

```org
#+TITLE: Programming Notes
#+AUTHOR: Developer

* Python :python:programming:
** List Comprehensions
Examples:
#+BEGIN_SRC python
squares = [x**2 for x in range(10)]
evens = [x for x in range(20) if x % 2 == 0]
#+END_SRC

** Decorators
#+BEGIN_SRC python
def timer(func):
    def wrapper(*args, **kwargs):
        start = time.time()
        result = func(*args, **kwargs)
        print(f"Time: {time.time() - start}")
        return result
    return wrapper
#+END_SRC

* JavaScript :javascript:programming:
** Async/Await
#+BEGIN_SRC javascript
async function fetchData() {
  try {
    const response = await fetch(url);
    const data = await response.json();
    return data;
  } catch (error) {
    console.error(error);
  }
}
#+END_SRC
```

### 4. Task Management

```org
#+TITLE: Personal Tasks
#+DATE: 2025-11-11

* TODO [#A] Urgent and Important
** TODO [#A] Submit tax documents
   DEADLINE: <2025-11-15 Fri>
** TODO [#A] Dentist appointment
   SCHEDULED: <2025-11-12 Tue 14:00>

* TODO [#B] Important but Not Urgent
** TODO [#B] Plan vacation
** TODO [#B] Review insurance policy

* TODO [#C] Nice to Have
** TODO [#C] Organize photo library
** TODO [#C] Clean garage

* DONE Completed
** DONE [#A] Pay rent
   CLOSED: [2025-11-01 Fri]
```

---

## Best Practices

### File Organization

**Single file approach**:
```org
# Everything in one file
main.org
```

**Multiple file approach**:
```org
notes/
├── inbox.org          # Quick capture
├── projects.org       # Active projects
├── reference.org      # Reference material
├── archive.org        # Completed items
└── personal.org       # Personal notes
```

### Headline Structure

**Good hierarchy**:
```org
* Project Name
** Phase 1: Planning
*** Task 1.1
*** Task 1.2
** Phase 2: Development
*** Task 2.1
*** Task 2.2
```

**Consistent levels**: Don't skip levels (no *** directly under *)

### TODO Workflow

**Basic workflow**:
```
TODO → IN-PROGRESS → DONE
```

**Advanced workflow**:
```
TODO → NEXT → IN-PROGRESS → WAITING → DONE/CANCELED
```

### Tag Strategy

**Categories**:
```org
:work: :personal: :hobby:
```

**Status**:
```org
:urgent: :important: :someday:
```

**Context**:
```org
:@home: :@office: :@phone: :@computer:
```

---

## Tips & Tricks

### 🎯 Pro Tips

1. **Use consistent structure**: Pick a system and stick to it
2. **Tag liberally**: Tags help find related items
3. **Archive regularly**: Move completed items to archive.org
4. **Date everything**: Timestamps help track progress
5. **Keep it simple**: Start simple, add complexity as needed

### 🚀 Power User Techniques

**Quick capture template**:
```org
* Inbox
** TODO Captured item [/]
   - [ ] Process this
   - [ ] Organize
   - [ ] File appropriately
```

**Project template**:
```org
* PROJECT Project Name [/]
  :PROPERTIES:
  :CATEGORY: ProjectA
  :END:
** TODO Planning [/]
** TODO Execution [/]
** TODO Review [/]
```

**Weekly review template**:
```org
* Week of <2025-11-11 Mon>
** Completed This Week
** In Progress
** Planned for Next Week
** Notes and Observations
```

---

## External Resources

### Official
- [Org Mode Website](https://orgmode.org/) - Official site
- [Org Mode Manual](https://orgmode.org/manual/) - Complete documentation
- [Worg](https://orgmode.org/worg/) - Community wiki

### Emacs Integration
- [Org Mode in Emacs](https://www.gnu.org/software/emacs/manual/html_node/emacs/Org-Mode.html)
- [Org Mode Tutorial](https://orgmode.org/quickstart.html)

### Mobile Apps
- **Org Mode**: Yole (cross-platform)
- **Beorg**: iOS Org Mode app
- **Orgzly**: Android Org Mode app
- **Organice**: Web-based Org Mode

### Resources
- [Awesome Org Mode](https://github.com/jmh/awesome-orgmode) - Curated list
- [Org Mode Syntax Cheatsheet](https://orgmode.org/worg/dev/org-syntax.html)

---

## Next Steps

- **[Markdown Format →](./markdown.md)** - Similar but simpler
- **[Todo.txt Format →](./todotxt.md)** - Simpler task management
- **[LaTeX Format →](./latex.md)** - Academic writing
- **[Back to Getting Started →](../getting-started.md)**

---

*Last updated: November 11, 2025*
*Yole version: 2.15.1+*
