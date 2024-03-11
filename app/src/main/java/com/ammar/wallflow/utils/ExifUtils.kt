package com.ammar.wallflow.utils

import android.content.Context
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.ammar.wallflow.extensions.trimAll
import com.lazygeniouz.dfc.file.DocumentFileCompat
import okio.use

enum class ExifWriteType {
    APPEND,
    OVERWRITE,
}

fun writeTagsToFile(
    context: Context,
    file: DocumentFileCompat,
    tags: Collection<String>,
    exifWriteType: ExifWriteType,
) {
    try {
        context.contentResolver.openInputStream(file.uri)?.use {
            val exifInterface = ExifInterface(it)
            var userComment = exifInterface.getAttribute(ExifInterface.TAG_USER_COMMENT)
                ?.trimAll()
                ?: ""
            val tagsString = tags.joinToString(",")
            userComment = when (exifWriteType) {
                ExifWriteType.APPEND -> {
                    if (userComment.isBlank()) {
                        tagsString
                    } else {
                        "$userComment $tagsString"
                    }
                }
                ExifWriteType.OVERWRITE -> tagsString
            }
            exifInterface.setAttribute(ExifInterface.TAG_USER_COMMENT, userComment)
            exifInterface.saveAttributes()
        }
    } catch (e: Exception) {
        Log.e("writeTagsToFile", "Error writing tags to file: ", e)
    }
}
