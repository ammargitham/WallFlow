package com.ammar.wallflow.ui.screens.wallpaper

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.TaskStackBuilder
import androidx.core.net.toUri
import com.ammar.wallflow.LOCAL_DEEPLINK_SCHEME
import com.ammar.wallflow.activities.main.MainActivity
import com.ammar.wallflow.extensions.urlEncoded
import com.ammar.wallflow.model.Source
import com.ammar.wallflow.model.wallhaven.WallhavenWallpaper
import kotlin.random.Random

const val wallpaperScreenLocalHost = "w"
const val wallpaperScreenLocalDeepLinkUriPattern =
    "$LOCAL_DEEPLINK_SCHEME://$wallpaperScreenLocalHost/{source}/{wallpaperId}"

fun getWallpaperScreenLocalDeepLink(
    source: Source,
    wallpaperId: String,
) = "$LOCAL_DEEPLINK_SCHEME://$wallpaperScreenLocalHost/${source.name}/${wallpaperId.urlEncoded()}"

fun getWallpaperScreenPendingIntent(
    context: Context,
    wallhavenWallpaper: WallhavenWallpaper,
) = getWallpaperScreenPendingIntent(
    context = context,
    source = wallhavenWallpaper.source,
    wallpaperId = wallhavenWallpaper.id,
)

fun getWallpaperScreenPendingIntent(
    context: Context,
    source: Source,
    wallpaperId: String,
): PendingIntent? {
    val deepLinkIntent = Intent(
        Intent.ACTION_VIEW,
        getWallpaperScreenLocalDeepLink(source, wallpaperId).toUri(),
        context,
        MainActivity::class.java,
    )
    return TaskStackBuilder.create(context).run {
        addNextIntentWithParentStack(deepLinkIntent)
        getPendingIntent(
            Random.nextInt(),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT,
        )
    }
}
