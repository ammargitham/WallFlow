package com.ammar.wallflow.utils

import android.os.Environment
import java.io.File

fun withAppDir(fileName: String = "") = "WallFlow${File.separator}$fileName"

fun withTempDir(fileName: String = "") = "temp${File.separator}$fileName"

fun withMLModelsDir(fileName: String = "") = "ml${File.separator}models${File.separator}$fileName"

fun isExternalStorageWritable() =
    Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

fun isExternalStorageReadable() = Environment.getExternalStorageState() in
    setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)

fun getPublicDownloadsFile(fileName: String) = File(
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
    withAppDir(fileName),
)

fun getPublicDownloadsDir() = File(
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
    withAppDir(),
)

fun getPublicDownloadsDirWithoutAppDir() =
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
