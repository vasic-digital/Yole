/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Shared Animation System for Yole
 * Provides consistent animations across all platforms
 *
 *########################################################*/

package digital.vasic.yole.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Animation timing constants
 * Following Material Design motion guidelines
 */
object AnimationTiming {
    /** Very quick animations (100-150ms) - micro-interactions */
    const val VERY_QUICK = 100

    /** Quick animations (200-300ms) - simple transitions */
    const val QUICK = 250

    /** Standard animations (300-400ms) - most UI transitions */
    const val STANDARD = 350

    /** Moderate animations (400-600ms) - complex transitions */
    const val MODERATE = 500

    /** Slow animations (600-800ms) - screen transitions */
    const val SLOW = 700

    /** Very slow animations (800-1000ms) - emphasized transitions */
    const val VERY_SLOW = 900
}

/**
 * Animation easing curves
 */
object AnimationEasing {
    /** Standard easing - most common use case */
    val Standard = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

    /** Emphasized easing - for important UI changes */
    val Emphasized = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    /** Decelerated easing - for entering elements */
    val Decelerate = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    /** Accelerated easing - for exiting elements */
    val Accelerate = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

    /** Sharp easing - for small, precise movements */
    val Sharp = CubicBezierEasing(0.4f, 0.0f, 0.6f, 1.0f)
}

/**
 * Micro-interaction animations
 */
object MicroInteractions {
    /**
     * Press animation spec - subtle scale down on press
     */
    val pressAnimationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /**
     * Hover animation spec - smooth scale up on hover
     */
    val hoverAnimationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    /**
     * Ripple animation spec - expanding circle effect
     */
    val rippleAnimationSpec = tween<Float>(
        durationMillis = AnimationTiming.STANDARD,
        easing = AnimationEasing.Standard
    )
}

/**
 * Modifier for press scale animation (button press effect)
 * Makes the element scale down slightly when pressed
 */
fun Modifier.pressScale(
    scale: Float = 0.95f,
    enabled: Boolean = true
): Modifier = composed {
    if (!enabled) return@composed this

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) scale else 1f,
        animationSpec = MicroInteractions.pressAnimationSpec,
        label = "PressScale"
    )

    this.scale(animatedScale)
}

/**
 * Modifier for hover scale animation (card/button hover effect)
 * Makes the element scale up slightly when hovered
 */
fun Modifier.hoverScale(
    scale: Float = 1.05f,
    enabled: Boolean = true
): Modifier = composed {
    if (!enabled) return@composed this

    var isHovered by remember { mutableStateOf(false) }

    val animatedScale by animateFloatAsState(
        targetValue = if (isHovered) scale else 1f,
        animationSpec = MicroInteractions.hoverAnimationSpec,
        label = "HoverScale"
    )

    this
        .scale(animatedScale)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isHovered = true
                    tryAwaitRelease()
                    isHovered = false
                }
            )
        }
}

/**
 * Modifier for elevation animation on hover
 * Animates shadow/elevation when hovering
 */
fun Modifier.hoverElevation(
    elevationDp: Dp = 8.dp,
    enabled: Boolean = true
): Modifier = composed {
    if (!enabled) return@composed this

    var isHovered by remember { mutableStateOf(false) }

    val animatedElevation by animateDpAsState(
        targetValue = if (isHovered) elevationDp else 0.dp,
        animationSpec = tween(
            durationMillis = AnimationTiming.QUICK,
            easing = AnimationEasing.Standard
        ),
        label = "HoverElevation"
    )

    this.graphicsLayer {
        shadowElevation = animatedElevation.toPx()
    }
}

/**
 * Shake animation for error feedback
 */
fun Modifier.shake(
    enabled: Boolean = false
): Modifier = composed {
    val shakeOffset by animateFloatAsState(
        targetValue = if (enabled) 0f else 1f,
        animationSpec = keyframes {
            durationMillis = AnimationTiming.MODERATE
            0f at 0
            -10f at 100
            10f at 200
            -10f at 300
            10f at 400
            0f at 500
        },
        label = "Shake"
    )

    this.graphicsLayer {
        translationX = if (enabled) shakeOffset else 0f
    }
}

/**
 * Pulse animation for highlighting
 */
fun Modifier.pulse(
    enabled: Boolean = true,
    scale: Float = 1.1f
): Modifier = composed {
    if (!enabled) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = scale,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = AnimationTiming.MODERATE,
                easing = AnimationEasing.Standard
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    this.scale(pulseScale)
}

/**
 * Screen transition specs
 */
