# Todo.txt Format Guide

**Format**: Todo.txt
**Extensions**: `.txt`
**Specification**: [todotxt.org](http://todotxt.org/)
**Yole Support**: ✅ Full (parsing, display, task management UI)

---

## Overview

Todo.txt is a simple, text-based task management format that stores tasks in a plain text file. Created by Gina Trapani, it follows the philosophy that your todos should be as portable and accessible as plain text.

### Why Todo.txt?

- **Plain Text**: Works everywhere, no proprietary formats
- **Future-Proof**: Will work 50 years from now
- **Portable**: Sync with any tool (Dropbox, Git, Syncthing)
- **Flexible**: Rich metadata with simple syntax
- **Searchable**: Use any text search tool
- **Version Control**: Track history with Git

---

## Basic Syntax

### Simple Task

```
Buy groceries
```

The simplest task is just plain text.

### Task with Priority

```
(A) Call Mom
(B) Finish report
(C) Clean garage
```

**Priority levels**: `(A)` to `(Z)`, where `(A)` is highest priority.

**Rules**:
- Priority MUST be at the start of the line
- Use uppercase letters
- Space after closing parenthesis

### Completed Task

```
x 2025-11-11 Buy groceries
x 2025-11-11 (A) Call Mom
```

**Format**: `x COMPLETION_DATE ORIGINAL_TASK`

**Rules**:
- Lowercase `x` at start of line
- Space after `x`
- Date in `YYYY-MM-DD` format
- Original task (including priority if it had one)

### Task with Dates

```
(A) 2025-11-11 Call Mom
2025-11-11 Buy groceries
```

**Format**: `(PRIORITY) CREATION_DATE TASK`

**Rules**:
- Creation date comes after priority (if any)
- Date format: `YYYY-MM-DD`
- Space after date

### Complete Example

```
x 2025-11-11 2025-11-01 (A) Call Mom
```

**Components**:
- `x` - Completed
- `2025-11-11` - Completion date
- `2025-11-01` - Creation date
- `(A)` - Original priority
- `Call Mom` - Task description

---

## Advanced Syntax

### Projects

Use `+` to tag projects:

```
(A) Update website +WebsiteRedesign
Write blog post +Blog +Marketing
Fix bug #123 +ProjectAlpha
```

**Multiple projects**:
```
Design landing page +Website +Marketing +Q4Goals
```

**Rules**:
- No spaces in project names
- Use CamelCase or hyphens: `+MyProject` or `+my-project`
- Can have multiple projects per task

### Contexts

Use `@` to tag contexts (location, tool, person):

```
(B) Call dentist @phone
Fix printer @office
Email John @computer @work
```

**Common contexts**:
- `@home`, `@work`, `@office`
- `@computer`, `@phone`, `@email`
- `@errands`, `@waiting`
- `@morning`, `@evening`

**Rules**:
- No spaces in context names
- Multiple contexts allowed
- Use for filtering and searching

### Key-Value Metadata

Use `key:value` format:

```
(A) Write report due:2025-11-15
Call client due:2025-11-12 t:2025-11-10
Submit taxes due:2025-04-15 rec:yearly
```

**Common keys**:
- `due:YYYY-MM-DD` - Due date
- `t:YYYY-MM-DD` - Threshold/start date (hide until this date)
- `rec:` - Recurrence (daily, weekly, monthly, yearly)
- `pri:A` - Alternative priority format

**Custom metadata**:
```
Buy laptop price:1500 store:BestBuy url:http://example.com
Read book pages:250 author:JohnDoe isbn:1234567890
```

---

## Todo.txt in Yole

### Dedicated To-Do Screen

Yole provides a dedicated UI for Todo.txt files:

1. **Go to To-Do tab** (bottom navigation)
2. **View all tasks** in a clean list
3. **Add tasks** with input field
4. **Check off completed** tasks
5. **Filter** by status (done/pending)
6. **Organize** with projects and contexts

### Task Operations

**Add new task**:
1. Type task in input field
2. Optionally add priority: `(A) Task name`
3. Add projects: `+Project`
4. Add contexts: `@Context`
5. Tap **Add** button

**Complete task**:
- Tap checkbox next to task
- Completion date added automatically

**Delete task**:
- Tap delete icon (🗑️)
- Or swipe left (if supported)

**Filter tasks**:
- Tap **Show Done** / **Hide Done** button
- Filter by project (future feature)
- Filter by context (future feature)

### Syntax Highlighting

Yole highlights:
- **Priorities**: Colored by level (A=red, B=orange, C=yellow)
- **Projects**: Green `+Project`
- **Contexts**: Blue `@Context`
- **Due dates**: Red if overdue, yellow if today, gray if future
- **Completed**: Strikethrough text

---

## Best Practices

### File Organization

**Single file approach**:
```
todo.txt                  # All tasks
```

**Multiple file approach**:
```
personal.txt             # Personal tasks
work.txt                 # Work tasks
project-alpha.txt        # Project-specific tasks
```

**Archive approach**:
```
todo.txt                 # Active tasks
done.txt                 # Completed tasks (archive)
```

### Naming Conventions

**Projects**:
```
+WebsiteRedesign         # CamelCase
+website-redesign        # Kebab-case
+URGENT                  # All caps for meta-projects
```

**Contexts**:
```
@home                    # Location
@phone                   # Tool
@john                    # Person (lowercase)
@5min                    # Time required
```

### Priority Guidelines

- **(A)**: Must do today, critical
- **(B)**: Important, should do soon
- **(C)**: Nice to have, when time permits
- **(D-Z)**: Lower priorities (rarely needed)

**Tips**:
- Don't overuse (A) priority
- Limit to 3-5 (A) tasks per day
- Review and adjust priorities daily

### Dating Strategy

**Creation dates**:
- Add when task created
- Helps track task age
- Example: `2025-11-11 Buy groceries`

**Due dates**:
- Use `due:YYYY-MM-DD` format
- Set realistic deadlines
- Example: `Write report due:2025-11-15`

**Threshold dates**:
- Use `t:YYYY-MM-DD` for "start date"
- Hide task until date
- Example: `Christmas shopping t:2025-12-01`

---

## Common Workflows

### 1. Daily Review

**Morning**:
```
# Add today's priorities
(A) 2025-11-11 Submit report due:2025-11-11 @work
(A) 2025-11-11 Call client @phone
(B) 2025-11-11 Review code +ProjectAlpha @work
```

**Evening**:
```
# Mark completed
x 2025-11-11 2025-11-11 (A) Submit report due:2025-11-11 @work
x 2025-11-11 2025-11-11 (A) Call client @phone

# Adjust priorities for tomorrow
(A) 2025-11-11 Finish presentation due:2025-11-12 @work
```

### 2. Project Planning

```
# Break down project
(B) Plan website redesign +WebsiteRedesign @computer
(C) Design mockups +WebsiteRedesign @design
(C) Implement homepage +WebsiteRedesign @coding
(C) Test on mobile +WebsiteRedesign @testing
(C) Deploy to production +WebsiteRedesign @deploy
```

### 3. Context-Based Workflow

**Filter by context**:
```
# When at @office
(A) Print documents @office @printer
(B) Meet with team @office @meeting-room

# When have @phone
(A) Call dentist @phone
(B) Schedule car service @phone

# When at @home
(C) Pay bills @home @computer
(C) Water plants @home
```

### 4. Recurring Tasks

```
# Weekly tasks
Pay rent due:2025-11-01 rec:+1m
Team meeting due:2025-11-13 rec:+1w +Work @office

# Daily tasks
Exercise rec:+1d @home
Review email rec:+1d @work @computer
```

---

## Advanced Features

### Query and Search

**In Yole**:
- Use search bar to filter tasks
- Search by text, project, or context
- Case-insensitive matching

**Example searches**:
- `+Website` - All website tasks
- `@phone` - All phone calls
- `due:2025-11` - All tasks due in November
- `(A)` - All priority A tasks

### Sorting

**By priority**:
```
(A) High priority first
(A) Another high priority
(B) Medium priority
(C) Low priority
No priority tasks last
```

**By date**:
- Sort by creation date (oldest first)
- Sort by due date (urgent first)
- Sort by completion date

### Archiving

**Move completed to archive**:
1. Periodically review `todo.txt`
2. Move completed tasks (`x ...`) to `done.txt`
3. Keeps main file clean and fast

**Example done.txt**:
```
x 2025-11-11 2025-11-01 (A) Completed task one
x 2025-11-10 2025-11-05 (B) Completed task two
x 2025-11-09 2025-11-01 Completed task three
```

---

## Tips & Tricks

### 🎯 Pro Tips

1. **One task per line**: Keep tasks atomic
2. **Be specific**: "Call dentist" not "dentist"
3. **Use verbs**: Start with action words
4. **Review daily**: Keep list current
5. **Archive regularly**: Move completed tasks monthly

### 🚀 Power User Techniques

**Prefix codes**:
```
(A) @URGENT: Critical system down +Infrastructure
(B) @WAITING: Response from client +ProjectAlpha
```

**Task IDs**:
```
(A) [TASK-123] Implement feature X +Sprint42 @coding
```

**Time estimates**:
```
(B) Write documentation t:2h +Project @work
(C) Review PR t:30m +Development @computer
```

**Energy levels**:
```
(A) Complex debugging e:high @work @morning
(C) Data entry e:low @work @afternoon
```

### 📊 Productivity Patterns

**MIT (Most Important Tasks)**:
```
# Start each day with 3 MITs
(A) MIT1: Finish presentation due:today
(A) MIT2: Call important client due:today
(A) MIT3: Review team deliverables due:today
```

**Batching**:
```
# Group similar tasks
(B) Call dentist @phone @calls
(B) Call insurance @phone @calls
(B) Call bank @phone @calls
```

**Time blocking**:
```
# Assign time blocks
(A) 09:00-10:00 Write report @work @deep-work
(B) 10:00-11:00 Team meeting @work @meeting
(C) 14:00-15:00 Email processing @work @admin
```

---

## Troubleshooting

### Tasks not showing priority colors?
- Ensure priority is at start: `(A) Task` not `Task (A)`
- Use uppercase letters: `(A)` not `(a)`
- Space after priority: `(A) ` not `(A)`

### Completed tasks not marked?
- Use lowercase `x`: `x Task` not `X Task`
- Add completion date: `x 2025-11-11 Task`
- Space after `x`: `x ` not `x`

### Projects/contexts not detected?
- No spaces: `+MyProject` not `+ My Project`
- Check prefix: `+project` and `@context`, not `#project`
- Ensure word boundary (space before marker)

### Dates not recognized?
- Use format: `YYYY-MM-DD` (e.g., `2025-11-11`)
- Not: `11/11/2025`, `11-Nov-2025`, `Nov 11`
- Use hyphens: `-` not slashes `/`

---

## External Resources

### Official
- [Todo.txt Format](http://todotxt.org/) - Official specification
- [GitHub repo](https://github.com/todotxt/todo.txt) - Reference implementation
- [Todo.txt Apps](http://todotxt.org/#app) - Compatible apps

### Tools & Extensions
- [todo.txt-cli](https://github.com/todotxt/todo.txt-cli) - Command-line tool
- [Simpletask](https://play.google.com/store/apps/details?id=nl.mpcjanssen.simpletask) - Android app
- [Todour](https://nerdur.com/todour-pl/) - Desktop app

### Sync Solutions
- [Syncthing](https://syncthing.net/) - P2P file sync
- [Nextcloud](https://nextcloud.com/) - Self-hosted cloud
- Dropbox, Google Drive, OneDrive - Cloud storage

### Community
- [/r/todotxt](https://reddit.com/r/todotxt) - Reddit community
- Todo.txt forums - Discussion and tips

---

## Examples

### Personal Life
```
(A) 2025-11-11 Pay rent due:2025-11-30 @home @bills
(B) 2025-11-11 Dentist appointment due:2025-11-15 @phone
(C) 2025-11-11 Buy birthday gift +Shopping @errands
(C) 2025-11-11 Call Mom @phone @family
Exercise rec:+1d @home @health
```

### Work Tasks
```
(A) 2025-11-11 Submit Q4 report due:2025-11-15 +Reporting @work
(A) 2025-11-11 Fix critical bug #456 +ProjectAlpha @coding @urgent
(B) 2025-11-11 Code review PR #123 +Development @computer
(B) 2025-11-11 Team standup due:2025-11-12 +Meetings @office
(C) 2025-11-11 Update documentation +Docs @writing
```

### Project Management
```
(A) 2025-11-11 Define project scope +NewFeature @planning
(A) 2025-11-11 Create technical spec +NewFeature @documentation
(B) 2025-11-11 Design database schema +NewFeature @architecture
(B) 2025-11-11 Implement backend API +NewFeature @backend @coding
(C) 2025-11-11 Write unit tests +NewFeature @testing
(C) 2025-11-11 Deploy to staging +NewFeature @devops
```

---

## Next Steps

- **[Markdown Format →](./markdown.md)** - Rich text formatting
- **[CSV Format →](./csv.md)** - Data and tables
- **[Plain Text Format →](./plaintext.md)** - Simple text files
- **[Back to Getting Started →](../getting-started.md)**

---

*Last updated: November 11, 2025*
*Yole version: 2.15.1+*
