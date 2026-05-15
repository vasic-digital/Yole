/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 F2 Phase 7 — bonede grammar smoke test (desktopMain only).
 *
 * Verifies that for EVERY language with a bonede artifact wired in
 * shared/build.gradle.kts, the TokenizerEngine can:
 *   1. Resolve the FQCN from BonedeGrammarRegistry.
 *   2. Class.forName + newInstance the bonede TSLanguage subclass.
 *   3. Drive a TSParser through a trivial source snippet.
 *   4. Get back >= 1 leaf token (i.e. a real parse, not an empty tree).
 *
 * If ANY of those steps fails for any language, this test FAILS —
 * and the user-visible feature gap is surfaced. We do NOT silently
 * skip langs that fail (CONST-035 anti-bluff). The 7 langs with no
 * bonede artifact (jsx, xml, vim, less, crystal, groovy, bibtex) are
 * intentionally excluded from this test — they're tracked in
 * KNOWN_DEFECTS#f2-phase-7-no-bonede-artifact.
 *
 * Anti-bluff anchors:
 *   - Mutation: stub TokenizerEngine.tokenize to return emptyList()
 *     -> EVERY assertion in this test FAILS (47 separate failures).
 *   - Mutation: delete BonedeGrammarRegistry entry for any lang
 *     -> that lang's test FAILS with "no bonede artifact".
 *   - Mutation: remove a libs.ts.<lang> from shared/build.gradle.kts
 *     -> compile-time failure (test won't even build).
 *
 *########################################################*/
package digital.vasic.yole.syntax

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

class BonedeGrammarSmokeTest {

    /**
     * Minimal source snippets per language. Each must be syntactically
     * valid enough for a Tree-Sitter parser to produce at least one
     * non-trivial child node (Tree-Sitter is permissive — even garbage
     * input yields a tree with ERROR nodes, but we want PRODUCTION
     * nodes here to prove the grammar is doing real work).
     *
     * Snippets are intentionally short so the test runs in <30s total.
     */
    private val snippets: Map<String, String> = mapOf(
        "markdown"   to "# Hello\n\nWorld.\n",
        "kotlin"     to "fun main() { println(\"hi\") }\n",
        "java"       to "class A { void m() {} }\n",
        "python"     to "def f(x):\n    return x + 1\n",
        "javascript" to "function f(x) { return x + 1; }\n",
        "typescript" to "function f(x: number): number { return x + 1; }\n",
        "go"         to "package main\nfunc main() { println(\"hi\") }\n",
        "rust"       to "fn main() { println!(\"hi\"); }\n",
        "c"          to "int main(void) { return 0; }\n",
        "cpp"        to "int main() { return 0; }\n",
        "html"       to "<html><body><p>hi</p></body></html>\n",
        "css"        to "body { color: red; }\n",
        "sql"        to "SELECT id, name FROM users WHERE id = 1;\n",
        "json"       to "{\"a\": 1, \"b\": [2, 3]}\n",
        "tsx"        to "const A = () => <div>hi</div>;\n",
        "yaml"       to "a: 1\nb:\n  - 2\n  - 3\n",
        "toml"       to "[section]\nkey = \"value\"\n",
        "bash"       to "echo hello world\nfor i in 1 2 3; do echo \$i; done\n",
        "ruby"       to "def f(x)\n  x + 1\nend\n",
        "php"        to "<?php\nfunction f(\$x) { return \$x + 1; }\n",
        "swift"      to "func f(_ x: Int) -> Int { return x + 1 }\n",
        "scala"      to "object A { def m(x: Int): Int = x + 1 }\n",
        "dart"       to "void main() { print('hi'); }\n",
        "lua"        to "local function f(x) return x + 1 end\nprint(f(2))\n",
        "perl"       to "sub f { my (\$x) = @_; return \$x + 1; }\n",
        "haskell"    to "f :: Int -> Int\nf x = x + 1\n",
        "ocaml"      to "let f x = x + 1\nlet () = print_int (f 2)\n",
        "julia"      to "function f(x)\n  x + 1\nend\n",
        "r"          to "f <- function(x) { x + 1 }\nprint(f(2))\n",
        "elixir"     to "defmodule M do\n  def f(x), do: x + 1\nend\n",
        "erlang"     to "-module(m).\n-export([f/1]).\nf(X) -> X + 1.\n",
        "fortran"    to "program p\n  integer :: x\n  x = 1\nend program p\n",
        "dockerfile" to "FROM alpine:latest\nRUN echo hi\nCMD [\"sh\"]\n",
        "makefile"   to "all: hello\n\nhello:\n\techo hi\n",
        "terraform"  to "resource \"aws_instance\" \"a\" {\n  ami = \"x\"\n}\n",
        "regex"      to "[a-z]+\\d*",
        "vue"        to "<template><div>hi</div></template>\n",
        "graphql"    to "type Query { hello: String }\n",
        "csharp"     to "class A { void M() {} }\n",
        "scss"       to ".a { color: red; .b { color: blue; } }\n",
        "nix"        to "{ pkgs ? import <nixpkgs> {} }: pkgs.hello\n",
        "zig"        to "pub fn main() void { _ = 42; }\n",
        "elm"        to "module M exposing (..)\nf x = x + 1\n",
        "clojure"    to "(defn f [x] (+ x 1))\n",
        // nim deliberately omitted — KNOWN_DEFECTS#f2-phase-7-nim-grammar-broken.
        "objc"       to "#import <Foundation/Foundation.h>\nint main() { return 0; }\n",
        "latex"      to "\\documentclass{article}\n\\begin{document}\nHi\n\\end{document}\n",
        "proto"      to "syntax = \"proto3\";\nmessage M { string name = 1; }\n",
    )

    @Before
    fun setUp() {
        EnabledFormatGate.setEnabled(BonedeGrammarRegistry.supportedLangs)
    }

    @After
    fun tearDown() {
        EnabledFormatGate.setEnabled(setOf("markdown"))
    }

    @Test
    fun allBundledLangs_loadAndParse() = runBlocking<Unit> {
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()

        val failures = mutableListOf<String>()
        val successes = mutableListOf<String>()

        // BonedeGrammarRegistry.supportedLangs is the source of truth
        // for which langs we claim to support. Every entry MUST have a
        // smoke snippet, and every snippet MUST produce >= 1 token.
        val supported = BonedeGrammarRegistry.supportedLangs
        for (lang in supported.sorted()) {
            val snippet = snippets[lang]
            if (snippet == null) {
                failures += "$lang: NO SMOKE SNIPPET (programmer error in this test)"
                continue
            }
            try {
                engine.loadGrammar(lang)
                val tokens = engine.tokenize(snippet, lang)
                if (tokens.isEmpty()) {
                    failures += "$lang: tokenize returned 0 tokens for snippet"
                } else {
                    successes += "$lang(${tokens.size})"
                }
            } catch (t: Throwable) {
                failures += "$lang: ${t.javaClass.simpleName}: ${t.message?.take(160)}"
            }
        }

        // Build a positive-evidence message even when test passes — this
        // is the anti-bluff anchor visible in CI logs: it names every
        // lang that actually parsed, not just "OK".
        val report = buildString {
            appendLine(
                "Phase 7 bonede smoke: " +
                    "${successes.size}/${supported.size} langs parsed >= 1 token."
            )
            appendLine("  successes: ${successes.joinToString(", ")}")
            if (failures.isNotEmpty()) {
                appendLine("  failures:")
                failures.forEach { appendLine("    - $it") }
            }
        }
        println(report)

        if (failures.isNotEmpty()) {
            fail(report)
        }
        // Positive evidence: more than half of advertised langs got a real parse.
        assertTrue(
            "expected all ${supported.size} bundled langs to parse; got ${successes.size}",
            successes.size == supported.size,
        )
    }

    @Test
    fun unsupportedLangs_throwHonestly() = runBlocking<Unit> {
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        // The 7 unsupported langs MUST throw — they cannot have a
        // bonede grammar today. If any of them silently succeeded, that
        // would be a sign someone synthesised a stub TSLanguage
        // (CONST-035 violation).
        for (lang in BonedeGrammarRegistry.unsupportedLangs) {
            EnabledFormatGate.setEnabled(setOf(lang))
            var threw = false
            try {
                engine.loadGrammar(lang)
            } catch (_: IllegalArgumentException) {
                threw = true
            } catch (_: IllegalStateException) {
                threw = true
            }
            assertTrue(
                "lang `$lang` MUST throw on loadGrammar (no bonede artifact)",
                threw,
            )
        }
    }

    @Test
    fun bonedeRegistry_isComplete() {
        // Defensive: the count constant on BonedeGrammarRegistry must
        // match the size of supportedLangs. Catches accidental copy-
        // paste typos in the map literal.
        assertNotNull(BonedeGrammarRegistry.supportedLangs)
        val expected = BonedeGrammarRegistry.supportedLangs.size
        assertTrue(
            "supportedCount=$expected expected to be 47 (Phase 7 inventory: " +
                "48 bonede artifacts minus nim which segfaults — see " +
                "KNOWN_DEFECTS#f2-phase-7-nim-grammar-broken)",
            expected == 47,
        )
    }
}
