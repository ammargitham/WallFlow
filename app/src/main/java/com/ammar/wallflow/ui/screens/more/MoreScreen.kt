package com.ammar.wallflow.ui.screens.more

import androidx.activity.compose.BackHandler
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
import com.ammar.wallflow.NavGraphs
import com.ammar.wallflow.navigation.AppNavGraphs
import com.ammar.wallflow.ui.common.LocalSystemController
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
import com.ammar.wallflow.ui.common.mainsearch.LocalMainSearchBarController
import com.ammar.wallflow.ui.common.topWindowInsets
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.rememberNavHostEngine
import com.ramcosta.composedestinations.spec.NavGraphSpec
import com.ramcosta.composedestinations.utils.startDestination

@Destination<AppNavGraphs.MoreNavGraph>(
    start = true,
)
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
                val route = getRoute(systemState.isExpanded, dest.activeOption)
                currentDestination?.hierarchy?.any {
                    it.route == route.route
                } == true
            }?.activeOption
        }
    }
    val currentIsStartDestination by remember(currentDestination) {
        derivedStateOf {
            MoreRootDestination.entries.any { dest ->
                val route = getRoute(systemState.isExpanded, dest.activeOption)
                route.startDestination.route == currentDestination?.route
            }
        }
    }

    val navHostEngine = rememberNavHostEngine()

    val moreNavigate: (ActiveOption) -> Unit = remember(
        detailNavController,
        navController,
        systemState.isExpanded,
    ) {
        {
            val route = getRoute(systemState.isExpanded, it)
            if (systemState.isExpanded) {
                detailNavController.navigateOrPop(route)
            } else {
                navController.navigate(route.route)
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
        onSettingsClick = { moreNavigate(ActiveOption.SETTINGS) },
        onBackupRestoreClick = { moreNavigate(ActiveOption.BACKUP_RESTORE) },
        onOpenSourceLicensesClick = { moreNavigate(ActiveOption.OSL) },
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

private fun getRoute(
    expanded: Boolean,
    option: ActiveOption,
): NavGraphSpec = if (expanded) {
    when (option) {
        ActiveOption.SETTINGS -> NavGraphs.settingsForMoreDetail
        ActiveOption.BACKUP_RESTORE -> NavGraphs.backupRestoreForMoreDetail
        ActiveOption.OSL -> NavGraphs.openSourceLicensesForMoreDetail
    }
} else {
    when (option) {
        ActiveOption.SETTINGS -> NavGraphs.settings
        ActiveOption.BACKUP_RESTORE -> NavGraphs.backupRestore
        ActiveOption.OSL -> NavGraphs.openSourceLicenses
    }
}

fun NavController.navigateOrPop(
    navGraph: NavGraphSpec,
) {
    val shouldPop = navGraph.route == currentDestination?.parent?.route &&
        navGraph.startDestination.route != currentDestination?.route
    if (shouldPop) {
        popBackStack()
        return
    }
    navigate(navGraph.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
