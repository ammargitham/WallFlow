package com.ammar.havenwalls.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.ammar.havenwalls.extensions.getScreenResolution

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int,
): Int {
    // Raw height and width of image
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2
        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}


fun decodeSampledBitmapFromFile(
    filePath: String,
    reqWidth: Int,
    reqHeight: Int,
): Bitmap = BitmapFactory.Options().run {
    inJustDecodeBounds = true
    BitmapFactory.decodeFile(filePath, this)
    // Calculate inSampleSize
    inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
    // Decode bitmap with inSampleSize set
    inJustDecodeBounds = false
    BitmapFactory.decodeFile(filePath, this)
}

fun decodeSampledBitmapFromFile(
    context: Context,
    filePath: String,
): Bitmap {
    val resolution = context.getScreenResolution()
    return decodeSampledBitmapFromFile(
        filePath = filePath,
        reqWidth = resolution.width / 2,
        reqHeight = resolution.height / 2,
    )
}

fun decodeSampledBitmapFromUri(
    context: Context,
    uri: Uri,
): Pair<Bitmap, Int>? {
    val resolution = context.getScreenResolution()
    val reqWidth = resolution.width / 2
    val reqHeight = resolution.height / 2
    return decodeSampledBitmapFromUri(
        context = context,
        uri = uri,
        reqWidth = reqWidth,
        reqHeight = reqHeight,
    )
}

fun getDecodeSampledBitmapOptions(
    context: Context,
    uri: Uri,
    reqWidth: Int,
    reqHeight: Int,
): Pair<BitmapFactory.Options, Int>? {
    val inSampleSize = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeStream(it, null, this)
            calculateInSampleSize(this, reqWidth, reqHeight)
        }
    } ?: return null
    return BitmapFactory.Options().run {
        inJustDecodeBounds = false
        this.inSampleSize = inSampleSize
        this
    } to inSampleSize
}

fun decodeSampledBitmapFromUri(
    context: Context,
    uri: Uri,
    reqWidth: Int,
    reqHeight: Int,
): Pair<Bitmap, Int>? {
    val (opts, inSampleSize) = getDecodeSampledBitmapOptions(
        context = context,
        uri = uri,
        reqWidth = reqWidth,
        reqHeight = reqHeight,
    ) ?: return null
    val bitmap = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opts)
    } ?: return null
    return bitmap to inSampleSize
}
