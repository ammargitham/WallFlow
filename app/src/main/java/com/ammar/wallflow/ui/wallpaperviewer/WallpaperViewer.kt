package com.ammar.wallflow.ui.wallpaperviewer

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.NullRequestData
import coil.request.NullRequestDataException
import coil.size.Scale
import com.ammar.wallflow.R
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.extensions.openUrl
import com.ammar.wallflow.extensions.toDp
import com.ammar.wallflow.extensions.toast
import com.ammar.wallflow.extensions.wallpaperManager
import com.ammar.wallflow.model.DownloadableWallpaper
import com.ammar.wallflow.model.LightDarkType
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.reddit.RedditWallpaper
import com.ammar.wallflow.model.reddit.withRedditDomainPrefix
import com.ammar.wallflow.model.wallhaven.WallhavenTag
import com.ammar.wallflow.model.wallhaven.WallhavenUploader
import com.ammar.wallflow.model.wallhaven.WallhavenWallpaper
import com.ammar.wallflow.ui.common.TopBar
import com.ammar.wallflow.ui.common.bottomWindowInsets
import com.ammar.wallflow.ui.common.permissions.DownloadPermissionsRationalDialog
import com.ammar.wallflow.ui.common.permissions.rememberDownloadPermissionsState
import com.ammar.wallflow.ui.screens.wallpaper.LightDarkInfoDialog
import com.ammar.wallflow.ui.screens.wallpaper.ShareButton
import com.ammar.wallflow.ui.screens.wallpaper.WallpaperActions
import com.ammar.wallflow.ui.screens.wallpaper.WallpaperInfoBottomSheet
import com.ammar.wallflow.utils.DownloadStatus
import java.net.SocketTimeoutException
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperViewer(
    modifier: Modifier = Modifier,
    wallpaper: Wallpaper? = null,
    actionsVisible: Boolean = true,
    downloadStatus: DownloadStatus? = null,
    loading: Boolean = false,
    thumbData: String? = null,
    showInfo: Boolean = false,
    isExpanded: Boolean = false,
    isFavorite: Boolean = false,
    showBackButton: Boolean = false,
    lightDarkTypeFlags: Int = LightDarkType.UNSPECIFIED,
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
    onFavoriteToggle: (Boolean) -> Unit = {},
    onBackClick: () -> Unit = {},
    onLightDarkTypeFlagsChange: (Int) -> Unit = {},
) {
    val clipboardManager = LocalClipboardManager.current
    val layoutDirection = LocalLayoutDirection.current
    var showRationale by rememberSaveable { mutableStateOf(false) }
    var containerIntSize by remember { mutableStateOf(IntSize.Zero) }
    var showLightDarkInfo by rememberSaveable { mutableStateOf(false) }

    val downloadPermissionsState = rememberDownloadPermissionsState(
        onShowRationale = { showRationale = true },
        onGranted = onDownloadPermissionsGranted,
    )

    // val imageSize: IntSize by produceState(
    //     initialValue = IntSize.Zero,
    //     key1 = wallpaper?.resolution,
    //     key2 = containerIntSize,
    // ) {
    //     val resolution = wallpaper?.resolution
    //     if (resolution == null) {
    //         value = IntSize.Zero
    //         return@produceState
    //     }
    //     val containerWidth = containerIntSize.width
    //     val containerHeight = containerIntSize.height
    //     val containerAspectRatio = containerWidth.toFloat() / containerHeight
    //     val imageAspectRatio = resolution.aspectRatio
    //     value = when {
    //         containerAspectRatio == imageAspectRatio -> IntSize(
    //             containerWidth,
    //             containerHeight,
    //         )
    //         containerAspectRatio < imageAspectRatio -> IntSize(
    //             containerWidth,
    //             (containerWidth / imageAspectRatio).roundToInt(),
    //         )
    //         else -> IntSize(
    //             (containerHeight * imageAspectRatio).roundToInt(),
    //             containerHeight,
    //         )
    //     }
    // }
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
        key3 = listOf(
            thumbData,
            // imageSize,
            listener,
        ),
    ) {
        // if (wallpaper?.data == null && thumbData == null) {
        //     return@produceState
        // }
        value = ImageRequest.Builder(context).apply {
            data(wallpaper?.data ?: thumbData)
            placeholderMemoryCacheKey(thumbData)
            // if (imageSize != IntSize.Zero) {
            //     size(imageSize.width, imageSize.height)
            // }
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

    LaunchedEffect(wallpaper?.data) {
        zoomableState.resetZoom(false)
    }

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
            .then(
                if (isExpanded) {
                    Modifier
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(
                            top = 16.dp,
                            bottom = 16.dp,
                            end = if (layoutDirection == LayoutDirection.Ltr) 16.dp else 0.dp,
                            start = if (layoutDirection == LayoutDirection.Rtl) 16.dp else 0.dp,
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceBright)
                } else {
                    Modifier
                },
            )
            .onSizeChanged { containerIntSize = it },
    ) {
        Crossfade(
            modifier = Modifier.fillMaxSize(),
            targetState = painter,
            label = "wallpaper",
        ) {
            val imageRequest = it.request
            if (
                isExpanded && // means two pane mode
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
                            if (it.state !is AsyncImagePainter.State.Success) {
                                return@zoomable
                            }
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
                showApplyWallpaperAction = applyWallpaperEnabled,
                showFullScreenAction = isExpanded,
                showDownloadAction = wallpaper is DownloadableWallpaper,
                isFavorite = isFavorite,
                lightDarkTypeFlags = lightDarkTypeFlags,
                onDownloadClick = { downloadPermissionsState.launchMultiplePermissionRequest() },
                onApplyWallpaperClick = onApplyWallpaperClick,
                onFullScreenClick = onFullScreenClick,
                onFavoriteToggle = onFavoriteToggle,
                onLightDarkTypeFlagsChange = onLightDarkTypeFlagsChange,
                onShowLightDarkInfoClick = { showLightDarkInfo = true },
                onInfoClick = onInfoClick,
            )
        }

        // AnimatedVisibility(
        //     modifier = Modifier
        //         .size(60.dp)
        //         .align(Alignment.Center),
        //     visible = loading || painter.state is AsyncImagePainter.State.Loading,
        //     enter = fadeIn(),
        //     exit = fadeOut(),
        // ) {
        //     CircularProgressIndicator(
        //         modifier = Modifier.fillMaxSize(),
        //     )
        // }

        TopBar(
            windowInsets = if (isExpanded) {
                WindowInsets(top = 0)
            } else {
                TopAppBarDefaults.windowInsets
            },
            visible = if (isExpanded) {
                painter.state is AsyncImagePainter.State.Success &&
                    painter.request.data == wallpaper?.data &&
                    actionsVisible
            } else {
                actionsVisible
            },
            gradientBg = if (showBackButton) {
                true
            } else {
                painter.state is AsyncImagePainter.State.Success &&
                    painter.request.data == wallpaper?.data
            },
            showBackButton = showBackButton,
            onBackClick = onBackClick,
            actions = {
                AnimatedVisibility(
                    visible = painter.state is AsyncImagePainter.State.Success &&
                        painter.request.data == wallpaper?.data,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    ShareButton(
                        showShareLinkAction = wallpaper is WallhavenWallpaper ||
                            wallpaper is RedditWallpaper,
                        onLinkClick = onShareLinkClick,
                        onImageClick = onShareImageClick,
                    )
                }
            },
        )
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
                    val url = getSourceUrl() ?: return@WallpaperInfoBottomSheet
                    context.openUrl(url)
                },
                onSourceLongClick = {
                    val url = getSourceUrl() ?: return@WallpaperInfoBottomSheet
                    clipboardManager.setText(AnnotatedString(url))
                    context.toast(context.getString(R.string.url_copied))
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

    if (showLightDarkInfo) {
        LightDarkInfoDialog(
            onDismissRequest = { showLightDarkInfo = false },
        )
    }
}

private fun Wallpaper.getSourceUrl(): String? {
    val url = when (this) {
        is WallhavenWallpaper -> wallhavenSource
        is RedditWallpaper -> postUrl.withRedditDomainPrefix()
        else -> null
    }
    return url
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
