package io.github.romantsisyk.mastodon.domain.model

import io.github.romantsisyk.mastodon.utils.AppConstants.MIN_SEARCH_QUERY_LENGTH

@JvmInline
value class SearchQuery private constructor(val value: String) {

    companion object {

        val EMPTY = SearchQuery("")

        fun create(query: String): SearchQuery? =
            when {
                query.isEmpty() -> EMPTY
                query.isValid() -> SearchQuery(query)
                else -> null
            }

        private fun String.isValid() = length >= MIN_SEARCH_QUERY_LENGTH && isNotBlank()
    }
}