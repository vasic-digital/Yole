# Jupyter Notebook Format Guide

**Format**: Jupyter Notebook
**Extensions**: `.ipynb`
**Specification**: [nbformat](https://nbformat.readthedocs.io/)
**Yole Support**: ✅ Syntax highlighting, JSON viewing

---

## Overview

Jupyter Notebooks are interactive documents that combine code, visualizations, and narrative text. The `.ipynb` format is a JSON file containing cells of different types, allowing for reproducible data science workflows.

### Why Jupyter Notebooks?

- **Interactive**: Execute code and see results immediately
- **Reproducible**: Share complete analysis with code and outputs
- **Multi-Language**: Python, R, Julia, and 100+ kernels
- **Visualization**: Embed plots and rich media
- **Documentation**: Mix code with markdown explanations
- **Educational**: Perfect for tutorials and teaching

---

## File Format

### Basic Structure

```json
{
  "cells": [
    {
      "cell_type": "markdown",
      "metadata": {},
      "source": ["# My Notebook\n", "This is a markdown cell."]
    },
    {
      "cell_type": "code",
      "execution_count": 1,
      "metadata": {},
      "outputs": [],
      "source": ["print('Hello, World!')"]
    }
  ],
  "metadata": {
    "kernelspec": {
      "display_name": "Python 3",
      "language": "python",
      "name": "python3"
    },
    "language_info": {
      "name": "python",
      "version": "3.9.0"
    }
  },
  "nbformat": 4,
  "nbformat_minor": 4
}
```

**Top-level structure**:
- `cells`: Array of cell objects
- `metadata`: Notebook-level metadata
- `nbformat`: Format version (4 is current)
- `nbformat_minor`: Minor version

---

## Cell Types

### Markdown Cells

```json
{
  "cell_type": "markdown",
  "metadata": {},
  "source": [
    "# Heading 1\n",
    "\n",
    "This is **bold** and *italic* text.\n",
    "\n",
    "```python\n",
    "# Code example in markdown\n",
    "x = 10\n",
    "```\n"
  ]
}
```

**Purpose**: Documentation, explanations, formatted text.

### Code Cells

```json
{
  "cell_type": "code",
  "execution_count": 1,
  "metadata": {},
  "outputs": [
    {
      "name": "stdout",
      "output_type": "stream",
      "text": ["Hello, World!\n"]
    }
  ],
  "source": [
    "# Python code\n",
    "print('Hello, World!')\n",
    "x = 10\n",
    "y = 20\n",
    "print(f'Sum: {x + y}')"
  ]
}
```

**Components**:
- `execution_count`: Execution order number
- `outputs`: Array of output objects
- `source`: Code content (array of strings)

### Raw Cells

```json
{
  "cell_type": "raw",
  "metadata": {},
  "source": [
    "Raw text that is not executed or rendered.\n",
    "Useful for LaTeX, custom formats, etc."
  ]
}
```

**Purpose**: Pass-through text for conversion tools.

---

## Cell Outputs

### Stream Output (print statements)

```json
{
  "output_type": "stream",
  "name": "stdout",
  "text": ["Output line 1\n", "Output line 2\n"]
}
```

### Display Data (plots, images)

```json
{
  "output_type": "display_data",
  "data": {
    "text/plain": ["<matplotlib.figure.Figure>"],
    "image/png": "iVBORw0KGgoAAAANS..."
  },
  "metadata": {}
}
```

### Execute Result (return values)

```json
{
  "output_type": "execute_result",
  "execution_count": 1,
  "data": {
    "text/plain": ["42"]
  },
  "metadata": {}
}
```

### Error Output

```json
{
  "output_type": "error",
  "ename": "NameError",
  "evalue": "name 'undefined_var' is not defined",
  "traceback": [
    "\u001b[0;31m---------------------------------------------------------------------------\u001b[0m",
    "\u001b[0;31mNameError\u001b[0m..."
  ]
}
```

---

## Metadata

### Notebook Metadata

```json
{
  "metadata": {
    "kernelspec": {
      "display_name": "Python 3",
      "language": "python",
      "name": "python3"
    },
    "language_info": {
      "codemirror_mode": {"name": "ipython", "version": 3},
      "file_extension": ".py",
      "mimetype": "text/x-python",
      "name": "python",
      "nbconvert_exporter": "python",
      "pygments_lexer": "ipython3",
      "version": "3.9.0"
    },
    "widgets": {
      "application/vnd.jupyter.widget-state+json": {}
    }
  }
}
```

### Cell Metadata

```json
{
  "cell_type": "code",
  "metadata": {
    "collapsed": false,
    "tags": ["hide-input"],
    "slideshow": {"slide_type": "slide"}
  },
  "source": ["# Code here"]
}
```

---

## Jupyter in Yole

### Supported Features

✅ **JSON Viewing**: View notebook structure
✅ **Syntax Highlighting**: JSON formatting
✅ **Cell Content**: Read markdown and code
✅ **Metadata**: View notebook metadata
✅ **Text Editing**: Edit as JSON

### Viewing in Yole

Yole displays the raw JSON format:
- Cell arrays visible
- Source code readable
- Outputs preserved
- Metadata accessible

### Limitations

❌ **Execution**: Cannot run code cells
❌ **Rendering**: No visual notebook view
❌ **Kernel**: No kernel support
❌ **Interactive Widgets**: Not supported
❌ **Rich Outputs**: Images/plots not displayed

**Recommendation**: Use JupyterLab/Notebook for execution, Yole for quick text viewing/editing.

---

## Common Use Cases

### 1. Data Analysis

**Example notebook structure**:

**Cell 1 (Markdown)**:
```markdown
# Sales Data Analysis

