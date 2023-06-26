package com.ammar.havenwalls.workers

import com.ammar.havenwalls.extensions.await
import com.ammar.havenwalls.utils.ContentDisposition
import java.io.File
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.buffer
import okio.sink

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
            val tempFile = createFile(response, dir, fileName)
            (response.body ?: throw IOException("Response body is null")).use { body ->
                val contentLength = body.contentLength()
                val source = body.source()
                tempFile.sink().buffer().use { sink ->
                    var totalBytesRead: Long = 0
                    val bufferSize = 8 * 1024
                    var bytesRead: Long
                    while (source.read(
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
            file = tempFile
        }
    } catch (e: Exception) {
        // delete created file on any exception and rethrow the error
        file?.delete()
        throw e
    }
    return file ?: throw IOException("This will never be thrown")
}

private fun createFile(
    response: Response,
    dir: String,
    fileName: String?,
): File {
    var fName = when {
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
    var tempFile = File(dir, fName)
    val extension = tempFile.extension
    val nameWoExt = tempFile.nameWithoutExtension
    var suffix = 0
    while (tempFile.exists()) {
        suffix++
        fName = "$nameWoExt-$suffix${if (extension.isNotEmpty()) ".$extension" else ""}"
        tempFile = File(dir, fName)
    }
    tempFile.parentFile?.mkdirs()
    tempFile.createNewFile()
    return tempFile
}
