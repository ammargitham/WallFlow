package com.ammar.wallflow.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavBackStackEntry
import com.ammar.wallflow.ui.animations.materialFadeThroughIn
import com.ammar.wallflow.ui.animations.materialFadeThroughOut
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle

object MainNavHostAnimatedDestinationStyle : NavHostAnimatedDestinationStyle() {
    override val enterTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
        get() = { materialFadeThroughIn() }
    override val exitTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition
        get() = { materialFadeThroughOut() }
}

object MoreDetailNavHostAnimatedDestinationStyle : NavHostAnimatedDestinationStyle() {
    override val enterTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
        get() = {
            val initialGraphRoute = initialState.destination.parent?.route
            val targetGraphRoute = targetState.destination.parent?.route
            if (initialGraphRoute == targetGraphRoute) {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                )
            } else {
                materialFadeThroughIn()
            }
        }

    override val exitTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition
        get() = {
            val initialGraphRoute = initialState.destination.parent?.route
            val targetGraphRoute = targetState.destination.parent?.route
            if (initialGraphRoute == targetGraphRoute) {
                ExitTransition.None
            } else {
                materialFadeThroughOut()
            }
        }

    override val popEnterTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
        get() = {
            val initialGraphRoute = initialState.destination.parent?.route
            val targetGraphRoute = targetState.destination.parent?.route
            if (initialGraphRoute == targetGraphRoute) {
                EnterTransition.None
            } else {
                materialFadeThroughIn()
            }
        }

    override val popExitTransition:
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition
        get() = {
            val initialGraphRoute = initialState.destination.parent?.route
            val targetGraphRoute = targetState.destination.parent?.route
            if (initialGraphRoute == targetGraphRoute) {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                )
            } else {
                materialFadeThroughOut()
            }
        }
}