Analyzing Q4 2025 sales data.

## Objectives
- Load and clean data
- Calculate summary statistics
- Visualize trends
```

**Cell 2 (Code)**:
```python
import pandas as pd
import matplotlib.pyplot as plt

# Load data
df = pd.read_csv('sales.csv')
print(f"Loaded {len(df)} records")
```

**Cell 3 (Code)**:
```python
# Summary statistics
print(df.describe())
print(f"\nTotal Sales: ${df['amount'].sum():,.2f}")
```

**Cell 4 (Code)**:
```python
# Visualization
df.groupby('month')['amount'].sum().plot(kind='bar')
plt.title('Monthly Sales')
plt.ylabel('Amount ($)')
plt.show()
```

### 2. Machine Learning

**Typical ML notebook**:

**Setup**:
```python
# Cell 1: Imports
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report
```

**Data Loading**:
```python
# Cell 2: Load data
data = pd.read_csv('dataset.csv')
X = data.drop('target', axis=1)
y = data['target']
print(f"Features: {X.shape[1]}, Samples: {X.shape[0]}")
```

**Training**:
```python
# Cell 3: Train model
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2)
model = RandomForestClassifier(n_estimators=100)
model.fit(X_train, y_train)
```

**Evaluation**:
```python
# Cell 4: Evaluate
y_pred = model.predict(X_test)
accuracy = accuracy_score(y_test, y_pred)
print(f"Accuracy: {accuracy:.2%}")
print(classification_report(y_test, y_pred))
```

### 3. Tutorial / Educational

**Teaching Python basics**:

```markdown
# Python Basics Tutorial

## Variables and Data Types
```

```python
# Integer
age = 30

# Float
price = 19.99

# String
name = "Alice"

# Boolean
is_student = True

print(f"{name} is {age} years old")
```

```markdown
## Lists and Loops
```

```python
# Create a list
fruits = ['apple', 'banana', 'orange']

# Loop through list
for fruit in fruits:
    print(f"I like {fruit}")

# List comprehension
uppercase = [f.upper() for f in fruits]
print(uppercase)
```

### 4. Research Paper / Report

**Scientific analysis notebook**:

```markdown
# Experiment Results - November 2025

**Author**: Research Team
**Date**: 2025-11-11
**Experiment**: Effect of Variable X on Output Y

## Abstract

Brief description of experiment...

## Methods

### Data Collection

Collected data from...
```

```python
# Load experimental data
import scipy.stats as stats

data_control = [/* data */]
data_treatment = [/* data */]

# Statistical test
t_stat, p_value = stats.ttest_ind(data_control, data_treatment)
print(f"t-statistic: {t_stat:.4f}")
print(f"p-value: {p_value:.4f}")
```

```markdown
## Results

The p-value of {p_value} indicates...

## Conclusion

