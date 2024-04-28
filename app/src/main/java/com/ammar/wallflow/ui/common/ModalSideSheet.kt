package com.ammar.wallflow.ui.common

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.view.Gravity
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.ammar.wallflow.ui.theme.WallFlowTheme
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// Lot of ideas from https://gist.github.com/sinasamaki/daa825d96235a18822177a2b1b323f49
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("RtlHardcoded")
@Composable
fun ModalSideSheet(
    modifier: Modifier = Modifier,
    state: SideSheetState = rememberSideSheetState(),
    onDismissRequest: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val targetValue by remember { derivedStateOf { state.targetValue } }
    val layoutDirection = LocalLayoutDirection.current
    var showAnimatedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(targetValue) {
        if (targetValue) {
            showAnimatedDialog = true
        }
    }

    if (showAnimatedDialog) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
            ),
        ) {
            val window = getDialogWindow()
            val predictiveBackProgress = remember { Animatable(initialValue = 0f) }
            var predictiveBackEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

            PredictiveBackHandler { progress: Flow<BackEventCompat> ->
                // code for gesture back started
                try {
                    progress.collect { backEvent ->
                        // code for progress
                        predictiveBackProgress.snapTo(backEvent.progress)
                        predictiveBackEdge = backEvent.swipeEdge
                    }
                    // code for completion
                    onDismissRequest()
                } catch (e: CancellationException) {
                    // code for cancellation
                    predictiveBackProgress.animateTo(0f)
                }
            }

            SideEffect {
                window?.apply {
                    setGravity(Gravity.RIGHT)
                    setDimAmount(0f)
                    setWindowAnimations(-1)
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = if (layoutDirection == LayoutDirection.Ltr) {
                    Alignment.CenterEnd
                } else {
                    Alignment.CenterStart
                },
            ) {
                val transitionState = remember { MutableTransitionState(false) }

                LaunchedEffect(targetValue) {
                    transitionState.targetState = targetValue
                }

                LaunchedEffect(transitionState.isIdle) {
                    if (transitionState.isIdle) {
                        state.done()
                    }
                }

                AnimatedVisibility(
                    visibleState = transitionState,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box(
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    onDismissRequest()
                                }
                            }
                            .background(Color.Black.copy(alpha = .56f))
                            .fillMaxSize(),
                    )
                }
                AnimatedVisibility(
                    visibleState = transitionState,
                    enter = slideInHorizontally { it } + fadeIn(),
                    exit = slideOutHorizontally { it } + fadeOut(),
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 400.dp)
                            .graphicsLayer {
                                val sheetHeight = size.height
                                if (!sheetHeight.isNaN() && sheetHeight != 0f) {
                                    val progress = predictiveBackProgress.value
                                    scaleX = calculatePredictiveBackScaleX(progress)
                                    scaleY = calculatePredictiveBackScaleY(progress)
                                    transformOrigin = TransformOrigin(
                                        if (predictiveBackEdge == BackEventCompat.EDGE_LEFT) {
                                            1f
                                        } else {
                                            0f
                                        },
                                        0.5f,
                                    )
                                }
                            }
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = BottomSheetDefaults.ContainerColor,
                            contentColor = contentColorFor(BottomSheetDefaults.ContainerColor),
                        ),
                    ) {
                        Box(modifier = modifier) {
                            Column { content() }
                        }
                    }

                    DisposableEffect(Unit) {
                        onDispose {
                            state.done()
                            showAnimatedDialog = false
                        }
                    }
                }
            }
        }
    }
}

class SideSheetState(
    initialVisible: Boolean = true,
) {
    private var doneCalled: CompletableJob? = null
    private var currentValue by mutableStateOf(initialVisible)
    var targetValue by mutableStateOf(initialVisible)

    val isVisible
        get() = currentValue

    suspend fun show() {
        // wait for doneCalled if not null
        if (doneCalled?.isActive == true) {
            doneCalled?.cancel()
        }
        // create a new job
        doneCalled = Job()
        targetValue = true
        coroutineScope {
            doneCalled?.join()
            currentValue = true
        }
    }

    suspend fun hide() {
        // wait for doneCalled if not null
        if (doneCalled?.isActive == true) {
            doneCalled?.cancel()
        }
        // create a new job
        doneCalled = Job()
        targetValue = false
        coroutineScope {
            doneCalled?.join()
            currentValue = false
        }
    }

    fun done() {
        doneCalled?.complete()
        doneCalled = null
    }
}

@Composable
fun rememberSideSheetState(): SideSheetState {
    return remember { SideSheetState() }
}

private val PredictiveBackMaxScaleXDistance = 48.dp
private fun GraphicsLayerScope.calculatePredictiveBackScaleX(progress: Float): Float {
    val width = size.width
    return if (width.isNaN() || width == 0f) {
        1f
    } else {
        1f - lerp(0f, min(PredictiveBackMaxScaleXDistance.toPx(), width), progress) / width
    }
}

private val PredictiveBackMaxScaleYDistance = 48.dp
private fun GraphicsLayerScope.calculatePredictiveBackScaleY(progress: Float): Float {
    val height = size.height
    return if (height.isNaN() || height == 0f) {
        1f
    } else {
        1f - lerp(0f, min(PredictiveBackMaxScaleYDistance.toPx(), height), progress) / height
    }
}

@ReadOnlyComposable
@Composable
fun getDialogWindow() = (LocalView.current.parent as? DialogWindowProvider)?.window

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewModalSideSheet() {
    WallFlowTheme {
        Surface {
            val state = rememberSideSheetState()
            val coroutineScope = rememberCoroutineScope()
            Box {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            state.show()
                        }
                    },
                ) {
                    Text(text = "Show")
                }
            }
            ModalSideSheet(
                state = state,
                onDismissRequest = {
                    coroutineScope.launch {
                        state.hide()
                    }
                },
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "This is a side sheet",
                        modifier = Modifier.padding(16.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    state.hide()
                                }
                            },
                            modifier = Modifier.padding(8.dp),
                        ) {
                            Text("Dismiss")
                        }
                        TextButton(
                            onClick = {},
                            modifier = Modifier.padding(8.dp),
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }
}
