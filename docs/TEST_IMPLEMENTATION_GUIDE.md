# Test Implementation Guide - Completing Phase 2

**Comprehensive guide for implementing the remaining 800+ tests to achieve >80% code coverage**

---

## Overview

This guide provides step-by-step instructions for completing Phase 2 test implementation. The test infrastructure is in place, and scaffolds have been generated. This guide focuses on completing and customizing tests for maximum coverage.

**Current Status**:
- ✅ FormatRegistry & TextFormat: 126 tests (99% coverage)
- ⏳ 17 Format Parsers: Scaffolds generated, need customization (0/540 tests complete)
- ✅ Android UI: 50+ tests already exist
- ⏸️ Desktop UI: Needs test creation (0/100 tests)
- ⏸️ Integration: Needs test creation (0/50 tests)

**Goal**: 920+ tests, >80% overall coverage

---

## Part 1: Completing Format Parser Tests (540 tests)

### Generated Test Scaffolds

All 17 parser test files have been generated in:
```
shared/src/commonTest/kotlin/digital/vasic/yole/format/
├── markdown/MarkdownParserTest.kt
├── todotxt/Todo.txtParserTest.kt
├── plaintext/PlainTextParserTest.kt
├── csv/CsvParserTest.kt
├── latex/LatexParserTest.kt
├── orgmode/OrgModeParserTest.kt
├── wikitext/WikitextParserTest.kt
├── asciidoc/AsciidocParserTest.kt
├── restructuredtext/RestructuredTextParserTest.kt
├── keyvalue/KeyValueParserTest.kt
├── taskpaper/TaskpaperParserTest.kt
├── textile/TextileParserTest.kt
├── creole/CreoleParserTest.kt
├── tiddlywiki/TiddlyWikiParserTest.kt
├── jupyter/JupyterParserTest.kt
├── rmarkdown/RMarkdownParserTest.kt
└── binary/BinaryParserTest.kt
```

### Step-by-Step: Customizing Parser Tests

Each parser test template has **placeholder content** that needs to be replaced with **format-specific samples**. Here's the process:

#### 1. Choose a Format to Test

Start with high-priority formats:
1. **Markdown** (.md) - Most complex, highest priority
2. **Todo.txt** (.txt) - Task management format
3. **Plain Text** (.txt) - Baseline format
4. **CSV** (.csv) - Data format

Then medium priority (LaTeX, Org Mode, WikiText, AsciiDoc, reStructuredText), then low priority (remaining formats).

#### 2. Open the Generated Test File

Example: `shared/src/commonTest/kotlin/digital/vasic/yole/format/markdown/MarkdownParserTest.kt`

#### 3. Replace Placeholder Content

The template has placeholders like:
```kotlin
val content = """
    Sample Markdown content here
""".trimIndent()
```

Replace with **real format samples**:

```kotlin
// ❌ BEFORE (Placeholder)
val content = """
    Sample Markdown content here
""".trimIndent()

// ✅ AFTER (Real Markdown)
val content = """
    # Title

    This is a paragraph with **bold** and *italic* text.

    - List item 1
    - List item 2

    ```kotlin
    fun main() {
        println("Hello")
    }
    ```
""".trimIndent()
```

#### 4. Add Format-Specific Tests

The template has a section marked:
```kotlin
// ==================== Format-Specific Tests ====================
// Add format-specific parsing tests below
```

Add tests for format-specific features:

