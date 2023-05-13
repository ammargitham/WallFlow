package com.ammar.havenwalls.ui.wallpaper

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.memory.MemoryCache
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.NullRequestDataException
import com.ammar.havenwalls.activities.main.MainActivityViewModel
import com.ammar.havenwalls.activities.setwallpaper.SetWallpaperActivity
import com.ammar.havenwalls.extensions.TAG
import com.ammar.havenwalls.extensions.getFileNameFromUrl
import com.ammar.havenwalls.extensions.getUriForFile
import com.ammar.havenwalls.extensions.isSetWallpaperAllowedCompat
import com.ammar.havenwalls.extensions.isWallpaperSupportedCompat
import com.ammar.havenwalls.extensions.openUrl
import com.ammar.havenwalls.extensions.parseMimeType
import com.ammar.havenwalls.extensions.produceState
import com.ammar.havenwalls.extensions.search
import com.ammar.havenwalls.extensions.share
import com.ammar.havenwalls.extensions.toDp
import com.ammar.havenwalls.extensions.toast
import com.ammar.havenwalls.extensions.wallpaperManager
import com.ammar.havenwalls.model.Search
import com.ammar.havenwalls.model.TagSearchMeta
import com.ammar.havenwalls.model.UploaderSearchMeta
import com.ammar.havenwalls.model.Wallpaper
import com.ammar.havenwalls.model.wallpaper1
import com.ammar.havenwalls.ui.common.LocalSystemBarsController
import com.ammar.havenwalls.ui.common.SystemBarsState
import com.ammar.havenwalls.ui.common.TopBar
import com.ammar.havenwalls.ui.common.bottombar.LocalBottomBarController
import com.ammar.havenwalls.ui.common.mainsearch.LocalMainSearchBarController
import com.ammar.havenwalls.ui.common.mainsearch.MainSearchBarState
import com.ammar.havenwalls.ui.common.permissions.DownloadPermissionsRationalDialog
import com.ammar.havenwalls.ui.common.permissions.MultiplePermissionItem
import com.ammar.havenwalls.ui.common.permissions.isGranted
import com.ammar.havenwalls.ui.common.permissions.rememberMultiplePermissionsState
import com.ammar.havenwalls.ui.common.permissions.shouldShowRationale
import com.ammar.havenwalls.ui.theme.HavenWallsTheme
import com.ammar.havenwalls.utils.DownloadStatus
import com.google.modernstorage.permissions.StoragePermissions
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import de.mr_pine.zoomables.ZoomableImage
import de.mr_pine.zoomables.ZoomableState
import de.mr_pine.zoomables.rememberZoomableState
import java.net.SocketTimeoutException
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Destination(
    navArgsDelegate = WallpaperScreenNavArgs::class,
    deepLinks = [
        DeepLink(uriPattern = wallpaperScreenLocalDeepLinkUriPattern),
        DeepLink(uriPattern = wallpaperScreenExternalDeepLinkUriPattern),
        DeepLink(uriPattern = wallpaperScreenExternalShortDeepLinkUriPattern),
    ]
)
@Composable
fun WallpaperScreen(
    navController: NavController,
    navArgs: WallpaperScreenNavArgs,
    mainActivityViewModel: MainActivityViewModel,
    viewModel: WallpaperViewModel = hiltViewModel(),
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val uiState = lifecycle.produceState(
        viewModel = viewModel,
        initialValue = WallpaperUiState(navArgs.wallpaperId),
    )
    val navigationBarSemiTransparentColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    val searchBarController = LocalMainSearchBarController.current
    val bottomBarController = LocalBottomBarController.current
    val systemBarsController = LocalSystemBarsController.current
    val context = LocalContext.current

    val storagePerms = remember {
        StoragePermissions.getPermissions(
            action = StoragePermissions.Action.READ_AND_WRITE,
            types = listOf(StoragePermissions.FileType.Image),
            createdBy = StoragePermissions.CreatedBy.Self
        ).map { MultiplePermissionItem(permission = it) }
    }

    @SuppressLint("InlinedApi")
    val downloadPermissionsState = rememberMultiplePermissionsState(
        permissions = storagePerms + MultiplePermissionItem(
            permission = Manifest.permission.POST_NOTIFICATIONS,
            minimumSdk = Build.VERSION_CODES.TIRAMISU,
        )
    ) { permissionStates ->
        val showRationale = permissionStates.map { it.status.shouldShowRationale }.any { it }
        if (showRationale) {
            viewModel.showNotificationPermissionRationaleDialog(true)
            return@rememberMultiplePermissionsState
        }
        // check if storage permissions are granted (notification permission is optional)
        val storagePermStrings = storagePerms.map { it.permission }
        val allGranted = permissionStates
            .filter { it.permission in storagePermStrings }
            .all { it.status.isGranted }
        if (!allGranted) return@rememberMultiplePermissionsState
        viewModel.download()
    }

    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            systemBarsController.update {
                SystemBarsState(
                    statusBarColor = Color.Transparent,
                    lightStatusBars = false,
                )
            }
        }
    }

    DisposableEffect(Unit) {
        searchBarController.update { MainSearchBarState(visible = false) }
        bottomBarController.update { it.copy(visible = false) }
        mainActivityViewModel.applyScaffoldPadding(false)

        onDispose {
            mainActivityViewModel.applyScaffoldPadding(true)
        }
    }

    LaunchedEffect(uiState.systemBarsVisible, uiState.showInfo) {
        systemBarsController.update {
            it.copy(
                statusBarVisible = uiState.systemBarsVisible,
                navigationBarVisible = uiState.systemBarsVisible,
                navigationBarColor = if (uiState.showInfo) Color.Unspecified else navigationBarSemiTransparentColor,
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        WallpaperScreenContent(
            wallpaper = uiState.wallpaper,
            actionsVisible = uiState.actionsVisible,
            downloadStatus = uiState.downloadStatus,
            loading = uiState.loading,
            cacheKey = navArgs.cacheKey,
            onWallpaperTransform = viewModel::onWallpaperTransform,
            onWallpaperTap = viewModel::onWallpaperTap,
            onInfoClick = viewModel::showInfo,
            onDownloadClick = { downloadPermissionsState.launchMultiplePermissionRequest() },
            onShareLinkClick = { uiState.wallpaper?.run { context.share(url) } },
            onShareImageClick = {
                val wallpaper = uiState.wallpaper ?: return@WallpaperScreenContent
                viewModel.downloadForSharing {
                    if (it == null) return@downloadForSharing
                    context.share(
                        uri = context.getUriForFile(it),
                        type = wallpaper.fileType.ifBlank { parseMimeType(wallpaper.path) },
                        title = wallpaper.path.getFileNameFromUrl(),
                        grantTempPermission = true,
                    )
                }
            },
            onApplyWallpaperClick = {
                viewModel.downloadForSharing {
                    val file = it ?: return@downloadForSharing
                    context.startActivity(
                        Intent().apply {
                            setClass(context, SetWallpaperActivity::class.java)
                            putExtra(
                                SetWallpaperActivity.EXTRA_URI,
                                context.getUriForFile(file),
                            )
                        }
                    )
                }
            }
        )

        TopBar(
            navController = navController,
            visible = uiState.systemBarsVisible,
            gradientBg = true,
        )
    }

    if (uiState.showInfo) {
        uiState.wallpaper?.run {
            WallpaperInfoBottomSheet(
                onDismissRequest = { viewModel.showInfo(false) },
                wallpaper = this,
                onTagClick = {
                    navController.search(
                        Search(
                            query = "id:${it.id}",
                            meta = TagSearchMeta(tag = it),
                        )
                    )
                },
                onUploaderClick = {
                    uploader?.run {
                        navController.search(
                            Search(
                                query = "@${username}",
                                meta = UploaderSearchMeta(uploader = this),
                            )
                        )
                    }
                },
                onSourceClick = { context.openUrl(source) }
            )
        }
    }

    if (uiState.showNotificationPermissionRationaleDialog) {
        DownloadPermissionsRationalDialog(
            permissions = downloadPermissionsState.shouldShowRationale.keys.map { it.permission },
            onConfirmOrDismiss = {
                viewModel.showNotificationPermissionRationaleDialog(false)
                // notificationPermissionState.launchPermissionRequest()
            }
        )
    }
}

@Composable
fun WallpaperScreenContent(
    modifier: Modifier = Modifier,
    wallpaper: Wallpaper? = null,
    actionsVisible: Boolean = true,
    downloadStatus: DownloadStatus? = null,
    loading: Boolean = false,
    cacheKey: MemoryCache.Key? = null,
    onWallpaperTransform: () -> Unit = {},
    onWallpaperTap: () -> Unit = {},
    onInfoClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onShareLinkClick: () -> Unit = {},
    onShareImageClick: () -> Unit = {},
    onApplyWallpaperClick: () -> Unit = {},
) {
    var containerIntSize by remember { mutableStateOf(IntSize.Zero) }

    val imageSize: IntSize by produceState(
        initialValue = IntSize.Zero,
        key1 = wallpaper,
        key2 = containerIntSize,
    ) {
        val resolution = wallpaper?.resolution
        if (resolution == null) {
            value = IntSize.Zero
            return@produceState
        }
        val containerAspectRatio = containerIntSize.width.toFloat() / containerIntSize.height
        val imageAspectRatio = resolution.aspectRatio
        value = when {
            containerAspectRatio == imageAspectRatio -> IntSize(
                containerIntSize.width,
                containerIntSize.height
            )
            containerAspectRatio < imageAspectRatio -> IntSize(
                containerIntSize.width,
                (containerIntSize.width / imageAspectRatio).roundToInt(),
            )
            else -> IntSize(
                (containerIntSize.height * imageAspectRatio).roundToInt(),
                containerIntSize.height,
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
                    is SocketTimeoutException -> context.toast("Request timed out!")
                    is NullRequestDataException -> return // Do nothing
                    else -> context.toast(throwable.message ?: "Error occurred!")
                }
                Log.e(TAG, "onError: ", throwable)
            }
        }
    }
    val request by produceState(
        initialValue = null as ImageRequest?,
        key1 = context,
        key2 = wallpaper,
        key3 = listOf(cacheKey, imageSize, listener),
    ) {
        if (wallpaper == null || imageSize == IntSize.Zero) {
            return@produceState
        }
        value = ImageRequest.Builder(context).apply {
            data(wallpaper.path)
            size(imageSize.width, imageSize.height)
            crossfade(true)
            lifecycle(lifecycleOwner)
            listener(listener)
            cacheKey?.run { placeholderMemoryCacheKey(this) }
        }.build()
    }
    val painter = rememberAsyncImagePainter(model = request)
    val zoomableState = rememberZoomableState(
        rotationBehavior = ZoomableState.Rotation.DISABLED,
        onTransformation = { zoomChange, panChange, _ ->
            val newScale = (scale.value * zoomChange).coerceIn(1f, 5f)
            scale.value = newScale
            val containerSize = containerIntSize.toSize()
            val scaledImageSize = imageSize.toSize() * newScale
            val newOffset = offset.value + panChange
            val offsetX = if (scaledImageSize.width <= containerSize.width) 0F else {
                val maxX = (scaledImageSize.width - containerSize.width) / 2
                newOffset.x.coerceIn(-maxX, maxX)
            }
            val offsetY = if (scaledImageSize.height <= containerSize.height) 0F else {
                val maxY = (scaledImageSize.height - containerSize.height) / 2
                newOffset.y.coerceIn(-maxY, maxY)
            }
            offset.value = Offset(offsetX, offsetY)
            onWallpaperTransform()
        },
    )
    val coroutineScope = rememberCoroutineScope()
    val navigationPadding = WindowInsets.navigationBars
        .getBottom(LocalDensity.current)
        .toDp()
    val applyWallpaperEnabled by remember(context) {
        val wallpaperManager = context.wallpaperManager
        derivedStateOf {
            wallpaperManager.isWallpaperSupportedCompat && wallpaperManager.isSetWallpaperAllowedCompat
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerIntSize = it }
    ) {
        ZoomableImage(
            modifier = Modifier.fillMaxSize(),
            coroutineScope = coroutineScope,
            zoomableState = zoomableState,
            painter = painter,
            onTap = {
                if (painter.state !is AsyncImagePainter.State.Success) return@ZoomableImage
                onWallpaperTap()
            },
            doubleTapBehaviour = zoomableState.DefaultDoubleTapBehaviour(
                coroutineScope = coroutineScope,
                zoomScale = 5f,
            ),
        )

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 0.dp,
                    bottom = navigationPadding + 32.dp,
                ),
            visible = painter.state is AsyncImagePainter.State.Success && actionsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            WallpaperActions(
                downloadStatus = downloadStatus,
                applyWallpaperEnabled = applyWallpaperEnabled,
                onInfoClick = onInfoClick,
                onDownloadClick = onDownloadClick,
                onShareLinkClick = onShareLinkClick,
                onShareImageClick = onShareImageClick,
                onApplyWallpaperClick = onApplyWallpaperClick,
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
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewWallpaperScreenContent() {
    HavenWallsTheme {
        Surface {
            WallpaperScreenContent(wallpaper = wallpaper1)
        }
    }
}
