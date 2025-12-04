/*#######################################################
 *
 * SPDX-FileCopyrightText: 2018-2025 Gregor Santner <gsantner AT mailbox DOT org>
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
package digital.vasic.opoc.model

/**
 * In-memory implementation of GsPropertyBackend using HashMaps.
 * Stores properties in separate maps by type for type safety.
 * Useful for temporary storage, testing, or when persistence isn't needed.
 *
 * Example usage:
 * ```kotlin
 * val props = GsMapPropertyBackend<String>()
 *     .setString("name", "John")
 *     .setInt("age", 30)
 *     .setBool("active", true)
 * ```
 */
@Suppress("unused")
class GsMapPropertyBackend<TKEY> : GsPropertyBackend<TKEY, GsMapPropertyBackend<TKEY>> {

    private val _pStringList = HashMap<TKEY, List<String>>()
    private val _pIntList = HashMap<TKEY, List<Int>>()
    private val _pBoolean = HashMap<TKEY, Boolean>()
    private val _pString = HashMap<TKEY, String>()
    private val _pDouble = HashMap<TKEY, Double>()
    private val _pInt = HashMap<TKEY, Int>()
    private val _pFloat = HashMap<TKEY, Float>()
    private val _pLong = HashMap<TKEY, Long>()

    // Getters
    override fun getString(key: TKEY, defaultValue: String): String {
        return _pString.getOrDefault(key, defaultValue)
    }

    override fun getInt(key: TKEY, defaultValue: Int): Int {
        return _pInt.getOrDefault(key, defaultValue)
    }

    override fun getLong(key: TKEY, defaultValue: Long): Long {
        return _pLong.getOrDefault(key, defaultValue)
    }

    override fun getBool(key: TKEY, defaultValue: Boolean): Boolean {
        return _pBoolean.getOrDefault(key, defaultValue)
    }

    override fun getFloat(key: TKEY, defaultValue: Float): Float {
        return _pFloat.getOrDefault(key, defaultValue)
    }

    override fun getDouble(key: TKEY, defaultValue: Double): Double {
        return _pDouble.getOrDefault(key, defaultValue)
    }

    override fun getIntList(key: TKEY): List<Int> {
        return _pIntList[key] ?: emptyList()
    }

    override fun getStringList(key: TKEY): List<String> {
        return _pStringList[key] ?: emptyList()
    }

    // Setters (fluent API)
    override fun setString(key: TKEY, value: String): GsMapPropertyBackend<TKEY> {
        _pString[key] = value
        return this
    }

    override fun setInt(key: TKEY, value: Int): GsMapPropertyBackend<TKEY> {
        _pInt[key] = value
        return this
    }

    override fun setLong(key: TKEY, value: Long): GsMapPropertyBackend<TKEY> {
        _pLong[key] = value
        return this
    }

    override fun setBool(key: TKEY, value: Boolean): GsMapPropertyBackend<TKEY> {
        _pBoolean[key] = value
        return this
    }

    override fun setFloat(key: TKEY, value: Float): GsMapPropertyBackend<TKEY> {
        _pFloat[key] = value
        return this
    }

    override fun setDouble(key: TKEY, value: Double): GsMapPropertyBackend<TKEY> {
        _pDouble[key] = value
        return this
    }

    override fun setIntList(key: TKEY, value: List<Int>): GsMapPropertyBackend<TKEY> {
        _pIntList[key] = value
        return this
    }

    override fun setStringList(key: TKEY, value: List<String>): GsMapPropertyBackend<TKEY> {
        _pStringList[key] = value
        return this
    }
}
