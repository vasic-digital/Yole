/*#######################################################
 *
 * SPDX-FileCopyrightText: 2023 Harshad Vedartham <harshad1 AT zoho DOT com>
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2023 by Harshad Vedartham <harshad1 AT zoho DOT com>
 * Migrated to Kotlin 2025 by Milos Vasic
 *
 * To the extent possible under law, the author(s) have dedicated all copyright
 * and related and neighboring rights to this software to the public domain worldwide.
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 #########################################################*/
package digital.vasic.opoc.util

import android.util.Pair
import digital.vasic.opoc.wrapper.GsCallback

/**
 * General collection utilities providing functional operations on collections.
 * Provides Java-compatible methods while leveraging Kotlin's expressive syntax.
 */
@Suppress("unused")
object GsCollectionUtils {

    /**
     * Like Arrays.asList, but supports null input (returns empty list)
     */
    @JvmStatic
    fun <T> asList(vararg items: T?): List<T> {
        return items.filterNotNull()
    }

    @JvmStatic
    fun <T> addAll(c: MutableCollection<in T>, vararg elements: T?): Boolean {
        return elements.isNotEmpty() && c.addAll(elements.filterNotNull())
    }

    @JvmStatic
    fun <T> addAll(c: MutableCollection<in T>, elements: Collection<T>?): Boolean {
        return elements != null && c.addAll(elements)
    }

    /**
     * Replace each element with result of op. Like list.replaceAll
     */
    @JvmStatic
    fun <T> replaceAll(l: MutableList<T>, op: GsCallback.r1<T, in T>) {
        for (i in l.indices) {
            l[i] = op.callback(l[i])
        }
    }

    /**
     * Apply the operation op on each element of in, and return the result.
     */
    @JvmStatic
    fun <I, O> map(input: Collection<I>, op: GsCallback.r2<O, in I, Int>): List<O> {
        return input.mapIndexed { index, value -> op.callback(value, index) }
    }

    // Map without index
    @JvmStatic
    fun <I, O> map(input: Collection<I>, op: GsCallback.r1<O, in I>): List<O> {
        return input.map { op.callback(it) }
    }

    @JvmStatic
    fun <T : Comparable<T>> listComp(a: List<T>, b: List<T>): Int {
        val minSize = minOf(a.size, b.size)
        for (i in 0 until minSize) {
            val comp = a[i].compareTo(b[i])
            if (comp != 0) {
                return comp
            }
        }
        return a.size.compareTo(b.size)
    }

    /**
     * Sort a list using a key function.
     * Refer to python's sort - https://docs.python.org/3/howto/sorting.html
     *
     * @param list  List to sort
     * @param keyFn Function to generate a self-comparable key from each list item
     * @param comp  Comparator for keys
     * @param T     List type
     * @param K     Key type
     */
    @JvmStatic
    fun <T, K> keySort(
        list: MutableList<T>,
        keyFn: GsCallback.r1<K, in T>,
        comp: Comparator<K>
    ) {
        val decorated = list.map { Pair.create(it, keyFn.callback(it)) }
        val sorted = decorated.sortedWith(compareBy(comp) { it.second })

        list.clear()
        list.addAll(sorted.map { it.first })
    }

    @JvmStatic
    fun <T, K : Comparable<K>> keySort(
        list: MutableList<T>,
        keyFn: GsCallback.r1<K, in T>
    ) {
        keySort(list, keyFn, Comparator { a, b -> a.compareTo(b) })
    }

    /**
     * Return set of elements in a which are not in b
     */
    @JvmStatic
    fun <T> setDiff(a: Collection<T>, b: Collection<T>?): Set<T> {
        val result = LinkedHashSet(a)
        b?.let { result.removeAll(it.toSet()) }
        return result
    }

    /**
     * Check if 2 collections have the same elements
     */
    @JvmStatic
    fun <T> setEquals(a: Collection<T>?, b: Collection<T>?): Boolean {
        return when {
            a === b -> true
            a == null || b == null -> false
            a.size != b.size -> false
            else -> a.containsAll(b)
        }
    }

    /**
     * Set union
     */
    @JvmStatic
    fun <T> union(a: Collection<T>, b: Collection<T>): Set<T> {
        val result = LinkedHashSet(a)
        result.addAll(b)
        return result
    }

    /**
     * Set intersection
     */
    @JvmStatic
    fun <T> intersection(a: Collection<T>, b: Collection<T>): Set<T> {
        val result = LinkedHashSet(a)
        result.retainAll(b.toSet())
        return result
    }

    @JvmStatic
    fun <T, V> accumulate(
        collection: Collection<T>,
        func: GsCallback.r2<V, in T, V>,
        initial: V
    ): V {
        var value = initial
        for (item in collection) {
            value = func.callback(item, value)
        }
        return value
    }

    @JvmStatic
    fun <T> any(
        collection: Collection<T>,
        predicate: GsCallback.b1<T>
    ): Boolean {
        return collection.any { predicate.callback(it) }
    }

    @JvmStatic
    fun <T> all(
        collection: Collection<T>,
        predicate: GsCallback.b1<T>
    ): Boolean {
        return collection.all { predicate.callback(it) }
    }

    /**
     * Get indices of data where predicate is true. Meaningless for unordered data.
     */
    @JvmStatic
    fun <T> indices(data: Collection<T>, predicate: GsCallback.b1<in T>): List<Int> {
        return data.mapIndexedNotNull { index, item ->
            if (predicate.callback(item)) index else null
        }
    }

    /**
     * Select elements of data where predicate is true
     */
    @JvmStatic
    fun <T> select(data: Collection<T>, predicate: GsCallback.b1<in T>): List<T> {
        return data.filter { predicate.callback(it) }
    }

    @JvmStatic
    fun <T> selectFirst(data: Collection<T>, predicate: GsCallback.b1<in T>): T? {
        return data.firstOrNull { predicate.callback(it) }
    }

    /**
     * Get a list of values (like np.arange())
     *
     * @param ops start, stop and step (all optional)
     * @return List of integers with values
     */
    @JvmStatic
    fun range(vararg ops: Int): List<Int> {
        val (start, end, step) = when (ops.size) {
            1 -> Triple(0, ops[0], 1)
            2 -> Triple(ops[0], ops[1], 1)
            else -> Triple(ops.getOrElse(0) { 0 }, ops.getOrElse(1) { 0 }, ops.getOrElse(2) { 1 })
        }

        return if (step > 0) {
            (start until end step step).toList()
        } else {
            emptyList()
        }
    }

    @JvmStatic
    fun <K, V> reverse(map: Map<K, V>): Map<V, K> {
        return map.entries.associate { it.value to it.key }
    }

    @JvmStatic
    fun <K, V> getOrDefault(map: Map<K, V>, key: K, defaultValue: V): V {
        return map.getOrDefault(key, defaultValue)
    }

    @JvmStatic
    fun <K, V> reverseSearch(map: Map<K, V>, value: V): K? {
        return map.entries.firstOrNull { it.value == value }?.key
    }

    @JvmStatic
    fun <T> deduplicate(data: MutableCollection<T>) {
        if (data !is Set<*>) {
            val deduped = LinkedHashSet(data)
            data.clear()
            data.addAll(deduped)
        }
    }

    @JvmStatic
    fun <T> removeIf(data: MutableCollection<T>, predicate: GsCallback.b1<in T>) {
        data.removeAll { predicate.callback(it) }
    }

    @JvmStatic
    fun <T> keepIf(data: MutableCollection<T>, predicate: GsCallback.b1<in T>) {
        data.retainAll { predicate.callback(it) }
    }
}
