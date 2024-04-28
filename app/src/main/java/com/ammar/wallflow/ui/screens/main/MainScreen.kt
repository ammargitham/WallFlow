package com.ammar.wallflow.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ammar.wallflow.NavGraphs
import com.ammar.wallflow.destinations.WallhavenApiKeyDialogDestination
import com.ammar.wallflow.extensions.toDp
import com.ammar.wallflow.navigation.AppNavGraphs.RootNavGraph
import com.ammar.wallflow.ui.common.bottombar.BottomBar
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
import com.ammar.wallflow.ui.common.bottombar.NavRail
import com.ammar.wallflow.ui.common.globalerrors.GlobalErrorsColumn
import com.ammar.wallflow.ui.common.topWindowInsets
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.rememberNavHostEngine
import com.ramcosta.composedestinations.spec.Route

@Destination<RootNavGraph>(
    start = true,
)
@Composable
fun MainScreen(
    rootNavController: NavController,
) {
    val viewModel: MainViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bottomBarController = LocalBottomBarController.current
    val bottomBarState by bottomBarController.state

    val engine = rememberNavHostEngine()
    val navController = engine.rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination

    Box {
        DestinationsNavHost(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (bottomBarState.isRail && bottomBarState.visible) {
                        bottomBarState.size.width.toDp()
                    } else {
                        0.dp
                    },
                ),
            engine = engine,
            navController = navController,
            navGraph = NavGraphs.main,
            dependenciesContainerBuilder = {
                dependency(RootNavControllerWrapper(rootNavController))
            },
        )
        if (uiState.globalErrors.isNotEmpty()) {
            GlobalErrorsColumn(
                modifier = Modifier
                    .windowInsetsPadding(topWindowInsets)
                    .padding(
                        start = if (bottomBarState.isRail) {
                            bottomBarState.size.width.toDp()
                        } else {
                            0.dp
                        },
                    ),
                globalErrors = uiState.globalErrors,
                onFixWallHavenApiKeyClick = {
                    navController.navigate(WallhavenApiKeyDialogDestination.route)
                },
                onDismiss = viewModel::dismissGlobalError,
            )
        }
        if (bottomBarState.isRail) {
            NavRail(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.TopStart)
                    .onSizeChanged {
                        bottomBarController.update { state ->
                            state.copy(size = it)
                        }
                    },
                currentDestination = currentDestination,
                showLocalTab = uiState.showLocalTab,
                onItemClick = { onBottomBarItemClick(navController, it) },
            )
        } else {
            BottomBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .onSizeChanged {
                        bottomBarController.update { state ->
                            state.copy(size = it)
                        }
                    },
                currentDestination = currentDestination,
                showLocalTab = uiState.showLocalTab,
                onItemClick = { onBottomBarItemClick(navController, it) },
            )
        }
    }
}

private fun onBottomBarItemClick(
    navController: NavController,
    route: Route,
) {
    navController.navigate(route.route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

data class RootNavControllerWrapper(
    val navController: NavController,
)
