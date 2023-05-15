package com.ammar.havenwalls.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.ammar.havenwalls.R
import com.ammar.havenwalls.ui.NavGraphs
import com.ammar.havenwalls.ui.appCurrentDestinationAsState
import com.ammar.havenwalls.ui.common.bottombar.BottomBarDestination
import com.ammar.havenwalls.ui.startAppDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    title: @Composable () -> Unit = {},
    visible: Boolean = true,
    gradientBg: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val currentDestination = navController.appCurrentDestinationAsState().value
        ?: NavGraphs.root.startAppDestination
    val rootDestinations = remember {
        BottomBarDestination.values().map { it.direction.route }
    }
    val showBackButton = remember(currentDestination, rootDestinations) {
        currentDestination.route !in rootDestinations
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(),
        exit = slideOutVertically(),
    ) {
        TopAppBar(
            modifier = modifier.run {
                if (gradientBg) {
                    background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black,
                                Color.Transparent,
                            )
                        )
                    )
                } else {
                    this
                }
            },
            windowInsets = windowInsets,
            navigationIcon = {
                AnimatedVisibility(
                    visible = showBackButton,
                    enter = slideInHorizontally(),
                    exit = slideOutHorizontally(),
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                }
            },
            title = title,
            // {
            //     AnimatedVisibility(
            //         visible = titleVisible,
            //         enter = fadeIn(),
            //         exit = fadeOut(),
            //     ) {
            //         Text(
            //             text = stringResource(R.string.app_name),
            //             maxLines = 1,
            //             overflow = TextOverflow.Ellipsis,
            //         )
            //     }
            // },
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = if (gradientBg) Color.Transparent else MaterialTheme.colorScheme.surface,
            ),
            // actions = {
            //     if (showActions) {
            //         OverflowMenu { closeMenu ->
            //             DropdownMenuItem(
            //                 text = { Text(stringResource(R.string.settings)) },
            //                 onClick = {
            //                     navController.navigate(SettingsScreenDestination) {
            //                         launchSingleTop = true
            //                     }
            //                     closeMenu()
            //                 },
            //             )
            //         }
            //     }
            // }
            actions = actions,
        )
    }
}
