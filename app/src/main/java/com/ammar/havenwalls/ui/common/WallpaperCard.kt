package com.ammar.havenwalls.ui.common

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.ammar.havenwalls.R
import com.ammar.havenwalls.extensions.TAG
import com.ammar.havenwalls.model.Wallpaper
import com.ammar.havenwalls.model.wallpaper1
import com.ammar.havenwalls.ui.theme.HavenWallsTheme
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.fade
import com.google.accompanist.placeholder.material.placeholder

private val cardHeight = 300.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperCard(
    modifier: Modifier = Modifier,
    wallpaper: Wallpaper,
    blur: Boolean = false,
    onClick: (cacheKey: MemoryCache.Key?) -> Unit = {},
) {
    var memoryCacheKey: MemoryCache.Key? by remember { mutableStateOf(null) }
    val listener = remember {
        object : ImageRequest.Listener {
            override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                memoryCacheKey = result.memoryCacheKey
            }
        }
    }
    val context = LocalContext.current
    val request = remember(
        context,
        wallpaper,
        listener
    ) {
        ImageRequest.Builder(context).apply {
            data(wallpaper.thumbs.original)
            crossfade(true)
            listener(listener)
        }.build()
    }

    Card(
        modifier = modifier.aspectRatio(wallpaper.resolution.aspectRatio),
        onClick = { onClick(memoryCacheKey) },
    ) {
        AsyncImage(
            modifier = Modifier
                .blur(if (blur) 16.dp else 0.dp)
                .clip(RectangleShape)
                .fillMaxHeight(),
            model = request,
            placeholder = ColorPainter(wallpaper.colors.firstOrNull() ?: Color.White),
            contentDescription = stringResource(R.string.wallpaper_description),
            contentScale = ContentScale.Crop,
            onError = {
                Log.e(TAG, "Error loading: ${wallpaper.path}", it.result.throwable)
            }
        )
    }
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

@Preview
@Composable
private fun PreviewWallpaperCard() {
    HavenWallsTheme {
        WallpaperCard(
            modifier = Modifier.width(200.dp),
            wallpaper = wallpaper1,
        )
    }
}