Based on the analysis...
```

---

## Best Practices

### Notebook Structure

**Good structure**:
```markdown
1. Title and metadata
2. Setup (imports, configuration)
3. Data loading
4. Analysis (step by step)
5. Visualization
6. Conclusions
```

**Example**:
```
├── Title cell (markdown)
├── Imports cell (code)
├── Configuration cell (code)
├── Section 1 heading (markdown)
├── Analysis code (code)
├── Explanation (markdown)
├── Section 2 heading (markdown)
├── More analysis (code)
└── Conclusion (markdown)
```

### Clear Documentation

**Document each step**:
```markdown
## Step 1: Load Data

We load the dataset and perform initial exploration.
```

```python
# Load dataset
df = pd.read_csv('data.csv')

# Check first few rows
df.head()
```

### Reproducibility

**Set random seeds**:
```python
import numpy as np
import random

np.random.seed(42)
random.seed(42)
```

**Pin versions**:
```python
# At top of notebook
import sys
print(f"Python: {sys.version}")
print(f"Pandas: {pd.__version__}")
print(f"NumPy: {np.__version__}")
```

### Clean Outputs

**Clear outputs before sharing**:
- Remove sensitive data
- Clear execution counts
- Remove unnecessary output
- Keep important visualizations

### Cell Organization

**One concept per cell**:
```python
# Good: Single purpose
data = load_data()
```

```python
# Good: Next step in separate cell
cleaned_data = clean_data(data)
```

**Avoid**:
```python
# Bad: Too much in one cell
data = load_data()
cleaned_data = clean_data(data)
analysis = analyze(cleaned_data)
visualize(analysis)
save_results(analysis)
```

---

## Tips & Tricks

### 🎯 Pro Tips

1. **Restart and run all**: Ensure reproducibility
2. **Document assumptions**: Explain choices
3. **Small cells**: Keep cells focused
4. **Clear outputs**: Before committing to Git
5. **Use markdown**: Explain what and why

### 🚀 Power User Techniques

**Magic commands**:
```python
%matplotlib inline  # Display plots inline
%time code_here    # Time execution
%timeit code_here  # Multiple runs timing
%%writefile file.py  # Write cell to file
```

**Cell metadata tags**:
```json
{
  "tags": ["parameters", "hide-input", "hide-output"]
}
```

**Notebook as script**:
```bash
jupyter nbconvert --to script notebook.ipynb
```

**Run specific cells**:
```python
# Cell 1
from IPython.display import display

# Cell 2
%run Cell-1  # Reference by number
```

---

## Working with Notebooks

### Version Control

**Good practices**:
- Clear outputs before committing
- Use `.gitattributes` for better diffs
- Consider using nbdime for notebook diffs

```bash
# .gitattributes
*.ipynb filter=nbstrip_full
```

### Conversion

**Convert to other formats**:
```bash
# To HTML
jupyter nbconvert --to html notebook.ipynb

# To PDF (requires LaTeX)
jupyter nbconvert --to pdf notebook.ipynb

# To Python script
jupyter nbconvert --to script notebook.ipynb

# To Markdown
jupyter nbconvert --to markdown notebook.ipynb
```

### Execution

**Run from command line**:
```bash
jupyter nbconvert --to notebook --execute notebook.ipynb
```

---

## External Resources

### Official
- [Project Jupyter](https://jupyter.org/)
- [JupyterLab Documentation](https://jupyterlab.readthedocs.io/)
- [nbformat Specification](https://nbformat.readthedocs.io/)

### Tools
- **JupyterLab**: Modern notebook interface
- **Jupyter Notebook**: Classic interface
- **VS Code**: Notebook support built-in
- **Google Colab**: Cloud notebooks
- **Kaggle Notebooks**: Data science platform
- **Yole**: Quick JSON viewing/editing

### Extensions
- **nbextensions**: Jupyter Notebook extensions
- **JupyterLab extensions**: Additional features
- **nbconvert**: Format conversion

### Resources
- [Real Python: Jupyter Notebooks](https://realpython.com/jupyter-notebook-introduction/)
- [Jupyter Notebook Best Practices](https://github.com/jupyter/jupyter/wiki/Jupyter-Notebook-Best-Practices)
- [Awesome Jupyter](https://github.com/markusschanta/awesome-jupyter)

---

## Next Steps

- **[R Markdown Format →](./rmarkdown.md)** - R alternative to Jupyter
- **[Markdown Format →](./markdown.md)** - Used in notebook cells
- **[Python Format →](./python.md)** - Code in notebooks
- **[Back to Getting Started →](../getting-started.md)**

---

*Last updated: November 11, 2025*
*Yole version: 2.15.1+*
