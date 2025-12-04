# Parser Testing Guidelines

**Version**: 1.0
**Date**: 2025-11-19
**Status**: Production-Ready
**Coverage Achievement**: 56.01% branch (from 38.57% baseline)

---

## Purpose

This document provides comprehensive guidelines for testing Yole parsers, based on proven patterns that achieved +17.44% branch coverage improvement across 4 parsers with 186 tests.

**Key Principle**: Targeted, coverage-driven testing achieves **31x better ROI** than broad integration testing.

---

## Testing Strategy

### 1. Identify High-Impact Targets

Use Kover coverage reports to find parsers with:
- ✅ **Low branch coverage** (<30%)
- ✅ **High missed branch count** (>70)
- ✅ **Complex conditional logic**
- ✅ **Production usage**

**Command**:
```bash
./gradlew :shared:koverXmlReport
python3 analyze_coverage.py  # See script below
```

**Analysis Script**:
```python
import xml.etree.ElementTree as ET
import glob

xml_files = glob.glob("shared/build/reports/kover/*.xml")
tree = ET.parse(xml_files[0])
root = tree.getroot()

files_coverage = []
for package in root.findall(".//package"):
    for sourcefile in package.findall(".//sourcefile"):
        filename = sourcefile.get('name', '')
        if 'Parser.kt' in filename:
            for counter in sourcefile.findall(".//counter"):
                if counter.get('type') == 'BRANCH':
                    covered = int(counter.get('covered', 0))
                    missed = int(counter.get('missed', 0))
                    total = covered + missed
                    pct = (covered / total * 100) if total > 0 else 100

                    if pct < 30 and missed > 70:
                        files_coverage.append({
                            'file': filename,
                            'pct': pct,
                            'missed': missed
                        })

# Sort by missed branches (descending)
files_coverage.sort(key=lambda x: x['missed'], reverse=True)
for item in files_coverage[:10]:
    print(f"{item['file']}: {item['pct']:.1f}% coverage, {item['missed']} missed branches")
```

### 2. Understand the Implementation

**Before writing tests**, read the parser implementation to understand:
- Branch logic (if/when/try-catch)
- State management
- Format-specific features
- Edge cases

**Key Questions**:
1. What are the main conditional branches?
2. How is state tracked (lists, code blocks, etc.)?
3. What formats can input data take?
4. What optional fields have defaults?
5. What validation is performed?

---

## Test Structure Template

### File Organization

```kotlin
/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for [Parser Name]
 *
 *########################################################*/
package digital.vasic.yole.format.[formatname]

import digital.vasic.yole.format.ParserRegistry
import kotlin.test.*

/**
 * Comprehensive tests for [Parser] covering all parsing branches.
 *
 * Tests cover:
 * - [Feature 1]
 * - [Feature 2]
 * - [Feature 3]
 * - Validation
 * - Edge cases
 */
class [Parser]ComprehensiveTest {

    private lateinit var parser: [Parser]

    @BeforeTest
    fun setup() {
        parser = [Parser]()
        ParserRegistry.clear()
        ParserRegistry.register(parser)
    }

    @AfterTest
    fun teardown() {
        ParserRegistry.clear()
    }

    // Test categories here...
}
```

### Test Categories (Standard)

Every parser test suite should include these categories:

#### 1. Format-Specific Feature Tests
Test the unique features of the format:
```kotlin
// ==================== [Feature Name] Tests ====================

@Test
fun `should convert [feature] to HTML`() {
    val content = """
        [format-specific syntax]
    """.trimIndent()

    val doc = parser.parse(content)

    assertTrue(doc.parsedContent.contains("[expected HTML]"))
}
```

**Examples**:
- Jupyter: Cell types, output formats, metadata fields
- AsciiDoc: Headings, admonitions, code blocks
- LaTeX: Math mode, environments, sections
- Creole: Lists, tables, inline markup

