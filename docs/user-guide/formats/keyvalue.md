# Key-Value Format Guide

**Format**: Key-Value (Properties, INI, ENV)
**Extensions**: `.properties`, `.ini`, `.env`, `.conf`, `.config`, `.cfg`
**Specification**: Various (Properties, INI, ENV formats)
**Yole Support**: ✅ Syntax highlighting, basic parsing

---

## Overview

Key-Value formats are simple text formats for storing configuration data as key-value pairs. They're widely used for application settings, environment variables, and configuration files.

### Why Key-Value Formats?

- **Simple**: Easy to read and write
- **Universal**: Supported by almost every programming language
- **Human-Friendly**: No complex syntax to learn
- **Portable**: Plain text, works everywhere
- **Version Control**: Git-friendly configuration
- **Fast Parsing**: Quick to load and process

---

## Format Variations

### 1. Java Properties (.properties)

```properties
# Java properties format
app.name=MyApplication
app.version=1.0.0
app.debug=true

# URLs and paths
server.url=https://example.com
config.path=/etc/app/config

# Lists (comma-separated)
allowed.hosts=localhost,example.com,api.example.com
```

**Characteristics**:
- `key=value` or `key:value` or `key value`
- `#` or `!` for comments
- Backslash `\` for line continuation
- Unicode escapes: `\uXXXX`
- Spaces in values preserved

### 2. INI Files (.ini, .cfg)

```ini
; INI format with sections

[database]
host=localhost
port=5432
name=mydb
user=admin
password=secret

[server]
host=0.0.0.0
port=8080
debug=true

[logging]
level=info
file=/var/log/app.log
```

**Characteristics**:
- `[section]` headers
- `key=value` pairs
- `;` or `#` for comments
- Sections group related settings
- Case-sensitive or insensitive (depends on parser)

### 3. Environment Variables (.env)

```env
# Environment variables

APP_NAME=MyApplication
APP_ENV=production
APP_DEBUG=false

DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_NAME=mydb
DATABASE_USER=admin
DATABASE_PASSWORD=secret

# API Keys
API_KEY=abc123xyz789
API_SECRET=supersecret

# Optional values
CACHE_ENABLED=true
CACHE_TTL=3600
```

**Characteristics**:
- `KEY=value` format
- UPPERCASE keys by convention
- No sections
- Used by Docker, Node.js, etc.
- Loaded as environment variables

### 4. Configuration Files (.conf, .config)

```conf
# Application configuration

name = MyApplication
version = 1.0.0

# Server settings
server.host = 0.0.0.0
server.port = 8080
server.timeout = 30

# Feature flags
features.authentication = true
features.caching = true
features.logging = true
```

**Characteristics**:
- Flexible format
- Often hierarchical with dots
- Mixed comment styles
- Sometimes supports sections

---

## Basic Syntax

### Key-Value Pairs

```properties
# Basic assignment
key=value
name=John Doe
age=30
enabled=true

# Alternative delimiters
key:value
key value

# With spaces
key = value
name = John Doe
```

### Comments

```properties
# Hash comment (most common)
key=value

; Semicolon comment (INI files)
key=value

! Exclamation comment (Java properties)
key=value
```

### Line Continuation

```properties
# Backslash for multi-line values
long.value=This is a very long value \
           that spans multiple lines \
           for readability

url=https://example.com/api/v1/\
    endpoint/resource
```

### Empty Values

```properties
# Empty value
empty.key=

# Or just the key (same as empty)
empty.key
```

---

## Data Types

### Strings

```properties
# Plain strings
name=John Doe
message=Hello, World!
path=/home/user/documents

# Quoted strings (some parsers)
name="John Doe"
message='Hello, World!'

# Escaped characters
path=C:\\Users\\John\\Documents
unicode=\u00A9 2025
```

### Numbers

```properties
# Integers
port=8080
count=100
max=-1

# Floats
price=19.99
temperature=98.6
ratio=0.5
```

### Booleans

```properties
# Common boolean values
enabled=true
disabled=false

# Alternative representations
debug=yes
logging=no

# Numeric booleans
active=1
inactive=0
```

### Lists/Arrays

```properties
# Comma-separated (common)
colors=red,green,blue
numbers=1,2,3,4,5

# Space-separated
tags=important urgent todo

# Multi-line
allowed.ips=192.168.1.1,\
            192.168.1.2,\
            192.168.1.3
```

---

## Sections (INI Format)

### Basic Sections

```ini
[section1]
key1=value1
key2=value2

[section2]
key1=value1
key2=value2
```

### Nested Sections

```ini
[database]
host=localhost
port=5432

[database.pool]
min=5
max=20
timeout=30

[server]
host=0.0.0.0
port=8080
```

