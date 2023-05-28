package com.ammar.havenwalls

import com.ammar.havenwalls.model.Resolution

const val WALLHAVEN_BASE_URL = "https://wallhaven.cc/api/v1/"

val COMMON_RESOLUTIONS = mapOf(
    "VGA" to Resolution(640, 480),
    "WVGA" to Resolution(768, 480),
    "SVGA" to Resolution(800, 600),
    "WSVGA" to Resolution(1024, 600),
    "XGA" to Resolution(1024, 768),
    "WXGA" to Resolution(1280, 768),
    "WXGA" to Resolution(1280, 800),
    "SXGA" to Resolution(1280, 960),
    "SXGA+" to Resolution(1400, 1050),
    "UXGA" to Resolution(1600, 1200),
    "FHD" to Resolution(1920, 1080),
    "WUXGA" to Resolution(1920, 1200),
    "FHD+" to Resolution(1920, 1280),
    "iPad Air" to Resolution(2360, 1640),
    "iPhone 12" to Resolution(2530, 1170),
    "QHD" to Resolution(2560, 1440),
    "WQXGA" to Resolution(2560, 1600),
    "4k, UHD" to Resolution(3840, 2160),
    "WQUXGA" to Resolution(3840, 2400),
    "5k iMac Retina" to Resolution(5120, 2880),
    "6k Apple Pro XDR" to Resolution(6016, 3384),
)

const val DISABLED_ALPHA = 0.38f

const val FILE_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".fileprovider"
const val LOCAL_DEEPLINK_SCHEME = "havenwalls"

const val EFFICIENT_DET_LITE_0_MODEL_NAME = "EfficientDet-Lite0"
const val EFFICIENT_DET_LITE_0_MODEL_URL =
    "https://tfhub.dev/tensorflow/lite-model/efficientdet/lite0/detection/metadata/1?lite-format=tflite"
const val EFFICIENT_DET_LITE_0_MODEL_FILE_NAME =
    "lite-model_efficientdet_lite0_detection_metadata_1.tflite"

val INTERNAL_MODELS = listOf(
    EFFICIENT_DET_LITE_0_MODEL_NAME,
)
