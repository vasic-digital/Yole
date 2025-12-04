# Assertion Library Fix Guide

**Issue**: Parser tests use AssertJ (JVM-only) instead of kotlin.test (multiplatform)
**Status**: Blocking 18 parser test files from compiling
**Priority**: Critical

---

## 🎯 Quick Fix

For each `*ParserTest.kt` file in `shared/src/commonTest/kotlin/digital/vasic/yole/format/`:

1. Replace imports
2. Convert all assertions
3. Verify compilation

---

## 📝 Step-by-Step Conversion

### Step 1: Update Imports

**Remove:**
```kotlin
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.assertNotNull  // Keep if present
import kotlin.test.assertTrue     // Keep if present
```

**Add:**
```kotlin
import kotlin.test.*
```

### Step 2: Convert Assertions

Use these find-and-replace patterns:

#### Equality Assertions

```kotlin
// Before
assertThat(format.id).isEqualTo(FormatRegistry.ID_MARKDOWN)
assertThat(format.name).isEqualTo("Markdown")

// After
assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
assertEquals("Markdown", format.name)
```

**Pattern**: `assertThat(X).isEqualTo(Y)` → `assertEquals(Y, X)`
**Note**: Arguments are **reversed** (expected comes first)

#### Contains Assertions

```kotlin
// Before
assertThat(result.parsedContent).contains("<h1>")
assertThat(result.parsedContent).contains("<strong>")

// After
assertTrue(result.parsedContent.contains("<h1>"))
assertTrue(result.parsedContent.contains("<strong>"))
```

**Pattern**: `assertThat(X).contains(Y)` → `assertTrue(X.contains(Y))`

#### Empty/Not Empty Assertions

```kotlin
// Before
assertThat(result.rawContent).isEmpty()
assertThat(result.parsedContent).isNotEmpty()

// After
assertTrue(result.rawContent.isEmpty())
assertTrue(result.parsedContent.isNotEmpty())
```

**Pattern**: `assertThat(X).isEmpty()` → `assertTrue(X.isEmpty())`

#### Null Assertions

```kotlin
// Before
assertThat(format).isNotNull()
assertThat(nullValue).isNull()

// After
assertNotNull(format)
assertNull(nullValue)
```

**Pattern**: Already uses kotlin.test (keep as-is if present)

#### Not Equal Assertions

```kotlin
// Before
assertThat(format.id).isNotEqualTo(FormatRegistry.ID_PLAIN_TEXT)

// After
assertNotEquals(FormatRegistry.ID_PLAIN_TEXT, format.id)
```

**Pattern**: `assertThat(X).isNotEqualTo(Y)` → `assertNotEquals(Y, X)`

---

## 🔄 Complete Conversion Reference

| AssertJ | kotlin.test |
|---------|-------------|
| `assertThat(x).isEqualTo(y)` | `assertEquals(y, x)` ⚠️ reversed |
| `assertThat(x).isNotEqualTo(y)` | `assertNotEquals(y, x)` ⚠️ reversed |
| `assertThat(x).contains(y)` | `assertTrue(x.contains(y))` |
| `assertThat(x).doesNotContain(y)` | `assertFalse(x.contains(y))` |
| `assertThat(x).isEmpty()` | `assertTrue(x.isEmpty())` |
| `assertThat(x).isNotEmpty()` | `assertTrue(x.isNotEmpty())` |
| `assertThat(x).isNull()` | `assertNull(x)` |
| `assertThat(x).isNotNull()` | `assertNotNull(x)` |
| `assertThat(x).isTrue()` | `assertTrue(x)` |
| `assertThat(x).isFalse()` | `assertFalse(x)` |
| `assertThat(x).isInstanceOf(Y::class.java)` | `assertTrue(x is Y)` |
| `assertThat(x).hasSize(n)` | `assertEquals(n, x.size)` |
| `assertThat(x).startsWith(y)` | `assertTrue(x.startsWith(y))` |
| `assertThat(x).endsWith(y)` | `assertTrue(x.endsWith(y))` |
| `assertThat(x).matches(regex)` | `assertTrue(x.matches(regex))` |

