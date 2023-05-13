package com.ammar.havenwalls.activities.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.ammar.havenwalls.ui.NavGraphs
import com.ammar.havenwalls.ui.appCurrentDestinationAsState
import com.ammar.havenwalls.ui.common.LocalSystemBarsController
import com.ammar.havenwalls.ui.common.bottombar.BottomBarDestination
import com.ammar.havenwalls.ui.common.bottombar.LocalBottomBarController
import com.ammar.havenwalls.ui.destinations.WallhavenApiKeyDialogDestination
import com.ammar.havenwalls.ui.startAppDestination
import com.ammar.havenwalls.ui.theme.HavenWallsTheme
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.utils.startDestination
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(
        ExperimentalAnimationApi::class,
        ExperimentalMaterial3WindowSizeClassApi::class,
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val navController = rememberAnimatedNavController()
            // val topAppBarState = rememberTopAppBarState()
            // val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            //     topAppBarState,
            // )
            // val fabController = rememberFABController()
            // val currentBackStackEntry by navController.currentBackStackEntryAsState()
            // LaunchedEffect(currentBackStackEntry) {
            //     // reveal TopAppBar on changing screens
            //     scrollBehavior.state.heightOffset = 0f
            // }
            val viewModel: MainActivityViewModel = hiltViewModel()
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            val uiState by produceState(
                initialValue = MainUiState(),
                key1 = lifecycle,
                key2 = viewModel
            ) {
                lifecycle.repeatOnLifecycle(state = Lifecycle.State.STARTED) {
                    viewModel.uiState.collect { value = it }
                }
            }
            val currentDestination = navController.appCurrentDestinationAsState().value
                ?: NavGraphs.root.startAppDestination
            val rootDestinations = remember {
                BottomBarDestination.values().map { it.direction.startDestination }
            }
            val showBackButton = remember(currentDestination, rootDestinations) {
                currentDestination !in rootDestinations
            }
            val systemBarsController = LocalSystemBarsController.current
            val systemBarsState by systemBarsController.state
            val bottomBarController = LocalBottomBarController.current
            val windowSizeClass = calculateWindowSizeClass(this)
            val useNavRail = windowSizeClass.widthSizeClass > WindowWidthSizeClass.Compact

            HavenWallsTheme(
                statusBarVisible = systemBarsState.statusBarVisible,
                statusBarColor = systemBarsState.statusBarColor,
                navigationBarVisible = systemBarsState.navigationBarVisible,
                navigationBarColor = systemBarsState.navigationBarColor,
                lightStatusBars = systemBarsState.lightStatusBars,
                lightNavigationBars = systemBarsState.lightNavigationBars,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainActivityContent(
                        currentDestination = currentDestination,
                        showBackButton = showBackButton,
                        globalErrors = uiState.globalErrors,
                        onBackClick = { navController.navigateUp() },
                        onFixWallHavenApiKeyClick = {
                            navController.navigate(WallhavenApiKeyDialogDestination)
                        },
                        onDismissGlobalError = viewModel::dismissGlobalError,
                        onBottomBarSizeChanged = { size ->
                            bottomBarController.update { it.copy(size = size) }
                        },
                        onBottomBarItemClick = {
                            navController.navigate(it) {
                                launchSingleTop = true
                            }
                        }
                    ) {
                        MainNavigation(
                            navController = navController,
                            contentPadding = it,
                            mainActivityViewModel = viewModel,
                            applyContentPadding = uiState.applyScaffoldPadding,
                        )
                    }
                }
            }
        }
    }
}