object ScreenTransitions {
    /**
     * Slide transition for navigating forward (left to right)
     */
    fun slideIn(durationMillis: Int = AnimationTiming.SLOW): EnterTransition {
        return slideInHorizontally(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = AnimationEasing.Emphasized
            ),
            initialOffsetX = { it }
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = AnimationEasing.Standard
            )
        )
    }

    /**
     * Slide transition for navigating back (right to left)
     */
    fun slideOut(durationMillis: Int = AnimationTiming.SLOW): ExitTransition {
        return slideOutHorizontally(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = AnimationEasing.Emphasized
            ),
            targetOffsetX = { it }
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = AnimationEasing.Standard
            )
        )
    }

    /**
     * Fade transition for simple content changes
     */
    fun fade(durationMillis: Int = AnimationTiming.STANDARD): EnterTransition {
        return fadeIn(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = AnimationEasing.Standard
            )
        )
    }

    /**
     * Scale + fade transition for modal dialogs
     */
    fun scaleIn(durationMillis: Int = AnimationTiming.STANDARD): EnterTransition {
        return scaleIn(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = AnimationEasing.Emphasized
            ),
            initialScale = 0.8f
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = AnimationEasing.Standard
            )
        )
    }

    /**
     * Scale + fade transition for closing modal dialogs
     */
    fun scaleOut(durationMillis: Int = AnimationTiming.STANDARD): ExitTransition {
        return scaleOut(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = AnimationEasing.Emphasized
            ),
            targetScale = 0.8f
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = AnimationEasing.Standard
            )
        )
    }

    /**
     * Expand transition for expanding elements
     */
    fun expandVertically(durationMillis: Int = AnimationTiming.STANDARD): EnterTransition {
        return expandVertically(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = AnimationEasing.Emphasized
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = AnimationEasing.Standard
            )
        )
    }

    /**
     * Shrink transition for collapsing elements
     */
    fun shrinkVertically(durationMillis: Int = AnimationTiming.STANDARD): ExitTransition {
        return shrinkVertically(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = AnimationEasing.Emphasized
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = AnimationEasing.Standard
            )
        )
    }
}

/**
 * Loading state animations
 */
object LoadingAnimations {
    /**
     * Infinite rotation animation for loading spinners
     */
    @Composable
    fun rememberRotation(): Float {
        val infiniteTransition = rememberInfiniteTransition(label = "Rotation")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1000,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "LoadingRotation"
        )
        return rotation
    }

    /**
     * Shimmer effect animation for skeleton screens
     */
    @Composable
    fun rememberShimmer(): Float {
        val infiniteTransition = rememberInfiniteTransition(label = "Shimmer")
        val shimmer by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1500,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "ShimmerProgress"
        )
        return shimmer
    }

    /**
     * Pulse animation for loading indicators
     */
    @Composable
    fun rememberPulse(): Float {
        val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
        val pulse by infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = AnimationTiming.MODERATE,
                    easing = AnimationEasing.Standard
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "PulseOpacity"
        )
        return pulse
    }
}

/**
 * List item animations
 */
object ListAnimations {
    /**
     * Item enter animation with delay for staggered effect
     */
    fun itemEnter(
        index: Int,
        itemsPerWave: Int = 3,
        durationMillis: Int = AnimationTiming.STANDARD
    ): EnterTransition {
        val delay = (index % itemsPerWave) * 50 // 50ms stagger
        return slideInVertically(
            animationSpec = tween(
                durationMillis = durationMillis,
                delayMillis = delay,
                easing = AnimationEasing.Decelerate
            ),
            initialOffsetY = { it / 2 }
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = durationMillis,
                delayMillis = delay,
                easing = AnimationEasing.Standard
            ),
            initialAlpha = 0f
        )
    }

    /**
     * Item exit animation
     */
    fun itemExit(durationMillis: Int = AnimationTiming.QUICK): ExitTransition {
        return slideOutHorizontally(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = AnimationEasing.Accelerate
            ),
            targetOffsetX = { -it }
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = AnimationEasing.Standard
            )
        )
    }
}

/**
 * Composable for animated visibility with custom transitions
 */
@Composable
fun AnimatedVisibilityWithTransition(
    visible: Boolean,
    enter: EnterTransition = ScreenTransitions.fade(),
    exit: ExitTransition = fadeOut(),
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = enter,
        exit = exit
    ) {
        content()
    }
}

/**
 * Composable wrapper for loading states with fade transition
 */
@Composable
fun LoadingStateWrapper(
    isLoading: Boolean,
    loadingContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Box {
        AnimatedVisibility(
            visible = !isLoading,
            enter = ScreenTransitions.fade(AnimationTiming.STANDARD),
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = AnimationTiming.QUICK,
                    easing = AnimationEasing.Standard
                )
            )
        ) {
            content()
        }

        AnimatedVisibility(
            visible = isLoading,
            enter = ScreenTransitions.fade(AnimationTiming.STANDARD),
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = AnimationTiming.QUICK,
                    easing = AnimationEasing.Standard
                )
            )
        ) {
            loadingContent()
        }
    }
}

/**
 * Animated counter for numeric values
 */
@Composable
fun rememberAnimatedCounter(
    targetValue: Int,
    durationMillis: Int = AnimationTiming.MODERATE
): Int {
    val animatedValue by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = AnimationEasing.Emphasized
        ),
        label = "Counter"
    )
    return animatedValue
}

/**
 * Animated progress for progress bars
 */
@Composable
fun rememberAnimatedProgress(
    targetProgress: Float,
    durationMillis: Int = AnimationTiming.MODERATE
): Float {
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = AnimationEasing.Emphasized
        ),
        label = "Progress"
    )
    return animatedProgress
}
