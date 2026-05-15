/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 F2 Phase 7 — bonede grammar JAR class-name registry.
 *
 * Each io.github.bonede:tree-sitter-<lang> JAR ships a single public
 * class `org.treesitter.TreeSitter<TitleCase>` extending TSLanguage,
 * with a zero-arg constructor that triggers the JAR's
 * NativeUtils.loadLib("lib/tree-sitter-<base>") flow and returns a
 * usable TSLanguage instance.
 *
 * This registry maps Yole language IDs to those class names. The
 * mapping is mostly identity ("python" → "TreeSitterPython") but
 * some languages have artifact-name vs Yole-id mismatches:
 *   - csharp     → TreeSitterCSharp  (bonede artifact tree-sitter-c-sharp)
 *   - terraform  → TreeSitterHcl     (bonede artifact tree-sitter-hcl)
 *   - makefile   → TreeSitterMake    (bonede artifact tree-sitter-make)
 *   - cpp        → TreeSitterCpp     (TitleCase, not "CPP")
 *
 * Anti-bluff (CONST-035): unknown language IDs return null; the
 * TokenizerEngine then throws IllegalArgumentException rather than
 * faking a grammar instance. Languages in the 7-lang gap set
 * (jsx, xml, vim, less, crystal, groovy, bibtex) are intentionally
 * absent from this map — they have no bonede artifact.
 *
 *########################################################*/
package digital.vasic.yole.syntax

internal object BonedeGrammarRegistry {

    /**
     * Yole language ID -> fully-qualified bonede TSLanguage subclass.
     * Each entry corresponds to a bonede artifact declared in
     * shared/build.gradle.kts desktopMain dependencies block.
     * Verified to load on x86_64-macos + aarch64-macos via
     * `tools/build-language-grammars.sh extract` + `verify` on
     * 2026-05-15.
     */
    private val classNames: Map<String, String> = mapOf(
        "markdown"   to "org.treesitter.TreeSitterMarkdown",
        "kotlin"     to "org.treesitter.TreeSitterKotlin",
        "java"       to "org.treesitter.TreeSitterJava",
        "python"     to "org.treesitter.TreeSitterPython",
        "javascript" to "org.treesitter.TreeSitterJavascript",
        "typescript" to "org.treesitter.TreeSitterTypescript",
        "go"         to "org.treesitter.TreeSitterGo",
        "rust"       to "org.treesitter.TreeSitterRust",
        "c"          to "org.treesitter.TreeSitterC",
        "cpp"        to "org.treesitter.TreeSitterCpp",
        "html"       to "org.treesitter.TreeSitterHtml",
        "css"        to "org.treesitter.TreeSitterCss",
        "sql"        to "org.treesitter.TreeSitterSql",
        "json"       to "org.treesitter.TreeSitterJson",
        "tsx"        to "org.treesitter.TreeSitterTsx",
        "yaml"       to "org.treesitter.TreeSitterYaml",
        "toml"       to "org.treesitter.TreeSitterToml",
        "bash"       to "org.treesitter.TreeSitterBash",
        "ruby"       to "org.treesitter.TreeSitterRuby",
        "php"        to "org.treesitter.TreeSitterPhp",
        "swift"      to "org.treesitter.TreeSitterSwift",
        "scala"      to "org.treesitter.TreeSitterScala",
        "dart"       to "org.treesitter.TreeSitterDart",
        "lua"        to "org.treesitter.TreeSitterLua",
        "perl"       to "org.treesitter.TreeSitterPerl",
        "haskell"    to "org.treesitter.TreeSitterHaskell",
        "ocaml"      to "org.treesitter.TreeSitterOcaml",
        "julia"      to "org.treesitter.TreeSitterJulia",
        "r"          to "org.treesitter.TreeSitterR",
        "elixir"     to "org.treesitter.TreeSitterElixir",
        "erlang"     to "org.treesitter.TreeSitterErlang",
        "fortran"    to "org.treesitter.TreeSitterFortran",
        "dockerfile" to "org.treesitter.TreeSitterDockerfile",
        "makefile"   to "org.treesitter.TreeSitterMake",
        "terraform"  to "org.treesitter.TreeSitterHcl",
        "regex"      to "org.treesitter.TreeSitterRegex",
        "vue"        to "org.treesitter.TreeSitterVue",
        "graphql"    to "org.treesitter.TreeSitterGraphql",
        "csharp"     to "org.treesitter.TreeSitterCSharp",
        "scss"       to "org.treesitter.TreeSitterScss",
        "nix"        to "org.treesitter.TreeSitterNix",
        "zig"        to "org.treesitter.TreeSitterZig",
        "elm"        to "org.treesitter.TreeSitterElm",
        "clojure"    to "org.treesitter.TreeSitterClojure",
        // nim deliberately excluded — see KNOWN_DEFECTS#f2-phase-7-nim-grammar-broken.
        // It's moved to unsupportedLangs below.
        "objc"       to "org.treesitter.TreeSitterObjc",
        "latex"      to "org.treesitter.TreeSitterLatex",
        "proto"      to "org.treesitter.TreeSitterProto",
    )

    /**
     * Yole language IDs with no working bonede artifact today:
     *  - jsx, xml, vim, less, crystal, groovy, bibtex
     *      → no published bonede JAR exists.
     *  - nim
     *      → bonede JAR exists (tree-sitter-nim 0.5.0 + 0.6.0) but the
     *        native .so segfaults on parse against all bonede cores
     *        tried (0.24.4, 0.25.3, 0.26.6). See
     *        docs/KNOWN_DEFECTS.md#f2-phase-7-nim-grammar-broken.
     *
     * Languages here MUST NOT be in [classNames] above.
     */
    val unsupportedLangs: Set<String> = setOf(
        "jsx", "xml", "vim", "less", "crystal", "groovy", "bibtex", "nim",
    )

    /** Number of languages with a working bonede grammar (= 47). */
    val supportedCount: Int get() = classNames.size

    /**
     * Returns the fully-qualified bonede class name for [lang], or
     * null if no bonede artifact is bundled for this language.
     * Callers must treat null as "fall back to a no-Tree-Sitter
     * code path" — do NOT fabricate a TSLanguage stub (CONST-035).
     */
    fun classNameFor(lang: String): String? = classNames[lang]

    /** All Yole language IDs supported by the bonede path. */
    val supportedLangs: Set<String> get() = classNames.keys
}
