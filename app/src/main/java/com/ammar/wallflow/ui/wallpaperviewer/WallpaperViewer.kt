package com.ammar.wallflow.ui.wallpaperviewer

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.NullRequestData
import coil.request.NullRequestDataException
import coil.size.Scale
import com.ammar.wallflow.R
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.extensions.aspectRatio
import com.ammar.wallflow.extensions.openUrl
import com.ammar.wallflow.extensions.toDp
import com.ammar.wallflow.extensions.toast
import com.ammar.wallflow.extensions.wallpaperManager
import com.ammar.wallflow.model.DownloadableWallpaper
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.model.wallhaven.WallhavenUploader
import com.ammar.wallflow.model.wallhaven.WallhavenWallpaper
import com.ammar.wallflow.ui.common.bottomWindowInsets
import com.ammar.wallflow.ui.common.permissions.DownloadPermissionsRationalDialog
import com.ammar.wallflow.ui.common.permissions.rememberDownloadPermissionsState
import com.ammar.wallflow.ui.screens.wallpaper.WallpaperActions
import com.ammar.wallflow.ui.screens.wallpaper.WallpaperInfoBottomSheet
import com.ammar.wallflow.utils.DownloadStatus
import java.net.SocketTimeoutException
import kotlin.math.roundToInt
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

