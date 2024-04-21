package com.ammar.wallflow.ui.common.bottombar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.ramcosta.composedestinations.spec.Route

@Composable
fun BottomBar(
    modifier: Modifier = Modifier,
    currentDestination: NavDestination? = null,
    showLocalTab: Boolean = true,
    onItemClick: (destination: Route) -> Unit = {},
) {
    val bottomBarController = LocalBottomBarController.current
    val state by bottomBarController.state

    AnimatedVisibility(
        modifier = modifier,
        visible = state.visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
    ) {
        NavigationBar {
            BottomBarDestination.entries
                .filter {
                    if (it != BottomBarDestination.Local) {
                        true
                    } else {
                        showLocalTab
                    }
                }
                .forEach { destination ->
                    NavigationBarItem(
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
    }
}
