package com.ammar.wallflow.ui.screens.more

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ammar.wallflow.ui.animations.materialFadeThroughIn
import com.ammar.wallflow.ui.animations.materialFadeThroughOut
import com.ammar.wallflow.ui.common.LocalSystemController
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
import com.ammar.wallflow.ui.common.mainsearch.LocalMainSearchBarController
import com.ammar.wallflow.ui.common.topWindowInsets
import com.ammar.wallflow.ui.navigation.NavGraphs
import com.ammar.wallflow.ui.screens.NavGraph
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.rememberNavHostEngine
import com.ramcosta.composedestinations.utils.startDestination

@Destination
@Composable
fun MoreScreen(
    navController: NavController,
) {
    val searchBarController = LocalMainSearchBarController.current
    val bottomBarController = LocalBottomBarController.current
    val systemController = LocalSystemController.current
    val systemState by systemController.state
    val detailNavController = rememberNavController()
    val navBackStackEntry by detailNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val activeOption by remember(currentDestination) {
        derivedStateOf {
            MoreRootDestination.entries.find { dest ->
                currentDestination?.hierarchy?.any {
                    it.route == dest.graph.route
                } == true
            }?.activeOption
        }
    }
    val currentIsStartDestination by remember(currentDestination) {
        derivedStateOf {
            MoreRootDestination.entries.any { dest ->
                dest.graph.startDestination.route == currentDestination?.route
            }
        }
    }

    val navHostEngine = rememberNavHostEngine(
        rootDefaultAnimations = RootNavGraphDefaultAnimations(
            enterTransition = {
                val initialGraphRoute = initialState.destination.parent?.route
                val targetGraphRoute = targetState.destination.parent?.route
                if (initialGraphRoute == targetGraphRoute) {
                    slideIntoContainer(towards = SlideDirection.Left)
                } else {
                    materialFadeThroughIn()
                }
            },
            exitTransition = {
                val initialGraphRoute = initialState.destination.parent?.route
                val targetGraphRoute = targetState.destination.parent?.route
                if (initialGraphRoute == targetGraphRoute) {
                    ExitTransition.None
                } else {
                    materialFadeThroughOut()
                }
            },
            popEnterTransition = {
                val initialGraphRoute = initialState.destination.parent?.route
                val targetGraphRoute = targetState.destination.parent?.route
                if (initialGraphRoute == targetGraphRoute) {
                    EnterTransition.None
                } else {
                    materialFadeThroughIn()
                }
            },
            popExitTransition = {
                val initialGraphRoute = initialState.destination.parent?.route
                val targetGraphRoute = targetState.destination.parent?.route
                if (initialGraphRoute == targetGraphRoute) {
                    slideOutOfContainer(towards = SlideDirection.Right)
                } else {
                    materialFadeThroughOut()
                }
            },
        ),
    )

    val moreNavigate: (NavGraph) -> Unit = remember(
        detailNavController,
        navController,
        systemState.isExpanded,
    ) {
        {
            if (systemState.isExpanded) {
                detailNavController.navigateOrPop(it)
            } else {
                navController.navigate(it)
            }
        }
    }

    LaunchedEffect(Unit) {
        searchBarController.update { it.copy(visible = false) }
        bottomBarController.update { it.copy(visible = true) }
    }

    MoreScreenContent(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(topWindowInsets),
        isExpanded = systemState.isExpanded,
        activeOption = activeOption,
        detailContent = {
            DestinationsNavHost(
                modifier = Modifier.fillMaxSize(),
                engine = navHostEngine,
                navController = detailNavController,
                navGraph = NavGraphs.moreDetail,
            )
        },
        onSettingsClick = { moreNavigate(NavGraphs.settings) },
        onBackupRestoreClick = { moreNavigate(NavGraphs.backup_restore) },
        onOpenSourceLicensesClick = { moreNavigate(NavGraphs.openSourceLicenses) },
    )

    BackHandler(
        enabled = systemState.isExpanded,
        onBack = {
            if (currentIsStartDestination) {
                navController.popBackStack()
            } else {
                detailNavController.popBackStack()
            }
        },
    )
}

fun NavController.navigateOrPop(
    navGraph: NavGraph,
) {
    val shouldPop = navGraph.route == currentDestination?.parent?.route &&
        navGraph.startDestination.route != currentDestination?.route
    if (shouldPop) {
        popBackStack()
        return
    }
    navigate(navGraph) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
