# Core Module Architecture Investigation
**Date:** November 10, 2025
**Investigator:** AI Assistant
**Status:** ✅ RESOLVED

---

## Executive Summary

**Finding:** The "core" module is a **legacy Android module** that only contains third-party encryption utilities. The actual format system (FormatRegistry, parsers, converters) is located in the **shared** module as part of the Kotlin Multiplatform architecture.

**Conclusion:** This is not a bug or missing code - it's the correct architecture for a Kotlin Multiplatform project.

---

## Investigation Details

### Initial Confusion

The audit reported that the `core` module appeared to be "missing main source code" because:
- Documentation (CLAUDE.md, ARCHITECTURE.md) referred to `core` as containing FormatRegistry and base classes
- `core/src/main/` directory does not exist
- Only `core/thirdparty/` directory found

### Actual Architecture Discovery

#### Core Module (`core/`) - Legacy Android Module
**Location:** `/Users/milosvasic/Projects/Yole/core/`

**Contents:**
```
core/
├── build.gradle (legacy)
├── build.gradle.kts (active)
├── .project (Eclipse metadata)
└── thirdparty/
    └── java/other/de/stanetz/jpencconverter/
        ├── JavaPasswordbasedCryption.java
        └── PasswordStore.java
```

**Purpose:**
- Contains **only** third-party encryption utilities for AES256 file encryption
- Legacy Android module from pre-KMP architecture
- No main source code - only thirdparty code

**Files:**
1. `JavaPasswordbasedCryption.java` - Password-based encryption implementation
2. `PasswordStore.java` - Secure password storage

#### Shared Module (`shared/`) - Kotlin Multiplatform Core
**Location:** `/Users/milosvasic/Projects/Yole/shared/`

**This is where the format system actually lives!**

**Contents:**
```
shared/src/
├── commonMain/kotlin/digital/vasic/yole/
│   ├── format/
│   │   ├── FormatRegistry.kt ✅ (Found here!)
│   │   ├── TextFormat.kt
│   │   ├── markdown/MarkdownParser.kt
│   │   ├── todotxt/TodoTxtParser.kt
│   │   ├── csv/CsvParser.kt
│   │   ├── wikitext/WikitextParser.kt
│   │   ├── latex/LatexParser.kt
│   │   ├── asciidoc/AsciidocParser.kt
│   │   ├── orgmode/OrgModeParser.kt
│   │   ├── restructuredtext/RestructuredTextParser.kt
│   │   ├── taskpaper/TaskpaperParser.kt
│   │   ├── textile/TextileParser.kt
│   │   ├── creole/CreoleParser.kt
│   │   ├── tiddlywiki/TiddlyWikiParser.kt
│   │   ├── jupyter/JupyterParser.kt
│   │   ├── rmarkdown/RMarkdownParser.kt
│   │   ├── plaintext/PlaintextParser.kt
│   │   └── keyvalue/KeyValueParser.kt
│   └── model/
│       ├── Document.kt
│       └── ...
├── commonTest/kotlin/
│   └── digital/vasic/yole/format/
│       ├── FormatRegistryTest.kt ✅
│       ├── latex/LatexParserTest.kt
│       └── asciidoc/AsciidocParserTest.kt
├── androidMain/kotlin/
├── desktopMain/kotlin/
├── iosMain/kotlin/ (disabled)
└── wasmJsMain/kotlin/
```

---

## Format System Architecture

