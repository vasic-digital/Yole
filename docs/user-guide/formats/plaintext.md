# Plain Text Format Guide

**Format**: Plain Text
**Extensions**: `.txt`, `.text`, `.log`
**Specification**: N/A (Universal text format)
**Yole Support**: ✅ Full (syntax highlighting for code, viewing)

---

## Overview

Plain text is the most basic and universal file format. It contains only readable characters without any formatting codes or structure. Plain text is supported by every text editor and operating system.

### Why Plain Text?

- **Universal**: Opens anywhere, on any device
- **Future-Proof**: Will work forever (no format obsolescence)
- **Simple**: No learning curve, just type
- **Lightweight**: Minimal file size
- **Searchable**: Grep, find, search tools work perfectly
- **Version Control**: Git-friendly, clean diffs

---

## When to Use Plain Text

### Perfect For:

1. **Quick Notes**: Jot down thoughts instantly
2. **Log Files**: Application logs, system logs
3. **Code Snippets**: Copy/paste code without formatting
4. **Lists**: Simple lists without structure
5. **Scratch Space**: Temporary clipboard storage
6. **Configuration Drafts**: Before converting to proper format
7. **Raw Data**: Unformatted data dumps

### Not Ideal For:

- **Structured Documents**: Use Markdown instead
- **Tasks with Metadata**: Use Todo.txt instead
- **Tabular Data**: Use CSV instead
- **Technical Writing**: Use LaTeX or reStructuredText
- **Rich Formatting**: Use Markdown or other markup

---

## Plain Text Types in Yole

Yole automatically detects different types of plain text files and provides appropriate syntax highlighting:

### 1. CODE Files

**Extensions**: `.py`, `.js`, `.java`, `.kt`, `.cpp`, `.rs`, `.go`, etc.

Code files get language-specific syntax highlighting:

```python
# Python code example
def hello_world():
    print("Hello, World!")
    return True
```

**Supported Languages** (40+):
- Python, JavaScript, TypeScript, Java, Kotlin
- C, C++, Rust, Go, Swift
- Ruby, PHP, Perl, Lua, R
- Shell/Bash scripts
- And many more...

### 2. HTML Files

**Extensions**: `.html`, `.htm`

HTML files render with proper HTML rendering:

```html
<!DOCTYPE html>
<html>
<head>
    <title>Page Title</title>
</head>
<body>
    <h1>Hello World</h1>
</body>
</html>
```

### 3. JSON Files

**Extensions**: `.json`

JSON files get pretty-printing and syntax highlighting:

```json
{
  "name": "Yole",
  "version": "2.15.1",
  "formats": ["markdown", "todotxt", "csv"]
}
```

### 4. XML Files

**Extensions**: `.xml`

XML files with proper tag highlighting:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<note>
    <to>User</to>
    <from>System</from>
    <message>Hello World</message>
</note>
```

### 5. Markdown Files (*.md)

Detected as Markdown format with full support.

### 6. Plain Text (Default)

Regular `.txt` files with no special formatting - just clean, readable text.

---

## Plain Text Features in Yole

### Syntax Highlighting

Even plain text files can have syntax highlighting when they contain recognizable code:

**Automatic Detection**:
- Python code (indentation, keywords)
- JavaScript/TypeScript (syntax patterns)
- Shell scripts (bash commands)
- Configuration files (key=value patterns)

**Manual Override**:
Use filename extensions or format menu to specify language.

### Monospace Font

Plain text displays in monospace font for:
- Consistent character width
- Better code readability
- Alignment of columns
- ASCII art preservation

### Line Numbers

Enable in **Settings** → **Editor** → **Show line numbers**

Perfect for:
- Code review
- Log analysis
- Referencing specific lines

### Word Wrap

Enable in **Settings** → **Editor** → **Word wrap**

Useful for:
- Long paragraphs
- Reading without horizontal scroll
- Mobile viewing

---

## Common Use Cases

### 1. Quick Notes

```
Meeting thoughts - 2025-11-11

Main points:
- Need to review Q4 targets
- Team wants more flexible work hours
- Budget approval pending

