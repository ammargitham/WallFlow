package com.ammar.havenwalls.ui.common.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import com.ammar.havenwalls.ui.common.navigation.TwoPaneNavigation.Mode
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.spec.Direction

object TwoPaneNavigation {
    class Controller constructor(
        initialPaneMode: Mode = Mode.TWO_PANE,
        val pane1NavHostController: NavHostController,
        val pane2NavHostController: NavHostController,
    ) {
        private val paneModeState = mutableStateOf(initialPaneMode)
        private var _paneMode by paneModeState
        val paneMode: State<Mode> = paneModeState

        fun navigate(
            direction: Direction,
            navOptionsBuilder: NavOptionsBuilder.() -> Unit = {},
        ) = navigatePane1(direction, navOptionsBuilder)

        fun navigatePane1(
            direction: Direction,
            navOptionsBuilder: NavOptionsBuilder.() -> Unit = {},
        ) = pane1NavHostController.navigate(direction, navOptionsBuilder)

        fun navigatePane2(
            direction: Direction,
            navOptionsBuilder: NavOptionsBuilder.() -> Unit = {},
        ) = when (_paneMode) {
            Mode.SINGLE_PANE -> pane1NavHostController.navigate(direction, navOptionsBuilder)
            Mode.TWO_PANE -> pane2NavHostController.navigate(direction, navOptionsBuilder)
        }

        fun setPaneMode(paneMode: Mode) {
            _paneMode = paneMode
        }
    }

    enum class Mode {
        SINGLE_PANE,
        TWO_PANE,
    }

    sealed class PaneSide {
        object Pane1 : PaneSide()
        object Pane2 : PaneSide()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun rememberTwoPaneNavController(
    initialPaneMode: Mode = Mode.SINGLE_PANE,
): TwoPaneNavigation.Controller {
    val pane1NavController = rememberAnimatedNavController()
    val pane2NavController = rememberAnimatedNavController()
    return remember {
        TwoPaneNavigation.Controller(
            initialPaneMode = initialPaneMode,
            pane1NavHostController = pane1NavController,
            pane2NavHostController = pane2NavController,
        )
    }
}
