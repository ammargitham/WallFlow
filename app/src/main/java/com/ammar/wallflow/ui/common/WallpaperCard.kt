package com.ammar.wallflow.ui.common

import android.util.Log
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.Parameters
import com.ammar.wallflow.R
import com.ammar.wallflow.data.preferences.ViewedWallpapersLook
import com.ammar.wallflow.extensions.TAG
import com.ammar.wallflow.extensions.aspectRatio
import com.ammar.wallflow.model.LightDarkType
import com.ammar.wallflow.model.Wallpaper
import com.ammar.wallflow.model.isUnspecified
import com.ammar.wallflow.model.wallhaven.WallhavenWallpaper
import com.ammar.wallflow.model.wallhaven.wallhavenWallpaper1
import com.ammar.wallflow.ui.theme.WallFlowTheme
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material3.fade
import com.google.accompanist.placeholder.material3.placeholder
import kotlin.math.roundToInt

private val cardHeight = 300.dp

@Composable
fun WallpaperCard(
    modifier: Modifier = Modifier,
    wallpaper: Wallpaper,
    blur: Boolean = false,
    fixedHeight: Boolean = false,
    roundedCorners: Boolean = true,
    isSelected: Boolean = false,
    isFavorite: Boolean = false,
    isViewed: Boolean = false,
    viewedWallpapersLook: ViewedWallpapersLook = ViewedWallpapersLook.DIM_WITH_LABEL,
    lightDarkTypeFlags: Int = LightDarkType.UNSPECIFIED,
    onClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val request = remember(
        context,
        wallpaper,
    ) {
        ImageRequest.Builder(context).apply {
            data(wallpaper.thumbData ?: wallpaper.data)
            crossfade(true)
            parameters(
                Parameters.Builder().apply {
                    set("fallback_url", wallpaper.data)
                }.build(),
            )
        }.build()
    }
    val transition = updateTransition(isSelected, label = "selection state")
    val selectionColor by transition.animateColor(label = "selection color") {
        if (it) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent
    }
    val selectionCircleColor by transition.animateColor(label = "circle color") {
        if (it) MaterialTheme.colorScheme.onPrimary else Color.Transparent
    }
    val checkImageColor by transition.animateColor(label = "check image color") {
        if (it) MaterialTheme.colorScheme.primary else Color.Transparent
    }
    val checkImageAlpha by transition.animateFloat(label = "check image alpha") {
        if (it) 1f else 0f
    }
    val checkImage = remember(context) {
        ResourcesCompat.getDrawable(
            context.resources,
            R.drawable.baseline_check_24,
            null,
        )?.toBitmap()?.asImageBitmap()
    }
    var loaded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .let {
                if (fixedHeight) {
                    it.height(200.dp)
                } else {
                    it
                        .aspectRatio(wallpaper.resolution.aspectRatio)
                        .heightIn(min = 60.dp)
                }
            }
            .clip(if (roundedCorners) CardDefaults.shape else RectangleShape)
            .clickable(onClick = onClick)
            .testTag("wallpaper"),
    ) {
        AsyncImage(
            modifier = Modifier
                .blur(if (blur) 16.dp else 0.dp)
                .clip(RectangleShape)
                .fillMaxHeight()
                .drawWithContent {
                    drawContent()
                    drawRect(selectionColor)
                    if (!isSelected && isViewed && viewedWallpapersLook in setOf(
                            ViewedWallpapersLook.DIM,
                            ViewedWallpapersLook.DIM_WITH_LABEL,
                            ViewedWallpapersLook.DIM_WITH_ICON,
                        )
                    ) {
                        drawRect(Color.Black.copy(alpha = 0.5f))
                    }
                    val radius = minOf(size.minDimension / 2f, 20.dp.toPx())
                    drawCircle(
                        color = selectionCircleColor,
                        radius = radius,
                    )
                    if (checkImage == null) return@drawWithContent
                    val imageSize = (radius * 1.5).roundToInt()
                    drawImage(
                        image = checkImage,
                        dstOffset = IntOffset(
                            x = (size.width / 2 - imageSize / 2).roundToInt(),
                            y = (size.height / 2 - imageSize / 2).roundToInt(),
                        ),
                        dstSize = IntSize(imageSize, imageSize),
                        colorFilter = ColorFilter.tint(checkImageColor),
                        alpha = checkImageAlpha,
                    )
                },
            model = request,
            placeholder = ColorPainter(
                if (wallpaper is WallhavenWallpaper) {
                    wallpaper.colors.firstOrNull() ?: Color.White
                } else {
                    Color.White
                },
            ),
            fallback = ColorPainter(
                if (wallpaper is WallhavenWallpaper) {
                    wallpaper.colors.firstOrNull() ?: Color.White
                } else {
                    Color.White
                },
            ),
            error = ColorPainter(
                if (wallpaper is WallhavenWallpaper) {
                    wallpaper.colors.firstOrNull() ?: Color.White
                } else {
                    Color.White
                },
            ),
            contentDescription = stringResource(R.string.wallpaper),
            contentScale = ContentScale.Crop,
            onError = {
                Log.e(
                    TAG,
                    "Error loading: ${wallpaper.thumbData}",
                    it.result.throwable,
                )
            },
            onSuccess = { loaded = true },
        )
        if (isViewed && viewedWallpapersLook in setOf(
                ViewedWallpapersLook.DIM_WITH_LABEL,
                ViewedWallpapersLook.LABEL,
            )
        ) {
            CardLabel(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = 8.dp,
                        bottom = 8.dp,
                    ),
                text = stringResource(R.string.viewed),
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    top = 8.dp,
                    start = 8.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isViewed && viewedWallpapersLook in setOf(
                    ViewedWallpapersLook.DIM_WITH_ICON,
                    ViewedWallpapersLook.ICON,
                )
            ) {
                CardViewedIcon(modifier = Modifier)
            }
            if (!lightDarkTypeFlags.isUnspecified()) {
                CardLightDarkIcon(
                    typeFlags = lightDarkTypeFlags,
                )
            }
        }
        CardFavoriteButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    bottom = 4.dp,
                    end = 4.dp,
                ),
            isFavorite = isFavorite,
            onClick = onFavoriteClick,
        )
    }
    ReportDrawnWhen { loaded }
}