Action items:
- Follow up with finance
- Schedule team discussion
- Draft policy proposal
```

**Tips**:
- Date at top
- Use dashes or bullets
- Keep it simple

### 2. Code Snippets

```
// JavaScript function to copy
function debounce(func, wait) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}
```

**Tips**:
- Copy directly from text file
- Save by language (`.js`, `.py`, etc.)
- Comment for context

### 3. Log Files

```
2025-11-11 10:00:00 INFO Server started on port 8080
2025-11-11 10:00:05 INFO Database connection established
2025-11-11 10:00:10 WARN High memory usage detected: 85%
2025-11-11 10:00:15 ERROR Failed to connect to external API
2025-11-11 10:00:20 INFO Retrying connection...
```

**Tips**:
- Timestamp each entry
- Use log levels (INFO, WARN, ERROR)
- Keep chronological order
- Use `.log` extension

### 4. Lists

```
Shopping List
=============
Milk
Eggs
Bread
Apples
Coffee
Cheese
Butter

Books to Read
=============
The Pragmatic Programmer
Clean Code
Design Patterns
Refactoring
```

**Tips**:
- Group related items
- Use blank lines for separation
- Simple is best

### 5. Scratch/Clipboard

```
Temporary clipboard content
---------------------------

URLs to check later:
https://example.com/article1
https://example.com/article2

Email draft:
Subject: Meeting follow-up
Hi team, following up on...

Random thoughts:
Need to research X
Call Y tomorrow
```

**Tips**:
- Use as temporary storage
- Organize with headers
- Clean up periodically

### 6. Data Dumps

```
User Data Export
================

User 1: john@example.com, Premium, 2024-01-15
User 2: jane@example.com, Free, 2024-03-20
User 3: bob@example.com, Premium, 2024-05-10

Total Users: 3
Premium: 2
Free: 1
```

**Tips**:
- Consider CSV for structured data
- Plain text for quick exports
- Include summary statistics

---

## Best Practices

### File Organization

**Naming conventions**:
```
notes-2025-11-11.txt          # Date-based
meeting-notes-project-x.txt   # Topic-based
scratch.txt                   # Temporary
todo-quick.txt                # Purpose-based
code-snippets-python.txt      # Category-based
```

**Directory structure**:
```
notes/
├── daily/
│   ├── 2025-11-11.txt
│   └── 2025-11-12.txt
├── projects/
│   ├── project-a-notes.txt
│   └── project-b-notes.txt
├── snippets/
│   ├── python-utils.txt
│   └── js-functions.txt
└── scratch.txt
```

### Content Structure

**Use visual separators**:
```
Section 1
=========

Content here...

Section 2
---------

More content...

***

Another section...
```

**Indentation for hierarchy**:
```
Main Topic
  Subtopic 1
    Detail 1
    Detail 2
  Subtopic 2
    Detail 3
```

**Consistent formatting**:
```
# Pick a style and stick to it

Style 1 (dashes):
-----------------
- Item 1
- Item 2

Style 2 (asterisks):
********************
* Item 1
* Item 2
```

### Line Length

**Recommended**: 80-100 characters per line

**Why?**:
- Easy to read without horizontal scroll
- Works well with version control
- Readable on all screen sizes

**Example**:
```
This is a paragraph with proper line wrapping. Each line is kept to
around 80 characters for maximum readability across different
devices and editors. This makes the text easier to read and works
well with Git diffs.
```

### Encoding

**Use UTF-8**:
- Supports all languages
- International characters
- Emojis ✓
- Standard encoding

**Avoid**:
- ASCII (limited character set)
- ISO-8859-1 (Latin-1 only)
- Platform-specific encodings

---

## Plain Text vs. Markup

### When Plain Text is Better:

✅ **Quick notes**: No structure needed
✅ **Temporary content**: Scratch space
✅ **Log files**: Just timestamps and messages
✅ **Simple lists**: No formatting required
✅ **Code snippets**: Copy/paste without markup
✅ **Maximum compatibility**: Open anywhere

### When to Use Markup Instead:

❌ **Documents**: Use Markdown for headers, links, formatting
❌ **Tasks**: Use Todo.txt for priorities and metadata
❌ **Tables**: Use CSV for structured data
❌ **Technical docs**: Use LaTeX, reStructuredText, or AsciiDoc
❌ **Complex structure**: Use appropriate markup language

---

## Tips & Tricks

### 🎯 Pro Tips

1. **Use descriptive filenames**: `notes-2025-11-11.txt` not `notes.txt`
2. **Add dates**: Timestamp important entries
3. **Keep it simple**: Don't overcomplicate
4. **Archive regularly**: Move old notes to archive folder
5. **Use consistent formatting**: Pick a style and stick to it

### 🚀 Power User Techniques

**ASCII Art**:
```
┌─────────────────────┐
│   Yole Text Editor  │
│                     │
│  Simple. Powerful.  │
└─────────────────────┘
```

**Table formatting**:
```
Name        Age    City
----------  -----  -------------
Alice       30     New York
Bob         25     Los Angeles
Charlie     35     Chicago
```

**Box drawing**:
```
╔═══════════════════════════════╗
║  Important Note               ║
╠═══════════════════════════════╣
║  This is a highlighted box    ║
║  for important information    ║
╚═══════════════════════════════╝
```

**Quick templates**:
```
# Save as templates/meeting-notes.txt

