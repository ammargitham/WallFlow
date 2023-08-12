/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ammar.wallflow.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
     */
)

@Composable
fun WallFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    statusBarVisible: Boolean = true,
    statusBarColor: Color = Color.Unspecified,
    navigationBarVisible: Boolean = false,
    navigationBarColor: Color = Color.Unspecified,
    lightStatusBars: Boolean? = null,
    lightNavigationBars: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        val currentWindow = (view.context as? Activity)?.window
            ?: throw Exception("Not in an activity - unable to get Window reference")

        val systemBarColor = colorScheme.surface
        val animatedStatusBarColor by animateColorAsState(
            statusBarColor.takeOrElse { systemBarColor },
            label = "statusBarColor",
        )
        val animatedNavigationBarColor by animateColorAsState(
            navigationBarColor.takeOrElse { systemBarColor },
            label = "navigationBarColor",
        )

        LaunchedEffect(
            animatedStatusBarColor,
            animatedNavigationBarColor,
            darkTheme,
            statusBarVisible,
        ) {
            currentWindow.statusBarColor = animatedStatusBarColor.toArgb()
            currentWindow.navigationBarColor = animatedNavigationBarColor.toArgb()
            val insetsController = WindowCompat.getInsetsController(currentWindow, view)
            insetsController.apply {
                isAppearanceLightStatusBars = lightStatusBars ?: !darkTheme
                isAppearanceLightNavigationBars = lightNavigationBars ?: !darkTheme
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
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
