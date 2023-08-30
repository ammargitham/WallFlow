package com.ammar.wallflow.ui.common.bottombar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
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
import com.ammar.wallflow.ui.screens.NavGraph

@Composable
fun NavRail(
    modifier: Modifier = Modifier,
    currentDestination: NavDestination? = null,
    onItemClick: (destination: NavGraph) -> Unit = {},
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
        ) {
            Spacer(Modifier.weight(1f))
            BottomBarDestination.entries.forEach { destination ->
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
