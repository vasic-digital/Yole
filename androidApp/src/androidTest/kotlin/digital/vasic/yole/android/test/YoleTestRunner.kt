/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Custom AndroidJUnitRunner that grants Yole's
 * MANAGE_EXTERNAL_STORAGE before any test runs, so MainActivity
 * does NOT intent-jump to system Settings on launch and leave the
 * Compose test rule with no UI tree.
 *
 * Documented as Bucket A of the iter-34 CONST-035 finding (see
 * docs/qa/iter-34/known-issues.md). Replaces the previous workaround
 * of `appops set ...` via shell before each `connectedAndroidTest`
 * invocation, which didn't survive APK reinstall.
 *
 *########################################################*/
package digital.vasic.yole.android.test

import androidx.test.runner.AndroidJUnitRunner

class YoleTestRunner : AndroidJUnitRunner() {
    override fun onStart() {
        // Best-effort grant. uiAutomation is available from API 21+; the
        // app's compile/min SDK both exceed that. If the grant fails for
        // any reason, the test will still launch — it'll just hit the
        // pre-iter-35 permission-bounce failure mode and surface as the
        // existing "No compose hierarchies found" error. Better to surface
        // that than to swallow the original error and produce a misleading
        // PASS.
        try {
            val pkg = "digital.vasic.yole.android"
            uiAutomation.executeShellCommand(
                "appops set $pkg MANAGE_EXTERNAL_STORAGE allow"
            ).close()
            // Pre-grant standard runtime permissions too so tests that
            // touch the storage path don't get a permission prompt mid-run.
            uiAutomation.executeShellCommand(
                "pm grant $pkg android.permission.READ_EXTERNAL_STORAGE"
            ).close()
            uiAutomation.executeShellCommand(
                "pm grant $pkg android.permission.WRITE_EXTERNAL_STORAGE"
            ).close()
        } catch (t: Throwable) {
            // Intentionally swallow — see above. Tests will fail explicitly
            // rather than this swallow producing silent passes.
            android.util.Log.w(
                "YoleTestRunner",
                "Pre-test permission grant failed; tests will likely hit " +
                    "the iter-34 'no compose hierarchies found' failure mode: ${t.message}"
            )
        }
        super.onStart()
    }
}
