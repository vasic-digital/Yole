# Contributing to Yole

Thank you for your interest in contributing to Yole! This document provides guidelines for contributing to this cross-platform text editor project.

## Getting Started

### Prerequisites
- **Android Studio** latest stable version (for Android development)
- **Xcode** (for iOS development)
- **IntelliJ IDEA** or similar (for Desktop/Web development)
- **Git** for version control
- **Kotlin** 1.9+ (managed by Gradle)
- **Android SDK** 21-35 (API levels)
- **Java** 8+ compatibility

### Setup
1. Fork the repository on GitHub
2. Clone your fork locally:
    ```bash
    git clone https://github.com/yourusername/yole.git
    cd yole
    ```
3. Open the project in your preferred IDE
4. Let Gradle sync and download dependencies
5. Build the project to verify setup:
    ```bash
    ./gradlew assembleDebug  # For Android
    ./gradlew desktopApp:run  # For Desktop
    ./gradlew webApp:wasmJsBrowserRun  # For Web
    ```

## Development Workflow

### Branch Strategy
- `main`: Stable release branch
- `develop`: Development branch (if exists)
- `feature/*`: Feature-specific branches
- `bugfix/*`: Bug fix branches
- `hotfix/*`: Critical fixes for releases

### Code Style Guidelines

#### Kotlin Code Style
- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use **camelCase** for variables and functions
- Use **PascalCase** for classes and objects
- Use **UPPER_SNAKE_CASE** for constants
- Keep lines under 120 characters when possible

#### Import Organization
1. Standard Android imports
2. AndroidX imports  
3. Third-party library imports
4. Project imports (`digital.vasic.yole.*`, `net.gsantner.opoc.*`)

#### File Headers
All source files must include SPDX license header:
```kotlin
/*
 * SPDX-FileCopyrightText: 2017-2025 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-FileCopyrightText: 2025 Your Name <your@email.com>
 * SPDX-License-Identifier: Apache 2.0
 */
```

### Testing

#### Running Tests
```bash
# Run all tests
./gradlew testFlavorDefaultDebugUnitTest

# Run specific test
./gradlew testFlavorDefaultDebugUnitTest --tests "digital.vasic.yole.format.todotxt.TodoTxtQuerySyntaxTests.ParseQuery"

# Run tests for specific module
./gradlew :format-markdown:testDebugUnitTest
```

#### Writing Tests
- Use **JUnit 4** with **AssertJ** assertions
- Test class names should end with `Tests` or `Test`
- Write unit tests for business logic in shared modules
- Write instrumented tests for Android-specific code
- Aim for high test coverage on core functionality

### Build Configuration

#### Gradle Build Files
**IMPORTANT:** This project uses **Kotlin DSL only** for Gradle build configuration.

- ✅ Use `build.gradle.kts` (Kotlin DSL)
- ❌ Do NOT use `build.gradle` (Groovy DSL)
- All modules must use `.gradle.kts` files
- Legacy Groovy build files have been removed

**Rationale:** Kotlin DSL provides:
- Type-safe build scripts
- Better IDE support
- Kotlin language features
- Consistency with project language

### Building and Linting

#### Build Commands
```bash
# Build debug APK
./gradlew assembleFlavorDefaultDebug

# Build release APK
./gradlew assembleFlavorDefaultRelease

# Run lint checks
./gradlew lintFlavorDefaultDebug

# Clean build
./gradlew clean
```

#### Before Submitting
1. **Run tests**: Ensure all tests pass
2. **Run lint**: Fix any lint warnings/errors
3. **Format code**: Use Android Studio's auto-reformat
4. **Build successfully**: Verify project builds without errors
5. **Test manually**: Test your changes on device/emulator

## Project Structure

### Module Architecture

**Yole uses Kotlin Multiplatform (KMP)** for cross-platform development:

