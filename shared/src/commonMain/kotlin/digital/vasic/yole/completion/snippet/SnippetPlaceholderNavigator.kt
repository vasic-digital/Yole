/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 8a: Snippet placeholder navigation (commonMain).
 *
 * VS Code snippet placeholder syntax supported (v1):
 *   ${N:default}  — tab stop N with default text.
 *   ${N}          — tab stop N, empty default.
 *   $N            — shorthand for ${N} (digits 1-9 only; $0 also valid).
 *   $0            — final tab stop.
 *
 * Explicitly NOT supported in v1 (silently passed through / ignored):
 *   ${N|a,b,c|}   — choice lists.
 *   $VAR_NAME     — variable references (non-numeric $IDENTIFIER).
 *   \$            — escaped dollar → literal "$" in strippedBody.
 *
 * Cross-platform impact:
 *   - Android: full implementation + Robolectric tests.
 *   - Desktop: navigator API available from commonMain (editor wiring deferred).
 *   - iOS:     same as Desktop.
 *   - Web:     same as Desktop.
 *
 * Anti-bluff covenant (CONST-035):
 *   Mutation procedure applied before commit:
 *   - Stubbed VsCodeSnippetExpander.expand to return
 *     ExpandedSnippet(body, emptyList(), false).
 *   - Re-ran SnippetPlaceholderNavigatorTest: tests 2-7 FAILED
 *     (wrong strippedBody or empty placeholder list).
 *   - Stubbed SnippetPlaceholderNavigator.advance() to always return null.
 *   - Re-ran: tests 7-8 FAILED.
 *   - Reverted all mutations. All 9 tests GREEN.
 *#######################################################*/
package digital.vasic.yole.completion.snippet

/**
 * A single placeholder (tab stop) extracted from a snippet body.
 *
 * @property index  tab-stop number from the snippet source (0 = final stop).
 * @property rangeInBody character range in the *stripped body* string.
 *   For empty/point stops (e.g. `$0` with no default) the convention is
 *   `start..(start-1)` — i.e. an empty range whose `.first` is the
 *   insertion-point offset.
 * @property default the default text between `${N:` and `}` (may be empty).
 */
data class Placeholder(
    val index: Int,
    val rangeInBody: IntRange,
    val default: String,
)

/**
 * The result of expanding a raw VS Code snippet body.
 *
 * @property strippedBody the literal text to insert into the document
 *   (all `${N:default}` markers replaced by their [default] text).
 * @property placeholders all tab stops found in the body, sorted ascending
 *   by [Placeholder.index]; the `$0` final stop (index=0) is always last
 *   when present.
 * @property hasFinalStop true when the original body contained a `$0` marker.
 */
data class ExpandedSnippet(
    val strippedBody: String,
    val placeholders: List<Placeholder>,
    val hasFinalStop: Boolean,
)

/**
 * Stateless parser for VS Code snippet bodies.
 *
 * Parses the body string and produces an [ExpandedSnippet] containing the
 * literal insertion text and the placeholder metadata.
 *
 * Thread-safe: no shared mutable state.
 */
object VsCodeSnippetExpander {

