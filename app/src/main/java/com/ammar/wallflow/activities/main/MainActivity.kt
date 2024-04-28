package com.ammar.wallflow.activities.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.ammar.wallflow.MainDispatcher
import com.ammar.wallflow.data.preferences.Theme
import com.ammar.wallflow.destinations.WallpaperScreenDestination
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.navigation.MainNavigation
import com.ammar.wallflow.ui.common.DefaultSystemController
import com.ammar.wallflow.ui.common.LocalSystemController
import com.ammar.wallflow.ui.common.SystemState
import com.ammar.wallflow.ui.common.bottombar.BottomBarState
import com.ammar.wallflow.ui.common.bottombar.DefaultBottomBarController
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
import com.ammar.wallflow.ui.theme.EdgeToEdge
import com.ammar.wallflow.ui.theme.WallFlowTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val wallhavenUriPattern = Regex(
        "https?://(?:whvn|wallhaven).cc(?:/w)?/(?<wallpaperId>\\S+)",
        RegexOption.IGNORE_CASE,
    )
    private lateinit var navController: NavHostController
    private var consumedIntent = false

    @Inject
    @MainDispatcher
    lateinit var mainDispatcher: CoroutineDispatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            consumedIntent = savedInstanceState.getBoolean(
                SAVED_INSTANCE_STATE_CONSUMED_INTENT,
                false,
            )
        }

        // Fix leak for Android Q
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            onBackPressedDispatcher.addCallback(
                this,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        if (isTaskRoot) {
                            finishAfterTransition()
                        }
                    }
                },
            )
        }

        setContent {
            CompositionLocalProvider(
                LocalSystemController provides DefaultSystemController(SystemState()),
                LocalBottomBarController provides DefaultBottomBarController(BottomBarState()),
            ) {
                Content()
            }
        }
    }

    @OptIn(
        ExperimentalMaterial3WindowSizeClassApi::class,
        ExperimentalComposeUiApi::class,
    )
    @Composable
    private fun Content() {
        val windowSizeClass = calculateWindowSizeClass(this)
        val isMedium = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium
        val isExpanded = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Expanded

        navController = rememberNavController()
        val viewModel: MainActivityViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val systemController = LocalSystemController.current
        val systemState by systemController.state
        val bottomBarController = LocalBottomBarController.current

        LaunchedEffect(Unit) {
            withContext(mainDispatcher) {
                handleIntent(intent ?: return@withContext)
            }
        }

        LaunchedEffect(isMedium) {
            bottomBarController.update { it.copy(isRail = isMedium) }
        }

        LaunchedEffect(isExpanded, isMedium) {
            systemController.update {
                it.copy(
                    isMedium = isMedium,
                    isExpanded = isExpanded,
                )
            }
        }

        val systemInDarkTheme = isSystemInDarkTheme()
        val darkTheme = remember(uiState.theme, systemInDarkTheme) {
            when (uiState.theme) {
                Theme.SYSTEM -> systemInDarkTheme
                Theme.LIGHT -> false
                Theme.DARK -> true
            }
        }

        EdgeToEdge(
            darkTheme = darkTheme,
            statusBarColor = systemState.statusBarColor,
            navigationBarColor = systemState.navigationBarColor,
            statusBarVisible = systemState.statusBarVisible,
            navigationBarVisible = systemState.navigationBarVisible,
            isStatusBarLight = systemState.isStatusBarLight,
        )

        WallFlowTheme(darkTheme = darkTheme) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        systemController.update {
                            it.copy(size = size)
                        }
                    }
                    .semantics { testTagsAsResourceId = true },
                color = if (isMedium) {
                    MaterialTheme.colorScheme.surfaceContainer
                } else {
                    MaterialTheme.colorScheme.background
                },
            ) {
                MainNavigation(
                    navController = navController,
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SAVED_INSTANCE_STATE_CONSUMED_INTENT, true)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (consumedIntent) {
            return
        }
        val handled = navController.handleDeepLink(intent)
        if (handled) {
            return
        }
        val result = wallhavenUriPattern.matchEntire(intent.data?.toString() ?: "") ?: return
        val wallpaperId = result.groups["wallpaperId"]?.value ?: return
        navController.navigate(
            WallpaperScreenDestination(
                source = Source.WALLHAVEN,
                wallpaperId = wallpaperId,
                thumbData = null,
            ).route,
        )
    }

    companion object {
        private const val SAVED_INSTANCE_STATE_CONSUMED_INTENT =
            "SAVED_INSTANCE_STATE_CONSUMED_INTENT"
    }
}
