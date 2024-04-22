package com.ammar.wallflow.ui.animations

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn

// From fornewid/material-motion-compose

const val DefaultMotionDuration: Int = 300

private const val ProgressThreshold = 0.35f

internal val Int.ForOutgoing: Int
    get() = (this * ProgressThreshold).toInt()

internal val Int.ForIncoming: Int
    get() = this - this.ForOutgoing

/**
 * [materialFadeThroughIn] allows to switch a layout with fade through enter transition.
 *
 * @param initialScale the starting scale of the enter transition.
 * @param durationMillis the duration of the enter transition.
 */
fun materialFadeThroughIn(
    initialScale: Float = 0.92f,
    durationMillis: Int = DefaultMotionDuration,
): EnterTransition = fadeIn(
    animationSpec = tween(
        durationMillis = durationMillis.ForIncoming,
        easing = LinearOutSlowInEasing,
    ),
) + scaleIn(
    animationSpec = tween(
        durationMillis = durationMillis.ForIncoming,
        easing = LinearOutSlowInEasing,
    ),
    initialScale = initialScale,
)

/**
 * [materialFadeThroughOut] allows to switch a layout with fade through exit transition.
 *
 * @param durationMillis the duration of the exit transition.
 */
fun materialFadeThroughOut(
    durationMillis: Int = DefaultMotionDuration,
): ExitTransition = fadeOut(
    animationSpec = tween(
        durationMillis = durationMillis.ForOutgoing,
        easing = FastOutLinearInEasing,
    ),
)