    /**
     * Parse [body] and return the expanded snippet data.
     *
     * The parser scans left-to-right for:
     *   - `\$`   → emit a literal `$` character; no placeholder.
     *   - `${N:default}` / `${N}` → emit default text; record placeholder.
     *   - `$N`   → emit "" (empty default); record placeholder (single digit).
     *   - anything else → emit verbatim.
     *
     * Final tab stop (`$0`) is sorted to the end of the placeholder list.
     */
    fun expand(body: String): ExpandedSnippet {
        val stripped = StringBuilder()
        val rawPlaceholders = mutableListOf<Placeholder>()
        var i = 0

        while (i < body.length) {
            when {
                // Escaped dollar: \$ → literal "$"
                body[i] == '\\' && i + 1 < body.length && body[i + 1] == '$' -> {
                    stripped.append('$')
                    i += 2
                }
                // Long-form: ${N:default} or ${N}
                body[i] == '$' && i + 1 < body.length && body[i + 1] == '{' -> {
                    val closeIdx = body.indexOf('}', i + 2)
                    if (closeIdx < 0) {
                        // Malformed — emit verbatim
                        stripped.append(body[i])
                        i++
                    } else {
                        val inner = body.substring(i + 2, closeIdx) // e.g. "1:default" or "1"
                        val colonIdx = inner.indexOf(':')
                        val (indexStr, default) = if (colonIdx >= 0) {
                            inner.substring(0, colonIdx) to inner.substring(colonIdx + 1)
                        } else {
                            inner to ""
                        }
                        val tabIndex = indexStr.toIntOrNull()
                        if (tabIndex != null) {
                            val startInStripped = stripped.length
                            stripped.append(default)
                            val endInStripped = stripped.length - 1
                            // Empty-range convention: when default is empty, end = start - 1.
                            val range = if (default.isEmpty()) {
                                startInStripped..(startInStripped - 1)
                            } else {
                                startInStripped..endInStripped
                            }
                            rawPlaceholders.add(Placeholder(tabIndex, range, default))
                        } else {
                            // Non-numeric (e.g. variable) — emit verbatim
                            stripped.append(body.substring(i, closeIdx + 1))
                        }
                        i = closeIdx + 1
                    }
                }
                // Short-form: $N where N is a single digit (0-9)
                body[i] == '$' && i + 1 < body.length && body[i + 1].isDigit() -> {
                    val tabIndex = body[i + 1].digitToInt()
                    val insertPoint = stripped.length
                    // empty default → empty range
                    rawPlaceholders.add(Placeholder(tabIndex, insertPoint..(insertPoint - 1), ""))
                    i += 2
                }
                else -> {
                    stripped.append(body[i])
                    i++
                }
            }
        }

        // Sort: non-zero indices ascending first, then $0 (final stop) last.
        val sorted = rawPlaceholders.sortedWith(
            compareBy(
                { if (it.index == 0) Int.MAX_VALUE else it.index },
                { it.rangeInBody.first },
            ),
        )
        val hasFinalStop = rawPlaceholders.any { it.index == 0 }
        return ExpandedSnippet(
            strippedBody = stripped.toString(),
            placeholders = sorted,
            hasFinalStop = hasFinalStop,
        )
    }
}

/**
 * Stateful navigator that tracks which placeholder is currently active
 * and translates body-relative ranges to absolute document offsets.
 *
 * Usage:
 * ```
 * val expansion = VsCodeSnippetExpander.expand(snippetBody)
 * // insert expansion.strippedBody at some `baseOffset` in the document
 * val nav = SnippetPlaceholderNavigator(expansion, baseOffset)
 * val firstSelection = nav.advance()   // select first placeholder
 * // on Tab:
 * val nextSelection = nav.advance()    // select next placeholder
 * // nav.complete() on Esc or final Tab
 * ```
 *
 * All returned [IntRange]s are in absolute document coordinates
 * (body-relative + [baseOffset]).
 *
 * Thread-safety: single-threaded Compose use only; no synchronization.
 *
 * Anti-bluff anchor (CONST-035):
 *   Stubbing [advance] to always return null causes
 *   SnippetPlaceholderNavigatorTest tests 7-8 to FAIL.
 */
class SnippetPlaceholderNavigator(
    private val expansion: ExpandedSnippet,
    private val baseOffset: Int,
) {
    private var currentIdx: Int = -1   // index into expansion.placeholders (-1 = not started)
    private var done: Boolean = expansion.placeholders.isEmpty()

    /**
     * True if there are still placeholders to navigate through.
     *
     * After [advance] returns the last placeholder (or null), this returns false.
     * Before any [advance] call, this is true when placeholders exist.
     */
    fun isActive(): Boolean {
        if (done) return false
        // If we're already at or past the last placeholder, nothing left to visit.
        return currentIdx < expansion.placeholders.size - 1
    }

    /**
     * Advance to the next placeholder.
     *
     * On first call: moves to placeholder at index 0 (first in sorted list).
     * Subsequent calls: step through the list.
     * Once all placeholders are exhausted: sets done=true and returns null.
     *
     * @return absolute document range to select, or null when navigation is complete.
     */
    fun advance(): IntRange? {
        if (done) return null
        currentIdx++
        if (currentIdx >= expansion.placeholders.size) {
            done = true
            return null
        }
        val ph = expansion.placeholders[currentIdx]
        // Translate body-relative range to absolute document offset.
        return (ph.rangeInBody.first + baseOffset)..(ph.rangeInBody.last + baseOffset)
    }

    /**
     * The absolute document range of the currently-active placeholder,
     * or null if navigation has not started or is complete.
     */
    fun current(): IntRange? {
        if (done || currentIdx < 0 || currentIdx >= expansion.placeholders.size) return null
        val ph = expansion.placeholders[currentIdx]
        return (ph.rangeInBody.first + baseOffset)..(ph.rangeInBody.last + baseOffset)
    }

    /**
     * Commit/Esc — immediately deactivate the navigator.
     *
     * After this call [isActive] returns false and [current] returns null.
     */
    fun complete() {
        done = true
        currentIdx = expansion.placeholders.size
    }
}
