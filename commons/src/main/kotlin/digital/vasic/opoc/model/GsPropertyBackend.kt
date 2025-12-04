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
 * Generic property storage backend interface.
 * Provides type-safe access to various primitive types and lists.
 * Implementations can use Maps, SharedPreferences, or other storage mechanisms.
 *
 * @param TKEY The type of keys used to access properties
 * @param TTHIS The implementing type (for fluent API with setters)
 */
@Suppress("unused")
interface GsPropertyBackend<TKEY, TTHIS> {

    // Getters
    fun getString(key: TKEY, defaultValue: String): String
    fun getInt(key: TKEY, defaultValue: Int): Int
    fun getLong(key: TKEY, defaultValue: Long): Long
    fun getBool(key: TKEY, defaultValue: Boolean): Boolean
    fun getFloat(key: TKEY, defaultValue: Float): Float
    fun getDouble(key: TKEY, defaultValue: Double): Double
    fun getIntList(key: TKEY): List<Int>
    fun getStringList(key: TKEY): List<String>

    // Setters (fluent API - return this for chaining)
    fun setString(key: TKEY, value: String): TTHIS
    fun setInt(key: TKEY, value: Int): TTHIS
    fun setLong(key: TKEY, value: Long): TTHIS
    fun setBool(key: TKEY, value: Boolean): TTHIS
    fun setFloat(key: TKEY, value: Float): TTHIS
    fun setDouble(key: TKEY, value: Double): TTHIS
    fun setIntList(key: TKEY, value: List<Int>): TTHIS
    fun setStringList(key: TKEY, value: List<String>): TTHIS
}
