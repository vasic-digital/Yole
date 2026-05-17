/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iOS Haptic Feedback
 * Tactile feedback for user interactions
 *
 * K/N API notes (iter-75 fixes):
 *   - UIImpactFeedbackStyle is a Kotlin CEnum. Entries are accessed as
 *     UIImpactFeedbackStyle.UIImpactFeedbackStyleLight etc.
 *   - UINotificationFeedbackType is a Kotlin CEnum. Entries accessed as
 *     UINotificationFeedbackType.UINotificationFeedbackTypeSuccess etc.
 *   - impactOccurred() takes no args; impactOccurredWithIntensity(CGFloat) is
 *     the intensity variant.
 *   - All K/N ObjC interop requires @OptIn(ExperimentalForeignApi::class).
 *
 *########################################################*/
package digital.vasic.yole.ios

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
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
@OptIn(ExperimentalForeignApi::class)
object YoleHapticFeedback {

    private var impactLight: UIImpactFeedbackGenerator? = null
    private var impactMedium: UIImpactFeedbackGenerator? = null
    private var impactHeavy: UIImpactFeedbackGenerator? = null
    private var impactRigid: UIImpactFeedbackGenerator? = null
    private var impactSoft: UIImpactFeedbackGenerator? = null
    private var notification: UINotificationFeedbackGenerator? = null
    private var selection: UISelectionFeedbackGenerator? = null

    /**
     * Initialize all haptic generators.
     * UIImpactFeedbackStyle entries are accessed as enum members, e.g.
     * UIImpactFeedbackStyle.UIImpactFeedbackStyleLight
     */
    fun initialize() {
        impactLight = UIImpactFeedbackGenerator(
            style = UIImpactFeedbackStyle.UIImpactFeedbackStyleLight
        )
        impactMedium = UIImpactFeedbackGenerator(
            style = UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium
        )
        impactHeavy = UIImpactFeedbackGenerator(
            style = UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy
        )
        impactRigid = UIImpactFeedbackGenerator(
            style = UIImpactFeedbackStyle.UIImpactFeedbackStyleRigid
        )
        impactSoft = UIImpactFeedbackGenerator(
            style = UIImpactFeedbackStyle.UIImpactFeedbackStyleSoft
        )
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
     * Trigger haptic feedback.
     * UINotificationFeedbackType entries: UINotificationFeedbackType.UINotificationFeedbackTypeSuccess etc.
     */
    fun trigger(type: HapticFeedbackType) {
        when (type) {
            HapticFeedbackType.LIGHT_IMPACT -> impactLight?.impactOccurred()
            HapticFeedbackType.MEDIUM_IMPACT -> impactMedium?.impactOccurred()
            HapticFeedbackType.HEAVY_IMPACT -> impactHeavy?.impactOccurred()
            HapticFeedbackType.RIGID_IMPACT -> impactRigid?.impactOccurred()
            HapticFeedbackType.SOFT_IMPACT -> impactSoft?.impactOccurred()
            HapticFeedbackType.SUCCESS ->
                notification?.notificationOccurred(
                    UINotificationFeedbackType.UINotificationFeedbackTypeSuccess
                )
            HapticFeedbackType.WARNING ->
                notification?.notificationOccurred(
                    UINotificationFeedbackType.UINotificationFeedbackTypeWarning
                )
            HapticFeedbackType.ERROR ->
                notification?.notificationOccurred(
                    UINotificationFeedbackType.UINotificationFeedbackTypeError
                )
            HapticFeedbackType.SELECTION -> selection?.selectionChanged()
        }
    }

    /**
     * Trigger impact with custom intensity (0.0–1.0).
     *
     * @param style UIImpactFeedbackStyle enum entry
     * @param intensity Intensity between 0.0 and 1.0
     */
    fun impactWithIntensity(style: UIImpactFeedbackStyle, intensity: CGFloat) {
        val generator = UIImpactFeedbackGenerator(style = style)
        generator.impactOccurredWithIntensity(intensity)
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
            HapticFeedbackType.SUCCESS,
            HapticFeedbackType.WARNING,
            HapticFeedbackType.ERROR -> notification?.prepare()
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

    fun onButtonPress() = YoleHapticFeedback.trigger(HapticFeedbackType.LIGHT_IMPACT)
    fun onSuccess() = YoleHapticFeedback.trigger(HapticFeedbackType.SUCCESS)
    fun onError() = YoleHapticFeedback.trigger(HapticFeedbackType.ERROR)
    fun onSelectionChange() = YoleHapticFeedback.trigger(HapticFeedbackType.SELECTION)
    fun onWarning() = YoleHapticFeedback.trigger(HapticFeedbackType.WARNING)
    fun onHeavyAction() = YoleHapticFeedback.trigger(HapticFeedbackType.HEAVY_IMPACT)
}
