package io.github.romantsisyk.mastodon.domain.model

import io.github.romantsisyk.mastodon.utils.AppConstants.MIN_SEARCH_QUERY_LENGTH

@JvmInline
value class SearchQuery private constructor(val value: String) {

    companion object {

        fun create(query: String): SearchQuery? =
            query.takeIf { it.isValid() }?.let(::SearchQuery)

        private fun String.isValid() = length >= MIN_SEARCH_QUERY_LENGTH && isNotBlank()
    }
}