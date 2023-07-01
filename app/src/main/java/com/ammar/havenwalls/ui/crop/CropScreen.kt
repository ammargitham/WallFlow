package com.ammar.havenwalls.ui.crop

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.ammar.havenwalls.R
import com.ammar.havenwalls.data.repository.utils.Resource
import com.ammar.havenwalls.data.repository.utils.successOr
import com.ammar.havenwalls.extensions.TAG
import com.ammar.havenwalls.extensions.aspectRatio
import com.ammar.havenwalls.extensions.getScreenResolution
import com.ammar.havenwalls.extensions.toDp
import com.ammar.havenwalls.extensions.toast
import com.ammar.havenwalls.ui.common.BlurTransformation
import com.ammar.havenwalls.ui.common.LocalSystemBarsController
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
    val request by produceState(
        initialValue = null as ImageRequest?,
        key1 = context,
        key2 = uiState.uri,
    ) {
        if (uiState.uri == null) {
            return@produceState
        }
        value = ImageRequest.Builder(context).apply {
            data(uiState.uri)
            crossfade(true)
            transformations(BlurTransformation())
            listener(onError = { _, result ->
                Log.e(TAG, "Error loading: $this", result.throwable)
            })
        }.build()
    }
    val backdropPainter = rememberAsyncImagePainter(model = request)
    val resolution by produceState(
        initialValue = IntSize.Zero,
        key1 = context,
        producer = { value = context.getScreenResolution(true) },
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
        modifier = modifier.fillMaxSize(),
    ) {
        uiState.uri?.run {
            Image(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.2f
                        scaleY = 1.2f
                    },
                painter = backdropPainter,
                contentDescription = stringResource(R.string.wallpaper_description),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = overlayColor)
            )
        }

        Crossfade(targetState = cropState to uiState.result) {
            val (innerCropState, innerResult) = it
            if (innerResult is Result.Pending) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    bitmap = (uiState.result as Result.Pending).bitmap,
                    contentDescription = stringResource(R.string.cropped_image),
                    contentScale = ContentScale.Fit,
                )
            }
            innerCropState?.run {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = actionsSize.height.toDp() + 16.dp)
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

        Actions(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(overlayColor)
                .padding(start = 32.dp, end = 32.dp, bottom = 16.dp)
                .onSizeChanged { actionsSize = it },
            objectDetectionEnabled = uiState.objectDetectionEnabled,
            modelDownloadStatus = uiState.modelDownloadStatus,
            detections = uiState.detectedObjects,
            onDetectionsClick = { viewModel.showDetections(true) },
            onCancelClick = { cropState?.done(false) },
            onSetClick = {
                viewModel.setWallpaperTargets(it)
                cropState?.done(true)
            }
        )

        if (uiState.showDetections) {
            DetectionsBottomSheet(
                detections = uiState.detectedObjects.successOr(emptyList()),
                onDetectionClick = viewModel::selectDetection,
                onDismissRequest = { viewModel.showDetections(false) }
            )
        }
    }
}