### FormatRegistry.kt
**Location:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt`
**Lines:** 390
**Purpose:** Central registry for all 18+ text formats

**Key Features:**
- Singleton object with comprehensive format management
- Format detection by extension, content, or filename
- 16 formats currently registered (2 missing: ICS, more key-value variants)
- Well-documented with KDoc examples

**Methods:**
- `getById(id: String): TextFormat?` - Lookup by ID
- `getByExtension(extension: String): TextFormat?` - Lookup by extension
- `detectByContent(content: String, maxLines: Int = 10): TextFormat?` - Content-based detection
- `detectByExtension(extension: String): TextFormat` - Extension-based detection with fallback
- `detectByFilename(filename: String): TextFormat` - Filename-based detection
- `getFormatsByExtension(extension: String): List<TextFormat>` - All formats for extension
- `isSupported(formatId: String): Boolean` - Check if format supported
- `getFormatNames(): List<String>` - All format names
- `getAllExtensions(): List<String>` - All supported extensions
- `isExtensionSupported(extension: String): Boolean` - Check extension support

**Registered Formats (16 total):**
1. Markdown (md, markdown, mdown, mkd)
2. Plain Text (txt, text, log)
3. Todo.txt (txt)
4. CSV (csv)
5. WikiText (wiki, wikitext)
6. Org Mode (org)
7. Creole (creole)
8. TiddlyWiki (tid, tiddly)
9. LaTeX (tex, latex)
10. AsciiDoc (adoc, asciidoc)
11. reStructuredText (rst, rest)
12. Key-Value (keyvalue, properties, ini)
13. TaskPaper (taskpaper)
14. Textile (textile)
15. Jupyter (ipynb)
16. R Markdown (rmd, rmarkdown)
17. Binary (bin) - special format

**Missing from original 18+ count:**
- ICS/VCS (calendar formats) - not registered
- Additional key-value variants (JSON, YAML, TOML, VCF) - mentioned but not separate formats

### Parser Architecture

Each format has a dedicated parser in `shared/src/commonMain/kotlin/digital/vasic/yole/format/[format]/`:
- `MarkdownParser.kt`
- `TodoTxtParser.kt`
- `LatexParser.kt`
- `AsciidocParser.kt`
- `OrgModeParser.kt`
- `RestructuredTextParser.kt`
- `PlaintextParser.kt`
- `WikitextParser.kt`
- `TaskpaperParser.kt`
- `TextileParser.kt`
- `CreoleParser.kt`
- `TiddlyWikiParser.kt`
- `JupyterParser.kt`
- `RMarkdownParser.kt`
- `KeyValueParser.kt` (handles JSON, YAML, TOML, INI, VCF, ICS)

---

## Why This Architecture Makes Sense

### Kotlin Multiplatform Best Practices

In KMP projects, the standard structure is:
1. **shared module** - Contains all cross-platform business logic
   - commonMain: Code that runs on all platforms
   - androidMain: Android-specific implementations
   - iosMain: iOS-specific implementations
   - desktopMain: Desktop-specific implementations
   - wasmJsMain: Web-specific implementations

2. **Platform app modules** - Platform-specific UI and app code
   - androidApp: Android UI
   - iosApp: iOS UI
   - desktopApp: Desktop UI
   - webApp: Web UI

3. **Legacy modules** (if migrating from single-platform)
   - core: Legacy Android utilities
   - commons: Shared Android utilities

### Yole's Architecture

**Current Structure:**
- ✅ `shared/` - **Primary location** for format system (FormatRegistry, parsers, models)
- ✅ `commons/` - Android utilities (GsFileUtils, GsContextUtils, etc.)
- ✅ `core/` - Legacy encryption utilities only
- ✅ `androidApp/` - Android UI
- 🚧 `desktopApp/` - Desktop UI (minimal)
- 🚧 `iosApp/` - iOS UI (disabled)
- 🚧 `webApp/` - Web UI (stub)

**This is correct KMP architecture!**

---

## Documentation Issues

### What Needs to Be Fixed

The confusion arose because documentation was outdated and referenced the old architecture:

**Files to Update:**
1. ✅ CLAUDE.md - Update to reflect actual shared module structure
2. ✅ ARCHITECTURE.md - Clarify that FormatRegistry is in shared module
3. ✅ FORMAT_DOCUMENTATION.md - Update module references
4. ✅ COMPREHENSIVE_AUDIT_REPORT.md - Note resolution

**Correct Documentation Should Say:**
- **FormatRegistry** is in `shared/src/commonMain/kotlin/digital/vasic/yole/format/`
- **Format parsers** are in `shared/src/commonMain/kotlin/digital/vasic/yole/format/[format]/`
- **Core module** only contains third-party encryption code (JavaPasswordbasedCryption)
- **Format system** is part of the Kotlin Multiplatform shared module

---

## Test Coverage Analysis

### Existing Tests

**FormatRegistry Tests:**
- ✅ `shared/src/commonTest/kotlin/digital/vasic/yole/format/FormatRegistryTest.kt`

**Parser Tests:**
- ✅ `shared/src/commonTest/kotlin/digital/vasic/yole/format/latex/LatexParserTest.kt`
- ✅ `shared/src/commonTest/kotlin/digital/vasic/yole/format/asciidoc/AsciidocParserTest.kt`

**Missing Tests:**
- 14 parsers have no tests yet

---

## Recommendations

### 1. Update Documentation (Immediate)

Update all documentation to accurately reflect the Kotlin Multiplatform architecture:

**CLAUDE.md:**
```markdown
## Module Structure

