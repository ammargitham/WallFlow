package com.ammar.wallflow.data.network.coil

import coil.intercept.Interceptor
import coil.request.ImageResult
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
        val newRequest = chain.request.newBuilder().data(fallbackURL).build()
        return chain.proceed(newRequest)
    }
}
