# Module 3: Todo.txt Manager (7 videos)

## Video 3.1: Understanding Todo.txt Format (8 min)

### Timestamps
- 0:00 Introduction to plain-text task management
- 1:00 Todo.txt format specification overview
- 2:30 Priority system: `(A)` through `(Z)` with color coding
- 3:30 Completion markers: `x ` prefix and completion dates
- 4:30 Creation dates in `YYYY-MM-DD` format
- 5:30 Projects (`+project`) and contexts (`@context`)
- 6:30 Key-value metadata pairs (`key:value`, e.g., `due:2025-12-31`)
- 7:15 Yole's extended query syntax overview
- 7:45 Summary and next video preview

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/todotxt/TodoTxtParser.kt` -- `TodoTxtTask` data class defines all task fields
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/TextFormat.kt` -- `TextFormat.ID_TODOTXT` format identifier

### Key Concepts
The `TodoTxtTask` data class models every component of a todo.txt line:

```kotlin
data class TodoTxtTask(
    val line: String,
    val priority: Char? = null,       // (A)-(Z)
    val description: String = "",
    val done: Boolean = false,
    val creationDate: String? = null,  // YYYY-MM-DD
    val completionDate: String? = null,
    val dueDate: String? = null,       // due:YYYY-MM-DD
    val projects: List<String> = emptyList(),   // +project
    val contexts: List<String> = emptyList(),   // @context
    val keyValues: Map<String, String> = emptyMap()
)
```

### Exercises
1. **Create a basic todo list** -- Write a `todo.txt` file with 5 tasks covering priorities A-C, at least 2 projects, and 3 contexts. Open it in Yole and verify parsing.
2. **Explore priority display** -- Add tasks with every priority from A to E and observe how Yole color-codes each level.
3. **Add due dates** -- Create tasks with `due:` metadata and observe how Yole extracts and displays them.

---

## Video 3.2: Parsing Todo.txt Files (12 min)

### Timestamps
- 0:00 Recap: TodoTxtTask data model
- 1:00 Parser architecture: regex-based line tokenizer
- 2:30 Parsing completion status (`x ` prefix detection)
- 3:30 Parsing priority with regex: `^\(([A-Z])\)\s`
- 4:30 Date extraction: creation date vs. completion date ordering rules
- 5:30 Project and context extraction with `\+\S+` and `@\S+` patterns
- 7:00 Key-value pair extraction with `\S+:\S+` pattern
- 8:00 Edge cases: empty lines, lines with only whitespace
- 9:00 Multi-line descriptions and continuation handling
- 10:00 Building a ParsedDocument from a list of TodoTxtTask objects
- 11:00 Property-based testing approach with random task generation
- 11:45 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/todotxt/TodoTxtParser.kt` -- main parser implementation
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/TextParser.kt` -- `ParsedDocument` class with lazy HTML caching
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/todotxt/TodoTxtParserTest.kt` -- parser test suite

### Key Code Walkthrough
Show how the parser processes a single line:

```kotlin
// Example: parsing "(A) 2025-01-15 Call mom +Family @phone due:2025-02-01"
// Step 1: Check for "x " prefix -> done = false
// Step 2: Match priority regex -> priority = 'A'
// Step 3: Extract dates -> creationDate = "2025-01-15"
// Step 4: Find +tokens -> projects = ["Family"]
// Step 5: Find @tokens -> contexts = ["phone"]
// Step 6: Find key:value -> keyValues = {"due": "2025-02-01"}, dueDate = "2025-02-01"
// Step 7: Remaining text -> description = "Call mom"
```

### Exercises
1. **Trace the parser** -- Open `TodoTxtParser.kt` and trace through the parsing of `x 2025-01-10 2025-01-01 (B) Finish report +Work @office`. Identify each field extracted.
2. **Write a malformed input test** -- Create a test case for a line with no priority, no dates, and special characters in the description.
3. **Random generation** -- Write a function that generates 100 random valid todo.txt lines and verify they all parse without errors.

---

## Video 3.3: Creating the UI (10 min)

### Timestamps
- 0:00 Task list layout with Compose Multiplatform
- 1:30 Priority color coding scheme (A=red, B=orange, C=yellow, D-Z=default)
- 3:00 Grouping tasks by project or context
- 4:30 Expandable/collapsible sections for groups
- 6:00 Swipe-to-complete gesture implementation (Android)
- 7:30 Desktop keyboard shortcuts for task completion
- 8:30 Cross-platform UI consistency considerations
- 9:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/ui/Theme.kt` -- color definitions and theme tokens
- `shared/src/commonMain/kotlin/digital/vasic/yole/ui/Accessibility.kt` -- accessibility support

### Exercises
1. **Customize priority colors** -- Modify the theme to use your own color palette for priorities A through E.
2. **Group by context** -- Filter a todo list to show only tasks with `@home` context.
3. **Test accessibility** -- Enable TalkBack/VoiceOver and verify all task elements are properly announced.

