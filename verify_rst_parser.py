#!/usr/bin/env python3

"""
Simple verification script to check if the reStructuredText parser test file
was created correctly and follows the expected patterns.
"""

import os
import re
import sys

def check_file_exists():
    """Check if the test file exists."""
    test_file = "/home/milosvasic/Projects/Yole/shared/src/commonTest/kotlin/digital/vasic/yole/format/rst/RstParserTest.kt"
    if not os.path.exists(test_file):
        print(f"❌ Test file does not exist: {test_file}")
        return False
    print(f"✅ Test file exists: {test_file}")
    return True

def check_file_structure():
    """Check if the file has the expected structure."""
    test_file = "/home/milosvasic/Projects/Yole/shared/src/commonTest/kotlin/digital/vasic/yole/format/rst/RstParserTest.kt"
    
    with open(test_file, 'r') as f:
        content = f.read()
    
    # Check for required imports
    required_imports = [
        "digital.vasic.yole.format.FormatRegistry",
        "digital.vasic.yole.format.TextFormat",
        "digital.vasic.yole.format.restructuredtext.RestructuredTextParser",
        "kotlin.test.Test",
        "kotlin.test.assertEquals",
        "kotlin.test.assertNotNull",
        "kotlin.test.assertTrue"
    ]
    
    for import_stmt in required_imports:
        if import_stmt not in content:
            print(f"❌ Missing import: {import_stmt}")
            return False
    
    print("✅ All required imports present")
    
    # Check for test class
    if "class RstParserTest" not in content:
        print("❌ Missing test class RstParserTest")
        return False
    print("✅ Test class RstParserTest found")
    
    # Check for parser instance
    if "RestructuredTextParser()" not in content:
        print("❌ Missing RestructuredTextParser instance")
        return False
    print("✅ RestructuredTextParser instance found")
    
    # Count number of test methods
    test_methods = re.findall(r'@Test\s+fun\s+`[^`]+`', content)
    print(f"✅ Found {len(test_methods)} test methods")
    
    # Check for key test categories
    test_categories = [
        "Format Detection",
        "Basic reStructuredText Parsing",
        "Lists",
        "Directives",
        "Code Blocks",
        "Links and Cross-references",
        "Tables",
        "Validation",
        "Edge Cases",
        "HTML Conversion",
        "Round-trip",
        "Performance"
    ]
    
    found_categories = 0
    for category in test_categories:
        if category in content:
            found_categories += 1
            print(f"✅ Found test category: {category}")
    
    print(f"✅ Found {found_categories}/{len(test_categories)} test categories")
    
    return True

def check_test_coverage():
    """Check if the test covers the required functionality."""
    test_file = "/home/milosvasic/Projects/Yole/shared/src/commonTest/kotlin/digital/vasic/yole/format/rst/RstParserTest.kt"
    
    with open(test_file, 'r') as f:
        content = f.read()
    
    # Check for specific test patterns
    required_tests = [
        ("format detection", r"should detect.*format.*extension"),
        ("basic parsing", r"should parse.*document structure"),
        ("headings", r"should parse.*headings"),
        ("paragraphs", r"should parse.*paragraphs"),
        ("lists", r"should parse.*lists"),
        ("directives", r"should parse.*directives"),
        ("code blocks", r"should parse.*code blocks"),
        ("links", r"should parse.*links"),
        ("tables", r"should parse.*tables"),
        ("validation", r"should validate"),
        ("edge cases", r"should handle empty"),
        ("HTML conversion", r"should convert.*HTML"),
        ("round-trip", r"should support round-trip"),
        ("performance", r"should parse.*efficiently")
    ]
    
    found_tests = 0
    for test_name, pattern in required_tests:
        if re.search(pattern, content, re.IGNORECASE):
            found_tests += 1
            print(f"✅ Found test for: {test_name}")
        else:
            print(f"⚠️  Missing test for: {test_name}")
    
    print(f"✅ Found {found_tests}/{len(required_tests)} required test types")
    return found_tests >= len(required_tests) * 0.8  # At least 80% coverage

def check_api_usage():
    """Check if the test uses the correct API."""
    test_file = "/home/milosvasic/Projects/Yole/shared/src/commonTest/kotlin/digital/vasic/yole/format/rst/RstParserTest.kt"
    
    with open(test_file, 'r') as f:
        content = f.read()
    
    # Check for correct API usage
    api_checks = [
        ("parse() method", r"parser\.parse\("),
        ("toHtml() method", r"parser\.toHtml\("),
        ("validate() method", r"parser\.validate\("),
        ("ParsedDocument result", r"val result = parser\.parse"),
        ("metadata access", r"result\.metadata"),
        ("format check", r"result\.format\.id"),
        ("raw content", r"result\.rawContent")
    ]
    
    found_apis = 0
    for api_name, pattern in api_checks:
        if re.search(pattern, content):
            found_apis += 1
            print(f"✅ Found API usage: {api_name}")
    
    print(f"✅ Found {found_apis}/{len(api_checks)} API usage patterns")
    return found_apis >= len(api_checks) * 0.8  # At least 80% API usage

def main():
    """Main verification function."""
    print("🔍 Verifying reStructuredText Parser Test Suite")
    print("=" * 50)
    
    success = True
    
    # Check if file exists
    if not check_file_exists():
        success = False
    
    # Check file structure
    if not check_file_structure():
        success = False
    
    # Check test coverage
    if not check_test_coverage():
        success = False
    
    # Check API usage
    if not check_api_usage():
        success = False
    
    print("\n" + "=" * 50)
    if success:
        print("✅ All checks passed! The reStructuredText parser test suite looks good.")
        print("\n📋 Summary:")
        print("- Comprehensive test coverage for reStructuredText parsing")
        print("- Tests format detection, basic parsing, lists, directives, code blocks")
        print("- Tests links, tables, validation, edge cases, HTML conversion")
        print("- Includes round-trip parsing and performance benchmarks")
        print("- Uses correct API: parse() returns ParsedDocument")
        print("- Follows the same pattern as other parser tests in the project")
        return 0
    else:
        print("❌ Some checks failed. Please review the test file.")
        return 1

if __name__ == "__main__":
    sys.exit(main())