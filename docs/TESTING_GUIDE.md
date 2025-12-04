# Yole Testing Guide

**Comprehensive guide to testing practices, frameworks, and templates in the Yole project.**

## Table of Contents

- [Overview](#overview)
- [Test Infrastructure](#test-infrastructure)
- [Test Types](#test-types)
- [Test Templates](#test-templates)
- [Writing Tests](#writing-tests)
- [Running Tests](#running-tests)
- [Code Coverage](#code-coverage)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

---

## Overview

Yole uses a comprehensive testing strategy with multiple test types and frameworks to ensure code quality and reliability across all platforms (Android, Desktop, iOS, Web).

### Testing Goals

- **Target Coverage**: >80% code coverage (current: ~15%)
- **Test Types**: Unit, Integration, UI, Property-based, Snapshot
- **Frameworks**: JUnit 4, AssertJ, MockK, Kotest, Espresso, Compose UI Test
- **Automation**: CI/CD integration with automated test runs

### Project Testing Structure

```
Yole/
├── shared/                          # Kotlin Multiplatform shared code
│   ├── src/
│   │   ├── commonMain/              # Production code
│   │   └── commonTest/              # Shared tests
│   │       └── kotlin/digital/vasic/yole/format/
│   │           ├── markdown/        # Markdown format tests
│   │           ├── todotxt/         # Todo.txt format tests
│   │           └── ...              # Other format tests
├── androidApp/src/androidTest/      # Android UI tests
├── templates/tests/                 # Test templates for generation
└── scripts/generate_format_tests.sh # Test generation script
```

---

## Test Infrastructure

### Test Frameworks and Libraries

| Framework | Version | Purpose | Documentation |
|-----------|---------|---------|---------------|
| **JUnit 4** | 4.13.2 | Unit test runner | [junit.org](https://junit.org/junit4/) |
| **AssertJ** | 3.26.3 | Fluent assertions | [assertj.github.io](https://assertj.github.io/doc/) |
| **MockK** | 1.13.13 | Mocking for Kotlin | [mockk.io](https://mockk.io/) |
| **Kotest** | 5.9.1 | Property-based testing | [kotest.io](https://kotest.io/) |
| **Espresso** | 3.5.0 | Android UI testing | [developer.android.com](https://developer.android.com/training/testing/espresso) |
| **Compose UI Test** | 1.7.3 | Compose UI testing | [developer.android.com](https://developer.android.com/jetpack/compose/testing) |
| **Turbine** | 1.2.0 | Flow testing | [github.com/cashapp/turbine](https://github.com/cashapp/turbine) |

### Dependencies Configuration

Tests are configured in `shared/build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.assertj:assertj-core:3.26.3")
                implementation("io.kotest:kotest-property:5.9.1")
                implementation("io.mockk:mockk:1.13.13")
            }
        }
    }
}
```

### Code Coverage with Kover

Kover 0.8.3 is configured for comprehensive code coverage reporting:

```bash
# Generate HTML coverage report
./gradlew koverHtmlReport

# Open report (after generation)
open build/reports/kover/html/index.html

# Generate XML report (for CI)
./gradlew koverXmlReport

# Verify coverage thresholds
./gradlew koverVerify
```

**Coverage Thresholds** (configured in `build.gradle.kts`):
- Minimum: 50% (warning)
- Target: 80% (goal)

---

## Test Types

### 1. Unit Tests

**Purpose**: Test individual classes and functions in isolation.

**Location**: `shared/src/commonTest/kotlin/`

**Naming Convention**: `[ClassName]Test.kt`

**Example**:
```kotlin
class FormatRegistryTest {
    @Test
    fun `getById should return correct format`() {
        val format = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)

        assertNotNull(format)
        assertThat(format.name).isEqualTo("Markdown")
    }
}
```

**When to Use**:
- Testing parser logic
- Format detection algorithms
- Utility functions
- Business logic

### 2. Integration Tests

**Purpose**: Test interaction between multiple components.

**Location**: `shared/src/commonTest/kotlin/`

**Naming Convention**: `[Feature]IntegrationTest.kt`

**Example**:
```kotlin
class MarkdownIntegrationTest {
    @Test
    fun `format detection should work with parser`() {
        val format = FormatRegistry.detectByFilename("test.md")
        assertNotNull(format)

        val parser = MarkdownParser()
        val result = parser.parse("# Hello World")
        assertNotNull(result)
    }
}
```

**When to Use**:
- Testing format detection → parsing pipeline
- Parser → HTML converter interaction
- Cross-module functionality
- End-to-end workflows

### 3. Property-Based Tests

**Purpose**: Generate hundreds of random test cases to find edge cases.

**Framework**: Kotest

**Location**: `shared/src/commonTest/kotlin/`

**Naming Convention**: `[ClassName]PropertyTest.kt`

**Example**:
```kotlin
class MarkdownPropertyTest : StringSpec({
    "parsing any valid markdown should not throw" {
        checkAll(validMarkdownContent()) { content ->
            val parser = MarkdownParser()
            val result = runCatching { parser.parse(content) }
            result.isSuccess shouldBe true
        }
    }
})

fun validMarkdownContent(): Arb<String> = arbitrary {
    "# Header\n\nParagraph text."
}
```

**When to Use**:
- Testing parsers with wide input variety
- Validating invariants (e.g., parse → serialize → parse)
- Discovering unexpected edge cases
- Performance characteristics

### 4. MockK Mocking Tests

**Purpose**: Test components with mocked dependencies.

**Framework**: MockK

**Location**: `shared/src/commonTest/kotlin/`

**Example**:
```kotlin
class FileProcessorTest {
    private lateinit var mockFileSystem: FileSystem

    @Before
    fun setup() {
        mockFileSystem = mockk<FileSystem>()
    }

    @Test
    fun `should read file content`() {
        every { mockFileSystem.readFile("test.md") } returns "# Hello"

        val processor = FileProcessor(mockFileSystem)
        val content = processor.process("test.md")

        assertThat(content).contains("Hello")
        verify { mockFileSystem.readFile("test.md") }
    }
}
```

**When to Use**:
- Testing with external dependencies (file system, network)
- Isolating unit under test
- Testing error handling with exceptions
- Verifying method call sequences

### 5. Android UI Tests

**Purpose**: Test Android-specific UI components.

**Framework**: Espresso + Compose UI Test

**Location**: `androidApp/src/androidTest/kotlin/`

**Naming Convention**: `[Screen]UITest.kt`

**Example**:
```kotlin
@RunWith(AndroidJUnit4::class)
class MainScreenUITest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testCreateNewDocument() {
        composeTestRule.setContent {
            MainScreen()
        }

        composeTestRule.onNodeWithTag("fab_new_document").performClick()
        composeTestRule.onNodeWithTag("dialog_new_document").assertIsDisplayed()
    }
}
```

**When to Use**:
- Testing UI interactions
- Verifying screen navigation
- Validating UI state changes
- Testing accessibility

### 6. Snapshot Tests

**Purpose**: Detect unintended UI or output changes.

**Status**: Planned (not yet implemented)

**When to Use**:
- UI regression testing
- Parser output validation
- HTML converter output

---

## Test Templates

Yole provides reusable test templates to accelerate test creation. All templates are located in `templates/tests/`.

### Available Templates

| Template | Purpose | Test Count | Framework |
|----------|---------|------------|-----------|
| **ParserTest.kt.template** | Unit tests for format parsers | 15 tests | JUnit 4 + AssertJ |
| **IntegrationTest.kt.template** | Integration tests for format pipeline | 12 tests | JUnit 4 + AssertJ |
| **MockKExample.kt.template** | MockK usage examples | 14 tests | JUnit 4 + MockK |
| **KotestPropertyTest.kt.template** | Property-based tests | 13 tests | Kotest |

### Template Variables

All templates use placeholder variables that are replaced during generation:

| Variable | Description | Example |
|----------|-------------|---------|
| `{{FORMAT_NAME}}` | Human-readable format name | `Markdown` |
| `{{FORMAT_CLASS}}` | Class name prefix (PascalCase) | `Markdown` |
| `{{FORMAT_PACKAGE}}` | Package name (lowercase) | `markdown` |
| `{{FORMAT_LOWERCASE}}` | Format name in lowercase | `markdown` |
| `{{FORMAT_ID}}` | Format ID constant | `MARKDOWN` |
| `{{PRIMARY_EXTENSION}}` | Primary file extension | `.md` |
| `{{EXTENSIONS_LIST}}` | Quoted extension list | `".md", ".markdown"` |
| `{{SAMPLE_CONTENT}}` | Multi-line sample content | `# Hello\n\nWorld` |
| `{{SINGLE_LINE_SAMPLE}}` | Single-line sample | `# Hello` |
| `{{SPECIAL_CHARS_SAMPLE}}` | Special characters sample | `@#$%^&*()` |
| `{{MALFORMED_SAMPLE}}` | Malformed content | `# Incomplete` |

### Generating Tests from Templates

Use the `generate_format_tests.sh` script to create tests:

```bash
# Generate all tests for a format
./scripts/generate_format_tests.sh markdown .md

# Generate specific test types
./scripts/generate_format_tests.sh "Org Mode" .org \
    --templates ParserTest,IntegrationTest

# Preview generation without creating files
./scripts/generate_format_tests.sh latex .tex --dry-run

# Custom package and class names
./scripts/generate_format_tests.sh "R Markdown" .rmd \
    --package rmarkdown \
    --class RMarkdown
```

**Script Options**:

```
--package NAME       Package name (default: format-name in lowercase)
--class NAME         Class name (default: format-name in PascalCase)
--format-id ID       Format ID constant (default: UPPERCASE_SNAKE_CASE)
--extensions LIST    Comma-separated list of extensions
--output-dir DIR     Output directory
--templates LIST     Templates to generate (default: all)
--dry-run            Preview without creating files
```

---

## Writing Tests

### Test Structure (AAA Pattern)

Follow the **Arrange-Act-Assert** pattern:

```kotlin
@Test
fun `should parse markdown header`() {
    // Arrange: Set up test data and dependencies
    val parser = MarkdownParser()
    val content = "# Hello World"

    // Act: Execute the code under test
    val result = parser.parse(content)

    // Assert: Verify expected outcomes
    assertNotNull(result)
    assertThat(result.hasHeader()).isTrue()
    assertThat(result.getHeaderText()).isEqualTo("Hello World")
}
```

### Naming Conventions

Use descriptive test names with backticks:

```kotlin
// ✅ Good: Describes what and expected outcome
@Test
fun `should detect markdown format by md extension`()

@Test
fun `should throw ParserException when content is malformed`()

@Test
fun `should preserve unicode characters during parsing`()

// ❌ Bad: Vague or implementation-focused
@Test
fun testParse()

@Test
fun testMarkdownParser()
```

### Assertion Library (AssertJ)

Use AssertJ for fluent, readable assertions:

```kotlin
// String assertions
assertThat(result).isEqualTo("expected")
assertThat(result).contains("substring")
assertThat(result).startsWith("prefix")
assertThat(result).matches("regex.*pattern")

// Collection assertions
assertThat(list).hasSize(3)
assertThat(list).contains("item1", "item2")
assertThat(list).containsExactly("a", "b", "c")
assertThat(list).isEmpty()

// Boolean assertions
assertThat(condition).isTrue()
assertThat(condition).isFalse()

// Null assertions
assertThat(value).isNotNull()
assertThat(value).isNull()

// Exception assertions
assertThatThrownBy { parser.parse(malformed) }
    .isInstanceOf(ParserException::class.java)
    .hasMessageContaining("invalid syntax")
```

### MockK Usage Patterns

#### Basic Mocking

```kotlin
val mockFileSystem = mockk<FileSystem>()

every { mockFileSystem.readFile("test.txt") } returns "content"
every { mockFileSystem.writeFile(any(), any()) } just Runs
```

#### Verification

```kotlin
verify { mockFileSystem.readFile("test.txt") }
verify(exactly = 3) { mockFileSystem.readFile(any()) }
verify(atLeast = 1) { mockFileSystem.writeFile(any(), any()) }
confirmVerified(mockFileSystem)
```

#### Argument Matching

```kotlin
every { mock.method(any()) } returns "result"
every { mock.method(match { it > 10 }) } returns "big"
every { mock.method(eq("exact")) } returns "exact match"
```

#### Argument Capture

```kotlin
val slot = slot<String>()
every { mock.writeFile("test.txt", capture(slot)) } just Runs

mock.writeFile("test.txt", "captured content")

assertThat(slot.captured).isEqualTo("captured content")
```

#### Relaxed Mocks

```kotlin
val relaxedMock = mockk<FileSystem>(relaxed = true)

// No need to stub - returns default values
relaxedMock.readFile("any.txt") // returns ""
```

### Property-Based Testing Patterns

#### Basic Property

```kotlin
"property description" {
    checkAll(generator) { input ->
        // Test property with input
        assertNotNull(parser.parse(input))
    }
}
```

#### Custom Generators

```kotlin
fun validMarkdownContent(): Arb<String> = arbitrary { rs ->
    val samples = listOf("# Header", "**bold**", "_italic_")
    samples.random(rs.random)
}
```

#### Multiple Inputs

```kotlin
checkAll(
    Arb.string(0..1000),
    Arb.int(1..100)
) { content, count ->
    // Test with both inputs
}
```

---

## Running Tests

### Command-Line Test Execution

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :shared:testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests "*.MarkdownParserTest"

# Run specific test method
./gradlew testDebugUnitTest --tests "*.MarkdownParserTest.should parse headers"

# Run with info logging
./gradlew test --info

# Run tests and generate coverage
./gradlew test koverHtmlReport
```

### Comprehensive Test Script

Use `run_all_tests.sh` for complete testing:

```bash
./run_all_tests.sh
```

This script:
- Runs all unit tests
- Runs integration tests
- Generates coverage reports
- Runs lint checks
- Outputs detailed results

### IDE Test Execution

**Android Studio / IntelliJ IDEA**:

1. **Run All Tests in File**: Right-click test file → Run
2. **Run Single Test**: Click green arrow next to `@Test`
3. **Debug Test**: Click debug icon next to test
4. **View Coverage**: Run → Run with Coverage

### CI/CD Integration

Tests run automatically on:
- Every commit (via GitHub Actions)
- Pull request creation
- Pre-merge validation

**GitHub Actions Configuration** (`.github/workflows/test.yml`):

```yaml
name: Run Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
      - name: Run tests
        run: ./gradlew test koverXmlReport
      - name: Upload coverage
        uses: codecov/codecov-action@v4
```

---

## Code Coverage

### Viewing Coverage Reports

```bash
# Generate HTML report
./gradlew koverHtmlReport

# View in browser
open build/reports/kover/html/index.html
```

### Coverage Metrics

**Current Status** (as of November 2025):
- **Overall Coverage**: ~15%
- **Goal**: >80%

**Per-Module Breakdown**:
| Module | Coverage | Status |
|--------|----------|--------|
| shared | 15% | 🔴 Low |
| androidApp | 5% | 🔴 Low |
| desktopApp | 2% | 🔴 Low |

### Improving Coverage

**Priority Areas** (from IMPLEMENTATION_PLAN.md):

1. **FormatRegistry** (shared module)
   - Target: 95% coverage
   - Tests needed: 30+

2. **Format Parsers** (all 18 formats)
   - Target: 90% coverage per format
   - Tests needed: 540+ total (30 per format)

3. **UI Components**
   - Target: 70% coverage
   - Tests needed: 200+

### Coverage Badges

Add coverage badge to README.md:

```markdown
![Code Coverage](https://img.shields.io/badge/coverage-15%25-red.svg)
```

Update badge color based on coverage:
- Red: 0-50%
- Yellow: 51-79%
- Green: 80-100%

---

## Best Practices

### General Principles

1. **Test Behavior, Not Implementation**
   ```kotlin
   // ✅ Good: Tests behavior
   @Test
   fun `should convert markdown to HTML`() {
       val html = converter.convert("**bold**")
       assertThat(html).contains("<strong>bold</strong>")
   }

   // ❌ Bad: Tests implementation details
   @Test
   fun `should call parseInline method`() {
       // Don't test internal method calls
   }
   ```

2. **One Assertion Per Logical Concept**
   ```kotlin
   // ✅ Good: Related assertions grouped
   @Test
   fun `should parse header with correct level and text`() {
       val result = parser.parse("## Hello")
       assertThat(result.level).isEqualTo(2)
       assertThat(result.text).isEqualTo("Hello")
   }

   // ❌ Bad: Unrelated assertions in one test
   @Test
   fun `should parse everything`() {
       // Tests too many things at once
   }
   ```

3. **Test Edge Cases and Boundaries**
   ```kotlin
   @Test fun `should handle empty input`()
   @Test fun `should handle single character`()
   @Test fun `should handle maximum length`()
   @Test fun `should handle null bytes`()
   @Test fun `should handle unicode`()
   ```

4. **Use Setup and Teardown Appropriately**
   ```kotlin
   class ParserTest {
       private lateinit var parser: MarkdownParser

       @Before
       fun setup() {
           parser = MarkdownParser()
       }

       @After
       fun tearDown() {
           // Clean up resources if needed
       }
   }
   ```

5. **Isolate Tests from Each Other**
   - Each test should be runnable independently
   - No shared mutable state between tests
   - Use `@Before` to reset state

### Format-Specific Testing

When testing format parsers:

1. **Test Format Detection**
   ```kotlin
   @Test
   fun `should detect format by extension`()

   @Test
   fun `should detect format by content patterns`()
   ```

2. **Test Core Syntax Elements**
   ```kotlin
   @Test
   fun `should parse headers`()

   @Test
   fun `should parse emphasis`()

   @Test
   fun `should parse lists`()
   ```

3. **Test Error Handling**
   ```kotlin
   @Test
   fun `should handle malformed syntax gracefully`()

   @Test
   fun `should provide meaningful error messages`()
   ```

4. **Test Unicode and Special Characters**
   ```kotlin
   @Test
   fun `should preserve unicode characters`()

   @Test
   fun `should handle line ending variations`()
   ```

### Performance Testing

For performance-critical code:

```kotlin
@Test
fun `should parse large file in reasonable time`() {
    val largeContent = generateLargeContent(10000)
    val startTime = System.currentTimeMillis()

    parser.parse(largeContent)

    val duration = System.currentTimeMillis() - startTime
    assertThat(duration).isLessThan(1000) // 1 second
}
```

### Testing Asynchronous Code

For Flow and coroutines, use Turbine:

```kotlin
@Test
fun `should emit parsing progress`() = runTest {
    parser.parseWithProgress(content).test {
        assertThat(awaitItem()).isEqualTo(0)
        assertThat(awaitItem()).isEqualTo(50)
        assertThat(awaitItem()).isEqualTo(100)
        awaitComplete()
    }
}
```

---

## Troubleshooting

### Common Issues

#### Test Not Found

**Problem**: `./gradlew test` doesn't find your test

**Solution**:
- Verify test is in correct directory (`src/commonTest/kotlin/`)
- Check test has `@Test` annotation
- Ensure class is public
- Run `./gradlew clean test`

#### MockK Not Working

**Problem**: `MockK could not initialize`

**Solution**:
- Add MockK dependency to `commonTest`
- Use `mockk<Type>()` not `mock()`
- Check you're using `every { }` not `when { }`

#### Coverage Report Empty

**Problem**: Coverage report shows no data

**Solution**:
```bash
# Ensure tests run first
./gradlew clean test koverHtmlReport
```

#### Test Fails on CI But Passes Locally

**Problem**: Flaky test or environment difference

**Solution**:
- Check for timing dependencies
- Verify test isolation (no shared state)
- Use `@Ignore` with JIRA ticket for known flaky tests
- Add detailed logging

#### OutOfMemoryError During Tests

**Problem**: Tests crash with OOM

**Solution**: Increase heap size in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m
```

### Debugging Tests

#### Enable Test Logging

In `build.gradle.kts`:

```kotlin
tasks.withType<Test> {
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
```

#### Debug Specific Test

```bash
# Run with stack traces
./gradlew test --info --stacktrace

# Run with debug logging
./gradlew test --debug
```

### Getting Help

- **Documentation**: [ARCHITECTURE.md](../ARCHITECTURE.md), [CONTRIBUTING.md](../CONTRIBUTING.md)
- **Issues**: [github.com/vasic-digital/Yole/issues](https://github.com/vasic-digital/Yole/issues)
- **Discussions**: [github.com/vasic-digital/Yole/discussions](https://github.com/vasic-digital/Yole/discussions)

---

## Summary

This guide covered:

✅ Test infrastructure and frameworks
✅ Six test types (unit, integration, property-based, mocking, UI, snapshot)
✅ Four reusable test templates
✅ Test generation automation
✅ Comprehensive testing patterns and examples
✅ Running tests and measuring coverage
✅ Best practices and troubleshooting

**Next Steps**:

1. Review existing test files in `shared/src/commonTest/`
2. Generate tests for untested formats using `generate_format_tests.sh`
3. Run full test suite: `./run_all_tests.sh`
4. Check coverage: `./gradlew koverHtmlReport`
5. Aim for >80% coverage incrementally

**Goal**: Comprehensive test coverage ensures Yole remains reliable, maintainable, and bug-free across all platforms.

---

*Last Updated: November 2025*
*Yole Testing Infrastructure v1.0*