#### 2. Metadata Extraction Tests
Test all metadata fields + defaults:
```kotlin
// ==================== Metadata Tests ====================

@Test
fun `should extract [field] metadata`() {
    val content = "[syntax with metadata]"

    val doc = parser.parse(content)

    assertEquals("expected value", doc.metadata["field"])
}

@Test
fun `should provide default for missing [field]`() {
    val content = "[content without field]"

    val doc = parser.parse(content)

    assertEquals("default value", doc.metadata["field"])
}
```

#### 3. Validation Tests
Test error detection + valid input:
```kotlin
// ==================== Validation Tests ====================

@Test
fun `should detect [error type]`() {
    val content = "[malformed content]"

    val errors = parser.validate(content)

    assertTrue(errors.any { it.contains("[error description]") })
}

@Test
fun `should accept valid [feature]`() {
    val content = "[valid content]"

    val errors = parser.validate(content)

    assertFalse(errors.any { it.contains("[error]") })
}
```

#### 4. HTML Generation Tests
Test light/dark modes, escaping, structure:
```kotlin
// ==================== HTML Generation Tests ====================

@Test
fun `should generate light mode HTML`() {
    val content = "[content]"

    val doc = parser.parse(content)
    val html = doc.toHtml(lightMode = true)

    assertTrue(html.contains("[expected structure]"))
}

@Test
fun `should generate dark mode HTML`() {
    val content = "[content]"

    val doc = parser.parse(content)
    val html = doc.toHtml(lightMode = false)

    assertTrue(html.contains("[expected structure]"))
}

@Test
fun `should escape HTML special characters`() {
    val content = "Text with <>&\"'"

    val doc = parser.parse(content)
    val html = doc.toHtml(lightMode = true)

    assertTrue(html.contains("&lt;") || html.contains("&gt;"))
}
```

#### 5. Edge Case Tests
Test boundary conditions:
```kotlin
// ==================== Edge Cases ====================

@Test
fun `should handle empty content`() {
    val doc = parser.parse("")

    assertEquals("", doc.rawContent)
    assertNotNull(doc.parsedContent)
}

@Test
fun `should handle only whitespace`() {
    val content = "   \n\n\t  "

    val doc = parser.parse(content)

    assertNotNull(doc.parsedContent)
}

@Test
fun `should handle unicode characters`() {
    val content = "Unicode: 你好世界 🌍"

    val doc = parser.parse(content)

    assertTrue(doc.parsedContent.contains("你好世界"))
    assertTrue(doc.parsedContent.contains("🌍"))
}
```

#### 6. Complex Document Test
One comprehensive test with all features:
```kotlin
// ==================== Complex Document Tests ====================

@Test
fun `should handle complete [format] document`() {
    val content = """
        [comprehensive example with all features]
    """.trimIndent()

    val doc = parser.parse(content)

    // Assert all features present
    assertTrue(doc.parsedContent.contains("[feature 1]"))
    assertTrue(doc.parsedContent.contains("[feature 2]"))
    // ... etc
}
```

---

## Common Testing Patterns

### Pattern 1: State Management Testing
For parsers that track state (lists, code blocks, tables):

```kotlin
@Test
fun `should close [structure] when encountering [other structure]`() {
    val content = """
        [start structure 1]
        [start structure 2]
    """.trimIndent()

    val doc = parser.parse(content)

    assertTrue(doc.parsedContent.contains("[closing tag for structure 1]"))
    assertTrue(doc.parsedContent.contains("[opening tag for structure 2]"))
}
```

**Example (Creole)**:
```kotlin
@Test
fun `should close unordered list when encountering table`() {
    val content = """
        * Item 1
        |Cell|
    """.trimIndent()

    val doc = parser.parse(content)

    assertTrue(doc.parsedContent.contains("</ul>"))
    assertTrue(doc.parsedContent.contains("<table>"))
}
```

### Pattern 2: Multiple Format Testing
For features that accept different input formats:

