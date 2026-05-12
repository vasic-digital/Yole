/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Firebase wiring verification — Robolectric anchors for the
 * production call sites added in commit 8bb926ac.
 *
 * Goal (CONST-035 anti-bluff): defining FirebaseUtil.Events.*
 * constants is meaningless unless something actually fires them
 * when the user-visible feature happens. These tests call the
 * real production functions (saveFile, openFileInTab path) and
 * assert that the corresponding Firebase events are captured via
 * the `testEventCapture` hook.
 *
 * Runs in the dedicated `robolectric-test` container per iter-27
 * convention (excluded from default :androidApp:testDebugUnitTest).
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import digital.vasic.yole.android.ui.saveFile
import digital.vasic.yole.android.util.FirebaseUtil
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class FirebaseWiringRobolectricTest {

    private lateinit var context: Context
    private val capturedEvents = mutableListOf<Pair<String, Map<String, String>>>()
    private val capturedNonFatals = mutableListOf<Pair<Throwable, String?>>()
    private val capturedTraces = mutableListOf<Pair<String, Boolean>>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        capturedEvents.clear()
        capturedNonFatals.clear()
        capturedTraces.clear()
        FirebaseUtil.testEventCapture = { n, p -> capturedEvents += n to p }
        FirebaseUtil.testNonFatalCapture = { t, c -> capturedNonFatals += t to c }
        FirebaseUtil.testTraceCapture = { n, started -> capturedTraces += n to started }
    }

    @After
    fun tearDown() {
        FirebaseUtil.testEventCapture = null
        FirebaseUtil.testNonFatalCapture = null
        FirebaseUtil.testTraceCapture = null
    }

    @Test
    fun saveFile_emitsFileSavedEvent_onSuccessfulCacheSave() {
        // saveFile(contentUri=null) writes to context.filesDir/autosave/<name>.
        // Robolectric supplies a real (temp) filesDir.
        val result = saveFile(
            context = context,
            contentUri = null,
            content = "# Test\n\nHello world.",
            fileName = "test.md"
        )

        assertTrue("saveFile should succeed on Robolectric temp filesDir", result.first)
        assertNotNull("Captured events should not be null", capturedEvents)
        val fileSavedEvents = capturedEvents.filter { it.first == FirebaseUtil.Events.FILE_SAVED }
        assertEquals(
            "Exactly one FILE_SAVED event should fire when saveFile succeeds",
            1, fileSavedEvents.size
        )
        val params = fileSavedEvents[0].second
        assertEquals("md", params[FirebaseUtil.Params.FILE_FORMAT])
        assertEquals("20", params[FirebaseUtil.Params.FILE_SIZE])  // "# Test\n\nHello world." = 20 chars
    }

    @Test
    fun saveFile_doesNotEmitErrorEvent_whenSuccessful() {
        saveFile(
            context = context,
            contentUri = null,
            content = "ok",
            fileName = "ok.txt"
        )
        val errorEvents = capturedEvents.filter { it.first == FirebaseUtil.Events.ERROR_OCCURRED }
        assertEquals("ERROR_OCCURRED should NOT fire on successful save", 0, errorEvents.size)
        assertEquals("No non-fatals should be recorded for happy-path save", 0, capturedNonFatals.size)
    }

    @Test
    fun saveFile_unknownExtension_passesUnknownFormatParam() {
        saveFile(
            context = context,
            contentUri = null,
            content = "no extension content",
            fileName = "noext"
        )
        val savedEvent = capturedEvents.first { it.first == FirebaseUtil.Events.FILE_SAVED }
        assertEquals("unknown", savedEvent.second[FirebaseUtil.Params.FILE_FORMAT])
    }

    @Test
    fun saveFile_startsAndStopsPerformanceTrace() {
        saveFile(
            context = context,
            contentUri = null,
            content = "trace test",
            fileName = "trace.md"
        )
        // startTrace fires the hook with `startedNow=true`. stopTrace short-
        // circuits with null trace because Performance isn't initialized in
        // Robolectric — that's expected and the hook isn't called for null
        // traces (per FirebaseUtilHookTest.stopTrace_invokesTraceCaptureHook).
        // So we should see exactly one trace start for FILE_SAVE.
        val starts = capturedTraces.filter { it.second && it.first == FirebaseUtil.Traces.FILE_SAVE }
        assertEquals(
            "Exactly one FILE_SAVE trace start should fire when saveFile runs",
            1, starts.size
        )
    }
}
