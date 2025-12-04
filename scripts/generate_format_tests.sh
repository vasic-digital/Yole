#!/bin/bash
###########################################################
#
# SPDX-FileCopyrightText: 2025 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# Test Generation Script
# Generates test files from templates for a specific format
#
###########################################################

set -e

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TEMPLATES_DIR="$PROJECT_ROOT/templates/tests"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Usage information
usage() {
    cat <<EOF
${BLUE}Test Generation Script for Yole${NC}

${GREEN}Usage:${NC}
    $0 <format-name> <primary-extension> [options]

${GREEN}Arguments:${NC}
    format-name          Format name (e.g., markdown, todotxt, latex)
    primary-extension    Primary file extension (e.g., .md, .txt, .tex)

${GREEN}Options:${NC}
    --package NAME       Package name (default: format-name in lowercase)
    --class NAME         Class name (default: format-name in PascalCase)
    --format-id ID       Format ID constant (default: UPPERCASE_SNAKE_CASE)
    --extensions LIST    Comma-separated list of extensions (default: primary only)
    --output-dir DIR     Output directory (default: shared/src/commonTest/kotlin/.../format/PACKAGE)
    --templates LIST     Comma-separated list of templates to generate (default: all)
    --dry-run            Show what would be generated without creating files
    --help               Show this help message

${GREEN}Examples:${NC}
    # Generate all tests for Markdown format
    $0 markdown .md

    # Generate specific tests with custom package
    $0 "Org Mode" .org --package orgmode --class OrgMode --templates ParserTest,IntegrationTest

    # Dry run to preview generation
    $0 latex .tex --dry-run

${GREEN}Available Templates:${NC}
    - ParserTest.kt.template         (Unit tests for parser)
    - IntegrationTest.kt.template    (Integration tests)
    - MockKExample.kt.template       (MockK mocking examples)
    - KotestPropertyTest.kt.template (Property-based tests)

${GREEN}Template Variables:${NC}
    {{FORMAT_NAME}}              - Human-readable format name
    {{FORMAT_CLASS}}             - Class name prefix (PascalCase)
    {{FORMAT_PACKAGE}}           - Package name (lowercase)
    {{FORMAT_LOWERCASE}}         - Format name in lowercase
    {{FORMAT_ID}}                - Format ID constant (UPPERCASE_SNAKE_CASE)
    {{PRIMARY_EXTENSION}}        - Primary file extension
    {{EXTENSIONS_LIST}}          - Quoted, comma-separated extension list
    {{SAMPLE_CONTENT}}           - Multi-line sample content
    {{SINGLE_LINE_SAMPLE}}       - Single-line sample
    {{MULTI_LINE_SAMPLE}}        - Multi-line sample
    {{SPECIAL_CHARS_SAMPLE}}     - Special characters sample
    {{MALFORMED_SAMPLE}}         - Malformed content sample
    {{FORMAT_SPECIFIC_FEATURE}}  - Format-specific feature name
    {{FORMAT_SPECIFIC_SAMPLE}}   - Format-specific sample
    {{FORMAT_SPECIFIC_PROPERTY_1}} - Property test description 1
    {{FORMAT_SPECIFIC_PROPERTY_2}} - Property test description 2
    {{FORMAT_SPECIFIC_GENERATOR}}  - Generator function name

EOF
    exit 0
}

# Parse arguments
FORMAT_NAME=""
PRIMARY_EXTENSION=""
PACKAGE_NAME=""
CLASS_NAME=""
FORMAT_ID=""
EXTENSIONS_LIST=""
OUTPUT_DIR=""
TEMPLATES="ParserTest,IntegrationTest,MockKExample,KotestPropertyTest"
DRY_RUN=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --help|-h)
            usage
            ;;
        --package)
            PACKAGE_NAME="$2"
            shift 2
            ;;
        --class)
            CLASS_NAME="$2"
            shift 2
            ;;
        --format-id)
            FORMAT_ID="$2"
            shift 2
            ;;
        --extensions)
            EXTENSIONS_LIST="$2"
            shift 2
            ;;
        --output-dir)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        --templates)
            TEMPLATES="$2"
            shift 2
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        -*)
            echo -e "${RED}Unknown option: $1${NC}"
            usage
            ;;
        *)
            if [[ -z "$FORMAT_NAME" ]]; then
                FORMAT_NAME="$1"
            elif [[ -z "$PRIMARY_EXTENSION" ]]; then
                PRIMARY_EXTENSION="$1"
            else
                echo -e "${RED}Too many arguments: $1${NC}"
                usage
            fi
            shift
            ;;
    esac
