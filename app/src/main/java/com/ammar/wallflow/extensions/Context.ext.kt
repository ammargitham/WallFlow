package com.ammar.wallflow.extensions

import android.app.Activity
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.UriPermission
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapRegionDecoder
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.Surface
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.unit.IntSize
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.toRect
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.core.provider.DocumentsContractCompat
import androidx.work.WorkManager
import com.ammar.wallflow.FILE_PROVIDER_AUTHORITY
import com.ammar.wallflow.MIME_TYPE_JPEG
import com.ammar.wallflow.R
import com.ammar.wallflow.WEB_URL_REGEX
import com.ammar.wallflow.model.WallpaperTarget
import com.ammar.wallflow.model.toWhichInt
import com.ammar.wallflow.ui.common.permissions.checkSetWallpaperPermission
import com.ammar.wallflow.utils.getDecodeSampledBitmapOptions
import com.ammar.wallflow.utils.isExternalStorageWritable
import com.ammar.wallflow.utils.withMLModelsDir
import com.ammar.wallflow.utils.withTempDir
import com.lazygeniouz.dfc.file.DocumentFileCompat
import java.io.File
import java.io.InputStream
import okio.buffer
import okio.sink
import okio.source
import okio.use

fun Context.openUrl(url: String) {
    if (!url.matches(WEB_URL_REGEX)) {
        toast(getString(R.string.invalid_url))
        return
    }
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    if (intent.resolveActivity(packageManager) == null) return
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        toast(getString(R.string.no_browser_found))
    } catch (e: Exception) {
        Log.e(TAG, "openUrl", e)
    }
}

fun parseMimeType(url: String) = parseMimeType(File(url))

fun parseMimeType(file: File): String {
    val ext = MimeTypeMap.getFileExtensionFromUrl(file.name)
    var type = MimeTypeMap
        .getSingleton()
        .getMimeTypeFromExtension(ext)
    type = type ?: "*/*"
    return type
}

fun Context.toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

fun Context.setWallpaper(
    display: Display,
    uri: Uri,
    cropRect: Rect,
    targets: Set<WallpaperTarget> = setOf(WallpaperTarget.HOME, WallpaperTarget.LOCKSCREEN),
) = this.contentResolver.openInputStream(uri).use {
    val screenResolution = getScreenResolution(true, display.displayId)
    if (it == null) return@use false
    val decoder = getBitmapRegionDecoder(it) ?: return@use false
    val (opts, _) = getDecodeSampledBitmapOptions(
        context = this,
        uri = uri,
        reqWidth = screenResolution.width,
        reqHeight = screenResolution.height,
    ) ?: return@use false
    val bitmap = decoder.decodeRegion(
        cropRect.toAndroidRectF().toRect(),
        opts,
    ) ?: return@use false
    return createDisplayContext(display).setWallpaper(bitmap, targets)
}

private fun getBitmapRegionDecoder(`is`: InputStream) = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        BitmapRegionDecoder.newInstance(`is`)
    } else {
        @Suppress("DEPRECATION")
        BitmapRegionDecoder.newInstance(`is`, false)
    }
} catch (e: Exception) {
    Log.e("getBitmapRegionDecoder", "Error: ", e)
    null
}

fun Context.setWallpaper(
    bitmap: Bitmap,
    targets: Set<WallpaperTarget> = setOf(WallpaperTarget.HOME, WallpaperTarget.LOCKSCREEN),
): Boolean {
    if (!checkSetWallpaperPermission()) return false
    wallpaperManager.setBitmap(
        bitmap,
        null,
        true,
        targets.toWhichInt(),
    )
    return true
}

fun Context.setWallpaper(
    inputStream: InputStream,
    rect: Rect,
    targets: Set<WallpaperTarget> = setOf(WallpaperTarget.HOME, WallpaperTarget.LOCKSCREEN),
): Boolean {
    if (!checkSetWallpaperPermission()) return false
    wallpaperManager.setStream(
        inputStream,
        rect.toAndroidRectF().toRect(),
        true,
        targets.toWhichInt(),
    )
    return true
}

fun Context.share(
    text: String,
    type: String = "text/plain",
) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        this.type = type
    }
    val intent = Intent.createChooser(sendIntent, null)
    startActivity(intent)
}

fun Context.share(
    uri: Uri,
    type: String,
    title: String? = null,
    grantTempPermission: Boolean = false,
) = startActivity(
    getShareChooserIntent(
        this,
        uri,
        type,
        title,
        grantTempPermission,
    ),
)

fun Context.getShareChooserIntent(
    file: DocumentFileCompat,
    grantTempPermission: Boolean,
): Intent = getShareChooserIntent(
    context = this,
    uri = if (file.uri.scheme == "file") {
        getUriForFile(file.uri.toFile())
    } else {
        file.uri
    },
    type = file.getType() ?: MIME_TYPE_JPEG,
    title = file.name,
    grantTempPermission = grantTempPermission,
)

