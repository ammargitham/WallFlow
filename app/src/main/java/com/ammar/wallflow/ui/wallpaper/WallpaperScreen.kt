package com.ammar.wallflow.ui.wallpaper

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.NullRequestData
import coil.request.NullRequestDataException
import coil.size.Scale
import com.ammar.wallflow.R
import com.ammar.wallflow.activities.main.MainActivityViewModel
import com.ammar.wallflow.activities.setwallpaper.SetWallpaperActivity
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.extensions.aspectRatio
import com.ammar.wallflow.extensions.getFileNameFromUrl
import com.ammar.wallflow.extensions.getUriForFile
import com.ammar.wallflow.extensions.isSetWallpaperAllowedCompat
import com.ammar.wallflow.extensions.openUrl
import com.ammar.wallflow.extensions.parseMimeType
import com.ammar.wallflow.extensions.search
import com.ammar.wallflow.extensions.share
import com.ammar.wallflow.extensions.toDp
import com.ammar.wallflow.extensions.toast
import com.ammar.wallflow.extensions.wallpaperManager
import com.ammar.wallflow.model.Search
import com.ammar.wallflow.model.TagSearchMeta
import com.ammar.wallflow.model.UploaderSearchMeta
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.ui.common.LocalSystemBarsController
import com.ammar.wallflow.ui.common.SystemBarsState
import com.ammar.wallflow.ui.common.TopBar
import com.ammar.wallflow.ui.common.bottomWindowInsets
import com.ammar.wallflow.ui.common.bottombar.LocalBottomBarController
import com.ammar.wallflow.ui.common.mainsearch.LocalMainSearchBarController
import com.ammar.wallflow.ui.common.mainsearch.MainSearchBarState
import com.ammar.wallflow.ui.common.navigation.TwoPaneNavigation
import com.ammar.wallflow.ui.common.navigation.TwoPaneNavigation.Mode
import com.ammar.wallflow.ui.common.navigation.TwoPaneNavigation.PaneSide
import com.ammar.wallflow.ui.common.permissions.DownloadPermissionsRationalDialog
import com.ammar.wallflow.ui.common.permissions.MultiplePermissionItem
import com.ammar.wallflow.ui.common.permissions.isGranted
import com.ammar.wallflow.ui.common.permissions.rememberMultiplePermissionsState
import com.ammar.wallflow.ui.common.permissions.shouldShowRationale
import com.ammar.wallflow.ui.destinations.WallpaperScreenDestination
import com.ammar.wallflow.utils.DownloadStatus
import com.google.modernstorage.permissions.StoragePermissions
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import java.net.SocketTimeoutException
import kotlin.math.roundToInt
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

