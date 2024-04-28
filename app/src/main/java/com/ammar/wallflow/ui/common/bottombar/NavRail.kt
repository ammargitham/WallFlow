package com.ammar.wallflow.ui.common.bottombar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
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
fun NavRail(
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
        enter = slideInHorizontally(initialOffsetX = { -it }),
        exit = slideOutHorizontally(targetOffsetX = { -it }),
    ) {
        NavigationRail(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Spacer(Modifier.weight(1f))
            BottomBarDestination.entries
                .filter {
                    if (it != BottomBarDestination.Local) {
                        true
                    } else {
                        showLocalTab
                    }
                }
                .forEach { destination ->
                    NavigationRailItem(
                        icon = {
                            Icon(
                                painter = painterResource(destination.icon),
                                contentDescription = stringResource(destination.label),
                            )
                        },
                        label = { Text(stringResource(destination.label)) },
                        selected = currentDestination?.hierarchy?.any {
                            it.route == destination.graph.route
                        } == true,
                        onClick = { onItemClick(destination.graph) },
                    )
                }
            Spacer(Modifier.weight(1f))
        }
    }
}