---

## Video 3.4: Adding and Editing Tasks (12 min)

### Timestamps
- 0:00 Task creation dialog overview
- 1:30 Priority picker UI component
- 3:00 Date picker integration for creation and due dates
- 4:30 Project and context auto-complete from existing tags
- 6:00 Inline editing with format preservation
- 7:30 Validation: date format `YYYY-MM-DD`, priority range `A-Z`
- 9:00 Preserving line ordering and formatting
- 10:30 Undo/redo support for task edits
- 11:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/todotxt/TodoTxtParser.kt` -- task serialization back to text
- `shared/src/commonMain/kotlin/digital/vasic/yole/model/Document.kt` -- document model and editing operations

### Exercises
1. **Create a task programmatically** -- Write code that creates a `TodoTxtTask` with priority B, project "Yole", context "dev", and a due date one week from today, then serialize it to a valid todo.txt line.
2. **Test format preservation** -- Edit a task's priority from A to C and verify the rest of the line remains unchanged.
3. **Validation edge case** -- Try creating a task with an invalid date like `2025-13-45` and verify the validation catches it.

---

## Video 3.5: Completing and Archiving (8 min)

### Timestamps
- 0:00 Marking tasks complete: the `x ` prefix convention
- 1:00 Completion date insertion: `x 2025-01-15 ...`
- 2:30 Archiving completed tasks to `done.txt`
- 3:30 Archive workflow: move vs. copy
- 5:00 Completion statistics and streaks
- 6:00 Batch operations: complete/archive multiple tasks
- 7:00 Cross-platform file handling for archive operations
- 7:45 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/todotxt/TodoTxtParser.kt` -- completion status handling
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/todotxt/TodoTxtParserTest.kt` -- completion test cases

### Exercises
1. **Create a todo list with mixed states** -- Write a file with 3 completed and 3 incomplete tasks. Open in Yole and verify visual distinction between completed and active tasks.
2. **Test the archive cycle** -- Create a task, complete it, archive it, and verify it appears in `done.txt` with the correct completion date.
3. **Streak tracking** -- Complete one task per day for a week and observe how completion statistics update.

---

## Video 3.6: Search and Filter (10 min)

### Timestamps
- 0:00 Introduction to Yole's query syntax
- 1:30 Filtering by context: `@context` search
- 2:30 Filtering by project: `+project` search
- 3:30 Priority filtering: `pri:A` and `pri:A-C` range syntax
- 4:30 Due date filtering: `due:today`, `due:this-week`, `due:overdue`
- 5:30 Combining filters: `@work +ProjectX pri:A due:this-week`
- 6:30 Saved filters as bookmarks
- 7:30 Sort options: priority, date, alphabetical, project
- 8:30 Advanced query operators: AND, OR, NOT
- 9:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/format/todotxt/TodoTxtParser.kt` -- query parsing and filtering logic
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/todotxt/TodoTxtParserTest.kt` -- query syntax test cases with real examples

### Key Query Syntax Examples

```
# Filter by context
@phone

# Filter by project and priority
+Work pri:A

# Due date queries
due:today
due:overdue

# Combined query
@office +Project pri:A-B due:this-week
```

### Exercises
1. **Create a todo list with 20+ tasks** -- Include a mix of priorities, projects, and contexts. Practice filtering with each query type.
2. **Use query syntax to filter** -- Write queries to find: (a) all high-priority phone calls, (b) all overdue work tasks, (c) all tasks in the "Family" project without a due date.
3. **Add priorities and contexts** -- Take a plain list of tasks and enrich them with priorities, projects, and contexts. Then write queries that would be useful for your daily workflow.

---

## Video 3.7: Cross-Platform Testing (8 min)

### Timestamps
- 0:00 KMP testing strategy for Todo.txt
- 1:00 Running TodoTxtParserTest on JVM target
- 2:00 Running on Wasm/JS target: differences and limitations
- 3:00 Running on Native (iOS) target
- 4:00 Platform differences in date formatting and locale handling
- 5:00 Integration test: create, edit, complete, archive cycle
- 6:00 Edge case tests: Unicode, emoji in task descriptions, long lines
- 7:00 Running the full test suite with `./gradlew test`
- 7:45 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/todotxt/TodoTxtParserTest.kt` -- the primary Todo.txt test suite
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/integration/CrossFormatIntegrationTest.kt` -- cross-format tests including todo.txt
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/FormatParsingStressTest.kt` -- stress tests

### Exercises
1. **Run the test suite** -- Execute `./gradlew test --tests "digital.vasic.yole.format.todotxt.TodoTxtParserTest"` and review the results.
2. **Add an edge case test** -- Write a test for a todo.txt line containing emoji characters (e.g., tasks with flag or checkmark emoji) and verify parsing handles them correctly.
3. **Cross-platform verification** -- Run the same test on at least two different targets (JVM and Wasm) and compare results to ensure consistent behavior.
