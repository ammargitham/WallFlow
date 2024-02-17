package com.ammar.wallflow.ui.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

@Composable
fun EdgeToEdge(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    statusBarVisible: Boolean = true,
    statusBarColor: Color = Color.Unspecified,
    navigationBarVisible: Boolean = false,
    navigationBarColor: Color = Color.Unspecified,
    isStatusBarLight: Boolean? = null,
) {
    val view = LocalView.current
    if (view.isInEditMode) {
        return
    }
    val activity = view.context as? ComponentActivity ?: return
    val currentWindow = activity.window
    val colorScheme = rememberColorScheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
    )
    LaunchedEffect(
        darkTheme,
        statusBarColor,
        navigationBarColor,
    ) {
        val statusBarScrimColor = statusBarColor.takeOrElse {
            colorScheme.surface
        }.toArgb()
        val navBarScrimColor = navigationBarColor.takeOrElse {
            if (!darkTheme && Build.VERSION.SDK_INT <= 26) {
                // below API 26 nav bar buttons are always white
                colorScheme.surfaceDim
            } else {
                colorScheme.surfaceColorAtElevation(3.dp)
            }
        }.toArgb()
        activity.enableEdgeToEdge(
            statusBarStyle = if (darkTheme) {
                SystemBarStyle.dark(
                    scrim = statusBarScrimColor,
                )
            } else {
                SystemBarStyle.light(
                    scrim = statusBarScrimColor,
                    darkScrim = statusBarScrimColor,
                )
            },
            navigationBarStyle = if (darkTheme) {
                SystemBarStyle.dark(
                    scrim = navBarScrimColor,
                )
            } else {
                SystemBarStyle.light(
                    scrim = navBarScrimColor,
                    darkScrim = navBarScrimColor,
                )
            },
        )
    }

    LaunchedEffect(
        statusBarVisible,
        navigationBarVisible,
    ) {
        val window = currentWindow ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.run {
            if (statusBarVisible) {
                show(WindowInsetsCompat.Type.statusBars())
            } else {
                hide(WindowInsetsCompat.Type.statusBars())
            }
            if (navigationBarVisible) {
                show(WindowInsetsCompat.Type.navigationBars())
            } else {
                hide(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }

    LaunchedEffect(
        isStatusBarLight,
    ) {
        val window = currentWindow ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.run {
            isAppearanceLightStatusBars = isStatusBarLight ?: !darkTheme
        }
    }
}
