/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * WikiText Parser Performance Benchmarks
 * Establishes baseline performance metrics for WikiText (MediaWiki) parsing
 *
 *########################################################*/
package digital.vasic.yole.format.benchmark

import digital.vasic.yole.format.wikitext.WikitextParser
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

/**
 * Performance benchmarks for WikiText (MediaWiki) parser.
 *
 * Tests parser performance across different document sizes and complexity levels.
 *
 * Performance Targets:
 * - Small documents (~2KB): < 20ms
 * - Medium documents (~20KB): < 100ms
 * - Large documents (~200KB): < 1000ms
 * - Complex documents: < 50ms
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class WikitextParserBenchmark {

    private val parser = WikitextParser()

    // Test data
    private lateinit var smallDocument: String
    private lateinit var mediumDocument: String
    private lateinit var largeDocument: String
    private lateinit var complexDocument: String

    @Setup
    fun setup() {
        // Small WikiText document (~2KB)
        smallDocument = generateSmallWikitextDocument()

        // Medium WikiText document (~20KB) - typical Wikipedia article
        mediumDocument = generateMediumWikitextDocument()

        // Large WikiText document (~200KB) - comprehensive Wikipedia page
        largeDocument = generateLargeWikitextDocument()

        // Complex WikiText document with advanced features
        complexDocument = generateComplexWikitextDocument()
    }

    @Benchmark
    fun parseSmallDocument() {
        parser.parse(smallDocument, emptyMap())
    }

    @Benchmark
    fun parseMediumDocument() {
        parser.parse(mediumDocument, emptyMap())
    }

    @Benchmark
    fun parseLargeDocument() {
        parser.parse(largeDocument, emptyMap())
    }

    @Benchmark
    fun parseComplexDocument() {
        parser.parse(complexDocument, emptyMap())
    }

    @Benchmark
    fun convertToHtml() {
        val document = parser.parse(mediumDocument, emptyMap())
        parser.toHtml(document, lightMode = false)
    }

    @Benchmark
    fun validateDocument() {
        parser.validate(mediumDocument)
    }

    // Test data generators

    private fun generateSmallWikitextDocument(): String = buildString {
        appendLine("== Simple WikiText Article ==")
        appendLine()
        appendLine("This is a '''simple''' WikiText document.")
        appendLine()
        appendLine("=== Introduction ===")
        appendLine()
        appendLine("WikiText is the markup language used by ''Wikipedia'' and other MediaWiki sites.")
        appendLine()
        appendLine("=== Features ===")
        appendLine()
        appendLine("* First feature")
        appendLine("* Second feature")
        appendLine("* Third feature")
        appendLine()
        appendLine("=== Code Example ===")
        appendLine()
        appendLine("<syntaxhighlight lang=\"kotlin\">")
        appendLine("fun main() {")
        appendLine("    println(\"Hello, WikiText!\")")
        appendLine("}")
        appendLine("</syntaxhighlight>")
        appendLine()
        appendLine("=== External Links ===")
        appendLine()
        appendLine("* [https://www.mediawiki.org MediaWiki]")
        appendLine("* [https://www.wikipedia.org Wikipedia]")
    }

    private fun generateMediumWikitextDocument(): String = buildString {
        appendLine("== MediaWiki Documentation ==")
        appendLine()
        appendLine("{{Infobox software")
        appendLine("| name = MediaWiki")
        appendLine("| logo = MediaWiki_logo.svg")
        appendLine("| developer = MediaWiki developers")
        appendLine("| released = {{Start date|2002|01|25}}")
        appendLine("}}")
        appendLine()
        appendLine("'''MediaWiki''' is a [[free software|free]] and [[open-source software|open-source]] [[wiki]] engine.")
        appendLine()

        // Generate 10 sections
        for (i in 1..10) {
            appendLine("=== Section $i: Feature Overview ===")
            appendLine()
            appendLine("This section describes feature $i of the system.")
            appendLine()
            appendLine("==== Description ====")
            appendLine()
            appendLine("Feature $i provides:")
            appendLine()
            appendLine("* '''Capability ${i}.1''' - Core functionality")
            appendLine("* '''Capability ${i}.2''' - Advanced features")
            appendLine("* '''Capability ${i}.3''' - Integration options")
            appendLine("* '''Capability ${i}.4''' - Performance enhancements")
            appendLine()
            appendLine("==== Implementation ====")
            appendLine()
            appendLine("<syntaxhighlight lang=\"kotlin\">")
            appendLine("class Feature$i {")
            appendLine("    fun execute() {")
            appendLine("        // Implementation details")
            appendLine("        println(\"Executing feature $i\")")
            appendLine("    }")
            appendLine("}")
            appendLine("</syntaxhighlight>")
            appendLine()
            appendLine("==== Configuration ====")
            appendLine()
            appendLine("{| class=\"wikitable\"")
            appendLine("! Option !! Description !! Default")
            appendLine("|-")
            appendLine("| enabled || Enable feature $i || true")
            appendLine("|-")
            appendLine("| timeout || Execution timeout || 30s")
            appendLine("|-")
            appendLine("| retries || Number of retries || 3")
            appendLine("|}")
            appendLine()
            appendLine("{{Note|Always test configuration changes in a staging environment.}}")
            appendLine()
            appendLine("==== Examples ====")
            appendLine()
            appendLine("<syntaxhighlight lang=\"kotlin\">")
            appendLine("val feature = Feature$i()")
            appendLine("feature.execute()")
            appendLine("</syntaxhighlight>")
            appendLine()
            appendLine("==== See Also ====")
            appendLine()
            appendLine("* [[Feature ${i-1}]]")
            appendLine("* [[Feature ${i+1}]]")
            appendLine("* [[Configuration Guide]]")
            appendLine()
        }

        appendLine("== References ==")
        appendLine()
        appendLine("<references />")
        appendLine()
        appendLine("== External Links ==")
        appendLine()
        appendLine("* [https://www.mediawiki.org Official MediaWiki Website]")
        appendLine("* [https://www.mediawiki.org/wiki/Help:Formatting WikiText Formatting Guide]")
        appendLine()
        appendLine("[[Category:Documentation]]")
        appendLine("[[Category:MediaWiki]]")
    }

    private fun generateLargeWikitextDocument(): String = buildString {
        appendLine("== Comprehensive MediaWiki Reference ==")
        appendLine()
        appendLine("{{Infobox documentation")
        appendLine("| title = MediaWiki Comprehensive Reference")
        appendLine("| version = 3.0")
        appendLine("| date = 2025-11-19")
        appendLine("| authors = Documentation Team")
        appendLine("}}")
        appendLine()
        appendLine("This is a '''comprehensive reference''' for MediaWiki markup and features.")
        appendLine()
        appendLine("__TOC__")
        appendLine()

        // Generate 20 chapters with 5 sections each
        for (chapter in 1..20) {
            appendLine("== Chapter $chapter: Advanced Topics ==")
            appendLine()
            appendLine("This chapter covers advanced WikiText features and patterns.")
            appendLine()

            for (section in 1..5) {
                appendLine("=== Section ${chapter}.$section: Topic Details ===")
                appendLine()
                appendLine("==== Overview ====")
                appendLine()
                appendLine("Detailed discussion of topic ${chapter}.$section.")
                appendLine()
                appendLine("===== Key Concepts =====")
                appendLine()
                appendLine("# '''Concept ${chapter}.$section.1''' - Architecture patterns")
                appendLine("# '''Concept ${chapter}.$section.2''' - Design principles")
                appendLine("# '''Concept ${chapter}.$section.3''' - Implementation strategies")
                appendLine("# '''Concept ${chapter}.$section.4''' - Common pitfalls")
                appendLine("# '''Concept ${chapter}.$section.5''' - Optimization techniques")
                appendLine()
                appendLine("==== Implementation ====")
                appendLine()
                appendLine("<syntaxhighlight lang=\"kotlin\" line highlight=\"3,7-9\">")
                appendLine("class Chapter${chapter}Section$section {")
                appendLine("    private val config = Configuration()")
                appendLine("    ")
                appendLine("    fun initialize() {")
                appendLine("        config.load(\"settings.properties\")")
                appendLine("        validateConfiguration()")
                appendLine("    }")
                appendLine("    ")
                appendLine("    fun process(input: String): Result {")
                appendLine("        val validated = validate(input)")
                appendLine("        if (!validated) {")
                appendLine("            return Result.error(\"Invalid input\")")
                appendLine("        }")
                appendLine("        val transformed = transform(input)")
                appendLine("        return Result.success(transformed)")
                appendLine("    }")
                appendLine("}")
                appendLine("</syntaxhighlight>")
                appendLine()
                appendLine("==== Configuration Table ====")
                appendLine()
                appendLine("{| class=\"wikitable sortable\"")
                appendLine("! Parameter !! Type !! Default !! Description")
                appendLine("|-")
                appendLine("| maxConnections || Integer || 100 || Maximum concurrent connections")
                appendLine("|-")
                appendLine("| timeout || Long || 5000 || Request timeout in milliseconds")
                appendLine("|-")
                appendLine("| retryPolicy || String || exponential || Retry strategy for failed requests")
                appendLine("|-")
                appendLine("| enableCaching || Boolean || true || Enable response caching")
                appendLine("|}")
                appendLine()
                appendLine("{{Warning|Validate all configuration values before deployment.}}")
                appendLine()
                appendLine("==== Related Topics ====")
                appendLine()
                appendLine("* [[Chapter ${chapter-1}:Section $section|Previous section]]")
                appendLine("* [[Chapter ${chapter+1}:Section $section|Next section]]")
                appendLine("* [[Configuration Reference]]")
                appendLine()
            }
        }

        appendLine("== Appendices ==")
        appendLine()
        appendLine("=== Glossary ===")
        appendLine()
        appendLine("; MediaWiki")
        appendLine(": A free wiki software platform")
        appendLine("; WikiText")
        appendLine(": The markup language used by MediaWiki")
        appendLine()
        appendLine("[[Category:Reference Documentation]]")
        appendLine("[[Category:MediaWiki]]")
        appendLine("[[Category:Advanced Topics]]")
    }

    private fun generateComplexWikitextDocument(): String = buildString {
        appendLine("== Complex WikiText Features ==")
        appendLine()
        appendLine("{{Featured article}}")
        appendLine()
        appendLine("This article demonstrates '''advanced WikiText''' features.")
        appendLine()
        appendLine("__NOTOC__")
        appendLine("__FORCETOC__")
        appendLine()
        appendLine("=== Templates and Parser Functions ===")
        appendLine()
        appendLine("{{Infobox feature")
        appendLine("| name = Complex Features")
        appendLine("| type = Advanced")
        appendLine("| status = Active")
        appendLine("}}")
        appendLine()
        appendLine("Current date: {{CURRENTDATE}}")
        appendLine()
        appendLine("Page count: {{NUMBEROFARTICLES}}")
        appendLine()
        appendLine("=== Magic Words ===")
        appendLine()
        appendLine("__NOTOC__")
        appendLine("__NOEDITSECTION__")
        appendLine("__NEWSECTIONLINK__")
        appendLine()
        appendLine("=== Complex Tables ===")
        appendLine()
        appendLine("{| class=\"wikitable\" style=\"text-align:center\"")
        appendLine("|+ Feature Comparison")
        appendLine("! scope=\"col\" | Feature")
        appendLine("! scope=\"col\" | Basic")
        appendLine("! scope=\"col\" | Pro")
        appendLine("! scope=\"col\" | Enterprise")
        appendLine("|-")
        appendLine("! scope=\"row\" | Users")
        appendLine("| 1-10")
        appendLine("| 11-100")
        appendLine("| Unlimited")
        appendLine("|-")
        appendLine("! scope=\"row\" | Storage")
        appendLine("| 10 GB")
        appendLine("| 100 GB")
        appendLine("| 1 TB")
        appendLine("|-")
        appendLine("! scope=\"row\" | Support")
        appendLine("| Email")
        appendLine("| Phone + Email")
        appendLine("| 24/7 Dedicated")
        appendLine("|}")
        appendLine()
        appendLine("=== Nested Lists ===")
        appendLine()
        appendLine("* First level")
        appendLine("** Second level")
        appendLine("*** Third level")
        appendLine("**** Fourth level")
        appendLine("** Another second level")
        appendLine("* Back to first level")
        appendLine()
        appendLine("=== Definition Lists ===")
        appendLine()
        appendLine("; Term 1")
        appendLine(": Definition of term 1")
        appendLine("; Term 2")
        appendLine(": Definition of term 2")
        appendLine(": Additional definition for term 2")
        appendLine()
        appendLine("=== Preformatted Text ===")
        appendLine()
        appendLine(" This text is preformatted")
        appendLine("   with custom indentation")
        appendLine("     preserved exactly")
        appendLine()
        appendLine("=== Gallery ===")
        appendLine()
        appendLine("<gallery mode=\"packed\" heights=\"200px\">")
        appendLine("File:Example1.jpg|Caption for image 1")
        appendLine("File:Example2.jpg|Caption for image 2")
        appendLine("File:Example3.jpg|Caption for image 3")
        appendLine("</gallery>")
        appendLine()
        appendLine("=== References ===")
        appendLine()
        appendLine("Here is a citation<ref>Author Name. \"Article Title\". ''Publication''. 2025.</ref>.")
        appendLine()
        appendLine("Another citation<ref name=\"wiki\">Wikipedia contributors. \"WikiText\". ''Wikipedia''. 2025.</ref>.")
        appendLine()
        appendLine("Reusing citation<ref name=\"wiki\" />.")
        appendLine()
        appendLine("<references />")
        appendLine()
        appendLine("=== Categories and Interwiki ===")
        appendLine()
        appendLine("[[Category:Documentation]]")
        appendLine("[[Category:MediaWiki]]")
        appendLine("[[Category:Advanced Features]]")
        appendLine()
        appendLine("[[de:Komplexe WikiText-Funktionen]]")
        appendLine("[[fr:Fonctionnalités WikiText complexes]]")
        appendLine("[[es:Características complejas de WikiText]]")
    }
}
