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
    fileName: String?,
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
            file = createFile(response, dir, fileName).also {
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

private fun createFile(
    response: Response,
    dir: String,
    fileName: String?,
): File {
    val fName = when {
        fileName != null -> fileName
        else -> {
            val contentDispositionStr = response.header("Content-Disposition")
            val parseExceptionMsg = "Could not parse file name from response"
            if (contentDispositionStr.isNullOrEmpty()) {
                throw IllegalArgumentException(parseExceptionMsg)
            }
            ContentDisposition.parse(contentDispositionStr).filename
                ?: throw IllegalArgumentException(parseExceptionMsg)
        }
    }
    return createFile(dir, fName)
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
    tempFile.createNewFile()
    return tempFile
}

@Throws(IOException::class)
fun copyFiles(
    context: Context,
    source: Uri,
    dest: File,
) {
    context.contentResolver.openInputStream(source)?.use {
        it.source().use { a ->
            dest.sink().buffer().use { b -> b.writeAll(a) }
        }
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
