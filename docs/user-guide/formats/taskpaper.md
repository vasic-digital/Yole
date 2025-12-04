# TaskPaper Format Guide

**Format**: TaskPaper
**Extensions**: `.taskpaper`, `.todo`
**Specification**: [TaskPaper Format](https://www.taskpaper.com/)
**Yole Support**: ✅ Syntax highlighting, basic parsing

---

## Overview

TaskPaper is a plain-text task management format that uses minimal syntax for maximum readability. Created by Jesse Grosjean, it organizes tasks into projects with tags for metadata.

### Why TaskPaper?

- **Plain Text**: Future-proof, portable, version-control friendly
- **Minimal Syntax**: Just three elements (projects, tasks, notes)
- **Flexible Tags**: Add metadata without rigid structure
- **Human-Readable**: Easy to read without special tools
- **Search & Filter**: Powerful query syntax for finding tasks
- **Distraction-Free**: Focus on tasks, not interface

---

## Basic Syntax

### Three Elements

TaskPaper has only three element types:

1. **Projects**: Lines ending with colon `:`
2. **Tasks**: Lines starting with dash `-`
3. **Notes**: Everything else

```taskpaper
Project Name:
- Task item
  Note about task

Another Project:
- Another task
```

---

## Projects

### Basic Projects

```taskpaper
Work:
- Important task
- Another work task

Personal:
- Personal task
- Another personal task
```

### Nested Projects

```taskpaper
Work:
  Client Projects:
    - Client A task
    - Client B task

  Internal Projects:
    - Internal task

Personal:
  Home:
    - Fix sink
    - Paint bedroom
```

**Indentation**: Use tabs or spaces consistently.

### Project Tags

```taskpaper
Work @active:
- Current task

Old Project @archived:
- Completed task
```

---

## Tasks

### Basic Tasks

```taskpaper
Project:
- Task 1
- Task 2
- Task 3
```

### Tasks with Tags

```taskpaper
Project:
- Important task @priority(high)
- Deadline task @due(2025-11-15)
- Quick task @duration(5m)
- Assignment @assigned(Alice)
```

### Completed Tasks

```taskpaper
Project:
- Finished task @done
- Also finished @done(2025-11-11)
- In progress task
```

**Done tag**: Use `@done` or `@done(date)` to mark completion.

### Cancelled Tasks

```taskpaper
Project:
- Cancelled task @cancelled
- Won't do this @cancelled(too expensive)
```

---

## Tags

### Tag Syntax

```taskpaper
- Task @tag
- Task @tag(value)
- Task @tag(value with spaces)
- Task @multiple @tags @here
```

**Tag Format**:
- Start with `@`
- Optional value in parentheses
- No spaces in tag name
- Values can have spaces

### Common Tags

```taskpaper
Project:
- High priority @priority(high) @urgent
- Has deadline @due(2025-12-01)
- Work estimate @estimate(2h)
- Assigned task @assigned(Bob)
- Context @context(office) @computer
- Done task @done(2025-11-10)
```

### Custom Tags

```taskpaper
Project:
- Research task @category(research)
- Waiting for approval @status(waiting)
- Needs review @flag
- Energy level @energy(high)
```

**Flexibility**: Create any tags you need!

### Date Tags

```taskpaper
Project:
- Task due today @due(2025-11-11)
- Task due next week @due(2025-11-18)
- Started yesterday @started(2025-11-10)
- Finished today @done(2025-11-11)
- Deferred @defer(2025-12-01)
```

**Date Format**: Usually `YYYY-MM-DD` but can be flexible.

---

## Notes

### Task Notes

```taskpaper
Project:
- Task with notes
  This is a note about the task.
  Notes are indented under the task.

  Can include multiple paragraphs.

- Another task
```

### Project Notes

```taskpaper
Work:
  General notes about work project.
  More information here.

- First task
- Second task
```

### Formatted Notes

```taskpaper
Project:
- Task
  Note: Important information

  Reference:
  - https://example.com
  - See document.pdf

  Steps:
  1. First step
  2. Second step
```

---

## Searching and Filtering

### Search Operators

```taskpaper
# Find tasks with tag
@priority

# Find high priority tasks
@priority(high)

# Find tasks without tag
not @done

# Find project by name
Work:

# Combine conditions
@priority and not @done

# Find due soon
@due < 2025-11-15
```

### Common Searches

```taskpaper
# Active tasks (not done)
not @done

# High priority incomplete tasks
@priority(high) and not @done

# Tasks due this week
@due >= 2025-11-11 and @due <= 2025-11-17

# Assigned to me
@assigned(me)

# Work tasks
Work: //or// @context(work)

# Blocked tasks
@waiting or @blocked
```

### Advanced Queries

```taskpaper
# Complex search
(@priority(high) or @urgent) and not @done and @due

# Project-specific
Work: and @priority(high)

# Tag combinations
(@computer or @online) and not @done
```

---

## TaskPaper in Yole

### Supported Features

✅ **Projects**: Colon-terminated lines
✅ **Tasks**: Dash-prefixed lines
✅ **Notes**: Indented text
✅ **Tags**: @tag and @tag(value) syntax
✅ **Syntax Highlighting**: Projects, tasks, tags, done
✅ **Indentation**: Nested structure
✅ **Editing**: Full text editing

### Syntax Highlighting

Yole highlights:
- **Projects**: Bold/distinct color
- **Tasks**: Standard color
- **Tags**: Tag highlighting
- **@done**: Strikethrough or dimmed
- **@priority**: Color-coded priorities
- **Dates**: Date value highlighting

### Limitations

❌ **Search Queries**: No built-in search (yet)
❌ **Filtering**: No dynamic filtering
❌ **Date Calculation**: No date math
❌ **Folding**: No project/task folding (yet)
❌ **Automatic Archiving**: Manual only

**Recommendation**: Use TaskPaper app for advanced features, Yole for editing.

---

## Common Use Cases

### 1. Personal Task Management

```taskpaper
Inbox:
- Process email @due(2025-11-11)
- Call dentist @phone @priority(high)
- Review budget @due(2025-11-15)

Work:
  Current Sprint:
    - Fix login bug @priority(high) @estimate(2h)
    - Update documentation @estimate(1h)
    - Code review PR #123 @waiting(Alice)

  Backlog:
    - Refactor authentication @estimate(1d)
    - Add new feature X @priority(low)

Personal:
  Home:
    - Fix leaky faucet @due(2025-11-12) @context(home)
    - Organize garage @duration(3h)

  Shopping:
    - Buy groceries @context(errands)
    - Get birthday gift for Mom @due(2025-11-20)

Someday:
- Learn Spanish
- Write blog post about TaskPaper
- Plan vacation

Archive:
- Completed project @done(2025-11-01) @archived
```

### 2. Project Management

```taskpaper
Website Redesign Project @active:
  Planning Phase @done(2025-11-01):
    - Define requirements @done(2025-10-15)
      Completed requirements document in /docs/

    - Create mockups @done(2025-10-22)
      Mockups approved by stakeholders

    - Get approval @done(2025-11-01)

  Development Phase @current:
    Frontend @progress(60%):
      - Setup React project @done(2025-11-05)
      - Implement navigation @done(2025-11-08)
      - Create homepage @in-progress @assigned(Alice)
      - Add contact form @priority(high) @due(2025-11-15)

    Backend @progress(40%):
      - Setup API server @done(2025-11-06)
      - Database schema @done(2025-11-09)
      - Authentication endpoints @in-progress @assigned(Bob)
      - Contact form API @due(2025-11-14)

  Testing Phase @planned @start(2025-11-20):
    - Unit testing
    - Integration testing
    - User acceptance testing

  Launch Phase @planned @start(2025-12-01):
    - Deploy to staging
    - Final review
    - Deploy to production

Project Notes:
  Budget: $50,000
  Timeline: Nov 1 - Dec 15
  Team: Alice (Frontend), Bob (Backend), Charlie (Design)
```

### 3. GTD (Getting Things Done)

```taskpaper
Capture:
- Random thought to process later
- Email follow-up needed
- Book recommendation from friend

Inbox @process:
- Sort these tasks into proper lists
- Review daily
- Archive when done

Next Actions @context:
  @computer:
    - Write report @priority(high) @due(2025-11-12)
    - Update spreadsheet @estimate(30m)

  @phone:
    - Call insurance company @priority(high)
    - Schedule dentist appointment

  @errands:
    - Post office @due(2025-11-11)
    - Grocery shopping

  @office:
    - File documents @priority(low)
    - Organize desk

Projects:
  Plan Conference @due(2025-12-15):
    - Book venue @priority(high) @due(2025-11-20)
    - Send invitations @due(2025-11-25)
    - Prepare agenda @due(2025-12-01)

  Update Resume:
    - Review current version
    - Add recent projects
    - Get feedback from mentor

Waiting For:
- Approval from manager @waiting(John) @follow-up(2025-11-15)
- Report from team member @waiting(Alice)
- Invoice payment @waiting(accounting)

Someday/Maybe:
- Learn photography
- Write a book
- Start a podcast
- Travel to Japan

Reference:
  Important info that's not actionable but good to keep.
  Meeting notes, ideas, etc.
```

### 4. Daily/Weekly Review

```taskpaper
Daily Review - 2025-11-11 @template:
  Morning:
    - Review inbox @done
    - Plan top 3 tasks for today @done
      1. @priority(high) task
      2. @priority(high) task
      3. @priority(medium) task
    - Check calendar @done

  Throughout Day:
    - Process new items as they arrive
    - Update task status
    - Add notes to tasks

  Evening:
    - Review completed tasks @done
    - Move incomplete tasks to tomorrow
    - Plan tomorrow's priorities
    - Archive completed projects

Weekly Review - Week of 2025-11-11:
  Sunday:
    - Review past week @done
    - Check all projects @done
    - Update project status @done
    - Archive completed @done

  Completed This Week:
    - 15 tasks completed @done
    - 2 projects finished @done
    - All high-priority items done @done

  Upcoming Week:
    - 20 tasks planned
    - 3 deadlines approaching @alert
    - 2 meetings scheduled

  Notes:
    Good progress on main project.
    Need to focus more on personal tasks.
    Consider delegating some work items.
```

---

## Best Practices

### File Organization

**Single file approach**:
```
tasks.taskpaper
```

**Multiple file approach**:
```
tasks/
├── work.taskpaper
├── personal.taskpaper
├── projects.taskpaper
└── archive.taskpaper
```

### Project Structure

**Good hierarchy**:
```taskpaper
Main Category:
  Sub-Category:
    - Task 1
    - Task 2

  Another Sub:
    - Task 3
```

**Consistent indentation**: Use tabs or 2/4 spaces.

### Tag Strategy

**Consistent tag names**:
```taskpaper
# Good - consistent
- Task @priority(high)
- Task @priority(medium)
- Task @priority(low)

# Bad - inconsistent
- Task @priority(high)
- Task @important
- Task @low-priority
```

**Common tag categories**:
```taskpaper
Priority: @priority(high/medium/low)
Status: @done, @in-progress, @waiting, @cancelled
Time: @due(date), @start(date), @estimate(time)
Context: @computer, @phone, @office, @home, @errands
Assignment: @assigned(person)
```

### Regular Maintenance

**Archive completed tasks**:
```taskpaper
Archive - 2025-11:
- Completed task 1 @done(2025-11-05)
- Completed task 2 @done(2025-11-08)
```

**Review and update**:
- Daily: Update task status
- Weekly: Review all projects
- Monthly: Archive completed tasks

### Date Formats

**Consistent dates**:
```taskpaper
# ISO format (recommended)
@due(2025-11-15)

# Natural language (if app supports)
@due(next Friday)
@due(in 2 weeks)
```

---

## Tips & Tricks

### 🎯 Pro Tips

1. **Keep it simple**: Don't over-tag
2. **Review regularly**: Daily and weekly reviews
3. **Archive often**: Move done tasks to archive
4. **Consistent structure**: Stick to your system
5. **Use contexts**: Group by where/when you can do tasks

### 🚀 Power User Techniques

**Templates**:
```taskpaper
Project Template:
  Planning:
    - Define goals
    - Set timeline
    - Assign resources

  Execution:
    - Task 1
    - Task 2
    - Task 3

  Review:
    - Evaluate results
    - Document lessons learned
```

**Repeated tasks**:
```taskpaper
Recurring Tasks:
- Weekly report @due(Friday) @repeat(weekly)
- Monthly review @due(last day) @repeat(monthly)
- Quarterly planning @due(end of quarter) @repeat(quarterly)
```

**Energy-based organization**:
```taskpaper
High Energy Tasks @energy(high):
- Creative work @priority(high)
- Important decisions
- Complex problem solving

Low Energy Tasks @energy(low):
- File organization
- Email cleanup
- Simple administrative tasks
```

**Time blocking**:
```taskpaper
Today @date(2025-11-11):
  Morning (9-12):
    - Deep work task @estimate(2h)
    - Meeting prep @estimate(30m)

  Afternoon (1-5):
    - Team meeting @time(2-3pm)
    - Email processing @estimate(1h)
    - Code review @estimate(1h)
```

---

## TaskPaper vs. Todo.txt

### Similarities

- Plain text format
- Portable and future-proof
- Human-readable syntax
- Tag-based metadata

### Differences

| Feature | TaskPaper | Todo.txt |
|---------|-----------|----------|
| **Structure** | Projects + Tasks | Flat task list |
| **Tags** | `@tag(value)` | `+project @context` |
| **Priority** | `@priority(high)` | `(A)` at start |
| **Done** | `@done` tag | `x` prefix |
| **Dates** | Flexible tags | Fixed format |
| **Nesting** | Full hierarchy | Flat list |
| **Flexibility** | Very flexible | More structured |

### When to Use Each

**Use TaskPaper** when:
- You want project hierarchy
- You prefer flexible tagging
- You need nested organization
- You want human-readable format

**Use Todo.txt** when:
- You prefer flat list
- You want strict spec
- You use todo.txt ecosystem
- You want simple syntax

---

## External Resources

### Official
- [TaskPaper Website](https://www.taskpaper.com/)
- [TaskPaper Format Guide](https://guide.taskpaper.com/getting-started/)

### Apps
- **TaskPaper**: Mac app (original)
- **Editorial**: iOS app with TaskPaper support
- **1Writer**: iOS app with TaskPaper mode
- **Yole**: Cross-platform TaskPaper editor

### Community
- [TaskPaper Forum](https://www.taskpaper.com/support)
- [TaskPaper Scripts](https://github.com/topics/taskpaper)

### Resources
- [TaskPaper Best Practices](https://www.taskpaper.com/learn)
- [GTD with TaskPaper](https://www.taskpaper.com/blog/gtd-setup)

---

## Next Steps

- **[Todo.txt Format →](./todotxt.md)** - Alternative task format
- **[Markdown Format →](./markdown.md)** - Formatted documents
- **[Plain Text Format →](./plaintext.md)** - Simple notes
- **[Back to Getting Started →](../getting-started.md)**

---

*Last updated: November 11, 2025*
*Yole version: 2.15.1+*
