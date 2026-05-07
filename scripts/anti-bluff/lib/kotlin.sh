#!/usr/bin/env bash
# Kotlin-flavored bluff patterns. Sourced by bluff-scanner.sh.
# Each pattern emits "<relative path>:<line>:BLUFF-K-NNN:<context>"
#
# Skip-marker convention (any of three forms suppresses a hit):
#   //  SKIP-OK: #<ticket>           -- preferred
#   //  ANTI-BLUFF-EXEMPT: <reason>  -- synonym
#   //  bluff-scan: no-assert-ok     -- synonym
#
# Precision: pattern matching is done on lines with string literals,
# triple-quoted raw strings, line comments (// ...), and block comments
# (/* ... */) stripped, so test bodies that *describe* bluff patterns in
# documentation/log text don't get falsely flagged. See
# scripts/anti-bluff/tests/fixtures/CleanWithStringLiterals.kt for the
# regression case this guards against.

# ---- Helper awk function library ----------------------------------------
# Embedded into each scanner awk via shared snippet. Multi-line strings are
# tracked across awk records via the global in_triple state variable.
_KT_STRIP_PRELUDE='
function strip_kt(line,  out, c, i, in_str, in_block, esc, two) {
  out = ""; in_str = 0; esc = 0
  for (i = 1; i <= length(line); i++) {
    c = substr(line, i, 1)
    two = substr(line, i, 2)
    # Triple-quoted string: spans lines via in_triple global.
    if (in_triple) {
      if (substr(line, i, 3) == "\"\"\"") { in_triple = 0; out = out "\"\"\""; i += 2; continue }
      continue
    }
    # Block comment: spans lines via in_block_g global.
    if (in_block_g) {
      if (two == "*/") { in_block_g = 0; i += 1; continue }
      continue
    }
    # Mid-line block comment start
    if (two == "/*") { in_block_g = 1; i += 1; continue }
    # Line comment
    if (two == "//") { break }
    # Triple-quoted string start
    if (substr(line, i, 3) == "\"\"\"") { in_triple = 1; out = out "\"\"\""; i += 2; continue }
    # Single-line string
    if (in_str) {
      if (esc) { esc = 0; continue }
      if (c == "\\") { esc = 1; continue }
      if (c == "\"") { in_str = 0; out = out c; continue }
      continue
    }
    if (c == "\"") { in_str = 1; out = out c; continue }
    out = out c
  }
  return out
}
'

