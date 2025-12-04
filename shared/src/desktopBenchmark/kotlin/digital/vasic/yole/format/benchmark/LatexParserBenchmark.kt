/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * LaTeX Parser Performance Benchmarks
 *
 *########################################################*/
package digital.vasic.yole.format.benchmark

import digital.vasic.yole.format.latex.LatexParser
import kotlinx.benchmark.*

/**
 * Performance benchmarks for LatexParser.
 *
 * This benchmark suite measures the performance of LaTeX parsing across
 * different document sizes and complexity levels to establish baselines and
 * track optimization improvements.
 *
 * Benchmark Scenarios:
 * - Small document (1-2 pages) - Simple LaTeX article
 * - Medium document (10-20 pages) - Academic paper
 * - Large document (100+ pages) - Thesis or book
 * - Complex document - Heavy math equations, tables, figures
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
class LatexParserBenchmark {

    private lateinit var parser: LatexParser

    // Test content of various sizes
    private lateinit var smallContent: String      // ~2KB
    private lateinit var mediumContent: String     // ~20KB
    private lateinit var largeContent: String      // ~200KB
    private lateinit var complexContent: String    // Complex LaTeX features

    @Setup
    fun setup() {
        parser = LatexParser()

        // Small content: Simple LaTeX article
        smallContent = """
            \documentclass{article}
            \usepackage{amsmath}
            \title{Simple Document}
            \author{Test Author}
            \date{\today}

            \begin{document}
            \maketitle

            \section{Introduction}

            This is a simple LaTeX document for performance testing. It contains
            basic text formatting including \textbf{bold}, \textit{italic}, and
            \texttt{monospace} text.

            \section{Mathematical Equations}

            Here is a simple inline equation: ${'$'}E = mc^2${'$'}.

            And a display equation:
            \begin{equation}
                \int_{0}^{\infty} e^{-x^2} dx = \frac{\sqrt{\pi}}{2}
            \end{equation}

            \section{Lists}

            \begin{itemize}
                \item First item
                \item Second item
                \item Third item
            \end{itemize}

            \section{Conclusion}

            This concludes our simple test document.

            \end{document}
        """.trimIndent()

        // Medium content: Academic paper
        mediumContent = buildString {
            append("""
                \documentclass[11pt,a4paper]{article}
                \usepackage{amsmath,amssymb,amsthm}
                \usepackage{graphicx}
                \title{Medium Complexity LaTeX Document}
                \author{Author Name}
                \date{\today}

                \begin{document}
                \maketitle

                \begin{abstract}
                This paper presents a comprehensive analysis of various topics
                with mathematical formulations and structured content.
                \end{abstract}

            """.trimIndent())

            for (section in 1..10) {
                append("""

                    \section{Section $section}

                    This section discusses topic $section in detail. We present several
                    important concepts and their mathematical formulations.

                    \subsection{Theoretical Background}

                    Consider the following theorem:

                    \begin{theorem}
                    Let ${'$'}f: \mathbb{R} \rightarrow \mathbb{R}${'$'} be a continuous function.
                    Then for all ${'$'}x \in \mathbb{R}${'$'}, we have:
                    \begin{equation}
                        f(x + h) - f(x) = \int_{x}^{x+h} f'(t) dt
                    \end{equation}
                    \end{theorem}

                    \subsection{Practical Applications}

                    \begin{enumerate}
                        \item Application in field A
                        \item Application in field B
                        \item Application in field C
                    \end{enumerate}

                    The main result can be expressed as:
                    \begin{align}
                        x &= \frac{-b \pm \sqrt{b^2 - 4ac}}{2a} \\
                        y &= mx + c \\
                        z &= f(x, y)
                    \end{align}

                """.trimIndent())
            }

            append("\n\\end{document}\n")
        }

        // Large content: Thesis/book
        largeContent = buildString {
            append("""
                \documentclass[12pt]{book}
                \usepackage{amsmath,amssymb,amsthm,graphicx,hyperref}
                \title{Large LaTeX Document}
                \author{Author Name}

                \begin{document}
                \maketitle
                \tableofcontents

            """.trimIndent())

            for (chapter in 1..20) {
                append("\n\\chapter{Chapter $chapter}\n\n")

                for (section in 1..5) {
                    append("""

                        \section{Section $chapter.$section}

                        Detailed content for section $chapter.$section.

                        \subsection{Theory}

                        Mathematical formulation:
                        \begin{equation}
                            \sum_{n=1}^{\infty} \frac{1}{n^2} = \frac{\pi^2}{6}
                        \end{equation}

                        \subsection{Examples}

                        \begin{enumerate}
                    """.trimIndent())

                    repeat(5) { item ->
                        append("\n\\item Example ${item + 1} with equations ${'$'}x_{${item + 1}} = ${item + 1}${'$'}")
                    }

                    append("""

                        \end{enumerate}

                    """.trimIndent())
                }
            }

            append("\n\\end{document}\n")
        }

        // Complex content: Heavy LaTeX features
        complexContent = """
            \documentclass{article}
            \usepackage{amsmath,amssymb,amsthm,graphicx,tikz,listings}
            \newtheorem{theorem}{Theorem}
            \newtheorem{lemma}{Lemma}

            \begin{document}

            \section{Complex Mathematical Structures}

            \begin{theorem}[Fundamental Theorem]
            For any measurable space ${'$'}(\Omega, \mathcal{F}, \mu)${'$'} and integrable function
            ${'$'}f: \Omega \rightarrow \mathbb{C}${'$'}, we have:
            \begin{equation}
                \int_{\Omega} |f| d\mu < \infty \implies \lim_{n \to \infty} \int_{\Omega} f_n d\mu = \int_{\Omega} f d\mu
            \end{equation}
            \end{theorem}

            \section{Matrix Operations}

            \begin{equation}
                \begin{bmatrix}
                    a_{11} & a_{12} & \cdots & a_{1n} \\
                    a_{21} & a_{22} & \cdots & a_{2n} \\
                    \vdots & \vdots & \ddots & \vdots \\
                    a_{m1} & a_{m2} & \cdots & a_{mn}
                \end{bmatrix}
                \begin{bmatrix}
                    x_1 \\ x_2 \\ \vdots \\ x_n
                \end{bmatrix}
                =
                \begin{bmatrix}
                    b_1 \\ b_2 \\ \vdots \\ b_m
                \end{bmatrix}
            \end{equation}

            \section{Nested Environments}

            \begin{align}
                \int_{0}^{1} \left( \sum_{n=1}^{\infty} \frac{x^n}{n!} \right) dx
                &= \int_{0}^{1} e^x dx \\
                &= e - 1 \\
                &\approx 1.718281828
            \end{align}

            \section{Complex Tables}

            \begin{table}[h]
            \centering
            \begin{tabular}{|c|c|c|}
                \hline
                ${'$'}x${'$'} & ${'$'}f(x) = x^2${'$'} & ${'$'}g(x) = \sqrt{x}${'$'} \\
                \hline
                1 & 1 & 1.000 \\
                2 & 4 & 1.414 \\
                3 & 9 & 1.732 \\
                \hline
            \end{tabular}
            \end{table}

            \section{Special Symbols}

            Greek: ${'$'}\alpha, \beta, \gamma, \Delta, \Theta, \Lambda, \Omega${'$'} \\
            Operators: ${'$'}\forall, \exists, \in, \subset, \cup, \cap, \times, \otimes${'$'} \\
            Arrows: ${'$'}\rightarrow, \Rightarrow, \leftrightarrow, \mapsto${'$'}

            \end{document}
        """.trimIndent()
    }

    /**
     * Benchmark parsing small LaTeX document (~2KB).
     *
     * Target: < 40ms for 2KB LaTeX
     */
    @Benchmark
    fun parseSmallDocument(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(smallContent, mapOf("filename" to "small.tex"))
    }

    /**
     * Benchmark parsing medium LaTeX document (~20KB).
     *
     * Target: < 200ms for 20KB LaTeX
     */
    @Benchmark
    fun parseMediumDocument(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(mediumContent, mapOf("filename" to "medium.tex"))
    }

    /**
     * Benchmark parsing large LaTeX document (~200KB).
     *
     * Target: < 2000ms for 200KB LaTeX
     */
    @Benchmark
    fun parseLargeDocument(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(largeContent, mapOf("filename" to "large.tex"))
    }

    /**
     * Benchmark parsing complex LaTeX with heavy math and structures.
     *
     * Target: < 100ms for complex features
     */
    @Benchmark
    fun parseComplexDocument(): digital.vasic.yole.format.ParsedDocument {
        return parser.parse(complexContent, mapOf("filename" to "complex.tex"))
    }

    /**
     * Benchmark HTML conversion (for preview).
     *
     * Target: < 50ms for HTML conversion
     */
    @Benchmark
    fun convertToHtml(): String {
        val document = parser.parse(smallContent, mapOf("filename" to "small.tex"))
        return parser.toHtml(document, lightMode = true)
    }

    /**
     * Benchmark validation of LaTeX syntax.
     *
     * Target: < 30ms for validation
     */
    @Benchmark
    fun validateDocument(): List<String> {
        return parser.validate(mediumContent)
    }
}