**Example for Markdown**:
```kotlin
@Test
fun `should parse headers correctly`() {
    val content = """
        # H1 Header
        ## H2 Header
        ### H3 Header
    """.trimIndent()

    val result = parser.parse(content)

    assertNotNull(result)
    assertThat(result.parsedContent).contains("<h1>")
    assertThat(result.parsedContent).contains("<h2>")
    assertThat(result.parsedContent).contains("<h3>")
}

@Test
fun `should parse bold and italic text`() {
    val content = "This is **bold** and *italic* text"
    val result = parser.parse(content)

    assertThat(result.parsedContent).contains("<strong>bold</strong>")
    assertThat(result.parsedContent).contains("<em>italic</em>")
}

@Test
fun `should parse lists`() {
    val content = """
        - Item 1
        - Item 2
        - Item 3
    """.trimIndent()

    val result = parser.parse(content)

    assertThat(result.parsedContent).contains("<ul>")
    assertThat(result.parsedContent).contains("<li>")
}

@Test
fun `should parse code blocks`() {
    val content = """
        ```kotlin
        fun test() {}
        ```
    """.trimIndent()

    val result = parser.parse(content)

    assertThat(result.parsedContent).contains("<pre>")
    assertThat(result.parsedContent).contains("<code>")
}

@Test
fun `should parse links`() {
    val content = "[Link text](https://example.com)"
    val result = parser.parse(content)

    assertThat(result.parsedContent).contains("<a href=")
    assertThat(result.parsedContent).contains("https://example.com")
}

@Test
fun `should parse images`() {
    val content = "![Alt text](image.png)"
    val result = parser.parse(content)

    assertThat(result.parsedContent).contains("<img")
    assertThat(result.parsedContent).contains("image.png")
}

@Test
fun `should parse tables`() {
    val content = """
        | Header 1 | Header 2 |
        |----------|----------|
        | Cell 1   | Cell 2   |
    """.trimIndent()

    val result = parser.parse(content)

    assertThat(result.parsedContent).contains("<table>")
    assertThat(result.parsedContent).contains("<th>")
    assertThat(result.parsedContent).contains("<td>")
}

@Test
fun `should parse task lists`() {
    val content = """
        - [x] Completed task
        - [ ] Pending task
    """.trimIndent()

    val result = parser.parse(content)

    assertThat(result.parsedContent).contains("checkbox")
}

@Test
fun `should parse blockquotes`() {
    val content = "> This is a quote"
    val result = parser.parse(content)

    assertThat(result.parsedContent).contains("<blockquote>")
}

@Test
fun `should parse horizontal rules`() {
    val content = "---"
    val result = parser.parse(content)

    assertThat(result.parsedContent).contains("<hr")
}
```

#### 5. Test with Real Parser Implementation

After customizing tests, run them:

```bash
# Run tests for specific format
./gradlew :shared:testDebugUnitTest --tests "*MarkdownParserTest"

# Run all parser tests
./gradlew :shared:testDebugUnitTest --tests "*.format.*ParserTest"

# Run with coverage
./gradlew :shared:testDebugUnitTest koverHtmlReport
```

#### 6. Achieve 90% Coverage Per Parser

Each parser should have at least:
- ✅ 5 format detection tests (extensions, filename, content)
- ✅ 5 basic parsing tests (empty, whitespace, single/multi-line)
- ✅ 3 special character tests (unicode, escaping, line endings)
- ✅ 3 error handling tests (malformed, long input, binary)
- ✅ 10+ format-specific feature tests (headers, lists, tables, etc.)
- ✅ 4 integration tests (registry, performance, concurrency)

**Minimum: 30 tests per parser**

### Format-Specific Testing Checklists

