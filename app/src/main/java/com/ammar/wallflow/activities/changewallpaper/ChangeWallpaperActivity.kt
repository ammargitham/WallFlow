package com.ammar.wallflow.activities.changewallpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ammar.wallflow.data.preferences.Theme
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.Status.Pending
import com.ammar.wallflow.workers.AutoWallpaperWorker.Companion.Status.Running
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class ChangeWallpaperActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel = hiltViewModel<ChangeWallpaperViewModel>()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(uiState.hasSources) {
                if (!uiState.hasSources) {
                    return@LaunchedEffect
                }
                delay(1500)
                viewModel.changeNow()
            }

            LaunchedEffect(uiState.autoWallpaperStatus) {
                val autoWallpaperStatus = uiState.autoWallpaperStatus ?: return@LaunchedEffect
                if (!autoWallpaperStatus.isSuccessOrFail()) {
                    return@LaunchedEffect
                }
                finishAndRemoveTask()
            }

            WallFlowTheme(
                darkTheme = when (uiState.theme) {
                    Theme.SYSTEM -> isSystemInDarkTheme()
                    Theme.LIGHT -> false
                    Theme.DARK -> true
                },
            ) {
                if (!uiState.hasSources) {
                    NoSourcesDialog(
                        onDismissRequest = {
                            finishAndRemoveTask()
                        },
                    )
                }
                if (uiState.autoWallpaperStatus == null ||
                    uiState.autoWallpaperStatus is Pending ||
                    uiState.autoWallpaperStatus is Running
                ) {
                    ChangingWallpaperDialog(
                        onDismissRequest = {},
                    )
                }
            }
        }
    }
}