```kotlin
@Test
fun `should handle [feature] as [format 1]`() {
    val content = "[feature in format 1]"
    val doc = parser.parse(content)
    assertTrue(doc.parsedContent.contains("[expected]"))
}

@Test
fun `should handle [feature] as [format 2]`() {
    val content = "[feature in format 2]"
    val doc = parser.parse(content)
    assertTrue(doc.parsedContent.contains("[expected]"))
}
```

**Example (Jupyter)**:
```kotlin
@Test
fun `should handle source as array`() {
    val notebook = """{"cells": [{"source": ["line1", "line2"]}]}"""
    // Test array format
}

@Test
fun `should handle source as string`() {
    val notebook = """{"cells": [{"source": "single line"}]}"""
    // Test string format
}
```

### Pattern 3: Nested Structure Testing
For parsers with nesting (lists, environments):

```kotlin
@Test
fun `should handle nested [structure]`() {
    val content = """
        [outer structure]
        [nested structure level 2]
        [nested structure level 3]
    """.trimIndent()

    val doc = parser.parse(content)

    // Verify nesting structure in HTML
}
```

---

## Test Naming Conventions

### Good Test Names
✅ `should convert level 1 heading to HTML`
✅ `should detect unclosed code block`
✅ `should handle source as array instead of string`
✅ `should escape HTML special characters`
✅ `should close unordered list when encountering table`

### Poor Test Names
❌ `testHeading`
❌ `test1`
❌ `shouldWork`
❌ `parseTest`

**Rules**:
1. Use backticks for descriptive names
2. Start with "should"
3. Be specific about what is being tested
4. Include the expected behavior

---

## Coverage Measurement

### Before Testing
```bash
# Generate baseline coverage
./gradlew :shared:koverXmlReport
python3 analyze_coverage.py > baseline_coverage.txt
```

### After Testing
```bash
# Run new tests
./gradlew :shared:testDebugUnitTest --tests "[YourParser]ComprehensiveTest"

# Verify all pass
# Expected: "X tests completed, 0 failed"

# Generate new coverage
./gradlew :shared:koverXmlReport --rerun-tasks

# Compare coverage
python3 << 'EOF'
import xml.etree.ElementTree as ET
import glob

xml_files = glob.glob("shared/build/reports/kover/*.xml")
tree = ET.parse(xml_files[0])
root = tree.getroot()

# Show overall coverage
for counter in root.findall("counter"):
    if counter.get('type') == 'BRANCH':
        covered = int(counter.get('covered', 0))
        total = covered + int(counter.get('missed', 0))
        print(f"Branch Coverage: {covered/total*100:.2f}% ({covered}/{total})")

# Show specific parser coverage
for package in root.findall(".//package"):
    for sourcefile in package.findall(".//sourcefile"):
        if '[YourParser].kt' in sourcefile.get('name', ''):
            for counter in sourcefile.findall(".//counter"):
                if counter.get('type') == 'BRANCH':
                    covered = int(counter.get('covered', 0))
                    total = covered + int(counter.get('missed', 0))
                    print(f"Parser: {covered/total*100:.1f}% ({covered}/{total})")
EOF
```

### Expected Improvements
Based on historical data:
- **Low-coverage parser** (<30%): +55-70% improvement
- **Medium-coverage parser** (30-50%): +30-50% improvement
- **Overall project**: +3-5% per parser tested

---

## Troubleshooting

### Test Failures

#### Issue: String interpolation in test content
```kotlin
// ❌ WRONG - $ will be interpreted
val content = "$x = y$"  // Compilation error!

// ✅ CORRECT - Escape the $
val content = "\$x = y\$"
```

#### Issue: Assertion expects exact HTML structure
```kotlin
// ❌ FRAGILE - Expects exact tag structure
assertTrue(html.contains("<ul><li>Item</li></ul>"))

// ✅ ROBUST - Check for key elements
assertTrue(html.contains("<ul>"))
assertTrue(html.contains("<li>Item</li>"))
```