---

## 📂 Files Requiring Conversion

### Priority 1: High-Priority Formats (Start Here)

1. ✅ **MarkdownParserTest.kt** - Fully implemented, needs assertion fix
   - Location: `shared/src/commonTest/kotlin/digital/vasic/yole/format/markdown/`
   - Tests: 34+ comprehensive tests
   - Status: Complete implementation, blocked by assertions

2. **TodoTxtParserTest.kt** - Scaffold only
   - Location: `shared/src/commonTest/kotlin/digital/vasic/yole/format/todotxt/`
   - Tests: Placeholder scaffolds
   - Status: Needs assertion fix + real test implementation

3. **CsvParserTest.kt** - Scaffold only
   - Location: `shared/src/commonTest/kotlin/digital/vasic/yole/format/csv/`

4. **PlainTextParserTest.kt** - Scaffold only (has syntax fix applied)
   - Location: `shared/src/commonTest/kotlin/digital/vasic/yole/format/plaintext/`

### Priority 2: Medium-Priority Formats

5. **LatexParserTest.kt**
6. **OrgModeParserTest.kt** - May have ID constant issues
7. **WikitextParserTest.kt**
8. **AsciidocParserTest.kt**
9. **RestructuredTextParserTest.kt**

### Priority 3: Low-Priority Formats

10. **KeyValueParserTest.kt** - May have parser name issues
11. **TaskpaperParserTest.kt**
12. **TextileParserTest.kt**
13. **CreoleParserTest.kt**
14. **TiddlyWikiParserTest.kt**
15. **JupyterParserTest.kt**
16. **RMarkdownParserTest.kt**
17. **BinaryParserTest.kt**

---

## 🔍 Example: Full File Conversion

### Before (AssertJ)

```kotlin
package digital.vasic.yole.format.markdown

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.markdown.MarkdownParser
import org.junit.Test
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MarkdownParserTest {
    private val parser = MarkdownParser()

    @Test
    fun `should parse headers H1 through H6`() {
        val content = """
            # H1 Header
            ## H2 Header
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertThat(result.parsedContent).contains("<h1>H1 Header</h1>")
        assertThat(result.parsedContent).contains("<h2>H2 Header</h2>")
        assertThat(result.rawContent).isEqualTo(content)
    }

    @Test
    fun `should detect Markdown format by extension`() {
        val format = FormatRegistry.getByExtension(".md")

        assertNotNull(format)
        assertThat(format.id).isEqualTo(FormatRegistry.ID_MARKDOWN)
        assertThat(format.name).isEqualTo("Markdown")
    }
}
```

### After (kotlin.test)

```kotlin
package digital.vasic.yole.format.markdown

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.markdown.MarkdownParser
import org.junit.Test
import kotlin.test.*

class MarkdownParserTest {
    private val parser = MarkdownParser()

    @Test
    fun `should parse headers H1 through H6`() {
        val content = """
            # H1 Header
            ## H2 Header
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertTrue(result.parsedContent.contains("<h1>H1 Header</h1>"))
        assertTrue(result.parsedContent.contains("<h2>H2 Header</h2>"))
        assertEquals(content, result.rawContent)  // Note: reversed args
    }

    @Test
    fun `should detect Markdown format by extension`() {
        val format = FormatRegistry.getByExtension(".md")

        assertNotNull(format)
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)  // Note: reversed args
        assertEquals("Markdown", format.name)                 // Note: reversed args
    }
}
```

### Key Changes

1. ✅ Import changed: `org.assertj.core.api.Assertions.assertThat` → `kotlin.test.*`
2. ✅ Contains: `assertThat(x).contains(y)` → `assertTrue(x.contains(y))`
3. ✅ Equality: `assertThat(x).isEqualTo(y)` → `assertEquals(y, x)` ⚠️ **reversed**
4. ✅ Null check: `assertNotNull(result)` - **no change** (already kotlin.test)

---

## ✅ Verification Steps

After converting each file:

### 1. Check Syntax
```bash
# Open file in editor and check for:
- No remaining `assertThat` calls
- No remaining `org.assertj` imports
- `import kotlin.test.*` present
```

