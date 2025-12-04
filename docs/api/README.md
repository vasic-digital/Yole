# Yole API Documentation

This directory contains the generated API documentation for the Yole project.

## Status

⚠️ **Documentation Not Yet Generated**

API documentation needs to be generated from the source code.

## How to Generate Documentation

### Prerequisites
- JDK 11 or later
- Gradle (included via wrapper)

### Generate Command

```bash
# From project root directory
./gradlew :shared:dokkaHtml
```

### Publish to This Directory

```bash
# Copy generated docs to website
mkdir -p docs/api
cp -r shared/build/dokka/html/* docs/api/
```

## What's Documented

The API documentation includes:

### Core Classes

**FormatRegistry** (`shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt`)
- Central registry for all 18+ supported text formats
- Format detection by extension, content, or filename
- Comprehensive KDoc with examples
- **390 lines** of well-documented code

**TextFormat** (`shared/src/commonMain/kotlin/digital/vasic/yole/format/TextFormat.kt`)
- Data class representing a text format
- Properties: id, name, extensions, detection patterns
- Complete KDoc documentation with usage examples

**Format Parsers** (`shared/src/commonMain/kotlin/digital/vasic/yole/format/[format]/`)
- MarkdownParser
- TodoTxtParser
- LatexParser
- AsciidocParser
- OrgModeParser
- And 11 more parsers...

### Kotlin Multiplatform Structure

The `shared` module contains all cross-platform business logic:
- `commonMain/` - Code that runs on all platforms
- `androidMain/` - Android-specific implementations
- `desktopMain/` - Desktop-specific implementations
- `iosMain/` - iOS-specific implementations (currently disabled)
- `wasmJsMain/` - Web-specific implementations

## Viewing Documentation

Once generated, open:
```bash
open docs/api/index.html
```

Or visit the published documentation at:
- **Local:** `file:///path/to/Yole/docs/api/index.html`
- **GitHub Pages:** https://vasic-digital.github.io/Yole/api/ (when published)

## Documentation Quality

✅ **FormatRegistry**: Comprehensive KDoc with examples
✅ **TextFormat**: Complete property documentation
⏳ **Parsers**: Need KDoc enhancement

## Quick Links

Once generated, you'll be able to browse:
- All Classes
- All Packages
- Search functionality
- Cross-references

## For Contributors

When adding new classes or modifying existing ones:
1. Add KDoc comments to public APIs
2. Include `@param`, `@return`, `@throws` documentation
3. Add usage examples in `@example` blocks
4. Regenerate documentation: `./gradlew :shared:dokkaHtml`

Example KDoc format:
```kotlin
/**
 * Short description of what this does.
 *
 * Longer description with more details.
 *
 * @param paramName Description of parameter
 * @return Description of return value
 * @throws ExceptionType When this exception is thrown
 *
 * @example
 * ```kotlin
 * val result = someFunction(param)
 * println(result)
 * ```
 */
fun someFunction(paramName: String): Result {
    // implementation
}
```

## Build Status

Last documentation generation: **Not yet generated**
Target completion: **Phase 1 of implementation plan**

---

**See also:**
- [ARCHITECTURE.md](../../ARCHITECTURE.md) - Overall architecture
- [CLAUDE.md](../../CLAUDE.md) - Development guidelines
- [AGENTS.md](../../AGENTS.md) - Build commands