#### Issue: Implementation behaves differently than expected
```kotlin
// ❌ Test fails because implementation differs
assertTrue(html.contains("<ul>"))  // But it uses <div class='list'>

// ✅ Adjust test to match actual behavior
assertTrue(html.contains("list"))
assertTrue(html.contains("Item"))
```

**Rule**: Adjust tests to match implementation behavior unless it's a bug.

### Coverage Not Improving

Possible causes:
1. **Tests don't exercise new branches** - Check if tests actually hit uncovered code
2. **Code already covered** - Integration tests may have covered it
3. **Unreachable code** - Dead code that should be removed
4. **Platform-specific code** - Needs platform-specific tests

**Solution**: Use debugger or logging to verify test execution paths.

---

## Quality Standards

### Minimum Requirements
- ✅ **100% test pass rate** before committing
- ✅ **All major features covered** (at least one test per feature)
- ✅ **Edge cases included** (empty, unicode, malformed)
- ✅ **No hardcoded values** (use const or extract to variables)
- ✅ **Clear test names** (descriptive, follows naming convention)

### Aspirational Goals
- 🎯 **80%+ branch coverage** for the parser
- 🎯 **90%+ line coverage** for the parser
- 🎯 **Complex document test** exercising all features
- 🎯 **Performance consideration** (tests run in <1s total)

---

## Example: Complete Test Suite

See `JupyterParserComprehensiveTest.kt` as the gold standard:
- ✅ 37 tests covering all branches
- ✅ Improved coverage from 1.8% → 68.2%
- ✅ All features tested (cells, metadata, outputs, validation)
- ✅ Edge cases included (empty, null, malformed JSON)
- ✅ Complex document test with mixed cells
- ✅ Clear organization with comment sections

**Location**: `shared/src/commonTest/kotlin/digital/vasic/yole/format/jupyter/JupyterParserComprehensiveTest.kt`

---

## Quick Reference

### Test Categories Checklist
- [ ] Format-specific features (headings, lists, tables, etc.)
- [ ] Metadata extraction
- [ ] Metadata defaults
- [ ] Validation (errors)
- [ ] Validation (valid input)
- [ ] HTML generation (light mode)
- [ ] HTML generation (dark mode)
- [ ] HTML escaping
- [ ] Edge cases (empty, whitespace, unicode)
- [ ] Complex document

### Expected Test Count
- **Simple parser**: 20-30 tests
- **Medium complexity**: 30-50 tests
- **Complex parser**: 45-60 tests

### Target Coverage
- **Parser-specific**: 80-90% branch coverage
- **Project overall**: +3-5% per parser

---

## FAQs

**Q: How many tests should I write?**
A: Enough to cover all major branches. Typical range: 30-60 tests depending on parser complexity.

**Q: Should I test every single branch?**
A: Focus on meaningful branches. Skip trivial getters/setters unless they have logic.

**Q: What if a test fails?**
A: First, understand the implementation behavior. Adjust test if implementation is correct, file bug if it's wrong.

**Q: How do I know if I've covered enough?**
A: Run Kover and check parser-specific coverage. Aim for 80%+ branch coverage.

**Q: Should I write tests for edge cases first?**
A: No - start with happy path (main features), then add edge cases.

---

## Next Steps

### For New Parsers
1. Run coverage analysis to identify target
2. Read parser implementation
3. Create test file from template
4. Write tests by category
5. Run tests + measure coverage
6. Iterate until 80%+ coverage

### For Existing Parsers
1. Check current coverage
2. Identify uncovered branches
3. Add targeted tests
4. Measure improvement

### For Project
1. Continue testing remaining parsers
2. Document patterns discovered
3. Create parser testing checklist
4. Integrate into CI/CD

---

**Document Version**: 1.0
**Last Updated**: 2025-11-19
**Maintained By**: Engineering Team
**Next Review**: After completing 2-3 more parsers