### 2. Compile Test
```bash
cd /Users/milosvasic/Projects/Yole
export GRADLE_USER_HOME="/Users/milosvasic/.gradle"
./gradlew :shared:desktopTest --tests "*MarkdownParserTest" --no-daemon
```

Expected output:
- ✅ **Success**: "Compilation successful" or "BUILD SUCCESSFUL"
- ❌ **Failure**: "Unresolved reference" → missed an assertion

### 3. Run Test
```bash
./gradlew :shared:desktopTest --tests "*MarkdownParserTest" --no-daemon --info
```

Expected output:
- Tests execute (pass or fail is OK at this stage)
- No compilation errors

---

## 🚨 Common Pitfalls

### 1. Reversed Arguments

**Wrong:**
```kotlin
assertEquals(format.id, FormatRegistry.ID_MARKDOWN)  // ❌
```

**Correct:**
```kotlin
assertEquals(FormatRegistry.ID_MARKDOWN, format.id)  // ✅
```

**Rule**: kotlin.test uses `assertEquals(expected, actual)`

### 2. Nested Contains

**Wrong:**
```kotlin
// Before:
assertThat(list).contains(item1).contains(item2)
// After (incorrect):
assertTrue(list.contains(item1).contains(item2))  // ❌ .contains() returns Boolean
```

**Correct:**
```kotlin
assertTrue(list.contains(item1))
assertTrue(list.contains(item2))
```

### 3. Complex Assertions

**Before:**
```kotlin
assertThat(result.parsedContent)
    .contains("<h1>")
    .contains("<strong>")
    .doesNotContain("<script>")
```

**After:**
```kotlin
assertTrue(result.parsedContent.contains("<h1>"))
assertTrue(result.parsedContent.contains("<strong>"))
assertFalse(result.parsedContent.contains("<script>"))
```

---

## 📊 Progress Tracking

| File | Assertions Fixed | Compiles | Notes |
|------|------------------|----------|-------|
| MarkdownParserTest.kt | ⏳ | ❌ | Priority 1 - Fully implemented |
| TodoTxtParserTest.kt | ⏳ | ❌ | Priority 1 |
| CsvParserTest.kt | ⏳ | ❌ | Priority 1 |
| PlainTextParserTest.kt | ⏳ | ❌ | Priority 1 - Syntax fixed |
| LatexParserTest.kt | ⏳ | ❌ | Priority 2 |
| OrgModeParserTest.kt | ⏳ | ❌ | Priority 2 - Check ID constants |
| WikitextParserTest.kt | ⏳ | ❌ | Priority 2 |
| AsciidocParserTest.kt | ⏳ | ❌ | Priority 2 |
| RestructuredTextParserTest.kt | ⏳ | ❌ | Priority 2 |
| KeyValueParserTest.kt | ⏳ | ❌ | Priority 3 - Check parser name |
| TaskpaperParserTest.kt | ⏳ | ❌ | Priority 3 |
| TextileParserTest.kt | ⏳ | ❌ | Priority 3 |
| CreoleParserTest.kt | ⏳ | ❌ | Priority 3 |
| TiddlyWikiParserTest.kt | ⏳ | ❌ | Priority 3 |
| JupyterParserTest.kt | ⏳ | ❌ | Priority 3 |
| RMarkdownParserTest.kt | ⏳ | ❌ | Priority 3 |
| BinaryParserTest.kt | ⏳ | ❌ | Priority 3 |

**Legend**: ⏳ Pending | ✅ Done | ❌ Not compiled

---

## 🎯 Success Criteria

- [ ] All 18 parser test files compile without errors
- [ ] No `Unresolved reference 'assertj'` errors
- [ ] All imports use `kotlin.test.*`
- [ ] Tests execute (pass or fail)
- [ ] Ready for test implementation phase

---

**Next**: After fixing assertions, implement real test samples starting with MarkdownParserTest.kt (already done), then Todo.txt, CSV, Plain Text.

**See**: [NEXT_STEPS.md](./NEXT_STEPS.md) for continuation plan
