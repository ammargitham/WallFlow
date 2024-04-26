package com.ammar.wallflow.activities.setwallpaper

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.ammar.wallflow.ui.common.DefaultSystemController
import com.ammar.wallflow.ui.common.LocalSystemController
import com.ammar.wallflow.ui.common.SystemState
import com.ammar.wallflow.ui.screens.crop.CropScreen
import com.ammar.wallflow.ui.screens.crop.CropViewModel
import com.ammar.wallflow.ui.screens.crop.Result
import com.ammar.wallflow.ui.theme.EdgeToEdge
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

        onBackPressedDispatcher.addCallback(
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finishAndRemoveTask()
                }
            },
        )

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
            },
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
            CompositionLocalProvider(
                LocalSystemController provides DefaultSystemController(SystemState()),
            ) {
                Content(viewModel)
            }
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Composable
    private fun Content(viewModel: CropViewModel) {
        val windowSizeClass = calculateWindowSizeClass(this)
        val isExpanded = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Expanded
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val systemController = LocalSystemController.current
        val systemState by systemController.state
        val darkTheme = when (uiState.theme) {
            Theme.SYSTEM -> isSystemInDarkTheme()
            Theme.LIGHT -> false
            Theme.DARK -> true
        }
        LaunchedEffect(isExpanded) {
            systemController.update { it.copy(isExpanded = isExpanded) }
        }
        EdgeToEdge(
            darkTheme = darkTheme,
            statusBarVisible = systemState.statusBarVisible,
            statusBarColor = systemState.statusBarColor,
            navigationBarVisible = systemState.navigationBarVisible,
            navigationBarColor = systemState.navigationBarColor,
        )
        WallFlowTheme(darkTheme = darkTheme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black,
            ) {
                CropScreen(
                    modifier = Modifier.windowInsetsPadding(
                        ScaffoldDefaults.contentWindowInsets,
                    ),
                    viewModel = viewModel,
                )
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
