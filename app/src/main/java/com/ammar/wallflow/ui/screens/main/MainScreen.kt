package com.ammar.wallflow.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ammar.wallflow.NavGraphs
import com.ammar.wallflow.destinations.WallhavenApiKeyDialogDestination
import com.ammar.wallflow.navigation.AppNavGraphs.RootNavGraph
import com.ammar.wallflow.ui.common.WallflowNavigationSuite
import com.ammar.wallflow.ui.common.bottombar.BottomBarDestination
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
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
    val engine = rememberNavHostEngine()
    val navController = engine.rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(
        currentWindowAdaptiveInfo(),
    )
    val currentLayoutType = remember(bottomBarController.state.value.visible) {
        if (bottomBarController.state.value.visible) {
            layoutType
        } else {
            NavigationSuiteType.None
        }
    }

    NavigationSuiteScaffoldLayout(
        navigationSuite = {
            WallflowNavigationSuite(
                modifier = Modifier.onSizeChanged {
                    bottomBarController.update { state ->
                        state.copy(size = it)
                    }
                },
                layoutType = currentLayoutType,
                colors = NavigationSuiteDefaults.colors(
                    navigationRailContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                content = {
                    BottomBarDestination.entries
                        .filter {
                            if (it != BottomBarDestination.Local) {
                                true
                            } else {
                                uiState.showLocalTab
                            }
                        }
                        .forEach { destination ->
                            item(
                                selected = currentDestination?.hierarchy?.any {
                                    it.route == destination.graph.route
                                } == true,
                                onClick = {
                                    onBottomBarItemClick(
                                        navController = navController,
                                        route = destination.graph,
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(destination.icon),
                                        contentDescription = stringResource(destination.label),
                                    )
                                },
                                label = { Text(stringResource(destination.label)) },
                            )
                        }
                },
            )
        },
        layoutType = currentLayoutType,
    ) {
        Box {
            DestinationsNavHost(
                engine = engine,
                navController = navController,
                navGraph = NavGraphs.main,
                dependenciesContainerBuilder = {
                    dependency(RootNavControllerWrapper(rootNavController))
                },
            )
            if (uiState.globalErrors.isNotEmpty()) {
                GlobalErrorsColumn(
                    modifier = Modifier.windowInsetsPadding(topWindowInsets),
                    globalErrors = uiState.globalErrors,
                    onFixWallHavenApiKeyClick = {
                        navController.navigate(WallhavenApiKeyDialogDestination.route)
                    },
                    onDismiss = viewModel::dismissGlobalError,
                )
            }
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
