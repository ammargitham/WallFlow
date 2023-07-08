package com.ammar.wallflow.activities.setwallpaper

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.MutableCreationExtras
import com.ammar.wallflow.IoDispatcher
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.Theme
import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.ObjectDetectionModelRepository
import com.ammar.wallflow.extensions.getParcelExtra
import com.ammar.wallflow.extensions.toast
import com.ammar.wallflow.ui.common.LocalSystemBarsController
import com.ammar.wallflow.ui.crop.CropScreen
import com.ammar.wallflow.ui.crop.CropViewModel
import com.ammar.wallflow.ui.crop.Result
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ammar.wallflow.utils.DownloadManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SetWallpaperActivity : ComponentActivity() {

    @Inject
    lateinit var appPreferencesRepository: AppPreferencesRepository

    @Inject
    lateinit var objectDetectionModelRepository: ObjectDetectionModelRepository

    @Inject
    lateinit var downloadManager: DownloadManager

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAndRemoveTask()
            }
        })

        val uri = getUri()
        if (uri == null) {
            toast(getString(R.string.invalid_uri))
            finishAndRemoveTask()
            return
        }

        val viewModel by viewModels<CropViewModel>(
            extrasProducer = {
                MutableCreationExtras(defaultViewModelCreationExtras).apply {
                    set(DEFAULT_ARGS_KEY, intent.extras ?: Bundle())
                }
            },
            factoryProducer = {
                CropViewModel.getFactory(
                    uri = uri,
                    appPreferencesRepository = appPreferencesRepository,
                    objectDetectionModelRepository = objectDetectionModelRepository,
                    downloadManager = downloadManager,
                    ioDispatcher = ioDispatcher,
                )
            }
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest {
                    if (it.result == Result.Cancelled || it.result is Result.Success) {
                        finishAndRemoveTask()
                        return@collectLatest
                    }
                }
            }
        }

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val systemBarsController = LocalSystemBarsController.current
            val systemBarsState by systemBarsController.state

            WallFlowTheme(
                darkTheme = when (uiState.theme) {
                    Theme.SYSTEM -> isSystemInDarkTheme()
                    Theme.LIGHT -> false
                    Theme.DARK -> true
                },
                statusBarVisible = systemBarsState.statusBarVisible,
                statusBarColor = systemBarsState.statusBarColor,
                navigationBarVisible = systemBarsState.navigationBarVisible,
                navigationBarColor = systemBarsState.navigationBarColor,
                lightStatusBars = systemBarsState.lightStatusBars,
                lightNavigationBars = systemBarsState.lightNavigationBars
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CropScreen(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
                        viewModel = viewModel,
                    )
                }
            }
        }
    }

    private fun getUri() = if (intent?.action == Intent.ACTION_SEND) {
        if (intent.type?.startsWith("image/") == true) {
            intent.getParcelExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            null
        }
    } else {
        intent.getParcelExtra(EXTRA_URI, Uri::class.java)
    }

    companion object {
        const val EXTRA_URI = "uri"
    }
}
