package com.ammar.wallflow.ui.screens.wallpaper

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.TaskStackBuilder
import androidx.core.net.toUri
import com.ammar.wallflow.LOCAL_DEEPLINK_SCHEME
import com.ammar.wallflow.activities.main.MainActivity
import com.ammar.wallflow.model.WallhavenWallpaper
import kotlin.random.Random

const val wallpaperScreenLocalHost = "w"
const val wallpaperScreenLocalDeepLinkUriPattern =
    "$LOCAL_DEEPLINK_SCHEME://$wallpaperScreenLocalHost/{wallpaperId}"
const val wallpaperScreenExternalDeepLinkUriPattern = "https://wallhaven.cc/w/{wallpaperId}"
const val wallpaperScreenExternalShortDeepLinkUriPattern = "https://whvn.cc/{wallpaperId}"

fun getWallpaperScreenLocalDeepLink(wallhavenWallpaper: WallhavenWallpaper) =
    getWallpaperScreenLocalDeepLink(wallhavenWallpaper.id)

fun getWallpaperScreenLocalDeepLink(wallpaperId: String) =
    "$LOCAL_DEEPLINK_SCHEME://$wallpaperScreenLocalHost/$wallpaperId"

fun getWallpaperScreenPendingIntent(
    context: Context,
    wallhavenWallpaper: WallhavenWallpaper,
) = getWallpaperScreenPendingIntent(context, wallhavenWallpaper.id)

fun getWallpaperScreenPendingIntent(
    context: Context,
    wallpaperId: String,
): PendingIntent? {
    val deepLinkIntent = Intent(
        Intent.ACTION_VIEW,
        getWallpaperScreenLocalDeepLink(wallpaperId).toUri(),
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
