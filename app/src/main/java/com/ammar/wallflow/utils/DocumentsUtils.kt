package com.ammar.wallflow.utils

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.core.provider.DocumentsContractCompat
import com.ammar.wallflow.data.preferences.AppPreferences
import com.ammar.wallflow.extensions.accessibleFolders
import com.ammar.wallflow.model.local.LocalDirectory

fun getRealPath(context: Context, uri: Uri) = try {
    val docUri = if (DocumentsContractCompat.isTreeUri(uri)) {
        DocumentsContract.buildDocumentUriUsingTree(
            uri,
            DocumentsContract.getTreeDocumentId(uri),
        )
    } else {
        uri
    }
    getPath(context, docUri)
} catch (e: Exception) {
    Log.e("getRealPath", "Error getting path", e)
    null
}

fun getPath(context: Context, uri: Uri): String? {
    if (!DocumentsContract.isDocumentUri(context, uri)) return null
    if (isExternalStorageDocument(uri)) {
        val docId = DocumentsContract.getDocumentId(uri)
        val split = docId.split(":").toTypedArray()
        val type = split[0]
        if ("primary".equals(type, ignoreCase = true)) {
            return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
        }
        // TODO handle non-primary volumes
    } else if (isDownloadsDocument(uri)) {
        val id = DocumentsContract.getDocumentId(uri)
        val contentUri = ContentUris.withAppendedId(
            Uri.parse("content://downloads/public_downloads"),
            id.toLong(),
        )
        return getDataColumn(context, contentUri, null, null)
    } else if (isMediaDocument(uri)) {
        val docId = DocumentsContract.getDocumentId(uri)
        val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val contentUri = when (split[0]) {
            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> null
        }
        val selection = "_id=?"
        val selectionArgs = arrayOf(
            split[1],
        )
        return getDataColumn(context, contentUri, selection, selectionArgs)
    }
    return null
}

fun getDataColumn(
    context: Context,
    uri: Uri?,
    selection: String?,
    selectionArgs: Array<String>?,
): String? {
    var cursor: Cursor? = null
    val column = "_data"
    val projection = arrayOf(
        column,
    )
    try {
        cursor = context.contentResolver.query(
            uri!!, projection, selection, selectionArgs,
            null,
        )
        if (cursor != null && cursor.moveToFirst()) {
            val index = cursor.getColumnIndexOrThrow(column)
            return cursor.getString(index)
        }
    } finally {
        cursor?.close()
    }
    return null
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is ExternalStorageProvider.
 */
fun isExternalStorageDocument(uri: Uri): Boolean {
    return "com.android.externalstorage.documents" == uri.authority
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is DownloadsProvider.
 */
fun isDownloadsDocument(uri: Uri): Boolean {
    return "com.android.providers.downloads.documents" == uri.authority
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is MediaProvider.
 */
fun isMediaDocument(uri: Uri): Boolean {
    return "com.android.providers.media.documents" == uri.authority
}

fun getLocalDirs(
    context: Application,
    appPreferences: AppPreferences,
): List<LocalDirectory> {
    val accessibleUris = context.accessibleFolders.map { it.uri }
    val dirs = appPreferences.localWallpapersPreferences.directories
        ?.filter { accessibleUris.contains(it) }
        ?.map {
            LocalDirectory(
                uri = it,
                path = getRealPath(
                    context = context,
                    uri = it,
                ) ?: it.toString(),
            )
        }
        ?: emptyList()
    return dirs
}
