package io.github.romantsisyk.mastodon.utils

import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

class RetryPolicy(
    private val maxAttempts: Int,
    private val initialDelay: Duration,
    private val maxDelay: Duration,
    private val factor: Double = 2.0
) {
    suspend fun <T> retry(block: suspend () -> T): T {
        var currentDelay = initialDelay
        repeat(maxAttempts - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                if (attempt == maxAttempts - 1) throw e

                delay(currentDelay.inWholeMilliseconds)
                currentDelay = (currentDelay.toDouble(DurationUnit.MILLISECONDS) * factor)
                    .coerceAtMost(maxDelay.toDouble(DurationUnit.MILLISECONDS))
                    .milliseconds
            }
        }
        return block()
    }
}