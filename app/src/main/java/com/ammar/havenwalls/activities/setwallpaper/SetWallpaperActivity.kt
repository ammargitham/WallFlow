package com.ammar.havenwalls.activities.setwallpaper

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.MutableCreationExtras
import com.ammar.havenwalls.IoDispatcher
import com.ammar.havenwalls.data.repository.AppPreferencesRepository
import com.ammar.havenwalls.data.repository.ObjectDetectionModelRepository
import com.ammar.havenwalls.extensions.getParcelExtra
import com.ammar.havenwalls.extensions.toast
import com.ammar.havenwalls.ui.common.LocalSystemBarsController
import com.ammar.havenwalls.ui.crop.CropScreen
import com.ammar.havenwalls.ui.crop.CropViewModel
import com.ammar.havenwalls.ui.crop.Result
import com.ammar.havenwalls.ui.theme.HavenWallsTheme
import com.ammar.havenwalls.utils.DownloadManager
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

        val uri = intent.getParcelExtra(EXTRA_URI, Uri::class.java)
        if (uri == null) {
            toast("Invalid URI!")
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
                    if (it.result == Result.CANCELLED) {
                        finishAndRemoveTask()
                        return@collectLatest
                    }
                }
            }
        }

        setContent {
            val systemBarsController = LocalSystemBarsController.current
            val systemBarsState by systemBarsController.state

            HavenWallsTheme(
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

    companion object {
        const val EXTRA_URI = "uri"
    }
}
