# TaskPaper Format Guide

## Overview

TaskPaper is a plain-text format for task management, developed by Hog Bay Software.

## Supported Extensions

- `.taskpaper` - TaskPaper files

## Format

```taskpaper
- Task item
- Another task
    - Subtask
```

## Tasks

```taskpaper
- Buy groceries @errand
- Submit report @work +urgent
- Review PR @github @work
```

## Projects

```taskpaper
Project Name:
- Task in project
- Another task

Personal:
- Personal task
```

## Tags

```taskpaper
- Task @tag1 @tag2
- Task with custom @priority(high)
```

## Notes

```taskpaper
This is a note without dash
It continues here.

- This is a task
```

## Done Tasks

```taskpaper
- Completed task @done
- x Completed task with x prefix
```

## Filter Syntax

```taskpaper
@work
@github and @done
+project
not @done
```

## See Also

- [TaskPaper Format](https://www.taskpaper.com)
