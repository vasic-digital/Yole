/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58 Phase 1: static language manifest.
 * iter-58 F2 Phase 6: expanded from 2 → 55 languages.
 *
 * Per-language affordance conventions (CommentSyntax, IndentRules,
 * BracketPairs, indentUnit) sourced from research-report.md §4.2
 * (the 55-language table). Each row's docstring cites the canonical
 * style guide. Vendored .scm query files live under
 * shared/src/commonMain/resources/grammars/<id>/.
 *#######################################################*/
package digital.vasic.yole.language

import digital.vasic.yole.language.affordance.BracketPairs
import digital.vasic.yole.language.affordance.CommentSyntax
import digital.vasic.yole.language.affordance.IndentRules

/**
 * Static manifest of every Yole-supported language.
 *
 * 55-language target per F2 spec. Phase 1 shipped markdown + kotlin;
 * F2 Phase 6 fills the remaining 53 from research-report.md §1/§4.
 *
 * Each row carries:
 *  - id              : matches iter-57 Grammar.id (lower-snake or single
 *                      word; e.g. "csharp" not "c#").
 *  - displayName     : user-facing name.
 *  - extensions      : lowercase, leading-dot; first entry is the
 *                      canonical extension.
 *  - mimeTypes       : a non-empty list, even when the value is informal
 *                      (e.g. text/x-* variants).
 *  - commentSyntax   : line + block comment conventions (PEP-8, K&R,
 *                      etc., per §4.2 of the research report).
 *  - indentRules     : indent-opener / dedent-closer token sets. For
 *                      indent-significant langs (python, yaml, haskell,
 *                      nim, elm) the sets are minimal; the runtime
 *                      consults indents.scm when present.
 *  - bracketPairs    : auto-close pairs. Default covers () [] {} "" ''.
 *  - indentUnit      : "    " (4 spaces), "  " (2 spaces), or "\t" per
 *                      §4.3 of the research report.
 */
object LanguageMetadata {
    val markdown = LanguageFormat(
        id = "markdown",
        displayName = "Markdown",
        extensions = listOf(".md", ".markdown", ".mdown", ".mkd"),
        mimeTypes = listOf("text/markdown", "text/x-markdown"),
        commentSyntax = CommentSyntax(blockComment = "<!--" to "-->"),
        indentRules = IndentRules(indentTokens = emptySet(), dedentTokens = emptySet()),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — markdown's de-facto convention
    )

