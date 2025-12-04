/*#######################################################
 *
 * SPDX-FileCopyrightText: 2016-2025 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 * Migrated to Kotlin 2025 by Milos Vasic
 *
 * To the extent possible under law, the author(s) have dedicated all copyright
 * and related and neighboring rights to this software to the public domain worldwide.
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 #########################################################*/

/*
 * This is a wrapper for settings based on SharedPreferences
 * with keys in resources. Extend from this class and add
 * getters/setters for the app's settings.
 * Example:
    fun isAppFirstStart(doSet: Boolean): Boolean {
        val value = getInt(R.string.pref_key__app_first_start, -1)
        if (doSet) {
            setBool(R.string.pref_key__app_first_start, true)
        }
        return value == -1
    }

    fun isAppCurrentVersionFirstStart(doSet: Boolean): Boolean {
        val value = getInt(R.string.pref_key__app_first_start_current_version, -1)
        if (doSet) {
            setInt(R.string.pref_key__app_first_start_current_version, BuildConfig.VERSION_CODE)
        }
        return value != BuildConfig.VERSION_CODE
    }
 */

package digital.vasic.opoc.model

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import java.io.File
import java.util.*

/**
 * Wrapper for settings based on SharedPreferences, optionally with keys in resources.
 * Default SharedPreference (_prefApp) will be taken if no SP is specified, else the first one.
 *
 * Provides type-safe access to SharedPreferences with support for:
 * - Primitives (String, Int, Long, Float, Double, Boolean)
 * - Arrays and Lists (String[], Int[], List<String>, List<Int>)
 * - Resource IDs as keys
 * - Preference change listeners
 * - Date/time helpers
 */
