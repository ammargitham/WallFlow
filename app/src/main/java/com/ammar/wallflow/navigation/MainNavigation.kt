package com.ammar.wallflow.navigation

import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ammar.wallflow.NavGraphs
import com.ammar.wallflow.ui.common.getPaddingValuesConverter
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.rememberNavHostEngine

@Composable
fun MainNavigation(
    navController: NavHostController,
    contentPadding: PaddingValues,
    applyContentPadding: Boolean,
    modifier: Modifier = Modifier,
) {
    val navHostEngine = rememberNavHostEngine()
    val layoutDirection = LocalLayoutDirection.current
    val hostPadding by animateValueAsState(
        targetValue = if (applyContentPadding) contentPadding else PaddingValues(0.dp),
        typeConverter = getPaddingValuesConverter(layoutDirection),
        animationSpec = remember { tween(delayMillis = 200) },
        label = "hostPadding",
    )

    DestinationsNavHost(
        modifier = modifier
            .fillMaxSize()
            .padding(hostPadding),
        engine = navHostEngine,
        navController = navController,
        navGraph = NavGraphs.main,
    )
}
