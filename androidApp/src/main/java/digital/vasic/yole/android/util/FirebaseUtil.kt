/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Firebase analytics and crash reporting utility.
 * Provides a unified interface for logging events,
 * crashes, and user properties across the app.
 *
 *########################################################*/
package digital.vasic.yole.android.util

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

/**
 * Centralized Firebase telemetry for Yole Android.
 *
 * Usage:
 *   FirebaseUtil.logEvent("file_saved", mapOf("format" to "markdown"))
 *   FirebaseUtil.recordNonFatal(exception, "Failed to load file")
 *   FirebaseUtil.setUserProperty("preferred_format", "markdown")
 */
object FirebaseUtil {

    private var analytics: FirebaseAnalytics? = null
    private var crashlytics: FirebaseCrashlytics? = null

    /**
     * Test-only hook for CONST-035 anti-bluff verification.
     *
     * When non-null, every `logEvent()` call fires the closure with the
     * event name + params BEFORE forwarding to the real Firebase SDK.
     * Tests can set this to assert that production call sites actually
     * emit the events they claim. Production code paths leave it null.
     *
     * Note: this is `internal` to keep it out of the public API surface
     * but allow same-module tests in androidApp/src/test/ to inject it.
     */
    @JvmField
    internal var testEventCapture: ((eventName: String, params: Map<String, String>) -> Unit)? = null

    /**
     * Test-only hook for verifying non-fatal recording from production code.
     * Mirrors [testEventCapture] for `recordNonFatal()`.
     */
    @JvmField
    internal var testNonFatalCapture: ((throwable: Throwable, contextHint: String?) -> Unit)? = null

    fun init(analyticsInstance: FirebaseAnalytics, crashlyticsInstance: FirebaseCrashlytics) {
        analytics = analyticsInstance
        crashlytics = crashlyticsInstance
        // Use the non-null params directly; Kotlin can't smart-cast the
        // `var` field as non-null at the call site even after assignment.
        crashlyticsInstance.setCrashlyticsCollectionEnabled(true)
        crashlyticsInstance.log("FirebaseUtil initialized")
        logEvent("app_initialized", emptyMap())
    }

    /** Log a Firebase Analytics event with optional parameters. */
    fun logEvent(eventName: String, params: Map<String, String> = emptyMap()) {
        testEventCapture?.invoke(eventName, params)
        val a = analytics ?: return
        val bundle = Bundle().apply {
            params.forEach { (key, value) -> putString(key, value) }
        }
        a.logEvent(eventName, bundle)
    }

    /** Record a non-fatal exception for Crashlytics. */
    fun recordNonFatal(throwable: Throwable, context: String? = null) {
        testNonFatalCapture?.invoke(throwable, context)
        crashlytics?.apply {
            if (context != null) log(context)
            recordException(throwable)
        }
    }

    /** Record a fatal crash log (will be sent on next app launch). */
    fun log(message: String) {
        crashlytics?.log(message)
    }

    /** Set a user property for Analytics segmentation. */
    fun setUserProperty(name: String, value: String) {
        analytics?.setUserProperty(name, value)
    }

    /** Set the user identifier for cross-platform tracking. */
    fun setUserId(userId: String?) {
        crashlytics?.setUserId(userId ?: "")
        analytics?.setUserId(userId)
    }

    // ----- Firebase Performance Monitoring ---------------------------------

    private var perf: FirebasePerformance? = null

    /**
     * Test-only hook firing whenever a Performance trace starts/stops.
     * Signature: (traceName, startedNow) — `startedNow=true` for start,
     * `false` for stop.
     */
    @JvmField
    internal var testTraceCapture: ((traceName: String, startedNow: Boolean) -> Unit)? = null

    /**
     * Begin a custom Performance trace. Returns the trace handle (or null
     * if Performance isn't initialized — production code paths can pass
     * `null` back to [stopTrace] safely). The returned [Trace] is the same
     * type Firebase exposes so callers can call `incrementMetric` / `putAttribute`
     * directly if they need finer-grained instrumentation.
     */
    fun startTrace(name: String): Trace? {
        testTraceCapture?.invoke(name, true)
        val instance = perf ?: return null
        return instance.newTrace(name).also { it.start() }
    }

    /**
     * Stop a Performance trace started by [startTrace]. Null-safe: callers
     * that started a trace before Performance was initialized can pass null
     * here without an extra check.
     */
    fun stopTrace(trace: Trace?) {
        if (trace == null) return
        testTraceCapture?.invoke(/* name */ "(unknown)", /* startedNow */ false)
        trace.stop()
    }

    // ----- Firebase Remote Config ------------------------------------------

    private var remoteConfig: FirebaseRemoteConfig? = null

