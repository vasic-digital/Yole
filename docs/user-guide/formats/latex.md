# LaTeX Format Guide

**Format**: LaTeX
**Extensions**: `.tex`, `.latex`
**Specification**: [LaTeX Project](https://www.latex-project.org/)
**Yole Support**: ✅ Syntax highlighting, preview (requires LaTeX installation)

---

## Overview

LaTeX is a high-quality typesetting system designed for the production of technical and scientific documentation. It's the de facto standard for academic papers, theses, and technical documents.

### Why LaTeX?

- **Professional Typography**: Beautiful mathematical equations
- **Consistent Formatting**: Automatic numbering, references
- **Academic Standard**: Required for many journals and conferences
- **Version Control**: Plain text works with Git
- **Portable**: Cross-platform compatibility
- **Powerful**: Complex documents with ease

---

## Basic Document Structure

### Minimal Document

```latex
\documentclass{article}

\begin{document}
Hello, World!
\end{document}
```

**Components**:
- `\documentclass{article}` - Document type
- `\begin{document}` - Content starts
- `\end{document}` - Content ends

### Document with Preamble

```latex
\documentclass[12pt,a4paper]{article}

% Preamble
\usepackage[utf8]{inputenc}
\usepackage{amsmath}
\usepackage{graphicx}

\title{My Document Title}
\author{John Doe}
\date{\today}

\begin{document}

\maketitle

\section{Introduction}
This is the introduction.

\end{document}
```

---

## Document Classes

### Common Classes

```latex
\documentclass{article}     % Articles, short documents
\documentclass{report}      % Reports, longer documents with chapters
\documentclass{book}        % Books
\documentclass{letter}      % Letters
\documentclass{beamer}      % Presentations
```

### Class Options

```latex
\documentclass[12pt,a4paper,twocolumn]{article}
```

**Common options**:
- **Font size**: `10pt`, `11pt`, `12pt` (default: 10pt)
- **Paper size**: `a4paper`, `letterpaper`, `legalpaper`
- **Columns**: `onecolumn`, `twocolumn`
- **Sides**: `oneside`, `twoside`
- **Draft mode**: `draft` (shows overfull boxes)

---

## Text Formatting

### Basic Formatting

```latex
% Emphasis
\textbf{Bold text}
\textit{Italic text}
\texttt{Typewriter (monospace)}
\underline{Underlined text}

% Combined
\textbf{\textit{Bold and italic}}

% Size
{\tiny tiny text}
{\small small text}
{\large large text}
{\Large Large text}
{\LARGE LARGE text}
{\huge huge text}
{\Huge Huge text}
```

### Text Alignment

```latex
\begin{center}
Centered text
\end{center}

\begin{flushleft}
Left-aligned text
\end{flushleft}

\begin{flushright}
Right-aligned text
\end{flushright}
```

---

## Document Structure

### Sections

```latex
\section{Section Title}
\subsection{Subsection Title}
\subsubsection{Subsubsection Title}
\paragraph{Paragraph Title}
\subparagraph{Subparagraph Title}
```

**Automatic numbering**: Sections are numbered automatically.

**Unnumbered sections**:
```latex
\section*{Introduction}  % No number
```

### Table of Contents

```latex
\tableofcontents  % Generates automatically from sections
```

### Abstract

```latex
\begin{abstract}
This document presents...
\end{abstract}
```

---

## Lists

### Itemized Lists (Bullets)

```latex
\begin{itemize}
  \item First item
  \item Second item
  \item Third item
\end{itemize}
```

### Enumerated Lists (Numbers)

```latex
\begin{enumerate}
  \item First item
  \item Second item
  \item Third item
\end{enumerate}
```

### Description Lists

```latex
\begin{description}
  \item[Term 1] Description of term 1
  \item[Term 2] Description of term 2
\end{description}
```

### Nested Lists

```latex
\begin{enumerate}
  \item Top level
  \begin{enumerate}
    \item Nested level
    \item Another nested
  \end{enumerate}
  \item Back to top level
\end{enumerate}
```

---

## Mathematics

### Inline Math

```latex
The formula $E = mc^2$ is Einstein's famous equation.
```

**Result**: The formula E = mc² is Einstein's famous equation.

### Display Math

```latex
\[
E = mc^2
\]

% Or using equation environment (numbered)
\begin{equation}
E = mc^2
\end{equation}
```

### Common Math Symbols

```latex
% Greek letters
\alpha, \beta, \gamma, \Delta, \Omega

% Operators
x + y, x - y, x \times y, x \div y
\frac{x}{y}, x^2, x_i, \sqrt{x}, \sqrt[3]{x}

% Relations
x < y, x > y, x \leq y, x \geq y, x \neq y, x \approx y

% Sets
\in, \notin, \subset, \subseteq, \cup, \cap, \emptyset

% Logic
\forall, \exists, \neg, \wedge, \vee, \implies, \iff

% Calculus
\int, \sum, \prod, \lim, \frac{d}{dx}, \partial

% Examples
\int_0^{\infty} e^{-x} dx = 1
\sum_{i=1}^{n} i = \frac{n(n+1)}{2}
```

### Multi-line Equations

```latex
\begin{align}
x &= y + z \\
  &= a + b + c \\
  &= 1 + 2 + 3
\end{align}
```

**Tips**:
- Use `&` for alignment
- Use `\\` for line breaks
- Use `\nonumber` to suppress equation numbers

---

## Tables

### Basic Table

```latex
\begin{tabular}{|c|c|c|}
\hline
Header 1 & Header 2 & Header 3 \\
\hline
Row 1 Col 1 & Row 1 Col 2 & Row 1 Col 3 \\
Row 2 Col 1 & Row 2 Col 2 & Row 2 Col 3 \\
\hline
\end{tabular}
```

**Column alignment**:
- `l` - Left aligned
- `c` - Centered
- `r` - Right aligned
- `|` - Vertical line
- `\hline` - Horizontal line

### Table Environment (with caption)

```latex
\begin{table}[h]
\centering
\begin{tabular}{|l|r|r|}
\hline
Item & Quantity & Price \\
\hline
Apples & 10 & \$5.00 \\
Oranges & 5 & \$3.00 \\
\hline
Total & 15 & \$8.00 \\
\hline
\end{tabular}
\caption{Shopping List}
\label{tab:shopping}
\end{table}
```

**Positioning**: `[h]` here, `[t]` top, `[b]` bottom, `[p]` page

---

## Figures

### Including Images

```latex
\usepackage{graphicx}  % In preamble

\begin{figure}[h]
\centering
\includegraphics[width=0.8\textwidth]{image.pdf}
\caption{My Figure}
\label{fig:myfigure}
\end{figure}
```

**Supported formats**: PDF, PNG, JPEG, EPS

### Referencing

```latex
As shown in Figure~\ref{fig:myfigure}...
See Table~\ref{tab:shopping} for details.
```

LaTeX automatically updates numbers when you add/remove figures.

---

## Citations and Bibliography

### BibTeX Style

**In document**:
```latex
\usepackage{cite}  % In preamble

According to Smith~\cite{smith2020}, the results...

\bibliographystyle{plain}
\bibliography{references}  % references.bib file
```

**In references.bib**:
```bibtex
@article{smith2020,
  author = {John Smith},
  title = {An Important Paper},
  journal = {Journal of Important Things},
  year = {2020},
  volume = {42},
  pages = {1--10}
}
```

### Manual Bibliography

```latex
\begin{thebibliography}{9}
\bibitem{smith2020}
John Smith.
\emph{An Important Paper}.
Journal of Important Things, 2020.
\end{thebibliography}
```

---

## Common Packages

### Essential Packages

```latex
\usepackage[utf8]{inputenc}    % UTF-8 input
\usepackage[T1]{fontenc}       % Font encoding
\usepackage{amsmath}           % Math symbols
\usepackage{amssymb}           % More math symbols
\usepackage{graphicx}          % Include graphics
\usepackage{hyperref}          % Hyperlinks
\usepackage{geometry}          % Page layout
\usepackage{listings}          % Code listings
\usepackage{xcolor}            % Colors
```

### Hyperlinks

```latex
\usepackage{hyperref}
\hypersetup{
    colorlinks=true,
    linkcolor=blue,
    filecolor=magenta,
    urlcolor=cyan,
}

\url{https://example.com}
\href{https://example.com}{Link text}
```

### Code Listings

```latex
\usepackage{listings}

\begin{lstlisting}[language=Python]
def hello():
    print("Hello, World!")
\end{lstlisting}
```

---

## LaTeX in Yole

### Syntax Highlighting

Yole highlights:
- **Commands**: `\textbf`, `\section`, etc.
- **Environments**: `\begin{...}` and `\end{...}`
- **Math mode**: `$...$` and `\[...\]`
- **Comments**: `%` comments
- **Special characters**: `{`, `}`, `[`, `]`

### Editing LaTeX

**Edit mode**:
- Syntax highlighting for all LaTeX commands
- Bracket matching
- Auto-indentation (future feature)

**Tips**:
- Use consistent indentation
- Add comments for complex sections
- Break long lines for readability

### Compiling LaTeX

**Note**: Yole provides editing and syntax highlighting but does not compile LaTeX documents. To compile:

1. **Save file** in Yole with `.tex` extension
2. **Transfer to computer** (if on mobile)
3. **Compile** with LaTeX distribution:
   ```bash
   pdflatex document.tex
   ```

---

## Best Practices

### Document Organization

```latex
% main.tex
\documentclass{article}
\input{preamble}
\begin{document}
\input{introduction}
\input{methodology}
\input{results}
\input{conclusion}
\end{document}

% preamble.tex - packages and settings
% introduction.tex - introduction section
% etc.
```

### Comments

```latex
% This is a comment
% Comments are useful for:
% - Explaining complex code
% - Disabling sections temporarily
% - Adding TODOs

% TODO: Add more examples here
```

### Special Characters

**Reserved characters**:
```latex
# $ % ^ & _ { } ~ \
```

**Escaping**:
```latex
\# \$ \% \^{} \& \_ \{ \} \~{} \textbackslash
```

### Line Breaks and Spacing

```latex
Line 1

Line 2 (blank line creates new paragraph)

Line with explicit break \\
Next line

\noindent No paragraph indentation
```

---

## Common Use Cases

### 1. Academic Paper

```latex
\documentclass[12pt]{article}
\usepackage[utf8]{inputenc}
\usepackage{amsmath,cite,graphicx}

\title{Research Paper Title}
\author{Your Name \\ University Name}
\date{\today}

\begin{document}
\maketitle

\begin{abstract}
This paper presents...
\end{abstract}

\section{Introduction}
The introduction...

\section{Methodology}
We conducted...

\section{Results}
The results show...

\section{Conclusion}
In conclusion...

\bibliographystyle{plain}
\bibliography{references}

\end{document}
```

### 2. Technical Report

```latex
\documentclass{report}
\title{Technical Report}
\author{Engineering Team}

\begin{document}
\maketitle
\tableofcontents

\chapter{Overview}
\chapter{Specifications}
\chapter{Implementation}
\chapter{Testing}
\end{document}
```

### 3. Resume/CV

```latex
\documentclass{article}
\usepackage[margin=1in]{geometry}

\begin{document}
\begin{center}
{\huge \textbf{John Doe}} \\
\vspace{0.2cm}
Email: john@example.com | Phone: 555-0101
\end{center}

\section*{Education}
\textbf{University Name} \hfill 2015-2019 \\
Bachelor of Science in Computer Science

\section*{Experience}
\textbf{Company Name} \hfill 2019-Present \\
Software Engineer
\begin{itemize}
  \item Developed web applications
  \item Led team of 5 developers
\end{itemize}

\end{document}
```

---

## Tips & Tricks

### 🎯 Pro Tips

1. **Use packages**: Don't reinvent the wheel
2. **Separate preamble**: Reuse settings across documents
3. **BibTeX for citations**: Automatic formatting
4. **Use labels**: `\label` and `\ref` for cross-references
5. **Compile twice**: For references and TOC to update

### 🚀 Power User Techniques

**Custom commands**:
```latex
\newcommand{\myemail}{john@example.com}
\newcommand{\important}[1]{\textbf{\textit{#1}}}

% Usage
Contact: \myemail
\important{This is important}
```

**Conditional compilation**:
```latex
\newif\ifdraft
\drafttrue  % or \draftfalse

\ifdraft
  Draft version content
\else
  Final version content
\fi
```

**Version control friendly**:
```latex
% One sentence per line for better diffs
This is sentence one.
This is sentence two.
This is sentence three.
```

---

## Troubleshooting

### Compilation errors?
- Check for unmatched braces `{}`
- Verify environment pairs (`\begin` and `\end`)
- Look for special characters (need escaping)
- Check package conflicts

### Missing references (??)?
- Compile twice (first pass collects, second pass resolves)
- Check `\label` and `\ref` match exactly

### Overfull/Underfull boxes?
- LaTeX warning about line breaking
- Use `\sloppy` for more lenient spacing (not recommended)
- Rewrite text or hyphenate manually

### Images not showing?
- Check file path
- Verify image format (PDF, PNG, JPEG)
- Ensure `graphicx` package loaded

---

## External Resources

### Official
- [LaTeX Project](https://www.latex-project.org/) - Official website
- [CTAN](https://www.ctan.org/) - Comprehensive TeX Archive Network
- [Overleaf Documentation](https://www.overleaf.com/learn) - Excellent tutorials

### Learning
- [LaTeX Wikibook](https://en.wikibooks.org/wiki/LaTeX) - Free comprehensive guide
- [Learn LaTeX in 30 Minutes](https://www.overleaf.com/learn/latex/Learn_LaTeX_in_30_minutes)
- [The Not So Short Introduction to LaTeX](https://tobi.oetiker.ch/lshort/lshort.pdf)

### Tools
- [Overleaf](https://www.overleaf.com/) - Online LaTeX editor
- [TeXworks](https://www.tug.org/texworks/) - Simple editor
- [TeXstudio](https://www.texstudio.org/) - Feature-rich editor
- [MiKTeX](https://miktex.org/) - Windows distribution
- [TeX Live](https://www.tug.org/texlive/) - Cross-platform distribution

### Symbols
- [Detexify](http://detexify.kirelabs.org/classify.html) - Draw symbol to find LaTeX command
- [LaTeX Math Symbols](http://www.math.harvard.edu/texman/) - Comprehensive list

---

## Next Steps

- **[Markdown Format →](./markdown.md)** - Simpler markup language
- **[reStructuredText Format →](./restructuredtext.md)** - Python documentation
- **[Back to Getting Started →](../getting-started.md)**

---

*Last updated: November 11, 2025*
*Yole version: 2.15.1+*
