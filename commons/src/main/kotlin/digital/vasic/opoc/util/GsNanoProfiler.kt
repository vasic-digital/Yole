/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2025 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 * Migrated to Kotlin 2025 by Milos Vasic
 *
 * To the extent possible under law, the author(s) have dedicated all copyright
 * and related and neighboring rights to this software to the public domain worldwide.
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 #########################################################*/
package digital.vasic.opoc.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * A timer for quick time measurement. Nano - in both, time and functions.
 *
 * Provides simple profiling capabilities for measuring execution time of code blocks.
 * Can group multiple measurements together and track cumulative time.
 *
 * Example usage:
 * ```kotlin
 * val profiler = GsNanoProfiler()
 * profiler.start(true, "initialization")
 * // ... code to profile
 * profiler.end()
 * profiler.printProfilingGroup()
 * ```
 */
class GsNanoProfiler {

    private val formatter = DecimalFormat("000000000.0000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
    private var profilingGroupValue = 0L
    private var groupCount = 0
    private var profilerEnabled = true
    private var profilingValue = -1L
    private var text: String = ""

    companion object {
        private var debugText = ""

        /**
         * Reset and retrieve the accumulated debug text from all profilers.
         */
        @JvmStatic
        fun resetDebugText(): String {
            val text = debugText
            debugText = ""
            return text
        }
    }

    /**
     * Enable or disable this profiler.
     * When disabled, profiling operations are no-ops.
     */
    fun setEnabled(enabled: Boolean): GsNanoProfiler {
        profilerEnabled = enabled
        return this
    }

    /**
     * Start profiling a new code block.
     *
     * @param increaseGroupCounter If true, starts a new profiling group
     * @param optionalText Optional label for this profiling session
     */
    fun start(increaseGroupCounter: Boolean, vararg optionalText: String) {
        if (profilerEnabled) {
            if (increaseGroupCounter) {
                groupCount++
                profilingGroupValue = 0
            }
            text = if (optionalText.size == 1) optionalText[0] else "action"
            profilingValue = System.nanoTime()
        }
    }

    /**
     * End the current profiling session and immediately start a new one.
     */
    fun restart(vararg optionalText: String) {
        end()
        start(false, *optionalText)
    }

    /**
     * Print the cumulative time for the current profiling group.
     */
    fun printProfilingGroup() {
        if (profilerEnabled) {
            val formattedTime = formatter.format(profilingGroupValue / 1000f).replace(Regex("\\G0"), " ")
            val output = "NanoProfiler::: $groupCount$formattedTime [ms] for Group $groupCount"
            debugText += "$output\n"
            println(output)
        }
    }

    /**
     * End the current profiling session and print the elapsed time.
     */
    fun end() {
        val now = System.nanoTime()
        if (profilerEnabled) {
            profilingValue = now - profilingValue
            profilingGroupValue += (profilingValue / 1000f).toLong()
            val formattedTime = formatter.format(profilingValue / 1000f).replace(Regex("\\G0"), " ")
            val output = "NanoProfiler::: $groupCount$formattedTime [µs] for $text"
            debugText += "$output\n"
            println(output)
        }
    }
}