```
├── shared/                 # Shared KMP code (PRIMARY MODULE)
│   ├── src/
│   │   ├── commonMain/     # Common Kotlin code
│   │   │   └── kotlin/digital/vasic/yole/
│   │   │       ├── format/          # Format system
│   │   │       │   ├── FormatRegistry.kt
│   │   │       │   ├── TextParser.kt
│   │   │       │   ├── markdown/MarkdownParser.kt
│   │   │       │   ├── todotxt/TodoTxtParser.kt
│   │   │       │   └── ... (all 18 format parsers)
│   │   │       └── model/           # Document model
│   │   ├── androidMain/    # Android-specific implementations
│   │   ├── desktopMain/    # Desktop-specific implementations
│   │   ├── iosMain/        # iOS-specific implementations
│   │   └── wasmJsMain/     # Web-specific implementations
│
├── androidApp/             # Android application (Compose UI)
├── desktopApp/             # Desktop application (Compose Desktop)
├── iosApp/                 # iOS application (SwiftUI + KMP)
├── webApp/                 # Web application (Compose for Web)
│
├── commons/                # Android-specific utilities
└── core/                   # Legacy encryption utilities
```

### Package Structure
- `digital.vasic.yole.format.*`: Format system (parsers, registry)
- `digital.vasic.yole.model.*`: Document model and data structures
- `net.gsantner.opoc.*`: Shared utility libraries (legacy)

## Documentation Standards

### KDoc Comments

All public APIs must have KDoc documentation:

```kotlin
/**
 * Parses text content into a structured document.
 *
 * This parser supports the following features:
 * - Feature 1 description
 * - Feature 2 description
 *
 * @param content The raw text content to parse
 * @param options Optional parsing configuration (default: empty map)
 * @return Parsed document with HTML representation and metadata
 * @throws IllegalArgumentException if content is empty
 *
 * @see TextParser for the common parser interface
 * @see FormatRegistry for format registration
 *
 * @sample digital.vasic.yole.format.markdown.MarkdownParserTest.basicParsing
 */
fun parse(content: String, options: Map<String, Any> = emptyMap()): ParsedDocument {
    // Implementation
}
```

**KDoc Guidelines:**
- **Purpose**: First sentence describes what the function/class does
- **Details**: Additional paragraphs explain behavior, features, edge cases
- **@param**: Document all parameters with clear descriptions
- **@return**: Describe return value and its structure
- **@throws**: Document exceptions that may be thrown
- **@see**: Link to related APIs
- **@sample**: Reference example code when helpful
- **Code examples**: Include usage examples for complex APIs

### Commit Messages

Follow conventional commit format:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code formatting (no logic change)
- `refactor`: Code refactoring
- `test`: Adding/updating tests
- `chore`: Build process, dependencies

**Examples:**
```
feat(markdown): add support for footnotes

Implement GFM footnote syntax [^1] for Markdown parser.
Includes rendering in HTML preview and syntax highlighting.

Closes #123

---

fix(todotxt): correct priority parsing for tasks without dates

Priority was incorrectly parsed when creation date was missing.
Now handles all valid todo.txt priority formats.

Fixes #456

---

docs(api): add KDoc to FormatRegistry methods

Complete documentation coverage for format registration,
detection, and retrieval methods.

Part of Phase 3 documentation initiative.
```

**Guidelines:**
- **Subject line**: 50 characters or less, imperative mood
- **Body**: Wrap at 72 characters, explain what and why (not how)
- **Footer**: Reference issues/PRs (`Closes #123`, `Fixes #456`, `Part of #789`)
- **Scope**: Module or component name (markdown, todotxt, ui, etc.)

## Adding New Format Parsers

### Step-by-Step Guide

**1. Create Parser Class** in `shared/src/commonMain/kotlin/digital/vasic/yole/format/[formatname]/`

```kotlin
package digital.vasic.yole.format.myformat

import digital.vasic.yole.format.ParsedDocument
import digital.vasic.yole.format.TextParser

/**
 * Parser for MyFormat text files.
 *
 * MyFormat is a lightweight markup language for [purpose].
 * Supports: [list key features]
 *
 * @see <a href="https://myformat.org/spec">MyFormat Specification</a>
 */
class MyFormatParser : TextParser {
    override val formatId: String = "myformat"
    override val displayName: String = "MyFormat"
    override val fileExtensions: List<String> = listOf(".myf", ".myformat")

    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        // 1. Detect format (optional validation)
        // 2. Parse syntax into structure
        // 3. Convert to HTML
        // 4. Extract metadata

        return ParsedDocument(
            html = convertToHtml(content),
            metadata = extractMetadata(content),
            sourceFormat = formatId
        )
    }

    private fun convertToHtml(content: String): String {
        // Implement conversion logic
    }

    private fun extractMetadata(content: String): Map<String, Any> {
        // Extract title, author, tags, etc.
    }
}
```

