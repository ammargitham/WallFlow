package com.ammar.wallflow.ui.common.bottombar

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.ammar.wallflow.ui.screens.NavGraph

@OptIn(ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)
fun NavigationSuiteScope.navSuiteItems(
    currentDestination: NavDestination? = null,
    showLocalTab: Boolean = true,
    onItemClick: (destination: NavGraph) -> Unit = {},
) {
    BottomBarDestination.entries
        .filter {
            if (it != BottomBarDestination.Local) {
                true
            } else {
                showLocalTab
            }
        }
        .forEach { destination ->
            item(
                selected = currentDestination?.hierarchy?.any {
                    it.route == destination.graph.route
                } == true,
                onClick = { onItemClick(destination.graph) },
                icon = {
                    Icon(
                        painter = painterResource(destination.icon),
                        contentDescription = stringResource(destination.label),
                    )
                },
                label = { Text(stringResource(destination.label)) },
            )
        }
}
