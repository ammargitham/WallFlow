package com.ammar.wallflow.ui.common

/**
 * From https://github.com/Commit451/coil-transformations/blob/7ed83c7a9b8b91d4c879b7e089f87a66db284e1a/transformations/src/main/java/com/commit451/coiltransformations/BlurTransformation.kt
 * Copyright 2022 Commit 451
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import coil.size.Size
import coil.transform.Transformation
import com.skydoves.cloudy.internals.render.RenderScriptToolkit

/**
 * A [Transformation] that applies a Gaussian blur to an image.
 *
 * @param radius The radius of the blur.
 * @param sampling The sampling multiplier used to scale the image. Values > 1
 *  will downscale the image. Values between 0 and 1 will upscale the image.
 */
class BlurTransformation constructor(
    private val radius: Int = DEFAULT_RADIUS,
    private val sampling: Int = DEFAULT_SAMPLING,
) : Transformation {

    init {
        require(radius in 0..25) { "radius must be in [0, 25]." }
        require(sampling > 0) { "sampling must be > 0" }
    }

    override val cacheKey = "${BlurTransformation::class.java.name}-$radius-$sampling"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val scaledWidth = input.width / sampling
        val scaledHeight = input.height / sampling
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        return createBitmap(scaledWidth, scaledHeight, input.safeConfig)
            .applyCanvas {
                scale(1f / sampling, 1f / sampling)
                drawBitmap(input, 0f, 0f, paint)
            }.run {
                RenderScriptToolkit.blur(this, radius)
            } ?: throw RuntimeException("Bitmap null")
    }

    private companion object {
        private const val DEFAULT_RADIUS = 10
        private const val DEFAULT_SAMPLING = 1
    }
}

internal val Bitmap.safeConfig: Bitmap.Config
    get() = config ?: Bitmap.Config.ARGB_8888
