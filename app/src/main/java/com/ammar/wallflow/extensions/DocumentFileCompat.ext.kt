package com.ammar.wallflow.extensions

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.ui.unit.IntSize
import androidx.core.net.toFile
import com.ammar.wallflow.model.local.LocalWallpaper
import com.lazygeniouz.dfc.file.DocumentFileCompat

fun DocumentFileCompat.deepListFiles(): List<DocumentFileCompat> =
    listFiles().fold(mutableListOf()) { acc, file ->
        when {
            file.isDirectory() -> acc.addAll(file.deepListFiles())
            else -> acc.add(file)
        }
        acc
    }

fun DocumentFileCompat.getResolution(
    context: Context,
) = try {
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        val opts = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, opts)
        opts.run {
            IntSize(outWidth, outHeight)
        }
    } ?: IntSize(500, 500)
} catch (e: Exception) {
    Log.e(TAG, "getResolution: ", e)
    IntSize(500, 500)
}

fun DocumentFileCompat.toLocalWallpaper(
    context: Context,
) = run {
    LocalWallpaper(
        id = uri.toString(),
        data = uri,
        fileSize = length,
        resolution = getResolution(context),
        mimeType = getType(),
        name = name,
    )
}

fun DocumentFileCompat.Companion.fromUri(
    context: Context,
    uri: Uri,
): DocumentFileCompat? {
    if (uri.scheme == "file") {
        return fromFile(context, uri.toFile())
    }
    if (isTreeUri(uri)) {
        return fromTreeUri(context, uri)
    }
    return fromSingleUri(context, uri)
}

private fun isTreeUri(uri: Uri): Boolean {
    val paths = uri.pathSegments
    return paths.size >= 2 && "tree" == paths[0]
}