@Composable
fun PlaceholderWallpaperCard(
    modifier: Modifier = Modifier,
    loading: Boolean = true,
) {
    Column(
        modifier = modifier
            .height(cardHeight)
            .clip(RectangleShape)
            .placeholder(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                visible = loading,
                highlight = PlaceholderHighlight.fade(),
            ),
    ) {}
}

private data class Props(
    val blur: Boolean = false,
    val fixedHeight: Boolean = false,
    val roundedCorners: Boolean = true,
    val isSelected: Boolean = false,
    val isFavorite: Boolean = false,
    val isViewed: Boolean = false,
    val viewedWallpapersLook: ViewedWallpapersLook = ViewedWallpapersLook.DIM_WITH_LABEL,
    val lightDarkTypeFlags: Int = LightDarkType.UNSPECIFIED,
)

private class PreviewProps : CollectionPreviewParameterProvider<Props>(
    listOf(
        Props(),
        Props(fixedHeight = true),
        Props(roundedCorners = false),
        Props(isSelected = true),
        Props(isFavorite = true),
        Props(isViewed = true),
        Props(lightDarkTypeFlags = LightDarkType.DARK),
        Props(
            isViewed = true,
            viewedWallpapersLook = ViewedWallpapersLook.ICON,
            lightDarkTypeFlags = LightDarkType.DARK,
        ),
    ),
)

@Preview
@Composable
private fun PreviewWallpaperCard(
    @PreviewParameter(PreviewProps::class) props: Props,
) {
    val (
        blur,
        fixedHeight,
        roundedCorners,
        isSelected,
        isFavorite,
        isViewed,
        viewedWallpapersLook,
        lightDarkTypeFlags,
    ) = props
    WallFlowTheme {
        WallpaperCard(
            modifier = Modifier.width(200.dp),
            wallpaper = wallhavenWallpaper1,
            blur = blur,
            fixedHeight = fixedHeight,
            roundedCorners = roundedCorners,
            isSelected = isSelected,
            isFavorite = isFavorite,
            isViewed = isViewed,
            viewedWallpapersLook = viewedWallpapersLook,
            lightDarkTypeFlags = lightDarkTypeFlags,
        )
    }
}
