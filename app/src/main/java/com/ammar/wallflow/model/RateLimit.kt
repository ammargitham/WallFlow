package com.ammar.wallflow.model

import com.ammar.wallflow.data.db.entity.RateLimitEntity
import kotlinx.datetime.Instant

data class RateLimit(
    val source: OnlineSource,
    val limit: Int? = null,
    val remaining: Int? = null,
    val reset: Instant? = null,
)

fun RateLimit.toEntity(id: Long = 0) = RateLimitEntity(
    id = id,
    source = source,
    limit = limit,
    remaining = remaining,
    reset = reset,
)

const val HEADER_RATELIMIT_LIMIT = "x-ratelimit-limit"
const val HEADER_RATELIMIT_REMAINING = "x-ratelimit-remaining"
const val HEADER_RATELIMIT_USED = "x-ratelimit-used"
const val HEADER_RATELIMIT_RESET = "x-ratelimit-reset"