### Section Comments

```ini
; Database configuration section
[database]
host=localhost

; Server configuration section
[server]
host=0.0.0.0
```

---

## Common Use Cases

### 1. Application Configuration

```properties
# config.properties

# Application Info
app.name=MyApplication
app.version=1.0.0
app.environment=production

# Server Settings
server.host=0.0.0.0
server.port=8080
server.timeout=30
server.max.connections=100

# Database Configuration
db.driver=postgresql
db.host=localhost
db.port=5432
db.name=myapp_db
db.user=app_user
db.password=secret123
db.pool.size=20

# Logging
logging.level=INFO
logging.file=/var/log/myapp.log
logging.max.size=10MB
logging.max.files=5

# Features
feature.auth=true
feature.cache=true
feature.analytics=false

# External Services
api.url=https://api.example.com
api.key=abc123xyz789
api.timeout=10

# Email
email.smtp.host=smtp.example.com
email.smtp.port=587
email.from=noreply@example.com
```

### 2. Environment Variables (.env)

```env
# .env file for local development

# Application
APP_NAME=MyApp
APP_ENV=development
APP_DEBUG=true
APP_URL=http://localhost:8080

# Database
DB_CONNECTION=postgresql
DB_HOST=127.0.0.1
DB_PORT=5432
DB_DATABASE=myapp_dev
DB_USERNAME=dev_user
DB_PASSWORD=dev_password

# Cache
CACHE_DRIVER=redis
REDIS_HOST=127.0.0.1
REDIS_PORT=6379

# Mail
MAIL_DRIVER=smtp
MAIL_HOST=mailhog
MAIL_PORT=1025
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM_ADDRESS=dev@example.com

# AWS (leave empty for local)
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
AWS_REGION=us-east-1

# API Keys
STRIPE_KEY=
STRIPE_SECRET=
```

### 3. INI Configuration File

```ini
; myapp.ini

[application]
name = MyApplication
version = 1.0.0
debug = false

[database]
driver = mysql
host = localhost
port = 3306
database = myapp
username = root
password =
charset = utf8mb4

[database.options]
auto_reconnect = true
timeout = 30
pool_size = 10

[server]
host = 0.0.0.0
port = 8080
workers = 4
timeout = 30

[logging]
level = warning
file = /var/log/myapp.log
max_size = 10MB
rotate = true

[cache]
enabled = true
driver = redis
ttl = 3600

[redis]
host = localhost
port = 6379
database = 0

[security]
allowed_hosts = localhost,example.com
cors_enabled = true
csrf_protection = true

[email]
driver = smtp
host = smtp.gmail.com
port = 587
encryption = tls
username = user@example.com
password = secret
from_address = noreply@example.com
from_name = MyApp
```

### 4. System Configuration

```conf
# System service configuration

# Service identification
name=myservice
description=My Application Service
version=1.0.0

# Runtime
user=appuser
group=appgroup
working_directory=/opt/myapp

# Process management
pid_file=/var/run/myapp.pid
restart_policy=always
restart_delay=5

# Resource limits
max_memory=512M
max_cpu_percent=50
max_open_files=1024

# Networking
bind_address=127.0.0.1
bind_port=8080
socket_timeout=30

# Logging
log_level=info
log_file=/var/log/myapp/service.log
log_rotate=daily
log_max_size=100M
log_retention=30

# Monitoring
health_check_url=/health
health_check_interval=10
metrics_enabled=true
metrics_port=9090
```

### 5. Build Configuration

```properties
# build.properties

# Project Info
project.name=MyProject
project.version=1.0.0
project.group=com.example

# Build Settings
build.target=production
build.optimize=true
build.source.encoding=UTF-8

# Output
build.output.dir=dist
build.temp.dir=build/tmp

# Compiler
compiler.version=11
compiler.warnings=all
compiler.debug=false

# Dependencies
dependency.repo=https://repo.maven.org
dependency.cache=~/.m2/repository

# Testing
test.unit.enabled=true
test.integration.enabled=true
test.coverage.threshold=80

# Deployment
deploy.target=production
deploy.url=https://deploy.example.com
deploy.strategy=rolling
```

---

## Best Practices

### Naming Conventions

**Properties Files**:
```properties
# Use dot notation for hierarchy
app.name=MyApp
app.server.host=localhost
app.server.port=8080

# Lowercase with underscores
database_host=localhost
database_port=5432
```

**Environment Variables**:
```env
# UPPERCASE with underscores
APP_NAME=MyApp
DATABASE_HOST=localhost
DATABASE_PORT=5432
```

