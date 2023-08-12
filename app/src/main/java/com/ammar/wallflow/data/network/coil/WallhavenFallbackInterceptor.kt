package com.ammar.wallflow.data.network.coil

import android.util.Log
import coil.intercept.Interceptor
import coil.request.ImageResult
import com.ammar.wallflow.extensions.TAG
import java.net.URL

class WallhavenFallbackInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val result = chain.proceed(chain.request)
        val data = chain.request.data
        if (result.drawable != null || data !is String) {
            return result
        }
        val host = URL(data).host
        val fallbackURL = chain.request.parameters.value<String>("fallback_url")
        if (host != "th.wallhaven.cc" || fallbackURL == null) {
            return result
        }
        Log.d(TAG, "intercept: Using fallback URL: $fallbackURL")
        val newRequest = chain.request.newBuilder().data(fallbackURL).build()
        return chain.proceed(newRequest)
    }
}
