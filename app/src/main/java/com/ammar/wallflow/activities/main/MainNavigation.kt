package com.ammar.wallflow.activities.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ammar.wallflow.ui.NavGraphs
import com.ammar.wallflow.ui.animations.ForIncoming
import com.ammar.wallflow.ui.animations.ForOutgoing
import com.ammar.wallflow.ui.animations.materialFadeThroughIn
import com.ammar.wallflow.ui.animations.materialFadeThroughOut
import com.ammar.wallflow.ui.common.getPaddingValuesConverter
import com.ammar.wallflow.ui.common.navigation.TwoPaneNavigation
import com.ammar.wallflow.ui.common.navigation.TwoPaneNavigation.Mode
import com.ammar.wallflow.ui.destinations.HomeScreenDestination
import com.ammar.wallflow.ui.destinations.WallpaperScreenDestination
import com.ammar.wallflow.ui.home.HomeScreen
import com.ammar.wallflow.ui.wallpaper.WallpaperViewModel
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.manualcomposablecalls.composable
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.rememberNavHostEngine
import com.ramcosta.composedestinations.spec.NavHostEngine
import com.ramcosta.composedestinations.spec.Route

@Composable
fun MainNavigation(
    modifier: Modifier = Modifier,
    twoPaneController: TwoPaneNavigation.Controller,
    contentPadding: PaddingValues,
    mainActivityViewModel: MainActivityViewModel,
    wallpaperViewModel: WallpaperViewModel,
    applyContentPadding: Boolean,
) {
    val navHostEngine = rememberNavHostEngine(
        rootDefaultAnimations = RootNavGraphDefaultAnimations(
            enterTransition = { materialFadeThroughIn() },
            exitTransition = { materialFadeThroughOut() }
        )
    )
    val layoutDirection = LocalLayoutDirection.current
    val hostPadding by animateValueAsState(
        targetValue = if (applyContentPadding) contentPadding else PaddingValues(0.dp),
        typeConverter = getPaddingValuesConverter(layoutDirection),
        animationSpec = remember { tween(delayMillis = 200) },
        label = "hostPadding",
    )

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(hostPadding),
    ) {
        Host(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            navHostEngine = navHostEngine,
            startRoute = HomeScreenDestination,
            navController = twoPaneController.pane1NavHostController,
            twoPaneController = twoPaneController,
            mainActivityViewModel = mainActivityViewModel,
            wallpaperViewModel = wallpaperViewModel,
            paneSide = TwoPaneNavigation.PaneSide.Pane1,
        )
        AnimatedVisibility(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            visible = twoPaneController.paneMode.value == Mode.TWO_PANE,
            enter = slideInHorizontally(
                animationSpec = tween(
                    durationMillis = 200.ForIncoming,
                    delayMillis = 200.ForOutgoing,
                    easing = LinearOutSlowInEasing
                ),
                initialOffsetX = { it },
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(
                    durationMillis = 200.ForOutgoing,
                    delayMillis = 200.ForOutgoing / 2,
                    easing = FastOutLinearInEasing
                ),
                targetOffsetX = { it },
            ),
        ) {
            Host(
                modifier = Modifier.fillMaxSize(),
                navHostEngine = navHostEngine,
                startRoute = WallpaperScreenDestination,
                navController = twoPaneController.pane2NavHostController,
                twoPaneController = twoPaneController,
                mainActivityViewModel = mainActivityViewModel,
                wallpaperViewModel = wallpaperViewModel,
                paneSide = TwoPaneNavigation.PaneSide.Pane2,
            )
        }
    }
}

@Composable
private fun Host(
    modifier: Modifier = Modifier,
    navHostEngine: NavHostEngine,
    startRoute: Route,
    navController: NavHostController,
    mainActivityViewModel: MainActivityViewModel,
    wallpaperViewModel: WallpaperViewModel,
    twoPaneController: TwoPaneNavigation.Controller,
    paneSide: TwoPaneNavigation.PaneSide,
) {
    DestinationsNavHost(
        modifier = modifier,
        engine = navHostEngine,
        navController = navController,
        navGraph = NavGraphs.root,
        startRoute = startRoute,
        dependenciesContainerBuilder = {
            dependency(paneSide)
            dependency(twoPaneController)
            dependency(mainActivityViewModel)
            dependency(wallpaperViewModel)
        }
    ) {
        composable(HomeScreenDestination) {
            HomeScreen(
                // navigator = destinationsNavigator,
                twoPaneController = twoPaneController,
                wallpaperViewModel = wallpaperViewModel,
            )
        }
    }
}