@Composable
fun WallpaperViewer(
    modifier: Modifier = Modifier,
    wallpaper: Wallpaper? = null,
    actionsVisible: Boolean = true,
    downloadStatus: DownloadStatus? = null,
    loading: Boolean = false,
    thumbData: String? = null,
    showInfo: Boolean = false,
    showFullScreenAction: Boolean = false,
    onWallpaperTransform: () -> Unit = {},
    onWallpaperTap: () -> Unit = {},
    onInfoClick: () -> Unit = {},
    onInfoDismiss: () -> Unit = {},
    onShareLinkClick: () -> Unit = {},
    onShareImageClick: () -> Unit = {},
    onApplyWallpaperClick: () -> Unit = {},
    onFullScreenClick: () -> Unit = {},
    onDownloadPermissionsGranted: () -> Unit = {},
    onTagClick: (WallhavenTag) -> Unit = {},
    onUploaderClick: (WallhavenUploader) -> Unit = {},
) {
    var showRationale by rememberSaveable { mutableStateOf(false) }
    var containerIntSize by remember { mutableStateOf(IntSize.Zero) }

    val downloadPermissionsState = rememberDownloadPermissionsState(
        onShowRationale = { showRationale = true },
        onGranted = onDownloadPermissionsGranted,
    )

    val imageSize: IntSize by produceState(
        initialValue = IntSize.Zero,
        key1 = wallpaper?.resolution,
        key2 = containerIntSize,
    ) {
        val resolution = wallpaper?.resolution
        if (resolution == null) {
            value = IntSize.Zero
            return@produceState
        }
        val containerWidth = containerIntSize.width
        val containerHeight = containerIntSize.height
        val containerAspectRatio = containerWidth.toFloat() / containerHeight
        val imageAspectRatio = resolution.aspectRatio
        value = when {
            containerAspectRatio == imageAspectRatio -> IntSize(
                containerWidth,
                containerHeight,
            )
            containerAspectRatio < imageAspectRatio -> IntSize(
                containerWidth,
                (containerWidth / imageAspectRatio).roundToInt(),
            )
            else -> IntSize(
                (containerHeight * imageAspectRatio).roundToInt(),
                containerHeight,
            )
        }
    }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val listener = remember {
        object : ImageRequest.Listener {
            override fun onError(request: ImageRequest, result: ErrorResult) {
                val throwable = result.throwable
                when (throwable) {
                    is SocketTimeoutException -> context.toast(
                        context.getString(R.string.request_timed_out),
                    )
                    is NullRequestDataException -> return // Do nothing
                    else -> context.toast(
                        throwable.message ?: context.getString(R.string.error_occurred),
                    )
                }
                Log.e(TAG, "onError: ", throwable)
            }
        }
    }
    val request by produceState(
        initialValue = null as ImageRequest?,
        key1 = context,
        key2 = wallpaper?.data,
        key3 = listOf(thumbData, imageSize, listener),
    ) {
        if (wallpaper?.data == null && thumbData == null) {
            return@produceState
        }
        value = ImageRequest.Builder(context).apply {
            data(wallpaper?.data ?: thumbData)
            placeholderMemoryCacheKey(thumbData)
            if (imageSize != IntSize.Zero) {
                size(imageSize.width, imageSize.height)
            }
            scale(Scale.FIT)
            crossfade(true)
            lifecycle(lifecycleOwner)
            listener(listener)
        }.build()
    }
    val painter = rememberAsyncImagePainter(model = request)
    val zoomableState = rememberZoomableState(
        zoomSpec = ZoomSpec(
            maxZoomFactor = 5f,
        ),
    )
    var hasTransformed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(zoomableState.contentTransformation, painter) {
        val scale = zoomableState.contentTransformation.scale
        if (
            painter.state !is AsyncImagePainter.State.Success ||
            painter.request.data is NullRequestData ||
            scale == ScaleFactor(0f, 0f)
        ) {
            return@LaunchedEffect
        }
        // if user has not interacted yet, do nothing
        if (!hasTransformed && scale.scaleX <= 1f) {
            return@LaunchedEffect
        }
        onWallpaperTransform()
        hasTransformed = true
    }
    val applyWallpaperEnabled by remember(context) {
        val wallpaperManager = context.wallpaperManager
        derivedStateOf {
            wallpaperManager.isWallpaperSupported && wallpaperManager.isSetWallpaperAllowed
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerIntSize = it },
    ) {
        Crossfade(
            modifier = Modifier.fillMaxSize(),
            targetState = painter,
            label = "wallpaper",
        ) {
            val imageRequest = it.request
            if (
                showFullScreenAction && // means two pane mode
                it.state !is AsyncImagePainter.State.Loading &&
                imageRequest.data is NullRequestData
            ) {
                NotSelectedPlaceholder(
                    containerIntSize = containerIntSize,
                )
                return@Crossfade
            }
            Image(
                modifier = Modifier
                    .fillMaxSize()
                    .zoomable(
                        state = zoomableState,
                        onClick = { _ ->
                            if (it.state !is AsyncImagePainter.State.Success) return@zoomable
                            onWallpaperTap()
                        },
                    ),
                painter = it,
                contentDescription = stringResource(R.string.wallpaper),
            )
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = painter.state is AsyncImagePainter.State.Success &&
                painter.request.data == wallpaper?.data &&
                actionsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            WallpaperActions(
                downloadStatus = downloadStatus,
                applyWallpaperEnabled = applyWallpaperEnabled,
                showFullScreenAction = showFullScreenAction,
                showDownloadAction = wallpaper is DownloadableWallpaper,
                showShareLinkAction = wallpaper is WallhavenWallpaper,
                onInfoClick = onInfoClick,
                onDownloadClick = { downloadPermissionsState.launchMultiplePermissionRequest() },
                onShareLinkClick = onShareLinkClick,
                onShareImageClick = onShareImageClick,
                onApplyWallpaperClick = onApplyWallpaperClick,
                onFullScreenClick = onFullScreenClick,
            )
        }

        AnimatedVisibility(
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.Center),
            visible = loading || painter.state is AsyncImagePainter.State.Loading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (showInfo) {
        wallpaper?.run {
            WallpaperInfoBottomSheet(
                contentModifier = Modifier.windowInsetsPadding(bottomWindowInsets),
                onDismissRequest = onInfoDismiss,
                wallpaper = this,
                onTagClick = onTagClick,
                onUploaderClick = {
                    if (this is WallhavenWallpaper) {
                        uploader?.let(onUploaderClick)
                    }
                },
                onSourceClick = {
                    if (this is WallhavenWallpaper) {
                        context.openUrl(wallhavenSource)
                    }
                },
            )
        }
    }

    if (showRationale) {
        DownloadPermissionsRationalDialog(
            permissions = downloadPermissionsState.shouldShowRationale.keys.map { it.permission },
            onConfirmOrDismiss = { showRationale = false },
        )
    }
}

@Composable
private fun NotSelectedPlaceholder(
    modifier: Modifier = Modifier,
    containerIntSize: IntSize,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            modifier = Modifier.size((containerIntSize.width / 2).toDp()),
            painter = painterResource(R.drawable.outline_image_24),
            contentDescription = stringResource(R.string.no_wallpaper_selected),
            contentScale = ContentScale.FillWidth,
            colorFilter = ColorFilter.tint(
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
            ),
        )
        Text(
            text = stringResource(R.string.no_wallpaper_selected),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}
