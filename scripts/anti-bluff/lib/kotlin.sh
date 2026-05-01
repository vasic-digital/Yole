#!/usr/bin/env bash
# Kotlin-flavored bluff patterns. Sourced by bluff-scanner.sh.
# Each pattern emits "<relative path>:<line>:BLUFF-K-NNN:<context>"
#
# Note: scope is conservative — these patterns catch the easy half of
# the BLUFF-K taxonomy. BLUFF-K-001 (mock-self), BLUFF-K-005 (runBlocking
# no-op), and BLUFF-K-007 (SUT-via-mock-only) require AST awareness and
# are deferred to a follow-up.

scan_kotlin() {
  local relpath="$1" fpath="$2"

  # BLUFF-K-002: trivial assertions on a single line.
  awk -v rel="${relpath}" '
    /assertTrue\(true\)/ {
      print rel ":" NR ":BLUFF-K-002:assertTrue(true)"
    }
    /assertFalse\(false\)/ {
      print rel ":" NR ":BLUFF-K-002:assertFalse(false)"
    }
  ' "$fpath"

  # BLUFF-K-002: assertEquals(x, x) — same identifier twice
  awk -v rel="${relpath}" '
    /assertEquals\([[:space:]]*([a-zA-Z_][a-zA-Z0-9_]*)[[:space:]]*,[[:space:]]*\1[[:space:]]*[,)]/ {
      print rel ":" NR ":BLUFF-K-002:assertEquals(x, x) tautological"
    }
  ' "$fpath"

  # BLUFF-K-003: @Ignore without SKIP-OK or ANTI-BLUFF-EXEMPT on prev line.
  awk -v rel="${relpath}" '
    /SKIP-OK|ANTI-BLUFF-EXEMPT/ { exempt[NR+1] = 1 }
    /^[[:space:]]*@Ignore([(].*[)])?[[:space:]]*$/ {
      if (!(NR in exempt)) print rel ":" NR ":BLUFF-K-003:@Ignore without exempt comment"
    }
  ' "$fpath"

  # BLUFF-K-004: assumeTrue(false) — unconditional skip.
  awk -v rel="${relpath}" '
    /[Aa]ssumeTrue\(false\)/ {
      print rel ":" NR ":BLUFF-K-004:assumeTrue(false)"
    }
    /[Aa]ssumeFalse\(true\)/ {
      print rel ":" NR ":BLUFF-K-004:assumeFalse(true)"
    }
  ' "$fpath"

  # BLUFF-K-006: @Test directly followed by an empty body on a single line.
  awk -v rel="${relpath}" '
    /^[[:space:]]*@Test[[:space:]]*$/ { test_line = NR; next }
    test_line > 0 && /^[[:space:]]*fun [A-Za-z_][A-Za-z0-9_`]*\([^)]*\)([[:space:]]*:[[:space:]]*[A-Za-z_][A-Za-z0-9_<>?, ]*)?[[:space:]]*\{[[:space:]]*\}[[:space:]]*$/ {
      print rel ":" test_line ":BLUFF-K-006:empty @Test method body"
      test_line = 0; next
    }
    test_line > 0 && /^[[:space:]]*fun / { test_line = 0; next }
    test_line > 0 && /^[[:space:]]*$/ { next }
    { test_line = 0 }
  ' "$fpath"

  # BLUFF-K-008: @Suppress("BLUFF...") without explicit justification.
  awk -v rel="${relpath}" '
    /SKIP-OK|ANTI-BLUFF-EXEMPT/ { exempt[NR+1] = 1 }
    /@Suppress\([^)]*"BLUFF[^"]*"[^)]*\)/ {
      if (!(NR in exempt)) print rel ":" NR ":BLUFF-K-008:@Suppress(\"BLUFF...\") without justification"
    }
  ' "$fpath"
}
