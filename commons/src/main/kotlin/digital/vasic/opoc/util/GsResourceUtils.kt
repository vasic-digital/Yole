/*#######################################################
 *
 * SPDX-FileCopyrightText: 2016-2025 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2016-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 * Migrated to Kotlin 2025 by Milos Vasic
 *
 * To the extent possible under law, the author(s) have dedicated all copyright
 * and related and neighboring rights to this software to the public domain worldwide.
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 #########################################################*/
package digital.vasic.opoc.util

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.os.Build
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.DisplayMetrics
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.app.ActivityManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import digital.vasic.opoc.format.GsSimpleMarkdownParser
import digital.vasic.opoc.format.GsTextUtils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.reflect.Field
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Resource utilities for Android applications.
 *
 * Provides functionality for:
 * - Resource access (strings, drawables, colors)
 * - App and device information (version, BuildConfig, hardware)
 * - Screen metrics and unit conversion
 * - Localization and locale management
 * - HTML/Spanned conversion
 * - Date/time formatting
 * - Color utilities
 *
 * Extracted from GsContextUtils as part of modularization.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate", "DEPRECATION")
object GsResourceUtils {

    /**
     * Initial locale captured at app startup.
     */
    @SuppressLint("ConstantLocale")
    @JvmField
    val INITIAL_LOCALE: Locale = Locale.getDefault()

    /**
     * Intent extra key for file path.
     */
    @JvmField
    val EXTRA_FILEPATH = "EXTRA_FILEPATH"

    /**
     * MIME type for plain text.
     */
    @JvmField
    val MIME_TEXT_PLAIN = "text/plain"

    //########################
    //## Resource Types
    //########################

    /**
     * Enum representing Android resource types.
     */
    enum class ResType {
        ID, BOOL, INTEGER, COLOR, STRING, ARRAY, DRAWABLE, PLURALS,
        ANIM, ATTR, DIMEN, LAYOUT, MENU, RAW, STYLE, XML
    }

    //########################
    //## Resource Access
    //########################

