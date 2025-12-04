/*#######################################################
 *
 * SPDX-FileCopyrightText: 2022-2025 Gregor Santner <gsantner AT mailbox DOT org>
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
package digital.vasic.opoc.wrapper

/**
 * A convenient wrapper around LinkedHashMap with fluent API and default value support.
 * Provides easy initialization with key-value pairs and automatic default value handling.
 *
 * Example usage:
 * ```kotlin
 * val map = GsHashMap<Int, String>()
 *     .load(1, "one", 2, "two", 3, "three")
 *     .withDefault("unknown")
 * ```
 */
@Suppress("UNCHECKED_CAST", "unused")
class GsHashMap<K, V> {
    private val _data = LinkedHashMap<K, V>()
    private var _defaultValue: V? = null

    /**
     * Clear the map and load it with new key-value pairs.
     * Usage: GsHashMap<Int,String>().load(5,"hi", 6,"bye", ...)
     */
    fun load(vararg keysAndValues2each: Any?): GsHashMap<K, V> {
        _data.clear()
        add(*keysAndValues2each)
        return this
    }

    /**
     * Add key-value pairs to the map.
     * If this is the first pair added and no default is set, the first value becomes the default.
     */
    fun add(vararg keysAndValues2each: Any?): GsHashMap<K, V> {
        if (keysAndValues2each.isNotEmpty() && keysAndValues2each.size >= 2) {
            var i = 0
            while (i + 1 < keysAndValues2each.size) {
                _data[keysAndValues2each[i] as K] = keysAndValues2each[i + 1] as V
                if (i == 0 && _defaultValue == null) {
                    _defaultValue = keysAndValues2each[i + 1] as V
                }
                i += 2
            }
        }
        return this
    }

    /**
     * Get value for key, or return the default value if key doesn't exist.
     */
    fun getOrDefault(key: K): V? {
        return _data[key] ?: _defaultValue
    }

    /**
     * Get value for key, or return the provided default if key doesn't exist.
     * Also sets the internal default value.
     */
    fun getOrDefault(key: K, defaultValue: V): V {
        withDefault(defaultValue)
        return _data[key] ?: _defaultValue!!
    }

    /**
     * Get the underlying LinkedHashMap.
     */
    fun data(): LinkedHashMap<K, V> = _data

    /**
     * Set the default value to be returned when a key doesn't exist.
     */
    fun withDefault(defaultValue: V): GsHashMap<K, V> {
        _defaultValue = defaultValue
        return this
    }

    /**
     * Get all keys in the map.
     */
    fun keySet(): Set<K> = _data.keys

    /**
     * Limit the size of the map by removing oldest entries (insertion order).
     */
    fun limitSizeByRemovingOldest(limit: Int) {
        val actualLimit = maxOf(0, limit)
        while (_data.size > actualLimit) {
            val firstKey = _data.keys.firstOrNull() ?: break
            _data.remove(firstKey)
        }
    }
}