#### Markdown Parser Tests
- [ ] Headers (H1-H6)
- [ ] Bold (**text**)
- [ ] Italic (*text*)
- [ ] Strikethrough (~~text~~)
- [ ] Links ([text](url))
- [ ] Images (![alt](url))
- [ ] Lists (ordered and unordered)
- [ ] Task lists ([x] and [ ])
- [ ] Code blocks (``` and ~~~)
- [ ] Inline code (`code`)
- [ ] Blockquotes (>)
- [ ] Tables (| | | )
- [ ] Horizontal rules (---)
- [ ] Footnotes ([^1])
- [ ] Nested structures

#### Todo.txt Parser Tests
- [ ] Priority parsing ((A), (B), etc.)
- [ ] Completion markers (x)
- [ ] Dates (YYYY-MM-DD)
- [ ] Contexts (@context)
- [ ] Projects (+project)
- [ ] Key:value pairs
- [ ] Sorting by priority
- [ ] Filtering completed
- [ ] Due dates
- [ ] Recurrence

#### LaTeX Parser Tests
- [ ] \\documentclass
- [ ] \\begin{document} / \\end{document}
- [ ] \\section, \\subsection
- [ ] Math mode ($ and $$)
- [ ] \\textbf, \\textit
- [ ] Lists (\\begin{itemize})
- [ ] Tables (\\begin{tabular})
- [ ] Figures (\\begin{figure})
- [ ] Citations (\\cite)
- [ ] Bibliography

#### CSV Parser Tests
- [ ] Comma-separated values
- [ ] Quoted fields
- [ ] Escaped quotes
- [ ] Different delimiters (;, tab)
- [ ] Headers
- [ ] Empty fields
- [ ] Multi-line fields
- [ ] Special characters in fields

*(See PHASE_2_PROGRESS.md for complete checklists for all 17 formats)*

---

## Part 2: Desktop UI Tests (100 tests)

### Current State

No Desktop UI tests exist yet. Need to create comprehensive test suite.

### Desktop Test Structure

Create tests in: `desktopApp/src/test/kotlin/digital/vasic/yole/desktop/`

```
desktopApp/src/test/kotlin/digital/vasic/yole/desktop/
├── ui/
│   ├── MainWindowTest.kt          (20 tests)
│   ├── EditorComponentTest.kt     (15 tests)
│   ├── FileTreeComponentTest.kt   (10 tests)
│   ├── MenuBarTest.kt             (10 tests)
│   └── ToolbarTest.kt             (10 tests)
├── integration/
│   ├── FileOperationsTest.kt      (15 tests)
│   └── FormatDetectionTest.kt     (10 tests)
└── platform/
    ├── WindowsSpecificTest.kt     (3 tests)
    ├── MacOSSpecificTest.kt       (3 tests)
    └── LinuxSpecificTest.kt       (4 tests)
```

### Desktop Test Template

```kotlin
/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop UI Tests - Main Window
 *
 *########################################################*/
package digital.vasic.yole.desktop.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertNotNull

class MainWindowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should launch main window successfully`() {
        composeTestRule.setContent {
            MainWindow()
        }

        // Verify window elements
        composeTestRule.onNodeWithText("File").assertIsDisplayed()
        composeTestRule.onNodeWithText("Edit").assertIsDisplayed()
        composeTestRule.onNodeWithText("View").assertIsDisplayed()
    }

    @Test
    fun `should display file tree`() {
        composeTestRule.setContent {
            MainWindow()
        }

        composeTestRule.onNodeWithText("Files").assertIsDisplayed()
    }

    @Test
    fun `should display editor panel`() {
        composeTestRule.setContent {
            MainWindow()
        }

        composeTestRule.onNodeWithText("Editor").assertIsDisplayed()
    }

    // Add 17 more tests for main window functionality
}
```

### Desktop Test Implementation Steps

1. **Set up Compose Desktop Test Dependencies** in `desktopApp/build.gradle.kts`:
   ```kotlin
   dependencies {
       testImplementation("androidx.compose.ui:ui-test-junit4:1.7.3")
       testImplementation("org.jetbrains.kotlin:kotlin-test")
       testImplementation("junit:junit:4.13.2")
       testImplementation("org.assertj:assertj-core:3.26.3")
   }
   ```

2. **Create Test Files** using the structure above

3. **Write Tests** for:
   - Window lifecycle (open, close, resize)
   - Menu bar functionality
   - Toolbar actions
   - File tree navigation
   - Editor operations
   - Platform-specific features
   - Keyboard shortcuts
   - Drag-and-drop
   - Multi-window support

4. **Run Desktop Tests**:
   ```bash
   ./gradlew :desktopApp:test
   ```

---

## Part 3: Cross-Platform Integration Tests (50 tests)

### Integration Test Structure

Create tests in: `shared/src/commonTest/kotlin/digital/vasic/yole/integration/`

```
shared/src/commonTest/kotlin/digital/vasic/yole/integration/
├── FormatPipelineTest.kt        (10 tests)
├── ParserRegistryTest.kt        (10 tests)
├── FileOperationsTest.kt        (10 tests)
├── CrossFormatTest.kt           (10 tests)
└── PerformanceTest.kt           (10 tests)
```

### Integration Test Examples

#### Format Pipeline Integration
```kotlin
@Test
fun `should detect format and parse correctly`() {
    // Test complete pipeline: file → format detection → parser → HTML
    val filename = "document.md"
    val content = "# Hello World"

    // 1. Detect format
    val format = FormatRegistry.detectByFilename(filename)
    assertNotNull(format)
    assertEquals(TextFormat.ID_MARKDOWN, format.id)

    // 2. Get parser
    val parser = ParserRegistry.getParser(format)
    assertNotNull(parser)

    // 3. Parse content
    val document = parser.parse(content)
    assertNotNull(document)

    // 4. Convert to HTML
    val html = parser.toHtml(document)
    assertThat(html).contains("<h1>")
}
```

#### Cross-Format Testing
```kotlin
@Test
fun `should handle format detection ambiguity`() {
    // Test formats that share extensions (.txt)
    val plaintextContent = "Just plain text"
    val todotxtContent = "(A) Priority task @context +project"

    // Plain text should not match todo.txt patterns
    val plainFormat = FormatRegistry.detectByContent(plaintextContent)
    assertNull(plainFormat) // No patterns match

    // Todo.txt should match specific patterns
    val todoFormat = FormatRegistry.detectByContent(todotxtContent)
    assertNotNull(todoFormat)
    assertEquals(TextFormat.ID_TODOTXT, todoFormat.id)
}
```

#### Performance Integration
```kotlin
@Test
fun `should parse large files efficiently`() {
    val largeContent = buildString {
        repeat(10000) {
            appendLine("# Header $it")
            appendLine("Paragraph content for section $it")
        }
    }

    val parser = MarkdownParser()
    val startTime = System.currentTimeMillis()

    val result = parser.parse(largeContent)

    val duration = System.currentTimeMillis() - startTime

    assertNotNull(result)
    assertThat(duration).isLessThan(5000) // 5 seconds max
}
```

---

## Part 4: Running and Verifying Tests

### Run All Tests
```bash
# Run complete test suite
./run_all_tests.sh

# Or run individually
./gradlew :shared:testDebugUnitTest          # Shared module tests
./gradlew :androidApp:testDebugUnitTest      # Android tests
./gradlew :desktopApp:test                   # Desktop tests
```

### Generate Coverage Reports
```bash
# Generate HTML coverage report
./gradlew koverHtmlReport

# View report
open build/reports/kover/html/index.html

# Generate XML for CI
./gradlew koverXmlReport
```

### Verify Coverage Targets
```bash
# Check coverage meets thresholds
./gradlew koverVerify
```

**Target Coverage by Module**:
| Module | Current | Target | Gap |
|--------|---------|--------|-----|
| shared | ~20% | >90% | +70% |
| androidApp | ~10% | >70% | +60% |
| desktopApp | ~2% | >70% | +68% |
| **Overall** | **~15%** | **>80%** | **+65%** |

---

## Part 5: Test Quality Checklist

For each test file, ensure:

### Code Quality
- [ ] All tests follow AAA pattern (Arrange-Act-Assert)
- [ ] Test names are descriptive with backticks
- [ ] No test depends on execution order
- [ ] No shared mutable state between tests
- [ ] Proper cleanup in @After methods
- [ ] No hardcoded paths or platform-specific assumptions

### Coverage
- [ ] All public methods tested
- [ ] All code paths covered (branches, loops)
- [ ] Edge cases tested (empty, null, max values)
- [ ] Error conditions tested
- [ ] Concurrent access tested where applicable

### Assertions
- [ ] Use AssertJ fluent assertions
- [ ] One logical concept per test
- [ ] Clear failure messages
- [ ] Verify both positive and negative cases

### Documentation
- [ ] File header with SPDX license
- [ ] Class-level KDoc describing test scope
- [ ] Complex tests have explanatory comments
- [ ] Test sections organized with comment headers

---

## Part 6: Automation and CI Integration

### Pre-Commit Hooks

Add `.git/hooks/pre-commit`:
```bash
#!/bin/bash
# Run tests before commit
./gradlew test --quiet || exit 1
```

### GitHub Actions

Tests run automatically on:
- Every push to master/main/develop
- Every pull request
- Manual workflow dispatch

See `.github/workflows/test-and-coverage.yml`

### Codecov Integration

Coverage automatically uploaded to Codecov:
```yaml
- name: Upload coverage
  uses: codecov/codecov-action@v4
  with:
    files: ./build/reports/kover/report.xml
```

---

## Part 7: Quick Reference Commands

### Test Generation
```bash
# Generate parser tests
./scripts/generate_format_tests.sh <FormatName> <extension> --package <pkg>

# Example
./scripts/generate_format_tests.sh Markdown .md --package markdown
```

### Test Execution
```bash
# Run specific test class
./gradlew test --tests "*MarkdownParserTest"

# Run specific test method
./gradlew test --tests "*MarkdownParserTest.should parse headers correctly"

# Run all parser tests
./gradlew test --tests "*.format.*ParserTest"

# Run with debug output
./gradlew test --info --stacktrace
```

### Coverage Commands
```bash
# Generate and view coverage
./gradlew test koverHtmlReport && open build/reports/kover/html/index.html

# Check coverage thresholds
./gradlew koverVerify

# Generate XML for CI
./gradlew koverXmlReport
```

---

## Part 8: Troubleshooting

### Tests Not Found
**Problem**: Gradle doesn't find tests

**Solution**:
```bash
# Clean and rebuild
./gradlew clean test --rerun-tasks

# Verify test file location
ls -la shared/src/commonTest/kotlin/
```

### Coverage Report Empty
**Problem**: Coverage shows 0%

**Solution**:
```bash
# Ensure tests run first
./gradlew clean test koverHtmlReport

# Check Kover configuration in build.gradle.kts
```

### Parser Tests Failing
**Problem**: Generated tests fail with placeholder content

**Solution**:
1. Replace placeholder content with real format samples
2. Update assertions to match actual parser output
3. Check parser implementation for format-specific behavior

### Slow Test Execution
**Problem**: Tests take too long

**Solution**:
```bash
# Run tests in parallel
./gradlew test --parallel --max-workers=4

# Run only changed tests
./gradlew test --rerun-tasks
```

---

## Part 9: Completion Checklist

Use this checklist to track progress:

### FormatRegistry & Core (✅ Complete)
- [x] FormatRegistry tests (72 tests)
- [x] TextFormat tests (54 tests)
- [x] 99% coverage achieved

### Parser Tests (⏳ In Progress - 0/540)
High Priority:
- [ ] Markdown (30 tests)
- [ ] Todo.txt (30 tests)
- [ ] Plain Text (30 tests)
- [ ] CSV (30 tests)

Medium Priority:
- [ ] LaTeX (30 tests)
- [ ] Org Mode (30 tests)
- [ ] WikiText (30 tests)
- [ ] AsciiDoc (30 tests)
- [ ] reStructuredText (30 tests)

Low Priority:
- [ ] Key-Value (30 tests)
- [ ] TaskPaper (30 tests)
- [ ] Textile (30 tests)
- [ ] Creole (30 tests)
- [ ] TiddlyWiki (30 tests)
- [ ] Jupyter (30 tests)
- [ ] R Markdown (30 tests)
- [ ] Binary (30 tests)

### Android UI Tests (✅ Complete)
- [x] YoleAppTest (50+ tests existing)
- [x] 50+ comprehensive UI tests

### Desktop UI Tests (⏸️ Pending - 0/100)
- [ ] MainWindowTest (20 tests)
- [ ] EditorComponentTest (15 tests)
- [ ] FileTreeComponentTest (10 tests)
- [ ] MenuBarTest (10 tests)
- [ ] ToolbarTest (10 tests)
- [ ] FileOperationsTest (15 tests)
- [ ] FormatDetectionTest (10 tests)
- [ ] Platform-specific tests (10 tests)

### Integration Tests (⏸️ Pending - 0/50)
- [ ] FormatPipelineTest (10 tests)
- [ ] ParserRegistryTest (10 tests)
- [ ] FileOperationsTest (10 tests)
- [ ] CrossFormatTest (10 tests)
- [ ] PerformanceTest (10 tests)

### Coverage Targets
- [ ] Shared module >90%
- [ ] AndroidApp >70%
- [ ] DesktopApp >70%
- [ ] Overall project >80%

---

## Summary

**Phase 2 Progress**: 176/920 tests (19%)

**Next Steps**:
1. Start with Markdown parser (highest priority, most complex)
2. Customize placeholder content with real samples
3. Add format-specific tests (10+ per format)
4. Run tests and achieve 90% coverage
5. Move to next format (Todo.txt, then CSV, then Plain Text)
6. Complete all high-priority parsers before medium/low
7. Create Desktop UI test infrastructure
8. Write Desktop UI tests (100 tests)
9. Create cross-platform integration tests (50 tests)
10. Verify >80% overall coverage

**Estimated Time**:
- Parser tests: 3-4 weeks (540 tests)
- Desktop tests: 1 week (100 tests)
- Integration tests: 3-4 days (50 tests)
- **Total**: 4-5 weeks

**Resources**:
- Test templates: `templates/tests/`
- Test generator: `scripts/generate_format_tests.sh`
- Documentation: `docs/TESTING_GUIDE.md`
- Progress tracking: `docs/PHASE_2_PROGRESS.md`

---

*Last Updated: November 2025*
*Yole Test Implementation Guide v1.0*
