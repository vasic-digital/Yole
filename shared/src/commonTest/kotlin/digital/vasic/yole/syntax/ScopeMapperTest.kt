/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 8: table-driven unit tests for ScopeMapper.
 *   - Exact-match coverage of ~20 spec'd Tree-Sitter scopes.
 *   - Hierarchical fallback: keyword.control.return → keyword.control.
 *   - Identity passthrough for unmapped scopes.
 *
 * Anti-bluff (CONST-035): if ScopeMapper.treeSitterToVsCode is mutated to
 *   always return "", at least 15 of the assertions below FAIL.
 *
 *########################################################*/
package digital.vasic.yole.syntax

import digital.vasic.yole.syntax.grammar.ScopeMapper
import kotlin.test.Test
import kotlin.test.assertEquals

class ScopeMapperTest {

    @Test
    fun exactMatch_comment() {
        assertEquals("comment", ScopeMapper.treeSitterToVsCode("comment"))
    }

    @Test
    fun exactMatch_commentLine() {
        assertEquals("comment.line.double-slash", ScopeMapper.treeSitterToVsCode("comment.line"))
    }

    @Test
    fun exactMatch_commentBlock() {
        assertEquals("comment.block", ScopeMapper.treeSitterToVsCode("comment.block"))
    }

    @Test
    fun exactMatch_keyword() {
        assertEquals("keyword", ScopeMapper.treeSitterToVsCode("keyword"))
    }

    @Test
    fun exactMatch_keywordControl() {
        assertEquals("keyword.control", ScopeMapper.treeSitterToVsCode("keyword.control"))
    }

    @Test
    fun exactMatch_keywordOperator() {
        assertEquals("keyword.operator", ScopeMapper.treeSitterToVsCode("keyword.operator"))
    }

    @Test
    fun exactMatch_operatorAlias() {
        assertEquals("keyword.operator", ScopeMapper.treeSitterToVsCode("operator"))
    }

    @Test
    fun exactMatch_string() {
        assertEquals("string", ScopeMapper.treeSitterToVsCode("string"))
    }

    @Test
    fun exactMatch_stringEscape() {
        assertEquals(
            "constant.character.escape",
            ScopeMapper.treeSitterToVsCode("string.escape"),
        )
    }

    @Test
    fun exactMatch_constantNumeric() {
        assertEquals("constant.numeric", ScopeMapper.treeSitterToVsCode("constant.numeric"))
    }

    @Test
    fun exactMatch_numberAlias() {
        assertEquals("constant.numeric", ScopeMapper.treeSitterToVsCode("number"))
    }

    @Test
    fun exactMatch_booleanAlias() {
        assertEquals("constant.language.boolean", ScopeMapper.treeSitterToVsCode("boolean"))
    }

    @Test
    fun exactMatch_function() {
        assertEquals("entity.name.function", ScopeMapper.treeSitterToVsCode("function"))
    }

    @Test
    fun exactMatch_functionBuiltin() {
        assertEquals(
            "support.function.builtin",
            ScopeMapper.treeSitterToVsCode("function.builtin"),
        )
    }

    @Test
    fun exactMatch_methodAlias() {
        assertEquals("entity.name.function", ScopeMapper.treeSitterToVsCode("method"))
    }

    @Test
    fun exactMatch_type() {
        assertEquals("entity.name.type", ScopeMapper.treeSitterToVsCode("type"))
    }

    @Test
    fun exactMatch_variable() {
        assertEquals("variable", ScopeMapper.treeSitterToVsCode("variable"))
    }

    @Test
    fun exactMatch_variableParameter() {
        assertEquals(
            "variable.parameter",
            ScopeMapper.treeSitterToVsCode("variable.parameter"),
        )
    }

    @Test
    fun exactMatch_parameterAlias() {
        assertEquals("variable.parameter", ScopeMapper.treeSitterToVsCode("parameter"))
    }

    @Test
    fun exactMatch_markupHeading() {
        assertEquals("markup.heading", ScopeMapper.treeSitterToVsCode("heading"))
    }

    @Test
    fun exactMatch_heading1() {
        assertEquals("markup.heading.1", ScopeMapper.treeSitterToVsCode("heading.1"))
    }

    @Test
    fun exactMatch_strong() {
        assertEquals("markup.bold", ScopeMapper.treeSitterToVsCode("strong"))
    }

    @Test
    fun exactMatch_emphasis() {
        assertEquals("markup.italic", ScopeMapper.treeSitterToVsCode("emphasis"))
    }

    @Test
    fun exactMatch_link() {
        assertEquals("markup.underline.link", ScopeMapper.treeSitterToVsCode("link"))
    }

    @Test
    fun exactMatch_tag() {
        assertEquals("entity.name.tag", ScopeMapper.treeSitterToVsCode("tag"))
    }

    @Test
    fun hierarchicalFallback_keywordControlReturn() {
        // "keyword.control.return" not mapped → falls back to "keyword.control".
        assertEquals(
            "keyword.control",
            ScopeMapper.treeSitterToVsCode("keyword.control.return"),
        )
    }

    @Test
    fun hierarchicalFallback_keywordOperatorComparison() {
        // "keyword.operator.comparison" not mapped → falls back to "keyword.operator".
        assertEquals(
            "keyword.operator",
            ScopeMapper.treeSitterToVsCode("keyword.operator.comparison"),
        )
    }

    @Test
    fun hierarchicalFallback_functionBuiltinPrint() {
        // "function.builtin.print" not mapped → falls back to "function.builtin".
        assertEquals(
            "support.function.builtin",
            ScopeMapper.treeSitterToVsCode("function.builtin.print"),
        )
    }

    @Test
    fun hierarchicalFallback_deepNesting() {
        // 3-level deep fallback all the way to top-level "string".
        assertEquals("string", ScopeMapper.treeSitterToVsCode("string.foo.bar.baz"))
    }

    @Test
    fun identity_unmappedScope() {
        // No mapping at any level → input returned unchanged.
        assertEquals("totally.unknown.scope", ScopeMapper.treeSitterToVsCode("totally.unknown.scope"))
    }

    @Test
    fun identity_singleSegmentUnmapped() {
        assertEquals("xyz", ScopeMapper.treeSitterToVsCode("xyz"))
    }

    @Test
    fun identity_empty() {
        assertEquals("", ScopeMapper.treeSitterToVsCode(""))
    }
}
