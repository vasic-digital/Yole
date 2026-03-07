/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive Animation Utility Tests
 *
 * Deep tests for AnimationTiming, AnimationEasing,
 * MicroInteractions, ScreenTransitions, and ListAnimations.
 *
 *########################################################*/
package digital.vasic.yole.ui

import androidx.compose.animation.core.Spring
import kotlin.test.*

/**
 * Comprehensive tests for animation utilities.
 *
 * Covers:
 * - AnimationTiming: constant values, ordering, reasonable ranges
 * - AnimationEasing: easing curve definitions
 * - MicroInteractions: press, hover, ripple animation specs
 * - ScreenTransitions: slide, fade, scale, expand/shrink transitions
 * - ListAnimations: staggered item enter/exit transitions
 * - Edge cases: zero duration, large duration, negative index
 */
class AnimationsTests {

    // ====================================================================
    // ANIMATION TIMING CONSTANTS
    // ====================================================================

    @Test
    fun `timing VERY_QUICK is 100ms`() {
        assertEquals(100, AnimationTiming.VERY_QUICK)
    }

    @Test
    fun `timing QUICK is 250ms`() {
        assertEquals(250, AnimationTiming.QUICK)
    }

    @Test
    fun `timing STANDARD is 350ms`() {
        assertEquals(350, AnimationTiming.STANDARD)
    }

    @Test
    fun `timing MODERATE is 500ms`() {
        assertEquals(500, AnimationTiming.MODERATE)
    }

    @Test
    fun `timing SLOW is 700ms`() {
        assertEquals(700, AnimationTiming.SLOW)
    }

    @Test
    fun `timing VERY_SLOW is 900ms`() {
        assertEquals(900, AnimationTiming.VERY_SLOW)
    }

    @Test
    fun `timing values are strictly ascending`() {
        val timings = listOf(
            AnimationTiming.VERY_QUICK,
            AnimationTiming.QUICK,
            AnimationTiming.STANDARD,
            AnimationTiming.MODERATE,
            AnimationTiming.SLOW,
            AnimationTiming.VERY_SLOW
        )
        for (i in 0 until timings.size - 1) {
            assertTrue(
                timings[i] < timings[i + 1],
                "Timing[${i}]=${timings[i]} should be less than Timing[${i + 1}]=${timings[i + 1]}"
            )
        }
    }

    @Test
    fun `timing values are all positive`() {
        assertTrue(AnimationTiming.VERY_QUICK > 0)
        assertTrue(AnimationTiming.QUICK > 0)
        assertTrue(AnimationTiming.STANDARD > 0)
        assertTrue(AnimationTiming.MODERATE > 0)
        assertTrue(AnimationTiming.SLOW > 0)
        assertTrue(AnimationTiming.VERY_SLOW > 0)
    }

    @Test
    fun `timing all values under 1 second`() {
        assertTrue(AnimationTiming.VERY_SLOW <= 1000, "No animation should exceed 1 second")
    }

    @Test
    fun `timing VERY_QUICK is suitable for micro-interactions`() {
        assertTrue(AnimationTiming.VERY_QUICK <= 150, "Micro-interactions should be under 150ms")
    }

    @Test
    fun `timing STANDARD is suitable for most UI transitions`() {
        assertTrue(AnimationTiming.STANDARD in 250..450, "Standard should be 250-450ms")
    }

    // ====================================================================
    // ANIMATION EASING CURVES
    // ====================================================================

    @Test
    fun `easing Standard is defined`() {
        assertNotNull(AnimationEasing.Standard)
    }

    @Test
    fun `easing Emphasized is defined`() {
        assertNotNull(AnimationEasing.Emphasized)
    }

    @Test
    fun `easing Decelerate is defined`() {
        assertNotNull(AnimationEasing.Decelerate)
    }

    @Test
    fun `easing Accelerate is defined`() {
        assertNotNull(AnimationEasing.Accelerate)
    }

    @Test
    fun `easing Sharp is defined`() {
        assertNotNull(AnimationEasing.Sharp)
    }

    @Test
    fun `easing Emphasized and Decelerate have same values`() {
        // Both use CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
        // They should be functionally equivalent
        assertEquals(AnimationEasing.Emphasized, AnimationEasing.Decelerate)
    }