scan_kotlin() {
  local relpath="$1" fpath="$2"

  # BLUFF-K-002: trivial assertions (assertTrue(true), assertFalse(false), assertEquals(x, x))
  awk -v rel="${relpath}" "${_KT_STRIP_PRELUDE}"'
    { stripped = strip_kt($0) }
    stripped ~ /assertTrue\(true\)/ {
      print rel ":" NR ":BLUFF-K-002:assertTrue(true)"
    }
    stripped ~ /assertFalse\(false\)/ {
      print rel ":" NR ":BLUFF-K-002:assertFalse(false)"
    }
    stripped ~ /assertEquals\([[:space:]]*([a-zA-Z_][a-zA-Z0-9_]*)[[:space:]]*,[[:space:]]*\1[[:space:]]*[,)]/ {
      print rel ":" NR ":BLUFF-K-002:assertEquals(x, x) tautological"
    }
  ' "$fpath"

  # BLUFF-K-003: @Ignore without exempt comment on prev line
  awk -v rel="${relpath}" "${_KT_STRIP_PRELUDE}"'
    { stripped = strip_kt($0) }
    /SKIP-OK|ANTI-BLUFF-EXEMPT|bluff-scan:/ { exempt[NR+1] = 1 }
    stripped ~ /^[[:space:]]*@Ignore([(].*[)])?[[:space:]]*$/ {
      if (!(NR in exempt)) print rel ":" NR ":BLUFF-K-003:@Ignore without exempt comment"
    }
  ' "$fpath"

  # BLUFF-K-004: assumeTrue(false) / assumeFalse(true)
  awk -v rel="${relpath}" "${_KT_STRIP_PRELUDE}"'
    { stripped = strip_kt($0) }
    stripped ~ /[Aa]ssumeTrue\(false\)/ {
      print rel ":" NR ":BLUFF-K-004:assumeTrue(false)"
    }
    stripped ~ /[Aa]ssumeFalse\(true\)/ {
      print rel ":" NR ":BLUFF-K-004:assumeFalse(true)"
    }
  ' "$fpath"

  # BLUFF-K-006: @Test directly followed by an empty body on a single line
  awk -v rel="${relpath}" "${_KT_STRIP_PRELUDE}"'
    { stripped = strip_kt($0) }
    stripped ~ /^[[:space:]]*@Test[[:space:]]*$/ { test_line = NR; next }
    test_line > 0 && stripped ~ /^[[:space:]]*fun [A-Za-z_][A-Za-z0-9_`]*\([^)]*\)([[:space:]]*:[[:space:]]*[A-Za-z_][A-Za-z0-9_<>?, ]*)?[[:space:]]*\{[[:space:]]*\}[[:space:]]*$/ {
      print rel ":" test_line ":BLUFF-K-006:empty @Test method body"
      test_line = 0; next
    }
    test_line > 0 && stripped ~ /^[[:space:]]*fun / { test_line = 0; next }
    test_line > 0 && stripped ~ /^[[:space:]]*$/ { next }
    { test_line = 0 }
  ' "$fpath"

  # BLUFF-K-008: @Suppress("BLUFF...") without justification on prev line.
  # NOTE: BLUFF-K-008 inspects the *original* line (not stripped) because the
  # detection target is a BLUFF-* identifier intentionally placed inside the
  # @Suppress() string argument. Stripping would erase the very thing we
  # detect.
  awk -v rel="${relpath}" '
    /SKIP-OK|ANTI-BLUFF-EXEMPT|bluff-scan:/ { exempt[NR+1] = 1 }
    /@Suppress\([^)]*"BLUFF[^"]*"[^)]*\)/ {
      if (!(NR in exempt)) print rel ":" NR ":BLUFF-K-008:@Suppress(\"BLUFF...\") without justification"
    }
  ' "$fpath"

  # BLUFF-K-009: catch block that emits COMPLETED on error — the "silent
  # success" pattern where exceptions are swallowed and the operation is
  # falsely reported as successful. Context-sensitive: checks for catch
  # keyword followed by emit(COMPLETED) within a few lines.
  awk -v rel="${relpath}" "${_KT_STRIP_PRELUDE}"'
    { stripped = strip_kt($0) }
    stripped ~ /^[[:space:]]*\} catch/ { in_catch = 1; catch_start = NR; next }
    in_catch == 1 && stripped ~ /\.COMPLETED/ {
      print rel ":" catch_start ":BLUFF-K-009:catch block may emit COMPLETED (silent success bluff)"
      in_catch = 0; next
    }
    in_catch == 1 && stripped ~ /^[[:space:]]*\}[[:space:]]*finally/ { in_catch = 0; next }
    in_catch == 1 && stripped ~ /^[[:space:]]*\}[[:space:]]*$/ { in_catch = 0; next }
    in_catch > 0 && NR > catch_start + 10 { in_catch = 0 }
  ' "$fpath"

  # BLUFF-K-010: simulated/fallback operations emitting COMPLETED — comments
  # like "Fallback", "simulated", "offline/test scenarios" next to emission
  # of COMPLETED status.
  awk -v rel="${relpath}" "${_KT_STRIP_PRELUDE}"'
    { stripped = strip_kt($0) }
    /Fallback|simulated|offline.test.scenario/ { fallback_hint = 1; fallback_line = NR; next }
    fallback_hint == 1 && stripped ~ /\.COMPLETED/ {
      print rel ":" fallback_line ":BLUFF-K-010:fallback/simulated code may emit COMPLETED (bluff)"
      fallback_hint = 0; next
    }
    fallback_hint > 0 && NR > fallback_line + 5 { fallback_hint = 0 }
  ' "$fpath"

  # BLUFF-K-011: silent data loss — readFileBytes().getOrElse { byteArrayOf() }
  # uploads empty bytes when local file read fails. Must fail explicitly
  # instead of silently transmitting zero bytes.
  awk -v rel="${relpath}" "${_KT_STRIP_PRELUDE}"'
    { stripped = strip_kt($0) }
    stripped ~ /readFileBytes.*getOrElse.*byteArrayOf/ {
      print rel ":" NR ":BLUFF-K-011:getOrElse { byteArrayOf() } — silent empty upload on read failure"
    }
  ' "$fpath"
}
