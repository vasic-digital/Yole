# R Markdown Format Guide

**Format**: R Markdown
**Extensions**: `.Rmd`, `.rmarkdown`
**Specification**: [R Markdown](https://rmarkdown.rstudio.com/)
**Yole Support**: ✅ Syntax highlighting, markdown viewing

---

## Overview

R Markdown combines Markdown syntax with embedded R code chunks, allowing you to create dynamic documents that integrate narrative text, code, and results. It's the standard for reproducible research in the R community.

### Why R Markdown?

- **Reproducible**: Code and results in one document
- **Dynamic**: Automatically updates when data changes
- **Flexible Output**: HTML, PDF, Word, slides, dashboards
- **Literate Programming**: Mix code and explanation
- **Professional**: Publication-quality output
- **R Integration**: Native R ecosystem support

---

## Basic Structure

### Complete Document

````rmd
---
title: "My R Markdown Document"
author: "John Doe"
date: "2025-11-11"
output: html_document
---

# Introduction

This is a **R Markdown** document.

```{r setup, include=FALSE}
knitr::opts_chunk$set(echo = TRUE)
```

## Analysis

Let's analyze some data:

```{r}
# Load data
data <- read.csv("data.csv")

# Summary statistics
summary(data)
```

The mean value is `r mean(data$value)`.

## Visualization

```{r plot, fig.cap="My Plot"}
plot(data$x, data$y)
```

## Conclusion

Results show that...
````

**Components**:
1. **YAML header**: Document metadata
2. **Markdown**: Narrative text
3. **Code chunks**: R code blocks
4. **Inline code**: Embedded R expressions

---

## YAML Header

### Basic Header

```yaml
---
title: "Document Title"
author: "Author Name"
date: "2025-11-11"
output: html_document
---
```

### Common Options

```yaml
---
title: "My Analysis"
author: "Data Scientist"
date: "`r Sys.Date()`"
output:
  html_document:
    toc: true
    toc_depth: 2
    toc_float: true
    theme: cosmo
    highlight: tango
    code_folding: hide
---
```

### Multiple Output Formats

```yaml
---
title: "Report"
output:
  html_document:
    toc: true
  pdf_document:
    toc: true
  word_document:
    toc: true
---
```

### Advanced Options

```yaml
---
title: "Advanced Report"
subtitle: "Detailed Analysis"
author:
  - name: "John Doe"
    affiliation: "University"
date: "`r format(Sys.time(), '%B %d, %Y')`"
output:
  html_document:
    theme: cerulean
    highlight: haddock
    toc: true
    toc_float:
      collapsed: false
      smooth_scroll: true
    code_download: true
    df_print: paged
bibliography: references.bib
csl: apa.csl
---
```

---

## R Code Chunks

### Basic Chunk

````rmd
```{r}
x <- 10
y <- 20
x + y
```
````

### Named Chunk

````rmd
```{r load-data}
data <- read.csv("data.csv")
head(data)
```
````

### Chunk with Options

````rmd
```{r analysis, echo=TRUE, message=FALSE, warning=FALSE}
library(dplyr)

data %>%
  group_by(category) %>%
  summarize(mean_value = mean(value))
```
````

---

## Chunk Options

### Display Options

````rmd
```{r, echo=FALSE}
# Code is hidden, only output shown
```

```{r, results='hide'}
# Output is hidden
```

```{r, include=FALSE}
# Code runs but nothing is shown
```

```{r, eval=FALSE}
# Code is shown but not executed
```
````

### Figure Options

````rmd
```{r, fig.width=8, fig.height=6}
plot(x, y)
```

```{r, fig.cap="My Caption"}
plot(x, y)
```

```{r, fig.align='center'}
plot(x, y)
```
````

### Message and Warning Control

````rmd
```{r, message=FALSE}
# Suppress messages
library(tidyverse)
```

```{r, warning=FALSE}
# Suppress warnings
```

```{r, error=TRUE}
# Continue even if code has errors
```
````

### Caching

````rmd
```{r, cache=TRUE}
# Results are cached for faster re-rendering
expensive_computation()
```
````

---

## Inline R Code

### Basic Inline Code

```rmd
The mean is `r mean(data$value)`.

There are `r nrow(data)` observations.

The date is `r Sys.Date()`.
```

### Formatted Inline Code

```rmd
The average price is $`r round(mean(prices), 2)`.

Percentage: `r sprintf("%.1f%%", percentage * 100)`.

P-value: `r format.pval(p_value, digits=3)`.
```

---

## Output Formats

### HTML Document

```yaml
output:
  html_document:
    toc: true
    theme: flatly
    highlight: kate
```

### PDF Document

```yaml
output:
  pdf_document:
    toc: true
    number_sections: true
    fig_caption: true
    keep_tex: true
```

### Word Document

```yaml
output:
  word_document:
    reference_docx: template.docx
```

### Presentation Slides

```yaml
output:
  ioslides_presentation:
    widescreen: true
    smaller: true
  beamer_presentation:
    theme: "Madrid"
```

### Dashboard

```yaml
output:
  flexdashboard::flex_dashboard:
    orientation: columns
    vertical_layout: fill
```

---

## R Markdown in Yole

### Supported Features

✅ **YAML Header**: View metadata
✅ **Markdown Content**: Full markdown support
✅ **Code Chunks**: Syntax highlighting
✅ **Inline Code**: View R expressions
✅ **Text Editing**: Edit as plain text

### Syntax Highlighting

Yole highlights:
- **YAML**: Header metadata
- **Markdown**: Standard markdown syntax
- **R Code**: Code chunk syntax highlighting
- **Chunk Options**: Option highlighting
- **Inline code**: `r` expressions

### Limitations

❌ **Knitting**: Cannot render to HTML/PDF
❌ **Code Execution**: R code not executed
❌ **Preview**: No rendered output view
❌ **R Environment**: No R runtime

**Recommendation**: Use RStudio for knitting, Yole for editing.

---

## Common Use Cases

### 1. Data Analysis Report

````rmd
---
title: "Sales Analysis Q4 2025"
author: "Analytics Team"
date: "`r Sys.Date()`"
output:
  html_document:
    toc: true
    theme: cosmo
---

```{r setup, include=FALSE}
knitr::opts_chunk$set(echo = TRUE, message = FALSE, warning = FALSE)
library(tidyverse)
library(knitr)
```

# Executive Summary

Key findings from Q4 2025 sales data analysis.

# Data Loading

```{r load-data}
sales <- read_csv("sales_q4_2025.csv")
glimpse(sales)
```

Dataset contains `r nrow(sales)` transactions.

# Summary Statistics

```{r summary-stats}
sales_summary <- sales %>%
  summarize(
    total_revenue = sum(amount),
    avg_transaction = mean(amount),
    num_transactions = n()
  )

kable(sales_summary, caption = "Q4 Summary")
```

Total revenue: $`r format(sales_summary$total_revenue, big.mark=",")`.

# Visualizations

```{r revenue-plot, fig.width=10, fig.height=6}
sales %>%
  group_by(month) %>%
  summarize(revenue = sum(amount)) %>%
  ggplot(aes(x = month, y = revenue)) +
  geom_col(fill = "steelblue") +
  labs(title = "Monthly Revenue", y = "Revenue ($)") +
  theme_minimal()
```

# Conclusion

Q4 showed strong performance with...
````

### 2. Statistical Analysis

````rmd
---
title: "Statistical Testing Example"
output: pdf_document
---

# Hypothesis Testing

Testing effect of treatment on outcome.

## Data Preparation

```{r}
set.seed(42)
control <- rnorm(30, mean = 100, sd = 15)
treatment <- rnorm(30, mean = 110, sd = 15)
```

## Descriptive Statistics

```{r}
data.frame(
  Group = c("Control", "Treatment"),
  Mean = c(mean(control), mean(treatment)),
  SD = c(sd(control), sd(treatment))
)
```

## T-Test

```{r}
test_result <- t.test(treatment, control)
print(test_result)
```

## Interpretation

The p-value is `r round(test_result$p.value, 4)`.
We `r ifelse(test_result$p.value < 0.05, "reject", "fail to reject")` the null hypothesis.

## Visualization

```{r, fig.width=8}
boxplot(control, treatment,
        names = c("Control", "Treatment"),
        main = "Group Comparison",
        ylab = "Value")
```
````

### 3. Reproducible Research Paper

````rmd
---
title: "Research Paper Title"
author: "Researchers"
date: "`r Sys.Date()`"
output:
  pdf_document:
    number_sections: true
bibliography: references.bib
---

# Abstract

Brief summary of research...

# Introduction

Background and motivation [@citation2025].

# Methods

## Data Collection

```{r}
# Simulated data for example
data <- data.frame(
  id = 1:100,
  treatment = sample(c("A", "B"), 100, replace = TRUE),
  outcome = rnorm(100)
)
```

## Statistical Analysis

Linear model:

```{r}
model <- lm(outcome ~ treatment, data = data)
summary(model)
```

# Results

```{r results-table}
kable(summary(model)$coefficients, digits = 3)
```

The treatment effect was `r round(coef(model)[2], 2)` (p = `r format.pval(summary(model)$coefficients[2, 4])`).

# Discussion

Findings suggest...

# References

References automatically generated from bibliography.
````

### 4. Tutorial / Educational Document

````rmd
---
title: "Introduction to Data Analysis in R"
output:
  html_document:
    code_folding: show
    toc: true
    toc_float: true
---

# Getting Started

Welcome to R data analysis!

## Installing Packages

```{r, eval=FALSE}
install.packages("tidyverse")
```

## Loading Libraries

```{r, message=FALSE}
library(tidyverse)
```

# Working with Data

## Creating Data

```{r}
# Create a data frame
students <- data.frame(
  name = c("Alice", "Bob", "Charlie"),
  age = c(20, 22, 21),
  grade = c(85, 90, 88)
)

students
```

## Basic Operations

```{r}
# Mean grade
mean(students$grade)

# Filter students
students %>% filter(age >= 21)
```

# Visualization

## Simple Plot

```{r, fig.width=6, fig.height=4}
ggplot(students, aes(x = name, y = grade)) +
  geom_col(fill = "skyblue") +
  theme_minimal() +
  labs(title = "Student Grades")
```

# Practice Exercises

Try these on your own:

1. Calculate the median age
2. Add a new student
3. Create a scatter plot

# Solutions

```{r, class.source = 'fold-hide'}
# 1. Median age
median(students$age)

# 2. Add student
new_student <- data.frame(name = "David", age = 23, grade = 92)
students <- rbind(students, new_student)

# 3. Scatter plot
plot(students$age, students$grade)
```
````

---

## Best Practices

### Document Organization

**Good structure**:
```rmd
1. YAML header (metadata)
2. Setup chunk (libraries, options)
3. Introduction (markdown)
4. Analysis sections (code + markdown)
5. Conclusion (markdown)
```

### Chunk Naming

**Use descriptive names**:
````rmd
```{r load-data}
```

```{r clean-data}
```

```{r visualize-results}
```
````

### Chunk Options

**Set global defaults**:
````rmd
```{r setup, include=FALSE}
knitr::opts_chunk$set(
  echo = TRUE,
  message = FALSE,
  warning = FALSE,
  fig.width = 8,
  fig.height = 6
)
```
````

### Reproducibility

**Use session info**:
````rmd
```{r session-info}
sessionInfo()
```
````

**Set seed for randomness**:
````rmd
```{r}
set.seed(42)
```
````

---

## Tips & Tricks

### 🎯 Pro Tips

1. **Name chunks**: Easier debugging
2. **Cache expensive operations**: Faster rendering
3. **Use templates**: Create reusable formats
4. **Version control**: Track .Rmd files in Git
5. **Parameterized reports**: Use params for flexibility

### 🚀 Power User Techniques

**Parameterized reports**:
```yaml
---
title: "Report"
params:
  year: 2025
  region: "North"
---
```

````rmd
```{r}
# Use params
data <- load_data(params$year, params$region)
```
````

**Child documents**:
````rmd
```{r, child='analysis-section.Rmd'}
```
````

**Custom engines**:
````rmd
```{python}
print("Python code in R Markdown!")
```

```{bash}
ls -la
```
````

**Tables**:
````rmd
```{r}
library(knitr)
kable(data, caption = "My Table")
```
````

---

## External Resources

### Official
- [R Markdown Website](https://rmarkdown.rstudio.com/)
- [R Markdown Cheatsheet](https://rstudio.com/wp-content/uploads/2015/02/rmarkdown-cheatsheet.pdf)
- [R Markdown Cookbook](https://bookdown.org/yihui/rmarkdown-cookbook/)

### Tools
- **RStudio**: IDE with R Markdown support
- **knitr**: R package for dynamic reports
- **pandoc**: Universal document converter
- **Yole**: Mobile R Markdown editor

### Books
- [R Markdown: The Definitive Guide](https://bookdown.org/yihui/rmarkdown/)
- [R for Data Science](https://r4ds.had.co.nz/)
- [bookdown](https://bookdown.org/yihui/bookdown/)

### Packages
- `rmarkdown`: Core package
- `knitr`: Knitting engine
- `bookdown`: Books and long documents
- `blogdown`: Websites and blogs
- `flexdashboard`: Dashboards

---

## Next Steps

- **[Jupyter Notebook →](./jupyter.md)** - Python alternative
- **[Markdown Format →](./markdown.md)** - Base syntax
- **[Back to Getting Started →](../getting-started.md)**

---

*Last updated: November 11, 2025*
*Yole version: 2.15.1+*
