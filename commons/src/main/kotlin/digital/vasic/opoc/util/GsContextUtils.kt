/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Compatibility stub for GsContextUtils - delegates to specialized utility classes
 * This class has been split into 5 focused modules for better maintainability.
 *
 #########################################################*/
package digital.vasic.opoc.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.print.PrintJob
import android.webkit.WebView

/**
 * Compatibility class for gradual migration.
 * The original GsContextUtils has been refactored into 5 specialized utility classes:
 * - GsResourceUtils: Resource access, BuildConfig, localization
 * - GsStorageUtils: Storage Access Framework, file I/O, permissions
 * - GsImageUtils: Bitmap operations, image processing
 * - GsIntentUtils: Intent creation, activity launching, sharing
 * - GsUiUtils: Theme, keyboard, dialogs, animations
 *
 * This stub maintains compatibility during migration.
 */
@Deprecated(
    "Use specialized utility classes instead: GsResourceUtils, GsStorageUtils, GsImageUtils, GsIntentUtils, GsUiUtils",
    ReplaceWith("GsUiUtils, GsIntentUtils, etc.")
)
open class GsContextUtils {

    /**
     * Start an activity with the given intent.
     * Delegates to GsIntentUtils.
     */
    open fun startActivity(context: Context, intent: Intent) {
        GsIntentUtils.startActivity(context, intent)
    }

    /**
     * Print a WebView.
     * Delegates to GsUiUtils.
     */
    open fun print(webview: WebView, jobName: String, landscape: Boolean): PrintJob? {
        return GsUiUtils.print(webview, jobName, landscape)
    }

    // Delegate methods that were commonly used

    /**
     * Check if dark mode is enabled.
     */
    open fun isDarkModeEnabled(context: Context): Boolean {
        return GsUiUtils.isDarkModeEnabled(context)
    }

    /**
     * Apply day/night theme.
     */
    open fun applyDayNightTheme(themeName: String) {
        GsUiUtils.applyDayNightTheme(themeName)
    }

    /**
     * Convert DP to pixels.
     */
    open fun convertDpToPx(context: Context, dp: Int): Int {
        return GsResourceUtils.convertDpToPx(context, dp.toFloat())
    }

    /**
     * Get resource ID by name.
     */
    open fun getResId(context: Context, type: String, name: String): Int {
        val resType = when (type.lowercase()) {
            "id" -> GsResourceUtils.ResType.ID
            "bool" -> GsResourceUtils.ResType.BOOL
            "integer" -> GsResourceUtils.ResType.INTEGER
            "color" -> GsResourceUtils.ResType.COLOR
            "string" -> GsResourceUtils.ResType.STRING
            "array" -> GsResourceUtils.ResType.ARRAY
            "drawable" -> GsResourceUtils.ResType.DRAWABLE
            "plurals" -> GsResourceUtils.ResType.PLURALS
            "anim" -> GsResourceUtils.ResType.ANIM
            "attr" -> GsResourceUtils.ResType.ATTR
            "dimen" -> GsResourceUtils.ResType.DIMEN
            "layout" -> GsResourceUtils.ResType.LAYOUT
            "menu" -> GsResourceUtils.ResType.MENU
            "raw" -> GsResourceUtils.ResType.RAW
            "style" -> GsResourceUtils.ResType.STYLE
            "xml" -> GsResourceUtils.ResType.XML
            else -> GsResourceUtils.ResType.ID // Default fallback
        }
        return GsResourceUtils.getResId(context, resType, name)
    }

    /**
     * Request APK installation.
     */
    open fun requestApkInstallation(activity: android.app.Activity, file: java.io.File) {
        GsIntentUtils.requestApkInstallation(activity, file)
    }

    /**
     * Animate to activity.
     */
    open fun animateToActivity(activity: android.app.Activity, intent: Intent, finishAnim: Boolean, requestCodeOrNull: Int?) {
        GsIntentUtils.animateToActivity(activity, intent, finishAnim, requestCodeOrNull)
    }

    companion object {
        /**
         * Legacy singleton instance for Java compatibility.
         */
        @JvmField
        @Deprecated("Use utility classes directly")
        val instance = GsContextUtils()
    }
}