    @Test
    fun `easing Standard differs from Accelerate`() {
        assertNotEquals(AnimationEasing.Standard, AnimationEasing.Accelerate)
    }

    @Test
    fun `easing Standard differs from Sharp`() {
        assertNotEquals(AnimationEasing.Standard, AnimationEasing.Sharp)
    }

    // ====================================================================
    // MICRO INTERACTIONS
    // ====================================================================

    @Test
    fun `microInteractions pressAnimationSpec has MediumBouncy damping`() {
        val spec = MicroInteractions.pressAnimationSpec
        assertEquals(Spring.DampingRatioMediumBouncy, spec.dampingRatio)
    }

    @Test
    fun `microInteractions pressAnimationSpec has Medium stiffness`() {
        val spec = MicroInteractions.pressAnimationSpec
        assertEquals(Spring.StiffnessMedium, spec.stiffness)
    }

    @Test
    fun `microInteractions hoverAnimationSpec has NoBouncy damping`() {
        val spec = MicroInteractions.hoverAnimationSpec
        assertEquals(Spring.DampingRatioNoBouncy, spec.dampingRatio)
    }

    @Test
    fun `microInteractions hoverAnimationSpec has Low stiffness`() {
        val spec = MicroInteractions.hoverAnimationSpec
        assertEquals(Spring.StiffnessLow, spec.stiffness)
    }

    @Test
    fun `microInteractions rippleAnimationSpec has STANDARD duration`() {
        val spec = MicroInteractions.rippleAnimationSpec
        assertEquals(AnimationTiming.STANDARD, spec.durationMillis)
    }

    @Test
    fun `microInteractions all specs are non-null`() {
        assertNotNull(MicroInteractions.pressAnimationSpec)
        assertNotNull(MicroInteractions.hoverAnimationSpec)
        assertNotNull(MicroInteractions.rippleAnimationSpec)
    }

    // ====================================================================
    // SCREEN TRANSITIONS
    // ====================================================================

    @Test
    fun `screenTransitions slideIn default duration`() {
        val transition = ScreenTransitions.slideIn()
        assertNotNull(transition)
    }

    @Test
    fun `screenTransitions slideIn custom duration`() {
        val transition = ScreenTransitions.slideIn(300)
        assertNotNull(transition)
    }

    @Test
    fun `screenTransitions slideOut default duration`() {
        val transition = ScreenTransitions.slideOut()
        assertNotNull(transition)
    }

    @Test
    fun `screenTransitions slideOut custom duration`() {
        val transition = ScreenTransitions.slideOut(300)
        assertNotNull(transition)
    }

    @Test
    fun `screenTransitions fade default duration`() {
        val transition = ScreenTransitions.fade()
        assertNotNull(transition)
    }

    @Test
    fun `screenTransitions fade custom duration`() {
        val transition = ScreenTransitions.fade(200)
        assertNotNull(transition)
    }

    @Test
    fun `screenTransitions scaleIn default duration`() {
        val transition = ScreenTransitions.scaleIn()
        assertNotNull(transition)
    }

    @Test
    fun `screenTransitions scaleOut default duration`() {
        val transition = ScreenTransitions.scaleOut()
        assertNotNull(transition)
    }

    @Test
    fun `screenTransitions expandVertically default duration`() {
        val transition = ScreenTransitions.expandVertically()
        assertNotNull(transition)
    }

    @Test
    fun `screenTransitions shrinkVertically default duration`() {
        val transition = ScreenTransitions.shrinkVertically()
        assertNotNull(transition)
    }

    @Test
    fun `screenTransitions all functions with zero duration`() {
        assertNotNull(ScreenTransitions.slideIn(0))
        assertNotNull(ScreenTransitions.slideOut(0))
        assertNotNull(ScreenTransitions.fade(0))
        assertNotNull(ScreenTransitions.scaleIn(0))
        assertNotNull(ScreenTransitions.scaleOut(0))
        assertNotNull(ScreenTransitions.expandVertically(0))
        assertNotNull(ScreenTransitions.shrinkVertically(0))
    }

