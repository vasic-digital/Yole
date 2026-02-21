# LaTeX Format Guide

## Overview

LaTeX is a high-quality typesetting system commonly used for technical and scientific documents. Yole provides LaTeX support including common commands, math formulas, and document structures.

## Supported Extensions

- `.tex` - LaTeX source
- `.latex` - LaTeX alternate
- `.ltx` - LaTeX short

## Document Structure

```latex
\documentclass{article}
\usepackage[utf8]{inputenc}
\usepackage[T1]{fontenc}

\title{Document Title}
\author{Author Name}
\date{\today}

\begin{document}
\maketitle

\section{Introduction}
Your content here.

\end{document}
```

## Common Classes

- `article` - Short documents
- `report` - Longer with chapters
- `book` - Book-style documents
- `letter` - Letters

## Text Formatting

- `\textbf{bold}` or `{\bfseries bold}`
- `\textit{italic}` or `{\itshape italic}`
- `\underline{underline}`
- `\emph{emphasis}`

## Math Mode

### Inline Math
```latex
The formula $E = mc^2$ is famous.
```

### Display Math
```latex
\[
\int_{0}^{\infty} e^{-x^2} dx = \frac{\sqrt{\pi}}{2}
\]
```

## Environments

- `itemize` - Bullet lists
- `enumerate` - Numbered lists
- `description` - Description lists
- `table` - Tables
- `figure` - Figures
- `verbatim` - Code blocks

## Common Packages

- `amsmath` - Advanced math
- `amssymb` - Math symbols
- `graphicx` - Images
- `hyperref` - Hyperlinks
- `listings` - Code highlighting

## See Also

- [LaTeX Project](https://www.latex-project.org)
- [CTAN](https://ctan.org)
