# Key-Value Format Guide

## Overview

Key-Value format stores data as simple key-value pairs, commonly used for configuration files and data exchange.

## Supported Extensions

- `.keyvalue` - Custom key-value
- `.conf` - Configuration
- `.cfg` - Config file

## Format

```keyvalue
# Comments start with #
key=value
name=John Doe
email=john@example.com
```

## Nested Values

```keyvalue
[section]
key=value

[nested]
level1.level2=value
```

## Types

```keyvalue
string=hello
number=42
float=3.14
boolean=true
array=item1,item2,item3
```

## Examples

```keyvalue
# Database config
database.host=localhost
database.port=5432
database.name=myapp

# API keys
api.key=your-api-key-here
api.secret=your-secret-here
```

## Parser Features

- Section support ([section])
- Type inference
- Array parsing
- Nested keys (dot notation)
- Comments (# and //)