fun getShareChooserIntent(
    context: Context,
    uri: Uri,
    type: String,
    title: String?,
    grantTempPermission: Boolean,
): Intent = Intent.createChooser(
    Intent().apply {
        action = Intent.ACTION_SEND
        if (grantTempPermission) {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        clipData = ClipData.newUri(context.contentResolver, title, uri)
        putExtra(Intent.EXTRA_STREAM, uri)
        setTypeAndNormalize(type)
    },
    title,
)

val Context.externalFilesDir
    get() = getExternalFilesDir(null)

fun Context.getTempFile(fileName: String) = getAppDir(withTempDir(fileName))
fun Context.getTempDir() = getAppDir(withTempDir())

fun Context.getMLModelsFile(fileName: String) = getAppDir(withMLModelsDir(fileName))
fun Context.getMLModelsDir() = getAppDir(withMLModelsDir())

private fun Context.getAppDir(subPath: String): File {
    val parentFile = if (isExternalStorageWritable()) externalFilesDir else filesDir
    return File(parentFile, subPath)
}

fun Context.getTempFileIfExists(fileName: String): File? {
    val file = getTempFile(fileName)
    return if (file.exists()) file else null
}

fun Context.getMLModelsFileIfExists(fileName: String): File? {
    val file = getMLModelsFile(fileName)
    return if (file.exists()) file else null
}

val Context.displayManager
    get() = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

fun Context.getScreenResolution(
    inDefaultOrientation: Boolean = false,
    displayId: Int = Display.DEFAULT_DISPLAY,
): IntSize {
    val display = displayManager.getDisplay(displayId) ?: return IntSize.Zero
    var changeOrientation = false
    if (inDefaultOrientation) {
        val rotation = display.rotation
        // if current rotation is 90 or 270, device is rotated, so we will swap width and height
        changeOrientation = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270
    }
    val metrics = DisplayMetrics()
    @Suppress("DEPRECATION")
    display.getRealMetrics(metrics)
    val height = metrics.heightPixels
    val width = metrics.widthPixels
    return IntSize(
        width = if (changeOrientation) height else width,
        height = if (changeOrientation) width else height,
    )
}

fun Context.isInDefaultOrientation(): Boolean {
    val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY) ?: return true
    val rotation = display.rotation
    return rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180
}

fun Context.getUriForFile(file: File): Uri = FileProvider.getUriForFile(
    this,
    FILE_PROVIDER_AUTHORITY,
    file,
)

val Context.wallpaperManager: WallpaperManager
    get() = WallpaperManager.getInstance(this)

fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("Permissions should be called in the context of an Activity")
}

val Context.workManager
    get() = WorkManager.getInstance(this)

val Context.notificationManager
    get() = NotificationManagerCompat.from(this)

val Context.accessibleUris: List<UriPermission>
    get() = contentResolver.persistedUriPermissions

val Context.accessibleFolders: List<UriPermission>
    get() = accessibleUris.filter { DocumentsContractCompat.isTreeUri(it.uri) }

fun Context.writeToUri(
    uri: Uri,
    content: String,
) = contentResolver.openOutputStream(uri)?.use { outputStream ->
    outputStream
        .sink()
        .buffer()
        .writeUtf8(content)
        .close()
}

fun Context.readFromUri(
    uri: Uri,
) = contentResolver.openInputStream(uri)?.use { inputStream ->
    val buffer = inputStream.source().buffer()
    buffer.readUtf8()
}

fun Context.isSystemInDarkTheme(): Boolean {
    val uiMode = resources.configuration.uiMode
    return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}

fun Context.isExtraDimActive() = try {
    Settings.Secure.getInt(
        contentResolver,
        "reduce_bright_colors_activated",
        0,
    ) == 1
} catch (e: Exception) {
    Log.e(TAG, "isExtraDimActive: ", e)
    false
}

// From https://stackoverflow.com/a/46848226/1436766
fun Context.restartApp() {
    val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
    val mainIntent = Intent.makeRestartActivityTask(intent.component)
    // Required for API 34 and later
    // Ref: https://developer.android.com/about/versions/14/behavior-changes-14#safer-intents
    mainIntent.setPackage(packageName)
    startActivity(mainIntent)
    Runtime.getRuntime().exit(0)
}

fun Context.sendEmail(
    address: String,
    subject: String,
    body: String,
) {
    try {
        startActivity(
            buildEmailChooserIntent(
                title = getString(R.string.send_email),
                address = address,
                subject = subject,
                body = body,
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    } catch (e: Exception) {
        Log.e(TAG, "sendEmail: No email app found", e)
    }
}

private fun buildEmailChooserIntent(
    title: String,
    address: String,
    subject: String,
    body: String?,
) = Intent.createChooser(
    Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.fromParts("mailto", address, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    },
    title,
)
