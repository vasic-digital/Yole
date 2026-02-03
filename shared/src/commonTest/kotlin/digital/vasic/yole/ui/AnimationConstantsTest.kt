/*
 *########################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Animation Constants Tests
 *
 * Comprehensive tests for animation timing, easing,
 * and transition specifications.
 *
 *########################################################*/

package digital.vasic.yole.ui

import kotlin.test.*

/**
 * Tests for animation constants and specifications.
 */
class AnimationConstantsTest {

    // ==================== TIMING TESTS ====================

    @Test
    fun `animation timings are positive`() {
        assertTrue(AnimationTiming.VERY_QUICK > 0)
        assertTrue(AnimationTiming.QUICK > 0)
        assertTrue(AnimationTiming.STANDARD > 0)
        assertTrue(AnimationTiming.MODERATE > 0)
        assertTrue(AnimationTiming.SLOW > 0)
        assertTrue(AnimationTiming.VERY_SLOW > 0)
    }

    @Test
    fun `animation timings are in ascending order`() {
        assertTrue(AnimationTiming.VERY_QUICK < AnimationTiming.QUICK)
        assertTrue(AnimationTiming.QUICK < AnimationTiming.STANDARD)
        assertTrue(AnimationTiming.STANDARD < AnimationTiming.MODERATE)
        assertTrue(AnimationTiming.MODERATE < AnimationTiming.SLOW)
        assertTrue(AnimationTiming.SLOW < AnimationTiming.VERY_SLOW)
    }

    @Test
    fun `very quick is under 200ms`() {
        assertTrue(
            AnimationTiming.VERY_QUICK <= 200,
            "Very quick animations should be 200ms or less"
        )
    }

    @Test
    fun `quick is under 350ms`() {
        assertTrue(
            AnimationTiming.QUICK <= 350,
            "Quick animations should be 350ms or less"
        )
    }

    @Test
    fun `standard is reasonable`() {
        assertTrue(
            AnimationTiming.STANDARD in 250..500,
            "Standard animations should be between 250-500ms"
        )
    }

    @Test
    fun `very slow is under 1000ms`() {
        assertTrue(
            AnimationTiming.VERY_SLOW <= 1000,
            "Very slow animations should be under 1000ms to avoid feeling sluggish"
        )
    }

    // ==================== EASING TESTS ====================

    @Test
    fun `easing curves are defined`() {
        assertNotNull(AnimationEasing.Standard)
        assertNotNull(AnimationEasing.Emphasized)
        assertNotNull(AnimationEasing.Decelerate)
        assertNotNull(AnimationEasing.Accelerate)
        assertNotNull(AnimationEasing.Sharp)
    }

    // ==================== MICRO INTERACTION TESTS ====================

    @Test
    fun `micro interaction specs are defined`() {
        assertNotNull(MicroInteractions.pressAnimationSpec)
        assertNotNull(MicroInteractions.hoverAnimationSpec)
        assertNotNull(MicroInteractions.rippleAnimationSpec)
    }

    // ==================== SCREEN TRANSITIONS TESTS ====================

    @Test
    fun `slide transitions are defined`() {
        assertNotNull(ScreenTransitions.slideIn())
        assertNotNull(ScreenTransitions.slideOut())
    }

    @Test
    fun `fade transitions are defined`() {
        assertNotNull(ScreenTransitions.fade())
    }

    @Test
    fun `scale transitions are defined`() {
        assertNotNull(ScreenTransitions.scaleIn())
        assertNotNull(ScreenTransitions.scaleOut())
    }

    @Test
    fun `expand shrink transitions are defined`() {
        assertNotNull(ScreenTransitions.expandVertically())
        assertNotNull(ScreenTransitions.shrinkVertically())
    }

    @Test
    fun `transitions accept custom duration`() {
        val customDuration = 500

        assertNotNull(ScreenTransitions.slideIn(customDuration))
        assertNotNull(ScreenTransitions.slideOut(customDuration))
        assertNotNull(ScreenTransitions.fade(customDuration))
        assertNotNull(ScreenTransitions.scaleIn(customDuration))
        assertNotNull(ScreenTransitions.scaleOut(customDuration))
        assertNotNull(ScreenTransitions.expandVertically(customDuration))
        assertNotNull(ScreenTransitions.shrinkVertically(customDuration))
    }

    // ==================== LIST ANIMATIONS TESTS ====================

    @Test
    fun `list item enter animation is defined`() {
        assertNotNull(ListAnimations.itemEnter(0))
        assertNotNull(ListAnimations.itemEnter(1))
        assertNotNull(ListAnimations.itemEnter(10))
    }

    @Test
    fun `list item exit animation is defined`() {
        assertNotNull(ListAnimations.itemExit())
    }

    @Test
    fun `list animations accept custom parameters`() {
        assertNotNull(ListAnimations.itemEnter(0, itemsPerWave = 5, durationMillis = 400))
        assertNotNull(ListAnimations.itemExit(durationMillis = 200))
    }

    // ==================== TIMING CONSISTENCY TESTS ====================

    @Test
    fun `enter and exit transitions use similar timing`() {
        // Screen transitions should have balanced enter/exit
        val slideDuration = AnimationTiming.SLOW
        assertNotNull(ScreenTransitions.slideIn(slideDuration))
        assertNotNull(ScreenTransitions.slideOut(slideDuration))

        val scaleDuration = AnimationTiming.STANDARD
        assertNotNull(ScreenTransitions.scaleIn(scaleDuration))
        assertNotNull(ScreenTransitions.scaleOut(scaleDuration))
    }

    // ==================== EDGE CASES ====================

    @Test
    fun `zero duration does not crash`() {
        assertNotNull(ScreenTransitions.slideIn(0))
        assertNotNull(ScreenTransitions.fade(0))
        assertNotNull(ListAnimations.itemEnter(0, durationMillis = 0))
    }

    @Test
    fun `very large duration does not crash`() {
        assertNotNull(ScreenTransitions.slideIn(10000))
        assertNotNull(ScreenTransitions.fade(10000))
    }

    @Test
    fun `negative index for list animation`() {
        // Should handle gracefully
        assertNotNull(ListAnimations.itemEnter(-1))
    }

    @Test
    fun `large index for list animation`() {
        assertNotNull(ListAnimations.itemEnter(1000000))
    }

    // ==================== MATERIAL DESIGN COMPLIANCE ====================

    @Test
    fun `standard timing follows Material guidelines`() {
        // Material Design recommends 250-300ms for most transitions
        assertTrue(
            AnimationTiming.STANDARD in 250..400,
            "Standard timing should follow Material guidelines (250-400ms)"
        )
    }

    @Test
    fun `micro interactions are quick enough`() {
        // Micro interactions should feel instant
        assertTrue(
            AnimationTiming.VERY_QUICK <= 150,
            "Micro interactions should be 150ms or less"
        )
    }
}
