/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for UI Animation system
 *
 *########################################################*/
package digital.vasic.yole.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Unit tests for the UI animation system.
 *
 * Tests cover:
 * - Animation timing constants validation
 * - Screen transition functions return proper types
 * - List animation functions return proper types
 * - Animation system constants are within reasonable ranges
 */
class AnimationTests {

    // ==================== AnimationTiming Tests ====================

    @Test
    fun `AnimationTiming constants have correct values`() {
        assertEquals(100, AnimationTiming.VERY_QUICK)
        assertEquals(250, AnimationTiming.QUICK)
        assertEquals(350, AnimationTiming.STANDARD)
        assertEquals(500, AnimationTiming.MODERATE)
        assertEquals(700, AnimationTiming.SLOW)
        assertEquals(900, AnimationTiming.VERY_SLOW)
    }

    // ==================== MicroInteractions Tests ====================

    @Test
    fun `MicroInteractions animation specs are properly configured`() {
        // Test that animation specs are not null
        assertNotNull(MicroInteractions.pressAnimationSpec)
        assertNotNull(MicroInteractions.hoverAnimationSpec)
        assertNotNull(MicroInteractions.rippleAnimationSpec)

        // Test specific properties
        val pressSpec = MicroInteractions.pressAnimationSpec
        assertTrue(pressSpec.dampingRatio > 0f)
        assertTrue(pressSpec.stiffness > 0f)

        val hoverSpec = MicroInteractions.hoverAnimationSpec
        assertTrue(hoverSpec.dampingRatio >= 0f)
        assertTrue(hoverSpec.stiffness > 0f)

        val rippleSpec = MicroInteractions.rippleAnimationSpec
        assertTrue(rippleSpec.durationMillis > 0)
    }

    // ==================== ScreenTransitions Tests ====================

    @Test
    fun `ScreenTransitions slideIn creates proper enter transition`() {
        val transition = ScreenTransitions.slideIn()
        assertNotNull(transition)

        val transitionWithDuration = ScreenTransitions.slideIn(500)
        assertNotNull(transitionWithDuration)
    }

    @Test
    fun `ScreenTransitions slideOut creates proper exit transition`() {
        val transition = ScreenTransitions.slideOut()
        assertNotNull(transition)

        val transitionWithDuration = ScreenTransitions.slideOut(500)
        assertNotNull(transitionWithDuration)
    }

    @Test
    fun `ScreenTransitions fade creates proper enter transition`() {
        val transition = ScreenTransitions.fade()
        assertNotNull(transition)

        val transitionWithDuration = ScreenTransitions.fade(500)
        assertNotNull(transitionWithDuration)
    }

    @Test
    fun `ScreenTransitions scaleIn creates proper enter transition`() {
        val transition = ScreenTransitions.scaleIn()
        assertNotNull(transition)

        val transitionWithDuration = ScreenTransitions.scaleIn(500)
        assertNotNull(transitionWithDuration)
    }

    @Test
    fun `ScreenTransitions scaleOut creates proper exit transition`() {
        val transition = ScreenTransitions.scaleOut()
        assertNotNull(transition)

        val transitionWithDuration = ScreenTransitions.scaleOut(500)
        assertNotNull(transitionWithDuration)
    }

    @Test
    fun `ScreenTransitions expandVertically creates proper enter transition`() {
        val transition = ScreenTransitions.expandVertically()
        assertNotNull(transition)

        val transitionWithDuration = ScreenTransitions.expandVertically(500)
        assertNotNull(transitionWithDuration)
    }

    @Test
    fun `ScreenTransitions shrinkVertically creates proper exit transition`() {
        val transition = ScreenTransitions.shrinkVertically()
        assertNotNull(transition)

        val transitionWithDuration = ScreenTransitions.shrinkVertically(500)
        assertNotNull(transitionWithDuration)
    }

    // ==================== ListAnimations Tests ====================

    @Test
    fun `ListAnimations itemEnter creates proper enter transition with index delay`() {
        val transition0 = ListAnimations.itemEnter(0)
        val transition1 = ListAnimations.itemEnter(1)
        val transition3 = ListAnimations.itemEnter(3)

        assertNotNull(transition0)
        assertNotNull(transition1)
        assertNotNull(transition3)

        // Test with custom parameters
        val customTransition = ListAnimations.itemEnter(2, 5, 600)
        assertNotNull(customTransition)
    }

    @Test
    fun `ListAnimations itemExit creates proper exit transition`() {
        val transition = ListAnimations.itemExit()
        assertNotNull(transition)

        val transitionWithDuration = ListAnimations.itemExit(500)
        assertNotNull(transitionWithDuration)
    }

    // ==================== Animation System Validation ====================

    @Test
    fun `animation timing values are within reasonable ranges`() {
        assertTrue(AnimationTiming.VERY_QUICK in 50..200)
        assertTrue(AnimationTiming.QUICK in 150..350)
        assertTrue(AnimationTiming.STANDARD in 250..450)
        assertTrue(AnimationTiming.MODERATE in 350..650)
        assertTrue(AnimationTiming.SLOW in 500..900)
        assertTrue(AnimationTiming.VERY_SLOW in 700..1200)
    }

    @Test
    fun `animation system functions return valid objects`() {
        // Test that animation functions return non-null objects
        val slideIn = ScreenTransitions.slideIn()
        val fadeIn = ScreenTransitions.fade()
        val scaleIn = ScreenTransitions.scaleIn()

        assertNotNull(slideIn)
        assertNotNull(fadeIn)
        assertNotNull(scaleIn)
    }
}