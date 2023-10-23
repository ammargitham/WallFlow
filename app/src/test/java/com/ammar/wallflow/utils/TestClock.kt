package com.ammar.wallflow.utils

import kotlin.time.Duration
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * From https://github.com/kotest/kotest-extensions-clock
 * A mutable [Clock] that supports millisecond precision.
 */
class TestClock(
    private var now: Instant,
) : Clock {

    override fun now() = now

    /**
     * Sets the `now` instant in this test clock to the given value.
     */
    fun setNow(instant: Instant) {
        now = instant
    }

    /**
     * Adds the given [duration] from the instant in this test clock.
     */
    operator fun plus(duration: Duration) {
        setNow(now.plus(duration.inWholeMilliseconds, DateTimeUnit.MILLISECOND))
    }

    /**
     * Removes the given [duration] from the instant in this test clock.
     */
    operator fun minus(duration: Duration) {
        setNow(now.minus(duration.inWholeMilliseconds, DateTimeUnit.MILLISECOND))
    }
}
