# Next Steps - Quick Reference

**Last Updated**: November 11, 2025

---

## ⚡ Quick Start

**To resume work:**
```
"please continue with the implementation"
```

**Current blocker:** Assertion library incompatibility (AssertJ → kotlin.test)

**Full details:** See [CURRENT_STATUS.md](./CURRENT_STATUS.md)

---

## 🎯 Immediate Priority: Fix Assertion Library

### Problem
All 18 parser test files use AssertJ (JVM-only) instead of kotlin.test (multiplatform).

**Compilation error**: `Unresolved reference 'assertj'`

### Solution: Convert Assertions

**Files to fix** (18 total):
```
shared/src/commonTest/kotlin/digital/vasic/yole/format/
├── markdown/MarkdownParserTest.kt    ← START HERE (fully implemented, just needs fix)
├── todotxt/TodoTxtParserTest.kt      ← HIGH PRIORITY
├── csv/CsvParserTest.kt              ← HIGH PRIORITY
├── plaintext/PlainTextParserTest.kt  ← HIGH PRIORITY
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

### Conversion Reference

| From (AssertJ) | To (kotlin.test) |
|----------------|------------------|
| `import org.assertj.core.api.Assertions.assertThat` | `import kotlin.test.*` |
| `assertThat(x).isEqualTo(y)` | `assertEquals(y, x)` |
| `assertThat(x).contains(y)` | `assertTrue(x.contains(y))` |
| `assertThat(x).isNotEmpty()` | `assertTrue(x.isNotEmpty())` |

**Example file to reference**: `shared/src/commonTest/kotlin/digital/vasic/yole/format/FormatRegistryTest.kt`

---

## 📝 Step-by-Step Execution Plan

### Step 1: Fix MarkdownParserTest.kt First
- File: `shared/src/commonTest/kotlin/digital/vasic/yole/format/markdown/MarkdownParserTest.kt`
- Status: Fully implemented with 34+ comprehensive tests
- Action: Convert all AssertJ assertions to kotlin.test
- Why first: Already complete, just needs assertion fix

### Step 2: Verify Compilation
```bash
cd /Users/milosvasic/Projects/Yole
export GRADLE_USER_HOME="/Users/milosvasic/.gradle"
./gradlew :shared:desktopTest --tests "*MarkdownParserTest" --no-daemon
```

Expected: Compiles successfully (tests may fail but no compilation errors)

### Step 3: Fix Remaining 17 Parser Tests
Convert assertions in priority order:
1. **High priority**: TodoTxt, CSV, PlainText (3 files)
2. **Medium priority**: LaTeX, OrgMode, WikiText, AsciiDoc, RestructuredText (5 files)
3. **Low priority**: KeyValue, TaskPaper, Textile, Creole, TiddlyWiki, Jupyter, RMarkdown, Binary (8 files)

### Step 4: Complete Test Implementation

After assertions fixed, add real samples to:
1. Todo.txt parser tests (priority markers, contexts, projects)
2. CSV parser tests (headers, quoted fields, escaping)
3. Plain Text parser tests (edge cases)
4. Continue with remaining formats

### Step 5: Verify Coverage
```bash
./gradlew koverHtmlReport
open build/reports/kover/html/index.html
```

Target: 90% coverage per parser module

---

## 📊 Current Progress

- **Phase 2**: 19% complete (176/920 tests)
- **Task 2.1** (FormatRegistry): ✅ 126 tests, ~99% coverage
- **Task 2.2** (Parsers): ⏸️ Blocked - 0/540 tests compiled
  - Markdown: ✅ 34+ tests written, ⚠️ needs assertion fix
  - Others: 📋 Scaffolds generated, need implementation
- **Task 2.3** (Android UI): ✅ 50+ tests exist
- **Tasks 2.4-2.5**: ⏸️ Pending

---

## 🔗 Key Documents

- **Current Status**: [CURRENT_STATUS.md](./CURRENT_STATUS.md) - Full context and blocker details
- **Phase 2 Progress**: [PHASE_2_PROGRESS.md](./PHASE_2_PROGRESS.md) - Detailed task breakdown
- **Test Guide**: [TEST_IMPLEMENTATION_GUIDE.md](./TEST_IMPLEMENTATION_GUIDE.md) - Implementation patterns
- **Session Summary**: [SESSION_SUMMARY.md](./SESSION_SUMMARY.md) - Historical record

---

## ✅ Success Criteria for Next Milestone

1. ✅ All 18 parser test files compile without errors
2. ✅ MarkdownParserTest runs successfully (34+ tests pass)
3. ✅ High-priority parser tests implemented (Todo.txt, CSV, Plain Text)
4. ✅ Coverage reports show progress toward 90% per parser

---

**To continue: "please continue with the implementation"**
