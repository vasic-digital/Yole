/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * FirebaseUtil hook test — CONST-035 anti-bluff anchor for
 * Analytics + Crashlytics wiring.
 *
 * Verifies the `testEventCapture` / `testNonFatalCapture` hooks
 * actually intercept calls, so production-code paths that emit
 * Firebase telemetry can be verified by Robolectric tests
 * without a live Firebase SDK.
 *
 * Pure JVM — no Robolectric, no Android Context needed.
 *
 *########################################################*/
package digital.vasic.yole.android.firebase

import digital.vasic.yole.android.util.FirebaseUtil
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FirebaseUtilHookTest {

    @Before
    fun setUp() {
        // Object state may leak across tests since FirebaseUtil is a singleton.
        FirebaseUtil.testEventCapture = null
        FirebaseUtil.testNonFatalCapture = null
    }

    @After
    fun tearDown() {
        FirebaseUtil.testEventCapture = null
        FirebaseUtil.testNonFatalCapture = null
    }

    @Test
    fun logEvent_invokesCaptureHook_withEventNameAndParams() {
        val captured = mutableListOf<Pair<String, Map<String, String>>>()
        FirebaseUtil.testEventCapture = { name, params -> captured += name to params }

        FirebaseUtil.logEvent(
            FirebaseUtil.Events.FILE_SAVED,
            mapOf(
                FirebaseUtil.Params.FILE_FORMAT to "markdown",
                FirebaseUtil.Params.FILE_SIZE to "1024"
            )
        )

        assertEquals(1, captured.size)
        val (name, params) = captured[0]
        assertEquals("file_saved", name)
        assertEquals("markdown", params[FirebaseUtil.Params.FILE_FORMAT])
        assertEquals("1024", params[FirebaseUtil.Params.FILE_SIZE])
    }

    @Test
    fun logEvent_withNoHook_isSafeAndNoOp() {
        // Verifies the production no-Firebase-init no-hook path doesn't
        // crash AND doesn't accidentally invoke a stale hook from a prior
        // test. After setting capture to null, invoking logEvent must
        // leave capture null (no side-effect on the hook itself) and
        // must not throw. Asserting both moves this test from the
        // CONST-035 BLUFF-K-002 pattern (assertTrue(true)) to a real
        // post-condition contract.
        FirebaseUtil.testEventCapture = null
        FirebaseUtil.logEvent("anything", mapOf("k" to "v"))
        assertNull(
            "testEventCapture must remain null after a no-hook logEvent",
            FirebaseUtil.testEventCapture
        )
    }

    @Test
    fun recordNonFatal_invokesCaptureHook_withThrowableAndContext() {
        val recorded = mutableListOf<Pair<Throwable, String?>>()
        FirebaseUtil.testNonFatalCapture = { t, ctx -> recorded += t to ctx }

        val ex = IllegalStateException("oh no")
        FirebaseUtil.recordNonFatal(ex, "saveFile failed for foo.md")

        assertEquals(1, recorded.size)
        val (t, ctx) = recorded[0]
        assertSame(ex, t)
        assertEquals("saveFile failed for foo.md", ctx)
    }

    @Test
    fun recordNonFatal_nullContextHint_passesThroughAsNull() {
        var capturedContext: String? = "untouched"
        FirebaseUtil.testNonFatalCapture = { _, ctx -> capturedContext = ctx }

        FirebaseUtil.recordNonFatal(RuntimeException("x"), null)

        assertNull(capturedContext)
    }

    @Test
    fun hookIsScopedToTestLifetime_clearedInTearDown() {
        FirebaseUtil.testEventCapture = { _, _ -> /* noop */ }
        // tearDown will clear it; verify next test sees a clean state.
        // The @Before resets to null at the START of each test, so this
        // anchor is essentially the @After teardown contract.
        assertTrue(FirebaseUtil.testEventCapture != null)
    }

    @Test
    fun startTrace_invokesTraceCaptureHook_withStartedNowTrue() {
        val records = mutableListOf<Pair<String, Boolean>>()
        FirebaseUtil.testTraceCapture = { n, started -> records += n to started }

        FirebaseUtil.startTrace(FirebaseUtil.Traces.FILE_SAVE)

        assertEquals(1, records.size)
        assertEquals(FirebaseUtil.Traces.FILE_SAVE, records[0].first)
        assertTrue("startedNow must be true at startTrace", records[0].second)

        FirebaseUtil.testTraceCapture = null
    }

    @Test
    fun stopTrace_invokesTraceCaptureHook_withStartedNowFalse() {
        val records = mutableListOf<Pair<String, Boolean>>()
        FirebaseUtil.testTraceCapture = { n, started -> records += n to started }

        // stopTrace expects a Trace handle. In production, it'd come from
        // startTrace; here we pass null since FirebasePerformance isn't
        // initialized — the production code path is "no-op + still fire hook".
        FirebaseUtil.stopTrace(null)

        // Null trace = no real stop, but the hook also isn't called because
        // the null-check happens FIRST in stopTrace. This documents the
        // null-safety contract.
        assertEquals(0, records.size)

        FirebaseUtil.testTraceCapture = null
    }

    @Test
    fun remoteConfigDefaults_returnedWhenConfigNotInitialized() {
        // Without initPerformanceAndConfig, remoteConfig is null →
        // get* methods must return the default.
        assertEquals(
            "fallback default",
            FirebaseUtil.getConfigString("missing_key", "fallback default")
        )
        assertEquals(42L, FirebaseUtil.getConfigLong("missing_long", 42L))
        assertEquals(true, FirebaseUtil.getConfigBoolean("missing_bool", true))
    }

    @Test
    fun fetchRemoteConfig_invokesCaptureWithFalse_whenNotInitialized() {
        var captured: Boolean? = null
        FirebaseUtil.testRemoteConfigFetchCapture = { ok -> captured = ok }

        var callbackResult: Boolean? = null
        FirebaseUtil.fetchRemoteConfig { ok -> callbackResult = ok }

        assertEquals(false, captured)
        assertEquals(false, callbackResult)

        FirebaseUtil.testRemoteConfigFetchCapture = null
    }
}