done

# Validate required arguments
if [[ -z "$FORMAT_NAME" ]] || [[ -z "$PRIMARY_EXTENSION" ]]; then
    echo -e "${RED}Error: format-name and primary-extension are required${NC}\n"
    usage
fi

# Ensure extension starts with dot
if [[ ! "$PRIMARY_EXTENSION" =~ ^\. ]]; then
    PRIMARY_EXTENSION=".$PRIMARY_EXTENSION"
fi

# Generate default values
if [[ -z "$PACKAGE_NAME" ]]; then
    PACKAGE_NAME=$(echo "$FORMAT_NAME" | tr '[:upper:]' '[:lower:]' | tr -d ' ' | tr -d '-')
fi

if [[ -z "$CLASS_NAME" ]]; then
    # Convert to PascalCase: capitalize first letter of each word, remove spaces/dashes
    CLASS_NAME=$(echo "$FORMAT_NAME" | awk '{for(i=1;i<=NF;i++){$i=toupper(substr($i,1,1)) tolower(substr($i,2))}}1' | tr -d ' ' | tr -d '-')
fi

if [[ -z "$FORMAT_ID" ]]; then
    FORMAT_ID=$(echo "$FORMAT_NAME" | tr '[:lower:]' '[:upper:]' | tr ' ' '_' | tr '-' '_')
fi

if [[ -z "$EXTENSIONS_LIST" ]]; then
    EXTENSIONS_LIST="\"$PRIMARY_EXTENSION\""
else
    # Convert comma-separated list to quoted list
    EXTENSIONS_LIST=$(echo "$EXTENSIONS_LIST" | sed 's/,/", "/g' | sed 's/^/"/' | sed 's/$/"/')
fi

if [[ -z "$OUTPUT_DIR" ]]; then
    OUTPUT_DIR="$PROJECT_ROOT/shared/src/commonTest/kotlin/digital/vasic/yole/format/$PACKAGE_NAME"
fi

FORMAT_LOWERCASE=$(echo "$FORMAT_NAME" | tr '[:upper:]' '[:lower:]')

# Generate sample content based on format
SAMPLE_CONTENT="Sample $FORMAT_NAME content here"
SINGLE_LINE_SAMPLE="Single line of $FORMAT_NAME"
MULTI_LINE_SAMPLE="Line 1\nLine 2\nLine 3"
SPECIAL_CHARS_SAMPLE="Special chars: @#$%^&*()"
MALFORMED_SAMPLE="Malformed $FORMAT_NAME content"
FORMAT_SPECIFIC_FEATURE="format-specific feature"
FORMAT_SPECIFIC_SAMPLE="Format specific sample"
FORMAT_SPECIFIC_PROPERTY_1="should maintain format-specific property 1"
FORMAT_SPECIFIC_PROPERTY_2="should maintain format-specific property 2"
FORMAT_SPECIFIC_GENERATOR="formatSpecific${CLASS_NAME}Content"

# Print configuration
echo -e "${BLUE}Test Generation Configuration${NC}"
echo -e "${BLUE}=============================${NC}"
echo -e "Format Name:         ${GREEN}$FORMAT_NAME${NC}"
echo -e "Class Name:          ${GREEN}$CLASS_NAME${NC}"
echo -e "Package Name:        ${GREEN}$PACKAGE_NAME${NC}"
echo -e "Format ID:           ${GREEN}$FORMAT_ID${NC}"
echo -e "Primary Extension:   ${GREEN}$PRIMARY_EXTENSION${NC}"
echo -e "All Extensions:      ${GREEN}$EXTENSIONS_LIST${NC}"
echo -e "Output Directory:    ${GREEN}$OUTPUT_DIR${NC}"
echo -e "Templates:           ${GREEN}$TEMPLATES${NC}"
echo -e "Dry Run:             ${GREEN}$DRY_RUN${NC}"
echo ""

