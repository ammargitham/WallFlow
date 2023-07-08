package com.ammar.wallflow.ui.crop

import android.view.Display
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ammar.wallflow.R
import com.ammar.wallflow.data.repository.utils.Resource
import com.ammar.wallflow.data.repository.utils.successOr
import com.ammar.wallflow.extensions.aspectRatio
import com.ammar.wallflow.extensions.getScreenResolution
import com.ammar.wallflow.extensions.toDp
import com.ammar.wallflow.extensions.toast
import com.ammar.wallflow.ui.common.LocalSystemBarsController
import com.mr0xf00.easycrop.CropperStyle
import com.mr0xf00.easycrop.LocalCropperStyle
import com.mr0xf00.easycrop.ui.CropperPreview

@Composable
fun CropScreen(
    modifier: Modifier = Modifier,
    viewModel: CropViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val systemBarsController = LocalSystemBarsController.current
    val overlayColor = Color.Black.copy(alpha = 0.5f)
    val systemBarColor = Color.Black.copy(alpha = 0.75f)
    val cropperStyle = remember {
        CropperStyle(
            backgroundColor = Color.Transparent,
            overlay = overlayColor,
        )
    }
    val cropState = viewModel.imageCropper.cropState
    val context = LocalContext.current
    // val request by produceState(
    //     initialValue = null as ImageRequest?,
    //     key1 = context,
    //     key2 = uiState.uri,
    // ) {
    //     if (uiState.uri == null) {
    //         return@produceState
    //     }
    //     value = ImageRequest.Builder(context).apply {
    //         data(uiState.uri)
    //         crossfade(true)
    //         transformations(BlurTransformation())
    //         listener(onError = { _, result ->
    //             Log.e(TAG, "Error loading: $this", result.throwable)
    //         })
    //     }.build()
    // }
    // val backdropPainter = rememberAsyncImagePainter(model = request)
    val resolution by produceState(
        initialValue = IntSize.Zero,
        key1 = context,
        key2 = uiState.selectedDisplay,
        producer = {
            value = context.getScreenResolution(
                inDefaultOrientation = true,
                displayId = uiState.selectedDisplay?.displayId ?: Display.DEFAULT_DISPLAY,
            )
        },
    )
    val maxCropSize by remember(
        cropState?.transform?.scale?.x,
        cropState?.src?.size,
        resolution.aspectRatio,
    ) {
        derivedStateOf {
            val state = cropState ?: return@derivedStateOf Size.Zero
            val cropScale = state.transform.scale.x
            val imageSize = state.src.size.toSize() * cropScale
            getMaxCropSize(
                resolution,
                imageSize
            )
        }
    }
    var actionsSize by remember { mutableStateOf(IntSize.Zero) }
    var topActionsSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(Unit) {
        systemBarsController.update {
            it.copy(
                statusBarColor = systemBarColor,
                navigationBarColor = systemBarColor,
                lightStatusBars = false,
                lightNavigationBars = false,
            )
        }
    }

    LaunchedEffect(uiState.detectedObjects) {
        if (uiState.detectedObjects !is Resource.Error) return@LaunchedEffect
        val e = (uiState.detectedObjects as Resource.Error).throwable
        context.toast(
            e.localizedMessage
                ?: e.message
                ?: context.getString(R.string.error_detecting_objects)
        )
    }

    LaunchedEffect(
        cropState?.transform,
        cropState?.src?.size,
        maxCropSize,
        uiState.detectedRectScale,
        uiState.selectedDetection,
    ) {
        // when lastDetectionCropRegion != null and selectedDetection == null,
        // it means user manually updated the crop region. Do nothing in such cases.
        if (uiState.lastCropRegion != null && uiState.selectedDetection == null) {
            return@LaunchedEffect
        }
        val state = cropState ?: return@LaunchedEffect
        state.reset()
        val cropScale = state.transform.scale.x
        val imageSize = state.src.size.toSize() * cropScale
        val newCropRegion = getCropRect(
            maxCropSize,
            uiState.selectedDetection?.detection?.boundingBox,
            uiState.detectedRectScale,
            imageSize,
            state.transform.scale.x,
        )
        viewModel.setLastCropRegion(newCropRegion)
        state.region = newCropRegion
        state.aspectLock = true
    }

    LaunchedEffect(cropState?.region) {
        // set uiState selectedDetection null when user updates crop region manually
        if (uiState.lastCropRegion == cropState?.region) return@LaunchedEffect
        if (cropState?.region == null) {
            return@LaunchedEffect
        }
        viewModel.removeSelectedDetectionAndUpdateRegion(cropState.region)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // uiState.uri?.run {
        //     Image(
        //         modifier = Modifier
        //             .fillMaxSize()
        //             .graphicsLayer {
        //                 scaleX = 1.2f
        //                 scaleY = 1.2f
        //             },
        //         painter = backdropPainter,
        //         contentDescription = stringResource(R.string.wallpaper_description),
        //         contentScale = ContentScale.Crop,
        //     )
        //     Box(
        //         modifier = Modifier
        //             .fillMaxSize()
        //             .background(color = overlayColor)
        //     )
        // }

        Crossfade(targetState = cropState to uiState.result) {
            val (innerCropState, innerResult) = it
            if (innerResult is Result.Pending) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    bitmap = innerResult.bitmap,
                    contentDescription = stringResource(R.string.cropped_image),
                    contentScale = ContentScale.Fit,
                )
            }
            innerCropState?.run {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = topActionsSize.height.toDp() + if (uiState.displays.size > 1) 16.dp else 0.dp,
                            bottom = actionsSize.height.toDp() + 16.dp,
                        )
                ) {
                    CompositionLocalProvider(LocalCropperStyle provides cropperStyle) {
                        CropperPreview(
                            modifier = Modifier.fillMaxSize(),
                            state = this@run,
                            bringToViewDelay = 200,
                            extraPadding = WindowInsets.mandatorySystemGestures.asPaddingValues(),
                        )
                    }
                }
            }
        }

        if (uiState.displays.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(overlayColor)
                    .padding(
                        start = 32.dp,
                        end = 32.dp,
                        top = 16.dp,
                    )
                    .onSizeChanged { topActionsSize = it },
            ) {
                DisplayButton(
                    selectedDisplay = uiState.selectedDisplay,
                    displays = uiState.displays,
                    onChange = viewModel::setSelectedDisplay,
                )
            }
        }

        Actions(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(overlayColor)
                .padding(
                    start = 32.dp,
                    end = 32.dp,
                    bottom = 16.dp,
                )
                .onSizeChanged { actionsSize = it },
            objectDetectionEnabled = uiState.objectDetectionPreferences.enabled,
            modelDownloadStatus = uiState.modelDownloadStatus,
            detections = uiState.detectedObjects ?: Resource.Success(emptyList()),
            onDetectionsClick = { viewModel.showDetections(true) },
            onCancelClick = { cropState?.done(false) },
            onSetClick = {
                viewModel.setWallpaperTargets(it)
                cropState?.done(true)
            }
        )

        if (uiState.showDetections) {
            DetectionsBottomSheet(
                detections = uiState.detectedObjects?.successOr(emptyList()) ?: emptyList(),
                onDetectionClick = viewModel::selectDetection,
                onDismissRequest = { viewModel.showDetections(false) }
            )
        }
    }
}
