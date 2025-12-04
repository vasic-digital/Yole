# Missing Screenshots for Website

This directory needs the following screenshot files to complete the website.

## Required Screenshots

### 1. Line Numbers Feature
**File:** `2023-10-11-line-numbers.webp`
**Description:** Screenshot showing the line numbers feature in the editor
**How to capture:**
- Open Yole on Android/Desktop
- Open a file with line numbers enabled
- Capture screenshot of editor with line numbers visible
- Convert to WebP format: `cwebp -q 85 input.png -o 2023-10-11-line-numbers.webp`

### 2. AsciiDoc Rendering
**File:** `2023-10-11-asciidoc.webp`
**Description:** Screenshot showing AsciiDoc format rendering/preview
**How to capture:**
- Open an .adoc file in Yole
- Show preview pane with rendered output
- Capture screenshot
- Convert to WebP format

### 3. CSV Landscape View
**File:** `csv/2023-06-25-csv-landscape.webp`
**Description:** CSV file displayed in landscape orientation
**How to capture:**
- Open a CSV file with multiple columns
- Rotate device to landscape (Android) or expand window (Desktop)
- Capture screenshot showing table layout
- Convert to WebP format
- Place in `csv/` subdirectory

### 4. Org Mode Rendering
**File:** `2023-10-07-orgmode.webp`
**Description:** Org-Mode format rendering/preview
**How to capture:**
- Open a .org file in Yole
- Show preview pane with formatted output
- Capture screenshot
- Convert to WebP format

### 5. SD Card Mount Info
**File:** `2019-05-06-sdcard-mount.webp`
**Description:** SD card/external storage mount information
**How to capture:**
- Navigate to storage settings or file browser
- Show external storage mount point
- Capture screenshot
- Convert to WebP format

### 6. Todo.txt Format Diagram (Light Mode)
**File:** `todotxt-format.png`
**Description:** Diagram showing todo.txt format syntax (light theme)
**How to capture:**
- Create or find diagram illustrating todo.txt format:
  - Priority: (A), (B), (C)
  - Dates: 2024-01-01
  - Contexts: @context
  - Projects: +project
- Use light background
- Save as PNG

### 7. Todo.txt Format Diagram (Dark Mode)
**File:** `todotxt-format-dark.png`
**Description:** Same diagram as above but with dark theme
**How to capture:**
- Same as above but with dark background
- Ensure text is light colored for visibility
- Save as PNG

## Image Specifications

- **Format:** WebP for photos/screenshots, PNG for diagrams
- **Quality:** 85% for WebP compression
- **Max Width:** 1200px (responsive design will scale down)
- **Optimization:** Use `cwebp` or `pngquant` to optimize file sizes

## Conversion Commands

```bash
# PNG to WebP
cwebp -q 85 input.png -o output.webp

# Batch conversion
for img in *.png; do
  cwebp -q 85 "$img" -o "${img%.png}.webp"
done

# Optimize PNG
pngquant --quality=85 input.png -o output.png
```

## Temporary Placeholders

Until real screenshots are captured, placeholder images have been created to prevent broken links on the website.

## Status

- [ ] 2023-10-11-line-numbers.webp
- [ ] 2023-10-11-asciidoc.webp
- [ ] csv/2023-06-25-csv-landscape.webp
- [ ] 2023-10-07-orgmode.webp
- [ ] 2019-05-06-sdcard-mount.webp
- [ ] todotxt-format.png
- [ ] todotxt-format-dark.png

**Total:** 0/7 screenshots captured
