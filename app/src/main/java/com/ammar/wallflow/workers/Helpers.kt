package com.ammar.wallflow.workers

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.core.net.toFile
import com.ammar.wallflow.MIME_TYPE_ANY
import com.ammar.wallflow.MIME_TYPE_TFLITE_MODEL
import com.ammar.wallflow.extensions.await
import com.ammar.wallflow.extensions.fromUri
import com.lazygeniouz.dfc.file.DocumentFileCompat
import java.io.File
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import okio.source
import okio.use

suspend fun download(
    context: Context,
    okHttpClient: OkHttpClient,
    url: String,
    dirUri: Uri,
    fileName: String,
    mimeType: String,
    progressCallback: suspend (total: Long, downloaded: Long) -> Unit,
): DocumentFileCompat {
    progressCallback(100, -1)
    val downloadRequest = Request.Builder().url(url).build()
    var file: DocumentFileCompat? = null
    try {
        okHttpClient.newCall(downloadRequest).await().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected code: $response")
            }
            file = createFile(
                context,
                dirUri,
                fileName,
                mimeType,
            ).also {
                val responseBody = response.body ?: throw IOException("Response body is null")
                responseBody.use { body ->
                    val contentLength = body.contentLength()
                    val source = body.source()
                    val outputStream = context.contentResolver.openOutputStream(it.uri)
                    outputStream?.use { os ->
                        os.sink().buffer().use { sink ->
                            var totalBytesRead: Long = 0
                            val bufferSize = 8 * 1024
                            var bytesRead: Long
                            while (
                                source.read(
                                    sink.buffer,
                                    bufferSize.toLong(),
                                ).also { bytesRead = it } != -1L
                            ) {
                                sink.emit()
                                totalBytesRead += bytesRead
                                progressCallback(contentLength, totalBytesRead)
                            }
                            sink.flush()
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        // delete created file on any exception and rethrow the error
        file?.delete()
        throw e
    }
    return file ?: throw IOException("File null!")
}

fun createFile(
    context: Context,
    dirUri: Uri,
    fileName: String,
    mimeType: String,
): DocumentFileCompat {
    val fileNameToUse = getUniqueFileName(context, dirUri, fileName)
    val directory = DocumentFileCompat.fromUri(context, dirUri)
        ?: throw IOException("Unable to get directory: $dirUri")
    if (!directory.exists()) {
        // create this directory
        // in this case, it has to be a legacy folder
        // since a tree uri would already exist
        dirUri.toFile().mkdirs()
    }
    return directory.createFile(
        mimeType,
        if (
            mimeType == MIME_TYPE_TFLITE_MODEL ||
            mimeType == MIME_TYPE_ANY
        ) {
            fileNameToUse
        } else {
            getFileNameWithoutExtension(fileNameToUse)
        },
    ) ?: throw IOException("Unable to create file: $fileName in dir: $dirUri")
}

fun getUniqueFileName(
    context: Context,
    dirUri: Uri,
    originalFileName: String,
): String {
    var uniqueFileName = originalFileName
    val directory = DocumentFileCompat.fromUri(context, dirUri)
        ?: throw IOException("Unable to get directory: $dirUri")
    var file = directory.findFile(uniqueFileName)

    // If the file with the original name already exists, rename it
    if (file != null && file.exists()) {
        var count = 1

        val fileNameWithoutExtension = getFileNameWithoutExtension(originalFileName)
        val fileExtension = getFileExtension(originalFileName)

        while (file != null && file.exists()) {
            uniqueFileName = "$fileNameWithoutExtension-$count.$fileExtension"
            file = directory.findFile(uniqueFileName)
            count++
        }
    }

    return uniqueFileName
}

fun getFileNameWithoutExtension(fileName: String): String {
    val dotIndex = fileName.lastIndexOf(".")
    return if (dotIndex != -1 && dotIndex < fileName.length) {
        fileName.substring(0, dotIndex)
    } else {
        fileName
    }
}

private fun getFileExtension(fileName: String): String {
    val dotIndex = fileName.lastIndexOf(".")
    return if (dotIndex != -1 && dotIndex < fileName.length - 1) {
        fileName.substring(dotIndex + 1)
    } else {
        ""
    }
}

fun renameFile(
    file: File,
    newFileName: String,
): Boolean {
    val newFile = File(file.parent, newFileName)
    if (newFile.exists()) {
        throw IOException("A file with name \"${newFile}\" already exists")
    }
    return file.renameTo(newFile)
}

// @Throws(IOException::class)
// fun copyFiles(
//     context: Context,
//     source: Uri,
//     dest: File,
// ) {
//     var deleteOnError = false
//     try {
//         if (!dest.exists()) {
//             deleteOnError = true
//             dest.parentFile?.mkdirs()
//             dest.createNewFile()
//         }
//         context.contentResolver.openInputStream(source)?.use {
//             it.source().use { a ->
//                 dest.sink().buffer().use { b -> b.writeAll(a) }
//             }
//         }
//     } catch (e: Exception) {
//         if (deleteOnError) {
//             dest.delete()
//         }
//         throw e
//     }
// }

@Throws(IOException::class)
fun copyFiles(
    context: Context,
    source: Uri,
    dest: Uri,
) {
    context.contentResolver.openInputStream(source)?.use { `is` ->
        `is`.source().use { a ->
            context.contentResolver.openOutputStream(dest)?.use { os ->
                os.sink().buffer().use { b -> b.writeAll(a) }
            }
        }
    }
}

fun scanFile(context: Context, file: File) {
    var connection: MediaScannerConnection? = null
    connection = MediaScannerConnection(
        context,
        object : MediaScannerConnection.MediaScannerConnectionClient {
            override fun onScanCompleted(path: String?, uri: Uri?) {
                connection?.disconnect()
            }

            override fun onMediaScannerConnected() {
                connection?.scanFile(file.absolutePath, null)
            }
        },
    )
    connection.connect()
}
