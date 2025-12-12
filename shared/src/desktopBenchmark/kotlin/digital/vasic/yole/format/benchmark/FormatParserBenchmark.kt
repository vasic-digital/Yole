/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Format Parser Performance Benchmarks
 * JMH benchmarks for all format parsers
 *
 *########################################################*/

package digital.vasic.yole.format.benchmark

import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

/**
 * Performance benchmarks for format parsers
 * Measures parsing speed, memory usage, and throughput
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
class FormatParserBenchmark {

    private lateinit var markdownParser: MarkdownParser
    private lateinit var todoTxtParser: TodoTxtParser
    private lateinit var csvParser: CsvParser
    private lateinit var plaintextParser: PlaintextParser

    // Test data of various sizes
    private lateinit var smallMarkdown: String
    private lateinit var mediumMarkdown: String
    private lateinit var largeMarkdown: String
    
    private lateinit var smallTodoTxt: String
    private lateinit var mediumTodoTxt: String
    private lateinit var largeTodoTxt: String
    
    private lateinit var smallCsv: String
    private lateinit var mediumCsv: String
    private lateinit var largeCsv: String
    
    private lateinit var smallPlaintext: String
    private lateinit var mediumPlaintext: String
    private lateinit var largePlaintext: String

    @Setup
    fun setup() {
        // Initialize parsers
        markdownParser = MarkdownParser()
        todoTxtParser = TodoTxtParser()
        csvParser = CsvParser()
        plaintextParser = PlaintextParser()
        
        // Generate test data
        generateTestData()
    }

    private fun generateTestData() {
        // Small documents (1-2 KB)
        smallMarkdown = """
            # Small Document
            
            This is a small markdown document for testing.
            
            ## Section 1
            
            Some **bold** text and *italic* text.
            
            - Item 1
            - Item 2
            - Item 3
            
            \`\`\`kotlin
            fun main() {
                println("Hello World")
            }
            \`\`\`
            
            [Link](https://example.com)
            
            > This is a blockquote
        """.trimIndent()
        
        smallTodoTxt = """
            (A) High priority task +project @context
            (B) Medium priority task +project @context  
            (C) Low priority task +project @context
            x 2023-12-01 Completed task +project @context
        """.trimIndent()
        
        smallCsv = """
            Name,Age,City,Department
            John Doe,30,New York,Engineering
            Jane Smith,25,Los Angeles,Marketing
            Bob Johnson,35,Chicago,Sales
        """.trimIndent()
        
        smallPlaintext = """
            This is a small plain text document.
            It contains some sample content for testing.
            
            The parser should handle this content efficiently.
            
            End of document.
        """.trimIndent()
        
        // Medium documents (10-15 KB)
        mediumMarkdown = buildString {
            repeat(10) { i ->
                appendLine("# Section $i")
                appendLine()
                appendLine("This is content for section $i with **bold** and *italic* text.")
                appendLine()
                appendLine("## Subsection $i.1")
                appendLine()
                appendLine("More content here with `inline code` and [links](https://example$i.com).")
                appendLine()
                appendLine("- List item 1 for section $i")
                appendLine("- List item 2 for section $i")
                appendLine("- List item 3 for section $i")
                appendLine()
                appendLine("\`\`\`python")
                appendLine("def function_$i():")
                appendLine("    print(f'Hello from function $i')")
                appendLine("    return $i * 2")
                appendLine("\`\`\`")
                appendLine()
                appendLine("> Blockquote for section $i")
                appendLine("> With multiple lines")
                appendLine()
            }
        }
        
        mediumTodoTxt = buildString {
            repeat(50) { i ->
                val priority = when (i % 3) {
                    0 -> "A"
                    1 -> "B"
                    else -> "C"
                }
                val projects = listOf("project1", "project2", "project3")
                val contexts = listOf("context1", "context2", "context3")
                
                appendLine("($priority) Task $i +${projects[i % 3]} @${contexts[i % 3]} due:2023-12-${(i % 30) + 1}")
            }
            repeat(10) { i ->
                appendLine("x 2023-12-01 2023-11-${(i % 30) + 1} Completed task $i +project @context")
            }
        }
        
        mediumCsv = buildString {
            appendLine("ID,Name,Email,Department,Salary,Location,Status,HireDate")
            repeat(100) { i ->
                appendLine("$i,Employee $i,employee$i@company.com,Department ${i % 10},${50000 + i * 100},Location ${i % 50},Active,2023-01-${(i % 28) + 1}")
            }
        }
        
        mediumPlaintext = buildString {
            repeat(20) { i ->
                appendLine("Paragraph $i:")
                appendLine("This is the content of paragraph $i. It contains various text patterns")
                appendLine("that should be handled by the plaintext parser efficiently.")
                appendLine("The parser needs to process this content quickly and accurately.")
                appendLine()
            }
        }
        
        // Large documents (50-100 KB)
        largeMarkdown = buildString {
            repeat(50) { i ->
                appendLine("# Major Section $i")
                appendLine()
                appendLine("This is a comprehensive section about topic $i.")
                appendLine()
                
                repeat(5) { j ->
                    appendLine("## Subsection $i.$j")
                    appendLine()
                    appendLine("Detailed content for subsection $i.$j with various formatting:")
                    appendLine()
                    appendLine("**Bold text** and *italic text* and `inline code`.")
                    appendLine()
                    appendLine("### Lists")
                    appendLine()
                    repeat(3) { k ->
                        appendLine("- Item $k for subsection $i.$j")
                        appendLine("  - Nested item $k.1")
                        appendLine("  - Nested item $k.2")
                    }
                    appendLine()
                    appendLine("### Code Block")
                    appendLine()
                    appendLine("\`\`\`kotlin")
                    appendLine("class Example_$i$j {")
                    appendLine("    fun method${i}_${j}() {")
                    appendLine("        println(\"Method $i.$j called\")")
                    appendLine("        return $i + $j")
                    appendLine("    }")
                    appendLine("}")
                    appendLine("\`\`\`")
                    appendLine()
                    appendLine("### Links and References")
                    appendLine()
                    appendLine("See [documentation](https://example.com/docs/$i/$j) for more info.")
                    appendLine()
                    appendLine("> Important note about subsection $i.$j")
                    appendLine("> This is a blockquote with multiple lines")
                    appendLine("> containing important information.")
                    appendLine()
                }
            }
        }
        
        largeTodoTxt = buildString {
            repeat(200) { i ->
                val priority = when (i % 4) {
                    0 -> "A"
                    1 -> "B"
                    2 -> "C"
                    else -> null
                }
                
                val projects = listOf("project1", "project2", "project3", "project4", "project5")
                val contexts = listOf("context1", "context2", "context3", "context4", "context5")
                val dueDate = if (i % 3 == 0) " due:2023-12-${(i % 30) + 1}" else ""
                
                val line = buildString {
                    priority?.let { append("($it) ") }
                    append("Large task $i with detailed description")
                    append(" +${projects[i % 5]}")
                    append(" @${contexts[i % 5]}")
                    append(dueDate)
                }
                appendLine(line)
            }
            
            repeat(50) { i ->
                val completionDate = "2023-12-01"
                val creationDate = "2023-11-${(i % 30) + 1}"
                appendLine("x $completionDate $creationDate Completed large task $i +project @context")
            }
        }
        
        largeCsv = buildString {
            appendLine("ID,Name,Email,Department,Salary,Location,Status,HireDate,Performance,Level")
            repeat(500) { i ->
                appendLine("$i,Employee $i,employee$i@company.com,Department ${i % 20},${45000 + i * 200},Location ${i % 100},Active,2023-01-${(i % 28) + 1},${(i % 5) + 1},Level ${(i % 10) + 1}")
            }
        }
        
        largePlaintext = buildString {
            repeat(100) { i ->
                appendLine("Section $i:")
                appendLine("=" * 50)
                appendLine()
                appendLine("This is section $i of a large plain text document.")
                appendLine("It contains various text patterns and content that needs to be processed.")
                appendLine("The plaintext parser should handle this efficiently without any issues.")
                appendLine()
                appendLine("Subsection $i.1:")
                appendLine("-" * 30)
                appendLine("Detailed information about subsection $i.1 goes here.")
                appendLine("This content is designed to test the parser's performance.")
                appendLine()
                appendLine("Subsection $i.2:")
                appendLine("-" * 30)
                appendLine("More detailed information about subsection $i.2.")
                appendLine("Including various text patterns and formatting.")
                appendLine()
            }
        }
    }

    // Markdown Parser Benchmarks

    @Benchmark
    fun benchmarkMarkdownSmall(blackhole: Blackhole) {
        val result = markdownParser.parse(smallMarkdown)
        blackhole.consume(result)
    }

    @Benchmark
    fun benchmarkMarkdownMedium(blackhole: Blackhole) {
        val result = markdownParser.parse(mediumMarkdown)
        blackhole.consume(result)
    }

    @Benchmark
    fun benchmarkMarkdownLarge(blackhole: Blackhole) {
        val result = markdownParser.parse(largeMarkdown)
        blackhole.consume(result)
    }

    @Benchmark
    fun benchmarkMarkdownToHtml(blackhole: Blackhole) {
        val document = markdownParser.parse(mediumMarkdown)
        val html = markdownParser.toHtml(document)
        blackhole.consume(html)
    }

    // Todo.txt Parser Benchmarks

    @Benchmark
    fun benchmarkTodoTxtSmall(blackhole: Blackhole) {
        val result = todoTxtParser.parse(smallTodoTxt)
        blackhole.consume(result)
    }

    @Benchmark
    fun benchmarkTodoTxtMedium(blackhole: Blackhole) {
        val result = todoTxtParser.parse(mediumTodoTxt)
        blackhole.consume(result)
    }

    @Benchmark
    fun benchmarkTodoTxtLarge(blackhole: Blackhole) {
        val result = todoTxtParser.parse(largeTodoTxt)
        blackhole.consume(result)
    }

    @Benchmark
    fun benchmarkTodoTxtToString(blackhole: Blackhole) {
        val document = todoTxtParser.parse(mediumTodoTxt)
        val todoTxt = todoTxtParser.toTodoTxt(document)
        blackhole.consume(todoTxt)
    }

    // CSV Parser Benchmarks

    @Benchmark
    fun benchmarkCsvSmall(blackhole: Blackhole) {
        val result = csvParser.parse(smallCsv)
        blackhole.consume(result)
    }

    @Benchmark
    fun benchmarkCsvMedium(blackhole: Blackhole) {
        val result = csvParser.parse(mediumCsv)
        blackhole.consume(result)
    }

    @Benchmark
    fun benchmarkCsvLarge(blackhole: Blackhole) {
        val result = csvParser.parse(largeCsv)
        blackhole.consume(result)
    }

    @Benchmark
    fun benchmarkCsvToHtml(blackhole: Blackhole) {
        val document = csvParser.parse(mediumCsv)
        val html = csvParser.toHtml(document)
        blackhole.consume(html)
    }

    // Plaintext Parser Benchmarks

    @Benchmark
    fun benchmarkPlaintextSmall(blackhole: Blackhole) {
        val result = plaintextParser.parse(smallPlaintext)
        blackhole.consume(result)
    }

    @Benchmark
    fun benchmarkPlaintextMedium(blackhole: Blackhole) {
        val result = plaintextParser.parse(mediumPlaintext)
        blackhole.consume(result)
    }

    @Benchmark
    fun benchmarkPlaintextLarge(blackhole: Blackhole) {
        val result = plaintextParser.parse(largePlaintext)
        blackhole.consume(result)
    }

    @Benchmark
    fun benchmarkPlaintextToHtml(blackhole: Blackhole) {
        val document = plaintextParser.parse(mediumPlaintext)
        val html = plaintextParser.toHtml(document)
        blackhole.consume(html)
    }

    // Memory allocation benchmarks

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    fun benchmarkMemoryAllocationMarkdown(blackhole: Blackhole) {
        // Force garbage collection before measurement
        System.gc()
        
        val result = markdownParser.parse(largeMarkdown)
        blackhole.consume(result)
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    fun benchmarkMemoryAllocationCsv(blackhole: Blackhole) {
        // Force garbage collection before measurement
        System.gc()
        
        val result = csvParser.parse(largeCsv)
        blackhole.consume(result)
    }

    // Throughput benchmarks

    @Benchmark
    @Threads(4)
    fun benchmarkConcurrentMarkdownParsing(blackhole: Blackhole) {
        val result = markdownParser.parse(mediumMarkdown)
        blackhole.consume(result)
    }

    @Benchmark
    @Threads(4)
    fun benchmarkConcurrentCsvParsing(blackhole: Blackhole) {
        val result = csvParser.parse(mediumCsv)
        blackhole.consume(result)
    }
}