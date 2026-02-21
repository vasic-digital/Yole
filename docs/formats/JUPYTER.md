# Jupyter Notebook Format Guide

## Overview

Jupyter Notebook is an open document format that combines live code, equations, visualizations, and narrative text.

## Supported Extensions

- `.ipynb` - Jupyter Notebook

## Structure

```json
{
 "cells": [
  {
   "cell_type": "markdown",
   "metadata": {},
   "source": ["# Title"]
  },
  {
   "cell_type": "code",
   "execution_count": 1,
   "metadata": {},
   "outputs": [],
   "source": ["print('Hello')"]
  }
 ],
 "metadata": {
  "kernelspec": {
   "display_name": "Python 3",
   "language": "python"
  }
 }
}
```

## Cell Types

- **Markdown**: Rich text with headers, lists, links
- **Code**: Executable code with output
- **Raw**: Unformatted content
- **Heading**: Section headers (deprecated)

## Metadata

```json
"metadata": {
 "kernelspec": {"name": "python3"},
 "language_info": {"name": "python", "version": "3.11"}
}
```

## Code Cell Outputs

```json
{
 "outputs": [
  {"output_type": "stream", "text": ["Hello\n"]},
  {"output_type": "execute_result", "data": {"text/plain": ["42"]}},
  {"output_type": "display_data", "data": {"image/png": "..."}}
 ]
}
```

## See Also

- [Jupyter](https://jupyter.org)
- [nbformat](https://nbformat.readthedocs.io)