@OptIn(ExperimentalMaterial3Api::class)
@Destination(
    navArgsDelegate = WallpaperScreenNavArgs::class,
    deepLinks = [
        DeepLink(uriPattern = wallpaperScreenLocalDeepLinkUriPattern),
        DeepLink(uriPattern = wallpaperScreenExternalDeepLinkUriPattern),
        DeepLink(uriPattern = wallpaperScreenExternalShortDeepLinkUriPattern),
    ],
)
@Composable
fun WallpaperScreen(
    mainActivityViewModel: MainActivityViewModel,
    twoPaneController: TwoPaneNavigation.Controller,
    paneSide: PaneSide,
    navViewModel: WallpaperViewModel,
) {
    val isTwoPaneMode = paneSide == PaneSide.Pane2
    val viewModel = if (isTwoPaneMode) navViewModel else hiltViewModel()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
        BottomSheetDefaults.Elevation,
    )
    // TODO: Use Color.Transparent for nav bar
    // fully transparent nav bar will require setting some extra flags,
    // so setting alpha 0.01 as current workaround
    val navigationBarColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.01f)
    // val navigationBarColor = Color.Transparent
    val searchBarController = LocalMainSearchBarController.current
    val bottomBarController = LocalBottomBarController.current
    val systemBarsController = LocalSystemBarsController.current
    val context = LocalContext.current
    val systemBars = WindowInsets.systemBars
    val windowInsets = remember {
        if (isTwoPaneMode) systemBars else WindowInsets(left = 0)
    }

    val storagePerms = remember {
        StoragePermissions.getPermissions(
            action = StoragePermissions.Action.READ_AND_WRITE,
            types = listOf(StoragePermissions.FileType.Image),
            createdBy = StoragePermissions.CreatedBy.Self,
        ).map { MultiplePermissionItem(permission = it) }
    }

    @SuppressLint("InlinedApi")
    val downloadPermissionsState = rememberMultiplePermissionsState(
        permissions = storagePerms + MultiplePermissionItem(
            permission = Manifest.permission.POST_NOTIFICATIONS,
            minimumSdk = Build.VERSION_CODES.TIRAMISU,
        ),
    ) { permissionStates ->
        val showRationale = permissionStates.map { it.status.shouldShowRationale }.any { it }
        if (showRationale) {
            viewModel.showPermissionRationaleDialog(true)
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

    LaunchedEffect(paneSide) {
        if (paneSide != PaneSide.Pane1) {
            return@LaunchedEffect
        }
        // if screen was launched in pane 1, set the pane mode to SINGLE_PANE
        twoPaneController.setPaneMode(Mode.SINGLE_PANE)
    }

    LaunchedEffect(Unit) {
        if (isTwoPaneMode) return@LaunchedEffect
        // no need for transparent status bar
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            systemBarsController.update {
                SystemBarsState(
                    statusBarColor = Color.Transparent,
                    lightStatusBars = false,
                    navigationBarColor = navigationBarColor,
                )
            }
        }
    }

    DisposableEffect(Unit) {
        if (isTwoPaneMode) return@DisposableEffect onDispose {}
        // for tablets, we do not update the search bar and bottom bar states
        searchBarController.update { MainSearchBarState(visible = false) }
        bottomBarController.update { it.copy(visible = false) }
        mainActivityViewModel.applyScaffoldPadding(false)

        onDispose {
            mainActivityViewModel.applyScaffoldPadding(true)
        }
    }

    LaunchedEffect(uiState.systemBarsVisible, uiState.showInfo) {
        // no need for transparent status bar, navigation bar when on tablets
        if (isTwoPaneMode) return@LaunchedEffect
        systemBarsController.update {
            it.copy(
                statusBarVisible = uiState.systemBarsVisible,
                navigationBarVisible = uiState.systemBarsVisible,
                navigationBarColor = if (uiState.showInfo) sheetColor else navigationBarColor,
            )
        }
    }

    Box(
        modifier = Modifier
            .windowInsetsPadding(windowInsets)
            .fillMaxSize(),
    ) {
        WallpaperScreenContent(
            wallpaper = uiState.wallpaper,
            actionsVisible = uiState.actionsVisible,
            downloadStatus = uiState.downloadStatus,
            loading = uiState.loading,
            thumbUrl = uiState.thumbUrl,
            showFullScreenAction = isTwoPaneMode,
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
                        },
                    )
                }
            },
            onFullScreenClick = {
                twoPaneController.setPaneMode(Mode.SINGLE_PANE)
                twoPaneController.navigatePane1(
                    WallpaperScreenDestination(
                        thumbUrl = uiState.thumbUrl,
                        wallpaperId = uiState.wallpaper?.id,
                    ),
                )
            },
        )

        if (!isTwoPaneMode) {
            // top bar required only for single pane mode
            TopBar(
                navController = twoPaneController.pane1NavHostController,
                visible = uiState.systemBarsVisible,
                gradientBg = true,
                showBackButton = true,
            )
        }
    }

    if (uiState.showInfo) {
        uiState.wallpaper?.run {
            WallpaperInfoBottomSheet(
                contentModifier = Modifier.windowInsetsPadding(bottomWindowInsets),
                onDismissRequest = { viewModel.showInfo(false) },
                wallpaper = this,
                onTagClick = {
                    twoPaneController.pane1NavHostController.search(
                        Search(
                            query = "id:${it.id}",
                            meta = TagSearchMeta(tag = it),
                        ),
                    )
                    if (isTwoPaneMode) {
                        // dismiss the bottom sheet
                        viewModel.showInfo(false)
                    }
                },
                onUploaderClick = {
                    uploader?.run {
                        twoPaneController.pane1NavHostController.search(
                            Search(
                                query = "@$username",
                                meta = UploaderSearchMeta(uploader = this),
                            ),
                        )
                        if (isTwoPaneMode) {
                            // dismiss the bottom sheet
                            viewModel.showInfo(false)
                        }
                    }
                },
                onSourceClick = { context.openUrl(source) },
            )
        }
    }

    if (uiState.showPermissionRationaleDialog) {
        DownloadPermissionsRationalDialog(
            permissions = downloadPermissionsState.shouldShowRationale.keys.map { it.permission },
            onConfirmOrDismiss = {
                viewModel.showPermissionRationaleDialog(false)
                // notificationPermissionState.launchPermissionRequest()
            },
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
    thumbUrl: String? = null,
    showFullScreenAction: Boolean = false,
    onWallpaperTransform: () -> Unit = {},
    onWallpaperTap: () -> Unit = {},
    onInfoClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onShareLinkClick: () -> Unit = {},
    onShareImageClick: () -> Unit = {},
    onApplyWallpaperClick: () -> Unit = {},
    onFullScreenClick: () -> Unit = {},
) {
    var containerIntSize by remember { mutableStateOf(IntSize.Zero) }

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
                    is SocketTimeoutException -> context.toast(context.getString(R.string.request_timed_out))
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
        key2 = wallpaper?.path,
        key3 = listOf(thumbUrl, imageSize, listener),
    ) {
        if (wallpaper?.path == null && thumbUrl == null) {
            return@produceState
        }
        value = ImageRequest.Builder(context).apply {
            data(wallpaper?.path ?: thumbUrl)
            placeholderMemoryCacheKey(thumbUrl)
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
            wallpaperManager.isWallpaperSupported && wallpaperManager.isSetWallpaperAllowedCompat
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
                painter.request.data == wallpaper?.path &&
                actionsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            WallpaperActions(
                downloadStatus = downloadStatus,
                applyWallpaperEnabled = applyWallpaperEnabled,
                showFullScreenAction = showFullScreenAction,
                onInfoClick = onInfoClick,
                onDownloadClick = onDownloadClick,
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