**INI Files**:
```ini
[section]
# lowercase or CamelCase
host=localhost
maxConnections=100
```

### Organization

**Group related settings**:
```properties
# Database
db.host=localhost
db.port=5432
db.name=mydb

# Server
server.host=0.0.0.0
server.port=8080
```

Or use INI sections:
```ini
[database]
host=localhost
port=5432

[server]
host=0.0.0.0
port=8080
```

### Comments

**Document settings**:
```properties
# Database connection timeout in seconds
db.timeout=30

# Maximum number of concurrent connections
# Default: 100, Range: 1-1000
server.max.connections=100

# Enable debug mode (use only in development!)
debug=false
```

### Security

**Never commit secrets**:
```env
# BAD: Don't do this
API_KEY=abc123secret

# GOOD: Use placeholder
API_KEY=<set-in-environment>

# GOOD: Reference in documentation
# Set API_KEY environment variable before running
```

**Use separate files**:
```
config/
├── app.properties          # Public settings
├── database.properties     # Connection strings
└── secrets.properties      # Secrets (gitignored)
```

### Environment-Specific

**Use different files per environment**:
```
config/
├── app.properties          # Common settings
├── dev.properties          # Development
├── staging.properties      # Staging
├── production.properties   # Production
└── .env.example            # Template
```

**Override pattern**:
```properties
# app.properties (defaults)
debug=false
server.port=8080

# dev.properties (overrides)
debug=true
server.port=3000
```

---

## Key-Value Formats in Yole

### Supported Features

✅ **Syntax Highlighting**: Keys, values, comments
✅ **Multiple Formats**: .properties, .ini, .env, .conf
✅ **Sections**: INI-style sections
✅ **Comments**: All comment styles
✅ **Line Continuation**: Backslash continuation
✅ **Editing**: Full text editing support

### Syntax Highlighting

Yole highlights:
- **Keys**: Variable names
- **Values**: Assigned values
- **Comments**: Comment lines
- **Sections**: Section headers (INI)
- **Delimiters**: `=`, `:`, spaces
- **Special Characters**: Quotes, escapes

### Limitations

❌ **Validation**: No syntax validation (yet)
❌ **Environment Expansion**: Variables not expanded
❌ **Includes**: No file inclusion
❌ **Type Checking**: No data type validation
❌ **Schema**: No schema validation

**Recommendation**: Use IDE plugins for validation, Yole for editing.

---

## Tips & Tricks

### 🎯 Pro Tips

1. **Use comments liberally**: Document every setting
2. **Group related keys**: Keep configuration organized
3. **Consistent naming**: Stick to one convention
4. **Version control**: Track config changes in Git
5. **Separate secrets**: Never commit sensitive data

### 🚀 Power User Techniques

**Template pattern**:
```properties
# config.template.properties
db.host=localhost
db.port=5432
db.user=REPLACE_ME
db.password=REPLACE_ME
```

**Multi-environment setup**:
```bash
# Load appropriate config
export ENV=production
app --config=config/${ENV}.properties
```

**Environment variable expansion** (if supported):
```properties
# Reference environment variables
home.dir=${HOME}
user.name=${USER}
temp.dir=${TMPDIR}
```

**Conditional configuration**:
```ini
[common]
base_path=/opt/app

[development : common]
debug=true
base_path=/home/dev/app

[production : common]
debug=false
base_path=/opt/app
```

---

## External Resources

### Specifications
- [Java Properties Format](https://docs.oracle.com/javase/8/docs/api/java/util/Properties.html)
- [INI File Format](https://en.wikipedia.org/wiki/INI_file)
- [.env File](https://www.dotenv.org/)

### Tools
- **Properties editors**: IntelliJ IDEA, VS Code
- **Environment management**: dotenv, direnv
- **Configuration libraries**: configparser (Python), ini (Node.js)
- **Yole**: Mobile config editor

### Best Practices
- [The Twelve-Factor App: Config](https://12factor.net/config)
- [Environment Variables Best Practices](https://www.doppler.com/blog/environment-variables-best-practices)

### Libraries

**Python**:
```python
import configparser
config = configparser.ConfigParser()
config.read('config.ini')
```

**JavaScript**:
```javascript
require('dotenv').config()
const value = process.env.API_KEY
```

**Java**:
```java
Properties props = new Properties();
props.load(new FileInputStream("config.properties"));
```

---

## Next Steps

- **[Plain Text Format →](./plaintext.md)** - Simple text files
- **[CSV Format →](./csv.md)** - Structured data
- **[Markdown Format →](./markdown.md)** - Formatted documents
- **[Back to Getting Started →](../getting-started.md)**

---

*Last updated: November 11, 2025*
*Yole version: 2.15.1+*
