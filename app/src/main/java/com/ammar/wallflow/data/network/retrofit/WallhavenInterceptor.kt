package com.ammar.wallflow.data.network.retrofit

import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.GlobalErrorsRepository
import com.ammar.wallflow.data.repository.GlobalErrorsRepository.GlobalErrorType
import com.ammar.wallflow.data.repository.GlobalErrorsRepository.RateLimitError
import com.ammar.wallflow.data.repository.GlobalErrorsRepository.WallHavenUnauthorisedError
import com.ammar.wallflow.data.repository.RateLimitRepository
import com.ammar.wallflow.model.HEADER_RATELIMIT_LIMIT
import com.ammar.wallflow.model.HEADER_RATELIMIT_REMAINING
import com.ammar.wallflow.model.OnlineSource
import com.ammar.wallflow.model.RateLimit
import com.ammar.wallflow.model.Source
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

@Singleton
class WallhavenInterceptor @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val globalErrorsRepository: GlobalErrorsRepository,
    private val rateLimitRepository: RateLimitRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.host != "wallhaven.cc") {
            return chain.proceed(request)
        }
        val newRequest = runBlocking { appendApiKey(request) }
        val response = chain.proceed(newRequest)
        runBlocking { checkRateLimit(response) }
        checkForErrors(response)
        return response
    }

    private suspend fun appendApiKey(request: Request): Request {
        var newRequest = request
        val wallHavenApiKey = appPreferencesRepository.getWallHavenApiKey()
        if (wallHavenApiKey.isNotBlank()) {
            val url = newRequest.url.newBuilder()
                .addQueryParameter("apikey", wallHavenApiKey)
                .build()
            newRequest = newRequest.newBuilder()
                .url(url)
                .build()
        }
        return newRequest
    }

    private suspend fun checkRateLimit(response: Response) {
        // wallhaven only sends the limit and remaining headers
        val limitHeader = response.headers.find {
            it.first.equals(HEADER_RATELIMIT_LIMIT, ignoreCase = true)
        }
        val remainingHeader = response.headers.find {
            it.first.equals(HEADER_RATELIMIT_REMAINING, ignoreCase = true)
        }
        rateLimitRepository.upsert(
            RateLimit(
                source = OnlineSource.WALLHAVEN,
                limit = limitHeader?.second?.toIntOrNull(),
                remaining = remainingHeader?.second?.toIntOrNull(),
                reset = null,
            ),
        )
    }

    private fun checkForErrors(response: Response) {
        if (!response.isSuccessful) {
            when (response.code) {
                401 -> globalErrorsRepository.addError(
                    WallHavenUnauthorisedError(),
                    replace = true,
                )

                429 -> globalErrorsRepository.addError(
                    RateLimitError(Source.WALLHAVEN),
                    replace = true,
                )
                else -> {}
            }
        } else {
            // remove any network related errors
            globalErrorsRepository.removeErrorByType(
                GlobalErrorType.WALLHAVEN_UNAUTHORISED,
                GlobalErrorType.RATE_LIMIT,
            )
        }
    }
}
