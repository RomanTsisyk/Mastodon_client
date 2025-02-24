package io.github.romantsisyk.mastodon.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SearchDebouncer(
    private val scope: CoroutineScope,
    delayMillis: Long
) {
    private val channel = Channel<String>()

    @OptIn(FlowPreview::class)
    val flow = channel.receiveAsFlow().debounce(delayMillis)

    fun accept(query: String) {
        scope.launch {
            channel.send(query)
        }
    }
}

