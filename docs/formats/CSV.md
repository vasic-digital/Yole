# CSV Format Guide

## Overview

CSV (Comma-Separated Values) is a plain-text format for tabular data. Yole provides comprehensive CSV support with automatic delimiter detection, quoting handling, and various encoding options.

## Supported Extensions

- `.csv` - Comma-separated values
- `.tsv` - Tab-separated values
- `.ssv` - Semicolon-separated values

## Format Specification

### Basic CSV
```csv
Name,Email,Phone
John Doe,john@example.com,555-0100
Jane Smith,jane@example.com,555-0101
```

### With Quoting
```csv
Name,Description
John,"Software Engineer, Full Stack"
Jane,"Product Manager, Tech"
```

### Multi-line Values
```csv
Name,Quote
Alice,"This is a
multi-line
value"
Bob,"Single line"
```

## Parser Features

- Automatic delimiter detection (, ; \t |)
- Quote handling (single/double)
- Escape character support
- Header row detection
- Type inference
- Encoding detection (UTF-8, ISO-8859-1)

## Configuration Options

- `delimiter`: Auto-detect, comma, semicolon, tab, pipe
- `hasHeader`: Boolean to indicate first row is header
- `quoteChar`: Character for quoting (default: ")
- `escapeChar`: Escape character (default: \)
- `trim`: Trim whitespace from values

## See Also

- [RFC 4180](https://tools.ietf.org/html/rfc4180)
