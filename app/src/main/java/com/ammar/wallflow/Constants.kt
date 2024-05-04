package com.ammar.wallflow

import androidx.compose.ui.unit.IntSize
import androidx.core.util.PatternsCompat

const val WALLHAVEN_BASE_URL = "https://wallhaven.cc/api/v1/"
const val REDDIT_BASE_URL = "https://reddit.com/"
const val ISSUE_TRACKER_URL = "https://github.com/ammargitham/WallFlow/issues"
const val BUG_REPORTS_EMAIL_ADDRESS = "bug-reports@wallflow.app"

val COMMON_RESOLUTIONS = mapOf(
    "VGA" to IntSize(640, 480),
    "WVGA" to IntSize(768, 480),
    "SVGA" to IntSize(800, 600),
    "WSVGA" to IntSize(1024, 600),
    "XGA" to IntSize(1024, 768),
    "WXGA" to IntSize(1280, 768),
    "WXGA" to IntSize(1280, 800),
    "SXGA" to IntSize(1280, 960),
    "SXGA+" to IntSize(1400, 1050),
    "UXGA" to IntSize(1600, 1200),
    "FHD" to IntSize(1920, 1080),
    "WUXGA" to IntSize(1920, 1200),
    "FHD+" to IntSize(1920, 1280),
    "Pixel 7" to IntSize(1080, 2400),
    "Pixel 7 Pro" to IntSize(1440, 3120),
    "QHD" to IntSize(2560, 1440),
    "WQXGA" to IntSize(2560, 1600),
    "4k, UHD" to IntSize(3840, 2160),
    "WQUXGA" to IntSize(3840, 2400),
    "5k iMac Retina" to IntSize(5120, 2880),
    "6k Apple Pro XDR" to IntSize(6016, 3384),
)

const val DISABLED_ALPHA = 0.38f

const val FILE_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".fileprovider"
const val LOCAL_DEEPLINK_SCHEME = "wallflow"

const val EFFICIENT_DET_LITE_0_MODEL_NAME = "EfficientDet-Lite0"
const val EFFICIENT_DET_LITE_0_MODEL_URL = "https://tfhub.dev/tensorflow/lite-model/" +
    "efficientdet/lite0/detection/metadata/1?lite-format=tflite"
const val EFFICIENT_DET_LITE_0_MODEL_FILE_NAME =
    "lite-model_efficientdet_lite0_detection_metadata_1.tflite"

val INTERNAL_MODELS = listOf(
    EFFICIENT_DET_LITE_0_MODEL_NAME,
)

const val MIME_TYPE_BMP = "image/bmp"
const val MIME_TYPE_HEIC = "image/heic"
const val MIME_TYPE_JPEG = "image/jpeg"
const val MIME_TYPE_PNG = "image/png"
const val MIME_TYPE_WEBP = "image/webp"
const val MIME_TYPE_JSON = "application/json"
const val MIME_TYPE_ANY = "*/*"
const val MIME_TYPE_TFLITE_MODEL = "application/tflite-model"

val SUPPORTED_MIME_TYPES = setOf(
    MIME_TYPE_BMP,
    MIME_TYPE_HEIC,
    MIME_TYPE_JPEG,
    MIME_TYPE_PNG,
    MIME_TYPE_WEBP,
)

val SUBREDDIT_REGEX = "(?>/?r/)?([a-zA-Z0-9][_a-zA-Z0-9]{2,20})(?>\\Z|\\s)".toRegex()
val WEB_URL_REGEX = PatternsCompat.WEB_URL.toRegex()

// https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/os/FileUtils.java;l=129
val VALID_FILE_NAME_REGEX = "[\\w%+,./=_-]+".toRegex()
