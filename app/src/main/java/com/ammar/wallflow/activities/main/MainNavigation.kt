package com.ammar.wallflow.activities.main

import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ammar.wallflow.ui.NavGraphs
import com.ammar.wallflow.ui.animations.materialFadeThroughIn
import com.ammar.wallflow.ui.animations.materialFadeThroughOut
import com.ammar.wallflow.ui.common.getPaddingValuesConverter
import com.ammar.wallflow.ui.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.rememberNavHostEngine
import com.ramcosta.composedestinations.spec.NavHostEngine
import com.ramcosta.composedestinations.spec.Route

@Composable
fun MainNavigation(
    navController: NavHostController,
    contentPadding: PaddingValues,
    applyContentPadding: Boolean,
    nestedScrollConnection: NestedScrollConnection,
    modifier: Modifier = Modifier,
) {
    val navHostEngine = rememberNavHostEngine(
        rootDefaultAnimations = RootNavGraphDefaultAnimations(
            enterTransition = { materialFadeThroughIn() },
            exitTransition = { materialFadeThroughOut() },
        ),
    )
    val layoutDirection = LocalLayoutDirection.current
    val hostPadding by animateValueAsState(
        targetValue = if (applyContentPadding) contentPadding else PaddingValues(0.dp),
        typeConverter = getPaddingValuesConverter(layoutDirection),
        animationSpec = remember { tween(delayMillis = 200) },
        label = "hostPadding",
    )

    Host(
        modifier = modifier
            .fillMaxSize()
            .padding(hostPadding),
        navHostEngine = navHostEngine,
        startRoute = HomeScreenDestination,
        navController = navController,
        nestedScrollConnection = nestedScrollConnection,
    )
}

@Composable
private fun Host(
    modifier: Modifier = Modifier,
    navHostEngine: NavHostEngine,
    startRoute: Route,
    navController: NavHostController,
    nestedScrollConnection: NestedScrollConnection,
) {
    DestinationsNavHost(
        modifier = modifier,
        engine = navHostEngine,
        navController = navController,
        navGraph = NavGraphs.root,
        startRoute = startRoute,
        dependenciesContainerBuilder = {
            dependency(nestedScrollConnection)
        },
    )
}
