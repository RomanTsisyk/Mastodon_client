package io.github.romantsisyk.mastodon.utils

import java.time.Duration


object AppConstants {
    const val BASE_URL = "https://mas.to"
    const val ACCESS_TOKEN = "fibZnAOa5dHe-TtYTXQk1o_ZqwmtFhm0-TLRP0ksZ9o"

    const val CONNECT_TIMEOUT_SECONDS = 10L
    const val READ_TIMEOUT_SECONDS = 60L
    const val WRITE_TIMEOUT_SECONDS = 30L

    const val MAX_RETRIES = 3
    const val INITIAL_RETRY_DELAY_SECONDS = 1L
    const val MAX_RETRY_DELAY_SECONDS = 10L

    const val SEARCH_DEBOUNCE_DELAY = 300L
    const val MIN_SEARCH_QUERY_LENGTH = 2

    const val DATABASE_NAME = "timeline.db"
    const val DATABASE_VERSION = 1

    val DEFAULT_TIMELINE_ITEM_LIFESPAN: Duration = Duration.ofMinutes(5)
    const val CLEANUP_INTERVAL_MS = 60_000L

    const val DEFAULT_ANIMATION_DURATION = 300
}