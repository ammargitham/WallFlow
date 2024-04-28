package com.ammar.wallflow.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ammar.wallflow.extensions.findActivity
import com.ammar.wallflow.extensions.toDp
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
import com.google.accompanist.adaptive.FoldAwareConfiguration
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import com.google.accompanist.adaptive.TwoPane
import com.google.accompanist.adaptive.calculateDisplayFeatures
import kotlin.math.roundToInt

@Composable
fun BottomBarAwareHorizontalTwoPane(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    splitFraction: Float = 0.5f,
) {
    val context = LocalContext.current
    val systemController = LocalSystemController.current
    val bottomBarController = LocalBottomBarController.current
    val systemState by systemController.state
    val bottomBarState by bottomBarController.state
    val feedWidth = (
        (systemState.size.width * splitFraction).roundToInt() - bottomBarState.size.width
        ).toDp()

    TwoPane(
        modifier = modifier,
        first = first,
        second = second,
        strategy = HorizontalTwoPaneStrategy(
            splitOffset = feedWidth,
            gapWidth = 24.dp,
        ),
        displayFeatures = calculateDisplayFeatures(context.findActivity()),
        foldAwareConfiguration = FoldAwareConfiguration.VerticalFoldsOnly,
    )
}