**2. Register Format** in `FormatRegistry.kt`:

```kotlin
init {
    registerFormat(
        TextFormat(
            id = "myformat",
            name = "MyFormat",
            extensions = listOf(".myf", ".myformat"),
            mimeType = "text/x-myformat",
            parser = { MyFormatParser() }
        )
    )
}
```

**3. Write Tests** in `shared/src/commonTest/kotlin/digital/vasic/yole/format/myformat/`

```kotlin
class MyFormatParserTest {
    private val parser = MyFormatParser()

    @Test
    fun `parse basic syntax`() {
        val content = """
            # Header
            Body text
        """.trimIndent()

        val result = parser.parse(content)

        assertThat(result.html).contains("<h1>Header</h1>")
        assertThat(result.html).contains("<p>Body text</p>")
    }

    @Test
    fun `detect format by extension`() {
        assertThat(parser.fileExtensions).contains(".myf")
    }
}
```

**4. Create User Documentation** in `docs/user-guide/formats/myformat.md`

Include:
- Format overview and use cases
- Complete syntax reference with examples
- Yole-specific features and limitations
- Best practices
- External resources

**5. Update Format Index** in `docs/user-guide/formats/README.md`

Add your format to the list with description and link.

### Testing Requirements

- ✅ Basic syntax parsing test
- ✅ HTML conversion test
- ✅ Metadata extraction test
- ✅ Edge case handling (empty content, malformed syntax)
- ✅ Format detection by extension
- ✅ Integration with FormatRegistry

### Reference Implementations

**Simple formats**: Look at `PlaintextParser.kt` for minimal implementation
**Complex formats**: Study `MarkdownParser.kt` for full-featured parser
**Structured data**: Check `CsvParser.kt` for table handling

## Contributing Types

### Bug Reports
1. Search existing issues first
2. Use the bug report template
3. Include device info, Android version, and steps to reproduce
4. Provide screenshots if applicable

### Feature Requests
1. Search existing discussions and issues
2. Use the feature request template
3. Describe the use case and expected behavior
4. Consider if it fits the project's scope

### Code Contributions
1. Fork and create a feature branch
2. Write clean, tested code following guidelines
3. Update documentation if needed
4. Submit a pull request with clear description
5. Respond to code review feedback

### Documentation
- Improve existing documentation
- Add examples for new features
- Update README and guides
- Fix typos and clarify unclear sections

### Translations
- Contribute via [Crowdin](https://crowdin.com/project/markor)
- Report translation issues in GitHub issues
- Help maintain language quality

## Pull Request Process

### Before Submitting
1. **Rebase** your branch on latest `main`
2. **Test thoroughly** on multiple devices if possible
3. **Update documentation** for any user-facing changes
4. **Add tests** for new functionality
5. **Run full test suite** and fix any failures

### Pull Request Template
```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] Manual testing completed
- [ ] Lint checks pass

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] Tests added/updated
```

### Code Review
- Be responsive to reviewer feedback
- Explain complex changes clearly
- Address all review comments
- Keep PRs focused and reasonably sized

## Community Guidelines

### Communication
- Be respectful and constructive
- Welcome newcomers and help them learn
- Focus on what is best for the project
- Show empathy towards other community members

### Code Review Etiquette
- Provide specific, actionable feedback
- Explain the reasoning behind suggestions
- Acknowledge good work and improvements
- Be patient with different perspectives

## Getting Help

### Resources
- [AGENTS.md](AGENTS.md): Development guide for agents
- [ARCHITECTURE.md](ARCHITECTURE.md): Project architecture
- [README.md](README.md): General project information
- [GitHub Discussions](https://github.com/vasic-digital/Yole/discussions): Community discussions

### Contact
- Create a GitHub issue for bugs and feature requests
- Start a discussion for questions and ideas
- Check existing issues and discussions before posting

## License

By contributing to Yole, you agree that your contributions will be licensed under the same license as the project (Apache 2.0 for code, CC0 1.0 for translations and samples).

---

Thank you for contributing to Yole! Your help makes this project better for everyone. 🎉