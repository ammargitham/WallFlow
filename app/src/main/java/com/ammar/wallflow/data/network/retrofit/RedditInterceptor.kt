package com.ammar.wallflow.data.network.retrofit

import com.ammar.wallflow.data.repository.AppPreferencesRepository
import com.ammar.wallflow.data.repository.GlobalErrorsRepository
import com.ammar.wallflow.data.repository.GlobalErrorsRepository.GlobalErrorType
import com.ammar.wallflow.data.repository.GlobalErrorsRepository.RedditUnauthorisedError
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

@Singleton
class RedditInterceptor @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val globalErrorsRepository: GlobalErrorsRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.url.host.endsWith("reddit.com")) {
            return chain.proceed(request)
        }
        val newRequest = runBlocking { appendCookie(request) }
        val response = chain.proceed(newRequest)
        checkForErrors(response)
        return response
    }

    private suspend fun appendCookie(request: Request): Request {
        var newRequest = request
        val redditCookie = appPreferencesRepository.getRedditCookie()
        if (redditCookie.isNotBlank()) {
            newRequest = newRequest.newBuilder()
                .header("Cookie", "session=$redditCookie")
                .build()
        }
        return newRequest
    }

    private fun checkForErrors(response: Response) {
        if (!response.isSuccessful) {
            when (response.code) {
                401, 403 -> globalErrorsRepository.addError(
                    RedditUnauthorisedError(),
                    replace = true,
                )
            }
        } else {
            globalErrorsRepository.removeErrorByType(
                GlobalErrorType.REDDIT_UNAUTHORISED,
            )
        }
    }
}
