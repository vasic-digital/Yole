# Archived: core module

**Archived on:** 2026-03-19
**Reason:** Orphaned legacy module

## Background

The `core` module was a legacy Android library module (Java-based) from the original
Markor/Gsantner codebase. It contained:

- `FormatRegistry.java` / `FormatRegistryImpl.java` -- Java interface/implementation
  for format detection (superseded by `shared` module's Kotlin `FormatRegistry` object)
- `ActionButtonBase.java` -- Base class for format action buttons
- `TextConverterBase.java` -- Base class for text format converters
- `SyntaxHighlighterBase.java` -- Base class for syntax highlighting
- `AppSettings.java`, `Document.java`, `DocumentManager.java`, `StorageManager.java`
  -- Model classes (superseded by KMP equivalents in `shared`)

## Why archived

No Gradle module referenced `project(":core")` as a dependency. All active source code
imports `digital.vasic.yole.format.FormatRegistry` from the KMP `shared` module
(`shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt`), not from
this legacy Java interface. The core module's functionality has been fully replaced by
the Kotlin Multiplatform shared module.

## Restoration

If needed, restore with:
```bash
git mv archive/core core
```
Then re-add `include(":core")` to `settings.gradle.kts`.
