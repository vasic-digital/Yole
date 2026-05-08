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

    fun init(analyticsInstance: FirebaseAnalytics, crashlyticsInstance: FirebaseCrashlytics) {
        analytics = analyticsInstance
        crashlytics = crashlyticsInstance
        crashlytics.setCrashlyticsCollectionEnabled(true)
        crashlytics.log("FirebaseUtil initialized")
        logEvent("app_initialized", emptyMap())
    }

    /** Log a Firebase Analytics event with optional parameters. */
    fun logEvent(eventName: String, params: Map<String, String> = emptyMap()) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) -> putString(key, value) }
        }
        analytics?.logEvent(eventName, bundle)
    }

    /** Record a non-fatal exception for Crashlytics. */
    fun recordNonFatal(throwable: Throwable, context: String? = null) {
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
