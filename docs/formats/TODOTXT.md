# Todo.txt Format Guide

## Overview

Todo.txt is a plain-text format for managing tasks, originally developed by Gina Trapani. It follows the Todo.txt specification with support for priorities, contexts, projects, and completion tracking.

## Format Specification

### Basic Task
```
Task description
```

### Task with Priority
```
(A) Task with priority A
(B) Task with priority B
(C) Task with priority C
```

### Completed Task
```
x 2024-01-15 Completed task
x 2024-01-10 2024-01-15 Completed with dates
```

### Task with Context
```
Task @context
Task @home @work
```

### Task with Project
```
Task +project
Task +project1 +project2
```

### Complete Task
```
(A) Task @context +project due:2024-01-31
```

## Priority Levels

- (A) - Highest priority
- (B) - High priority  
- (C) - Medium priority
- (D) - Low priority
- No priority - Unprioritized

## Date Formats

- Completion: `x YYYY-MM-DD`
- Due date: `due:YYYY-MM-DD`
- Threshold: `t:YYYY-MM-DD`

## Examples

### Simple Task
```
Buy groceries
```

### Priority Task
```
(A) Submit quarterly report
```

### Context and Project
```
x 2024-01-15 Review pull request @github +yole due:2024-01-20
```

### Task with Tags
```
Task description @context +project due:2024-02-01 t:2024-01-15
```

## Parser Features

Yole's Todo.txt parser supports:
- Priority parsing (A-Z)
- Context extraction (@context)
- Project extraction (+project)
- Date parsing (due:, t:, completion)
- Task completion toggle
- Filter and query syntax
- Bulk operations

## Query Syntax

The parser supports advanced filtering:
- `pri:A` - Priority A tasks
- `@context` - Tasks with context
- `+project` - Tasks in project
- `due:2024` - Due in 2024
- `completed:true` - Completed tasks

## See Also

- [Todo.txt Format Specification](https://github.com/todotxt/todo.txt)