# Create output directory
if [[ "$DRY_RUN" == false ]]; then
    mkdir -p "$OUTPUT_DIR"
    echo -e "${GREEN}✓${NC} Created output directory: $OUTPUT_DIR"
fi

# Process each template
IFS=',' read -ra TEMPLATE_ARRAY <<< "$TEMPLATES"
for template_name in "${TEMPLATE_ARRAY[@]}"; do
    template_file="$TEMPLATES_DIR/${template_name}.kt.template"
    output_file="$OUTPUT_DIR/${CLASS_NAME}${template_name}.kt"

    if [[ ! -f "$template_file" ]]; then
        echo -e "${YELLOW}⚠${NC}  Template not found: $template_file (skipping)"
        continue
    fi

    echo -e "${BLUE}Processing:${NC} $template_name"

    if [[ "$DRY_RUN" == false ]]; then
        # Read template and replace placeholders
        sed -e "s|{{FORMAT_NAME}}|$FORMAT_NAME|g" \
            -e "s|{{FORMAT_CLASS}}|$CLASS_NAME|g" \
            -e "s|{{FORMAT_PACKAGE}}|$PACKAGE_NAME|g" \
            -e "s|{{FORMAT_LOWERCASE}}|$FORMAT_LOWERCASE|g" \
            -e "s|{{FORMAT_ID}}|$FORMAT_ID|g" \
            -e "s|{{PRIMARY_EXTENSION}}|$PRIMARY_EXTENSION|g" \
            -e "s|{{EXTENSIONS_LIST}}|$EXTENSIONS_LIST|g" \
            -e "s|{{SAMPLE_CONTENT}}|$SAMPLE_CONTENT|g" \
            -e "s|{{SINGLE_LINE_SAMPLE}}|$SINGLE_LINE_SAMPLE|g" \
            -e "s|{{MULTI_LINE_SAMPLE}}|$MULTI_LINE_SAMPLE|g" \
            -e "s|{{SPECIAL_CHARS_SAMPLE}}|$SPECIAL_CHARS_SAMPLE|g" \
            -e "s|{{MALFORMED_SAMPLE}}|$MALFORMED_SAMPLE|g" \
            -e "s|{{FORMAT_SPECIFIC_FEATURE}}|$FORMAT_SPECIFIC_FEATURE|g" \
            -e "s|{{FORMAT_SPECIFIC_SAMPLE}}|$FORMAT_SPECIFIC_SAMPLE|g" \
            -e "s|{{FORMAT_SPECIFIC_PROPERTY_1}}|$FORMAT_SPECIFIC_PROPERTY_1|g" \
            -e "s|{{FORMAT_SPECIFIC_PROPERTY_2}}|$FORMAT_SPECIFIC_PROPERTY_2|g" \
            -e "s|{{FORMAT_SPECIFIC_GENERATOR}}|$FORMAT_SPECIFIC_GENERATOR|g" \
            "$template_file" > "$output_file"

        echo -e "  ${GREEN}✓${NC} Generated: $output_file"
    else
        echo -e "  ${YELLOW}[DRY RUN]${NC} Would generate: $output_file"
    fi
done

echo ""
if [[ "$DRY_RUN" == false ]]; then
    echo -e "${GREEN}✓ Test generation complete!${NC}"
    echo ""
    echo -e "${BLUE}Next Steps:${NC}"
    echo "1. Review generated test files in: $OUTPUT_DIR"
    echo "2. Customize format-specific test cases"
    echo "3. Add actual sample content for your format"
    echo "4. Run tests: ./gradlew :shared:testDebugUnitTest"
    echo ""
    echo -e "${YELLOW}Note:${NC} Generated tests use placeholder content."
    echo "      Update with real $FORMAT_NAME samples for meaningful tests."
else
    echo -e "${YELLOW}Dry run complete. No files were created.${NC}"
    echo "Run without --dry-run to generate actual test files."
fi
