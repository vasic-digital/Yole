# CSV Format Guide

**Format**: CSV (Comma-Separated Values)
**Extensions**: `.csv`
**Specification**: [RFC 4180](https://tools.ietf.org/html/rfc4180)
**Yole Support**: ✅ Full (parsing, table display, syntax highlighting)

---

## Overview

CSV (Comma-Separated Values) is a simple, universal format for storing tabular data in plain text. Each line represents a row, and values are separated by commas.

### Why CSV?

- **Universal**: Supported by every spreadsheet app
- **Simple**: Easy to read and write
- **Portable**: Works everywhere (Excel, Google Sheets, databases)
- **Lightweight**: Small file size
- **Future-Proof**: Plain text, no proprietary format
- **Scriptable**: Easy to process with code

---

## Basic Syntax

### Simple CSV

```csv
Name,Age,City
Alice,30,New York
Bob,25,Los Angeles
Charlie,35,Chicago
```

**Structure**:
- **First row**: Headers (column names)
- **Subsequent rows**: Data
- **Separator**: Comma (`,`)

### Without Headers

```csv
Alice,30,New York
Bob,25,Los Angeles
Charlie,35,Chicago
```

Valid CSV doesn't require headers, but they're highly recommended for clarity.

---

## Advanced Syntax

### Quoted Fields

Use double quotes when fields contain:
- Commas
- Newlines
- Double quotes themselves

```csv
Name,Description,Price
"Laptop, 15-inch","High-performance laptop",1299.99
"Monitor, 27-inch","4K display",499.99
```

### Embedded Quotes

Escape quotes by doubling them:

```csv
Name,Quote
Alice,"She said, ""Hello!"""
Bob,"He replied, ""Hi there!"""
```

**Result**:
- Alice: She said, "Hello!"
- Bob: He replied, "Hi there!"

### Multi-line Fields

```csv
Name,Address,Notes
Alice,"123 Main St
Apt 4B
New York, NY 10001","VIP customer"
Bob,"456 Oak Ave
Los Angeles, CA 90001","Regular customer"
```

### Empty Fields

```csv
Name,Email,Phone
Alice,alice@example.com,555-1234
Bob,,555-5678
Charlie,charlie@example.com,
```

**Result**:
- Alice: Has both email and phone
- Bob: Missing email
- Charlie: Missing phone

---

## CSV in Yole

### Table View

Yole renders CSV files as formatted tables:

1. **Headers**: Bold, centered
2. **Rows**: Alternating colors for readability
3. **Columns**: Auto-sized to content
4. **Scrolling**: Horizontal and vertical as needed

### Landscape Mode (Android)

For wide tables:
1. Rotate device to landscape
2. Table expands to fill screen
3. More columns visible

### Syntax Highlighting

Yole highlights:
- **Headers**: Bold text in first row
- **Delimiters**: Visible commas
- **Quoted fields**: Different color
- **Numbers**: Numeric highlighting

### Editing

**Edit mode**:
- Raw CSV text with syntax highlighting
- Easy to add/remove rows and columns

**Tips**:
- Align columns for readability (not required)
- Use consistent quoting style
- Validate with Preview mode

---

## Common Use Cases

### 1. Contact List

```csv
Name,Email,Phone,Company
Alice Johnson,alice@company.com,555-0101,TechCorp
Bob Smith,bob@startup.com,555-0102,InnovateLabs
Charlie Davis,charlie@enterprise.com,555-0103,MegaSoft
```

### 2. Product Catalog

```csv
SKU,Product,Category,Price,Stock
A001,"Laptop, 15-inch",Electronics,1299.99,45
A002,Wireless Mouse,Accessories,29.99,200
A003,"Monitor, 27-inch 4K",Electronics,499.99,30
```

### 3. Financial Data

```csv
Date,Category,Description,Amount,Balance
2025-11-01,Income,Salary,5000.00,5000.00
2025-11-05,Expense,Rent,-1500.00,3500.00
2025-11-10,Expense,Groceries,-200.00,3300.00
2025-11-15,Income,Freelance,500.00,3800.00
```

### 4. Survey Results

```csv
Timestamp,Name,Rating,Feedback
2025-11-11 10:00,Alice,5,"Excellent service!"
2025-11-11 11:30,Bob,4,"Good, but could improve response time"
2025-11-11 14:15,Charlie,5,"Very satisfied"
```

### 5. Project Tasks

```csv
Task ID,Task,Assignee,Status,Due Date,Priority
T001,Design homepage,Alice,In Progress,2025-11-15,High
T002,Implement API,Bob,Not Started,2025-11-20,High
T003,Write documentation,Charlie,Completed,2025-11-10,Medium
T004,Test functionality,Alice,Not Started,2025-11-25,Medium
```

### 6. Inventory Tracking

```csv
Item,Location,Quantity,Unit,Last Updated
Paper Reams,Storage A,150,Box,2025-11-10
Pens,Storage B,500,Each,2025-11-09
Notebooks,Storage A,75,Each,2025-11-11
Staplers,Storage C,20,Each,2025-11-08
```

---

## Best Practices

### File Organization

**Naming**:
```
contacts-2025.csv          # Include year
products-export-20251111.csv # Include date
survey-results-q4.csv      # Include period
```

**Multiple files**:
```
data/
├── customers.csv
├── orders.csv
├── products.csv
└── archive/
    ├── customers-2024.csv
    └── orders-2024.csv
```

### Header Guidelines

**Good headers**:
```csv
Customer_ID,Full_Name,Email_Address,Phone_Number,Join_Date
```

**Better headers**:
```csv
customer_id,full_name,email,phone,join_date
```

**Rules**:
- Use descriptive names
- Avoid spaces (use underscores or camelCase)
- Keep consistent naming convention
- Be concise but clear

### Data Quality

**Consistent formatting**:
```csv
# Good - consistent date format
Date,Event
2025-11-01,Meeting
2025-11-15,Conference

# Bad - inconsistent dates
Date,Event
11/1/2025,Meeting
2025-11-15,Conference
```

**Use appropriate types**:
```csv
# Numbers without quotes (unless needed)
Price,Quantity
19.99,100
29.99,50

# Dates in ISO format
Date,Event
2025-11-11,Launch
2025-12-25,Holiday
```

### Handling Special Cases

**Large numbers**:
```csv
Population,GDP
1500000,50000000000
```

Don't use commas in numbers: `1,500,000` → `1500000`

**Booleans**:
```csv
Name,Active,Premium
Alice,true,false
Bob,false,true
```

Or use: `yes/no`, `1/0`, `Y/N`

**Null values**:
```csv
Name,Middle_Name,Email
Alice,Marie,alice@example.com
Bob,,bob@example.com
Charlie,Lee,
```

Empty field = null/missing value

---

## Advanced Techniques

### Different Delimiters

**Tab-separated (TSV)**:
```tsv
Name	Age	City
Alice	30	New York
Bob	25	Los Angeles
```

**Semicolon-separated** (common in Europe):
```csv
Name;Age;City
Alice;30;New York
Bob;25;Los Angeles
```

**Pipe-separated**:
```csv
Name|Age|City
Alice|30|New York
Bob|25|Los Angeles
```

**Note**: Yole expects comma-separated by default.

### Exporting from Spreadsheets

**From Excel**:
1. File → Save As
2. Choose "CSV (Comma delimited) (*.csv)"
3. Click Save

**From Google Sheets**:
1. File → Download
2. Choose "Comma Separated Values (.csv)"

**From LibreOffice Calc**:
1. File → Save As
2. File type: "Text CSV (.csv)"
3. Choose delimiter and encoding

### Importing to Databases

**SQL Server**:
```sql
BULK INSERT table_name
FROM 'C:\path\to\file.csv'
WITH (FIELDTERMINATOR = ',', ROWTERMINATOR = '\n', FIRSTROW = 2);
```

**MySQL**:
```sql
LOAD DATA INFILE '/path/to/file.csv'
INTO TABLE table_name
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS;
```

**PostgreSQL**:
```sql
COPY table_name
FROM '/path/to/file.csv'
DELIMITER ','
CSV HEADER;
```

---

## Tips & Tricks

### 🎯 Pro Tips

1. **Always use headers**: Makes data self-documenting
2. **Quote when in doubt**: Safer to over-quote than under-quote
3. **Consistent delimiters**: Stick to commas
4. **UTF-8 encoding**: Handles international characters
5. **Test with preview**: Verify format before sharing

### 🚀 Power User Techniques

**Creating CSV from command line**:
```bash
# Simple CSV
echo "Name,Age,City" > people.csv
echo "Alice,30,NYC" >> people.csv
echo "Bob,25,LA" >> people.csv

# From database
sqlite3 -header -csv mydb.db "SELECT * FROM users" > users.csv
```

**Processing CSV with tools**:
```bash
# Count rows
wc -l file.csv

# View first 10 rows
head -10 file.csv

# Search for pattern
grep "pattern" file.csv

# Sort by column
sort -t, -k2 file.csv  # Sort by 2nd column
```

**CSV to JSON** (with `jq` or `csvkit`):
```bash
# Using csvkit
csvjson file.csv > file.json

# Using Miller
mlr --icsv --ojson cat file.csv > file.json
```

### 📊 Productivity Patterns

**Template files**:
```csv
# Save as template
Date,Category,Amount,Notes
,,,

# Use for monthly expenses
```

**Data validation**:
```csv
# Use consistent formats
Email,Phone,ZIP
user@domain.com,555-0101,12345
user@domain.com,555-0102,67890
```

**Batch operations**:
```bash
# Merge multiple CSV files
cat file1.csv > merged.csv
tail -n +2 file2.csv >> merged.csv  # Skip header
tail -n +2 file3.csv >> merged.csv
```

---

## Troubleshooting

### Commas in data breaking columns?
**Solution**: Quote the field
```csv
# Wrong
Product,Price
Laptop, 15-inch,1299.99

# Correct
Product,Price
"Laptop, 15-inch",1299.99
```

### Quotes not displaying correctly?
**Solution**: Escape with double quotes
```csv
# Wrong
Name,Quote
Alice,"She said, "Hello!""

# Correct
Name,Quote
Alice,"She said, ""Hello!"""
```

### Excel opening CSV incorrectly?
**Solutions**:
1. Use "Text Import Wizard" instead of double-click
2. Change file extension to `.txt`, then import
3. Specify UTF-8 encoding
4. Set delimiter explicitly

### Special characters garbled?
**Solution**: Use UTF-8 encoding
1. Save file as UTF-8
2. Add BOM (Byte Order Mark) for Excel compatibility
3. Test opening in different apps

### Numbers formatted as text?
**In Excel**:
- Numbers should not be quoted in CSV
- Remove leading zeros if not needed
- Use numeric format: `1234.56` not `"1,234.56"`

---

## External Resources

### Specifications
- [RFC 4180](https://tools.ietf.org/html/rfc4180) - Official CSV specification
- [CSV on Wikipedia](https://en.wikipedia.org/wiki/Comma-separated_values) - Detailed explanation

### Tools
- [csvkit](https://csvkit.readthedocs.io/) - Command-line CSV tools
- [Miller](https://miller.readthedocs.io/) - Text processing tool for CSV
- [Papa Parse](https://www.papaparse.com/) - JavaScript CSV parser
- [CSVLint](https://csvlint.io/) - CSV validation tool

### Converters
- [Mr. Data Converter](https://shancarter.github.io/mr-data-converter/) - CSV to various formats
- [ConvertCSV](https://www.convertcsv.com/) - CSV conversion tools
- [CSV to JSON](https://csvjson.com/csv2json) - Online converter

### Learning
- [CSV Tutorial](https://www.tutorialspoint.com/articles/what-is-csv-file-and-how-to-open-it) - Basics
- [Working with CSV in Python](https://realpython.com/python-csv/) - Programming guide

---

## Examples

### Minimal CSV
```csv
Name,Age
Alice,30
Bob,25
```

### Complex CSV
```csv
"ID","Name","Email","Phone","Address","Notes","Join Date","Status"
1,"Alice Johnson","alice@company.com","555-0101","123 Main St, Apt 4B
New York, NY 10001","VIP customer, prefers email contact","2024-01-15","Active"
2,"Bob Smith","bob@startup.com","","456 Oak Ave
Los Angeles, CA 90001","New customer","2024-06-20","Active"
3,"Charlie Davis","charlie@enterprise.com","555-0103","789 Pine Rd
Chicago, IL 60601","Bulk orders, ""special pricing"" applied","2024-03-10","Inactive"
```

### Real-World: Sales Data
```csv
Date,Product,Quantity,Unit_Price,Total,Customer,Region
2025-11-01,"Laptop, 15-inch",2,1299.99,2599.98,Acme Corp,West
2025-11-02,Wireless Mouse,10,29.99,299.90,TechStart Inc,East
2025-11-03,"Monitor, 27-inch",5,499.99,2499.95,Global Systems,West
2025-11-04,Keyboard,8,79.99,639.92,Acme Corp,West
2025-11-05,"Laptop, 15-inch",1,1299.99,1299.99,InnovateLabs,South
```

---

## Next Steps

- **[Markdown Format →](./markdown.md)** - Rich text documents
- **[Todo.txt Format →](./todotxt.md)** - Task management
- **[Plain Text Format →](./plaintext.md)** - Simple text
- **[Back to Getting Started →](../getting-started.md)**

---

*Last updated: November 11, 2025*
*Yole version: 2.15.1+*