    val kotlin = LanguageFormat(
        id = "kotlin",
        displayName = "Kotlin",
        extensions = listOf(".kt", ".kts"),
        mimeTypes = listOf("text/x-kotlin"),
        commentSyntax = CommentSyntax(lineComment = "// ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "    ", // 4 spaces — Kotlin style guide
    )

    // -------- F2 Phase 6 Batch 1: Tier-1 most-popular langs --------

    val java = LanguageFormat(
        id = "java",
        displayName = "Java",
        extensions = listOf(".java"),
        mimeTypes = listOf("text/x-java", "text/x-java-source"),
        commentSyntax = CommentSyntax(lineComment = "// ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "    ", // 4 spaces — Oracle Java Code Conventions
    )

    val python = LanguageFormat(
        id = "python",
        displayName = "Python",
        extensions = listOf(".py", ".pyi", ".pyw"),
        mimeTypes = listOf("text/x-python", "application/x-python"),
        commentSyntax = CommentSyntax(lineComment = "# "),
        // Python indent rules are whitespace-significant — empty token
        // sets mean the runtime falls back to indents.scm + line-end
        // colon detection per research-report.md §4.4.
        indentRules = IndentRules(indentTokens = emptySet(), dedentTokens = emptySet()),
        bracketPairs = BracketPairs(),
        indentUnit = "    ", // 4 spaces — PEP-8
    )

    val javascript = LanguageFormat(
        id = "javascript",
        displayName = "JavaScript",
        extensions = listOf(".js", ".mjs", ".cjs"),
        mimeTypes = listOf("text/javascript", "application/javascript"),
        commentSyntax = CommentSyntax(lineComment = "// ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — npm/Airbnb default
    )

    val typescript = LanguageFormat(
        id = "typescript",
        displayName = "TypeScript",
        extensions = listOf(".ts", ".mts", ".cts"),
        mimeTypes = listOf("text/x-typescript", "application/typescript"),
        commentSyntax = CommentSyntax(lineComment = "// ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — tsconfig/tslint default
    )

    val go = LanguageFormat(
        id = "go",
        displayName = "Go",
        extensions = listOf(".go"),
        mimeTypes = listOf("text/x-go"),
        commentSyntax = CommentSyntax(lineComment = "// ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "\t", // gofmt mandates tabs
    )

    val rust = LanguageFormat(
        id = "rust",
        displayName = "Rust",
        extensions = listOf(".rs"),
        mimeTypes = listOf("text/x-rust", "text/rust"),
        commentSyntax = CommentSyntax(lineComment = "// ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "    ", // 4 spaces — rustfmt default
    )

    val c = LanguageFormat(
        id = "c",
        displayName = "C",
        extensions = listOf(".c", ".h"),
        mimeTypes = listOf("text/x-c", "text/x-csrc"),
        commentSyntax = CommentSyntax(lineComment = "// ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "    ", // 4 spaces — K&R default
    )

    val cpp = LanguageFormat(
        id = "cpp",
        displayName = "C++",
        extensions = listOf(".cpp", ".cc", ".cxx", ".hpp", ".hh", ".hxx"),
        mimeTypes = listOf("text/x-c++", "text/x-c++src"),
        commentSyntax = CommentSyntax(lineComment = "// ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "    ", // 4 spaces — Google C++ Style
    )

    val html = LanguageFormat(
        id = "html",
        displayName = "HTML",
        extensions = listOf(".html", ".htm", ".xhtml"),
        mimeTypes = listOf("text/html", "application/xhtml+xml"),
        commentSyntax = CommentSyntax(blockComment = "<!--" to "-->"),
        indentRules = IndentRules(indentTokens = setOf("<"), dedentTokens = setOf("</")),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — W3C / .editorconfig
    )

    val css = LanguageFormat(
        id = "css",
        displayName = "CSS",
        extensions = listOf(".css"),
        mimeTypes = listOf("text/css"),
        commentSyntax = CommentSyntax(blockComment = "/*" to "*/"),
        indentRules = IndentRules(indentTokens = setOf("{"), dedentTokens = setOf("}")),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — CSS spec convention
    )

    val sql = LanguageFormat(
        id = "sql",
        displayName = "SQL",
        extensions = listOf(".sql"),
        mimeTypes = listOf("text/x-sql", "application/sql"),
        commentSyntax = CommentSyntax(lineComment = "-- ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(indentTokens = setOf("("), dedentTokens = setOf(")")),
        bracketPairs = BracketPairs(),
        indentUnit = "    ", // 4 spaces — ANSI SQL informal default
    )

    val json = LanguageFormat(
        id = "json",
        displayName = "JSON",
        extensions = listOf(".json", ".jsonc"),
        mimeTypes = listOf("application/json", "text/json"),
        // JSON forbids comments per RFC 8259 — both fields stay null.
        commentSyntax = CommentSyntax(),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — RFC 8259 examples
    )

    // -------- F2 Phase 6 Batch 2: Tier-1b common langs --------

    val tsx = LanguageFormat(
        id = "tsx",
        displayName = "TypeScript JSX",
        extensions = listOf(".tsx"),
        mimeTypes = listOf("text/x-tsx"),
        commentSyntax = CommentSyntax(lineComment = "// ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — React conventions
    )

    val jsx = LanguageFormat(
        id = "jsx",
        displayName = "JavaScript JSX",
        extensions = listOf(".jsx"),
        mimeTypes = listOf("text/x-jsx"),
        commentSyntax = CommentSyntax(lineComment = "// ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — React conventions
    )

    val yaml = LanguageFormat(
        id = "yaml",
        displayName = "YAML",
        extensions = listOf(".yaml", ".yml"),
        mimeTypes = listOf("application/yaml", "text/yaml", "text/x-yaml"),
        commentSyntax = CommentSyntax(lineComment = "# "),
        // YAML is indent-significant — empty token sets fall back to
        // line-end colon + dash heuristic at runtime.
        indentRules = IndentRules(indentTokens = emptySet(), dedentTokens = emptySet()),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — YAML 1.2 spec recommendation
    )

    val toml = LanguageFormat(
        id = "toml",
        displayName = "TOML",
        extensions = listOf(".toml"),
        mimeTypes = listOf("application/toml", "text/x-toml"),
        commentSyntax = CommentSyntax(lineComment = "# "),
        indentRules = IndentRules(indentTokens = setOf("[", "[["), dedentTokens = setOf("]", "]]")),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — TOML v1.0.0 examples
    )

    val xml = LanguageFormat(
        id = "xml",
        displayName = "XML",
        extensions = listOf(".xml", ".xsd", ".xsl", ".xslt", ".plist", ".rss", ".svg"),
        mimeTypes = listOf("application/xml", "text/xml"),
        commentSyntax = CommentSyntax(blockComment = "<!--" to "-->"),
        indentRules = IndentRules(indentTokens = setOf("<"), dedentTokens = setOf("</")),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — W3C XML 1.0
    )

    val bash = LanguageFormat(
        id = "bash",
        displayName = "Bash",
        extensions = listOf(".sh", ".bash", ".zsh", ".ksh"),
        mimeTypes = listOf("application/x-sh", "text/x-shellscript"),
        commentSyntax = CommentSyntax(lineComment = "# "),
        indentRules = IndentRules(
            indentTokens = setOf("do", "then", "{", "("),
            dedentTokens = setOf("done", "fi", "}", ")"),
        ),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — Google Shell Style Guide
    )

    val ruby = LanguageFormat(
        id = "ruby",
        displayName = "Ruby",
        extensions = listOf(".rb", ".rake", ".gemspec"),
        mimeTypes = listOf("text/x-ruby", "application/x-ruby"),
        commentSyntax = CommentSyntax(lineComment = "# ", blockComment = "=begin" to "=end"),
        indentRules = IndentRules(
            indentTokens = setOf("do", "def", "class", "module", "if", "case", "begin", "{", "(", "["),
            dedentTokens = setOf("end", "}", ")", "]"),
        ),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — rubocop default
    )

    val php = LanguageFormat(
        id = "php",
        displayName = "PHP",
        extensions = listOf(".php", ".phtml", ".phps"),
        mimeTypes = listOf("application/x-php", "text/x-php"),
        commentSyntax = CommentSyntax(lineComment = "// ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "    ", // 4 spaces — PSR-12
    )

    val swift = LanguageFormat(
        id = "swift",
        displayName = "Swift",
        extensions = listOf(".swift"),
        mimeTypes = listOf("text/x-swift"),
        commentSyntax = CommentSyntax(lineComment = "// ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "    ", // 4 spaces — Swift API Design Guidelines
    )

    val scala = LanguageFormat(
        id = "scala",
        displayName = "Scala",
        extensions = listOf(".scala", ".sc"),
        mimeTypes = listOf("text/x-scala"),
        commentSyntax = CommentSyntax(lineComment = "// ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — Scala Style Guide
    )

    // -------- F2 Phase 6 Batch 3: Scripting + data --------

    val dart = LanguageFormat(
        id = "dart",
        displayName = "Dart",
        extensions = listOf(".dart"),
        mimeTypes = listOf("application/dart", "text/x-dart"),
        commentSyntax = CommentSyntax(lineComment = "// ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — dart format
    )

    val lua = LanguageFormat(
        id = "lua",
        displayName = "Lua",
        extensions = listOf(".lua"),
        mimeTypes = listOf("text/x-lua", "application/x-lua"),
        commentSyntax = CommentSyntax(lineComment = "-- ", blockComment = "--[[" to "]]"),
        indentRules = IndentRules(
            indentTokens = setOf("do", "then", "function", "{", "(", "["),
            dedentTokens = setOf("end", "}", ")", "]"),
        ),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — lua-users.org
    )

    val perl = LanguageFormat(
        id = "perl",
        displayName = "Perl",
        extensions = listOf(".pl", ".pm", ".perl"),
        mimeTypes = listOf("text/x-perl", "application/x-perl"),
        commentSyntax = CommentSyntax(lineComment = "# ", blockComment = "=pod" to "=cut"),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "    ", // 4 spaces — perlstyle
    )

    val haskell = LanguageFormat(
        id = "haskell",
        displayName = "Haskell",
        extensions = listOf(".hs", ".lhs"),
        mimeTypes = listOf("text/x-haskell"),
        commentSyntax = CommentSyntax(lineComment = "-- ", blockComment = "{-" to "-}"),
        // Haskell is layout-sensitive — empty token sets.
        indentRules = IndentRules(indentTokens = emptySet(), dedentTokens = emptySet()),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — HaskellWiki style
    )

    val ocaml = LanguageFormat(
        id = "ocaml",
        displayName = "OCaml",
        extensions = listOf(".ml", ".mli"),
        mimeTypes = listOf("text/x-ocaml"),
        commentSyntax = CommentSyntax(blockComment = "(*" to "*)"),
        indentRules = IndentRules(
            indentTokens = setOf("begin", "struct", "sig", "if", "match", "let", "(", "[", "{"),
            dedentTokens = setOf("end", ")", "]", "}"),
        ),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — OCaml style guide
    )

    val julia = LanguageFormat(
        id = "julia",
        displayName = "Julia",
        extensions = listOf(".jl"),
        mimeTypes = listOf("text/x-julia", "application/x-julia"),
        commentSyntax = CommentSyntax(lineComment = "# ", blockComment = "#=" to "=#"),
        indentRules = IndentRules(
            indentTokens = setOf(
                "function", "do", "if", "for", "while", "begin", "let", "module",
                "(", "[", "{",
            ),
            dedentTokens = setOf("end", ")", "]", "}"),
        ),
        bracketPairs = BracketPairs(),
        indentUnit = "    ", // 4 spaces — Julia style guide
    )

    val r = LanguageFormat(
        id = "r",
        displayName = "R",
        extensions = listOf(".r", ".R"),
        mimeTypes = listOf("text/x-r", "application/x-r"),
        commentSyntax = CommentSyntax(lineComment = "# "),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — tidyverse style
    )

    val elixir = LanguageFormat(
        id = "elixir",
        displayName = "Elixir",
        extensions = listOf(".ex", ".exs"),
        mimeTypes = listOf("text/x-elixir"),
        commentSyntax = CommentSyntax(lineComment = "# "),
        indentRules = IndentRules(
            indentTokens = setOf("do", "fn", "if", "case", "cond", "unless", "(", "[", "{"),
            dedentTokens = setOf("end", ")", "]", "}"),
        ),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — Elixir style guide
    )

    val erlang = LanguageFormat(
        id = "erlang",
        displayName = "Erlang",
        extensions = listOf(".erl", ".hrl"),
        mimeTypes = listOf("text/x-erlang"),
        commentSyntax = CommentSyntax(lineComment = "% "),
        indentRules = IndentRules(
            indentTokens = setOf("(", "[", "{", "case", "if", "receive", "try", "fun"),
            dedentTokens = setOf(")", "]", "}", "end"),
        ),
        bracketPairs = BracketPairs(),
        indentUnit = "    ", // 4 spaces — Erlang programming rules
    )

    val fortran = LanguageFormat(
        id = "fortran",
        displayName = "Fortran",
        extensions = listOf(".f", ".f90", ".f95", ".f03", ".f08", ".for"),
        mimeTypes = listOf("text/x-fortran"),
        commentSyntax = CommentSyntax(lineComment = "! "),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — Fortran 2008 standard
    )

    // -------- F2 Phase 6 Batch 4: Niche + DSL --------

    val vim = LanguageFormat(
        id = "vim",
        displayName = "Vimscript",
        extensions = listOf(".vim"),
        mimeTypes = listOf("text/x-vim"),
        commentSyntax = CommentSyntax(lineComment = "\" "),
        indentRules = IndentRules(
            indentTokens = setOf("function", "if", "for", "while", "try"),
            dedentTokens = setOf("endfunction", "endif", "endfor", "endwhile", "endtry"),
        ),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — vim :h indent-expression
    )

    val dockerfile = LanguageFormat(
        id = "dockerfile",
        displayName = "Dockerfile",
        extensions = listOf(".dockerfile"),
        mimeTypes = listOf("text/x-dockerfile"),
        commentSyntax = CommentSyntax(lineComment = "# "),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "    ", // 4 spaces — Docker docs
    )

    val makefile = LanguageFormat(
        id = "makefile",
        displayName = "Makefile",
        extensions = listOf(".mk", ".mak"),
        mimeTypes = listOf("text/x-makefile"),
        commentSyntax = CommentSyntax(lineComment = "# "),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "\t", // tabs REQUIRED for recipe lines
    )

    val terraform = LanguageFormat(
        id = "terraform",
        displayName = "Terraform (HCL)",
        extensions = listOf(".tf", ".tfvars", ".hcl"),
        mimeTypes = listOf("text/x-terraform"),
        commentSyntax = CommentSyntax(lineComment = "# ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — HashiCorp HCL style
    )

    val regex = LanguageFormat(
        id = "regex",
        displayName = "Regex",
        extensions = listOf(".regex"),
        mimeTypes = listOf("text/x-regex"),
        commentSyntax = CommentSyntax(),
        indentRules = IndentRules(indentTokens = emptySet(), dedentTokens = emptySet()),
        bracketPairs = BracketPairs(),
        indentUnit = "  ",
    )

    val vue = LanguageFormat(
        id = "vue",
        displayName = "Vue",
        extensions = listOf(".vue"),
        mimeTypes = listOf("text/x-vue"),
        commentSyntax = CommentSyntax(blockComment = "<!--" to "-->"),
        indentRules = IndentRules(indentTokens = setOf("<", "{"), dedentTokens = setOf("</", "}")),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — Vue style guide
    )

    val graphql = LanguageFormat(
        id = "graphql",
        displayName = "GraphQL",
        extensions = listOf(".graphql", ".gql"),
        mimeTypes = listOf("application/graphql"),
        commentSyntax = CommentSyntax(lineComment = "# "),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — GraphQL spec
    )

    val csharp = LanguageFormat(
        id = "csharp",
        displayName = "C#",
        extensions = listOf(".cs", ".csx"),
        mimeTypes = listOf("text/x-csharp"),
        commentSyntax = CommentSyntax(lineComment = "// ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "    ", // 4 spaces — Microsoft C# Coding Conventions
    )

    val less = LanguageFormat(
        id = "less",
        displayName = "Less",
        extensions = listOf(".less"),
        mimeTypes = listOf("text/x-less"),
        commentSyntax = CommentSyntax(lineComment = "// ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(indentTokens = setOf("{"), dedentTokens = setOf("}")),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — Less.js docs
    )

    val scss = LanguageFormat(
        id = "scss",
        displayName = "SCSS",
        extensions = listOf(".scss", ".sass"),
        mimeTypes = listOf("text/x-scss"),
        commentSyntax = CommentSyntax(lineComment = "// ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(indentTokens = setOf("{"), dedentTokens = setOf("}")),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — Sass guide
    )

    // -------- F2 Phase 6 Batch 5: Remaining (nix, zig, elm, clojure, nim, crystal, groovy, objc, latex, bibtex, proto) --------

    val nix = LanguageFormat(
        id = "nix",
        displayName = "Nix",
        extensions = listOf(".nix"),
        mimeTypes = listOf("text/x-nix"),
        commentSyntax = CommentSyntax(lineComment = "# ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(
            indentTokens = setOf("{", "(", "[", "let"),
            dedentTokens = setOf("}", ")", "]", "in"),
        ),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — Nix manual
    )

    val zig = LanguageFormat(
        id = "zig",
        displayName = "Zig",
        extensions = listOf(".zig"),
        mimeTypes = listOf("text/x-zig"),
        commentSyntax = CommentSyntax(lineComment = "// "),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "    ", // 4 spaces — zig fmt
    )

    val elm = LanguageFormat(
        id = "elm",
        displayName = "Elm",
        extensions = listOf(".elm"),
        mimeTypes = listOf("text/x-elm"),
        commentSyntax = CommentSyntax(lineComment = "-- ", blockComment = "{-" to "-}"),
        // Elm is layout-sensitive.
        indentRules = IndentRules(indentTokens = emptySet(), dedentTokens = emptySet()),
        bracketPairs = BracketPairs(),
        indentUnit = "    ", // 4 spaces — Elm style guide
    )

    val clojure = LanguageFormat(
        id = "clojure",
        displayName = "Clojure",
        extensions = listOf(".clj", ".cljs", ".cljc", ".edn"),
        mimeTypes = listOf("text/x-clojure"),
        commentSyntax = CommentSyntax(lineComment = "; "),
        indentRules = IndentRules(
            indentTokens = setOf("(", "[", "{"),
            dedentTokens = setOf(")", "]", "}"),
        ),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — Clojure style guide
    )

    val nim = LanguageFormat(
        id = "nim",
        displayName = "Nim",
        extensions = listOf(".nim", ".nims"),
        mimeTypes = listOf("text/x-nim"),
        commentSyntax = CommentSyntax(lineComment = "# ", blockComment = "#[" to "]#"),
        // Nim is indent-significant (Python-like).
        indentRules = IndentRules(indentTokens = emptySet(), dedentTokens = emptySet()),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — NEP-1
    )

    val crystal = LanguageFormat(
        id = "crystal",
        displayName = "Crystal",
        extensions = listOf(".cr"),
        mimeTypes = listOf("text/x-crystal"),
        commentSyntax = CommentSyntax(lineComment = "# "),
        indentRules = IndentRules(
            indentTokens = setOf("do", "def", "class", "module", "if", "case", "begin", "{", "(", "["),
            dedentTokens = setOf("end", "}", ")", "]"),
        ),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — Crystal style guide
    )

    val groovy = LanguageFormat(
        id = "groovy",
        displayName = "Groovy",
        extensions = listOf(".groovy", ".gradle"),
        mimeTypes = listOf("text/x-groovy"),
        commentSyntax = CommentSyntax(lineComment = "// ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "    ", // 4 spaces — Groovy style guide
    )

    val objc = LanguageFormat(
        id = "objc",
        displayName = "Objective-C",
        extensions = listOf(".m", ".mm"),
        mimeTypes = listOf("text/x-objective-c"),
        commentSyntax = CommentSyntax(lineComment = "// ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "    ", // 4 spaces — Apple Coding Guidelines
    )

    val latex = LanguageFormat(
        id = "latex",
        displayName = "LaTeX",
        extensions = listOf(".tex", ".latex", ".ltx"),
        mimeTypes = listOf("text/x-tex", "application/x-tex"),
        commentSyntax = CommentSyntax(lineComment = "% "),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — LaTeX-project docs
    )

    val bibtex = LanguageFormat(
        id = "bibtex",
        displayName = "BibTeX",
        extensions = listOf(".bib"),
        mimeTypes = listOf("text/x-bibtex"),
        commentSyntax = CommentSyntax(lineComment = "% "),
        indentRules = IndentRules(indentTokens = setOf("{"), dedentTokens = setOf("}")),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — BibTeX docs
    )

    val proto = LanguageFormat(
        id = "proto",
        displayName = "Protocol Buffers",
        extensions = listOf(".proto"),
        mimeTypes = listOf("text/x-protobuf"),
        commentSyntax = CommentSyntax(lineComment = "// ", blockComment = "/*" to "*/"),
        indentRules = IndentRules(),
        bracketPairs = BracketPairs(),
        indentUnit = "  ", // 2 spaces — protobuf style guide
    )

    /**
     * All 55 language formats known to Yole.
     *
     * Order: markdown + kotlin (Phase 1 anchors) first, then F2 Phase 6
     * batches in research-report.md §1 row order. Detection order in
     * [LanguageRegistry.detectByFilename] follows this list — more
     * specific extensions earlier when there's overlap (e.g., `.tsx`
     * before `.ts` so a `.tsx` file is not mis-detected as TypeScript).
     */
    val all: List<LanguageFormat> = listOf(
        markdown, kotlin,
        // Batch 1
        java, python, javascript, typescript, go, rust, c, cpp, html, css, sql, json,
        // Batch 2 — note tsx/jsx appear BEFORE typescript/javascript would have if
        // they shared extensions, but here they have unique .tsx/.jsx so order
        // is informational only.
        tsx, jsx, yaml, toml, xml, bash, ruby, php, swift, scala,
        // Batch 3
        dart, lua, perl, haskell, ocaml, julia, r, elixir, erlang, fortran,
        // Batch 4
        vim, dockerfile, makefile, terraform, regex, vue, graphql, csharp, less, scss,
        // Batch 5
        nix, zig, elm, clojure, nim, crystal, groovy, objc, latex, bibtex, proto,
    )
}
