package com.ammar.havenwalls.data.network.retrofit

import com.ammar.havenwalls.data.repository.AppPreferencesRepository
import com.ammar.havenwalls.data.repository.GlobalErrorsRepository
import com.ammar.havenwalls.data.repository.GlobalErrorsRepository.GlobalErrorType
import com.ammar.havenwalls.data.repository.GlobalErrorsRepository.WallHavenRateLimitError
import com.ammar.havenwalls.data.repository.GlobalErrorsRepository.WallHavenUnauthorisedError
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallHavenInterceptor @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val globalErrorsRepository: GlobalErrorsRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        if (request.url.host != "wallhaven.cc") {
            return chain.proceed(request)
        }
        val newRequest = runBlocking {
            val wallHavenApiKey = appPreferencesRepository.getWallHavenApiKey()
            if (wallHavenApiKey.isNotBlank()) {
                val url = request.url.newBuilder()
                    .addQueryParameter("apikey", wallHavenApiKey)
                    .build()
                request = request.newBuilder()
                    .url(url)
                    .build()
            }
            request
        }
        val response = chain.proceed(newRequest)
        if (!response.isSuccessful) {
            when (response.code) {
                401 -> globalErrorsRepository.addError(
                    WallHavenUnauthorisedError(),
                    replace = true
                )

                429 -> globalErrorsRepository.addError(
                    WallHavenRateLimitError(),
                    replace = true
                )
            }
        } else {
            // remove any network related errors
            globalErrorsRepository.removeErrorByType(
                GlobalErrorType.WALLHAVEN_UNAUTHORISED,
                GlobalErrorType.WALLHAVEN_RATE_LIMIT
            )
        }
        return response
    }
}
