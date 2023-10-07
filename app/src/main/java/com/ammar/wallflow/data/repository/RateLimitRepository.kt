package com.ammar.wallflow.data.repository

import com.ammar.wallflow.IoDispatcher
import com.ammar.wallflow.data.db.dao.RateLimitDao
import com.ammar.wallflow.model.RateLimit
import com.ammar.wallflow.model.toEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class RateLimitRepository @Inject constructor(
    private val rateLimitDao: RateLimitDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun upsert(rateLimit: RateLimit) = withContext(ioDispatcher) {
        val existing = rateLimitDao.getBySource(rateLimit.source)
        val updated = existing?.copy(
            limit = rateLimit.limit,
            remaining = rateLimit.remaining,
            reset = rateLimit.reset,
        ) ?: rateLimit.toEntity()
        rateLimitDao.upsert(updated)
    }
}