    @Test
    fun `screenTransitions all functions with large duration`() {
        assertNotNull(ScreenTransitions.slideIn(5000))
        assertNotNull(ScreenTransitions.slideOut(5000))
        assertNotNull(ScreenTransitions.fade(5000))
        assertNotNull(ScreenTransitions.scaleIn(5000))
        assertNotNull(ScreenTransitions.scaleOut(5000))
        assertNotNull(ScreenTransitions.expandVertically(5000))
        assertNotNull(ScreenTransitions.shrinkVertically(5000))
    }

    @Test
    fun `screenTransitions all functions with STANDARD timing`() {
        val duration = AnimationTiming.STANDARD
        assertNotNull(ScreenTransitions.slideIn(duration))
        assertNotNull(ScreenTransitions.slideOut(duration))
        assertNotNull(ScreenTransitions.fade(duration))
        assertNotNull(ScreenTransitions.scaleIn(duration))
        assertNotNull(ScreenTransitions.scaleOut(duration))
        assertNotNull(ScreenTransitions.expandVertically(duration))
        assertNotNull(ScreenTransitions.shrinkVertically(duration))
    }

    // ====================================================================
    // LIST ANIMATIONS
    // ====================================================================

    @Test
    fun `listAnimations itemEnter index 0`() {
        val transition = ListAnimations.itemEnter(0)
        assertNotNull(transition)
    }

    @Test
    fun `listAnimations itemEnter index 1`() {
        val transition = ListAnimations.itemEnter(1)
        assertNotNull(transition)
    }

    @Test
    fun `listAnimations itemEnter index 5`() {
        val transition = ListAnimations.itemEnter(5)
        assertNotNull(transition)
    }

    @Test
    fun `listAnimations itemEnter with custom itemsPerWave`() {
        val transition = ListAnimations.itemEnter(2, itemsPerWave = 5)
        assertNotNull(transition)
    }

    @Test
    fun `listAnimations itemEnter with custom duration`() {
        val transition = ListAnimations.itemEnter(0, durationMillis = 500)
        assertNotNull(transition)
    }

    @Test
    fun `listAnimations itemEnter with all custom params`() {
        val transition = ListAnimations.itemEnter(3, itemsPerWave = 4, durationMillis = 600)
        assertNotNull(transition)
    }

    @Test
    fun `listAnimations itemExit default`() {
        val transition = ListAnimations.itemExit()
        assertNotNull(transition)
    }

    @Test
    fun `listAnimations itemExit custom duration`() {
        val transition = ListAnimations.itemExit(durationMillis = 100)
        assertNotNull(transition)
    }

    @Test
    fun `listAnimations itemEnter negative index does not crash`() {
        val transition = ListAnimations.itemEnter(-1)
        assertNotNull(transition)
    }

    @Test
    fun `listAnimations itemEnter very large index does not crash`() {
        val transition = ListAnimations.itemEnter(1_000_000)
        assertNotNull(transition)
    }

    @Test
    fun `listAnimations itemEnter zero duration does not crash`() {
        val transition = ListAnimations.itemEnter(0, durationMillis = 0)
        assertNotNull(transition)
    }

    @Test
    fun `listAnimations itemExit zero duration does not crash`() {
        val transition = ListAnimations.itemExit(durationMillis = 0)
        assertNotNull(transition)
    }

    // ====================================================================
    // ANIMATION TIMING WITH ACCESSIBILITY
    // ====================================================================

    @Test
    fun `all animation durations are under the accessibility threshold`() {
        // Animations over 1 second can cause issues for users sensitive to motion
        val allTimings = listOf(
            AnimationTiming.VERY_QUICK,
            AnimationTiming.QUICK,
            AnimationTiming.STANDARD,
            AnimationTiming.MODERATE,
            AnimationTiming.SLOW,
            AnimationTiming.VERY_SLOW
        )
        allTimings.forEach { timing ->
            assertTrue(timing <= 1000, "Animation timing $timing should be <= 1000ms for accessibility")
        }
    }

    @Test
    fun `micro-interaction durations are imperceptible`() {
        // Micro-interactions should complete within 200ms to feel instant
        assertTrue(AnimationTiming.VERY_QUICK <= 200)
    }
}
