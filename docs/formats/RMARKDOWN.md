# R Markdown Format Guide

## Overview

R Markdown is an extension of Markdown that supports embedded R (and other) code chunks for dynamic documents.

## Supported Extensions

- `.rmd` - R Markdown
- `.Rmd` - Alternate

## Document Structure

```rmd
---
title: "Title"
author: "Author"
date: "2024-01-15"
output: html_document
---

# R Markdown

This is an R Markdown document.
```

## Code Chunks

<pre>
```{r, echo=TRUE}
# R code here
summary(cars)
```
</pre>

## Chunk Options

- `echo=TRUE` - Show code
- `eval=TRUE` - Run code
- `include=TRUE` - Include output
- `fig.width=6` - Figure width

## Inline Code

```rmd
The mean is `r mean(cars$speed)`.
```

## Output Formats

```rmd
---
output: html_document
output: pdf_document
output: word_document
output: beamer_presentation
---
```

## Tables

```rmd
```{r}
library(kntr)
kable(head(cars))
```
```

## See Also

- [R Markdown](https://rmarkdown.rstudio.com)