    /**
     * Find out the numerical resource id by given [ResType].
     *
     * @param context Android context
     * @param resType Resource type
     * @param name Resource name (will be normalized)
     * @return A valid id if the id could be found, else 0
     */
    @JvmStatic
    @SuppressLint("DiscouragedApi")
    fun getResId(context: Context, resType: ResType, name: String): Int {
        return try {
            val normalizedName = name.lowercase(Locale.ROOT)
                .replace("#", "no")
                .replace(Regex("[^A-Za-z0-9_]"), "_")
            context.resources.getIdentifier(
                normalizedName,
                resType.name.lowercase(Locale.ENGLISH),
                context.packageName
            )
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Get String by given string resource id (numeric).
     *
     * @param context Android context
     * @param strResId String resource id
     * @return The string, or null if not found
     */
    @JvmStatic
    fun rstr(context: Context, @StringRes strResId: Int): String? {
        return try {
            context.getString(strResId)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get String by given string resource identifier (textual).
     *
     * @param context Android context
     * @param strResKey String resource key (textual)
     * @param a0getResKeyAsFallback If provided, returns the key itself as fallback
     * @return The string, or fallback/null if not found
     */
    @JvmStatic
    fun rstr(context: Context, strResKey: String, vararg a0getResKeyAsFallback: Any?): String? {
        return try {
            val s = rstr(context, getResId(context, ResType.STRING, strResKey))
            s ?: if (a0getResKeyAsFallback.isNotEmpty()) strResKey else null
        } catch (ignored: Exception) {
            if (a0getResKeyAsFallback.isNotEmpty()) strResKey else null
        }
    }

    /**
     * Get drawable from given resource identifier.
     *
     * @param context Android context
     * @param resId Drawable resource id
     * @return The drawable, or null if not found
     */
    @JvmStatic
    fun rdrawable(context: Context, @DrawableRes resId: Int): Drawable? {
        return try {
            ContextCompat.getDrawable(context, resId)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get color by given color resource id.
     *
     * @param context Android context
     * @param resId Color resource id
     * @return The color as int, or black if invalid
     */
    @JvmStatic
    @ColorInt
    fun rcolor(context: Context?, @ColorRes resId: Int): Int {
        if (context == null || resId == 0) {
            android.util.Log.e("GsResourceUtils", "rcolor: resId is 0!")
            return Color.BLACK
        }
        return ContextCompat.getColor(context, resId)
    }

    /**
     * Checks if all given (textual) resource ids are available.
     *
     * @param context Android context
     * @param resType Resource type
     * @param resIdsTextual Textual identifiers to check
     * @return True if all given ids are available
     */
    @JvmStatic
    fun areResourcesAvailable(context: Context, resType: ResType, vararg resIdsTextual: String): Boolean {
        for (name in resIdsTextual) {
            if (getResId(context, resType, name) == 0) {
                return false
            }
        }
        return true
    }

    //########################
    //## App & Device Information
    //########################

    /**
     * Get Android version as string (e.g., "14 (34)").
     */
    @JvmStatic
    fun getAndroidVersion(): String {
        return "${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})"
    }

    /**
     * Get the app version name from PackageManager.
     *
     * @param context Android context
     * @return Version name, or "?" if not found
     */
    @JvmStatic
    fun getAppVersionName(context: Context): String {
        val manager = context.packageManager
        return try {
            val info = manager.getPackageInfo(getAppIdFlavorSpecific(context), 0)
            info.versionName ?: "unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            try {
                val info = manager.getPackageInfo(getAppIdUsedAtManifest(context), 0)
                info.versionName ?: "?"
            } catch (ignored: PackageManager.NameNotFoundException) {
                "?"
            }
        }
    }

    /**
     * Get the app installation source (Google Play, F-Droid, Sideloaded, etc.).
     *
     * @param context Android context
     * @return Installation source as string
     */
    @JvmStatic
    fun getAppInstallationSource(context: Context): String {
        var src: String? = null
        try {
            src = context.packageManager.getInstallerPackageName(getAppIdFlavorSpecific(context))
        } catch (ignored: Exception) {
            try {
                src = context.packageManager.getInstallerPackageName(getAppIdUsedAtManifest(context))
            } catch (ignored2: Exception) {
            }
        }

        src = if (TextUtils.isEmpty(src)) "" else src!!
        src = src.replace(Regex("^\\s*$"), "Sideloaded")
            .replace(Regex("(?i).*(vending)|(google).*"), "Google Play")
            .replace(Regex("(?i).*fdroid.*"), "F-Droid")
            .replace(Regex("(?i).*amazon.*"), "Amazon Appstore")
            .replace(Regex("(?i).*yalp.*"), "Yalp Store")
            .replace(Regex("(?i).*aptoide.*"), "Aptoide")
            .replace(Regex("(?i).*package.*installer.*"), "Package Installer")
        return src
    }

    /**
     * Get the Application object using reflection.
     *
     * @return Application object, or null if not found
     */
    @JvmStatic
    @SuppressLint("PrivateApi")
    fun getApplicationObject(): Application? {
        return try {
            Class.forName("android.app.AppGlobals")
                .getMethod("getInitialApplication")
                .invoke(null, null as Array<Any?>?) as? Application
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get the app's base package name, which is equal across all build flavors and variants.
     * Reads from string resource "manifest_package_id" if available.
     *
     * @param context Android context
     * @return Package name from manifest
     */
    @JvmStatic
    fun getAppIdUsedAtManifest(context: Context): String {
        val pkg = rstr(context, "manifest_package_id")
        return if (!TextUtils.isEmpty(pkg)) pkg!! else context.packageName
    }

    /**
     * Get this app's package name (flavor-specific).
     *
     * @param context Android context
     * @return Flavor-specific package name
     */
    @JvmStatic
    fun getAppIdFlavorSpecific(context: Context): String {
        return context.packageName
    }

    /**
     * Get field from ${applicationId}.BuildConfig.
     * May be helpful in libraries, where access to BuildConfig would only get values
     * of the library rather than the app ones.
     *
     * @param context Android context
     * @param fieldName BuildConfig field name
     * @return Field value, or null if not found
     */
    @JvmStatic
    fun getBuildConfigValue(context: Context, fieldName: String): Any? {
        val pkg = "${getAppIdUsedAtManifest(context)}.BuildConfig"
        return try {
            val c = Class.forName(pkg)
            c.getField(fieldName).get(null)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get all field names from BuildConfig.
     *
     * @param context Android context
     * @return List of field names
     */
    @JvmStatic
    fun getBuildConfigFields(context: Context): List<String> {
        val pkg = "${getAppIdUsedAtManifest(context)}.BuildConfig"
        val fields = mutableListOf<String>()
        try {
            for (f in Class.forName(pkg).fields) {
                fields.add(f.name)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return fields
    }

    /**
     * Get a BuildConfig bool value.
     *
     * @param context Android context
     * @param fieldName BuildConfig field name
     * @param defaultValue Default value if not found
     * @return Boolean value
     */
    @JvmStatic
    fun bcbool(context: Context, fieldName: String, defaultValue: Boolean): Boolean {
        val field = getBuildConfigValue(context, fieldName)
        return if (field is Boolean) field else defaultValue
    }

    /**
     * Get a BuildConfig string value.
     *
     * @param context Android context
     * @param fieldName BuildConfig field name
     * @param defaultValue Default value if not found
     * @return String value
     */
    @JvmStatic
    fun bcstr(context: Context, fieldName: String, defaultValue: String): String {
        val field = getBuildConfigValue(context, fieldName)
        return if (field is String) field else defaultValue
    }

    /**
     * Get a BuildConfig int value.
     *
     * @param context Android context
     * @param fieldName BuildConfig field name
     * @param defaultValue Default value if not found
     * @return Int value
     */
    @JvmStatic
    fun bcint(context: Context, fieldName: String, defaultValue: Int): Int {
        val field = getBuildConfigValue(context, fieldName)
        return if (field is Int) field else defaultValue
    }

    /**
     * Check if this is a Google Play build (requires BuildConfig field).
     *
     * @param context Android context
     * @return True if Google Play build
     */
    @JvmStatic
    fun isGooglePlayBuild(context: Context): Boolean {
        return bcbool(context, "IS_GPLAY_BUILD", true)
    }

    /**
     * Check if this is a FOSS build (requires BuildConfig field).
     *
     * @param context Android context
     * @return True if FOSS build
     */
    @JvmStatic
    fun isFossBuild(context: Context): Boolean {
        return bcbool(context, "IS_FOSS_BUILD", false)
    }

    //########################
    //## Resource Reading
    //########################

    /**
     * Read text file from raw resources with optional line prefix/postfix.
     *
     * @param context Android context
     * @param rawResId Raw resource id
     * @param linePrefix Prefix to add to each line (optional)
     * @param linePostfix Postfix to add to each line (optional)
     * @return Text content as string
     */
    @JvmStatic
    fun readTextfileFromRawRes(
        context: Context,
        @RawRes rawResId: Int,
        linePrefix: String? = null,
        linePostfix: String? = null
    ): String {
        val sb = StringBuilder()
        val prefix = linePrefix ?: ""
        val postfix = linePostfix ?: ""

        try {
            BufferedReader(InputStreamReader(context.resources.openRawResource(rawResId))).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    sb.append(prefix)
                    sb.append(line)
                    sb.append(postfix)
                    sb.append("\n")
                }
            }
        } catch (ignored: Exception) {
        }
        return sb.toString()
    }

    /**
     * Load a markdown file from raw resources, prepend each line with given text,
     * and convert markdown to HTML using [GsSimpleMarkdownParser].
     *
     * @param context Android context
     * @param rawMdFile Raw resource id of markdown file
     * @param prepend Text to prepend to each line
     * @return HTML string
     */
    @JvmStatic
    fun loadMarkdownForTextViewFromRaw(context: Context, @RawRes rawMdFile: Int, prepend: String): String {
        return try {
            GsSimpleMarkdownParser()
                .parse(
                    context.resources.openRawResource(rawMdFile),
                    prepend,
                    GsSimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW
                )
                .replaceColor("#000001", rcolor(context, getResId(context, ResType.COLOR, "accent")))
                .removeMultiNewlines()
                .replaceBulletCharacter("*")
                .getHtml()
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }
    }

    //########################
    //## Network & Device Status
    //########################

    /**
     * Get internet connection state.
     * Requires ACCESS_NETWORK_STATE permission.
     *
     * @param context Android context
     * @return True if internet connection available
     * @throws RuntimeException if permission not declared
     */
    @JvmStatic
    @SuppressLint("MissingPermission")
    fun isConnectedToInternet(context: Context): Boolean {
        return try {
            val con = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetInfo = con?.activeNetworkInfo
            activeNetInfo != null && activeNetInfo.isConnectedOrConnecting
        } catch (ignored: Exception) {
            throw RuntimeException("Error: Developer forgot to declare a permission")
        }
    }

    /**
     * Check if app with given appId is installed.
     *
     * @param context Android context
     * @param appId Package name to check
     * @return True if app is installed
     */
    @JvmStatic
    fun isAppInstalled(context: Context, appId: String): Boolean {
        return try {
            val pm = context.packageManager
            pm.getPackageInfo(appId, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Check if the device has good hardware capabilities.
     * Criteria: Not low RAM device, 4+ CPU cores, 128+ MB memory class.
     *
     * @param context Android context
     * @return True if device has good hardware
     */
    @JvmStatic
    fun isDeviceGoodHardware(context: Context): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return true
            !ActivityManagerCompat.isLowRamDevice(activityManager) &&
                    Runtime.getRuntime().availableProcessors() >= 4 &&
                    activityManager.memoryClass >= 128
        } catch (ignored: Exception) {
            true
        }
    }

    //########################
    //## Screen & Metrics
    //########################

    /**
     * Estimate this device's screen diagonal size in inches.
     *
     * @param context Android context
     * @return Estimated screen size in inches (clamped between 4 and 12)
     */
    @JvmStatic
    fun getEstimatedScreenSizeInches(context: Context): Double {
        val dm = context.resources.displayMetrics

        var calc = dm.density * 160.0
        val x = Math.pow(dm.widthPixels / calc, 2.0)
        val y = Math.pow(dm.heightPixels / calc, 2.0)
        calc = Math.sqrt(x + y) * 1.16  // 1.16 = est. Nav/Statusbar
        return Math.min(12.0, Math.max(4.0, calc))
    }

    /**
     * Check if the device is currently in portrait orientation.
     *
     * @param context Android context
     * @return True if in portrait mode
     */
    @JvmStatic
    fun isInPortraitMode(context: Context): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

    /**
     * Convert pixel unit to Android dp unit.
     *
     * @param context Android context
     * @param px Pixels to convert
     * @return Dp value
     */
    @JvmStatic
    fun convertPxToDp(context: Context, px: Float): Float {
        return px / context.resources.displayMetrics.density
    }

    /**
     * Convert Android dp unit to pixel unit.
     *
     * @param context Android context
     * @param dp Dp to convert
     * @return Pixel value
     */
    @JvmStatic
    fun convertDpToPx(context: Context, dp: Float): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    //########################
    //## Localization
    //########################

    /**
     * Get a [Locale] out of an Android language code.
     * The androidLC may be in any of the forms: de, en, de-rAt
     *
     * @param androidLC Android language code
     * @return Locale object
     */
    @JvmStatic
    fun getLocaleByAndroidCode(androidLC: String): Locale {
        if (!TextUtils.isEmpty(androidLC)) {
            return if (androidLC.contains("-r")) {
                Locale(androidLC.substring(0, 2), androidLC.substring(4, 6))  // de-rAt
            } else {
                Locale(androidLC)  // de
            }
        }
        return Resources.getSystem().configuration.locale
    }

    /**
     * Set the app's language.
     * androidLC may be in any of the forms: en, de, de-rAt.
     * If given an empty string, the default (system) locale gets loaded.
     *
     * @param context Android context
     * @param androidLC Android language code
     */
    @JvmStatic
    fun setAppLanguage(context: Context, androidLC: String) {
        var locale = getLocaleByAndroidCode(androidLC)
        locale = if (androidLC.isNotEmpty()) locale else Resources.getSystem().configuration.locale
        setAppLocale(context, locale)
    }

    /**
     * Set the app's locale.
     *
     * @param context Android context
     * @param locale Locale to set
     */
    @JvmStatic
    fun setAppLocale(context: Context, locale: Locale?) {
        val config = context.resources.configuration
        config.locale = locale ?: Resources.getSystem().configuration.locale
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, null)
        Locale.setDefault(config.locale)
    }

    //########################
    //## HTML/Spanned
    //########################

    /**
     * Load HTML into a [Spanned] object and set the [TextView]'s text.
     *
     * @param textView TextView to set text on
     * @param html HTML string
     */
    @JvmStatic
    fun setHtmlToTextView(textView: TextView, html: String) {
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.text = SpannableString(htmlToSpanned(html))
    }

    /**
     * Convert an HTML string to an Android [Spanned] object.
     *
     * @param html HTML string
     * @return Spanned object
     */
    @JvmStatic
    fun htmlToSpanned(html: String): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html)
        }
    }

    //########################
    //## Date/Time Formatting
    //########################

    /**
     * Get the localized date format pattern.
     *
     * @param context Android context
     * @return Date format pattern (e.g., "MM/dd/yyyy")
     */
    @JvmStatic
    fun getLocalizedDateFormat(context: Context): String {
        return (android.text.format.DateFormat.getDateFormat(context) as SimpleDateFormat).toPattern()
    }

    /**
     * Get the localized time format pattern.
     *
     * @param context Android context
     * @return Time format pattern (e.g., "HH:mm")
     */
    @JvmStatic
    fun getLocalizedTimeFormat(context: Context): String {
        return (android.text.format.DateFormat.getTimeFormat(context) as SimpleDateFormat).toPattern()
    }

    /**
     * Get the localized date-time format pattern.
     *
     * @param context Android context
     * @return Date-time format pattern
     */
    @JvmStatic
    fun getLocalizedDateTimeFormat(context: Context): String {
        return "${getLocalizedDateFormat(context)} ${getLocalizedTimeFormat(context)}"
    }

    /**
     * Format a date/time using a custom format string.
     *
     * @param locale Locale to use (null = default)
     * @param format Format string for [SimpleDateFormat]
     * @param datetime Time in milliseconds (null = current time)
     * @param fallback Fallback value if format is incorrect
     * @return Formatted string
     */
    @JvmStatic
    fun formatDateTime(
        locale: Locale?,
        format: String,
        datetime: Long?,
        vararg fallback: String?
    ): String {
        return try {
            val l = locale ?: Locale.getDefault()
            val t = datetime ?: System.currentTimeMillis()
            SimpleDateFormat(GsTextUtils.unescapeString(format), l).format(t)
        } catch (err: Exception) {
            if (fallback.isNotEmpty()) fallback[0] ?: format else format
        }
    }

    /**
     * Format a date/time using a custom format string with context locale.
     *
     * @param context Android context (null = use default locale)
     * @param format Format string for [SimpleDateFormat]
     * @param datetime Time in milliseconds (null = current time)
     * @param def Default fallback value
     * @return Formatted string
     */
    @JvmStatic
    fun formatDateTime(
        context: Context?,
        format: String,
        datetime: Long?,
        vararg def: String?
    ): String {
        val locale = if (context != null) {
            ConfigurationCompat.getLocales(context.resources.configuration)[0]
        } else {
            null
        }
        return formatDateTime(locale, format, datetime, *def)
    }

    //########################
    //## Color Utilities
    //########################

    /**
     * Try to guess if the color on top of the given colorOnBottomInt
     * should be light or dark. Returns true if top color should be light.
     *
     * @param colorOnBottomInt Bottom color as int
     * @return True if top color should be light
     */
    @JvmStatic
    fun shouldColorOnTopBeLight(@ColorInt colorOnBottomInt: Int): Boolean {
        val r = Color.red(colorOnBottomInt)
        val g = Color.green(colorOnBottomInt)
        val b = Color.blue(colorOnBottomInt)
        return 186 > (0.299 * r + 0.587 * g + 0.114 * b)
    }

    /**
     * Create an RGB color from components.
     *
     * @param r Red component (0-255)
     * @param g Green component (0-255)
     * @param b Blue component (0-255)
     * @return Color as int
     */
    @JvmStatic
    @ColorInt
    fun rgb(r: Int, g: Int, b: Int): Int {
        return argb(255, r, g, b)
    }

    /**
     * Create an ARGB color from components.
     *
     * @param a Alpha component (0-255)
     * @param r Red component (0-255)
     * @param g Green component (0-255)
     * @param b Blue component (0-255)
     * @return Color as int
     */
    @JvmStatic
    @ColorInt
    fun argb(a: Int, r: Int, g: Int, b: Int): Int {
        val clampedA = a.coerceIn(0, 255)
        val clampedR = r.coerceIn(0, 255)
        val clampedG = g.coerceIn(0, 255)
        val clampedB = b.coerceIn(0, 255)
        return (clampedA shl 24) or (clampedR shl 16) or (clampedG shl 8) or clampedB
    }

    //########################
    //## App Restart
    //########################

    /**
     * Restart the current app. Supply the class to start on startup.
     *
     * @param context Android context
     * @param classToStart Activity class to start on restart
     */
    @JvmStatic
    fun restartApp(context: Context, classToStart: Class<*>) {
        val intent = Intent(context, classToStart)
        var flags = PendingIntent.FLAG_CANCEL_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        val pendi = PendingIntent.getActivity(context, 555, intent, flags)
        val mgr = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

        if (context is Activity) {
            context.finish()
        }

        if (mgr != null) {
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendi)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        Runtime.getRuntime().exit(0)
    }
}
