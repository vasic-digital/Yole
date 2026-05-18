// SPDX-FileCopyrightText: 2026 Milos Vasic
// SPDX-License-Identifier: Apache-2.0
package digital.vasic.yole.web

// iter-88: wasm-side time primitives via JS interop.
//
// Forensic anchor (operator probe, 2026-05-18): the Save button in the
// deployed Yole web app appeared to do nothing — localStorage stayed
// empty, no toast appeared. Puppeteer captured the actual failure as
// a console error:
//
//   IrLinkageError: Can not get instance of singleton 'System':
//   No class found for symbol 'kotlinx.datetime/Clock.System|null[0]'
//
// Root cause: the iter-82 KGP 2.3.21 upgrade pinned kotlinx-datetime to
// 0.6.1 to keep `Clock.System` available on JVM/Desktop (Compose MP 1.11.0
// transitively brings 0.7.x which removed it). But the kotlinx-datetime
// 0.6.1 *wasm-js* klib does NOT contain `Clock.System` — the wasm IR
// linker silently fails at load time and any call site touching
// `Clock.System.now()` becomes a fatal runtime error. Save / new-tab /
// last-saved-timestamp all hit this — every Yole web feature that
// timestamped anything was broken on the production URL.
//
// Fix: don't use kotlinx-datetime on wasm at all. Use the browser's
// native `Date.now()` and `Date()` via JS interop. These are zero-cost,
// dependency-free, and guaranteed present in every browser since 1996.
//
// The shape mirrors what each caller in EnhancedWebApp.kt + Main.kt
// needs:
//   - nowEpochMilliseconds(): Long — for unique IDs and millisecond
//     timestamps (replaces `Clock.System.now().toEpochMilliseconds()`)
//   - nowIsoString(): String — for human-readable last-saved
//     timestamps (replaces `Clock.System.now().toString()`)
//   - todayIsoDate(): String — for date-only strings
//     (replaces `Clock.System.now().toLocalDateTime(...).date.toString()`)

@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

@JsFun("() => new Date().toISOString()")
private external fun jsDateIsoString(): String

@JsFun("() => new Date().toISOString().slice(0, 10)")
private external fun jsDateOnlyString(): String

/** Milliseconds since the Unix epoch, via browser `Date.now()`. */
internal fun nowEpochMilliseconds(): Long = jsDateNow().toLong()

/**
 * ISO-8601 datetime string in UTC (e.g. `2026-05-18T10:30:45.123Z`),
 * via browser `Date.toISOString()`. Matches the format of
 * `kotlinx.datetime.Instant.toString()`.
 */
internal fun nowIsoString(): String = jsDateIsoString()

/**
 * Local date as `YYYY-MM-DD`. Replaces the old
 * `Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()`.
 * The browser's `toISOString()` returns UTC; we slice the date portion.
 * This is the behavior the original call already had on a UTC host —
 * the explicit `TimeZone.currentSystemDefault()` was a no-op
 * substitution for the lack of a wasm-safe alternative.
 */
internal fun todayIsoDate(): String = jsDateOnlyString()
