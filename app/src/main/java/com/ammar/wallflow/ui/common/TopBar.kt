package com.ammar.wallflow.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.ammar.wallflow.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    showBackButton: Boolean = false,
    title: @Composable () -> Unit = {},
    visible: Boolean = true,
    gradientBg: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopBar(
        modifier = modifier,
        windowInsets = windowInsets,
        scrollBehavior = scrollBehavior,
        showBackButton = showBackButton,
        title = title,
        visible = visible,
        gradientBg = gradientBg,
        actions = actions,
        onBackClick = { navController.popBackStack() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    showBackButton: Boolean = false,
    title: @Composable () -> Unit = {},
    backIcon: (@Composable () -> Unit)? = null,
    visible: Boolean = true,
    gradientBg: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    onBackClick: () -> Unit = {},
) {
    TopBar(
        modifier = modifier,
        windowInsets = windowInsets,
        scrollBehavior = scrollBehavior,
        showBackButton = showBackButton,
        title = title,
        backIcon = backIcon ?: {
            BackIcon(
                onClick = onBackClick,
            )
        },
        visible = visible,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (gradientBg) {
                BottomAppBarDefaults.containerColor.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.surface
            },
            actionIconContentColor = if (gradientBg) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ),
        actions = actions,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    showBackButton: Boolean = false,
    title: @Composable () -> Unit = {},
    visible: Boolean = true,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    actions: @Composable RowScope.() -> Unit = {},
    onBackClick: () -> Unit = {},
) {
    TopBar(
        modifier = modifier,
        windowInsets = windowInsets,
        scrollBehavior = scrollBehavior,
        showBackButton = showBackButton,
        title = title,
        backIcon = { BackIcon(onBackClick) },
        visible = visible,
        colors = colors,
        actions = actions,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    showBackButton: Boolean = false,
    title: @Composable () -> Unit = {},
    backIcon: @Composable () -> Unit = {},
    visible: Boolean = true,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    actions: @Composable RowScope.() -> Unit = {},
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(),
        exit = slideOutVertically(),
    ) {
        TopAppBar(
            modifier = modifier,
            windowInsets = windowInsets,
            navigationIcon = {
                AnimatedVisibility(
                    visible = showBackButton,
                    enter = slideInHorizontally(),
                    exit = slideOutHorizontally(),
                ) {
                    backIcon()
                }
            },
            title = title,
            scrollBehavior = scrollBehavior,
            colors = colors,
            actions = actions,
        )
    }
}

@Composable
fun BackIcon(
    onClick: () -> Unit = {},
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back),
        )
    }
}
