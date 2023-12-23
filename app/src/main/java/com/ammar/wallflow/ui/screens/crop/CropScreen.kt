package com.ammar.wallflow.ui.screens.crop

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.RectF
import android.view.Display
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.ObjectDetectionPreferences
import com.ammar.wallflow.data.repository.utils.Resource
import com.ammar.wallflow.data.repository.utils.successOr
import com.ammar.wallflow.extensions.aspectRatio
import com.ammar.wallflow.extensions.getScreenResolution
import com.ammar.wallflow.extensions.toDp
import com.ammar.wallflow.extensions.toast
import com.ammar.wallflow.model.Detection
import com.ammar.wallflow.model.DetectionCategory
import com.ammar.wallflow.model.DetectionWithBitmap
import com.ammar.wallflow.model.WallpaperTarget
import com.ammar.wallflow.ui.common.LocalSystemController
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.ammar.wallflow.utils.DownloadStatus
import com.mr0xf00.easycrop.CropState
import com.mr0xf00.easycrop.CropperStyle
import com.mr0xf00.easycrop.LocalCropperStyle
import com.mr0xf00.easycrop.ui.CropperPreview

@Composable
fun CropScreen(
    modifier: Modifier = Modifier,
    viewModel: CropViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val systemController = LocalSystemController.current
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
                imageSize,
            )
        }
    }

    LaunchedEffect(Unit) {
        systemController.update {
            it.copy(
                statusBarColor = systemBarColor,
                navigationBarColor = systemBarColor,
            )
        }
    }

    LaunchedEffect(uiState.detectedObjects) {
        if (uiState.detectedObjects !is Resource.Error) return@LaunchedEffect
        val e = (uiState.detectedObjects as Resource.Error).throwable
        context.toast(
            e.localizedMessage
                ?: e.message
                ?: context.getString(R.string.error_detecting_objects),
        )
    }

    LaunchedEffect(
        cropState?.enabled,
        cropState?.transform,
        cropState?.src?.size,
        maxCropSize,
        uiState.detectedRectScale,
        uiState.selectedDetection,
    ) {
        if (cropState?.enabled == false) {
            return@LaunchedEffect
        }
        // when lastDetectionCropRegion != null and selectedDetection == null,
        // it means user manually updated the crop region. Do nothing in such cases.
        if (uiState.lastCropRegion != null && uiState.selectedDetection == null) {
            return@LaunchedEffect
        }
        val state = cropState ?: return@LaunchedEffect
        state.reset()
        val cropScale = state.transform.scale.x
        val imageSize = state.src.size.toSize() * cropScale
        viewModel.setImageSize(imageSize)
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

    LaunchedEffect(
        cropState?.region,
        cropState?.enabled,
    ) {
        if (cropState?.enabled == false) {
            return@LaunchedEffect
        }
        // set uiState selectedDetection null when user updates crop region manually
        if (uiState.lastCropRegion == cropState?.region) return@LaunchedEffect
        if (cropState?.region == null) {
            return@LaunchedEffect
        }
        viewModel.removeSelectedDetectionAndUpdateRegion(cropState.region)
    }

    CropScreenContent(
        modifier = modifier,
        cropState = cropState,
        cropperStyle = cropperStyle,
        result = uiState.result,
        objectDetectionPreferences = uiState.objectDetectionPreferences,
        modelDownloadStatus = uiState.modelDownloadStatus,
        detectedObjects = uiState.detectedObjects,
        showDetections = uiState.showDetections,
        displays = uiState.displays,
        selectedDisplay = uiState.selectedDisplay,
        setSelectedDisplay = viewModel::setSelectedDisplay,
        onDetectionsClick = { viewModel.showDetections(true) },
        onDetectionClick = viewModel::selectDetection,
        onDetectionsDismissRequest = { viewModel.showDetections(false) },
        onCancelClick = { cropState?.done(false) },
        onSetClick = {
            viewModel.setWallpaperTargets(it)
            cropState?.done(true)
        },
        onCropChange = viewModel::onCropChange,
    )
}

