# Module 3: Todo.txt Manager (7 videos)

## Video 3.1: Understanding Todo.txt Format (8 min)
- Todo.txt syntax specification
- Priority system (A)-(Z), completion markers, dates
- Projects (+project) and contexts (@context)
- Yole's extended query syntax

## Video 3.2: Parsing Todo.txt Files (12 min)
- Implement TodoTxtParser with regex-based tokenizer
- Handle edge cases: empty lines, multi-line descriptions
- Property-based testing with random task generation

## Video 3.3: Creating the UI (10 min)
- Task list with priority color coding
- Group by project or context
- Swipe-to-complete gesture (Android)

## Video 3.4: Adding and Editing Tasks (12 min)
- Task creation dialog with priority picker
- Inline editing with format preservation
- Validation: date formats, priority ranges

## Video 3.5: Completing and Archiving (8 min)
- Mark tasks complete (prepend `x`)
- Archive completed tasks to `done.txt`
- Completion statistics and streaks

## Video 3.6: Search and Filter (10 min)
- Yole query syntax: `@context +project pri:A due:today`
- Saved filters as bookmarks
- Sort by priority, date, or alphabetical

## Video 3.7: Cross-Platform Testing (8 min)
- Run TodoTxtParserTests on JVM, Wasm, Native
- Handle platform differences in date formatting
- Integration test: create, edit, complete, archive cycle