@Suppress("unused", "MemberVisibilityCanBePrivate", "SpellCheckingInspection")
open class GsSharedPreferencesPropertyBackend(
    protected val context: Context,
    prefAppName: String? = null
) : GsPropertyBackend<String, GsSharedPreferencesPropertyBackend> {

    protected val prefAppName: String = if (!TextUtils.isEmpty(prefAppName)) {
        prefAppName!!
    } else {
        "${context.packageName}_preferences"
    }

    protected val prefApp: SharedPreferences = context.getSharedPreferences(this.prefAppName, Context.MODE_PRIVATE)

    // Context getter for compatibility (renamed to avoid clash with property getter)
    @JvmName("getContextCompat")
    fun getContext(): Context = context

    // Check if key equals string resource
    fun isKeyEqual(key: String, @StringRes stringKeyResourceId: Int): Boolean {
        return key == rstr(stringKeyResourceId)
    }

    // Reset all settings
    fun resetSettings() {
        resetSettings(prefApp)
    }

    @SuppressLint("ApplySharedPref")
    fun resetSettings(pref: SharedPreferences) {
        pref.edit().clear().commit()
    }

    // Check if preference is set
    fun isPrefSet(@StringRes stringKeyResourceId: Int): Boolean {
        return isPrefSet(prefApp, stringKeyResourceId)
    }

    fun isPrefSet(pref: SharedPreferences, @StringRes stringKeyResourceId: Int): Boolean {
        return pref.contains(rstr(stringKeyResourceId))
    }

    // Preference change listeners
    fun registerPreferenceChangedListener(value: SharedPreferences.OnSharedPreferenceChangeListener) {
        registerPreferenceChangedListener(prefApp, value)
    }

    fun registerPreferenceChangedListener(
        pref: SharedPreferences,
        value: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        pref.registerOnSharedPreferenceChangeListener(value)
    }

    fun unregisterPreferenceChangedListener(value: SharedPreferences.OnSharedPreferenceChangeListener) {
        unregisterPreferenceChangedListener(prefApp, value)
    }

    fun unregisterPreferenceChangedListener(
        pref: SharedPreferences,
        value: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        pref.unregisterOnSharedPreferenceChangeListener(value)
    }

    // Get default preferences
    fun getDefaultPreferences(): SharedPreferences = prefApp

    fun getDefaultPreferencesEditor(): SharedPreferences.Editor = prefApp.edit()

    fun getDefaultPreferencesName(): String = prefAppName

    // Get preference object (default to prefApp if not specified)
    private fun gp(vararg pref: SharedPreferences): SharedPreferences {
        return if (pref.isNotEmpty()) pref[0] else prefApp
    }

    // Resource getters
    fun rstr(@StringRes stringKeyResourceId: Int): String {
        return context.getString(stringKeyResourceId)
    }

    @Suppress("DEPRECATION")
    fun rcolor(@ColorRes resColorId: Int): Int {
        return context.resources.getColor(resColorId)
    }

    fun rstrs(vararg keyResourceIds: Int): Array<String> {
        return Array(keyResourceIds.size) { i -> rstr(keyResourceIds[i]) }
    }

    //
    // String operations
    //
    fun setString(@StringRes keyResourceId: Int, value: String, vararg pref: SharedPreferences) {
        gp(*pref).edit().putString(rstr(keyResourceId), value).apply()
    }

    fun setString(key: String, value: String, vararg pref: SharedPreferences) {
        gp(*pref).edit().putString(key, value).apply()
    }

    fun setString(@StringRes keyResourceId: Int, @StringRes defaultValueResourceId: Int, vararg pref: SharedPreferences) {
        gp(*pref).edit().putString(rstr(keyResourceId), rstr(defaultValueResourceId)).apply()
    }

    fun getString(@StringRes keyResourceId: Int, defaultValue: String, vararg pref: SharedPreferences): String {
        return gp(*pref).getString(rstr(keyResourceId), defaultValue) ?: defaultValue
    }

    fun getString(
        @StringRes keyResourceId: Int,
        @StringRes defaultValueResourceId: Int,
        vararg pref: SharedPreferences
    ): String {
        return gp(*pref).getString(rstr(keyResourceId), rstr(defaultValueResourceId)) ?: rstr(defaultValueResourceId)
    }

    fun getString(key: String, defaultValue: String, vararg pref: SharedPreferences): String {
        return try {
            gp(*pref).getString(key, defaultValue) ?: defaultValue
        } catch (e: ClassCastException) {
            defaultValue
        }
    }

    fun getString(
        @StringRes keyResourceId: Int,
        defaultValue: String,
        @StringRes keyResourceIdDefaultValue: Int,
        vararg pref: SharedPreferences
    ): String {
        return getString(rstr(keyResourceId), rstr(keyResourceIdDefaultValue), *pref)
    }

    // String list/array operations
    private fun setStringListOne(key: String, values: List<String>, pref: SharedPreferences) {
        val sb = StringBuilder()
        values.forEach { value ->
            sb.append(ARRAY_SEPARATOR)
            sb.append(value.replace(ARRAY_SEPARATOR, ARRAY_SEPARATOR_SUBSTITUTE))
        }
        setString(key, sb.toString().replaceFirst(ARRAY_SEPARATOR, ""), pref)
    }

    private fun getStringListOne(key: String, pref: SharedPreferences): ArrayList<String> {
        val value = getString(key, ARRAY_SEPARATOR, pref).replace(ARRAY_SEPARATOR_SUBSTITUTE, ARRAY_SEPARATOR)
        if (value == ARRAY_SEPARATOR || TextUtils.isEmpty(value)) {
            return ArrayList()
        }
        return ArrayList(value.split(ARRAY_SEPARATOR))
    }

    fun setStringArray(@StringRes keyResourceId: Int, values: Array<String>, vararg pref: SharedPreferences) {
        setStringArray(rstr(keyResourceId), values, *pref)
    }

    fun setStringArray(key: String, values: Array<String>, vararg pref: SharedPreferences) {
        setStringListOne(key, values.toList(), gp(*pref))
    }

    fun setStringList(@StringRes keyResourceId: Int, values: List<String>, vararg pref: SharedPreferences) {
        setStringArray(rstr(keyResourceId), values.toTypedArray(), *pref)
    }

    fun setStringList(key: String, values: List<String>, vararg pref: SharedPreferences) {
        setStringArray(key, values.toTypedArray(), *pref)
    }

    fun getStringArray(@StringRes keyResourceId: Int, vararg pref: SharedPreferences): Array<String> {
        return getStringArray(rstr(keyResourceId), *pref)
    }

    fun getStringArray(key: String, vararg pref: SharedPreferences): Array<String> {
        val list = getStringListOne(key, gp(*pref))
        return list.toTypedArray()
    }

    fun getStringList(@StringRes keyResourceId: Int, vararg pref: SharedPreferences): ArrayList<String> {
        return getStringListOne(rstr(keyResourceId), gp(*pref))
    }

    fun getStringList(key: String, vararg pref: SharedPreferences): ArrayList<String> {
        return getStringListOne(key, gp(*pref))
    }

    //
    // Integer operations
    //
    fun setInt(@StringRes keyResourceId: Int, value: Int, vararg pref: SharedPreferences) {
        gp(*pref).edit().putInt(rstr(keyResourceId), value).apply()
    }

    fun setInt(key: String, value: Int, vararg pref: SharedPreferences) {
        gp(*pref).edit().putInt(key, value).apply()
    }

    fun getInt(@StringRes keyResourceId: Int, defaultValue: Int, vararg pref: SharedPreferences): Int {
        return getInt(rstr(keyResourceId), defaultValue, *pref)
    }

    fun getInt(key: String, defaultValue: Int, vararg pref: SharedPreferences): Int {
        return try {
            gp(*pref).getInt(key, defaultValue)
        } catch (e: ClassCastException) {
            defaultValue
        }
    }

    fun getIntOfStringPref(@StringRes keyResId: Int, defaultValue: Int, vararg pref: SharedPreferences): Int {
        return getIntOfStringPref(rstr(keyResId), defaultValue, *pref)
    }

    fun getIntOfStringPref(key: String, defaultValue: Int, vararg pref: SharedPreferences): Int {
        val strNum = getString(key, defaultValue.toString(), gp(*pref))
        return strNum.toInt()
    }

    // Integer list/array operations
    private fun setIntListOne(key: String, values: List<Int>, pref: SharedPreferences) {
        val sb = StringBuilder()
        values.forEach { value ->
            sb.append(ARRAY_SEPARATOR)
            sb.append(value.toString())
        }
        setString(key, sb.toString().replaceFirst(ARRAY_SEPARATOR, ""), pref)
    }

    private fun getIntListOne(key: String, pref: SharedPreferences): ArrayList<Int> {
        val value = getString(key, ARRAY_SEPARATOR, pref)
        if (value == ARRAY_SEPARATOR) {
            return ArrayList()
        }
        return ArrayList(value.split(ARRAY_SEPARATOR).map { it.toInt() })
    }

    fun setIntArray(@StringRes keyResourceId: Int, values: Array<Int>, vararg pref: SharedPreferences) {
        setIntArray(rstr(keyResourceId), values, *pref)
    }

    fun setIntArray(key: String, values: Array<Int>, vararg pref: SharedPreferences) {
        setIntListOne(key, values.toList(), gp(*pref))
    }

    fun getIntArray(@StringRes keyResourceId: Int, vararg pref: SharedPreferences): Array<Int> {
        return getIntArray(rstr(keyResourceId), *pref)
    }

    fun getIntArray(key: String, vararg pref: SharedPreferences): Array<Int> {
        val data = getIntListOne(key, gp(*pref))
        return data.toTypedArray()
    }

    fun setIntList(@StringRes keyResourceId: Int, values: List<Int>, vararg pref: SharedPreferences) {
        setIntListOne(rstr(keyResourceId), values, gp(*pref))
    }

    fun setIntList(key: String, values: List<Int>, vararg pref: SharedPreferences) {
        setIntListOne(key, values, gp(*pref))
    }

    fun getIntList(@StringRes keyResourceId: Int, vararg pref: SharedPreferences): ArrayList<Int> {
        return getIntListOne(rstr(keyResourceId), gp(*pref))
    }

    fun getIntList(key: String, vararg pref: SharedPreferences): ArrayList<Int> {
        return getIntListOne(key, gp(*pref))
    }

    //
    // Long operations
    //
    fun setLong(@StringRes keyResourceId: Int, value: Long, vararg pref: SharedPreferences) {
        gp(*pref).edit().putLong(rstr(keyResourceId), value).apply()
    }

    fun setLong(key: String, value: Long, vararg pref: SharedPreferences) {
        gp(*pref).edit().putLong(key, value).apply()
    }

    fun getLong(@StringRes keyResourceId: Int, defaultValue: Long, vararg pref: SharedPreferences): Long {
        return getLong(rstr(keyResourceId), defaultValue, *pref)
    }

    fun getLong(key: String, defaultValue: Long, vararg pref: SharedPreferences): Long {
        return try {
            gp(*pref).getLong(key, defaultValue)
        } catch (e: ClassCastException) {
            defaultValue
        }
    }

    //
    // Float operations
    //
    fun setFloat(@StringRes keyResourceId: Int, value: Float, vararg pref: SharedPreferences) {
        gp(*pref).edit().putFloat(rstr(keyResourceId), value).apply()
    }

    fun setFloat(key: String, value: Float, vararg pref: SharedPreferences) {
        gp(*pref).edit().putFloat(key, value).apply()
    }

    fun getFloat(@StringRes keyResourceId: Int, defaultValue: Float, vararg pref: SharedPreferences): Float {
        return getFloat(rstr(keyResourceId), defaultValue, *pref)
    }

    fun getFloat(key: String, defaultValue: Float, vararg pref: SharedPreferences): Float {
        return try {
            gp(*pref).getFloat(key, defaultValue)
        } catch (e: ClassCastException) {
            defaultValue
        }
    }

    //
    // Double operations
    //
    fun setDouble(@StringRes keyResourceId: Int, value: Double, vararg pref: SharedPreferences) {
        setLong(rstr(keyResourceId), java.lang.Double.doubleToRawLongBits(value), *pref)
    }

    fun setDouble(key: String, value: Double, vararg pref: SharedPreferences) {
        setLong(key, java.lang.Double.doubleToRawLongBits(value), *pref)
    }

    fun getDouble(@StringRes keyResourceId: Int, defaultValue: Double, vararg pref: SharedPreferences): Double {
        return getDouble(rstr(keyResourceId), defaultValue, *pref)
    }

    fun getDouble(key: String, defaultValue: Double, vararg pref: SharedPreferences): Double {
        return java.lang.Double.longBitsToDouble(
            getLong(key, java.lang.Double.doubleToRawLongBits(defaultValue), gp(*pref))
        )
    }

    //
    // Boolean operations
    //
    fun setBool(@StringRes keyResourceId: Int, value: Boolean, vararg pref: SharedPreferences) {
        gp(*pref).edit().putBoolean(rstr(keyResourceId), value).apply()
    }

    fun setBool(key: String, value: Boolean, vararg pref: SharedPreferences) {
        gp(*pref).edit().putBoolean(key, value).apply()
    }

    fun getBool(@StringRes keyResourceId: Int, defaultValue: Boolean, vararg pref: SharedPreferences): Boolean {
        return getBool(rstr(keyResourceId), defaultValue, *pref)
    }

    fun getBool(key: String, defaultValue: Boolean, vararg pref: SharedPreferences): Boolean {
        return try {
            gp(*pref).getBoolean(key, defaultValue)
        } catch (e: ClassCastException) {
            defaultValue
        }
    }

    // String set operations
    fun getStringSet(@StringRes keyResourceId: Int, defaultValue: List<String>, vararg pref: SharedPreferences): List<String> {
        return getStringSet(rstr(keyResourceId), defaultValue, *pref)
    }

    fun getStringSet(key: String, defaultValue: List<String>, vararg pref: SharedPreferences): List<String> {
        return try {
            ArrayList(gp(*pref).getStringSet(key, HashSet(defaultValue)) ?: HashSet(defaultValue))
        } catch (e: ClassCastException) {
            defaultValue
        }
    }

    //
    // Color operations
    //
    fun getColor(key: String, @ColorRes defaultColor: Int, vararg pref: SharedPreferences): Int {
        return getInt(key, rcolor(defaultColor), *pref)
    }

    fun getColor(@StringRes keyResourceId: Int, @ColorRes defaultColor: Int, vararg pref: SharedPreferences): Int {
        return getColor(rstr(keyResourceId), defaultColor, *pref)
    }

    //
    // PropertyBackend<String> interface implementations
    //
    override fun getString(key: String, defaultValue: String): String {
        return getString(key, defaultValue, prefApp)
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return getInt(key, defaultValue, prefApp)
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return getLong(key, defaultValue, prefApp)
    }

    override fun getBool(key: String, defaultValue: Boolean): Boolean {
        return getBool(key, defaultValue, prefApp)
    }

    fun getStringSet(key: String, defaultValue: List<String>): List<String> {
        return getStringSet(key, defaultValue, prefApp)
    }

    override fun getFloat(key: String, defaultValue: Float): Float {
        return getFloat(key, defaultValue, prefApp)
    }

    override fun getDouble(key: String, defaultValue: Double): Double {
        return getDouble(key, defaultValue, prefApp)
    }

    override fun getIntList(key: String): ArrayList<Int> {
        return getIntList(key, prefApp)
    }

    override fun getStringList(key: String): ArrayList<String> {
        return getStringList(key, prefApp)
    }

    override fun setString(key: String, value: String): GsSharedPreferencesPropertyBackend {
        setString(key, value, prefApp)
        return this
    }

    override fun setInt(key: String, value: Int): GsSharedPreferencesPropertyBackend {
        setInt(key, value, prefApp)
        return this
    }

    override fun setLong(key: String, value: Long): GsSharedPreferencesPropertyBackend {
        setLong(key, value, prefApp)
        return this
    }

    override fun setBool(key: String, value: Boolean): GsSharedPreferencesPropertyBackend {
        setBool(key, value, prefApp)
        return this
    }

    override fun setFloat(key: String, value: Float): GsSharedPreferencesPropertyBackend {
        setFloat(key, value, prefApp)
        return this
    }

    override fun setDouble(key: String, value: Double): GsSharedPreferencesPropertyBackend {
        setDouble(key, value, prefApp)
        return this
    }

    override fun setIntList(key: String, value: List<Int>): GsSharedPreferencesPropertyBackend {
        setIntListOne(key, value, prefApp)
        return this
    }

    override fun setStringList(key: String, value: List<String>): GsSharedPreferencesPropertyBackend {
        setStringListOne(key, value, prefApp)
        return this
    }

    // Contains and remove operations
    fun contains(key: String, vararg pref: SharedPreferences): Boolean {
        return gp(*pref).contains(key)
    }

    fun remove(@StringRes keyResourceId: Int, vararg pref: SharedPreferences) {
        gp(*pref).edit().remove(rstr(keyResourceId)).apply()
    }

    fun remove(key: String, vararg pref: SharedPreferences) {
        gp(*pref).edit().remove(key).apply()
    }

    //
    // Date/time helper methods
    //
    /**
     * Subtract current datetime by given amount of days.
     */
    fun getDateOfDaysAgo(days: Int): Date {
        val cal = GregorianCalendar()
        cal.add(Calendar.DATE, -days)
        return cal.time
    }

    /**
     * Subtract current datetime by given amount of days and check if the given date passed.
     */
    fun didDaysPassedSince(date: Date?, days: Int): Boolean {
        if (date == null || days < 0) {
            return false
        }
        return date.before(getDateOfDaysAgo(days))
    }

    fun afterDaysTrue(key: String, daysSinceLastTime: Int, firstTime: Int, vararg pref: SharedPreferences): Boolean {
        var d = Date(System.currentTimeMillis())

        if (!contains(key, *pref)) {
            d = getDateOfDaysAgo(daysSinceLastTime - firstTime)
            setLong(key, d.time, *pref)
            return firstTime < 1
        } else {
            d = Date(getLong(key, d.time, *pref))
        }

        val trigger = didDaysPassedSince(d, daysSinceLastTime)
        if (trigger) {
            setLong(key, Date(System.currentTimeMillis()).time, *pref)
        }

        return trigger
    }

    companion object {
        const val ARRAY_SEPARATOR = "%%%"
        const val ARRAY_SEPARATOR_SUBSTITUTE = "§§§"
        const val SHARED_PREF_APP = "app"

        private var debugLog = ""

        @JvmStatic
        fun clearDebugLog() {
            debugLog = ""
        }

        @JvmStatic
        fun getDebugLog(): String = debugLog

        @JvmStatic
        @Synchronized
        fun appendDebugLog(text: String) {
            debugLog += "[${Date()}] $text\n"
        }

        @JvmStatic
        fun limitListTo(list: MutableList<*>, maxSize: Int, removeDuplicates: Boolean) {
            var o: Any?
            var pos: Int

            if (removeDuplicates) {
                var i = 0
                while (i < list.size) {
                    o = list[i]
                    pos = list.lastIndexOf(o)
                    while (pos != i && pos >= 0) {
                        list.removeAt(pos)
                        pos = list.lastIndexOf(o)
                    }
                    i++
                }
            }

            while (list.size > maxSize && list.isNotEmpty()) {
                list.removeAt(list.size - 1)
            }
        }

        @JvmStatic
        fun ne(str: String?): Boolean {
            return str != null && str.trim().isNotEmpty()
        }

        @JvmStatic
        fun fexists(fp: String?): Boolean {
            return ne(fp) && File(fp!!).exists()
        }
    }
}
