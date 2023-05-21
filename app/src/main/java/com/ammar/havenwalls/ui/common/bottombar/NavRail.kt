package com.ammar.havenwalls.ui.common.bottombar

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
import androidx.compose.ui.res.stringResource
import com.ammar.havenwalls.ui.destinations.TypedDestination

@Composable
fun NavRail(
    modifier: Modifier = Modifier,
    currentDestination: TypedDestination<*>? = null,
    onItemClick: (destination: TypedDestination<*>) -> Unit = {},
) {
    val bottomBarController = LocalBottomBarController.current
    val state by bottomBarController.state

    AnimatedVisibility(
        modifier = modifier,
        visible = state.visible,
        enter = slideInHorizontally(initialOffsetX = { -it }),
        exit = slideOutHorizontally(targetOffsetX = { -it })
    ) {
        NavigationRail(
            modifier = modifier,
        ) {
            Spacer(Modifier.weight(1f))
            BottomBarDestination.values().forEach { destination ->
                NavigationRailItem(
                    icon = {
                        Icon(
                            destination.icon,
                            contentDescription = stringResource(destination.label)
                        )
                    },
                    label = { Text(stringResource(destination.label)) },
                    selected = currentDestination == destination.direction,
                    onClick = { onItemClick(destination.direction) },
                )
            }
            Spacer(Modifier.weight(1f))
        }
    }
}
