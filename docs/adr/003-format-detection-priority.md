# ADR 003: Format Detection Priority Order

<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

## Status

Accepted (2025-12-01)

## Context

Yole supports 17 text formats. Some formats share file extensions (e.g., `.txt` could be plaintext or todo.txt) and content patterns can overlap (e.g., markdown-like syntax appears in several formats). FormatRegistry needs a deterministic detection order.

## Decision

FormatRegistry.formats is an ordered list where more specific formats appear before general ones:

1. **Binary** (first — detected by content, not extension)
2. **Markdown** (most common format)
3. **Todo.txt** (specific `.txt` variant with detectable patterns)
4. **CSV** (specific content patterns)
5. **LaTeX, OrgMode, WikiText, AsciiDoc** (each has unique syntax markers)
6. **reStructuredText, R Markdown, TaskPaper, Textile** (less common)
7. **Creole, TiddlyWiki, Jupyter, Key-Value** (specialized formats)
8. **Plain Text** (last — universal fallback)

Detection uses two strategies:
- `detectByExtension()` — first match wins, so `.md` matches Markdown before any other
- `detectByContent()` — regex patterns checked in list order, first confident match wins

Format IDs are string constants on `TextFormat.Companion` for compile-time safety.

## Consequences

**Positive:**
- Deterministic detection (same content always produces same format)
- Plaintext as fallback ensures no content goes unrecognized
- Binary detection first prevents attempting text parse on binary data

**Negative:**
- Order matters — adding new formats requires careful placement
- Ambiguous content (e.g., `.txt` that looks like todo.txt) resolved by list order, not confidence scoring