Meeting Notes - [DATE]
======================

Attendees:
-
-
-

Agenda:
1.
2.
3.

Discussion:


Action Items:
[ ]
[ ]
[ ]

Next Meeting: [DATE]
```

### 📊 Productivity Patterns

**Daily notes template**:
```
Date: 2025-11-11
================

Tasks:
- [ ] Task 1
- [ ] Task 2
- [ ] Task 3

Notes:
...

Ideas:
...

Done:
- [x] Previous task
```

**Quick capture**:
```
# Keep a quick-capture.txt file open
# Dump everything there
# Process/organize later

Idea: Feature X could improve Y
TODO: Call John about project
Link: https://example.com/article
Quote: "Interesting insight..."
```

**Command line integration**:
```bash
# Quick note from command line
echo "Quick thought" >> ~/notes/scratch.txt

# Timestamp notes
echo "$(date): Server started" >> server.log

# Search notes
grep -r "keyword" ~/notes/

# Count lines in log
wc -l application.log
```

---

## Troubleshooting

### Special characters not displaying?

**Solution**: Use UTF-8 encoding
- Save file as UTF-8
- Check editor encoding settings
- Avoid binary characters in text files

### Lines too long to read?

**Solutions**:
1. Enable word wrap in settings
2. Manually break lines at 80 characters
3. Rotate device to landscape (mobile)

### Lost in large file?

**Solutions**:
1. Enable line numbers
2. Use search (Ctrl+F / Cmd+F)
3. Split into smaller files
4. Add section headers

### Can't tell code apart?

**Solutions**:
1. Save with language extension (`.py`, `.js`)
2. Add syntax hints (comments)
3. Use code format instead of plain text
4. Enable syntax highlighting in settings

---

## External Resources

### Tools

**Command Line**:
- `cat`, `less`, `more` - View files
- `grep` - Search in files
- `wc` - Count lines/words
- `sort`, `uniq` - Process text

**Editors**:
- **Vim/Neovim** - Terminal editor
- **Emacs** - Powerful editor
- **Nano** - Simple terminal editor
- **Sublime Text** - GUI editor
- **VS Code** - Modern editor

**Utilities**:
- `dos2unix` - Convert line endings
- `iconv` - Convert encoding
- `sed`, `awk` - Text processing
- `diff` - Compare files

### Learning

- [The Art of Plain Text](https://www.edunham.net/2014/11/17/plain_text.html)
- [Plain Text Power](https://plaintextproject.online/)
- [The Text Editor Guide](https://en.wikipedia.org/wiki/Text_editor)

---

## Examples

### Quick Note
```
Just a quick thought about the project architecture.
Maybe we should split the monolith into microservices?
Need to discuss with team.

Pros:
- Scalability
- Independent deployment
- Better fault isolation

Cons:
- Complexity
- Network overhead
- Debugging harder

Decision: Discuss at next meeting
```

### Code Reference
```
Python String Methods Cheat Sheet
==================================

.strip()      - Remove whitespace
.split()      - Split string by delimiter
.join()       - Join list into string
.replace()    - Replace substring
.startswith() - Check if starts with
.endswith()   - Check if ends with
.upper()      - Convert to uppercase
.lower()      - Convert to lowercase
```

### Simple Log
```
Application Log - 2025-11-11
=============================

10:00 - Started server
10:05 - User login: john@example.com
10:10 - Database backup completed
10:15 - Warning: High CPU usage
10:20 - CPU usage normalized
10:25 - User logout: john@example.com
10:30 - Stopped server
```

---

## Next Steps

- **[Markdown Format →](./markdown.md)** - Add formatting to text
- **[Todo.txt Format →](./todotxt.md)** - Add structure to tasks
- **[CSV Format →](./csv.md)** - Add structure to data
- **[Back to Getting Started →](../getting-started.md)**

---

*Last updated: November 11, 2025*
*Yole version: 2.15.1+*