@Composable
private fun CropScreenContent(
    modifier: Modifier = Modifier,
    cropState: CropState? = null,
    cropperStyle: CropperStyle = CropperStyle(),
    result: Result = Result.NotStarted,
    objectDetectionPreferences: ObjectDetectionPreferences = ObjectDetectionPreferences(),
    modelDownloadStatus: DownloadStatus? = null,
    detectedObjects: Resource<List<DetectionWithBitmap>>? = null,
    showDetections: Boolean = false,
    displays: List<Display> = emptyList(),
    selectedDisplay: Display? = null,
    setSelectedDisplay: (Display) -> Unit = {},
    onDetectionsClick: () -> Unit = {},
    onDetectionClick: (DetectionWithBitmap) -> Unit = {},
    onDetectionsDismissRequest: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onSetClick: (Set<WallpaperTarget>) -> Unit = {},
    onCropChange: (Boolean) -> Unit = {},
) {
    var actionsSize by remember { mutableStateOf(IntSize.Zero) }
    var topActionsSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Crossfade(
            targetState = cropState to result,
            label = "crop",
        ) {
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
                            top = topActionsSize.height.toDp() + if (displays.size > 1) {
                                16.dp
                            } else {
                                0.dp
                            },
                            bottom = actionsSize.height.toDp() + 16.dp,
                        ),
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

        TopActions(
            modifier = Modifier
                .background(cropperStyle.overlayColor)
                .padding(
                    start = 32.dp,
                    end = 32.dp,
                    top = 16.dp,
                )
                .onSizeChanged { topActionsSize = it },
            crop = cropState?.enabled ?: true,
            displays = displays,
            selectedDisplay = selectedDisplay,
            setSelectedDisplay = setSelectedDisplay,
            onCropChange = onCropChange,
        )

        Actions(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(cropperStyle.overlayColor)
                .padding(
                    start = 32.dp,
                    end = 32.dp,
                    bottom = 16.dp,
                )
                .onSizeChanged { actionsSize = it },
            objectDetectionEnabled = objectDetectionPreferences.enabled,
            modelDownloadStatus = modelDownloadStatus,
            detections = detectedObjects ?: Resource.Success(emptyList()),
            onDetectionsClick = onDetectionsClick,
            onCancelClick = onCancelClick,
            onSetClick = onSetClick,
        )

        if (showDetections) {
            DetectionsBottomSheet(
                detections = detectedObjects?.successOr(emptyList()) ?: emptyList(),
                onDetectionClick = onDetectionClick,
                onDismissRequest = onDetectionsDismissRequest,
            )
        }
    }
}

private data class CropScreenContentProps(
    val objectDetectionPreferences: ObjectDetectionPreferences = ObjectDetectionPreferences(),
    val detectedObjects: Resource<List<DetectionWithBitmap>>? = Resource.Success(emptyList()),
    val showDetections: Boolean = false,
)

private class CropScreenContentCPPP : CollectionPreviewParameterProvider<CropScreenContentProps>(
    listOf(
        CropScreenContentProps(),
        CropScreenContentProps(
            objectDetectionPreferences = ObjectDetectionPreferences(
                enabled = true,
            ),
            detectedObjects = Resource.Success(
                listOf(
                    DetectionWithBitmap(
                        detection = Detection(
                            categories = listOf(
                                DetectionCategory(
                                    index = 0,
                                    label = "test",
                                    displayName = "test",
                                    score = 0.8f,
                                ),
                            ),
                            boundingBox = RectF(0f, 0f, 0f, 0f),
                        ),
                        bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                    ),
                ),
            ),
        ),
    ),
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCropScreenContent(
    @PreviewParameter(CropScreenContentCPPP::class) props: CropScreenContentProps,
) {
    val overlayColor = Color.Black.copy(alpha = 0.5f)
    val cropperStyle = remember {
        CropperStyle(
            backgroundColor = Color.Transparent,
            overlay = overlayColor,
        )
    }

    WallFlowTheme {
        Surface {
            CropScreenContent(
                cropperStyle = cropperStyle,
                objectDetectionPreferences = props.objectDetectionPreferences,
                detectedObjects = props.detectedObjects,
                showDetections = props.showDetections,
            )
        }
    }
}
