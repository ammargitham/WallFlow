package com.ammar.wallflow.workers

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import com.ammar.wallflow.extensions.await
import com.ammar.wallflow.utils.ContentDisposition
import java.io.File
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.buffer
import okio.sink
import okio.source

suspend fun download(
    okHttpClient: OkHttpClient,
    url: String,
    dir: String,
    fileName: String,
    progressCallback: suspend (total: Long, downloaded: Long) -> Unit,
): File {
    progressCallback(100, -1)
    val downloadRequest = Request.Builder().url(url).build()
    var file: File? = null
    try {
        okHttpClient.newCall(downloadRequest).await().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected code: $response")
            }
            file = createFile(dir, fileName).also {
                (response.body ?: throw IOException("Response body is null")).use { body ->
                    val contentLength = body.contentLength()
                    val source = body.source()
                    it.sink().buffer().use { sink ->
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
    } catch (e: Exception) {
        // delete created file on any exception and rethrow the error
        file?.delete()
        throw e
    }
    return file ?: throw IOException("File null!")
}

private fun getFileNameFromResponse(response: Response): String {
    val contentDispositionStr = response.header("Content-Disposition")
    val parseExceptionMsg = "Could not parse file name from response"
    val pathFileName = response.request.url.pathSegments.joinToString("_")
    return if (!contentDispositionStr.isNullOrEmpty()) {
        ContentDisposition.parse(contentDispositionStr).filename
            ?: throw IllegalArgumentException(parseExceptionMsg)
    } else {
        pathFileName
    }
}

fun createFile(
    dir: String,
    fileName: String,
): File {
    var fileName1 = fileName
    var tempFile = File(dir, fileName1)
    val extension = tempFile.extension
    val nameWoExt = tempFile.nameWithoutExtension
    var suffix = 0
    while (tempFile.exists()) {
        suffix++
        fileName1 = "$nameWoExt-$suffix${if (extension.isNotEmpty()) ".$extension" else ""}"
        tempFile = File(dir, fileName1)
    }
    tempFile.parentFile?.mkdirs()
    val created = tempFile.createNewFile()
    if (!created) {
        throw IOException("Unable to create file: ${tempFile.absolutePath}")
    }
    return tempFile
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

@Throws(IOException::class)
fun copyFiles(
    context: Context,
    source: Uri,
    dest: File,
) {
    var deleteOnError = false
    try {
        if (!dest.exists()) {
            deleteOnError = true
            dest.parentFile?.mkdirs()
            dest.createNewFile()
        }
        context.contentResolver.openInputStream(source)?.use {
            it.source().use { a ->
                dest.sink().buffer().use { b -> b.writeAll(a) }
            }
        }
    } catch (e: Exception) {
        if (deleteOnError) {
            dest.delete()
        }
        throw e
    }
}

@Throws(IOException::class)
fun copyFiles(
    source: File,
    dest: File,
) {
    source.source().use { a ->
        dest.sink().buffer().use { b -> b.writeAll(a) }
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