    /**
     * Test-only hook firing on each Remote Config fetch attempt with the
     * eventual success/failure outcome. Useful so production code that
     * gates feature behavior on Remote Config can be asserted by tests.
     */
    @JvmField
    internal var testRemoteConfigFetchCapture: ((succeeded: Boolean) -> Unit)? = null

    /**
     * Asynchronously fetch + activate Remote Config from the server. On
     * completion, the [onComplete] callback fires with a Boolean indicating
     * whether the fetch + activation succeeded.
     *
     * Production behavior: silently no-op if Remote Config wasn't
     * initialized via [initPerformanceAndConfig], because the app stays
     * functional with whatever default values are in code.
     */
    fun fetchRemoteConfig(onComplete: (success: Boolean) -> Unit = {}) {
        val rc = remoteConfig
        if (rc == null) {
            testRemoteConfigFetchCapture?.invoke(false)
            onComplete(false)
            return
        }
        rc.fetchAndActivate().addOnCompleteListener { task ->
            val ok = task.isSuccessful
            testRemoteConfigFetchCapture?.invoke(ok)
            onComplete(ok)
        }
    }

    /** Synchronous read of a string Remote Config value with a default. */
    fun getConfigString(key: String, default: String): String {
        return remoteConfig?.getString(key)?.takeIf { it.isNotEmpty() } ?: default
    }

    /** Synchronous read of a long Remote Config value with a default. */
    fun getConfigLong(key: String, default: Long): Long {
        return remoteConfig?.getLong(key) ?: default
    }

    /** Synchronous read of a boolean Remote Config value with a default. */
    fun getConfigBoolean(key: String, default: Boolean): Boolean {
        return remoteConfig?.getBoolean(key) ?: default
    }

    /**
     * Initialize Performance Monitoring + Remote Config alongside Analytics.
     * Called after [init] when the caller wants the full stack. Idempotent:
     * subsequent calls overwrite the cached instances but don't double-init
     * the underlying Firebase singletons.
     *
     * [defaults] seeds Remote Config so feature gates work BEFORE the first
     * server fetch completes — critical for cold-start latency. Callers
     * should set this map to the same key/value pairs as the server-side
     * defaults so behavior is stable when offline.
     *
     * [minimumFetchIntervalSeconds] tunes how aggressively the SDK refetches.
     * Default 3600 (1 hour) is safe; lower it during development.
     */
    fun initPerformanceAndConfig(
        defaults: Map<String, Any> = emptyMap(),
        minimumFetchIntervalSeconds: Long = 3600
    ) {
        perf = FirebasePerformance.getInstance()
        val rc = FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(minimumFetchIntervalSeconds)
                    .build()
            )
            if (defaults.isNotEmpty()) {
                setDefaultsAsync(defaults)
            }
        }
        remoteConfig = rc
    }

    /** Predefined Remote Config keys for consistency. */
    object ConfigKeys {
        /** Maximum file size in bytes the editor will open without a warning. */
        const val EDITOR_OPEN_WARN_BYTES = "editor_open_warn_bytes"
        /** Backup retention in days. */
        const val BACKUP_RETENTION_DAYS = "backup_retention_days"
        /** Enable experimental WASM editor surface (currently dev-only). */
        const val ENABLE_WASM_EDITOR = "enable_wasm_editor"
    }

    /** Predefined Performance trace names for consistency. */
    object Traces {
        const val FILE_SAVE = "yole_file_save"
        const val FILE_OPEN = "yole_file_open"
        const val APP_STARTUP_TO_FIRST_TAB = "yole_app_startup_to_first_tab"
    }

    // Predefined event names for consistency
    object Events {
        const val APP_OPEN = "app_open"
        const val FILE_OPENED = "file_opened"
        const val FILE_SAVED = "file_saved"
        const val FILE_DELETED = "file_deleted"
        const val FILE_CREATED = "file_created"
        const val FORMAT_SWITCHED = "format_switched"
        const val CLOUD_CONNECT = "cloud_connect"
        const val CLOUD_SYNC = "cloud_sync"
        const val CLOUD_DISCONNECT = "cloud_disconnect"
        const val SEARCH_PERFORMED = "search_performed"
        const val SETTINGS_CHANGED = "settings_changed"
        const val ERROR_OCCURRED = "error_occurred"
    }

    object Params {
        const val FILE_FORMAT = "file_format"
        const val FILE_SIZE = "file_size"
        const val CLOUD_PROTOCOL = "cloud_protocol"
        const val ERROR_TYPE = "error_type"
        const val ERROR_MESSAGE = "error_message"
        const val DURATION_MS = "duration_ms"
        const val SEARCH_QUERY_LENGTH = "search_query_length"
    }
}
