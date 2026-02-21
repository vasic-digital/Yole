/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iOS Haptic Feedback
 * Tactile feedback for user interactions
 *
 *########################################################*/
package digital.vasic.yole.ios

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UISelectionFeedbackGenerator
import platform.CoreGraphics.CGFloat

/**
 * Haptic Feedback Types
 */
enum class HapticFeedbackType {
    LIGHT_IMPACT,
    MEDIUM_IMPACT,
    HEAVY_IMPACT,
    RIGID_IMPACT,
    SOFT_IMPACT,
    SUCCESS,
    WARNING,
    ERROR,
    SELECTION
}

/**
 * Haptic Feedback Manager for iOS
 * Provides tactile feedback for user interactions
 */
object YoleHapticFeedback {
    
    private var impactLight: UIImpactFeedbackGenerator? = null
    private var impactMedium: UIImpactFeedbackGenerator? = null
    private var impactHeavy: UIImpactFeedbackGenerator? = null
    private var impactRigid: UIImpactFeedbackGenerator? = null
    private var impactSoft: UIImpactFeedbackGenerator? = null
    private var notification: UINotificationFeedbackGenerator? = null
    private var selection: UISelectionFeedbackGenerator? = null
    
    /**
     * Initialize all haptic generators
     */
    fun initialize() {
        impactLight = UIImpactFeedbackGenerator(style = UIImpactFeedbackGenerator.FeedbackStyle.Light)
        impactMedium = UIImpactFeedbackGenerator(style = UIImpactFeedbackGenerator.FeedbackStyle.Medium)
        impactHeavy = UIImpactFeedbackGenerator(style = UIImpactFeedbackGenerator.FeedbackStyle.Heavy)
        impactRigid = UIImpactFeedbackGenerator(style = UIImpactFeedbackGenerator.FeedbackStyle.Rigid)
        impactSoft = UIImpactFeedbackGenerator(style = UIImpactFeedbackGenerator.FeedbackStyle.Soft)
        notification = UINotificationFeedbackGenerator()
        selection = UISelectionFeedbackGenerator()
        
        // Prepare generators for immediate feedback
        impactLight?.prepare()
        impactMedium?.prepare()
        impactHeavy?.prepare()
        impactRigid?.prepare()
        impactSoft?.prepare()
        notification?.prepare()
        selection?.prepare()
    }
    
    /**
     * Trigger haptic feedback
     */
    fun trigger(type: HapticFeedbackType) {
        when (type) {
            HapticFeedbackType.LIGHT_IMPACT -> impactLight?.impactOccurred()
            HapticFeedbackType.MEDIUM_IMPACT -> impactMedium?.impactOccurred()
            HapticFeedbackType.HEAVY_IMPACT -> impactHeavy?.impactOccurred()
            HapticFeedbackType.RIGID_IMPACT -> impactRigid?.impactOccurred()
            HapticFeedbackType.SOFT_IMPACT -> impactSoft?.impactOccurred()
            HapticFeedbackType.SUCCESS -> notification?.notificationOccurred(UINotificationFeedbackGenerator.FeedbackType.Success)
            HapticFeedbackType.WARNING -> notification?.notificationOccurred(UINotificationFeedbackGenerator.FeedbackType.Warning)
            HapticFeedbackType.ERROR -> notification?.notificationOccurred(UINotificationFeedbackGenerator.FeedbackType.Error)
            HapticFeedbackType.SELECTION -> selection?.selectionChanged()
        }
    }
    
    /**
     * Trigger impact with custom intensity
     */
    fun impactWithIntensity(style: UIImpactFeedbackGenerator.FeedbackStyle, intensity: CGFloat) {
        val generator = UIImpactFeedbackGenerator(style = style)
        generator.impactOccurred(intensity)
    }
    
    /**
     * Prepare for upcoming haptic event
     */
    fun prepare(type: HapticFeedbackType) {
        when (type) {
            HapticFeedbackType.LIGHT_IMPACT -> impactLight?.prepare()
            HapticFeedbackType.MEDIUM_IMPACT -> impactMedium?.prepare()
            HapticFeedbackType.HEAVY_IMPACT -> impactHeavy?.prepare()
            HapticFeedbackType.RIGID_IMPACT -> impactRigid?.prepare()
            HapticFeedbackType.SOFT_IMPACT -> impactSoft?.prepare()
            HapticFeedbackType.SUCCESS, HapticFeedbackType.WARNING, HapticFeedbackType.ERROR -> notification?.prepare()
            HapticFeedbackType.SELECTION -> selection?.prepare()
        }
    }
    
    /**
     * Cleanup generators
     */
    fun cleanup() {
        impactLight = null
        impactMedium = null
        impactHeavy = null
        impactRigid = null
        impactSoft = null
        notification = null
        selection = null
    }
}

/**
 * Extension functions for common UI interactions
 */
object YoleHapticExtensions {
    
    /**
     * Trigger haptic on button press
     */
    fun onButtonPress() {
        YoleHapticFeedback.trigger(HapticFeedbackType.LIGHT_IMPACT)
    }
    
    /**
     * Trigger haptic on success
     */
    fun onSuccess() {
        YoleHapticFeedback.trigger(HapticFeedbackType.SUCCESS)
    }
    
    /**
     * Trigger haptic on error
     */
    fun onError() {
        YoleHapticFeedback.trigger(HapticFeedbackType.ERROR)
    }
    
    /**
     * Trigger haptic on selection change
     */
    fun onSelectionChange() {
        YoleHapticFeedback.trigger(HapticFeedbackType.SELECTION)
    }
    
    /**
     * Trigger haptic on warning
     */
    fun onWarning() {
        YoleHapticFeedback.trigger(HapticFeedbackType.WARNING)
    }
    
    /**
     * Trigger haptic on heavy action
     */
    fun onHeavyAction() {
        YoleHapticFeedback.trigger(HapticFeedbackType.HEAVY_IMPACT)
    }
}