### shared/ - Kotlin Multiplatform Core
- **FormatRegistry** - `shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt`
- **Format Parsers** - `shared/src/commonMain/kotlin/digital/vasic/yole/format/[format]/`
- **Models** - `shared/src/commonMain/kotlin/digital/vasic/yole/model/`

### core/ - Legacy Encryption Module
- Contains only third-party encryption utilities (JavaPasswordbasedCryption, PasswordStore)
- No main application code
```

### 2. Consider Renaming (Optional)

To avoid confusion, consider renaming:
- `core/` → `encryption/` or `crypto/`
- This clearly indicates its actual purpose

### 3. Add Module README Files (Phase 3)

Create `shared/README.md`:
```markdown
# Shared Module

This module contains the Kotlin Multiplatform core logic shared across all platforms.

## Structure

- `commonMain/` - Code that runs on all platforms
  - `format/` - Format system (FormatRegistry, parsers)
  - `model/` - Data models (Document, etc.)
- `androidMain/` - Android-specific implementations
- `desktopMain/` - Desktop-specific implementations
- `iosMain/` - iOS-specific implementations (currently disabled)
- `wasmJsMain/` - Web-specific implementations
```

Create `core/README.md`:
```markdown
# Core Module

**Note:** This is a legacy Android module containing only third-party encryption utilities.

## Contents

- JavaPasswordbasedCryption.java - AES256 password-based encryption
- PasswordStore.java - Secure password storage

## Note on Architecture

The main application logic (FormatRegistry, parsers, etc.) is in the `shared/` module, not here.
This module only contains encryption utilities for file encryption features.
```

---

## Conclusion

**Status:** ✅ **RESOLVED - No Bug, Correct Architecture**

The "missing core module code" was not actually missing. The format system is correctly located in the `shared/` module as part of the Kotlin Multiplatform architecture. The `core/` module only contains legacy encryption utilities and serves a specific, limited purpose.

**Actions Required:**
1. ✅ Update CLAUDE.md to reflect actual structure
2. ✅ Update ARCHITECTURE.md to clarify module purposes
3. ✅ Update COMPREHENSIVE_AUDIT_REPORT.md with findings
4. ⏳ Create module README files (Phase 3)
5. ⏳ Consider renaming core module (optional, low priority)

**No Code Changes Needed:** The architecture is correct and follows KMP best practices.

---

## Files Referenced

- `shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt` (390 lines)
- `core/thirdparty/java/other/de/stanetz/jpencconverter/JavaPasswordbasedCryption.java`
- `core/thirdparty/java/other/de/stanetz/jpencconverter/PasswordStore.java`
- `shared/src/commonTest/kotlin/digital/vasic/yole/format/FormatRegistryTest.kt`

---

**Investigation Complete:** November 10, 2025
